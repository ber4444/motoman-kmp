package com.marcowong.motoman

import com.marcowong.motoman.assets.ClasspathAssets
import com.marcowong.motoman.gl.GlslTarget

/**
 * Desktop entry point.
 *
 *   --frames N   auto-exit after N frames (CI smoke run); omit for an interactive window
 *   --model P    asset path of the OBJ to display (default: the bike)
 *   --game       run the full game instead of the single-model viewer
 *   --capture P  write the final frame to P as a PNG (needs --frames)
 *   --drive T    hold throttle at T (0..1) so a scripted capture is taken in motion
 *
 * Requires `-XstartOnFirstThread` on macOS; `:motoman:runDesktop` adds it there.
 */
fun main(args: Array<String>) {
    val argv = args.toList()
    fun opt(name: String): String? = argv.zipWithNext().firstOrNull { it.first == name }?.second

    val frames = opt("--frames")?.toIntOrNull() ?: 0
    val modelPath = opt("--model") ?: "data/bike.obj"
    val dumpPath = opt("--dump")

    val isGame = argv.contains("--game")
    
    val app: GameApp = if (isGame) {
        val generator = com.marcowong.motoman.track.TrackGenerator()
        val trackData = generator.generate() ?: error("Failed to generate track data")
        val assets = ClasspathAssets()
        MotomanGameApp(
            assets = assets,
            trackData = trackData,
            glslTarget = GlslTarget.DESKTOP_120,
            audio = com.marcowong.motoman.audio.DesktopAudio(assets),
            haptics = com.marcowong.motoman.audio.DesktopHaptics(),
            config = RenderConfig(
                resolutionReduction = opt("--res")?.toFloatOrNull() ?: 0.5f,
                modelTextureLinearFilter = argv.contains("--tex-linear"),
                frameBufferLinearFilter = argv.contains("--fb-linear"),
                bloom = !argv.contains("--no-bloom"),
                motionBlur = !argv.contains("--no-mb"),
                antiAliasing = !argv.contains("--no-aa"),
            ),
        )
    } else {
        ModelViewerApp(
            assets = ClasspathAssets(),
            modelPath = modelPath,
            glslTarget = GlslTarget.DESKTOP_120,
            sampleFramebuffer = frames > 0,
            batched = argv.contains("--batched"),
        )
    }

    val host = DesktopHost(
        title = if (isGame) "Motoman" else "Motoman — $modelPath",
        debugGl = true,
        maxFrames = frames,
        // Scripted runs must be reproducible frame-for-frame.
        fixedTimestep = if (frames > 0) 1f / 60f else null,
        capturePath = opt("--capture"),
        scriptedThrottle = opt("--drive")?.toFloatOrNull() ?: 0f,
    )
    host.run(app)

    println("model:      $modelPath${if (argv.contains("--batched")) " (batched)" else ""}")
    if (app is ModelViewerApp && app.shaderLog.isNotBlank()) println("shader log: ${app.shaderLog.trim()}")
    println("GL errors:  ${host.glErrorCount}")

    if (frames > 0 && app is ModelViewerApp) {
        val pct = app.drawnPixelFraction * 100f
        println("drawn:      ${(pct * 100).toInt() / 100f}% of pixels")
        // A blank frame means the pipeline silently failed somewhere upstream.
        check(app.drawnPixelFraction > 0.001f) {
            "nothing was drawn — the frame is entirely clear colour"
        }
    }
    // Optional raw RGBA dump so a frame can be eyeballed rather than only measured.
    if (dumpPath != null && app is ModelViewerApp) {
        app.lastFramePixels?.let { pixels ->
            java.io.DataOutputStream(java.io.File(dumpPath).outputStream().buffered()).use { out ->
                out.writeInt(app.frameWidth)
                out.writeInt(app.frameHeight)
                out.write(pixels)
            }
            println("dumped:     $dumpPath (${app.frameWidth}x${app.frameHeight} RGBA)")
        }
    }

    check(host.glErrorCount == 0) { "run reported ${host.glErrorCount} GL error(s)" }
}

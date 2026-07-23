package com.marcowong.motoman

import com.marcowong.motoman.assets.ClasspathAssets
import com.marcowong.motoman.gl.GlslTarget

/**
 * Desktop entry point.
 *
 *   --frames N   auto-exit after N frames (CI smoke run); omit for an interactive window
 *   --model P    asset path of the OBJ to display (default: the bike)
 *
 * Requires `-XstartOnFirstThread` on macOS; `:motoman:runDesktop` adds it there.
 */
fun main(args: Array<String>) {
    val argv = args.toList()
    fun opt(name: String): String? = argv.zipWithNext().firstOrNull { it.first == name }?.second

    val frames = opt("--frames")?.toIntOrNull() ?: 0
    val modelPath = opt("--model") ?: "data/bike.obj"
    val dumpPath = opt("--dump")

    val app = ModelViewerApp(
        assets = ClasspathAssets(),
        modelPath = modelPath,
        glslTarget = GlslTarget.DESKTOP_120,
        // Sampling the framebuffer is only worth its cost on the scripted smoke run.
        sampleFramebuffer = frames > 0,
        batched = argv.contains("--batched"),
    )

    val host = DesktopHost(
        title = "Motoman — $modelPath",
        debugGl = true,
        maxFrames = frames,
        // Scripted runs must be reproducible frame-for-frame.
        fixedTimestep = if (frames > 0) 1f / 60f else null,
    )
    host.run(app)

    println("model:      $modelPath${if (argv.contains("--batched")) " (batched)" else ""}")
    if (app.shaderLog.isNotBlank()) println("shader log: ${app.shaderLog.trim()}")
    println("GL errors:  ${host.glErrorCount}")

    if (frames > 0) {
        val pct = app.drawnPixelFraction * 100f
        println("drawn:      ${(pct * 100).toInt() / 100f}% of pixels")
        // A blank frame means the pipeline silently failed somewhere upstream.
        check(app.drawnPixelFraction > 0.001f) {
            "nothing was drawn — the frame is entirely clear colour"
        }
    }
    // Optional raw RGBA dump so a frame can be eyeballed rather than only measured.
    if (dumpPath != null) {
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

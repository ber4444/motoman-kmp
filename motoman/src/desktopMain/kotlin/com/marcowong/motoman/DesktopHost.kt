package com.marcowong.motoman

import com.marcowong.motoman.gl.GL_RGBA
import com.marcowong.motoman.gl.GL_UNSIGNED_BYTE
import com.marcowong.motoman.gl.Gl
import com.marcowong.motoman.gl.GlDebug
import com.marcowong.motoman.gl.GlOptimized
import com.marcowong.motoman.gl.createPlatformGl
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL as LwjglGL
import org.lwjgl.system.MemoryUtil.NULL

/**
 * Plain LWJGL 3 / GLFW window that drives a [GameApp]. No Compose (Phase 3 Correction 1):
 * desktop exists for the fast edit-run loop on the renderer, not the HUD.
 *
 * **macOS note:** GLFW must own the first thread, so the JVM has to start with
 * `-XstartOnFirstThread`. The `:motoman-desktop:run` task adds this automatically on macOS;
 * launching by hand without it will abort inside `glfwInit`.
 *
 * A GL 2.1 / GLSL 120 context is requested to match the engine's GLES 2.0-era shaders
 * (see `ShaderPreprocessor`). Requesting a 3.2+ core profile would reject them.
 */
class DesktopHost(
    private val title: String = "Motoman",
    private var width: Int = 1280,
    private var height: Int = 720,
    private val debugGl: Boolean = false,
    /** Auto-exit after this many frames (0 = run until closed). Used for CI smoke runs. */
    private val maxFrames: Int = 0,
    /**
     * When set, every frame advances by exactly this many seconds instead of wall-clock
     * time. Makes scripted runs reproducible frame-for-frame, which wall-clock dt is not.
     */
    private val fixedTimestep: Float? = null,
    /**
     * When set, the final frame is read back and written to this path as a PNG. Works for any
     * [GameApp], which is what makes cross-platform frame comparison possible at all.
     */
    private val capturePath: String? = null,
    /**
     * Constant throttle (0..1) for scripted runs, so a capture can be taken with the bike
     * moving. Parity against the original's mid-race screenshot needs motion; a capture at the
     * start line exercises neither motion blur nor the track ahead. Full throttle from a
     * standstill highsides the bike, so a partial value is usually what you want.
     */
    private val scriptedThrottle: Float = 0f,
    /**
     * When set, steer is taken from this function of the frame index instead of the keyboard,
     * so a turn-and-recover maneuver can be replayed deterministically for steering tests.
     */
    private val scriptedSteer: ((Int) -> Float)? = null,
    /**
     * When > 0 (with [capturePath] set), a PNG is written every this-many frames to
     * `<capturePath>_<frame>.png`, producing a filmstrip of a scripted maneuver.
     */
    private val captureEveryFrames: Int = 0,
    /**
     * Opt into a High-DPI (Retina) framebuffer on macOS. When on, the drawable is the display's
     * real pixel size — e.g. a 1280x720 window backs a 2560x1440 framebuffer on a 2x display —
     * so the scene is rendered and shown at native sharpness instead of being upscaled by the OS.
     * Left off for frame captures, whose readback size must stay at the logical resolution.
     */
    private val retina: Boolean = true,
) {
    /** GL errors seen when [debugGl] is on; the verification plan asserts this is zero. */
    var glErrorCount: Int = 0
        private set

    private var window: Long = NULL
    private val input = InputState()
    private lateinit var gl: Gl

    fun run(app: GameApp) {
        val errorCallback = GLFWErrorCallback.createPrint(System.err)
        errorCallback.set()
        check(GLFW.glfwInit()) {
            "Unable to initialise GLFW. On macOS the JVM must be started with -XstartOnFirstThread."
        }
        try {
            window = createWindow()
            GLFW.glfwMakeContextCurrent(window)
            LwjglGL.createCapabilities()
            GLFW.glfwSwapInterval(1) // vsync
            GLFW.glfwShowWindow(window)

            // The window is sized in points, but everything downstream works in framebuffer
            // pixels (viewport, scene FBOs, glReadPixels). With Retina the two differ, so seed
            // the initial size from the actual drawable rather than the requested window size.
            val fbW = IntArray(1); val fbH = IntArray(1)
            GLFW.glfwGetFramebufferSize(window, fbW, fbH)
            width = fbW[0]; height = fbH[0]

            var raw: Gl = createPlatformGl()
            val debug = if (debugGl) GlDebug(raw).also { raw = it } else null
            gl = GlOptimized(raw)

            app.create(gl, width, height)
            app.resize(width, height)
            loop(app)
            app.dispose()
            glErrorCount = debug?.errorCount ?: 0
        } finally {
            if (window != NULL) GLFW.glfwDestroyWindow(window)
            GLFW.glfwTerminate()
            errorCallback.free()
        }
    }

    private fun createWindow(): Long {
        GLFW.glfwDefaultWindowHints()
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE)
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE)
        // Compatibility GL 2.1: the engine's shaders are GLSL ES 100 -> GLSL 120.
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 2)
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 1)
        GLFW.glfwWindowHint(GLFW.GLFW_DEPTH_BITS, 24)
        // macOS-only: back the window with a full-resolution Retina drawable instead of a
        // point-sized one the OS stretches. Ignored on other platforms.
        GLFW.glfwWindowHint(GLFW.GLFW_COCOA_RETINA_FRAMEBUFFER, if (retina) GLFW.GLFW_TRUE else GLFW.GLFW_FALSE)

        val handle = GLFW.glfwCreateWindow(width, height, title, NULL, NULL)
        check(handle != NULL) { "Failed to create the GLFW window" }
        return handle
    }

    private fun loop(app: GameApp) {
        var lastTime = GLFW.glfwGetTime()
        val widthBuf = IntArray(1)
        val heightBuf = IntArray(1)

        var frame = 0
        while (!GLFW.glfwWindowShouldClose(window)) {
            if (maxFrames > 0 && frame >= maxFrames) break
            frame++
            GLFW.glfwPollEvents()

            GLFW.glfwGetFramebufferSize(window, widthBuf, heightBuf)
            if (widthBuf[0] != width || heightBuf[0] != height) {
                width = widthBuf[0]
                height = heightBuf[0]
                app.resize(width, height)
            }

            val now = GLFW.glfwGetTime()
            // Clamp dt so a breakpoint or a stalled frame cannot explode the physics step.
            val dt = fixedTimestep
                ?: ((now - lastTime).toFloat()).coerceAtMost(MAX_FRAME_SECONDS)
            lastTime = now

            pollInput()
            scriptedSteer?.let { input.steer = it(frame) }
            app.update(dt, input)
            app.render()

            // Read back before the swap: after swapping, the back buffer contents are undefined.
            if (capturePath != null) {
                if (captureEveryFrames > 0 && frame % captureEveryFrames == 0) {
                    capture(capturePath.removeSuffix(".png") + "_$frame.png")
                }
                if (maxFrames > 0 && frame >= maxFrames) capture(capturePath)
            }

            GLFW.glfwSwapBuffers(window)
        }
    }

    /** Reads the current framebuffer and writes it as a PNG, flipped to top-left origin. */
    private fun capture(path: String) {
        val pixels = gl.glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE)
        val image = java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_RGB)
        for (y in 0 until height) {
            // glReadPixels is bottom-left origin; BufferedImage is top-left.
            val srcRow = (height - 1 - y) * width * 4
            for (x in 0 until width) {
                val i = srcRow + x * 4
                val r = pixels[i].toInt() and 0xFF
                val g = pixels[i + 1].toInt() and 0xFF
                val b = pixels[i + 2].toInt() and 0xFF
                image.setRGB(x, y, (r shl 16) or (g shl 8) or b)
            }
        }
        val file = java.io.File(path)
        javax.imageio.ImageIO.write(image, "png", file)
        println("captured:   $path (${width}x$height)")
    }

    private fun down(key: Int): Boolean = GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS

    private fun pollInput() {
        val left = down(GLFW.GLFW_KEY_LEFT) || down(GLFW.GLFW_KEY_A)
        val right = down(GLFW.GLFW_KEY_RIGHT) || down(GLFW.GLFW_KEY_D)
        input.steer = (if (right) 1f else 0f) - (if (left) 1f else 0f)
        input.throttle = when {
            scriptedThrottle > 0f -> scriptedThrottle
            down(GLFW.GLFW_KEY_UP) || down(GLFW.GLFW_KEY_W) -> 1f
            else -> 0f
        }
        input.brake = if (down(GLFW.GLFW_KEY_DOWN) || down(GLFW.GLFW_KEY_S)) 1f else 0f
        input.shiftUp = down(GLFW.GLFW_KEY_E)
        input.shiftDown = down(GLFW.GLFW_KEY_Q)

        if (down(GLFW.GLFW_KEY_ESCAPE)) GLFW.glfwSetWindowShouldClose(window, true)
    }

    private companion object {
        /** 4 frames at 60Hz; beyond this we assume the process was paused, not slow. */
        const val MAX_FRAME_SECONDS = 1f / 15f
    }
}

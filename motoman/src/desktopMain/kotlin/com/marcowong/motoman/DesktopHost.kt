package com.marcowong.motoman

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
) {
    /** GL errors seen when [debugGl] is on; the verification plan asserts this is zero. */
    var glErrorCount: Int = 0
        private set

    private var window: Long = NULL
    private val input = InputState()

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

            var gl: Gl = createPlatformGl()
            val debug = if (debugGl) GlDebug(gl).also { gl = it } else null
            gl = GlOptimized(gl)

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
            app.update(dt, input)
            app.render()

            GLFW.glfwSwapBuffers(window)
        }
    }

    private fun down(key: Int): Boolean = GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS

    private fun pollInput() {
        val left = down(GLFW.GLFW_KEY_LEFT) || down(GLFW.GLFW_KEY_A)
        val right = down(GLFW.GLFW_KEY_RIGHT) || down(GLFW.GLFW_KEY_D)
        input.steer = (if (right) 1f else 0f) - (if (left) 1f else 0f)
        input.throttle = if (down(GLFW.GLFW_KEY_UP) || down(GLFW.GLFW_KEY_W)) 1f else 0f
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

package com.marcowong.motoman

import com.marcowong.motoman.gl.GL_COLOR_BUFFER_BIT
import com.marcowong.motoman.gl.GL_DEPTH_BUFFER_BIT
import com.marcowong.motoman.gl.GL_DEPTH_TEST
import com.marcowong.motoman.gl.GL_RENDERER
import com.marcowong.motoman.gl.GL_SHADING_LANGUAGE_VERSION
import com.marcowong.motoman.gl.GL_VERSION
import com.marcowong.motoman.gl.Gl

/**
 * Minimal [GameApp] that brings up a context and clears the screen. It exists to prove the
 * host + `Gl` decorator chain work end-to-end on a real driver before any geometry lands.
 */
class SmokeApp : GameApp {
    private lateinit var gl: Gl

    override fun create(gl: Gl, width: Int, height: Int) {
        this.gl = gl
        println("GL_VERSION  = ${gl.glGetString(GL_VERSION)}")
        println("GL_RENDERER = ${gl.glGetString(GL_RENDERER)}")
        println("GLSL        = ${gl.glGetString(GL_SHADING_LANGUAGE_VERSION)}")
        gl.glEnable(GL_DEPTH_TEST)
        gl.glClearColor(0.1f, 0.12f, 0.16f, 1f)
    }

    override fun resize(width: Int, height: Int) {
        gl.glViewport(0, 0, width, height)
    }

    override fun update(dt: Float, input: InputState) = Unit

    override fun render() {
        gl.glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
    }

    override fun dispose() = Unit
}

/**
 * Desktop entry point. Pass `--frames N` to auto-exit after N frames (CI smoke run);
 * omit it for an interactive window. Requires `-XstartOnFirstThread` on macOS.
 */
fun main(args: Array<String>) {
    val frames = args.toList().zipWithNext()
        .firstOrNull { it.first == "--frames" }
        ?.second?.toIntOrNull() ?: 0

    val host = DesktopHost(title = "Motoman (smoke)", debugGl = true, maxFrames = frames)
    host.run(SmokeApp())

    println("GL errors: ${host.glErrorCount}")
    if (host.glErrorCount != 0) {
        error("smoke run reported ${host.glErrorCount} GL error(s)")
    }
}

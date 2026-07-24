package com.marcowong.motoman

import com.marcowong.motoman.gl.Gl

/** Digital inputs the game reads, mapped from keyboard on desktop and tilt/touch on Android. */
class InputState {
    var steer: Float = 0f      // -1 left .. +1 right
    var throttle: Float = 0f   // 0 .. 1
    var brake: Float = 0f      // 0 .. 1
    var shiftUp: Boolean = false
    var shiftDown: Boolean = false
}

/**
 * The platform-agnostic game loop. Per Phase 3 Correction 4 these stay ordinary common
 * functions with no assumption about which thread drives them: a `GLSurfaceView.Renderer`
 * calls them on Android's GL thread, `DesktopHost` calls them on GLFW's main thread.
 */
interface GameApp {
    fun create(gl: Gl, width: Int, height: Int)
    fun resize(width: Int, height: Int)
    fun update(dt: Float, input: InputState)
    fun render()
    fun dispose()
}

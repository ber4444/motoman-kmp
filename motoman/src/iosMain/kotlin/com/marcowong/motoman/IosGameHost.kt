package com.marcowong.motoman

import com.marcowong.motoman.assets.IosAssets
import com.marcowong.motoman.audio.IosAudio
import com.marcowong.motoman.audio.IosHaptics
import com.marcowong.motoman.gl.Gl
import com.marcowong.motoman.gl.GlDebug
import com.marcowong.motoman.gl.GlOptimized
import com.marcowong.motoman.gl.createPlatformGl
import com.marcowong.motoman.gl.GlslTarget
import com.marcowong.motoman.gl.IosGl
import com.marcowong.motoman.track.TrackGenerator
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.cinterop.convert
import kotlinx.cinterop.IntVar
import platform.gles2.GL_FRAMEBUFFER_BINDING
import platform.gles2.glGetIntegerv

class IosGameHost(private val debugGl: Boolean = false) {
    var glErrorCount: Int = 0
        private set

    lateinit var app: MotomanGameApp
    private lateinit var rawGl: Gl
    private var debug: GlDebug? = null
    private val input = InputState()
    
    // Default throttle to 0.7f as per Android's MAX_THROTTLE cap.
    init {
        input.throttle = 0.7f
    }

    fun create(widthPx: Int, heightPx: Int) {
        rawGl = createPlatformGl()
        debug = if (debugGl) GlDebug(rawGl).also { rawGl = it } else null
        val gl = GlOptimized(rawGl)

        val assets = IosAssets()
        val audio = IosAudio()
        val haptics = IosHaptics()
        val trackData = TrackGenerator().generate()!!

        app = MotomanGameApp(assets, trackData, GlslTarget.ES_100, audio, haptics, RenderConfig.HIGH_QUALITY)
        app.create(gl, widthPx, heightPx)
    }

    fun resize(widthPx: Int, heightPx: Int) {
        app.resize(widthPx, heightPx)
    }

    fun render(dtSeconds: Float) {
        // Read the default FBO from GLKit
        val fbo = memScoped {
            val v = alloc<IntVar>()
            glGetIntegerv(GL_FRAMEBUFFER_BINDING.convert(), v.ptr)
            v.value
        }
        
        var currentGl = rawGl
        if (currentGl is GlDebug) {
            currentGl = currentGl.delegate
        }
        if (currentGl is IosGl) {
            currentGl.defaultFramebuffer = fbo
        }

        app.update(dtSeconds.coerceAtMost(1f / 15f), input)
        app.render()
        
        glErrorCount = debug?.errorCount ?: 0
    }

    fun dispose() {
        app.dispose()
    }

    fun setSteer(steer: Float) {
        input.steer = steer.coerceIn(-1f, 1f)
    }

    fun setTilt(steer: Float) {
        setSteer(steer)
    }
}

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
import kotlinx.cinterop.useContents
import platform.gles2.GL_FRAMEBUFFER_BINDING
import platform.gles2.glGetIntegerv
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSOperationQueue

class IosGameHost(private val debugGl: Boolean = false) : HardwareSensors {
    var glErrorCount: Int = 0
        private set

    lateinit var app: MotomanGameApp
    private lateinit var rawGl: Gl
    private var debug: GlDebug? = null
    
    // UI input state populated by CommonGameOverlay (via MainViewController)
    val uiInputState = InputState()
    
    // Game logic input controller
    private lateinit var inputController: InputController

    private val motionManager = CMMotionManager()
    
    override val devicePitch: Float
        get() {
            // Forward/back tilt axis is Y on iOS when held in landscape (assuming typical orientation handling).
            val gravity = motionManager.deviceMotion?.gravity
            return if (gravity != null) {
                gravity.useContents { (y * 9.8).toFloat() }
            } else {
                0f
            }
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
        
        inputController = InputController(this)
        
        if (motionManager.isDeviceMotionAvailable()) {
            motionManager.deviceMotionUpdateInterval = 1.0 / 60.0
            motionManager.startDeviceMotionUpdates()
        }
    }

    fun resize(widthPx: Int, heightPx: Int) {
        app.resize(widthPx, heightPx)
    }

    fun render(dtSeconds: Float) {
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

        val finalInput = inputController.update(dtSeconds.coerceAtMost(1f / 15f), uiInputState)

        app.update(dtSeconds.coerceAtMost(1f / 15f), finalInput)
        app.render()
        
        glErrorCount = debug?.errorCount ?: 0
    }

    fun dispose() {
        motionManager.stopDeviceMotionUpdates()
        app.dispose()
    }
}

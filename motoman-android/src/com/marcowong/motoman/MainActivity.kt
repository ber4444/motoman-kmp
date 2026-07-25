package com.marcowong.motoman

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.fillMaxSize
import com.marcowong.motoman.ui.CommonGameOverlay
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import com.marcowong.motoman.assets.AndroidAssets
import com.marcowong.motoman.gl.GlslTarget
import com.marcowong.motoman.gl.createPlatformGl
import com.marcowong.motoman.track.TrackGenerator
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class MainActivity : ComponentActivity(), SensorEventListener, HardwareSensors {
    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    
    // UI input state populated by CommonGameOverlay
    private val uiInputState = InputState()
    
    // Game logic input controller
    private lateinit var inputController: InputController
    private var lastTime = 0L

    @Volatile override var devicePitch: Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        
        val audio = AndroidAudio(this)
        val haptics = AndroidHaptics(this)
        val trackData = TrackGenerator().generate() ?: error("Failed to generate track data")
        val config = RenderConfig.HIGH_QUALITY
        android.util.Log.i("Motoman", "RenderConfig: $config")
        val app = MotomanGameApp(AndroidAssets(assets), trackData, GlslTarget.ES_100, audio, haptics, config)
        
        inputController = InputController(this)

        glSurfaceView = GLSurfaceView(this)
        glSurfaceView.setEGLContextClientVersion(2)
        glSurfaceView.setRenderer(object : GLSurfaceView.Renderer {
            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                var platformGl: com.marcowong.motoman.gl.Gl = createPlatformGl()
                val isDebug = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
                if (isDebug) {
                    platformGl = com.marcowong.motoman.gl.GlDebug(platformGl)
                }
                platformGl = com.marcowong.motoman.gl.GlOptimized(platformGl)
                app.create(platformGl, 1, 1)
                lastTime = System.nanoTime()
            }

            override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
                app.resize(width, height)
            }

            override fun onDrawFrame(gl: GL10?) {
                val now = System.nanoTime()
                val dt = (now - lastTime) / 1000000000.0f
                lastTime = now
                
                val finalInput = inputController.update(dt, uiInputState)
                
                app.update(dt, finalInput)
                app.render()
            }
        })
        
        setContent {
            val state by app.gameStateFlow.state.collectAsState()
            Box(Modifier.fillMaxSize()) {
                AndroidView(factory = { glSurfaceView }, modifier = Modifier.fillMaxSize())
                
                // Shared UI layer handling touch drag steering and boost button
                CommonGameOverlay(state = state, inputState = uiInputState)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        glSurfaceView.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            // Forward/back tilt axis is X since we hold the phone in landscape.
            devicePitch = event.values[0]
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

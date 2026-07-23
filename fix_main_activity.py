import os

with open("motoman-android/src/com/marcowong/motoman/MainActivity.kt", "w") as f:
    f.write("""package com.marcowong.motoman

import android.app.Activity
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

class MainActivity : Activity(), SensorEventListener {
    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private val inputState = InputState()
    private var lastTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        
        val trackData = TrackGenerator().generate() ?: error("Failed to generate track data")
        val app = MotomanGameApp(AndroidAssets(assets), trackData, GlslTarget.ES_100)
        
        glSurfaceView = GLSurfaceView(this)
        glSurfaceView.setEGLContextClientVersion(2)
        glSurfaceView.setRenderer(object : GLSurfaceView.Renderer {
            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                app.create(createPlatformGl(), 1, 1)
                lastTime = System.nanoTime()
            }

            override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
                app.resize(width, height)
            }

            override fun onDrawFrame(gl: GL10?) {
                val now = System.nanoTime()
                val dt = (now - lastTime) / 1000000000.0f
                lastTime = now
                app.update(dt, inputState)
                app.render()
            }
        })
        
        setContentView(glSurfaceView)
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
            val y = event.values[1] // Tilt along Y axis (landscape)
            // Normalize tilt roughly
            inputState.steer = (y / 5f).coerceIn(-1f, 1f)
            inputState.throttle = 1f // Auto-throttle for now? 
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
""")

if os.path.exists("motoman-android/src/com/marcowong/motoman/MainActivity.java"):
    os.remove("motoman-android/src/com/marcowong/motoman/MainActivity.java")


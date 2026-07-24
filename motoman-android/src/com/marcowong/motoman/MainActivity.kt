package com.marcowong.motoman

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.viewinterop.AndroidView
import com.marcowong.motoman.ui.MotomanHUD
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

class MainActivity : ComponentActivity(), SensorEventListener {
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
        
        val audio = AndroidAudio(this)
        val haptics = AndroidHaptics(this)
        val trackData = TrackGenerator().generate() ?: error("Failed to generate track data")
        // Render toggles are overridable from the launch intent so parity work can bisect
        // effects on-device without a rebuild, e.g.
        //   adb shell am start -n com.marcowong.motoman/.MainActivity --ez bloom false
        val d = RenderConfig.ORIGINAL
        val config = RenderConfig(
            resolutionReduction = intent.getFloatExtra("res", d.resolutionReduction),
            modelTextureLinearFilter = intent.getBooleanExtra("texLinear", d.modelTextureLinearFilter),
            frameBufferLinearFilter = intent.getBooleanExtra("fbLinear", d.frameBufferLinearFilter),
            bloom = intent.getBooleanExtra("bloom", d.bloom),
            motionBlur = intent.getBooleanExtra("motionBlur", d.motionBlur),
            antiAliasing = intent.getBooleanExtra("aa", d.antiAliasing),
        )
        android.util.Log.i("Motoman", "RenderConfig: $config")
        val app = MotomanGameApp(AndroidAssets(assets), trackData, GlslTarget.ES_100, audio, haptics, config)
        
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
                app.update(dt, inputState)
                app.render()
            }
        })
        
        setContent {
            val state by app.gameStateFlow.state.collectAsState()
            Box {
                AndroidView(factory = { glSurfaceView })
                MotomanHUD(state = state)
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
            // The phone is held flat in landscape (screen up, like a tray). Sensor values stay
            // in the device's natural (portrait) frame even though the UI is locked to
            // landscape, so: Y is the left/right lean axis, X is the forward/back tilt axis.
            val lean = event.values[1]
            val pitch = event.values[0]

            // Roll left/right to steer. Negated so tilting right steers right.
            inputState.steer = (-lean / 5f).coerceIn(-1f, 1f)

            // Tilt the far edge down (forward) to accelerate, tilt toward you to brake. A
            // deadzone lets a level phone coast, and full throttle needs a firm tilt, so the
            // bike no longer rockets — it used to be pinned at full throttle every frame.
            val drive = when {
                pitch > TILT_DEADZONE -> ((pitch - TILT_DEADZONE) / TILT_RANGE).coerceAtMost(1f)
                pitch < -TILT_DEADZONE -> ((pitch + TILT_DEADZONE) / TILT_RANGE).coerceAtLeast(-1f)
                else -> 0f
            }
            inputState.throttle = (drive * MAX_THROTTLE).coerceAtLeast(0f)
            inputState.brake = (-drive).coerceAtLeast(0f)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private companion object {
        // Tilt-throttle tuning, in m/s^2 of gravity on the forward/back axis (max ~9.8).
        /** Level-ish phone coasts: no throttle or brake within this band (~6°). */
        const val TILT_DEADZONE = 1.0f
        /** Gravity delta beyond the deadzone for full input — ~30° of tilt. */
        const val TILT_RANGE = 4.5f
        /** Caps top-end throttle so the bike accelerates more gently than the old full-throttle. */
        const val MAX_THROTTLE = 0.7f
    }
}

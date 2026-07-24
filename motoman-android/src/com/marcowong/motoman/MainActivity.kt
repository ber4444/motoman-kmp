package com.marcowong.motoman

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.fillMaxSize
import com.marcowong.motoman.ui.MotomanHUD
import android.content.Context
import android.os.SystemClock
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

    /** Boost expiry, in the SystemClock.uptimeMillis() clock; 0 when not boosting. */
    @Volatile private var boostEndUptimeMs = 0L

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
                // Boost forces full throttle for its duration, overriding the tilt throttle.
                if (SystemClock.uptimeMillis() < boostEndUptimeMs) {
                    inputState.throttle = 1f
                    inputState.brake = 0f
                }
                app.update(dt, inputState)
                app.render()
            }
        })
        
        setContent {
            val state by app.gameStateFlow.state.collectAsState()
            Box(Modifier.fillMaxSize()) {
                AndroidView(factory = { glSurfaceView }, modifier = Modifier.fillMaxSize())

                // Touch-drag steering (the only steering input): press anywhere and slide.
                // Steer tracks how far the finger has moved from where it first touched down,
                // so holding still goes straight and sliding turns. A quarter of the screen
                // width is full lock. Sliding right is +steer (InputState: +1 = right), which
                // the combined steering model turns into a right turn. The boost button below
                // is drawn on top, so presses there are consumed by it and never start a drag.
                Box(
                    Modifier.fillMaxSize().pointerInput(Unit) {
                        val fullLockPx = size.width / 4f
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            inputState.steer = 0f
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                if (change == null || !change.pressed) break
                                val dx = change.position.x - down.position.x
                                inputState.steer = (dx / fullLockPx).coerceIn(-1f, 1f)
                                change.consume()
                            }
                            inputState.steer = 0f
                        }
                    }
                )

                MotomanHUD(state = state)

                // Boost button, lower-left: full throttle for 3 seconds.
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(24.dp)
                        .size(96.dp)
                        .background(Color(0x66FFFFFF), CircleShape)
                        .clickable {
                            boostEndUptimeMs = SystemClock.uptimeMillis() + BOOST_MILLIS
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    BasicText(
                        text = "BOOST",
                        style = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold),
                    )
                }
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
            // Tilt controls throttle only — steering is by touch drag (see setContent). The
            // phone is held flat in landscape (screen up); sensor values stay in the device's
            // natural (portrait) frame even though the UI is locked to landscape, so the
            // forward/back tilt axis is X.
            val pitch = event.values[0]

            // Tilt the far edge down (forward) to accelerate, tilt toward you to brake. A
            // deadzone lets a level phone coast, and full throttle needs a firm tilt.
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
        /** Boost button holds full throttle this long. */
        const val BOOST_MILLIS = 1500L
    }
}

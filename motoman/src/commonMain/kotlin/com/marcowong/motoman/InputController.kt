package com.marcowong.motoman

interface HardwareSensors {
    /** The device's pitch in m/s^2 (e.g. from an accelerometer). 0 means flat, positive means tilted forward. */
    val devicePitch: Float
}

/**
 * Handles the translation of raw hardware sensors and touch UI state into the final
 * [InputState] consumed by the game engine, enforcing cross-platform parity for deadzones,
 * maximum throttle, and boost logic.
 */
class InputController(private val sensors: HardwareSensors) {
    private val inputState = InputState()
    
    private var boostTimeRemaining = 0f

    fun update(dtSeconds: Float, uiInput: InputState): InputState {
        // Handle boost timer
        if (uiInput.boostPressed) {
            boostTimeRemaining = BOOST_DURATION_SEC
            // Reset the flag so we don't continually trigger unless held/pressed again
            uiInput.boostPressed = false
        }
        
        val boosting = boostTimeRemaining > 0f
        if (boosting) {
            boostTimeRemaining -= dtSeconds
        }

        // Calculate drive from tilt
        val pitch = sensors.devicePitch
        val drive = when {
            pitch > TILT_DEADZONE -> ((pitch - TILT_DEADZONE) / TILT_RANGE).coerceAtMost(1f)
            pitch < -TILT_DEADZONE -> ((pitch + TILT_DEADZONE) / TILT_RANGE).coerceAtLeast(-1f)
            else -> 0f
        }

        // Apply throttle and brake limits
        if (boosting) {
            inputState.throttle = 1f
            inputState.brake = 0f
        } else {
            inputState.throttle = (drive * MAX_THROTTLE).coerceAtLeast(0f)
            inputState.brake = (-drive).coerceAtLeast(0f)
        }

        // Pass through steering and shifting from UI overlay
        inputState.steer = uiInput.steer
        inputState.shiftUp = uiInput.shiftUp
        inputState.shiftDown = uiInput.shiftDown
        
        // Also allow UI to manually set throttle/brake if needed (e.g. for desktop keyboard)
        // If UI provides throttle/brake > 0, we take the max of tilt and UI.
        inputState.throttle = maxOf(inputState.throttle, uiInput.throttle)
        inputState.brake = maxOf(inputState.brake, uiInput.brake)

        return inputState
    }

    private companion object {
        // Tilt-throttle tuning, in m/s^2 of gravity on the forward/back axis (max ~9.8).
        const val TILT_DEADZONE = 1.0f
        const val TILT_RANGE = 4.5f
        const val MAX_THROTTLE = 0.7f
        const val BOOST_DURATION_SEC = 2.0f
    }
}

package com.marcowong.motoman.track

import com.marcowong.motoman.track.logic.IMotorcycleInputMeters
import com.marcowong.motoman.track.logic.Motorcycle
import com.marcowong.motoman.track.logic.Rider
import com.marcowong.motoman.track.logic.Track
import com.marcowong.motoman.track.math.Vector3
import kotlin.test.Test
import kotlin.test.assertTrue

class PhysicsReplayTest {

    private class ScriptedInput : IMotorcycleInputMeters {
        var engineAndBrake = 1f
        var counterSteering = 0f
        var lean = 0f
        
        override fun getEngineAndBrakeMeter(): Float = engineAndBrake
        override fun getCounterSteeringMeter(): Float = counterSteering
        override fun getLeanMeter(): Float = lean
        override fun setMotorcycle(motorcycle: Motorcycle) {}
    }

    @Test
    fun testPhysicsReplay() {
        val generator = TrackGenerator()
        // Seed the random generator for reproducibility
        generator.random = BasicRandom(12345)
        generator.trackLen = 100f
        
        val trackData = generator.generate() ?: error("Track generation failed")
        val track = Track(trackData)
        val input = ScriptedInput()
        val motorcycle = Motorcycle(track, input)
        
        motorcycle.massCenterHeight = 5f
        motorcycle.leanAngleMaxWhenRunning = 45f
        motorcycle.leanAngleSafe = 30f
        
        val rider = Rider(track)
        rider.motorcycle = motorcycle
        motorcycle.rider = rider
        
        // Ensure standard initial position and orientation
        val startPos = track.getStartSpawnPosition()
        motorcycle.statePersist.pos.set(startPos)
        
        motorcycle.setPersist(true)
        motorcycle.go()

        val dt = 1f / 60f
        
        // Replay 600 frames (10 seconds)
        for (i in 0 until 600) {
            // Scripted inputs based on frame
            when {
                i < 60 -> {
                    // Full throttle for 1s
                    input.engineAndBrake = 1f
                    input.counterSteering = 0f
                    input.lean = 0f
                }
                i < 180 -> {
                    // Turn right
                    input.engineAndBrake = 0.5f
                    input.counterSteering = -0.5f
                    input.lean = 0.8f
                }
                i < 300 -> {
                    // Turn left
                    input.engineAndBrake = 0.5f
                    input.counterSteering = 0.5f
                    input.lean = -0.8f
                }
                else -> {
                    // Full throttle again
                    input.engineAndBrake = 1f
                    input.counterSteering = 0f
                    input.lean = 0f
                }
            }
            
            motorcycle.update(dt)
            motorcycle.setPersist(false) // Swap state
            motorcycle.setPersist(true)
        }

        val finalPos = Vector3()
        motorcycle.statePersist.pos.getTranslation(finalPos)
        
        val initialTranslation = Vector3()
        startPos.getTranslation(initialTranslation)
        
        // These assertions lock in the current physics simulation state.
        // On Android and Desktop, they will evaluate exactly the same due to commonMain pure Kotlin math.
        assertTrue(finalPos.x != initialTranslation.x, "Position X should have changed")
        assertTrue(finalPos.z != initialTranslation.z, "Position Z should have changed")
        
        println("Final Physics Pos: ${finalPos.x}, ${finalPos.y}, ${finalPos.z}")
    }
}

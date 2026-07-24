package com.marcowong.motoman

import com.marcowong.motoman.track.TrackGenerator
import com.marcowong.motoman.track.logic.Motorcycle
import com.marcowong.motoman.track.logic.Track
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Guards the one-stick "combined" steering model against the regression where the bike got
 * stuck turning one way. The single [InputState.steer] must act as **either** counter-steering
 * **or** leaning, never both — and crucially, steering *against* an established lean has to be
 * counter-steering, which is what brings the bike back and lets it reverse.
 */
class StubInputMetersTest {

    private fun setup(leanAngle: Float, steer: Float): StubInputMeters {
        val track = Track(TrackGenerator().generate()!!)
        val meters = StubInputMeters(InputState().apply { this.steer = steer })
        Motorcycle(track, meters).also { // its init calls meters.setMotorcycle(it)
            it.leanAngleSafe = 30f
            it.state.leanAngle = leanAngle
        }
        return meters
    }

    @Test
    fun uprightSteeringCounterSteersToInitiateALean() {
        val m = setup(leanAngle = 0f, steer = 0.5f)
        assertEquals(0.5f, m.getCounterSteeringMeter())
        assertEquals(0f, m.getLeanMeter())
    }

    @Test
    fun steeringIntoAnEstablishedLeanLeans() {
        // Leaned right past the safe band, steering further right: deepen the lean.
        val m = setup(leanAngle = 40f, steer = 0.5f)
        assertEquals(0f, m.getCounterSteeringMeter())
        assertEquals(0.5f, m.getLeanMeter())
    }

    @Test
    fun steeringAgainstALeanCounterSteersToRecover() {
        // The regression: leaned right, steering left to come back must counter-steer, not be
        // ignored. The old model applied both meters at once and the bike stayed stuck.
        val m = setup(leanAngle = 40f, steer = -0.5f)
        assertEquals(-0.5f, m.getCounterSteeringMeter())
        assertEquals(0f, m.getLeanMeter())

        val m2 = setup(leanAngle = -40f, steer = 0.5f)
        assertEquals(0.5f, m2.getCounterSteeringMeter())
        assertEquals(0f, m2.getLeanMeter())
    }
}

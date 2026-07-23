package com.marcowong.motoman.scene

import com.marcowong.motoman.track.TrackDirection
import com.marcowong.motoman.track.TrackSegment
import com.marcowong.motoman.track.logic.ITrackee
import com.marcowong.motoman.track.logic.Motorcycle
import com.marcowong.motoman.track.math.Matrix4
import com.marcowong.motoman.track.math.PerspectiveCamera
import com.marcowong.motoman.track.math.Quaternion
import com.marcowong.motoman.track.math.Vector3
import kotlin.math.max
import kotlin.random.Random

class MotomanCamera(
    fieldOfView: Float,
    viewportW: Float,
    viewportHeight: Float,
    private val motorcycle: Motorcycle,
    private val deviceRotationProvider: () -> Float
) : PerspectiveCamera(fieldOfView, viewportW, viewportHeight), ITrackee {

    class UpdateState {
        var lastTrackSegment: TrackSegment? = null
        val vanishingPoint = Vector3()
        val vanishingPointSmoothed = Vector3()
        var vanishingPointLookingFactor = 0f
        var vanishingPointLookingFactorTarget = 0f
        var shakeRot = 0f
        var shakeness = 0f
        var distance = 0f
        var motorcycleCounterSteeringShift = 0f
        var motorcycleEngineOutput = 0f
        var vanishingPointNeedReset = true
        val shake = Vector3()

        fun copyTo(s: UpdateState) {
            s.lastTrackSegment = lastTrackSegment
            s.vanishingPoint.set(vanishingPoint)
            s.vanishingPointSmoothed.set(vanishingPointSmoothed)
            s.vanishingPointLookingFactor = vanishingPointLookingFactor
            s.vanishingPointLookingFactorTarget = vanishingPointLookingFactorTarget
            s.shakeRot = shakeRot
            s.shakeness = shakeness
            s.distance = distance
            s.motorcycleCounterSteeringShift = motorcycleCounterSteeringShift
            s.motorcycleEngineOutput = motorcycleEngineOutput
            s.vanishingPointNeedReset = vanishingPointNeedReset
            s.shake.set(shake)
        }
    }

    val statePersist = UpdateState()
    val stateTmp = UpdateState()
    var state = statePersist

    override fun getTrackeePos(vec: Vector3) {
        vec.set(position.x, position.y, position.z)
    }

    override fun setLastTrackSegment(ts: TrackSegment?) {
        state.lastTrackSegment = ts
    }

    override fun getLastTrackSegment(): TrackSegment? {
        return state.lastTrackSegment
    }

    fun setPersist(b: Boolean) {
        if (b) {
            if (state !== statePersist) {
                state = statePersist
            }
        } else {
            if (state !== stateTmp) {
                statePersist.copyTo(stateTmp)
                state = stateTmp
            }
        }
    }

    private val tmpMat2 = Matrix4()
    private val tmpQua = Quaternion()
    private val tmpQua2 = Quaternion()
    private val tmpVec = Vector3()
    private val tmpVec2 = Vector3()
    private val tmpVec5 = Vector3()
    private val tmpVec6 = Vector3()
    private val shakeRotFactor = 5f

    fun followMotorcycle(delta: Float) {
        if (!motorcycle.state.isCrashed) {
            val shakeVectorChangeRate = 0.2f
            state.shake.x = (1 - shakeVectorChangeRate) * state.shake.x + shakeVectorChangeRate * (Random.nextFloat() - 0.5f)
            state.shake.y = (1 - shakeVectorChangeRate) * state.shake.y + shakeVectorChangeRate * (Random.nextFloat() - 0.5f)
            state.shake.z = (1 - shakeVectorChangeRate) * state.shake.z + shakeVectorChangeRate * (Random.nextFloat() - 0.5f)
            state.shake.nor()
            state.shakeRot = (1 - shakeVectorChangeRate) * state.shakeRot + shakeVectorChangeRate * (Random.nextFloat() - 0.5f)

            var s = 0f
            val engineOutput = motorcycle.getEngineOutputPercentage()
            if (motorcycle.state.isStandBy) s = 0.05f
            else if (engineOutput > state.motorcycleEngineOutput) s = 1 - engineOutput
            else if (engineOutput < state.motorcycleEngineOutput) s = engineOutput

            val shakenessChangeRate = 0.05f
            val shakeFactor = 1f
            state.shakeness = state.shakeness * (1 - shakenessChangeRate) + s * shakenessChangeRate
            tmpVec5.set(state.shake).mul(state.shakeness * shakeFactor)

            val distanceChangeRate = 0.05f
            var distanceTarget = 0f
            if (engineOutput > state.motorcycleEngineOutput) distanceTarget = 1f
            else if (engineOutput < state.motorcycleEngineOutput) distanceTarget = -1f
            state.distance = state.distance * (1 - distanceChangeRate) + distanceTarget * distanceChangeRate

            if (state.vanishingPointNeedReset) {
                state.vanishingPointNeedReset = false
                state.vanishingPoint.set(0f, 0f, 10f).mul(motorcycle.state.pos)
                state.vanishingPoint.y = 0f
                state.vanishingPointSmoothed.set(state.vanishingPoint)
            } else {
                tmpVec.set(state.vanishingPoint).sub(state.vanishingPointSmoothed)
                tmpVec.y = 0f
                val stepFullLen = tmpVec.len()
                if (stepFullLen > 0) {
                    val step1 = motorcycle.state.bikeVelo.len() * 2
                    val step2 = delta * tmpVec2.set(state.vanishingPoint).sub(state.vanishingPointSmoothed).len()
                    val step = max(step1, step2)
                    if (step < stepFullLen) {
                        tmpVec.div(stepFullLen).mul(step)
                        state.vanishingPointSmoothed.x += tmpVec.x
                        state.vanishingPointSmoothed.z += tmpVec.z
                    } else {
                        state.vanishingPointSmoothed.x = state.vanishingPoint.x
                        state.vanishingPointSmoothed.z = state.vanishingPoint.z
                    }
                }
            }

            val motorcycleCounterSteeringShiftDecayFactor = 1.5f
            state.motorcycleCounterSteeringShift += motorcycle.state.latestCounterSteeringPositionShift
            state.motorcycleCounterSteeringShift -= state.motorcycleCounterSteeringShift * motorcycleCounterSteeringShiftDecayFactor * delta

            val vanishingPointLookingChangeRate = 0.01f
            state.vanishingPointLookingFactor = (1 - vanishingPointLookingChangeRate) * state.vanishingPointLookingFactor + vanishingPointLookingChangeRate * state.vanishingPointLookingFactorTarget

            motorcycle.state.pos.getTranslation(tmpVec)
            tmpVec.y = 0f
            tmpVec2.set(0f, 1f, 0f)
            tmpVec6.set(state.vanishingPointSmoothed).sub(tmpVec).mul(-1f).add(tmpVec)
            tmpMat2.setToLookAt(tmpVec, tmpVec6, tmpVec2).inv()
            tmpMat2.getRotation(tmpQua2)
            motorcycle.state.pos.getRotation(tmpQua)
            tmpQua.slerp(tmpQua2, state.vanishingPointLookingFactor).nor()
            tmpVec.set(state.motorcycleCounterSteeringShift, 5f, 0f).mul(motorcycle.state.pos)
            tmpMat2.idt().set(tmpQua).trn(tmpVec)
            tmpVec.set(0f, 0f, -(12f + state.distance)).add(tmpVec5).mul(tmpMat2)
            tmpVec2.set(state.motorcycleCounterSteeringShift, 5f, 0f).mul(motorcycle.state.pos)

            position.set(tmpVec)
            tmpMat2.idt().rotate(0f, 0f, 1f, state.shakeRot * state.shakeness * shakeRotFactor)
            up.set(0f, 1f, 0f).mul(tmpMat2)
            lookAt(tmpVec2.x, tmpVec2.y, tmpVec2.z)

            state.motorcycleEngineOutput = engineOutput
        } else {
            val tv = Vector3()
            motorcycle.getTrackeePos(tv)
            tmpVec.set(tv)
            
            val tv2 = Vector3()
            motorcycle.rider?.getTrackeePos(tv2) ?: tv2.set(tv)
            tmpVec2.set(tv2)

            tmpVec.add(tmpVec2).mul(0.5f)
            this.lookAt(tmpVec.x, 5f, tmpVec.z)

            state.vanishingPointNeedReset = true
            state.motorcycleCounterSteeringShift = 0f
            state.vanishingPointLookingFactor = state.vanishingPointLookingFactorTarget
            state.shakeRot = 0f
            state.shakeness = 0f
            state.distance = 0f
            state.motorcycleEngineOutput = 0f
        }
    }

    private val tmpMat = Matrix4()
    private val tmpVec3 = Vector3()
    private val tmpVec4 = Vector3()

    fun alignCameraWithDevice() {
        val deviceRot = deviceRotationProvider()
        tmpVec3.set(0f, 0f, 0f)
        tmpVec4.set(0f, 1f, 0f)
        tmpMat.setToLookAt(tmpVec3, direction, tmpVec4).inv()
        val crashShake = if (motorcycle.state.isCrashed) 0f else state.shakeRot * state.shakeness * shakeRotFactor
        tmpMat.rotate(0f, 0f, 1f, -deviceRot + crashShake)
        up.set(0f, 1f, 0f).mul(tmpMat)
    }

    fun updateVanishingPoint(vec: Vector3?) {
        if (vec != null) state.vanishingPoint.set(vec)
    }

    fun setVanishingPointLookingFactor(f: Float) {
        state.vanishingPointLookingFactorTarget = f
    }

    private fun getVanishingPointLookingFactorForDirection(td: TrackDirection?): Float {
        return when (td) {
            TrackDirection.LeftMed, TrackDirection.RightMed -> 1 / 6f
            TrackDirection.LeftHigh, TrackDirection.RightHigh -> 1 / 4f
            TrackDirection.LeftSharp, TrackDirection.RightSharp -> 1 / 3f
            else -> 0f
        }
    }

    fun setVanishingPointLookingFactor(td: TrackDirection?, scale: Float) {
        val f = getVanishingPointLookingFactorForDirection(td) * scale
        setVanishingPointLookingFactor(f)
    }
}

package com.marcowong.motoman.track.logic

import com.marcowong.motoman.track.TrackSegment
import com.marcowong.motoman.track.math.Matrix4
import com.marcowong.motoman.track.math.Quaternion
import com.marcowong.motoman.track.math.Vector3
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sign

class Motorcycle(
    var  track: Track,
    private val inputMeters: IMotorcycleInputMeters
) : ITrackee {
    
    var  rider: Rider? = null
    
    var  massCenterHeight = 0f
    var  leanAngleMaxWhenRunning = 0f
    var  leanAngleSafe = 0f

    // Lean-feel knobs, defaulted to the original game's values so the physics-replay parity
    // test is unaffected. The app lowers them (see MainMotorcycle) for a gentler ride where a
    // held turn does not run its lean away into a crash.
    /** Rate at which counter-steering tips the bike into a lean while turning in (upright). */
    var  counterSteeringLeanInc = 90f
    /** Rate at which the lean meter deepens an already-established lean. */
    var  leanAnglePressure = 90f
    /** Self-reinforcing "fall over" force that deepens an existing lean; the runaway term. */
    var  gravityForceWhenRunning = 25f
    
    private val engineOutputMin = 20f
    private val engineOutputMax = 200f

    class UpdateState {
        var leanAngle = 0f
        var isCrashed = false
        var isStandBy = true
        var engineOutput = 20f // engineOutputMin
        var frontTraction = 1f
        var backTraction = 1f
        var slideDuration = 0f
        var isTouchingGround = false
        val bikeVelo = Vector3()
        val pos = Matrix4()
        val lean = Matrix4()
        val frontWheelRot = Matrix4()
        val rearWheelRot = Matrix4()
        var latestCounterSteeringPositionShift = 0f
        var lastTrackSegment: TrackSegment? = null
        
        var justCrashed = false
        var lastBackfireSize = 0f

        fun copyTo(s: UpdateState) {
            s.leanAngle = leanAngle
            s.isCrashed = isCrashed
            s.isStandBy = isStandBy
            s.engineOutput = engineOutput
            s.frontTraction = frontTraction
            s.backTraction = backTraction
            s.slideDuration = slideDuration
            s.isTouchingGround = isTouchingGround
            s.bikeVelo.set(bikeVelo)
            s.pos.set(pos)
            s.lean.set(lean)
            s.frontWheelRot.set(frontWheelRot)
            s.rearWheelRot.set(rearWheelRot)
            s.latestCounterSteeringPositionShift = latestCounterSteeringPositionShift
            s.lastTrackSegment = lastTrackSegment
            s.justCrashed = justCrashed
            s.lastBackfireSize = lastBackfireSize
        }
    }

    val  stateTmp = UpdateState()
    val  statePersist = UpdateState()
    var  state = statePersist

    init {
        inputMeters.setMotorcycle(this)
    }

    fun getRawEngineAndBrakeMeter(): Float {
        return inputMeters.getEngineAndBrakeMeter()
    }

    fun getEngineAndBrakeMeter(): Float {
        if (state.isCrashed || state.isStandBy) return 0f
        return getRawEngineAndBrakeMeter()
    }

    private fun getCounterSteeringMeter(): Float {
        if (state.isCrashed || state.isStandBy) return 0f
        return inputMeters.getCounterSteeringMeter()
    }

    fun getLeanMeter(): Float {
        if (state.isCrashed || state.isStandBy) return 0f
        return inputMeters.getLeanMeter()
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

    fun update(delta: Float) {
        updateBikePhysics(delta)
    }

    private val tmpVec = Vector3()
    private val tmpVec2 = Vector3()
    private val tmpVec5 = Vector3()
    private val tmpMat4 = Matrix4()
    private val tmpQua = Quaternion()
    private val tmpBikeVeloNew = Vector3()

    private fun updateBikePhysics(delta: Float) {
        state.justCrashed = false
        var isOverLean = false
        val leanAngleMax = if (state.isCrashed) 90f else leanAngleMaxWhenRunning
        var rotAngle = 0f
        val leanToRotFactor = 2f

        val counterSteeringAffectRotAngleFactor = 1 / 5f
        val counterSteeringMeter = getCounterSteeringMeter()
        val counterSteeringTractionFactor = if (state.frontTraction <= 0 && state.backTraction <= 0) 0f else 1f
        val riderStrength = rider?.getStrength() ?: 1f
        val counterSteeringRiderStrengthFactor = 3 / 4f + riderStrength * 1 / 4f
        var counterSteeringLean = delta * counterSteeringMeter * counterSteeringLeanInc * counterSteeringTractionFactor * counterSteeringRiderStrengthFactor
        var counterSteeredLeanAngle = state.leanAngle + counterSteeringLean
        if (counterSteeredLeanAngle < -leanAngleMax) counterSteeredLeanAngle = -leanAngleMax
        if (counterSteeredLeanAngle > leanAngleMax) counterSteeredLeanAngle = leanAngleMax
        counterSteeringLean = counterSteeredLeanAngle - state.leanAngle
        tmpMat4.idt()
        tmpMat4.rotate(0f, 0f, 1f, state.leanAngle)
        tmpVec.set(0f, massCenterHeight, 0f)
        tmpVec.mul(tmpMat4)
        tmpMat4.idt()
        tmpMat4.rotate(0f, 0f, 1f, counterSteeredLeanAngle)
        tmpVec2.set(0f, massCenterHeight, 0f)
        tmpVec2.mul(tmpMat4)
        val massCenterShift = tmpVec2.x - tmpVec.x
        state.leanAngle = counterSteeredLeanAngle
        rotAngle += counterSteeringLean * counterSteeringAffectRotAngleFactor * leanToRotFactor
        state.pos.translate(massCenterShift * delta, 0f, 0f)

        val leanPressureForceRatio = 0.8f + 0.2f * (abs(state.leanAngle) / 90f)
        val leanAnglePressEpsilon = 10f
        val leanMeter = getLeanMeter()
        val leanRiderStrengthFactor = 2 / 3f + riderStrength * 1 / 3f
        if (state.leanAngle in -leanAnglePressEpsilon..leanAnglePressEpsilon ||
            (state.leanAngle >= 0 && leanMeter > 0) ||
            (state.leanAngle <= 0 && leanMeter < 0)
        ) {
            state.leanAngle += delta * leanPressureForceRatio * leanAnglePressure * leanMeter * leanRiderStrengthFactor
        }
        if (state.leanAngle < -leanAngleMax) { state.leanAngle = -leanAngleMax; isOverLean = true }
        if (state.leanAngle > leanAngleMax) { state.leanAngle = leanAngleMax; isOverLean = true }

        val gravityForceRatio = abs(state.leanAngle) / 90f
        val gravityForceWhenCrashed = gravityForceWhenRunning * 10f
        val gravityForce = if (state.isCrashed) gravityForceWhenCrashed else gravityForceWhenRunning
        if (state.leanAngle > 0) state.leanAngle += delta * gravityForceRatio * gravityForce
        if (state.leanAngle < 0) state.leanAngle -= delta * gravityForceRatio * gravityForce
        if (state.leanAngle < -leanAngleMax) { state.leanAngle = -leanAngleMax; isOverLean = true }
        if (state.leanAngle > leanAngleMax) { state.leanAngle = leanAngleMax; isOverLean = true }

        val engineOutputAdjust = 50f
        val engineAndBrakeMeter = getEngineAndBrakeMeter()
        if (!state.isStandBy) {
            state.engineOutput += engineOutputAdjust * delta * engineAndBrakeMeter
            if (state.engineOutput > engineOutputMax) state.engineOutput = engineOutputMax
            if (state.engineOutput < engineOutputMin) state.engineOutput = engineOutputMin
        } else {
            state.engineOutput = 0f
        }
        val isTheWheelIncreasingSpeed = engineAndBrakeMeter > 0 && state.engineOutput < engineOutputMax
        val isTheWheelDecreasingSpeed = engineAndBrakeMeter < 0 && state.engineOutput > engineOutputMin

        rotAngle += -state.leanAngle * leanToRotFactor * delta

        val traction = if (state.isCrashed) 0f else min((state.frontTraction + state.backTraction) * 0.5f, state.backTraction)
        state.pos.rotate(0f, 1f, 0f, rotAngle * traction)
        state.pos.getRotation(tmpQua)
        tmpQua.toMatrix(tmpMat4.`val`)
        tmpVec.set(0f, 0f, delta * state.engineOutput)
        tmpVec.mul(tmpMat4)
        tmpBikeVeloNew.set(
            traction * tmpVec.x + (1 - traction) * state.bikeVelo.x,
            traction * tmpVec.y + (1 - traction) * state.bikeVelo.y,
            traction * tmpVec.z + (1 - traction) * state.bikeVelo.z
        )
        state.pos.trn(tmpBikeVeloNew)
        val bikeVeloScalar = tmpBikeVeloNew.len() / delta

        var directionFalse = 0f
        if (state.bikeVelo.len2() > 0 && tmpBikeVeloNew.len2() > 0) {
            tmpVec.set(state.bikeVelo)
            tmpVec.crs(0f, 1f, 0f)
            tmpVec.nor()
            directionFalse = tmpVec.dot(tmpBikeVeloNew)
        }

        val cornerForceFactor = 30f
        val cornerForceRatio = 1 - abs(state.leanAngle) / 90f
        state.leanAngle -= cornerForceRatio * directionFalse * cornerForceFactor
        if (state.leanAngle < -leanAngleMax) { state.leanAngle = -leanAngleMax; isOverLean = true }
        if (state.leanAngle > leanAngleMax) { state.leanAngle = leanAngleMax; isOverLean = true }

        if (state.isStandBy) {
            val standByStandUpFactor = 90f
            if (state.leanAngle < 0) {
                state.leanAngle += standByStandUpFactor * delta
                if (state.leanAngle > 0) state.leanAngle = 0f
            }
            if (state.leanAngle > 0) {
                state.leanAngle -= standByStandUpFactor * delta
                if (state.leanAngle < 0) state.leanAngle = 0f
            }
        }

        var isSlided = false
        if (state.backTraction < state.frontTraction || state.isCrashed) {
            val bikeSlideRotFactor = 30f
            val bikeSlideIncreaseDuration = 1f
            val slideDirection = if (state.leanAngle > 0) 1f else if (state.leanAngle < 0) -1f else 0f
            if (slideDirection != 0f) {
                val bikeSlideDegree = min(state.slideDuration / bikeSlideIncreaseDuration, 1f) * min(bikeVeloScalar, 1f) * delta
                val bikeSlideRotAngle = -slideDirection * bikeSlideRotFactor * bikeSlideDegree
                state.pos.rotate(0f, 1f, 0f, bikeSlideRotAngle)
                isSlided = true
            }
        }
        if (isSlided) state.slideDuration += delta
        else state.slideDuration = 0f

        state.lean.idt()
        state.lean.rotate(0f, 0f, 1f, state.leanAngle)

        val bikeVeloDecay = 0.5f
        state.bikeVelo.set(tmpBikeVeloNew)
        state.bikeVelo.sub(
            state.bikeVelo.x * bikeVeloDecay * delta,
            state.bikeVelo.y * bikeVeloDecay * delta,
            state.bikeVelo.z * bikeVeloDecay * delta
        )

        val leanAngleOverTractionLimitBase = 75f
        val leanAngleOverTractionLimitFactor = 0.2f
        var leanAngleOverTractionLimit = leanAngleOverTractionLimitBase - bikeVeloScalar * leanAngleOverTractionLimitFactor
        if (leanAngleOverTractionLimit < 0) leanAngleOverTractionLimit = 0f
        if (leanAngleOverTractionLimit > leanAngleMax) leanAngleOverTractionLimit = leanAngleMax

        if (state.isCrashed) {
            state.frontTraction = 0f
            state.backTraction = 0f
        } else {
            val leanAngleAbs = abs(state.leanAngle)
            val overLeanTractionDecay = 1 / 2f
            val overTractionLimitTractionDecay = 1 / 5f
            val overTractionLimitTractionFrontWheelDecay = 1 / 5f
            val overTractionLimitTractionRearWheelDecay = 1 / 5f
            val tractionRecoverRate = 1 / 3f
            if (leanAngleAbs >= leanAngleMax || isOverLean) {
                state.frontTraction -= delta * overLeanTractionDecay
                state.backTraction -= delta * overLeanTractionDecay
                if (state.frontTraction < 0) state.frontTraction = 0f
                if (state.backTraction < 0) state.backTraction = 0f
            } else if (leanAngleAbs <= leanAngleSafe) {
                state.frontTraction = 1f
                state.backTraction = 1f
            } else if (leanAngleAbs >= leanAngleOverTractionLimit) {
                state.frontTraction -= delta * overTractionLimitTractionDecay
                state.backTraction -= delta * overTractionLimitTractionDecay
                if (isTheWheelDecreasingSpeed) state.frontTraction -= delta * overTractionLimitTractionFrontWheelDecay
                if (isTheWheelIncreasingSpeed) state.backTraction -= delta * overTractionLimitTractionRearWheelDecay
                if (state.frontTraction < 0) state.frontTraction = 0f
                if (state.backTraction < 0) state.backTraction = 0f
            } else {
                state.frontTraction += delta * tractionRecoverRate
                state.backTraction += delta * tractionRecoverRate
                if (state.frontTraction > 1) state.frontTraction = 1f
                if (state.backTraction > 1) state.backTraction = 1f
            }
            val counterSteeringTractionDecay = 1.5f
            if (leanAngleAbs > leanAngleSafe &&
                counterSteeringLean != 0f &&
                sign(state.leanAngle) == sign(counterSteeringLean)
            ) {
                state.frontTraction -= (abs(counterSteeringLean) / 90f) * counterSteeringTractionDecay
                if (state.frontTraction < 0) state.frontTraction = 0f
            }
            if (state.frontTraction <= 0 && state.backTraction > 0) state.backTraction -= delta * overTractionLimitTractionRearWheelDecay
            if (state.backTraction <= 0 && state.frontTraction > 0) state.frontTraction -= delta * overTractionLimitTractionFrontWheelDecay
        }

        if (!state.isStandBy) {
            val isInsideTrack = track.isInsideTrack(this)
            val isInsideTrackRider = rider?.let { track.isInsideTrack(it) } ?: true
            if (!isInsideTrack ||
                !isInsideTrackRider ||
                (state.frontTraction == 0f && state.backTraction == 0f)
            ) {
                if (!state.isCrashed) {
                    state.isCrashed = true
                    if (!isInsideTrack || !isInsideTrackRider) {
                        state.justCrashed = true
                    }
                }
                if (rider?.state?.attached == true) {
                    rider?.detach()
                }
            }

            if (!isInsideTrack &&
                track.getTrackCollisionVector(this, tmpVec, tmpVec2)
            ) {
                state.pos.getTranslation(tmpVec5)
                state.pos.trn(tmpVec.sub(tmpVec5))
                state.bikeVelo.sub(tmpVec2.mul(2 * tmpVec2.dot(state.bikeVelo))).mul(0.5f)
                state.justCrashed = true
            }
        }

        val rotFactor = 720 * (state.engineOutput / 20f)
        state.frontWheelRot.rotate(1f, 0f, 0f, rotFactor * delta)
        state.rearWheelRot.rotate(1f, 0f, 0f, rotFactor * delta)

        state.latestCounterSteeringPositionShift = massCenterShift

        state.isTouchingGround = isOverLean || state.leanAngle >= leanAngleMaxWhenRunning
    }

    fun reset() {
        state.isCrashed = false
        state.isStandBy = true
        state.frontTraction = 1f
        state.backTraction = 1f
        state.leanAngle = 0f
        state.lean.idt()
        state.engineOutput = engineOutputMin
        state.bikeVelo.set(0f, 0f, 0f)
        if (rider?.state?.attached == false) {
            rider?.attach()
        }
    }

    fun go() {
        if (state.isCrashed || !state.isStandBy) return
        state.isStandBy = false
    }

    fun standBy() {
        if (state.isCrashed || state.isStandBy) return
        state.isStandBy = true
        state.frontTraction = 1f
        state.backTraction = 1f
        state.engineOutput = engineOutputMin
        state.bikeVelo.set(0f, 0f, 0f)
    }

    fun getEngineOutputPercentage(): Float {
        var p = (state.engineOutput - engineOutputMin) / (engineOutputMax - engineOutputMin)
        if (p < 0) p = 0f
        return p
    }

    override fun getTrackeePos(vec: Vector3) {
        state.pos.getTranslation(vec)
    }

    override fun setLastTrackSegment(ts: TrackSegment?) {
        state.lastTrackSegment = ts
    }

    override fun getLastTrackSegment(): TrackSegment? {
        return state.lastTrackSegment
    }
}

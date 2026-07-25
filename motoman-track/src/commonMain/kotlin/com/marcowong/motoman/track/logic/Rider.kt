package com.marcowong.motoman.track.logic

import com.marcowong.motoman.track.TrackDirection
import com.marcowong.motoman.track.TrackSegment
import com.marcowong.motoman.track.math.Matrix4
import com.marcowong.motoman.track.math.Quaternion
import com.marcowong.motoman.track.math.Vector3
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class Rider(var  track: Track) : ITrackee {
    var  motorcycle: Motorcycle? = null
    var  poseStandBy: Pose? = null
    var  poseGo: Pose? = null

    inner class UpdateState {
        var lastTrackSegment: TrackSegment? = null
        var attached = true
        var detachedVelo = Vector3()
        var detachedPos = Matrix4()
        var strength = 1f
        var pose = Pose()
        var poseSrc = Pose()
        var poseDst: Pose? = null
        var poseDst2: Pose? = null
        var poseTime = 0f
        var poseTimeRemaining = 0f
        var directionCurrent = 0
        var directionNoticed = 0
        var bodyShiftTarget = 0f
        var bodyExposeTarget = 1f
        var leanReadingSmoothed = 0f

        fun copyTo(s: UpdateState) {
            s.lastTrackSegment = lastTrackSegment
            s.attached = attached
            s.detachedVelo.set(detachedVelo)
            s.detachedPos.set(detachedPos)
            s.strength = strength
            s.pose.set(pose)
            s.poseSrc.set(poseSrc)
            s.poseDst = poseDst
            s.poseDst2 = poseDst2
            s.poseTime = poseTime
            s.poseTimeRemaining = poseTimeRemaining
            s.directionCurrent = directionCurrent
            s.directionNoticed = directionNoticed
            s.bodyShiftTarget = bodyShiftTarget
            s.bodyExposeTarget = bodyExposeTarget
            s.leanReadingSmoothed = leanReadingSmoothed
        }
    }

    val  statePersist = UpdateState()
    val  stateTmp = UpdateState()
    var  state = statePersist

    class Pose {
        val matHip = Matrix4()
        val matWaist = Matrix4()
        val matChest = Matrix4()
        val matHead = Matrix4()
        val matArmUpperL = Matrix4()
        val matArmLowerL = Matrix4()
        val matArmUpperR = Matrix4()
        val matArmLowerR = Matrix4()
        val matLegUpperL = Matrix4()
        val matLegLowerL = Matrix4()
        val matLegUpperR = Matrix4()
        val matLegLowerR = Matrix4()

        fun set(p: Pose) {
            matHip.set(p.matHip)
            matWaist.set(p.matWaist)
            matChest.set(p.matChest)
            matHead.set(p.matHead)
            matArmUpperL.set(p.matArmUpperL)
            matArmLowerL.set(p.matArmLowerL)
            matArmUpperR.set(p.matArmUpperR)
            matArmLowerR.set(p.matArmLowerR)
            matLegUpperL.set(p.matLegUpperL)
            matLegLowerL.set(p.matLegLowerL)
            matLegUpperR.set(p.matLegUpperR)
            matLegLowerR.set(p.matLegLowerR)
        }
    }

    companion object {
        fun rotMatrix(m: Matrix4, x: Float, y: Float, z: Float) {
            m.rotate(1f, 0f, 0f, x)
            m.rotate(0f, 1f, 0f, y)
            m.rotate(0f, 0f, 1f, z)
        }

        fun rotMatrix(l: Matrix4, r: Matrix4, x: Float, y: Float, z: Float) {
            l.rotate(1f, 0f, 0f, x)
            l.rotate(0f, 1f, 0f, y)
            l.rotate(0f, 0f, 1f, z)
            r.rotate(1f, 0f, 0f, x)
            r.rotate(0f, 1f, 0f, -y)
            r.rotate(0f, 0f, 1f, -z)
        }

        private val tmpQua3 = Quaternion()
        private val tmpVec3 = Vector3()
        fun mirrorMatrix(m: Matrix4, m2: Matrix4) {
            m.idt()
            m2.getRotation(tmpQua3)
            m2.getTranslation(tmpVec3)
            tmpQua3.x = -tmpQua3.x
            tmpQua3.w = -tmpQua3.w
            tmpVec3.x = -tmpVec3.x
            m.set(tmpQua3)
            m.trn(tmpVec3)
        }

        fun mirrorPose(p: Pose, p2: Pose) {
            mirrorMatrix(p.matHead, p2.matHead)
            mirrorMatrix(p.matChest, p2.matChest)
            mirrorMatrix(p.matWaist, p2.matWaist)
            mirrorMatrix(p.matHip, p2.matHip)
            mirrorMatrix(p.matArmUpperL, p2.matArmUpperR)
            mirrorMatrix(p.matArmLowerL, p2.matArmLowerR)
            mirrorMatrix(p.matArmUpperR, p2.matArmUpperL)
            mirrorMatrix(p.matArmLowerR, p2.matArmLowerL)
            mirrorMatrix(p.matLegUpperL, p2.matLegUpperR)
            mirrorMatrix(p.matLegLowerL, p2.matLegLowerR)
            mirrorMatrix(p.matLegUpperR, p2.matLegUpperL)
            mirrorMatrix(p.matLegLowerR, p2.matLegLowerL)
        }
        
        val centerBrakingPose = Pose()
        val leftBrakingPose = Pose()
        val leftBrakingFootDraggingPose = Pose()
        val leftBrakingKneeDraggingPose = Pose()
        val rightBrakingPose = Pose()
        val rightBrakingFootDraggingPose = Pose()
        val rightBrakingKneeDraggingPose = Pose()
        val leftBrakingLeanedPose = Pose()
        val leftBrakingKneeDraggingLeanedPose = Pose()
        val leftBrakingFootDraggingLeanedPose = Pose()
        val rightBrakingLeanedPose = Pose()
        val rightBrakingFootDraggingLeanedPose = Pose()
        val rightBrakingKneeDraggingLeanedPose = Pose()
        val centerThottlingPose = Pose()
        val leftThottlingPose = Pose()
        val leftThottlingKneeDraggingPose = Pose()
        val rightThottlingPose = Pose()
        val rightThottlingKneeDraggingPose = Pose()
        val leftThottlingLeanedPose = Pose()
        val leftThottlingKneeDraggingLeanedPose = Pose()
        val rightThottlingLeanedPose = Pose()
        val rightThottlingKneeDraggingLeanedPose = Pose()
        val crashedPose = Pose()
        val standByPose = Pose()
        
        init {
            rotMatrix(centerBrakingPose.matHip, -5f, 0f, 0f)
            rotMatrix(centerBrakingPose.matWaist, 25f, 0f, 0f)
            rotMatrix(centerBrakingPose.matChest, 10f, 0f, 0f)
            rotMatrix(centerBrakingPose.matHead, -55f, 0f, 0f)
            rotMatrix(centerBrakingPose.matArmUpperL, centerBrakingPose.matArmUpperR, -15f, 5f, 0f)
            rotMatrix(centerBrakingPose.matArmLowerL, centerBrakingPose.matArmLowerR, -15f, 0f, 0f)
            rotMatrix(centerBrakingPose.matLegUpperL, centerBrakingPose.matLegUpperR, -10f, 10f, 10f)

            rotMatrix(centerThottlingPose.matHip, 15f, 0f, 0f)
            rotMatrix(centerThottlingPose.matWaist, 25f, 0f, 0f)
            rotMatrix(centerThottlingPose.matChest, 10f, 0f, 0f)
            rotMatrix(centerThottlingPose.matHead, -65f, 0f, 0f)
            rotMatrix(centerThottlingPose.matArmUpperL, centerThottlingPose.matArmUpperR, -15f, 5f, -5f)
            rotMatrix(centerThottlingPose.matArmLowerL, centerThottlingPose.matArmLowerR, -70f, 0f, 0f)
            rotMatrix(centerThottlingPose.matLegUpperL, centerThottlingPose.matLegUpperR, -30f, 10f, 10f)

            leftBrakingPose.matHip.translate(1f, 0f, 0.25f)
            rotMatrix(leftBrakingPose.matHip, -5f, 0f, 0f)
            rotMatrix(leftBrakingPose.matWaist, 25f, -35f, 0f)
            rotMatrix(leftBrakingPose.matChest, 10f, 0f, 10f)
            rotMatrix(leftBrakingPose.matHead, -55f, 25f, 15f)
            rotMatrix(leftBrakingPose.matArmUpperR, -32.5f, 30f, 0f)
            rotMatrix(leftBrakingPose.matArmLowerR, 20f, 0f, 0f)
            rotMatrix(leftBrakingPose.matArmUpperL, 30f, 30f, 0f)
            rotMatrix(leftBrakingPose.matArmLowerL, -50f, 0f, 0f)
            rotMatrix(leftBrakingPose.matLegUpperR, -20f, -45f, -30f)
            rotMatrix(leftBrakingPose.matLegLowerR, -10f, 0f, 0f)
            rotMatrix(leftBrakingPose.matLegUpperL, 0f, -5f, -10f)
            mirrorPose(rightBrakingPose, leftBrakingPose)

            leftBrakingLeanedPose.set(leftBrakingPose)
            rotMatrix(leftBrakingLeanedPose.matWaist, 0f, 35f, 0f)
            rotMatrix(leftBrakingLeanedPose.matChest, 0f, 0f, -10f)
            rotMatrix(leftBrakingLeanedPose.matHead, 0f, -25f, -15f)
            rotMatrix(leftBrakingLeanedPose.matArmUpperR, 50f, -10f, -60f)
            rotMatrix(leftBrakingLeanedPose.matArmLowerR, -30f, 0f, 0f)
            rotMatrix(leftBrakingLeanedPose.matArmUpperL, -40f, -40f, 0f)
            rotMatrix(leftBrakingLeanedPose.matArmLowerL, 15f, 0f, 0f)
            mirrorPose(rightBrakingLeanedPose, leftBrakingLeanedPose)

            leftBrakingFootDraggingPose.set(leftBrakingPose)
            rotMatrix(leftBrakingFootDraggingPose.matLegUpperL, 20f, 50f, 0f)
            rotMatrix(leftBrakingFootDraggingPose.matLegLowerL, -90f, 0f, 0f)
            mirrorPose(rightBrakingFootDraggingPose, leftBrakingFootDraggingPose)

            leftBrakingFootDraggingLeanedPose.set(leftBrakingFootDraggingPose)
            rotMatrix(leftBrakingFootDraggingLeanedPose.matWaist, 0f, 35f, 0f)
            rotMatrix(leftBrakingFootDraggingLeanedPose.matChest, 0f, 0f, -10f)
            rotMatrix(leftBrakingFootDraggingLeanedPose.matHead, 0f, -25f, -15f)
            rotMatrix(leftBrakingFootDraggingLeanedPose.matArmUpperR, 50f, -10f, -60f)
            rotMatrix(leftBrakingFootDraggingLeanedPose.matArmLowerR, -30f, 0f, 0f)
            rotMatrix(leftBrakingFootDraggingLeanedPose.matArmUpperL, -40f, -40f, 0f)
            rotMatrix(leftBrakingFootDraggingLeanedPose.matArmLowerL, 15f, 0f, 0f)
            mirrorPose(rightBrakingFootDraggingLeanedPose, leftBrakingFootDraggingLeanedPose)

            leftBrakingKneeDraggingPose.set(leftBrakingPose)
            rotMatrix(leftBrakingKneeDraggingPose.matLegUpperL, -10f, 50f, 0f)
            rotMatrix(leftBrakingKneeDraggingPose.matLegLowerL, 10f, 0f, 0f)
            mirrorPose(rightBrakingKneeDraggingPose, leftBrakingKneeDraggingPose)

            leftBrakingKneeDraggingLeanedPose.set(leftBrakingKneeDraggingPose)
            rotMatrix(leftBrakingKneeDraggingLeanedPose.matWaist, 0f, 35f, 0f)
            rotMatrix(leftBrakingKneeDraggingLeanedPose.matChest, 0f, 0f, -10f)
            rotMatrix(leftBrakingKneeDraggingLeanedPose.matHead, 0f, -25f, -15f)
            rotMatrix(leftBrakingKneeDraggingLeanedPose.matArmUpperR, 50f, -10f, -60f)
            rotMatrix(leftBrakingKneeDraggingLeanedPose.matArmLowerR, -30f, 0f, 0f)
            rotMatrix(leftBrakingKneeDraggingLeanedPose.matArmUpperL, -40f, -40f, 0f)
            rotMatrix(leftBrakingKneeDraggingLeanedPose.matArmLowerL, 15f, 0f, 0f)
            mirrorPose(rightBrakingKneeDraggingLeanedPose, leftBrakingKneeDraggingLeanedPose)

            leftThottlingPose.matHip.translate(1f, 0f, 0.25f)
            rotMatrix(leftThottlingPose.matHip, 15f, 0f, 0f)
            rotMatrix(leftThottlingPose.matWaist, 25f, -35f, 0f)
            rotMatrix(leftThottlingPose.matChest, 10f, 0f, 10f)
            rotMatrix(leftThottlingPose.matHead, -55f, 25f, 15f)
            rotMatrix(leftThottlingPose.matArmUpperR, -30f, 30f, 0f)
            rotMatrix(leftThottlingPose.matArmLowerR, -35f, 0f, 0f)
            rotMatrix(leftThottlingPose.matArmUpperL, 20f, 40f, -20f)
            rotMatrix(leftThottlingPose.matArmLowerL, -90f, 0f, 0f)
            rotMatrix(leftThottlingPose.matLegUpperR, -30f, -25f, -30f)
            rotMatrix(leftThottlingPose.matLegLowerR, -10f, 0f, 0f)
            rotMatrix(leftThottlingPose.matLegUpperL, -20f, -5f, -10f)
            mirrorPose(rightThottlingPose, leftThottlingPose)

            leftThottlingLeanedPose.set(leftThottlingPose)
            rotMatrix(leftThottlingLeanedPose.matWaist, 0f, 35f, 0f)
            rotMatrix(leftThottlingLeanedPose.matChest, 0f, 0f, -10f)
            rotMatrix(leftThottlingLeanedPose.matHead, 0f, -25f, -15f)
            rotMatrix(leftThottlingLeanedPose.matArmUpperR, 30f, -50f, -40f)
            rotMatrix(leftThottlingLeanedPose.matArmLowerR, -30f, 0f, 0f)
            rotMatrix(leftThottlingLeanedPose.matArmUpperL, -20f, -60f, 20f)
            rotMatrix(leftThottlingLeanedPose.matArmLowerL, 20f, 0f, 0f)
            mirrorPose(rightThottlingLeanedPose, leftThottlingLeanedPose)

            leftThottlingKneeDraggingPose.set(leftThottlingPose)
            rotMatrix(leftThottlingKneeDraggingPose.matLegUpperL, -10f, 50f, 0f)
            rotMatrix(leftThottlingKneeDraggingPose.matLegLowerL, 10f, 0f, 0f)
            mirrorPose(rightThottlingKneeDraggingPose, leftThottlingKneeDraggingPose)

            leftThottlingKneeDraggingLeanedPose.set(leftThottlingKneeDraggingPose)
            rotMatrix(leftThottlingKneeDraggingLeanedPose.matWaist, 0f, 35f, 0f)
            rotMatrix(leftThottlingKneeDraggingLeanedPose.matChest, 0f, 0f, -10f)
            rotMatrix(leftThottlingKneeDraggingLeanedPose.matHead, 0f, -25f, -15f)
            rotMatrix(leftThottlingKneeDraggingLeanedPose.matArmUpperR, 30f, -50f, -40f)
            rotMatrix(leftThottlingKneeDraggingLeanedPose.matArmLowerR, -30f, 0f, 0f)
            rotMatrix(leftThottlingKneeDraggingLeanedPose.matArmUpperL, -20f, -60f, 20f)
            rotMatrix(leftThottlingKneeDraggingLeanedPose.matArmLowerL, 20f, 0f, 0f)
            mirrorPose(rightThottlingKneeDraggingLeanedPose, leftThottlingKneeDraggingLeanedPose)

            rotMatrix(crashedPose.matHip, 40f, 0f, 0f)
            rotMatrix(crashedPose.matArmUpperL, crashedPose.matArmUpperR, 70f, 60f, 0f)
            rotMatrix(crashedPose.matArmLowerL, crashedPose.matArmLowerR, 15f, 0f, 0f)
            rotMatrix(crashedPose.matLegUpperL, crashedPose.matLegUpperR, 105f, 30f, 0f)
            rotMatrix(crashedPose.matLegLowerL, crashedPose.matLegLowerR, -120f, 0f, 0f)

            standByPose.set(centerBrakingPose)
            rotMatrix(standByPose.matLegUpperL, standByPose.matLegUpperR, 45f, 15f, 0f)
            rotMatrix(standByPose.matLegLowerL, standByPose.matLegLowerR, -90f, 0f, 0f)
        }
    }

    private val tmpQua = Quaternion()
    private val tmpQua2 = Quaternion()
    private val tmpVec = Vector3()
    private val tmpVec2 = Vector3()
    private fun lerpMatrix(m: Matrix4, a: Matrix4, b: Matrix4, d: Float) {
        m.idt()
        a.getRotation(tmpQua)
        b.getRotation(tmpQua2)
        a.getTranslation(tmpVec)
        b.getTranslation(tmpVec2)
        tmpQua.slerp(tmpQua2, d)
        tmpVec.lerp(tmpVec2, d)
        m.set(tmpQua)
        m.trn(tmpVec)
    }

    private fun lerpPose(p: Pose, a: Pose, b: Pose, d: Float) {
        lerpMatrix(p.matHead, a.matHead, b.matHead, d)
        lerpMatrix(p.matChest, a.matChest, b.matChest, d)
        lerpMatrix(p.matWaist, a.matWaist, b.matWaist, d)
        lerpMatrix(p.matHip, a.matHip, b.matHip, d)
        lerpMatrix(p.matArmUpperL, a.matArmUpperL, b.matArmUpperL, d)
        lerpMatrix(p.matArmLowerL, a.matArmLowerL, b.matArmLowerL, d)
        lerpMatrix(p.matArmUpperR, a.matArmUpperR, b.matArmUpperR, d)
        lerpMatrix(p.matArmLowerR, a.matArmLowerR, b.matArmLowerR, d)
        lerpMatrix(p.matLegUpperL, a.matLegUpperL, b.matLegUpperL, d)
        lerpMatrix(p.matLegLowerL, a.matLegLowerL, b.matLegLowerL, d)
        lerpMatrix(p.matLegUpperR, a.matLegUpperR, b.matLegUpperR, d)
        lerpMatrix(p.matLegLowerR, a.matLegLowerR, b.matLegLowerR, d)
    }
    
    init {
        poseStandBy = standByPose
        poseGo = centerBrakingPose
        state.poseDst = poseStandBy
        state.poseDst?.let { state.poseSrc.set(it) }
        state.poseDst?.let { state.pose.set(it) }
        state.poseTime = 1f
        state.poseTimeRemaining = 0f
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
    
    private fun getDirectionNum(td: TrackDirection?): Int {
        return when (td) {
            TrackDirection.Straight, TrackDirection.LeftLow, TrackDirection.RightLow -> 0
            TrackDirection.LeftMed, TrackDirection.LeftHigh, TrackDirection.LeftSharp -> 1
            TrackDirection.RightMed, TrackDirection.RightHigh, TrackDirection.RightSharp -> -1
            else -> 0
        }
    }
    
    private val tmpVec4 = Vector3()
    private val tmpVec5 = Vector3()
    private val tmpVec6 = Vector3()
    private val tmpQua4 = Quaternion()
    private val tmpQua5 = Quaternion()
    private val leanedPose = Pose()
    
    fun update(delta: Float) {
        val moto = motorcycle ?: return
        val bikeThottleBrakeReading = moto.getEngineAndBrakeMeter()
        val leanReading = moto.getLeanMeter()
        
        if (state.attached) {
            val strengthDecayFactor = 0.1f
            val strengthRecoverFactor = 0.04f
            var strengthDecay = 0f
            if (bikeThottleBrakeReading < 0) strengthDecay = max(strengthDecay, -bikeThottleBrakeReading * 0.5f)
            if (leanReading != 0f) strengthDecay = max(strengthDecay, abs(leanReading))
            state.strength -= strengthDecay * strengthDecayFactor * delta
            state.strength += strengthRecoverFactor * delta
            if (state.strength > 1f) state.strength = 1f
            if (state.strength < 0f) state.strength = 0f
        }
        
        if (state.attached && !moto.state.isStandBy) {    
            val bodyShiftTargetO = state.bodyShiftTarget
            val bodyExposeTargetO = state.bodyExposeTarget
            
            val tdChange = track.getTrackeeDirectionChange(this, 1)
            if (tdChange != null) state.directionCurrent = getDirectionNum(tdChange)
            val tdNotice = track.getTrackeeDirectionNotice(this)
            if (tdNotice != null) state.directionNoticed = getDirectionNum(tdNotice)
            if (state.directionCurrent == state.directionNoticed) {
                state.bodyShiftTarget = state.directionCurrent.toFloat()
            } else if (state.directionCurrent == 0 && state.directionNoticed != 0) {
                state.bodyShiftTarget = state.directionNoticed.toFloat()
            }
            
            if (bikeThottleBrakeReading > 0) {
                state.bodyExposeTarget = 0f
            } else if (bikeThottleBrakeReading < 0) {
                state.bodyExposeTarget = 1f
            } else {
                if (state.bodyShiftTarget == 0f) {
                    state.bodyExposeTarget = 0f
                } else {
                    state.bodyExposeTarget = 1f
                }
            }
            
            if (bodyShiftTargetO != state.bodyShiftTarget ||
                bodyExposeTargetO != state.bodyExposeTarget) {
                if (state.bodyShiftTarget < 0) {
                    if (state.poseDst !== rightBrakingPose &&
                        state.poseDst !== rightBrakingKneeDraggingPose &&
                        state.poseDst !== rightThottlingPose &&
                        state.poseDst !== rightThottlingKneeDraggingPose)
                        runToPose(if (state.bodyExposeTarget == 0f) rightThottlingPose else rightBrakingPose, 0.5f)
                    else if (state.bodyExposeTarget == 1f) {
                        if (state.poseDst === rightThottlingPose)
                            runToPose(rightBrakingPose, 0.5f)
                        if (state.poseDst === rightThottlingKneeDraggingPose)
                            runToPose(rightBrakingKneeDraggingPose, 0.5f)
                    }
                    else if (state.bodyExposeTarget == 0f) {
                        if (state.poseDst === rightBrakingPose)
                            runToPose(rightThottlingPose, 0.5f)
                        if (state.poseDst === rightBrakingKneeDraggingPose)
                            runToPose(rightThottlingKneeDraggingPose, 0.5f)
                    }
                } else if (state.bodyShiftTarget > 0) {
                    if (state.poseDst !== leftBrakingPose &&
                        state.poseDst !== leftBrakingKneeDraggingPose &&
                        state.poseDst !== leftThottlingPose &&
                        state.poseDst !== leftThottlingKneeDraggingPose)
                        runToPose(if (state.bodyExposeTarget == 0f) leftThottlingPose else leftBrakingPose, 0.5f)
                    else if (state.bodyExposeTarget == 1f) {
                        if (state.poseDst === leftThottlingPose)
                            runToPose(leftBrakingPose, 0.5f)
                        if (state.poseDst === leftThottlingKneeDraggingPose)
                            runToPose(leftBrakingKneeDraggingPose, 0.5f)
                    }
                    else if (state.bodyExposeTarget == 0f) {
                        if (state.poseDst === leftBrakingPose)
                            runToPose(leftThottlingPose, 0.5f)
                        if (state.poseDst === leftBrakingKneeDraggingPose)
                            runToPose(leftThottlingKneeDraggingPose, 0.5f)
                    }
                } else {
                    if (state.poseDst !== centerBrakingPose &&
                        state.poseDst !== centerThottlingPose)
                        runToPose(if (state.bodyExposeTarget == 0f) centerThottlingPose else centerBrakingPose, 0.5f)
                    else if (state.bodyExposeTarget == 0f &&
                            state.poseDst !== centerThottlingPose)
                        runToPose(centerThottlingPose, 0.5f)
                    else if (state.bodyExposeTarget == 1f &&
                            state.poseDst !== centerBrakingPose)
                        runToPose(centerBrakingPose, 0.5f)
                }
            }
            
            val footDraggingAngle = 15f
            if (state.poseTimeRemaining == 0f) {
                if (state.poseDst === leftBrakingPose && moto.state.leanAngle <= 0) {
                    if (moto.state.leanAngle > -footDraggingAngle && state.directionCurrent == 0)
                        runToPose(leftBrakingFootDraggingPose, 0.5f)
                    else
                        runToPose(leftBrakingKneeDraggingPose, 0.25f)
                }
                if (state.poseDst === leftThottlingPose && moto.state.leanAngle <= 0)
                    runToPose(leftThottlingKneeDraggingPose, 0.25f)
                if (state.poseDst === rightBrakingPose && moto.state.leanAngle >= 0) {
                    if (moto.state.leanAngle < footDraggingAngle && state.directionCurrent == 0)
                        runToPose(rightBrakingFootDraggingPose, 0.5f)
                    else
                        runToPose(rightBrakingKneeDraggingPose, 0.25f)
                }
                if (state.poseDst === rightThottlingPose && moto.state.leanAngle >= 0)
                    runToPose(rightThottlingKneeDraggingPose, 0.25f)
            }
            if (state.poseDst === leftBrakingFootDraggingPose && !(moto.state.leanAngle > -footDraggingAngle && state.directionCurrent == 0))
                runToPose(leftBrakingKneeDraggingPose, 0.25f)
            if (state.poseDst === rightBrakingFootDraggingPose && !(moto.state.leanAngle < footDraggingAngle && state.directionCurrent == 0))
                runToPose(rightBrakingKneeDraggingPose, 0.25f)
            
            if (state.poseDst === leftBrakingPose ||
                state.poseDst === leftBrakingFootDraggingPose ||
                state.poseDst === leftBrakingKneeDraggingPose ||
                state.poseDst === leftThottlingPose ||
                state.poseDst === leftThottlingKneeDraggingPose ||
                state.poseDst === rightBrakingPose ||
                state.poseDst === rightBrakingFootDraggingPose ||
                state.poseDst === rightBrakingKneeDraggingPose ||
                state.poseDst === rightThottlingPose ||
                state.poseDst === rightThottlingKneeDraggingPose) {
                val leanReadingSmoothFactor = 5f
                val leanReadingSmoothFactor1 = min(1f, leanReadingSmoothFactor * delta)
                state.leanReadingSmoothed = (1 - leanReadingSmoothFactor1) * state.leanReadingSmoothed + leanReadingSmoothFactor1 * leanReading
                state.poseDst2 = null
                if (state.leanReadingSmoothed <= 0) {
                    if (state.poseDst === leftBrakingPose) { lerpPose(leanedPose, leftBrakingPose, leftBrakingLeanedPose, -state.leanReadingSmoothed); state.poseDst2 = leanedPose }
                    if (state.poseDst === leftBrakingFootDraggingPose) { lerpPose(leanedPose, leftBrakingFootDraggingPose, leftBrakingFootDraggingLeanedPose, -state.leanReadingSmoothed); state.poseDst2 = leanedPose }
                    if (state.poseDst === leftBrakingKneeDraggingPose) { lerpPose(leanedPose, leftBrakingKneeDraggingPose, leftBrakingKneeDraggingLeanedPose, -state.leanReadingSmoothed); state.poseDst2 = leanedPose }
                    if (state.poseDst === leftThottlingPose) { lerpPose(leanedPose, leftThottlingPose, leftThottlingLeanedPose, -state.leanReadingSmoothed); state.poseDst2 = leanedPose }
                    if (state.poseDst === leftThottlingKneeDraggingPose) { lerpPose(leanedPose, leftThottlingKneeDraggingPose, leftThottlingKneeDraggingLeanedPose, -state.leanReadingSmoothed); state.poseDst2 = leanedPose }
                }
                if (state.leanReadingSmoothed >= 0) {
                    if (state.poseDst === rightBrakingPose) { lerpPose(leanedPose, rightBrakingPose, rightBrakingLeanedPose, state.leanReadingSmoothed); state.poseDst2 = leanedPose }
                    if (state.poseDst === rightBrakingFootDraggingPose) { lerpPose(leanedPose, rightBrakingFootDraggingPose, rightBrakingFootDraggingLeanedPose, state.leanReadingSmoothed); state.poseDst2 = leanedPose }
                    if (state.poseDst === rightBrakingKneeDraggingPose) { lerpPose(leanedPose, rightBrakingKneeDraggingPose, rightBrakingKneeDraggingLeanedPose, state.leanReadingSmoothed); state.poseDst2 = leanedPose }
                    if (state.poseDst === rightThottlingPose) { lerpPose(leanedPose, rightThottlingPose, rightThottlingLeanedPose, state.leanReadingSmoothed); state.poseDst2 = leanedPose }
                    if (state.poseDst === rightThottlingKneeDraggingPose) { lerpPose(leanedPose, rightThottlingKneeDraggingPose, rightThottlingKneeDraggingLeanedPose, state.leanReadingSmoothed); state.poseDst2 = leanedPose }
                }
            } else {
                state.leanReadingSmoothed = 0f
                state.poseDst2 = null
            }
            
            if (state.poseDst === poseStandBy) {
                state.poseDst2 = null
                poseGo?.let { runToPose(it, 0.25f) }
            }
        } else if (state.attached && moto.state.isStandBy) {
            if (state.poseDst !== poseStandBy) {
                state.poseDst2 = null
                poseStandBy?.let { runToPose(it, 0.25f) }
            }
        } else {
            val detachedVeloDecay = 1f
            state.detachedVelo.sub(
                    state.detachedVelo.x * detachedVeloDecay * delta,
                    state.detachedVelo.y * detachedVeloDecay * delta,
                    state.detachedVelo.z * detachedVeloDecay * delta)
            state.detachedPos.trn(state.detachedVelo)
            
            if (!track.isInsideTrack(this) &&
                track.getTrackCollisionVector(this, tmpVec4, tmpVec5)) {
                state.detachedPos.getTranslation(tmpVec6)
                state.detachedPos.trn(tmpVec4.sub(tmpVec6))
                state.detachedVelo.sub(tmpVec5.mul(2f * tmpVec5.dot(state.detachedVelo))).mul(0.5f)
            }
            val detachedHeight = 0.25f
            state.detachedPos.getTranslation(tmpVec4)
            state.detachedPos.trn(0f, detachedVeloDecay * delta * (detachedHeight - tmpVec4.y), 0f)
            state.detachedPos.getRotation(tmpQua4)
            tmpQua5.setEulerAngles(0f, 0f, 0f)
            tmpQua4.slerp(tmpQua5, 0.1f)
            state.detachedPos.getTranslation(tmpVec4)
            state.detachedPos.set(tmpQua4).trn(tmpVec4)
        }
        
        updatePose(delta)
    }

    private fun runToPose(targetPose: Pose, t: Float) {
        state.poseSrc.set(state.pose)
        state.poseDst = targetPose
        state.poseTime = t
        state.poseTimeRemaining = t
    }

    private fun updatePose(delta: Float) {
        if (state.poseTimeRemaining > 0) {
            state.poseTimeRemaining -= delta
            if (state.poseTimeRemaining <= 0) {
                state.poseTimeRemaining = 0f
            }
        }
        
        val pdst2 = state.poseDst2
        val pdst = state.poseDst
        if (pdst != null) {
            if (pdst2 == null)
                lerpPose(state.pose, pdst, state.poseSrc, state.poseTimeRemaining / state.poseTime)
            else
                lerpPose(state.pose, pdst2, state.poseSrc, state.poseTimeRemaining / state.poseTime)
        }
    }

    fun isKneeDragging(): Boolean {
        val moto = motorcycle ?: return false
        return abs(moto.state.leanAngle) >= 45 &&
            (state.poseDst === leftThottlingKneeDraggingPose ||
            state.poseDst === rightThottlingKneeDraggingPose ||
            state.poseDst === leftBrakingKneeDraggingPose ||
            state.poseDst === rightBrakingKneeDraggingPose) &&
            state.poseTimeRemaining <= 0
    }

    override fun getTrackeePos(vec: Vector3) {
        val moto = motorcycle
        if (state.attached && moto != null) {
            // Note: In logic we might just approximate this to the motorcycle's position for collisions 
            // since ridePos and getLeanHeightShift are renderer concepts, OR we provide access to them.
            // For now, we will just use the motorcycle's position as the rider's position for track collisions.
            moto.state.pos.getTranslation(vec)
        } else {
            state.detachedPos.getTranslation(vec)
        }
    }

    override fun setLastTrackSegment(ts: TrackSegment?) {
        state.lastTrackSegment = ts
    }

    override fun getLastTrackSegment(): TrackSegment? {
        return state.lastTrackSegment
    }

    fun attach() {
        state.attached = true
        state.poseDst2 = null
        poseStandBy?.let { state.poseSrc.set(it) }
        poseStandBy?.let { state.pose.set(it) }
        poseStandBy?.let { runToPose(it, Float.MIN_VALUE) }
        
        state.directionCurrent = 0
        state.directionNoticed = 0
        state.bodyShiftTarget = 0f
        state.bodyExposeTarget = 1f
        state.leanReadingSmoothed = 0f
    }

    fun detach() {
        state.poseDst2 = null
        runToPose(crashedPose, 0.5f)
        
        state.attached = false
        val moto = motorcycle
        if (moto != null) {
            // we should set detachedPos to motorcycle pos initially.
            state.detachedPos.set(moto.state.pos)
            state.detachedVelo.set(moto.state.bikeVelo)
        }
    }
    
    fun getStrength(): Float {
        return state.strength
    }
}

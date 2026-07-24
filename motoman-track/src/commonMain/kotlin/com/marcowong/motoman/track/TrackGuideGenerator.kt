package com.marcowong.motoman.track

import com.marcowong.motoman.track.math.Vector3
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt

class TrackGuideGenerator {
    var earlyNoticeDistance = 20f
    
    private val getSegAngleVec = Vector3()
    private fun getSegAngle(ts: TrackSegment): Float {
        getSegAngleVec.set(ts.x2 - ts.x1, ts.y2 - ts.y1, 0f).nor()
        var turnAngle = (atan2(getSegAngleVec.y.toDouble(), getSegAngleVec.x.toDouble()) * (180.0 / PI)).toFloat() + 90f
        if (turnAngle >= 360f) turnAngle -= 360f
        if (turnAngle < 0f) turnAngle += 360f
        return turnAngle
    }
    
    private fun getDirByAngle(angle: Float): TrackDirection {
        if (angle <= -75f) return TrackDirection.LeftSharp
        if (angle >= 75f) return TrackDirection.RightSharp
        if (angle <= -50f) return TrackDirection.LeftHigh
        if (angle >= 50f) return TrackDirection.RightHigh
        if (angle <= -30f) return TrackDirection.LeftMed
        if (angle >= 30f) return TrackDirection.RightMed
        if (angle <= -10f) return TrackDirection.LeftLow
        if (angle >= 10f) return TrackDirection.RightLow
        return TrackDirection.Straight
    }
    
    private fun getSegLen(ts: TrackSegment): Float {
        return sqrt((ts.x2 - ts.x1).toDouble().pow(2.0) + (ts.y2 - ts.y1).toDouble().pow(2.0)).toFloat()
    }
    
    fun generate(tss: List<TrackSegment>) {
        var tLen = 0f
        for (ts in tss) tLen += getSegLen(ts)
        
        var tLenCur = 0f
        var lastTurnAngle = getSegAngle(tss[0])
        var lastDir = getDirByAngle(0f)
        
        for (i in tss.indices) {
            val ts = tss[i]
            val turnAngle = getSegAngle(ts)
            var angleDiff = turnAngle - lastTurnAngle
            if (angleDiff > 180f) angleDiff -= 360f
            if (angleDiff <= -180f) angleDiff += 360f
            
            val dir = getDirByAngle(angleDiff)
            
            ts.attributes["length"] = tLenCur
            ts.attributes["percentage"] = tLenCur / tLen
            ts.attributes["angle"] = angleDiff
            ts.attributes["direction"] = dir
            
            if (dir != lastDir) {
                ts.attributes["directionChange"] = dir
                var earlyNoticeTS = ts
                var segLenT = 0f
                for (j in i - 1 downTo 0) {
                    val candidate = tss[j]
                    if (candidate.attributes["directionChange"] != null) {
                        if (candidate.attributes["directionNotice"] == null) {
                            earlyNoticeTS = candidate
                        }
                        break
                    }
                    earlyNoticeTS = candidate
                    segLenT += getSegLen(candidate)
                    if (segLenT > earlyNoticeDistance) break
                }
                earlyNoticeTS.attributes["directionNotice"] = dir
            }
            lastTurnAngle = turnAngle
            lastDir = dir
            tLenCur += getSegLen(ts)
        }
    }
}

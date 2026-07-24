package com.marcowong.motoman.track.logic

import com.marcowong.motoman.track.TrackData
import com.marcowong.motoman.track.TrackDirection
import com.marcowong.motoman.track.TrackSegLine
import com.marcowong.motoman.track.TrackSegLines
import com.marcowong.motoman.track.TrackSegment
import com.marcowong.motoman.track.math.Matrix4
import com.marcowong.motoman.track.math.Vector3
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.sign
import kotlin.math.sqrt

class Track(trackData: TrackData) {
    val trackSegments: List<TrackSegment> = trackData.trackSegments!!
    val tsStart: TrackSegment
    val tsEnd: TrackSegment
    val trackSegmentsOfStart: List<TrackSegment>
    val trackSegmentsOfEnd: List<TrackSegment>
    val trackScaleFactor = 40f
    val trackSegHeight = 5f
    val trackScaleMat = Matrix4()
    val trackTSLen: Float

    init {
        trackScaleMat.scale(trackScaleFactor, trackSegHeight, trackScaleFactor)
        
        val firstTs = trackSegments[0]
        trackTSLen = sqrt(
            (firstTs.x2 - firstTs.x1) * trackScaleFactor * (firstTs.x2 - firstTs.x1) * trackScaleFactor +
            (firstTs.y2 - firstTs.y1) * trackScaleFactor * (firstTs.y2 - firstTs.y1) * trackScaleFactor
        )

        val tmpVec6 = Vector3()
        for (ts in trackSegments) {
            val tsl = ts.attributes["lines"] as TrackSegLines
            val tslS = TrackSegLines()
            
            tmpVec6.set(tsl.h.x1, 0f, tsl.h.y1).mul(trackScaleMat); tslS.h.x1 = tmpVec6.x; tslS.h.y1 = tmpVec6.z
            tmpVec6.set(tsl.h.x2, 0f, tsl.h.y2).mul(trackScaleMat); tslS.h.x2 = tmpVec6.x; tslS.h.y2 = tmpVec6.z
            tmpVec6.set(tsl.t.x1, 0f, tsl.t.y1).mul(trackScaleMat); tslS.t.x1 = tmpVec6.x; tslS.t.y1 = tmpVec6.z
            tmpVec6.set(tsl.t.x2, 0f, tsl.t.y2).mul(trackScaleMat); tslS.t.x2 = tmpVec6.x; tslS.t.y2 = tmpVec6.z
            tmpVec6.set(tsl.l.x1, 0f, tsl.l.y1).mul(trackScaleMat); tslS.l.x1 = tmpVec6.x; tslS.l.y1 = tmpVec6.z
            tmpVec6.set(tsl.l.x2, 0f, tsl.l.y2).mul(trackScaleMat); tslS.l.x2 = tmpVec6.x; tslS.l.y2 = tmpVec6.z
            tmpVec6.set(tsl.r.x1, 0f, tsl.r.y1).mul(trackScaleMat); tslS.r.x1 = tmpVec6.x; tslS.r.y1 = tmpVec6.z
            tmpVec6.set(tsl.r.x2, 0f, tsl.r.y2).mul(trackScaleMat); tslS.r.x2 = tmpVec6.x; tslS.r.y2 = tmpVec6.z
            
            ts.attributes["linesS"] = tslS
        }
        
        tsStart = trackSegments[0]
        tsEnd = trackSegments[trackSegments.size - 1]
        
        val segmentsStart = mutableListOf<TrackSegment>()
        for (i in 0 until 5) {
            val ts = TrackSegment()
            val xOffset = tsStart.x1 - tsStart.x2
            val yOffset = tsStart.y1 - tsStart.y2
            ts.x1 = tsStart.x1 + xOffset * (i + 1)
            ts.y1 = tsStart.y1 + yOffset * (i + 1)
            ts.w1 = tsStart.w1
            ts.l1 = tsStart.l1
            ts.r1 = tsStart.r1
            ts.x2 = tsStart.x1 + xOffset * i
            ts.y2 = tsStart.y1 + yOffset * i
            ts.w2 = tsStart.w1
            ts.l2 = tsStart.l1
            ts.r2 = tsStart.r1
            segmentsStart.add(ts)
        }
        trackSegmentsOfStart = segmentsStart
        
        val segmentsEnd = mutableListOf<TrackSegment>()
        for (i in 0 until 5) {
            val ts = TrackSegment()
            val xOffset = tsEnd.x2 - tsEnd.x1
            val yOffset = tsEnd.y2 - tsEnd.y1
            ts.x1 = tsEnd.x2 + xOffset * i
            ts.y1 = tsEnd.y2 + yOffset * i
            ts.w1 = tsEnd.w2
            ts.l1 = tsEnd.l2
            ts.r1 = tsEnd.r2
            ts.x2 = tsEnd.x2 + xOffset * (i + 1)
            ts.y2 = tsEnd.y2 + yOffset * (i + 1)
            ts.w2 = tsEnd.w2
            ts.l2 = tsEnd.l2
            ts.r2 = tsEnd.r2
            segmentsEnd.add(ts)
        }
        trackSegmentsOfEnd = segmentsEnd
    }

    private val tmpVec4 = Vector3()
    private fun getTrackSegmentDistance2(ts: TrackSegment, px: Float, py: Float, pz: Float): Float {
        val ox = (ts.x1 + ts.x2) * 0.5f
        val oy = (ts.y1 + ts.y2) * 0.5f
        return tmpVec4.set(ox, 0f, oy).mul(trackScaleMat).sub(px, py, pz).len2()
    }

    private fun getNearestTrackSegment(px: Float, py: Float, pz: Float): TrackSegment {
        var nearestTS = trackSegments[0]
        var nearestTSDis = Float.POSITIVE_INFINITY
        for (i in trackSegments.indices) {
            val ts = trackSegments[i]
            val dis = getTrackSegmentDistance2(ts, px, py, pz)
            if (nearestTSDis >= dis) {
                nearestTS = ts
                nearestTSDis = dis
            }
        }
        return nearestTS
    }

    private val tmpVec3 = Vector3()
    fun getTrackeeTrackSegment(trackee: ITrackee): TrackSegment {
        trackee.getTrackeePos(tmpVec3)
        tmpVec3.y = 0f
        val lastTS = trackee.getLastTrackSegment() ?: return getNearestTrackSegment(tmpVec3.x, tmpVec3.y, tmpVec3.z)

        var nearestTS = lastTS
        var nearestTSDis = getTrackSegmentDistance2(lastTS, tmpVec3.x, tmpVec3.y, tmpVec3.z)
        
        if (lastTS.next != null) {
            val dis = getTrackSegmentDistance2(lastTS.next!!, tmpVec3.x, tmpVec3.y, tmpVec3.z)
            if (dis < nearestTSDis) {
                nearestTSDis = dis
                nearestTS = lastTS.next!!
            }
        }
        if (lastTS.prev != null) {
            val dis = getTrackSegmentDistance2(lastTS.prev!!, tmpVec3.x, tmpVec3.y, tmpVec3.z)
            if (dis < nearestTSDis) {
                nearestTSDis = dis
                nearestTS = lastTS.prev!!
            }
        }
        
        return if (nearestTSDis <= trackTSLen * trackTSLen) {
            nearestTS
        } else {
            getNearestTrackSegment(tmpVec3.x, tmpVec3.y, tmpVec3.z)
        }
    }

    fun updateCurrentTrackSegment(trackee: ITrackee) {
        trackee.setLastTrackSegment(getTrackeeTrackSegment(trackee))
    }

    fun isInTrackEnd(trackee: ITrackee): Boolean {
        return getTrackeeTrackSegment(trackee) === tsEnd
    }

    fun getStartSpawnPosition(): Matrix4 {
        return getSpawnPosition(tsStart)
    }

    fun getSpawnPosition(trackee: ITrackee): Matrix4 {
        val ts = getTrackeeTrackSegment(trackee)
        return getSpawnPosition(ts)
    }

    private fun getSpawnPosition(ts: TrackSegment): Matrix4 {
        val tmpVec = Vector3()
        tmpVec.set((ts.x1 + ts.x2) * 0.5f, 0f, (ts.y1 + ts.y2) * 0.5f)
        val pos = Matrix4()
        pos.mul(trackScaleMat)
        tmpVec.mul(pos)
        pos.idt()
        pos.trn(tmpVec)
        
        tmpVec.set(ts.y2 - ts.y1, ts.x2 - ts.x1, 0f).nor()
        val or = atan2(tmpVec.y.toDouble(), tmpVec.x.toDouble()).toFloat() * (180f / PI.toFloat())
        pos.rotate(0f, 1f, 0f, or)
        
        return pos
    }

    fun getTrackeeDirectionNotice(trackee: ITrackee): TrackDirection? {
        val ts = getTrackeeTrackSegment(trackee)
        return ts.attributes["directionNotice"] as? TrackDirection
    }

    fun getTrackeeDirectionChange(trackee: ITrackee, forseen: Int): TrackDirection? {
        var ts: TrackSegment? = getTrackeeTrackSegment(trackee)
        for (i in 0 until forseen) {
            if (ts?.next != null) {
                ts = ts.next
            }
        }
        return ts?.attributes?.get("directionChange") as? TrackDirection
    }

    private fun pointLineSide(x1: Float, y1: Float, x2: Float, y2: Float, px: Float, py: Float): Float {
        return sign((x2 - x1) * (py - y1) - (y2 - y1) * (px - x1))
    }

    private val tmpVec5 = Vector3()
    private val tmpTSL = arrayOfNulls<TrackSegLine>(4)
    private fun isInsideTrackInternal(trackee: ITrackee, ts: TrackSegment): Boolean {
        trackee.getTrackeePos(tmpVec5)
        val tsl = ts.attributes["linesS"] as TrackSegLines
        tmpTSL[0] = tsl.h
        tmpTSL[1] = tsl.l
        tmpTSL[2] = tsl.r
        tmpTSL[3] = tsl.t
        for (i in 0 until 4) {
            val t = tmpTSL[i]!!
            val outside = pointLineSide(t.x1, t.y1, t.x2, t.y2, tmpVec5.x, tmpVec5.z) < 0
            if (outside) return false
        }
        return true
    }

    fun isInsideTrack(trackee: ITrackee): Boolean {
        val ts = getTrackeeTrackSegment(trackee)
        return isInsideTrackInternal(trackee, ts) ||
               (ts.next != null && isInsideTrackInternal(trackee, ts.next!!)) ||
               (ts.prev != null && isInsideTrackInternal(trackee, ts.prev!!))
    }

    private val tmpVec10 = Vector3()
    private fun getTrackCollisionVectorInternal(trackee: ITrackee, ts: TrackSegment, collisionPos: Vector3, collisionNor: Vector3): Boolean {
        trackee.getTrackeePos(tmpVec10)
        val tsl = ts.attributes["linesS"] as TrackSegLines
        val notThisSegment = pointLineSide(tsl.h.x1, tsl.h.y1, tsl.h.x2, tsl.h.y2, tmpVec10.x, tmpVec10.z) < 0 ||
                             pointLineSide(tsl.t.x1, tsl.t.y1, tsl.t.x2, tsl.t.y2, tmpVec10.x, tmpVec10.z) < 0
        if (notThisSegment) return false
        
        if (pointLineSide(tsl.l.x1, tsl.l.y1, tsl.l.x2, tsl.l.y2, tmpVec10.x, tmpVec10.z) < 0) {
            val m = (tsl.l.y2 - tsl.l.y1) / (tsl.l.x2 - tsl.l.x1)
            val b = tsl.l.y1 - m * tsl.l.x1
            collisionPos.x = (m * tmpVec10.z + tmpVec10.x - m * b) / (m * m + 1)
            collisionPos.y = tmpVec10.y
            collisionPos.z = (m * m * tmpVec10.z + m * tmpVec10.x + b) / (m * m + 1)
            collisionNor.x = -(tsl.l.y2 - tsl.l.y1)
            collisionNor.y = 0f
            collisionNor.z = (tsl.l.x2 - tsl.l.x1)
            collisionNor.nor()
            return true
        }
        if (pointLineSide(tsl.r.x1, tsl.r.y1, tsl.r.x2, tsl.r.y2, tmpVec10.x, tmpVec10.z) < 0) {
            val m = (tsl.r.y2 - tsl.r.y1) / (tsl.r.x2 - tsl.r.x1)
            val b = tsl.r.y1 - m * tsl.r.x1
            collisionPos.x = (m * tmpVec10.z + tmpVec10.x - m * b) / (m * m + 1)
            collisionPos.y = tmpVec10.y
            collisionPos.z = (m * m * tmpVec10.z + m * tmpVec10.x + b) / (m * m + 1)
            collisionNor.x = -(tsl.r.y2 - tsl.r.y1)
            collisionNor.y = 0f
            collisionNor.z = (tsl.r.x2 - tsl.r.x1)
            collisionNor.nor()
            return true
        }
        return false
    }

    private val tmpVec9 = Vector3()
    private val tmpVec11 = Vector3()
    fun getTrackCollisionVector(trackee: ITrackee, collisionPos: Vector3, collisionNor: Vector3): Boolean {
        val ts = getTrackeeTrackSegment(trackee)
        collisionPos.set(0f, 0f, 0f)
        collisionNor.set(0f, 0f, 0f)
        var collisionCount = 0
        if (getTrackCollisionVectorInternal(trackee, ts, tmpVec9, tmpVec11)) {
            ++collisionCount
            collisionPos.add(tmpVec9)
            collisionNor.add(tmpVec11)
        }
        if (ts.next != null && getTrackCollisionVectorInternal(trackee, ts.next!!, tmpVec9, tmpVec11)) {
            ++collisionCount
            collisionPos.add(tmpVec9)
            collisionNor.add(tmpVec11)
        }
        if (ts.prev != null && getTrackCollisionVectorInternal(trackee, ts.prev!!, tmpVec9, tmpVec11)) {
            ++collisionCount
            collisionPos.add(tmpVec9)
            collisionNor.add(tmpVec11)
        }
        if (collisionCount > 1) {
            collisionPos.div(collisionCount.toFloat())
            collisionNor.nor()
            return true
        } else if (collisionCount == 1) {
            return true
        } else {
            return false
        }
    }
    fun getTrackeeTrackPercentage(trackee: ITrackee): Float {
        val ts = getTrackeeTrackSegment(trackee)
        val index = trackSegments.indexOf(ts)
        return if (index >= 0) {
            index.toFloat() / trackSegments.size
        } else {
            0f
        }
    }
}

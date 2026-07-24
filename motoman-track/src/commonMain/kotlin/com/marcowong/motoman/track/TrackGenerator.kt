package com.marcowong.motoman.track

import com.marcowong.motoman.track.math.Matrix4
import com.marcowong.motoman.track.math.Vector3
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.max

class TrackGenerator {
    var random: IRandom = BasicRandom(100000)
    var turnAngleSmoothFactor = 2
    var turnAngleZeroFactor = 0.2f
    var sharpAngleReference = 75f
    var trackLen = 100f
    var segLen = 1f
    var segWidth = 1f
    var segPad = 0.5f

    private var maxProgress = 0f
    private var tTLen = 0f
    var trackDataRandomSeed: Long = 100000

    private class UpdateTurnAngleContext {
        var retryCount = 0
    }

    private var lastTurnAngle = 0f
    private var updateTurnAngleCounter = 0

    private fun updateTurnAngle(context: UpdateTurnAngleContext): Float {
        if (++context.retryCount > 10) return Float.NaN
        if (((++updateTurnAngleCounter) % turnAngleSmoothFactor) == 0) {
            return lastTurnAngle
        }
        if (abs(lastTurnAngle) > 60 && random.next() < 0.8f) {
            return lastTurnAngle
        }
        if (abs(lastTurnAngle) > 30 && random.next() < 0.2f) {
            return lastTurnAngle
        }
        var a = random.next()
        a = max(0f, -turnAngleZeroFactor + a * (1 + turnAngleZeroFactor)) * 90f
        val signedA = a * if (random.next() > 0.5f) 1f else -1f
        if (a < sharpAngleReference) {
            lastTurnAngle = signedA
        }
        return signedA
    }

    fun generate(): TrackData? {
        val tss = mutableListOf<TrackSegment>()
        tTLen = 0f
        val turnAngleContextKey = "turnAngleContext"

        var lastSeg = TrackSegment()
        lastSeg.x1 = 0f
        lastSeg.y1 = 0f
        lastSeg.l1 = -segWidth
        lastSeg.r1 = segWidth
        lastSeg.w1 = 0f
        lastSeg.x2 = 0f
        lastSeg.y2 = segLen
        lastSeg.attributes[turnAngleContextKey] = UpdateTurnAngleContext()
        tss.add(lastSeg)
        tTLen += segLen

        val vec = Vector3()
        val mat = Matrix4()
        val st = IsLineIntersectST()
        val linesA = arrayOfNulls<TrackSegLine>(4)
        val linesB = arrayOfNulls<TrackSegLine>(4)

        genLoop@ while (tTLen < trackLen) {
            val turnAngle = updateTurnAngle(lastSeg.attributes[turnAngleContextKey] as UpdateTurnAngleContext)
            if (turnAngle.isNaN()) {
                if (tss.size < 2) {
                    lastSeg.attributes[turnAngleContextKey] = UpdateTurnAngleContext()
                } else {
                    tTLen -= vec.set(lastSeg.x2 - lastSeg.x1, lastSeg.y2 - lastSeg.y1, 0f).len()
                    lastSeg = tss[tss.size - 2]
                    tss.removeAt(tss.size - 1)
                }
                println("pop ${tss.size}")
                continue@genLoop
            }

            val curSeg = TrackSegment()
            curSeg.x1 = lastSeg.x2
            curSeg.y1 = lastSeg.y2
            curSeg.attributes[turnAngleContextKey] = UpdateTurnAngleContext()

            vec.set(lastSeg.x2 - lastSeg.x1, lastSeg.y2 - lastSeg.y1, 0f).nor()
            val lastTurnAngleVal = (atan2(vec.y.toDouble(), vec.x.toDouble()) * (180.0 / PI)).toFloat() - 90f

            curSeg.w1 = lastTurnAngleVal + turnAngle
            vec.set(0f, segLen, 0f)
            mat.idt()
            mat.translate(curSeg.x1, curSeg.y1, 0f)
            mat.rotate(0f, 0f, 1f, curSeg.w1)
            vec.mul(mat)
            curSeg.x2 = vec.x
            curSeg.y2 = vec.y
            curSeg.l1 = -segWidth
            curSeg.r1 = segWidth

            val lastSegLines = getLinesByPathAndHead(lastSeg)
            val curSegLines = getLinesByPathAndHead(curSeg)
            doLineIntersect(
                lastSegLines.l.x1, lastSegLines.l.y1, lastSegLines.l.x2, lastSegLines.l.y2,
                curSegLines.l.x2, curSegLines.l.y2, curSegLines.l.x1, curSegLines.l.y1, st
            )
            val lx: Float
            val ly: Float
            if (!st.t.isNaN()) {
                lx = lastSegLines.l.x1 + (lastSegLines.l.x2 - lastSegLines.l.x1) * st.t
                ly = lastSegLines.l.y1 + (lastSegLines.l.y2 - lastSegLines.l.y1) * st.t
            } else {
                lx = curSegLines.l.x2
                ly = curSegLines.l.y2
            }
            doLineIntersect(
                lastSegLines.r.x2, lastSegLines.r.y2, lastSegLines.r.x1, lastSegLines.r.y1,
                curSegLines.r.x1, curSegLines.r.y1, curSegLines.r.x2, curSegLines.r.y2, st
            )
            val rx: Float
            val ry: Float
            if (!st.t.isNaN()) {
                rx = lastSegLines.r.x2 + (lastSegLines.r.x1 - lastSegLines.r.x2) * st.t
                ry = lastSegLines.r.y2 + (lastSegLines.r.y1 - lastSegLines.r.y2) * st.t
            } else {
                rx = curSegLines.r.x1
                ry = curSegLines.r.y1
            }
            val lLen = vec.set(lx - curSeg.x1, ly - curSeg.y1, 0f).len()
            val rLen = vec.set(rx - curSeg.x1, ry - curSeg.y1, 0f).len()
            vec.set(ry - ly, rx - lx, 0f).nor()
            val ang = (atan2(vec.x.toDouble(), vec.y.toDouble()) * (180.0 / PI)).toFloat()
            lastSeg.l2 = -lLen
            lastSeg.r2 = rLen
            lastSeg.w2 = ang
            curSeg.l1 = -lLen
            curSeg.r1 = rLen
            curSeg.w1 = ang

            linesA[0] = curSegLines.hp
            linesA[1] = curSegLines.lp
            linesA[2] = curSegLines.rp
            linesA[3] = curSegLines.tp
            var noCollision = true
            trackSegCollsionTest1@ for (i in 0 until tss.size - 1) {
                val ts = tss[i]
                val tsLines = getLinesByPathAndAll(ts)
                if (ts === lastSeg) continue
                linesB[0] = tsLines.hp
                linesB[1] = tsLines.lp
                linesB[2] = tsLines.rp
                linesB[3] = tsLines.tp
                for (lineA in linesA) {
                    for (lineB in linesB) {
                        if (isLineIntersect(
                                lineA!!.x1, lineA.y1, lineA.x2, lineA.y2,
                                lineB!!.x1, lineB.y1, lineB.x2, lineB.y2
                            )
                        ) {
                            noCollision = false
                            break@trackSegCollsionTest1
                        }
                    }
                }
            }
            val semiFinalizedLastSegLines = getLinesByPathAndAll(lastSeg)
            linesA[0] = semiFinalizedLastSegLines.hp
            linesA[1] = semiFinalizedLastSegLines.lp
            linesA[2] = semiFinalizedLastSegLines.rp
            linesA[3] = semiFinalizedLastSegLines.tp
            trackSegCollsionTest2@ for (i in 0 until tss.size - 2) {
                val ts = tss[i]
                val tsLines = getLinesByPathAndAll(ts)
                if (ts === lastSeg) continue
                linesB[0] = tsLines.hp
                linesB[1] = tsLines.lp
                linesB[2] = tsLines.rp
                linesB[3] = tsLines.tp
                for (lineA in linesA) {
                    for (lineB in linesB) {
                        if (isLineIntersect(
                                lineA!!.x1, lineA.y1, lineA.x2, lineA.y2,
                                lineB!!.x1, lineB.y1, lineB.x2, lineB.y2
                            )
                        ) {
                            noCollision = false
                            break@trackSegCollsionTest2
                        }
                    }
                }
            }

            if (noCollision) {
                tTLen += vec.set(curSeg.x2 - curSeg.x1, curSeg.y2 - curSeg.y1, 0f).len()
                tss.add(curSeg)
                lastSeg = curSeg
            }
        }

        if (tTLen < trackLen) return null

        val finalSeg = lastSeg
        val finalSegLines = getLinesByPathAndHead(finalSeg)
        val lLen = vec.set(finalSegLines.l.x1 - finalSeg.x2, finalSegLines.l.y1 - finalSeg.y2, 0f).len()
        val rLen = vec.set(finalSegLines.r.x2 - finalSeg.x2, finalSegLines.r.y2 - finalSeg.y2, 0f).len()
        vec.set(finalSegLines.r.y2 - finalSegLines.l.y1, finalSegLines.r.x2 - finalSegLines.l.x1, 0f).nor()
        val ang = (atan2(vec.x.toDouble(), vec.y.toDouble()) * (180.0 / PI)).toFloat()
        finalSeg.l2 = -lLen
        finalSeg.r2 = rLen
        finalSeg.w2 = ang

        var prevTS: TrackSegment? = null
        for (ts in tss) {
            ts.attributes.remove(turnAngleContextKey)
            ts.attributes["lines"] = getLinesByPathAndAll(ts)
            ts.prev = prevTS
            if (prevTS != null) prevTS.next = ts
            prevTS = ts
        }

        val ti = TrackData()
        ti.trackSegments = tss
        ti.randomSeed = trackDataRandomSeed
        return ti
    }

    fun getGenerationProgress(): Float {
        var p = tTLen / trackLen
        if (p > 1f) p = 1f
        if (p > maxProgress) {
            maxProgress = p
            return p
        } else {
            return maxProgress
        }
    }

    private fun getLinesByPathAndHead(ts: TrackSegment): TrackSegLines {
        return getLinesOfTrackSegment(ts, true)
    }

    private fun getLinesByPathAndAll(ts: TrackSegment): TrackSegLines {
        return getLinesOfTrackSegment(ts, false)
    }

    private val getLinesByPathAndHeadTmpVec = Vector3()
    private val getLinesByPathAndHeadTmpVec2 = Vector3()
    private val getLinesByPathAndHeadTmpVec3 = Vector3()
    private val getLinesByPathAndHeadTmpMat = Matrix4()
    private val getLinesByPathAndHeadTmpST = IsLineIntersectST()

    private fun getLinesOfTrackSegment(ts: TrackSegment, isEndUndefined: Boolean): TrackSegLines {
        val tsl = TrackSegLines()

        getLinesByPathAndHeadTmpMat.idt()
        getLinesByPathAndHeadTmpMat.translate(ts.x1, ts.y1, 0f)
        getLinesByPathAndHeadTmpMat.rotate(0f, 0f, 1f, ts.w1)

        getLinesByPathAndHeadTmpVec.set(ts.l1, 0f, 0f)
        getLinesByPathAndHeadTmpVec.mul(getLinesByPathAndHeadTmpMat)
        tsl.h.x1 = getLinesByPathAndHeadTmpVec.x; tsl.l.x2 = tsl.h.x1
        tsl.h.y1 = getLinesByPathAndHeadTmpVec.y; tsl.l.y2 = tsl.h.y1
        getLinesByPathAndHeadTmpVec.set(ts.r1, 0f, 0f)
        getLinesByPathAndHeadTmpVec.mul(getLinesByPathAndHeadTmpMat)
        tsl.h.x2 = getLinesByPathAndHeadTmpVec.x; tsl.r.x1 = tsl.h.x2
        tsl.h.y2 = getLinesByPathAndHeadTmpVec.y; tsl.r.y1 = tsl.h.y2

        getLinesByPathAndHeadTmpVec.set(ts.x2 - ts.x1, ts.y2 - ts.y1, 0f)
        getLinesByPathAndHeadTmpVec.crs(0f, 0f, 1f)
        getLinesByPathAndHeadTmpVec.nor()
        getLinesByPathAndHeadTmpVec2.set(ts.l1, 0f, 0f)
        getLinesByPathAndHeadTmpVec2.mul(getLinesByPathAndHeadTmpMat)
        getLinesByPathAndHeadTmpVec3.set(getLinesByPathAndHeadTmpVec)
        getLinesByPathAndHeadTmpVec3.mul(-(segWidth + segPad))
        getLinesByPathAndHeadTmpVec3.add(ts.x1, ts.y1, 0f)
        doLineIntersect(
            ts.x1, ts.y1,
            getLinesByPathAndHeadTmpVec2.x,
            getLinesByPathAndHeadTmpVec2.y,
            getLinesByPathAndHeadTmpVec3.x,
            getLinesByPathAndHeadTmpVec3.y,
            getLinesByPathAndHeadTmpVec3.x + (ts.x2 - ts.x1),
            getLinesByPathAndHeadTmpVec3.y + (ts.y2 - ts.y1),
            getLinesByPathAndHeadTmpST
        )
        tsl.hp.x1 = ts.x1 + (getLinesByPathAndHeadTmpVec2.x - ts.x1) * getLinesByPathAndHeadTmpST.t
        tsl.lp.x2 = tsl.hp.x1
        tsl.hp.y1 = ts.y1 + (getLinesByPathAndHeadTmpVec2.y - ts.y1) * getLinesByPathAndHeadTmpST.t
        tsl.lp.y2 = tsl.hp.y1
        
        getLinesByPathAndHeadTmpVec3.set(getLinesByPathAndHeadTmpVec)
        getLinesByPathAndHeadTmpVec3.mul(segWidth + segPad)
        getLinesByPathAndHeadTmpVec3.add(ts.x1, ts.y1, 0f)
        doLineIntersect(
            ts.x1, ts.y1,
            getLinesByPathAndHeadTmpVec2.x,
            getLinesByPathAndHeadTmpVec2.y,
            getLinesByPathAndHeadTmpVec3.x,
            getLinesByPathAndHeadTmpVec3.y,
            getLinesByPathAndHeadTmpVec3.x + (ts.x2 - ts.x1),
            getLinesByPathAndHeadTmpVec3.y + (ts.y2 - ts.y1),
            getLinesByPathAndHeadTmpST
        )
        tsl.hp.x2 = ts.x1 + (getLinesByPathAndHeadTmpVec2.x - ts.x1) * getLinesByPathAndHeadTmpST.t
        tsl.rp.x1 = tsl.hp.x2
        tsl.hp.y2 = ts.y1 + (getLinesByPathAndHeadTmpVec2.y - ts.y1) * getLinesByPathAndHeadTmpST.t
        tsl.rp.y1 = tsl.hp.y2

        if (isEndUndefined) {
            getLinesByPathAndHeadTmpVec.set(ts.x2 - ts.x1, ts.y2 - ts.y1, 0f)
            getLinesByPathAndHeadTmpVec.crs(0f, 0f, 1f)
            getLinesByPathAndHeadTmpVec.nor()

            tsl.t.x2 = ts.x2 + getLinesByPathAndHeadTmpVec.x * -segWidth; tsl.l.x1 = tsl.t.x2
            tsl.t.y2 = ts.y2 + getLinesByPathAndHeadTmpVec.y * -segWidth; tsl.l.y1 = tsl.t.y2
            tsl.t.x1 = ts.x2 + getLinesByPathAndHeadTmpVec.x * segWidth; tsl.r.x2 = tsl.t.x1
            tsl.t.y1 = ts.y2 + getLinesByPathAndHeadTmpVec.y * segWidth; tsl.r.y2 = tsl.t.y1

            tsl.tp.x2 = ts.x2 + getLinesByPathAndHeadTmpVec.x * -(segWidth + segPad); tsl.lp.x1 = tsl.tp.x2
            tsl.tp.y2 = ts.y2 + getLinesByPathAndHeadTmpVec.y * -(segWidth + segPad); tsl.lp.y1 = tsl.tp.y2
            tsl.tp.x1 = ts.x2 + getLinesByPathAndHeadTmpVec.x * (segWidth + segPad); tsl.rp.x2 = tsl.tp.x1
            tsl.tp.y1 = ts.y2 + getLinesByPathAndHeadTmpVec.y * (segWidth + segPad); tsl.rp.y2 = tsl.tp.y1
        } else {
            getLinesByPathAndHeadTmpMat.idt()
            getLinesByPathAndHeadTmpMat.translate(ts.x2, ts.y2, 0f)
            getLinesByPathAndHeadTmpMat.rotate(0f, 0f, 1f, ts.w2)

            getLinesByPathAndHeadTmpVec.set(ts.l2, 0f, 0f)
            getLinesByPathAndHeadTmpVec.mul(getLinesByPathAndHeadTmpMat)
            tsl.t.x2 = getLinesByPathAndHeadTmpVec.x; tsl.l.x1 = tsl.t.x2
            tsl.t.y2 = getLinesByPathAndHeadTmpVec.y; tsl.l.y1 = tsl.t.y2
            getLinesByPathAndHeadTmpVec.set(ts.r2, 0f, 0f)
            getLinesByPathAndHeadTmpVec.mul(getLinesByPathAndHeadTmpMat)
            tsl.t.x1 = getLinesByPathAndHeadTmpVec.x; tsl.r.x2 = tsl.t.x1
            tsl.t.y1 = getLinesByPathAndHeadTmpVec.y; tsl.r.y2 = tsl.t.y1

            getLinesByPathAndHeadTmpVec.set(ts.x2 - ts.x1, ts.y2 - ts.y1, 0f)
            getLinesByPathAndHeadTmpVec.crs(0f, 0f, 1f)
            getLinesByPathAndHeadTmpVec.nor()
            getLinesByPathAndHeadTmpVec2.set(ts.l2, 0f, 0f)
            getLinesByPathAndHeadTmpVec2.mul(getLinesByPathAndHeadTmpMat)
            getLinesByPathAndHeadTmpVec3.set(getLinesByPathAndHeadTmpVec)
            getLinesByPathAndHeadTmpVec3.mul(-(segWidth + segPad))
            getLinesByPathAndHeadTmpVec3.add(ts.x2, ts.y2, 0f)
            doLineIntersect(
                ts.x2, ts.y2,
                getLinesByPathAndHeadTmpVec2.x,
                getLinesByPathAndHeadTmpVec2.y,
                getLinesByPathAndHeadTmpVec3.x,
                getLinesByPathAndHeadTmpVec3.y,
                getLinesByPathAndHeadTmpVec3.x + (ts.x2 - ts.x1),
                getLinesByPathAndHeadTmpVec3.y + (ts.y2 - ts.y1),
                getLinesByPathAndHeadTmpST
            )
            tsl.tp.x2 = ts.x2 + (getLinesByPathAndHeadTmpVec2.x - ts.x2) * getLinesByPathAndHeadTmpST.t
            tsl.lp.x1 = tsl.tp.x2
            tsl.tp.y2 = ts.y2 + (getLinesByPathAndHeadTmpVec2.y - ts.y2) * getLinesByPathAndHeadTmpST.t
            tsl.lp.y1 = tsl.tp.y2
            
            getLinesByPathAndHeadTmpVec3.set(getLinesByPathAndHeadTmpVec)
            getLinesByPathAndHeadTmpVec3.mul(segWidth + segPad)
            getLinesByPathAndHeadTmpVec3.add(ts.x2, ts.y2, 0f)
            doLineIntersect(
                ts.x2, ts.y2,
                getLinesByPathAndHeadTmpVec2.x,
                getLinesByPathAndHeadTmpVec2.y,
                getLinesByPathAndHeadTmpVec3.x,
                getLinesByPathAndHeadTmpVec3.y,
                getLinesByPathAndHeadTmpVec3.x + (ts.x2 - ts.x1),
                getLinesByPathAndHeadTmpVec3.y + (ts.y2 - ts.y1),
                getLinesByPathAndHeadTmpST
            )
            tsl.tp.x1 = ts.x2 + (getLinesByPathAndHeadTmpVec2.x - ts.x2) * getLinesByPathAndHeadTmpST.t
            tsl.rp.x2 = tsl.tp.x1
            tsl.tp.y1 = ts.y2 + (getLinesByPathAndHeadTmpVec2.y - ts.y2) * getLinesByPathAndHeadTmpST.t
            tsl.rp.y2 = tsl.tp.y1
        }

        return tsl
    }

    private class IsLineIntersectST {
        var t = 0f
        var s = 0f
    }

    companion object {
        private fun isLineIntersect(
            ax1: Float, ay1: Float, ax2: Float, ay2: Float,
            bx1: Float, by1: Float, bx2: Float, by2: Float
        ): Boolean {
            return doLineIntersect(ax1, ay1, ax2, ay2, bx1, by1, bx2, by2, null)
        }

        private const val doLineIntersectEpslion = 0.0001
        private fun doLineIntersect(
            ax1: Float, ay1: Float, ax2: Float, ay2: Float,
            bx1: Float, by1: Float, bx2: Float, by2: Float,
            st: IsLineIntersectST?
        ): Boolean {
            val axd = ax2 - ax1
            val ayd = ay2 - ay1
            val bxd = bx2 - bx1
            val byd = by2 - by1

            val d = bxd * ayd - axd * byd
            if (-doLineIntersectEpslion <= d && d <= doLineIntersectEpslion) {
                if (st != null) {
                    st.t = Float.NaN
                    st.s = Float.NaN
                }
                return false
            }

            val di = 1f / d
            val s = di * ((ax1 - bx1) * ayd - (ay1 - by1) * axd)
            val t = di * -(-(ax1 - bx1) * byd + (ay1 - by1) * bxd)

            if (st != null) {
                st.t = t
                st.s = s
            }
            return s in 0f..1f && t in 0f..1f
        }
    }
}

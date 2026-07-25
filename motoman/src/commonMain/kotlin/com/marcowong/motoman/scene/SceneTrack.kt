package com.marcowong.motoman.scene

import com.marcowong.motoman.assets.Assets
import com.marcowong.motoman.gl.Gl
import com.marcowong.motoman.gl.IMeshContext
import com.marcowong.motoman.gl.MeshOptimized
import com.marcowong.motoman.gl.ShaderProgram
import com.marcowong.motoman.gl.TextureWrap
import com.marcowong.motoman.gl.decodePixmap
import com.marcowong.motoman.model.ObjLoader
import com.marcowong.motoman.model.ObjLoaderSkeletonPatcher
import com.marcowong.motoman.model.TextureCache
import com.marcowong.motoman.render.TrackPortalFrustum
import com.marcowong.motoman.track.TrackData
import com.marcowong.motoman.track.TrackSegLine
import com.marcowong.motoman.track.TrackSegLines
import com.marcowong.motoman.track.TrackSegment
import com.marcowong.motoman.track.math.Frustum
import com.marcowong.motoman.track.math.Matrix4
import com.marcowong.motoman.track.math.Plane
import com.marcowong.motoman.track.math.Vector3
import com.marcowong.motoman.track.logic.Track as LogicTrack
import com.marcowong.motoman.track.logic.ITrackee
import kotlin.math.PI
import kotlin.math.atan2

class SceneTrack(
    assets: Assets,
    textures: TextureCache,
    private val gl: Gl,
    trackData: TrackData,
    private val decorationQuota: Int
) {
    val logic = LogicTrack(trackData)
    
    private val trackScaleMat = Matrix4()
    private val trackModelLen = 1f
    private val trackModelRot = -90f
    private val trackSegHeight = 5f
    private val trackScaleFactor = 40f
    
    private val lampScaleMat = Matrix4().scale(5f, 5f, 5f)
    private val buildingScaleMat = Matrix4().scale(39f, 39f, 39f)

    private val batch = MeshOptimized(gl)
    private val trackModelMeshContext: IMeshContext
    private val lampModelMeshContext: IMeshContext
    
    private val trackModelISkeMats: Array<Matrix4>
    private val trackModelISkeMatsFBuf: FloatArray
    private val trackTSLen: Float
    
    private val lampModelISkeMats: Array<Matrix4>
    private val lampModelISkeMatsFBuf: FloatArray

    init {
        trackScaleMat.scale(trackScaleFactor, trackSegHeight, trackScaleFactor)
        
        val objLoader = ObjLoader(assets)
        
        val trackModel = objLoader.loadObj("data/track.obj", true) ?: error("Failed to load track.obj")
        val trackModelSkeletonMapping = decodePixmap(assets.readBytes("data/track.skeleton.png"))
        ObjLoaderSkeletonPatcher().patch(trackModel, trackModelSkeletonMapping)
        trackModel.subMeshes.forEach { sub ->
            sub.material?.diffuseTextureName?.let { name ->
                val texture = textures.get(name)
                sub.material!!.diffuseTexture = texture
            }
        }
        trackModelMeshContext = batch.add(trackModel, 16)
        
        val lampModel = objLoader.loadObj("data/lamp.obj", true) ?: error("Failed to load lamp.obj")
        val lampModelSkeletonMapping = decodePixmap(assets.readBytes("data/lamp.skeleton.png"))
        ObjLoaderSkeletonPatcher().patch(lampModel, lampModelSkeletonMapping)
        lampModel.subMeshes.forEach { sub ->
            sub.material?.diffuseTextureName?.let { name ->
                val texture = textures.get(name)
                sub.material!!.diffuseTexture = texture
            }
        }
        lampModelMeshContext = batch.add(lampModel, 8)

        batch.optimize()
        
        trackModelISkeMats = Array(trackModelMeshContext.nCopies * 2) { Matrix4() }
        trackModelISkeMatsFBuf = FloatArray(trackModelMeshContext.nCopies * 2 * 16)
        trackTSLen = logic.trackTSLen
        
        lampModelISkeMats = Array(lampModelMeshContext.nCopies) { Matrix4() }
        lampModelISkeMatsFBuf = FloatArray(lampModelMeshContext.nCopies * 16)
    }
    
    fun resume() {
        // Track map recreation was here, but omitted for now as it's not supported by KMP Pixmap
    }
    
    fun getTrackeeTrackMapLoc(trackee: ITrackee, trackMapPos: Vector3) {
        // Omitted track map logic for now, trackMapPos remains unchanged
        trackMapPos.set(0f, 0f, 0f)
    }
    
    private val tmpVec = Vector3()
    private val tmpMat = Matrix4()
    
    private fun renderTS(
        shader: ShaderProgram, camera: MotomanCamera, ts: TrackSegment, decorate: Boolean,
        trackSkeMatsOffset: Int, lampSkeMatsOffset: Int
    ) {
        val ox = (ts.x1 + ts.x2) * 0.5f
        val oy = (ts.y1 + ts.y2) * 0.5f
        tmpVec.set(ts.y2 - ts.y1, ts.x2 - ts.x1, 0f).nor()
        val or = (atan2(tmpVec.y.toDouble(), tmpVec.x.toDouble()) * (180.0 / PI)).toFloat() + 90f + trackModelRot
        val p1Len = tmpVec.set(ox - ts.x1, oy - ts.y1, 0f).len()
        val p2Len = tmpVec.set(ox - ts.x2, oy - ts.y2, 0f).len()
        
        tmpMat.set(trackScaleMat)
        tmpMat.translate(ox, 0f, oy)
        tmpMat.rotate(0f, 1f, 0f, or)
        
        val trackSkeMat0 = trackModelISkeMats[trackSkeMatsOffset]
        val trackSkeMat1 = trackModelISkeMats[trackSkeMatsOffset + 1]
        
        trackSkeMat0.set(tmpMat)
        trackSkeMat0.translate(0f, 0f, -p1Len)
        trackSkeMat0.rotate(0f, 1f, 0f, -(or + ts.w1))
        trackSkeMat0.scale((-ts.l1 + ts.r1) * 0.5f, 1f, 1f)
        trackSkeMat0.translate(0f, 0f, trackModelLen)
        
        trackSkeMat1.set(tmpMat)
        trackSkeMat1.translate(0f, 0f, p2Len)
        trackSkeMat1.rotate(0f, 1f, 0f, -(or + ts.w2))
        trackSkeMat1.scale((-ts.l2 + ts.r2) * 0.5f, 1f, 1f)
        trackSkeMat1.translate(0f, 0f, -trackModelLen)
        
        if (decorate) {
            tmpVec.set(0f, 0f, 0f)
            tmpMat.idt()
            tmpMat.mul(trackScaleMat)
            tmpMat.translate(ox, 0f, oy)
            tmpMat.rotate(0f, 1f, 0f, or)
            tmpMat.translate(1.2f, 0f, 0f)
            tmpVec.mul(tmpMat)
            
            val lampSkeMat0 = lampModelISkeMats[lampSkeMatsOffset]
            lampSkeMat0.idt()
            lampSkeMat0.trn(tmpVec)
            lampSkeMat0.rotate(0f, 1f, 0f, or + 180f)
            lampSkeMat0.mul(lampScaleMat)
            
            tmpVec.set(0f, 0f, 0f)
            tmpMat.idt()
            tmpMat.mul(trackScaleMat)
            tmpMat.translate(ox, 0f, oy)
            tmpMat.rotate(0f, 1f, 0f, or)
            tmpMat.translate(-1.2f, 0f, 0f)
            tmpVec.mul(tmpMat)
            
            val lampSkeMat1 = lampModelISkeMats[lampSkeMatsOffset + 1]
            lampSkeMat1.idt()
            lampSkeMat1.trn(tmpVec)
            lampSkeMat1.rotate(0f, 1f, 0f, or)
            lampSkeMat1.mul(lampScaleMat)
        }
    }

    private val tmpBoxPoints = Array(8) { Vector3() }
    private fun isTrackSegmentInsideCamera(ts: TrackSegment, f: Frustum): Boolean {
        val tsl = ts.attributes["linesS"] as TrackSegLines
        tmpBoxPoints[0].set(tsl.h.x1, 0f, tsl.h.y1)
        tmpBoxPoints[1].set(tsl.h.x2, 0f, tsl.h.y2)
        tmpBoxPoints[2].set(tsl.t.x1, 0f, tsl.t.y1)
        tmpBoxPoints[3].set(tsl.t.x2, 0f, tsl.t.y2)
        tmpBoxPoints[4].set(tsl.h.x1, trackSegHeight, tsl.h.y1)
        tmpBoxPoints[5].set(tsl.h.x2, trackSegHeight, tsl.h.y2)
        tmpBoxPoints[6].set(tsl.t.x1, trackSegHeight, tsl.t.y1)
        tmpBoxPoints[7].set(tsl.t.x2, trackSegHeight, tsl.t.y2)
        
        for (plane in f.planes) {
            var allOutside = true
            for (i in 0 until 8) {
                if (plane.testPoint(tmpBoxPoints[i]) != Plane.PlaneSide.Back) {
                    allOutside = false
                    break
                }
            }
            if (allOutside) return false
        }
        return true
    }

    private val tmpPP = FloatArray(4 * 3)
    private fun constructPortalFrustum(c: MotomanCamera, f: TrackPortalFrustum, f2: Frustum, ts: TrackSegment, isNextTS: Boolean) {
        val tsl = ts.attributes["linesS"] as TrackSegLines
        val x1: Float; val y1: Float
        val x2: Float; val y2: Float
        if (isNextTS) {
            x1 = tsl.t.x1; y1 = tsl.t.y1
            x2 = tsl.t.x2; y2 = tsl.t.y2
        } else {
            x1 = tsl.h.x1; y1 = tsl.h.y1
            x2 = tsl.h.x2; y2 = tsl.h.y2
        }
        tmpPP[0] = x1; tmpPP[1] = 2f * trackSegHeight; tmpPP[2] = y1
        tmpPP[3] = x1; tmpPP[4] = -trackSegHeight; tmpPP[5] = y1
        tmpPP[6] = x2; tmpPP[7] = 2f * trackSegHeight; tmpPP[8] = y2
        tmpPP[9] = x2; tmpPP[10] = -trackSegHeight; tmpPP[11] = y2
        
        f.update(c, f2, tmpPP)
    }
    
    private fun renderTrackModelI(shader: ShaderProgram, camera: MotomanCamera, nInst: Int) {
        shader.setUniformMatrix("modelviewproj", camera.combined)
        shader.setUniformMatrix("modelview", camera.view)
        val nSkeMats = nInst * 2
        for (i in 0 until nSkeMats) {
            val mat = trackModelISkeMats[i].`val`
            for (j in 0..15) {
                trackModelISkeMatsFBuf[i * 16 + j] = mat[j]
            }
        }
        shader.setUniformMatrix4fv("skeletonmat", trackModelISkeMatsFBuf, false)
        trackModelMeshContext.render(shader, nInst)
    }

    private fun renderLampModelI(shader: ShaderProgram, camera: MotomanCamera, nInst: Int) {
        shader.setUniformMatrix("modelviewproj", camera.combined)
        shader.setUniformMatrix("modelview", camera.view)
        val nSkeMats = nInst
        for (i in 0 until nSkeMats) {
            val mat = lampModelISkeMats[i].`val`
            for (j in 0..15) {
                lampModelISkeMatsFBuf[i * 16 + j] = mat[j]
            }
        }
        shader.setUniformMatrix4fv("skeletonmat", lampModelISkeMatsFBuf, false)
        lampModelMeshContext.render(shader, nInst)
    }
    
    private var wasUseTmpTPF1 = false
    private val tmpTPF1 = TrackPortalFrustum()
    private val tmpTPF2 = TrackPortalFrustum()
    private fun getNextTmpTPF(): TrackPortalFrustum {
        return if (wasUseTmpTPF1) {
            wasUseTmpTPF1 = false
            tmpTPF2
        } else {
            wasUseTmpTPF1 = true
            tmpTPF1
        }
    }
    
    private val tmpVec8 = Vector3()
    private val tmpMat2 = Matrix4()
    
    fun updateVanishingPoint(camera: MotomanCamera) {
        val nearestTS = logic.getTrackeeTrackSegment(camera)
        var vanishingTS = nearestTS
        var ts: TrackSegment? = nearestTS
        var f = camera.frustum
        
        while (ts?.next != null) {
            ts = ts.next
            if (ts != null) {
                if (isTrackSegmentInsideCamera(ts, f)) {
                    vanishingTS = ts
                } else {
                    break
                }
                val tmpTPF = getNextTmpTPF()
                constructPortalFrustum(camera, tmpTPF, f, ts, true)
                f = tmpTPF
            }
        }
        
        if (vanishingTS === nearestTS) {
            tmpVec8.set(vanishingTS.x2, 0f, vanishingTS.y2).mul(trackScaleMat)
            tmpMat2.idt().trn(tmpVec8.x, 0f, tmpVec8.z).rotate(0f, 1f, 0f, vanishingTS.w2)
            tmpVec8.set(0f, 0f, trackTSLen * 0.5f).mul(tmpMat2)
        } else if (nearestTS.next != null && vanishingTS === nearestTS.next) {
            tmpVec8.set(vanishingTS.x2, 0f, vanishingTS.y2).mul(trackScaleMat)
        } else {
            tmpVec8.set(vanishingTS.x1, 0f, vanishingTS.y1).mul(trackScaleMat)
        }
        tmpVec8.y = trackSegHeight
        camera.updateVanishingPoint(tmpVec8)
    }
    
    fun render(shader: ShaderProgram, camera: MotomanCamera) {
        var nTrackInst = 0
        var nLampInst = 0
        val nearestTS = logic.getTrackeeTrackSegment(camera)
        val decorateNearestTS = this.decorationQuota > 0
        renderTS(shader, camera, nearestTS, decorateNearestTS, 0, 0)
        
        if (++nTrackInst >= trackModelMeshContext.nCopies) {
            renderTrackModelI(shader, camera, nTrackInst)
            nTrackInst = 0
        }
        if (decorateNearestTS) {
            nLampInst += 2
            if (nLampInst >= lampModelMeshContext.nCopies) {
                renderLampModelI(shader, camera, nLampInst)
                nLampInst = 0
            }
        }
        
        var ts: TrackSegment? = nearestTS
        var f = camera.frustum
        var decorationQuta = this.decorationQuota - 1
        var isThisWay = false
        var seeEnd = (nearestTS === logic.tsEnd)
        
        while (ts?.next != null) {
            ts = ts.next
            if (ts != null) {
                if (isTrackSegmentInsideCamera(ts, f)) {
                    if (ts === logic.tsEnd) seeEnd = true
                } else {
                    if (!isThisWay) break
                    if (decorationQuta <= 0) break
                }
                val tmpTPF = getNextTmpTPF()
                constructPortalFrustum(camera, tmpTPF, f, ts, true)
                f = tmpTPF
                isThisWay = true
                val decorateThisTS = --decorationQuta >= 0
                renderTS(shader, camera, ts, decorateThisTS, nTrackInst * 2, nLampInst)
                
                if (++nTrackInst >= trackModelMeshContext.nCopies) {
                    renderTrackModelI(shader, camera, nTrackInst)
                    nTrackInst = 0
                }
                if (decorateThisTS) {
                    nLampInst += 2
                    if (nLampInst >= lampModelMeshContext.nCopies) {
                        renderLampModelI(shader, camera, nLampInst)
                        nLampInst = 0
                    }
                }
            }
        }
        
        if (seeEnd) {
            for (i in logic.trackSegmentsOfEnd.indices) {
                renderTS(shader, camera, logic.trackSegmentsOfEnd[i], false, nTrackInst * 2, nLampInst)
                if (++nTrackInst >= trackModelMeshContext.nCopies) {
                    renderTrackModelI(shader, camera, nTrackInst)
                    nTrackInst = 0
                }
            }
        }
        
        ts = nearestTS
        f = camera.frustum
        decorationQuta = this.decorationQuota - 1
        isThisWay = false
        var seeStart = (nearestTS === logic.tsStart)
        
        while (ts?.prev != null) {
            ts = ts.prev
            if (ts != null) {
                if (isTrackSegmentInsideCamera(ts, f)) {
                    if (ts === logic.tsStart) seeStart = true
                } else {
                    if (!isThisWay) break
                    if (decorationQuta <= 0) break
                }
                val tmpTPF = getNextTmpTPF()
                constructPortalFrustum(camera, tmpTPF, f, ts, false)
                f = tmpTPF
                isThisWay = true
                val decorateThisTS = --decorationQuta >= 0
                renderTS(shader, camera, ts, decorateThisTS, nTrackInst * 2, nLampInst)
                
                if (++nTrackInst >= trackModelMeshContext.nCopies) {
                    renderTrackModelI(shader, camera, nTrackInst)
                    nTrackInst = 0
                }
                if (decorateThisTS) {
                    nLampInst += 2
                    if (nLampInst >= lampModelMeshContext.nCopies) {
                        renderLampModelI(shader, camera, nLampInst)
                        nLampInst = 0
                    }
                }
            }
        }
        
        if (seeStart) {
            for (i in logic.trackSegmentsOfStart.indices) {
                renderTS(shader, camera, logic.trackSegmentsOfStart[i], false, nTrackInst * 2, nLampInst)
                if (++nTrackInst >= trackModelMeshContext.nCopies) {
                    renderTrackModelI(shader, camera, nTrackInst)
                    nTrackInst = 0
                }
            }
        }
        
        if (nTrackInst != 0) renderTrackModelI(shader, camera, nTrackInst)
        if (nLampInst != 0) renderLampModelI(shader, camera, nLampInst)
    }
    
    fun dispose() {
        batch.dispose()
    }
}

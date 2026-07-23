package com.marcowong.motoman.scene

import com.marcowong.motoman.assets.Assets
import com.marcowong.motoman.gl.Gl
import com.marcowong.motoman.gl.IMeshContext
import com.marcowong.motoman.gl.MeshOptimized
import com.marcowong.motoman.gl.ShaderProgram
import com.marcowong.motoman.gl.TextureWrap
import com.marcowong.motoman.model.ObjLoader
import com.marcowong.motoman.model.ObjLoaderSkeletonPatcher
import com.marcowong.motoman.model.TextureCache
import com.marcowong.motoman.gl.decodePixmap
import com.marcowong.motoman.track.logic.Rider as LogicRider
import com.marcowong.motoman.track.math.Matrix4
import com.marcowong.motoman.track.math.Vector3
import com.marcowong.motoman.track.TrackData
import com.marcowong.motoman.track.logic.Track as LogicTrack

class Rider(
    assets: Assets,
    textures: TextureCache,
    batch: MeshOptimized,
    logicTrack: LogicTrack
) {
    val logic = LogicRider(logicTrack)
    var motorcycle: Motorcycle? = null

    private val skeHead = 0
    private val skeChest = 1
    private val skeWaist = 2
    private val skeHip = 3
    private val skeArmUpperL = 4
    private val skeArmLowerL = 5
    private val skeArmUpperR = 6
    private val skeArmLowerR = 7
    private val skeLegUpperL = 8
    private val skeLegLowerL = 9
    private val skeLegUpperR = 10
    private val skeLegLowerR = 11

    private val posHip = Vector3(0f, 0f, 0f)
    private val posWaist = Vector3(0f, 1.05f, 0.78f)
    private val posChest = Vector3(0f, 1.68f, 1.49f)
    private val posHead = Vector3(0f, 2.45f, 2.41f)
    private val posArmRoot = Vector3(1f, 2.1f, 2.03f)
    private val posArmUpper = Vector3(1.19f, 0.68f, 3.24f)
    private val posLegRoot = Vector3(0.5f, -0.1f, -0.14f)
    private val posLegUpper = Vector3(0.75f, -1.63f, 2.27f)

    private val offHip = Vector3(posHip.x, posHip.y, posHip.z)
    private val offWaist = Vector3(posWaist.x, posWaist.y, posWaist.z).sub(posHip)
    private val offChest = Vector3(posChest.x, posChest.y, posChest.z).sub(posWaist)
    private val offHead = Vector3(posHead.x, posHead.y, posHead.z).sub(posChest)
    private val offArmUpperR = Vector3(-posArmRoot.x, posArmRoot.y, posArmRoot.z).sub(posChest)
    private val offArmLowerR = Vector3(-posArmUpper.x, posArmUpper.y, posArmUpper.z).sub(-posArmRoot.x, posArmRoot.y, posArmRoot.z)
    private val offArmUpperL = Vector3(posArmRoot.x, posArmRoot.y, posArmRoot.z).sub(posChest)
    private val offArmLowerL = Vector3(posArmUpper.x, posArmUpper.y, posArmUpper.z).sub(posArmRoot)
    private val offLegUpperR = Vector3(-posLegRoot.x, posLegRoot.y, posLegRoot.z).sub(posHip)
    private val offLegLowerR = Vector3(-posLegUpper.x, posLegUpper.y, posLegUpper.z).sub(-posLegRoot.x, posLegRoot.y, posLegRoot.z)
    private val offLegUpperL = Vector3(posLegRoot.x, posLegRoot.y, posLegRoot.z).sub(posHip)
    private val offLegLowerL = Vector3(posLegUpper.x, posLegUpper.y, posLegUpper.z).sub(posLegRoot)

    private val dynHip = Matrix4()
    private val dynWaist = Matrix4()
    private val dynChest = Matrix4()
    private val dynHead = Matrix4()
    private val dynArmUpperL = Matrix4()
    private val dynArmLowerL = Matrix4()
    private val dynArmUpperR = Matrix4()
    private val dynArmLowerR = Matrix4()
    private val dynLegUpperL = Matrix4()
    private val dynLegLowerL = Matrix4()
    private val dynLegUpperR = Matrix4()
    private val dynLegLowerR = Matrix4()

    private val modelSkeMatsFBuf = FloatArray(12 * 16)
    
    private val modelMeshContext: IMeshContext
    private val pos = Matrix4().translate(0f, 0.35f, -0.05f)
    private val scaleMat = Matrix4().scale(0.55f, 0.55f, 0.55f)
    private val detachedHeight = 0.25f

    init {
        val objLoader = ObjLoader(assets)
        val model = objLoader.loadObj("data/rider.obj", true) ?: error("Failed to load rider.obj")
        val riderSkeletonMapping = decodePixmap(assets.readBytes("data/rider.skeleton.png"))
        ObjLoaderSkeletonPatcher().patch(model, riderSkeletonMapping)
        
        model.subMeshes.forEach { sub ->
            sub.material?.diffuseTextureName?.let { name ->
                val texture = textures.get(name)
                // Filter setup from StaticModelTextureFilterConfigManager equivalent
                texture.setWrap(TextureWrap.ClampToEdge, TextureWrap.ClampToEdge)
                sub.material!!.diffuseTexture = texture
            }
        }
        
        modelMeshContext = batch.add(model)
    }

    private fun copyMat(src: Matrix4, dst: Matrix4) {
        System.arraycopy(src.`val`, 0, dst.`val`, 0, 16)
    }

    private val tmpMatP1 = Matrix4()
    private val tmpMatP2 = Matrix4()
    private val tmpMatP3 = Matrix4()
    private val tmpMatP4 = Matrix4()
    private val tmpMatP5 = Matrix4()
    private val tmpMatP6 = Matrix4()
    private val tmpMatP7 = Matrix4()
    private val tmpMatP8 = Matrix4()
    private val tmpMatP9 = Matrix4()
    private val tmpMatP10 = Matrix4()
    private val tmpMatP11 = Matrix4()
    private val tmpMatP12 = Matrix4()

    private val tmpMat = Matrix4()
    private val tmpMat2 = Matrix4()

    private fun putMatToBuf(index: Int, mat: Matrix4) {
        System.arraycopy(mat.`val`, 0, modelSkeMatsFBuf, index * 16, 16)
    }

    fun render(shader: ShaderProgram, camera: MotomanCamera) {
        val pose = logic.state.pose
        copyMat(pose.matHip, tmpMatP1)
        copyMat(pose.matWaist, tmpMatP2)
        copyMat(pose.matChest, tmpMatP3)
        copyMat(pose.matHead, tmpMatP4)
        copyMat(pose.matLegUpperR, tmpMatP5)
        copyMat(pose.matLegLowerR, tmpMatP6)
        copyMat(pose.matLegUpperL, tmpMatP7)
        copyMat(pose.matLegLowerL, tmpMatP8)
        copyMat(pose.matArmUpperR, tmpMatP9)
        copyMat(pose.matArmLowerR, tmpMatP10)
        copyMat(pose.matArmUpperL, tmpMatP11)
        copyMat(pose.matArmLowerL, tmpMatP12)

        dynHip.set(tmpMatP1).trn(offHip)
        tmpMat.idt().trn(-posHip.x, -posHip.y, -posHip.z).mul(dynHip)
        putMatToBuf(skeHip, tmpMat)

        tmpMat.set(tmpMatP2).trn(offWaist)
        dynWaist.set(dynHip).mul(tmpMat)
        tmpMat.set(dynWaist).translate(-posWaist.x, -posWaist.y, -posWaist.z)
        putMatToBuf(skeWaist, tmpMat)

        tmpMat.set(tmpMatP3).trn(offChest)
        dynChest.set(dynWaist).mul(tmpMat)
        tmpMat.set(dynChest).translate(-posChest.x, -posChest.y, -posChest.z)
        putMatToBuf(skeChest, tmpMat)

        tmpMat.set(tmpMatP4).trn(offHead)
        dynHead.set(dynChest).mul(tmpMat)
        tmpMat.set(dynHead).translate(-posHead.x, -posHead.y, -posHead.z)
        putMatToBuf(skeHead, tmpMat)

        tmpMat.set(tmpMatP5).trn(offLegUpperR)
        dynLegUpperR.set(dynHip).mul(tmpMat)
        tmpMat.set(dynLegUpperR).translate(posLegRoot.x, -posLegRoot.y, -posLegRoot.z)
        putMatToBuf(skeLegUpperR, tmpMat)

        tmpMat.set(tmpMatP6).trn(offLegLowerR)
        dynLegLowerR.set(dynLegUpperR).mul(tmpMat)
        tmpMat.set(dynLegLowerR).translate(posLegUpper.x, -posLegUpper.y, -posLegUpper.z)
        putMatToBuf(skeLegLowerR, tmpMat)

        tmpMat.set(tmpMatP7).trn(offLegUpperL)
        dynLegUpperL.set(dynHip).mul(tmpMat)
        tmpMat.set(dynLegUpperL).translate(-posLegRoot.x, -posLegRoot.y, -posLegRoot.z)
        putMatToBuf(skeLegUpperL, tmpMat)

        tmpMat.set(tmpMatP8).trn(offLegLowerL)
        dynLegLowerL.set(dynLegUpperL).mul(tmpMat)
        tmpMat.set(dynLegLowerL).translate(-posLegUpper.x, -posLegUpper.y, -posLegUpper.z)
        putMatToBuf(skeLegLowerL, tmpMat)

        tmpMat.set(tmpMatP9).trn(offArmUpperR)
        dynArmUpperR.set(dynChest).mul(tmpMat)
        tmpMat.set(dynArmUpperR).translate(posArmRoot.x, -posArmRoot.y, -posArmRoot.z)
        putMatToBuf(skeArmUpperR, tmpMat)

        tmpMat.set(tmpMatP10).trn(offArmLowerR)
        dynArmLowerR.set(dynArmUpperR).mul(tmpMat)
        tmpMat.set(dynArmLowerR).translate(posArmUpper.x, -posArmUpper.y, -posArmUpper.z)
        putMatToBuf(skeArmLowerR, tmpMat)

        tmpMat.set(tmpMatP11).trn(offArmUpperL)
        dynArmUpperL.set(dynChest).mul(tmpMat)
        tmpMat.set(dynArmUpperL).translate(-posArmRoot.x, -posArmRoot.y, -posArmRoot.z)
        putMatToBuf(skeArmUpperL, tmpMat)

        tmpMat.set(tmpMatP12).trn(offArmLowerL)
        dynArmLowerL.set(dynArmUpperL).mul(tmpMat)
        tmpMat.set(dynArmLowerL).translate(-posArmUpper.x, -posArmUpper.y, -posArmUpper.z)
        putMatToBuf(skeArmLowerL, tmpMat)

        shader.setUniformMatrix4fv("skeletonmat", modelSkeMatsFBuf, false)

        val mot = motorcycle
        if (logic.state.attached && mot != null) {
            val motPos = Matrix4()
            copyMat(mot.logic.state.pos, motPos)
            val motLean = Matrix4()
            copyMat(mot.logic.state.lean, motLean)

            tmpMat2.set(motPos)
            tmpMat2.translate(0f, mot.getLeanHeightShift(), 0f)
            tmpMat2.mul(motLean)
            tmpMat2.mul(mot.ridePos)
            tmpMat2.mul(pos)
            tmpMat2.mul(scaleMat)

            tmpMat.set(camera.combined)
            tmpMat.mul(tmpMat2)
            shader.setUniformMatrix("modelviewproj", tmpMat)
            tmpMat.set(camera.view)
            tmpMat.mul(tmpMat2)
            shader.setUniformMatrix("modelview", tmpMat)
        } else {
            val detPos = Matrix4()
            copyMat(logic.state.detachedPos, detPos)
            tmpMat.set(camera.combined)
            tmpMat.mul(detPos)
            tmpMat.mul(scaleMat)
            shader.setUniformMatrix("modelviewproj", tmpMat)
            tmpMat.set(camera.view)
            tmpMat.mul(detPos)
            tmpMat.mul(scaleMat)
            shader.setUniformMatrix("modelview", tmpMat)
        }
        modelMeshContext.render(shader)
    }

    fun setPersist(b: Boolean) {
        logic.setPersist(b)
    }

    fun update(delta: Float) {
        logic.update(delta)
    }

    fun isKneeDragging(): Boolean {
        return logic.isKneeDragging()
    }

    fun attach() {
        logic.attach()
    }

    fun detach() {
        logic.detach()
    }

    var strength: Float
        get() = logic.state.strength
        set(value) {
            logic.state.strength = value
        }

    fun dispose() {
    }
}

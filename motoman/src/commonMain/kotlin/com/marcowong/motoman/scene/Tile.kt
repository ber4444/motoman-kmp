package com.marcowong.motoman.scene

import com.marcowong.motoman.assets.Assets
import com.marcowong.motoman.gl.IMeshContext
import com.marcowong.motoman.gl.MeshOptimized
import com.marcowong.motoman.gl.ShaderProgram
import com.marcowong.motoman.gl.TextureWrap
import com.marcowong.motoman.gl.decodePixmap
import com.marcowong.motoman.model.ObjLoader
import com.marcowong.motoman.model.ObjLoaderSkeletonPatcher
import com.marcowong.motoman.model.TextureCache
import com.marcowong.motoman.track.math.Matrix4
import com.marcowong.motoman.track.math.PerspectiveCamera
import kotlin.math.roundToInt

class Tile(
    assets: Assets,
    textures: TextureCache,
    batch: MeshOptimized
) {
    private val copies = 12
    private val modelMeshContext: IMeshContext
    private val tileLen = 1000f
    private val height = -1f
    private val duplicate = 1

    private val modelISkeMats = Array(copies) { Matrix4() }
    private val modelISkeMatsFBuf = FloatArray(copies * 16)

    init {
        val objLoader = ObjLoader(assets)
        val model = objLoader.loadObj("data/tile.obj", true) ?: error("Failed to load tile.obj")

        val skeletonBytes = assets.readBytes("data/tile.skeleton.png")
        val skeletonMapping = decodePixmap(skeletonBytes)
        ObjLoaderSkeletonPatcher().patch(model, skeletonMapping)
        
        for (subMesh in model.subMeshes) {
            val material = subMesh.material ?: continue
            if (material.diffuseTextureName != null) {
                val texture = textures.get(material.diffuseTextureName!!)
                texture.setWrap(TextureWrap.Repeat, TextureWrap.Repeat)
                material.diffuseTexture = texture
            }
        }

        modelMeshContext = batch.add(model, copies)
    }

    private fun renderModelI(shader: ShaderProgram, camera: PerspectiveCamera, nInst: Int) {
        shader.setUniformMatrix("modelviewproj", camera.combined)
        shader.setUniformMatrix("modelview", camera.view)

        for (i in 0 until nInst) {
            modelISkeMats[i].`val`.copyInto(modelISkeMatsFBuf, i * 16)
        }
        val matsToSend = modelISkeMatsFBuf.copyOf(nInst * 16)
        shader.setUniformMatrix4fv("skeletonmat", matsToSend, false)
        modelMeshContext.render(shader, nInst)
    }

    fun render(shader: ShaderProgram, camera: PerspectiveCamera) {
        val startX = (camera.position.x / tileLen).roundToInt()
        val startZ = (camera.position.z / tileLen).roundToInt()

        var nInst = 0
        for (x in startX - duplicate..startX + duplicate) {
            for (z in startZ - duplicate..startZ + duplicate) {
                val skeMat = modelISkeMats[nInst]
                skeMat.idt()
                skeMat.trn(x * tileLen, height, z * tileLen)
                nInst++
                if (nInst >= copies) {
                    renderModelI(shader, camera, nInst)
                    nInst = 0
                }
            }
        }
        if (nInst != 0) {
            renderModelI(shader, camera, nInst)
        }
    }

    fun dispose() {
    }
}

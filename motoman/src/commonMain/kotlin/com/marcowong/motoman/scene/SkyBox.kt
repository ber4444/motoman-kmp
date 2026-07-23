package com.marcowong.motoman.scene

import com.marcowong.motoman.assets.Assets
import com.marcowong.motoman.gl.Gl
import com.marcowong.motoman.gl.IMeshContext
import com.marcowong.motoman.gl.MeshOptimized
import com.marcowong.motoman.gl.ShaderProgram
import com.marcowong.motoman.gl.Texture
import com.marcowong.motoman.gl.TextureFilter
import com.marcowong.motoman.gl.TextureWrap
import com.marcowong.motoman.gl.decodePixmap
import com.marcowong.motoman.model.ModelData
import com.marcowong.motoman.model.ObjLoader
import com.marcowong.motoman.model.TextureCache
import com.marcowong.motoman.track.math.Color
import com.marcowong.motoman.track.math.Matrix4
import com.marcowong.motoman.track.math.PerspectiveCamera

class SkyBox(
    private val gl: Gl,
    private val assets: Assets,
    textures: TextureCache,
    batch: MeshOptimized,
    preBloomedBatch: MeshOptimized
) {
    private val model: ModelData
    private val modelMeshContext: IMeshContext
    private val modelPreBloomed: ModelData
    private val modelPreBloomedMeshContext: IMeshContext
    
    val skyBloomColor = Color(0.3f, 0.3f, 1f, 1f)
    private val scaleMatrix = Matrix4().scale(1f, 1f, 1f)
    
    private val tmpMat = Matrix4()

    init {
        val objLoader = ObjLoader(assets)
        model = objLoader.loadObj("data/skybox.obj", true) ?: error("Failed to load skybox.obj")
        modelPreBloomed = objLoader.loadObj("data/skybox.obj", true) ?: error("Failed to load skybox.obj")

        for (sub in model.subMeshes) {
            val material = sub.material ?: continue
            val name = material.diffuseTextureName ?: continue
            val texture = textures.get(name)
            texture.setWrap(TextureWrap.Repeat, TextureWrap.Repeat)
            material.diffuseTexture = texture
        }
        
        preBloomModel()
        
        modelMeshContext = batch.add(model)
        modelPreBloomedMeshContext = preBloomedBatch.add(modelPreBloomed)
    }

    fun resume() {
        // Texture cache recreation is handled at the app level.
    }

    private fun preBloomModel() {
        for (k in modelPreBloomed.subMeshes.indices) {
            val bloomingSub = model.subMeshes[k]
            val bloomedSub = modelPreBloomed.subMeshes[k]
            
            if (bloomingSub.material != null && bloomingSub.material!!.diffuseTextureName != null &&
                bloomedSub.material != null && bloomedSub.material!!.diffuseTextureName != null) {
                
                val path = bloomingSub.material!!.diffuseTextureName!!
                val pixmap = decodePixmap(assets.readBytes(path))
                
                val w = pixmap.width
                val h = pixmap.height
                val c = pixmap.format.components
                
                for (y in 0 until h) {
                    for (x in 0 until w) {
                        val i = (y * w + x) * c
                        val r = (pixmap.pixels[i].toInt() and 0xFF) / 255f
                        val g = (pixmap.pixels[i + 1].toInt() and 0xFF) / 255f
                        val b = (pixmap.pixels[i + 2].toInt() and 0xFF) / 255f
                        
                        var newR = r + skyBloomColor.r; if (newR > 1f) newR = 1f
                        var newG = g + skyBloomColor.g; if (newG > 1f) newG = 1f
                        var newB = b + skyBloomColor.b; if (newB > 1f) newB = 1f
                        
                        pixmap.pixels[i] = (newR * 255f).toInt().toByte()
                        pixmap.pixels[i + 1] = (newG * 255f).toInt().toByte()
                        pixmap.pixels[i + 2] = (newB * 255f).toInt().toByte()
                    }
                }
                
                val newTexture = Texture(gl, pixmap, minFilter = TextureFilter.Linear, magFilter = TextureFilter.Linear)
                newTexture.setWrap(TextureWrap.Repeat, TextureWrap.Repeat)
                bloomedSub.material!!.diffuseTexture = newTexture
            }
        }
    }

    fun render(shader: ShaderProgram, camera: PerspectiveCamera, bloomEnabled: Boolean) {
        tmpMat.set(camera.combined)
        tmpMat.translate(camera.position.x, camera.position.y, camera.position.z)
        tmpMat.mul(scaleMatrix)
        shader.setUniformMatrix("modelviewproj", tmpMat)
        
        tmpMat.set(camera.view)
        tmpMat.translate(camera.position.x, camera.position.y, camera.position.z)
        tmpMat.mul(scaleMatrix)
        shader.setUniformMatrix("modelview", tmpMat)
        
        if (bloomEnabled) {
            modelMeshContext.render(shader)
        } else {
            modelPreBloomedMeshContext.render(shader)
        }
    }

    fun dispose() {
        for (sub in modelPreBloomed.subMeshes) {
            sub.material?.diffuseTexture?.dispose()
        }
    }
}

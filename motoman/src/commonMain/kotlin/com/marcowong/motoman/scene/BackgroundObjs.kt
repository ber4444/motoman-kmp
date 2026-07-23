package com.marcowong.motoman.scene

import com.marcowong.motoman.assets.Assets
import com.marcowong.motoman.gl.IMeshContext
import com.marcowong.motoman.gl.MeshOptimized
import com.marcowong.motoman.gl.ShaderProgram
import com.marcowong.motoman.gl.TextureWrap
import com.marcowong.motoman.model.ObjLoader
import com.marcowong.motoman.model.TextureCache
import com.marcowong.motoman.track.math.Matrix4
import com.marcowong.motoman.track.math.PerspectiveCamera
import kotlin.random.Random

class BackgroundObjs(
    assets: Assets,
    textures: TextureCache,
    batch: MeshOptimized
) {
    private val hillsModelMeshContext: IMeshContext
    private val buildingsModelMeshContext: IMeshContext
    private val airplaneModelMeshContext: IMeshContext

    private val hillsScale = Matrix4().scale(5f, 2f, 5f)
    private val buildingsScale = Matrix4().scale(3.5f, 1f, 3.5f)
    private val airplaneScale = Matrix4().scale(0.4f, 0.4f, 0.4f)

    class UpdateState {
        val airplanePos = Matrix4()
        var airplaneShowing = false
        var airplaneTotalTrans = 0f
        var airplaneRestRemains = 0f

        fun copyTo(s: UpdateState) {
            s.airplanePos.set(airplanePos)
            s.airplaneShowing = airplaneShowing
            s.airplaneTotalTrans = airplaneTotalTrans
            s.airplaneRestRemains = airplaneRestRemains
        }
    }

    val statePersist = UpdateState()
    val stateTmp = UpdateState()
    var state = statePersist

    private val tmpMat = Matrix4()

    init {
        val objLoader = ObjLoader(assets)
        
        val hillsModel = objLoader.loadObj("data/hills.obj", true) ?: error("Failed to load hills.obj")
        hillsModel.subMeshes.forEach { sub ->
            sub.material?.diffuseTextureName?.let { name ->
                val texture = textures.get(name)
                texture.setWrap(TextureWrap.Repeat, TextureWrap.Repeat)
                sub.material!!.diffuseTexture = texture
            }
        }
        hillsModelMeshContext = batch.add(hillsModel)

        val buildingsModel = objLoader.loadObj("data/buildings.obj", true) ?: error("Failed to load buildings.obj")
        buildingsModel.subMeshes.forEach { sub ->
            sub.material?.diffuseTextureName?.let { name ->
                val texture = textures.get(name)
                texture.setWrap(TextureWrap.Repeat, TextureWrap.Repeat)
                sub.material!!.diffuseTexture = texture
            }
        }
        buildingsModelMeshContext = batch.add(buildingsModel)

        val airplaneModel = objLoader.loadObj("data/airplane.obj", true) ?: error("Failed to load airplane.obj")
        airplaneModel.subMeshes.forEach { sub ->
            sub.material?.diffuseTextureName?.let { name ->
                val texture = textures.get(name)
                texture.setWrap(TextureWrap.Repeat, TextureWrap.Repeat)
                sub.material!!.diffuseTexture = texture
            }
        }
        airplaneModelMeshContext = batch.add(airplaneModel)

        state.airplaneShowing = false
        state.airplaneRestRemains = getAirplaneRestSpan()
    }

    fun render(shader: ShaderProgram, camera: PerspectiveCamera) {
        tmpMat.set(camera.combined)
        tmpMat.translate(camera.position.x, camera.position.y, camera.position.z)
        tmpMat.mul(hillsScale)
        shader.setUniformMatrix("modelviewproj", tmpMat)
        
        tmpMat.set(camera.view)
        tmpMat.translate(camera.position.x, camera.position.y, camera.position.z)
        tmpMat.mul(hillsScale)
        shader.setUniformMatrix("modelview", tmpMat)
        hillsModelMeshContext.render(shader)

        tmpMat.set(camera.combined)
        tmpMat.translate(camera.position.x, camera.position.y, camera.position.z)
        tmpMat.mul(buildingsScale)
        shader.setUniformMatrix("modelviewproj", tmpMat)
        
        tmpMat.set(camera.view)
        tmpMat.translate(camera.position.x, camera.position.y, camera.position.z)
        tmpMat.mul(buildingsScale)
        shader.setUniformMatrix("modelview", tmpMat)
        buildingsModelMeshContext.render(shader)

        if (state.airplaneShowing) {
            tmpMat.set(camera.combined)
            tmpMat.translate(camera.position.x, camera.position.y, camera.position.z)
            tmpMat.mul(state.airplanePos)
            tmpMat.mul(airplaneScale)
            shader.setUniformMatrix("modelviewproj", tmpMat)
            
            tmpMat.set(camera.view)
            tmpMat.translate(camera.position.x, camera.position.y, camera.position.z)
            tmpMat.mul(state.airplanePos)
            tmpMat.mul(airplaneScale)
            shader.setUniformMatrix("modelview", tmpMat)
            airplaneModelMeshContext.render(shader)
        }
    }

    private fun getAirplaneRestSpan(): Float {
        return (15f + Random.nextDouble() * 15f).toFloat()
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
        if (state.airplaneShowing) {
            val trans = delta * 3f
            state.airplanePos.translate(0f, 0f, trans)
            state.airplaneTotalTrans += trans
            if (state === statePersist && state.airplaneTotalTrans > 100f) {
                state.airplaneShowing = false
                state.airplaneRestRemains = getAirplaneRestSpan()
            }
        } else {
            if (state !== statePersist || state.airplaneRestRemains > 0f) {
                state.airplaneRestRemains -= delta
            } else {
                state.airplaneShowing = true
                state.airplaneTotalTrans = 0f
                state.airplanePos.idt()
                state.airplanePos.rotate(0f, 1f, 0f, (360.0 * Random.nextDouble()).toFloat())
                val sign = if (Random.nextBoolean()) 1f else -1f
                state.airplanePos.translate(
                    ((30f + Random.nextDouble() * 20f) * sign).toFloat(),
                    (10f + Random.nextDouble() * 4f).toFloat(),
                    -50f
                )
            }
        }
    }

    fun dispose() {
    }
}

package com.marcowong.motoman.scene

import com.marcowong.motoman.assets.Assets
import com.marcowong.motoman.gl.Gl
import com.marcowong.motoman.gl.IMeshContext
import com.marcowong.motoman.gl.MeshOptimized
import com.marcowong.motoman.gl.ShaderProgram
import com.marcowong.motoman.model.ObjLoader
import com.marcowong.motoman.model.TextureCache
import com.marcowong.motoman.track.math.Matrix4
import com.marcowong.motoman.track.math.Vector3
import com.marcowong.motoman.track.logic.Motorcycle as LogicMotorcycle
import com.marcowong.motoman.track.logic.IMotorcycleInputMeters
import com.marcowong.motoman.track.logic.Track as LogicTrack
import kotlin.math.abs

// Placeholder for Particle FX (to be implemented in PR 7)
class MotorcycleFX {
    fun init(camera: MotomanCamera) {}
    fun update(delta: Float) {}
    fun render() {}
    fun dispose() {}
}

// Placeholder for Audio SFX (to be implemented in PR 6)
class MotorcycleSFX {
    fun update(delta: Float) {}
    fun gamePause() {}
    fun gameResume() {}
    fun dispose() {}
}

open class Motorcycle(
    assets: Assets,
    textures: TextureCache,
    batch: MeshOptimized,
    logicTrack: LogicTrack,
    inputMeters: IMotorcycleInputMeters
) {
    val logic = LogicMotorcycle(logicTrack, inputMeters)
    
    val fx = MotorcycleFX()
    val sfx = MotorcycleSFX()
    
    var rider: Rider? = null
        set(value) {
            field = value
            logic.rider = value?.logic
        }
    
    val ridePos = Matrix4()
    
    val bodyPos = Matrix4()
    val frontWheelPos = Matrix4()
    val rearWheelPos = Matrix4()
    val backfirePos = Vector3()
    
    var leanAngleMaxWhenRunningRenderHeightShift = 0.07f
    var leanAngleMaxWhenCrashedRenderHeightShift = 0.7f
    
    fun getLeanHeightShift(): Float {
        return if (logic.state.isCrashed) {
            leanAngleMaxWhenCrashedRenderHeightShift
        } else {
            abs(logic.state.leanAngle) / logic.leanAngleMaxWhenRunning * leanAngleMaxWhenRunningRenderHeightShift
        }
    }
    
    protected var bodyModelMeshContext: IMeshContext? = null
    protected var frontWheelModelMeshContext: IMeshContext? = null
    protected var rearWheelModelMeshContext: IMeshContext? = null
    
    private val shadowModelMeshContext: IMeshContext
    
    init {
        val objLoader = ObjLoader(assets)
        val shadowModel = objLoader.loadObj("data/bikeShadow.obj", true) ?: error("Failed to load bikeShadow.obj")
        shadowModelMeshContext = batch.add(shadowModel)
    }
    
    private val tmpMat2 = Matrix4()
    private fun renderModel(shader: ShaderProgram, camera: MotomanCamera, meshContext: IMeshContext?, modelPos: Matrix4) {
        if (meshContext == null) return
        tmpMat2.set(camera.combined)
        tmpMat2.mul(modelPos)
        shader.setUniformMatrix("modelviewproj", tmpMat2)
        tmpMat2.set(camera.view)
        tmpMat2.mul(modelPos)
        shader.setUniformMatrix("modelview", tmpMat2)
        meshContext.render(shader)
    }
    
    private fun copyMat(src: Matrix4, dst: Matrix4) {
        System.arraycopy(src.`val`, 0, dst.`val`, 0, 16)
    }
    
    private val tmpVec4 = Vector3()
    private val tmpMat = Matrix4()
    private val tmpMat3 = Matrix4()
    private val statePos = Matrix4()
    private val stateLean = Matrix4()
    private val stateFrontWheelRot = Matrix4()
    private val stateRearWheelRot = Matrix4()
    
    open fun render(shader: ShaderProgram, camera: MotomanCamera) {
        copyMat(logic.state.pos, statePos)
        copyMat(logic.state.lean, stateLean)
        copyMat(logic.state.frontWheelRot, stateFrontWheelRot)
        copyMat(logic.state.rearWheelRot, stateRearWheelRot)
        
        tmpMat3.set(statePos)
        tmpMat3.translate(0f, this.getLeanHeightShift(), 0f)
        tmpMat3.mul(stateLean)
        
        tmpMat.set(tmpMat3)
        tmpMat.mul(bodyPos)
        renderModel(shader, camera, bodyModelMeshContext, tmpMat)
        
        tmpMat.set(tmpMat3)
        tmpMat.mul(frontWheelPos)
        tmpMat.mul(stateFrontWheelRot)
        renderModel(shader, camera, frontWheelModelMeshContext, tmpMat)
        
        tmpMat.set(tmpMat3)
        tmpMat.mul(rearWheelPos)
        tmpMat.mul(stateRearWheelRot)
        renderModel(shader, camera, rearWheelModelMeshContext, tmpMat)
        
        tmpVec4.set(0f, logic.massCenterHeight, 0f)
        tmpVec4.mul(stateLean)
        tmpMat.set(statePos)
        tmpMat.translate(tmpVec4.x, 0.01f, 0f)
        tmpMat.scale(1f + 0.5f * abs(tmpVec4.x), 1f, 1f)
        renderModel(shader, camera, shadowModelMeshContext, tmpMat)
    }
    
    fun update(delta: Float) {
        logic.update(delta)
        if (logic.state === logic.statePersist) {
            // fx and sfx will be updated here later
            fx.update(delta)
            sfx.update(delta)
        }
    }
    
    fun dispose() {
        fx.dispose()
        sfx.dispose()
    }
    
    fun setPersist(b: Boolean) {
        logic.setPersist(b)
    }
}

class MainMotorcycle(
    assets: Assets,
    textures: TextureCache,
    batch: MeshOptimized,
    logicTrack: LogicTrack,
    inputMeters: IMotorcycleInputMeters
) : Motorcycle(assets, textures, batch, logicTrack, inputMeters) {
    init {
        val objLoader = ObjLoader(assets)
        val mainBodyModel = objLoader.loadObj("data/bikeBody.obj", true) ?: error("Failed to load bikeBody.obj")
        bodyModelMeshContext = batch.add(mainBodyModel)
        
        val mainFrontWheelModel = objLoader.loadObj("data/bikeFrontWheel.obj", true) ?: error("Failed to load bikeFrontWheel.obj")
        frontWheelModelMeshContext = batch.add(mainFrontWheelModel)
        
        val mainRearWheelModel = objLoader.loadObj("data/bikeRearWheel.obj", true) ?: error("Failed to load bikeRearWheel.obj")
        rearWheelModelMeshContext = batch.add(mainRearWheelModel)
        
        val zAdjust = 0.95f
        bodyPos.translate(0f, zAdjust, 0f)
        frontWheelPos.translate(0f, zAdjust, 2.35f)
        rearWheelPos.translate(0f, zAdjust + 0.05f, -2.4f)
        ridePos.translate(0f, zAdjust + 1.6f, -1f)
        backfirePos.set(0f, zAdjust + 2.14038f, -2.74571f)
        logic.massCenterHeight = zAdjust + 1.75f // height of oil tank
        logic.leanAngleMaxWhenRunning = 55f
        leanAngleMaxWhenRunningRenderHeightShift = 0.07f
        leanAngleMaxWhenCrashedRenderHeightShift = 0.7f
        logic.leanAngleSafe = 30f
    }
}

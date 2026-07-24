package com.marcowong.motoman.scene

import com.marcowong.motoman.assets.Assets
import com.marcowong.motoman.gl.Gl
import com.marcowong.motoman.gl.IMeshContext
import com.marcowong.motoman.gl.MeshOptimized
import com.marcowong.motoman.gl.ShaderProgram
import com.marcowong.motoman.model.ModelData
import com.marcowong.motoman.model.ObjLoader
import com.marcowong.motoman.model.TextureCache
import com.marcowong.motoman.track.math.Matrix4
import com.marcowong.motoman.track.math.Vector3
import com.marcowong.motoman.track.logic.Motorcycle as LogicMotorcycle
import com.marcowong.motoman.track.logic.IMotorcycleInputMeters
import com.marcowong.motoman.track.logic.Track as LogicTrack
import kotlin.math.abs

import com.marcowong.motoman.audio.Audio
import com.marcowong.motoman.audio.Haptics
import com.marcowong.motoman.audio.MotorcycleSFX

/**
 * Attaches each sub-mesh's diffuse texture from its material name.
 *
 * The port's `ObjLoader` runs in commonMain and cannot create GL textures, so — unlike the
 * original engine, whose `loadObj(..., true)` resolved them inline — every model must have its
 * textures resolved after loading. Every other scene object (Tile, SceneTrack, BackgroundObjs,
 * Rider, SkyBox) does this; the motorcycle did not, so its shadow quad had no texture and
 * sampled whatever was last bound (the bike's own texture), drawing a coloured pad instead of
 * a soft shadow.
 */
private fun resolveModelTextures(model: ModelData, textures: TextureCache) {
    for (sub in model.subMeshes) {
        val name = sub.material?.diffuseTextureName ?: continue
        sub.material!!.diffuseTexture = textures.get(name)
    }
}

open class Motorcycle(
    val gl: Gl,
    val glslTarget: com.marcowong.motoman.gl.GlslTarget,
    val audio: Audio,
    val haptics: Haptics,
    assets: Assets,
    textures: TextureCache,
    batch: MeshOptimized,
    logicTrack: LogicTrack,
    inputMeters: IMotorcycleInputMeters
) {
    val logic = LogicMotorcycle(logicTrack, inputMeters)
    
    val fx = MotorcycleFX(gl, assets, textures, glslTarget)
    
    private var lastBackfireSize = 0f
    private val backfireReporter = object : MotorcycleSFX.BackfireReporter {
        override fun reportBackfire(size: Float) {
            lastBackfireSize = size
        }
    }
    
    val sfx = MotorcycleSFX(this, backfireReporter, audio, haptics)
    
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
        resolveModelTextures(shadowModel, textures)
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
        src.`val`.copyInto(dst.`val`, 0, 0, 16)
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
    
    private val tmpMat5 = Matrix4()
    private val tmpVec3 = Vector3()
    private val backfireFXPosition = object : com.marcowong.motoman.scene.DynamicFXPosition {
        override fun getPosition(vec: Vector3) {
            vec.set(backfirePos)
            vec.mul(logic.state.lean)
            vec.mul(logic.state.pos)
        }
    }

    private fun updateFX(delta: Float) {
        val speedRatio = kotlin.math.min(1f, logic.state.bikeVelo.len2())
        if (logic.state.frontTraction < 1f) {
            tmpMat5.set(logic.state.pos)
            tmpVec3.set(0f, 0f, 0f)
            tmpVec3.mul(frontWheelPos)
            tmpMat5.translate(tmpVec3.x, 0f, tmpVec3.z)
            tmpMat5.getTranslation(tmpVec3)
            fx.addSmoke(((1f - logic.state.frontTraction) * 4f + (kotlin.random.Random.nextFloat() - 0.5f)) * speedRatio,
                    tmpVec3.x + 2f * (kotlin.random.Random.nextFloat() - 0.5f) * speedRatio,
                    tmpVec3.y + kotlin.random.Random.nextFloat() * speedRatio,
                    tmpVec3.z + 2f * (kotlin.random.Random.nextFloat() - 0.5f) * speedRatio)
        }
        if (logic.state.backTraction < 1f) {
            tmpMat5.set(logic.state.pos)
            tmpVec3.set(0f, 0f, 0f)
            tmpVec3.mul(rearWheelPos)
            tmpMat5.translate(tmpVec3.x, 0f, tmpVec3.z)
            tmpMat5.getTranslation(tmpVec3)
            fx.addSmoke(((1f - logic.state.backTraction) * 4f + (kotlin.random.Random.nextFloat() - 0.5f)) * speedRatio,
                    tmpVec3.x + 2f * (kotlin.random.Random.nextFloat() - 0.5f) * speedRatio,
                    tmpVec3.y + kotlin.random.Random.nextFloat() * speedRatio,
                    tmpVec3.z + 2f * (kotlin.random.Random.nextFloat() - 0.5f) * speedRatio)
        }
        if (logic.state.isTouchingGround) {
            tmpVec3.set(0f, logic.massCenterHeight, 0f)
            tmpVec3.mul(logic.state.lean)
            tmpVec3.y = 0f
            tmpVec3.mul(logic.state.pos)
            fx.addSpark((kotlin.random.Random.nextFloat() + 0.5f) * speedRatio,
                    tmpVec3.x + 2f * (kotlin.random.Random.nextFloat() - 0.5f) * speedRatio,
                    tmpVec3.y + kotlin.random.Random.nextFloat() * speedRatio,
                    tmpVec3.z + 2f * (kotlin.random.Random.nextFloat() - 0.5f) * speedRatio)
        }
        if (lastBackfireSize > 0f) {
            fx.addBackfire(2f * lastBackfireSize, backfireFXPosition)
            lastBackfireSize = 0f
        }
        fx.update(delta)
    }

    fun update(delta: Float) {
        logic.update(delta)
        if (logic.state === logic.statePersist) {
            updateFX(delta)
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
    gl: Gl,
    glslTarget: com.marcowong.motoman.gl.GlslTarget,
    audio: Audio,
    haptics: Haptics,
    assets: Assets,
    textures: TextureCache,
    batch: MeshOptimized,
    logicTrack: LogicTrack,
    inputMeters: IMotorcycleInputMeters
) : Motorcycle(gl, glslTarget, audio, haptics, assets, textures, batch, logicTrack, inputMeters) {
    init {
        val objLoader = ObjLoader(assets)
        val mainBodyModel = objLoader.loadObj("data/bikeBody.obj", true) ?: error("Failed to load bikeBody.obj")
        resolveModelTextures(mainBodyModel, textures)
        bodyModelMeshContext = batch.add(mainBodyModel)

        val mainFrontWheelModel = objLoader.loadObj("data/bikeFrontWheel.obj", true) ?: error("Failed to load bikeFrontWheel.obj")
        resolveModelTextures(mainFrontWheelModel, textures)
        frontWheelModelMeshContext = batch.add(mainFrontWheelModel)

        val mainRearWheelModel = objLoader.loadObj("data/bikeRearWheel.obj", true) ?: error("Failed to load bikeRearWheel.obj")
        resolveModelTextures(mainRearWheelModel, textures)
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

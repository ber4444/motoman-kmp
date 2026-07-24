package com.marcowong.motoman

import com.marcowong.motoman.assets.Assets
import com.marcowong.motoman.gl.Gl
import com.marcowong.motoman.gl.MeshOptimized
import com.marcowong.motoman.gl.ShaderProgram
import com.marcowong.motoman.gl.ShaderPreprocessor
import com.marcowong.motoman.gl.GL_DEPTH_TEST
import com.marcowong.motoman.gl.GL_BLEND
import com.marcowong.motoman.gl.GL_SRC_ALPHA
import com.marcowong.motoman.gl.GL_ONE_MINUS_SRC_ALPHA
import com.marcowong.motoman.gl.GL_DEPTH_BUFFER_BIT
import com.marcowong.motoman.gl.GL_COLOR_BUFFER_BIT
import com.marcowong.motoman.gl.GL_CULL_FACE
import com.marcowong.motoman.scene.*
import com.marcowong.motoman.track.TrackData
import com.marcowong.motoman.track.logic.IMotorcycleInputMeters
import com.marcowong.motoman.track.TrackDirection
import com.marcowong.motoman.track.TrackSegment
import com.marcowong.motoman.model.TextureCache
import com.marcowong.motoman.track.math.Matrix4
import com.marcowong.motoman.gl.GlslTarget
import com.marcowong.motoman.gl.FrameBuffer
import com.marcowong.motoman.gl.IMeshContext
import com.marcowong.motoman.gl.GL_TRIANGLES
import com.marcowong.motoman.gl.GL_LESS
import com.marcowong.motoman.gl.GL_EQUAL
import com.marcowong.motoman.gl.GL_FLOAT
import com.marcowong.motoman.gl.TextureFilter
import com.marcowong.motoman.gl.TextureWrap

import com.marcowong.motoman.audio.Audio
import com.marcowong.motoman.audio.Haptics
import com.marcowong.motoman.audio.MotomanBGMusic
import com.marcowong.motoman.audio.MotorcycleSFX

import com.marcowong.motoman.track.logic.Motorcycle as LogicMotorcycle

/**
 * Adapts the host's [InputState] to the physics' meter interface, using the original game's
 * "combined" one-stick steering model (`MotorcycleInputMetersEasy`).
 *
 * A single [InputState.steer] value drives the bike. The physics has two separate steering
 * meters — counter-steering (initiates and reverses a lean) and leaning (deepens a turn) — and
 * the trick is that the input acts as **either one or the other**, never both at once:
 *
 *  - steering *against* the current lean, or while nearly upright, is counter-steering;
 *  - steering *into* an established lean is leaning.
 *
 * The previous stub applied both simultaneously (and negated counter-steering), so they fought
 * each other: once leaned, the bike could not be brought back and got stuck turning one way.
 *
 * [input] is a `var` on purpose: the motorcycle keeps this adapter for its whole life, so the
 * live [InputState] is swapped in here rather than replacing the adapter.
 */
class StubInputMeters(var input: InputState) : IMotorcycleInputMeters {
    private var motorcycle: LogicMotorcycle? = null

    override fun getEngineAndBrakeMeter(): Float = input.throttle - input.brake

    /** Matches the original when |lean| is below this fraction of the safe angle, steer counter-steers. */
    private val useMoreCounterSteeringRatio = 0.75f

    private fun shouldCounterSteer(meter: Float): Boolean {
        val moto = motorcycle ?: return true
        val leanAngle = moto.state.leanAngle
        // Steering opposite the current lean is always a counter-steer (recover / reverse).
        if (meter < 0f && leanAngle > 0f) return true
        if (meter > 0f && leanAngle < 0f) return true
        // Near upright, steering counter-steers to tip the bike into a new lean.
        if (kotlin.math.abs(leanAngle) <= moto.leanAngleSafe * useMoreCounterSteeringRatio) return true
        // Already leaned and steering into it: lean instead.
        return false
    }

    override fun getCounterSteeringMeter(): Float =
        if (shouldCounterSteer(input.steer)) input.steer else 0f

    override fun getLeanMeter(): Float =
        if (shouldCounterSteer(input.steer)) 0f else input.steer

    override fun setMotorcycle(motorcycle: LogicMotorcycle) {
        this.motorcycle = motorcycle
    }
}

class MotomanGameApp(
    private val assets: Assets,
    private val trackData: TrackData,
    private val glslTarget: GlslTarget,
    private val audio: Audio,
    private val haptics: Haptics,
    private val config: RenderConfig = RenderConfig.ORIGINAL,
    /** Desktop-only: log the bike's world x,z each step for steering direction tests. */
    debugPositions: Boolean = false,
) : GameApp {
    val gameStateFlow = GameStateFlow()
    private lateinit var gl: Gl
    private lateinit var textures: TextureCache
    private lateinit var batch: MeshOptimized
    private lateinit var standardShader: ShaderProgram
    private lateinit var maskShader: ShaderProgram
    private lateinit var ppFinalShader: ShaderProgram
    private lateinit var ppCopyShader: ShaderProgram
    private lateinit var ppMotionBlurShader: ShaderProgram
    private lateinit var ppBloom1Shader: ShaderProgram
    private lateinit var ppBloom2Shader: ShaderProgram
    private lateinit var ppAntiAliasingShader: ShaderProgram
    private lateinit var ppShader: ShaderProgram

    private var mainFrameBufferA: FrameBuffer? = null
    private var mainFrameBufferB: FrameBuffer? = null
    private var bloomFrameBufferA: FrameBuffer? = null
    private var bloomFrameBufferB: FrameBuffer? = null
    private lateinit var frameBufferMeshContext: IMeshContext

    private val lastCameraView = Matrix4()
    private var lastCameraViewReset = true

    /** Output surface size, as distinct from the (possibly reduced) scene buffer size. */
    private var screenWidth = 1
    private var screenHeight = 1

    private val debugPos: com.marcowong.motoman.track.math.Vector3? =
        if (debugPositions) com.marcowong.motoman.track.math.Vector3() else null

    private var standByPending = false
    private var standByTimeRemaining = 0f
    private var crashedPending = false
    private var crashedTimeRemaining = 0f
    
    private val useMotionBlur get() = config.motionBlur
    private val useBloom get() = config.bloom
    private val useAA get() = config.antiAliasing
    private val useLinearFilter get() = config.frameBufferLinearFilter
    
    private lateinit var skyBox: SkyBox
    private lateinit var track: SceneTrack
    private lateinit var rider: Rider
    private lateinit var motorcycle: Motorcycle
    private lateinit var tile: Tile
    private lateinit var backgroundObjs: BackgroundObjs
    private lateinit var camera: MotomanCamera
    private lateinit var inputMeters: StubInputMeters
    private var inputState: InputState? = null
    
    private lateinit var sfx: MotorcycleSFX
    private lateinit var bgm: MotomanBGMusic
    
    private var gameUpdating = true
    private var deltaBudget = 0f
    
    override fun create(gl: Gl, width: Int, height: Int) {
        this.gl = gl
        // The original point-samples model textures (ConfigHelper.turnOnModelTextureLinearFilter
        // is false). Linear here is the single biggest reason the port's surfaces look smoother
        // and cleaner than the 2013 game's.
        val modelFilter =
            if (config.modelTextureLinearFilter) TextureFilter.Linear else TextureFilter.Nearest
        textures = TextureCache(gl, assets, modelFilter, modelFilter)
        batch = MeshOptimized(gl)
        
        val preprocessor = ShaderPreprocessor(glslTarget)
        standardShader = ShaderProgram(
            gl,
            assets.readText("data/shader.standard.vertex.txt"),
            assets.readText("data/shader.standard.fragment.txt"),
            preprocessor
        )
        if (!standardShader.isCompiled) error("Standard shader failed to compile: ${standardShader.log}")
        
        maskShader = ShaderProgram(
            gl,
            assets.readText("data/shader.standard.vertex.txt"),
            assets.readText("data/shader.mask.fragment.txt"),
            preprocessor
        )
        if (!maskShader.isCompiled) error("maskShader failed to compile: ${maskShader.log}")

        ppFinalShader = ShaderProgram(
            gl,
            assets.readText("data/shader.postprocess.vertex.txt"),
            assets.readText("data/shader.final.fragment.txt"),
            preprocessor
        )
        if (!ppFinalShader.isCompiled) error("ppFinalShader failed to compile: ${ppFinalShader.log}")

        ppCopyShader = ShaderProgram(
            gl,
            assets.readText("data/shader.postprocess.vertex.txt"),
            assets.readText("data/shader.final.fragment.txt"),
            preprocessor
        )
        if (!ppCopyShader.isCompiled) error("ppCopyShader failed to compile: ${ppCopyShader.log}")

        ppMotionBlurShader = ShaderProgram(
            gl,
            assets.readText("data/shader.motionblur.vertex.txt"),
            assets.readText("data/shader.motionblur.fragment.txt"),
            preprocessor
        )
        if (!ppMotionBlurShader.isCompiled) error("ppMotionBlurShader failed to compile: ${ppMotionBlurShader.log}")

        ppBloom1Shader = ShaderProgram(
            gl,
            assets.readText("data/shader.postprocess.vertex.txt"),
            assets.readText("data/shader.bloom1.fragment.txt"),
            preprocessor
        )
        if (!ppBloom1Shader.isCompiled) error("ppBloom1Shader failed to compile: ${ppBloom1Shader.log}")

        ppBloom2Shader = ShaderProgram(
            gl,
            assets.readText("data/shader.postprocess.vertex.txt"),
            assets.readText("data/shader.bloom2.fragment.txt"),
            preprocessor
        )
        if (!ppBloom2Shader.isCompiled) error("ppBloom2Shader failed to compile: ${ppBloom2Shader.log}")

        ppAntiAliasingShader = ShaderProgram(
            gl,
            assets.readText("data/shader.postprocess.vertex.txt"),
            assets.readText("data/shader.antialiasing.fragment.txt"),
            preprocessor
        )
        if (!ppAntiAliasingShader.isCompiled) error("ppAntiAliasingShader failed to compile: ${ppAntiAliasingShader.log}")

        ppShader = ShaderProgram(
            gl,
            assets.readText("data/shader.postprocess.vertex.txt"),
            assets.readText("data/shader.postprocess.fragment.txt"),
            preprocessor
        )
        if (!ppShader.isCompiled) error("ppShader failed to compile: ${ppShader.log}")

        val frameBufferMesh = com.marcowong.motoman.model.MeshData().apply {
            vertices = floatArrayOf(
                -1f, -1f, 0f, 0f, 0f,
                1f, -1f, 0f, 1f, 0f,
                1f, 1f, 0f, 1f, 1f,
                -1f, 1f, 0f, 0f, 1f
            )
            indices = shortArrayOf(0, 1, 2, 2, 3, 0)
            hasNorms = false
            hasUVs = true
            hasSkeleton = false
        }
        frameBufferMeshContext = batch.add(frameBufferMesh, GL_TRIANGLES)
        
        skyBox = SkyBox(gl, assets, textures, batch, batch)
        val decorationQuota = 10
        track = SceneTrack(assets, textures, gl, trackData, decorationQuota)
        inputMeters = StubInputMeters(InputState())
        rider = Rider(assets, textures, batch, track.logic)
        motorcycle = MainMotorcycle(gl, glslTarget, audio, haptics, assets, textures, batch, track.logic, inputMeters)
        motorcycle.rider = rider
        rider.motorcycle = motorcycle
        
        tile = Tile(assets, textures, batch)
        backgroundObjs = BackgroundObjs(assets, textures, batch)
        
        camera = MotomanCamera(67f, 1f, 1f, motorcycle, { 0f })
        camera.far = 1500f
        motorcycle.fx.init(camera)
        
        val m = track.logic.getStartSpawnPosition()
        m.`val`.copyInto(motorcycle.logic.state.pos.`val`, 0, 0, 16)
        motorcycle.rider!!.strength = 1f
        
        sfx = MotorcycleSFX(motorcycle, object : MotorcycleSFX.BackfireReporter {
            override fun reportBackfire(size: Float) {
                // Handle backfire visuals if needed
            }
        }, audio, haptics)
        
        bgm = MotomanBGMusic(audio)
        bgm.play()
        
        batch.optimize()

        resize(width, height)
    }

    override fun resize(width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
        val aspectRatio = width.toFloat() / height.toFloat()
        camera.viewportWidth = aspectRatio
        camera.viewportHeight = 1f
        
        mainFrameBufferA?.dispose()
        mainFrameBufferB?.dispose()
        bloomFrameBufferA?.dispose()
        bloomFrameBufferB?.dispose()
        
        // The original renders the scene at half size and upscales (ConfigHelper
        // .getResolutionReduction), which is a large part of why it looks soft rather than
        // crisp. Clamped to at least 1px so a degenerate viewport cannot produce a 0-sized FBO.
        val sceneWidth = kotlin.math.max(1, (width * config.resolutionReduction).toInt())
        val sceneHeight = kotlin.math.max(1, (height * config.resolutionReduction).toInt())
        mainFrameBufferA = FrameBuffer(gl, sceneWidth, sceneHeight, true)
        mainFrameBufferB = FrameBuffer(gl, sceneWidth, sceneHeight, true)
        val bloomSize = 48
        bloomFrameBufferA = FrameBuffer(gl, kotlin.math.ceil(aspectRatio * bloomSize).toInt(), bloomSize, true)
        bloomFrameBufferB = FrameBuffer(gl, kotlin.math.ceil(aspectRatio * bloomSize).toInt(), bloomSize, true)
        
        val filter = if (useLinearFilter) TextureFilter.Linear else TextureFilter.Nearest
        mainFrameBufferA?.texture?.setFilter(filter, filter)
        mainFrameBufferB?.texture?.setFilter(filter, filter)
        bloomFrameBufferA?.texture?.setFilter(filter, filter)
        bloomFrameBufferB?.texture?.setFilter(filter, filter)
    }

    override fun update(dt: Float, input: InputState) {
        this.inputState = input
        // Point the adapter the motorcycle already holds at this frame's input, rather than
        // replacing the adapter (which the motorcycle would never see).
        inputMeters.input = input

        if (gameUpdating) {
            deltaBudget += dt
            val deltaTarget = 1f / 60f
            while (true) {
                val delta: Float
                val isPersistUpdateStep: Boolean
                if (deltaBudget >= deltaTarget) {
                    deltaBudget -= deltaTarget
                    delta = deltaTarget
                    isPersistUpdateStep = true
                    backgroundObjs.setPersist(true)
                    motorcycle.setPersist(true)
                    rider.setPersist(true)
                    camera.setPersist(true)
                } else {
                    delta = deltaBudget
                    isPersistUpdateStep = false
                    backgroundObjs.setPersist(false)
                    motorcycle.setPersist(false)
                    rider.setPersist(false)
                    camera.setPersist(false)
                }
                
                backgroundObjs.update(delta)
                motorcycle.update(delta)
                rider.update(delta)
                track.logic.updateCurrentTrackSegment(motorcycle.logic)
                track.logic.updateCurrentTrackSegment(rider.logic)
                
                if (isPersistUpdateStep) {
                    runStandByRule(delta)
                    debugPos?.let { v ->
                        motorcycle.logic.state.pos.getTranslation(v)
                        val s = motorcycle.logic.state
                        println("POS ${v.x} ${v.z} steer ${inputState?.steer} lean ${s.leanAngle} tract ${s.frontTraction},${s.backTraction} crashed ${s.isCrashed}")
                    }
                }
                
                sfx.update(delta)
                
                camera.setVanishingPointLookingFactor(0f)
                camera.followMotorcycle(delta)
                camera.update(false)
                track.logic.updateCurrentTrackSegment(camera)
                track.updateVanishingPoint(camera)
                
                if (!isPersistUpdateStep) break
            }
            camera.update(true)
            
            // Extract UI info from logic state
            val speed = motorcycle.logic.state.bikeVelo.len() * 3.6f // m/s to km/h
            val gear = 1 // Simplified for now
            // Placeholder for corner notification
            val nextCorner: String? = null
            gameStateFlow.update(speed, gear, nextCorner)
        }
    }

    /**
     * The subset of the original `MotomanGameScreen.runGameRules` that keeps the bike rideable:
     * the standby release and the crash respawn. Without the first, the bike is permanently
     * immobile — every input is discarded by `Motorcycle.getEngineAndBrakeMeter` while
     * `isStandBy` holds. Without the second, the first crash leaves the bike face-down forever.
     *
     * Finish/win handling from the original is still not ported.
     */
    private fun runStandByRule(delta: Float) {
        if (standByTimeRemaining > 0f) standByTimeRemaining -= delta
        if (crashedTimeRemaining > 0f) crashedTimeRemaining -= delta
        val logic = motorcycle.logic

        // Crash -> wait -> respawn at the track's spawn point, then stand by again.
        if (!crashedPending && logic.state.isCrashed) {
            crashedPending = true
            crashedTimeRemaining = CRASH_SECONDS
        }
        if (crashedPending && crashedTimeRemaining <= 0f) {
            crashedPending = false
            logic.reset()
            track.logic.getSpawnPosition(logic).`val`.copyInto(logic.state.pos.`val`, 0, 0, 16)
            lastCameraViewReset = true
        }

        // Standby -> wait -> go.
        if (!standByPending && logic.state.isStandBy) {
            standByPending = true
            standByTimeRemaining = STANDBY_SECONDS
        }
        if (standByPending && standByTimeRemaining <= 0f) {
            standByPending = false
            logic.go()
        }
    }

    override fun render() {
        var mainFB = mainFrameBufferA!!
        var mainFBSpare = mainFrameBufferB!!
        val bloomFBA = bloomFrameBufferA!!
        val bloomFBB = bloomFrameBufferB!!
        
        mainFB.bind()
        // No glEnable(GL_TEXTURE_2D): it is fixed-function GL, implicit under GLES 2.0 where it
        // raises GL_INVALID_ENUM. Desktop GL 2.1 compat tolerated it; Android reported it.
        gl.glEnable(GL_CULL_FACE)
        gl.glDisable(GL_DEPTH_TEST)
        gl.glClearColor(0f, 0f, 0f, 1f)
        gl.glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        
        standardShader.bind()
        standardShader.setUniformi("isNoLightEffect", 1)
        skyBox.render(standardShader, camera, useBloom)
        
        gl.glEnable(GL_BLEND)
        gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        gl.glEnable(GL_DEPTH_TEST)
        gl.glClear(GL_DEPTH_BUFFER_BIT)
        backgroundObjs.render(standardShader, camera)
        
        gl.glClear(GL_DEPTH_BUFFER_BIT)
        tile.render(standardShader, camera)
        track.render(standardShader, camera)
        
        if (useMotionBlur) {
            gl.glUseProgram(0)
            mainFB.end()
            mainFBSpare.bind()
            gl.glDisable(GL_DEPTH_TEST)
            ppCopyShader.bind()
            mainFB.texture.bind(0)
            frameBufferMeshContext.render(ppCopyShader)
            gl.glUseProgram(0)
            gl.glEnable(GL_DEPTH_TEST)
            mainFBSpare.end()
            mainFB.bind()
            gl.glDepthFunc(GL_EQUAL)
            if (!useLinearFilter) mainFBSpare.texture.setFilter(TextureFilter.Linear, TextureFilter.Linear)
            if (lastCameraViewReset) {
                lastCameraViewReset = false
                camera.combined.`val`.copyInto(lastCameraView.`val`, 0, 0, 16)
            }
            ppMotionBlurShader.bind()
            mainFBSpare.texture.bind(1)
            ppMotionBlurShader.setUniformi("pass1Texture", 1)
            ppMotionBlurShader.setUniformf("frameBufferPixelSize", 1f / mainFB.width, 1f / mainFB.height)
            ppMotionBlurShader.setUniformMatrix("viewproj", camera.combined, false)
            ppMotionBlurShader.setUniformMatrix("viewprojinv", camera.invProjectionView, false)
            ppMotionBlurShader.setUniformMatrix("lastviewproj", lastCameraView, false)
            track.render(ppMotionBlurShader, camera)
            gl.glUseProgram(0)
            camera.combined.`val`.copyInto(lastCameraView.`val`, 0, 0, 16)
            if (!useLinearFilter) mainFBSpare.texture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest)
            gl.glDepthFunc(GL_LESS)
            standardShader.bind()
        }
        
        standardShader.setUniformi("isNoLightEffect", 0)
        rider.render(standardShader, camera)
        motorcycle.render(standardShader, camera)
        standardShader.setUniformi("isNoLightEffect", 1)
        gl.glDisable(GL_BLEND)
        gl.glUseProgram(0)
        motorcycle.fx.render()
        mainFB.end()
        
        if (useBloom) {
            bloomFBA.bind()
            gl.glDisable(GL_DEPTH_TEST)
            gl.glEnable(GL_CULL_FACE)
            maskShader.bind()
            maskShader.setUniformi("isNoLightEffect", 1)
            val bloomColor = skyBox.skyBloomColor
            gl.glClearColor(bloomColor.r, bloomColor.g, bloomColor.b, 1f)
            gl.glClear(GL_COLOR_BUFFER_BIT)
            gl.glEnable(GL_BLEND)
            gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            gl.glEnable(GL_DEPTH_TEST)
            gl.glClear(GL_DEPTH_BUFFER_BIT)
            maskShader.setUniformf("maskColor", 0f, 0f, 0f, 1f)
            backgroundObjs.render(maskShader, camera)
            gl.glClear(GL_DEPTH_BUFFER_BIT)
            tile.render(maskShader, camera)
            track.render(maskShader, camera)
            gl.glDisable(GL_BLEND)
            gl.glUseProgram(0)
            bloomFBA.end()
            
            bloomFBB.bind()
            gl.glDisable(GL_DEPTH_TEST)
            ppBloom1Shader.bind()
            ppBloom1Shader.setUniformf("blurSize", 1f / bloomFBA.width)
            bloomFBA.texture.bind(0)
            frameBufferMeshContext.render(ppBloom1Shader)
            gl.glUseProgram(0)
            bloomFBB.end()
            
            bloomFBA.bind()
            gl.glDisable(GL_DEPTH_TEST)
            ppBloom2Shader.bind()
            ppBloom2Shader.setUniformf("blurSize", 1f / bloomFBB.height)
            bloomFBB.texture.bind(0)
            frameBufferMeshContext.render(ppBloom2Shader)
            gl.glUseProgram(0)
            bloomFBA.end()
            
            if (!useLinearFilter) bloomFBA.texture.setFilter(TextureFilter.Linear, TextureFilter.Linear)
            
            mainFBSpare.bind()
            gl.glDisable(GL_DEPTH_TEST)
            ppShader.bind()
            mainFB.texture.bind(0)
            bloomFBA.texture.bind(1)
            ppShader.setUniformi("mainFrameBuffer", 0)
            ppShader.setUniformi("bloomFrameBuffer", 1)
            frameBufferMeshContext.render(ppShader)
            gl.glUseProgram(0)
            mainFBSpare.end()
            
            if (!useLinearFilter) bloomFBA.texture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest)
            
            val tmp = mainFB
            mainFB = mainFBSpare
            mainFBSpare = tmp
        }
        
        if (useAA) {
            if (!useLinearFilter) mainFB.texture.setFilter(TextureFilter.Linear, TextureFilter.Linear)
            
            mainFBSpare.bind()
            gl.glDisable(GL_DEPTH_TEST)
            ppAntiAliasingShader.bind()
            ppAntiAliasingShader.setUniformf("frameBufferPixelSize", 1f / mainFB.width, 1f / mainFB.height)
            mainFB.texture.bind(0)
            frameBufferMeshContext.render(ppAntiAliasingShader)
            gl.glUseProgram(0)
            mainFBSpare.end()
            
            if (!useLinearFilter) mainFB.texture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest)
            
            val tmp = mainFB
            mainFB = mainFBSpare
            mainFBSpare = tmp
        }
        
        gl.glDisable(GL_CULL_FACE)
        gl.glDisable(GL_DEPTH_TEST)
        // Every FrameBuffer.bind() sets the viewport to its own size and end() does not restore
        // it, so the final blit to the screen must set it explicitly. This was invisible while
        // the scene buffers matched the window; with resolutionReduction they no longer do.
        gl.glViewport(0, 0, screenWidth, screenHeight)
        ppFinalShader.bind()
        mainFB.texture.bind(0)
        frameBufferMeshContext.render(ppFinalShader)
        gl.glUseProgram(0)
    }

    override fun dispose() {
        skyBox.dispose()
        track.dispose()
        motorcycle.dispose()
        rider.dispose()
        tile.dispose()
        backgroundObjs.dispose()
        mainFrameBufferA?.dispose()
        mainFrameBufferB?.dispose()
        bloomFrameBufferA?.dispose()
        bloomFrameBufferB?.dispose()
        sfx.dispose()
        bgm.dispose()
    }

    private companion object {
        /** Matches the original's `motorcycleStandByTimeRemaining = 2`. */
        const val STANDBY_SECONDS = 2f
        /** Matches the original's `motorcycleCrashedTimeRemaining = 2`. */
        const val CRASH_SECONDS = 2f
    }

}

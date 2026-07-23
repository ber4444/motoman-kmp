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
import com.marcowong.motoman.gl.GL_TEXTURE_2D
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

import com.marcowong.motoman.track.logic.Motorcycle as LogicMotorcycle

class StubInputMeters(private val input: InputState) : IMotorcycleInputMeters {
    override fun getEngineAndBrakeMeter(): Float = input.throttle - input.brake
    override fun getCounterSteeringMeter(): Float = -input.steer
    override fun getLeanMeter(): Float = input.steer
    override fun setMotorcycle(motorcycle: LogicMotorcycle) {}
}

class MotomanGameApp(
    private val assets: Assets,
    private val trackData: TrackData,
    private val glslTarget: GlslTarget
) : GameApp {
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
    
    private val useMotionBlur = true
    private val useBloom = true
    private val useAA = true
    private val useLinearFilter = true
    
    private lateinit var skyBox: SkyBox
    private lateinit var track: SceneTrack
    private lateinit var rider: Rider
    private lateinit var motorcycle: Motorcycle
    private lateinit var tile: Tile
    private lateinit var backgroundObjs: BackgroundObjs
    private lateinit var camera: MotomanCamera
    private lateinit var inputMeters: StubInputMeters
    private var inputState: InputState? = null
    
    private var gameUpdating = true
    private var deltaBudget = 0f
    
    override fun create(gl: Gl, width: Int, height: Int) {
        this.gl = gl
        textures = TextureCache(gl, assets)
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
        motorcycle = MainMotorcycle(assets, textures, batch, track.logic, inputMeters)
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
        
        batch.optimize()
        
        resize(width, height)
    }

    override fun resize(width: Int, height: Int) {
        val aspectRatio = width.toFloat() / height.toFloat()
        camera.viewportWidth = aspectRatio
        camera.viewportHeight = 1f
        
        mainFrameBufferA?.dispose()
        mainFrameBufferB?.dispose()
        bloomFrameBufferA?.dispose()
        bloomFrameBufferB?.dispose()
        
        mainFrameBufferA = FrameBuffer(gl, width, height, true)
        mainFrameBufferB = FrameBuffer(gl, width, height, true)
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
        inputMeters = StubInputMeters(input) // Update reference if needed or rely on mutation
        // Since we created it with a stub earlier, we can just update its fields if we made it mutable,
        // or just recreate it and set to motorcycle (but motorcycle takes IMotorcycleInputMeters in constructor)
        // Let's assume input state fields are updated directly by host, so we don't need to recreate.
        
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
                    // runGameRules(delta) // omitted crashes/respawn for basic loop
                }
                
                camera.setVanishingPointLookingFactor(0f)
                camera.followMotorcycle(delta)
                camera.update(false)
                track.logic.updateCurrentTrackSegment(camera)
                track.updateVanishingPoint(camera)
                
                if (!isPersistUpdateStep) break
            }
            camera.update(true)
        }
    }

    override fun render() {
        var mainFB = mainFrameBufferA!!
        var mainFBSpare = mainFrameBufferB!!
        val bloomFBA = bloomFrameBufferA!!
        val bloomFBB = bloomFrameBufferB!!
        
        mainFB.bind()
        gl.glEnable(GL_TEXTURE_2D)
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
            gl.glEnable(GL_TEXTURE_2D)
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
    }
}

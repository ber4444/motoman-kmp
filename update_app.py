import re

with open("motoman/src/commonMain/kotlin/com/marcowong/motoman/MotomanGameApp.kt", "r") as f:
    content = f.read()

# Add imports
content = content.replace("import com.marcowong.motoman.gl.GlslTarget", """import com.marcowong.motoman.gl.GlslTarget
import com.marcowong.motoman.gl.FrameBuffer
import com.marcowong.motoman.gl.IMeshContext
import com.marcowong.motoman.gl.GL_TRIANGLES
import com.marcowong.motoman.gl.GL_LESS
import com.marcowong.motoman.gl.GL_EQUAL
import com.marcowong.motoman.gl.GL_FLOAT
import com.marcowong.motoman.gl.TextureFilter
import com.marcowong.motoman.gl.TextureWrap""")

# Add fields
fields = """    private lateinit var standardShader: ShaderProgram
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
    private val useLinearFilter = true"""
content = re.sub(r"    private lateinit var standardShader: ShaderProgram", fields, content)

# Add shaders in create
shaders = """        val preprocessor = ShaderPreprocessor(glslTarget)
        standardShader = ShaderProgram(
            gl,
            assets.readText("data/shader.standard.vertex.txt"),
            assets.readText("data/shader.standard.fragment.txt"),
            preprocessor
        )
        maskShader = ShaderProgram(
            gl,
            assets.readText("data/shader.standard.vertex.txt"),
            assets.readText("data/shader.mask.fragment.txt"),
            preprocessor
        )
        ppFinalShader = ShaderProgram(
            gl,
            assets.readText("data/shader.postprocess.vertex.txt"),
            assets.readText("data/shader.final.fragment.txt"),
            preprocessor
        )
        ppCopyShader = ShaderProgram(
            gl,
            assets.readText("data/shader.postprocess.vertex.txt"),
            assets.readText("data/shader.final.fragment.txt"),
            preprocessor
        )
        ppMotionBlurShader = ShaderProgram(
            gl,
            assets.readText("data/shader.motionblur.vertex.txt"),
            assets.readText("data/shader.motionblur.fragment.txt"),
            preprocessor
        )
        ppBloom1Shader = ShaderProgram(
            gl,
            assets.readText("data/shader.postprocess.vertex.txt"),
            assets.readText("data/shader.bloom1.fragment.txt"),
            preprocessor
        )
        ppBloom2Shader = ShaderProgram(
            gl,
            assets.readText("data/shader.postprocess.vertex.txt"),
            assets.readText("data/shader.bloom2.fragment.txt"),
            preprocessor
        )
        ppAntiAliasingShader = ShaderProgram(
            gl,
            assets.readText("data/shader.postprocess.vertex.txt"),
            assets.readText("data/shader.antialiasing.fragment.txt"),
            preprocessor
        )
        ppShader = ShaderProgram(
            gl,
            assets.readText("data/shader.postprocess.vertex.txt"),
            assets.readText("data/shader.postprocess.fragment.txt"),
            preprocessor
        )

        val frameBufferMesh = com.marcowong.motoman.model.MeshData(
            vertices = floatArrayOf(
                -1f, -1f, 0f, 0f, 0f,
                1f, -1f, 0f, 1f, 0f,
                1f, 1f, 0f, 1f, 1f,
                -1f, 1f, 0f, 0f, 1f
            ),
            indices = shortArrayOf(0, 1, 2, 2, 3, 0),
            vertexSize = 5,
            hasNorms = false,
            hasUVs = true,
            hasSkeleton = false
        )
        frameBufferMeshContext = batch.add(frameBufferMesh, GL_TRIANGLES)"""
content = re.sub(r"        val preprocessor = ShaderPreprocessor.*?error\(\"Standard shader failed to compile: \$\{standardShader\.log\}\"\)\n        \}", shaders, content, flags=re.DOTALL)

# Add resize
resize_method = """    override fun resize(width: Int, height: Int) {
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
    }"""
content = re.sub(r"    override fun resize\(width: Int, height: Int\) \{.*?    \}", resize_method, content, flags=re.DOTALL)

# Add render
render_method = """    override fun render() {
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
            standardShader.unbind()
            mainFB.end()
            mainFBSpare.bind()
            gl.glDisable(GL_DEPTH_TEST)
            ppCopyShader.bind()
            mainFB.texture.bind(0)
            frameBufferMeshContext.render(ppCopyShader)
            ppCopyShader.unbind()
            gl.glEnable(GL_DEPTH_TEST)
            mainFBSpare.end()
            mainFB.bind()
            gl.glDepthFunc(GL_EQUAL)
            if (!useLinearFilter) mainFBSpare.texture.setFilter(TextureFilter.Linear, TextureFilter.Linear)
            if (lastCameraViewReset) {
                lastCameraViewReset = false
                System.arraycopy(camera.combined.`val`, 0, lastCameraView.`val`, 0, 16)
            }
            ppMotionBlurShader.bind()
            mainFBSpare.texture.bind(1)
            ppMotionBlurShader.setUniformi("pass1Texture", 1)
            ppMotionBlurShader.setUniform2f("frameBufferPixelSize", 1f / mainFB.width, 1f / mainFB.height)
            ppMotionBlurShader.setUniformMatrix4fv("viewproj", false, camera.combined.`val`)
            ppMotionBlurShader.setUniformMatrix4fv("viewprojinv", false, camera.invProjectionView.`val`)
            ppMotionBlurShader.setUniformMatrix4fv("lastviewproj", false, lastCameraView.`val`)
            track.render(ppMotionBlurShader, camera)
            ppMotionBlurShader.unbind()
            System.arraycopy(camera.combined.`val`, 0, lastCameraView.`val`, 0, 16)
            if (!useLinearFilter) mainFBSpare.texture.setFilter(TextureFilter.Nearest, TextureFilter.Nearest)
            gl.glDepthFunc(GL_LESS)
            standardShader.bind()
        }
        
        standardShader.setUniformi("isNoLightEffect", 0)
        rider.render(standardShader, camera)
        motorcycle.render(standardShader, camera)
        standardShader.setUniformi("isNoLightEffect", 1)
        gl.glDisable(GL_BLEND)
        standardShader.unbind()
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
            maskShader.setUniform4f("maskColor", 0f, 0f, 0f, 1f)
            backgroundObjs.render(maskShader, camera)
            gl.glClear(GL_DEPTH_BUFFER_BIT)
            tile.render(maskShader, camera)
            track.render(maskShader, camera)
            gl.glDisable(GL_BLEND)
            maskShader.unbind()
            bloomFBA.end()
            
            bloomFBB.bind()
            gl.glDisable(GL_DEPTH_TEST)
            ppBloom1Shader.bind()
            ppBloom1Shader.setUniform1f("blurSize", 1f / bloomFBA.width)
            bloomFBA.texture.bind(0)
            frameBufferMeshContext.render(ppBloom1Shader)
            ppBloom1Shader.unbind()
            bloomFBB.end()
            
            bloomFBA.bind()
            gl.glDisable(GL_DEPTH_TEST)
            ppBloom2Shader.bind()
            ppBloom2Shader.setUniform1f("blurSize", 1f / bloomFBB.height)
            bloomFBB.texture.bind(0)
            frameBufferMeshContext.render(ppBloom2Shader)
            ppBloom2Shader.unbind()
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
            ppShader.unbind()
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
            ppAntiAliasingShader.setUniform2f("frameBufferPixelSize", 1f / mainFB.width, 1f / mainFB.height)
            mainFB.texture.bind(0)
            frameBufferMeshContext.render(ppAntiAliasingShader)
            ppAntiAliasingShader.unbind()
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
        ppFinalShader.unbind()
    }"""
content = re.sub(r"    override fun render\(\) \{.*?    \}", render_method, content, flags=re.DOTALL)

# Add dispose
dispose_method = """    override fun dispose() {
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
    }"""
content = re.sub(r"    override fun dispose\(\) \{.*?    \}", dispose_method, content, flags=re.DOTALL)

with open("motoman/src/commonMain/kotlin/com/marcowong/motoman/MotomanGameApp.kt", "w") as f:
    f.write(content)

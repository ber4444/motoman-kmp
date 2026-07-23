import re

with open("motoman/src/commonMain/kotlin/com/marcowong/motoman/MotomanGameApp.kt", "r") as f:
    content = f.read()

create_func = """    override fun create(gl: Gl, width: Int, height: Int) {
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
        System.arraycopy(m.`val`, 0, motorcycle.logic.state.pos.`val`, 0, 16)
        motorcycle.rider!!.strength = 1f
        
        batch.optimize()
        
        resize(width, height)
    }"""
    
content = re.sub(r"    override fun create\(gl: Gl, width: Int, height: Int\) \{.*?resize\(width, height\)\n    \}", create_func, content, flags=re.DOTALL)

with open("motoman/src/commonMain/kotlin/com/marcowong/motoman/MotomanGameApp.kt", "w") as f:
    f.write(content)

package com.marcowong.motoman

import com.marcowong.motoman.assets.Assets
import com.marcowong.motoman.gl.GL_COLOR_BUFFER_BIT
import com.marcowong.motoman.gl.GL_DEPTH_BUFFER_BIT
import com.marcowong.motoman.gl.GL_DEPTH_TEST
import com.marcowong.motoman.gl.GL_LESS
import com.marcowong.motoman.gl.GL_RGBA
import com.marcowong.motoman.gl.GL_UNSIGNED_BYTE
import com.marcowong.motoman.gl.Gl
import com.marcowong.motoman.gl.GlslTarget
import com.marcowong.motoman.gl.ShaderPreprocessor
import com.marcowong.motoman.gl.IMeshContext
import com.marcowong.motoman.gl.MeshOptimized
import com.marcowong.motoman.gl.ShaderProgram
import com.marcowong.motoman.model.ObjLoader
import com.marcowong.motoman.model.RenderableModel
import com.marcowong.motoman.model.TextureCache
import com.marcowong.motoman.track.math.Matrix4
import com.marcowong.motoman.track.math.PerspectiveCamera
import com.marcowong.motoman.track.math.Vector3
import kotlin.math.max
import kotlin.math.sqrt

/**
 * First light: loads one OBJ with its materials and textures and draws it with the
 * engine's own `shader.standard.*` shaders. This is the step that proves the whole
 * pipeline joins up — asset IO → OBJ/MTL parse → Mesh upload → texture bind → draw —
 * which nothing before it could.
 *
 * Lives in `commonMain` so the Android host (3b) drives exactly the same code.
 */
class ModelViewerApp(
    private val assets: Assets,
    private val modelPath: String,
    private val glslTarget: GlslTarget,
    /** When true, the framebuffer is sampled after each frame; see [drawnPixelFraction]. */
    private val sampleFramebuffer: Boolean = false,
    /** Draw through the batched [MeshOptimized] path instead of one Mesh per sub-mesh. */
    private val batched: Boolean = false,
) : GameApp {

    private lateinit var gl: Gl
    private lateinit var shader: ShaderProgram
    private var renderable: RenderableModel? = null
    private var batch: MeshOptimized? = null
    private var batchContext: IMeshContext? = null
    private lateinit var textures: TextureCache
    private lateinit var camera: PerspectiveCamera

    private val modelMatrix = Matrix4()
    private val modelView = Matrix4()
    private val modelViewProj = Matrix4()
    private var angle = 0f

    private var width = 1
    private var height = 1

    val frameWidth: Int get() = width
    val frameHeight: Int get() = height

    /** Fraction of sampled pixels that differ from the clear colour. 0 means nothing drew. */
    var drawnPixelFraction: Float = 0f
        private set

    /** Raw RGBA of the last sampled frame (bottom-up, as GL returns it), for inspection. */
    var lastFramePixels: ByteArray? = null
        private set

    /** Compile/link diagnostics, empty when the shader built cleanly. */
    var shaderLog: String = ""
        private set

    override fun create(gl: Gl, width: Int, height: Int) {
        this.gl = gl
        this.width = width
        this.height = height

        val preprocessor = ShaderPreprocessor(glslTarget)
        shader = ShaderProgram(
            gl,
            assets.readText("data/shader.standard.vertex.txt"),
            assets.readText("data/shader.standard.fragment.txt"),
            preprocessor,
        )
        shaderLog = shader.log
        check(shader.isCompiled) { "standard shader failed to build:\n${shader.log}" }

        val model = ObjLoader(assets).loadObj(modelPath)
            ?: error("failed to load model $modelPath")
        textures = TextureCache(gl, assets)
        if (batched) {
            // Textures still have to be attached to the materials the batch will bind.
            for (sub in model.subMeshes) {
                val material = sub.material ?: continue
                material.diffuseTextureName?.let { material.diffuseTexture = textures.get(it) }
            }
            batch = MeshOptimized(gl).also {
                batchContext = it.add(model)
                it.optimize()
            }
        } else {
            renderable = RenderableModel(gl, model, textures)
        }

        camera = PerspectiveCamera(67f, width.toFloat(), height.toFloat())
        frameModel(model)

        gl.glEnable(GL_DEPTH_TEST)
        gl.glDepthFunc(GL_LESS)
        gl.glClearColor(CLEAR_R, CLEAR_G, CLEAR_B, 1f)
    }

    /** Positions the camera so the whole model is visible, whatever its scale. */
    private fun frameModel(model: com.marcowong.motoman.model.ModelData) {
        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE; var minZ = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE; var maxZ = -Float.MAX_VALUE
        for (sub in model.subMeshes) {
            val mesh = sub.mesh ?: continue
            val stride = mesh.vertexSize
            var i = 0
            while (i + 2 < mesh.vertices.size) {
                val x = mesh.vertices[i]; val y = mesh.vertices[i + 1]; val z = mesh.vertices[i + 2]
                if (x < minX) minX = x; if (x > maxX) maxX = x
                if (y < minY) minY = y; if (y > maxY) maxY = y
                if (z < minZ) minZ = z; if (z > maxZ) maxZ = z
                i += stride
            }
        }
        centre.set((minX + maxX) * 0.5f, (minY + maxY) * 0.5f, (minZ + maxZ) * 0.5f)
        val dx = maxX - minX; val dy = maxY - minY; val dz = maxZ - minZ
        radius = max(0.001f, sqrt(dx * dx + dy * dy + dz * dz) * 0.5f)

        camera.near = radius * 0.05f
        camera.far = radius * 20f
    }

    private val centre = Vector3()
    private var radius = 1f

    override fun resize(width: Int, height: Int) {
        this.width = width
        this.height = height
        gl.glViewport(0, 0, width, height)
        camera.viewportWidth = width.toFloat()
        camera.viewportHeight = height.toFloat()
    }

    override fun update(dt: Float, input: InputState) {
        // Orbit slowly, plus manual steer input so the keyboard path is exercised.
        angle += (dt * 40f) + (input.steer * dt * 120f)
        modelMatrix.idt().rotate(0f, 1f, 0f, angle)
    }

    override fun render() {
        val dist = radius * 1.7f
        camera.position.set(centre.x, centre.y + radius * 0.4f, centre.z + dist)
        camera.up.set(0f, 1f, 0f)
        camera.lookAt(centre.x, centre.y, centre.z)
        camera.update()

        gl.glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        shader.bind()
        modelView.set(camera.view).mul(modelMatrix)
        modelViewProj.set(camera.combined).mul(modelMatrix)
        shader.setUniformMatrix("modelview", modelView)
        shader.setUniformMatrix("modelviewproj", modelViewProj)
        shader.setUniformi("isNoLightEffect", 0)

        renderable?.render(shader)
        batchContext?.render(shader)

        if (sampleFramebuffer) drawnPixelFraction = measureDrawnPixels()
    }

    /** Counts pixels that differ from the clear colour, i.e. that geometry actually covered. */
    private fun measureDrawnPixels(): Float {
        val pixels = gl.glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE)
        lastFramePixels = pixels
        val clearR = (CLEAR_R * 255f).toInt()
        val clearG = (CLEAR_G * 255f).toInt()
        val clearB = (CLEAR_B * 255f).toInt()
        var drawn = 0
        var total = 0
        var i = 0
        while (i + 3 < pixels.size) {
            val r = pixels[i].toInt() and 0xFF
            val g = pixels[i + 1].toInt() and 0xFF
            val b = pixels[i + 2].toInt() and 0xFF
            if (kotlin.math.abs(r - clearR) > 6 || kotlin.math.abs(g - clearG) > 6 || kotlin.math.abs(b - clearB) > 6) drawn++
            total++
            i += 4
        }
        return if (total == 0) 0f else drawn.toFloat() / total
    }

    override fun dispose() {
        renderable?.dispose()
        batch?.dispose()
        textures.dispose()
        shader.dispose()
    }

    private companion object {
        const val CLEAR_R = 0.10f
        const val CLEAR_G = 0.12f
        const val CLEAR_B = 0.16f
    }
}

package com.marcowong.motoman.scene

import com.marcowong.motoman.assets.Assets
import com.marcowong.motoman.gl.Gl
import com.marcowong.motoman.gl.MeshOptimized
import com.marcowong.motoman.gl.ShaderProgram
import com.marcowong.motoman.gl.ShaderPreprocessor
import com.marcowong.motoman.gl.GL_BLEND
import com.marcowong.motoman.gl.GL_SRC_ALPHA
import com.marcowong.motoman.gl.GL_ONE_MINUS_SRC_ALPHA
import com.marcowong.motoman.model.MaterialData
import com.marcowong.motoman.model.MeshData
import com.marcowong.motoman.model.SubMeshData
import com.marcowong.motoman.model.ModelData
import com.marcowong.motoman.model.TextureCache
import com.marcowong.motoman.track.math.Vector3
import com.marcowong.motoman.gl.GlslTarget
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import com.marcowong.motoman.gl.IMeshContext

interface DynamicFXPosition {
    fun getPosition(vec: Vector3)
}

class MotorcycleFX(private val gl: Gl, private val assets: Assets, private val textures: TextureCache, private val glslTarget: GlslTarget) {
    private val smokeTex = MaterialData("data/smoke.png").apply {
        diffuseTextureName = "data/smoke.png"
        diffuseTexture = textures.get("data/smoke.png")
    }
    private val sparkTex = MaterialData("data/spark.png").apply {
        diffuseTextureName = "data/spark.png"
        diffuseTexture = textures.get("data/spark.png")
    }
    private val backfireTex = MaterialData("data/backfire.png").apply {
        diffuseTextureName = "data/backfire.png"
        diffuseTexture = textures.get("data/backfire.png")
    }
    
    private val particleShader: ShaderProgram
    
    init {
        val preprocessor = ShaderPreprocessor(glslTarget)
        particleShader = ShaderProgram(
            gl,
            assets.readText("data/shader.particle.vertex.txt"),
            assets.readText("data/shader.particle.fragment.txt"),
            preprocessor
        )
        if (!particleShader.isCompiled) error("particleShader failed to compile: ${particleShader.log}")
    }

    private class DecalInst {
        var tex: MaterialData? = null
        var duration: Float = 0f
        var fadeOutTime: Float = 0f
        var rendered: Boolean = false
        var up: Vector3 = Vector3()
        var dFXPos: DynamicFXPosition? = null
        var size: Float = 0f
        var pos: Vector3 = Vector3()
    }
    
    private val decals = LinkedHashSet<DecalInst>()
    private var camera: MotomanCamera? = null

    fun init(camera: MotomanCamera) {
        this.camera = camera
    }

    private fun addDecal(tex: MaterialData, size: Float, duration: Float, fadeOutTime: Float, px: Float, py: Float, pz: Float, dFXPos: DynamicFXPosition?) {
        val inst = DecalInst()
        inst.size = size * (0.5f + Random.nextFloat())
        inst.tex = tex
        if (dFXPos == null) {
            inst.pos.set(px, py, pz)
        } else {
            dFXPos.getPosition(inst.pos)
        }
        inst.duration = duration
        inst.fadeOutTime = fadeOutTime
        inst.up.set(0f, 1f, 0f)
        val angle = Random.nextFloat() * 360f * (kotlin.math.PI.toFloat() / 180f)
        val s = sin(angle)
        val c = cos(angle)
        inst.up.set(-s, c, 0f)
        inst.dFXPos = dFXPos
        decals.add(inst)
    }

    fun addSmoke(size: Float, px: Float, py: Float, pz: Float) {
        addDecal(smokeTex, size, 1f, 0.5f, px, py, pz, null)
    }

    fun addSpark(size: Float, px: Float, py: Float, pz: Float) {
        addDecal(sparkTex, size, 1f, 1f, px, py, pz, null)
    }

    fun addBackfire(size: Float, dFXPos: DynamicFXPosition) {
        addDecal(backfireTex, size, -1f, -1f, 0f, 0f, 0f, dFXPos)
    }

    private val removingDecalInst = ArrayList<DecalInst>()

    fun update(delta: Float) {
        if (decals.isEmpty()) return
        for (inst in decals) {
            if (inst.duration >= 0) {
                inst.duration -= delta
                if (inst.duration <= 0) removingDecalInst.add(inst)
            } else {
                if (inst.rendered) removingDecalInst.add(inst)
            }
        }
        decals.removeAll(removingDecalInst.toSet())
        removingDecalInst.clear()
    }

    private val tmpVecCam = Vector3()
    private val right = Vector3()
    private val up = Vector3()
    private val p1 = Vector3()
    private val p2 = Vector3()
    private val p3 = Vector3()
    private val p4 = Vector3()
    
    fun render() {
        if (decals.isEmpty()) return
        val cam = camera ?: return

        val batch = MeshOptimized(gl)
        val contexts = ArrayList<IMeshContext>()

        tmpVecCam.set(cam.direction).nor()

        for (inst in decals) {
            inst.rendered = true
            if (inst.dFXPos != null) {
                inst.dFXPos!!.getPosition(inst.pos)
            }
            
            var alpha = 1f
            if (inst.fadeOutTime > 0 && inst.duration < inst.fadeOutTime) {
                alpha = inst.duration / inst.fadeOutTime
            }

            val N = tmpVecCam
            right.set(inst.up).crs(N.x, N.y, N.z).nor()
            up.set(N).crs(right.x, right.y, right.z).nor()
            
            val halfSize = inst.size * 0.5f
            
            p1.set(inst.pos).sub(right.x * halfSize, right.y * halfSize, right.z * halfSize)
              .sub(up.x * halfSize, up.y * halfSize, up.z * halfSize)
            p2.set(inst.pos).add(right.x * halfSize, right.y * halfSize, right.z * halfSize)
              .sub(up.x * halfSize, up.y * halfSize, up.z * halfSize)
            p3.set(inst.pos).add(right.x * halfSize, right.y * halfSize, right.z * halfSize)
              .add(up.x * halfSize, up.y * halfSize, up.z * halfSize)
            p4.set(inst.pos).sub(right.x * halfSize, right.y * halfSize, right.z * halfSize)
              .add(up.x * halfSize, up.y * halfSize, up.z * halfSize)

            val vertices = FloatArray(4 * 9)
            val indices = ShortArray(6)
            
            val meshData = MeshData()
            meshData.vertices = vertices
            meshData.indices = indices
            meshData.hasNorms = true
            meshData.hasUVs = true
            meshData.hasSkeleton = false
            // vertexSize is val

            fun setV(idx: Int, p: Vector3, u: Float, v: Float) {
                val offset = idx * 9
                vertices[offset] = p.x
                vertices[offset + 1] = p.y
                vertices[offset + 2] = p.z
                // pass alpha in normal.x
                vertices[offset + 3] = alpha
                vertices[offset + 4] = 0f
                vertices[offset + 5] = 0f
                vertices[offset + 6] = u
                vertices[offset + 7] = v
                vertices[offset + 8] = 0f
            }
            
            setV(0, p1, 0f, 1f)
            setV(1, p2, 1f, 1f)
            setV(2, p3, 1f, 0f)
            setV(3, p4, 0f, 0f)
            
            indices[0] = 0; indices[1] = 1; indices[2] = 2
            indices[3] = 2; indices[4] = 3; indices[5] = 0
            
            val subMesh = SubMeshData()
            subMesh.mesh = meshData
            subMesh.material = inst.tex
            
            val modelData = ModelData()
            modelData.subMeshes = arrayOf(subMesh)
            
            contexts.add(batch.add(modelData))
        }
        
        batch.optimize()
        
        particleShader.bind()
        particleShader.setUniformMatrix("modelviewproj", cam.combined)
        
        gl.glDepthMask(false)
        gl.glEnable(GL_BLEND)
        gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        
        for (ctx in contexts) {
            ctx.render(particleShader)
        }
        
        gl.glUseProgram(0)
        gl.glDepthMask(true)
        gl.glDisable(GL_BLEND)
        
        batch.dispose()
    }
    
    fun clear() {
        decals.clear()
    }
    
    fun dispose() {
        particleShader.dispose()
    }
}

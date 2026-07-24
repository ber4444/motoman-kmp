package com.marcowong.motoman.model

import com.marcowong.motoman.gl.GL_TRIANGLES
import com.marcowong.motoman.gl.ShaderProgram
import com.marcowong.motoman.gl.Texture
import com.marcowong.motoman.track.math.Color

/** A loaded model: one or more sub-meshes, each with its own material. */
class ModelData {
    var subMeshes: Array<SubMeshData> = emptyArray()
}

class SubMeshData {
    var name: String = ""
    var mesh: MeshData? = null
    var material: MaterialData? = null
    var primitiveType: Int = GL_TRIANGLES
}

/** Interleaved vertex data plus optional indices, as produced by the OBJ loader. */
class MeshData {
    var vertices: FloatArray = FloatArray(0)
    var indices: ShortArray? = null
    var hasNorms: Boolean = false
    var hasUVs: Boolean = false
    var hasSkeleton: Boolean = false

    /** Floats per vertex for this layout. */
    val vertexSize: Int
        get() = 3 + (if (hasNorms) 3 else 0) + (if (hasUVs) 2 else 0) + (if (hasSkeleton) 1 else 0)

    val numVertices: Int
        get() = if (vertexSize == 0) 0 else vertices.size / vertexSize
}

/**
 * Material description. Parsing produces the colours and the *name* of the diffuse
 * texture; [diffuseTexture] is attached later by the renderer, which is what keeps the
 * OBJ/MTL loader free of GL and testable in `commonMain`.
 */
class MaterialData(val name: String) {
    var diffuseTextureName: String? = null
    var diffuseTexture: Texture? = null
    var ambientColor: Color = Color(1f, 1f, 1f, 1f)
    var diffuseColor: Color = Color(1f, 1f, 1f, 1f)
    var specularColor: Color = Color(1f, 1f, 1f, 1f)
    var shininessColor: Color = Color(1f, 1f, 1f, 1f)

    /** Binds this material's texture and colours to the standard shader's uniforms. */
    fun bind(program: ShaderProgram) {
        diffuseTexture?.bind(0)
        program.setUniformi("diffuseTexture", 0)
        program.setUniformf("ambientColor", ambientColor.r, ambientColor.g, ambientColor.b, ambientColor.a)
        program.setUniformf("diffuseColor", diffuseColor.r, diffuseColor.g, diffuseColor.b, diffuseColor.a)
        program.setUniformf("specularColor", specularColor.r, specularColor.g, specularColor.b, specularColor.a)
        program.setUniformf("shininessColor", shininessColor.r, shininessColor.g, shininessColor.b, shininessColor.a)
    }

    override fun equals(other: Any?): Boolean = other is MaterialData && other.name == name
    override fun hashCode(): Int = name.hashCode()
}

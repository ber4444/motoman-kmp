package com.marcowong.motoman

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.glutils.ShaderProgram

class ModelData {
    @JvmField var subMeshes: Array<SubMeshData> = emptyArray()
}

class SubMeshData {
    @JvmField var name: String = ""
    @JvmField var mesh: MeshData? = null
    @JvmField var material: MaterialData? = null
    @JvmField var primitiveType: Int = 4 // GL_TRIANGLES
}

class MeshData {
    @JvmField var vertices: FloatArray = FloatArray(0)
    @JvmField var indices: ShortArray = ShortArray(0)
    @JvmField var hasNorms: Boolean = false
    @JvmField var hasUVs: Boolean = false
    @JvmField var hasSkeleton: Boolean = false
}

class MaterialData(val name: String) {
    @JvmField var diffuseTexture: Texture? = null
    @JvmField var ambientColor: Color = Color(Color.WHITE)
    @JvmField var diffuseColor: Color = Color(Color.WHITE)
    @JvmField var specularColor: Color = Color(Color.WHITE)
    @JvmField var shininessColor: Color = Color(Color.WHITE)
    
    fun bind(program: ShaderProgram) {
        diffuseTexture?.bind(0)
        program.setUniformi("s_texture", 0)
    }
}

package com.marcowong.motoman.model

import com.marcowong.motoman.assets.Assets
import com.marcowong.motoman.gl.Gl
import com.marcowong.motoman.gl.Mesh
import com.marcowong.motoman.gl.ShaderProgram
import com.marcowong.motoman.gl.Texture
import com.marcowong.motoman.gl.TextureFilter
import com.marcowong.motoman.gl.VertexAttribute
import com.marcowong.motoman.gl.VertexAttributes
import com.marcowong.motoman.gl.VertexUsage
import com.marcowong.motoman.gl.decodePixmap

/**
 * Loads and caches [Texture]s by asset path. Models reference textures by name, so the
 * same image shared by several materials is decoded and uploaded exactly once.
 */
class TextureCache(
    private val gl: Gl,
    private val assets: Assets,
    private val minFilter: TextureFilter = TextureFilter.Linear,
    private val magFilter: TextureFilter = TextureFilter.Linear,
) {
    private val cache = HashMap<String, Texture>()

    fun get(path: String): Texture = cache.getOrPut(path) {
        Texture(gl, decodePixmap(assets.readBytes(path)), minFilter, magFilter)
    }

    fun dispose() {
        cache.values.forEach { it.dispose() }
        cache.clear()
    }
}

/**
 * A [ModelData] uploaded to the GPU: one [Mesh] per sub-mesh, with each material's
 * texture resolved from its name. This is the join between the loader (CPU side) and
 * the GL object layer.
 */
class RenderableModel(
    gl: Gl,
    private val model: ModelData,
    textures: TextureCache,
) {
    private val meshes: List<Mesh> = model.subMeshes.map { sub ->
        val meshData = sub.mesh ?: error("sub-mesh '${sub.name}' has no mesh data")
        val mesh = Mesh(gl, isStatic = true, maxIndices = meshData.indices?.size ?: 0, attributes = attributesFor(meshData))
        mesh.setVertices(meshData.vertices)
        meshData.indices?.let { mesh.setIndices(it) }
        mesh
    }

    init {
        // Attach textures now that a GL context exists; the loader only recorded names.
        for (sub in model.subMeshes) {
            val material = sub.material ?: continue
            val name = material.diffuseTextureName ?: continue
            material.diffuseTexture = textures.get(name)
        }
    }

    fun render(shader: ShaderProgram) {
        for (i in model.subMeshes.indices) {
            val sub = model.subMeshes[i]
            sub.material?.bind(shader)
            val mesh = meshes[i]
            val count = mesh.numIndices.takeIf { it > 0 } ?: mesh.numVertices
            mesh.render(shader, sub.primitiveType, 0, count)
        }
    }

    fun dispose() = meshes.forEach { it.dispose() }

    companion object {
        /**
         * Builds the attribute layout matching how [ObjLoader] interleaves a vertex:
         * position, then normal, then UV, then skeleton index.
         */
        fun attributesFor(mesh: MeshData): VertexAttributes {
            val attributes = ArrayList<VertexAttribute>(4)
            attributes += VertexAttribute(VertexUsage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE)
            if (mesh.hasNorms) attributes += VertexAttribute(VertexUsage.Normal, 3, ShaderProgram.NORMAL_ATTRIBUTE)
            if (mesh.hasUVs) attributes += VertexAttribute(VertexUsage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0")
            if (mesh.hasSkeleton) attributes += VertexAttribute(VertexUsage.Generic, 1, ShaderProgram.SKELETON_ATTRIBUTE)
            return VertexAttributes(*attributes.toTypedArray())
        }
    }
}

package com.marcowong.motoman.gl

import com.marcowong.motoman.model.MaterialData
import com.marcowong.motoman.model.MeshData
import com.marcowong.motoman.model.ModelData

/** A batch of geometry inside a [MeshOptimized] that can be drawn with one shader. */
interface IMeshContext {
    val nCopies: Int
    fun render(shader: ShaderProgram)
    fun render(shader: ShaderProgram, copies: Int)
}

/**
 * Packs many models into a single shared VBO/IBO, grouped by material, with duplicate
 * vertices removed. Port of the engine's `MeshOptimized` — the batched path that makes a
 * whole track drawable in a handful of draw calls, as opposed to `RenderableModel`'s
 * naive one-`Mesh`-per-sub-mesh approach.
 *
 * Every vertex is expanded to a fixed 9-float layout (position, normal, UV, skeleton
 * index) regardless of what the source mesh carried, so one attribute layout serves the
 * entire buffer.
 *
 * Two deliberate departures from the original, neither of which changes output:
 *  - material grouping uses insertion-ordered maps, where the original used `Hashtable`.
 *    Draw order is now deterministic instead of depending on hash iteration order.
 *  - vertex de-duplication is hash-based rather than the original's O(n²) scan. It
 *    produces byte-identical results (see `MeshOptimizedDedupTest`), which matters
 *    because batching a track pushes the vertex count far past where O(n²) is viable.
 */
class MeshOptimized(private val gl: Gl) {

    private class RenderContext(val material: MaterialData?, val offset: Int, val count: Int)

    private inner class MeshContext(
        override val nCopies: Int,
        private val primitiveType: Int,
        private val renderContexts: List<RenderContext>,
    ) : IMeshContext {

        override fun render(shader: ShaderProgram) = render(shader, 1)

        override fun render(shader: ShaderProgram, copies: Int) {
            check(uploaded) { "optimize() must be called before rendering" }
            gl.glBindBuffer(GL_ARRAY_BUFFER, vbo)
            gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo)
            for (rc in renderContexts) {
                rc.material?.bind(shader)
                for (i in 0 until attributes.size()) {
                    val attribute = attributes[i]
                    shader.enableVertexAttribute(attribute.alias)
                    shader.setVertexAttribute(
                        attribute.alias, attribute.numComponents, GL_FLOAT,
                        false, attributes.vertexSize, attribute.offset,
                    )
                }
                gl.glDrawElements(primitiveType, rc.count * copies, GL_UNSIGNED_SHORT, rc.offset * 2)
            }
        }
    }

    private val vertexChunks = ArrayList<FloatArray>()
    private val indexChunks = ArrayList<ShortArray>()

    private var vbo = 0
    private var ibo = 0
    private var uploaded = false

    private val attributes = VertexAttributes(
        VertexAttribute(VertexUsage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE),
        VertexAttribute(VertexUsage.Normal, 3, ShaderProgram.NORMAL_ATTRIBUTE),
        VertexAttribute(VertexUsage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0"),
        VertexAttribute(VertexUsage.Generic, 1, ShaderProgram.SKELETON_ATTRIBUTE),
    )

    /** Adds every sub-mesh of [model], grouped by material, optionally [copies] times. */
    fun add(model: ModelData, copies: Int = 1): IMeshContext {
        // Insertion-ordered so the resulting draw order is deterministic.
        val byMaterial = LinkedHashMap<MaterialData?, LinkedHashSet<MeshData>>()
        for (sub in model.subMeshes) {
            val mesh = sub.mesh ?: continue
            byMaterial.getOrPut(sub.material) { LinkedHashSet() }.add(mesh)
        }

        val contexts = ArrayList<RenderContext>(byMaterial.size)
        for ((material, meshes) in byMaterial) {
            val packed = createOptimizedMesh(meshes, copies)
            requireShortIndexable(currentVertexCount() + packed.vertices.size / VERTEX_FLOATS)
            val offset = currentIndexCount()
            shiftIndices(packed.indices)
            vertexChunks.add(packed.vertices)
            indexChunks.add(packed.indices)
            contexts.add(RenderContext(material, offset, packed.indices.size / copies))
        }
        return MeshContext(copies, GL_TRIANGLES, contexts)
    }

    /** Adds a single raw mesh with an explicit primitive type and no material. */
    fun add(mesh: MeshData, primitiveType: Int): IMeshContext {
        val packed = createOptimizedMesh(linkedSetOf(mesh), 1)
        val offset = currentIndexCount()
        shiftIndices(packed.indices)
        vertexChunks.add(packed.vertices)
        indexChunks.add(packed.indices)
        return MeshContext(1, primitiveType, listOf(RenderContext(null, offset, packed.indices.size)))
    }

    private class Packed(val vertices: FloatArray, val indices: ShortArray)

    private fun currentIndexCount(): Int = indexChunks.sumOf { it.size }

    private fun currentVertexCount(): Int = vertexChunks.sumOf { it.size / VERTEX_FLOATS }

    /** Rebases indices onto the vertices already accumulated in the shared buffer. */
    private fun shiftIndices(indices: ShortArray) {
        val base = currentVertexCount()
        for (i in indices.indices) indices[i] = (indices[i] + base).toShort()
    }

    private fun createOptimizedMesh(meshes: Set<MeshData>, copies: Int): Packed {
        val vertexParts = ArrayList<FloatArray>()
        val indexParts = ArrayList<ShortArray>()
        var runningVertices = 0
        var maxSkeletonId = 0

        for (mesh in meshes) {
            val sourceStride = mesh.vertexSize
            val count = if (sourceStride == 0) 0 else mesh.vertices.size / sourceStride
            val expanded = FloatArray(count * VERTEX_FLOATS)
            for (i in 0 until count) {
                val src = i * sourceStride
                val dst = i * VERTEX_FLOATS
                expanded[dst] = mesh.vertices[src]
                expanded[dst + 1] = mesh.vertices[src + 1]
                expanded[dst + 2] = mesh.vertices[src + 2]
                if (mesh.hasNorms) {
                    expanded[dst + 3] = mesh.vertices[src + 3]
                    expanded[dst + 4] = mesh.vertices[src + 4]
                    expanded[dst + 5] = mesh.vertices[src + 5]
                }
                if (mesh.hasUVs) {
                    val uv = src + 3 + (if (mesh.hasNorms) 3 else 0)
                    expanded[dst + 6] = mesh.vertices[uv]
                    expanded[dst + 7] = mesh.vertices[uv + 1]
                }
                if (mesh.hasSkeleton) {
                    val ske = src + 3 + (if (mesh.hasNorms) 3 else 0) + (if (mesh.hasUVs) 2 else 0)
                    expanded[dst + 8] = mesh.vertices[ske]
                    if (expanded[dst + 8] > maxSkeletonId) maxSkeletonId = expanded[dst + 8].toInt()
                }
            }

            val source = mesh.indices
            val shifted = ShortArray(source?.size ?: 0)
            if (source != null) {
                for (i in source.indices) shifted[i] = (source[i] + runningVertices).toShort()
            }
            runningVertices += count
            vertexParts.add(expanded)
            indexParts.add(shifted)
        }

        var vertices = concatFloats(vertexParts)
        var indices = concatShorts(indexParts)

        val reduced = deduplicate(vertices)
        vertices = reduced.vertices
        for (i in indices.indices) indices[i] = reduced.remap[indices[i].toInt()]

        if (copies > 1) {
            val baseVertices = vertices
            val baseIndices = indices
            val vertexCount = baseVertices.size / VERTEX_FLOATS
            vertices = FloatArray(baseVertices.size * copies)
            indices = ShortArray(baseIndices.size * copies)
            for (c in 0 until copies) {
                baseVertices.copyInto(vertices, c * baseVertices.size)
                for (v in 0 until vertexCount) {
                    vertices[c * baseVertices.size + v * VERTEX_FLOATS + 8] += (c * maxSkeletonId).toFloat()
                }
                baseIndices.copyInto(indices, c * baseIndices.size)
                for (i in baseIndices.indices) {
                    indices[c * baseIndices.size + i] = (baseIndices[i] + c * vertexCount).toShort()
                }
            }
        }
        return Packed(vertices, indices)
    }

    /** Uploads everything added so far into one VBO and one IBO. */
    fun optimize() {
        requireShortIndexable(totalVertices)
        val vertices = concatFloats(vertexChunks)
        val indices = concatShorts(indexChunks)

        vbo = gl.glGenBuffer()
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo)
        gl.glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW)

        ibo = gl.glGenBuffer()
        gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo)
        gl.glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW)

        uploaded = true
    }

    /** Total vertices across everything added; useful for asserting the short-index limit. */
    val totalVertices: Int get() = currentVertexCount()

    fun dispose() {
        if (vbo != 0) { gl.glDeleteBuffer(vbo); vbo = 0 }
        if (ibo != 0) { gl.glDeleteBuffer(ibo); ibo = 0 }
        uploaded = false
    }

    internal class Dedup(val vertices: FloatArray, val remap: ShortArray)

    internal companion object {
        const val VERTEX_FLOATS = 9

        /**
         * Distinct vertices addressable by a 16-bit index. GLES 2.0 only guarantees
         * GL_UNSIGNED_SHORT; 32-bit indices need OES_element_index_uint. Past this the
         * index rebasing silently wraps and the geometry turns to confetti, so every
         * place that could overflow fails loudly instead.
         */
        const val MAX_BATCH_VERTICES = Short.MAX_VALUE + 1

        fun requireShortIndexable(vertexCount: Int) {
            check(vertexCount <= MAX_BATCH_VERTICES) {
                "batch holds $vertexCount vertices, more than the $MAX_BATCH_VERTICES that " +
                    "short indices can address — split it across multiple MeshOptimized instances"
            }
        }

        fun concatFloats(parts: List<FloatArray>): FloatArray {
            val out = FloatArray(parts.sumOf { it.size })
            var at = 0
            for (p in parts) { p.copyInto(out, at); at += p.size }
            return out
        }

        fun concatShorts(parts: List<ShortArray>): ShortArray {
            val out = ShortArray(parts.sumOf { it.size })
            var at = 0
            for (p in parts) { p.copyInto(out, at); at += p.size }
            return out
        }

        /**
         * Collapses identical 9-float vertices, keeping first occurrences in ascending
         * order and returning the old→new index mapping. Hash-based, but ordered so the
         * result is identical to the original's O(n²) scan.
         */
        fun deduplicate(vertices: FloatArray): Dedup {
            val count = vertices.size / VERTEX_FLOATS
            requireShortIndexable(count)
            val remap = ShortArray(count)
            val seen = HashMap<VertexKey, Int>(count * 2)
            var unique = 0

            for (i in 0 until count) {
                val key = VertexKey(vertices, i * VERTEX_FLOATS)
                val existing = if (key.dedupable) seen[key] else null
                if (existing != null) {
                    remap[i] = existing.toShort()
                } else {
                    if (key.dedupable) seen[key] = unique
                    remap[i] = unique.toShort()
                    unique++
                }
            }

            // Write every vertex into its reduced slot, in ascending order, so a later
            // duplicate overwrites an earlier one. The original does the same, and it is
            // observable: -0.0f and 0.0f compare equal but are not the same bits, so
            // last-wins vs first-wins produce different buffers.
            val out = FloatArray(unique * VERTEX_FLOATS)
            for (i in 0 until count) {
                val from = i * VERTEX_FLOATS
                vertices.copyInto(out, remap[i].toInt() * VERTEX_FLOATS, from, from + VERTEX_FLOATS)
            }
            return Dedup(out, remap)
        }
    }

    /**
     * Hash key over one vertex. Normalises -0.0 to 0.0 because `==` treats them as equal
     * (the original compared with `==`), and refuses to de-duplicate vertices containing
     * NaN because `==` is false for NaN.
     */
    internal class VertexKey(source: FloatArray, offset: Int) {
        private val values = FloatArray(VERTEX_FLOATS)
        val dedupable: Boolean

        init {
            var ok = true
            for (i in 0 until VERTEX_FLOATS) {
                val v = source[offset + i]
                if (v.isNaN()) ok = false
                values[i] = if (v == 0f) 0f else v
            }
            dedupable = ok
        }

        override fun equals(other: Any?): Boolean =
            other is VertexKey && values.contentEquals(other.values)

        override fun hashCode(): Int = values.contentHashCode()
    }
}

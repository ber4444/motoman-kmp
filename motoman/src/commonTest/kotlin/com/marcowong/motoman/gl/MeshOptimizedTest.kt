package com.marcowong.motoman.gl

import com.marcowong.motoman.model.MaterialData
import com.marcowong.motoman.model.MeshData
import com.marcowong.motoman.model.ModelData
import com.marcowong.motoman.model.SubMeshData
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The hash-based de-duplication replaced the original's O(n²) scan. These tests pin it
 * against a literal transcription of that scan, because the two must agree exactly —
 * the reduced vertex buffer and the index remapping both feed straight into the GPU.
 */
class MeshOptimizedDedupTest {

    /** Literal port of the original algorithm, used only as a test oracle. */
    private fun referenceDedup(vertices: FloatArray): Pair<FloatArray, ShortArray> {
        val stride = MeshOptimized.VERTEX_FLOATS
        val n = vertices.size / stride
        val mapping = IntArray(n) { it }
        val reduced = ShortArray(n)
        var kept = 0
        var nextNewIndex = 0
        for (i in 0 until n) {
            if (mapping[i] == i) {
                kept++
                reduced[i] = (nextNewIndex++).toShort()
                for (j in i + 1 until n) {
                    if (mapping[j] != j) continue
                    var equal = true
                    for (k in 0 until stride) {
                        if (vertices[i * stride + k] != vertices[j * stride + k]) { equal = false; break }
                    }
                    if (equal) {
                        mapping[j] = i
                        reduced[j] = reduced[i]
                    }
                }
            }
        }
        val out = FloatArray(kept * stride)
        for (i in 0 until n) {
            val dst = reduced[i].toInt() * stride
            for (k in 0 until stride) out[dst + k] = vertices[i * stride + k]
        }
        return out to reduced
    }

    private fun assertMatchesReference(vertices: FloatArray) {
        val (refVerts, refRemap) = referenceDedup(vertices.copyOf())
        val actual = MeshOptimized.deduplicate(vertices.copyOf())
        assertContentEquals(refRemap, actual.remap, "index remapping diverged")
        assertContentEquals(refVerts, actual.vertices, "reduced vertex buffer diverged")
    }

    @Test
    fun matchesReferenceOnDuplicateHeavyData() {
        // Few distinct vertices repeated a lot — the case dedup exists for.
        val random = Random(1234)
        val distinct = Array(5) { FloatArray(9) { random.nextInt(-3, 3).toFloat() } }
        val vertices = FloatArray(60 * 9)
        for (i in 0 until 60) {
            distinct[random.nextInt(distinct.size)].copyInto(vertices, i * 9)
        }
        assertMatchesReference(vertices)
    }

    @Test
    fun matchesReferenceOnAllUniqueData() {
        val random = Random(99)
        val vertices = FloatArray(40 * 9) { random.nextFloat() }
        assertMatchesReference(vertices)
    }

    @Test
    fun matchesReferenceAcrossManySeeds() {
        for (seed in 0 until 25) {
            val random = Random(seed)
            val n = random.nextInt(1, 30)
            val palette = Array(random.nextInt(1, 6)) { FloatArray(9) { random.nextInt(-2, 2).toFloat() } }
            val vertices = FloatArray(n * 9)
            for (i in 0 until n) palette[random.nextInt(palette.size)].copyInto(vertices, i * 9)
            assertMatchesReference(vertices)
        }
    }

    @Test
    fun treatsNegativeZeroAsEqualToZeroLikeTheOriginal() {
        // `==` says -0.0f == 0.0f, so these must collapse to one vertex.
        val vertices = FloatArray(18)
        vertices[0] = -0.0f
        vertices[9] = 0.0f
        assertMatchesReference(vertices)
        assertEquals(9, MeshOptimized.deduplicate(vertices).vertices.size, "should collapse to one vertex")
    }

    @Test
    fun neverCollapsesNaNVerticesBecauseEqualityIsFalse() {
        val vertices = FloatArray(18)
        vertices[0] = Float.NaN
        vertices[9] = Float.NaN
        val result = MeshOptimized.deduplicate(vertices)
        assertEquals(18, result.vertices.size, "NaN vertices must stay distinct")
        assertEquals(0, result.remap[0].toInt())
        assertEquals(1, result.remap[1].toInt())
    }

    @Test
    fun keepsFirstOccurrenceOrder() {
        val vertices = FloatArray(4 * 9)
        FloatArray(9) { 7f }.copyInto(vertices, 0)       // A
        FloatArray(9) { 3f }.copyInto(vertices, 9)       // B
        FloatArray(9) { 7f }.copyInto(vertices, 18)      // A again
        FloatArray(9) { 1f }.copyInto(vertices, 27)      // C
        val result = MeshOptimized.deduplicate(vertices)
        assertContentEquals(shortArrayOf(0, 1, 0, 2), result.remap)
        assertEquals(3 * 9, result.vertices.size)
        assertEquals(7f, result.vertices[0])
        assertEquals(3f, result.vertices[9])
        assertEquals(1f, result.vertices[18])
    }
}

class MeshOptimizedBatchTest {

    private fun quad(material: String, hasNorms: Boolean = true, hasUVs: Boolean = true): SubMeshData {
        val stride = 3 + (if (hasNorms) 3 else 0) + (if (hasUVs) 2 else 0)
        val verts = FloatArray(4 * stride) { (it % 7).toFloat() }
        return SubMeshData().also { sub ->
            sub.name = material
            sub.material = MaterialData(material)
            sub.mesh = MeshData().also {
                it.vertices = verts
                it.indices = shortArrayOf(0, 1, 2, 0, 2, 3)
                it.hasNorms = hasNorms
                it.hasUVs = hasUVs
            }
        }
    }

    private fun model(vararg subs: SubMeshData) = ModelData().also { it.subMeshes = arrayOf(*subs) }

    @Test
    fun expandsEveryVertexToTheFixedNineFloatLayout() {
        val gl = FakeGl()
        val batch = MeshOptimized(gl)
        // Position-only source must still occupy 9 floats per vertex in the batch.
        batch.add(model(quad("m", hasNorms = false, hasUVs = false)))
        batch.optimize()
        val upload = gl.calls.first { it.startsWith("glBufferData($GL_ARRAY_BUFFER,f") }
        val floats = upload.substringAfter(",f").substringBefore(',').toInt()
        assertEquals(0, floats % MeshOptimized.VERTEX_FLOATS)
        assertEquals(batch.totalVertices * MeshOptimized.VERTEX_FLOATS, floats)
    }

    @Test
    fun groupsSubMeshesByMaterialIntoOneDrawEach() {
        val gl = FakeGl()
        val batch = MeshOptimized(gl)
        val context = batch.add(model(quad("red"), quad("blue"), quad("red")))
        batch.optimize()
        gl.clear()
        context.render(FakeShader(gl))
        // Two materials -> two draw calls, not three sub-meshes' worth.
        assertEquals(2, gl.countOf("glDrawElements"))
    }

    @Test
    fun uploadsASingleSharedVertexAndIndexBuffer() {
        val gl = FakeGl()
        val batch = MeshOptimized(gl)
        batch.add(model(quad("a")))
        batch.add(model(quad("b")))
        batch.optimize()
        assertEquals(2, gl.countOf("glGenBuffer"), "one VBO and one IBO for the whole batch")
    }

    @Test
    fun laterBatchesAreRebasedOntoEarlierVertices() {
        val gl = FakeGl()
        val batch = MeshOptimized(gl)
        batch.add(model(quad("a")))
        val firstCount = batch.totalVertices
        batch.add(model(quad("b")))
        assertTrue(batch.totalVertices > firstCount, "second add must extend the shared buffer")
    }

    @Test
    fun copiesReplicateGeometry() {
        val gl = FakeGl()
        val single = MeshOptimized(gl).also { it.add(model(quad("a")), 1) }
        val quadrupled = MeshOptimized(FakeGl()).also { it.add(model(quad("a")), 4) }
        assertEquals(single.totalVertices * 4, quadrupled.totalVertices)
    }

    @Test
    fun renderBeforeOptimizeFails() {
        val gl = FakeGl()
        val batch = MeshOptimized(gl)
        val context = batch.add(model(quad("a")))
        var threw = false
        try {
            context.render(FakeShader(gl))
        } catch (_: IllegalStateException) {
            threw = true
        }
        assertTrue(threw, "rendering before optimize() should fail loudly")
    }
}

/** A ShaderProgram over FakeGl; compile/link succeed so attribute plumbing can be exercised. */
private fun FakeShader(gl: FakeGl): ShaderProgram =
    ShaderProgram(gl, "void main(){}", "void main(){}", ShaderPreprocessor(GlslTarget.ES_100))

class MeshOptimizedIndexLimitTest {

    /** One sub-mesh with [vertexCount] unique vertices and no indices. */
    private fun bigModel(vertexCount: Int): ModelData {
        val verts = FloatArray(vertexCount * 3) { it.toFloat() } // all distinct -> no dedup
        return ModelData().also { model ->
            model.subMeshes = arrayOf(
                SubMeshData().also { sub ->
                    sub.material = MaterialData("m")
                    sub.mesh = MeshData().also {
                        it.vertices = verts
                        it.indices = ShortArray(0)
                    }
                },
            )
        }
    }

    @Test
    fun rejectsBatchesTooLargeForShortIndices() {
        val batch = MeshOptimized(FakeGl())
        var message: String? = null
        try {
            // Fails at add() rather than optimize(): that is where the short remap is
            // built, so the overflow is caught before any wrapped data exists.
            batch.add(bigModel(Short.MAX_VALUE + 10))
            batch.optimize()
        } catch (e: IllegalStateException) {
            message = e.message
        }
        assertTrue(message != null, "over-large batch must fail loudly, not wrap silently")
        assertTrue(message!!.contains("short indices"), "error should explain the cause: $message")
    }

    @Test
    fun acceptsBatchesWithinTheShortIndexLimit() {
        val batch = MeshOptimized(FakeGl())
        batch.add(bigModel(1000))
        batch.optimize() // must not throw
        assertEquals(1000, batch.totalVertices)
    }
}

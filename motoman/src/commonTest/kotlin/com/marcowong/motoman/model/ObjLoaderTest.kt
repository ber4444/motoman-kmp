package com.marcowong.motoman.model

import com.marcowong.motoman.assets.Assets
import com.marcowong.motoman.assets.AssetNotFoundException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** In-memory [Assets] so the loader can be exercised with no filesystem. */
private class MapAssets(private val entries: Map<String, String>) : Assets {
    override fun exists(path: String) = entries.containsKey(path)
    override fun readBytes(path: String) = readText(path).encodeToByteArray()
    override fun readText(path: String) = entries[path] ?: throw AssetNotFoundException(path)
}

class ObjLoaderTest {

    private fun load(obj: String, extra: Map<String, String> = emptyMap()): ModelData? {
        val assets = MapAssets(mapOf("data/m.obj" to obj) + extra)
        return ObjLoader(assets).loadObj("data/m.obj")
    }

    @Test
    fun loadsSingleTriangle() {
        val model = load(
            """
            v 0 0 0
            v 1 0 0
            v 0 1 0
            f 1 2 3
            """.trimIndent(),
        )
        assertNotNull(model)
        assertEquals(1, model.subMeshes.size)
        val mesh = model.subMeshes[0].mesh!!
        assertEquals(3, mesh.numVertices)
        assertEquals(3, mesh.indices!!.size)
        assertEquals(3, mesh.vertexSize) // position only
        assertTrue(!mesh.hasNorms && !mesh.hasUVs)
        assertEquals(floatArrayOf(0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f, 0f).toList(), mesh.vertices.toList())
    }

    @Test
    fun triangulatesQuadAsFan() {
        // A quad must become two triangles: (1,2,3) and (1,3,4) — 6 vertices.
        val model = load(
            """
            v 0 0 0
            v 1 0 0
            v 1 1 0
            v 0 1 0
            f 1 2 3 4
            """.trimIndent(),
        )!!
        val mesh = model.subMeshes[0].mesh!!
        assertEquals(6, mesh.numVertices, "quad should fan into 2 triangles")
        // Second triangle starts at vertex 1 again, then 3, then 4.
        val v = mesh.vertices
        assertEquals(0f, v[9]); assertEquals(0f, v[10]) // tri2 v0 == quad v1 (0,0,0)
        assertEquals(1f, v[12]); assertEquals(1f, v[13]) // tri2 v1 == quad v3 (1,1,0)
        assertEquals(0f, v[15]); assertEquals(1f, v[16]) // tri2 v2 == quad v4 (0,1,0)
    }

    @Test
    fun parsesNormalsAndUvs() {
        val model = load(
            """
            v 0 0 0
            v 1 0 0
            v 0 1 0
            vn 0 0 1
            vt 0.25 0.75
            f 1/1/1 2/1/1 3/1/1
            """.trimIndent(),
        )!!
        val mesh = model.subMeshes[0].mesh!!
        assertTrue(mesh.hasNorms, "normals should be detected")
        assertTrue(mesh.hasUVs, "uvs should be detected")
        assertEquals(8, mesh.vertexSize) // 3 pos + 3 norm + 2 uv
        assertEquals(3, mesh.numVertices)
        // First vertex: pos(0,0,0) norm(0,0,1) uv(0.25,0.75)
        assertEquals(1f, mesh.vertices[5], "normal z")
        assertEquals(0.25f, mesh.vertices[6], "u")
        assertEquals(0.75f, mesh.vertices[7], "v")
    }

    @Test
    fun flipVInvertsTextureV() {
        val assets = MapAssets(
            mapOf(
                "data/m.obj" to """
                    v 0 0 0
                    v 1 0 0
                    v 0 1 0
                    vt 0.25 0.75
                    f 1/1 2/1 3/1
                """.trimIndent(),
            ),
        )
        val mesh = ObjLoader(assets).loadObj("data/m.obj", flipV = true)!!.subMeshes[0].mesh!!
        assertEquals(0.25f, mesh.vertices[3], "u unchanged")
        assertEquals(0.25f, mesh.vertices[4], "v flipped to 1-0.75")
    }

    @Test
    fun splitsGroupsIntoSubMeshes() {
        val model = load(
            """
            v 0 0 0
            v 1 0 0
            v 0 1 0
            g alpha
            f 1 2 3
            g beta
            f 1 2 3
            """.trimIndent(),
        )!!
        assertEquals(2, model.subMeshes.size)
        assertEquals("alpha", model.subMeshes[0].name)
        assertEquals("beta", model.subMeshes[1].name)
    }

    @Test
    fun dropsGroupsWithNoFaces() {
        val model = load(
            """
            v 0 0 0
            v 1 0 0
            v 0 1 0
            g empty
            g real
            f 1 2 3
            """.trimIndent(),
        )!!
        assertEquals(1, model.subMeshes.size)
        assertEquals("real", model.subMeshes[0].name)
    }

    @Test
    fun returnsNullWhenNoFacesAtAll() {
        assertNull(load("v 0 0 0\nv 1 0 0\n"))
    }

    @Test
    fun ignoresCommentsAndBlankLines() {
        val model = load(
            """
            # a comment

            v 0 0 0
            v 1 0 0
            v 0 1 0
            # another
            f 1 2 3
            """.trimIndent(),
        )!!
        assertEquals(3, model.subMeshes[0].mesh!!.numVertices)
    }

    @Test
    fun resolvesMaterialFromMtlLib() {
        val model = load(
            """
            mtllib m.mtl
            v 0 0 0
            v 1 0 0
            v 0 1 0
            usemtl paint
            f 1 2 3
            """.trimIndent(),
            extra = mapOf(
                "data/m.mtl" to """
                    newmtl paint
                    Kd 0.5 0.25 0.125
                    Ks 1 0 0
                    map_Kd bike.png
                """.trimIndent(),
            ),
        )!!
        val material = model.subMeshes[0].material!!
        assertEquals("paint", material.name)
        assertEquals(0.5f, material.diffuseColor.r)
        assertEquals(0.25f, material.diffuseColor.g)
        assertEquals(0.125f, material.diffuseColor.b)
        assertEquals(1f, material.specularColor.r)
        // Texture name is resolved relative to the mtl, and left for the renderer to load.
        assertEquals("data/bike.png", material.diffuseTextureName)
        assertNull(material.diffuseTexture, "loader must not create GL textures")
    }

    @Test
    fun materialNameDotsBecomeUnderscores() {
        val model = load(
            """
            mtllib m.mtl
            v 0 0 0
            v 1 0 0
            v 0 1 0
            usemtl bike.body
            f 1 2 3
            """.trimIndent(),
            extra = mapOf("data/m.mtl" to "newmtl bike.body\nKd 1 0 0"),
        )!!
        assertEquals("bike_body", model.subMeshes[0].material!!.name)
    }

    @Test
    fun unknownMaterialFallsBackToDefault() {
        val model = load(
            """
            v 0 0 0
            v 1 0 0
            v 0 1 0
            usemtl nope
            f 1 2 3
            """.trimIndent(),
        )!!
        assertEquals("default", model.subMeshes[0].material!!.name)
    }

    @Test
    fun negativeIndicesAreRelative() {
        // -1 refers to the most recent vertex. Faithful to the original's arithmetic.
        val model = load(
            """
            v 0 0 0
            v 1 0 0
            v 0 1 0
            f -3 -2 -1
            """.trimIndent(),
        )
        assertNotNull(model)
        assertEquals(3, model.subMeshes[0].mesh!!.numVertices)
    }
}

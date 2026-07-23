package com.marcowong.motoman.model

import com.marcowong.motoman.assets.ClasspathAssets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Loader parity, per the verification plan: every shipped model is loaded from the real
 * assets and its sub-mesh / vertex / index counts are pinned.
 *
 * Honest caveat on provenance: these numbers are a snapshot of this Kotlin port, not
 * values dumped from the original 0.9.8 build (that build is gone, and the surviving
 * gdx.jar cannot run here — Matrix4.prj is a JNI native with Windows-only binaries).
 * The port was written to be behaviourally faithful, including the fan triangulation and
 * the original's index arithmetic. So this guards against *drift*, which is what the
 * plan wants it for; it is not an independent oracle for the original.
 */
class ObjLoaderParityTest {

    // model -> (subMeshes, vertices, indices)
    private val baseline = mapOf(
        "airplane.obj" to Triple(1, 87, 87),
        "bike.obj" to Triple(10, 3078, 3078),
        "bikeBody.obj" to Triple(8, 2046, 2046),
        "bikeBody_lowpoly.obj" to Triple(8, 1074, 1074),
        "bikeFrontWheel.obj" to Triple(1, 468, 468),
        "bikeFrontWheel_lowpoly.obj" to Triple(1, 132, 132),
        "bikeRearWheel.obj" to Triple(1, 564, 564),
        "bikeRearWheel_lowpoly.obj" to Triple(1, 228, 228),
        "bikeShadow.obj" to Triple(1, 6, 6),
        "building.obj" to Triple(1, 18, 18),
        "buildings.obj" to Triple(1, 48, 48),
        "hills.obj" to Triple(1, 324, 324),
        "lamp.obj" to Triple(1, 21, 21),
        "rider.obj" to Triple(1, 798, 798),
        "skybox.obj" to Triple(5, 30, 30),
        "test.obj" to Triple(1, 1680, 1680),
        "tile.obj" to Triple(1, 6, 6),
        "track.obj" to Triple(1, 18, 18),
    )

    private val assets = ClasspathAssets()
    private val loader = ObjLoader(assets)

    @Test
    fun everyShippedModelMatchesTheBaseline() {
        for ((name, expected) in baseline) {
            val path = "data/$name"
            assertTrue(assets.exists(path), "$name missing from the desktop classpath")
            val model = loader.loadObj(path)
            assertNotNull(model, "$name failed to load")

            val (subMeshes, vertices, indices) = expected
            assertEquals(subMeshes, model.subMeshes.size, "$name sub-mesh count")
            assertEquals(vertices, model.subMeshes.sumOf { it.mesh!!.numVertices }, "$name vertex count")
            assertEquals(indices, model.subMeshes.sumOf { it.mesh!!.indices?.size ?: 0 }, "$name index count")
        }
    }

    @Test
    fun everyShippedModelHasCoherentVertexLayout() {
        for (name in baseline.keys) {
            val model = loader.loadObj("data/$name")!!
            for (sub in model.subMeshes) {
                val mesh = sub.mesh!!
                assertTrue(mesh.vertexSize > 0, "$name/${sub.name} has no vertex layout")
                assertEquals(
                    0, mesh.vertices.size % mesh.vertexSize,
                    "$name/${sub.name} vertex buffer is not a whole number of vertices",
                )
                // The loader emits unshared vertices, so indices are 1:1 with vertices
                // whenever they fit in a short.
                mesh.indices?.let {
                    assertEquals(mesh.numVertices, it.size, "$name/${sub.name} index/vertex mismatch")
                }
            }
        }
    }

    @Test
    fun materialsResolveToRealTextureAssets() {
        // Every map_Kd the models reference must exist — this is what caught the ETC1
        // materials pointing at deleted files.
        var checked = 0
        for (name in baseline.keys) {
            val model = loader.loadObj("data/$name")!!
            for (sub in model.subMeshes) {
                val texture = sub.material?.diffuseTextureName ?: continue
                assertTrue(assets.exists(texture), "$name references missing texture $texture")
                checked++
            }
        }
        assertTrue(checked > 0, "expected at least one textured material")
    }
}

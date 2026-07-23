package com.marcowong.motoman.track.math

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 3a-1 math parity. Golden values are hand-derived from the libGDX algorithms these
 * types port, so the tests are an independent oracle rather than a restatement of the
 * implementation. Kept platform-agnostic (no native gdx dependency).
 */
class MathTypesTest {

    private fun identity() = Matrix4()

    @Test
    fun prjThroughIdentityLeavesPointsUnchanged() {
        val pts = floatArrayOf(1f, 2f, 3f, -4f, 5f, -6f)
        Matrix4.prj(identity().`val`, pts, 0, 2, 3)
        assertEquals(1f, pts[0]); assertEquals(2f, pts[1]); assertEquals(3f, pts[2])
        assertEquals(-4f, pts[3]); assertEquals(5f, pts[4]); assertEquals(-6f, pts[5])
    }

    @Test
    fun prjAppliesPerspectiveDivide() {
        // Column-major matrix: scale xyz by 2 (M00=M11=M22=2) and set w = 2*z + 1 via M32.
        val m = identity()
        m.`val`[Matrix4.M00] = 2f
        m.`val`[Matrix4.M11] = 2f
        m.`val`[Matrix4.M22] = 2f
        m.`val`[Matrix4.M32] = 2f // w = 2*z + 1
        val pts = floatArrayOf(1f, 1f, 1f)
        Matrix4.prj(m.`val`, pts, 0, 1, 3)
        // w = 2*1 + 1 = 3; each component = 2*coord / 3
        val expected = 2f / 3f
        assertTrue(kotlin.math.abs(pts[0] - expected) < 1e-6f, "x=${pts[0]}")
        assertTrue(kotlin.math.abs(pts[1] - expected) < 1e-6f, "y=${pts[1]}")
        assertTrue(kotlin.math.abs(pts[2] - expected) < 1e-6f, "z=${pts[2]}")
    }

    @Test
    fun planeFromCcwPointsFacesPositiveZ() {
        // Three points on the z=0 plane, wound CCW as seen from +z.
        val p = Plane().set(
            Vector3(0f, 0f, 0f),
            Vector3(1f, 0f, 0f),
            Vector3(1f, 1f, 0f),
        )
        // normal = ((p1-p2) x (p2-p3)).nor() = ((-1,0,0) x (0,-1,0)) = (0,0,1)
        assertTrue(kotlin.math.abs(p.normal.x) < 1e-6f)
        assertTrue(kotlin.math.abs(p.normal.y) < 1e-6f)
        assertEquals(1f, p.normal.z, "normal.z")
        assertTrue(kotlin.math.abs(p.d) < 1e-6f, "d=${p.d}")
        assertEquals(Plane.PlaneSide.Front, p.testPoint(Vector3(0f, 0f, 5f)))
        assertEquals(Plane.PlaneSide.Back, p.testPoint(Vector3(0f, 0f, -5f)))
    }

    @Test
    fun frustumFromIdentityYieldsClipCubeCorners() {
        val f = Frustum()
        f.update(identity())
        // With an identity inverse-projection-view, corners equal the clip-space cube.
        assertEquals(-1f, f.planePoints[0].x); assertEquals(-1f, f.planePoints[0].y); assertEquals(-1f, f.planePoints[0].z)
        assertEquals(1f, f.planePoints[6].x); assertEquals(1f, f.planePoints[6].y); assertEquals(1f, f.planePoints[6].z)
        // Every derived plane must be well-formed (unit normal), independent of orientation.
        for ((i, plane) in f.planes.withIndex()) {
            assertTrue(kotlin.math.abs(plane.normal.len() - 1f) < 1e-6f, "plane[$i] normal not unit: ${plane.normal.len()}")
        }
    }

    @Test
    fun pointInFrustumRespectsPlanes() {
        // A real (non-degenerate) frustum: the origin sits inside a unit cube whose planes
        // face inward. Build it directly so the test does not depend on projection math.
        val f = object : Frustum() {}
        // Manually place the six planes of the axis-aligned [-1,1]^3 box, normals inward.
        f.planes[0].set(0f, 0f, 1f, 1f)   // z >= -1
        f.planes[1].set(0f, 0f, -1f, 1f)  // z <=  1
        f.planes[2].set(1f, 0f, 0f, 1f)   // x >= -1
        f.planes[3].set(-1f, 0f, 0f, 1f)  // x <=  1
        f.planes[4].set(0f, 1f, 0f, 1f)   // y >= -1
        f.planes[5].set(0f, -1f, 0f, 1f)  // y <=  1
        assertTrue(f.pointInFrustum(Vector3(0f, 0f, 0f)), "origin should be inside")
        assertTrue(!f.pointInFrustum(Vector3(5f, 0f, 0f)), "far point should be outside")
    }

    @Test
    fun colorPacksLikeLibGdxWhite() {
        // White (1,1,1,1): ABGR8888 = 0xFFFFFFFF, top alpha bit cleared -> 0xFEFFFFFF.
        val bits = Color(1f, 1f, 1f, 1f).toFloatBits().toRawBits()
        assertEquals(0xFEFFFFFF.toInt(), bits)
    }

    @Test
    fun colorPacksLikeLibGdxRed() {
        // Red (1,0,0,1): 0xFF0000FF, top alpha bit cleared -> 0xFE0000FF.
        val bits = Color(1f, 0f, 0f, 1f).toFloatBits().toRawBits()
        assertEquals(0xFE0000FF.toInt(), bits)
    }

    @Test
    fun projectionMatrixMatchesLibGdxFormula() {
        val m = Matrix4().setToProjection(1f, 100f, 67f, 1f)
        // Depth terms are rational and exact regardless of the fov transcendental.
        assertTrue(kotlin.math.abs(m.`val`[Matrix4.M22] - (101f / -99f)) < 1e-5f, "M22=${m.`val`[Matrix4.M22]}")
        assertTrue(kotlin.math.abs(m.`val`[Matrix4.M23] - (200f / -99f)) < 1e-5f, "M23=${m.`val`[Matrix4.M23]}")
        assertEquals(-1f, m.`val`[Matrix4.M32], "M32")
        assertEquals(0f, m.`val`[Matrix4.M33], "M33")
        // Square aspect -> equal x/y scale.
        assertTrue(kotlin.math.abs(m.`val`[Matrix4.M00] - m.`val`[Matrix4.M11]) < 1e-6f)
    }

    @Test
    fun invertRoundTripsToIdentity() {
        val m = Matrix4().setToProjection(1f, 100f, 67f, 1.333f)
        val original = m.`val`.copyOf()
        val product = Matrix4().set(m).mul(Matrix4().set(m).inv())
        for (i in 0 until 16) {
            val expected = if (i % 5 == 0) 1f else 0f // identity has 1s on the diagonal (indices 0,5,10,15)
            assertTrue(kotlin.math.abs(product.`val`[i] - expected) < 1e-4f, "M*M^-1[$i]=${product.`val`[i]}")
        }
        // inv() must not have mutated the source we copied.
        assertTrue(original.contentEquals(m.`val`))
    }

    @Test
    fun perspectiveCameraProjectsPointAhead() {
        val cam = PerspectiveCamera(67f, 800f, 600f)
        // Default: at origin looking down -z. A point 10 ahead should land near NDC centre.
        val p = floatArrayOf(0f, 0f, -10f)
        Matrix4.prj(cam.combined.`val`, p, 0, 1, 3)
        assertTrue(kotlin.math.abs(p[0]) < 1e-4f, "ndc.x=${p[0]}")
        assertTrue(kotlin.math.abs(p[1]) < 1e-4f, "ndc.y=${p[1]}")
        assertTrue(p[2] > -1f && p[2] < 1f, "ndc.z=${p[2]} should be within clip range")
    }

    @Test
    fun cameraLookAtSetsDirection() {
        val cam = PerspectiveCamera(67f, 800f, 600f)
        cam.lookAt(10f, 0f, 0f)
        assertTrue(kotlin.math.abs(cam.direction.x - 1f) < 1e-6f, "dir.x=${cam.direction.x}")
        assertTrue(kotlin.math.abs(cam.direction.y) < 1e-6f)
        assertTrue(kotlin.math.abs(cam.direction.z) < 1e-6f)
        // up stays unit and orthogonal to direction.
        assertTrue(kotlin.math.abs(cam.up.len() - 1f) < 1e-6f)
        assertTrue(kotlin.math.abs(cam.up.dot(cam.direction)) < 1e-6f)
    }
}

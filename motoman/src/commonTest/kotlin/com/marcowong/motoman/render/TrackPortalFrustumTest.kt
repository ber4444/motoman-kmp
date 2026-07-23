package com.marcowong.motoman.render

import com.marcowong.motoman.track.math.Matrix4
import com.marcowong.motoman.track.math.PerspectiveCamera
import com.marcowong.motoman.track.math.Vector3
import kotlin.test.Test
import kotlin.test.assertTrue

class TrackPortalFrustumTest {

    /** Camera at the origin looking down -z, so world -z is "ahead". */
    private fun camera(): PerspectiveCamera = PerspectiveCamera(90f, 100f, 100f).also {
        it.near = 1f
        it.far = 100f
        it.position.set(0f, 0f, 0f)
        it.up.set(0f, 1f, 0f)
        it.lookAt(0f, 0f, -1f)
        it.update()
    }

    /** A quad at z=-10 spanning [-half, half] in x and y. */
    private fun portalQuad(half: Float) = floatArrayOf(
        -half, -half, -10f,
        half, -half, -10f,
        half, half, -10f,
        -half, half, -10f,
    )

    private fun ndcOf(camera: PerspectiveCamera, x: Float, y: Float, z: Float): Pair<Float, Float> {
        val p = floatArrayOf(x, y, z)
        Matrix4.prj(camera.combined.`val`, p, 0, 1, 3)
        return p[0] to p[1]
    }

    @Test
    fun planesAreWellFormedAfterNarrowing() {
        val cam = camera()
        val portal = TrackPortalFrustum()
        portal.update(cam, cam.frustum, portalQuad(2f))
        for ((i, plane) in portal.planes.withIndex()) {
            assertTrue(
                kotlin.math.abs(plane.normal.len() - 1f) < 1e-4f,
                "plane[$i] normal should be unit, was ${plane.normal.len()}",
            )
        }
    }

    @Test
    fun narrowPortalCullsWhatTheFullFrustumWouldKeep() {
        val cam = camera()

        // A point off to the side but still well inside the 90-degree camera frustum.
        val offToTheSide = Vector3(4f, 0f, -10f)
        val (ndcX, _) = ndcOf(cam, offToTheSide.x, offToTheSide.y, offToTheSide.z)
        assertTrue(kotlin.math.abs(ndcX) < 1f, "test point must be inside the camera frustum (ndc.x=$ndcX)")
        assertTrue(cam.frustum.pointInFrustum(offToTheSide), "camera frustum should contain it")

        // Narrow to a small portal straight ahead; the side point is now behind a wall.
        val portal = TrackPortalFrustum()
        portal.update(cam, cam.frustum, portalQuad(1f))
        assertTrue(!portal.pointInFrustum(offToTheSide), "portal should cull the off-axis point")
    }

    @Test
    fun portalStillContainsWhatIsVisibleThroughIt() {
        val cam = camera()
        val portal = TrackPortalFrustum()
        portal.update(cam, cam.frustum, portalQuad(1f))
        // Dead centre, at the portal's own depth, must survive.
        assertTrue(portal.pointInFrustum(Vector3(0f, 0f, -10f)))
    }

    @Test
    fun portalWiderThanTheFrustumIsClampedToIt() {
        val cam = camera()
        val portal = TrackPortalFrustum()
        // A portal far wider than the view cannot widen the frustum beyond it.
        portal.update(cam, cam.frustum, portalQuad(500f))
        val onAxis = Vector3(0f, 0f, -10f)
        assertTrue(portal.pointInFrustum(onAxis))
        for ((i, plane) in portal.planes.withIndex()) {
            assertTrue(
                kotlin.math.abs(plane.normal.len() - 1f) < 1e-4f,
                "plane[$i] should stay well-formed when clamped",
            )
        }
    }

    @Test
    fun projectsThePortalPointsInPlace() {
        val cam = camera()
        val points = portalQuad(2f)
        val before = points.copyOf()
        TrackPortalFrustum().update(cam, cam.frustum, points)
        assertTrue(!before.contentEquals(points), "points are projected in place, as in the original")
    }
}

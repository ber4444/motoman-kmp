package com.marcowong.motoman.render

import com.marcowong.motoman.track.math.Camera
import com.marcowong.motoman.track.math.Frustum
import com.marcowong.motoman.track.math.Matrix4

/**
 * A [Frustum] narrowed to a track portal. Port of the engine's `TrackPortalFrustum`.
 *
 * Given the screen-space bounds of a portal (a track segment's opening) and the frustum
 * being looked through, this intersects the two rectangles in clip space and rebuilds the
 * six planes from the result — so anything not visible *through the opening* is culled,
 * not merely anything outside the camera frustum.
 */
class TrackPortalFrustum : Frustum() {

    private val scratch = FloatArray(24)

    /**
     * Narrows this frustum to the portal described by [points] as seen by [camera],
     * clipped against [through].
     *
     * @param points flat xyz world-space portal corners. **Mutated in place** (projected
     *   into clip space), matching the original's behaviour.
     */
    fun update(camera: Camera, through: Frustum, points: FloatArray) {
        Matrix4.prj(camera.combined.`val`, points, 0, points.size / 3, 3)

        var minX = 1f
        var minY = 1f
        var maxX = -1f
        var maxY = -1f
        var i = 0
        while (i < points.size) {
            val x = points[i]
            val y = points[i + 1]
            if (x < minX) minX = x
            if (y < minY) minY = y
            if (x > maxX) maxX = x
            if (y > maxY) maxY = y
            i += 3
        }

        // Project the frustum we are looking through, and keep the overlap.
        var j = 0
        for (k in 0 until 8) {
            scratch[j] = through.planePoints[k].x
            scratch[j + 1] = through.planePoints[k].y
            scratch[j + 2] = through.planePoints[k].z
            j += 3
        }
        Matrix4.prj(camera.combined.`val`, scratch, 0, 8, 3)

        var minX2 = 1f
        var minY2 = 1f
        var maxX2 = -1f
        var maxY2 = -1f
        i = 0
        while (i < 24) {
            val x = scratch[i]
            val y = scratch[i + 1]
            if (x < minX2) minX2 = x
            if (y < minY2) minY2 = y
            if (x > maxX2) maxX2 = x
            if (y > maxY2) maxY2 = y
            i += 3
        }
        if (minX2 > minX) minX = minX2
        if (maxX2 < maxX) maxX = maxX2
        if (minY2 > minY) minY = minY2
        if (maxY2 < maxY) maxY = maxY2

        if (minX < -1f) minX = -1f
        if (minY < -1f) minY = -1f
        if (maxX > 1f) maxX = 1f
        if (maxY > 1f) maxY = 1f
        // Degenerate overlap collapses to a line rather than inverting.
        if (minX > maxX) { minX = (minX + maxX) * 0.5f; maxX = minX }
        if (minY > maxY) { minY = (minY + maxY) * 0.5f; maxY = minY }

        planePointsArray[0] = minX; planePointsArray[1] = minY; planePointsArray[2] = -1f
        planePointsArray[3] = maxX; planePointsArray[4] = minY; planePointsArray[5] = -1f
        planePointsArray[6] = maxX; planePointsArray[7] = maxY; planePointsArray[8] = -1f
        planePointsArray[9] = minX; planePointsArray[10] = maxY; planePointsArray[11] = -1f
        planePointsArray[12] = minX; planePointsArray[13] = minY; planePointsArray[14] = 1f
        planePointsArray[15] = maxX; planePointsArray[16] = minY; planePointsArray[17] = 1f
        planePointsArray[18] = maxX; planePointsArray[19] = maxY; planePointsArray[20] = 1f
        planePointsArray[21] = minX; planePointsArray[22] = maxY; planePointsArray[23] = 1f

        Matrix4.prj(camera.invProjectionView.`val`, planePointsArray, 0, 8, 3)
        var p = 0
        for (k in 0 until 8) {
            planePoints[k].set(planePointsArray[p], planePointsArray[p + 1], planePointsArray[p + 2])
            p += 3
        }
        rebuildPlanes()
    }
}

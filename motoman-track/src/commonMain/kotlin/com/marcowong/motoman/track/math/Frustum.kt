package com.marcowong.motoman.track.math

/**
 * A camera view frustum as six planes derived from its eight corner points.
 * Port of libGDX `Frustum`. The [TrackPortalFrustum][com.marcowong.motoman] engine
 * subclass narrows these planes to a track portal; the plane winding here matches the
 * indices that subclass expects.
 */
open class Frustum {
    /** The eight clip-space corners, transformed to world space by [update]. */
    @JvmField val planePoints: Array<Vector3> = Array(8) { Vector3() }

    /** Flat backing array (x,y,z ×8) reused during [update] and by portal subclasses. */
    @JvmField val planePointsArray = FloatArray(8 * 3)

    /** near, far, left, right, top, bottom. */
    @JvmField val planes: Array<Plane> = Array(6) { Plane() }

    /**
     * Rebuilds the frustum from a camera's inverse projection-view matrix by projecting
     * the canonical clip-space cube corners into world space and fitting the six planes.
     */
    fun update(inverseProjectionView: Matrix4) {
        CLIP_SPACE_PLANE_POINTS.copyInto(planePointsArray)
        Matrix4.prj(inverseProjectionView.`val`, planePointsArray, 0, 8, 3)
        var i = 0
        var j = 0
        while (i < 8) {
            planePoints[i].set(planePointsArray[j], planePointsArray[j + 1], planePointsArray[j + 2])
            i++
            j += 3
        }
        planes[0].set(planePoints[1], planePoints[0], planePoints[2])
        planes[1].set(planePoints[4], planePoints[5], planePoints[7])
        planes[2].set(planePoints[0], planePoints[4], planePoints[3])
        planes[3].set(planePoints[5], planePoints[1], planePoints[6])
        planes[4].set(planePoints[2], planePoints[3], planePoints[6])
        planes[5].set(planePoints[4], planePoints[0], planePoints[1])
    }

    /** True if [point] lies inside all six planes. */
    fun pointInFrustum(point: Vector3): Boolean {
        for (plane in planes) {
            if (plane.testPoint(point) == Plane.PlaneSide.Back) return false
        }
        return true
    }

    companion object {
        private val CLIP_SPACE_PLANE_POINTS = floatArrayOf(
            -1f, -1f, -1f, 1f, -1f, -1f, 1f, 1f, -1f, -1f, 1f, -1f, // near
            -1f, -1f, 1f, 1f, -1f, 1f, 1f, 1f, 1f, -1f, 1f, 1f, // far
        )
    }
}

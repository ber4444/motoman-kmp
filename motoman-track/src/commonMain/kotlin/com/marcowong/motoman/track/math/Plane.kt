package com.marcowong.motoman.track.math

/**
 * A plane in Hessian normal form (`normal · x + d = 0`). Port of libGDX `Plane`,
 * limited to what the engine's portal-frustum culling uses.
 */
class Plane {
    @JvmField val normal = Vector3()
    @JvmField var d: Float = 0f

    enum class PlaneSide { OnPlane, Back, Front }

    /**
     * Sets the plane from three points wound counter-clockwise when viewed from the
     * front side. Matches libGDX: `normal = ((p1 - p2) × (p2 - p3)).nor()`, `d = -p1·normal`.
     */
    fun set(point1: Vector3, point2: Vector3, point3: Vector3): Plane {
        normal.set(point1).sub(point2)
            .crs(point2.x - point3.x, point2.y - point3.y, point2.z - point3.z)
            .nor()
        d = -point1.dot(normal)
        return this
    }

    fun set(nx: Float, ny: Float, nz: Float, d: Float): Plane {
        normal.set(nx, ny, nz)
        this.d = d
        return this
    }

    /** Signed distance from [point] to the plane; sign indicates the side. */
    fun distance(point: Vector3): Float = normal.dot(point) + d

    fun testPoint(point: Vector3): PlaneSide {
        val dist = distance(point)
        return when {
            dist == 0f -> PlaneSide.OnPlane
            dist < 0f -> PlaneSide.Back
            else -> PlaneSide.Front
        }
    }
}

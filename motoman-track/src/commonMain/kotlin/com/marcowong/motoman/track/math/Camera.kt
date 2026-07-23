package com.marcowong.motoman.track.math

/**
 * Base camera holding the position/orientation and the derived projection/view matrices
 * and frustum. Port of libGDX `Camera`, limited to what the engine's `MotomanCamera` uses.
 */
abstract class Camera {
    @JvmField val position = Vector3()
    @JvmField val direction = Vector3(0f, 0f, -1f)
    @JvmField val up = Vector3(0f, 1f, 0f)

    @JvmField val projection = Matrix4()
    @JvmField val view = Matrix4()
    @JvmField val combined = Matrix4()
    @JvmField val invProjectionView = Matrix4()

    @JvmField var near = 1f
    @JvmField var far = 100f
    @JvmField var viewportWidth = 0f
    @JvmField var viewportHeight = 0f

    @JvmField val frustum = Frustum()

    private val tmpVec = Vector3()

    abstract fun update()

    /** Points [direction] at (x,y,z), keeping [up] orthonormal. Port of libGDX `Camera.lookAt`. */
    fun lookAt(x: Float, y: Float, z: Float) {
        tmpVec.set(x, y, z).sub(position)
        if (!tmpVec.isZero()) {
            tmpVec.nor()
            val dot = tmpVec.dot(up)
            if (kotlin.math.abs(dot - 1f) < 1e-9f) {
                up.set(direction).mul(-1f)
            } else if (kotlin.math.abs(dot + 1f) < 1e-9f) {
                up.set(direction)
            }
            direction.set(tmpVec)
            normalizeUp()
        }
    }

    /** Re-orthogonalises [up] against [direction]. */
    fun normalizeUp() {
        tmpVec.set(direction).crs(up.x, up.y, up.z).nor()
        up.set(tmpVec).crs(direction.x, direction.y, direction.z).nor()
    }
}

/** Perspective camera, port of libGDX `PerspectiveCamera`. */
open class PerspectiveCamera : Camera {
    @JvmField var fieldOfView: Float = 67f

    private val tmp = Vector3()

    constructor()

    constructor(fieldOfView: Float, viewportWidth: Float, viewportHeight: Float) {
        this.fieldOfView = fieldOfView
        this.viewportWidth = viewportWidth
        this.viewportHeight = viewportHeight
        doUpdate(true)
    }

    override fun update() = doUpdate(true)

    fun update(updateFrustum: Boolean) = doUpdate(updateFrustum)

    private fun doUpdate(updateFrustum: Boolean) {
        val aspect = viewportWidth / viewportHeight
        projection.setToProjection(kotlin.math.abs(near), kotlin.math.abs(far), fieldOfView, aspect)
        view.setToLookAt(position, tmp.set(position).add(direction), up)
        combined.set(projection).mul(view)
        if (updateFrustum) {
            invProjectionView.set(combined).inv()
            frustum.update(invProjectionView)
        }
    }
}

package com.marcowong.motoman.track.math

import kotlin.math.sqrt

class Vector3(@JvmField var x: Float = 0f, @JvmField var y: Float = 0f, @JvmField var z: Float = 0f) {
    fun set(x: Float, y: Float, z: Float): Vector3 { this.x = x; this.y = y; this.z = z; return this }
    fun set(v: Vector3): Vector3 { this.x = v.x; this.y = v.y; this.z = v.z; return this }
    fun len(): Float = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
    fun nor(): Vector3 {
        val len = len()
        if (len != 0f) { x /= len; y /= len; z /= len }
        return this
    }
    fun crs(x: Float, y: Float, z: Float): Vector3 {
        val cx = this.y * z - this.z * y
        val cy = this.z * x - this.x * z
        val cz = this.x * y - this.y * x
        this.x = cx; this.y = cy; this.z = cz
        return this
    }
    fun mul(scalar: Float): Vector3 { x *= scalar; y *= scalar; z *= scalar; return this }
    fun div(scalar: Float): Vector3 { x /= scalar; y /= scalar; z /= scalar; return this }
    fun add(v: Vector3): Vector3 { x += v.x; y += v.y; z += v.z; return this }
    fun add(x: Float, y: Float, z: Float): Vector3 { this.x += x; this.y += y; this.z += z; return this }
    fun sub(v: Vector3): Vector3 { x -= v.x; y -= v.y; z -= v.z; return this }
    fun sub(x: Float, y: Float, z: Float): Vector3 { this.x -= x; this.y -= y; this.z -= z; return this }
    fun dot(v: Vector3): Float = x * v.x + y * v.y + z * v.z
    fun len2(): Float = x * x + y * y + z * z
    fun isZero(): Boolean = x == 0f && y == 0f && z == 0f
    fun lerp(target: Vector3, alpha: Float): Vector3 {
        x += alpha * (target.x - x)
        y += alpha * (target.y - y)
        z += alpha * (target.z - z)
        return this
    }
    fun mul(matrix: Matrix4): Vector3 {
        val l_mat = matrix.`val`
        val vX = x * l_mat[Matrix4.M00] + y * l_mat[Matrix4.M01] + z * l_mat[Matrix4.M02] + l_mat[Matrix4.M03]
        val vY = x * l_mat[Matrix4.M10] + y * l_mat[Matrix4.M11] + z * l_mat[Matrix4.M12] + l_mat[Matrix4.M13]
        val vZ = x * l_mat[Matrix4.M20] + y * l_mat[Matrix4.M21] + z * l_mat[Matrix4.M22] + l_mat[Matrix4.M23]
        x = vX; y = vY; z = vZ
        return this
    }
}

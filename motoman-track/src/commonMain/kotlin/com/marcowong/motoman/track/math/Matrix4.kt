package com.marcowong.motoman.track.math

import kotlin.math.*

class Matrix4 {
    val `val` = FloatArray(16)
    init { idt() }

    // Reusable scratch for look-at / inverse; not part of the matrix's value.
    private val lookVex = Vector3()
    private val lookVey = Vector3()
    private val lookVez = Vector3()
    private val lookTmp = Vector3()
    private val invTmp = FloatArray(16)
    
    fun idt(): Matrix4 {
        `val`.fill(0f)
        `val`[M00] = 1f; `val`[M11] = 1f; `val`[M22] = 1f; `val`[M33] = 1f
        return this
    }
    
    fun set(matrix: Matrix4): Matrix4 {
        matrix.`val`.copyInto(`val`)
        return this
    }
    
    fun set(quaternion: Quaternion): Matrix4 {
        quaternion.toMatrix(`val`)
        return this
    }
    
    fun mul(matrix: Matrix4): Matrix4 {
        val mata = `val`
        val matb = matrix.`val`
        val m00 = mata[M00] * matb[M00] + mata[M01] * matb[M10] + mata[M02] * matb[M20] + mata[M03] * matb[M30]
        val m01 = mata[M00] * matb[M01] + mata[M01] * matb[M11] + mata[M02] * matb[M21] + mata[M03] * matb[M31]
        val m02 = mata[M00] * matb[M02] + mata[M01] * matb[M12] + mata[M02] * matb[M22] + mata[M03] * matb[M32]
        val m03 = mata[M00] * matb[M03] + mata[M01] * matb[M13] + mata[M02] * matb[M23] + mata[M03] * matb[M33]
        
        val m10 = mata[M10] * matb[M00] + mata[M11] * matb[M10] + mata[M12] * matb[M20] + mata[M13] * matb[M30]
        val m11 = mata[M10] * matb[M01] + mata[M11] * matb[M11] + mata[M12] * matb[M21] + mata[M13] * matb[M31]
        val m12 = mata[M10] * matb[M02] + mata[M11] * matb[M12] + mata[M12] * matb[M22] + mata[M13] * matb[M32]
        val m13 = mata[M10] * matb[M03] + mata[M11] * matb[M13] + mata[M12] * matb[M23] + mata[M13] * matb[M33]
        
        val m20 = mata[M20] * matb[M00] + mata[M21] * matb[M10] + mata[M22] * matb[M20] + mata[M23] * matb[M30]
        val m21 = mata[M20] * matb[M01] + mata[M21] * matb[M11] + mata[M22] * matb[M21] + mata[M23] * matb[M31]
        val m22 = mata[M20] * matb[M02] + mata[M21] * matb[M12] + mata[M22] * matb[M22] + mata[M23] * matb[M32]
        val m23 = mata[M20] * matb[M03] + mata[M21] * matb[M13] + mata[M22] * matb[M23] + mata[M23] * matb[M33]
        
        val m30 = mata[M30] * matb[M00] + mata[M31] * matb[M10] + mata[M32] * matb[M20] + mata[M33] * matb[M30]
        val m31 = mata[M30] * matb[M01] + mata[M31] * matb[M11] + mata[M32] * matb[M21] + mata[M33] * matb[M31]
        val m32 = mata[M30] * matb[M02] + mata[M31] * matb[M12] + mata[M32] * matb[M22] + mata[M33] * matb[M32]
        val m33 = mata[M30] * matb[M03] + mata[M31] * matb[M13] + mata[M32] * matb[M23] + mata[M33] * matb[M33]
        
        mata[M00] = m00; mata[M01] = m01; mata[M02] = m02; mata[M03] = m03
        mata[M10] = m10; mata[M11] = m11; mata[M12] = m12; mata[M13] = m13
        mata[M20] = m20; mata[M21] = m21; mata[M22] = m22; mata[M23] = m23
        mata[M30] = m30; mata[M31] = m31; mata[M32] = m32; mata[M33] = m33
        return this
    }
    
    fun trn(x: Float, y: Float, z: Float): Matrix4 {
        `val`[M03] += x
        `val`[M13] += y
        `val`[M23] += z
        return this
    }
    
    fun trn(vector: Vector3): Matrix4 {
        `val`[M03] += vector.x
        `val`[M13] += vector.y
        `val`[M23] += vector.z
        return this
    }
    
    fun translate(x: Float, y: Float, z: Float): Matrix4 {
        `val`[M03] += `val`[M00] * x + `val`[M01] * y + `val`[M02] * z
        `val`[M13] += `val`[M10] * x + `val`[M11] * y + `val`[M12] * z
        `val`[M23] += `val`[M20] * x + `val`[M21] * y + `val`[M22] * z
        `val`[M33] += `val`[M30] * x + `val`[M31] * y + `val`[M32] * z
        return this
    }
    
    fun scale(scaleX: Float, scaleY: Float, scaleZ: Float): Matrix4 {
        `val`[M00] *= scaleX; `val`[M01] *= scaleY; `val`[M02] *= scaleZ
        `val`[M10] *= scaleX; `val`[M11] *= scaleY; `val`[M12] *= scaleZ
        `val`[M20] *= scaleX; `val`[M21] *= scaleY; `val`[M22] *= scaleZ
        `val`[M30] *= scaleX; `val`[M31] *= scaleY; `val`[M32] *= scaleZ
        return this
    }
    
    fun getTranslation(position: Vector3): Vector3 {
        position.x = `val`[M03]
        position.y = `val`[M13]
        position.z = `val`[M23]
        return position
    }
    
    fun rotate(axisX: Float, axisY: Float, axisZ: Float, degrees: Float): Matrix4 {
        if (degrees == 0f) return this
        val q = Quaternion()
        val axis = Vector3(axisX, axisY, axisZ).nor()
        val rad = (degrees.toDouble() / 180.0 * PI).toFloat()
        val sin = sin(rad * 0.5f)
        q.set(axis.x * sin, axis.y * sin, axis.z * sin, cos(rad * 0.5f))
        val rotMat = Matrix4().set(q)
        return mul(rotMat)
    }

    fun setToTranslation(x: Float, y: Float, z: Float): Matrix4 {
        idt()
        `val`[M03] = x
        `val`[M13] = y
        `val`[M23] = z
        return this
    }

    /** Perspective projection, port of libGDX `Matrix4.setToProjection(near, far, fovy, aspect)`. */
    fun setToProjection(near: Float, far: Float, fovy: Float, aspectRatio: Float): Matrix4 {
        idt()
        val fd = (1.0 / tan((fovy * (PI / 180)) / 2.0)).toFloat()
        val a1 = (far + near) / (near - far)
        val a2 = (2f * far * near) / (near - far)
        `val`[M00] = fd / aspectRatio; `val`[M10] = 0f; `val`[M20] = 0f; `val`[M30] = 0f
        `val`[M01] = 0f; `val`[M11] = fd; `val`[M21] = 0f; `val`[M31] = 0f
        `val`[M02] = 0f; `val`[M12] = 0f; `val`[M22] = a1; `val`[M32] = -1f
        `val`[M03] = 0f; `val`[M13] = 0f; `val`[M23] = a2; `val`[M33] = 0f
        return this
    }

    /** Rotation-only look-at from a forward [direction] and [up], port of libGDX. */
    fun setToLookAt(direction: Vector3, up: Vector3): Matrix4 {
        lookVez.set(direction).nor()
        lookVex.set(direction).nor()
        lookVex.crs(up.x, up.y, up.z).nor()
        lookVey.set(lookVex).crs(lookVez.x, lookVez.y, lookVez.z).nor()
        idt()
        `val`[M00] = lookVex.x; `val`[M01] = lookVex.y; `val`[M02] = lookVex.z
        `val`[M10] = lookVey.x; `val`[M11] = lookVey.y; `val`[M12] = lookVey.z
        `val`[M20] = -lookVez.x; `val`[M21] = -lookVez.y; `val`[M22] = -lookVez.z
        return this
    }

    /** Full look-at (rotation + translation), port of libGDX `setToLookAt(position, target, up)`. */
    fun setToLookAt(position: Vector3, target: Vector3, up: Vector3): Matrix4 {
        lookTmp.set(target).sub(position)
        setToLookAt(lookTmp, up)
        // Right-multiplying the rotation by T(-position) is exactly translate(-position).
        translate(-position.x, -position.y, -position.z)
        return this
    }

    /** In-place 4x4 inverse, port of libGDX `Matrix4.inv()`. */
    fun inv(): Matrix4 {
        val m = `val`
        val lDet = m[M30] * m[M21] * m[M12] * m[M03] - m[M20] * m[M31] * m[M12] * m[M03] -
            m[M30] * m[M11] * m[M22] * m[M03] + m[M10] * m[M31] * m[M22] * m[M03] +
            m[M20] * m[M11] * m[M32] * m[M03] - m[M10] * m[M21] * m[M32] * m[M03] -
            m[M30] * m[M21] * m[M02] * m[M13] + m[M20] * m[M31] * m[M02] * m[M13] +
            m[M30] * m[M01] * m[M22] * m[M13] - m[M00] * m[M31] * m[M22] * m[M13] -
            m[M20] * m[M01] * m[M32] * m[M13] + m[M00] * m[M21] * m[M32] * m[M13] +
            m[M30] * m[M11] * m[M02] * m[M23] - m[M10] * m[M31] * m[M02] * m[M23] -
            m[M30] * m[M01] * m[M12] * m[M23] + m[M00] * m[M31] * m[M12] * m[M23] +
            m[M10] * m[M01] * m[M32] * m[M23] - m[M00] * m[M11] * m[M32] * m[M23] -
            m[M20] * m[M11] * m[M02] * m[M33] + m[M10] * m[M21] * m[M02] * m[M33] +
            m[M20] * m[M01] * m[M12] * m[M33] - m[M00] * m[M21] * m[M12] * m[M33] -
            m[M10] * m[M01] * m[M22] * m[M33] + m[M00] * m[M11] * m[M22] * m[M33]
        if (lDet == 0f) throw IllegalStateException("non-invertible matrix")
        val invDet = 1.0f / lDet
        val t = invTmp
        t[M00] = m[M12] * m[M23] * m[M31] - m[M13] * m[M22] * m[M31] + m[M13] * m[M21] * m[M32] -
            m[M11] * m[M23] * m[M32] - m[M12] * m[M21] * m[M33] + m[M11] * m[M22] * m[M33]
        t[M01] = m[M03] * m[M22] * m[M31] - m[M02] * m[M23] * m[M31] - m[M03] * m[M21] * m[M32] +
            m[M01] * m[M23] * m[M32] + m[M02] * m[M21] * m[M33] - m[M01] * m[M22] * m[M33]
        t[M02] = m[M02] * m[M13] * m[M31] - m[M03] * m[M12] * m[M31] + m[M03] * m[M11] * m[M32] -
            m[M01] * m[M13] * m[M32] - m[M02] * m[M11] * m[M33] + m[M01] * m[M12] * m[M33]
        t[M03] = m[M03] * m[M12] * m[M21] - m[M02] * m[M13] * m[M21] - m[M03] * m[M11] * m[M22] +
            m[M01] * m[M13] * m[M22] + m[M02] * m[M11] * m[M23] - m[M01] * m[M12] * m[M23]
        t[M10] = m[M13] * m[M22] * m[M30] - m[M12] * m[M23] * m[M30] - m[M13] * m[M20] * m[M32] +
            m[M10] * m[M23] * m[M32] + m[M12] * m[M20] * m[M33] - m[M10] * m[M22] * m[M33]
        t[M11] = m[M02] * m[M23] * m[M30] - m[M03] * m[M22] * m[M30] + m[M03] * m[M20] * m[M32] -
            m[M00] * m[M23] * m[M32] - m[M02] * m[M20] * m[M33] + m[M00] * m[M22] * m[M33]
        t[M12] = m[M03] * m[M12] * m[M30] - m[M02] * m[M13] * m[M30] - m[M03] * m[M10] * m[M32] +
            m[M00] * m[M13] * m[M32] + m[M02] * m[M10] * m[M33] - m[M00] * m[M12] * m[M33]
        t[M13] = m[M02] * m[M13] * m[M20] - m[M03] * m[M12] * m[M20] + m[M03] * m[M10] * m[M22] -
            m[M00] * m[M13] * m[M22] - m[M02] * m[M10] * m[M23] + m[M00] * m[M12] * m[M23]
        t[M20] = m[M11] * m[M23] * m[M30] - m[M13] * m[M21] * m[M30] + m[M13] * m[M20] * m[M31] -
            m[M10] * m[M23] * m[M31] - m[M11] * m[M20] * m[M33] + m[M10] * m[M21] * m[M33]
        t[M21] = m[M03] * m[M21] * m[M30] - m[M01] * m[M23] * m[M30] - m[M03] * m[M20] * m[M31] +
            m[M00] * m[M23] * m[M31] + m[M01] * m[M20] * m[M33] - m[M00] * m[M21] * m[M33]
        t[M22] = m[M01] * m[M13] * m[M30] - m[M03] * m[M11] * m[M30] + m[M03] * m[M10] * m[M31] -
            m[M00] * m[M13] * m[M31] - m[M01] * m[M10] * m[M33] + m[M00] * m[M11] * m[M33]
        t[M23] = m[M03] * m[M11] * m[M20] - m[M01] * m[M13] * m[M20] - m[M03] * m[M10] * m[M21] +
            m[M00] * m[M13] * m[M21] + m[M01] * m[M10] * m[M23] - m[M00] * m[M11] * m[M23]
        t[M30] = m[M12] * m[M21] * m[M30] - m[M11] * m[M22] * m[M30] - m[M12] * m[M20] * m[M31] +
            m[M10] * m[M22] * m[M31] + m[M11] * m[M20] * m[M32] - m[M10] * m[M21] * m[M32]
        t[M31] = m[M01] * m[M22] * m[M30] - m[M02] * m[M21] * m[M30] + m[M02] * m[M20] * m[M31] -
            m[M00] * m[M22] * m[M31] - m[M01] * m[M20] * m[M32] + m[M00] * m[M21] * m[M32]
        t[M32] = m[M02] * m[M11] * m[M30] - m[M01] * m[M12] * m[M30] - m[M02] * m[M10] * m[M31] +
            m[M00] * m[M12] * m[M31] + m[M01] * m[M10] * m[M32] - m[M00] * m[M11] * m[M32]
        t[M33] = m[M01] * m[M12] * m[M20] - m[M02] * m[M11] * m[M20] + m[M02] * m[M10] * m[M21] -
            m[M00] * m[M12] * m[M21] - m[M01] * m[M10] * m[M22] + m[M00] * m[M11] * m[M22]
        for (i in 0 until 16) m[i] = t[i] * invDet
        return this
    }

    fun getRotation(rotation: Quaternion): Quaternion {
        val trace = `val`[M00] + `val`[M11] + `val`[M22]
        if (trace > 0) {
            var s = sqrt(trace + 1.0f) * 2f
            rotation.w = 0.25f * s
            rotation.x = (`val`[M21] - `val`[M12]) / s
            rotation.y = (`val`[M02] - `val`[M20]) / s
            rotation.z = (`val`[M10] - `val`[M01]) / s
        } else if (`val`[M00] > `val`[M11] && `val`[M00] > `val`[M22]) {
            var s = sqrt(1.0f + `val`[M00] - `val`[M11] - `val`[M22]) * 2f
            rotation.w = (`val`[M21] - `val`[M12]) / s
            rotation.x = 0.25f * s
            rotation.y = (`val`[M01] + `val`[M10]) / s
            rotation.z = (`val`[M02] + `val`[M20]) / s
        } else if (`val`[M11] > `val`[M22]) {
            var s = sqrt(1.0f + `val`[M11] - `val`[M00] - `val`[M22]) * 2f
            rotation.w = (`val`[M02] - `val`[M20]) / s
            rotation.x = (`val`[M01] + `val`[M10]) / s
            rotation.y = 0.25f * s
            rotation.z = (`val`[M12] + `val`[M21]) / s
        } else {
            var s = sqrt(1.0f + `val`[M22] - `val`[M00] - `val`[M11]) * 2f
            rotation.w = (`val`[M10] - `val`[M01]) / s
            rotation.x = (`val`[M02] + `val`[M20]) / s
            rotation.y = (`val`[M12] + `val`[M21]) / s
            rotation.z = 0.25f * s
        }
        return rotation
    }

    companion object {
        const val M00 = 0; const val M01 = 4; const val M02 = 8; const val M03 = 12
        const val M10 = 1; const val M11 = 5; const val M12 = 9; const val M13 = 13
        const val M20 = 2; const val M21 = 6; const val M22 = 10; const val M23 = 14
        const val M30 = 3; const val M31 = 7; const val M32 = 11; const val M33 = 15

        /**
         * Projects [numVecs] 3-component points in-place through the column-major matrix
         * [mat], applying the perspective divide. Bit-for-bit port of libGDX `Matrix4.prj`;
         * used by [com.marcowong.motoman.track.math.Frustum]-style portal culling.
         *
         * @param mat 16-float column-major matrix.
         * @param vecs point buffer, mutated in place.
         * @param offset index of the first point's x component.
         * @param numVecs number of points to project.
         * @param stride element stride between consecutive points.
         */
        fun prj(mat: FloatArray, vecs: FloatArray, offset: Int, numVecs: Int, stride: Int) {
            var index = offset
            var i = 0
            while (i < numVecs) {
                val x = vecs[index]
                val y = vecs[index + 1]
                val z = vecs[index + 2]
                val invW = 1f / (x * mat[M30] + y * mat[M31] + z * mat[M32] + mat[M33])
                vecs[index] = (x * mat[M00] + y * mat[M01] + z * mat[M02] + mat[M03]) * invW
                vecs[index + 1] = (x * mat[M10] + y * mat[M11] + z * mat[M12] + mat[M13]) * invW
                vecs[index + 2] = (x * mat[M20] + y * mat[M21] + z * mat[M22] + mat[M23]) * invW
                index += stride
                i++
            }
        }
    }
}

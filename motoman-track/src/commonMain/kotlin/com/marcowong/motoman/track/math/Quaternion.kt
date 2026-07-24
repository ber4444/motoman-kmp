package com.marcowong.motoman.track.math

import kotlin.math.*

class Quaternion(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f, var w: Float = 1f) {
    fun set(x: Float, y: Float, z: Float, w: Float): Quaternion {
        this.x = x; this.y = y; this.z = z; this.w = w
        return this
    }
    
    fun set(q: Quaternion): Quaternion {
        this.x = q.x; this.y = q.y; this.z = q.z; this.w = q.w
        return this
    }

    fun setEulerAngles(yaw: Float, pitch: Float, roll: Float): Quaternion {
        val hr = (roll.toDouble() / 180.0 * PI) * 0.5
        val shr = sin(hr).toFloat()
        val chr = cos(hr).toFloat()
        val hp = (pitch.toDouble() / 180.0 * PI) * 0.5
        val shp = sin(hp).toFloat()
        val chp = cos(hp).toFloat()
        val hy = (yaw.toDouble() / 180.0 * PI) * 0.5
        val shy = sin(hy).toFloat()
        val chy = cos(hy).toFloat()
        val chy_shp = chy * shp
        val shy_chp = shy * chp
        val chy_chp = chy * chp
        val shy_shp = shy * shp
        
        x = (chy_shp * chr) + (shy_chp * shr)
        y = (shy_chp * chr) - (chy_shp * shr)
        z = (chy_chp * shr) - (shy_shp * chr)
        w = (chy_chp * chr) + (shy_shp * shr)
        return this
    }

    fun slerp(end: Quaternion, alpha: Float): Quaternion {
        val d = x * end.x + y * end.y + z * end.z + w * end.w
        var absDot = if (d < 0f) -d else d
        var scale0 = 1f - alpha
        var scale1 = alpha

        if ((1f - absDot) > 0.1f) {
            val angle = acos(absDot.toDouble()).toFloat()
            val invSinTheta = 1f / sin(angle.toDouble()).toFloat()
            scale0 = (sin(((1f - alpha) * angle).toDouble()) * invSinTheta).toFloat()
            scale1 = (sin((alpha * angle).toDouble()) * invSinTheta).toFloat()
        }

        if (d < 0f) scale1 = -scale1

        x = (scale0 * x) + (scale1 * end.x)
        y = (scale0 * y) + (scale1 * end.y)
        z = (scale0 * z) + (scale1 * end.z)
        w = (scale0 * w) + (scale1 * end.w)
        
        return this
    }
    fun nor(): Quaternion {
        val len = x * x + y * y + z * z + w * w
        if (len != 0f && len != 1f) {
            val sq = sqrt(len)
            x /= sq; y /= sq; z /= sq; w /= sq
        }
        return this
    }
    
    fun toMatrix(matrix: FloatArray) {
        val xx = x * x
        val xy = x * y
        val xz = x * z
        val xw = x * w
        val yy = y * y
        val yz = y * z
        val yw = y * w
        val zz = z * z
        val zw = z * w
        
        matrix[Matrix4.M00] = 1 - 2 * (yy + zz)
        matrix[Matrix4.M01] = 2 * (xy - zw)
        matrix[Matrix4.M02] = 2 * (xz + yw)
        matrix[Matrix4.M03] = 0f
        
        matrix[Matrix4.M10] = 2 * (xy + zw)
        matrix[Matrix4.M11] = 1 - 2 * (xx + zz)
        matrix[Matrix4.M12] = 2 * (yz - xw)
        matrix[Matrix4.M13] = 0f
        
        matrix[Matrix4.M20] = 2 * (xz - yw)
        matrix[Matrix4.M21] = 2 * (yz + xw)
        matrix[Matrix4.M22] = 1 - 2 * (xx + yy)
        matrix[Matrix4.M23] = 0f
        
        matrix[Matrix4.M30] = 0f
        matrix[Matrix4.M31] = 0f
        matrix[Matrix4.M32] = 0f
        matrix[Matrix4.M33] = 1f
    }
}

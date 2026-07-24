package com.marcowong.motoman.gl

import android.graphics.BitmapFactory

/**
 * Android image decoding via `BitmapFactory`, converted to the same tightly packed
 * RGBA8888 layout the desktop STB path produces.
 */
actual fun decodePixmap(bytes: ByteArray): Pixmap {
    require(bytes.isNotEmpty()) { "cannot decode an empty byte array" }

    var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        ?: error("BitmapFactory failed to decode image")
    try {
        var w = bitmap.width
        var h = bitmap.height
        val potW = nextPowerOfTwo(w)
        val potH = nextPowerOfTwo(h)
        if (w != potW || h != potH) {
            val scaled = android.graphics.Bitmap.createScaledBitmap(bitmap, potW, potH, true)
            bitmap.recycle()
            bitmap = scaled
            w = potW
            h = potH
        }
        val argb = IntArray(w * h)
        bitmap.getPixels(argb, 0, w, 0, 0, w, h)

        // getPixels yields packed ARGB ints; GL wants RGBA bytes in memory order.
        val out = ByteArray(w * h * 4)
        for (i in argb.indices) {
            val p = argb[i]
            val o = i * 4
            out[o] = ((p shr 16) and 0xFF).toByte()      // R
            out[o + 1] = ((p shr 8) and 0xFF).toByte()   // G
            out[o + 2] = (p and 0xFF).toByte()           // B
            out[o + 3] = ((p ushr 24) and 0xFF).toByte() // A
        }
        return Pixmap(w, h, PixmapFormat.RGBA8888, out)
    } finally {
        bitmap.recycle()
    }
}

private fun nextPowerOfTwo(value: Int): Int {
    if (value == 0) return 1
    var v = value - 1
    v = v or (v ushr 1)
    v = v or (v ushr 2)
    v = v or (v ushr 4)
    v = v or (v ushr 8)
    v = v or (v ushr 16)
    return v + 1
}

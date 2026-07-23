package com.marcowong.motoman.gl

import android.graphics.BitmapFactory

/**
 * Android image decoding via `BitmapFactory`, converted to the same tightly packed
 * RGBA8888 layout the desktop STB path produces.
 */
actual fun decodePixmap(bytes: ByteArray): Pixmap {
    require(bytes.isNotEmpty()) { "cannot decode an empty byte array" }

    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        ?: error("BitmapFactory failed to decode image")
    try {
        val w = bitmap.width
        val h = bitmap.height
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

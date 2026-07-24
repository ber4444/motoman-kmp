package com.marcowong.motoman.track.math

/**
 * RGBA colour with libGDX-compatible float-bit packing. Port of libGDX `Color`,
 * limited to what the engine's materials and vertex packing use.
 */
class Color(
    var r: Float = 0f,
    var g: Float = 0f,
    var b: Float = 0f,
    var a: Float = 0f,
) {
    fun set(r: Float, g: Float, b: Float, a: Float): Color {
        this.r = r; this.g = g; this.b = b; this.a = a
        return this
    }

    fun set(other: Color): Color = set(other.r, other.g, other.b, other.a)

    /**
     * Packs this colour into a single float (ABGR8888 with the top alpha bit cleared to
     * dodge NaN), bit-identical to libGDX `Color.toFloatBits()`.
     */
    fun toFloatBits(): Float {
        val color = ((255 * a).toInt() shl 24) or
            ((255 * b).toInt() shl 16) or
            ((255 * g).toInt() shl 8) or
            (255 * r).toInt()
        return Float.fromBits(color and 0xfeffffff.toInt())
    }

    override fun equals(other: Any?): Boolean =
        other is Color && r == other.r && g == other.g && b == other.b && a == other.a

    override fun hashCode(): Int {
        var result = r.toRawBits()
        result = 31 * result + g.toRawBits()
        result = 31 * result + b.toRawBits()
        result = 31 * result + a.toRawBits()
        return result
    }
}

package com.marcowong.motoman.gl

/**
 * A rectangular sub-area of a [Texture], expressed in normalised UV coordinates.
 * Port of libGDX `TextureRegion` limited to what the engine needs.
 */
class TextureRegion(
    val texture: Texture,
    var u: Float = 0f,
    var v: Float = 0f,
    var u2: Float = 1f,
    var v2: Float = 1f,
) {
    /** Builds a region from pixel coordinates within [texture]. */
    constructor(texture: Texture, x: Int, y: Int, width: Int, height: Int) : this(
        texture,
        x.toFloat() / texture.width,
        y.toFloat() / texture.height,
        (x + width).toFloat() / texture.width,
        (y + height).toFloat() / texture.height,
    )

    val regionWidth: Int get() = kotlin.math.round((u2 - u) * texture.width).toInt()
    val regionHeight: Int get() = kotlin.math.round((v2 - v) * texture.height).toInt()

    /** Flips the region in place; `v`/`v2` swap is the common case for GL's origin. */
    fun flip(x: Boolean, y: Boolean) {
        if (x) { val t = u; u = u2; u2 = t }
        if (y) { val t = v; v = v2; v2 = t }
    }
}

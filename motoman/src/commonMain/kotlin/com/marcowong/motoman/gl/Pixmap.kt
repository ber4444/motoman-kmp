package com.marcowong.motoman.gl

/** Pixel layout of a decoded image. */
enum class PixmapFormat(val components: Int) {
    RGB888(3),
    RGBA8888(4),
}

/**
 * A decoded image in CPU memory: tightly packed pixels with a top-left origin, ready to
 * hand to [Texture]. Port of the parts of libGDX `Pixmap`/`TextureData` the engine uses.
 *
 * Decoding itself has no `commonMain` answer, so it lives behind [decodePixmap].
 */
class Pixmap(
    val width: Int,
    val height: Int,
    val format: PixmapFormat,
    val pixels: ByteArray,
) {
    init {
        val expected = width * height * format.components
        require(pixels.size == expected) {
            "pixel buffer is ${pixels.size} bytes, expected $expected for ${width}x$height ${format.name}"
        }
    }

    /** The GL format enum matching [format]. */
    val glFormat: Int
        get() = when (format) {
            PixmapFormat.RGB888 -> GL_RGB
            PixmapFormat.RGBA8888 -> GL_RGBA
        }

    /** Returns the pixel at (x, y) packed as 0xRRGGBBAA. Intended for tests and tooling. */
    fun getPixel(x: Int, y: Int): Int {
        val i = (y * width + x) * format.components
        val r = pixels[i].toInt() and 0xFF
        val g = pixels[i + 1].toInt() and 0xFF
        val b = pixels[i + 2].toInt() and 0xFF
        val a = if (format == PixmapFormat.RGBA8888) pixels[i + 3].toInt() and 0xFF else 0xFF
        return (r shl 24) or (g shl 16) or (b shl 8) or a
    }
}

/**
 * Decodes PNG/JPEG bytes into an RGBA8888 [Pixmap].
 *
 * Desktop uses STB (bundled with LWJGL, so no new native dependency and it works on
 * macOS and Linux alike); Android uses `BitmapFactory`.
 */
expect fun decodePixmap(bytes: ByteArray): Pixmap

package com.marcowong.motoman.gl

import org.lwjgl.BufferUtils
import org.lwjgl.stb.STBImage

/**
 * Desktop image decoding via STB, which ships with LWJGL — no extra native dependency,
 * and identical behaviour on macOS and Linux.
 */
actual fun decodePixmap(bytes: ByteArray): Pixmap {
    require(bytes.isNotEmpty()) { "cannot decode an empty byte array" }

    val encoded = BufferUtils.createByteBuffer(bytes.size)
    encoded.put(bytes)
    encoded.flip()

    val width = BufferUtils.createIntBuffer(1)
    val height = BufferUtils.createIntBuffer(1)
    val channels = BufferUtils.createIntBuffer(1)

    // GL expects the first row to be the bottom row; our Pixmap contract is top-left
    // origin, so leave STB unflipped and let Texture deal with orientation.
    STBImage.stbi_set_flip_vertically_on_load(false)

    // Force 4 channels so every decoded image is RGBA8888 regardless of source format.
    val decoded = STBImage.stbi_load_from_memory(encoded, width, height, channels, 4)
        ?: error("stb failed to decode image: ${STBImage.stbi_failure_reason()}")

    try {
        val w = width.get(0)
        val h = height.get(0)
        val out = ByteArray(w * h * 4)
        decoded.get(out)
        return Pixmap(w, h, PixmapFormat.RGBA8888, out)
    } finally {
        STBImage.stbi_image_free(decoded)
    }
}

package com.marcowong.motoman.gl

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Exercises the real STB decoder on the host, on macOS and Linux alike.
 *
 * The fixture is an embedded 2x2 RGBA PNG rather than one generated with ImageIO on the
 * fly: initialising AWT in the same JVM as LWJGL aborts natively on macOS (both want the
 * main thread), and embedding also removes any dependency on which ImageIO writers exist.
 */
class DesktopPixmapDecodeTest {

    // 2x2 RGBA PNG: (0,0) red, (1,0) green, (0,1) blue, (1,1) fully transparent.
    private val png2x2 = intArrayOf(
        137, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 13,
        73, 72, 68, 82, 0, 0, 0, 2, 0, 0, 0, 2,
        8, 6, 0, 0, 0, 114, 182, 13, 36, 0, 0, 0,
        19, 73, 68, 65, 84, 120, 156, 99, 248, 207, 192, 240,
        31, 12, 129, 52, 136, 96, 0, 0, 63, 210, 5, 251,
        203, 23, 166, 206, 0, 0, 0, 0, 73, 69, 78, 68,
        174, 66, 96, 130
    ).map { it.toByte() }.toByteArray()

    @Test
    fun decodesPngPreservingSizeAndFormat() {
        val pixmap = decodePixmap(png2x2)
        assertEquals(2, pixmap.width)
        assertEquals(2, pixmap.height)
        assertEquals(PixmapFormat.RGBA8888, pixmap.format)
        assertEquals(2 * 2 * 4, pixmap.pixels.size)
    }

    @Test
    fun decodesPixelsWithTopLeftOrigin() {
        val pixmap = decodePixmap(png2x2)
        // getPixel packs as 0xRRGGBBAA; row 0 must be the top row.
        assertEquals(0xFF0000FF.toInt(), pixmap.getPixel(0, 0), "top-left should be red")
        assertEquals(0x00FF00FF.toInt(), pixmap.getPixel(1, 0), "top-right should be green")
        assertEquals(0x0000FFFF.toInt(), pixmap.getPixel(0, 1), "bottom-left should be blue")
        assertEquals(0x00000000, pixmap.getPixel(1, 1), "bottom-right should be transparent")
    }

    @Test
    fun rejectsEmptyInput() {
        assertFailsWith<IllegalArgumentException> { decodePixmap(ByteArray(0)) }
    }

    @Test
    fun failsLoudlyOnGarbageInput() {
        assertFailsWith<IllegalStateException> { decodePixmap(ByteArray(64) { 0x7F }) }
    }
}

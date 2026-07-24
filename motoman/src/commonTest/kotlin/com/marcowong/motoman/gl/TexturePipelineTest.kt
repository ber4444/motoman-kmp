package com.marcowong.motoman.gl

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PixmapTest {

    private fun rgba(w: Int, h: Int) = Pixmap(w, h, PixmapFormat.RGBA8888, ByteArray(w * h * 4))

    @Test
    fun rejectsMismatchedBufferSize() {
        assertFailsWith<IllegalArgumentException> {
            Pixmap(2, 2, PixmapFormat.RGBA8888, ByteArray(3))
        }
    }

    @Test
    fun acceptsCorrectlySizedBuffers() {
        assertEquals(4 * 4, rgba(2, 2).pixels.size)
        assertEquals(2 * 2 * 3, Pixmap(2, 2, PixmapFormat.RGB888, ByteArray(12)).pixels.size)
    }

    @Test
    fun glFormatFollowsComponentCount() {
        assertEquals(GL_RGBA, rgba(1, 1).glFormat)
        assertEquals(GL_RGB, Pixmap(1, 1, PixmapFormat.RGB888, ByteArray(3)).glFormat)
    }

    @Test
    fun getPixelReadsRowMajorTopLeftOrigin() {
        // 2x2 RGBA; set pixel (1,1) to opaque red.
        val px = ByteArray(2 * 2 * 4)
        val i = (1 * 2 + 1) * 4
        px[i] = 0xFF.toByte(); px[i + 1] = 0; px[i + 2] = 0; px[i + 3] = 0xFF.toByte()
        val pixmap = Pixmap(2, 2, PixmapFormat.RGBA8888, px)
        assertEquals(0xFF0000FF.toInt(), pixmap.getPixel(1, 1))
        assertEquals(0x00000000, pixmap.getPixel(0, 0))
    }
}

class TextureTest {

    private fun pixmap(w: Int = 2, h: Int = 2) =
        Pixmap(w, h, PixmapFormat.RGBA8888, ByteArray(w * h * 4))

    @Test
    fun uploadsPixelsWithCorrectDimensionsAndFormat() {
        val gl = FakeGl()
        val texture = Texture(gl, pixmap(4, 8))
        assertEquals(4, texture.width)
        assertEquals(8, texture.height)
        assertTrue(gl.calls.any { it == "glTexImage2D(4,8,$GL_RGBA)" }, gl.calls.toString())
        assertEquals(4 * 8 * 4, gl.lastTexImageSize)
    }

    @Test
    fun doesNotGenerateMipmapsForNonMipmapFilter() {
        val gl = FakeGl()
        Texture(gl, pixmap(), minFilter = TextureFilter.Linear)
        assertEquals(0, gl.countOf("glGenerateMipmap"))
    }

    @Test
    fun generatesMipmapsForMipmapFilter() {
        val gl = FakeGl()
        Texture(gl, pixmap(), minFilter = TextureFilter.MipMapLinearLinear)
        assertEquals(1, gl.countOf("glGenerateMipmap"))
    }

    @Test
    fun fallsBackToLinearForNonPowerOfTwoMipmapWithoutNpotSupport() {
        val gl = FakeGl() // no GL_OES_texture_npot advertised
        val texture = Texture(gl, pixmap(3, 4), minFilter = TextureFilter.MipMapLinearLinear)
        // Mipmapping a NPOT texture would make it incomplete on GLES2; drop to plain Linear.
        assertEquals(0, gl.countOf("glGenerateMipmap"))
        assertEquals(TextureFilter.Linear, texture.minFilter)
    }

    @Test
    fun mipmapsNonPowerOfTwoWhenNpotSupported() {
        val gl = FakeGl().apply { glExtensions = "GL_OES_texture_npot" }
        val texture = Texture(gl, pixmap(3, 4), minFilter = TextureFilter.MipMapLinearLinear)
        assertEquals(1, gl.countOf("glGenerateMipmap"))
        assertEquals(TextureFilter.MipMapLinearLinear, texture.minFilter)
    }

    @Test
    fun appliesFilterAndWrapParameters() {
        val gl = FakeGl()
        Texture(
            gl, pixmap(),
            minFilter = TextureFilter.Nearest,
            magFilter = TextureFilter.Linear,
            uWrap = TextureWrap.ClampToEdge,
            vWrap = TextureWrap.Repeat,
        )
        assertTrue(gl.calls.contains("glTexParameteri($GL_TEXTURE_MIN_FILTER,$GL_NEAREST)"))
        assertTrue(gl.calls.contains("glTexParameteri($GL_TEXTURE_MAG_FILTER,$GL_LINEAR)"))
        assertTrue(gl.calls.contains("glTexParameteri($GL_TEXTURE_WRAP_S,$GL_CLAMP_TO_EDGE)"))
        assertTrue(gl.calls.contains("glTexParameteri($GL_TEXTURE_WRAP_T,$GL_REPEAT)"))
    }

    @Test
    fun bindSelectsRequestedTextureUnit() {
        val gl = FakeGl()
        val texture = Texture(gl, pixmap())
        gl.clear()
        texture.bind(3)
        assertTrue(gl.calls.contains("glActiveTexture(${GL_TEXTURE0 + 3})"), gl.calls.toString())
    }

    @Test
    fun disposeIsIdempotent() {
        val gl = FakeGl()
        val texture = Texture(gl, pixmap())
        texture.dispose()
        texture.dispose()
        assertEquals(1, gl.countOf("glDeleteTexture"))
    }
}

class TextureRegionTest {

    private fun texture(gl: FakeGl, w: Int, h: Int) =
        Texture(gl, Pixmap(w, h, PixmapFormat.RGBA8888, ByteArray(w * h * 4)))

    @Test
    fun pixelConstructorComputesUvs() {
        val gl = FakeGl()
        val region = TextureRegion(texture(gl, 100, 200), 25, 50, 50, 100)
        assertEquals(0.25f, region.u)
        assertEquals(0.25f, region.v)
        assertEquals(0.75f, region.u2)
        assertEquals(0.75f, region.v2)
        assertEquals(50, region.regionWidth)
        assertEquals(100, region.regionHeight)
    }

    @Test
    fun flipSwapsCoordinates() {
        val gl = FakeGl()
        val region = TextureRegion(texture(gl, 10, 10), 0f, 0.2f, 1f, 0.8f)
        region.flip(false, true)
        assertEquals(0.8f, region.v)
        assertEquals(0.2f, region.v2)
    }
}

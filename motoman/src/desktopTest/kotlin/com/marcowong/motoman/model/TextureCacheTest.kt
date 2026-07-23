package com.marcowong.motoman.model

import com.marcowong.motoman.assets.ClasspathAssets
import com.marcowong.motoman.gl.FakeGl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

/**
 * Exercises the cache against real shipped images, decoded for real, but with a recording
 * [FakeGl] so no GL context is needed.
 */
class TextureCacheTest {

    private val assets = ClasspathAssets()

    @Test
    fun decodesAndUploadsOnce() {
        val gl = FakeGl()
        val cache = TextureCache(gl, assets)
        val first = cache.get("data/bike.png")
        assertEquals(1, gl.countOf("glGenTexture"))
        assertEquals(1, gl.countOf("glTexImage2D"))
        // The uploaded payload must be 4 bytes per pixel of the real image.
        assertEquals(first.width * first.height * 4, gl.lastTexImageSize)
    }

    @Test
    fun repeatedRequestsShareOneTexture() {
        val gl = FakeGl()
        val cache = TextureCache(gl, assets)
        val a = cache.get("data/bike.png")
        val b = cache.get("data/bike.png")
        assertSame(a, b, "same path must yield the same Texture")
        assertEquals(1, gl.countOf("glGenTexture"), "must not re-upload a cached image")
    }

    @Test
    fun distinctPathsGetDistinctTextures() {
        val gl = FakeGl()
        val cache = TextureCache(gl, assets)
        cache.get("data/bike.png")
        cache.get("data/rider.png")
        assertEquals(2, gl.countOf("glGenTexture"))
    }

    @Test
    fun disposeReleasesEveryTexture() {
        val gl = FakeGl()
        val cache = TextureCache(gl, assets)
        cache.get("data/bike.png")
        cache.get("data/rider.png")
        cache.dispose()
        assertEquals(2, gl.countOf("glDeleteTexture"))
    }
}

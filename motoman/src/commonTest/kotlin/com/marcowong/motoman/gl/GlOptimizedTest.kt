package com.marcowong.motoman.gl

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Guards the state-change elision ported from the original `GL20Optimized` — the part of
 * the renderer that made it viable on 2013 hardware. Regressions here are invisible in a
 * screenshot and only show up as frame-time jank.
 */
class GlOptimizedTest {

    private fun fixture(): Pair<FakeGl, GlOptimized> {
        val fake = FakeGl()
        val gl = GlOptimized(fake)
        fake.clear() // drop the constructor's initial state-reset calls
        return fake to gl
    }

    @Test
    fun redundantEnableIsElided() {
        val (fake, gl) = fixture()
        gl.glEnable(GL_DEPTH_TEST)
        gl.glEnable(GL_DEPTH_TEST)
        gl.glEnable(GL_DEPTH_TEST)
        assertEquals(1, fake.countOf("glEnable"))
    }

    @Test
    fun enableAfterDisableIsNotElided() {
        val (fake, gl) = fixture()
        gl.glEnable(GL_BLEND)
        gl.glDisable(GL_BLEND)
        gl.glEnable(GL_BLEND)
        assertEquals(2, fake.countOf("glEnable"))
        assertEquals(1, fake.countOf("glDisable"))
    }

    @Test
    fun distinctCapabilitiesTrackedSeparately() {
        val (fake, gl) = fixture()
        gl.glEnable(GL_DEPTH_TEST)
        gl.glEnable(GL_CULL_FACE)
        assertEquals(2, fake.countOf("glEnable"))
    }

    @Test
    fun repeatedProgramBindIsElided() {
        val (fake, gl) = fixture()
        gl.glUseProgram(7)
        gl.glUseProgram(7)
        gl.glUseProgram(9)
        assertEquals(2, fake.countOf("glUseProgram"))
    }

    @Test
    fun unchangedUniformIsElidedButChangedOneIsNot() {
        val (fake, gl) = fixture()
        gl.glUseProgram(1)
        gl.glUniform1i(3, 0)
        gl.glUniform1i(3, 0)
        assertEquals(1, fake.countOf("glUniform1i"))
        gl.glUniform1i(3, 1)
        assertEquals(2, fake.countOf("glUniform1i"))
    }

    @Test
    fun uniformCacheIsPerProgram() {
        val (fake, gl) = fixture()
        gl.glUseProgram(1)
        gl.glUniform4f(0, 1f, 0f, 0f, 1f)
        // Same location and value, different program -> must not be elided.
        gl.glUseProgram(2)
        gl.glUniform4f(0, 1f, 0f, 0f, 1f)
        assertEquals(2, fake.countOf("glUniform4f"))
    }

    @Test
    fun repeatedTextureBindOnSameUnitIsElided() {
        val (fake, gl) = fixture()
        gl.glBindTexture(GL_TEXTURE_2D, 5)
        gl.glBindTexture(GL_TEXTURE_2D, 5)
        assertEquals(1, fake.countOf("glBindTexture"))
        gl.glBindTexture(GL_TEXTURE_2D, 6)
        assertEquals(2, fake.countOf("glBindTexture"))
    }

    @Test
    fun redundantBlendFuncAndClearColorAreElided() {
        val (fake, gl) = fixture()
        gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        gl.glClearColor(0f, 0f, 0f, 1f)
        gl.glClearColor(0f, 0f, 0f, 1f)
        assertEquals(1, fake.countOf("glBlendFunc"))
        assertEquals(1, fake.countOf("glClearColor"))
    }

    @Test
    fun vertexAttribArrayIsEnabledLazilyByPointerNotByEnableCall() {
        val (fake, gl) = fixture()
        // The explicit enable is deferred and must not reach the driver on its own.
        gl.glEnableVertexAttribArray(2)
        assertEquals(0, fake.countOf("glEnableVertexAttribArray"))
        // Setting the pointer is what actually enables it, exactly once.
        gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 36, 0)
        gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 36, 0)
        assertEquals(1, fake.countOf("glEnableVertexAttribArray"))
    }

    @Test
    fun attributesUnusedInTheNextDrawAreDisabled() {
        val (fake, gl) = fixture()
        gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 36, 0)
        gl.glVertexAttribPointer(1, 3, GL_FLOAT, false, 36, 12)
        gl.glDrawElements(GL_TRIANGLES, 3, GL_UNSIGNED_SHORT, 0)
        assertEquals(0, fake.countOf("glDisableVertexAttribArray"))

        // Second draw uses only attribute 0, so attribute 1 must be disabled.
        fake.clear()
        gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 36, 0)
        gl.glDrawElements(GL_TRIANGLES, 3, GL_UNSIGNED_SHORT, 0)
        assertTrue(fake.calls.contains("glDisableVertexAttribArray(1)"), fake.calls.toString())
        assertEquals(1, fake.countOf("glDisableVertexAttribArray"))
    }

    @Test
    fun deletingBoundBufferUnbindsIt() {
        val (fake, gl) = fixture()
        gl.glBindBuffer(GL_ARRAY_BUFFER, 4)
        fake.clear()
        gl.glDeleteBuffer(4)
        assertTrue(fake.calls.contains("glBindBuffer($GL_ARRAY_BUFFER,0)"), fake.calls.toString())
    }
}

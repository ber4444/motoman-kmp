package com.marcowong.motoman.gl

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShaderPreprocessorTest {

    private val esFrag = """
        precision mediump float;
        varying vec2 v_texCoord0;
        uniform sampler2D u_texture;
        void main() {
            gl_FragColor = texture2D(u_texture, v_texCoord0);
        }
    """.trimIndent()

    private val esVert = """
        attribute vec4 a_position;
        uniform mat4 u_projTrans;
        void main() {
            gl_Position = u_projTrans * a_position;
        }
    """.trimIndent()

    @Test
    fun desktopPrependsVersion120() {
        val out = ShaderPreprocessor(GlslTarget.DESKTOP_120).process(esVert, ShaderStage.VERTEX)
        assertTrue(out.startsWith("#version 120\n"), "should start with #version 120")
    }

    @Test
    fun desktopStripsDefaultPrecisionStatement() {
        val out = ShaderPreprocessor(GlslTarget.DESKTOP_120).process(esFrag, ShaderStage.FRAGMENT)
        assertFalse(out.contains("precision mediump float"), "default precision statement must be removed")
        assertContains(out, "gl_FragColor")
    }

    @Test
    fun desktopStripsInlinePrecisionQualifier() {
        val src = "uniform lowp vec4 u_color;\nvoid main() { gl_FragColor = u_color; }"
        val out = ShaderPreprocessor(GlslTarget.DESKTOP_120).process(src, ShaderStage.FRAGMENT)
        assertFalse(out.contains("lowp"), "inline precision qualifier must be removed")
        assertContains(out, "uniform vec4 u_color;")
    }

    @Test
    fun esPrependsVersion100AndKeepsPrecision() {
        val out = ShaderPreprocessor(GlslTarget.ES_100).process(esFrag, ShaderStage.FRAGMENT)
        assertTrue(out.startsWith("#version 100\n"))
        assertContains(out, "precision mediump float;")
    }

    @Test
    fun esAddsDefaultPrecisionToFragmentWhenMissing() {
        val src = "varying vec2 v_texCoord0;\nvoid main() { gl_FragColor = vec4(v_texCoord0, 0.0, 1.0); }"
        val out = ShaderPreprocessor(GlslTarget.ES_100).process(src, ShaderStage.FRAGMENT)
        assertContains(out, "precision mediump float;")
    }

    @Test
    fun esDoesNotAddPrecisionToVertex() {
        val out = ShaderPreprocessor(GlslTarget.ES_100).process(esVert, ShaderStage.VERTEX)
        assertFalse(out.contains("precision"), "vertex shader should not get a float precision default")
    }

    @Test
    fun existingVersionDirectiveIsReplaced() {
        val src = "#version 100\nattribute vec4 a_position;\nvoid main() { gl_Position = a_position; }"
        val out = ShaderPreprocessor(GlslTarget.DESKTOP_120).process(src, ShaderStage.VERTEX)
        assertTrue(out.startsWith("#version 120\n"))
        assertEquals(1, Regex("#version").findAll(out).count(), "must not double up #version directives")
    }
}

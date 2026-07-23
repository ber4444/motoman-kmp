package com.marcowong.motoman.gl

enum class ShaderStage { VERTEX, FRAGMENT }

/** GLSL dialect a shader is compiled against. */
enum class GlslTarget {
    /** GLES 2.0 / GLSL ES 1.00 — Android and a future ANGLE-backed iOS target. */
    ES_100,

    /** Desktop GL 2.1 / GLSL 1.20 — LWJGL on desktop. Rejects ES `precision` qualifiers. */
    DESKTOP_120,
}

/**
 * Reconciles a single GLSL ES 1.00 shader source (the format the original engine ships)
 * with the target platform's dialect (Phase 3 Correction 3, preamble-injection approach):
 *
 *  - prepends the appropriate `#version` directive;
 *  - on desktop, neutralises the `precision` qualifiers ES requires but desktop GLSL 1.20
 *    does not understand — both default-precision statements (`precision mediump float;`)
 *    and inline qualifiers (`lowp`/`mediump`/`highp`);
 *  - on ES, guarantees a fragment shader has a default float precision.
 *
 * One source of truth, no native dependency, fully unit-testable in commonMain.
 */
class ShaderPreprocessor(private val target: GlslTarget) {

    fun process(source: String, stage: ShaderStage): String {
        val body = stripLeadingVersion(source)
        return when (target) {
            GlslTarget.ES_100 -> buildString {
                append("#version 100\n")
                if (stage == ShaderStage.FRAGMENT && !hasDefaultFloatPrecision(body)) {
                    append("precision mediump float;\n")
                }
                append(body)
            }
            GlslTarget.DESKTOP_120 -> buildString {
                append("#version 120\n")
                append(neutralisePrecision(body))
            }
        }
    }

    private fun stripLeadingVersion(source: String): String {
        val trimmed = source.trimStart('﻿', ' ', '\t', '\n', '\r')
        if (!trimmed.startsWith("#version")) return source
        val newline = trimmed.indexOf('\n')
        return if (newline < 0) "" else trimmed.substring(newline + 1)
    }

    private fun hasDefaultFloatPrecision(source: String): Boolean =
        DEFAULT_PRECISION.containsMatchIn(source)

    private fun neutralisePrecision(source: String): String =
        source
            // Drop whole `precision <p> <type>;` default-precision statements.
            .replace(DEFAULT_PRECISION, "")
            // Drop inline precision qualifiers, keeping the declaration they modify.
            .replace(INLINE_PRECISION, "")

    private companion object {
        val DEFAULT_PRECISION = Regex("""precision\s+(lowp|mediump|highp)\s+\w+\s*;\s*""")
        val INLINE_PRECISION = Regex("""\b(lowp|mediump|highp)\s+""")
    }
}

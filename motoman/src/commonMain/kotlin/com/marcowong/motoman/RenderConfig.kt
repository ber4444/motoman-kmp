package com.marcowong.motoman

/**
 * Port of the original game's `ConfigHelper` render settings.
 *
 * The defaults here are the original's values, not "sensible" ones — this type exists so the
 * KMP renderer can be held to what the 2013 game actually did. Two of them are easy to get
 * wrong because the nicer-sounding option is the wrong one:
 *
 *  - [resolutionReduction] renders the 3D scene at **half** size and upscales. The original
 *    looks soft and slightly chunky because of this; rendering at full resolution is not a
 *    better image, it is a different one.
 *  - [frameBufferLinearFilter] and [modelTextureLinearFilter] are **off**. Post-process
 *    buffers and model textures are point-sampled except where a pass explicitly flips to
 *    linear for its own duration.
 *
 * Every flag is also a bisect handle: parity work needs to enable one effect at a time, which
 * is impossible while these are hardcoded inside the renderer.
 */
data class RenderConfig(
    /** Scene is rendered into a framebuffer this fraction of the output size. Original: 1/2. */
    val resolutionReduction: Float = 0.5f,
    /** Original: false — model textures are point-sampled. */
    val modelTextureLinearFilter: Boolean = false,
    /** Original: false — post-process buffers are point-sampled. */
    val frameBufferLinearFilter: Boolean = false,
    val bloom: Boolean = true,
    val motionBlur: Boolean = true,
    val antiAliasing: Boolean = true,
) {
    companion object {
        /** The original game's settings. */
        val ORIGINAL = RenderConfig()

        /**
         * Sharper-than-original preset for desktop, where the half-resolution look is a
         * limitation rather than a goal: the scene renders at full output resolution and both
         * model textures and post-process buffers are linearly filtered. Effects (bloom, motion
         * blur, AA) stay on. Not used for parity captures — [ORIGINAL] is the golden baseline.
         */
        val HIGH_QUALITY = RenderConfig(
            resolutionReduction = 1f,
            modelTextureLinearFilter = true,
            frameBufferLinearFilter = true,
        )
    }
}

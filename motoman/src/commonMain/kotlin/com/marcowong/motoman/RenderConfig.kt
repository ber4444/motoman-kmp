package com.marcowong.motoman

/**
 * Port of the original game's `ConfigHelper` render settings.
 *
 * Two presets, and which one you want depends on what you are doing:
 *
 *  - [HIGH_QUALITY] is what every platform ships with. Full-resolution scene, linearly
 *    filtered textures and post-process buffers.
 *  - [ORIGINAL] reproduces what the 2013 game actually did. Two of its values are easy to
 *    get wrong because the nicer-sounding option is the wrong one: [resolutionReduction]
 *    renders the 3D scene at **half** size and upscales, and [frameBufferLinearFilter] and
 *    [modelTextureLinearFilter] are **off** so buffers and model textures are point-sampled
 *    except where a pass explicitly flips to linear for its own duration. The original looks
 *    soft and slightly chunky because of this; rendering it sharp is not a better image, it
 *    is a different one.
 *
 * The bare constructor defaults match [HIGH_QUALITY], so a `RenderConfig()` with no arguments
 * is the shipping configuration rather than the parity one.
 *
 * Every flag is also a bisect handle: parity work needs to enable one effect at a time, which
 * is impossible while these are hardcoded inside the renderer. `DesktopSmoke` exposes each one
 * as a CLI flag on top of either preset (`--parity`, `--res`, `--tex-linear`/`--tex-nearest`,
 * `--fb-linear`/`--fb-nearest`, `--no-bloom`, `--no-mb`, `--no-aa`), which is how render
 * regressions get narrowed down to a single effect. `GoldenSceneTest` depends on `--parity`:
 * the golden frame is an [ORIGINAL] render, so dropping the flag silently turns that test into
 * a comparison against a configuration it was never captured at.
 */
data class RenderConfig(
    /** Scene is rendered into a framebuffer this fraction of the output size. Original: 1/2. */
    val resolutionReduction: Float = 1f,
    /** Original: false — model textures are point-sampled. */
    val modelTextureLinearFilter: Boolean = true,
    /** Original: false — post-process buffers are point-sampled. */
    val frameBufferLinearFilter: Boolean = true,
    val bloom: Boolean = true,
    val motionBlur: Boolean = true,
    val antiAliasing: Boolean = true,
) {
    companion object {
        /** The shipping configuration on every platform: full resolution, linearly filtered. */
        val HIGH_QUALITY = RenderConfig()

        /** The original 2013 game's settings; the baseline `GoldenSceneTest` is captured at. */
        val ORIGINAL = RenderConfig(
            resolutionReduction = 0.5f,
            modelTextureLinearFilter = false,
            frameBufferLinearFilter = false,
        )
    }
}

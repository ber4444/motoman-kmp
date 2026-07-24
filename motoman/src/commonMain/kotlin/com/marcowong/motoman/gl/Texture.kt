package com.marcowong.motoman.gl

/** Minification/magnification filters. Values are the GL enums. */
enum class TextureFilter(val glEnum: Int) {
    Nearest(GL_NEAREST),
    Linear(GL_LINEAR),
    MipMapNearestNearest(GL_NEAREST_MIPMAP_NEAREST),
    MipMapLinearNearest(GL_LINEAR_MIPMAP_NEAREST),
    MipMapNearestLinear(GL_NEAREST_MIPMAP_LINEAR),
    MipMapLinearLinear(GL_LINEAR_MIPMAP_LINEAR);

    val isMipMap: Boolean
        get() = this != Nearest && this != Linear
}

/** Texture coordinate wrap modes. Values are the GL enums. */
enum class TextureWrap(val glEnum: Int) {
    ClampToEdge(GL_CLAMP_TO_EDGE),
    Repeat(GL_REPEAT),
}

/**
 * A GL texture object. Port of the parts of libGDX `Texture` the engine uses, with
 * `TextureData` folded in — construction takes an already-decoded [Pixmap].
 *
 * **Wrap defaults to [TextureWrap.ClampToEdge], matching libGDX.** This is not cosmetic: many
 * of the game's textures are non-power-of-two (`tile.jpg` is 1229x1142, `hills.jpg` 1000x1000,
 * `tile7.jpg` 512x384). Under GLES 2.0 an NPOT texture with `GL_REPEAT` — or a mipmapping min
 * filter — is *incomplete* and samples as transparent black, so the surface silently vanishes
 * on Android while rendering correctly on desktop GL 2.1, which supports NPOT fully.
 */
class Texture(
    private val gl: Gl,
    val width: Int,
    val height: Int,
    pixels: ByteArray?,
    glFormat: Int,
    minFilter: TextureFilter = TextureFilter.Linear,
    magFilter: TextureFilter = TextureFilter.Linear,
    uWrap: TextureWrap = TextureWrap.ClampToEdge,
    vWrap: TextureWrap = TextureWrap.ClampToEdge,
) {
    constructor(
        gl: Gl,
        pixmap: Pixmap,
        minFilter: TextureFilter = TextureFilter.Linear,
        magFilter: TextureFilter = TextureFilter.Linear,
        uWrap: TextureWrap = TextureWrap.ClampToEdge,
        vWrap: TextureWrap = TextureWrap.ClampToEdge,
    ) : this(gl, pixmap.width, pixmap.height, pixmap.pixels, pixmap.glFormat, minFilter, magFilter, uWrap, vWrap)

    constructor(
        gl: Gl,
        width: Int,
        height: Int,
        glFormat: Int = GL_RGBA,
        minFilter: TextureFilter = TextureFilter.Linear,
        magFilter: TextureFilter = TextureFilter.Linear,
        uWrap: TextureWrap = TextureWrap.ClampToEdge,
        vWrap: TextureWrap = TextureWrap.ClampToEdge,
    ) : this(gl, width, height, null, glFormat, minFilter, magFilter, uWrap, vWrap)

    var handle: Int = gl.glGenTexture()
        private set

    var minFilter: TextureFilter = minFilter
        private set
    var magFilter: TextureFilter = magFilter
        private set
    var uWrap: TextureWrap = uWrap
        private set
    var vWrap: TextureWrap = vWrap
        private set

    init {
        gl.glBindTexture(GL_TEXTURE_2D, handle)
        gl.glTexImage2D(
            GL_TEXTURE_2D, 0, glFormat,
            width, height, 0,
            glFormat, GL_UNSIGNED_BYTE, pixels,
        )
        // A mipmapping min filter needs a mip chain to exist first. On GLES2 a non-power-of-two
        // texture can only be mipmapped when GL_OES_texture_npot is present — otherwise mipmaps
        // (and REPEAT wrap) make the texture incomplete and it samples black. Power-of-two
        // textures are always safe. Where mipmaps aren't allowed, fall back to plain Linear so
        // the texture stays valid rather than sampling black.
        if (minFilter.isMipMap && pixels != null) {
            if (isPowerOfTwo(width) && isPowerOfTwo(height) || supportsNpotMipmaps(gl)) {
                gl.glGenerateMipmap(GL_TEXTURE_2D)
            } else {
                this.minFilter = TextureFilter.Linear
            }
        }
        applyFilters()
        applyWraps()
    }

    private fun applyFilters() {
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, minFilter.glEnum)
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, magFilter.glEnum)
    }

    private fun applyWraps() {
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, uWrap.glEnum)
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, vWrap.glEnum)
    }

    /** Binds to texture unit [unit] (0-based). */
    fun bind(unit: Int = 0) {
        gl.glActiveTexture(GL_TEXTURE0 + unit)
        gl.glBindTexture(GL_TEXTURE_2D, handle)
    }

    fun setFilter(min: TextureFilter, mag: TextureFilter) {
        minFilter = min
        magFilter = mag
        gl.glBindTexture(GL_TEXTURE_2D, handle)
        applyFilters()
    }

    fun setWrap(u: TextureWrap, v: TextureWrap) {
        uWrap = u
        vWrap = v
        gl.glBindTexture(GL_TEXTURE_2D, handle)
        applyWraps()
    }

    fun dispose() {
        if (handle != 0) {
            gl.glDeleteTexture(handle)
            handle = 0
        }
    }
}

private fun isPowerOfTwo(n: Int): Boolean = n > 0 && (n and (n - 1)) == 0

/**
 * Whether the context can mipmap (and REPEAT-wrap) non-power-of-two textures. Desktop GL 2.x
 * has NPOT in core and lists `GL_ARB_texture_non_power_of_two`; GLES 2.0 needs
 * `GL_OES_texture_npot`. Absent either, an NPOT texture must stay non-mipmapped.
 */
private fun supportsNpotMipmaps(gl: Gl): Boolean {
    val ext = gl.glGetString(GL_EXTENSIONS) ?: return false
    return "GL_OES_texture_npot" in ext || "texture_non_power_of_two" in ext
}

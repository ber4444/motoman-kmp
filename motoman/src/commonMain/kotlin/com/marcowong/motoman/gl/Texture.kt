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
 */
class Texture(
    private val gl: Gl,
    pixmap: Pixmap,
    minFilter: TextureFilter = TextureFilter.Linear,
    magFilter: TextureFilter = TextureFilter.Linear,
    uWrap: TextureWrap = TextureWrap.Repeat,
    vWrap: TextureWrap = TextureWrap.Repeat,
) {
    @JvmField val width: Int = pixmap.width
    @JvmField val height: Int = pixmap.height

    private var handle: Int = gl.glGenTexture()

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
            GL_TEXTURE_2D, 0, pixmap.glFormat,
            pixmap.width, pixmap.height, 0,
            pixmap.glFormat, GL_UNSIGNED_BYTE, pixmap.pixels,
        )
        // Mipmaps must exist before a mipmapping min filter is legal.
        if (minFilter.isMipMap) gl.glGenerateMipmap(GL_TEXTURE_2D)
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

package com.marcowong.motoman.gl

class FrameBuffer(
    private val gl: Gl,
    val width: Int,
    val height: Int,
    hasDepth: Boolean = true
) {
    private val frameBufferHandle: Int
    private val depthBufferHandle: Int
    val texture: Texture

    init {
        texture = Texture(gl, width, height, glFormat = GL_RGBA)
        texture.setFilter(TextureFilter.Linear, TextureFilter.Linear)
        texture.setWrap(TextureWrap.ClampToEdge, TextureWrap.ClampToEdge)

        frameBufferHandle = gl.glGenFramebuffer()
        gl.glBindFramebuffer(GL_FRAMEBUFFER, frameBufferHandle)

        gl.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture.handle, 0)

        if (hasDepth) {
            depthBufferHandle = gl.glGenRenderbuffer()
            gl.glBindRenderbuffer(GL_RENDERBUFFER, depthBufferHandle)
            // Prefer a 24-bit depth buffer. A 16-bit buffer over the scene's near=1..far=1500
            // range leaves only ~4 world units of depth resolution near the horizon, so the
            // ground tile and the distant road/track segments collapse into the same depth
            // slice and Z-fight — the flickering "low quality" band the player sees. 24 bits
            // gives 256x the resolution and settles it. Desktop GL has it in core; on GLES2 it
            // needs GL_OES_depth24, so fall back to 16-bit where the attachment is unsupported.
            gl.glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, width, height)
            gl.glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthBufferHandle)
            if (gl.glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
                gl.glBindRenderbuffer(GL_RENDERBUFFER, depthBufferHandle)
                gl.glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT16, width, height)
            }
        } else {
            depthBufferHandle = 0
        }

        val status = gl.glCheckFramebufferStatus(GL_FRAMEBUFFER)
        check(status == GL_FRAMEBUFFER_COMPLETE) { "Framebuffer incomplete: 0x${status.toString(16)}" }

        gl.glBindFramebuffer(GL_FRAMEBUFFER, gl.defaultFramebuffer)
    }

    fun bind() {
        gl.glBindFramebuffer(GL_FRAMEBUFFER, frameBufferHandle)
        gl.glViewport(0, 0, width, height)
    }

    fun end() {
        gl.glBindFramebuffer(GL_FRAMEBUFFER, gl.defaultFramebuffer)
    }

    fun dispose() {
        texture.dispose()
        if (depthBufferHandle != 0) {
            gl.glDeleteRenderbuffer(depthBufferHandle)
        }
        gl.glDeleteFramebuffer(frameBufferHandle)
    }
}

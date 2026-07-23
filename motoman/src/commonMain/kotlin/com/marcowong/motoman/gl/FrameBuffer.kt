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
            gl.glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT16, width, height)
            gl.glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthBufferHandle)
        } else {
            depthBufferHandle = 0
        }

        val status = gl.glCheckFramebufferStatus(GL_FRAMEBUFFER)
        check(status == GL_FRAMEBUFFER_COMPLETE) { "Framebuffer incomplete: 0x${status.toString(16)}" }

        gl.glBindFramebuffer(GL_FRAMEBUFFER, 0)
    }

    fun bind() {
        gl.glBindFramebuffer(GL_FRAMEBUFFER, frameBufferHandle)
        gl.glViewport(0, 0, width, height)
    }

    fun end() {
        gl.glBindFramebuffer(GL_FRAMEBUFFER, 0)
    }

    fun dispose() {
        texture.dispose()
        if (depthBufferHandle != 0) {
            gl.glDeleteRenderbuffer(depthBufferHandle)
        }
        gl.glDeleteFramebuffer(frameBufferHandle)
    }
}

package com.marcowong.motoman.gl

/**
 * The engine's own GL abstraction. Deliberately a plain interface (not an `expect object`)
 * so it can be decorated: [GlOptimized] elides redundant state changes and [GlDebug] logs
 * and asserts on `glGetError`. Composition is the point — `GlOptimized(GlDebug(createPlatformGl()))`.
 *
 * The surface is only what the Motoman renderer actually calls (a small subset of GLES 2.0),
 * ported from the original `GL20Optimized`/`GL20Debug` decorators and `MeshOptimized`.
 *
 * Uploads take Kotlin arrays rather than `java.nio` buffers so this interface is valid in
 * `commonMain` (Android, desktop, and a future Kotlin/Native target); each platform `actual`
 * marshals arrays into whatever native buffer its GL binding requires.
 */
interface Gl {
    /** The default framebuffer to bind when rendering to the screen (0 on Android/Desktop, custom FBO on iOS GLKit). */
    val defaultFramebuffer: Int

    // ---- Whole-frame state ----
    fun glViewport(x: Int, y: Int, width: Int, height: Int)
    fun glClear(mask: Int)
    fun glClearColor(red: Float, green: Float, blue: Float, alpha: Float)
    fun glEnable(cap: Int)
    fun glDisable(cap: Int)
    fun glDepthFunc(func: Int)
    fun glDepthMask(flag: Boolean)
    fun glCullFace(mode: Int)
    fun glFrontFace(mode: Int)
    fun glBlendFunc(sfactor: Int, dfactor: Int)

    // ---- Buffers (VBO/IBO) ----
    fun glGenBuffer(): Int
    fun glBindBuffer(target: Int, buffer: Int)
    fun glBufferData(target: Int, data: FloatArray, usage: Int)
    fun glBufferData(target: Int, data: ShortArray, usage: Int)
    fun glDeleteBuffer(buffer: Int)

    // ---- Vertex attributes ----
    fun glEnableVertexAttribArray(index: Int)
    fun glDisableVertexAttribArray(index: Int)
    /** VBO-offset form: [offsetBytes] is a byte offset into the bound GL_ARRAY_BUFFER. */
    fun glVertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, strideBytes: Int, offsetBytes: Int)

    // ---- Drawing ----
    fun glDrawArrays(mode: Int, first: Int, count: Int)
    /** VBO-offset form: [offsetBytes] is a byte offset into the bound GL_ELEMENT_ARRAY_BUFFER. */
    fun glDrawElements(mode: Int, count: Int, type: Int, offsetBytes: Int)

    // ---- Shaders / programs ----
    fun glCreateShader(type: Int): Int
    fun glShaderSource(shader: Int, source: String)
    fun glCompileShader(shader: Int)
    fun glGetShaderCompileStatus(shader: Int): Boolean
    fun glGetShaderInfoLog(shader: Int): String
    fun glDeleteShader(shader: Int)
    fun glCreateProgram(): Int
    fun glAttachShader(program: Int, shader: Int)
    fun glBindAttribLocation(program: Int, index: Int, name: String)
    fun glLinkProgram(program: Int)
    fun glGetProgramLinkStatus(program: Int): Boolean
    fun glGetProgramInfoLog(program: Int): String
    fun glUseProgram(program: Int)
    fun glDeleteProgram(program: Int)
    fun glGetAttribLocation(program: Int, name: String): Int
    fun glGetUniformLocation(program: Int, name: String): Int

    // ---- Uniforms ----
    fun glUniform1i(location: Int, x: Int)
    fun glUniform1f(location: Int, x: Float)
    fun glUniform2f(location: Int, x: Float, y: Float)
    fun glUniform3f(location: Int, x: Float, y: Float, z: Float)
    fun glUniform4f(location: Int, x: Float, y: Float, z: Float, w: Float)
    fun glUniformMatrix4fv(location: Int, transpose: Boolean, value: FloatArray)

    // ---- Textures ----
    fun glActiveTexture(texture: Int)
    fun glGenTexture(): Int
    fun glBindTexture(target: Int, texture: Int)
    fun glTexParameteri(target: Int, pname: Int, param: Int)
    /** RGBA/RGB pixel upload. [pixels] is tightly packed, `null` allocates without initialising. */
    fun glTexImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, format: Int, type: Int, pixels: ByteArray?)
    fun glGenerateMipmap(target: Int)
    fun glDeleteTexture(texture: Int)

    // ---- Framebuffers & Renderbuffers ----
    fun glGenFramebuffer(): Int
    fun glBindFramebuffer(target: Int, framebuffer: Int)
    fun glDeleteFramebuffer(framebuffer: Int)
    fun glFramebufferTexture2D(target: Int, attachment: Int, textarget: Int, texture: Int, level: Int)
    fun glCheckFramebufferStatus(target: Int): Int
    fun glGenRenderbuffer(): Int
    fun glBindRenderbuffer(target: Int, renderbuffer: Int)
    fun glDeleteRenderbuffer(renderbuffer: Int)
    fun glRenderbufferStorage(target: Int, internalformat: Int, width: Int, height: Int)
    fun glFramebufferRenderbuffer(target: Int, attachment: Int, renderbuffertarget: Int, renderbuffer: Int)

    /**
     * Reads back a rectangle of the framebuffer as tightly packed bytes. Used to assert
     * that a frame actually drew something rather than trusting that it did.
     */
    fun glReadPixels(x: Int, y: Int, width: Int, height: Int, format: Int, type: Int): ByteArray

    // ---- Diagnostics ----
    fun glGetError(): Int
    fun glGetString(name: Int): String?
}

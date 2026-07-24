package com.marcowong.motoman.gl

import platform.gles2.*
import platform.glescommon.*
import kotlinx.cinterop.*

actual fun createPlatformGl(): Gl = IosGl()

class IosGl : Gl {
    override var defaultFramebuffer: Int = 0

    override fun glViewport(x: Int, y: Int, width: Int, height: Int) {
        platform.gles2.glViewport(x, y, width, height)
    }

    override fun glClear(mask: Int) {
        platform.gles2.glClear(mask.convert())
    }

    override fun glClearColor(red: Float, green: Float, blue: Float, alpha: Float) {
        platform.gles2.glClearColor(1.0f, 0.0f, 0.0f, 1.0f)
    }

    override fun glEnable(cap: Int) {
        platform.gles2.glEnable(cap.convert())
    }

    override fun glDisable(cap: Int) {
        platform.gles2.glDisable(cap.convert())
    }

    override fun glDepthFunc(func: Int) {
        platform.gles2.glDepthFunc(func.convert())
    }

    override fun glDepthMask(flag: Boolean) {
        platform.gles2.glDepthMask(if (flag) 1u else 0u)
    }

    override fun glCullFace(mode: Int) {
        platform.gles2.glCullFace(mode.convert())
    }

    override fun glFrontFace(mode: Int) {
        platform.gles2.glFrontFace(mode.convert())
    }

    override fun glBlendFunc(sfactor: Int, dfactor: Int) {
        platform.gles2.glBlendFunc(sfactor.convert(), dfactor.convert())
    }

    override fun glGenBuffer(): Int = memScoped {
        val id = alloc<GLuintVar>()
        platform.gles2.glGenBuffers(1, id.ptr)
        id.value.toInt()
    }

    override fun glBindBuffer(target: Int, buffer: Int) {
        platform.gles2.glBindBuffer(target.convert(), buffer.convert())
    }

    override fun glBufferData(target: Int, data: FloatArray, usage: Int) {
        data.usePinned { pinned ->
            platform.gles2.glBufferData(target.convert(), (data.size * 4).convert(), pinned.addressOf(0), usage.convert())
        }
    }

    override fun glBufferData(target: Int, data: ShortArray, usage: Int) {
        data.usePinned { pinned ->
            platform.gles2.glBufferData(target.convert(), (data.size * 2).convert(), pinned.addressOf(0), usage.convert())
        }
    }

    override fun glDeleteBuffer(buffer: Int) = memScoped {
        val id = alloc<GLuintVar>()
        id.value = buffer.convert()
        platform.gles2.glDeleteBuffers(1, id.ptr)
    }

    override fun glEnableVertexAttribArray(index: Int) {
        platform.gles2.glEnableVertexAttribArray(index.convert())
    }

    override fun glDisableVertexAttribArray(index: Int) {
        platform.gles2.glDisableVertexAttribArray(index.convert())
    }

    override fun glVertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, strideBytes: Int, offsetBytes: Int) {
        platform.gles2.glVertexAttribPointer(index.convert(), size.convert(), type.convert(), if (normalized) 1u else 0u, strideBytes.convert(), offsetBytes.toLong().toCPointer<CPointed>())
    }

    override fun glDrawArrays(mode: Int, first: Int, count: Int) {
        platform.gles2.glDrawArrays(mode.convert(), first.convert(), count.convert())
    }

    override fun glDrawElements(mode: Int, count: Int, type: Int, offsetBytes: Int) {
        platform.gles2.glDrawElements(mode.convert(), count.convert(), type.convert(), offsetBytes.toLong().toCPointer<CPointed>())
    }

    override fun glCreateShader(type: Int): Int {
        return platform.gles2.glCreateShader(type.convert()).toInt()
    }

    override fun glShaderSource(shader: Int, source: String) = memScoped {
        val arr = allocArrayOf(source.cstr.ptr)
        platform.gles2.glShaderSource(shader.convert(), 1, arr, null)
    }

    override fun glCompileShader(shader: Int) {
        platform.gles2.glCompileShader(shader.convert())
    }

    override fun glGetShaderCompileStatus(shader: Int): Boolean = memScoped {
        val status = alloc<GLintVar>()
        platform.gles2.glGetShaderiv(shader.convert(), GL_COMPILE_STATUS.convert(), status.ptr)
        status.value != 0
    }

    override fun glGetShaderInfoLog(shader: Int): String = memScoped {
        val len = alloc<GLintVar>()
        platform.gles2.glGetShaderiv(shader.convert(), GL_INFO_LOG_LENGTH.convert(), len.ptr)
        if (len.value <= 0) return ""
        val log = allocArray<ByteVar>(len.value)
        platform.gles2.glGetShaderInfoLog(shader.convert(), len.value, null, log)
        log.toKString()
    }

    override fun glDeleteShader(shader: Int) {
        platform.gles2.glDeleteShader(shader.convert())
    }

    override fun glCreateProgram(): Int {
        return platform.gles2.glCreateProgram().toInt()
    }

    override fun glAttachShader(program: Int, shader: Int) {
        platform.gles2.glAttachShader(program.convert(), shader.convert())
    }

    override fun glBindAttribLocation(program: Int, index: Int, name: String) {
        platform.gles2.glBindAttribLocation(program.convert(), index.convert(), name)
    }

    override fun glLinkProgram(program: Int) {
        platform.gles2.glLinkProgram(program.convert())
    }

    override fun glGetProgramLinkStatus(program: Int): Boolean = memScoped {
        val status = alloc<GLintVar>()
        platform.gles2.glGetProgramiv(program.convert(), GL_LINK_STATUS.convert(), status.ptr)
        status.value != 0
    }

    override fun glGetProgramInfoLog(program: Int): String = memScoped {
        val len = alloc<GLintVar>()
        platform.gles2.glGetProgramiv(program.convert(), GL_INFO_LOG_LENGTH.convert(), len.ptr)
        if (len.value <= 0) return ""
        val log = allocArray<ByteVar>(len.value)
        platform.gles2.glGetProgramInfoLog(program.convert(), len.value, null, log)
        log.toKString()
    }

    override fun glUseProgram(program: Int) {
        platform.gles2.glUseProgram(program.convert())
    }

    override fun glDeleteProgram(program: Int) {
        platform.gles2.glDeleteProgram(program.convert())
    }

    override fun glGetAttribLocation(program: Int, name: String): Int {
        return platform.gles2.glGetAttribLocation(program.convert(), name)
    }

    override fun glGetUniformLocation(program: Int, name: String): Int {
        return platform.gles2.glGetUniformLocation(program.convert(), name)
    }

    override fun glUniform1i(location: Int, x: Int) {
        platform.gles2.glUniform1i(location.convert(), x)
    }

    override fun glUniform1f(location: Int, x: Float) {
        platform.gles2.glUniform1f(location.convert(), x)
    }

    override fun glUniform2f(location: Int, x: Float, y: Float) {
        platform.gles2.glUniform2f(location.convert(), x, y)
    }

    override fun glUniform3f(location: Int, x: Float, y: Float, z: Float) {
        platform.gles2.glUniform3f(location.convert(), x, y, z)
    }

    override fun glUniform4f(location: Int, x: Float, y: Float, z: Float, w: Float) {
        platform.gles2.glUniform4f(location.convert(), x, y, z, w)
    }

    override fun glUniformMatrix4fv(location: Int, transpose: Boolean, value: FloatArray) {
        value.usePinned { pinned ->
            platform.gles2.glUniformMatrix4fv(location.convert(), 1, if (transpose) 1u else 0u, pinned.addressOf(0))
        }
    }

    override fun glActiveTexture(texture: Int) {
        platform.gles2.glActiveTexture(texture.convert())
    }

    override fun glGenTexture(): Int = memScoped {
        val id = alloc<GLuintVar>()
        platform.gles2.glGenTextures(1, id.ptr)
        id.value.toInt()
    }

    override fun glBindTexture(target: Int, texture: Int) {
        platform.gles2.glBindTexture(target.convert(), texture.convert())
    }

    override fun glTexParameteri(target: Int, pname: Int, param: Int) {
        platform.gles2.glTexParameteri(target.convert(), pname.convert(), param.convert())
    }

    override fun glTexImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, format: Int, type: Int, pixels: ByteArray?) {
        if (pixels == null) {
            platform.gles2.glTexImage2D(target.convert(), level.convert(), internalformat.convert(), width.convert(), height.convert(), border.convert(), format.convert(), type.convert(), null)
        } else {
            pixels.usePinned { pinned ->
                platform.gles2.glTexImage2D(target.convert(), level.convert(), internalformat.convert(), width.convert(), height.convert(), border.convert(), format.convert(), type.convert(), pinned.addressOf(0))
            }
        }
    }

    override fun glGenerateMipmap(target: Int) {
        platform.gles2.glGenerateMipmap(target.convert())
    }

    override fun glDeleteTexture(texture: Int) = memScoped {
        val id = alloc<GLuintVar>()
        id.value = texture.convert()
        platform.gles2.glDeleteTextures(1, id.ptr)
    }

    override fun glGenFramebuffer(): Int = memScoped {
        val id = alloc<GLuintVar>()
        platform.gles2.glGenFramebuffers(1, id.ptr)
        id.value.toInt()
    }

    override fun glBindFramebuffer(target: Int, framebuffer: Int) {
        platform.gles2.glBindFramebuffer(target.convert(), framebuffer.convert())
    }

    override fun glDeleteFramebuffer(framebuffer: Int) = memScoped {
        val id = alloc<GLuintVar>()
        id.value = framebuffer.convert()
        platform.gles2.glDeleteFramebuffers(1, id.ptr)
    }

    override fun glFramebufferTexture2D(target: Int, attachment: Int, textarget: Int, texture: Int, level: Int) {
        platform.gles2.glFramebufferTexture2D(target.convert(), attachment.convert(), textarget.convert(), texture.convert(), level.convert())
    }

    override fun glCheckFramebufferStatus(target: Int): Int {
        return platform.gles2.glCheckFramebufferStatus(target.convert()).toInt()
    }

    override fun glGenRenderbuffer(): Int = memScoped {
        val id = alloc<GLuintVar>()
        platform.gles2.glGenRenderbuffers(1, id.ptr)
        id.value.toInt()
    }

    override fun glBindRenderbuffer(target: Int, renderbuffer: Int) {
        platform.gles2.glBindRenderbuffer(target.convert(), renderbuffer.convert())
    }

    override fun glDeleteRenderbuffer(renderbuffer: Int) = memScoped {
        val id = alloc<GLuintVar>()
        id.value = renderbuffer.convert()
        platform.gles2.glDeleteRenderbuffers(1, id.ptr)
    }

    override fun glRenderbufferStorage(target: Int, internalformat: Int, width: Int, height: Int) {
        platform.gles2.glRenderbufferStorage(target.convert(), internalformat.convert(), width.convert(), height.convert())
    }

    override fun glFramebufferRenderbuffer(target: Int, attachment: Int, renderbuffertarget: Int, renderbuffer: Int) {
        platform.gles2.glFramebufferRenderbuffer(target.convert(), attachment.convert(), renderbuffertarget.convert(), renderbuffer.convert())
    }

    override fun glReadPixels(x: Int, y: Int, width: Int, height: Int, format: Int, type: Int): ByteArray {
        // According to Step 2, glReadPixels returns a ByteArray: allocate width * height * bytesPerPixel
        val bpp = 4 // Assuming RGBA
        val arr = ByteArray(width * height * bpp)
        arr.usePinned { pinned ->
            platform.gles2.glReadPixels(x.convert(), y.convert(), width.convert(), height.convert(), format.convert(), type.convert(), pinned.addressOf(0))
        }
        return arr
    }

    override fun glGetError(): Int {
        return platform.gles2.glGetError().toInt()
    }

    override fun glGetString(name: Int): String? {
        val ptr = platform.gles2.glGetString(name.convert())
        return ptr?.reinterpret<ByteVar>()?.toKString()
    }
}

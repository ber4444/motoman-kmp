@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.marcowong.motoman.gl

import mwgl.*
import kotlinx.cinterop.*

actual fun createPlatformGl(): Gl = IosGl()

class IosGl : Gl {
    override var defaultFramebuffer: Int = 0

    override fun glViewport(x: Int, y: Int, width: Int, height: Int) {
        mwgl_viewport(x, y, width, height)
    }

    override fun glClear(mask: Int) {
        mwgl_clear(mask)
    }

    override fun glClearColor(red: Float, green: Float, blue: Float, alpha: Float) {
        mwgl_clear_color(red, green, blue, alpha)
    }

    override fun glEnable(cap: Int) {
        mwgl_enable(cap)
    }

    override fun glDisable(cap: Int) {
        mwgl_disable(cap)
    }

    override fun glDepthFunc(func: Int) {
        mwgl_depth_func(func)
    }

    override fun glDepthMask(flag: Boolean) {
        mwgl_depth_mask(if (flag) 1 else 0)
    }

    override fun glCullFace(mode: Int) {
        mwgl_cull_face(mode)
    }

    override fun glFrontFace(mode: Int) {
        mwgl_front_face(mode)
    }

    override fun glBlendFunc(sfactor: Int, dfactor: Int) {
        mwgl_blend_func(sfactor, dfactor)
    }

    override fun glGenBuffer(): Int = mwgl_create_buffer()

    override fun glBindBuffer(target: Int, buffer: Int) {
        mwgl_bind_buffer(target, buffer)
    }

    override fun glBufferData(target: Int, data: FloatArray, usage: Int) {
        data.usePinned { pinned ->
            mwgl_buffer_data(target, pinned.addressOf(0), data.size, usage)
        }
    }

    override fun glBufferData(target: Int, data: ShortArray, usage: Int) {
        data.usePinned { pinned ->
            mwgl_buffer_data_short(target, pinned.addressOf(0), data.size, usage)
        }
    }

    override fun glDeleteBuffer(buffer: Int) {
        mwgl_delete_buffer(buffer)
    }

    override fun glEnableVertexAttribArray(index: Int) {
        mwgl_enable_vertex_attrib_array(index)
    }

    override fun glDisableVertexAttribArray(index: Int) {
        mwgl_disable_vertex_attrib_array(index)
    }

    override fun glVertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, strideBytes: Int, offsetBytes: Int) {
        mwgl_vertex_attrib_pointer(index, size, type, if (normalized) 1 else 0, strideBytes, offsetBytes)
    }

    override fun glDrawArrays(mode: Int, first: Int, count: Int) {
        mwgl_draw_arrays(mode, first, count)
    }

    override fun glDrawElements(mode: Int, count: Int, type: Int, offsetBytes: Int) {
        mwgl_draw_elements(mode, count, type, offsetBytes)
    }

    override fun glCreateShader(type: Int): Int {
        return mwgl_create_shader(type)
    }

    override fun glShaderSource(shader: Int, source: String) {
        mwgl_shader_source(shader, source)
    }

    override fun glCompileShader(shader: Int) {
        mwgl_compile_shader(shader)
    }

    override fun glGetShaderCompileStatus(shader: Int): Boolean {
        return mwgl_get_shader_compile_status(shader) != 0
    }

    override fun glGetShaderInfoLog(shader: Int): String {
        val bufSize = 2048
        val log = ByteArray(bufSize)
        log.usePinned { pinned ->
            mwgl_get_shader_info_log(shader, pinned.addressOf(0), bufSize)
        }
        return log.toKString()
    }

    override fun glDeleteShader(shader: Int) {
        mwgl_delete_shader(shader)
    }

    override fun glCreateProgram(): Int {
        return mwgl_create_program_id()
    }

    override fun glAttachShader(program: Int, shader: Int) {
        mwgl_attach_shader(program, shader)
    }

    override fun glBindAttribLocation(program: Int, index: Int, name: String) {
        mwgl_bind_attrib_location(program, index, name)
    }

    override fun glLinkProgram(program: Int) {
        mwgl_link_program(program)
    }

    override fun glGetProgramLinkStatus(program: Int): Boolean {
        return mwgl_get_program_link_status(program) != 0
    }

    override fun glGetProgramInfoLog(program: Int): String {
        val bufSize = 2048
        val log = ByteArray(bufSize)
        log.usePinned { pinned ->
            mwgl_get_program_info_log(program, pinned.addressOf(0), bufSize)
        }
        return log.toKString()
    }

    override fun glUseProgram(program: Int) {
        mwgl_use_program(program)
    }

    override fun glDeleteProgram(program: Int) {
        mwgl_delete_program(program)
    }

    override fun glGetAttribLocation(program: Int, name: String): Int {
        return mwgl_attrib_location(program, name)
    }

    override fun glGetUniformLocation(program: Int, name: String): Int {
        return mwgl_uniform_location(program, name)
    }

    override fun glUniform1i(location: Int, x: Int) {
        mwgl_uniform1i(location, x)
    }

    override fun glUniform1f(location: Int, x: Float) {
        mwgl_uniform1f(location, x)
    }

    override fun glUniform2f(location: Int, x: Float, y: Float) {
        mwgl_uniform2f(location, x, y)
    }

    override fun glUniform3f(location: Int, x: Float, y: Float, z: Float) {
        mwgl_uniform3f(location, x, y, z)
    }

    override fun glUniform4f(location: Int, x: Float, y: Float, z: Float, w: Float) {
        mwgl_uniform4f(location, x, y, z, w)
    }

    override fun glUniformMatrix4fv(location: Int, transpose: Boolean, value: FloatArray) {
        value.usePinned { pinned ->
            mwgl_uniform_matrix4fv(location, 1, if (transpose) 1 else 0, pinned.addressOf(0))
        }
    }

    override fun glActiveTexture(texture: Int) {
        mwgl_active_texture(texture)
    }

    override fun glGenTexture(): Int {
        return mwgl_create_texture()
    }

    override fun glBindTexture(target: Int, texture: Int) {
        mwgl_bind_texture(target, texture)
    }

    override fun glTexParameteri(target: Int, pname: Int, param: Int) {
        mwgl_tex_parameteri(target, pname, param)
    }

    override fun glTexImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, format: Int, type: Int, pixels: ByteArray?) {
        if (pixels == null) {
            mwgl_tex_image_2d(target, level, internalformat, width, height, border, format, type, null)
        } else {
            pixels.usePinned { pinned ->
                mwgl_tex_image_2d(target, level, internalformat, width, height, border, format, type, pinned.addressOf(0))
            }
        }
    }

    override fun glGenerateMipmap(target: Int) {
        mwgl_generate_mipmap(target)
    }

    override fun glDeleteTexture(texture: Int) {
        mwgl_delete_texture(texture)
    }

    override fun glGenFramebuffer(): Int {
        return mwgl_create_framebuffer()
    }

    override fun glBindFramebuffer(target: Int, framebuffer: Int) {
        mwgl_bind_framebuffer(target, framebuffer)
    }

    override fun glDeleteFramebuffer(framebuffer: Int) {
        mwgl_delete_framebuffer(framebuffer)
    }

    override fun glFramebufferTexture2D(target: Int, attachment: Int, textarget: Int, texture: Int, level: Int) {
        mwgl_framebuffer_texture_2d(target, attachment, textarget, texture, level)
    }

    override fun glCheckFramebufferStatus(target: Int): Int {
        return mwgl_check_framebuffer_status(target)
    }

    override fun glGenRenderbuffer(): Int {
        return mwgl_create_renderbuffer()
    }

    override fun glBindRenderbuffer(target: Int, renderbuffer: Int) {
        mwgl_bind_renderbuffer(target, renderbuffer)
    }

    override fun glDeleteRenderbuffer(renderbuffer: Int) {
        mwgl_delete_renderbuffer(renderbuffer)
    }

    override fun glRenderbufferStorage(target: Int, internalformat: Int, width: Int, height: Int) {
        mwgl_renderbuffer_storage(target, internalformat, width, height)
    }

    override fun glFramebufferRenderbuffer(target: Int, attachment: Int, renderbuffertarget: Int, renderbuffer: Int) {
        mwgl_framebuffer_renderbuffer(target, attachment, renderbuffertarget, renderbuffer)
    }

    override fun glReadPixels(x: Int, y: Int, width: Int, height: Int, format: Int, type: Int): ByteArray {
        val bpp = 4 // Assuming RGBA
        val arr = ByteArray(width * height * bpp)
        arr.usePinned { pinned ->
            mwgl_read_pixels(x, y, width, height, format, type, pinned.addressOf(0))
        }
        return arr
    }

    override fun glGetError(): Int {
        return mwgl_get_error()
    }

    override fun glGetString(name: Int): String? {
        return mwgl_get_string(name)?.toKString()
    }
}

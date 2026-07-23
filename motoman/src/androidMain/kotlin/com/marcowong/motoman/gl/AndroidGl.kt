package com.marcowong.motoman.gl

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Android GL over the platform GLES 2.0 bindings. Runs on the GLSurfaceView GL thread. */
actual fun createPlatformGl(): Gl = AndroidGl()

private class AndroidGl : Gl {

    private val scratch = IntArray(1)

    override fun glViewport(x: Int, y: Int, width: Int, height: Int) = GLES20.glViewport(x, y, width, height)
    override fun glClear(mask: Int) = GLES20.glClear(mask)
    override fun glClearColor(red: Float, green: Float, blue: Float, alpha: Float) = GLES20.glClearColor(red, green, blue, alpha)
    override fun glEnable(cap: Int) = GLES20.glEnable(cap)
    override fun glDisable(cap: Int) = GLES20.glDisable(cap)
    override fun glDepthFunc(func: Int) = GLES20.glDepthFunc(func)
    override fun glDepthMask(flag: Boolean) = GLES20.glDepthMask(flag)
    override fun glCullFace(mode: Int) = GLES20.glCullFace(mode)
    override fun glFrontFace(mode: Int) = GLES20.glFrontFace(mode)
    override fun glBlendFunc(sfactor: Int, dfactor: Int) = GLES20.glBlendFunc(sfactor, dfactor)

    override fun glGenBuffer(): Int {
        GLES20.glGenBuffers(1, scratch, 0)
        return scratch[0]
    }
    override fun glBindBuffer(target: Int, buffer: Int) = GLES20.glBindBuffer(target, buffer)
    override fun glBufferData(target: Int, data: FloatArray, usage: Int) {
        val buf = ByteBuffer.allocateDirect(data.size * 4).order(ByteOrder.nativeOrder())
        buf.asFloatBuffer().put(data)
        GLES20.glBufferData(target, data.size * 4, buf, usage)
    }
    override fun glBufferData(target: Int, data: ShortArray, usage: Int) {
        val buf = ByteBuffer.allocateDirect(data.size * 2).order(ByteOrder.nativeOrder())
        buf.asShortBuffer().put(data)
        GLES20.glBufferData(target, data.size * 2, buf, usage)
    }
    override fun glDeleteBuffer(buffer: Int) {
        scratch[0] = buffer
        GLES20.glDeleteBuffers(1, scratch, 0)
    }

    override fun glEnableVertexAttribArray(index: Int) = GLES20.glEnableVertexAttribArray(index)
    override fun glDisableVertexAttribArray(index: Int) = GLES20.glDisableVertexAttribArray(index)
    override fun glVertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, strideBytes: Int, offsetBytes: Int) =
        GLES20.glVertexAttribPointer(index, size, type, normalized, strideBytes, offsetBytes)

    override fun glDrawArrays(mode: Int, first: Int, count: Int) = GLES20.glDrawArrays(mode, first, count)
    override fun glDrawElements(mode: Int, count: Int, type: Int, offsetBytes: Int) =
        GLES20.glDrawElements(mode, count, type, offsetBytes)

    override fun glCreateShader(type: Int): Int = GLES20.glCreateShader(type)
    override fun glShaderSource(shader: Int, source: String) = GLES20.glShaderSource(shader, source)
    override fun glCompileShader(shader: Int) = GLES20.glCompileShader(shader)
    override fun glGetShaderCompileStatus(shader: Int): Boolean {
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, scratch, 0)
        return scratch[0] != 0
    }
    override fun glGetShaderInfoLog(shader: Int): String = GLES20.glGetShaderInfoLog(shader)
    override fun glDeleteShader(shader: Int) = GLES20.glDeleteShader(shader)
    override fun glCreateProgram(): Int = GLES20.glCreateProgram()
    override fun glAttachShader(program: Int, shader: Int) = GLES20.glAttachShader(program, shader)
    override fun glBindAttribLocation(program: Int, index: Int, name: String) = GLES20.glBindAttribLocation(program, index, name)
    override fun glLinkProgram(program: Int) = GLES20.glLinkProgram(program)
    override fun glGetProgramLinkStatus(program: Int): Boolean {
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, scratch, 0)
        return scratch[0] != 0
    }
    override fun glGetProgramInfoLog(program: Int): String = GLES20.glGetProgramInfoLog(program)
    override fun glUseProgram(program: Int) = GLES20.glUseProgram(program)
    override fun glDeleteProgram(program: Int) = GLES20.glDeleteProgram(program)
    override fun glGetAttribLocation(program: Int, name: String): Int = GLES20.glGetAttribLocation(program, name)
    override fun glGetUniformLocation(program: Int, name: String): Int = GLES20.glGetUniformLocation(program, name)

    override fun glUniform1i(location: Int, x: Int) = GLES20.glUniform1i(location, x)
    override fun glUniform1f(location: Int, x: Float) = GLES20.glUniform1f(location, x)
    override fun glUniform2f(location: Int, x: Float, y: Float) = GLES20.glUniform2f(location, x, y)
    override fun glUniform3f(location: Int, x: Float, y: Float, z: Float) = GLES20.glUniform3f(location, x, y, z)
    override fun glUniform4f(location: Int, x: Float, y: Float, z: Float, w: Float) = GLES20.glUniform4f(location, x, y, z, w)
    override fun glUniformMatrix4fv(location: Int, transpose: Boolean, value: FloatArray) =
        GLES20.glUniformMatrix4fv(location, 1, transpose, value, 0)

    override fun glActiveTexture(texture: Int) = GLES20.glActiveTexture(texture)
    override fun glGenTexture(): Int {
        GLES20.glGenTextures(1, scratch, 0)
        return scratch[0]
    }
    override fun glBindTexture(target: Int, texture: Int) = GLES20.glBindTexture(target, texture)
    override fun glTexParameteri(target: Int, pname: Int, param: Int) = GLES20.glTexParameteri(target, pname, param)
    override fun glTexImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, format: Int, type: Int, pixels: ByteArray?) {
        val buf = pixels?.let {
            ByteBuffer.allocateDirect(it.size).order(ByteOrder.nativeOrder()).apply { put(it); position(0) }
        }
        GLES20.glTexImage2D(target, level, internalformat, width, height, border, format, type, buf)
    }
    override fun glGenerateMipmap(target: Int) = GLES20.glGenerateMipmap(target)
    override fun glDeleteTexture(texture: Int) {
        scratch[0] = texture
        GLES20.glDeleteTextures(1, scratch, 0)
    }

    override fun glReadPixels(x: Int, y: Int, width: Int, height: Int, format: Int, type: Int): ByteArray {
        val components = if (format == GL_RGBA) 4 else 3
        val buf = ByteBuffer.allocateDirect(width * height * components).order(ByteOrder.nativeOrder())
        GLES20.glReadPixels(x, y, width, height, format, type, buf)
        val out = ByteArray(width * height * components)
        buf.position(0)
        buf.get(out)
        return out
    }

    override fun glGetError(): Int = GLES20.glGetError()
    override fun glGetString(name: Int): String? = GLES20.glGetString(name)
}

package com.marcowong.motoman.gl

import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30
import java.nio.ByteBuffer

/** Desktop GL over LWJGL's static GL bindings. Requires a current GLFW/GL context. */
actual fun createPlatformGl(): Gl = DesktopGl()

private class DesktopGl : Gl {

    override fun glViewport(x: Int, y: Int, width: Int, height: Int) = GL11.glViewport(x, y, width, height)
    override fun glClear(mask: Int) = GL11.glClear(mask)
    override fun glClearColor(red: Float, green: Float, blue: Float, alpha: Float) = GL11.glClearColor(red, green, blue, alpha)
    override fun glEnable(cap: Int) = GL11.glEnable(cap)
    override fun glDisable(cap: Int) = GL11.glDisable(cap)
    override fun glDepthFunc(func: Int) = GL11.glDepthFunc(func)
    override fun glDepthMask(flag: Boolean) = GL11.glDepthMask(flag)
    override fun glCullFace(mode: Int) = GL11.glCullFace(mode)
    override fun glFrontFace(mode: Int) = GL11.glFrontFace(mode)
    override fun glBlendFunc(sfactor: Int, dfactor: Int) = GL11.glBlendFunc(sfactor, dfactor)

    override fun glGenBuffer(): Int = GL15.glGenBuffers()
    override fun glBindBuffer(target: Int, buffer: Int) = GL15.glBindBuffer(target, buffer)
    override fun glBufferData(target: Int, data: FloatArray, usage: Int) {
        val buf = BufferUtils.createFloatBuffer(data.size)
        buf.put(data).flip()
        GL15.glBufferData(target, buf, usage)
    }
    override fun glBufferData(target: Int, data: ShortArray, usage: Int) {
        val buf = BufferUtils.createShortBuffer(data.size)
        buf.put(data).flip()
        GL15.glBufferData(target, buf, usage)
    }
    override fun glDeleteBuffer(buffer: Int) = GL15.glDeleteBuffers(buffer)

    override fun glEnableVertexAttribArray(index: Int) = GL20.glEnableVertexAttribArray(index)
    override fun glDisableVertexAttribArray(index: Int) = GL20.glDisableVertexAttribArray(index)
    override fun glVertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, strideBytes: Int, offsetBytes: Int) =
        GL20.glVertexAttribPointer(index, size, type, normalized, strideBytes, offsetBytes.toLong())

    override fun glDrawArrays(mode: Int, first: Int, count: Int) = GL11.glDrawArrays(mode, first, count)
    override fun glDrawElements(mode: Int, count: Int, type: Int, offsetBytes: Int) =
        GL11.glDrawElements(mode, count, type, offsetBytes.toLong())

    override fun glCreateShader(type: Int): Int = GL20.glCreateShader(type)
    override fun glShaderSource(shader: Int, source: String) = GL20.glShaderSource(shader, source)
    override fun glCompileShader(shader: Int) = GL20.glCompileShader(shader)
    override fun glGetShaderCompileStatus(shader: Int): Boolean =
        GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) != GL11.GL_FALSE
    override fun glGetShaderInfoLog(shader: Int): String = GL20.glGetShaderInfoLog(shader)
    override fun glDeleteShader(shader: Int) = GL20.glDeleteShader(shader)
    override fun glCreateProgram(): Int = GL20.glCreateProgram()
    override fun glAttachShader(program: Int, shader: Int) = GL20.glAttachShader(program, shader)
    override fun glBindAttribLocation(program: Int, index: Int, name: String) = GL20.glBindAttribLocation(program, index, name)
    override fun glLinkProgram(program: Int) = GL20.glLinkProgram(program)
    override fun glGetProgramLinkStatus(program: Int): Boolean =
        GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) != GL11.GL_FALSE
    override fun glGetProgramInfoLog(program: Int): String = GL20.glGetProgramInfoLog(program)
    override fun glUseProgram(program: Int) = GL20.glUseProgram(program)
    override fun glDeleteProgram(program: Int) = GL20.glDeleteProgram(program)
    override fun glGetAttribLocation(program: Int, name: String): Int = GL20.glGetAttribLocation(program, name)
    override fun glGetUniformLocation(program: Int, name: String): Int = GL20.glGetUniformLocation(program, name)

    override fun glUniform1i(location: Int, x: Int) = GL20.glUniform1i(location, x)
    override fun glUniform1f(location: Int, x: Float) = GL20.glUniform1f(location, x)
    override fun glUniform2f(location: Int, x: Float, y: Float) = GL20.glUniform2f(location, x, y)
    override fun glUniform3f(location: Int, x: Float, y: Float, z: Float) = GL20.glUniform3f(location, x, y, z)
    override fun glUniform4f(location: Int, x: Float, y: Float, z: Float, w: Float) = GL20.glUniform4f(location, x, y, z, w)
    override fun glUniformMatrix4fv(location: Int, transpose: Boolean, value: FloatArray) {
        val buf = BufferUtils.createFloatBuffer(value.size)
        buf.put(value).flip()
        GL20.glUniformMatrix4fv(location, transpose, buf)
    }

    override fun glActiveTexture(texture: Int) = GL13.glActiveTexture(texture)
    override fun glGenTexture(): Int = GL11.glGenTextures()
    override fun glBindTexture(target: Int, texture: Int) = GL11.glBindTexture(target, texture)
    override fun glTexParameteri(target: Int, pname: Int, param: Int) = GL11.glTexParameteri(target, pname, param)
    override fun glTexImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, format: Int, type: Int, pixels: ByteArray?) {
        val buf: ByteBuffer? = pixels?.let {
            val b = BufferUtils.createByteBuffer(it.size)
            b.put(it).flip()
            b
        }
        GL11.glTexImage2D(target, level, internalformat, width, height, border, format, type, buf)
    }
    override fun glGenerateMipmap(target: Int) = GL30.glGenerateMipmap(target)
    override fun glDeleteTexture(texture: Int) = GL11.glDeleteTextures(texture)

    override fun glGetError(): Int = GL11.glGetError()
    override fun glGetString(name: Int): String? = GL11.glGetString(name)
}

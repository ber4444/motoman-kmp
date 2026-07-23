package com.marcowong.motoman.gl

/**
 * Decorator that checks `glGetError` after every GL call and records failures.
 * Ported in spirit from the original `GL20Debug`. Doubles as the CI harness the
 * verification plan calls for: run a scripted replay wrapped in [GlDebug] and assert
 * [errorCount] stays zero.
 *
 * @param delegate the underlying platform GL.
 * @param checkErrors when true, `glGetError` is polled after each state-changing call.
 * @param log sink for human-readable messages (call trace + error reports).
 * @param trace when true, every call is emitted to [log] (verbose).
 */
class GlDebug(
    private val delegate: Gl,
    private val checkErrors: Boolean = true,
    private val log: (String) -> Unit = ::println,
    private val trace: Boolean = false,
) : Gl {

    var errorCount: Int = 0
        private set
    var lastError: Int = GL_NO_ERROR
        private set

    private inline fun <T> op(name: String, block: () -> T): T {
        if (trace) log("gl.$name")
        val result = block()
        if (checkErrors) check(name)
        return result
    }

    private fun check(name: String) {
        val err = delegate.glGetError()
        if (err != GL_NO_ERROR) {
            errorCount++
            lastError = err
            log("GL ERROR 0x${err.toString(16)} after $name")
        }
    }

    override fun glViewport(x: Int, y: Int, width: Int, height: Int) = op("glViewport") { delegate.glViewport(x, y, width, height) }
    override fun glClear(mask: Int) = op("glClear") { delegate.glClear(mask) }
    override fun glClearColor(red: Float, green: Float, blue: Float, alpha: Float) = op("glClearColor") { delegate.glClearColor(red, green, blue, alpha) }
    override fun glEnable(cap: Int) = op("glEnable") { delegate.glEnable(cap) }
    override fun glDisable(cap: Int) = op("glDisable") { delegate.glDisable(cap) }
    override fun glDepthFunc(func: Int) = op("glDepthFunc") { delegate.glDepthFunc(func) }
    override fun glDepthMask(flag: Boolean) = op("glDepthMask") { delegate.glDepthMask(flag) }
    override fun glCullFace(mode: Int) = op("glCullFace") { delegate.glCullFace(mode) }
    override fun glFrontFace(mode: Int) = op("glFrontFace") { delegate.glFrontFace(mode) }
    override fun glBlendFunc(sfactor: Int, dfactor: Int) = op("glBlendFunc") { delegate.glBlendFunc(sfactor, dfactor) }

    override fun glGenBuffer(): Int = op("glGenBuffer") { delegate.glGenBuffer() }
    override fun glBindBuffer(target: Int, buffer: Int) = op("glBindBuffer") { delegate.glBindBuffer(target, buffer) }
    override fun glBufferData(target: Int, data: FloatArray, usage: Int) = op("glBufferData[f]") { delegate.glBufferData(target, data, usage) }
    override fun glBufferData(target: Int, data: ShortArray, usage: Int) = op("glBufferData[s]") { delegate.glBufferData(target, data, usage) }
    override fun glDeleteBuffer(buffer: Int) = op("glDeleteBuffer") { delegate.glDeleteBuffer(buffer) }

    override fun glEnableVertexAttribArray(index: Int) = op("glEnableVertexAttribArray") { delegate.glEnableVertexAttribArray(index) }
    override fun glDisableVertexAttribArray(index: Int) = op("glDisableVertexAttribArray") { delegate.glDisableVertexAttribArray(index) }
    override fun glVertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, strideBytes: Int, offsetBytes: Int) =
        op("glVertexAttribPointer") { delegate.glVertexAttribPointer(index, size, type, normalized, strideBytes, offsetBytes) }

    override fun glDrawArrays(mode: Int, first: Int, count: Int) = op("glDrawArrays") { delegate.glDrawArrays(mode, first, count) }
    override fun glDrawElements(mode: Int, count: Int, type: Int, offsetBytes: Int) = op("glDrawElements") { delegate.glDrawElements(mode, count, type, offsetBytes) }

    override fun glCreateShader(type: Int): Int = op("glCreateShader") { delegate.glCreateShader(type) }
    override fun glShaderSource(shader: Int, source: String) = op("glShaderSource") { delegate.glShaderSource(shader, source) }
    override fun glCompileShader(shader: Int) = op("glCompileShader") { delegate.glCompileShader(shader) }
    override fun glGetShaderCompileStatus(shader: Int): Boolean = op("glGetShaderCompileStatus") { delegate.glGetShaderCompileStatus(shader) }
    override fun glGetShaderInfoLog(shader: Int): String = op("glGetShaderInfoLog") { delegate.glGetShaderInfoLog(shader) }
    override fun glDeleteShader(shader: Int) = op("glDeleteShader") { delegate.glDeleteShader(shader) }
    override fun glCreateProgram(): Int = op("glCreateProgram") { delegate.glCreateProgram() }
    override fun glAttachShader(program: Int, shader: Int) = op("glAttachShader") { delegate.glAttachShader(program, shader) }
    override fun glBindAttribLocation(program: Int, index: Int, name: String) = op("glBindAttribLocation") { delegate.glBindAttribLocation(program, index, name) }
    override fun glLinkProgram(program: Int) = op("glLinkProgram") { delegate.glLinkProgram(program) }
    override fun glGetProgramLinkStatus(program: Int): Boolean = op("glGetProgramLinkStatus") { delegate.glGetProgramLinkStatus(program) }
    override fun glGetProgramInfoLog(program: Int): String = op("glGetProgramInfoLog") { delegate.glGetProgramInfoLog(program) }
    override fun glUseProgram(program: Int) = op("glUseProgram") { delegate.glUseProgram(program) }
    override fun glDeleteProgram(program: Int) = op("glDeleteProgram") { delegate.glDeleteProgram(program) }
    override fun glGetAttribLocation(program: Int, name: String): Int = op("glGetAttribLocation") { delegate.glGetAttribLocation(program, name) }
    override fun glGetUniformLocation(program: Int, name: String): Int = op("glGetUniformLocation") { delegate.glGetUniformLocation(program, name) }

    override fun glUniform1i(location: Int, x: Int) = op("glUniform1i") { delegate.glUniform1i(location, x) }
    override fun glUniform1f(location: Int, x: Float) = op("glUniform1f") { delegate.glUniform1f(location, x) }
    override fun glUniform2f(location: Int, x: Float, y: Float) = op("glUniform2f") { delegate.glUniform2f(location, x, y) }
    override fun glUniform3f(location: Int, x: Float, y: Float, z: Float) = op("glUniform3f") { delegate.glUniform3f(location, x, y, z) }
    override fun glUniform4f(location: Int, x: Float, y: Float, z: Float, w: Float) = op("glUniform4f") { delegate.glUniform4f(location, x, y, z, w) }
    override fun glUniformMatrix4fv(location: Int, transpose: Boolean, value: FloatArray) = op("glUniformMatrix4fv") { delegate.glUniformMatrix4fv(location, transpose, value) }

    override fun glActiveTexture(texture: Int) = op("glActiveTexture") { delegate.glActiveTexture(texture) }
    override fun glGenTexture(): Int = op("glGenTexture") { delegate.glGenTexture() }
    override fun glBindTexture(target: Int, texture: Int) = op("glBindTexture") { delegate.glBindTexture(target, texture) }
    override fun glTexParameteri(target: Int, pname: Int, param: Int) = op("glTexParameteri") { delegate.glTexParameteri(target, pname, param) }
    override fun glTexImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, format: Int, type: Int, pixels: ByteArray?) =
        op("glTexImage2D") { delegate.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels) }
    override fun glGenerateMipmap(target: Int) = op("glGenerateMipmap") { delegate.glGenerateMipmap(target) }
    override fun glDeleteTexture(texture: Int) = op("glDeleteTexture") { delegate.glDeleteTexture(texture) }

    // Not wrapped: querying error would recurse. Pass through.
    override fun glGetError(): Int = delegate.glGetError()
    override fun glGetString(name: Int): String? = delegate.glGetString(name)
}

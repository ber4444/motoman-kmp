package com.marcowong.motoman.gl

/**
 * Recording [Gl] test double. Captures the call sequence so tests can assert what the
 * layers above actually issue, with no GPU or context involved.
 */
class FakeGl : Gl {
    val calls = mutableListOf<String>()

    private var nextBuffer = 1
    private var nextTexture = 1
    private var nextShader = 1
    private var nextProgram = 1

    /** Last pixel payload handed to glTexImage2D. */
    var lastTexImageSize: Int = -1
        private set
    var lastTexImageFormat: Int = -1
        private set

    var compileStatus = true
    var linkStatus = true

    fun countOf(prefix: String): Int = calls.count { it.substringBefore('(') == prefix }
    fun clear() = calls.clear()

    override fun glViewport(x: Int, y: Int, width: Int, height: Int) { calls += "glViewport($x,$y,$width,$height)" }
    override fun glClear(mask: Int) { calls += "glClear($mask)" }
    override fun glClearColor(red: Float, green: Float, blue: Float, alpha: Float) { calls += "glClearColor($red,$green,$blue,$alpha)" }
    override fun glEnable(cap: Int) { calls += "glEnable($cap)" }
    override fun glDisable(cap: Int) { calls += "glDisable($cap)" }
    override fun glDepthFunc(func: Int) { calls += "glDepthFunc($func)" }
    override fun glDepthMask(flag: Boolean) { calls += "glDepthMask($flag)" }
    override fun glCullFace(mode: Int) { calls += "glCullFace($mode)" }
    override fun glFrontFace(mode: Int) { calls += "glFrontFace($mode)" }
    override fun glBlendFunc(sfactor: Int, dfactor: Int) { calls += "glBlendFunc($sfactor,$dfactor)" }

    override fun glGenBuffer(): Int { calls += "glGenBuffer()"; return nextBuffer++ }
    override fun glBindBuffer(target: Int, buffer: Int) { calls += "glBindBuffer($target,$buffer)" }
    override fun glBufferData(target: Int, data: FloatArray, usage: Int) { calls += "glBufferData($target,f${data.size},$usage)" }
    override fun glBufferData(target: Int, data: ShortArray, usage: Int) { calls += "glBufferData($target,s${data.size},$usage)" }
    override fun glDeleteBuffer(buffer: Int) { calls += "glDeleteBuffer($buffer)" }

    override fun glEnableVertexAttribArray(index: Int) { calls += "glEnableVertexAttribArray($index)" }
    override fun glDisableVertexAttribArray(index: Int) { calls += "glDisableVertexAttribArray($index)" }
    override fun glVertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, strideBytes: Int, offsetBytes: Int) {
        calls += "glVertexAttribPointer($index,$size,$strideBytes,$offsetBytes)"
    }

    override fun glDrawArrays(mode: Int, first: Int, count: Int) { calls += "glDrawArrays($mode,$first,$count)" }
    override fun glDrawElements(mode: Int, count: Int, type: Int, offsetBytes: Int) { calls += "glDrawElements($mode,$count,$offsetBytes)" }

    override fun glCreateShader(type: Int): Int { calls += "glCreateShader($type)"; return nextShader++ }
    override fun glShaderSource(shader: Int, source: String) { calls += "glShaderSource($shader)" }
    override fun glCompileShader(shader: Int) { calls += "glCompileShader($shader)" }
    override fun glGetShaderCompileStatus(shader: Int): Boolean = compileStatus
    override fun glGetShaderInfoLog(shader: Int): String = "fake shader log"
    override fun glDeleteShader(shader: Int) { calls += "glDeleteShader($shader)" }
    override fun glCreateProgram(): Int { calls += "glCreateProgram()"; return nextProgram++ }
    override fun glAttachShader(program: Int, shader: Int) { calls += "glAttachShader($program,$shader)" }
    override fun glBindAttribLocation(program: Int, index: Int, name: String) { calls += "glBindAttribLocation($index,$name)" }
    override fun glLinkProgram(program: Int) { calls += "glLinkProgram($program)" }
    override fun glGetProgramLinkStatus(program: Int): Boolean = linkStatus
    override fun glGetProgramInfoLog(program: Int): String = "fake program log"
    override fun glUseProgram(program: Int) { calls += "glUseProgram($program)" }
    override fun glDeleteProgram(program: Int) { calls += "glDeleteProgram($program)" }
    override fun glGetAttribLocation(program: Int, name: String): Int = name.hashCode() and 0x7
    override fun glGetUniformLocation(program: Int, name: String): Int = name.hashCode() and 0x7

    override fun glUniform1i(location: Int, x: Int) { calls += "glUniform1i($location,$x)" }
    override fun glUniform1f(location: Int, x: Float) { calls += "glUniform1f($location,$x)" }
    override fun glUniform2f(location: Int, x: Float, y: Float) { calls += "glUniform2f($location)" }
    override fun glUniform3f(location: Int, x: Float, y: Float, z: Float) { calls += "glUniform3f($location)" }
    override fun glUniform4f(location: Int, x: Float, y: Float, z: Float, w: Float) { calls += "glUniform4f($location,$x,$y,$z,$w)" }
    override fun glUniformMatrix4fv(location: Int, transpose: Boolean, value: FloatArray) { calls += "glUniformMatrix4fv($location)" }

    override fun glActiveTexture(texture: Int) { calls += "glActiveTexture($texture)" }
    override fun glGenTexture(): Int { calls += "glGenTexture()"; return nextTexture++ }
    override fun glBindTexture(target: Int, texture: Int) { calls += "glBindTexture($target,$texture)" }
    override fun glTexParameteri(target: Int, pname: Int, param: Int) { calls += "glTexParameteri($pname,$param)" }
    override fun glTexImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, format: Int, type: Int, pixels: ByteArray?) {
        lastTexImageSize = pixels?.size ?: -1
        lastTexImageFormat = format
        calls += "glTexImage2D($width,$height,$format)"
    }
    override fun glGenerateMipmap(target: Int) { calls += "glGenerateMipmap($target)" }
    override fun glDeleteTexture(texture: Int) { calls += "glDeleteTexture($texture)" }

    override fun glGetError(): Int = GL_NO_ERROR
    override fun glGetString(name: Int): String = "fake"
}

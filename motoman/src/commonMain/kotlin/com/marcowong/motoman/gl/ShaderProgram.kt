package com.marcowong.motoman.gl

import com.marcowong.motoman.track.math.Matrix4

/**
 * A compiled and linked GLSL program. Port of the parts of libGDX `ShaderProgram` the
 * engine uses, expressed over the [Gl] interface so the decorator chain still applies.
 *
 * Sources are written once as GLSL ES 1.00 and reconciled with the target dialect by
 * [ShaderPreprocessor], so the same shader text serves desktop and Android.
 */
class ShaderProgram(
    private val gl: Gl,
    vertexSource: String,
    fragmentSource: String,
    preprocessor: ShaderPreprocessor,
) {
    var isCompiled: Boolean = false
        private set

    var log: String = ""
        private set

    private var program = 0
    private var vertexShader = 0
    private var fragmentShader = 0

    private val uniformLocations = HashMap<String, Int>()
    private val attributeLocations = HashMap<String, Int>()

    init {
        val vs = preprocessor.process(vertexSource, ShaderStage.VERTEX)
        val fs = preprocessor.process(fragmentSource, ShaderStage.FRAGMENT)
        compile(vs, fs)
    }

    private fun compile(vertexSource: String, fragmentSource: String) {
        vertexShader = compileShader(GL_VERTEX_SHADER, vertexSource) ?: return
        fragmentShader = compileShader(GL_FRAGMENT_SHADER, fragmentSource) ?: return

        program = gl.glCreateProgram()
        if (program == 0) {
            log += "could not create program\n"
            return
        }
        gl.glAttachShader(program, vertexShader)
        gl.glAttachShader(program, fragmentShader)
        gl.glLinkProgram(program)
        if (!gl.glGetProgramLinkStatus(program)) {
            log += gl.glGetProgramInfoLog(program)
            return
        }
        isCompiled = true
    }

    private fun compileShader(type: Int, source: String): Int? {
        val handle = gl.glCreateShader(type)
        if (handle == 0) {
            log += "could not create shader (type $type)\n"
            return null
        }
        gl.glShaderSource(handle, source)
        gl.glCompileShader(handle)
        if (!gl.glGetShaderCompileStatus(handle)) {
            val stage = if (type == GL_VERTEX_SHADER) "vertex" else "fragment"
            log += "$stage shader: " + gl.glGetShaderInfoLog(handle) + "\n"
            return null
        }
        return handle
    }

    /** Makes this the active program. */
    fun bind() {
        gl.glUseProgram(program)
    }

    fun getUniformLocation(name: String): Int =
        uniformLocations.getOrPut(name) { gl.glGetUniformLocation(program, name) }

    fun getAttributeLocation(name: String): Int =
        attributeLocations.getOrPut(name) { gl.glGetAttribLocation(program, name) }

    fun setUniformi(name: String, value: Int) = gl.glUniform1i(getUniformLocation(name), value)
    fun setUniformf(name: String, value: Float) = gl.glUniform1f(getUniformLocation(name), value)
    fun setUniformf(name: String, x: Float, y: Float) = gl.glUniform2f(getUniformLocation(name), x, y)
    fun setUniformf(name: String, x: Float, y: Float, z: Float) = gl.glUniform3f(getUniformLocation(name), x, y, z)
    fun setUniformf(name: String, x: Float, y: Float, z: Float, w: Float) = gl.glUniform4f(getUniformLocation(name), x, y, z, w)

    fun setUniformMatrix(name: String, matrix: Matrix4, transpose: Boolean = false) =
        gl.glUniformMatrix4fv(getUniformLocation(name), transpose, matrix.`val`)

    fun enableVertexAttribute(name: String) {
        val location = getAttributeLocation(name)
        if (location >= 0) gl.glEnableVertexAttribArray(location)
    }

    fun disableVertexAttribute(name: String) {
        val location = getAttributeLocation(name)
        if (location >= 0) gl.glDisableVertexAttribArray(location)
    }

    /** Binds an attribute to the currently bound GL_ARRAY_BUFFER at [offsetBytes]. */
    fun setVertexAttribute(
        name: String,
        numComponents: Int,
        type: Int,
        normalize: Boolean,
        strideBytes: Int,
        offsetBytes: Int,
    ) {
        val location = getAttributeLocation(name)
        if (location < 0) return
        gl.glVertexAttribPointer(location, numComponents, type, normalize, strideBytes, offsetBytes)
    }

    fun dispose() {
        gl.glUseProgram(0)
        if (vertexShader != 0) gl.glDeleteShader(vertexShader)
        if (fragmentShader != 0) gl.glDeleteShader(fragmentShader)
        if (program != 0) gl.glDeleteProgram(program)
        isCompiled = false
    }

    companion object {
        const val POSITION_ATTRIBUTE = "a_position"
        const val NORMAL_ATTRIBUTE = "a_normal"
        const val COLOR_ATTRIBUTE = "a_color"
        const val TEXCOORD_ATTRIBUTE = "a_texCoord"
        const val SKELETON_ATTRIBUTE = "a_skeleton"
    }
}

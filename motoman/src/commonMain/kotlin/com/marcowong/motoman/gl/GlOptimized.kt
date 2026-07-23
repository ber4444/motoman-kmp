package com.marcowong.motoman.gl

/**
 * Decorator that elides redundant GL state changes, ported from the original
 * `GL20Optimized`. This is the reason the renderer is fast on 2013-era hardware:
 * it drops repeated `glEnable`/`glDisable`, texture/program/blend/clear-colour
 * rebinds, unchanged uniforms, and lazily manages vertex-attribute arrays,
 * auto-disabling any left enabled but unused between draws.
 *
 * State is tracked at this layer only; wrap the platform GL with this and, if
 * desired, [GlDebug] underneath it: `GlOptimized(GlDebug(createPlatformGl()))`.
 */
class GlOptimized(private val delegate: Gl) : Gl {

    private val capEnabled = HashMap<Int, Boolean>()
    private var textureActive = GL_TEXTURE0
    // activeTexture -> (target -> texture)
    private val textureTarget = HashMap<Int, HashMap<Int, Int>>()
    private var boundVbo = 0
    private var boundIbo = 0
    private val enabledVertexAttribArray = HashSet<Int>()
    private val usedVertexAttribArray = HashSet<Int>()
    private var activeProgram = 0
    // program -> (location -> value)
    private val uniform1iCache = HashMap<Int, HashMap<Int, Int>>()
    private val uniform4fCache = HashMap<Int, HashMap<Int, FloatArray>>()
    private var blendSFactor: Int? = null
    private var blendDFactor: Int? = null
    private var clearColor: FloatArray? = null

    init {
        delegate.glActiveTexture(GL_TEXTURE0)
        delegate.glBindBuffer(GL_ARRAY_BUFFER, 0)
        delegate.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    override fun glEnable(cap: Int) {
        if (capEnabled[cap] != true) {
            capEnabled[cap] = true
            delegate.glEnable(cap)
        }
    }

    override fun glDisable(cap: Int) {
        if (capEnabled[cap] != false) {
            capEnabled[cap] = false
            delegate.glDisable(cap)
        }
    }

    override fun glActiveTexture(texture: Int) {
        if (texture != textureActive) {
            textureActive = texture
            textureTarget.clear()
            delegate.glActiveTexture(texture)
        }
    }

    override fun glBindTexture(target: Int, texture: Int) {
        val m = textureTarget.getOrPut(textureActive) { HashMap() }
        if (m[target] != texture) {
            m[target] = texture
            delegate.glBindTexture(target, texture)
        }
    }

    override fun glBlendFunc(sfactor: Int, dfactor: Int) {
        if (sfactor != blendSFactor || dfactor != blendDFactor) {
            blendSFactor = sfactor
            blendDFactor = dfactor
            delegate.glBlendFunc(sfactor, dfactor)
        }
    }

    override fun glClearColor(red: Float, green: Float, blue: Float, alpha: Float) {
        val c = clearColor
        if (c == null || c[0] != red || c[1] != green || c[2] != blue || c[3] != alpha) {
            clearColor = floatArrayOf(red, green, blue, alpha)
            delegate.glClearColor(red, green, blue, alpha)
        }
    }

    override fun glUseProgram(program: Int) {
        if (program != 0 && program != activeProgram) {
            activeProgram = program
            delegate.glUseProgram(program)
        }
    }

    override fun glUniform1i(location: Int, x: Int) {
        val m = uniform1iCache.getOrPut(activeProgram) { HashMap() }
        if (m[location] != x) {
            m[location] = x
            delegate.glUniform1i(location, x)
        }
    }

    override fun glUniform4f(location: Int, x: Float, y: Float, z: Float, w: Float) {
        val m = uniform4fCache.getOrPut(activeProgram) { HashMap() }
        val f = m[location]
        if (f == null) {
            m[location] = floatArrayOf(x, y, z, w)
            delegate.glUniform4f(location, x, y, z, w)
        } else if (f[0] != x || f[1] != y || f[2] != z || f[3] != w) {
            f[0] = x; f[1] = y; f[2] = z; f[3] = w
            delegate.glUniform4f(location, x, y, z, w)
        }
    }

    override fun glBindBuffer(target: Int, buffer: Int) {
        if (buffer != 0) {
            if (target == GL_ARRAY_BUFFER) boundVbo = buffer
            if (target == GL_ELEMENT_ARRAY_BUFFER) boundIbo = buffer
            delegate.glBindBuffer(target, buffer)
        }
    }

    override fun glDeleteBuffer(buffer: Int) {
        if (buffer == boundVbo) {
            boundVbo = 0
            delegate.glBindBuffer(GL_ARRAY_BUFFER, 0)
        }
        if (buffer == boundIbo) {
            boundIbo = 0
            delegate.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
        }
        delegate.glDeleteBuffer(buffer)
    }

    // Enable/disable are deferred: the real enable happens lazily in glVertexAttribPointer,
    // and disableUnused() prunes arrays that were enabled but not used this draw.
    override fun glEnableVertexAttribArray(index: Int) { /* deferred */ }
    override fun glDisableVertexAttribArray(index: Int) { /* deferred */ }

    override fun glVertexAttribPointer(index: Int, size: Int, type: Int, normalized: Boolean, strideBytes: Int, offsetBytes: Int) {
        if (enabledVertexAttribArray.add(index)) {
            delegate.glEnableVertexAttribArray(index)
        }
        usedVertexAttribArray.add(index)
        delegate.glVertexAttribPointer(index, size, type, normalized, strideBytes, offsetBytes)
    }

    private fun disableUnusedVertexAttribArray() {
        val it = enabledVertexAttribArray.iterator()
        while (it.hasNext()) {
            val index = it.next()
            if (index !in usedVertexAttribArray) {
                delegate.glDisableVertexAttribArray(index)
                it.remove()
            }
        }
        usedVertexAttribArray.clear()
    }

    override fun glDrawArrays(mode: Int, first: Int, count: Int) {
        disableUnusedVertexAttribArray()
        delegate.glDrawArrays(mode, first, count)
    }

    override fun glDrawElements(mode: Int, count: Int, type: Int, offsetBytes: Int) {
        disableUnusedVertexAttribArray()
        delegate.glDrawElements(mode, count, type, offsetBytes)
    }

    override fun glDeleteProgram(program: Int) {
        if (program == activeProgram) {
            uniform1iCache.remove(program)
            uniform4fCache.remove(program)
            activeProgram = 0
            delegate.glUseProgram(0)
        }
        delegate.glDeleteProgram(program)
    }

    override fun glDeleteTexture(texture: Int) {
        for (m in textureTarget.values) {
            val entries = m.entries.iterator()
            while (entries.hasNext()) {
                if (entries.next().value == texture) entries.remove()
            }
        }
        delegate.glDeleteTexture(texture)
    }

    // ---- Straight pass-through (no state to elide for this engine's use) ----
    override fun glViewport(x: Int, y: Int, width: Int, height: Int) = delegate.glViewport(x, y, width, height)
    override fun glClear(mask: Int) = delegate.glClear(mask)
    override fun glDepthFunc(func: Int) = delegate.glDepthFunc(func)
    override fun glDepthMask(flag: Boolean) = delegate.glDepthMask(flag)
    override fun glCullFace(mode: Int) = delegate.glCullFace(mode)
    override fun glFrontFace(mode: Int) = delegate.glFrontFace(mode)
    override fun glGenBuffer(): Int = delegate.glGenBuffer()
    override fun glBufferData(target: Int, data: FloatArray, usage: Int) = delegate.glBufferData(target, data, usage)
    override fun glBufferData(target: Int, data: ShortArray, usage: Int) = delegate.glBufferData(target, data, usage)
    override fun glCreateShader(type: Int): Int = delegate.glCreateShader(type)
    override fun glShaderSource(shader: Int, source: String) = delegate.glShaderSource(shader, source)
    override fun glCompileShader(shader: Int) = delegate.glCompileShader(shader)
    override fun glGetShaderCompileStatus(shader: Int): Boolean = delegate.glGetShaderCompileStatus(shader)
    override fun glGetShaderInfoLog(shader: Int): String = delegate.glGetShaderInfoLog(shader)
    override fun glDeleteShader(shader: Int) = delegate.glDeleteShader(shader)
    override fun glCreateProgram(): Int = delegate.glCreateProgram()
    override fun glAttachShader(program: Int, shader: Int) = delegate.glAttachShader(program, shader)
    override fun glBindAttribLocation(program: Int, index: Int, name: String) = delegate.glBindAttribLocation(program, index, name)
    override fun glLinkProgram(program: Int) = delegate.glLinkProgram(program)
    override fun glGetProgramLinkStatus(program: Int): Boolean = delegate.glGetProgramLinkStatus(program)
    override fun glGetProgramInfoLog(program: Int): String = delegate.glGetProgramInfoLog(program)
    override fun glGetAttribLocation(program: Int, name: String): Int = delegate.glGetAttribLocation(program, name)
    override fun glGetUniformLocation(program: Int, name: String): Int = delegate.glGetUniformLocation(program, name)
    override fun glUniform1f(location: Int, x: Float) = delegate.glUniform1f(location, x)
    override fun glUniform2f(location: Int, x: Float, y: Float) = delegate.glUniform2f(location, x, y)
    override fun glUniform3f(location: Int, x: Float, y: Float, z: Float) = delegate.glUniform3f(location, x, y, z)
    override fun glUniformMatrix4fv(location: Int, transpose: Boolean, value: FloatArray) = delegate.glUniformMatrix4fv(location, transpose, value)
    override fun glGenTexture(): Int = delegate.glGenTexture()
    override fun glTexParameteri(target: Int, pname: Int, param: Int) = delegate.glTexParameteri(target, pname, param)
    override fun glTexImage2D(target: Int, level: Int, internalformat: Int, width: Int, height: Int, border: Int, format: Int, type: Int, pixels: ByteArray?) =
        delegate.glTexImage2D(target, level, internalformat, width, height, border, format, type, pixels)
    override fun glGenerateMipmap(target: Int) = delegate.glGenerateMipmap(target)
    override fun glReadPixels(x: Int, y: Int, width: Int, height: Int, format: Int, type: Int): ByteArray =
        delegate.glReadPixels(x, y, width, height, format, type)
    override fun glGetError(): Int = delegate.glGetError()
    override fun glGetString(name: Int): String? = delegate.glGetString(name)
}

package com.marcowong.motoman.gl

/**
 * A vertex buffer plus optional index buffer with a known attribute layout.
 * Port of the parts of libGDX `Mesh` the engine uses (`InstancingModel` builds one
 * per sub-mesh; `MeshOptimized` hand-rolls its own equivalent for the batched path).
 *
 * Uploads go through [Gl]'s array-based `glBufferData`, so no `BufferUtils`
 * expect/actual is required — each platform actual marshals to its own native buffer.
 */
class Mesh(
    private val gl: Gl,
    isStatic: Boolean,
    maxIndices: Int,
    val attributes: VertexAttributes,
) {
    private val usage = if (isStatic) GL_STATIC_DRAW else GL_DYNAMIC_DRAW

    private var vbo = gl.glGenBuffer()
    private var ibo = if (maxIndices > 0) gl.glGenBuffer() else 0

    var numVertices: Int = 0
        private set
    var numIndices: Int = 0
        private set

    fun setVertices(vertices: FloatArray): Mesh {
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo)
        gl.glBufferData(GL_ARRAY_BUFFER, vertices, usage)
        val floatsPerVertex = attributes.vertexSize / 4
        numVertices = if (floatsPerVertex > 0) vertices.size / floatsPerVertex else 0
        return this
    }

    fun setIndices(indices: ShortArray): Mesh {
        require(ibo != 0) { "mesh was created without an index buffer" }
        gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo)
        gl.glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, usage)
        numIndices = indices.size
        return this
    }

    /**
     * Draws [count] elements starting at [offset]. When indexed, [offset] and [count]
     * are in indices; otherwise they are in vertices.
     */
    fun render(shader: ShaderProgram, primitiveType: Int, offset: Int, count: Int) {
        bind(shader)
        if (numIndices > 0) {
            // 2 bytes per short index.
            gl.glDrawElements(primitiveType, count, GL_UNSIGNED_SHORT, offset * 2)
        } else {
            gl.glDrawArrays(primitiveType, offset, count)
        }
    }

    private fun bind(shader: ShaderProgram) {
        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo)
        if (ibo != 0) gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo)
        for (i in 0 until attributes.size()) {
            val attribute = attributes[i]
            shader.enableVertexAttribute(attribute.alias)
            shader.setVertexAttribute(
                attribute.alias,
                attribute.numComponents,
                GL_FLOAT,
                false,
                attributes.vertexSize,
                attribute.offset,
            )
        }
    }

    fun dispose() {
        if (vbo != 0) {
            gl.glDeleteBuffer(vbo)
            vbo = 0
        }
        if (ibo != 0) {
            gl.glDeleteBuffer(ibo)
            ibo = 0
        }
    }
}

package com.marcowong.motoman.gl

/** Vertex attribute usage flags. Values match libGDX so ported engine code reads the same. */
object VertexUsage {
    const val Position = 1
    const val ColorUnpacked = 2
    const val ColorPacked = 4
    const val Normal = 8
    const val TextureCoordinates = 16
    const val Generic = 32
    const val BoneWeight = 64
    const val Tangent = 128
    const val BiNormal = 256
}

/**
 * A single vertex attribute: how many float components it has, the shader `in`/`attribute`
 * name it binds to, and its byte [offset] within a vertex (assigned by [VertexAttributes]).
 */
class VertexAttribute(
    val usage: Int,
    val numComponents: Int,
    val alias: String,
) {
    /** Byte offset into the vertex; set when this attribute joins a [VertexAttributes] set. */
    var offset: Int = 0

    /** All engine attributes are float-typed. */
    val sizeInBytes: Int get() = numComponents * 4

    override fun equals(other: Any?): Boolean =
        other is VertexAttribute && usage == other.usage &&
            numComponents == other.numComponents && alias == other.alias

    override fun hashCode(): Int = (usage * 31 + numComponents) * 31 + alias.hashCode()
}

/**
 * An ordered set of [VertexAttribute]s describing one vertex's memory layout.
 * Computes each attribute's byte offset and the total [vertexSize] (the stride).
 */
class VertexAttributes(vararg attributes: VertexAttribute) {
    private val attributes: Array<out VertexAttribute> = attributes

    /** Stride in bytes between consecutive vertices. */
    val vertexSize: Int

    init {
        var offset = 0
        for (attribute in attributes) {
            attribute.offset = offset
            offset += attribute.sizeInBytes
        }
        vertexSize = offset
    }

    fun size(): Int = attributes.size

    operator fun get(index: Int): VertexAttribute = attributes[index]

    fun findByUsage(usage: Int): VertexAttribute? = attributes.firstOrNull { it.usage == usage }
}

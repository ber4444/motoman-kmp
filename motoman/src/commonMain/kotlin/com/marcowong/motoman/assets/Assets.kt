package com.marcowong.motoman.assets

/**
 * Reads game data (OBJ, MTL, images, audio) from wherever the platform keeps it.
 *
 * This is a plain interface rather than an `expect`/`actual` pair on purpose: Android's
 * implementation needs an `AssetManager` handed to it from a `Context`, which a
 * parameterless `expect fun` cannot supply. The interface is the seam; each platform
 * provides its own implementation and the host wires it in.
 *
 * Paths are relative and `/`-separated, e.g. `"data/bike.obj"`.
 */
interface Assets {
    fun exists(path: String): Boolean

    /** Reads the whole entry. Throws if [path] is missing. */
    fun readBytes(path: String): ByteArray

    /** Reads the whole entry as UTF-8 text. Throws if [path] is missing. */
    fun readText(path: String): String

    /**
     * Resolves [path] relative to the directory containing [siblingOf]. Used for the
     * `mtllib`/`map_Kd` references inside an OBJ, which are relative to the model file.
     */
    fun sibling(siblingOf: String, path: String): String {
        val slash = siblingOf.lastIndexOf('/')
        return if (slash < 0) path else siblingOf.substring(0, slash + 1) + path
    }
}

/** Thrown when an asset cannot be found; platform implementations should use this. */
class AssetNotFoundException(path: String) : RuntimeException("asset not found: $path")

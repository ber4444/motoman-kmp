package com.marcowong.motoman.assets

/**
 * Desktop assets read from the classpath. The engine's `desktopMain` resources point at
 * the shared asset directory, so desktop and Android load byte-identical data.
 */
class ClasspathAssets(
    private val classLoader: ClassLoader = ClasspathAssets::class.java.classLoader!!,
) : Assets {

    override fun exists(path: String): Boolean = classLoader.getResource(path) != null

    override fun readBytes(path: String): ByteArray =
        classLoader.getResourceAsStream(path)?.use { it.readBytes() }
            ?: throw AssetNotFoundException(path)

    override fun readText(path: String): String = readBytes(path).decodeToString()
}

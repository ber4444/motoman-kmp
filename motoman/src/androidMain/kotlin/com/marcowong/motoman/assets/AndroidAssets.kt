package com.marcowong.motoman.assets

import android.content.res.AssetManager
import java.io.FileNotFoundException

/**
 * Android assets read through the APK's [AssetManager]. Construct with
 * `AndroidAssets(context.assets)` from the hosting Activity.
 */
class AndroidAssets(private val assetManager: AssetManager) : Assets {

    override fun exists(path: String): Boolean = try {
        assetManager.open(path).close()
        true
    } catch (_: FileNotFoundException) {
        false
    } catch (_: java.io.IOException) {
        false
    }

    override fun readBytes(path: String): ByteArray = try {
        assetManager.open(path).use { it.readBytes() }
    } catch (_: java.io.IOException) {
        throw AssetNotFoundException(path)
    }

    override fun readText(path: String): String = readBytes(path).decodeToString()
}

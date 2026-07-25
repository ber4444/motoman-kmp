package com.marcowong.motoman.assets

import platform.Foundation.NSBundle
import platform.Foundation.NSFileManager
import platform.Foundation.NSData
import platform.Foundation.dataWithContentsOfFile
import platform.posix.memcpy
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned

class IosAssets : Assets {
    private val root: String = NSBundle.mainBundle.resourcePath ?: error("no resourcePath")
    
    private fun resolve(path: String) = "$root/$path"

    override fun exists(path: String): Boolean {
        return NSFileManager.defaultManager.fileExistsAtPath(resolve(path))
    }

    override fun readBytes(path: String): ByteArray {
        val resolved = resolve(path)
        val data = NSData.dataWithContentsOfFile(resolved) ?: throw AssetNotFoundException(path)
        
        val size = data.length.toInt()
        val bytes = ByteArray(size)
        if (size > 0) {
            bytes.usePinned { pinned ->
                memcpy(pinned.addressOf(0), data.bytes, data.length)
            }
        }
        return bytes
    }

    override fun readText(path: String): String {
        return readBytes(path).decodeToString()
    }
}

package com.marcowong.motoman.gl

import platform.UIKit.UIImage
import platform.CoreGraphics.*
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import kotlinx.cinterop.*

actual fun decodePixmap(bytes: ByteArray): Pixmap {
    require(bytes.isNotEmpty()) { "cannot decode an empty byte array" }
    
    val nsData = bytes.usePinned { pinned ->
        NSData.dataWithBytes(pinned.addressOf(0), bytes.size.convert())
    }
    
    val uiImage = UIImage(data = nsData) ?: error("Failed to decode image from bytes")
    val cgImage = uiImage.CGImage ?: error("UIImage has no CGImage")
    
    val width = CGImageGetWidth(cgImage).toInt()
    val height = CGImageGetHeight(cgImage).toInt()
    
    val outBytes = ByteArray(width * height * 4)
    outBytes.usePinned { pinned ->
        val colorSpace = CGColorSpaceCreateDeviceRGB()
        
        val context = CGBitmapContextCreate(
            data = pinned.addressOf(0),
            width = width.convert(),
            height = height.convert(),
            bitsPerComponent = 8u.convert(),
            bytesPerRow = (width * 4).convert(),
            space = colorSpace,
            bitmapInfo = CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value
        ) ?: run {
            CGColorSpaceRelease(colorSpace)
            error("Failed to create CGBitmapContext")
        }
        
        // Fix origin: Core Graphics has bottom-left origin, Pixmap expects top-left.
        CGContextTranslateCTM(context, 0.0, height.toDouble())
        CGContextScaleCTM(context, 1.0, -1.0)
        
        CGContextDrawImage(context, CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble()), cgImage)
        
        CGContextRelease(context)
        CGColorSpaceRelease(colorSpace)
        
        // Un-premultiply alpha: Core Graphics uses premultiplied alpha (R*A, G*A, B*A, A).
        // AndroidPixmap and STB desktop path produce straight alpha.
        val ptr = pinned.addressOf(0).reinterpret<UByteVar>()
        for (i in 0 until width * height) {
            val offset = i * 4
            val a = ptr[offset + 3].toInt()
            if (a in 1..254) {
                val r = ptr[offset]
                val g = ptr[offset + 1]
                val b = ptr[offset + 2]
                
                ptr[offset] = ((r.toInt() * 255) / a).coerceIn(0, 255).toUByte()
                ptr[offset + 1] = ((g.toInt() * 255) / a).coerceIn(0, 255).toUByte()
                ptr[offset + 2] = ((b.toInt() * 255) / a).coerceIn(0, 255).toUByte()
            }
        }
    }
    
    return Pixmap(width, height, PixmapFormat.RGBA8888, outBytes)
}

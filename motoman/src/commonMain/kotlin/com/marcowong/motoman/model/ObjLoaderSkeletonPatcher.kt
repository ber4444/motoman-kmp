package com.marcowong.motoman.model

import com.marcowong.motoman.gl.Pixmap
import com.marcowong.motoman.track.math.Color
import kotlin.math.abs
import kotlin.math.roundToInt

class ObjLoaderSkeletonPatcher {
    companion object {
        val skeletonIdColors = arrayOf(
            Color(1f, 1f, 1f, 1f), // WHITE
            Color(0f, 0f, 0f, 1f), // BLACK
            Color(1f, 0f, 0f, 1f), // RED
            Color(1f, 0.647f, 0f, 1f), // ORANGE
            Color(1f, 1f, 0f, 1f), // YELLOW
            Color(0f, 1f, 0f, 1f), // GREEN
            Color(0f, 1f, 1f, 1f), // CYAN
            Color(0f, 0f, 1f, 1f), // BLUE
            Color(1f, 0f, 1f, 1f), // MAGENTA
            Color(1f, 0.686f, 0.686f, 1f), // PINK
            Color(0.75f, 0.75f, 0.75f, 1f), // LIGHT_GRAY
            Color(0.5f, 0.5f, 0.5f, 1f), // GRAY
            Color(0.25f, 0.25f, 0.25f, 1f) // DARK_GRAY
        )
    }

    fun patch(model: ModelData, skeletonMapping: Pixmap) {
        for (subMesh in model.subMeshes) {
            val mesh = subMesh.mesh ?: continue
            val sizeVertex = 3 + (if (mesh.hasNorms) 3 else 0) + (if (mesh.hasUVs) 2 else 0)
            val nVertex = if (sizeVertex > 0) mesh.vertices.size / sizeVertex else 0
            val vertices = mesh.vertices
            val vertices2 = FloatArray(nVertex * (sizeVertex + 1))

            val uvOffset = 3 + (if (mesh.hasNorms) 3 else 0)

            for (i in 0 until nVertex) {
                val offset = i * sizeVertex
                val offset2 = i * (sizeVertex + 1)
                val offsetUV = offset2 + uvOffset
                val offsetSke = offset2 + sizeVertex
                for (j in 0 until sizeVertex) {
                    vertices2[offset2 + j] = vertices[offset + j]
                }
                var u = 0f
                var v = 0f
                if (mesh.hasUVs) {
                    u = vertices2[offsetUV]
                    v = vertices2[offsetUV + 1]
                }
                vertices2[offsetSke] = getSkeletonId(skeletonMapping, u, v).toFloat()
            }

            mesh.vertices = vertices2
            mesh.hasSkeleton = true
        }
    }

    private fun getSkeletonId(m: Pixmap, u: Float, v: Float): Int {
        val x = (u * m.width).roundToInt()
        val y = (v * m.height).roundToInt()
        // clamp x and y to pixmap bounds
        val cx = x.coerceIn(0, m.width - 1)
        val cy = y.coerceIn(0, m.height - 1)
        val pixel = m.getPixel(cx, cy)
        val r = ((pixel ushr 24) and 0xFF) / 255f
        val g = ((pixel ushr 16) and 0xFF) / 255f
        val b = ((pixel ushr 8) and 0xFF) / 255f

        var id = 0
        var lowestDiff = Float.POSITIVE_INFINITY
        for (i in skeletonIdColors.indices) {
            val color = skeletonIdColors[i]
            val diff = abs(color.r - r) + abs(color.g - g) + abs(color.b - b)
            if (diff < lowestDiff) {
                id = i
                lowestDiff = diff
            }
        }
        return id
    }
}

package com.marcowong.motoman.model

import com.marcowong.motoman.gl.ShaderProgram
import com.marcowong.motoman.gl.VertexUsage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VertexLayoutTest {

    private fun meshData(norms: Boolean, uvs: Boolean, skeleton: Boolean = false) = MeshData().also {
        it.hasNorms = norms
        it.hasUVs = uvs
        it.hasSkeleton = skeleton
    }

    @Test
    fun positionOnlyLayout() {
        val attrs = RenderableModel.attributesFor(meshData(norms = false, uvs = false))
        assertEquals(1, attrs.size())
        assertEquals(ShaderProgram.POSITION_ATTRIBUTE, attrs[0].alias)
        assertEquals(12, attrs.vertexSize, "3 floats")
    }

    @Test
    fun positionNormalUvLayoutMatchesLoaderInterleaving() {
        val attrs = RenderableModel.attributesFor(meshData(norms = true, uvs = true))
        assertEquals(3, attrs.size())
        // Order must match how ObjLoader writes a vertex: position, normal, uv.
        assertEquals(ShaderProgram.POSITION_ATTRIBUTE, attrs[0].alias)
        assertEquals(ShaderProgram.NORMAL_ATTRIBUTE, attrs[1].alias)
        assertEquals("a_texCoord0", attrs[2].alias, "must match the shader's varying name")
        assertEquals(0, attrs[0].offset)
        assertEquals(12, attrs[1].offset)
        assertEquals(24, attrs[2].offset)
        assertEquals(32, attrs.vertexSize, "3 + 3 + 2 floats")
    }

    @Test
    fun uvWithoutNormalsShiftsOffsets() {
        val attrs = RenderableModel.attributesFor(meshData(norms = false, uvs = true))
        assertEquals(2, attrs.size())
        assertEquals(12, attrs[1].offset, "uv follows position directly when normals are absent")
        assertEquals(20, attrs.vertexSize)
    }

    @Test
    fun skeletonAttributeComesLast() {
        val attrs = RenderableModel.attributesFor(meshData(norms = true, uvs = true, skeleton = true))
        assertEquals(4, attrs.size())
        assertEquals(ShaderProgram.SKELETON_ATTRIBUTE, attrs[3].alias)
        assertEquals(VertexUsage.Generic, attrs[3].usage)
        assertEquals(36, attrs.vertexSize)
    }

    @Test
    fun layoutStrideMatchesMeshDataVertexSize() {
        for (norms in listOf(false, true)) for (uvs in listOf(false, true)) {
            val mesh = meshData(norms, uvs)
            val attrs = RenderableModel.attributesFor(mesh)
            assertEquals(
                mesh.vertexSize * 4, attrs.vertexSize,
                "declared stride must equal MeshData's float count (norms=$norms uvs=$uvs)",
            )
        }
    }
}

class MaterialDataTest {

    @Test
    fun loaderLeavesTextureUnattached() {
        val material = MaterialData("m")
        material.diffuseTextureName = "data/bike.png"
        assertNull(material.diffuseTexture, "the GL texture is the renderer's job, not the loader's")
    }

    @Test
    fun materialsCompareByName() {
        assertEquals(MaterialData("paint"), MaterialData("paint"))
        assertEquals(MaterialData("paint").hashCode(), MaterialData("paint").hashCode())
    }
}

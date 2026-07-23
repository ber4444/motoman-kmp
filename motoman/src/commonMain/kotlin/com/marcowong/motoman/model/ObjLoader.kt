/*******************************************************************************
 * Copyright 2011 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.marcowong.motoman.model

import com.marcowong.motoman.assets.Assets
import com.marcowong.motoman.gl.GL_TRIANGLES
import com.marcowong.motoman.track.math.Color

/**
 * Loads Wavefront OBJ files. Kotlin port of the engine's `ObjLoaderEx`, itself derived
 * from libGDX's `ObjLoader` (original authors mzechner, espitz; Apache-2.0, retained above).
 *
 * Deliberately faithful to the original's output, including its polygon fan
 * triangulation, so per-model vertex/index counts still match the Phase 2b baseline.
 * Texture *creation* is not done here — materials carry the texture's name and the
 * renderer attaches it later, which keeps this loader GL-free and unit-testable.
 */
class ObjLoader(private val assets: Assets) {

    private class Group(val name: String) {
        var materialName: String = "default"
        val faces = IntList(200)
        var numFaces: Int = 0
        var hasNorms: Boolean = false
        var hasUVs: Boolean = false
    }

    fun loadObj(path: String, flipV: Boolean = false): ModelData? =
        parseObj(assets.readText(path), path, flipV)

    /** Parses OBJ source directly. [path] is used to resolve `mtllib`/`map_Kd` siblings. */
    fun parseObj(source: String, path: String = "", flipV: Boolean = false): ModelData? {
        val verts = FloatList(300)
        val norms = FloatList(300)
        val uvs = FloatList(200)
        val groups = ArrayList<Group>(10)
        var materials: List<MaterialData> = emptyList()

        // A "default" group covers OBJs that declare no o/g at all.
        var activeGroup = Group("default")
        groups.add(activeGroup)

        for (line in source.lineSequence()) {
            val tokens = line.split(WHITESPACE)
            if (tokens[0].isEmpty()) continue
            val firstChar = tokens[0].lowercase()[0]

            when {
                firstChar == '#' -> continue

                firstChar == 'v' -> when {
                    tokens[0].length == 1 -> {
                        verts.add(tokens[1].toFloat())
                        verts.add(tokens[2].toFloat())
                        verts.add(tokens[3].toFloat())
                    }
                    tokens[0][1] == 'n' -> {
                        norms.add(tokens[1].toFloat())
                        norms.add(tokens[2].toFloat())
                        norms.add(tokens[3].toFloat())
                    }
                    tokens[0][1] == 't' -> {
                        uvs.add(tokens[1].toFloat())
                        uvs.add(if (flipV) 1f - tokens[2].toFloat() else tokens[2].toFloat())
                    }
                }

                firstChar == 'f' -> {
                    val faces = activeGroup.faces
                    // Fan triangulation, replicating the original's index arithmetic
                    // exactly: each pass emits (v1, vi, vi+1), so a quad becomes
                    // (1,2,3) and (1,3,4). Net movement per pass is +1.
                    var i = 1
                    while (i < tokens.size - 2) {
                        var parts = tokens[1].split('/')
                        faces.add(getIndex(parts[0], verts.size))
                        if (parts.size > 2) {
                            if (i == 1) activeGroup.hasNorms = true
                            faces.add(getIndex(parts[2], norms.size))
                        }
                        if (parts.size > 1 && parts[1].isNotEmpty()) {
                            if (i == 1) activeGroup.hasUVs = true
                            faces.add(getIndex(parts[1], uvs.size))
                        }

                        i++
                        parts = tokens[i].split('/')
                        faces.add(getIndex(parts[0], verts.size))
                        if (parts.size > 2) faces.add(getIndex(parts[2], norms.size))
                        if (parts.size > 1 && parts[1].isNotEmpty()) faces.add(getIndex(parts[1], uvs.size))

                        i++
                        parts = tokens[i].split('/')
                        faces.add(getIndex(parts[0], verts.size))
                        if (parts.size > 2) faces.add(getIndex(parts[2], norms.size))
                        if (parts.size > 1 && parts[1].isNotEmpty()) faces.add(getIndex(parts[1], uvs.size))

                        activeGroup.numFaces++
                        i--
                    }
                }

                firstChar == 'o' || firstChar == 'g' -> {
                    // Only a single object/group name is honoured, as in the original.
                    val name = if (tokens.size > 1) tokens[1] else "default"
                    activeGroup = groups.firstOrNull { it.name == name } ?: Group(name).also { groups.add(it) }
                }

                tokens[0] == "mtllib" -> {
                    val mtlPath = assets.sibling(path, tokens[1])
                    if (assets.exists(mtlPath)) {
                        materials = MtlLoader.parse(assets.readText(mtlPath), mtlPath, assets)
                    }
                }

                tokens[0] == "usemtl" ->
                    activeGroup.materialName = if (tokens.size == 1) "default" else tokens[1]
            }
        }

        // Drop groups that never received a face.
        groups.retainAll { it.numFaces >= 1 }
        if (groups.isEmpty()) return null

        val model = ModelData()
        model.subMeshes = Array(groups.size) { g ->
            val group = groups[g]
            val faces = group.faces
            val hasNorms = group.hasNorms
            val hasUVs = group.hasUVs

            val finalVerts = FloatArray(group.numFaces * 3 * (3 + (if (hasNorms) 3 else 0) + (if (hasUVs) 2 else 0)))
            var i = 0
            var vi = 0
            while (i < faces.size) {
                var vertIndex = faces[i++] * 3
                finalVerts[vi++] = verts[vertIndex++]
                finalVerts[vi++] = verts[vertIndex++]
                finalVerts[vi++] = verts[vertIndex]
                if (hasNorms) {
                    var normIndex = faces[i++] * 3
                    finalVerts[vi++] = norms[normIndex++]
                    finalVerts[vi++] = norms[normIndex++]
                    finalVerts[vi++] = norms[normIndex]
                }
                if (hasUVs) {
                    var uvIndex = faces[i++] * 2
                    finalVerts[vi++] = uvs[uvIndex++]
                    finalVerts[vi++] = uvs[uvIndex]
                }
            }

            // Too many vertices to index with a short: fall back to non-indexed.
            val numIndices = if (group.numFaces * 3 >= Short.MAX_VALUE) 0 else group.numFaces * 3
            val finalIndices = ShortArray(numIndices) { it.toShort() }

            val mesh = MeshData()
            mesh.vertices = finalVerts
            mesh.indices = if (numIndices > 0) finalIndices else null
            mesh.hasNorms = hasNorms
            mesh.hasUVs = hasUVs

            SubMeshData().also {
                it.name = group.name
                it.mesh = mesh
                it.primitiveType = GL_TRIANGLES
                it.material = findMaterial(materials, group.materialName)
            }
        }
        return model
    }

    private fun findMaterial(materials: List<MaterialData>, name: String): MaterialData {
        val normalised = name.replace('.', '_')
        return materials.firstOrNull { it.name == normalised } ?: MaterialData("default")
    }

    /**
     * Resolves an OBJ index token. Mirrors the original exactly, including its treatment
     * of negative (relative) indices, where `size` is the raw component count rather than
     * a vertex count — preserved so loader output stays byte-identical to the baseline.
     */
    private fun getIndex(index: String?, size: Int): Int {
        if (index.isNullOrEmpty()) return 0
        val idx = index.toInt()
        return if (idx < 0) size + idx else idx - 1
    }

    private companion object {
        val WHITESPACE = Regex("\\s+")
    }
}

/** Parses Wavefront MTL material libraries. */
internal object MtlLoader {

    fun parse(source: String, mtlPath: String, assets: Assets): List<MaterialData> {
        val materials = ArrayList<MaterialData>()
        var curMatName = "default"
        var ambient = Color(1f, 1f, 1f, 1f)
        var diffuse = Color(1f, 1f, 1f, 1f)
        var specular = Color(1f, 1f, 1f, 1f)
        var shininess = Color(1f, 1f, 1f, 1f)
        var textureName: String? = null

        fun flush() {
            materials.add(
                MaterialData(curMatName).also {
                    it.diffuseTextureName = textureName
                    it.ambientColor = ambient
                    it.diffuseColor = diffuse
                    it.specularColor = specular
                    it.shininessColor = shininess
                },
            )
        }

        for (raw in source.lineSequence()) {
            val line = if (raw.isNotEmpty() && raw[0] == '\t') raw.substring(1).trim() else raw
            val tokens = line.split(WHITESPACE)
            if (tokens[0].isEmpty() || tokens[0][0] == '#') continue

            when (tokens[0].lowercase()) {
                "newmtl" -> {
                    // The original emits the *previous* material here, so a leading
                    // "default" entry is always present. Preserved for parity.
                    flush()
                    curMatName = if (tokens.size > 1) tokens[1].replace('.', '_') else "default"
                    ambient = Color(1f, 1f, 1f, 1f)
                    diffuse = Color(1f, 1f, 1f, 1f)
                    specular = Color(1f, 1f, 1f, 1f)
                    shininess = Color(1f, 1f, 1f, 1f)
                    textureName = null
                }

                "ka", "kd", "ks" -> {
                    val r = tokens[1].toFloat()
                    val g = tokens[2].toFloat()
                    val b = tokens[3].toFloat()
                    val a = if (tokens.size > 4) tokens[4].toFloat() else 1f
                    when (tokens[0].lowercase()) {
                        "kd" -> diffuse = Color(r, g, b, a)
                        "ks" -> specular = Color(r, g, b, a)
                        else -> ambient = Color(r, g, b, a)
                    }
                }

                "ns" -> {
                    val v = 1.0f / tokens[1].toFloat()
                    shininess = Color(v, v, v, 1f)
                }

                "map_kd" -> {
                    val name = tokens.getOrNull(1).orEmpty()
                    textureName = if (name.isNotEmpty()) assets.sibling(mtlPath, name) else null
                }
            }
        }
        flush() // trailing material
        return materials
    }

    private val WHITESPACE = Regex("\\s+")
}

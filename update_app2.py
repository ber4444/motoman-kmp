import re

with open("motoman/src/commonMain/kotlin/com/marcowong/motoman/MotomanGameApp.kt", "r") as f:
    content = f.read()

# Fix MeshData
old_mesh = """val frameBufferMesh = com.marcowong.motoman.model.MeshData(
            vertices = floatArrayOf(
                -1f, -1f, 0f, 0f, 0f,
                1f, -1f, 0f, 1f, 0f,
                1f, 1f, 0f, 1f, 1f,
                -1f, 1f, 0f, 0f, 1f
            ),
            indices = shortArrayOf(0, 1, 2, 2, 3, 0),
            vertexSize = 5,
            hasNorms = false,
            hasUVs = true,
            hasSkeleton = false
        )"""

new_mesh = """val frameBufferMesh = com.marcowong.motoman.model.MeshData().apply {
            vertices = floatArrayOf(
                -1f, -1f, 0f, 0f, 0f,
                1f, -1f, 0f, 1f, 0f,
                1f, 1f, 0f, 1f, 1f,
                -1f, 1f, 0f, 0f, 1f
            )
            indices = shortArrayOf(0, 1, 2, 2, 3, 0)
            hasNorms = false
            hasUVs = true
            hasSkeleton = false
        }"""
content = content.replace(old_mesh, new_mesh)

# Fix unbind
content = content.replace("standardShader.unbind()", "gl.glUseProgram(0)")
content = content.replace("ppCopyShader.unbind()", "gl.glUseProgram(0)")
content = content.replace("ppMotionBlurShader.unbind()", "gl.glUseProgram(0)")
content = content.replace("maskShader.unbind()", "gl.glUseProgram(0)")
content = content.replace("ppBloom1Shader.unbind()", "gl.glUseProgram(0)")
content = content.replace("ppBloom2Shader.unbind()", "gl.glUseProgram(0)")
content = content.replace("ppShader.unbind()", "gl.glUseProgram(0)")
content = content.replace("ppAntiAliasingShader.unbind()", "gl.glUseProgram(0)")
content = content.replace("ppFinalShader.unbind()", "gl.glUseProgram(0)")

# Fix uniforms
content = content.replace("ppMotionBlurShader.setUniform2f(\"frameBufferPixelSize\", 1f / mainFB.width, 1f / mainFB.height)", "ppMotionBlurShader.setUniformf(\"frameBufferPixelSize\", 1f / mainFB.width, 1f / mainFB.height)")
content = content.replace("ppMotionBlurShader.setUniformMatrix4fv(\"viewproj\", false, camera.combined.`val`)", "ppMotionBlurShader.setUniformMatrix(\"viewproj\", camera.combined, false)")
content = content.replace("ppMotionBlurShader.setUniformMatrix4fv(\"viewprojinv\", false, camera.invProjectionView.`val`)", "ppMotionBlurShader.setUniformMatrix(\"viewprojinv\", camera.invProjectionView, false)")
content = content.replace("ppMotionBlurShader.setUniformMatrix4fv(\"lastviewproj\", false, lastCameraView.`val`)", "ppMotionBlurShader.setUniformMatrix(\"lastviewproj\", lastCameraView, false)")
content = content.replace("maskShader.setUniform4f(\"maskColor\", 0f, 0f, 0f, 1f)", "maskShader.setUniformf(\"maskColor\", 0f, 0f, 0f, 1f)")
content = content.replace("ppBloom1Shader.setUniform1f(\"blurSize\", 1f / bloomFBA.width)", "ppBloom1Shader.setUniformf(\"blurSize\", 1f / bloomFBA.width)")
content = content.replace("ppBloom2Shader.setUniform1f(\"blurSize\", 1f / bloomFBB.height)", "ppBloom2Shader.setUniformf(\"blurSize\", 1f / bloomFBB.height)")
content = content.replace("ppAntiAliasingShader.setUniform2f(\"frameBufferPixelSize\", 1f / mainFB.width, 1f / mainFB.height)", "ppAntiAliasingShader.setUniformf(\"frameBufferPixelSize\", 1f / mainFB.width, 1f / mainFB.height)")

with open("motoman/src/commonMain/kotlin/com/marcowong/motoman/MotomanGameApp.kt", "w") as f:
    f.write(content)

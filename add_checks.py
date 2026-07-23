import re

with open("motoman/src/commonMain/kotlin/com/marcowong/motoman/MotomanGameApp.kt", "r") as f:
    content = f.read()

shaders_to_check = ["maskShader", "ppFinalShader", "ppCopyShader", "ppMotionBlurShader", "ppBloom1Shader", "ppBloom2Shader", "ppAntiAliasingShader", "ppShader"]

for shader in shaders_to_check:
    check_code = f"\n        if (!{shader}.isCompiled) error(\"{shader} failed to compile: ${{{shader}.log}}\")\n"
    # insert right after shader creation
    pattern = rf"({shader} = ShaderProgram\([^)]+\))"
    content = re.sub(pattern, r"\1" + check_code, content)

with open("motoman/src/commonMain/kotlin/com/marcowong/motoman/MotomanGameApp.kt", "w") as f:
    f.write(content)

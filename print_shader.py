import re

with open("motoman/src/commonMain/kotlin/com/marcowong/motoman/gl/ShaderPreprocessor.kt", "r") as f:
    content = f.read()

content = content.replace("append(neutralisePrecision(body))", """val processed = neutralisePrecision(body)
                if (processed.contains("BlurScale")) {
                    println("--- DESKTOP SHADER OUTPUT ---")
                    println(processed)
                    println("-----------------------------")
                }
                append(processed)""")

with open("motoman/src/commonMain/kotlin/com/marcowong/motoman/gl/ShaderPreprocessor.kt", "w") as f:
    f.write(content)

import re

with open("motoman/src/commonTest/kotlin/com/marcowong/motoman/gl/FakeGl.kt", "r") as f:
    content = f.read()

dummy_methods = """
    override fun glGenFramebuffer(): Int = 1
    override fun glBindFramebuffer(target: Int, framebuffer: Int) {}
    override fun glDeleteFramebuffer(framebuffer: Int) {}
    override fun glFramebufferTexture2D(target: Int, attachment: Int, textarget: Int, texture: Int, level: Int) {}
    override fun glCheckFramebufferStatus(target: Int): Int = 0x8CD5 // GL_FRAMEBUFFER_COMPLETE
    override fun glGenRenderbuffer(): Int = 2
    override fun glBindRenderbuffer(target: Int, renderbuffer: Int) {}
    override fun glDeleteRenderbuffer(renderbuffer: Int) {}
    override fun glRenderbufferStorage(target: Int, internalformat: Int, width: Int, height: Int) {}
    override fun glFramebufferRenderbuffer(target: Int, attachment: Int, renderbuffertarget: Int, renderbuffer: Int) {}
}"""

content = content.replace("\n}", dummy_methods)

with open("motoman/src/commonTest/kotlin/com/marcowong/motoman/gl/FakeGl.kt", "w") as f:
    f.write(content)

package com.marcowong.motoman.gl

/**
 * GL enum constants used by the engine. These numeric values are identical between
 * GLES 2.0 and desktop GL for everything Motoman touches, so they are declared once
 * in commonMain rather than being `expect`ed per platform.
 */
// Declared top-level (not inside an `object GL`) on purpose: an `object GL` compiles to
// GL.class, which collides with the Gl interface's Gl.class on case-insensitive
// filesystems (macOS). Top-level consts land in GlConstantsKt.class and stay portable.
// Errors
const val GL_NO_ERROR = 0

// Clear bits
const val GL_DEPTH_BUFFER_BIT = 0x00000100
const val GL_COLOR_BUFFER_BIT = 0x00004000

// Booleans / capabilities
const val GL_DEPTH_TEST = 0x0B71
const val GL_CULL_FACE = 0x0B44
const val GL_BLEND = 0x0BE2

// Comparison / depth funcs
const val GL_NEVER = 0x0200
const val GL_LESS = 0x0201
const val GL_EQUAL = 0x0202
const val GL_LEQUAL = 0x0203
const val GL_GREATER = 0x0204
const val GL_ALWAYS = 0x0207

// Blend factors
const val GL_ZERO = 0
const val GL_ONE = 1
const val GL_SRC_ALPHA = 0x0302
const val GL_ONE_MINUS_SRC_ALPHA = 0x0303

// Cull / winding
const val GL_FRONT = 0x0404
const val GL_BACK = 0x0405
const val GL_CW = 0x0900
const val GL_CCW = 0x0901

// Buffer targets / usage
const val GL_ARRAY_BUFFER = 0x8892
const val GL_ELEMENT_ARRAY_BUFFER = 0x8893
const val GL_STATIC_DRAW = 0x88E4
const val GL_DYNAMIC_DRAW = 0x88E8

// Data types
const val GL_BYTE = 0x1400
const val GL_UNSIGNED_BYTE = 0x1401
const val GL_SHORT = 0x1402
const val GL_UNSIGNED_SHORT = 0x1403
const val GL_INT = 0x1404
const val GL_UNSIGNED_INT = 0x1405
const val GL_FLOAT = 0x1406

// Primitive modes
const val GL_POINTS = 0x0000
const val GL_LINES = 0x0001
const val GL_LINE_STRIP = 0x0003
const val GL_TRIANGLES = 0x0004
const val GL_TRIANGLE_STRIP = 0x0005
const val GL_TRIANGLE_FAN = 0x0006

// Textures
const val GL_TEXTURE_2D = 0x0DE1
const val GL_TEXTURE0 = 0x84C0
const val GL_TEXTURE_MAG_FILTER = 0x2800
const val GL_TEXTURE_MIN_FILTER = 0x2801
const val GL_TEXTURE_WRAP_S = 0x2802
const val GL_TEXTURE_WRAP_T = 0x2803
const val GL_NEAREST = 0x2600
const val GL_LINEAR = 0x2601
const val GL_NEAREST_MIPMAP_NEAREST = 0x2700
const val GL_LINEAR_MIPMAP_NEAREST = 0x2701
const val GL_NEAREST_MIPMAP_LINEAR = 0x2702
const val GL_LINEAR_MIPMAP_LINEAR = 0x2703
const val GL_CLAMP_TO_EDGE = 0x812F
const val GL_REPEAT = 0x2901

// Pixel formats
const val GL_ALPHA = 0x1906
const val GL_RGB = 0x1907
const val GL_RGBA = 0x1908
const val GL_LUMINANCE = 0x1909

// Shaders / programs
const val GL_FRAGMENT_SHADER = 0x8B30
const val GL_VERTEX_SHADER = 0x8B31
const val GL_COMPILE_STATUS = 0x8B81
const val GL_LINK_STATUS = 0x8B82

// glGetString names
const val GL_VENDOR = 0x1F00
const val GL_RENDERER = 0x1F01
const val GL_VERSION = 0x1F02
const val GL_EXTENSIONS = 0x1F03
const val GL_SHADING_LANGUAGE_VERSION = 0x8B8C

// Framebuffers
const val GL_FRAMEBUFFER = 0x8D40
const val GL_COLOR_ATTACHMENT0 = 0x8CE0
const val GL_DEPTH_ATTACHMENT = 0x8D00
const val GL_RENDERBUFFER = 0x8D41
const val GL_DEPTH_COMPONENT16 = 0x81A5
const val GL_FRAMEBUFFER_COMPLETE = 0x8CD5

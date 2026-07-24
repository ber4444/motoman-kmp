#pragma once

#ifdef __cplusplus
extern "C" {
#endif

// Define standard GL types to avoid including OpenGLES headers
typedef unsigned int GLuint;
typedef int GLint;
typedef int GLsizei;
typedef unsigned int GLenum;
typedef unsigned char GLboolean;
typedef unsigned int GLbitfield;

// External GL functions that will be provided by MetalANGLE at runtime
extern GLuint glCreateShader(GLenum type);
extern void glShaderSource(GLuint shader, GLsizei count, const char *const*string, const GLint *length);
extern void glCompileShader(GLuint shader);
extern void glGetShaderiv(GLuint shader, GLenum pname, GLint *params);
extern void glGetShaderInfoLog(GLuint shader, GLsizei bufSize, GLsizei *length, char *infoLog);
extern void glDeleteShader(GLuint shader);
extern GLuint glCreateProgram(void);
extern void glAttachShader(GLuint program, GLuint shader);
extern void glBindAttribLocation(GLuint program, GLuint index, const char *name);
extern void glLinkProgram(GLuint program);
extern void glGetProgramiv(GLuint program, GLenum pname, GLint *params);
extern void glGetProgramInfoLog(GLuint program, GLsizei bufSize, GLsizei *length, char *infoLog);
extern void glDeleteProgram(GLuint program);
extern void glUseProgram(GLuint program);
extern GLint glGetAttribLocation(GLuint program, const char *name);
extern GLint glGetUniformLocation(GLuint program, const char *name);
extern void glUniformMatrix4fv(GLint location, GLsizei count, GLboolean transpose, const float *value);
extern void glUniform1i(GLint location, GLint v0);
extern void glUniform1f(GLint location, float v0);
extern void glUniform2f(GLint location, float v0, float v1);
extern void glUniform3f(GLint location, float v0, float v1, float v2);
extern void glUniform4f(GLint location, float v0, float v1, float v2, float v3);
extern void glUniform3fv(GLint location, GLsizei count, const float *value);
extern void glUniform4fv(GLint location, GLsizei count, const float *value);
extern void glGenBuffers(GLsizei n, GLuint *buffers);
extern void glBufferData(GLenum target, GLsizei size, const void *data, GLenum usage);
extern void glBindBuffer(GLenum target, GLuint buffer);
extern void glDeleteBuffers(GLsizei n, const GLuint *buffers);
extern void glVertexAttribPointer(GLuint index, GLint size, GLenum type, GLboolean normalized, GLsizei stride, const void *pointer);
extern void glEnableVertexAttribArray(GLuint index);
extern void glDisableVertexAttribArray(GLuint index);
extern void glGenTextures(GLsizei n, GLuint *textures);
extern void glBindTexture(GLenum target, GLuint texture);
extern void glActiveTexture(GLenum texture);
extern void glTexImage2D(GLenum target, GLint level, GLint internalformat, GLsizei width, GLsizei height, GLint border, GLenum format, GLenum type, const void *pixels);
extern void glTexParameteri(GLenum target, GLenum pname, GLint param);
extern void glGenerateMipmap(GLenum target);
extern void glDeleteTextures(GLsizei n, const GLuint *textures);
extern void glGenFramebuffers(GLsizei n, GLuint *framebuffers);
extern void glBindFramebuffer(GLenum target, GLuint framebuffer);
extern void glDeleteFramebuffers(GLsizei n, const GLuint *framebuffers);
extern void glFramebufferTexture2D(GLenum target, GLenum attachment, GLenum textarget, GLuint texture, GLint level);
extern GLenum glCheckFramebufferStatus(GLenum target);
extern void glGenRenderbuffers(GLsizei n, GLuint *renderbuffers);
extern void glBindRenderbuffer(GLenum target, GLuint renderbuffer);
extern void glDeleteRenderbuffers(GLsizei n, const GLuint *renderbuffers);
extern void glRenderbufferStorage(GLenum target, GLenum internalformat, GLsizei width, GLsizei height);
extern void glFramebufferRenderbuffer(GLenum target, GLenum attachment, GLenum renderbuffertarget, GLuint renderbuffer);
extern void glReadPixels(GLint x, GLint y, GLsizei width, GLsizei height, GLenum format, GLenum type, void *pixels);
extern void glViewport(GLint x, GLint y, GLsizei width, GLsizei height);
extern void glClearColor(float red, float green, float blue, float alpha);
extern void glClear(GLbitfield mask);
extern void glEnable(GLenum cap);
extern void glDisable(GLenum cap);
extern void glDepthFunc(GLenum func);
extern void glDepthMask(GLboolean flag);
extern void glCullFace(GLenum mode);
extern void glFrontFace(GLenum mode);
extern void glBlendFunc(GLenum sfactor, GLenum dfactor);
extern void glDrawArrays(GLenum mode, GLint first, GLsizei count);
extern void glDrawElements(GLenum mode, GLsizei count, GLenum type, const void *indices);
extern GLenum glGetError(void);
extern const unsigned char *glGetString(GLenum name);

#define GL_COMPILE_STATUS 0x8B81
#define GL_LINK_STATUS 0x8B82

int mwgl_create_shader(int type);
void mwgl_shader_source(int shader, const char* source);
void mwgl_compile_shader(int shader);
int mwgl_get_shader_compile_status(int shader);
void mwgl_get_shader_info_log(int shader, char* log, int bufSize);
void mwgl_delete_shader(int shader);

int mwgl_create_program_id(void);
void mwgl_attach_shader(int program, int shader);
void mwgl_bind_attrib_location(int program, int index, const char* name);
void mwgl_link_program(int program);
int mwgl_get_program_link_status(int program);
void mwgl_get_program_info_log(int program, char* log, int bufSize);
void mwgl_delete_program(int program);

void mwgl_use_program(int program);
int mwgl_attrib_location(int program, const char* name);
int mwgl_uniform_location(int program, const char* name);
void mwgl_uniform_matrix4fv(int loc, int count, int transpose, const float* value);
void mwgl_uniform1i(int loc, int v);
void mwgl_uniform1f(int loc, float v);
void mwgl_uniform2f(int loc, float x, float y);
void mwgl_uniform3f(int loc, float x, float y, float z);
void mwgl_uniform4f(int loc, float x, float y, float z, float w);
void mwgl_uniform3fv(int loc, const float* v);
void mwgl_uniform4fv(int loc, const float* v);
int mwgl_create_buffer(void);
void mwgl_buffer_data(int target, const float* data, int count, int usage);
void mwgl_buffer_data_short(int target, const short* data, int count, int usage);
void mwgl_bind_buffer(int target, int buffer);
void mwgl_delete_buffer(int buffer);
void mwgl_vertex_attrib_pointer(int index, int size, int type, int normalized, int stride, int offset);
void mwgl_enable_vertex_attrib_array(int index);
void mwgl_disable_vertex_attrib_array(int index);
int mwgl_create_texture(void);
void mwgl_bind_texture(int target, int texture);
void mwgl_active_texture(int unit);
void mwgl_tex_image_2d(int target, int level, int internalFormat, int w, int h, int border, int format, int type, const void* data);
void mwgl_tex_parameteri(int target, int pname, int value);
void mwgl_generate_mipmap(int target);
void mwgl_delete_texture(int texture);
int mwgl_create_framebuffer(void);
void mwgl_bind_framebuffer(int target, int framebuffer);
void mwgl_delete_framebuffer(int framebuffer);
void mwgl_framebuffer_texture_2d(int target, int attachment, int textarget, int texture, int level);
int mwgl_check_framebuffer_status(int target);
int mwgl_create_renderbuffer(void);
void mwgl_bind_renderbuffer(int target, int renderbuffer);
void mwgl_delete_renderbuffer(int renderbuffer);
void mwgl_renderbuffer_storage(int target, int internalformat, int width, int height);
void mwgl_framebuffer_renderbuffer(int target, int attachment, int renderbuffertarget, int renderbuffer);
void mwgl_read_pixels(int x, int y, int width, int height, int format, int type, void* pixels);
void mwgl_viewport(int x, int y, int w, int h);
void mwgl_clear_color(float r, float g, float b, float a);
void mwgl_clear(int mask);
void mwgl_enable(int cap);
void mwgl_disable(int cap);
void mwgl_depth_func(int func);
void mwgl_depth_mask(int flag);
void mwgl_cull_face(int mode);
void mwgl_front_face(int mode);
void mwgl_blend_func(int sfactor, int dfactor);
void mwgl_draw_arrays(int mode, int first, int count);
void mwgl_draw_elements(int mode, int count, int type, int offset);
int mwgl_get_error(void);
const char* mwgl_get_string(int name);

#ifdef __cplusplus
}
#endif

#pragma once
#include <OpenGLES/ES2/gl.h>

#ifdef __cplusplus
extern "C" {
#endif

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

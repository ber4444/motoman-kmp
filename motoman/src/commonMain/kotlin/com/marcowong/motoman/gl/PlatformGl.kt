package com.marcowong.motoman.gl

/**
 * Constructs the raw, undecorated platform GL. `expect` is confined to construction
 * (per Phase 3 Correction 2) so the interface itself stays common and decoratable.
 * Typical wiring: `GlOptimized(GlDebug(createPlatformGl()))`.
 */
expect fun createPlatformGl(): Gl

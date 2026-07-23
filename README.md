# Motoman

This is a Kotlin Multiplatform motorcycle racing game, ported from https://github.com/mammalwong/motoman.

The game relies purely on LibGDX as an OpenGL wrapper and doesn't use any pre-existing game engine.

Blender was used to create, model, and export all the 3D environmental and character assets (such as the motorcycle, rider, buildings, and skybox) as .obj and .mtl files for rendering within the game.

### Features
- Random seed generated racing tracks, unlimited tracks as you progress the game.
- Incoming corner type notification like rally car games.
- Active camera that looks into the incoming corner apex.
- Two seperated steering control, counter steering and leaning.
- Supports using tilting to lean (disabled in source).
- Supports simple traditional steering by combining the two steering methods.
- A physical strength system to reduce the effectiveness of steering if too much action inputed in a row.
- GLSL shader effects.

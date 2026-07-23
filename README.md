# Motoman

This is a Kotlin Multiplatform motorcycle racing game, ported from https://github.com/mammalwong/motoman.

The game relies on a custom `Gl` interface mapping directly to raw Android GLES 2.0 and Desktop LWJGL/GLFW, without using any pre-existing game engine or heavy frameworks like LibGDX. The HUD and UI layers are rendered natively using Jetpack Compose.

Blender was used to create, model, and export all the 3D environmental and character assets (such as the motorcycle, rider, buildings, and skybox) as .obj and .mtl files for rendering within the game.

### Features
- Random seed generated racing tracks, unlimited tracks as you progress the game.
- Incoming corner type notification like rally car games (powered by Jetpack Compose).
- Active camera that looks into the incoming corner apex.
- Two separated steering controls: counter steering and leaning.
- Supports using device tilt to lean (enabled natively via Android SensorManager).
- Supports simple traditional steering by combining the two steering methods.
- A physical strength system to reduce the effectiveness of steering if too much action is inputted in a row.
- Native GLSL shader effects (Bloom, Motion Blur, FXAA).

# Motoman

This is a Kotlin Multiplatform motorcycle racing game, ported from https://github.com/mammalwong/motoman.

The game relies on a custom `Gl` interface mapping directly to raw Android GLES 2.0 and Desktop LWJGL/GLFW, without using any game engine or heavy frameworks. 

The HUD and UI layers are rendered natively using Jetpack Compose.

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

### Getting Started

#### Prerequisites
- JDK 17 or newer
- Android Studio or IntelliJ IDEA with Kotlin Multiplatform support
- Android SDK (for Android builds)

#### Building and Running

**Desktop:**
The desktop module provides a fast edit-run cycle using LWJGL.
To run the game on desktop:
```sh
./gradlew :motoman:runDesktop --args="--game"
```
To run the model viewer to preview a specific `.obj` asset:
```sh
./gradlew :motoman:runDesktop --args="--model data/bike.obj"
```

**Android:**
To build and install the Android app to a connected device or emulator:
```sh
./gradlew :motoman-android:installDebug
```
Alternatively, open the project in Android Studio and run the `motoman-android` configuration.

### Architecture

While structured as a standard Kotlin Multiplatform (KMP) project, this game features several unique architectural choices that set it apart:

- **Engine-less Design:** Instead of relying on a heavyweight game engine, the game is built entirely from scratch in Kotlin.
- **Custom Abstracted `Gl` Interface:** The core rendering logic is entirely platform-agnostic. It runs against a custom `Gl` interface which directly maps to `android.opengl.GLES20` on Android and `LWJGL GL11/20` on Desktop, guaranteeing high-performance native OpenGL execution on both platforms.
- **Native UI Overlay:** Instead of drawing the Heads-Up Display (HUD) and menus within the OpenGL context, the game relies on native UI toolkits. On Android, the HUD is built entirely in Jetpack Compose, effortlessly overlaying the `GLSurfaceView`.
- **Custom Asset Pipeline:** Features a bespoke, lightweight `.obj` and `.mtl` parser written in pure Kotlin to load 3D assets and materials directly into the rendering pipeline.
- **Native Shaders:** Advanced visual effects like Bloom, Motion Blur, and FXAA are implemented directly via custom GLSL shaders executing natively against the platform's graphics hardware.

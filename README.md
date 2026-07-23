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

The project is structured as a Kotlin Multiplatform (KMP) application with a core shared engine and platform-specific hosts.

- **`motoman/src/commonMain`**: Contains the core game loop, physics engine, scene graph, track generation, and the abstracted `Gl` interface. This code is entirely platform-agnostic. 
- **`motoman/src/desktopMain`**: Implements the desktop host using LWJGL and GLFW for windowing, rendering, and input.
- **`motoman/src/androidMain`**: Contains Android-specific implementations such as `Gl` mapping directly to `android.opengl.GLES20` and asset loading.
- **`motoman-android`**: The Android application module. It hosts the game inside a `GLSurfaceView` and natively overlays the HUD and UI using Jetpack Compose.
- **Rendering Pipeline**: The engine loads `.obj` and `.mtl` assets natively and draws them using a custom OpenGL pipeline with GLSL shaders. We rely on OpenGL ES 2.0 (Android) and OpenGL 2.1 (Desktop), avoiding heavy game engine dependencies.

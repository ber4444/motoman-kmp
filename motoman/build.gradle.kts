plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    // No Compose here. Per Phase 3 Correction 1 the engine stays Compose-free so
    // "desktop has no Compose" is enforced by the dependency graph, not convention.
    // The HUD lives in the separate motoman-ui module (Android target only for now).
}

// LWJGL ships its native binaries as classifier artifacts, one per OS/arch. Resolve the
// right one for the building machine so desktop runs on macOS (Intel + Apple Silicon),
// Linux (x64 + arm64) and Windows without hand-editing the build.
val lwjglNatives: String = run {
    val os = System.getProperty("os.name")!!
    val arch = System.getProperty("os.arch")!!
    when {
        os.startsWith("Mac OS X") || os.startsWith("Darwin") ->
            if (arch.startsWith("aarch64")) "natives-macos-arm64" else "natives-macos"
        os.startsWith("Linux") ->
            if (arch.startsWith("aarch64")) "natives-linux-arm64" else "natives-linux"
        os.startsWith("Windows") -> "natives-windows"
        else -> error("Unsupported host OS for LWJGL natives: $os")
    }
}

kotlin {
    androidTarget {
        publishLibraryVariants("release", "debug")
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }
    jvm("desktop") {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":motoman-track"))
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.android)
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.lwjgl.core)
                implementation(libs.lwjgl.opengl)
                implementation(libs.lwjgl.glfw)
                implementation(libs.lwjgl.stb) // image decoding, no extra native dependency

                val lwjglVersion = libs.versions.lwjgl.get()
                runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:$lwjglNatives")
                runtimeOnly("org.lwjgl:lwjgl-opengl:$lwjglVersion:$lwjglNatives")
                runtimeOnly("org.lwjgl:lwjgl-glfw:$lwjglVersion:$lwjglNatives")
                runtimeOnly("org.lwjgl:lwjgl-stb:$lwjglVersion:$lwjglNatives")
            }
        }
    }
}

/**
 * Runs the desktop smoke app. macOS requires GLFW to own the first thread, so
 * -XstartOnFirstThread is added there; Linux and Windows must NOT get it.
 *
 *   ./gradlew :motoman:runDesktop                      (interactive window)
 *   ./gradlew :motoman:runDesktop --args="--frames 3"  (auto-exit smoke run)
 */
tasks.register<JavaExec>("runDesktop") {
    group = "application"
    description = "Runs the LWJGL/GLFW desktop host."
    val desktopMain = kotlin.jvm("desktop").compilations.getByName("main")
    dependsOn(desktopMain.compileTaskProvider)
    classpath = files(desktopMain.output.allOutputs, desktopMain.runtimeDependencyFiles)
    mainClass.set("com.marcowong.motoman.DesktopSmokeKt")
    if (System.getProperty("os.name")!!.let { it.startsWith("Mac OS X") || it.startsWith("Darwin") }) {
        jvmArgs("-XstartOnFirstThread")
    }
}

android {
    namespace = "com.marcowong.motoman"
    compileSdk = 34
    defaultConfig {
        minSdk = 21
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

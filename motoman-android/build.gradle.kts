plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.marcowong.motoman"
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.marcowong.motoman"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    
    sourceSets {
        getByName("main") {
            manifest.srcFile("AndroidManifest.xml")
            java.srcDirs("src")
            res.srcDirs("res")
            assets.srcDirs("assets")
            jniLibs.srcDirs("libs") // This includes armeabi and armeabi-v7a .so files
        }
    }
    
    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(project(":motoman"))
}

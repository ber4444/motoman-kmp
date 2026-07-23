plugins {
    alias(libs.plugins.android.application)
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
}

dependencies {
    implementation(project(":motoman"))
    implementation(files("libs/gdx-backend-android.jar"))
}

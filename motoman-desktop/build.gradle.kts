plugins {
    id("java")
}
sourceSets {
    main {
        java.srcDirs("src")
        resources.srcDirs("assets")
    }
}
dependencies {
    implementation(project(":motoman"))
    implementation(files("libs/gdx-backend-lwjgl.jar"))
    runtimeOnly(files("libs/gdx-backend-lwjgl-natives.jar", "libs/gdx-natives.jar"))
}

plugins{
    alias { libs.plugins.multiplatform } apply false
    alias { libs.plugins.agp } apply false
}

buildscript {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

tasks.register<Delete>("clean") {
    description = "Clean project build directory"
    group = JavaBasePlugin.BUILD_TASK_NAME
    delete(project.layout.buildDirectory)
}

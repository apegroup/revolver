buildscript {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.android.gradlePlugin)
    }
}

tasks.register<Delete>("clean") {
    description = "Clean project build directory"
    group = JavaBasePlugin.BUILD_TASK_NAME
    delete(project.layout.buildDirectory)
}

@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}
rootProject.name = "revolver-root"
include(":revolver")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
        mavenLocal()
    }
}

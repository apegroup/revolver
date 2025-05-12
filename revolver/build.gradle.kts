@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.multiplatform)
    alias(libs.plugins.agp)
    id("maven-publish")
}

/* Library Specs */
val libAndroidNamespace: String by project
val libBaseGroup: String by project
val libBaseVersion: String by project

group = libBaseGroup
version = libBaseVersion

kotlin {
    androidLibrary {
        namespace = libAndroidNamespace
        compileSdk = findProperty("android.compileSdk").toString().toInt()
        minSdk = findProperty("android.minSdk").toString().toInt()
    }

    jvmToolchain(17)
    iosArm64()
    iosX64()
    iosSimulatorArm64()

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.coroutines.core)
                implementation(libs.napier)
            }
        }
        commonTest {
            dependencies {
                implementation(libs.bundles.testDependencies)
            }
        }
        androidMain {
            dependencies {
                api(libs.androidx.lifecycle.viewmodel)
            }
        }
    }
}

publishing {
    repositories {
        maven {
            setUrl("https://maven.pkg.github.com/apegroup/revolver")
            name = "GitHubPackages"
            credentials {
                username = System.getenv("GH_USERNAME") ?: properties["GH_USERNAME"]?.toString()
                password = System.getenv("GH_TOKEN") ?: properties["GH_TOKEN"]?.toString()
            }
        }
    }
}
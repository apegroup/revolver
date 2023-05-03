plugins {
    kotlin("multiplatform") version "1.8.0"
    id("com.android.library")
    id("maven-publish")
}

/* Library Specs */
val libAndroidNamespace: String by project
val libDeveloperOrg: String by project
val libMavenPublish: String by project
val libBaseName: String by project
val libBaseGroup: String by project
val libBaseVersion: String by project

group = libBaseGroup
version = libBaseVersion

kotlin {
    android {
        publishLibraryVariants("release", "debug")
        publishLibraryVariantsGroupedByFlavor = true
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()


    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.coroutines.core)
                implementation(libs.napier)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.bundles.testDependencies)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.lifecycle.viewmodel)
            }
        }

        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosX64Main by getting

        val iosMain by creating {
            dependsOn(commonMain)
        }

        val iosDeviceMain by creating {
            dependsOn(iosMain)
            iosArm64Main.dependsOn(this)
        }

        val iosSimulatorMain by creating {
            dependsOn(iosMain)
            iosSimulatorArm64Main.dependsOn(this)
            iosX64Main.dependsOn(this)
        }

        val iosX64Test by getting
        val iosArm64Test by getting
        val iosSimulatorArm64Test by getting
        val iosTest by creating {
            dependsOn(commonTest)
            iosX64Test.dependsOn(this)
            iosArm64Test.dependsOn(this)
            iosSimulatorArm64Test.dependsOn(this)
        }
    }
}

publishing {
    repositories {
        maven {
            setUrl("https://maven.pkg.github.com/apegroup/revolver")

            credentials {
                username = System.getenv("GH_USERNAME") ?: properties["GH_USERNAME"]?.toString()
                password = System.getenv("GH_TOKEN") ?: properties["GH_TOKEN"]?.toString()
            }
        }
    }
}

android {
    compileSdk = 30
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = 21
        targetSdk = 30
    }
}
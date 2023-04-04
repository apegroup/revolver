buildscript {
    repositories {
        mavenCentral()
        google()
    }
    dependencies {
        classpath(libs.android.gradlePlugin)
    }
}

plugins {
    kotlin("multiplatform") version "1.8.0"
    id("com.android.library") version "7.4.0-beta02"
    `version-catalog`
    `maven-publish`
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

repositories {
    mavenCentral()
    google()
}

kotlin {

    android()
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

android {
    namespace = libAndroidNamespace
    compileSdk = (findProperty("android.compileSdk") as String).toInt()

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        minSdk = (findProperty("android.minSdk") as String).toInt()
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = false
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

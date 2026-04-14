import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val jdkVersion = findProperty("jdkVersion").toString()

android {
    namespace = "com.umain.mediumandroidintegration"
    compileSdk = findProperty("android.compileSdk").toString().toInt()

    defaultConfig {
        applicationId = "com.umain.mediumandroidintegration"
        minSdk = findProperty("android.minSdk").toString().toInt()
        targetSdk = findProperty("android.targetSdk").toString().toInt()
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(jdkVersion)
        targetCompatibility = JavaVersion.toVersion(jdkVersion)
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(jdkVersion))
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.revolver)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.compose.material.icons)

    debugImplementation(libs.androidx.ui.tooling)
}

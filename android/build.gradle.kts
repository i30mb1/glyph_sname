import com.github.triplet.gradle.androidpublisher.ReleaseStatus

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.play.publisher)
}

// Google Play publishing: straight to production. Store listing graphics and release notes live in src/main/play/.
play {
    serviceAccountCredentials.set(rootProject.file("play-service-account.json"))
    defaultToAppBundles.set(true)
    track.set("production")
    releaseStatus.set(ReleaseStatus.COMPLETED)
}

android {
    namespace = "n7.snake"
    compileSdk = 37

    defaultConfig {
        applicationId = "n7.snake"
        minSdk = 35
        targetSdk = 37
        versionCode = 9
        versionName = "1.2.1"
    }

    // Upload key from ~/.gradle/gradle.properties (n7.upload.*, or the shared kcal.upload.* key).
    // Without it, release builds fall back to the debug key so a fresh clone still builds.
    fun uploadProp(name: String): String? =
        providers.gradleProperty("n7.upload.$name").orNull ?: providers.gradleProperty("kcal.upload.$name").orNull
    val uploadStore = uploadProp("storeFile")
    signingConfigs {
        if (uploadStore != null) {
            create("upload") {
                storeFile = file(uploadStore)
                storePassword = uploadProp("storePassword")
                keyAlias = uploadProp("keyAlias")
                keyPassword = uploadProp("keyPassword")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        release {
            isDebuggable = false
            // R8: shrinks, optimizes and renames the code and drops unused resources.
            // Keep rules: every file in src/main/keepRules/.
            optimization {
                enable = true
            }
            signingConfig = signingConfigs.findByName("upload") ?: signingConfigs.getByName("debug")
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

kotlin {
    explicitApi()
}

dependencies {
    implementation(files("../libs/glyph-matrix-sdk-2.0.aar"))
    implementation(libs.n7.glyph.ui)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
    implementation(libs.material)
    implementation(libs.play.services.location)
}
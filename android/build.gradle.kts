plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "n7.snake"
    compileSdk = 37

    defaultConfig {
        applicationId = "n7.snake"
        minSdk = 35
        targetSdk = 37
        versionCode = 4
        versionName = "1.0.3"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        release {
            isDebuggable = false
            isShrinkResources = true
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
    implementation("com.github.i30mb1:n7-glyph-ui:0.1.0")
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
    implementation(libs.material)
    implementation(libs.play.services.location)
}
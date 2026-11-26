plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.toddlerdoodle"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.toddlerdoodle"
        minSdk = 26 // Android 8.0 and newer
        // Android only allows updates without the confirm screen for apps that target a
        // recent API level (34+ on Android 16, rising each year), so keep this current
        targetSdk = 35
        // Goes up with every GitHub build so each APK counts as an update
        versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionName = System.getenv("APP_VERSION") ?: "0.1.0"
    }

    // A fixed key kept in the repo, so every build is signed the same way and
    // installs over the previous one. (Debug use only, not for the Play Store.)
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    // Builds are "release" builds (not debuggable), signed with the same fixed key so
    // they still install over earlier versions. Debuggable apps look more suspicious
    // to Google Play Protect.
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    lint {
        // Don't fail the cloud build on lint warnings
        checkReleaseBuilds = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

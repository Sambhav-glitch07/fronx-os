plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.fronx.os"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.fronx.os"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    // Release signing is only configured when the CI (or you) provide a keystore.
    val ksPath: String? = System.getenv("FRONX_KEYSTORE_PATH")
    val hasKeystore = ksPath != null && File(ksPath).exists()
    if (hasKeystore) {
        signingConfigs {
            create("release") {
                storeFile = File(ksPath!!)
                storePassword = System.getenv("FRONX_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("FRONX_KEY_ALIAS")
                keyPassword = System.getenv("FRONX_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            if (hasKeystore) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}
// No third-party dependencies on purpose: native Android SDK only.

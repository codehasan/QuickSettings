plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "io.github.codehasan.quicksettings"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.codehasan.quicksettings"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.annotation.jvm)
}
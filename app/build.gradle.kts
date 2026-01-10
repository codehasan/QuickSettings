import com.android.build.gradle.internal.api.ApkVariantOutputImpl

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "io.github.codehasan.quicksettings"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.codehasan.quicksettings"
        minSdk = 24
        //noinspection ExpiredTargetSdkVersion
        targetSdk = 32
        versionCode = 3
        versionName = "1.2"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
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

    dependenciesInfo {
        includeInBundle = false
        includeInApk = false
    }

    applicationVariants.all {
        this.outputs
            .map { it as ApkVariantOutputImpl }
            .forEach { output ->
                output.outputFileName = " Quick_Settings_v${this.versionName}-${this.name}.apk"
            }
    }
}

dependencies {
    implementation(libs.annotation.jvm)
}
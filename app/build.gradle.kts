import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// CI sets GITHUB_RUN_NUMBER so every build installs as an update over the last one.
val buildNumber = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1

android {
    namespace = "dev.memehammad.clashtimers"
    compileSdk = 36

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        applicationId = "dev.memehammad.clashtimers"
        minSdk = 31
        targetSdk = 35
        versionCode = buildNumber
        versionName = "1.0.$buildNumber"
    }

    // Uses app/release.jks when present (local, or decoded from the SIGNING_KEY secret in CI);
    // otherwise falls back to the debug key so the build still produces an installable APK.
    val releaseKey = file("release.jks")
    signingConfigs {
        create("release") {
            storeFile = releaseKey
            storePassword = "clashtimers"
            keyAlias = "clashtimers"
            keyPassword = "clashtimers"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (releaseKey.exists()) signingConfigs.getByName("release")
            else signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        aidl = true
        buildConfig = true
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        // The Smartspacer SDK is built with a newer Kotlin.
        freeCompilerArgs.add("-Xskip-metadata-version-check")
    }
}

// The Smartspacer SDK asks for compile SDK 37 / AGP 9 and pulls a newer androidx.core; it works
// fine with ours, so keep our versions and skip that metadata check.
configurations.all {
    resolutionStrategy.force("androidx.core:core:1.15.0", "androidx.core:core-ktx:1.15.0")
}
tasks.matching { it.name.startsWith("check") && it.name.endsWith("AarMetadata") }.configureEach { enabled = false }

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    // Optional auto-import: a clipboard watcher running with shell rights via Shizuku.
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")
    implementation("org.lsposed.hiddenapibypass:hiddenapibypass:4.3")

    // Smartspacer (At a Glance) targets and complications.
    implementation("com.kieronquinn.smartspacer:sdk-plugin:1.1.2")

    // Wear OS sync (experimental, preview builds).
    implementation("com.google.android.gms:play-services-wearable:19.0.0")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}

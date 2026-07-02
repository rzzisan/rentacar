import java.io.ByteArrayOutputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// versionCode এখন থেকে git commit সংখ্যা থেকে automatic — versionCode = 3-এ ম্যানুয়ালি বাম্প করা
// ভুলে যাওয়া হতো (build.gradle.kts-এ বহুদিন versionCode = 1 স্থির ছিল, দেখুন
// saas_modiul_plan.md Phase 7)। commit সংখ্যা কখনো কমে না, তাই monotonic guarantee ফ্রি।
// git না পাওয়া গেলে (যেমন .git ছাড়া zip থেকে build) fallback 105-এ — যেন কোনোভাবেই
// আগের কোনো রিলিজের (সর্বোচ্চ manually-set versionCode 3 ছিল) চেয়ে ছোট versionCode তৈরি না হয়।
fun gitCommitCount(): Int {
    return try {
        val stdout = ByteArrayOutputStream()
        exec {
            commandLine("git", "-C", rootDir.toString(), "rev-list", "--count", "HEAD")
            standardOutput = stdout
        }
        stdout.toString().trim().toIntOrNull()?.takeIf { it > 0 } ?: 105
    } catch (e: Exception) {
        105
    }
}

android {
    namespace = "com.rzzisan.carrental"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.rzzisan.carrental"
        minSdk = 24
        targetSdk = 34
        versionCode = gitCommitCount()
        versionName = "1.1.1" // মানুষের পড়ার জন্য — semver স্টাইলে ম্যানুয়ালি বাম্প করবে, update-check এটার উপর নির্ভর করে না
        buildConfigField("String", "API_BASE_URL", "\"https://car.zisan.me/api/\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.8" }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.02.01"))
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // HTTP — Retrofit + Moshi
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.0")

    // Secure storage
    implementation("androidx.security:security-crypto:1.0.0")

    // GPS — FusedLocationProvider
    implementation("com.google.android.gms:play-services-location:21.1.0")

    // Camera (receipt photos)
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")

    // Image loading (profile picture, receipts)
    implementation("io.coil-kt:coil-compose:2.5.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}

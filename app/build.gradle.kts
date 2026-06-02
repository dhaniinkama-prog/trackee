plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.trackee"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.trackee"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
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
    kotlinOptions {
        jvmTarget = "11"
    }

    // =========================================================================
    // OPTIMASI: Mencegah kompresi model AI agar ML Kit bisa mendeteksi secara offline
    // =========================================================================
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        dex {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    // Dependensi Default Bawaan Projek
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.runtime.saved.instance.state)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // ==========================================================
    // DEPENDENSI: FITUR KAMERA & AI DETEKSI OBJEK (TRACKEE)
    // ==========================================================
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    // Tambahan ekstensi camera untuk optimasi analisa gambar ML Kit
    implementation("androidx.camera:camera-extensions:1.3.1")

    // ML Kit Object Detection (Bawaan Google, Ringan & Offline)
    implementation("com.google.mlkit:object-detection:17.0.2")

    // ==========================================================
    // DEPENDENSI: RETROFIT & COROUTINES (KONEKSI LARAVEL)
    // ==========================================================
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // ==========================================================
    // DEPENDENSI TAMBAHAN: GLIDE (LOAD FOTO PROFIL DARI LARAVEL)
    // ==========================================================
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
}
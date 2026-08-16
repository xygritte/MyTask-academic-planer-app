plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.kapt")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.mytask"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mytask"
        minSdk = 24
        targetSdk = 35

        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    // Compose
    val composeBom =
        platform("androidx.compose:compose-bom:2024.09.00")

    implementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    debugImplementation(
        "androidx.compose.ui:ui-tooling"
    )

    // Activity
    implementation(
        "androidx.activity:activity-compose:1.9.3"
    )

    // Lifecycle
    implementation(
        "androidx.lifecycle:lifecycle-runtime-compose:2.8.6"
    )

    implementation(
        "androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6"
    )

    // Navigation
    implementation(
        "androidx.navigation:navigation-compose:2.8.2"
    )

    // Coroutines
    implementation(
        "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1"
    )

    implementation(
        "org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1"
    )

    // Firebase
    implementation(
        platform("com.google.firebase:firebase-bom:34.16.0")
    )

    implementation(
        "com.google.firebase:firebase-auth"
    )

    implementation(
        "com.google.firebase:firebase-firestore"
    )

    // Google Sign-In via Credential Manager
    implementation(
        "androidx.credentials:credentials:1.3.0"
    )

    implementation(
        "androidx.credentials:credentials-play-services-auth:1.3.0"
    )

    implementation(
        "com.google.android.libraries.identity.googleid:googleid:1.1.1"
    )

    // Image picker / crop
    implementation(
        "com.github.yalantis:ucrop:2.2.11"
    )

    // Hilt
    implementation("com.google.dagger:hilt-android:2.57")
    kapt("com.google.dagger:hilt-android-compiler:2.57")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    kapt("androidx.hilt:hilt-compiler:1.2.0")

    // Room
    implementation(
        "androidx.room:room-runtime:2.7.2"
    )

    implementation(
        "androidx.room:room-ktx:2.7.2"
    )

    ksp(
        "androidx.room:room-compiler:2.7.2"
    )

    // DataStore
    implementation(
        "androidx.datastore:datastore-preferences:1.1.1"
    )

    // Coil
    implementation(
        "io.coil-kt:coil-compose:2.7.0"
    )

    // WorkManager
    implementation(
        "androidx.work:work-runtime-ktx:2.11.2"
    )

    // Testing
    testImplementation(
        "junit:junit:4.13.2"
    )
}

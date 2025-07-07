plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
}

import java.util.Properties // Ensure this import is present and correctly placed

// Helper function to get properties from local.properties
fun getLocalProperty(key: String, project: org.gradle.api.Project): String {
    val properties = Properties() // Use the imported Properties
    val localPropertiesFile = project.rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { properties.load(it) }
    }
    return properties.getProperty(key, "")
}

android {
    namespace = "com.mohdharish.geolocapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.mohdharish.geolocapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Make the API key available in BuildConfig
        buildConfigField("String", "MAPS_API_KEY", "\"${getLocalProperty("MAPS_API_KEY", project)}\"")
    }

    signingConfigs {
        create("release") {
            // Use the environment variable set in the CI workflow
            val storeFileFromEnv = System.getenv("SIGNING_STORE_FILE")
            if (storeFileFromEnv != null) {
                storeFile = file(storeFileFromEnv)
            }
            storePassword = System.getenv("SIGNING_STORE_PASSWORD")
            keyAlias = System.getenv("SIGNING_KEY_ALIAS")
            keyPassword = System.getenv("SIGNING_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true // Ensure BuildConfig is generated for API key access
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
        // Removed invalid kotlinCompilerPluginArgs property
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs += listOf(
            "-P", "plugin:androidx.compose.compiler.plugins.kotlin:featureFlag=IntrinsicRemember",
            "-P", "plugin:androidx.compose.compiler.plugins.kotlin:featureFlag=OptimizeNonSkippingGroups",
            "-P", "plugin:androidx.compose.compiler.plugins.kotlin:featureFlag=StrongSkipping"
        )
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Google Maps SDK
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    // Location Services (FusedLocationProviderClient, Geofencing)
    implementation("com.google.android.gms:play-services-location:21.0.1")
    // Room (Database)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1") // Added for Coroutine support
    kapt("androidx.room:room-compiler:2.6.1")
    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    // AndroidX Activity & Permissions
    implementation("androidx.activity:activity-ktx:1.9.0")
    // Notification (core-ktx already included)
    // MediaPlayer is built-in, ExoPlayer if you want advanced audio
    implementation("androidx.media:media:1.7.0")
    // (Optional) Firebase for friend-to-friend geofence
    // implementation("com.google.firebase:firebase-database-ktx:20.3.0")
    // implementation("com.google.firebase:firebase-auth-ktx:22.3.0")
    // Jetpack Compose
    implementation("androidx.compose.ui:ui:1.6.7")
    implementation("androidx.compose.material:material:1.6.7")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.7")
    implementation("androidx.activity:activity-compose:1.9.0")
    debugImplementation("androidx.compose.ui:ui-tooling:1.6.7")
    implementation("com.google.maps.android:maps-compose:4.1.1")
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.compose.runtime:runtime-livedata:1.6.7")
    // Places SDK for search
    implementation("com.google.android.libraries.places:places:3.4.0") // Added Google Places SDK
    // For await() on Play Services tasks
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    // Material Icons Extended for more icons (including MyLocation)
    implementation("androidx.compose.material:material-icons-extended:1.6.7")
    
    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.compose.material3:material3-window-size-class:1.2.1")
    // SplashScreen API
    implementation("androidx.core:core-splashscreen:1.0.1")
}
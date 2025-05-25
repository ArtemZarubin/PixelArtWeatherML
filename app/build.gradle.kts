// Module-level build file: WeatherML/app/build.gradle.kts
import java.util.Properties

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { fis ->
            load(fis)
        }
    }
}
val openWeatherApiKeyFromProperties: String =
    localProperties.getProperty("OPEN_WEATHER_API_KEY") ?: ""

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android.gradle)    // Apply Hilt plugin
    alias(libs.plugins.kotlin.serialization)  // Apply Kotlin Serialization plugin
    alias(libs.plugins.kotlin.kapt)           // Apply Kapt plugin
    alias(libs.plugins.ksp)   // Apply new KSP plugin
}

android {
    namespace = "com.artemzarubin.weatherml"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.artemzarubin.weatherml"
        minSdk = 26
        targetSdk = 35 // Match compileSdk
        versionCode = 2
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "OPEN_WEATHER_API_KEY", "\"$openWeatherApiKeyFromProperties\"")

        val geoapifyApiKey: String = localProperties.getProperty("GEOAPIFY_API_KEY") ?: ""
        buildConfigField("String", "GEOAPIFY_API_KEY", "\"$geoapifyApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21 // Recommended for modern Android
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21" // Match Java version
    }
    buildFeatures {
        compose = true
        buildConfig = true
        mlModelBinding = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/LICENSE.md"
            excludes += "/META-INF/LICENSE-notice.md"
        }
    }
}

dependencies {
    // Existing dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom)) // Import Compose BOM
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // New dependencies
    // Lifecycle (ViewModel, LiveData, Compose Integration)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.foundation)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Retrofit & OkHttp & Kotlinx Serialization Converter
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging.interceptor) // Useful for debugging network calls

    // Kotlinx Serialization (Runtime)
    implementation(libs.kotlinx.serialization.json)

    // Hilt for Dependency Injection
    implementation(libs.hilt.android)
    implementation(libs.protolite.well.known.types)
    kapt(libs.hilt.compiler) // Kapt for Hilt's annotation processor
    implementation(libs.hilt.navigation.compose) // For Hilt integration with Compose Navigation

    // TensorFlow Lite
    implementation(libs.tensorflow.lite)
    implementation(libs.tensorflow.lite.support)

    // Testing dependencies (existing)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom)) // Align UI tests with Compose BOM
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling) // For Compose tooling in debug builds
    debugImplementation(libs.androidx.ui.test.manifest) // For UI tests
    // New tests
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk.core)
    testImplementation(libs.mockk.android)
    testImplementation(libs.cash.turbine)


    // Location check
    implementation(libs.play.services.location) // For location

    // Room
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    // Compose
    implementation(libs.androidx.navigation.compose)

    // Icons
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)

    // Pager
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.foundation.layout)

    // Refreshing
    implementation(libs.androidx.compose.material)

    // Settings
    implementation(libs.androidx.datastore.preferences)
}

// Kapt configuration for Hilt (add to the end of the file if not present)
kapt {
    correctErrorTypes = true
}
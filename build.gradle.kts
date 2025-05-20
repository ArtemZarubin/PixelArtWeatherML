// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.kapt) apply false          // Kapt plugin
    alias(libs.plugins.hilt.android.gradle) apply false // Hilt Gradle plugin
    alias(libs.plugins.kotlin.serialization) apply false // Kotlinx Serialization plugin
    alias(libs.plugins.ksp) apply false          // Google KSP plugin
}
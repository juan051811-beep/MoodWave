
// Archivo Gradle principal del proyecto MoodWave

plugins {
    // Plugin de Android
    alias(libs.plugins.android.application) apply false

    // Plugin de Jetpack Compose
    alias(libs.plugins.kotlin.compose) apply false

    // Plugin para generar el código de Room
    alias(libs.plugins.ksp) apply false
}

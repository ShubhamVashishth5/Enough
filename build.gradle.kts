plugins {
    alias(libs.plugins.android.application) apply false
    // AGP's built-in Kotlin support is used; this only pins the Kotlin Gradle plugin version.
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.room) apply false
}

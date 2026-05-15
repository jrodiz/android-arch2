plugins {
    alias(libs.plugins.arch.android.library)
    alias(libs.plugins.arch.android.hilt)
}

android {
    namespace = "com.rodiz.arch2.core.navigation"
}

dependencies {
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.navigation3.runtime)
}

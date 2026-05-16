plugins {
    alias(libs.plugins.tinpet.android.library)
    alias(libs.plugins.tinpet.android.hilt)
}

android {
    namespace = "com.rodiz.arch2.core.navigation"
}

dependencies {
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.navigation3.runtime)
}

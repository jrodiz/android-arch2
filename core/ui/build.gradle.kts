plugins {
    alias(libs.plugins.tinpet.android.library)
    alias(libs.plugins.tinpet.android.library.compose)
}

android {
    namespace = "com.rodiz.arch2.core.ui"
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(libs.androidx.compose.material.icons)
}

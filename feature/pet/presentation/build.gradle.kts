plugins {
    alias(libs.plugins.tinpet.android.feature)
}

android {
    namespace = "com.rodiz.arch2.feature.pet.presentation"
}

dependencies {
    implementation(project(":feature:pet:nav"))
    implementation(project(":feature:pet:domain"))
    implementation(project(":core:navigation"))
    implementation(project(":core:designsystem"))

    implementation(libs.androidx.compose.material.icons)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.datetime)
}

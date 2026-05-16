plugins {
    alias(libs.plugins.tinpet.android.feature)
}

android {
    namespace = "com.rodiz.arch2.feature.profile.presentation"
}

dependencies {
    implementation(project(":feature:profile:nav"))
    implementation(project(":feature:login:nav"))
    implementation(project(":core:navigation"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:session:domain"))

    implementation(libs.androidx.compose.material.icons)
}

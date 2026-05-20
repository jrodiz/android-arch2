plugins {
    alias(libs.plugins.tinpet.android.feature)
}

android {
    namespace = "com.rodiz.arch2.feature.notifications.presentation"
}

dependencies {
    implementation(project(":feature:notifications:nav"))
    implementation(project(":feature:deck:nav"))         // post-onboarding lands on DeckHome
    implementation(project(":core:firebase"))
    implementation(project(":core:navigation"))
    implementation(project(":core:designsystem"))

    implementation(libs.androidx.compose.material.icons)
    implementation(libs.kotlinx.coroutines.android)
}

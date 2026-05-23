plugins {
    alias(libs.plugins.tinpet.android.feature)
}

android {
    namespace = "com.rodiz.arch2.feature.deck.presentation"
}

dependencies {
    implementation(project(":feature:deck:nav"))
    implementation(project(":feature:deck:domain"))
    implementation(project(":feature:pet:nav"))
    implementation(project(":feature:pet:domain"))
    implementation(project(":feature:settings:nav"))
    implementation(project(":core:filters:domain"))
    implementation(project(":core:navigation"))
    implementation(project(":core:designsystem"))

    implementation(libs.androidx.compose.material.icons)
    implementation(libs.coil.compose)
}

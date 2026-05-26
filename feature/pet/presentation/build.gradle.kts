plugins {
    alias(libs.plugins.tinpet.android.feature)
}

android {
    namespace = "com.rodiz.arch2.feature.pet.presentation"
}

dependencies {
    implementation(project(":feature:pet:nav"))
    implementation(project(":feature:pet:domain"))
    implementation(project(":core:featuredpets:domain"))
    implementation(project(":core:navigation"))
    implementation(project(":core:designsystem"))

    implementation(libs.androidx.compose.material.icons)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.datetime)

    // JUnit5 + MockK + Turbine + coroutines-test come from tinpet.android.test
    // (applied transitively by the feature convention plugin).
    testImplementation(project(":core:testing"))
    testImplementation(libs.kotlinx.datetime)
}

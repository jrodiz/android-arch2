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
    implementation(project(":feature:match:nav")) // MatchCelebration target post-like
    implementation(project(":core:filters:domain"))
    implementation(project(":core:navigation"))
    implementation(project(":core:designsystem"))

    implementation(libs.androidx.compose.material.icons)
    implementation(libs.coil.compose)

    // JUnit5 + MockK + Turbine + coroutines-test come from tinpet.android.test
    // (applied transitively by the feature convention plugin).
    testImplementation(project(":core:testing"))
    testImplementation(libs.kotlinx.datetime)
}

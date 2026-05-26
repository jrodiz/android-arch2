plugins {
    alias(libs.plugins.tinpet.android.feature)
}

android {
    namespace = "com.rodiz.arch2.feature.notifications.presentation"
}

dependencies {
    implementation(project(":feature:notifications:nav"))
    implementation(project(":feature:deck:nav")) // post-onboarding lands on DeckHome
    implementation(project(":core:common"))
    implementation(project(":core:firebase"))
    implementation(project(":core:navigation"))
    implementation(project(":core:designsystem"))

    implementation(libs.androidx.compose.material.icons)
    implementation(libs.kotlinx.coroutines.android)

    // PermissionsOnboarding screen pulls the user's last-known location after
    // they grant ACCESS_COARSE_LOCATION at onboarding.
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.play.services)

    // PrimaryButton + ErrorBanner used by the permissions screen.
    implementation(project(":core:ui"))

    // JUnit5 + MockK + Turbine + coroutines-test come from tinpet.android.test
    // (applied transitively by the feature convention plugin).
    testImplementation(project(":core:testing"))
}

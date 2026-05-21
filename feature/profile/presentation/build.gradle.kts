plugins {
    alias(libs.plugins.tinpet.android.feature)
}

android {
    namespace = "com.rodiz.arch2.feature.profile.presentation"
}

dependencies {
    implementation(project(":feature:profile:nav"))
    implementation(project(":feature:profile:domain"))
    implementation(project(":feature:login:nav"))
    implementation(project(":feature:pet:nav"))
    implementation(project(":feature:settings:nav"))
    // The Profile tab is the natural place to surface "your account is being deleted"
    // because it's where users come to manage their account. settings:domain is JVM-only
    // so there's no Android baggage in the transitive dep.
    implementation(project(":feature:settings:domain"))
    implementation(project(":core:navigation"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:session:domain"))

    implementation(libs.androidx.compose.material.icons)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.datetime)
}

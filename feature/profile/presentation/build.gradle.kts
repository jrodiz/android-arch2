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
    // The Profile hero shows the owner's pet count + a horizontal rail of pet chips,
    // which means we read `Pet` + `ObserveMyPetsUseCase` from pet:domain. Same precedent
    // as settings:domain above: pet:domain is JVM-only so nothing Android leaks in.
    implementation(project(":feature:pet:domain"))
    implementation(project(":core:navigation"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:session:domain"))

    implementation(libs.androidx.compose.material.icons)
    implementation(libs.coil.compose)
    implementation(libs.kotlinx.datetime)
}

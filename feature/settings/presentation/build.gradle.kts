plugins {
    alias(libs.plugins.tinpet.android.feature)
}

android {
    namespace = "com.rodiz.arch2.feature.settings.presentation"
}

dependencies {
    implementation(project(":feature:settings:nav"))
    implementation(project(":feature:settings:domain"))
    implementation(project(":feature:profile:domain"))
    implementation(project(":feature:notifications:nav"))
    implementation(project(":feature:login:nav"))
    implementation(project(":core:filters:domain"))

    implementation(libs.coil.compose)
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(project(":core:common"))
    implementation(project(":core:navigation"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:session:domain"))

    implementation(libs.androidx.compose.material.icons)
    implementation(libs.kotlinx.datetime)
}

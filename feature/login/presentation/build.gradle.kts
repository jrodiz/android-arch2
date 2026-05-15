plugins {
    alias(libs.plugins.arch.android.feature)
    alias(libs.plugins.arch.kotlin.serialization)
}

android {
    namespace = "com.rodiz.arch2.feature.login.presentation"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    implementation(project(":feature:login:nav"))
    implementation(project(":feature:login:domain"))
    implementation(project(":feature:home:nav"))
    implementation(project(":core:navigation"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":core:common"))
    implementation(project(":core:session:domain"))
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.compose.material.icons)

    testImplementation(project(":core:testing"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.jupiter.engine)

    androidTestImplementation(libs.androidx.test.ext)
    androidTestImplementation(libs.androidx.compose.ui.test)
    androidTestImplementation(libs.junit4)
}

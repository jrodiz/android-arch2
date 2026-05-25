plugins {
    alias(libs.plugins.tinpet.android.feature)
    alias(libs.plugins.tinpet.kotlin.serialization)
}

android {
    namespace = "com.rodiz.arch2.feature.login.presentation"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Read from gradle.properties (or -P override). Blank when Firebase setup
        // is incomplete — Google sign-in fails fast at runtime.
        val webClientId = providers.gradleProperty("firebase.web.client.id").getOrElse("")
        buildConfigField("String", "GCP_WEB_CLIENT_ID", "\"$webClientId\"")
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project(":feature:login:nav"))
    implementation(project(":feature:login:domain"))
    implementation(project(":feature:deck:nav"))
    implementation(project(":feature:notifications:nav"))
    implementation(project(":feature:settings:nav"))
    implementation(project(":core:navigation"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":core:common"))
    implementation(project(":core:session:domain"))
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.google.id)
    implementation(libs.coil.compose)

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

plugins {
    alias(libs.plugins.arch.android.library)
    alias(libs.plugins.arch.android.hilt)
}

android {
    namespace = "com.rodiz.arch2.core.firebase"
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
}

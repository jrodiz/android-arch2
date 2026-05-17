plugins {
    alias(libs.plugins.tinpet.android.library)
    alias(libs.plugins.tinpet.android.hilt)
}

android {
    namespace = "com.rodiz.arch2.feature.login.data"
}

dependencies {
    implementation(project(":feature:login:domain"))
    implementation(project(":core:common"))
    implementation(project(":core:session:domain"))
    implementation(project(":core:session:data"))
    implementation(project(":core:firebase"))
    implementation(libs.androidx.security.crypto)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.storage.ktx)
}

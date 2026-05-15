plugins {
    alias(libs.plugins.arch.android.library)
    alias(libs.plugins.arch.android.hilt)
}

android {
    namespace = "com.rodiz.arch2.feature.login.data"
}

dependencies {
    implementation(project(":feature:login:domain"))
    implementation(project(":core:common"))
    implementation(project(":core:session:domain"))
    implementation(project(":core:session:data"))
    implementation(libs.androidx.security.crypto)
    implementation(libs.kotlinx.coroutines.android)
}

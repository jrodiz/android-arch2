plugins {
    alias(libs.plugins.tinpet.android.library)
    alias(libs.plugins.tinpet.android.hilt)
}

android {
    namespace = "com.rodiz.arch2.feature.home.data"
}

dependencies {
    implementation(project(":feature:home:domain"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.datetime)
}

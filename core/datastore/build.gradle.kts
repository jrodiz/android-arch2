plugins {
    alias(libs.plugins.arch.android.library)
    alias(libs.plugins.arch.android.hilt)
}

android {
    namespace = "com.rodiz.arch2.core.datastore"
}

dependencies {
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
}

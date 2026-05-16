plugins {
    alias(libs.plugins.tinpet.android.library)
    alias(libs.plugins.tinpet.android.hilt)
}

android {
    namespace = "com.rodiz.arch2.core.datastore"
}

dependencies {
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
}

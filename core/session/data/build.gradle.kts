plugins {
    alias(libs.plugins.arch.android.library)
    alias(libs.plugins.arch.android.hilt)
}

android {
    namespace = "com.rodiz.arch2.core.session.data"
}

dependencies {
    api(project(":core:session:domain"))
    implementation(project(":core:datastore"))
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
}

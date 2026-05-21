plugins {
    alias(libs.plugins.tinpet.android.library)
    alias(libs.plugins.tinpet.android.hilt)
}

android {
    namespace = "com.rodiz.arch2.core.filters.data"
}

dependencies {
    implementation(project(":core:filters:domain"))
    implementation(project(":core:datastore"))
    implementation(project(":feature:pet:domain"))

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
}

plugins {
    alias(libs.plugins.tinpet.android.library)
    alias(libs.plugins.tinpet.android.hilt)
    alias(libs.plugins.tinpet.kotlin.serialization)
    alias(libs.plugins.tinpet.android.test)
}

android {
    namespace = "com.rodiz.arch2.core.featuredpets.data"
}

dependencies {
    implementation(project(":core:featuredpets:domain"))
    implementation(project(":core:datastore"))

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    // JUnit5 + MockK + Turbine + coroutines-test come from tinpet.android.test.
    testImplementation(project(":core:testing"))
}

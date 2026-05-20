plugins {
    alias(libs.plugins.tinpet.android.library)
    alias(libs.plugins.tinpet.android.hilt)
}

android {
    namespace = "com.rodiz.arch2.feature.pet.data"
}

dependencies {
    implementation(project(":feature:pet:domain"))
    implementation(project(":core:firebase"))
    implementation(project(":core:common"))
    implementation(project(":core:session:domain"))

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.datetime)
}

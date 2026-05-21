plugins {
    alias(libs.plugins.tinpet.android.library)
    alias(libs.plugins.tinpet.android.hilt)
}

android {
    namespace = "com.rodiz.arch2.core.ownerlookup.data"
}

dependencies {
    implementation(project(":core:ownerlookup:domain"))
    implementation(project(":core:firebase"))
    implementation(project(":core:common"))

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
}

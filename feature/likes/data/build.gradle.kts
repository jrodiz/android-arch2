plugins {
    alias(libs.plugins.tinpet.android.library)
    alias(libs.plugins.tinpet.android.hilt)
}

android {
    namespace = "com.rodiz.arch2.feature.likes.data"
}

dependencies {
    implementation(project(":feature:likes:domain"))
    implementation(project(":feature:pet:domain"))
    implementation(project(":feature:deck:domain")) // DeckRepository for likeBack delegation
    implementation(project(":core:firebase"))
    implementation(project(":core:common"))
    implementation(project(":core:session:domain"))

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.datetime)
}

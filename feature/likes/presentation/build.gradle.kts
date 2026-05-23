plugins {
    alias(libs.plugins.tinpet.android.feature)
}

android {
    namespace = "com.rodiz.arch2.feature.likes.presentation"
}

dependencies {
    implementation(project(":feature:likes:nav"))
    implementation(project(":feature:likes:domain"))
    implementation(project(":feature:deck:nav"))
    implementation(project(":feature:deck:domain"))     // SwipeResult for like-back outcome
    implementation(project(":feature:pet:domain"))
    implementation(project(":core:navigation"))
    implementation(project(":core:designsystem"))

    implementation(libs.androidx.compose.material.icons)
    implementation(libs.coil.compose)
    // Used by the LikeCard's relative-time helper to read `IncomingLike.likedAt` (a
    // `kotlinx.datetime.Instant` exposed by `:feature:likes:domain`).
    implementation(libs.kotlinx.datetime)
}

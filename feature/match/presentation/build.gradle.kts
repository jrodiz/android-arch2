plugins {
    alias(libs.plugins.tinpet.android.feature)
}

android {
    namespace = "com.rodiz.arch2.feature.match.presentation"
}

dependencies {
    implementation(project(":feature:match:nav"))
    implementation(project(":feature:match:domain"))
    implementation(project(":feature:deck:nav"))
    implementation(project(":feature:chat:nav"))
    implementation(project(":core:navigation"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:session:domain"))

    implementation(libs.androidx.compose.material.icons)
    implementation(libs.kotlinx.datetime)
}

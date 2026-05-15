plugins {
    alias(libs.plugins.arch.android.feature)
}

android {
    namespace = "com.rodiz.arch2.feature.home.presentation"
}

dependencies {
    implementation(project(":feature:home:nav"))
    implementation(project(":feature:login:nav"))
    implementation(project(":core:navigation"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:session:domain"))
}

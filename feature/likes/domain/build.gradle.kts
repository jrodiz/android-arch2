plugins {
    alias(libs.plugins.tinpet.jvm.library)
}

dependencies {
    implementation(project(":feature:pet:domain"))
    implementation(project(":feature:deck:domain"))     // SwipeResult for likeBack
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.javax.inject)
}

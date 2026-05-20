plugins {
    alias(libs.plugins.tinpet.jvm.library)
}

dependencies {
    implementation(project(":feature:match:domain"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.javax.inject)
}

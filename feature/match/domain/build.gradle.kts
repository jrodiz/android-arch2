plugins {
    alias(libs.plugins.tinpet.jvm.library)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.javax.inject)
}

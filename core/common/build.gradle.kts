plugins {
    alias(libs.plugins.arch.jvm.library)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.javax.inject)
}

plugins {
    alias(libs.plugins.arch.android.library)
}

android {
    namespace = "com.rodiz.arch2.core.testing"
}

dependencies {
    api(libs.junit.jupiter)
    api(libs.mockk)
    api(libs.turbine)
    api(libs.kotlinx.coroutines.test)
}

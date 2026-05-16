plugins {
    alias(libs.plugins.tinpet.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.tinpet.android.hilt)
    alias(libs.plugins.tinpet.kotlin.serialization)
    alias(libs.plugins.tinpet.android.firebase)
}

android {
    namespace = "com.rodiz.arch2"

    defaultConfig {
        applicationId = "com.rodiz.arch2"
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        compose = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":core:session:domain"))
    implementation(project(":core:session:data"))
    implementation(project(":core:datastore"))
    implementation(project(":core:firebase"))

    implementation(project(":feature:login:nav"))
    implementation(project(":feature:login:domain"))
    implementation(project(":feature:login:data"))
    implementation(project(":feature:login:presentation"))

    implementation(project(":feature:home:nav"))
    implementation(project(":feature:home:domain"))
    implementation(project(":feature:home:data"))
    implementation(project(":feature:home:presentation"))

    implementation(project(":feature:discover:nav"))
    implementation(project(":feature:discover:presentation"))

    implementation(project(":feature:profile:nav"))
    implementation(project(":feature:profile:presentation"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

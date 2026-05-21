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
    implementation(project(":core:filters:data"))

    implementation(project(":feature:login:nav"))
    implementation(project(":feature:login:domain"))
    implementation(project(":feature:login:data"))
    implementation(project(":feature:login:presentation"))

    implementation(project(":feature:profile:nav"))
    implementation(project(":feature:profile:domain"))
    implementation(project(":feature:profile:data"))
    implementation(project(":feature:profile:presentation"))

    implementation(project(":feature:pet:nav"))
    implementation(project(":feature:pet:data"))
    implementation(project(":feature:pet:presentation"))

    implementation(project(":feature:deck:nav"))
    implementation(project(":feature:deck:data"))
    implementation(project(":feature:deck:presentation"))

    implementation(project(":feature:likes:nav"))
    implementation(project(":feature:likes:data"))
    implementation(project(":feature:likes:presentation"))

    implementation(project(":feature:match:nav"))
    implementation(project(":feature:match:data"))
    implementation(project(":feature:match:presentation"))

    implementation(project(":feature:chat:nav"))
    implementation(project(":feature:chat:data"))
    implementation(project(":feature:chat:presentation"))

    implementation(project(":feature:settings:nav"))
    implementation(project(":feature:settings:data"))
    implementation(project(":feature:settings:presentation"))

    implementation(project(":feature:notifications:nav"))
    implementation(project(":feature:notifications:presentation"))

    // MainActivity references NotificationRationale to wire the tinpet://notify deep link.
    // (Already covered by :feature:notifications:nav above; no extra dep needed.)

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

plugins {
    `kotlin-dsl`
}

group = "com.rodiz.tinpet.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.compiler.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    implementation(libs.google.services.plugin)
    implementation(libs.firebase.crashlytics.plugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "tinpet.android.application"
            implementationClass = "com.rodiz.arch2.convention.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "tinpet.android.library"
            implementationClass = "com.rodiz.arch2.convention.AndroidLibraryConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = "tinpet.android.library.compose"
            implementationClass = "com.rodiz.arch2.convention.AndroidLibraryComposeConventionPlugin"
        }
        register("androidFeature") {
            id = "tinpet.android.feature"
            implementationClass = "com.rodiz.arch2.convention.AndroidFeatureConventionPlugin"
        }
        register("androidHilt") {
            id = "tinpet.android.hilt"
            implementationClass = "com.rodiz.arch2.convention.AndroidHiltConventionPlugin"
        }
        register("androidFirebase") {
            id = "tinpet.android.firebase"
            implementationClass = "com.rodiz.arch2.convention.AndroidFirebaseConventionPlugin"
        }
        register("jvmLibrary") {
            id = "tinpet.jvm.library"
            implementationClass = "com.rodiz.arch2.convention.JvmLibraryConventionPlugin"
        }
        register("kotlinSerialization") {
            id = "tinpet.kotlin.serialization"
            implementationClass = "com.rodiz.arch2.convention.KotlinSerializationConventionPlugin"
        }
        register("androidTest") {
            id = "tinpet.android.test"
            implementationClass = "com.rodiz.arch2.convention.AndroidTestConventionPlugin"
        }
        register("composeMetrics") {
            id = "tinpet.compose.metrics"
            implementationClass = "com.rodiz.arch2.convention.ComposeMetricsConventionPlugin"
        }
    }
}

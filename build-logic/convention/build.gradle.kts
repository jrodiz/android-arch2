plugins {
    `kotlin-dsl`
}

group = "com.rodiz.arch2.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "arch.android.application"
            implementationClass = "com.rodiz.arch2.convention.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "arch.android.library"
            implementationClass = "com.rodiz.arch2.convention.AndroidLibraryConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = "arch.android.library.compose"
            implementationClass = "com.rodiz.arch2.convention.AndroidLibraryComposeConventionPlugin"
        }
        register("androidFeature") {
            id = "arch.android.feature"
            implementationClass = "com.rodiz.arch2.convention.AndroidFeatureConventionPlugin"
        }
        register("androidHilt") {
            id = "arch.android.hilt"
            implementationClass = "com.rodiz.arch2.convention.AndroidHiltConventionPlugin"
        }
        register("jvmLibrary") {
            id = "arch.jvm.library"
            implementationClass = "com.rodiz.arch2.convention.JvmLibraryConventionPlugin"
        }
        register("kotlinSerialization") {
            id = "arch.kotlin.serialization"
            implementationClass = "com.rodiz.arch2.convention.KotlinSerializationConventionPlugin"
        }
        register("androidTest") {
            id = "arch.android.test"
            implementationClass = "com.rodiz.arch2.convention.AndroidTestConventionPlugin"
        }
    }
}

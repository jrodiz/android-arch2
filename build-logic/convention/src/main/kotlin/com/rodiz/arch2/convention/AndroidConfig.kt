package com.rodiz.arch2.convention

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

internal fun Project.configureAndroid(extension: CommonExtension<*, *, *, *, *, *>) {
    val catalog = libs
    extension.apply {
        compileSdk = catalog.intVersion("compileSdk")

        defaultConfig {
            minSdk = catalog.intVersion("minSdk")
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
            isCoreLibraryDesugaringEnabled = false
        }

        packaging {
            resources {
                excludes += "/META-INF/{AL2.0,LGPL2.1}"
                excludes += "/META-INF/LICENSE*"
                excludes += "/META-INF/NOTICE*"
            }
        }
    }

    tasks.withType(KotlinJvmCompile::class.java).configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            freeCompilerArgs.addAll(
                "-Xjvm-default=all",
                "-opt-in=kotlin.RequiresOptIn",
            )
        }
    }
}

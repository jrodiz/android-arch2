package com.rodiz.arch2.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

/**
 * Shared unit-test dependencies. Adds JUnit5, MockK, Turbine, coroutines-test.
 */
class AndroidTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

        // Use JUnit5 for unit tests
        target.tasks.withType(org.gradle.api.tasks.testing.Test::class.java).configureEach {
            useJUnitPlatform()
        }

        dependencies {
            add("testImplementation", libs.findLibrary("junit-jupiter").get())
            add("testRuntimeOnly", libs.findLibrary("junit-jupiter-engine").get())
            add("testImplementation", libs.findLibrary("mockk").get())
            add("testImplementation", libs.findLibrary("turbine").get())
            add("testImplementation", libs.findLibrary("kotlinx-coroutines-test").get())
        }
    }
}

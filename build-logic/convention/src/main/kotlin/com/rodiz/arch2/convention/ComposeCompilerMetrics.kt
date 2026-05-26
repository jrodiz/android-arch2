package com.rodiz.arch2.convention

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

/**
 * Opt-in Compose compiler metrics + stability reports.
 *
 * Pass `-Pcompose.metrics=true` on any Gradle command to enable. Reports land
 * under `<module>/build/compose_compiler/` per module:
 *
 *   *-composables.txt   restartable / skippable / inline classifications
 *   *-composables.csv   same data, machine-readable
 *   *-classes.txt       stability classification of every class a composable touches
 *   *-module.json       per-module summary (counts of unstable params, etc.)
 *
 * Off by default — generating the reports adds ~10% to compile time and we
 * don't want every CI run paying that cost. Trigger ad-hoc when investigating
 * recomposition issues:
 *
 *   ./gradlew :feature:deck:presentation:assembleDebug -Pcompose.metrics=true
 *
 * Then grep for `unstable` under
 * `feature/deck/presentation/build/compose_compiler/` to find composables whose
 * params Compose can't smart-skip.
 */
internal fun configureComposeCompilerMetrics(project: Project) {
    val enabled = project.providers.gradleProperty("compose.metrics")
        .map { it.equals("true", ignoreCase = true) }
        .orElse(false)

    project.pluginManager.withPlugin("org.jetbrains.kotlin.plugin.compose") {
        project.extensions.configure<ComposeCompilerGradlePluginExtension> {
            val outDir = project.layout.buildDirectory.dir("compose_compiler")
            metricsDestination.set(
                enabled.flatMap { on -> if (on) outDir else project.provider { null } },
            )
            reportsDestination.set(
                enabled.flatMap { on -> if (on) outDir else project.provider { null } },
            )
        }
    }
}

package com.rodiz.arch2.convention

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Tiny convention plugin that wires Compose compiler metrics + stability reports.
 * Apply this on any module that has the Compose compiler plugin applied — most
 * library modules already get it transitively via [AndroidLibraryComposeConventionPlugin],
 * so this is primarily for `:app` and any one-off Application/Library targets.
 *
 * Reports are gated by `-Pcompose.metrics=true`; the apply call is otherwise a no-op.
 */
class ComposeMetricsConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        configureComposeCompilerMetrics(target)
    }
}

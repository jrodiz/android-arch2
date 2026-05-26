plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ktlint) apply false
}

// ktlint applied to every subproject. Pinned ktlint version keeps local +
// CI runs deterministic. Rule overrides (e.g. disabling function-naming
// for Compose composables) live in /.editorconfig so IDE + ktlint stay
// in sync. Generated sources and build dirs are excluded — ktlint
// would otherwise fight Hilt-generated code.
subprojects {
    apply(plugin = rootProject.libs.plugins.ktlint.get().pluginId)

    extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set("1.3.1")
        android.set(true)
        ignoreFailures.set(false)
        verbose.set(false)
        outputToConsole.set(true)
        filter {
            exclude { it.file.path.contains("/build/") }
            exclude { it.file.path.contains("/generated/") }
        }
    }
}

plugins {
    alias(libs.plugins.tinpet.jvm.library)
}

dependencies {
    // api so FilterPrefs.intents (Intent) and .species (Species) — which live in
    // :feature:pet:domain — are visible to anyone consuming :core:filters:domain
    // without forcing them to also declare a direct pet:domain dep.
    api(project(":feature:pet:domain"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.javax.inject)
}

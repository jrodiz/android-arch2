plugins {
    alias(libs.plugins.tinpet.jvm.library)
}

dependencies {
    // api: MatchSummary.other is OwnerDisplay, exposed to consumers.
    api(project(":core:ownerlookup:domain"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.javax.inject)
}

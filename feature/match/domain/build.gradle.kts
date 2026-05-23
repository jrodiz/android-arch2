plugins {
    alias(libs.plugins.tinpet.jvm.library)
}

dependencies {
    // api: MatchSummary.other is OwnerDisplay, MatchSummary.otherPet is PetDisplay,
    // both exposed to consumers (Inbox row, Chat header).
    api(project(":core:ownerlookup:domain"))
    api(project(":core:petlookup:domain"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.javax.inject)
}

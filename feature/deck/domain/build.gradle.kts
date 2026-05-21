plugins {
    alias(libs.plugins.tinpet.jvm.library)
}

dependencies {
    implementation(project(":feature:pet:domain"))
    implementation(project(":core:filters:domain"))
    // api: DeckCard.owner is OwnerDisplay, exposed to consumers (deck:presentation).
    api(project(":core:ownerlookup:domain"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.datetime)
    implementation(libs.javax.inject)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
}

tasks.test {
    useJUnitPlatform()
}

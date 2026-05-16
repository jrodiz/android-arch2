pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "TinPet"

include(":app")

include(":core:common")
include(":core:navigation")
include(":core:designsystem")
include(":core:ui")
include(":core:datastore")
include(":core:session:domain")
include(":core:session:data")
include(":core:firebase")
include(":core:testing")

include(":feature:login:nav")
include(":feature:login:domain")
include(":feature:login:data")
include(":feature:login:presentation")

include(":feature:home:nav")
include(":feature:home:presentation")

include(":feature:discover:nav")
include(":feature:discover:presentation")

include(":feature:profile:nav")
include(":feature:profile:presentation")

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
include(":core:filters:domain")
include(":core:filters:data")
include(":core:testing")

include(":feature:login:nav")
include(":feature:login:domain")
include(":feature:login:data")
include(":feature:login:presentation")

include(":feature:profile:nav")
include(":feature:profile:domain")
include(":feature:profile:data")
include(":feature:profile:presentation")

include(":feature:pet:nav")
include(":feature:pet:domain")
include(":feature:pet:data")
include(":feature:pet:presentation")

include(":feature:deck:nav")
include(":feature:deck:domain")
include(":feature:deck:data")
include(":feature:deck:presentation")

include(":feature:likes:nav")
include(":feature:likes:domain")
include(":feature:likes:data")
include(":feature:likes:presentation")

include(":feature:match:nav")
include(":feature:match:domain")
include(":feature:match:data")
include(":feature:match:presentation")

include(":feature:chat:nav")
include(":feature:chat:domain")
include(":feature:chat:data")
include(":feature:chat:presentation")

include(":feature:settings:nav")
include(":feature:settings:domain")
include(":feature:settings:data")
include(":feature:settings:presentation")

include(":feature:notifications:nav")
include(":feature:notifications:presentation")

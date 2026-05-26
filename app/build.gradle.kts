import java.util.Properties

plugins {
    alias(libs.plugins.tinpet.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.tinpet.android.hilt)
    alias(libs.plugins.tinpet.kotlin.serialization)
    alias(libs.plugins.tinpet.android.firebase)
    alias(libs.plugins.tinpet.compose.metrics)
}

// Pulls release-signing credentials from (in priority order):
//   1. `signing.*` keys in /local.properties (gitignored; per-machine).
//   2. RELEASE_KEYSTORE_* environment variables (for CI when we set up secrets).
//   3. None — `assembleRelease` falls back to the debug signing config so the
//      output is still installable, just not Play-store-shippable. This is
//      intentional: it keeps CI green without secrets, and a local
//      `:app:installRelease` works the day you hook up a device.
val releaseSigning: SigningCredentials? = run {
    val local = Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) file.inputStream().use { load(it) }
    }
    fun pick(key: String, env: String) =
        local.getProperty(key) ?: System.getenv(env)

    val storeFilePath = pick("signing.storeFile", "RELEASE_KEYSTORE_PATH") ?: return@run null
    val storePassword = pick("signing.storePassword", "RELEASE_KEYSTORE_PASSWORD") ?: return@run null
    val keyAlias = pick("signing.keyAlias", "RELEASE_KEY_ALIAS") ?: return@run null
    val keyPassword = pick("signing.keyPassword", "RELEASE_KEY_PASSWORD") ?: return@run null
    SigningCredentials(file(storeFilePath), storePassword, keyAlias, keyPassword)
}

data class SigningCredentials(
    val storeFile: File,
    val storePassword: String,
    val keyAlias: String,
    val keyPassword: String,
)

android {
    namespace = "com.rodiz.arch2"

    defaultConfig {
        applicationId = "com.rodiz.arch2"
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        compose = true
    }

    signingConfigs {
        // Only register `release` when real credentials are present — if we
        // declared the block unconditionally and the storeFile was missing,
        // every `assembleRelease` would fail with a confusing
        // "Keystore file not set" error.
        releaseSigning?.let { creds ->
            create("release") {
                storeFile = creds.storeFile
                storePassword = creds.storePassword
                keyAlias = creds.keyAlias
                keyPassword = creds.keyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // Use the real release signing config if credentials are wired in,
            // otherwise the debug config so the APK is still installable on
            // a connected emulator / device for smoke-testing.
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    lint {
        // AGP 8.10's lintVital can't resolve FragmentActivity / FirebaseMessagingService
        // through the release classpath, producing false-positive "must extend Activity / Service"
        // errors for classes that already extend them. The rule has not caught a real
        // misconfiguration on this project; disable to keep `assembleRelease` green.
        disable += "Instantiatable"
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":core:session:domain"))
    implementation(project(":core:session:data"))
    implementation(project(":core:datastore"))
    implementation(project(":core:firebase"))
    implementation(project(":core:filters:data"))
    implementation(project(":core:ownerlookup:data"))
    implementation(project(":core:petlookup:data"))

    implementation(project(":feature:login:nav"))
    implementation(project(":feature:login:domain"))
    implementation(project(":feature:login:data"))
    implementation(project(":feature:login:presentation"))

    implementation(project(":feature:profile:nav"))
    implementation(project(":feature:profile:domain"))
    implementation(project(":feature:profile:data"))
    implementation(project(":feature:profile:presentation"))

    implementation(project(":feature:pet:nav"))
    implementation(project(":feature:pet:data"))
    implementation(project(":feature:pet:presentation"))

    implementation(project(":feature:deck:nav"))
    implementation(project(":feature:deck:data"))
    implementation(project(":feature:deck:presentation"))

    implementation(project(":feature:likes:nav"))
    implementation(project(":feature:likes:domain"))
    implementation(project(":feature:likes:data"))
    implementation(project(":feature:likes:presentation"))

    implementation(project(":feature:match:nav"))
    implementation(project(":feature:match:domain"))
    implementation(project(":feature:match:data"))
    implementation(project(":feature:match:presentation"))

    implementation(project(":feature:chat:nav"))
    implementation(project(":feature:chat:data"))
    implementation(project(":feature:chat:presentation"))

    implementation(project(":feature:settings:nav"))
    implementation(project(":feature:settings:data"))
    implementation(project(":feature:settings:presentation"))

    implementation(project(":feature:notifications:nav"))
    implementation(project(":feature:notifications:presentation"))

    // MainActivity references NotificationRationale to wire the tinpet://notify deep link.
    // (Already covered by :feature:notifications:nav above; no extra dep needed.)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

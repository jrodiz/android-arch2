# R8 / ProGuard rules for the app module.
#
# We intentionally keep this short: the Firebase, Hilt, Compose, Coil, Datastore
# and Coroutines libraries all ship `consumer-rules.pro` artifacts that R8 picks
# up automatically. The only rules we need here are for code R8 can't reason
# about by itself — typically reflection over our own classes.
#
# A successful `:app:assembleRelease` with no `missing_rules.txt` written under
# `app/build/outputs/mapping/release/` means R8 didn't hit any unresolved
# references; if that file reappears after a dependency bump, copy its contents
# in here as the starting point.

# Keep route classes used by kotlinx.serialization for Navigation 3 type-safe
# routes. Without this, route `data object` / `data class` declarations get
# obfuscated and `EntryProviderBuilder<Any>.entry<Route> { ... }` blows up at
# runtime trying to look them up by qualified name.
-keep,allowobfuscation,allowshrinking class com.rodiz.arch2.feature.**.nav.** { *; }
-keepclassmembers class com.rodiz.arch2.feature.**.nav.** { *; }

# kotlinx.serialization companion accessors — only kicks in if a `@Serializable`
# class lives outside :feature:*:nav. Cheap, defensive, no measurable size cost.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
    static **$$serializer INSTANCE;
}

# Domain models cross module boundaries and Firestore deserializes them via
# reflection (snapshot.toObject<T>() in :core:firebase). Keep the field
# names so the SDK can map JSON keys → fields after obfuscation.
-keepclassmembers class com.rodiz.arch2.**.domain.model.** {
    <fields>;
    <init>(...);
}

# Crashlytics — keep our handled-error pipeline visible in the dashboard.
# FirebaseCrashlytics ships its own consumer rules; this just keeps OUR
# CrashReporter facade and impl uninlined so stack frames in non-fatal
# reports still read as `CrashReporter.recordException(...)` instead of
# `a.b(...)`.
-keep class com.rodiz.arch2.core.common.logging.CrashReporter { *; }
-keep class com.rodiz.arch2.core.firebase.FirebaseCrashReporter { *; }

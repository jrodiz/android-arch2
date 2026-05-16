# Plan: Rebrand Arch2.0 → TinPet

> **Note (project CLAUDE.md):** plans must live in the project's `plans/` directory. The first action of the implementation will be to move this file to `plans/rebrand-tinpet.md` and version it with the code.

## Context

The project is being repositioned from a generic "Arch2.0" scaffold to a real product called **TinPet** — a Tinder-style app for pets. This change is purely cosmetic / identity-level. The user explicitly wants to **preserve all package names, applicationId, namespace, Firebase configs, and existing integrations** so the change is reversible and safe. Anything that would invalidate the Firebase OAuth client, Crashlytics association, or break compiled bytecode contracts stays as `com.rodiz.arch2.*`.

The intended outcome:
- App launcher shows "TinPet"
- Android Studio shows the project as "TinPet"
- Design-system theme, splash theme, and convention plugins all use the TinPet brand
- Docs read as a TinPet project
- `com.rodiz.arch2.*` packages, `applicationId`, Firebase credentials, `google-services.json` are untouched

## Scope summary

| Category | Action |
|---|---|
| User-facing strings & themes | **Rename** |
| Compose theme composable (`Arch2Theme`) | **Rename** |
| Splash theme XML id (`Theme.Arch2.Splash`) | **Rename** |
| Gradle `rootProject.name` | **Rename** |
| build-logic plugin IDs (`arch.*` → `tinpet.*`) + group | **Rename** (user confirmed) |
| Docs (CLAUDE.md, README, plans/) | **Rename** in prose only |
| `com.rodiz.arch2.*` packages, imports, applicationId, namespace | **Keep** |
| `google-services.json`, Firebase config, web client ID | **Keep** |
| Project directory name `Arch2.0/` | **Keep** (not in scope; flag for later) |

## Phase 1 — User-facing identifiers

**Files to edit:**

1. `app/src/main/res/values/strings.xml:3` — `<string name="app_name">Arch2</string>` → `<string name="app_name">TinPet</string>`
2. `app/src/main/res/values/themes.xml:3` — `<style name="Theme.Arch2.Splash" ...>` → `<style name="Theme.TinPet.Splash" ...>`
3. `app/src/main/AndroidManifest.xml:14` — `android:theme="@style/Theme.Arch2.Splash"` → `android:theme="@style/Theme.TinPet.Splash"`
4. `core/designsystem/src/main/kotlin/com/rodiz/arch2/core/designsystem/theme/Theme.kt:8` — `fun Arch2Theme(...)` → `fun TinPetTheme(...)`
5. Callers of `Arch2Theme` (auto-rename via IDE or grep-replace):
   - `app/src/main/kotlin/com/rodiz/arch2/MainActivity.kt:9, 28`
   - `feature/login/presentation/src/main/kotlin/com/rodiz/arch2/feature/login/presentation/screen/LoginScreen.kt:49, 300, 308, 323`

Note: file is **inside** `com/rodiz/arch2/core/designsystem/theme/` (a package path) — that directory stays. Only the Kotlin identifier `Arch2Theme` changes.

## Phase 2 — Build-system plugin IDs

This phase has a sequencing requirement: the registry must be updated **before** the consumers, or `:build-logic:convention` won't find the renamed IDs. Do them in this order:

1. **`gradle/libs.versions.toml:112-120`** — rename the 9 plugin aliases & IDs:
   - `arch-android-application = { id = "arch.android.application" }` → `tinpet-android-application = { id = "tinpet.android.application" }`
   - Same shape for: `library`, `library-compose`, `feature`, `hilt`, `firebase`, `test` (android.*), `jvm-library`, `kotlin-serialization`.
2. **`build-logic/convention/build.gradle.kts`**
   - `group = "com.rodiz.arch2.buildlogic"` (line 5) → `"com.rodiz.tinpet.buildlogic"`
   - Inside `gradlePlugin.plugins { register("...") { id = "arch.*" } }` (lines 20-59) — change each `id = "arch.*"` to `id = "tinpet.*"`. The `register("...")` short names can stay or change — they're internal Gradle handles only, not referenced elsewhere; renaming them keeps the file consistent. **Action:** rename them.
3. **Apply-site updates** in every module's `build.gradle.kts` that uses `alias(libs.plugins.arch.*)`:
   - `app/build.gradle.kts`
   - `core/{common,navigation,designsystem,ui,testing,datastore,firebase}/build.gradle.kts`
   - `core/session/{data,domain}/build.gradle.kts`
   - `feature/login/{nav,data,domain,presentation}/build.gradle.kts`
   - `feature/home/{nav,presentation}/build.gradle.kts`
   - Mechanical: `alias(libs.plugins.arch.X)` → `alias(libs.plugins.tinpet.X)` (17 files).

Tip: `find . -name 'build.gradle.kts' -not -path '*/build/*' -exec sed -i '' 's/libs.plugins.arch\./libs.plugins.tinpet./g' {} +` from the repo root will handle apply-sites in one shot. The `libs.versions.toml` and `build-logic/convention/build.gradle.kts` need manual edits because they also touch the `id = "..."` literal.

## Phase 3 — Project name & docs

1. **`settings.gradle.kts:18`** — `rootProject.name = "Arch2"` → `"TinPet"`
2. **`README.md`** (if present) and **`CLAUDE.md`** — replace "Arch2.0" and "Arch2" in prose with "TinPet". Preserve any code blocks that reference `com.rodiz.arch2.*` packages, gradle commands that reference `:app`, etc.
3. **`ANDROID_APP_SCAFFOLD_PROMPT.md`** — same prose rewrite. This is the canonical spec doc.
4. **`plans/*.md`** — same rule: rewrite prose only, preserve any `com.rodiz.arch2.*` snippets and the `Arch2Theme` references that point at the *old* state (so historic context isn't lost). Use judgment: if a sentence describes the current/future state ("the Arch2 theme..."), update it; if it describes a past decision ("...originally generated by the Arch2 scaffold"), leave it.

## Phase 4 — Verification

1. Sync project in Android Studio — verify `rootProject.name = "TinPet"` shows in the project window.
2. Build:
   ```bash
   JAVA_HOME=/Users/jrodiz/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home ./gradlew assembleDebug
   ```
   This validates: plugin IDs resolve, no `arch.*` references remain in consumer build files, all `Arch2Theme` callers updated.
3. Install and launch on emulator; confirm the launcher icon label reads **TinPet**:
   ```bash
   ./gradlew :app:installDebug
   adb shell monkey -p com.rodiz.arch2.debug -c android.intent.category.LAUNCHER 1
   ```
   (applicationId is unchanged, so the package is still `com.rodiz.arch2.debug` — intentional.)
4. Confirm Firebase / Google sign-in still works (the OAuth client ID is bound to the unchanged package name + SHA-1).
5. Run tests:
   ```bash
   ./gradlew test
   ```
   Catches any lingering `Arch2Theme` reference in @Preview composables.
6. Final sweep — there should be **zero matches** for these patterns in source/resources (excluding `plans/`, `CLAUDE.md`, generated `build/` dirs, and intentional package paths):
   ```bash
   grep -rn 'Arch2Theme\|Theme\.Arch2\|"Arch2"\|libs\.plugins\.arch\.' --include='*.kt' --include='*.kts' --include='*.xml' --include='*.toml' . | grep -v '/build/'
   ```

## Out of scope (flagged for later)

- Renaming the on-disk directory `~/AndroidStudioProjects/Rodiz/Arch2.0/` to `TinPet/` — would invalidate IDE indexes, git worktree paths, and the user's shell history. Defer.
- Renaming the Kotlin packages `com.rodiz.arch2.*` to `com.rodiz.tinpet.*` — would break the Firebase OAuth client binding. Defer until the team is ready to re-issue Firebase credentials and update `google-services.json`.
- A pet-themed launcher icon / splash drawable — the visual asset overhaul. This plan only changes textual identity.

## Critical files (quick reference)

- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values/themes.xml`
- `app/src/main/AndroidManifest.xml`
- `core/designsystem/src/main/kotlin/com/rodiz/arch2/core/designsystem/theme/Theme.kt`
- `app/src/main/kotlin/com/rodiz/arch2/MainActivity.kt`
- `feature/login/presentation/src/main/kotlin/com/rodiz/arch2/feature/login/presentation/screen/LoginScreen.kt`
- `settings.gradle.kts`
- `gradle/libs.versions.toml`
- `build-logic/convention/build.gradle.kts`
- Every `build.gradle.kts` under `app/`, `core/`, `feature/` (17 apply-sites)
- `CLAUDE.md`, `README.md` (if exists), `ANDROID_APP_SCAFFOLD_PROMPT.md`, `plans/*.md`

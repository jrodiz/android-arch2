# Android App Scaffold — Prompt

Create a new Android application following Google's latest recommended architecture and tech stack. The output must be production-ready scaffolding I can extend by dropping in new feature modules.

This document is the **canonical spec**. When in doubt, prefer it over assumptions. It is informed by the `android-clean-architecture` and `navigation-3` skills (see "Skill alignment" at the bottom).

---

## 1. Project setup

- Kotlin (latest stable), **JDK 17 minimum** (JDK 21 preferred when available — see §13.1 of any feature plan for the upgrade flip), target & compile SDK = latest stable supported by the chosen AGP.
- **Android Gradle Plugin** — pin to a **stable** version. As of the first scaffold build that's **AGP 8.10.x** with `compileSdk 36`. AGP 9 was alpha and broke `CommonExtension`'s type-parameter shape; do not adopt it for the scaffold until it stabilizes.
- All build scripts in Kotlin DSL (`.gradle.kts`)
- Centralize dependencies in a Gradle **version catalog** (`gradle/libs.versions.toml`). Include `javax.inject:javax.inject:1` — pure-JVM `:domain` modules need it explicitly for `@Inject` constructors (it's not transitively provided by the JVM convention).
- Use **convention plugins** in a `build-logic/` included build to share configuration:
  - `android.application`, `android.library`, `android.feature`
  - `jvm.library` (for pure-Kotlin modules: `domain`, `nav`)
  - `compose`, `hilt`, `room`, `testing`, `lint`
- Prefer **KSP** over kapt everywhere (Hilt, Room)
- R8 full mode in release; only add keep rules with justification
- **Kotlin Gradle Plugin gotcha:** to set `jvmTarget` in a convention plugin, configure `KotlinJvmCompile` — `KotlinCompilationTask` exposes only `KotlinCommonCompilerOptions`, which has no `jvmTarget`.

---

## 2. Module structure

Each feature is split into **4 modules**: `data`, `domain`, `presentation`, and `nav`. This keeps navigation contracts decoupled from screen implementations so features can navigate to each other without compiling each other's UI.

```
:app                              // Application class, DI graph, NavDisplay host
:build-logic                      // Convention plugins (included build)

:core
  :core:designsystem              // Material 3 theme, tokens, reusable Composables  (Android)
  :core:ui                        // Shared UI utilities, previews, state holders     (Android)
  :core:common                    // Try/AppError, dispatcher qualifiers              (JVM)
  :core:navigation                // Shared Navigator, EntryProviderInstaller typealias (Android — Compose runtime)
  :core:database                  // Room database, shared converters                 (Android)
  :core:datastore                 // Preferences/Proto DataStore                      (Android)
  :core:network                   // Retrofit/OkHttp client, serializers, interceptors (Android)
  :core:testing                   // Test doubles, fixtures, rules                    (Android)
  :core:session                   // Example of a JVM/Android split:
    :core:session:domain          //   - Session model, SessionRepository interface   (JVM)
    :core:session:data            //   - DataStore-backed impl + @Binds              (Android)

:feature
  :feature:<name>
    :feature:<name>:nav           // Pure Kotlin — route keys only (public contract)
    :feature:<name>:domain        // Pure Kotlin — entities, repo interfaces, UseCases
    :feature:<name>:data          // Repo impls, DTOs, mappers, DAOs (if feature-scoped)
    :feature:<name>:presentation  // ViewModels, Composables, EntryProviderInstaller
```

### 2.1 Dependency rules (strict)

```
:app
  └─► every :feature:*:presentation
  └─► :core:navigation, :core:designsystem, :core:common

:feature:X:presentation
  ├─► :feature:X:domain
  ├─► :feature:X:nav
  ├─► :core:navigation, :core:designsystem, :core:ui, :core:common
  └─► :feature:Y:nav         (ONLY :nav — never another feature's :presentation/:domain/:data)

:feature:X:data
  ├─► :feature:X:domain
  └─► :core:network, :core:database, :core:common

:feature:X:domain
  └─► :core:common           (pure Kotlin/JVM, NO Android)

:feature:X:nav
  └─► (nothing or :core:navigation only — pure Kotlin/JVM, NO Android, NO Compose)
```

**Hard invariants — verify these in CI:**

- `:feature:*:domain` and `:feature:*:nav` apply **only** `org.jetbrains.kotlin.jvm`. They must not transitively pull in `androidx.*`, Compose, Room, Retrofit, or Hilt.
- No feature `:presentation` depends on another feature's `:presentation`, `:domain`, or `:data`. Cross-feature navigation goes through `:nav`.
- `:domain` never depends on `:data`. Repository interfaces live in `:domain`; implementations in `:data`.
- Domain models cross module boundaries. DTOs and Room entities stay inside `:data`.
- **Core modules consumed by a `:feature:*:domain` or `:feature:*:nav` must themselves be JVM-only** — Gradle variant matching can't pass an `androidJvm` artifact to a `jvm` consumer. If a core module needs Android types (DataStore, Room, Hilt-on-`SingletonComponent`), **split it the same way features are split**:
  - `:core:foo:domain` (JVM) — interfaces, models, pure Kotlin.
  - `:core:foo:data` (Android) — impl + `@Binds`/`@Provides`.
  - Or keep the core module Android-only and have *no* JVM domain ever depend on it.

  In this scaffold, `:core:session` is split this way; `:core:common` is fully JVM (the dispatcher `@Module` lives in `:app/di/`).

---

## 3. The `:feature:<name>:nav` module — what goes in it

This is the **public navigation contract** for a feature. Other feature `:presentation` modules depend on it (and only it) to navigate to this feature.

**Contents:**

- Route keys (Navigation 3 destinations) — `@Serializable` Kotlin objects/data classes:

  ```kotlin
  // :feature:profile:nav
  @Serializable data object ProfileGraph         // graph root
  @Serializable data object ProfileHome
  @Serializable data class ProfileEdit(val userId: String)
  ```

- Deep-link patterns / URI matchers for this feature's routes (optional but recommended — keeps deep-link knowledge local to the feature).
- Result/argument contracts: stable data classes exchanged with callers when returning results from a flow.

**Explicitly NOT in `:nav`:** Composables, ViewModels, `EntryProviderInstaller`, Hilt modules, business logic. Those live in `:presentation`. Keeping `:nav` pure means a feature can be referenced by 10 other features without dragging in its UI graph.

---

## 4. The `:feature:<name>:presentation` module

- ViewModels (`@HiltViewModel`), `UiState` (immutable, exposed via `StateFlow`), `UiEvent` channel for one-shot effects.
- Composables, screen entry points, navigation animations specific to this feature.
- A **single `EntryProviderInstaller`** that maps this feature's `:nav` route keys to its Composables, contributed to the app's set via Hilt multibinding (`@IntoSet`):

  ```kotlin
  // :feature:profile:presentation
  @Module
  @InstallIn(ActivityRetainedComponent::class)
  object ProfileNavModule {
      @IntoSet
      @Provides
      fun provideProfileEntries(navigator: Navigator): EntryProviderInstaller = {
          entry<ProfileHome> { ProfileHomeScreen(...) }
          entry<ProfileEdit> { key -> ProfileEditScreen(key.userId, ...) }
      }
  }
  ```

- Composables remain stateless: `(state, onEvent) -> Unit`. UDF throughout.

---

## 5. The `:feature:<name>:domain` module

- Pure Kotlin/JVM. No Android, no Compose, no Retrofit, no Room.
- Entities (plain `data class`).
- Repository **interfaces**.
- **UseCases** with `operator fun invoke` — one business operation each:

  ```kotlin
  class GetProfileUseCase(private val repo: ProfileRepository) {
      suspend operator fun invoke(userId: String): Result<Profile> =
          repo.getProfile(userId)
  }
  ```

- Flow-based UseCases for reactive streams.
- `Result<T>` or a sealed `Try<T>` / `AppError` type for error propagation. Never throw across module boundaries.

---

## 6. The `:feature:<name>:data` module

- Repository **implementations** of the interfaces declared in `:domain`.
- Local + remote data sources for the feature.
- DTOs (Retrofit / kotlinx.serialization) and Room entities, kept internal to the module.
- Mappers as **extension functions** in the data layer: `ProfileDto.toEntity()`, `ProfileEntity.toDomain()`.
- **Single source of truth** pattern: Room is the SoT; network calls write to the DB; UI observes the DB via `Flow`.
- Hilt module providing the repository binding:

  ```kotlin
  @Module
  @InstallIn(SingletonComponent::class)
  abstract class ProfileDataModule {
      @Binds
      abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository
  }
  ```

---

## 7. Tech stack (Google-recommended)

- **UI:** Jetpack Compose + Material 3 (Material You), Compose BOM, adaptive layouts, **edge-to-edge** enabled, predictive back gestures supported.
- **Navigation:** Jetpack **Navigation 3** (`androidx.navigation3.*`). Use `NavDisplay` in the app module, type-safe `@Serializable` routes from `:feature:*:nav` modules, and a shared `Navigator` in `:core:navigation` injected as `@ActivityRetainedScoped`.
- **DI:** Hilt (KSP). Use `@IntoSet` multibindings to collect each feature's `EntryProviderInstaller`.
- **Async:** Kotlin Coroutines + Flow. `StateFlow` for UI state; `SharedFlow`/`Channel` for one-shot events.
- **Networking:** Retrofit + OkHttp + **kotlinx.serialization** (JSON converter).
- **Local DB:** Room (KSP) with `Flow`-returning DAOs.
- **Preferences:** Jetpack DataStore (Preferences or Proto).
- **Images:** Coil 3 (Compose).
- **Background:** WorkManager with Hilt integration.
- **Lifecycle/ViewModel:** androidx.lifecycle + `viewModelScope`.

---

## 8. Navigator (shared in `:core:navigation`)

A single `Navigator` owns the back stack and is injected wherever navigation is triggered. ViewModels and Composables call `navigator.goTo(SomeFeatureRoute)`; `SomeFeatureRoute` is imported from another feature's `:nav` module.

```kotlin
// :core:navigation — receiver type depends on the navigation3 alpha you're on
// (older alphas: EntryProviderScope<Any>; alpha02+: EntryProviderBuilder<Any>).
// Keep this typealias as the single point of change.
typealias EntryProviderInstaller = EntryProviderBuilder<Any>.() -> Unit

@ActivityRetainedScoped
class Navigator @Inject constructor(@StartDestination start: Any) {
    val backStack: SnapshotStateList<Any> = mutableStateListOf(start)
    fun goTo(destination: Any) { backStack.add(destination) }
    fun goBack() { backStack.removeLastOrNull() }
    fun replaceAll(destination: Any) { backStack.clear(); backStack.add(destination) }
}
```

The start destination is supplied by `:app` via a `@StartDestination` Hilt qualifier so the initial route can vary by app state (e.g. session present → Home, else → Login).

The `:app` `Activity` injects `Navigator` plus `Set<@JvmSuppressWildcards EntryProviderInstaller>` and composes a `NavDisplay`:

```kotlin
NavDisplay(
    backStack = navigator.backStack,
    onBack = { navigator.goBack() },
    entryProvider = entryProvider {
        entryProviderInstallers.forEach { install -> install() }
    },
)
```

---

## 9. Quality, testing, CI

- **Unit tests:** JUnit5 (or 4 if simpler), **MockK**, **Turbine** for `Flow`, `kotlinx-coroutines-test` with a test dispatcher rule.
- **Repository tests:** Room in-memory + fake network source.
- **UI tests:** Compose UI testing + screenshot tests (Paparazzi or Roborazzi) for the design system.
- **Static analysis:** detekt + Spotless (ktlint), wired into a `check` convention plugin.
- **CI:** GitHub Actions workflow running `assemble`, `check`, and unit tests on PRs.
- Optional but scaffolded: Macrobenchmark module and Baseline Profiles.

---

## 10. Constraints

- No new libraries beyond what's listed without README justification.
- `:domain` and `:nav` are Android-free — enforced by applying only the JVM Kotlin plugin.
- No `runBlocking` in production code. No `GlobalScope`.
- Inject `CoroutineDispatcher`s via a `@Dispatcher` qualifier; never hardcode `Dispatchers.IO`.
- All user-facing strings localized; no hardcoded strings in Composables.
- Mappers never reach across module boundaries — DTOs/entities never leave `:data`.

---

## 11. Deliverables

1. Full module graph above, building cleanly on a fresh checkout.
2. **One end-to-end sample feature** (e.g., `:feature:items`) demonstrating: Retrofit DTO → mapper → Room entity → repository → UseCase → ViewModel `UiState` → Compose screen with loading / empty / error / success states, navigated to from a second sample feature using only its `:nav` module.
3. `README.md` explaining: module graph, dependency rules, the 4-module-per-feature pattern, and a "how to add a new feature" checklist.
4. `docs/architecture.md` with a Mermaid diagram of the layers and dependency direction.

---

## 12. Workflow

Start by proposing:

1. The version catalog (`libs.versions.toml`).
2. The convention plugin layout in `build-logic/`.
3. The list of modules to be created with their applied plugins.

**Wait for confirmation**, then scaffold the modules.

---

## Lessons from the first build (read these before scaffolding)

These are concrete pitfalls discovered while bringing the scaffold up from scratch. They are not theoretical.

1. **Don't ship on AGP 9 alpha.** It removes type parameters from `CommonExtension` and breaks convention plugins. Pin to the latest stable AGP that supports the `compileSdk` your dependencies require (8.10.x with `compileSdk 36` at time of first build).
2. **Pin `androidx.navigation3` to a version that actually resolves** — recent alphas often don't. `1.0.0-alpha02` worked. The receiver type for `entry { ... }` blocks was renamed from `EntryProviderScope` to `EntryProviderBuilder` somewhere in this alpha range — keep a single typealias in `:core:navigation` and change it there.
3. **`KotlinJvmCompile`, not `KotlinCompilationTask`** — only the JVM-specific task has `jvmTarget` on its `compilerOptions`.
4. **`org.gradle.kotlin.dsl.platform` doesn't exist** as an import — `platform(...)` is already in scope inside `dependencies { ... }`.
5. **`javax.inject:javax.inject:1` is not transitive** in pure-JVM modules. Add it explicitly to the version catalog and to any `:domain` that uses `@Inject` constructors.
6. **Library `arch.android.library.compose` convention is library-only.** The `:app` module applies the Compose plugin and Compose BOM dependencies directly — don't reuse the library convention there.
7. **`FragmentActivity` is required** for `BiometricPrompt` (not `ComponentActivity`). `:app` needs `androidx.appcompat:appcompat` if `Theme.AppCompat.*` is used as a splash theme.
8. **Don't reference `@mipmap/ic_launcher`** in the manifest until you actually have launcher icon assets — it breaks the build.
9. **Library modules consumed by a JVM module must themselves be JVM.** Plan core-module Android-vs-JVM up front; refactoring later is annoying. See the dependency invariants in §2.1.

---

## Skill alignment (reference)

This spec was cross-checked against two installed skills:

- **`android-clean-architecture`** — confirms: pure-Kotlin `domain`, repository interfaces in `domain` / implementations in `data`, UseCases with `operator invoke`, mappers as extension functions in `data`, Room as single-source-of-truth with `Flow`-returning DAOs, Hilt `@Binds` for repositories, no `GlobalScope`, no framework types leaking out of `data`.
- **`navigation-3`** (modular-hilt recipe) — confirms: split each feature into an **api** module (route keys only — what we call `:nav`) and an **impl** module (composables + `EntryProviderInstaller` — what we call `:presentation`), contribute installers via Hilt `@IntoSet` multibinding, single shared `Navigator` (`@ActivityRetainedScoped`) owning the back stack, `NavDisplay` wired in `:app`.

The 4-module split (`nav` / `domain` / `data` / `presentation`) is a strict superset of the official recipe's 2-module split, adding Clean Architecture's `data`/`domain` separation on top of Navigation 3's `api`/`impl` separation.

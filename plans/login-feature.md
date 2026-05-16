# Plan — Bootstrap Arch2.0 scaffold + `:feature:login`

## Context

The repo at `/Users/jrodiz/AndroidStudioProjects/Rodiz/Arch2.0/` is empty save for `.claude/` and `ANDROID_APP_SCAFFOLD_PROMPT.md`. This plan does two things in one pass:

1. **Bootstraps the entire scaffold** described in `ANDROID_APP_SCAFFOLD_PROMPT.md` — version catalog, `build-logic/` convention plugins, `:app`, the `:core:*` modules, and the navigation infrastructure (shared `Navigator`, `EntryProviderInstaller` Hilt multibinding).
2. **Implements `:feature:login`** as the first end-to-end feature, exercising all 4 sub-modules (`nav`, `domain`, `data`, `presentation`) plus a minimal `:feature:home` to navigate to on success.

The intended outcome is a buildable AGP 9 / Kotlin / Compose project where `:feature:login` works against a fake auth API (1 s delay), persists a fake session (always-remember), supports biometric unlock backed by Android Keystore, clears the back stack on success, and has the required tests green.

### Confirmed user choices

- **Biometric:** full flow — `BiometricPrompt` + Keystore-backed credential storage.
- **Remember me:** always remember (no checkbox). Persist session to DataStore on success; on launch, skip login if session present.
- **Project root:** initialize directly in `/Users/jrodiz/AndroidStudioProjects/Rodiz/Arch2.0/`.
- **Application id:** `com.rodiz.arch2`.

---

## 1. Build infrastructure

### 1.1 Gradle / toolchain

- `settings.gradle.kts`: declare `pluginManagement`, `dependencyResolutionManagement` (FAIL_ON_PROJECT_REPOS), `includeBuild("build-logic")`, then `include(...)` every module listed below.
- `gradle/wrapper/gradle-wrapper.properties` — Gradle version compatible with AGP 9.
- Root `build.gradle.kts` — plugins block applying `false` for: AGP, Kotlin, Hilt, KSP, kotlinx.serialization, Compose Compiler.
- `gradle.properties` — `org.gradle.jvmargs`, `kotlin.code.style=official`, `android.useAndroidX=true`, `android.nonTransitiveRClass=true`, `org.gradle.configuration-cache=true`.
- `gradle/libs.versions.toml` with versions for: AGP 9.x, Kotlin (latest stable), Compose BOM, Material3, AndroidX Lifecycle, Hilt, KSP, Room, DataStore, Retrofit, OkHttp, kotlinx-serialization, kotlinx-coroutines, Navigation 3 (`androidx.navigation3.*`), androidx.security.crypto, androidx.biometric, Coil 3, JUnit5, MockK, Turbine, kotlinx-coroutines-test, Robolectric, androidx.compose.ui.test.

### 1.2 `build-logic/` (included build)

Convention plugins in `build-logic/convention/src/main/kotlin/`:

- `arch.android.application` — applies AGP app, sets compileSdk/targetSdk/minSdk, JVM target, Kotlin compiler args, packagingOptions.
- `arch.android.library` — same for libraries.
- `arch.android.library.compose` — adds Compose plugin, Compose BOM, Compose dependencies.
- `arch.android.feature` — composition of `library` + Hilt + Compose + common deps; applied to every `:feature:*:presentation` module.
- `arch.android.hilt` — Hilt + KSP.
- `arch.android.room` — Room + KSP.
- `arch.jvm.library` — pure Kotlin/JVM (applied to every `:nav` and `:domain` module). **No Android plugins.**
- `arch.kotlin.serialization` — applies kotlinx.serialization plugin.
- `arch.android.test` — shared test deps + JUnit5 + MockK + Turbine + coroutines-test.

Each convention plugin lives in its own `.kt` file and is registered in `build-logic/convention/build.gradle.kts` via the `gradlePlugin { plugins { create(...) } }` block.

---

## 2. Module graph (final list)

```
:app
:core:designsystem
:core:ui
:core:common
:core:navigation
:core:datastore
:core:session
:core:testing
:feature:login:nav
:feature:login:domain
:feature:login:data
:feature:login:presentation
:feature:home:nav
:feature:home:presentation
```

`:core:network`, `:core:database`, and the `:core:domain` umbrella module are deliberately **not created yet** — the login feature uses a fake in-memory auth source, and Room/Retrofit aren't needed for this slice. They'll be added with the first feature that needs real network/DB.

### 2.1 `:core:common`

- `coroutine/Dispatcher.kt` — `@Qualifier` annotations `@IoDispatcher`, `@DefaultDispatcher`, `@MainDispatcher` + a Hilt module providing them.
- `result/AppError.kt` — sealed base for cross-feature error types.
- `result/Try.kt` — `sealed interface Try<out T> { Success(value); Failure(error) }` (chosen over `Result<T>` for ergonomic exhaustive `when`).

### 2.2 `:core:navigation`

- `Navigator.kt`:

  ```kotlin
  typealias EntryProviderInstaller = EntryProviderScope<Any>.() -> Unit

  @ActivityRetainedScoped
  class Navigator @Inject constructor(@StartDestination start: Any) {
      val backStack: SnapshotStateList<Any> = mutableStateListOf(start)
      fun goTo(d: Any) { backStack.add(d) }
      fun goBack() { backStack.removeLastOrNull() }
      fun replaceAll(d: Any) { backStack.clear(); backStack.add(d) }
  }
  ```

- `@StartDestination` qualifier so `:app` can provide the initial route based on session state.

### 2.3 `:core:designsystem`

- `theme/TinPetTheme.kt` — Material 3 theme, dynamic color, light/dark schemes.
- `theme/Type.kt`, `theme/Color.kt`.

### 2.4 `:core:ui`

- `components/EmailField.kt`, `components/PasswordField.kt` (with show/hide toggle), `components/PrimaryButton.kt` (loading state), `components/ErrorBanner.kt` / `SnackbarHostHelper.kt`.

### 2.5 `:core:datastore`

- Hilt module providing `DataStore<Preferences>` singleton named `"app_prefs"`.

### 2.6 `:core:session`

- `domain/Session.kt` — `data class Session(val userId: String, val token: String) { override fun toString() = "Session(userId=$userId, token=[REDACTED])" }`.
- `domain/SessionRepository.kt`:

  ```kotlin
  interface SessionRepository {
      fun observe(): Flow<Session?>
      suspend fun save(session: Session)
      suspend fun clear()
      suspend fun current(): Session?
  }
  ```

- `data/SessionRepositoryImpl.kt` — DataStore-backed.
- Hilt `@Binds` module.

This is one module (not split into `domain`/`data`) — the spec only mandates the 4-way split for **features**, not core utilities.

---

## 3. `:feature:login` — 4 sub-modules

### 3.1 `:feature:login:nav` (pure Kotlin, JVM-only)

```kotlin
@Serializable data object LoginHome
@Serializable data object ForgotPassword   // stub destination
@Serializable data object CreateAccount    // stub destination
```

Depends on: nothing (or `:core:common` if needed). **No Compose, no Android.**

### 3.2 `:feature:login:domain` (pure Kotlin, JVM-only)

- `model/Credentials.kt`:

  ```kotlin
  data class Credentials(val email: String, val password: String) {
      override fun toString() = "Credentials(email=$email, password=[REDACTED])"
  }
  ```

- `model/AuthError.kt`:

  ```kotlin
  sealed interface AuthError : AppError {
      data object InvalidCredentials : AuthError      // maps to 401
      data object NoNetwork : AuthError
      data object Timeout : AuthError
      data class Server(val code: Int) : AuthError    // 5xx
      data object Unknown : AuthError
  }
  ```

- `model/ValidationError.kt` — `EmailEmpty`, `EmailMalformed`, `PasswordTooShort`, `PasswordEmpty`.
- `repository/AuthRepository.kt` — interface with `suspend fun login(creds): Try<Session, AuthError>`, `suspend fun loginWithStoredCredentials(): Try<Session, AuthError>`, `suspend fun hasStoredCredentials(): Boolean`, `suspend fun storeForBiometric(creds: Credentials)`.
- `usecase/LoginUseCase.kt`, `usecase/LoginWithBiometricUseCase.kt`, `usecase/ValidateEmailUseCase.kt`, `usecase/ValidatePasswordUseCase.kt` (8-char min). All `operator fun invoke`.

### 3.3 `:feature:login:data` (Android library)

- `remote/FakeAuthRemoteDataSource.kt` — `delay(1000)`, then return a hardcoded `AuthDto(token, userId)`. For tests, a separate `FakeAuthRemoteDataSource` parameterized to throw `IOException`/`HttpException(401)`/`HttpException(500)`/`TimeoutCancellationException` via a `behavior` knob.
- `remote/dto/AuthDto.kt` (internal — never leaves the module).
- `mapper/AuthDtoMapper.kt` — `fun AuthDto.toSession(): Session`.
- `local/CredentialVault.kt` — `EncryptedSharedPreferences` (`androidx.security.crypto` 1.1.0-alpha) keyed by `MasterKey.Builder(context).setKeyScheme(AES256_GCM).build()`. Stores email + password. Provides `store(creds)`, `load(): Credentials?`, `clear()`, `exists(): Boolean`.
- `repository/AuthRepositoryImpl.kt` — coordinates remote + vault + SessionRepository. On success: persist `Session` via `SessionRepository.save(...)`. Maps exceptions to `AuthError`: `IOException` → `NoNetwork`, `SocketTimeoutException` / `TimeoutCancellationException` → `Timeout`, 401 → `InvalidCredentials`, 5xx → `Server(code)`, else → `Unknown`.
- Hilt module: `@Binds` for `AuthRepository`, `@Provides` for `CredentialVault` and `FakeAuthRemoteDataSource`.

Depends on: `:feature:login:domain`, `:core:common`, `:core:session`.

### 3.4 `:feature:login:presentation` (Android Compose library + Hilt)

- `state/LoginUiState.kt`:

  ```kotlin
  data class LoginUiState(
      val email: String = "",
      val password: String = "",
      val passwordVisible: Boolean = false,
      val emailError: ValidationError? = null,
      val passwordError: ValidationError? = null,
      val isSubmitting: Boolean = false,
      val transientError: AuthError? = null,
      val biometricAvailable: Boolean = false,
  ) {
      val canSubmit: Boolean get() =
          email.isNotBlank() && password.isNotBlank() &&
          emailError == null && passwordError == null && !isSubmitting
  }
  ```

- `state/LoginEvent.kt` — `sealed interface LoginEvent { NavigateHome; NavigateForgot; NavigateCreate; ShowBiometricPrompt }`.
- `viewmodel/LoginViewModel.kt` — `@HiltViewModel`, exposes `StateFlow<LoginUiState>` + `SharedFlow<LoginEvent>`. Handles `onEmailChange`, `onPasswordChange`, `onTogglePasswordVisibility`, `onSubmit`, `onBiometricSuccess`, `onBiometricRequested`, `onErrorBannerDismissed`. Form fields live in the ViewModel so rotation preserves them.
- `screen/LoginScreen.kt` — stateless `(state, onEvent) -> Unit`. Uses `:core:ui` components. All strings from `strings.xml` via `stringResource(...)`.
- `screen/ForgotPasswordStubScreen.kt`, `screen/CreateAccountStubScreen.kt` — single-Text placeholders.
- `biometric/BiometricPromptManager.kt` — wraps `androidx.biometric.BiometricPrompt`, exposes a `suspend fun authenticate(activity: FragmentActivity): Boolean`.
- `navigation/LoginEntryProvider.kt`:

  ```kotlin
  @Module
  @InstallIn(ActivityRetainedComponent::class)
  object LoginNavModule {
      @IntoSet @Provides
      fun provideLoginEntries(nav: Navigator): EntryProviderInstaller = {
          entry<LoginHome> { LoginRoute(onNavigateHome = { nav.replaceAll(HomeHome) }, onForgot = { nav.goTo(ForgotPassword) }, onCreate = { nav.goTo(CreateAccount) }) }
          entry<ForgotPassword> { ForgotPasswordStubScreen(onBack = { nav.goBack() }) }
          entry<CreateAccount> { CreateAccountStubScreen(onBack = { nav.goBack() }) }
      }
  }
  ```

- `res/values/strings.xml` — every user-facing string: title, field labels, hints, button labels, validation messages (one per `ValidationError`), error messages (one per `AuthError`), forgot/create links, password visibility content descriptions.

Depends on: `:feature:login:nav`, `:feature:login:domain`, `:feature:home:nav` (only `:nav` — to call `nav.replaceAll(HomeHome)`), `:core:navigation`, `:core:ui`, `:core:designsystem`, `:core:common`.

---

## 4. `:feature:home`

### 4.1 `:feature:home:nav`

```kotlin
@Serializable data object HomeHome
```

### 4.2 `:feature:home:presentation`

- `screen/HomeScreen.kt` — placeholder showing "Welcome, $userId" pulled from `SessionRepository`, plus a "Sign out" button that calls `SessionRepository.clear()` + `CredentialVault.clear()` + `navigator.replaceAll(LoginHome)`.

Skipping `:feature:home:domain` / `:data` for the placeholder is fine — when home grows real behavior, those get added.

---

## 5. `:app`

- `App.kt` — `@HiltAndroidApp`.
- `MainActivity.kt` — `FragmentActivity` (needed for BiometricPrompt) + `@AndroidEntryPoint`. Injects `Navigator` + `Set<@JvmSuppressWildcards EntryProviderInstaller>`. Calls `enableEdgeToEdge()`. Composes `TinPetTheme { Scaffold { NavDisplay(navigator.backStack, ...) } }`.
- `di/StartDestinationModule.kt`:

  ```kotlin
  @Module @InstallIn(ActivityRetainedComponent::class)
  object StartDestinationModule {
      @Provides @StartDestination @ActivityRetainedScoped
      fun provideStart(session: SessionRepository): Any = runBlocking { session.current() }?.let { HomeHome } ?: LoginHome
  }
  ```

  The single `runBlocking` here reads one DataStore value at Activity creation — acceptable per AndroidX guidance, and isolated to startup (not in production hot paths). Documented in code with a `// Startup-only: one-shot DataStore read on Activity creation.` comment to explain the why.

Depends on: every `:feature:*:presentation`, `:core:designsystem`, `:core:navigation`, `:core:session`.

---

## 6. Navigation flow summary

1. **Cold start, no session:** start = `LoginHome` → user submits → on success VM persists `Session` and emits `LoginEvent.NavigateHome` → `LoginRoute` calls `navigator.replaceAll(HomeHome)` (clears stack).
2. **Cold start, session present:** start = `HomeHome` → login skipped entirely.
3. **Biometric:** if cold start lands on `LoginHome` AND `CredentialVault.exists()`, VM sets `biometricAvailable = true`; tapping the biometric button triggers `BiometricPromptManager.authenticate(activity)` → on success calls `LoginWithBiometricUseCase` → same success path.
4. **Forgot / Create:** stub `goTo`.

---

## 7. Error mapping (strings.xml keys)

| AuthError | Resource id |
|---|---|
| `InvalidCredentials` | `login_error_invalid_credentials` |
| `NoNetwork` | `login_error_no_network` |
| `Timeout` | `login_error_timeout` |
| `Server` | `login_error_server` (`%1$d` for code) |
| `Unknown` | `login_error_unknown` |

| ValidationError | Resource id |
|---|---|
| `EmailEmpty` | `login_validation_email_empty` |
| `EmailMalformed` | `login_validation_email_malformed` |
| `PasswordEmpty` | `login_validation_password_empty` |
| `PasswordTooShort` | `login_validation_password_short` (`%1$d` for min length) |

Mapping happens in the Composable layer via a small `@Composable fun AuthError.localized(): String` extension — keeps `:domain` framework-free.

---

## 8. Secret hygiene

- `Credentials.toString()` and `Session.toString()` redact `password` / `token`.
- No `Log.*` calls anywhere in `:feature:login:*`.
- `CredentialVault` accepts/returns `Credentials`, never raw strings logged.
- Lint rule in convention plugin: disallow `Log.d`/`Log.v` outside debug builds (optional, mark as nice-to-have).

---

## 9. Testing strategy

### 9.1 `:feature:login:domain` — `src/test/`

- `ValidateEmailUseCaseTest` — empty, malformed (no `@`, no `.`, spaces), valid.
- `ValidatePasswordUseCaseTest` — empty, 7-char (below min), exactly 8, longer.
- `LoginUseCaseTest` — given a fake `AuthRepository` returning each `Try.Failure(AuthError.*)`, assert the use case propagates them.

### 9.2 `:feature:login:presentation` — `src/test/` (Robolectric not required)

- `LoginViewModelTest` using **Turbine** + `kotlinx-coroutines-test`:
  - Email/password change updates state and validation errors.
  - `canSubmit` flips to true only when both fields valid.
  - `onSubmit` sets `isSubmitting = true`, then on success emits `LoginEvent.NavigateHome` and clears submitting.
  - On each `AuthError` (parameterized), assert `transientError` is set and `isSubmitting` is false.
  - Rotation: re-creating the VM with a `SavedStateHandle` containing prior email preserves it (use `SavedStateHandle` for email/password only; password also persisted so user doesn't lose typing — acceptable since it's in-process VM memory, not disk).

### 9.3 `:feature:login:presentation` — `src/androidTest/`

- `LoginScreenHappyPathTest` using `createAndroidComposeRule` + a fake `LoginViewModel` (or a real one with fake repo): enter valid email → enter valid password → button enables → click → loading indicator visible → eventually `onNavigateHome` lambda invoked. Assert via test rule callback flag.

### 9.4 Shared `:core:testing`

- `MainDispatcherRule` (JUnit5 extension or JUnit4 rule — pick one based on JUnit choice).
- `FakeAuthRepository` builder used by VM tests.

**JUnit pick:** JUnit5 for unit, JUnit4 for `androidTest` (Compose UI test requires JUnit4 currently).

---

## 10. Files to create (high-level inventory)

```
settings.gradle.kts
build.gradle.kts
gradle.properties
gradle/libs.versions.toml
gradle/wrapper/gradle-wrapper.properties
gradle/wrapper/gradle-wrapper.jar
gradlew, gradlew.bat

build-logic/
  settings.gradle.kts
  convention/
    build.gradle.kts
    src/main/kotlin/AndroidApplicationConventionPlugin.kt
    src/main/kotlin/AndroidLibraryConventionPlugin.kt
    src/main/kotlin/AndroidLibraryComposeConventionPlugin.kt
    src/main/kotlin/AndroidFeatureConventionPlugin.kt
    src/main/kotlin/AndroidHiltConventionPlugin.kt
    src/main/kotlin/JvmLibraryConventionPlugin.kt
    src/main/kotlin/KotlinSerializationConventionPlugin.kt
    src/main/kotlin/AndroidTestConventionPlugin.kt
    src/main/kotlin/ext/*.kt   // shared helpers (configureKotlinAndroid, etc.)

app/
  build.gradle.kts
  src/main/AndroidManifest.xml
  src/main/kotlin/com/rodiz/arch2/App.kt
  src/main/kotlin/com/rodiz/arch2/MainActivity.kt
  src/main/kotlin/com/rodiz/arch2/di/StartDestinationModule.kt
  src/main/res/values/strings.xml
  src/main/res/values/themes.xml (or fully Compose)

core/designsystem/...
core/ui/...
core/common/...
core/navigation/...
core/datastore/...
core/session/...
core/testing/...

feature/login/nav/build.gradle.kts + src/main/kotlin/com/rodiz/arch2/feature/login/nav/Routes.kt
feature/login/domain/...    (model, repository, usecase)
feature/login/data/...      (remote, local, mapper, repository, di)
feature/login/presentation/... (state, viewmodel, screen, biometric, navigation, res)

feature/home/nav/...
feature/home/presentation/...
```

Module names use `:feature:login:nav` (Gradle path) with disk path `feature/login/nav`.

---

## 11. Verification

After implementation:

1. **Build**: `./gradlew assembleDebug` — must succeed.
2. **Module isolation checks** (manual but cheap):
   - `./gradlew :feature:login:nav:dependencies` — confirm only Kotlin stdlib + kotlinx-serialization show up, no androidx.
   - `./gradlew :feature:login:domain:dependencies` — confirm no Compose, no Room, no Retrofit.
3. **Unit tests**: `./gradlew test` — `LoginViewModelTest`, `LoginUseCaseTest`, validator tests pass.
4. **Compose UI test**: `./gradlew :feature:login:presentation:connectedDebugAndroidTest` (requires emulator) — happy-path test passes.
5. **Manual smoke** on an emulator:
   - First launch → login screen → type invalid email → see inline error → button disabled.
   - Type valid email + 7-char password → see password inline error → button disabled.
   - Type valid email + 8-char password → button enables → tap → 1 s spinner → land on Home with username visible.
   - Kill app → relaunch → land directly on Home (always-remember).
   - Tap "Sign out" → land on Login → biometric button now visible → tap → BiometricPrompt → on success land on Home.
   - Toggle airplane mode and force a fresh login (by signing out + uninstalling): the fake source still returns success, so this only proves the error path via tests, not manual smoke. Error paths are covered by `LoginViewModelTest`.

---

## 12. Out of scope / known limitations (intentional)

- Real auth API integration (Retrofit/OkHttp wired up) — fake source only.
- `:core:network` and `:core:database` modules — added when first real-network/DB feature lands.
- Token refresh / expiry — fake session has no expiry.
- Sign-up flow body — `CreateAccount` is a stub destination.
- Deep links — Navigation 3 deep-link patterns are not wired in this slice; routes are reachable from in-app navigation only.
- Macrobenchmark / baseline profile module — listed in the scaffold spec but not part of this first slice.
- `androidx.security.crypto` is in alpha; if it's been deprecated by the time this is implemented, swap to direct Android Keystore + DataStore encrypted-bytes pattern. Note this in `feature/login/data/local/CredentialVault.kt` with a single-line comment pointing to the alternative.

---

## 13. Build findings — adjustments made during implementation

The sections above describe the **design**. The notes below record **what changed while making the scaffold compile**, so the design intent stays readable and the deviations are explicit.

### 13.1 Toolchain pin (downgrades from the spec)

| Spec said | Built with | Why |
|---|---|---|
| AGP 9.x | **AGP 8.10.1** | `9.0.0-alpha09` doesn't resolve from Google Maven, and the AGP 9 alpha removed type parameters from `CommonExtension`, breaking the convention plugins. 8.10.1 still supports `compileSdk 36` which the newer alpha libraries demand. |
| JDK 21 | **JDK 17** | Only JDK 17 (Zulu / JBR) and JDK 24 are installed locally. JDK 17 is the broadly-stable AGP 8.10 target. To bump back to 21, change `JavaVersion.VERSION_17` and `JvmTarget.JVM_17` in `build-logic/convention/src/main/kotlin/com/rodiz/arch2/convention/AndroidConfig.kt` and `JvmLibraryConventionPlugin.kt`. |
| `compileSdk 36`, `targetSdk 36` | same | Demanded by `androidx.activity:activity-compose:1.12.0-alpha01` and `androidx.navigationevent:1.0.0-alpha01` (transitive via Navigation 3). |

### 13.2 Library version reality checks

- **`androidx.navigation3` pinned to `1.0.0-alpha02`** — alpha versions past ~02 don't yet resolve.
- **Navigation 3 API rename:** the `navigation-3` skill recipe uses `EntryProviderScope` as the receiver for `entry { ... }` blocks. In alpha02 it's **`EntryProviderBuilder`**. Applied in `core/navigation/Navigator.kt`:

  ```kotlin
  typealias EntryProviderInstaller = EntryProviderBuilder<Any>.() -> Unit
  ```

- **Coil 2.7.0** (`io.coil-kt`), not Coil 3 (`io.coil-kt.coil3`). Stays consistent with the AGP 8.10 / Compose BOM 2024.10.01 stack.
- **Lifecycle 2.8.7** (not 2.9.x — 2.9 needs newer AGP).
- The consistent triple is **Kotlin 2.0.21 + KSP 2.0.21-1.0.27 + Hilt 2.52**. Bumping any one of these requires bumping all three.

### 13.3 Convention plugin / Kotlin Gradle Plugin gotchas

- `tasks.withType(KotlinCompilationTask::class.java).configureEach { compilerOptions { jvmTarget.set(...) } }` **fails** — `KotlinCompilationTask` exposes only `KotlinCommonCompilerOptions`, which has no `jvmTarget`. Use `KotlinJvmCompile::class.java` instead. Applied in `AndroidConfig.kt` and `JvmLibraryConventionPlugin.kt`.
- `import org.gradle.kotlin.dsl.platform` does not exist. Inside `dependencies { ... }`, `platform(...)` is already in scope via `DependencyHandlerScope`. Drop the import.

### 13.4 Core-module split required by the JVM-domain invariant

The original plan kept `:core:common` and `:core:session` as single Android library modules. That **broke `:feature:login:domain`** — Gradle variant matching cannot consume `androidJvm` from a pure-`jvm` consumer.

Resolution:

- **`:core:common` is now JVM-only.** It holds `Try`, `AppError`, and the dispatcher qualifier annotations (`@IoDispatcher`, etc. — `@javax.inject.Qualifier` is pure Kotlin).
- **The `DispatchersModule` Hilt binding moved to `:app/di/DispatchersModule.kt`** — that's the only place it needs to install on `SingletonComponent`.
- **`:core:session` was split into two modules**, mirroring the feature pattern:
  - `:core:session:domain` (JVM) — `Session`, `SessionRepository` interface.
  - `:core:session:data` (Android) — DataStore-backed impl + `@Binds` module.

**General scaffold rule (now part of the spec):** any core module consumed by a `:feature:*:domain` or `:feature:*:nav` must itself be JVM-only. Otherwise it needs the same `:domain` / `:data` split features get.

### 13.5 `javax.inject` is not transitively present in pure-JVM domain modules

`@Inject` on use case constructors requires `javax.inject:javax.inject:1` at compile time. The JVM convention plugin doesn't pull it in. Added `javax-inject` to `gradle/libs.versions.toml` and `implementation(libs.javax.inject)` to `feature/login/domain/build.gradle.kts`.

### 13.6 `:app` module specifics

- Removed `tinpet.android.library.compose` from `:app/build.gradle.kts` — that convention is library-only. The application module applies `tinpet.android.application` + `kotlin.compose` directly and depends on the Compose BOM explicitly.
- Removed `android:icon="@mipmap/ic_launcher"` from `AndroidManifest.xml` — no launcher icon assets exist yet; referencing a missing resource breaks the build. Generate icons via Image Asset Studio, then re-add.
- `MainActivity` extends `FragmentActivity` (required for `BiometricPrompt`); `:app` depends on `androidx.appcompat:appcompat` so `Theme.AppCompat.DayNight.NoActionBar` (used by `Theme.TinPet.Splash`) resolves.

### 13.7 Verification (executed)

```
./gradlew assembleDebug        # BUILD SUCCESSFUL
./gradlew test                 # All unit tests pass:
                               #   - 3 validator / use case tests in :feature:login:domain
                               #   - 7 VM tests in :feature:login:presentation
./gradlew :feature:login:nav:dependencies --configuration runtimeClasspath
./gradlew :feature:login:domain:dependencies --configuration runtimeClasspath
# Both JVM modules confirmed: zero androidx / com.android.* on the runtime classpath.
```

`LoginScreenHappyPathTest` (Compose UI) is **unverified** — it needs an emulator (`./gradlew :feature:login:presentation:connectedDebugAndroidTest`).


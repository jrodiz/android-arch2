# Plan — Firebase Auth (Google sign-in) + Firestore user profile

## Context

The user already scaffolded Firebase infra: `arch.android.firebase` convention plugin (applies google-services + Crashlytics plugins, pulls Firebase BOM + analytics + crashlytics + messaging), `google-services.json` with both `com.rodiz.arch2` and `com.rodiz.arch2.debug` registered, and an `AppFirebaseMessagingService`. This plan layers in **actual authentication** using **Credential Manager + "Sign in with Google"**, persists the signed-in user's profile to **Firestore (`users/{uid}`)** for use by future features, and shows the display name on `HomeScreen` to prove multi-user works.

The existing fake email/password flow stays as-is (per user decision). Biometric stays gated to that flow (it relies on `CredentialVault`, which is only populated by the password path).

### Critical pre-implementation step (user action)

`google-services.json` currently has empty `oauth_client` arrays. Google Sign-In needs an OAuth client tied to your app's SHA-1 fingerprint, plus a Web client ID for Credential Manager. **Steps you must do in the consoles before code can work:**

1. Get the debug keystore SHA-1:
   ```bash
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey \
     -storepass android -keypass android | grep -E 'SHA1|SHA-1'
   ```
2. In **Firebase Console** → project `arch2-cac87` → **Project Settings** → "Your apps":
   - For both `com.rodiz.arch2` and `com.rodiz.arch2.debug`, click **Add fingerprint** and paste the debug SHA-1. (Release SHA-1 comes later, when you create a release keystore.)
3. **Build** → **Authentication** → **Sign-in method** → enable **Google**. Save. This auto-creates a **Web OAuth client ID** in Google Cloud and an **Android OAuth client** per registered Android app.
4. Back in Project Settings, **download the updated `google-services.json`** for the app — both apps' configs come in one merged file. Replace `app/google-services.json`. The new file will contain `oauth_client` entries including `client_type: 3` (Web). The `google-services` Gradle plugin auto-generates `R.string.default_web_client_id` from that entry — we'll reference it in code, no manual paste needed.
5. **Build** → **Firestore Database** → **Create database** → Native mode, start in **production** (we ship rules below). Pick a region.
6. Replace the default Firestore rules with:
   ```
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /users/{userId} {
         allow read, write: if request.auth != null && request.auth.uid == userId;
       }
     }
   }
   ```

Once steps 1–6 are done, the code below works end-to-end on the emulator (a real Google account is needed; emulators with Play Services let you add one in Settings).

---

## 1. Version catalog additions (`gradle/libs.versions.toml`)

Add to `[versions]`:

```
androidxCredentials = "1.3.0"
googleId = "1.1.1"
```

Add to `[libraries]`:

```
firebase-auth-ktx       = { group = "com.google.firebase", name = "firebase-auth-ktx" }
firebase-firestore-ktx  = { group = "com.google.firebase", name = "firebase-firestore-ktx" }
androidx-credentials                 = { group = "androidx.credentials", name = "credentials", version.ref = "androidxCredentials" }
androidx-credentials-play-services   = { group = "androidx.credentials", name = "credentials-play-services-auth", version.ref = "androidxCredentials" }
google-id                            = { group = "com.google.android.libraries.identity.googleid", name = "googleid", version.ref = "googleId" }
kotlinx-coroutines-play-services     = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-play-services", version.ref = "kotlinxCoroutines" }
```

(`kotlinx-coroutines-play-services` gives us `.await()` on the `Task<AuthResult>` returned by Firebase.)

No new convention plugin needed — the existing `arch.android.firebase` (analytics/crashlytics/messaging) stays as-is, and we add `firebase-auth-ktx` + `firebase-firestore-ktx` only to the module that needs them (`:core:firebase`).

---

## 2. New module: `:core:firebase`

Android library that exposes Firebase Auth, Firestore, and a user-profile repository. Single module (not split into `:domain`/`:data`) since both layers are Android-only.

```
core/firebase/
  build.gradle.kts
  src/main/AndroidManifest.xml           (empty — namespace via DSL)
  src/main/kotlin/com/rodiz/arch2/core/firebase/
    FirebaseModule.kt                    // @Provides FirebaseAuth, FirebaseFirestore
    model/UserProfile.kt                 // data class
    UserProfileRepository.kt             // interface
    UserProfileRepositoryImpl.kt         // Firestore-backed impl
    UserProfileBindingsModule.kt         // @Binds
```

### 2.1 `UserProfile.kt`

```kotlin
data class UserProfile(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
    val provider: String,   // "google" | "password"
)
```

### 2.2 `UserProfileRepository.kt`

```kotlin
interface UserProfileRepository {
    suspend fun upsertOnSignIn(profile: UserProfile)   // writes users/{uid}, merge, sets lastSignInAt = now
    fun observe(uid: String): Flow<UserProfile?>       // reactive read of one doc
}
```

### 2.3 `UserProfileRepositoryImpl.kt`

- Uses `firestore.collection("users").document(uid).set(map, SetOptions.merge())` with fields: `uid`, `email`, `displayName`, `photoUrl`, `provider`, `createdAt` (only on first write — use `FieldValue.serverTimestamp()` + `SetOptions.merge()` with a separate doc-existence check), `lastSignInAt` (always set to `FieldValue.serverTimestamp()`).
- `.snapshots()` extension (from `firebase-firestore-ktx`) produces a Flow.

### 2.4 `FirebaseModule.kt`

```kotlin
@Module @InstallIn(SingletonComponent::class)
object FirebaseModule {
    @Provides @Singleton fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()
    @Provides @Singleton fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
}
```

### 2.5 `build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.arch.android.library)
    alias(libs.plugins.arch.android.hilt)
}
android { namespace = "com.rodiz.arch2.core.firebase" }
dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
}
```

Wire into `settings.gradle.kts`: `include(":core:firebase")`.

---

## 3. Session model update (`:core:session:domain`)

Extend `Session` to carry display info so HomeScreen can render it without an extra Firestore call:

```kotlin
data class Session(
    val userId: String,
    val token: String,
    val displayName: String? = null,
    val photoUrl: String? = null,
) {
    override fun toString() = "Session(userId=$userId, displayName=$displayName, token=[REDACTED])"
}
```

Update `SessionRepositoryImpl` in `:core:session:data` to persist + restore `displayName` and `photoUrl` (two new string keys, both nullable). Default fallback values on read for backwards compatibility.

---

## 4. Domain changes (`:feature:login:domain`)

### 4.1 `AuthRepository`

Add one method:

```kotlin
suspend fun signInWithGoogle(idToken: String): Try<Session, AuthError>
```

### 4.2 `AuthError`

Add cases for Google-specific failures:

```kotlin
data object GoogleSignInCancelled : AuthError
data object GoogleSignInFailed : AuthError       // generic / no Google account on device / network
```

### 4.3 New use case `SignInWithGoogleUseCase`

```kotlin
class SignInWithGoogleUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(idToken: String): Try<Session, AuthError> =
        repository.signInWithGoogle(idToken)
}
```

No new dependencies — pure Kotlin module stays pure Kotlin.

---

## 5. Data changes (`:feature:login:data`)

### 5.1 `build.gradle.kts`

Add deps:

```kotlin
implementation(project(":core:firebase"))
implementation(platform(libs.firebase.bom))
implementation(libs.firebase.auth.ktx)
implementation(libs.kotlinx.coroutines.play.services)
```

### 5.2 `AuthRepositoryImpl`

Inject `FirebaseAuth` and `UserProfileRepository`. Add:

```kotlin
override suspend fun signInWithGoogle(idToken: String): Try<Session, AuthError> = withContext(io) {
    try {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val authResult = firebaseAuth.signInWithCredential(credential).await()
        val user = authResult.user ?: return@withContext Try.Failure(AuthError.GoogleSignInFailed)
        val session = Session(
            userId = user.uid,
            token = user.getIdToken(false).await().token.orEmpty(),
            displayName = user.displayName,
            photoUrl = user.photoUrl?.toString(),
        )
        sessionRepository.save(session)
        userProfileRepository.upsertOnSignIn(
            UserProfile(
                uid = user.uid,
                email = user.email,
                displayName = user.displayName,
                photoUrl = user.photoUrl?.toString(),
                provider = "google",
            )
        )
        Try.Success(session)
    } catch (e: FirebaseAuthException) {
        Try.Failure(AuthError.GoogleSignInFailed)
    } catch (e: java.io.IOException) {
        Try.Failure(AuthError.NoNetwork)
    } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
        Try.Failure(AuthError.Unknown)
    }
}
```

The existing fake email/password `performLogin(...)` path is **unchanged** — still no Firebase, still hits the `FakeAuthRemoteDataSource`, still stores credentials in the vault for biometric.

---

## 6. Presentation changes (`:feature:login:presentation`)

### 6.1 New helper: `googlesignin/GoogleSignInLauncher.kt`

A small object (not Hilt-injected — it just wraps platform APIs) that runs the Credential Manager flow and returns an idToken:

```kotlin
object GoogleSignInLauncher {
    suspend fun launch(activity: Activity, serverClientId: String): Try<String, AuthError> = try {
        val manager = CredentialManager.create(activity)
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetGoogleIdOption.Builder()
                    .setServerClientId(serverClientId)
                    .setFilterByAuthorizedAccounts(false)
                    .setAutoSelectEnabled(true)
                    .build()
            )
            .build()
        val response = manager.getCredential(activity, request)
        val cred = response.credential
        if (cred is CustomCredential && cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            Try.Success(GoogleIdTokenCredential.createFrom(cred.data).idToken)
        } else {
            Try.Failure(AuthError.GoogleSignInFailed)
        }
    } catch (e: GetCredentialCancellationException) {
        Try.Failure(AuthError.GoogleSignInCancelled)
    } catch (e: NoCredentialException) {
        Try.Failure(AuthError.GoogleSignInFailed)
    } catch (e: GetCredentialException) {
        Try.Failure(AuthError.GoogleSignInFailed)
    }
}
```

Needs deps in `:feature:login:presentation/build.gradle.kts`:

```kotlin
implementation(libs.androidx.credentials)
implementation(libs.androidx.credentials.play.services)
implementation(libs.google.id)
```

### 6.2 `LoginUiState` / `LoginAction` / `LoginEvent`

- `LoginUiState`: no new fields (Google submit reuses `isSubmitting` + `transientError`).
- `LoginAction`: add `data object GoogleSignInRequested : LoginAction`.
- `LoginEvent`: add `data object PromptGoogleSignIn : LoginEvent`.

### 6.3 `LoginViewModel`

Inject `SignInWithGoogleUseCase`. Handle `LoginAction.GoogleSignInRequested` by emitting `LoginEvent.PromptGoogleSignIn` (the route launches the credential picker and feeds the idToken back). Add a new entry point `onGoogleIdToken(idToken: String)` that sets `isSubmitting = true`, calls the use case, and reuses `handleResult(...)` — same success → `NavigateHome` and failure → `transientError` machinery.

### 6.4 `LoginRoute`

On `LoginEvent.PromptGoogleSignIn`, get the `Activity` from `LocalContext.current` and call `GoogleSignInLauncher.launch(activity, context.getString(R.string.default_web_client_id))`. On `Try.Success(idToken)` call `viewModel.onGoogleIdToken(idToken)`. On `Try.Failure` set transientError directly via a new VM method `onGoogleSignInFailed(error)`. Pattern mirrors the existing `BiometricPromptManager` handling.

`R.string.default_web_client_id` is auto-generated by the google-services Gradle plugin from `google-services.json` after step 4 of the pre-implementation work. **No hard-coded ID.**

### 6.5 `LoginScreen` UI additions

Below the existing `PrimaryButton` (and after the optional biometric `OutlinedButton`), add:

```
Spacer(20.dp)
Row (or divider): horizontal line — Text("or") — horizontal line
Spacer(16.dp)
OutlinedButton "Continue with Google" → onAction(LoginAction.GoogleSignInRequested)
```

The Google button is an `OutlinedButton` with the Google "G" leading icon. Use a vector drawable `res/drawable/ic_google.xml` (4-color G) — small hand-crafted vector, or pull the official asset.

### 6.6 Strings (`res/values/strings.xml`)

```
<string name="login_or_divider">or</string>
<string name="login_continue_google">Continue with Google</string>
<string name="login_google_cancelled">Google sign-in was cancelled.</string>
<string name="login_google_failed">Couldn\'t sign in with Google. Try again.</string>
```

Map the two new `AuthError` cases in `ErrorMessages.kt`.

---

## 7. `:feature:login:data` Hilt module update

`AuthDataModule` already binds `AuthRepositoryImpl`. No change in shape — just make sure `AuthRepositoryImpl`'s new constructor params (`FirebaseAuth`, `UserProfileRepository`) are reachable. Both come from `:core:firebase`'s `FirebaseModule` and `UserProfileBindingsModule`, so as long as `:app` depends on `:core:firebase` (and it will once we add `implementation(project(":core:firebase"))` in `app/build.gradle.kts`), the graph resolves.

Add to `app/build.gradle.kts`:
```kotlin
implementation(project(":core:firebase"))
```

---

## 8. HomeScreen update (`:feature:home:presentation`)

`HomeScreen.kt:31` currently shows `stringResource(R.string.home_welcome, session?.userId.orEmpty())`. Change to prefer display name:

```kotlin
Text(stringResource(R.string.home_welcome, session?.displayName ?: session?.userId.orEmpty()))
```

(Optional polish: also render the photo via Coil if `session?.photoUrl != null`. Skip for v1 to keep scope tight.)

---

## 9. Firestore schema (committed to documentation)

`users/{uid}`:

| Field | Type | Source |
|---|---|---|
| `uid` | string | `FirebaseAuth.currentUser.uid` |
| `email` | string? | `FirebaseUser.email` |
| `displayName` | string? | `FirebaseUser.displayName` |
| `photoUrl` | string? | `FirebaseUser.photoUrl` |
| `provider` | string | `"google"` |
| `createdAt` | server timestamp | written only on first sign-in (merge will skip if present) |
| `lastSignInAt` | server timestamp | overwritten on every sign-in |

Rules already cover read/write isolation per user (step 6 of pre-implementation).

---

## 10. Files to modify / add

```
gradle/libs.versions.toml                                                          [edit] add credentials/google-id/firebase-auth+firestore/coroutines-play-services
settings.gradle.kts                                                                 [edit] include :core:firebase

core/firebase/build.gradle.kts                                                      [new]
core/firebase/src/main/kotlin/com/rodiz/arch2/core/firebase/FirebaseModule.kt       [new]
core/firebase/src/main/kotlin/com/rodiz/arch2/core/firebase/UserProfileRepository.kt[new]
core/firebase/src/main/kotlin/com/rodiz/arch2/core/firebase/UserProfileRepositoryImpl.kt [new]
core/firebase/src/main/kotlin/com/rodiz/arch2/core/firebase/UserProfileBindingsModule.kt [new]
core/firebase/src/main/kotlin/com/rodiz/arch2/core/firebase/model/UserProfile.kt    [new]

core/session/domain/src/main/kotlin/com/rodiz/arch2/core/session/domain/Session.kt  [edit] add displayName / photoUrl
core/session/data/src/main/kotlin/com/rodiz/arch2/core/session/data/SessionRepositoryImpl.kt [edit] persist new fields

feature/login/domain/src/main/kotlin/com/rodiz/arch2/feature/login/domain/model/AuthError.kt          [edit] add GoogleSignIn cases
feature/login/domain/src/main/kotlin/com/rodiz/arch2/feature/login/domain/repository/AuthRepository.kt [edit] add signInWithGoogle
feature/login/domain/src/main/kotlin/com/rodiz/arch2/feature/login/domain/usecase/SignInWithGoogleUseCase.kt [new]

feature/login/data/build.gradle.kts                                                 [edit] add :core:firebase + firebase-auth + coroutines-play-services
feature/login/data/src/main/kotlin/com/rodiz/arch2/feature/login/data/repository/AuthRepositoryImpl.kt [edit] inject FirebaseAuth + UserProfileRepository, implement signInWithGoogle

feature/login/presentation/build.gradle.kts                                         [edit] add credentials + credentials-play-services + google-id
feature/login/presentation/src/main/kotlin/com/rodiz/arch2/feature/login/presentation/googlesignin/GoogleSignInLauncher.kt [new]
feature/login/presentation/src/main/kotlin/com/rodiz/arch2/feature/login/presentation/state/LoginAction.kt  [edit] add GoogleSignInRequested
feature/login/presentation/src/main/kotlin/com/rodiz/arch2/feature/login/presentation/state/LoginEvent.kt   [edit] add PromptGoogleSignIn
feature/login/presentation/src/main/kotlin/com/rodiz/arch2/feature/login/presentation/viewmodel/LoginViewModel.kt [edit] inject SignInWithGoogleUseCase, handle Google path
feature/login/presentation/src/main/kotlin/com/rodiz/arch2/feature/login/presentation/screen/LoginRoute.kt  [edit] launch credential manager on PromptGoogleSignIn
feature/login/presentation/src/main/kotlin/com/rodiz/arch2/feature/login/presentation/screen/LoginScreen.kt [edit] add divider + Google button
feature/login/presentation/src/main/kotlin/com/rodiz/arch2/feature/login/presentation/screen/ErrorMessages.kt [edit] map new AuthError cases
feature/login/presentation/src/main/res/drawable/ic_google.xml                       [new]
feature/login/presentation/src/main/res/values/strings.xml                          [edit] new strings

feature/home/presentation/src/main/kotlin/com/rodiz/arch2/feature/home/presentation/HomeScreen.kt [edit] prefer displayName

app/build.gradle.kts                                                                [edit] implementation(project(":core:firebase"))
app/google-services.json                                                            [user replaces] after enabling Google sign-in + adding SHA-1s

plans/google-signin-firebase.md                                                     [new — moved from runtime plan file after approval]
```

---

## 11. Testing

### 11.1 Unit (pure JVM)

- `:feature:login:domain` — add `SignInWithGoogleUseCaseTest`: parameterized — for each `Try.Success`/`Try.Failure(AuthError.*)` the repo returns, the use case passes it through unchanged.

### 11.2 Presentation (JUnit5)

- `LoginViewModelTest` — three new tests:
  - `GoogleSignInRequested action emits PromptGoogleSignIn event`.
  - `onGoogleIdToken success emits NavigateHome and clears isSubmitting`.
  - `onGoogleIdToken failure sets transientError and clears isSubmitting` (parameterized over `GoogleSignInCancelled`, `GoogleSignInFailed`, `NoNetwork`, `Unknown`).
- Existing tests stay green — fake email/password flow is untouched.

### 11.3 Manual smoke (per the post-plan emulator memory)

1. `./gradlew :app:installDebug` → launch `com.rodiz.arch2.debug/com.rodiz.arch2.MainActivity`.
2. Tap **Continue with Google** → Credential Manager bottom sheet appears → pick a Google account on the emulator → land on Home with `Welcome, <Your Name>` (display name).
3. Sign out → land on Login → repeat with a different Google account → Home shows the new name. (Proves multi-user.)
4. Check Firebase Console → Firestore → `users` collection → one doc per uid, with `provider: "google"`, server-set timestamps.
5. Email/password still works as a fake: type any valid email + 8-char password → spinner → Home with `Welcome, <userId-derived-name>`.
6. Capture a screenshot via adb to `/tmp/login-google.png` and `/tmp/home-google.png`.

---

## 12. Out of scope (intentional)

- Email/password going through Firebase Auth (kept as fake per user decision).
- Sign-up flow (`CreateAccount` stays a stub).
- Forgot-password flow (`ForgotPassword` stays a stub; `sendPasswordResetEmail` is an easy follow-up).
- Linking accounts (Google + password for the same user).
- Token refresh handling (Firebase SDK refreshes silently; we don't surface failures).
- Firestore offline persistence tuning (default is on; we don't tweak it).
- Sign-in-with-other-providers (Apple, GitHub, etc.).
- Display name editing on `HomeScreen` (just rendering this iteration).
- Coil-loaded profile photo (deferred until the design system gets an `Avatar` component).
- Release-keystore SHA-1 in Firebase (debug only for now; add when there's a real release flow).

---

## 13. Risks

- **Web client ID resolution.** `R.string.default_web_client_id` only exists after the google-services plugin sees the merged Web OAuth client in `google-services.json`. If the user skips the "re-download after enabling Google sign-in" step, the resource is missing → compile error. Mitigation: documented loudly in §0 pre-implementation; the plan won't be marked done until a manual smoke test succeeds.
- **Two registered apps share one `google-services.json`.** Both `com.rodiz.arch2` and `com.rodiz.arch2.debug` need their OWN SHA-1 fingerprints added in Firebase Console (same debug keystore for both is fine since they're built from the same machine). If only one is added, the other build variant's Google sign-in returns `DEVELOPER_ERROR`.
- **`FirebaseAuth.getInstance()` persists session across launches.** With this change, the `runBlocking` startup decision in `StartDestinationModule` still uses our own `SessionRepository` (DataStore). They can drift if Firebase signs the user out (token revoked) while DataStore still holds a session — the app would land on Home and then break on the first Firestore read. Mitigation acceptable for v1: in `StartDestinationModule`, before returning `HomeHome`, also check `FirebaseAuth.getInstance().currentUser != null`. Documented but deferred to follow-up — would be cleanly enforced via a `SessionGuard` use case.
- **Credential Manager on emulators without Play Services.** API 36 system images usually include Play Services + a Google account picker. If the chosen AVD doesn't, the test fails with `NoCredentialException`. Use a Google Play system image AVD (the current `Medium_Phone_API_36.0` should work).
- **Firestore region permanence.** Once the database is created with a region, it can't be changed. Pick the closest one (e.g., `us-central1`, `nam5` multi-region, or one near you) on first creation.

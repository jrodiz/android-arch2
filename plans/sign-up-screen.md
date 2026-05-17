# Plan — Sign Up screen (name + email + password + avatar)

## Context

The login screen now has two top tabs (Sign In / Sign Up — see commit `0caeb15`). The Sign Up tab today is a placeholder: it reuses the Email + Password fields, and tapping **Register** emits `LoginEvent.ShowRegisterComingSoon` which the route surfaces as a Toast. No registration actually happens.

This plan wires registration end-to-end behind a dedicated **Sign Up screen** modeled on the reference mockup (full-screen, back arrow, card-style fields, primary "Register" pill button). The screen collects:

1. **First name** (required, ≥ 1 visible character after trim)
2. **Last name** (required, ≥ 1 visible character after trim)
3. **Email** (required, RFC-style malformed check — reuse `ValidateEmailUseCase`)
4. **Password** (required, ≥ 8 characters — reuse `ValidatePasswordUseCase`)
5. **Confirm password** (required, must equal `password`)
6. **Avatar photo** (optional) — sourced via the system **Photo Picker** (gallery) or the **camera** through `ActivityResultContracts.TakePicture`

On submit, the account is created in **FirebaseAuth (email/password)**, the avatar (if any) is uploaded to **Firebase Storage** at `users/{uid}/avatar.jpg`, and the resulting `displayName` + `photoUrl` are written into `users/{uid}` via the existing `UserProfileRepository`. On success the screen navigates to `HomeHome` exactly like sign-in does.

This builds **inside `:feature:login`** rather than spawning a new `:feature:signup` — authentication is one bounded context and the existing login `:nav`/`:domain`/`:data`/`:presentation` modules already hold every primitive we need (`AuthRepository`, `Session`, `UserProfile`, `ValidateEmailUseCase`, etc.). No new feature module.

The canonical scaffold spec is `plans/ANDROID_APP_SCAFFOLD_PROMPT.md`; the architectural invariants listed there (JVM-only `:nav`/`:domain`, no cross-feature `:presentation` deps, repository interfaces in `:domain` only) apply unchanged here.

### Critical pre-implementation step (user action)

The current `google-services.json` enables Auth (Google) and Firestore. **Email/password sign-in must be enabled separately in the Firebase console**:

1. **Firebase Console** → project `arch2-cac87` → **Build** → **Authentication** → **Sign-in method** → **Add new provider** → **Email/Password** → enable. Save.
2. **Build** → **Storage** → **Get started** → start in **production**. Pick the same region as Firestore (or `us-central1` if uncertain).
3. Storage rules (replace the default):
   ```
   rules_version = '2';
   service firebase.storage {
     match /b/{bucket}/o {
       match /users/{uid}/{allPaths=**} {
         allow read: if true;                                  // avatars are public
         allow write: if request.auth != null
                      && request.auth.uid == uid
                      && request.resource.size < 5 * 1024 * 1024
                      && request.resource.contentType.matches('image/.*');
       }
     }
   }
   ```
4. No new `google-services.json` download is needed — Auth/Storage are enabled per-project, not per-app.

---

## 1. UX spec — mapping to current theme

The reference mockup is monochrome (dark navy on white). For TinPet, recolor to the existing palette so it sits next to the login screen consistently. Use **only** tokens already in `core:designsystem` — no new colors, no new shapes, no new typography.

### 1.1 Layout (top → bottom)

| Region | Composable | Tokens / styling |
|---|---|---|
| Status-bar inset | — | Light icons (`isAppearanceLightStatusBars = false`) for the first 16dp while the back-arrow row sits over the coral wash; flip back on dispose like `LoginScreen` does (`LightStatusBarIconsWhileShown`). |
| Hero strip (≈ 120dp tall) | `BrandHeader(patternRes = R.drawable.ic_login_topographic, height = 120.dp)` | Reuse the same `BrandHeader` + `WaveBottomShape` used by Sign In, just **shorter** (120dp instead of 320dp) so it reads as an accent rather than dominating the screen. Pattern alpha stays at the default 0.28. |
| Back arrow + title | `Row` overlayed on the hero, padded `(16.dp top + status-bar inset, 16.dp horizontal)` | `IconButton(Icons.AutoMirrored.Outlined.ArrowBack)` tinted `BrandColors.CoralOnPattern` (white). Title `Text("Sign up")` centered, `MaterialTheme.typography.headlineSmall` with `FontWeight.ExtraBold`, color `BrandColors.CoralOnPattern`. |
| Avatar picker (≈ 96dp circle) | `AvatarPicker(state.avatarUri, onPick, onCapture, onClear)` | Centered. See §6.4. |
| Field card group | `Column` with `Modifier.padding(horizontal = 28.dp)` and `Arrangement.spacedBy(20.dp)` | Same horizontal padding as the login form so the two screens align. |
| First name | `BrandTextField` (`leadingIcon = Icons.Outlined.Person`) | Reuse `BrandTextField`; pass `KeyboardOptions(capitalization = Words, imeAction = Next)`. |
| Last name | `BrandTextField` (`leadingIcon = Icons.Outlined.Person`) | Same as above. |
| Email | `EmailField` from `core:ui` | Already mode-aware. |
| Password | `PasswordField` from `core:ui` | `imeAction = Next` (we have a confirm field after it — see §6.3 about overriding). |
| Confirm password | `PasswordField` (new variant — see §6.3) | `imeAction = Done`; pressing Done dispatches `SignUpAction.Submit`. |
| Error banner | `ErrorBanner` from `core:ui` | Only when `state.transientError != null`. |
| Register button | `PrimaryButton(text = "Register", loading = state.isSubmitting, enabled = state.canSubmit)` | Coral pill; matches the login Submit button verbatim — uses `AppShapes.large` already. |
| Bottom inset | `Modifier.windowInsetsPadding(WindowInsets.navigationBars)` | Match `LoginScreen`. |

### 1.2 Why this is consistent with the rest of the app

- **Coral primary** (`Color(0xFFE97A7A)`) for the Register pill, focused field underlines, and back-arrow ripple — same token used by the login button.
- **`AppShapes.large` (28dp)** for the Register pill — same shape used by `PrimaryButton`.
- **`BrandHeader` + `WaveBottomShape`** for the top strip — same coral wave used on `LoginScreen`, smaller height.
- **`BrandTextField` / `EmailField` / `PasswordField`** from `core:ui` — every field on the screen is one of these three components. No bespoke field styling.
- **`AppTypography` only** — no `FontFamily` imports, no custom sizes.

The reference's flat outlined cards aren't reused; we already have a coherent underlined-field style in `BrandTextField`, and introducing a second field style for one screen would fork the design system.

---

## 2. Integration with the login tabs

Two viable patterns. **Recommendation: B (the Sign Up tab opens the screen).**

| Option | Behavior | Tradeoffs |
|---|---|---|
| **A. Sign Up tab embeds the form** | Reuse the existing `LoginScreen` Sign Up tab body and inline every new field there. | Long scroll inside the tab; the avatar picker fights with the coral hero overhead; harder to land back on Sign In after canceling. |
| **B. Sign Up tab navigates to a full screen** *(recommended)* | Tapping the **Sign Up** tab on `LoginScreen` dispatches `LoginAction.ModeSelected(SignUp)` **plus** emits `LoginEvent.NavigateSignUp`, which the route turns into `navigator.goTo(SignUpHome)`. The Sign Up tab on `LoginScreen` becomes effectively a launcher. | Matches the mockup (back arrow). Keeps `LoginScreen` lean. Reusable from any future "Create account" CTA. |

Option B requires one additional event/route — no code is lost.

If A is preferred later, the same `RegisterUseCase`, `SignUpUiState`, and composables described below port over unchanged; only the entry point changes.

---

## 3. New / changed files

```
feature/login/
  nav/
    src/main/kotlin/com/rodiz/arch2/feature/login/nav/Routes.kt        // + SignUpHome
  domain/
    src/main/kotlin/com/rodiz/arch2/feature/login/domain/
      model/SignUpRequest.kt                                             // NEW
      model/ValidationError.kt                                           // + FirstNameEmpty, LastNameEmpty, ConfirmPasswordMismatch
      model/AuthError.kt                                                 // + EmailAlreadyInUse, WeakPassword (Firebase-specific)
      repository/AuthRepository.kt                                       // + register(request): Try<Session, AuthError>
      usecase/RegisterUseCase.kt                                         // NEW
      usecase/ValidateNameUseCase.kt                                     // NEW (reused for first + last)
      usecase/ValidateConfirmPasswordUseCase.kt                          // NEW
    src/test/kotlin/.../{RegisterUseCaseTest,ValidateNameUseCaseTest,
                          ValidateConfirmPasswordUseCaseTest}.kt          // NEW
  data/
    src/main/AndroidManifest.xml                                         // (no permission needed — see §5)
    src/main/kotlin/com/rodiz/arch2/feature/login/data/
      repository/AuthRepositoryImpl.kt                                   // implement register(...)
      remote/AvatarUploader.kt                                           // NEW — uploads to Firebase Storage, returns download URL
  presentation/
    src/main/AndroidManifest.xml                                         // + queries for camera intent (Android 11+)
    src/main/kotlin/com/rodiz/arch2/feature/login/presentation/
      state/SignUpUiState.kt                                             // NEW
      state/SignUpAction.kt                                              // NEW
      state/SignUpEvent.kt                                               // NEW
      viewmodel/SignUpViewModel.kt                                       // NEW
      screen/SignUpScreen.kt                                             // NEW (stateless)
      screen/SignUpRoute.kt                                              // NEW (hilt VM + photo picker wiring)
      screen/AvatarPicker.kt                                             // NEW — UI for the circular picker
      navigation/LoginNavModule.kt                                       // + entry<SignUpHome>
      navigation/ValidatorModule.kt                                      // + provideValidateName, provideValidateConfirmPassword
      screen/LoginScreen.kt                                              // tab tap → LoginAction.SignUpTabSelected (see §2)
      state/LoginAction.kt                                               // + SignUpTabSelected (or keep ModeSelected + a new event)
      state/LoginEvent.kt                                                // + NavigateSignUp
      viewmodel/LoginViewModel.kt                                        // emit NavigateSignUp when SignUp tab is chosen
    src/main/res/values/strings.xml                                      // see §11
    src/main/res/drawable/ic_camera.xml, ic_gallery.xml                  // NEW small icons for the picker sheet
    src/androidTest/kotlin/.../SignUpScreenHappyPathTest.kt              // NEW
    src/test/kotlin/.../SignUpViewModelTest.kt                           // NEW

app/
  src/main/AndroidManifest.xml                                           // + <uses-feature camera> (optional)

core/
  ui/
    src/main/kotlin/com/rodiz/arch2/core/ui/components/
      PasswordField.kt                                                   // accept ImeAction parameter (default Done) so confirm-password can pass Done while password passes Next
```

Module dependencies stay within the existing rules — the new files only deepen `:feature:login` and one `core:ui` tweak.

---

## 4. Domain model + validators

### 4.1 `SignUpRequest`

```kotlin
// :feature:login:domain
data class SignUpRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String,
    val avatarUri: String? = null,   // stringified content:// from the picker; null if skipped
) {
    val displayName: String get() = "${firstName.trim()} ${lastName.trim()}".trim()
}
```

`avatarUri` is `String?` (not `android.net.Uri`) so the `:domain` module stays Android-free per the scaffold invariant. The `:data` layer parses it via `Uri.parse(it)` at the boundary.

### 4.2 Validators (pure JVM, `:domain`)

```kotlin
class ValidateNameUseCase {
    operator fun invoke(value: String, field: NameField): ValidationError? = when {
        value.isBlank() -> if (field == NameField.First) ValidationError.FirstNameEmpty
                          else ValidationError.LastNameEmpty
        value.trim().length > MAX_LENGTH -> ValidationError.NameTooLong(MAX_LENGTH)
        else -> null
    }
    companion object { const val MAX_LENGTH = 50 }
}
enum class NameField { First, Last }

class ValidateConfirmPasswordUseCase {
    operator fun invoke(password: String, confirm: String): ValidationError? =
        if (confirm != password) ValidationError.ConfirmPasswordMismatch else null
}
```

### 4.3 `ValidationError` additions

```kotlin
sealed interface ValidationError {
    // existing: EmailEmpty, EmailMalformed, PasswordEmpty, PasswordTooShort
    data object FirstNameEmpty : ValidationError
    data object LastNameEmpty : ValidationError
    data class NameTooLong(val maxLength: Int) : ValidationError
    data object ConfirmPasswordMismatch : ValidationError
}
```

### 4.4 `AuthError` additions (Firebase email/password failure modes)

```kotlin
sealed interface AuthError {
    // existing: InvalidCredentials, NoNetwork, Timeout, Server(code), Unknown,
    //           GoogleSignInFailed, GoogleSignInCancelled
    data object EmailAlreadyInUse : AuthError
    data object WeakPassword : AuthError              // server rejects despite our 8-char min
    data object AvatarUploadFailed : AuthError        // soft-fail — account exists, avatar didn't upload
}
```

`AvatarUploadFailed` is **non-fatal**: the account exists, the user is signed in, only the picture didn't make it. The VM treats it as `transientError` after navigating home (or surfaces it as a Toast with a retry).

---

## 5. `AuthRepository.register(...)` — `:data` impl

### 5.1 Interface (`:domain`)

```kotlin
interface AuthRepository {
    // existing four methods …
    suspend fun register(request: SignUpRequest): Try<Session, AuthError>
}
```

### 5.2 Implementation outline (`:data`)

```kotlin
override suspend fun register(request: SignUpRequest): Try<Session, AuthError> = withContext(io) {
    try {
        val authResult = firebaseAuth
            .createUserWithEmailAndPassword(request.email, request.password)
            .await()
        val user = authResult.user ?: return@withContext Try.Failure(AuthError.Unknown)

        // Upload avatar first (if provided) so the profile write contains the final URL.
        val avatarUrl: String? = request.avatarUri?.let { uri ->
            avatarUploader.upload(user.uid, Uri.parse(uri)).getOrNull()
        }
        val avatarUploadFailed = request.avatarUri != null && avatarUrl == null

        // Set displayName / photoUrl on the FirebaseUser so subsequent
        // user.displayName reads (e.g. on Profile) match what's in Firestore.
        user.updateProfile(
            UserProfileChangeRequest.Builder()
                .setDisplayName(request.displayName)
                .setPhotoUri(avatarUrl?.let(Uri::parse))
                .build()
        ).await()

        val session = Session(
            userId = user.uid,
            token = user.getIdToken(false).await().token.orEmpty(),
            displayName = request.displayName,
            photoUrl = avatarUrl,
        )
        sessionRepository.save(session)
        userProfileRepository.upsertOnSignIn(
            UserProfile(
                uid = user.uid,
                email = request.email,
                displayName = request.displayName,
                photoUrl = avatarUrl,
                provider = "password",
            ),
        )

        if (avatarUploadFailed) {
            // Account created successfully — bubble up a soft warning the VM can surface.
            Try.Failure(AuthError.AvatarUploadFailed)
        } else {
            Try.Success(session)
        }
    } catch (e: FirebaseAuthUserCollisionException) {
        Try.Failure(AuthError.EmailAlreadyInUse)
    } catch (e: FirebaseAuthWeakPasswordException) {
        Try.Failure(AuthError.WeakPassword)
    } catch (e: FirebaseAuthInvalidCredentialsException) {
        Try.Failure(AuthError.InvalidCredentials)
    } catch (e: FirebaseNetworkException) {
        Try.Failure(AuthError.NoNetwork)
    } catch (e: IOException) {
        Try.Failure(AuthError.NoNetwork)
    } catch (@Suppress("TooGenericExceptionCaught") e: Throwable) {
        Try.Failure(AuthError.Unknown)
    }
}
```

Note the **two-step success model**: the VM treats `Try.Success(session)` as "navigate Home" and `Try.Failure(AuthError.AvatarUploadFailed)` as "navigate Home AND show a warning Snackbar". All other failures stay on the screen with the form populated.

### 5.3 `AvatarUploader` (`:data`)

```kotlin
@Singleton
internal class AvatarUploader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storage: FirebaseStorage,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    suspend fun upload(uid: String, source: Uri): Result<String> = withContext(io) {
        runCatching {
            val ref = storage.reference.child("users/$uid/avatar.jpg")
            context.contentResolver.openInputStream(source).use { stream ->
                requireNotNull(stream) { "Cannot open avatar source" }
                ref.putStream(stream).await()
            }
            ref.downloadUrl.await().toString()
        }
    }
}
```

Add `firebase-storage-ktx` to the version catalog and to `:feature:login:data`'s dependencies. `FirebaseStorage` is provided via a new `@Provides` in `core:firebase`'s `FirebaseModule`.

### 5.4 No new manifest permissions

- Photo Picker (`PickVisualMedia`) returns a temporary read-grant `content://media/...` URI **without any permission**.
- `TakePicture` writes to an app-owned `FileProvider` URI; no `CAMERA` permission is required when the contract is invoked.
- `<uses-feature android:name="android.hardware.camera" android:required="false" />` is *optional* in `app/AndroidManifest.xml` — declare it so the listing accurately reflects camera use.
- For the camera intent on Android 11+, add to `:feature:login:presentation`'s manifest:
  ```xml
  <queries>
      <intent>
          <action android:name="android.media.action.IMAGE_CAPTURE" />
      </intent>
  </queries>
  ```

A `file_provider_paths.xml` (in `res/xml/`) and matching `<provider>` declaration are needed so `TakePicture` has somewhere to write — see §6.4.

---

## 6. UI breakdown

### 6.1 `SignUpUiState`

```kotlin
data class SignUpUiState(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val passwordVisible: Boolean = false,
    val confirmPasswordVisible: Boolean = false,
    val avatarUri: String? = null,                            // content:// from picker
    val firstNameError: ValidationError? = null,
    val lastNameError: ValidationError? = null,
    val emailError: ValidationError? = null,
    val passwordError: ValidationError? = null,
    val confirmPasswordError: ValidationError? = null,
    val isSubmitting: Boolean = false,
    val transientError: AuthError? = null,
    val showAvatarSourceSheet: Boolean = false,
) {
    val canSubmit: Boolean
        get() = firstName.isNotBlank() &&
            lastName.isNotBlank() &&
            email.isNotBlank() &&
            password.isNotBlank() &&
            confirmPassword.isNotBlank() &&
            firstNameError == null &&
            lastNameError == null &&
            emailError == null &&
            passwordError == null &&
            confirmPasswordError == null &&
            !isSubmitting
}
```

### 6.2 `SignUpAction`

```kotlin
sealed interface SignUpAction {
    data class FirstNameChanged(val value: String) : SignUpAction
    data class LastNameChanged(val value: String) : SignUpAction
    data class EmailChanged(val value: String) : SignUpAction
    data class PasswordChanged(val value: String) : SignUpAction
    data class ConfirmPasswordChanged(val value: String) : SignUpAction
    data object TogglePasswordVisibility : SignUpAction
    data object ToggleConfirmPasswordVisibility : SignUpAction
    data object PickAvatarTapped : SignUpAction                          // opens bottom sheet
    data class AvatarSelected(val uri: String) : SignUpAction
    data object AvatarCleared : SignUpAction
    data object DismissAvatarSheet : SignUpAction
    data object Submit : SignUpAction
    data object BackTapped : SignUpAction
    data object DismissError : SignUpAction
}
```

### 6.3 `SignUpEvent`

```kotlin
sealed interface SignUpEvent {
    data object NavigateHome : SignUpEvent
    data object NavigateBack : SignUpEvent
    data object LaunchGalleryPicker : SignUpEvent
    data object LaunchCamera : SignUpEvent
    data class ShowSoftWarning(val message: SoftWarning) : SignUpEvent   // e.g. avatar upload failed
}

enum class SoftWarning { AvatarUploadFailed }
```

### 6.4 `AvatarPicker` composable

```
┌──────────────────────────────┐
│        96dp circle           │   (clipped via CircleShape)
│   ┌────────────────────┐     │
│   │   avatarUri image  │     │   AsyncImage(coil) if state.avatarUri != null
│   │        OR          │     │   else Icons.Outlined.AddAPhoto tint = onPrimary
│   │   add-photo icon   │     │
│   └────────────────────┘     │
│       on the bottom-right    │   small 28dp circle pencil edit badge if avatar set
│       a coral edit badge     │
└──────────────────────────────┘
```

Behavior:

- Tap circle → dispatch `PickAvatarTapped` → VM sets `showAvatarSourceSheet = true`.
- The screen renders a `ModalBottomSheet` with two list items:
  - **Choose from gallery** → dispatch through a `LocalAvatarSourceHandler` (see §7) → triggers `rememberLauncherForActivityResult(PickVisualMedia())`.
  - **Take a photo** → dispatch → triggers `rememberLauncherForActivityResult(TakePicture())` with a pre-created `FileProvider` URI under `Context.cacheDir/avatars/cap-{ts}.jpg`.
- When either contract returns a non-null URI → dispatch `AvatarSelected(uri.toString())`.
- A small "Remove" text button under the circle dispatches `AvatarCleared` when an avatar is set.

The picker is **stateless** — it takes `(avatarUri, onPickTapped, onCleared)` and emits actions. All side effects live in `SignUpRoute`.

### 6.5 `PasswordField` `core:ui` change

Add an `imeAction: ImeAction = ImeAction.Done` parameter and pass it through to `KeyboardOptions`. Existing callers (`LoginScreen` → `PasswordField`) keep the default. The new confirm-password field passes `ImeAction.Done`; the regular password field on Sign Up passes `ImeAction.Next`. The existing `onImeDone` callback continues to fire on Done.

---

## 7. `SignUpRoute` — wiring photo picker + camera contracts

```kotlin
@Composable
fun SignUpRoute(
    onNavigateHome: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: SignUpViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Pre-create the camera destination URI lazily — only on first LaunchCamera.
    val pendingCameraUri = remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) viewModel.onAction(SignUpAction.AvatarSelected(uri.toString()))
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val uri = pendingCameraUri.value
        if (success && uri != null) viewModel.onAction(SignUpAction.AvatarSelected(uri.toString()))
        pendingCameraUri.value = null
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                SignUpEvent.NavigateHome -> onNavigateHome()
                SignUpEvent.NavigateBack -> onNavigateBack()
                SignUpEvent.LaunchGalleryPicker ->
                    galleryLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
                SignUpEvent.LaunchCamera -> {
                    val target = createTempAvatarUri(context)        // FileProvider getUriForFile
                    pendingCameraUri.value = target
                    cameraLauncher.launch(target)
                }
                is SignUpEvent.ShowSoftWarning ->
                    Toast.makeText(context, event.message.localized(context), Toast.LENGTH_LONG).show()
            }
        }
    }

    SignUpScreen(state = state, onAction = viewModel::onAction)
}
```

`createTempAvatarUri` writes a 0-byte placeholder under `cacheDir/avatars/cap-{ts}.jpg` and returns `FileProvider.getUriForFile(...)`. The FileProvider authority is `${applicationId}.fileprovider` (single declaration in `:app`'s manifest pointing at `xml/file_provider_paths.xml`).

### 7.1 `xml/file_provider_paths.xml` (in `:app`)

```xml
<paths>
    <cache-path name="avatars" path="avatars/" />
</paths>
```

### 7.2 `:app` manifest provider

```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_provider_paths" />
</provider>
```

---

## 8. `SignUpViewModel`

```kotlin
@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val registerUseCase: RegisterUseCase,
    private val validateName: ValidateNameUseCase,
    private val validateEmail: ValidateEmailUseCase,
    private val validatePassword: ValidatePasswordUseCase,
    private val validateConfirmPassword: ValidateConfirmPasswordUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SignUpUiState())
    val state: StateFlow<SignUpUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<SignUpEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<SignUpEvent> = _events.asSharedFlow()

    fun onAction(action: SignUpAction) = when (action) {
        is SignUpAction.FirstNameChanged -> onFirstNameChanged(action.value)
        is SignUpAction.LastNameChanged -> onLastNameChanged(action.value)
        is SignUpAction.EmailChanged -> onEmailChanged(action.value)
        is SignUpAction.PasswordChanged -> onPasswordChanged(action.value)
        is SignUpAction.ConfirmPasswordChanged -> onConfirmChanged(action.value)
        SignUpAction.TogglePasswordVisibility ->
            _state.update { it.copy(passwordVisible = !it.passwordVisible) }
        SignUpAction.ToggleConfirmPasswordVisibility ->
            _state.update { it.copy(confirmPasswordVisible = !it.confirmPasswordVisible) }
        SignUpAction.PickAvatarTapped ->
            _state.update { it.copy(showAvatarSourceSheet = true) }
        SignUpAction.DismissAvatarSheet ->
            _state.update { it.copy(showAvatarSourceSheet = false) }
        is SignUpAction.AvatarSelected ->
            _state.update { it.copy(avatarUri = action.uri, showAvatarSourceSheet = false) }
        SignUpAction.AvatarCleared ->
            _state.update { it.copy(avatarUri = null) }
        SignUpAction.Submit -> submit()
        SignUpAction.BackTapped -> tryEmit(SignUpEvent.NavigateBack)
        SignUpAction.DismissError -> _state.update { it.copy(transientError = null) }
    }

    private fun submit() {
        val current = _state.value
        val errs = mapOf(
            "first" to validateName(current.firstName, NameField.First),
            "last" to validateName(current.lastName, NameField.Last),
            "email" to validateEmail(current.email),
            "password" to validatePassword(current.password),
            "confirm" to validateConfirmPassword(current.password, current.confirmPassword),
        )
        if (errs.values.any { it != null }) {
            _state.update {
                it.copy(
                    firstNameError = errs["first"],
                    lastNameError = errs["last"],
                    emailError = errs["email"],
                    passwordError = errs["password"],
                    confirmPasswordError = errs["confirm"],
                )
            }
            return
        }
        _state.update { it.copy(isSubmitting = true, transientError = null) }
        viewModelScope.launch {
            val result = registerUseCase(
                SignUpRequest(
                    firstName = current.firstName.trim(),
                    lastName = current.lastName.trim(),
                    email = current.email.trim(),
                    password = current.password,
                    avatarUri = current.avatarUri,
                ),
            )
            handleResult(result)
        }
    }

    private fun handleResult(result: Try<Session, AuthError>) {
        when (result) {
            is Try.Success -> {
                _state.update { it.copy(isSubmitting = false) }
                tryEmit(SignUpEvent.NavigateHome)
            }
            is Try.Failure -> when (result.error) {
                AuthError.AvatarUploadFailed -> {
                    // Account succeeded; only the avatar didn't upload.
                    _state.update { it.copy(isSubmitting = false) }
                    tryEmit(SignUpEvent.ShowSoftWarning(SoftWarning.AvatarUploadFailed))
                    tryEmit(SignUpEvent.NavigateHome)
                }
                else -> _state.update {
                    it.copy(isSubmitting = false, transientError = result.error)
                }
            }
        }
    }

    // per-field validation on change — clear errors as the user fixes them
    private fun onFirstNameChanged(v: String) = _state.update {
        it.copy(firstName = v, firstNameError = if (v.isEmpty()) null else validateName(v, NameField.First))
    }
    // (analogous for last, email, password, confirm — confirm re-runs on password change too)
}
```

### 8.1 Image saved-state survival

Persist `firstName`, `lastName`, `email`, and `avatarUri` to `SavedStateHandle` the same way `LoginViewModel` persists email today (KEY_FOO). Password fields are **not** saved (sensitive).

---

## 9. Navigation wiring

### 9.1 `Routes.kt` (in `:feature:login:nav`)

```kotlin
@Serializable data object LoginHome
@Serializable data object ForgotPassword
@Serializable data object SignUpHome                     // NEW
```

### 9.2 `LoginNavModule.kt`

```kotlin
entry<SignUpHome> {
    SignUpRoute(
        onNavigateHome = { navigator.replaceAll(HomeHome) },
        onNavigateBack = { navigator.goBack() },
    )
}
```

### 9.3 `LoginScreen` tab-tap behavior

Add a new event `LoginEvent.NavigateSignUp`. In `LoginViewModel.onAction(ModeSelected(SignUp))`, emit `NavigateSignUp` instead of just flipping `mode`. The `LoginRoute` translates it to `navigator.goTo(SignUpHome)`.

When the user navigates back, the login screen still shows Sign In selected (no mode persistence needed — the navigation IS the disclosure). Remove the now-unused `LoginEvent.ShowRegisterComingSoon` + the dummy Register submit branch in `LoginViewModel.submit()`.

The `LoginUiState.mode` field can stay (it still controls Forgot link visibility etc., though after this change `mode` only ever holds `SignIn` in practice). For tidiness, **delete `LoginMode` and the mode-conditional branches in `LoginScreen`** — the screen becomes purely Sign-In with one "Sign up" tab that's a launcher button styled identically to the active Sign-In tab look-and-feel **but without underline accent**. The simplest implementation: render two tab buttons; tap on Sign In = no-op (or re-validate); tap on Sign Up = navigate.

---

## 10. UI: `SignUpScreen.kt`

```kotlin
@Composable
fun SignUpScreen(
    state: SignUpUiState,
    onAction: (SignUpAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    LightStatusBarIconsWhileShown()                              // identical helper to LoginScreen
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        Box(Modifier.fillMaxWidth().height(120.dp)) {
            BrandHeader(patternRes = R.drawable.ic_login_topographic, modifier = Modifier.matchParentSize())
            Row(
                modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { onAction(SignUpAction.BackTapped) }) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.common_back),
                        tint = BrandColors.CoralOnPattern,
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.signup_title),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                    color = BrandColors.CoralOnPattern,
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(48.dp))                    // visual balance for the back icon
            }
        }

        Spacer(Modifier.height(20.dp))

        AvatarPicker(
            avatarUri = state.avatarUri,
            onPickTapped = { onAction(SignUpAction.PickAvatarTapped) },
            onCleared = { onAction(SignUpAction.AvatarCleared) },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Spacer(Modifier.height(28.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .padding(bottom = 32.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            state.transientError?.let { error ->
                ErrorBanner(
                    message = error.localized(),
                    onDismiss = { onAction(SignUpAction.DismissError) },
                    dismissContentDescription = stringResource(R.string.login_error_dismiss),
                )
            }

            BrandTextField(
                value = state.firstName,
                onValueChange = { onAction(SignUpAction.FirstNameChanged(it)) },
                label = stringResource(R.string.signup_first_name),
                placeholder = stringResource(R.string.signup_first_name_placeholder),
                leadingIcon = Icons.Outlined.Person,
                errorMessage = state.firstNameError?.localized(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
                fieldModifier = Modifier.testTag("signup_first_name_field"),
            )

            BrandTextField(/* lastName — analogous */)

            EmailField(value = state.email, onValueChange = { onAction(SignUpAction.EmailChanged(it)) }, …)

            PasswordField(
                value = state.password,
                onValueChange = { onAction(SignUpAction.PasswordChanged(it)) },
                visible = state.passwordVisible,
                onToggleVisibility = { onAction(SignUpAction.TogglePasswordVisibility) },
                imeAction = ImeAction.Next,                      // NEW parameter — see §6.5
                onImeDone = {},
                errorMessage = state.passwordError?.localized(),
                …,
            )

            PasswordField(
                value = state.confirmPassword,
                onValueChange = { onAction(SignUpAction.ConfirmPasswordChanged(it)) },
                visible = state.confirmPasswordVisible,
                onToggleVisibility = { onAction(SignUpAction.ToggleConfirmPasswordVisibility) },
                imeAction = ImeAction.Done,
                onImeDone = { onAction(SignUpAction.Submit) },
                errorMessage = state.confirmPasswordError?.localized(),
                label = stringResource(R.string.signup_confirm_password),
                …,
            )

            Spacer(Modifier.height(8.dp))

            PrimaryButton(
                text = stringResource(R.string.signup_submit),
                loading = state.isSubmitting,
                enabled = state.canSubmit,
                onClick = { onAction(SignUpAction.Submit) },
                testTag = "signup_submit",
            )
        }
    }

    if (state.showAvatarSourceSheet) {
        AvatarSourceSheet(
            onDismiss = { onAction(SignUpAction.DismissAvatarSheet) },
            onGallery = { /* route emits LaunchGalleryPicker via VM */ },
            onCamera = { /* route emits LaunchCamera via VM */ },
        )
    }
}
```

The `AvatarSourceSheet` is a `ModalBottomSheet` (Material 3) with two `ListItem`s. To keep the screen stateless, the sheet's gallery/camera actions go through the VM (`SignUpAction` → emits `LaunchGalleryPicker`/`LaunchCamera` events for the route to consume).

---

## 11. Strings (`feature/login/presentation/src/main/res/values/strings.xml`)

```xml
<string name="signup_title">Sign up</string>
<string name="signup_first_name">First name</string>
<string name="signup_first_name_placeholder">e.g. Steve</string>
<string name="signup_last_name">Last name</string>
<string name="signup_last_name_placeholder">e.g. Rogers</string>
<string name="signup_confirm_password">Confirm password</string>
<string name="signup_submit">Register</string>
<string name="signup_avatar_picker_cd">Choose profile photo</string>
<string name="signup_avatar_clear">Remove photo</string>
<string name="signup_avatar_source_title">Add a profile photo</string>
<string name="signup_avatar_source_gallery">Choose from gallery</string>
<string name="signup_avatar_source_camera">Take a photo</string>
<string name="signup_warning_avatar_upload_failed">Profile created, but we couldn\'t upload your photo. You can try again from your profile.</string>
<string name="signup_validation_first_name_empty">First name is required.</string>
<string name="signup_validation_last_name_empty">Last name is required.</string>
<string name="signup_validation_name_too_long">Name can\'t be longer than %1$d characters.</string>
<string name="signup_validation_confirm_mismatch">Passwords don\'t match.</string>
<string name="signup_error_email_in_use">An account with that email already exists.</string>
<string name="signup_error_weak_password">That password is too weak — pick a stronger one.</string>
```

Extend the existing `ValidationError.localized()` / `AuthError.localized()` helpers in `ErrorMessages.kt` with the new cases.

---

## 12. Accessibility

- Every field is a `BrandTextField` / `EmailField` / `PasswordField` — labels are already external `Text`s merged into the field's semantics (`semantics(mergeDescendants = true)`).
- Back arrow `contentDescription = stringResource(R.string.common_back)`.
- Avatar picker circle `contentDescription = stringResource(R.string.signup_avatar_picker_cd)` whether or not an avatar is set; if set, append "Selected" via `stateDescription`.
- Bottom sheet items are standard `ListItem`s with text-based labels.
- All error text uses `MaterialTheme.colorScheme.error` (TalkBack reads error state for `BrandTextField` via `isError`).
- Touch targets ≥ 48dp: avatar circle is 96dp, IconButtons are Material defaults.

---

## 13. Tests

### 13.1 `:feature:login:domain` (JVM unit tests)

- `ValidateNameUseCaseTest` — empty/blank, too-long, valid.
- `ValidateConfirmPasswordUseCaseTest` — match/mismatch, both empty.
- `RegisterUseCaseTest` — happy + each `AuthError` passthrough (mock `AuthRepository`).
- Extend `ValidateEmailUseCaseTest` — no changes; verifies reuse.

### 13.2 `:feature:login:presentation` (JVM unit tests)

`SignUpViewModelTest`:

- All-blank submit emits per-field validation errors.
- Mismatched confirm surfaces `ConfirmPasswordMismatch`.
- Valid form + success → emits `NavigateHome`, clears `isSubmitting`.
- Each `AuthError` populates `transientError` and clears `isSubmitting`.
- `AuthError.AvatarUploadFailed` emits `ShowSoftWarning(AvatarUploadFailed)` AND `NavigateHome`.
- `AvatarSelected` populates `state.avatarUri` and clears `showAvatarSourceSheet`.
- `AvatarCleared` resets `state.avatarUri` to `null`.
- `PickAvatarTapped` flips `showAvatarSourceSheet = true`; `DismissAvatarSheet` flips it back.
- `SavedStateHandle` persists `firstName/lastName/email/avatarUri` across VM recreation; password fields do NOT persist.

### 13.3 `:feature:login:presentation` (Compose UI test)

`SignUpScreenHappyPathTest` (analogous to `LoginScreenHappyPathTest`):

- Type into each field via `onNodeWithTag(...).performTextInput(...)`.
- Assert `login_submit` (signup_submit) is initially disabled.
- Fill all five fields → `assertIsEnabled`.
- `performClick()` → assert `submitDispatched`.
- Type mismatched confirm → button stays disabled and the error message is visible.

(Don't test the avatar picker in the Compose UI test — it requires real launcher contracts; cover the action-dispatch side of `PickAvatarTapped` / `AvatarSelected` in the VM test instead.)

---

## 14. Version catalog additions

```
[versions]
firebaseStorage = "21.0.1"      # tracks the BoM, but pin explicitly so the dep is discoverable in libs.versions.toml
                                # (in practice prefer the BoM version — the firebase-bom group will line it up)

[libraries]
firebase-storage-ktx           = { group = "com.google.firebase", name = "firebase-storage-ktx" }   # BoM-versioned
coil-compose                   = (already present)
```

Add `firebase-storage-ktx` to `:feature:login:data` and `:core:firebase` (FirebaseStorage provider). Coil is already in the catalog and is used by `AsyncImage` in the avatar picker.

---

## 15. Out of scope (intentional)

- **Email verification email** — `FirebaseUser.sendEmailVerification()` flow + a "Verify your email" gate before Home. Track as a follow-up.
- **Resending verification / "Already have an account? Sign in"** link on the screen — the back arrow plus the login tabs cover both.
- **Image cropping / rotation** — the picker returns the source URI as-is. Add `androidx.core:core-ktx` `ImageDecoder.createSource` + manual crop later if product wants square enforcement before upload (`AvatarUploader` would just compress before `putStream`).
- **Avatar progress UI** — the Register button shows the global submit spinner; a separate per-image progress bar is not in this plan.
- **Real password strength meter** — minimum-8-chars is the contract; richer scoring (zxcvbn) is a separate plan.
- **Phone number / OAuth provider variants** — Google sign-in already exists on the login screen and stays the only OAuth path for now.
- **Account-deletion / GDPR right-to-be-forgotten** — separate flow, lives behind Profile, not Sign Up.

---

## 16. Verification checklist (post-implementation)

- `./gradlew :feature:login:presentation:test :feature:login:domain:test :app:assembleDebug` green.
- `:feature:login:domain:dependencies --configuration runtimeClasspath` shows no `androidx.*` — proves the new use cases stayed JVM-only.
- Manual on emulator (per `feedback_post_plan_emulator.md`):
  1. Launch app → Sign In tab visible.
  2. Tap **Sign Up** tab → navigates to the new screen (not a toast).
  3. Back arrow → returns to Sign In.
  4. Type each field, mismatch the confirm → error renders inline; submit disabled.
  5. Match confirm → submit enables.
  6. Tap avatar circle → bottom sheet appears with Gallery + Camera. Pick a gallery image → circle shows it; "Remove" button appears.
  7. Submit → spinner → app navigates to Home, profile shows name and avatar.
  8. Try registering again with the same email → `EmailAlreadyInUse` error banner; form keeps content.
  9. Pull `users/{uid}` from Firestore — verifies `displayName`, `photoUrl`, `provider = "password"`.
- Screenshot of the empty Sign Up screen and one of the filled-with-avatar state, surfaced in the post-implementation summary.

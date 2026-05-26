# Plan — Capture location during Sign Up (combined permissions onboarding)

> **NOTE on plan location:** project convention ([[feedback_plans_location]]) is to save plans under `plans/` in the Arch2.0 repo. Plan mode pins the working copy here. **Step 0 of implementation: copy this file to `plans/onboarding-permissions-screen.md`** so it gets versioned with the code.

## 1. Context

Location is a core matching primitive — Deck queries by distance from the signed-in user. Today, **a brand-new account has no location** because:

- Sign Up writes only Firebase Auth + `UserProfileRepository.upsertOnSignIn(...)` (email + displayName + photoUrl); no `owners/{uid}` location field is set (`feature/login/data/.../AuthRepositoryImpl.kt:115-123`).
- The only path to set `owners/{uid}.location` is **Settings → Edit Profile → "Add your city" → Update** (`feature/settings/presentation/.../EditProfileScreen.kt:133-170`).
- Most users never visit Settings on day one, so the Deck shows the petless / location-missing guard even when the user is otherwise fully set up.

The user wants location captured **during onboarding** so it just works. The post-signup chain today is:

```
SignUp success → replaceAll(NotificationRationaleOnboarding) → tap Allow/Skip → replaceAll(DeckHome)
                  └─ feature/login/.../LoginNavModule.kt:40-48
                  └─ feature/notifications/.../NotificationsNavModule.kt:21-28
```

**Confirmed scope (this session):**

1. **Combined permissions screen** — one "Set up TinPet" screen with two rows (Location + Notifications) replaces the standalone NotificationRationaleOnboarding step. Saves the user one screen tap and groups onboarding gates together.
2. **Skippable** — Skip / Deny lands the user on Deck anyway. The existing Deck location guard handles the "no location" case. Edit Profile keeps its location row for users who want to add it later.
3. The standalone `NotificationRationale` route (entered from Settings → Notifications) **stays as-is** — notifications-only. Only the post-signup chain swaps to the combined screen.

**Non-goals:** Removing the EditProfile location section; making location required to use the app; new "high-accuracy" permission (we stay on `ACCESS_COARSE_LOCATION`); cross-device sync of the chosen city; a "re-prompt later" gentle reminder if the user skipped.

## 2. Final flow

```
SignUp success → replaceAll(PermissionsOnboarding)
  ┌─ User taps "Allow location"      → permission prompt → on grant: fetch + write
  ├─ User taps "Allow notifications" → permission prompt → on grant: FcmTokenSync
  └─ User taps "Continue"            → replaceAll(DeckHome)
                                       (works whether both, one, or neither was granted)
```

Both permission requests are independent. "Continue" is always enabled — it's not gated on either grant. The visual state of each row reflects whether the permission has been granted (filled checkmark, "Allowed" sublabel) or not (outline icon, default label).

## 3. Module layout (where the screen lives)

The new screen lives in `:feature:notifications:{nav,presentation}` — same module that owns the existing NotificationRationale screens, because:

- `:feature:notifications:presentation` already imports `feature:deck:nav` to `replaceAll(DeckHome)` after the rationale.
- It already injects `FcmTokenSync` for the notification permission grant.
- Onboarding gates are conceptually "notifications + adjacent" rather than a new bounded context.

**Cross-feature location-write challenge:** the existing `UpdateLocationUseCase` lives in `:feature:profile:domain`, but `:feature:notifications:presentation` **cannot depend on another feature's `:domain`** (architecture rule: `[[feedback_cross_feature_display_pattern]]`). The clean fix is to add `updateLocation(...)` to the existing `UserProfileRepository` in `:core/firebase` (which notifications:presentation already depends on for `FcmTokenSync`). This is consistent with what `UserProfileRepository.upsertOnSignIn(...)` already does — write owner-profile fields directly.

Settings/Profile continues to use its existing `UpdateLocationUseCase` → `FirestoreOwnerProfileRepository.updateLocation(...)` chain unchanged. Both paths write to the same `owners/{uid}` doc with the same fields, so there's no schema drift.

### Module-rename consideration (deferred)

If we ever add more onboarding gates (terms acceptance, marketing opt-in, etc.), splitting out `:feature:onboarding:{nav,presentation}` becomes worth it. For now (2 gates, both already touched by `:feature:notifications`), the rename isn't justified — but the screen file name is `PermissionsOnboardingScreen.kt` (not `NotificationsRationaleScreen.kt`) so it's already grouped under the right conceptual name when the module split happens.

## 4. Screen design

**Route:** `@Serializable data object PermissionsOnboarding` in `feature/notifications/nav/.../Routes.kt`. The existing `NotificationRationale` route stays for the Settings entry point. The existing `NotificationRationaleOnboarding` route is **deleted** (replaced by `PermissionsOnboarding` in the post-signup chain).

**Layout** (top to bottom):

- Status-bar padding.
- 64dp coral icon tile (`Icons.Outlined.Tune` or paw motif) — centered, matches existing rationale screens.
- Headline: "**Set up TinPet**" / "**Configura TinPet**" — `headlineMedium ExtraBold`, centered, `semantics { heading() }`.
- Body: "Two quick permissions and you're matching." / "Dos permisos rápidos y estás listo para hacer match." — `bodyMedium`, `onSurfaceVariant`, centered.
- Spacer 28dp.
- **Location row** — see §4.1.
- Spacer 12dp.
- **Notifications row** — see §4.2.
- Spacer 28dp.
- `PrimaryButton(text = "Continue", enabled = true, onClick = onDone)` — always enabled.
- Spacer 8dp.
- `TextButton("Maybe later")` — same `onDone`. Optional escape valve; clicking either button has the same effect (lands on Deck). Could collapse to a single CTA if we want — kept for now because "Skip everything" is a clearer signal than "Continue with nothing granted".

### 4.1. Location row

`Surface(shape = RoundedCornerShape(16.dp), color = BrandColors.PeachWarmLight)` with:

- `Icons.Outlined.LocationOn` (24dp, `BrandColors.CoralDeep`) in a 44dp peach-tint circle.
- Title: "Find pets nearby" / "Encuentra mascotas cerca". `titleMedium SemiBold`.
- Subtitle (dynamic):
  - Before grant: "Use your approximate location to show pets close to you." / "Usa tu ubicación aproximada para mostrarte mascotas cerca."
  - On grant + city resolved: "Allowed · `<cityLabel>`" / "Activado · `<cityLabel>`"
  - On grant + city unknown (reverse geocode failed): "Allowed" / "Activado"
  - On grant + working (fetch in flight): "Getting your area…" / "Obteniendo tu zona…"
  - On deny: "Denied — you can change this in Settings later." / "Denegado — puedes cambiarlo en Ajustes."
- Trailing affordance: a coral `OutlinedButton` "Allow" / "Permitir" when no permission yet, replaced by a `Icons.Filled.CheckCircle` (mint) when granted. Disabled while in-flight (subtitle shows the spinner copy).

Tapping the row (or the Allow button) → `permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)`. On grant: VM's `onLocationPermissionGranted()` runs → `FusedLocationProviderClient.lastLocation.await()` (mirroring `EditProfileScreen.fetchAndSaveLocation()`) → `reverseGeocode(lat, lng)` → `userProfileRepository.updateLocation(lat, lng, cityLabel)` (the new method in §5.2) → state.locationStatus = Allowed(cityLabel). On deny → state.locationStatus = Denied.

### 4.2. Notifications row

Same shape as §4.1 but using `Icons.Outlined.Notifications` + `BrandColors.LavenderTint` / `LavenderInk`. Title: "Match alerts" / "Avisos de matches". Subtitle:

- Before grant: "Get pinged for matches and messages." / "Recibe avisos de matches y mensajes."
- On grant: "Allowed" / "Activado"
- On deny: "Denied — enable any time from Settings → Notifications." / "Denegado — actívalo desde Ajustes → Notificaciones."

Tap → `permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)` (only on API 33+; on lower API the row renders "Allowed" with no tap target). On grant: VM's `onNotificationsPermissionGranted()` → `FcmTokenSync.syncForSignedInUser()` (mirroring the existing `NotificationRationaleViewModel.onPermissionGranted()`).

### 4.3. Empty / loading / error states

- The screen is stateless across recompositions — re-entering wouldn't normally happen because the route is `replaceAll`-only.
- If `FusedLocationProviderClient.lastLocation` returns null (no last known location), the subtitle reads "Location unavailable — try outdoors with GPS on." (existing string `edit_profile_location_unavailable`).
- All errors are best-effort: they update the row's subtitle but don't block the Continue button.

## 5. State / behavior

### 5.1. New ViewModel

`PermissionsOnboardingViewModel` in `:feature:notifications:presentation`:

```kotlin
internal data class PermissionsOnboardingUiState(
    val location: PermissionStatus = PermissionStatus.NotRequested,
    val notifications: PermissionStatus = PermissionStatus.NotRequested,
    val errorMessage: String? = null,
)

internal sealed interface PermissionStatus {
    data object NotRequested : PermissionStatus
    data object Working : PermissionStatus           // fetch in flight after grant
    data class Allowed(val cityLabel: String? = null) : PermissionStatus
    data object Denied : PermissionStatus
}

@HiltViewModel
internal class PermissionsOnboardingViewModel @Inject constructor(
    private val userProfileRepository: UserProfileRepository,
    private val fcmTokenSync: FcmTokenSync,
    private val crashReporter: CrashReporter,
    @IoDispatcher private val io: CoroutineDispatcher,
) : ViewModel() {
    val state = MutableStateFlow(PermissionsOnboardingUiState()).asStateFlow()

    fun onLocationGranted(lat: Double, lng: Double, cityLabel: String?) { … }
    fun onLocationDenied() = update { it.copy(location = Denied) }
    fun onLocationFetchFailed(msgRes: Int) = update { … }

    fun onNotificationsGranted() { … }    // calls FcmTokenSync, logs failure
    fun onNotificationsDenied() = update { it.copy(notifications = Denied) }
}
```

The VM does **not** own the permission launcher or the FusedLocationProvider — those live in the Route, mirroring the existing EditProfile pattern. The Route calls into the VM with the resolved lat/lng/cityLabel (or the denial signal).

### 5.2. New `UserProfileRepository.updateLocation(...)`

Add to `core/firebase/.../UserProfileRepository.kt` (interface) and `UserProfileRepositoryImpl.kt`:

```kotlin
suspend fun updateLocation(lat: Double, lng: Double, cityLabel: String?)
```

Implementation mirrors `FirestoreOwnerProfileRepository.updateLocation(...)` exactly:

```kotlin
override suspend fun updateLocation(lat: Double, lng: Double, cityLabel: String?) = withContext(io) {
    val uid = firebaseAuth.currentUser?.uid ?: error("No signed-in user")
    val now = Clock.System.now()
    firestore.collection("owners").document(uid).set(
        mapOf(
            "location" to FirestoreGeoPoint(lat, lng),
            "geohash" to Geohash.encode(lat, lng, precision = 6),
            "cityLabel" to cityLabel,
            "updatedAt" to now.toTimestamp(),
            "createdAt" to now.toTimestamp(),
        ),
        SetOptions.merge(),
    ).await()
}
```

This is duplication with `FirestoreOwnerProfileRepository.updateLocation(...)` (same Firestore write, same fields). The duplication is intentional and called out: hoisting `UpdateLocationUseCase` into `:core:profile:domain` would be a bigger refactor and isn't justified by one extra caller. If a third caller appears, do the hoist.

### 5.3. Route — wires permission launchers + FusedLocationProvider

`PermissionsOnboardingRoute` parallels `EditProfileRoute`'s location plumbing:

```kotlin
@Composable
internal fun PermissionsOnboardingRoute(
    onDone: () -> Unit,
    viewModel: PermissionsOnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationClient = remember { LocationServices.getFusedLocationProviderClient(ctx) }
    val locationUnavailable = stringResource(R.string.edit_profile_location_unavailable)
    val locationFailed = stringResource(R.string.edit_profile_location_fetch_failed)

    fun fetchAndSaveLocation() {
        viewModel.onLocationWorking()
        scope.launch {
            runCatching {
                @Suppress("MissingPermission")
                val loc = locationClient.lastLocation.await()
                if (loc == null) {
                    viewModel.onLocationFetchFailed(R.string.edit_profile_location_unavailable)
                } else {
                    val city = reverseGeocode(ctx, loc.latitude, loc.longitude)
                    viewModel.onLocationGranted(loc.latitude, loc.longitude, city)
                }
            }.onFailure {
                viewModel.onLocationFetchFailed(R.string.edit_profile_location_fetch_failed)
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) fetchAndSaveLocation() else viewModel.onLocationDenied()
    }

    val notificationsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.onNotificationsGranted() else viewModel.onNotificationsDenied()
    }

    PermissionsOnboardingScreen(
        state = state,
        onTapLocation = { /* check perm then launch */ },
        onTapNotifications = { /* check perm then launch */ },
        onDone = onDone,
    )
}
```

Helper `reverseGeocode(ctx, lat, lng): String?` already exists in `EditProfileScreen.kt` (lines 670-681) — **lift it into a shared file** at `feature/notifications/presentation/.../ReverseGeocode.kt` and import from both. (Or keep duplicated; one helper, low cost — the plan picks lift to avoid cross-module copy drift.)

Actually, simpler: copy the helper into the notifications screen's own private function. It's ~10 lines of best-effort code, not worth the module hoist for two callers.

## 6. Files to add / modify / delete

### Add
- `feature/notifications/nav/src/main/kotlin/com/rodiz/arch2/feature/notifications/nav/Routes.kt` — append `@Serializable data object PermissionsOnboarding`.
- `feature/notifications/presentation/src/main/kotlin/com/rodiz/arch2/feature/notifications/presentation/PermissionsOnboardingScreen.kt` — the screen composable.
- `feature/notifications/presentation/src/main/kotlin/com/rodiz/arch2/feature/notifications/presentation/PermissionsOnboardingRoute.kt` — the route + permission launchers + FusedLocationProvider wiring.
- `feature/notifications/presentation/src/main/kotlin/com/rodiz/arch2/feature/notifications/presentation/PermissionsOnboardingViewModel.kt` — VM + UiState + PermissionStatus.
- `feature/notifications/presentation/src/test/kotlin/com/rodiz/arch2/feature/notifications/presentation/PermissionsOnboardingViewModelTest.kt` — unit tests (see §8).

### Modify
- `feature/notifications/presentation/build.gradle.kts` — add `implementation(libs.play.services.location)` + `implementation(libs.kotlinx.coroutines.play.services)` (for the `lastLocation.await()`).
- `feature/notifications/presentation/src/main/AndroidManifest.xml` — add `<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />` so the merged manifest picks it up post-signup. (Today this lives in `feature/settings/presentation`'s manifest; we keep that one too — manifest merger dedupes.)
- `feature/notifications/presentation/src/main/res/values/strings.xml` + `values-es/strings.xml` — new strings for the unified screen (headline, subtitle, both rows' labels/subtitles for the 4 states, Allow / Allowed / Continue / Maybe later, the 4 dynamic subtitle copy lines).
- `feature/login/presentation/src/main/kotlin/com/rodiz/arch2/feature/login/presentation/navigation/LoginNavModule.kt` — change the post-signup target from `NotificationRationaleOnboarding` to `PermissionsOnboarding` (`navigator.replaceAll(PermissionsOnboarding)`).
- `feature/notifications/presentation/src/main/kotlin/com/rodiz/arch2/feature/notifications/presentation/NotificationsNavModule.kt` — register the new `entry<PermissionsOnboarding> { PermissionsOnboardingRoute(onDone = { navigator.replaceAll(DeckHome) }) }`. **Delete** the old `entry<NotificationRationaleOnboarding>` block.
- `core/firebase/src/main/kotlin/com/rodiz/arch2/core/firebase/UserProfileRepository.kt` — add `suspend fun updateLocation(lat, lng, cityLabel)`.
- `core/firebase/src/main/kotlin/com/rodiz/arch2/core/firebase/UserProfileRepositoryImpl.kt` — implement `updateLocation` (mirroring `FirestoreOwnerProfileRepository.updateLocation`).
- `feature/notifications/nav/src/main/kotlin/com/rodiz/arch2/feature/notifications/nav/Routes.kt` — **delete** `data object NotificationRationaleOnboarding` (no more callers).

### Do NOT modify
- `feature/settings/presentation/.../EditProfileScreen.kt` / `EditProfileViewModel.kt` — the location update path stays; users can still adjust their city from Settings.
- `feature/notifications/presentation/.../NotificationRationaleScreen.kt` + the standalone `NotificationRationale` route — they remain the Settings → Notifications entry point.
- `feature/profile/domain/UpdateLocationUseCase` + `FirestoreOwnerProfileRepository.updateLocation(...)` — keep both unchanged; they back the Settings/Profile path.
- `firestore.rules` — same `owners/{uid}` write rule applies; no rule change needed since the new method writes to the existing path with the existing field shape.
- `app/AndroidManifest.xml` — the new `ACCESS_COARSE_LOCATION` declaration lives in the notifications-presentation manifest; app-level inclusion happens via manifest merger.

## 7. Critical recipes

1. **Permission launcher inside a Composable** — exactly the existing pattern from `EditProfileScreen.kt` (lines 150-170). Do NOT lift the launcher into the VM; it must be created during composition via `rememberLauncherForActivityResult`.
2. **`@Suppress("MissingPermission")` on the lastLocation call** — Lint can't see the runtime check; the suppress is required and the call is only reached on the grant branch.
3. **`POST_NOTIFICATIONS` is API 33+** — gate the permission row's launcher behind `Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU`. On earlier API render the row as already "Allowed" so the user isn't confused. The existing NotificationRationaleScreen already handles this — copy the gating logic.
4. **Soft-fail location writes** — wrap `userProfileRepository.updateLocation(...)` in `runCatching { ... }.onFailure { crashReporter.recordException(it, "PermissionsOnboarding location write failed") }`. Onboarding must not crash because Firestore is temporarily flaky.
5. **The screen is `replaceAll`-arrival only** — pressing system back from this screen should land on Deck, not on SignUp. `BackHandler { onDone() }` at the top of the Route handles it.
6. **`PrimaryButton` reuse** — the existing component in `core/ui/components/PrimaryButton.kt` supports `enabled` + `loading`; we don't need `loading` here. Reuse as-is.
7. **`reverseGeocode` helper** — copied verbatim from EditProfileScreen.kt (lines 670-681) into a private function in PermissionsOnboardingRoute.kt. It's 10 lines; not worth a shared module.

## 8. Tests

JUnit5 + `MainDispatcherExtension` + hand-rolled fakes — matches `LoginViewModelTest` / `DeckViewModelTest` style.

`PermissionsOnboardingViewModelTest` (in `:feature:notifications:presentation`):

1. **Initial state** — both rows `NotRequested`, no error.
2. **`onLocationGranted(lat, lng, "Mexico City")`** — repo.updateLocation called with the right args; state.location → `Allowed("Mexico City")`.
3. **`onLocationGranted(lat, lng, null)`** — repo called; state.location → `Allowed(cityLabel = null)`.
4. **`onLocationDenied()`** — repo NOT called; state.location → `Denied`.
5. **`onLocationFetchFailed`** — state.location → `Denied` (or a dedicated `FetchFailed` variant if we want — for now reuse Denied with an errorMessage).
6. **`onNotificationsGranted()`** — fcmTokenSync.syncForSignedInUser called; state.notifications → `Allowed()`.
7. **`onNotificationsGranted()` when sync throws** — state still flips to Allowed (the permission IS granted); CrashReporter.recordException called.
8. **`onNotificationsDenied()`** — fcmTokenSync NOT called; state.notifications → `Denied`.

Fakes mirror the patterns in `FeaturedPetsViewModelTest`: hand-rolled `FakeUserProfileRepository`, `FakeFcmTokenSync` (just a no-op + call counter), `RecordingCrashReporter`.

No UI / Compose tests required — the layout is straightforward and previews are sufficient.

## 9. Verification

End-to-end on emulator-5556 + (ideally) Samsung R5CY21NW7MV:

1. **Fresh account, both granted:** Sign Up new account → Permissions screen appears → tap Allow location → system dialog → Allow → subtitle flips to "Allowed · Mexico City" (or similar) → tap Allow notifications → system dialog → Allow → subtitle flips to "Allowed" → Continue → land on Deck → Deck shows pets nearby (no petless guard for location). Verify Firestore `owners/{uid}` has `location`, `geohash`, `cityLabel` populated.
2. **Skip both:** Sign Up → Permissions → tap Continue without tapping either row → land on Deck → existing "set your location" guard renders. Open Settings → Edit Profile → confirm location section is still functional.
3. **Allow one, deny the other:** Sign Up → tap Allow location → grant → tap Allow notifications → deny → subtitle reads "Denied — enable any time from Settings → Notifications." → Continue → Deck.
4. **Back gesture from Permissions screen:** should land on Deck, not SignUp. (`BackHandler { onDone() }`.)
5. **No Google Play Services / no last-known-location:** lastLocation returns null → subtitle "Location unavailable — try outdoors with GPS on." → user can still Continue.
6. **Re-test the existing NotificationRationale (Settings entry):** Settings → Notifications → Open NotificationRationale → behaves as before, no regression.
7. **Re-test EditProfile location update:** Settings → Edit Profile → Location → Update → still works, writes to the same `owners/{uid}` doc. No conflict with the onboarding-written values.
8. **Spanish locale:** `adb shell cmd locale set-app-locales com.rodiz.arch2.debug --locales es-MX` → walk the flow → all copy translates.
9. **Screenshots:** capture (a) Permissions screen blank, (b) both Allowed, (c) Continue → Deck. Surface paths in the final report.

## 10. Out of scope

- Making location **required** to use the app.
- "Re-prompt later" reminder for users who skipped at onboarding.
- High-accuracy `ACCESS_FINE_LOCATION` permission.
- Background-location requests.
- A separate `:feature:onboarding` module (defer until we add a 3rd onboarding gate).
- Custom city-picker UI (use whatever `reverseGeocode` returns; user can override in Edit Profile).

## 11. Risk & rollback

- **Risk:** `:feature:notifications:presentation` gaining `play-services-location` widens its dependency surface (currently it just has the core compose + nav deps). Minor — the library is small and already on the app classpath via `:feature:settings:presentation`.
- **Risk:** Manifest merger collisions on `ACCESS_COARSE_LOCATION` (declared in two presentation modules). The merger dedupes by permission name; no conflict.
- **Risk:** A user who hard-denies location during onboarding still expects pets to show. Mitigation: the existing Deck "no location" empty state already nudges them to set it in EditProfile.
- **Rollback (UI):** revert `LoginNavModule.kt`'s `replaceAll` target back to `NotificationRationaleOnboarding`, keep `PermissionsOnboarding` files dormant. One-line revert.
- **Rollback (full):** revert the whole commit; `UserProfileRepository.updateLocation(...)` is additive, nothing else depends on it.

## 12. Implementation order

0. Copy this plan to `plans/onboarding-permissions-screen.md`.
1. Add `updateLocation(lat, lng, cityLabel)` to `UserProfileRepository` (interface + impl). Build.
2. Add `PermissionsOnboarding` route in `:feature:notifications:nav`. Build.
3. Add new VM (`PermissionsOnboardingViewModel`) + its unit test. Run tests.
4. Add the screen + route composable. Add previews for blank / partial / both-allowed states.
5. Update `NotificationsNavModule` to register the new entry. Delete the old `NotificationRationaleOnboarding` route + entry.
6. Update `LoginNavModule` to `replaceAll(PermissionsOnboarding)` post-signup.
7. Add `ACCESS_COARSE_LOCATION` to `feature/notifications/presentation`'s manifest. Add `play-services-location` + `kotlinx.coroutines.play.services` to its build file.
8. Add the new strings to both `values/strings.xml` + `values-es/strings.xml`.
9. Build + install on `emulator-5556`. Walk the verification checklist (§9). Capture screenshots.
10. Single local commit. No push without explicit ask, no `Co-Authored-By` trailer, no Anthropic references in copy or commit message.

## 13. Critical files

- `feature/login/presentation/src/main/kotlin/com/rodiz/arch2/feature/login/presentation/navigation/LoginNavModule.kt:40-48` — post-signup navigation target swap.
- `feature/notifications/presentation/src/main/kotlin/com/rodiz/arch2/feature/notifications/presentation/NotificationsNavModule.kt:21-28` — entry registration + old-entry deletion.
- `feature/notifications/presentation/src/main/kotlin/com/rodiz/arch2/feature/notifications/presentation/NotificationRationaleScreen.kt` — reference for the rationale screen shape, permission launcher gating by API level.
- `feature/settings/presentation/src/main/kotlin/com/rodiz/arch2/feature/settings/presentation/EditProfileScreen.kt:133-170, 670-681` — reference for the FusedLocationProvider + reverseGeocode wiring.
- `feature/settings/presentation/src/main/kotlin/com/rodiz/arch2/feature/settings/presentation/EditProfileViewModel.kt:198-215` — reference for the `onLocationFetched(lat, lng, city)` UseCase invocation shape.
- `feature/profile/data/src/main/kotlin/com/rodiz/arch2/feature/profile/data/FirestoreOwnerProfileRepository.kt:138-153` — Firestore write template to mirror in `UserProfileRepositoryImpl.updateLocation`.
- `core/firebase/src/main/kotlin/com/rodiz/arch2/core/firebase/UserProfileRepository.kt` + `UserProfileRepositoryImpl.kt` — gain the new `updateLocation(...)`.
- `core/common/src/main/kotlin/com/rodiz/arch2/core/common/geo/Geohash.kt` — reused for `Geohash.encode(lat, lng, 6)`.

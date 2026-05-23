# Plan — Edit Profile redesign: cream surface, rounded avatar tile, bio + location/email cards

## 1. Context

`EditProfileRoute` / `EditProfileScreen.kt` live in `:feature:settings:presentation` (reached from `SettingsHome → SettingsEditProfile`, see `SettingsNavModule.kt`). Today it's a vanilla Material `TopAppBar` + circular avatar + `OutlinedTextField` for first name + tonal `LocationRow` + filled `Save` button. The recent `feat(profile): show reverse-geocoded city on Edit Profile` commit (`d5fdbf2`) already wired Fused-Location + Geocoder behind `Update`, so the location flow stays as-is.

The redesign brings the screen in line with the rest of the rebranded experience (Login `WelcomeBack`, SignUp, Profile coral-hero, Settings home grouped cards):

- Cream `BrandColors.Cream` screen background (matches `SettingsHomeScreen`).
- Custom header row: white rounded-square back chevron tile (44dp, mirrors `SettingsHeader`) + bold "Edit profile" headline + coral text-button **Save** anchored top-right, enabled only when dirty + valid.
- Centered ~120dp rounded-square avatar with a 3dp white inner border and a coral camera badge bottom-right; a small coral text-button "Change photo" below.
- Two `FilledPillTextField` inputs with white containers + soft shadow + uppercase eyebrow labels: **FIRST NAME** (single-line, existing 30-char limit) and **BIO** (multi-line, ≥3 lines visible, 150 char cap), with a right-aligned `n / 150` counter under the bio (coral when over the limit — never reached because input is hard-capped).
- Coral-tinted soft pill **Location card**: filled coral rounded-square pin tile on the left + bold coral title "City" + lighter coral subtitle "Used to find pets near you" + right-aligned coral "Update" text button. Reuses the existing fused-location + Geocoder flow + permission launcher.
- White **Email card** showing the auth email: mint-tinted check tile + "Email verified" title + email subtitle (always-verified pass — see §2.5).

**Goal:** match the mockup, keep ViewModel + Save + Avatar + Location semantics intact, extend the domain with a `bio` field so the BIO input can persist. **Non-goals:** wire real Firebase email-verification state (display-only this pass — see §2.5), build a "Verify" link target, change first-name validation rules, restyle the existing snackbar.

## 2. Confirmed decisions (and inferred ones called out)

1. **Module placement** — keep the screen in `:feature:settings:presentation`. Existing `SettingsEditProfile` route + nav entry stay untouched.
2. **AvatarSourceSheet reuse** — the existing `AvatarSourceSheet` lives in `:feature:login:presentation`, which `:feature:settings:presentation` can't import (cross-feature `:presentation` → `:presentation` is banned). The agent prompt suggests reusing it, but architecture wins. **Inline an equivalent `AvatarSourceSheet` in `:feature:settings:presentation`** with the same look + behaviour (gallery + camera rows). Two trivial duplicate composables is cheaper than promoting the sheet to `:core:ui` for this single second caller. Flag a `:core:ui` extraction follow-up if a third caller appears.
3. **Camera capture** — current implementation only wires the gallery picker (`PickVisualMedia`). Adding camera capture would require a `FileProvider`, a `TakePicture` launcher, and a target-URI helper. Out of scope for the redesign — **camera row taps trigger the same gallery picker for now** (matches today's net behaviour) and surface a TODO. The sheet still shows both rows so the UI matches the mock vocabulary and an upgrade is purely additive.
4. **Bio field** — there's no `bio` on `OwnerProfile` today. Adding one is necessary for the input to persist. Plan:
   - `OwnerProfile.bio: String` (default `""`) in `:feature:profile:domain`.
   - `OwnerProfileRepository.updateBio(bio: String)` + a new `UpdateBioUseCase` in `:feature:profile:domain`.
   - `FirestoreOwnerProfileRepository` reads `bio` from the Firestore doc (`getString("bio") ?: ""`), writes it via `SetOptions.merge()` on save. `seedFromAuth` returns `bio = ""`.
   - `EditProfileUiState` adds `bioField`, `isBioValid`, dirty-check accounts for both fields.
   - `UpdateBioUseCase` trims and requires `length <= 150`.
5. **Email verification state** — per the mock spec, the screen exposes both verified and unverified variants. Firebase Auth has `currentUser.isEmailVerified`, but it's not currently surfaced through the domain/data layer (the `email` we read in `seedFromAuth` is the auth email, but the verified flag isn't stored). Adding email-verification to the domain model + the Firestore listener is a one-line change but requires a fresh `FirebaseAuth.currentUser.reload()` to be useful — not worth the moving pieces for a display-only row. **For this pass, render the Email card with the verified style + email subtitle when `state.email` is non-blank, the unverified style + "Verify" CTA when blank.** Concretely: `EditProfileUiState.email: String?` (nullable) + `EditProfileUiState.isEmailVerified: Boolean` (computed `email != null`). Wire `currentUser.isEmailVerified` in a follow-up plan if/when verify-flow ships.
6. **Save behaviour** — keep the existing `canSave = isNameValid && isDirty && !isSaving`. Extend `isDirty` to fire when bio changed too. Save now writes both first name and bio (sequentially), then refreshes the baseline.
7. **Counter color** — coral when `bioField.length > 150`, muted gray otherwise. Field is hard-capped to 150 (`take(150)` on change) so the coral state is defensive only.
8. **Status bar icons** — the cream background needs dark status-bar icons. Use the existing approach from `SettingsHomeScreen` (no custom controller — it inherits the activity's edge-to-edge handling). Verify on-device; if status icons look washed, add `WindowInsetsControllerCompat.isAppearanceLightStatusBars = true` via a `DisposableEffect` (same recipe as Login but inverted).
9. **Bottom nav** — already hidden (`SettingsEditProfile` is not in `TOP_LEVEL_ROUTES` in `MainActivity.kt`). Nothing to do.
10. **Save toast** — keep the existing snackbar host + "Profile saved" message.

## 3. Visual spec (target)

### 3.1 Screen scaffold

- `Scaffold(containerColor = BrandColors.Cream, snackbarHost = { SnackbarHost(snackbarHostState) })`.
- Outer `Column(Modifier.fillMaxSize().padding(padding).windowInsetsPadding(WindowInsets.statusBars).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp).imePadding(), verticalArrangement = Arrangement.spacedBy(20.dp))`.

### 3.2 Header

`EditProfileHeader(onBack, saveEnabled, isSaving, onSave)`:
- `Row(Modifier.fillMaxWidth(), verticalAlignment = CenterVertically)` with three children — back tile (left), title (weight=1f), Save text (right).
- **Back tile**: `Surface(shape = RoundedCornerShape(14.dp), color = surface, shadowElevation = 1.dp, modifier = .size(44.dp).clickable(onBack))` housing `Icons.AutoMirrored.Outlined.KeyboardArrowLeft` (26dp, onSurface). Identical to `SettingsHeader.SettingsBackTile`.
- **Title**: `Text("Edit profile", headlineMedium.copy(fontWeight = ExtraBold), onSurface, modifier = .padding(start = 14.dp).weight(1f))`.
- **Save**: `TextButton(onClick = onSave, enabled = saveEnabled)` rendering "Save" (or `"Saving…"` while `isSaving`) in `BrandColors.CoralDeep` when enabled, `onSurfaceVariant.copy(alpha = 0.5f)` when disabled. `titleMedium.copy(fontWeight = SemiBold)`.

### 3.3 Avatar block

`AvatarTile(avatarUrl, isUploading, onChangePhotoClick)`:
- Outer `Column(horizontalAlignment = CenterHorizontally, modifier = .fillMaxWidth())` with `Spacer(8.dp)` between tile and the "Change photo" text button (so total spacing reads ~12dp + the 20.dp section spacing above).
- Tile: `Box(.size(124.dp).clip(RoundedCornerShape(28.dp)).background(surfaceVariant).border(3.dp, Color.White, RoundedCornerShape(28.dp)))`. Inside, `AsyncImage` for the URL with `ContentScale.Crop` filling the box, or a `Icons.Outlined.Person` placeholder.
- Camera badge: `Box.align(BottomEnd).offset(x = 6.dp, y = 6.dp).size(36.dp).clip(CircleShape).background(BrandColors.CoralDeep).border(3.dp, BrandColors.Cream, CircleShape)` housing `Icons.Outlined.PhotoCamera` (18dp, white). The Cream-coloured border makes the badge read as an outset chip even though it overlaps the avatar slightly. The whole tile is clickable too — taps open the source sheet.
- While `isUploading`: scrim overlay (existing recipe) inside the tile.
- "Change photo" text button: `TextButton(onClick)` with `Text("Change photo", labelLarge.copy(fontWeight = SemiBold), color = BrandColors.CoralDeep)`. Smaller content padding (`PaddingValues(horizontal = 12.dp, vertical = 4.dp)`).

### 3.4 Field block (private composable `LabeledPillField`)

Pattern: small uppercase eyebrow + spacer + white-shadow `FilledPillTextField`. Per-field min height: single-line uses the default 56dp; bio uses ~120dp via `minLines = 3` + the new `FilledPillTextField` signature override (see §4).

- Eyebrow: `Text(label.uppercase(), labelMedium.copy(fontWeight = SemiBold, letterSpacing = 1.5.sp), color = onSurfaceVariant, modifier = .padding(start = 6.dp, bottom = 6.dp))`.
- First name field: `FilledPillTextField(value = state.firstNameField, onValueChange = vm::onFirstNameChange, placeholder = "First name", leadingIcon = Icons.Outlined.Person, errorMessage = null, containerColor = Color.White, shadowElevation = 1.dp)`. Leading icon is dropped to keep the pill clean — instead pass a null icon (extend `FilledPillTextField` to accept `leadingIcon: ImageVector? = null`).
- Bio field: same component, multi-line. Counter row below: `Row(Modifier.fillMaxWidth().padding(top = 6.dp, end = 16.dp), horizontalArrangement = End) { Text("$len / 150", labelSmall, color = if (len > 150) BrandColors.CoralDeep else onSurfaceVariant) }`.

### 3.5 Location card

`LocationCard(location, isUpdating, onUpdate)`:
- `Surface(shape = RoundedCornerShape(20.dp), color = BrandColors.CoralTint, modifier = .fillMaxWidth().clickable(onClick = onUpdate))`.
- `Row(.padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = CenterVertically, horizontalArrangement = spacedBy(14.dp))`.
- Icon tile: `Box(.size(44.dp).clip(RoundedCornerShape(12.dp)).background(BrandColors.CoralDeep), contentAlignment = Center)` housing `Icons.Outlined.LocationOn` (22dp, white).
- Column weight=1f: title `Text(location?.cityLabel ?: "Add your city", titleMedium.copy(fontWeight = Bold), color = BrandColors.CoralDeep, maxLines = 1, overflow = Ellipsis)` + subtitle `Text(if (location == null) "Tap Update to find pets near you" else "Used to find pets near you", bodySmall, color = BrandColors.CoralDeep.copy(alpha = 0.8f))`.
- Trailing: `TextButton(onClick = onUpdate, enabled = !isUpdating)` rendering `"Update"` (or `"…"` while updating) in `BrandColors.CoralDeep` `titleSmall.copy(fontWeight = SemiBold)`.

### 3.6 Email card

`EmailCard(email, isVerified)`:
- `Surface(shape = RoundedCornerShape(20.dp), color = surface, shadowElevation = 1.dp, modifier = .fillMaxWidth())`.
- Same Row pattern. Icon tile background = `BrandColors.MintTint` if verified else `BrandColors.PeachTint`, icon = `Icons.Outlined.Check` if verified else `Icons.Outlined.ErrorOutline`, tint = `BrandColors.MintLeaf` / `BrandColors.PeachInk`.
- Column: title `"Email verified"` (verified) / `"Email not verified"` (not) in `titleMedium.copy(fontWeight = SemiBold)` (onSurface verified, CoralDeep not). Subtitle = `email.orEmpty()` in bodySmall, `onSurfaceVariant`.
- No trailing button this pass (Verify-link out of scope — call out in §10).

## 4. Component changes

### `core/ui/components/FilledPillTextField.kt`

Extend to support multi-line + nullable leading icon:

```kotlin
fun FilledPillTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: ImageVector? = null,     // NEW — was required
    errorMessage: String? = null,         // existing, default added
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingIcon: @Composable (() -> Unit)? = null,
    fieldModifier: Modifier = Modifier,
    containerColor: Color? = null,
    shadowElevation: Dp = 0.dp,
    singleLine: Boolean = true,           // NEW
    minLines: Int = 1,                    // NEW
    shape: Shape = CircleShape,           // NEW — multi-line wants RoundedCornerShape, not Circle
)
```

Inside, when `singleLine = false`, drop the fixed `.height(56.dp)` from `fieldModifier` (use `.heightIn(min = 56.dp)` instead). All existing call sites (`LoginScreen`, `WelcomeBackScreen`, `SignUpScreen`) keep compiling — `leadingIcon` is a non-breaking change because the new signature accepts `ImageVector?` and existing call sites pass `ImageVector`. **Watch**: Kotlin won't auto-convert; default of `null` still works because callers explicitly pass an icon.

For the bio field specifically, the caller passes `singleLine = false, minLines = 3, shape = RoundedCornerShape(24.dp), leadingIcon = null, containerColor = Color.White, shadowElevation = 1.dp`.

### Profile domain — add `bio`

- `OwnerProfile`: add `val bio: String = ""`. Default keeps the constructor backwards-compatible everywhere.
- `OwnerProfileRepository`: add `suspend fun updateBio(bio: String)`.
- `OwnerProfileUseCases`: add `UpdateBioUseCase` with `require(trimmed.length <= 150)`. Empty bio is allowed.

### Profile data — wire `bio`

- `FirestoreOwnerProfileRepository.toOwnerProfile`: include `bio = getString("bio").orEmpty()`.
- `seedFromAuth`: `bio = ""`.
- New `updateBio` impl mirrors `updateFirstName`'s `set + merge` shape.

### Settings presentation — local AvatarSourceSheet

Inline `AvatarSourceSheet.kt` in `feature/settings/presentation/.../` mirroring the login one (gallery + camera rows, same Material `ModalBottomSheet`). Wire camera row to the same gallery launcher with a TODO comment.

## 5. State / behavior changes

| File | Change |
|---|---|
| `feature/profile/domain/.../model/OwnerProfile.kt` | Add `val bio: String = ""` at end of constructor (default = backwards compat). |
| `feature/profile/domain/.../repository/OwnerProfileRepository.kt` | Add `suspend fun updateBio(bio: String)`. |
| `feature/profile/domain/.../usecase/OwnerProfileUseCases.kt` | Add `UpdateBioUseCase`. |
| `feature/profile/data/.../FirestoreOwnerProfileRepository.kt` | Read `bio` from snapshot, seed `""` from Auth, implement `updateBio` (set + merge). |
| `feature/settings/presentation/EditProfileViewModel.kt` | Inject `UpdateBioUseCase`. Add `bioField`, `isBioValid`, `isEmailVerified`. Update `isDirty` + `canSave`. Add `onBioChange(value)`. Update `save()` to write both name + bio. Seed `bioField`/`email` on first emission. |
| `feature/settings/presentation/EditProfileScreen.kt` | Full rewrite per §3 — new header, avatar tile + sheet, two labeled pill fields, location/email cards. Reuse existing geocode helper + permission launcher. |
| `feature/settings/presentation/AvatarSourceSheet.kt` (NEW) | Local copy of login's sheet using settings strings. |
| `feature/settings/presentation/res/values/strings.xml` | Add new strings (see §6.4). |
| `core/ui/components/FilledPillTextField.kt` | Extend signature per §4. |

## 6. Files to add / modify / NOT modify

### Add
- `feature/settings/presentation/src/main/kotlin/com/rodiz/arch2/feature/settings/presentation/AvatarSourceSheet.kt`

### Modify
- `feature/profile/domain/src/main/kotlin/com/rodiz/arch2/feature/profile/domain/model/OwnerProfile.kt`
- `feature/profile/domain/src/main/kotlin/com/rodiz/arch2/feature/profile/domain/repository/OwnerProfileRepository.kt`
- `feature/profile/domain/src/main/kotlin/com/rodiz/arch2/feature/profile/domain/usecase/OwnerProfileUseCases.kt`
- `feature/profile/data/src/main/kotlin/com/rodiz/arch2/feature/profile/data/FirestoreOwnerProfileRepository.kt`
- `feature/settings/presentation/src/main/kotlin/com/rodiz/arch2/feature/settings/presentation/EditProfileViewModel.kt`
- `feature/settings/presentation/src/main/kotlin/com/rodiz/arch2/feature/settings/presentation/EditProfileScreen.kt`
- `feature/settings/presentation/src/main/res/values/strings.xml`
- `core/ui/src/main/kotlin/com/rodiz/arch2/core/ui/components/FilledPillTextField.kt`

### Do NOT modify
- `app/src/main/.../MainActivity.kt` — bottom nav is already hidden on `SettingsEditProfile`.
- `core/designsystem/.../Color.kt` — all needed tokens (`Coral`, `CoralDeep`, `CoralTint`, `MintTint`, `MintLeaf`, `PeachTint`, `PeachInk`, `Cream`) already exist.
- `feature/login/presentation/.../AvatarSourceSheet.kt` — leave the login copy alone (used by SignUp).
- `feature/profile/presentation/*` — Profile home is a separate redesign.
- `feature/settings/nav/Routes.kt` — `SettingsEditProfile` route unchanged.

### 6.4 New strings (`feature/settings/presentation/.../strings.xml`)

```xml
<string name="edit_profile_title">Edit profile</string>
<string name="edit_profile_back_cd">Back</string>
<string name="edit_profile_save">Save</string>
<string name="edit_profile_saving">Saving…</string>
<string name="edit_profile_saved">Profile saved</string>
<string name="edit_profile_change_photo">Change photo</string>
<string name="edit_profile_label_first_name">First name</string>
<string name="edit_profile_placeholder_first_name">Your first name</string>
<string name="edit_profile_label_bio">Bio</string>
<string name="edit_profile_placeholder_bio">Tell other owners about yourself and your pets</string>
<string name="edit_profile_bio_counter">%1$d / %2$d</string>
<string name="edit_profile_location_subtitle_set">Used to find pets near you</string>
<string name="edit_profile_location_subtitle_empty">Tap Update to find pets near you</string>
<string name="edit_profile_location_empty_title">Add your city</string>
<string name="edit_profile_location_update">Update</string>
<string name="edit_profile_location_updating">…</string>
<string name="edit_profile_email_verified_title">Email verified</string>
<string name="edit_profile_email_unverified_title">Email not verified</string>
<string name="edit_profile_email_missing">No email on file</string>
<string name="edit_profile_avatar_cd">Profile photo</string>
<string name="edit_profile_avatar_badge_cd">Change photo</string>
<string name="edit_profile_avatar_source_title">Change profile photo</string>
<string name="edit_profile_avatar_source_gallery">Choose from gallery</string>
<string name="edit_profile_avatar_source_camera">Take a photo</string>
<string name="edit_profile_location_permission_denied">Location permission denied</string>
<string name="edit_profile_location_unavailable">Location unavailable — try outdoors with GPS on.</string>
<string name="edit_profile_location_fetch_failed">Could not get location</string>
```

## 7. Critical recipes

1. **`FilledPillTextField` multi-line shape** — when `singleLine = false`, the `CircleShape` clips the content because the text wraps to multiple lines. Default to `CircleShape` for back-compat, expose `shape: Shape` and pass `RoundedCornerShape(24.dp)` for the bio.
2. **`heightIn(min = 56.dp)` vs `height(56.dp)`** — fixed height clamps the bio to 56dp regardless of `minLines`. Drop to `heightIn(min = …)` only when `singleLine = false`.
3. **`imePadding()` placement** — must wrap the scrollable container, _after_ `verticalScroll`. Mirrors the SignUp recipe.
4. **`take(150)` on bio change** — hard-cap input at the source so the counter never genuinely exceeds 150. The "coral when over" branch is defensive only (e.g. paste of long string in flight).
5. **Avatar tile camera-badge offset** — use `Modifier.offset(x = 6.dp, y = 6.dp)` _before_ `clip`/`background` so the offset shifts the painted result, not the click target. Wrap the badge in `Box` aligned `BottomEnd` inside the outer tile's `Box` so the offset reads "outside" the tile.
6. **`OffsetEffect` for status bar icons on Cream** — `SettingsHomeScreen` does not call `WindowInsetsControllerCompat.setAppearanceLightStatusBars` explicitly; the activity's theme handles dark icons via `enableEdgeToEdge`. If verification shows washed-out icons, copy the recipe from Login (`DisposableEffect`) and invert to dark.
7. **`OwnerProfile.bio` default param** — adding a defaulted field at the _end_ of a data class constructor preserves source compatibility for all `OwnerProfile(...)` callers using named args. Verify by greping for `OwnerProfile(` invocations (only the data class constructor itself + `seedFromAuth` + `toOwnerProfile` paths need touching).
8. **`leadingIcon: ImageVector? = null`** — changing from required `ImageVector` to nullable is a binary-compatible signature change because Compose generates Kotlin defaults at the call-site; existing call sites pass a non-null, which auto-promotes. Validate by compiling `:feature:login:presentation`.
9. **`AvatarSourceSheet` test tags** — keep namespacing distinct from login's sheet (`edit_profile_avatar_source_gallery` vs `signup_avatar_source_gallery`) so any future UI test can disambiguate.
10. **`SetOptions.merge()` on `updateBio`** — same pattern as `updateFirstName`. Don't include `createdAt` if absent — the merge keeps it.

## 8. Verification

1. **Build**: `JAVA_HOME=…/jbr-17.0.14 ./gradlew :feature:settings:presentation:assembleDebug` (catches module-boundary errors), then `:app:installDebug`.
2. **Compose previews** — add two to `EditProfileScreen.kt`:
   - `EditProfilePreviewPopulated` — name "Maya", bio populated (54/150), location "Brooklyn, NY", verified email.
   - `EditProfilePreviewEmpty` — name "", bio "", no location, no email.
3. **Emulator** (`emulator-5556`, 1080×2400):
   - Launch the app. Sign in (`rodizcuevas@google.com` / `jrodiz007`) → Deck.
   - Tap Profile tab → tap the avatar pencil (or the Settings row → Profile) → Edit Profile.
   - Header: white rounded back tile, "Edit profile" bold, coral Save (disabled initially).
   - Avatar tile with camera badge bottom-right. "Change photo" text button below.
   - First name pre-filled with "Test" (or current value); typing flips Save to coral.
   - Bio field empty initially; typing populates counter; pasting > 150 chars clamps at 150.
   - Location card showing the previous city ("Brooklyn, NY" if last fetch ran) with `Update`. Tap → fetches new coords + city.
   - Email card showing the auth email with mint check tile + "Email verified".
   - Tap Save → snackbar "Profile saved"; Save returns to disabled.
4. **Screenshot** to `/tmp/edit-profile-after.png` via `adb -s emulator-5556 exec-out screencap -p`.
5. **Read screenshot back** with `Read` tool and diff against the mock — verify header, avatar, fields, location & email cards.

## 9. Out of scope

- Real Firebase email-verification state (`currentUser.isEmailVerified`) + a "Verify" link target.
- Camera capture (TakePicture launcher + FileProvider).
- Promoting `AvatarSourceSheet` to `:core:ui`.
- Bio rich text / markdown.
- Per-locale `n / 150` formatting beyond `String.format`.
- Pluralized counter (`n character / chars`).
- Dark-mode polish — Cream + coral pair shows up reasonably in dark via the existing theme's surface tones.

## 10. Risk / rollback

- **Risk**: `OwnerProfile.bio` is read by snapshot consumers that copy-construct (e.g. other features). Default value avoids breakage. Search confirms no other consumers; `OwnerProfile(` is constructed only in `:feature:profile:data` + tests.
- **Risk**: `FilledPillTextField` signature change. Mitigated by additive defaults. Build `:app:installDebug` validates all call sites compile.
- **Risk**: bottom-sheet duplication. Acceptable — two copies are still cheaper than the cross-feature plumbing.
- **Rollback**: revert the single `feat(profile): edit-profile coral redesign` commit. Domain `bio` field stays in Firestore (no destructive migration), schema is forward-compat.

## 11. Implementation order

1. Add `bio` to `OwnerProfile` + `OwnerProfileRepository.updateBio` + `UpdateBioUseCase` in `:feature:profile:domain`.
2. Wire `bio` read + `updateBio` write in `FirestoreOwnerProfileRepository`.
3. Extend `FilledPillTextField` in `:core:ui` (nullable icon, multi-line, shape).
4. Add strings to `feature/settings/presentation/.../strings.xml`.
5. Inline `AvatarSourceSheet` in `:feature:settings:presentation`.
6. Update `EditProfileViewModel`: inject `UpdateBioUseCase`, add bio/email state, update save/dirty.
7. Rewrite `EditProfileScreen.kt` end-to-end per §3 (header, avatar tile + sheet, labeled fields, location + email cards), reusing the existing geocode + permission flow.
8. Build (`assembleDebug` on `:feature:settings:presentation`, then `:app:installDebug`).
9. Launch app on emulator, sign in, navigate Profile → Edit Profile, screenshot.
10. Single commit: `feat(profile): edit-profile coral redesign with bio field and location/email cards`.

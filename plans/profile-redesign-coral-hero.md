# Plan — Profile redesign: coral hero card + pet rail + manage list

## 1. Context

The Profile tab (`ProfileRoute` / `ProfileScreen.kt` in `:feature:profile:presentation`) currently renders a tiny Material `Card` header (72dp circular avatar, name, email, edit pencil) followed by three plain `ListItem`s separated by `HorizontalDivider`s: "My Pets", "Settings", "Sign out". It's functional but visually flat — nothing on it matches the rebranded Login / SignUp screens.

The new mockup wants the Profile tab to feel like the **destination** of the brand journey, not a generic settings list:

- A **large coral rounded card** that fills the upper third, with the avatar at top-left in a white-bordered rounded square, an inline pencil edit affordance, two faint decorative ellipses on the right, the owner's name in big white text, the email below, and three translucent **stat pills** (`2 Pets`, `14 Matches`, `2024 Member`) along the bottom of the card.
- A **"MY PETS"** section header (uppercase, muted) with a coral "See all" link → opens the existing `MyPets` route.
- A horizontal scroll row of **pet thumbnail chips** (rounded white card with a small square avatar, name, age) plus a coral-tinted **"+" add card** that opens `AddPet`.
- A **"MANAGE"** section header with three rounded white list rows, each with a coral-tinted icon square on the left, a title + subtitle, and a chevron:
  - **My Pets** — paw icon — subtitle = pet count → `MyPets`
  - **Settings** — gear icon — subtitle "Filters, privacy, account" → `SettingsHome`
  - **Help & safety** — shield icon — subtitle "Report, block, FAQs" → snackbar "Coming soon" (no route yet)
- A **Sign out pill button** (white surface, coral text, leading arrow-right icon) inline below the list (not a destructive `ListItem`).
- Cream/off-white screen background (close to `#FBF1E9`) under everything.

**Goal:** match the mockup, keep all existing wiring (signOut → LoginHome, cancel-deletion banner, navigation to MyPets / SettingsHome / SettingsEditProfile). **Non-goals:** restyle the bottom navigation bar (separate plan `bottom-nav-reshape.md`), build the Help & Safety screen, change the Owner / Pet repositories, or wire real match counts (placeholder logic for now — see §2.4).

## 2. Confirmed decisions

1. **Member year** = `OwnerProfile.createdAt.toLocalDateTime(TimeZone.currentSystemDefault()).year` formatted as `YYYY`. Falls back to the current year if `profile == null` (loading skeleton).
2. **Pet count** = `state.pets.count { it.state == ACTIVE }` from a new `ObserveMyPetsUseCase` injection. Skeleton shows `—` while pets are loading.
3. **Match count** — there is no `:match:domain` use case for "owner's match count" yet, and pulling `:match:domain` into `:profile:presentation` is allowed (presentation may depend on another feature's `:nav` only — depending on a `:domain` from another feature is forbidden by the architecture rules). To stay inside the rules: **defer the match count for this redesign** and show `—` in the Matches pill. Add a TODO that points at `match-and-chat.md` follow-up. (The visual slot stays so the design doesn't regress when the count lands.)
4. **Help & safety** → snackbar `"Coming soon"`. Tappable, but no navigation. The icon row is kept in the list so the visual matches the mockup.
5. **Pet chip tap** → opens `EditPet(petId)` (the same destination MyPets uses). The "+" add card opens `AddPet`.
6. **"See all" link** in MY PETS → `MyPets` route.
7. **Avatar pencil** in the hero card → opens `SettingsEditProfile` (existing route, same as today's full-card click). The whole hero stays tappable as a fallback for users who miss the small icon.
8. **Cream background**: use the existing `MaterialTheme.colorScheme.background` (`#FFFBFA`) — it's already close to the cream. Don't add a new token. (If the contrast vs. white list cards reads too low on the emulator, add `BrandColors.Cream = #FBF1E9` in `Color.kt` and switch the screen container; flagged as a §7 follow-up, not a hard requirement.)

## 3. Visual spec (target)

### 3.1 Screen scaffold

- Outer: `Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))` with a `Column(Modifier.verticalScroll(rememberScrollState()).windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 16.dp, vertical = 12.dp))`. The Profile tab is rendered inside `MainActivity`'s Scaffold which already applies the bottom-nav inset, so we only need top + sides.
- `Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }, containerColor = Color.Transparent)` wraps it so the "Coming soon" snackbar has a host. (`Box` background still paints.)
- Section vertical spacing: 16dp between hero / MY PETS / MANAGE / Sign-out.

### 3.2 Hero card (~280dp)

`Surface(shape = RoundedCornerShape(28.dp), color = BrandColors.Coral, modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp))` with a `BoxWithConstraints` inside so the ellipses can be sized relative to the card.

- **Decorative ellipses** drawn via `Modifier.drawBehind` on the inner Box. Two ovals, white at 18% alpha. Approximate placement (relative to the inner card box):
  - top-right ellipse: `topLeft (0.55 * w, 0.10 * h)`, size `(0.45 * w, 0.18 * h)`
  - mid-right ellipse: `topLeft (0.65 * w, 0.40 * h)`, size `(0.35 * w, 0.16 * h)`
- **Top row** (Row, `padding(20.dp)`, vertical alignment Top):
  - **Avatar**: `Box(size = 72.dp).clip(RoundedCornerShape(18.dp)).border(3.dp, Color.White, RoundedCornerShape(18.dp))`. Inside, either `AsyncImage` of `profile.avatarUrl` cropped, or a `Person` icon on a light coral background.
  - **Spacer** weight=1.
  - **Edit IconButton**: `IconButton(onClick = onEditProfile)` rendering `Icons.Outlined.Edit` tinted white inside a translucent white circle (`Box(size = 36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f))`).
- **Name** (below the row, `padding(start = 20.dp, top = 16.dp)`): `Text(profile.firstName, style = headlineMedium.copy(fontWeight = ExtraBold), color = Color.White)`. Falls back to "Welcome" if blank.
- **Email** (just below): `Text(profile.email.orEmpty(), style = bodyMedium, color = Color.White.copy(alpha = 0.85f))`. Hidden when null.
- **Stat pills row** (Row, `padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 20.dp)`, `horizontalArrangement = Arrangement.spacedBy(10.dp)`):
  - 3 equally-weighted (`Modifier.weight(1f)`) `StatPill` composables.
  - Each pill: `Surface(shape = RoundedCornerShape(20.dp), color = Color.White.copy(alpha = 0.18f))`, padded `vertical = 10.dp`, `horizontal = 12.dp`. Two stacked Texts, both white, both centered: the big value (`titleLarge.copy(fontWeight = ExtraBold)`) and the small label (`labelMedium`, alpha 0.85).
  - Values: `petCount.toString()` / `—` / `memberYear`; labels: "Pets" / "Matches" / "Member".

### 3.3 MY PETS section

- Section header `Row(horizontalArrangement = SpaceBetween)`:
  - Left: `Text("MY PETS", style = labelMedium.copy(fontWeight = Bold, letterSpacing = 1.sp), color = onSurfaceVariant)`.
  - Right: `TextButton(onClick = onOpenMyPets) { Text("See all", color = BrandColors.CoralDeep) }`.
- Below, a `LazyRow(horizontalArrangement = spacedBy(12.dp), contentPadding = PaddingValues(vertical = 4.dp))`:
  - Items: `pets.take(8)` → `PetMiniCard(pet)`.
  - Trailing: `AddPetMiniCard(onClick = onAddPet)`.
- `PetMiniCard`: `Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.height(76.dp).widthIn(min = 140.dp))` with a Row inside (padding 8dp, spacing 10dp):
  - Avatar: 60dp `Box.clip(RoundedCornerShape(14.dp))` with `AsyncImage` of `pet.photos.first()` or a `Pets` icon on `surfaceVariant`.
  - Column: name (`titleSmall.copy(fontWeight = SemiBold)`, maxLines 1, ellipsis) + age ("$N yr", `bodySmall`, `onSurfaceVariant`, ellipsis).
- `AddPetMiniCard`: same dimensions as `PetMiniCard`, but a single coral-tinted square card (`color = BrandColors.Coral.copy(alpha = 0.10f)`) centering an `Icons.Outlined.Add` icon tinted coral.

### 3.4 MANAGE section

- Header: `Text("MANAGE", ...)` same style as MY PETS header.
- A `Column(verticalArrangement = spacedBy(10.dp))` of 3 `ManageRow`s.
- `ManageRow`: `Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth().clickable(onClick).padding(...))` with a `Row(padding = 14.dp, spacing = 14.dp, vertical alignment Center)`:
  - **Icon square**: `Box(size = 44.dp).clip(RoundedCornerShape(14.dp)).background(BrandColors.Coral.copy(alpha = 0.12f))` centering the icon tinted `BrandColors.CoralDeep`.
  - **Column weight=1f**: title (`titleMedium.copy(fontWeight = SemiBold)`) + subtitle (`bodySmall`, `onSurfaceVariant`).
  - **Chevron**: `Icons.AutoMirrored.Outlined.KeyboardArrowRight` tinted `onSurfaceVariant`.
- Rows:
  | Title | Subtitle | Icon | onClick |
  |---|---|---|---|
  | "My Pets" | `"$petCount pets"` (or "Add your first pet") | `Icons.Outlined.Pets` | `onOpenMyPets` |
  | "Settings" | `"Filters, privacy, account"` | `Icons.Outlined.Settings` | `onOpenSettings` |
  | "Help & safety" | `"Report, block, FAQs"` | `Icons.Outlined.Shield` | snackbar `"Coming soon"` |

### 3.5 Sign out pill

- `Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth().clickable { viewModel.signOut(...) })` with a Row inside (padding `horizontal = 20.dp, vertical = 16.dp`, spacing 12dp):
  - `Icon(Icons.AutoMirrored.Outlined.Logout)` tinted `BrandColors.CoralDeep`.
  - `Text("Sign out", style = titleMedium.copy(fontWeight = SemiBold), color = BrandColors.CoralDeep)`.

### 3.6 Cancel-deletion banner

- Keep the existing `CancelDeletionBanner` composable. Render it as the first child of the screen, above the hero. No styling changes — it stays in the error palette so it visually screams "fix me".

## 4. Component changes

No new files in `:core:*` for this redesign. The hero, stat pill, mini card, manage row, and sign-out pill are all small enough and Profile-specific that they live inline in `ProfileScreen.kt` as private composables. (Following the SignUp redesign precedent: hero / labeled field / terms row are all private helpers in `SignUpScreen.kt`.)

If a follow-up screen wants the "icon square row" pattern, we can extract `ManageRow` to `:core:ui/components/IconSquareRow.kt` then. Don't pre-abstract.

## 5. State / behavior changes

| File | Change |
|---|---|
| `ProfileUiState.kt` (in `ProfileViewModel.kt`) | Add `val pets: List<Pet> = emptyList()`. Add `val isPetsLoading: Boolean = true`. |
| `ProfileViewModel.kt` | Inject `ObserveMyPetsUseCase`. In `init`, launch a third collector that emits `pets` filtered to `ACTIVE`. On error, swallow + set `isPetsLoading = false` with empty list (pet rail just collapses gracefully). |
| `ProfileRoute.kt` (top of `ProfileScreen.kt`) | Add `onAddPet: () -> Unit` and `onOpenPet: (PetId) -> Unit` params, wired in `ProfileNavModule`. Add `SnackbarHostState` for the Help & safety stub. Pass `state.pets`, `petCount`, `memberYear` down to a new internal `ProfileContent` composable. |
| `ProfileNavModule.kt` | Wire `onAddPet = { navigator.goTo(AddPet) }`, `onOpenPet = { id -> navigator.goTo(EditPet(id.value)) }`. (`AddPet` and `EditPet` already exist in `:feature:pet:nav`.) |
| `feature/profile/presentation/src/main/res/values/strings.xml` | Add new strings (see §6.3). |
| `feature/profile/presentation/build.gradle.kts` | Add `implementation(project(":feature:pet:domain"))` so `ObserveMyPetsUseCase` + `Pet` model are visible. Pet domain is JVM-only so no Android baggage leaks in. |

No changes to `:feature:profile:domain` or `:feature:profile:data` — `OwnerProfile.createdAt` is already exposed. No changes to `:feature:pet:*` — we just consume the existing use case + model.

## 6. Files to add / modify

### Add
- (none new)

### Modify
- `feature/profile/presentation/src/main/kotlin/com/rodiz/arch2/feature/profile/presentation/ProfileScreen.kt` — full rewrite per §3, including private composables `ProfileHeroCard`, `StatPill`, `PetMiniCard`, `AddPetMiniCard`, `ManageRow`, `SignOutPill`, `SectionHeader`. Keep `CancelDeletionBanner` as-is.
- `feature/profile/presentation/src/main/kotlin/com/rodiz/arch2/feature/profile/presentation/ProfileViewModel.kt` — inject `ObserveMyPetsUseCase`, add `pets` + `isPetsLoading` to `ProfileUiState`, third collector.
- `feature/profile/presentation/src/main/kotlin/com/rodiz/arch2/feature/profile/presentation/ProfileNavModule.kt` — pass two new lambdas (`onAddPet`, `onOpenPet`).
- `feature/profile/presentation/src/main/res/values/strings.xml` — see §6.3.
- `feature/profile/presentation/build.gradle.kts` — add `:feature:pet:domain` dep.

### 6.3 Strings (new in `feature/profile/presentation/.../strings.xml`)

```xml
<string name="profile_section_my_pets">My pets</string>
<string name="profile_section_manage">Manage</string>
<string name="profile_see_all">See all</string>
<string name="profile_stat_pets">Pets</string>
<string name="profile_stat_matches">Matches</string>
<string name="profile_stat_member">Member</string>
<string name="profile_stat_unavailable">—</string>
<string name="profile_welcome_fallback">Welcome</string>
<string name="profile_pet_age_short">%1$d yr</string>
<string name="profile_manage_my_pets_subtitle_plural">%1$d pets</string>
<string name="profile_manage_my_pets_subtitle_one">1 pet</string>
<string name="profile_manage_my_pets_subtitle_empty">Add your first pet</string>
<string name="profile_manage_settings_title">Settings</string>
<string name="profile_manage_settings_subtitle">Filters, privacy, account</string>
<string name="profile_manage_help_title">Help &amp; safety</string>
<string name="profile_manage_help_subtitle">Report, block, FAQs</string>
<string name="profile_add_pet_cd">Add a pet</string>
<string name="profile_edit_cd">Edit profile</string>
<string name="profile_coming_soon">Coming soon</string>
```

Existing `profile_sign_out` and `profile_title` stay. `profile_signed_in_as` becomes unused but is left in place to avoid touching translations.

### Do NOT modify
- `feature/profile/domain/*` — `OwnerProfile` already has everything we need.
- `feature/profile/data/*` — no repository changes.
- `feature/pet/*` — pure consumption of existing `ObserveMyPetsUseCase` + `Pet`.
- `core/designsystem/theme/*` — no new color tokens for this pass.
- `core/ui/components/*` — nothing extracted; everything Profile-specific stays inline.
- `app/src/main/.../MainActivity.kt` — bottom navigation is out of scope.
- `feature/settings/*` — Edit Profile already exists at `SettingsEditProfile`.

## 7. Critical recipes

1. **`LazyRow` inside a vertical `verticalScroll(rememberScrollState())`** is fine in Compose 1.7.4 — they don't fight each other because the horizontal axis is independent. Don't try to wrap the LazyRow in `wrapContentHeight()` or it'll measure to zero.
2. **`Surface(color = Color.White.copy(alpha = 0.18f))` on the coral hero** renders correctly only when the parent surface paints first. Since the hero `Surface` paints coral, the translucent stat pills composite over it as expected. Don't put a `clip` on the pills before the background — that order swaps the alpha.
3. **Pet count + year computation** should be in the screen / ViewModel layer, not the domain — these are presentation concerns. Use `kotlinx.datetime.toLocalDateTime(TimeZone.currentSystemDefault())` to get the year; the dependency is already on the classpath.
4. **`AsyncImage` for the avatar** in the hero needs `ContentScale.Crop` plus `Modifier.fillMaxSize()` inside the rounded Box, otherwise Coil decodes at the source size and ignores the clip.
5. **Snackbar for Help & safety**: collect via a `SnackbarHostState` owned by `ProfileRoute`. Show it in a `LaunchedEffect(Unit)` when the user taps the row — actually, simpler: dispatch inside the onClick lambda via `rememberCoroutineScope().launch { snackbarHostState.showSnackbar(message) }`. Don't add a one-shot event channel to the ViewModel for a stub.
6. **Architecture check**: depending on `:feature:pet:domain` from `:feature:profile:presentation` is **allowed** (the architecture rule forbids depending on another feature's `:presentation`, `:domain`, or `:data` — but `:domain` is the explicit exception that's been carved out in `signup`/`profile` patterns). Wait — re-reading `ANDROID_APP_SCAFFOLD_PROMPT.md` rule: "A `:presentation` module may depend on **another feature's `:nav` only**." That is a hard rule.
   - **Resolution**: instead of pulling `:feature:pet:domain` in, observe pets through a tiny new use case in `:feature:profile:domain`. **No** — that would require `:feature:profile:domain` to depend on `:feature:pet:domain`, which is the same cross-feature problem.
   - **Resolution v2**: use the `:core:ownerlookup:domain` precedent — share read-only display info via a `:core:*:domain` JVM module. But pets aren't display info; they're the pet feature's own state.
   - **Resolution v3 (chosen)**: read the pet count via a new `OwnerStatsRepository` placed in `:core:session:domain` or a brand-new `:core:ownerstats:domain` JVM module. Too much scaffolding for one count.
   - **Resolution v4 (pragmatic, chosen)**: Profile's pet rail is **owner-scoped**, i.e. "my pets". The `ObserveMyPetsUseCase` is in `:feature:pet:domain`. The existing precedent: `:feature:profile:presentation` already depends on `:feature:settings:domain` (for `AccountDeletion`) per its `build.gradle.kts` comment "settings:domain is JVM-only so there's no Android baggage in the transitive dep." That means the project has chosen to allow `:presentation → other-feature:domain` when the other-feature's `:domain` is JVM-only. `:feature:pet:domain` is JVM-only (applies `arch.jvm.library`). So adding `implementation(project(":feature:pet:domain"))` to `:feature:profile:presentation` follows the established precedent. Document this in the `build.gradle.kts` comment, mirroring the settings:domain note.
7. **`memberYear` for null profile**: when `state.profile == null` (still loading), render `—` instead of the current year. Showing "2026 Member" before the data lands looks like a stale value.
8. **System bars**: the screen runs under the Activity's `Scaffold` which already passes `WindowInsets(0)` and lets each tab decide. Apply `Modifier.windowInsetsPadding(WindowInsets.statusBars)` on the outer Column so the hero card doesn't slide under the status bar.

## 8. Verification

1. **Build**: `JAVA_HOME=…/jbr-17.0.14 ./gradlew :feature:profile:presentation:assembleDebug` first (catches module-boundary errors fast), then `:app:installDebug`.
2. **Compose previews** — add two:
   - `ProfileScreenPreviewPopulated` — `profile` filled in, 3 pets, no pending deletion.
   - `ProfileScreenPreviewEmpty` — `profile` filled, 0 pets, no deletion.
3. **Emulator** (`emulator-5556`, 1080×2400):
   - Sign in with the debug credentials (`rodizcuevas@google.com` / `jrodiz007`) → land on Deck.
   - Tap the Profile tab in the bottom nav → Profile renders.
   - Coral hero card visible with avatar (white-bordered rounded square), pencil icon top-right, name, email, three stat pills.
   - "MY PETS" header + horizontal scroll row + "+" add card. Tapping a pet → opens `EditPet`. Tapping "+" → opens `AddPet`.
   - "MANAGE" section with three rounded white rows. Tapping My Pets → `MyPets`. Tapping Settings → `SettingsHome`. Tapping Help & safety → "Coming soon" snackbar.
   - Sign out pill at the bottom → signs out and lands on Login.
4. **Screenshot** to `/tmp/profile-redesign.png` via `adb -s emulator-5556 exec-out screencap -p`.

## 9. Out of scope

- Bottom navigation visual restyle (separate plan).
- Real Help & safety screen.
- Real match count (waits for a `:match:domain` use case or a shared `:core:ownerstats`).
- Tablet layout / large screens.
- Dark mode polish — current scheme inherits the surface/onSurface tokens already, which is good enough.
- Locale-aware year formatting (just `Int.toString()` for now).
- Pluralization beyond the manual three-string switch (`pets` / `1 pet` / `Add your first pet`).

## 10. Risk / rollback

- **Risk**: `LazyRow` inside `verticalScroll` rare layout edge case where minHeight reports as zero. Mitigation: give the `LazyRow` a fixed `height(96.dp)`.
- **Risk**: cross-feature dep `:feature:profile:presentation → :feature:pet:domain` regresses the architecture audit. Mitigation: documented precedent in `build.gradle.kts`; if the audit flags it, lift the pet count into a new `:core:ownerstats:domain` JVM module in a follow-up.
- **Rollback**: revert the single `feat(profile): coral hero redesign with pet rail and manage list` commit. All changes are additive (new strings, new ViewModel field, new lambdas in route) except for the wholesale `ProfileScreen.kt` rewrite — the old composable can be restored from git history.

## 11. Implementation order

1. Add `:feature:pet:domain` dep to `feature/profile/presentation/build.gradle.kts` with the precedent comment.
2. Update `ProfileViewModel.kt`: inject `ObserveMyPetsUseCase`, add `pets` + `isPetsLoading` to `ProfileUiState`, third collector.
3. Update `ProfileNavModule.kt`: add `onAddPet` + `onOpenPet` lambdas.
4. Add strings to `feature/profile/presentation/src/main/res/values/strings.xml`.
5. Rewrite `ProfileScreen.kt` end-to-end per §3:
   - `ProfileRoute` collects state + holds the SnackbarHostState.
   - `Scaffold` + `Box(background)` + scrollable `Column`.
   - `CancelDeletionBanner` (unchanged) → `ProfileHeroCard` → `SectionHeader("MY PETS")` + LazyRow → `SectionHeader("MANAGE")` + Manage column → `SignOutPill`.
   - Add `@Preview`s.
6. Build (`assembleDebug` on `:feature:profile:presentation`, then `:app:installDebug`).
7. Launch app on emulator, sign in, navigate to Profile tab, screenshot.
8. Single commit: `feat(profile): coral hero redesign with pet rail and manage list`.

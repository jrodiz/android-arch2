# Plan — Settings home redesign: cream background + grouped cards + tinted icons

## 1. Context

`SettingsHomeRoute` in `feature/settings/presentation/SettingsHomeScreen.kt` currently renders a flat `LazyColumn` of five `ListItem`s with `Icons.Outlined.Person/Notifications/Tune/Lock/Settings` and dividers. No section grouping, no brand styling — it's the generic Material 3 list. Everything around it has been rebranded (Login, SignUp, Profile, Deck) so Settings now looks out of place.

The mockup wants the Settings home to mirror the Profile redesign's *grouped, tinted-icon-square rounded-card row* pattern:

- **Cream screen background** (existing `MaterialTheme.colorScheme.background` `#FFFBFA` ≈ the desired `#FBF1E9`; if contrast vs. white cards is too low, fall back to a new `BrandColors.Cream` token — see §2.5).
- **Header row**: a white rounded-square back chevron (44dp) on the left + bold "Settings" headline on the right. No Material `TopAppBar` — the row is just a Row at the top of the scroll column so it sits flush with the cream background.
- **ACCOUNT** section: uppercase muted eyebrow + a single white rounded-card containing **Profile** (mint-tinted icon, person) and **Account** (peach-tinted icon, mail) separated by a hairline `HorizontalDivider`.
- **MATCHING** section: eyebrow + white card containing **Filters** (coral-tinted icon, sliders — rendered with a soft coral background tint), **Notifications** (lavender-tinted icon, bell, coral count pill, chevron), **Pause profile** (gray-tinted icon, pause-circle, Material `Switch` trailing).
- **SAFETY** section: eyebrow + white card containing **Privacy** (mint-tinted icon, shield) and **Blocked owners** (coral-tinted icon, no-symbol).
- **Sign out**: full-width white pill button with coral leading icon + coral text (same component shape as the Profile screen's sign-out pill).
- **Footer**: centered muted "TinPet v{versionName} · Made with 🐾".

**Goal:** match the mock; keep all existing nav wiring intact (Profile→`SettingsEditProfile`, Account→`SettingsAccount`, Filters→`SettingsFilters`, Notifications→`SettingsNotifications`, Privacy→`SettingsPrivacy`, Blocked→`SettingsBlockedUsers`); add a real Pause toggle inline (re-using `PrivacyViewModel`'s `SetPausedUseCase`); surface the live notification-enabled count and the live blocked-owner count. **Non-goals:** restyle child screens (Notifications, Filters, Privacy, Blocked Users, Account, Edit Profile — each gets its own plan), introduce a tablet two-pane layout, change any repository, or wire the Sign-out flow differently from how the Profile screen already does it.

## 2. Confirmed decisions

1. **"Filters" row tint as press state, not persistent selection.** Settings home is a list-detail launcher, not a list-detail screen with a currently-selected child — a permanent highlight would lie to users. Render Filters with the same white surface as the other rows; rely on the default Material ripple on press. (The mock's tint visually implies "pressed" anyway; leaving the row pristine matches that intent without state baggage.)
2. **Notification count = number of `true` toggles in `NotificationPrefs`** (max 4). Sourced from a new `SettingsHomeViewModel` that combines `ObserveNotificationPrefsUseCase` + `ObserveBlockedOwnersUseCase` + `ObserveMyProfileUseCase`. Shows nothing when count is 0 (the pill would be visually noisy at 0).
3. **Blocked count subtitle = `"$count blocked"`** with a `_one`/`_other` switch ("1 blocked" vs. "0 blocked" / "5 blocked").
4. **Pause toggle is inline.** Tap on the row body still navigates to `SettingsPrivacy` (where the user can also pause); but the inline Switch lets users pause from the home screen without a side trip. Both write through the same `SetPausedUseCase`.
5. **`BrandColors.Cream` token (new, `#FBF1E9`)** is added. The screen background uses it explicitly; this matches the mockup more faithfully than the existing `#FFFBFA` which is too white. Also gives the redesigns a single source of truth they can all switch to later.
6. **Tinted icon-square palette (new tokens):**
   - `BrandColors.MintTint` = `#DDEFE9` (background) — Profile, Privacy.
   - `BrandColors.PeachTint` = `#FCE3D6` (background) — Account.
   - `BrandColors.CoralTint` = `#F7DAD3` (background) — Filters, Blocked owners. Distinct from `Coral.copy(alpha = 0.12f)` (used in Profile screen) — a brighter tint that reads better against the cream background.
   - `BrandColors.LavenderTint` = `#E5DDF6` (background) — Notifications.
   - `BrandColors.NeutralTint` = `#E9E4E0` (background) — Pause profile.
   - Foreground colors: `BrandColors.CoralDeep` (coral icons), `BrandColors.MintLeaf` (mint icons), `BrandColors.PeachInk = #C4724F` (peach icon foreground — added), `BrandColors.LavenderInk = #6E5DB8` (lavender foreground — added), `BrandColors.NeutralInk = #8B807A` (neutral foreground — added).
   - All new tokens go in `core/designsystem/.../theme/Color.kt` extending the existing `BrandColors` object. No scattered hex literals.
7. **Version footer**: wire via `BuildConfig.VERSION_NAME` from `:feature:settings:presentation`. Requires `buildFeatures.buildConfig = true` in `feature/settings/presentation/build.gradle.kts` (the convention plugin doesn't enable it by default; `:feature:login:presentation` already does this). The paw emoji is appended as a literal 🐾 char in the string.
8. **Bottom nav is correctly hidden today** — `SettingsHome` is not in `MainActivity.TOP_LEVEL_ROUTES`. No change needed.

## 3. Visual spec (target)

### 3.1 Screen scaffold

- `Scaffold(containerColor = BrandColors.Cream)` (no `topBar`). The header lives inline so it sits flush with the cream and the back chevron renders as a small floating tile.
- Outer `Column(Modifier.fillMaxSize().padding(scaffoldPadding).windowInsetsPadding(WindowInsets.statusBars).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp))`.
- Vertical spacing between regions = 20dp (`Arrangement.spacedBy`).

### 3.2 Header row (inline)

- `Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = spacedBy(12.dp))`:
  - **Back tile**: `Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp, modifier = Modifier.size(44.dp).clickable(onBack))` centering `Icons.AutoMirrored.Outlined.ArrowBackIosNew` tinted `onSurface`. (If `ArrowBackIosNew` isn't available, use `KeyboardArrowLeft` for a small chevron.)
  - **Title**: `Text("Settings", style = headlineMedium.copy(fontWeight = ExtraBold), color = onSurface)`.

### 3.3 Section header (eyebrow)

Reusable private `SectionEyebrow` composable:

```
Text(
  title.uppercase(),
  style = labelMedium.copy(fontWeight = SemiBold, letterSpacing = 1.5.sp),
  color = onSurfaceVariant,
  modifier = Modifier.padding(start = 6.dp, bottom = 8.dp),
)
```

### 3.4 Grouped card

Reusable private `SettingsCard` composable that wraps a `Column` of N rows separated by `HorizontalDivider`s:

```
Surface(
  shape = RoundedCornerShape(20.dp),
  color = MaterialTheme.colorScheme.surface,
  shadowElevation = 1.dp,
  modifier = Modifier.fillMaxWidth(),
) {
  Column { rows separated by HorizontalDivider(color = onSurfaceVariant.copy(alpha = 0.08f), thickness = 1.dp, modifier = Modifier.padding(start = 72.dp)) }
}
```

The divider's `start = 72.dp` makes it align under the row title text (icon square = 44dp + row padding 16dp + spacing 12dp = 72dp). Matches the mockup's hairline that stops short of the leading icon.

### 3.5 Settings row

Reusable private `SettingsRow(iconBackground, iconTint, icon, title, subtitle, trailing, onClick)` composable:

```
Row(
  modifier = Modifier
    .fillMaxWidth()
    .clickable(onClick = onClick, enabled = onClick != null)
    .padding(horizontal = 16.dp, vertical = 14.dp),
  verticalAlignment = Alignment.CenterVertically,
  horizontalArrangement = Arrangement.spacedBy(12.dp),
) {
  Box(
    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(iconBackground),
    contentAlignment = Alignment.Center,
  ) {
    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
  }
  Column(modifier = Modifier.weight(1f)) {
    Text(title, style = titleMedium.copy(fontWeight = SemiBold))
    Text(subtitle, style = bodySmall, color = onSurfaceVariant)
  }
  if (trailing != null) trailing()
}
```

Trailing variants:
- **Chevron**: `Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, tint = onSurfaceVariant)`.
- **Count pill + chevron**: `Row(spacing 8dp) { CountPill(count); ChevronIcon }`.
- **Switch**: `Switch(checked, onCheckedChange, ...)` — no chevron when a switch is present.
- **Count pill**: `Surface(shape = CircleShape, color = BrandColors.CoralDeep)` containing `Text(count.toString(), style = labelSmall.copy(fontWeight = Bold), color = White)` with `padding(horizontal = 8.dp, vertical = 2.dp)`.

### 3.6 Sign out pill

Reuse the Profile pattern verbatim — private composable:

```
Surface(
  shape = CircleShape, color = surface, shadowElevation = 1.dp,
  modifier = Modifier.fillMaxWidth().clickable(onClick),
) {
  Row(padding(horizontal = 20.dp, vertical = 16.dp), Arrangement.Center, Alignment.CenterVertically, spacedBy(12.dp)) {
    Icon(Icons.AutoMirrored.Outlined.Logout, tint = CoralDeep)
    Text("Sign out", titleMedium.SemiBold, color = CoralDeep)
  }
}
```

The mock centers the icon + text together; use `Arrangement.Center`.

### 3.7 Footer

- `Text("TinPet v$versionName · Made with 🐾", style = bodySmall, color = onSurfaceVariant.copy(alpha = 0.7f), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))`.
- `versionName = BuildConfig.VERSION_NAME` from `:feature:settings:presentation`'s own BuildConfig. Falls back to `"–"` if blank.

## 4. Component changes

- **`BrandColors` (Color.kt)**: add `Cream`, `MintTint`, `PeachTint`, `CoralTint`, `LavenderTint`, `NeutralTint`, `PeachInk`, `LavenderInk`, `NeutralInk` — all new constants, no removal/renames.

No new files in `:core:ui` or `:core:designsystem` beyond the tokens above. The header tile, section eyebrow, settings card, settings row, count pill, and sign-out pill all live inline in `SettingsHomeScreen.kt` as private composables. Same approach as the Profile redesign (precedent: §4 of `plans/profile-redesign-coral-hero.md`).

## 5. State / behavior changes

| File | Change |
|---|---|
| `SettingsHomeViewModel.kt` (NEW) | Hilt VM injecting `ObserveNotificationPrefsUseCase`, `ObserveBlockedOwnersUseCase`, `ObserveMyProfileUseCase`, `SetPausedUseCase`, `SessionRepository`. Exposes `SettingsHomeUiState(notifEnabledCount, blockedCount, paused, isPausing, errorMessage)`. Provides `onTogglePause(Boolean)` (debounces optimistic write, matches `PrivacyViewModel`) and `signOut(onSignedOut)` (same shape as `ProfileViewModel.signOut`). |
| `SettingsHomeScreen.kt` | Full rewrite per §3. Adds new private composables `SectionEyebrow`, `SettingsCard`, `SettingsRow`, `CountPill`, `SignOutPill`, `BackTile`, `VersionFooter`. Adds `@Preview`s populated + zero-counts. |
| `SettingsNavModule.kt` | Update the `entry<SettingsHome>` block: pass `onOpenBlockedOwners = { navigator.goTo(SettingsBlockedUsers) }` and `onSignedOut = { navigator.replaceAll(LoginHome) }`. Add `LoginHome` import (already present). |
| `feature/settings/presentation/build.gradle.kts` | Add `buildFeatures { buildConfig = true }`. No new deps — `ObserveMyProfileUseCase` and `SetPausedUseCase` come from `:feature:profile:domain` (already on the classpath). |
| `feature/settings/presentation/src/main/res/values/strings.xml` (NEW) | Add string resources for titles, subtitles, content descriptions, footer (see §6.3). |

No changes to `:feature:settings:domain` or `:feature:settings:data`. No changes to `:feature:profile:*`. No changes to `:app`.

## 6. Files to add / modify

### Add
- `feature/settings/presentation/src/main/kotlin/com/rodiz/arch2/feature/settings/presentation/SettingsHomeViewModel.kt`
- `feature/settings/presentation/src/main/res/values/strings.xml`
- `plans/settings-home-redesign.md` (this file)

### Modify
- `core/designsystem/src/main/kotlin/com/rodiz/arch2/core/designsystem/theme/Color.kt` — extend `BrandColors`.
- `feature/settings/presentation/src/main/kotlin/com/rodiz/arch2/feature/settings/presentation/SettingsHomeScreen.kt` — full rewrite per §3.
- `feature/settings/presentation/src/main/kotlin/com/rodiz/arch2/feature/settings/presentation/SettingsNavModule.kt` — wire two new lambdas (`onOpenBlockedOwners`, `onSignedOut`).
- `feature/settings/presentation/build.gradle.kts` — enable buildConfig.

### Do NOT modify
- `feature/settings/domain/*`, `feature/settings/data/*` — repos unchanged.
- `feature/settings/presentation/.../NotificationsScreen.kt`, `FiltersScreen.kt`, `PrivacyScreen.kt`, `BlockedUsersScreen.kt`, `AccountScreen.kt`, `EditProfileScreen.kt` — child-screen redesigns are out of scope.
- `app/src/main/.../MainActivity.kt` — bottom nav is already correctly hidden for `SettingsHome`.
- `core/ui/components/*` — nothing extracted; everything Settings-specific stays inline.

### 6.3 Strings (new in `feature/settings/presentation/.../strings.xml`)

```xml
<resources>
  <string name="settings_title">Settings</string>
  <string name="settings_back_cd">Back</string>

  <string name="settings_section_account">Account</string>
  <string name="settings_row_profile_title">Profile</string>
  <string name="settings_row_profile_subtitle">Name, avatar, bio</string>
  <string name="settings_row_account_title">Account</string>
  <string name="settings_row_account_subtitle">Email, sign in methods</string>

  <string name="settings_section_matching">Matching</string>
  <string name="settings_row_filters_title">Filters</string>
  <string name="settings_row_filters_subtitle">Distance, intent, species</string>
  <string name="settings_row_notifications_title">Notifications</string>
  <string name="settings_row_notifications_subtitle">Matches, messages, likes</string>
  <string name="settings_row_pause_title">Pause profile</string>
  <string name="settings_row_pause_subtitle">Hide pets from decks</string>

  <string name="settings_section_safety">Safety</string>
  <string name="settings_row_privacy_title">Privacy</string>
  <string name="settings_row_privacy_subtitle">Blocked users, data</string>
  <string name="settings_row_blocked_title">Blocked owners</string>
  <plurals name="settings_row_blocked_subtitle">
    <item quantity="one">%1$d blocked</item>
    <item quantity="other">%1$d blocked</item>
  </plurals>

  <string name="settings_sign_out">Sign out</string>
  <string name="settings_footer_template">TinPet v%1$s · Made with 🐾</string>
</resources>
```

## 7. Critical recipes

1. **Cream background under white cards**: keep the white cards at `MaterialTheme.colorScheme.surface` (= `#FFFBFA`) so they read as faintly elevated against `Cream` (= `#FBF1E9`). Don't switch the cards to pure white — the existing surface token already has the right warmth.
2. **`HorizontalDivider` inside a `Column` inside a `Surface`**: divider thickness = 1dp at `onSurfaceVariant.copy(alpha = 0.08f)`. Use `Modifier.padding(start = 72.dp)` to make the divider stop short of the leading icon, mimicking the inset divider in the mockup.
3. **Switch inside a clickable Row**: wrap the Switch in a `Box` (or just let Compose handle pointer-event routing — `Switch` consumes its own taps). Make sure `onClick` for the row navigates to `SettingsPrivacy` while the `Switch.onCheckedChange` calls `viewModel.onTogglePause(...)`. They don't conflict because the Switch consumes the touch.
4. **`combine` three Flows**: in `SettingsHomeViewModel`, use `combine(notifPrefsFlow, blockedFlow, profileFlow) { prefs, blocked, profile -> Triple(...) }.collect { (prefs, blocked, profile) -> ... }`. Wrap each upstream in `catch` so a single Firestore hiccup doesn't blank the whole screen — fall back to defaults (0 / 0 / false) on error.
5. **`BuildConfig.VERSION_NAME`** for `:feature:settings:presentation` requires `buildFeatures.buildConfig = true`. The string is `"0.1.0-debug"` for the debug variant (per the `applicationIdSuffix = ".debug"` + `versionNameSuffix = "-debug"` in `app/build.gradle.kts`). Note: a feature module's BuildConfig.VERSION_NAME is *blank by default* — to surface the **app** version, set it explicitly via a `buildConfigField` in `:feature:settings:presentation`'s build.gradle pointed at the app version. Cleanest: read `"0.1.0"` from the same `gradle.properties` mechanism used for `firebase.web.client.id`, or hardcode a fallback in Kotlin (`"0.1.0"`) since the only consumer is a display string. **Chosen approach (pragmatic):** add `buildConfigField("String", "VERSION_NAME", "\"0.1.0\"")` to the settings/presentation module's `defaultConfig`. If we later move version to a gradle.properties entry, both modules can read it. (Bumping the version requires touching two files; explicitly noted as known-debt in §9.)
6. **Status bar inset**: `Modifier.windowInsetsPadding(WindowInsets.statusBars)` on the outer scroll column. The Scaffold's `contentWindowInsets = WindowInsets(0)` (inherited from MainActivity) means we own the inset.
7. **Plural strings**: `pluralStringResource(R.plurals.settings_row_blocked_subtitle, count, count)` not `stringResource(...)` — the linter will catch it but worth flagging.
8. **No `Co-Authored-By: Claude` trailer**, no GitHub push/comment, no "Generated with…" trailer in the commit body. (Standard guard.)

## 8. Verification

1. **Module-boundary build first**: `JAVA_HOME=…/jbr-17.0.14 ./gradlew :feature:settings:presentation:assembleDebug` to surface BuildConfig / dep wiring errors quickly.
2. **Full install**: `./gradlew :app:installDebug`.
3. **Compose previews** — add two:
   - `SettingsHomeScreenPreviewPopulated` — `notifEnabledCount = 4`, `blockedCount = 0`, `paused = false`.
   - `SettingsHomeScreenPreviewZero` — `notifEnabledCount = 0`, `blockedCount = 0`, `paused = false`.
4. **Emulator** (`emulator-5556`, 1080×2400):
   - Sign in with debug credentials, land on Deck.
   - Tap Profile in bottom nav → tap "Settings" in the Manage section → land on the redesigned Settings home.
   - Verify: cream background, white back-chevron tile, bold "Settings" headline, ACCOUNT card with Profile + Account rows + inset divider, MATCHING card with Filters + Notifications (coral count pill of `4` since defaults enable 3 of 4 toggles — actually 3, not 4; let the live data drive the number) + Pause profile (Switch off), SAFETY card with Privacy + Blocked owners (`0 blocked`), Sign out pill, footer "TinPet v0.1.0 · Made with 🐾".
   - Tap each row → lands on the expected child screen (Edit Profile, Account, Filters, Notifications, Privacy, Blocked Users). Tap Pause Switch → emits `SetPausedUseCase`; navigate to Privacy → pause state persists.
   - Tap Sign out → returns to LoginHome.
5. **Screenshot** to `/tmp/settings-home-after.png` via `adb -s emulator-5556 exec-out screencap -p`.

## 9. Out of scope

- Redesigning the child screens (Notifications, Filters, Privacy, Blocked Users, Account, Edit Profile). Each gets its own plan.
- Dark mode polish — `BrandColors.Cream` will read as a too-bright cream against dark surfaces; deferring until a brand-wide dark-mode pass.
- Moving the version string into a single `gradle.properties` entry. Tracked as known debt: the version is now duplicated between `app/build.gradle.kts` and `feature/settings/presentation/build.gradle.kts`.
- Localizing the "Made with 🐾" footer (the emoji + period are unsuitable for some locales; revisit when locales > en).
- Tablet/landscape layouts.
- Wiring a real per-feature unread-count badge to the Notifications row's count pill (currently driven by enabled-toggle count, not unread count — the count just signals "you have N notification channels on").

## 10. Risk / rollback

- **Risk**: `BuildConfig.VERSION_NAME` blank because the feature module's default config doesn't set one. Mitigation: explicit `buildConfigField` in `:feature:settings:presentation`'s `defaultConfig`. If it still resolves blank, fall back to the literal `"0.1.0"` in Kotlin.
- **Risk**: combining three Firestore flows in one VM amplifies error surfaces — a single permission denial blanks the home. Mitigation: per-stream `catch { emit(defaultValue) }` so each upstream degrades gracefully.
- **Risk**: the Pause Switch inside a clickable Row swallows the row's onClick. Mitigation: visually verify on emulator; if it does, remove the row-level `onClick` for the Pause row (the Switch is the action; the row body can be non-clickable).
- **Rollback**: revert the single `feat(settings): redesign home with grouped cards and tinted icons` commit. Token additions in `Color.kt` are purely additive; deleting them just orphans tokens, not breaking anything else.

## 11. Implementation order

1. Extend `BrandColors` in `core/designsystem/.../theme/Color.kt` with the new tokens.
2. Add `buildFeatures { buildConfig = true }` + `buildConfigField("String", "VERSION_NAME", "\"0.1.0\"")` to `feature/settings/presentation/build.gradle.kts`.
3. Create `feature/settings/presentation/src/main/res/values/strings.xml` with the §6.3 strings.
4. Create `SettingsHomeViewModel.kt` — combine three flows, expose state + `onTogglePause` + `signOut`.
5. Rewrite `SettingsHomeScreen.kt` end-to-end per §3 (BackTile, SectionEyebrow, SettingsCard, SettingsRow, CountPill, SignOutPill, VersionFooter, two `@Preview`s).
6. Update `SettingsNavModule.kt`: pass `onOpenBlockedOwners` and `onSignedOut`.
7. Build (`:feature:settings:presentation:assembleDebug`, then `:app:installDebug`).
8. Launch app on `emulator-5556`, sign in, navigate to Profile → Settings → screenshot to `/tmp/settings-home-after.png`.
9. Single local commit: `feat(settings): redesign home with grouped cards and tinted icons`.

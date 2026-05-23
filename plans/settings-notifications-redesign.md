# Plan — Settings → Notifications redesign: status banner + grouped cards + quiet hours

## 1. Context

`NotificationsRoute` in `feature/settings/presentation/NotificationsScreen.kt` is the
generic Material 3 list: a `TopAppBar("Notifications")` over a single `LazyColumn`
of four `ListItem`s with `Switch` trailing, separated by `HorizontalDivider`s. No
brand styling, no section grouping, no permission-state banner, no quiet hours.
The Settings home was recently rebranded with cream background + grouped white
cards + tinted icon squares (see `plans/settings-home-redesign.md`), and the
Notifications child screen now reads as Material-default against that.

The mockup (`/Users/jrodiz/Desktop/notifications.png`) wants the same visual
language as the Settings home, plus two new things:

- **Top status banner** (coral-tinted soft pill) that surfaces whether the OS-level
  push permission is granted. When granted, it reassures the user that toggle gating
  still applies; when denied, it offers a "Turn on" link that deep-links to system
  settings.
- **QUIET HOURS** section with a single row showing the configured window
  (default 22:00 → 08:00) and a Switch to enable/disable. Tapping the row body
  opens a time-range picker; that picker is **deferred to a follow-up** — this
  PR ships the row and the persisted model, the picker is `// TODO` snackbar.

**Goal:** match the mockup, keep all existing wiring intact (`ObserveNotificationPrefsUseCase` /
`UpdateNotificationPrefsUseCase` via `FirestoreNotificationPrefsRepository`), extend
the `NotificationPrefs` domain model with three new fields (`quietHoursEnabled`,
`quietHoursStartMinutes`, `quietHoursEndMinutes`), and wire the runtime permission
read so the banner reflects reality. **Non-goals:** ship the time-range picker UI;
restyle the Settings home (already shipped); change Cloud Functions delivery (the
quiet-hours model is client-only for v1 — server-side enforcement is a follow-up
flagged in §9); add new icon glyphs to `:core:designsystem`.

## 2. Confirmed decisions

1. **Status banner has two states; both implemented in this PR.**
   - **Granted (default on API < 33):** coral-filled square (40dp, `RoundedCornerShape(12dp)`,
     `BrandColors.Coral` background) with white `Icons.Outlined.Notifications`
     glyph, title "Push notifications on" in `BrandColors.CoralDeep` `SemiBold`,
     subtitle "We'll send only what you toggle on below" in `CoralDeep.copy(alpha = 0.75f)`.
   - **Denied (API 33+ only):** same square + glyph but title flips to "Push
     notifications off" and subtitle becomes a single line "Turn on" rendered
     as a small coral chip on the right side of the banner. Tapping anywhere
     on the banner (or the chip) fires an Intent to the app's notification
     system-settings page (`Settings.ACTION_APP_NOTIFICATION_SETTINGS` with
     `EXTRA_APP_PACKAGE`).
   - Permission state read via `ContextCompat.checkSelfPermission` at composition
     time, re-evaluated on lifecycle resume (so flipping the toggle in system
     settings and returning updates the banner). Implemented inline with a
     `LifecycleEventEffect(Lifecycle.Event.ON_RESUME)` that triggers a
     `mutableStateOf<Boolean>` re-read.
2. **Quiet hours model lives in `NotificationPrefs`.** Three new fields:
   - `quietHoursEnabled: Boolean` (default `false`)
   - `quietHoursStartMinutes: Int` (minutes since midnight, default 22*60 = 1320)
   - `quietHoursEndMinutes: Int` (minutes since midnight, default 8*60 = 480)
   - Persisted to Firestore under the same `notifications.*` map alongside the
     existing four toggles. Backwards-compatible: existing documents missing
     these fields fall back to defaults.
3. **Quiet-hours time picker is deferred.** Tapping the row body shows a
   snackbar "Custom quiet-hours coming soon" (matching the precedent of the
   `// TODO` cells in `BlockedUsersScreen`). The plumbing is in place: when a
   future PR replaces the snackbar with a real `TimePicker`-based dialog, only
   the click handler changes — no model/repo work.
4. **Tinted icon-square palette per row** (matches mock exactly):
   - New match: `CoralTint` background, `CoralDeep` glyph (`Icons.Outlined.FavoriteBorder`).
     This re-uses the existing token added in `plans/settings-home-redesign.md`.
   - New message: `LavenderTint` background, `LavenderInk` glyph (`Icons.AutoMirrored.Outlined.Message`).
   - Someone liked your pet: `PeachTint` background, `PeachInk` glyph (`Icons.Outlined.AutoAwesome` — a 4-point sparkle).
   - Weekly digest: new `MintTint` background but with a **distinct** soft mint
     foreground `MintLeaf` glyph (`Icons.Outlined.Notifications` — re-use the bell).
   - Quiet hours: `Color(0xFFFFF1C2)` (soft yellow) background, `Color(0xFFE5B73B)`
     (warm yellow) glyph (`Icons.Outlined.Bedtime` — a crescent moon, the closest
     out-of-box Material icon to the mock's emoji moon). These two are added as
     new `BrandColors.MoonTint` + `BrandColors.MoonInk` tokens in
     `core/designsystem/.../theme/Color.kt`.
5. **Banner is rendered above the first eyebrow**, with the same 20dp
   inter-region spacing the Settings home uses. The screen layout is otherwise
   a clone of the Settings home recipe: cream background, inline back-tile + bold
   "Notifications" headline, then the banner, then ACTIVITY card, then QUIET
   HOURS card, then 16dp tail spacer.
6. **Switch styling matches the Settings home pause Switch** (coral checked track,
   white thumb, transparent unchecked border). The mock's switch has a faint coral
   tail in the OFF state; this is the default Material 3 unchecked track with
   `onSurfaceVariant.copy(alpha = 0.35f)` which already reads as a soft warm grey
   against the white card. Don't tint OFF tracks coral — that misreads as ON.
7. **No `TopAppBar`** — the inline `BackTile` + bold headline pattern from the
   Settings home is the contract.
8. **Bottom nav is correctly hidden** — `SettingsNotifications` is not in
   `MainActivity.TOP_LEVEL_ROUTES`. No change needed.

## 3. Visual spec (target)

### 3.1 Screen scaffold

- `Scaffold(containerColor = BrandColors.Cream, snackbarHost = …)` (no `topBar`).
- Outer `Column(Modifier.fillMaxSize().padding(scaffoldPadding).windowInsetsPadding(WindowInsets.statusBars).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = spacedBy(20.dp))`.

### 3.2 Header row (inline)

Same as the Settings home `SettingsHeader`:

- 44dp `RoundedCornerShape(14.dp)` white `Surface(shadowElevation = 1.dp)` with
  `Icons.AutoMirrored.Outlined.KeyboardArrowLeft` centered.
- `Text("Notifications", style = headlineMedium.copy(fontWeight = ExtraBold), color = onSurface)`.
- 14dp gap between tile and title.

### 3.3 Status banner

```
Surface(
  shape = RoundedCornerShape(20.dp),
  color = BrandColors.Coral.copy(alpha = 0.20f),
  modifier = Modifier.fillMaxWidth().clickable(enabled = !granted, onClick = openSettings),
) {
  Row(padding(horizontal = 14.dp, vertical = 14.dp), Arrangement.spacedBy(14.dp), Alignment.CenterVertically) {
    Box(
      modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(BrandColors.Coral),
      contentAlignment = Alignment.Center,
    ) { Icon(Icons.Outlined.Notifications, contentDescription = null, tint = White, modifier = Modifier.size(22.dp)) }
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = if (granted) "Push notifications on" else "Push notifications off",
        style = titleMedium.copy(fontWeight = Bold),
        color = BrandColors.CoralDeep,
      )
      Text(
        text = if (granted) "We'll send only what you toggle on below" else "Tap to turn on in system settings",
        style = bodySmall,
        color = BrandColors.CoralDeep.copy(alpha = 0.75f),
      )
    }
    if (!granted) {
      Surface(shape = CircleShape, color = BrandColors.CoralDeep) {
        Text("Turn on", style = labelSmall.SemiBold, color = White, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
      }
    }
  }
}
```

### 3.4 Section eyebrow

Re-use the visual recipe from Settings home (private `SectionEyebrow` composable):

```
Text(
  title.uppercase(),
  style = labelMedium.copy(fontWeight = SemiBold, letterSpacing = 1.5.sp),
  color = onSurfaceVariant,
  modifier = Modifier.padding(start = 6.dp, bottom = 10.dp),
)
```

### 3.5 Notification row (clone of `SettingsRow` from Settings home)

`NotificationToggleRow(icon, iconBackground, iconTint, title, subtitle, checked, onCheckedChange, testTag, onRowClick = null)`:

- Outer `Row(padding(horizontal = 16.dp, vertical = 14.dp), spacedBy(12.dp), Alignment.CenterVertically)`.
- Leading 44dp `RoundedCornerShape(12.dp)` icon square.
- Column with `titleMedium SemiBold` title + `bodySmall onSurfaceVariant` subtitle, `weight(1f)`, both `maxLines = 1, overflow = Ellipsis` (the longer subtitles for "Someone liked your pet" and the moon row need `maxLines = 2` — see §3.6 / §3.7).
- Trailing `Switch(checked, onCheckedChange, colors = brandedSwitchColors())`.
- The row body itself is **not** clickable for toggle rows; the Switch consumes
  the touch. (Tapping the row title shouldn't toggle the switch — that pattern
  conflicts with the picker row in §3.7.)

### 3.6 ACTIVITY card

`SettingsCard { … }` wrapping four `NotificationToggleRow`s separated by
`RowDivider()` (the start = 72dp inset divider). The "Someone liked your pet"
subtitle is longer ("When another owner likes one of your pets") — bump
`maxLines = 2` for the subtitle on every row in this card so the longest one
doesn't ellipsize at 1080×2400.

### 3.7 QUIET HOURS card

`SettingsCard { QuietHoursRow(...) }`. Single row, no divider.

- Leading 44dp `MoonTint` square with `Icons.Outlined.Bedtime` in `MoonInk`.
- Title text: `"$startHHmm → $endHHmm"` rendered in `titleMedium SemiBold`
  (e.g. "22:00 → 08:00"). The "→" is U+2192 (rightwards arrow). Bold to
  match the mock's prominence.
- Subtitle: "No pushes during quiet hours" in `bodySmall onSurfaceVariant`,
  `maxLines = 1`.
- Trailing Switch with same coral track styling.
- **Whole row is clickable** (above-Switch hit region) — fires
  `onPickQuietHours()` which currently calls `viewModel.requestPickerSnackbar()`
  → snackbar "Custom quiet-hours coming soon". The Switch still works
  independently because Compose's pointer dispatch consumes the toggle there.

`HHmm` is rendered by `formatMinutesAsHHmm(minutes: Int)` in
`NotificationsScreen.kt` — pure Kotlin, no `LocalTime`: `String.format("%02d:%02d",
minutes / 60, minutes % 60)`.

## 4. Component changes

- **`BrandColors` (Color.kt)**: add `MoonTint = #FFF1C2`, `MoonInk = #E5B73B`. No
  other tokens added; all existing tints (`CoralTint`, `LavenderTint`, `PeachTint`,
  `MintTint`, `Coral`, `CoralDeep`, `MintLeaf`, `PeachInk`, `LavenderInk`) are
  re-used as-is.
- **`NotificationPrefs` (domain)**: extend with `quietHoursEnabled: Boolean = false`,
  `quietHoursStartMinutes: Int = 22 * 60`, `quietHoursEndMinutes: Int = 8 * 60`.
  Update `DEFAULT` constant accordingly.
- **`FirestoreNotificationPrefsRepository` (data)**: extend `toMap()` and
  `toPrefsOrDefault()` to (de)serialize the three new fields. Use `Long`
  serialization for minutes (Firestore converts `Int` to `Long`); read back as
  `(this["quietHoursStartMinutes"] as? Number)?.toInt() ?: d.quietHoursStartMinutes`.
- **`NotificationsScreen.kt`**: full rewrite per §3 — private composables
  `NotificationsHeader`, `PushPermissionBanner`, `SectionEyebrow`, `SettingsCard`,
  `NotificationToggleRow`, `QuietHoursRow`, `RowDivider`, `brandedSwitchColors()`,
  `formatMinutesAsHHmm()`. Add two `@Preview`s (granted + denied).
- **`NotificationsViewModel.kt`**: add three new toggle handlers
  (`onToggleQuietHoursEnabled(Boolean)`, future `onSetQuietHours(start: Int, end: Int)`).
  Add `onRequestPickerSnackbar()` that pushes a transient message into
  `errorMessage` (re-use the existing snackbar wiring) with copy "Custom
  quiet-hours coming soon". This is purely additive — the four existing
  handlers (`onToggleNewMatch`, etc.) are unchanged.

No new files in `:core:ui` or `:core:designsystem` beyond the two tokens. No
changes to navigation, DI modules, or `:app`.

## 5. State / behavior changes

| File | Change |
|---|---|
| `core/designsystem/.../theme/Color.kt` | Add `BrandColors.MoonTint` (`#FFF1C2`) and `BrandColors.MoonInk` (`#E5B73B`). |
| `feature/settings/domain/.../model/NotificationPrefs.kt` | Add `quietHoursEnabled`, `quietHoursStartMinutes`, `quietHoursEndMinutes` with defaults. Update `DEFAULT`. |
| `feature/settings/data/.../FirestoreNotificationPrefsRepository.kt` | (De)serialize the three new fields. |
| `feature/settings/presentation/.../NotificationsScreen.kt` | Full rewrite per §3. Adds private composables (`NotificationsHeader`, `PushPermissionBanner`, `SectionEyebrow`, `SettingsCard`, `NotificationToggleRow`, `QuietHoursRow`, `RowDivider`, `brandedSwitchColors`, `formatMinutesAsHHmm`). Two `@Preview`s. |
| `feature/settings/presentation/.../NotificationsViewModel.kt` | Add `onToggleQuietHoursEnabled(Boolean)` (calls `patch`). Add `onRequestPickerSnackbar()` which pushes a snackbar message via `_uiState.update { it.copy(errorMessage = "...") }`. The existing snackbar wiring catches it and dismisses. |
| `feature/settings/presentation/src/main/res/values/strings.xml` | Add resources for titles, subtitles, content descriptions, banner text, eyebrows, picker-coming-soon copy. |

No changes to `:feature:settings:nav`, DI modules, `:app`, `:feature:profile:*`,
or any other module. Settings home's notification count pill keeps working —
adding three new fields doesn't change the count logic (it sums only the four
existing booleans).

## 6. Files to add / modify

### Add
- `plans/settings-notifications-redesign.md` (this file).

### Modify
- `core/designsystem/src/main/kotlin/com/rodiz/arch2/core/designsystem/theme/Color.kt`
- `feature/settings/domain/src/main/kotlin/com/rodiz/arch2/feature/settings/domain/model/NotificationPrefs.kt`
- `feature/settings/data/src/main/kotlin/com/rodiz/arch2/feature/settings/data/FirestoreNotificationPrefsRepository.kt`
- `feature/settings/presentation/src/main/kotlin/com/rodiz/arch2/feature/settings/presentation/NotificationsScreen.kt`
- `feature/settings/presentation/src/main/kotlin/com/rodiz/arch2/feature/settings/presentation/NotificationsViewModel.kt`
- `feature/settings/presentation/src/main/res/values/strings.xml`

### Do NOT modify
- `feature/settings/nav/*` — no new routes.
- `feature/settings/domain/repository/NotificationPrefsRepository.kt` — interface unchanged.
- `feature/settings/domain/usecase/SettingsUseCases.kt` — use cases unchanged.
- `feature/settings/presentation/SettingsHomeScreen.kt` / `SettingsHomeViewModel.kt`
  — Settings home is out of scope (notification-count logic still uses only the
  four toggles).
- `app/src/main/.../MainActivity.kt` — bottom nav already correctly hidden for
  `SettingsNotifications`.
- `core/ui/components/*` — nothing extracted; everything Notifications-specific
  stays inline (mirrors Settings home decision).

## 7. Strings (new in `feature/settings/presentation/.../strings.xml`)

```xml
<!-- Notifications screen -->
<string name="notifications_title">Notifications</string>
<string name="notifications_back_cd">Back</string>

<string name="notifications_banner_on_title">Push notifications on</string>
<string name="notifications_banner_on_subtitle">We'll send only what you toggle on below</string>
<string name="notifications_banner_off_title">Push notifications off</string>
<string name="notifications_banner_off_subtitle">Tap to turn on in system settings</string>
<string name="notifications_banner_turn_on">Turn on</string>

<string name="notifications_section_activity">Activity</string>
<string name="notifications_row_new_match_title">New match</string>
<string name="notifications_row_new_match_subtitle">When you and another pet match</string>
<string name="notifications_row_new_message_title">New message</string>
<string name="notifications_row_new_message_subtitle">When someone messages you</string>
<string name="notifications_row_liked_title">Someone liked your pet</string>
<string name="notifications_row_liked_subtitle">When another owner likes one of your pets</string>
<string name="notifications_row_digest_title">Weekly digest</string>
<string name="notifications_row_digest_subtitle">A summary of activity once a week</string>

<string name="notifications_section_quiet_hours">Quiet hours</string>
<string name="notifications_quiet_hours_subtitle">No pushes during quiet hours</string>
<string name="notifications_quiet_hours_picker_soon">Custom quiet-hours coming soon</string>
<string name="notifications_quiet_hours_window_template">%1$s → %2$s</string>
```

## 8. Critical recipes

1. **Runtime permission read on API 33+.** Inline helper:
   ```kotlin
   @Composable
   private fun rememberPushPermissionGranted(): State<Boolean> {
       val context = LocalContext.current
       val granted = remember { mutableStateOf(checkPushGranted(context)) }
       val lifecycleOwner = LocalLifecycleOwner.current
       DisposableEffect(lifecycleOwner) {
           val observer = LifecycleEventObserver { _, event ->
               if (event == Lifecycle.Event.ON_RESUME) {
                   granted.value = checkPushGranted(context)
               }
           }
           lifecycleOwner.lifecycle.addObserver(observer)
           onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
       }
       return granted
   }
   private fun checkPushGranted(context: Context): Boolean =
       if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) true
       else ContextCompat.checkSelfPermission(
           context, Manifest.permission.POST_NOTIFICATIONS,
       ) == PackageManager.PERMISSION_GRANTED
   ```
   `LocalLifecycleOwner` comes from `androidx.lifecycle:lifecycle-runtime-compose`
   (already on the classpath via `androidx.lifecycle.compose.collectAsStateWithLifecycle`).
2. **Opening app notification settings.**
   ```kotlin
   val context = LocalContext.current
   val openSettings: () -> Unit = {
       val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
           putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
       }
       context.startActivity(intent)
   }
   ```
   Falls back gracefully (the intent exists since API 26 = minSdk).
3. **Switch inside a clickable Row** (quiet-hours row): same trick as the
   Settings home pause row — Compose pointer dispatch routes the Switch tap
   to the Switch and the rest of the row to the row's `onClick`. Verified
   working in the Settings home redesign.
4. **Firestore `Int` round-trip.** Firestore stores numbers as `Long`. Always
   read minutes back as `(this["quietHoursStartMinutes"] as? Number)?.toInt()` —
   `as? Int` would return `null` for a `Long` value and silently fall back to
   the default. (This was the bug pattern that bit the Privacy screen's
   `paused` boolean before it was migrated to `as? Boolean`.)
5. **`Icons.Outlined.Bedtime` vs. `NightlightRound`.** Material 3 icons bundle
   ships `Bedtime` (`androidx.compose.material.icons.outlined.Bedtime`) — a
   crescent moon — which matches the mock visually. `NightlightRound` is an
   alternative filled-circle moon, less close to the line-art moon in the mock.
6. **Snackbar for "picker coming soon".** Re-use the existing
   `LaunchedEffect(state.errorMessage)` → `snackbarHostState.showSnackbar(it)`
   → `viewModel.clearError()` chain. Rename the state field in the ViewModel
   from `errorMessage` to `transientMessage`? No — keep as `errorMessage` to
   avoid touching the snackbar wiring; the field is overloaded semantically
   but UX-equivalent (both are dismissible one-shot messages).
7. **Status bar.** Cream-on-white screen — keep dark status bar icons (the
   theme default). No `LightStatusBarIconsWhileShown()` call.
8. **No `Co-Authored-By: Claude` trailer**; commit body must not reference
   Claude/Anthropic/AI.

## 9. Verification

1. **Module-boundary build first**: `JAVA_HOME=…/jbr-17.0.14 ./gradlew :feature:settings:presentation:assembleDebug` to surface model/repo wiring errors quickly (the domain + data changes are the riskiest part).
2. **Full install**: `./gradlew :app:installDebug`.
3. **Compose previews** — two:
   - `NotificationsScreenPreviewGranted` — permission granted, all four activity toggles on, quiet hours on at 22:00 → 08:00.
   - `NotificationsScreenPreviewDenied` — permission denied banner, three toggles on + digest off, quiet hours off.
4. **Emulator** (`emulator-5556`, 1080×2400):
   - Sign in with debug credentials, land on Deck.
   - Bottom-nav Profile → tap Settings card → tap Notifications row → land on the redesigned Notifications screen.
   - Verify: cream background, back tile + bold "Notifications", coral banner with bell + "Push notifications on" + reassuring subtitle, ACTIVITY eyebrow + 4-row card with the tints from §2.4, QUIET HOURS eyebrow + single-row card showing "22:00 → 08:00" with the Switch.
   - Toggle each Activity Switch — verify the optimistic update is instant and persists across re-entry (back → Settings → Notifications again).
   - Toggle the quiet-hours Switch — verify it persists.
   - Tap the quiet-hours row body (not the Switch) — verify the snackbar "Custom quiet-hours coming soon" appears.
   - Long-press the Notifications row in Settings → revoke `POST_NOTIFICATIONS` in system settings → return to the app → verify the banner flips to "Push notifications off" with the "Turn on" chip.
   - Tap the banner — verify the system app-notification-settings page opens.
5. **Screenshot** to `/tmp/notifications-after.png` via `adb -s emulator-5556 exec-out screencap -p`.
6. **Settings home count pill regression**: revisit Settings home; the count pill
   should still show the count of the *four* original toggles, unaffected by
   the new quiet-hours field.
7. **Server-side enforcement**: out of scope. Cloud Functions still send pushes
   based on the original four toggles. Quiet hours is a v1 client-only display
   contract — a follow-up plan will teach Functions to honor it server-side
   (see §11).

## 10. Out of scope

- The quiet-hours time-range picker UI (deferred). The plumbing supports it;
  only the click handler changes when a future PR ships it.
- Server-side enforcement of quiet hours. v1 is client-display-only; Cloud
  Functions will need to read the prefs and honor the window before sending
  pushes (deferred to a follow-up).
- Per-channel mute persistence to system settings (Android already lets users
  mute individual channels; this screen reflects in-app prefs only).
- Localizing time-of-day to the user's locale's preferred format (12h vs 24h).
  Currently hardcoded to "HH:mm". Revisit when locales > en or when the picker ships.
- Restyling the post-signup `NotificationRationaleScreen` in `:feature:notifications:presentation` — different screen, different flow.
- Dark mode polish — the cream/coral palette is light-mode-first; dark mode is a
  separate brand-wide pass.

## 11. Risk / rollback

- **Risk**: Firestore deserialization of the new `Long`-typed minute fields. Mitigation: read via `(value as? Number)?.toInt()` so existing documents (which lack the fields and return `null`) safely fall back to defaults.
- **Risk**: the inline lifecycle observer for permission re-reads on `ON_RESUME` leaks if `DisposableEffect` isn't paired with `onDispose`. Mitigation: tested pattern (matches the `LightStatusBarIconsWhileShown` recipe in `SignUpScreen`).
- **Risk**: the snackbar "picker coming soon" message is overloaded onto the
  `errorMessage` field. Mitigation: the UI flow is identical (one-shot,
  dismissed-on-show, cleared via `clearError()`); no functional regression.
- **Rollback**: revert the single `feat(settings): redesign notifications with banner and quiet hours` commit. The three added `NotificationPrefs` fields are backwards-compatible (default-on-read); existing Firestore docs are unaffected. Token additions in `Color.kt` are purely additive.

## 12. Implementation order

1. Extend `BrandColors` in `core/designsystem/.../theme/Color.kt` (`MoonTint`, `MoonInk`).
2. Extend `NotificationPrefs` (`feature/settings/domain/.../model/NotificationPrefs.kt`) with the three new fields + defaults.
3. Extend `FirestoreNotificationPrefsRepository` to (de)serialize the new fields.
4. Add the new strings to `feature/settings/presentation/.../strings.xml`.
5. Extend `NotificationsViewModel`: add `onToggleQuietHoursEnabled`, `onRequestPickerSnackbar` (push transient via `errorMessage`).
6. Rewrite `NotificationsScreen.kt` end-to-end per §3 (header, banner, sections, rows, picker stub, previews).
7. Build (`:feature:settings:presentation:assembleDebug`, then `:app:installDebug`).
8. Launch app on `emulator-5556`, sign in, navigate Profile → Settings → Notifications, screenshot to `/tmp/notifications-after.png`.
9. Single local commit: `feat(notifications): redesign settings notifications with banner and quiet hours`.

# Plan — Privacy screen redesign: grouped cards + tinted icons + danger zone

## 1. Context

`PrivacyRoute` in `feature/settings/presentation/PrivacyScreen.kt` is the last Material-3-`ListItem` flat list left in the Settings feature. The Settings home (`SettingsHomeScreen.kt`, commit `b861e60`) has already moved to a grouped-card / tinted-icon language. The privacy screen needs the same visual vocabulary so the parent → child transition reads as one design language.

The mock adds three new affordances on top of the existing two rows:

- **DATA** section: *Download my data* (mint envelope), *Privacy policy* (lavender shield).
- **DANGER ZONE** section: *Delete account* (coral no-symbol icon, red title, outlined coral card border, "30-day grace period before hard delete" subtitle).

Neither *Download my data* nor *Privacy policy* has any wiring yet; both are deferred to a Coming-soon snackbar (same pattern as `match_coming_soon` in the Inbox "See all"). *Delete account* must reach the existing `SettingsAccount` route which already owns the typed-`DELETE` confirmation modal + 30-day grace-period banner — no logic moves.

**Goal:** match the mock; keep all existing behavior intact (Pause toggle, Blocked owners navigation, Delete-account confirmation flow); add two snackbar-deferred entries; add a Delete-account row that pushes onto `SettingsAccount`. **Non-goals:** redesigning `AccountScreen` itself (separate plan, future), wiring a real Privacy Policy page, implementing the data-download email, dark-mode tuning, or moving the deletion logic out of `AccountViewModel`.

## 2. Confirmed decisions

1. **"Delete account" routes to `SettingsAccount`.** That screen already owns the typed-`DELETE` modal, the `RequestAccountDeletionUseCase` call, and the 30-day grace-period banner. Inlining a parallel modal into `PrivacyScreen` would fork the deletion UI; routing to the existing screen reuses it. The mock's chevron on the row supports "this opens a detail screen."
2. **"Download my data" and "Privacy policy" both show a Coming-soon snackbar.** Same pattern Inbox "See all" and Filters chips use. They're new entries with no backend / static page / external URL wired yet. Flagged as deferred — see §9.
3. **Mock label is "Blocked owners" (plural).** Settings home already uses "Blocked owners"; Privacy currently says "Blocked users". Standardize on "Blocked owners" for consistency and because the data model is `BlockedOwner`. Re-use the existing `settings_row_blocked_*` strings and `settings_row_blocked_subtitle` plural so the count stays consistent across screens.
4. **Inline live blocked count.** `PrivacyViewModel` adds `ObserveBlockedOwnersUseCase` so the row subtitle shows "$N blocked" (matches Settings home behavior and the mock's "0 blocked"). Same plural resource.
5. **Lavender shield uses the existing `BrandColors.LavenderTint` + `LavenderInk` tokens.** Settings home already added these for the Notifications row. Hex values match the mock spec (~`#E5DDF6` background, ~`#6E5DB8` ink). No new tokens needed.
6. **Danger-zone card has an outlined coral 1dp border.** Visual signal that this card is destructive. All other cards continue to use a flat 1dp shadowElevation white surface. Implementation: `Surface(border = BorderStroke(1.dp, BrandColors.CoralDeep.copy(alpha = 0.5f)))` on the danger card (no shadow), all others use the existing flat shadow.
7. **Title color of "Delete account" = `MaterialTheme.colorScheme.error`** (the existing M3 red `#BA1A1A`). Matches the mock and the existing `AccountScreen.kt` delete-button color contract.
8. **Pause profile is still a Switch only** (no nav on row click) — the mock shows no chevron on that row. Tapping anywhere on the row toggles the switch via `Modifier.toggleable(Role.Switch)`.
9. **Blocked owners is still a chevron + navigation only** — tap navigates to `SettingsBlockedUsers`; no inline action.

## 3. Visual spec

### 3.1 Screen scaffold

- `Scaffold(containerColor = BrandColors.Cream)` (no `topBar`).
- Outer `Column(Modifier.fillMaxSize().padding(scaffoldPadding).windowInsetsPadding(WindowInsets.statusBars).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp))`, `Arrangement.spacedBy(20.dp)`.
- Same `PrivacyHeader` row as Settings home: 44dp white rounded-square back tile + bold "Privacy" headline.

### 3.2 Top card (no eyebrow — mirrors Settings home's first card pattern)

- `SettingsCard` (local copy of the Settings-home composable; same `RoundedCornerShape(20.dp)`, `surface` color, `shadowElevation = 1.dp`).
- Row 1 — **Pause profile**:
  - Icon: `Icons.Outlined.PauseCircleOutline`, background `BrandColors.NeutralTint`, tint `BrandColors.NeutralInk`.
  - Title: "Pause profile" (`titleMedium.SemiBold`, `onSurface`).
  - Subtitle: "Hide all your pets from other owners' decks" (`bodySmall`, `onSurfaceVariant`). Two-line clamp.
  - Trailing: `Switch` (coral track when checked, same `SwitchDefaults.colors(...)` as Settings home).
  - The whole row uses `Modifier.toggleable(value = paused, role = Role.Switch, onValueChange = onTogglePause)` so the entire row is tappable.
- `RowDivider` (inset `start = 72.dp`).
- Row 2 — **Blocked owners**:
  - Icon: `Icons.Outlined.Block`, background `BrandColors.CoralTint`, tint `BrandColors.CoralDeep`.
  - Title: "Blocked owners".
  - Subtitle: `pluralStringResource(R.plurals.settings_row_blocked_subtitle, count, count)`.
  - Trailing: chevron.
  - `onClick = onOpenBlockedOwners`.

### 3.3 DATA section

- `SectionEyebrow("DATA")` (reused composable shape, `start = 6.dp`, `bottom = 10.dp` padding, `letterSpacing = 1.5.sp`).
- `SettingsCard` with two rows:
  - **Download my data** — icon `Icons.Outlined.MailOutline`, background `BrandColors.MintTint`, tint `BrandColors.MintLeaf`. Subtitle "Email a copy of your profile + matches". Trailing chevron. `onClick` → snackbar "Coming soon".
  - `RowDivider`.
  - **Privacy policy** — icon `Icons.Outlined.Shield`, background `BrandColors.LavenderTint`, tint `BrandColors.LavenderInk`. Subtitle "How TinPet uses your data". Trailing chevron. `onClick` → snackbar "Coming soon".

### 3.4 DANGER ZONE section

- `SectionEyebrow("DANGER ZONE")`.
- **Danger card variant** — same 20dp rounded shape and `surface` color, but `border = BorderStroke(1.dp, BrandColors.CoralDeep.copy(alpha = 0.5f))` and `shadowElevation = 0.dp` (no shadow under the outlined card; matches the mock's hairline coral outline).
- Single row inside:
  - Icon: `Icons.Outlined.Block` (same no-symbol shape as Blocked owners), background `BrandColors.CoralTint`, tint `BrandColors.CoralDeep`.
  - Title: "Delete account" — `titleMedium.SemiBold`, color `MaterialTheme.colorScheme.error`.
  - Subtitle: "30-day grace period before hard delete" — `bodySmall`, `onSurfaceVariant`.
  - Trailing: chevron tinted `onSurfaceVariant` (mock uses the same neutral chevron color as the other rows — the redness is in the title, not the icon).
  - `onClick = onOpenDeleteAccount`.

### 3.5 Snackbar

- Reuse the existing `SnackbarHostState` already present in `PrivacyRoute`. Trigger via the existing error-message channel: add a `transientMessage: String?` field to `PrivacyUiState` and reuse the same `LaunchedEffect`-shows-snackbar pattern.

## 4. Component changes

- **No `:core:designsystem` token additions.** Everything needed already exists (`Cream`, `MintTint`, `MintLeaf`, `LavenderTint`, `LavenderInk`, `CoralTint`, `CoralDeep`, `NeutralTint`, `NeutralInk`).
- **No `:core:ui` extractions.** `SettingsCard`, `SettingsRow`, `SectionEyebrow`, `ChevronTrailing`, `RowDivider`, `PrivacyHeader` are all private composables inside `PrivacyScreen.kt`. They mirror the Settings home shapes but stay local for now — extraction is a follow-up once a third screen needs them (precedent: §4 of `plans/settings-home-redesign.md`). The `SettingsCard` here also gets an optional `border` parameter to support the danger variant; we don't introduce a separate `DangerSettingsCard` — same component, parameterized.

## 5. State / behavior changes

| File | Change |
|---|---|
| `PrivacyViewModel.kt` | Inject `ObserveBlockedOwnersUseCase`. Extend `PrivacyUiState` with `blockedCount: Int = 0` and `transientMessage: String? = null`. Add `notifyComingSoon(message: String)` that sets `transientMessage`. Add `clearTransient()`. Use `combine(observeMyProfile, observeBlockedOwners)` so both stream updates fold into one state, each upstream `catch`-ed so a single Firestore hiccup doesn't blank the screen. |
| `PrivacyScreen.kt` | Full rewrite per §3. Adds the new private composables (`PrivacyHeader`, `SectionEyebrow`, `SettingsCard`, `SettingsRow`, `ChevronTrailing`, `RowDivider`). Adds new lambdas `onOpenDeleteAccount: () -> Unit`. Wires snackbar for Download / Policy. Adds two `@Preview` composables (default + paused/has-blocked). |
| `SettingsNavModule.kt` | `entry<SettingsPrivacy>` — add `onOpenDeleteAccount = { navigator.goTo(SettingsAccount) }`. |
| `feature/settings/presentation/src/main/res/values/strings.xml` | Add `privacy_title`, `privacy_back_cd`, `privacy_pause_title`, `privacy_pause_subtitle`, `privacy_section_data`, `privacy_section_danger_zone`, `privacy_download_title`, `privacy_download_subtitle`, `privacy_policy_title`, `privacy_policy_subtitle`, `privacy_delete_title`, `privacy_delete_subtitle`, `privacy_coming_soon`. Reuse existing `settings_row_blocked_title` and `settings_row_blocked_subtitle` plural for the Blocked owners row. |

No changes to `:feature:settings:domain` or `:feature:settings:data`. No changes to `:feature:profile:*`. No changes to `:app`.

## 6. Files to add / modify

### Add
- `plans/privacy-redesign-grouped-cards.md` (this file).

### Modify
- `feature/settings/presentation/src/main/kotlin/com/rodiz/arch2/feature/settings/presentation/PrivacyScreen.kt` — full rewrite per §3.
- `feature/settings/presentation/src/main/kotlin/com/rodiz/arch2/feature/settings/presentation/PrivacyViewModel.kt` — add blocked-count and snackbar state.
- `feature/settings/presentation/src/main/kotlin/com/rodiz/arch2/feature/settings/presentation/SettingsNavModule.kt` — wire `onOpenDeleteAccount`.
- `feature/settings/presentation/src/main/res/values/strings.xml` — add privacy strings.

### Do NOT modify
- `feature/settings/presentation/.../AccountScreen.kt` / `AccountViewModel.kt` — destination for Delete account; redesign is its own future plan.
- `feature/settings/presentation/.../SettingsHomeScreen.kt` — already shipped in `b861e60`.
- `feature/settings/presentation/.../BlockedUsersScreen.kt` — separate redesign in the future.
- `core/designsystem/.../theme/Color.kt` — all tokens already present.
- `app/src/main/.../MainActivity.kt` — nav already correct.

## 7. Critical recipes

1. **`Modifier.toggleable` with `Role.Switch`** for the Pause row so screen readers announce it correctly and so the whole row is tappable without conflicting with the `Switch`'s own gesture handling. The `Switch` inside the row gets no `onCheckedChange` itself — the row owns the gesture.
2. **`combine` two flows in the VM** — `combine(observeMyProfile(), observeBlockedOwners()) { profile, blocked -> ... }`. Each upstream `.catch { e -> emit(default); _uiState.update { it.copy(errorMessage = e.message) } }` so a single Firestore failure doesn't blank the whole screen.
3. **`Surface` with `border` and `shadowElevation = 0.dp`** for the danger card. Mixing `border` + `shadowElevation > 0` draws both the shadow and the stroke and looks busy — the mock is shadow-less outlined.
4. **`pluralStringResource(R.plurals.settings_row_blocked_subtitle, count, count)`** — `count` argument is required twice (once for selection, once for substitution).
5. **Snackbar via a transient-message channel, not a SharedFlow** — keeps the VM lifecycle simple and mirrors how `errorMessage` already works in this VM. `LaunchedEffect(state.transientMessage) { state.transientMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearTransient() } }`.
6. **`isSystemInDarkTheme()` not needed** — `BrandColors.Cream` is intentionally a light-only background; dark-mode polish is a global TODO (already deferred in the Settings home plan).
7. **Status-bar inset**: `Modifier.windowInsetsPadding(WindowInsets.statusBars)` on the outer scroll column — Scaffold inherits `WindowInsets(0)` from MainActivity so we own the inset.

## 8. Verification

1. Build: `JAVA_HOME=…/jbr-17.0.14 ./gradlew :app:installDebug`.
2. Compose previews — two:
   - `PrivacyScreenPreviewDefault` — `paused = false`, `blockedCount = 0`.
   - `PrivacyScreenPreviewPopulated` — `paused = true`, `blockedCount = 3`.
3. Emulator (`emulator-5556`, 1080×2400):
   - Sign in → tap Profile bottom-nav → tap Settings → tap **Privacy**.
   - Verify cream background, bold "Privacy" headline, back tile.
   - Top card: Pause profile with neutral pause icon + Switch (off by default); Blocked owners with coral no-symbol icon + "0 blocked" + chevron.
   - DATA card: Download my data (mint envelope) + Privacy policy (lavender shield).
   - DANGER ZONE card: outlined coral border, coral no-symbol icon, red "Delete account" title, "30-day grace period before hard delete" subtitle.
   - Tap Pause Switch → toggles; navigating back to Settings home shows the same state.
   - Tap Blocked owners → lands on Blocked Users screen.
   - Tap Download my data → "Coming soon" snackbar.
   - Tap Privacy policy → "Coming soon" snackbar.
   - Tap Delete account → lands on the existing AccountScreen with its DELETE modal flow.
4. Screenshot to `/tmp/privacy-after.png` via `adb -s emulator-5556 exec-out screencap -p`.

## 9. Out of scope / deferred

- **Privacy policy destination** — no static page or external URL is wired. Currently lands a "Coming soon" snackbar. Real wiring requires a hosted policy page (e.g., a CustomTabs intent to `https://tinpet.app/privacy`) or a static Compose screen and a new `SettingsPrivacyPolicy` route.
- **Download my data wiring** — no backend / Cloud Function exists to assemble the export email. Currently a "Coming soon" snackbar.
- **AccountScreen redesign** — Delete account routes to `SettingsAccount` which still uses the old Material-3 `TopAppBar` + `ListItem` look. Its redesign is its own plan.
- **BlockedUsersScreen redesign** — still uses the old M3 list. Its redesign is its own plan.
- **Dark-mode polish** for the cream + outlined coral combination.
- **Localizing "Coming soon"** — single English string for now.

## 10. Risk / rollback

- **Risk:** the danger card border + no-shadow combo reads thinner than the other cards in some screen densities. Mitigation: verify visually; bump border alpha from `0.5f` to `0.65f` if it reads too faint.
- **Risk:** `Modifier.toggleable` on the row may swallow the inner Switch's gesture, preventing fine-grained drag. Mitigation: if the Switch feels unresponsive on the emulator, drop the row-level `toggleable` and keep `onCheckedChange` on the Switch — the row body becomes non-clickable, but the Switch still works.
- **Risk:** `combine` two flows with first-emission semantics — `observeBlockedOwners()` might never emit until Firestore connects, blocking `isLoading = false`. Mitigation: initialize each upstream with `onStart { emit(default) }` so the combined flow emits immediately with defaults and `isLoading` flips false on first frame.
- **Rollback:** revert the single `feat(privacy): redesign with grouped cards and danger zone` commit. No schema or data changes.

## 11. Implementation order

1. Update `PrivacyViewModel.kt` — combine flows, add `blockedCount` + `transientMessage` + `notifyComingSoon` + `clearTransient`.
2. Update `SettingsNavModule.kt` — wire `onOpenDeleteAccount` to `SettingsAccount`.
3. Extend `feature/settings/presentation/.../res/values/strings.xml` with the new privacy strings.
4. Rewrite `PrivacyScreen.kt` per §3 — header, cards, rows, snackbar, two `@Preview`s.
5. Build: `:app:installDebug`.
6. Launch app on `emulator-5556`, sign in, navigate to Settings → Privacy, screenshot to `/tmp/privacy-after.png`.
7. Single local commit: `feat(privacy): redesign with grouped cards and danger zone`.

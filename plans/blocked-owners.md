# Plan — Blocked owners screen: cream canvas + floating back tile + centered empty state

> **Parent context:** [`plans/settings-home-redesign.md`](./settings-home-redesign.md) (the row that opens this screen, plurals subtitle, and `BrandColors` tokens already in place).

## 1. Context

`feature/settings/presentation/BlockedUsersScreen.kt` exists today as a stock Material 3
`Scaffold` + `TopAppBar("Blocked users")` + `EmptyTabState(Icons.Outlined.Block, "No blocked users", …)`.
It still uses the generic shared `EmptyTabState` composable (96dp tinted icon, no chip backing),
which doesn't match the recently shipped Settings home / Inbox / Profile / Likes redesigns.

The mock wants this screen reworked to match the established cream + floating back tile +
bold inline title pattern (`SettingsHomeScreen.kt` header), with a centered empty state that
sits slightly above true center and uses:

- a large pale-coral **circular chip** (`BrandColors.CoralTint`) holding a coral
  block / no-entry glyph,
- a bold black headline **"No one's blocked"**,
- a muted, centered body copy at ~280dp max-width.

This screen is reached from Settings → Safety → "Blocked owners" (already wired in
`SettingsNavModule.entry<SettingsBlockedUsers>`), and also from Privacy →
"Blocked users" row.

**Goal:** match the mock for the empty state and keep the existing populated list
working with the cream + floating header chrome (rather than the old `TopAppBar`
shell). **Non-goals:** redesigning the populated row layout itself (still the
plain `ListItem` row with an avatar + `Unblock` `TextButton` — the mock only
shows empty), wiring snackbars or confirmation dialogs differently, and changing
any `:feature:settings:domain`/`:data` code.

## 2. Confirmed decisions

1. **Copy uses "owners" everywhere** to match the Settings home row title
   ("Blocked owners"), the screen header ("Blocked owners"), and the body
   text ("When you block an owner from chat…"). The current screen says
   "users" inconsistently — switch to "owners" in the title and "owner"
   in the body. The Material `TopAppBar` is dropped in favor of the inline
   `BackTile + bold title` pattern from `SettingsHomeScreen.kt`.
2. **Empty state stays inline, not via shared `EmptyTabState`.** The shared
   component renders a flat tinted 96dp icon with no surrounding chip and
   uses `headlineSmall`; the mock wants a 120dp pale-coral circular chip
   containing a smaller 48dp coral icon, with a bold extra-weight headline.
   Building this inline (a small private `BlockedEmptyState` composable)
   avoids polluting `:core:designsystem` with a one-off variant.
3. **Icon for the block chip = `Icons.Outlined.Block`.** Material already
   ships this as the circle-with-diagonal-slash glyph — exactly the mock's
   "no entry" symbol. No custom vector asset needed.
4. **Vertical centering bias.** Mock places the chip roughly 1/3 from the
   top of the body region (not true center). Use `Arrangement.spacedBy` +
   a top `Spacer(weight = 1f)` and a bottom `Spacer(weight = 1.6f)` so the
   block sits slightly above center.
5. **Status bar.** Screen background is `BrandColors.Cream` — keep default
   dark status-bar icons (matches Settings home, Profile, Inbox). No
   `LightStatusBarIconsWhileShown`.
6. **Populated list reuse.** When `blocked` is non-empty, render the existing
   `BlockedRow(owner, isUnblocking, onUnblock)` inside a `LazyColumn` under
   the new header. We keep the row's current `ListItem` styling for now
   (deferred polish — the mock doesn't show this state) but switch the
   container to the cream background so it visually matches the rest of the
   redesign.
7. **No new strings file**, no new tokens. All copy lives in
   `feature/settings/presentation/.../res/values/strings.xml`. Reuse
   `BrandColors.Cream`, `BrandColors.CoralTint`, `BrandColors.CoralDeep`,
   and the existing `settings_back_cd` content description. New keys:
   `blocked_title`, `blocked_empty_headline`, `blocked_empty_body`,
   `blocked_chip_cd`.

## 3. Visual spec

### 3.1 Scaffold

- `Scaffold(containerColor = BrandColors.Cream, snackbarHost = …)`.
- Outer `Column(Modifier.fillMaxSize().padding(scaffoldPadding))`.
- Inside, a header row + the body. The body fills the remaining vertical
  space so the empty state can center.

### 3.2 Header

Identical recipe to `SettingsHomeScreen.SettingsHeader` so the two screens
feel like a single surface:

```
Row(
  Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)
    .windowInsetsPadding(WindowInsets.statusBars),
  horizontalArrangement = Arrangement.spacedBy(14.dp),
  verticalAlignment = Alignment.CenterVertically,
) {
  BackTile(onBack)
  Text("Blocked owners", headlineMedium.copy(ExtraBold), onSurface)
}
```

`BackTile`:

```
Surface(
  shape = RoundedCornerShape(14.dp),
  color = MaterialTheme.colorScheme.surface,
  shadowElevation = 1.dp,
  modifier = Modifier.size(44.dp).clickable(onBack).testTag("blocked_back"),
) { Box(contentAlignment = Center) {
  Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = settings_back_cd,
       tint = onSurface, modifier = Modifier.size(26.dp))
}}
```

### 3.3 Empty state body

`Column(Modifier.fillMaxSize().padding(horizontal = 32.dp), horizontalAlignment = CenterHorizontally)`
with three children separated by weighted spacers (1f, content, 1.6f):

- **Chip**:
  ```
  Box(
    Modifier.size(120.dp).clip(CircleShape).background(BrandColors.CoralTint),
    contentAlignment = Center,
  ) {
    Icon(
      imageVector = Icons.Outlined.Block,
      contentDescription = stringResource(R.string.blocked_chip_cd),
      tint = BrandColors.CoralDeep,
      modifier = Modifier.size(48.dp),
    )
  }
  ```
- `Spacer(Modifier.height(24.dp))`.
- **Headline**: `Text("No one's blocked", titleLarge.copy(fontWeight = ExtraBold), onSurface, textAlign = Center)`.
- `Spacer(Modifier.height(10.dp))`.
- **Body**: `Text(body, bodyMedium, onSurfaceVariant, textAlign = Center,
   modifier = Modifier.widthIn(max = 300.dp))`.

### 3.4 Populated list (kept simple)

When `state.blocked.isNotEmpty()`:

```
LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
  items(state.blocked, key = { it.id }) { owner ->
    BlockedRow(owner, owner.id in state.pendingUnblock, onUnblock = { vm.unblock(owner.id) })
    HorizontalDivider(color = onSurfaceVariant.copy(alpha = 0.08f),
                       modifier = Modifier.padding(start = 72.dp))
  }
}
```

Existing `BlockedRow` is reused unchanged; the cream `Scaffold` background lets
the white `ListItem` surface read as a faint card row.

### 3.5 Loading state

`Box(Modifier.fillMaxSize(), contentAlignment = Center) { CircularProgressIndicator(color = BrandColors.CoralDeep) }`.

## 4. Component changes

- **No new `:core:designsystem` or `:core:ui` components.** Everything new is a
  private composable inside `BlockedUsersScreen.kt` (`BlockedHeader`,
  `BlockedBackTile`, `BlockedEmptyState`).
- **No new `BrandColors` tokens** — reuses `Cream`, `CoralTint`, `CoralDeep`,
  already added in the Settings home redesign.

## 5. State / behavior changes

- `BlockedUsersViewModel` unchanged.
- `BlockedUsersRoute` signature unchanged (`onBack: () -> Unit`).
- Behavior unchanged: same `unblock()`, same snackbar on error, same loading
  spinner.

## 6. Files to add / modify

### Modify
- `feature/settings/presentation/src/main/kotlin/com/rodiz/arch2/feature/settings/presentation/BlockedUsersScreen.kt`
  — full rewrite per §3.
- `feature/settings/presentation/src/main/res/values/strings.xml` — add
  `blocked_title`, `blocked_empty_headline`, `blocked_empty_body`,
  `blocked_chip_cd`.

### Add
- `plans/blocked-owners.md` (this file).

### Do NOT modify
- `feature/settings/domain/*`, `feature/settings/data/*` — repos and use cases unchanged.
- `feature/settings/presentation/SettingsNavModule.kt` — route entry already
  wires `BlockedUsersRoute(onBack = navigator.goBack)`.
- `feature/settings/presentation/SettingsHomeScreen.kt` — the Safety row that
  opens this screen is correct.
- `feature/settings/presentation/PrivacyScreen.kt` — opens this screen via
  the same nav.
- `core/designsystem/*` — no new tokens, no new components.

## 7. Critical recipes

1. **Inline header instead of `TopAppBar`**: keeps the back chevron rendered as
   a small floating tile against cream (matches Settings home). Mock has no
   right-side action so the row is just `BackTile + Text(title)`.
2. **`windowInsetsPadding(WindowInsets.statusBars)` on the header row, not on
   the outer column**: the body needs the full remaining height for vertical
   centering. Putting the inset on the header lets the body fill the area
   below the status bar cleanly.
3. **Weighted spacers** (`Spacer(Modifier.weight(1f))`, content,
   `Spacer(Modifier.weight(1.6f))`) bias the chip slightly above true center
   — matches the mock more faithfully than `Arrangement.Center`.
4. **`Modifier.widthIn(max = 300.dp)`** on the body text keeps the muted copy
   wrapping at ~3 lines on a 1080×2400 emulator without explicit line breaks.
5. **`Icons.Outlined.Block`** *is* the no-entry glyph (a circle with a
   diagonal slash). No need to load a custom vector.
6. **CircularProgressIndicator tinted coral**: `color = BrandColors.CoralDeep`
   matches the brand instead of the default purple. (Same trick as `LoadingBody`
   in `LikesScreen.kt`.)
7. **No `Co-Authored-By: Claude` trailer**, no GitHub push, no AI references in
   the commit. (Standard guard.)

## 8. Verification checklist

1. `JAVA_HOME=…/jbr-17.0.14 ./gradlew :feature:settings:presentation:assembleDebug`
   completes without errors.
2. `JAVA_HOME=…/jbr-17.0.14 ./gradlew :app:installDebug` completes.
3. Launch on `emulator-5556`. Sign in (if needed). Navigate Profile (bottom
   nav) → Settings → Safety → "Blocked owners".
4. Verify the empty state matches the mock: cream background, white floating
   back chevron tile in top-left, bold "Blocked owners" title, large pale-coral
   chip with the coral no-entry icon roughly slightly above center, "No one's
   blocked" headline, muted body copy below.
5. Tap the back chevron → returns to Settings home.
6. Screencap to `/tmp/blocked-owners-after.png`.

## 9. Out of scope

- Redesigning the populated row (avatar + `Unblock` `TextButton`) — mock
  doesn't show it; deferred until we have a real populated state to iterate
  against.
- Confirmation dialog on `Unblock` — currently a single tap with optimistic
  UI; defer.
- Populated-list section header ("Blocked since …" grouping by week) — defer.
- Adding a "Block someone" affordance from this screen — blocking originates
  from chat overflow / pet detail, not from Settings.

## 10. Risk / rollback

- Pure presentation change in a single file + strings.xml additions. Revert is
  `git revert <hash>`.
- Worst case: rendered headline or chip is mis-sized on small phones. Mitigation:
  the chip is a fixed 120dp; on `emulator-5556` (1080×2400, ~3x density)
  this consumes ~33% of the body width — safe on the smallest minSdk-26
  device (≈360dp wide).

## 11. Implementation order

1. Add four new string keys to `feature/settings/presentation/.../res/values/strings.xml`.
2. Rewrite `BlockedUsersScreen.kt` end-to-end per §3 (header + empty state +
   populated list + loading).
3. Build with JBR-17 → `:feature:settings:presentation:assembleDebug` →
   `:app:installDebug`.
4. Force-stop, launch, navigate to Settings → Safety → Blocked owners,
   screencap.
5. Single local commit, no push.

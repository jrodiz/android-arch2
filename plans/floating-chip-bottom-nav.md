# Plan — Floating chip bottom navigation

> **NOTE on plan location:** project convention ([[feedback_plans_location]]) is to save plans under `plans/` in the Arch2.0 repo. Plan mode pins the working copy here. **Step 0 of implementation: copy this file to `plans/floating-chip-bottom-nav.md`** so it gets versioned with the code.

## 1. Context

Today the app uses a stock Material 3 `NavigationBar` defined in `app/src/main/kotlin/com/rodiz/arch2/MainActivity.kt:122–137`. It renders the four top-level destinations (`DeckHome`, `LikesHome`, `MatchesHome`, `ProfileHome`) full-width across the bottom of the screen, with default Material colors and no badges.

The mock replaces it with a **floating dark "capsule" containing chip-style actions**:

- A pill-shaped dark surface (warm near-black), floating ~16dp above the system navigation bar and inset ~16dp from the screen edges. Content scrolls behind it.
- Each destination is an icon slot inside the capsule. **Unselected** items are icon-only in a muted color. The **selected** item expands into a coral filled inner pill containing the icon **and** its label in white.
- Selected and unselected items can carry a **numeric count badge** in the top-right corner: a coral disc with white count for unselected, a white disc with coral count for the selected (inverted because the selected pill is already coral). Cap at `99+`.
- The destination set and routing semantics don't change — same four tabs, same Nav3 `Navigator.replaceAll(route)` on tap.

**Goal:** ship the redesign while wiring real count badges for the two destinations that already have domain-layer counts available (Likes, Matches). **Non-goals:** add new destinations, change routing semantics, ship unread badges for destinations that don't have a count source yet (Chat, Notifications, Deck, Profile).

## 2. Confirmed / inferred decisions

1. **Match badge source = `InboxSnapshot.newMatches.size`** (matches with zero messages yet). Unread-conversations badge is deferred — would need a new domain UseCase comparing per-message `readBy: Map<String, Instant>` per conversation, which is out of scope. *Inferred — flag for user.*
2. **Badge cap = `99+`** above 99. *Inferred.*
3. **Re-tap on already-selected destination = no-op.** Don't re-`replaceAll` — that would clear any nested back stack the user built inside that tab. *Inferred — matches current behavior since `selected == true` already short-circuits visibly, but make the no-op explicit in the new composable.*
4. **Hide-on-non-top-level rule is unchanged** (`showBottomBar = current in BOTTOM_TABS`). The floating capsule simply isn't rendered on detail screens (Chat, Settings, etc.).
5. **IME behavior:** the floating capsule does not adjust to the keyboard. Top-level tabs rarely surface a soft keyboard; if one opens the capsule simply overlaps the IME. No `imePadding()`. *Inferred — keeps the code small; revisit if it looks broken.*
6. **Capsule lives in `:app`,** not `:core:ui`. The current bottom nav already lives in `:app` and is tightly coupled to `Navigator` (also in `:core:navigation` but consumed from `:app`); promoting it to `:core:ui` would require introducing a generic item model just for this one consumer. Single use, single home.

## 3. Visual spec

### 3.1 Outer capsule
- Shape: `RoundedCornerShape(percent = 50)` (full pill).
- Background: new token `BrandColors.NavSurface = Color(0xFF2A1F1D)` (warm dark brown — sits in the same family as `LightScheme.onSurface = #211B1A` but slightly warmer to read as a deliberate surface, not just text color). Add to `core/designsystem/.../theme/Color.kt`.
- Elevation: `shadowElevation = 8.dp` on the wrapping `Surface`.
- Padding: `8.dp` all sides inside the capsule.
- Outer placement: `Modifier.padding(horizontal = 16.dp).windowInsetsPadding(WindowInsets.navigationBars).padding(bottom = 16.dp)`, aligned `BottomCenter` of the screen.

### 3.2 Item row
- `Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically)` inside the capsule.
- Each item is a `Box` with `clickable(role = Role.Tab, onClick = ...)`.

### 3.3 Unselected item
- Size: `48.dp` square (touch target).
- Content: icon only, `Icons.Outlined.{Pets, Favorite, Bolt, Person}`, tinted `Color.White.copy(alpha = 0.55f)`.
- Badge: when `count > 0`, draw a coral disc `BrandColors.CoralDeep` at top-right with white count `labelSmall`, `FontWeight.Bold`. Use a `BadgedBox`-like `Box` with an aligned overlay (do NOT use Material's `BadgedBox` — its anchor offsets fight the small capsule; hand-roll the overlay with `Modifier.offset(x = 6.dp, y = (-6).dp)`).

### 3.4 Selected item (the coral inner pill)
- Background: `BrandColors.Coral` (the matte coral, not `CoralDeep` — matches the mock's brightness).
- Shape: `RoundedCornerShape(percent = 50)`.
- Padding: `horizontal = 16.dp, vertical = 10.dp`.
- Content: `Row(verticalAlignment = CenterVertically, horizontalArrangement = spacedBy(8.dp))` → icon (filled variant: `Icons.Filled.{Pets, Favorite, Bolt, Person}`) tinted `Color.White` + `Text(label, style = titleSmall, fontWeight = FontWeight.ExtraBold, color = Color.White)`.
- Badge: when `count > 0`, white disc with `BrandColors.CoralDeep` count text, same `(6.dp, -6.dp)` offset.

### 3.5 Animations
- The selected→unselected transition is an `AnimatedContent` keyed on `selectedRoute`. Use `slideIntoContainer + fadeIn` / `slideOutOfContainer + fadeOut` so the label slides out as the next pill grows in. Keep durations short (`200ms`).
- Optional and additive — if animations feel janky drop them in favor of recomposition only. Don't gate the ship on this.

## 4. Files to add / modify

### Add
- `app/src/main/kotlin/com/rodiz/arch2/ui/FloatingChipNavBar.kt` — new composable + private item subcomposables + `data class NavBarItem(val route: Any, val label: String, val icon: ImageVector, val selectedIcon: ImageVector, val badgeCount: Int)`. ~150 LOC.
- `app/src/main/kotlin/com/rodiz/arch2/ui/BottomNavViewModel.kt` — Hilt VM that injects `ObserveLikesYouUseCase` + `ObserveInboxUseCase` + `SessionRepository` (for `currentUid`), combines them with `combine(...)` into a `StateFlow<BottomNavBadges>` data class with `likes: Int, matches: Int` fields. Default `0` while loading. Use `viewModelScope` + `stateIn(SharingStarted.WhileSubscribed(5_000), BottomNavBadges())`.

### Modify
- `core/designsystem/src/main/kotlin/com/rodiz/arch2/core/designsystem/theme/Color.kt` — add `BrandColors.NavSurface = Color(0xFF2A1F1D)`. Existing tokens unchanged.
- `app/src/main/kotlin/com/rodiz/arch2/MainActivity.kt`:
  - Delete `DashboardBottomBar` (lines 122–137) and the `BottomTab` / `BOTTOM_TABS` declarations (lines 109–120).
  - Replace `Scaffold(bottomBar = { if (showBottomBar) DashboardBottomBar(...) })` with a `Box(Modifier.fillMaxSize())` containing the `Scaffold` content (no `bottomBar` slot) and a sibling `FloatingChipNavBar(...)` aligned to `BottomCenter` with `AnimatedVisibility(showBottomBar)`. The Scaffold's content padding loses its bottom inset, so screens that need to clear the floating capsule add `Modifier.padding(bottom = 80.dp)` to their last item (or rely on `windowInsetsPadding(WindowInsets.navigationBars)` and accept that the capsule overlaps the bottom of long scroll content — see verification §7).
  - Inject `BottomNavViewModel` via `hiltViewModel()` at the call site (`MainActivity` is a `ComponentActivity`; use `composeViewModels` inside the `setContent` block).
  - Wire `navigator.replaceAll(tabRoute)` only when the tapped route differs from the current selection.

### Do NOT modify
- `feature/likes/domain/.../LikesYouUseCases.kt` — already exposes what we need; don't add a count-only wrapper.
- `feature/match/domain/.../MatchUseCases.kt` — same; we'll call it and `.map { it.newMatches.size }` inline in the VM.
- Any `:feature:*:presentation` ViewModel — the bottom nav reads from `:domain` directly.
- `core/navigation/.../Navigator.kt` — no API change.

## 5. State / data wiring

```kotlin
@HiltViewModel
class BottomNavViewModel @Inject constructor(
    observeLikesYou: ObserveLikesYouUseCase,
    observeInbox: ObserveInboxUseCase,
    sessionRepository: SessionRepository,
) : ViewModel() {
    val badges: StateFlow<BottomNavBadges> = sessionRepository
        .observeCurrentUid()                       // existing API; verify exact name
        .filterNotNull()
        .flatMapLatest { uid ->
            combine(
                observeLikesYou(),                  // Flow<List<IncomingLike>>
                observeInbox(uid),                  // Flow<InboxSnapshot>
            ) { likes, inbox ->
                BottomNavBadges(
                    likes = likes.size,
                    matches = inbox.newMatches.size,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BottomNavBadges())
}

data class BottomNavBadges(val likes: Int = 0, val matches: Int = 0)
```

Verify `SessionRepository.observeCurrentUid()` name at wire time — fall back to whatever the existing `MainActivity` uses to gate logged-in vs anonymous (likely `SessionRepository` or `AuthState`). If no observable uid exists, take `currentUid` once at VM construction and don't re-subscribe on auth changes — the Activity is recreated on sign-out anyway.

## 6. Critical recipes

1. **Floating layout, not bottom-bar slot.** Use a top-level `Box(Modifier.fillMaxSize())` with the `Scaffold` as one child (full size) and the `FloatingChipNavBar` as a sibling aligned `Alignment.BottomCenter`. Don't put the nav in `Scaffold.bottomBar` — that reserves layout space and content won't scroll behind it.
2. **Window insets:** the capsule must apply `Modifier.windowInsetsPadding(WindowInsets.navigationBars)` so it sits above the gesture handle. Test on a 3-button-nav device and a gesture-nav device.
3. **Don't use `BadgedBox`.** Material's `BadgedBox` has fixed anchor offsets that look wrong on a small `48.dp` slot. Hand-roll: `Box { Icon(...); if (count > 0) CountBadge(count, Modifier.align(TopEnd).offset(6.dp, (-6).dp)) }`.
4. **Tap suppression for selected tab.** `onClick = { if (selectedRoute != item.route) navigator.replaceAll(item.route) }`. Avoid re-`replaceAll` on the active tab — it would wipe any nested back stack (e.g. user is on `Chat(matchId)` reached from Matches and tapping Matches should pop back, not no-op; verify the existing semantics first and decide).
5. **`AnimatedContent` key = selectedRoute.** Wraps the entire item row so the pill expand/contract is one animation, not four independent ones.
6. **Filled vs outlined icons:** the selected pill uses filled icon variants (`Icons.Filled.Favorite`), the unselected uses outlined (`Icons.Outlined.Favorite`). The mock confirms this — the selected heart is solid.

## 7. Verification

1. **Build:**
   ```bash
   JAVA_HOME=/Users/jrodiz/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home ./gradlew :app:installDebug
   ```
2. **Emulator (`emulator-5556`):**
   - Sign in. Land on Deck (default).
   - Floating dark capsule visible above the home indicator with paw selected (coral pill, "Deck" label).
   - Tap Likes: capsule animates to coral pill on heart, label "Likes". If any likes exist, badge "N" (or "99+") shows top-right of the selected pill in white-on-coral. The bolt destination shows a coral-on-dark badge if newMatches > 0.
   - Navigate into a chat / settings / pet detail: capsule hides via `AnimatedVisibility`.
   - Pop back to a top-level tab: capsule re-appears with the correct selected tab.
   - Rotate device: selection persists (state survives because it derives from `Navigator.backStack`, which is `@ActivityRetainedScoped`).
   - Verify content scrolls behind the capsule (Deck swipes, Likes list at the bottom is partially visible behind the nav until you scroll past).
   - Cold-launch the app: badges start at `0` and update within ~1s as the first emissions land. Acceptable.
3. **Screenshot** `/tmp/floating-nav-after.png` via `adb -s emulator-5556 exec-out screencap -p`.

## 8. Out of scope

- Chat unread badge — no domain count source today.
- Notifications badge — FCM-only, no local tally.
- Per-tab back-stack preservation (already absent from current code; introducing it is a separate plan).
- Theming the capsule for dark mode — same warm-dark color reads on both schemes since it's a fixed brand surface like the coral hero.
- Replacing `Material3 NavigationBar` references anywhere else (this is the only one).

## 9. Risk / rollback

- **Risk:** `Scaffold` content padding changes when removing `bottomBar`. Screens that hard-coded `Modifier.padding(it)` won't get bottom inset → bottom rows clip behind the capsule. Mitigation: each top-level screen (Deck, Likes, Matches, Profile) already uses `LazyColumn`/`LazyVerticalGrid` with `contentPadding(bottom = ...)` or `windowInsetsPadding(WindowInsets.navigationBars)` — verify per-screen during the emulator pass. If any clip, add `contentPadding = PaddingValues(bottom = 96.dp)` to the offender.
- **Risk:** `ObserveLikesYouUseCase` is fired by the bottom-nav VM even when the user isn't viewing the Likes tab — extra Firestore listener. Mitigation: it's the same listener the Likes screen would attach when visible, just running for longer. Use `SharingStarted.WhileSubscribed(5_000)` so it tears down 5s after the last subscriber (Activity destroy).
- **Rollback:** revert the single `feat(nav): floating chip bottom navigation` commit. The only cross-module change is `BrandColors.NavSurface` (additive) and a new VM that injects existing UseCases.

## 10. Implementation order

0. Copy this plan to `plans/floating-chip-bottom-nav.md` in the repo.
1. Add `BrandColors.NavSurface` to `Color.kt`.
2. Write `BottomNavViewModel` + `BottomNavBadges`. Build to confirm Hilt graph resolves (`./gradlew :app:assembleDebug`).
3. Write `FloatingChipNavBar.kt` as a self-contained composable. Drive previews from hand-built `NavBarItem` lists for [empty badges], [Likes=6, Matches=1], [Likes=120 → renders "99+"].
4. Modify `MainActivity.kt`: drop `DashboardBottomBar` + `BottomTab` + `BOTTOM_TABS`, restructure the Scaffold to a `Box` overlay layout, wire `FloatingChipNavBar` with `AnimatedVisibility(showBottomBar)` + collected `bottomNavViewModel.badges`.
5. Build + install on emulator-5556. Walk every top-level destination + at least one nested route to confirm hide/show.
6. Iterate on visual spacing / shadow / badge offset until it matches the mock.
7. Single commit: `feat(nav): floating chip bottom navigation with live likes/matches badges`. Local only — no push.

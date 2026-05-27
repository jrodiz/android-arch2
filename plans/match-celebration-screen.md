# Plan — "It's a match!" celebration screen

> **NOTE on plan location:** project convention ([[feedback_plans_location]]) is to save plans under `plans/` in the Arch2.0 repo. Plan mode pins the working copy here. **Step 0 of implementation: copy this file to `plans/match-celebration-screen.md`** so it gets versioned with the code.

## 1. Context

Today, when a user's like results in a mutual match, the Deck quietly surfaces a snackbar with the text `"It's a match!"` and stays in place (`DeckViewModel.swipe(...)` → `it.copy(matchMessage = "It's a match!")` at `feature/deck/presentation/.../DeckViewModel.kt:145-156` → consumed by `DeckScreen.kt:76-81`). The user has to find the match later via the inbox to react.

The user wants a full-screen celebration on match — same beat every dating-style app does at the moment of mutual interest. Mock (provided): coral background, two pet photo tiles with the owners' small avatars + names beneath, headline "**It's a match!**", subtitle "**`{myPet.name}` and `{theirPet.name}` want to be friends 🐾**", a primary white "Say hello" CTA and a "Keep swiping" secondary CTA.

**Confirmed scope (this session):**

1. The celebration is **one-shot at the moment of the like**. Not revisitable from the inbox. The inbox still routes straight to chat (existing `MatchNavModule.kt:23-24` behavior unchanged).
2. **"Say hello"** pops the celebration off the back stack first, *then* pushes the chat route, so back from Chat lands on Deck (not on the celebration). The celebration never sits in the user's nav history.
3. **"Keep swiping"** + system back gesture both = pop celebration → back on Deck where they were swiping.
4. The Deck snackbar `matchMessage` is **deleted** — fully superseded by the new screen. Same trigger (`SwipeResult.Match`), different surface.

**Non-goals:** Animations / confetti / haptics (static layout in v1); reachability from the matches inbox; "look back at past celebrations"; share-to-social or screenshot CTAs.

## 2. Final flow

```
DeckScreen → user taps Like → DeckViewModel.likeTop()
  → submitSwipe(petId, LIKE) returns SwipeResult.Match(matchId)
  → VM stamps state.pendingMatchId = matchId (NEW field)
  → DeckScreen sees pendingMatchId go non-null → fires LaunchedEffect
    → navigator.goTo(MatchCelebration(matchId))      ← push
    → viewModel.clearMatch()                          ← clear sentinel

MatchCelebration screen
  ┌─ "Say hello"   → navigator.goBack()              ← pop celebration
  │                  navigator.goTo(ChatRoute(matchId))
  ├─ "Keep swiping" → navigator.goBack()             ← pop celebration → Deck
  └─ system back    → navigator.goBack()             ← pop celebration → Deck
```

The two-step "pop then push" pattern in "Say hello" is the established way to keep the celebration out of the back stack — `Navigator.kt` exposes `goTo`, `goBack`, and `replaceAll`, no `popThenGoTo` primitive needed. Doing it from the screen-level callback is fine because both calls are synchronous against the in-memory `SnapshotStateList<Any>`.

## 3. Module layout

The screen lives in **`:feature:match:{nav, presentation}`** — the natural home for match-related UI, alongside the inbox.

- `:feature:match:nav` gains `@Serializable data class MatchCelebration(val matchId: String)`.
- `:feature:match:presentation` gains `MatchCelebrationScreen.kt` + `MatchCelebrationRoute.kt` + `MatchCelebrationViewModel.kt`.

Cross-feature wiring needed:

- `:feature:deck:presentation` already depends on **another feature's `:nav`** (it imports `feature:settings:nav` for filters, `feature:pet:nav` for AddPet, etc.) — adding `:feature:match:nav` is the same pattern.
- `:feature:match:presentation` already imports `:feature:chat:nav` for `ChatRoute` (from the inbox → chat flow), so reusing that nav is free.
- The new VM needs `MatchRepository.observeMatch(id)` (from `:feature:match:domain`, already on classpath), `PetLookupRepository.observe(petId)` (from `:core:petlookup:domain`), `OwnerLookupRepository.observe(ownerId)` (from `:core:ownerlookup:domain`), `SessionRepository.current()` (from `:core:session:domain`) for "which uid is me". Match presentation already has all of these via the inbox VM.

## 4. Screen design (from the mock)

Layout, top to bottom on `BrandColors.Coral` background:

- Status-bar padding (light-on-coral icons via the same `LightStatusBarIconsWhileShown()` helper Login uses — extract once into `:core:ui` if not already there, or copy the 10-line DisposableEffect inline).
- 56dp top spacer.
- Headline "**It's a match!**" — `displaySmall ExtraBold`, white, centered, `semantics { heading() }`.
- 8dp spacer.
- Subtitle "**`{myPet.name}` and `{theirPet.name}` want to be friends 🐾**" — `bodyLarge`, white-with-90%-alpha, centered. Pet names from `PetLookup` lookups; if either is `null` (still loading), fall back to "Your pet" / "Their pet" so the layout doesn't reflow.
- 36dp spacer.
- **Pet tiles row** — two photo tiles side-by-side with slight rotations, mimicking the Login hero pattern but scaled larger:
  - Slot 0 (left): MY pet. `AsyncImage` of `myPet.avatarUrl`, ~150×190dp aspect, `RoundedCornerShape(20.dp)`, `border(4.dp, White)`, `Modifier.rotate(-6f)`, with a small owner-chip overlapping the bottom-right of the tile (see Owner chip below). Falls back to `Icons.Outlined.Pets` on a pale circle when avatar URL is null.
  - Centerpiece: a 44dp white circle with `Icons.Filled.Favorite` (coral) — sits between the two tiles, vertically centered, drawn on top.
  - Slot 1 (right): THEIR pet, same shape, `Modifier.rotate(6f)`. Owner-chip overlapping bottom-left.
- 8dp spacer.
- **Owner names row** — under each tile, the corresponding owner's `firstName` in `labelLarge SemiBold`, white. (The mock shows them inside the chip, but rendering them as plain text below works without overlapping the tiles.)
- Spacer with `weight(1f)` — pushes the CTAs to the bottom.
- White `PrimaryButton("Say hello")` (from `core/ui/components/PrimaryButton.kt`, recolored: `containerColor = Color.White`, `contentColor = BrandColors.CoralDeep`). Optional `Icons.Outlined.ChatBubbleOutline` 16dp leading icon.
- 12dp spacer.
- `OutlinedButton("Keep swiping")` — white-bordered, transparent fill, white text.
- 24dp spacer + navigation-bar padding.

**Decorative paw + heart sprinkles** (mock detail): three small `Icons.Outlined.Pets` 12-16dp at scattered positions in the background, white-with-20%-alpha. Match the Login hero's decorative restraint — no more than four total marks. Can be deferred to a polish pass if needed.

**Owner chip** (the small avatar + name underneath each tile in the mock): 28dp circular `AsyncImage` of the owner's `avatarUrl` (fallback `Icons.Outlined.Person` on a tint background). Owners are rendered as a small badge attached to each tile's bottom-inner corner with `Modifier.align(Alignment.BottomEnd)` (left tile) / `BottomStart` (right tile). For the v1 simpler layout, the owner name + avatar can live in a single row beneath the tiles instead of overlapping — pick whichever reads cleaner in the first preview pass.

## 5. State / behavior

### 5.1. VM in :feature:match:presentation

```kotlin
internal data class MatchCelebrationUiState(
    val myPet: PetDisplay? = null,
    val theirPet: PetDisplay? = null,
    val me: OwnerDisplay? = null,
    val them: OwnerDisplay? = null,
    val match: Match? = null,
    val errorMessage: String? = null,
) {
    val isReady: Boolean = match != null && myPet != null && theirPet != null
}

@HiltViewModel
internal class MatchCelebrationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionRepo: SessionRepository,
    private val observeMatch: ObserveMatchUseCase,
    private val petLookup: PetLookupRepository,
    private val ownerLookup: OwnerLookupRepository,
) : ViewModel() {

    // Hilt-Navigation3 surfaces route args via SavedStateHandle keyed by property name.
    private val matchIdValue: String = checkNotNull(savedStateHandle["matchId"])
    private val matchId = MatchId(matchIdValue)

    val uiState: StateFlow<MatchCelebrationUiState> = …
}
```

The VM collects:

1. `observeMatch(matchId)` → `Match?`. From the match it derives my uid (`sessionRepo.current()?.userId`) and the four ids: `myPetId`, `theirPetId`, `myOwnerId = me`, `theirOwnerId = match.otherOwnerId(me)`.
2. `flatMapLatest` into 4 parallel lookups: `petLookup.observe(myPetId)`, `petLookup.observe(theirPetId)`, `ownerLookup.observe(me)`, `ownerLookup.observe(them)`.
3. `combine(...) { match, myPet, theirPet, me, them -> UiState(...) }.stateIn(viewModelScope, ...)`.

**`Match.myPetId(me)` helper.** Currently `Match` exposes `otherPetId(me)` but not the inverse. Add a one-liner:

```kotlin
fun myPetId(me: String): String? = if (ownerAId == me) petAId else petBId
```

In `feature/match/domain/src/main/kotlin/com/rodiz/arch2/feature/match/domain/model/Match.kt`. Symmetric with `otherPetId(me)`.

**Loading state.** Both pet lookups + match resolve within a couple frames (Firestore listener is hot once the doc exists). Show a coral background with the headline visible + the tile area showing two pale skeleton placeholders (`Surface(color = Color.White.copy(alpha = 0.25f))`) until `isReady`. CTAs remain enabled — tapping "Say hello" doesn't depend on pet/owner data being ready.

### 5.2. DeckViewModel change

Replace the snackbar trigger with a navigation sentinel:

```kotlin
// Before
val matchMessage: String? = null,

// After
val pendingMatchId: String? = null,
```

And in `swipe(...)`:

```kotlin
// Before
is SwipeResult.Match -> _uiState.update { it.copy(matchMessage = "It's a match!") }

// After
is SwipeResult.Match -> _uiState.update { it.copy(pendingMatchId = result.matchId) }
```

`fun clearMatch() = _uiState.update { it.copy(pendingMatchId = null) }` stays (just operating on the new field).

### 5.3. DeckRoute → navigation

Today the route consumes `state.matchMessage` via a snackbar `LaunchedEffect`. Replace with:

```kotlin
LaunchedEffect(state.pendingMatchId) {
    state.pendingMatchId?.let { matchId ->
        viewModel.clearMatch()
        onMatchHappened(matchId)
    }
}
```

`onMatchHappened: (String) -> Unit` is a new callback parameter of `DeckRoute`, wired in `DeckNavModule.kt`:

```kotlin
entry<DeckHome> {
    DeckRoute(
        onAddPet = { navigator.goTo(AddPet) },
        onOpenFilters = { navigator.goTo(SettingsFilters) },
        onOpenNotifications = { navigator.goTo(SettingsNotifications) },
        onOpenPetDetail = { petId -> navigator.goTo(DeckPetDetail(petId.value)) },
        onMatchHappened = { matchId -> navigator.goTo(MatchCelebration(matchId)) },
    )
}
```

### 5.4. MatchCelebrationRoute → navigation

The screen's CTAs are callback-driven, wired in `MatchNavModule.kt`:

```kotlin
entry<MatchCelebration> { key ->
    MatchCelebrationRoute(
        matchId = key.matchId,                       // also reachable via SavedStateHandle
        onSayHello = {
            navigator.goBack()                       // pop celebration first…
            navigator.goTo(ChatRoute(key.matchId))   // …then push chat
        },
        onKeepSwiping = { navigator.goBack() },
    )
}
```

System back gesture from inside the screen = `onKeepSwiping` (just a `BackHandler { onKeepSwiping() }` for symmetry, optional since `goBack()` is the default).

## 6. Files to add / modify / delete

### Add
- `feature/match/nav/src/main/kotlin/com/rodiz/arch2/feature/match/nav/Routes.kt` — append `@Serializable data class MatchCelebration(val matchId: String)`.
- `feature/match/presentation/src/main/kotlin/com/rodiz/arch2/feature/match/presentation/MatchCelebrationScreen.kt` — the stateless composable.
- `feature/match/presentation/src/main/kotlin/com/rodiz/arch2/feature/match/presentation/MatchCelebrationRoute.kt` — VM injection + callback wiring.
- `feature/match/presentation/src/main/kotlin/com/rodiz/arch2/feature/match/presentation/MatchCelebrationViewModel.kt` — combines match + pet + owner lookups into UiState.
- `feature/match/presentation/src/test/kotlin/com/rodiz/arch2/feature/match/presentation/MatchCelebrationViewModelTest.kt` — unit tests (see §8).
- `feature/match/presentation/src/main/res/values/strings.xml` + `values-es/strings.xml` — append 4-6 new strings.

### Modify
- `feature/match/domain/src/main/kotlin/com/rodiz/arch2/feature/match/domain/model/Match.kt` — add `fun myPetId(me: String): String?` mirroring `otherPetId(me)`.
- `feature/match/presentation/src/main/kotlin/com/rodiz/arch2/feature/match/presentation/MatchNavModule.kt` — register the new `entry<MatchCelebration> { ... }`.
- `feature/deck/presentation/build.gradle.kts` — add `implementation(project(":feature:match:nav"))` if not already there.
- `feature/deck/presentation/src/main/kotlin/com/rodiz/arch2/feature/deck/presentation/DeckViewModel.kt` — rename `matchMessage: String?` → `pendingMatchId: String?` + update both setter and `clearMatch()`. Drop the hard-coded `"It's a match!"` literal (no longer needed; the celebration screen owns all copy).
- `feature/deck/presentation/src/main/kotlin/com/rodiz/arch2/feature/deck/presentation/DeckScreen.kt` — replace the `LaunchedEffect(state.matchMessage) { snackbarHostState.showSnackbar(...) }` with the navigation `LaunchedEffect(state.pendingMatchId)`. Add `onMatchHappened: (String) -> Unit` to the `DeckRoute` signature.
- `feature/deck/presentation/src/main/kotlin/com/rodiz/arch2/feature/deck/presentation/DeckNavModule.kt` — pass `onMatchHappened = { matchId -> navigator.goTo(MatchCelebration(matchId)) }`.
- `feature/deck/presentation/src/test/kotlin/com/rodiz/arch2/feature/deck/presentation/DeckViewModelTest.kt` — update the existing "match message" test to assert on `pendingMatchId` instead.

### Do NOT modify
- `feature/match/domain/model/MatchSummary.kt`, `InboxSnapshot`, `ObserveInboxUseCase` — the inbox flow stays the same; we're adding a sibling screen, not refactoring the existing match surface.
- `feature/chat/nav/Routes.kt` — `ChatRoute(matchId)` is reused as-is.
- `feature/deck/data/.../FirestoreDeckRepository.kt` + the Cloud Function (`onLikeCreate.ts`) — the `SwipeResult.Match(matchId)` contract is already exactly what we need.
- `firestore.rules` — no Firestore writes from this screen.
- Storage rules — N/A.

## 7. Critical recipes

1. **Two-step pop-then-push for "Say hello".** `navigator.goBack()` then `navigator.goTo(ChatRoute(matchId))` on the same callback frame. Both operate against the in-memory `SnapshotStateList`; no need for `LaunchedEffect` or `withFrameNanos`. Keeps the celebration out of the back stack so back-from-Chat lands on Deck.
2. **`SavedStateHandle` keys** map by **property name** for navigation3 + Hilt. The route param `val matchId: String` → `savedStateHandle.get<String>("matchId")`. Matches the existing pattern in `ChatViewModel` for its `@Assisted matchIdValue` (different style — chat uses Assisted-injection; the celebration uses SavedStateHandle for simplicity since it doesn't need Activity-scoped factories).
3. **`combine(...)` of 5 flows.** Kotlin's `combine` has overloads up to 5; if we need 6+ inputs (e.g., adding more lookups), switch to the vararg overload. For now `combine(match, myPet, theirPet, me, them) { … }` works.
4. **Owner / pet lookups can return `null`.** Both repos emit `null` for "doc doesn't exist yet". Render the fallback (initials chip / paw icon) instead of forcing the user to wait.
5. **The hot-StateFlow ready guard.** `isReady = match != null && myPet != null && theirPet != null`. Owner data isn't blocking — render the screen as soon as pets are present and let owner chips fade in when they resolve. This is the same pattern the chat header uses (see `ChatViewModel.kt`'s lookup wiring).
6. **`LightStatusBarIconsWhileShown()`** — extract into `core/ui/components/StatusBarIcons.kt` so Login + this screen + any future white-on-coral hero shares one implementation. Or inline the 10-line `DisposableEffect` block both places for now — pick when implementing.
7. **`PrimaryButton` recoloring.** The existing `core/ui/components/PrimaryButton.kt` accepts `containerColor` + `contentColor` overrides per its earlier usage in the Featured Pets work. White-on-coral is a single-call-site reuse, not a new component.
8. **Decorative paw sprinkles via `drawBehind`.** Pull from the Login hero's `drawBehind { drawCircle(...) }` recipe (radar rings) — same approach with `Icons.Outlined.Pets` rendered as `Painter` and drawn at fixed offsets.

## 8. Tests

JUnit5 + `MainDispatcherExtension` + hand-rolled fakes — matches `ChatViewModelTest` / `LoginViewModelTest` style.

`MatchCelebrationViewModelTest` (in `:feature:match:presentation`):

1. **Initial state** — `isReady == false`, all fields null.
2. **All lookups populated** — emits a UiState with `isReady == true`, my/their pets + owners filled, match present.
3. **`Match.myPetId(me)` and `otherPetId(me)` route correctly** — flip the test with ownerA vs ownerB context; verify the right pet ids are requested from the lookup.
4. **`otherPet` returns null** — UiState surfaces `theirPet == null`, screen would render the fallback. VM doesn't crash.
5. **Match emits null** — owner lookup short-circuits to null; `isReady == false`.
6. **Session has no signed-in user** — VM surfaces an error state (`errorMessage = "Sign-in required"` or similar) and `isReady == false`.

Update `DeckViewModelTest`:

7. **`swipe + SwipeResult.Match → pendingMatchId set`** — replace the existing snackbar-message assertion with `assertEquals("m1", vm.uiState.value.pendingMatchId)`.
8. **`clearMatch` sets pendingMatchId back to null**.

No Compose UI test required. Cover the tile / button layout with one preview per state (loading / both ready / fallback both null).

## 9. Verification

End-to-end on `emulator-5556` + `R5CY21NW7MV`:

1. **Mutual match path.** Sign in as user A; have a pet. Sign in as user B on the other device; have a pet. A likes B's pet first (no match → snackbar nothing). B likes A's pet → mutual → celebration screen appears for B with both pets' photos + names + "Say hello" / "Keep swiping" CTAs.
2. **Same beat on the other device.** A's Deck shows the celebration the next time A's `observeDeck` snapshot lands (because the match doc was created server-side via `onLikeCreate`). Actually — the celebration in this design fires only when the **liker** sees `SwipeResult.Match` come back, which means A *won't* see the celebration; A would discover the match via the inbox. Document this in the plan as an intentional limitation: only the user whose like completed the match sees the celebration. The other side learns via the inbox + push (future).
3. **"Say hello"** → pops celebration, pushes Chat. Back from Chat → Deck. Confirmed via `dumpsys activity activities | grep MainActivity` or just walking the screens.
4. **"Keep swiping"** + system back → both pop straight back to Deck. The next deck card should be ready (the just-liked pet is filtered out via the existing `recentlySwiped` set in `DeckViewModel`).
5. **Loading state** — kill the network momentarily; the celebration screen should render the headline immediately and fill in tiles + chips as lookups arrive (within ~200ms in practice).
6. **Locale** — `adb shell cmd locale set-app-locales com.rodiz.arch2.debug --locales es-MX`; verify Spanish strings render (headline + subtitle + both CTAs).
7. **Screenshots** — capture (a) loading skeleton state, (b) fully populated celebration, (c) post-`Say hello` chat. Surface paths in the final report.

## 10. Out of scope

- Animations (tiles dropping in, hearts confetti, haptics).
- Reachability from the matches inbox (it remains: inbox row tap → chat).
- Showing the celebration to the OTHER side of the match (the user who got liked first). That requires a separate "show pending matches once" mechanism — future work, ideally driven by an FCM push the other user receives.
- Sharing the match (screenshot, social).
- Match-on-match analytics events.

## 11. Risk & rollback

- **Risk:** the `pendingMatchId` field gets set but `LaunchedEffect` doesn't fire (Compose lifecycle edge case if the user backgrounds the app mid-swipe). Mitigated by `viewModel.clearMatch()` running inside the effect — if it never runs, the next foreground brings the effect back with the same non-null key and navigation completes.
- **Risk:** `Match.myPetId(me)` returns `null` because the match doc was created with `undefined` `petAId`/`petBId` (legacy / out-of-band creation). Mitigated by the fallback render path ("Your pet" / "Their pet" + paw glyph) — screen never crashes on null.
- **Risk:** the celebration intercepts the back gesture for users who like by accident. Mitigated by "Keep swiping" being a clear secondary action + the pop semantics (back gesture has the same outcome).
- **Rollback (UI only):** revert `DeckNavModule.kt` to drop the `onMatchHappened` wiring + revert `DeckScreen.kt` to restore the snackbar `LaunchedEffect`. The celebration files stay dormant until the wiring is restored. ~3-line revert.
- **Rollback (full):** revert the whole commit; no data-layer or rules changes to unwind.

## 12. Implementation order

0. Copy this plan to `plans/match-celebration-screen.md`.
1. Add `MatchCelebration(matchId)` route in `:feature:match:nav`. Build.
2. Add `Match.myPetId(me)` helper in `:feature:match:domain`. Build.
3. Add `MatchCelebrationViewModel` + its unit test. Run tests.
4. Add `MatchCelebrationScreen` (composable + previews). Render loading + populated states.
5. Add `MatchCelebrationRoute` (callback-driven, calls into VM). Register entry in `MatchNavModule`.
6. Add `:feature:match:nav` to `feature:deck:presentation/build.gradle.kts` if missing. Build.
7. Refactor `DeckViewModel` (`matchMessage` → `pendingMatchId`) + update `DeckViewModelTest`. Run tests.
8. Refactor `DeckScreen` + `DeckRoute` (snackbar → navigation `LaunchedEffect`). Wire `onMatchHappened` in `DeckNavModule`.
9. Add strings (EN + ES). Confirm `values-es/` parity.
10. Build + install on `emulator-5556`. Walk verification §9. Capture screenshots.
11. Single local commit. No push without explicit ask. No `Co-Authored-By` trailer. No Anthropic references in copy or commit message.

## 13. Critical files

- `feature/deck/presentation/src/main/kotlin/com/rodiz/arch2/feature/deck/presentation/DeckViewModel.kt:145-156` — `swipe(...)` end of when-block; pivot from `matchMessage` to `pendingMatchId`.
- `feature/deck/presentation/src/main/kotlin/com/rodiz/arch2/feature/deck/presentation/DeckScreen.kt:76-81` — snackbar `LaunchedEffect` to replace.
- `feature/deck/presentation/src/main/kotlin/com/rodiz/arch2/feature/deck/presentation/DeckNavModule.kt` — add the `onMatchHappened` wiring.
- `feature/match/domain/src/main/kotlin/com/rodiz/arch2/feature/match/domain/model/Match.kt` — add `myPetId(me)`.
- `feature/match/domain/src/main/kotlin/com/rodiz/arch2/feature/match/domain/usecase/MatchUseCases.kt` — `ObserveMatchUseCase` is already what we need; no change.
- `feature/match/nav/src/main/kotlin/com/rodiz/arch2/feature/match/nav/Routes.kt` — new `MatchCelebration(matchId)` route.
- `feature/match/presentation/src/main/kotlin/com/rodiz/arch2/feature/match/presentation/MatchNavModule.kt` — register the new entry.
- `core/petlookup/domain/src/main/kotlin/com/rodiz/arch2/core/petlookup/domain/PetLookupRepository.kt` — reused as-is.
- `core/ownerlookup/domain/src/main/kotlin/com/rodiz/arch2/core/ownerlookup/domain/OwnerLookupRepository.kt` — reused as-is.
- `core/session/domain/src/main/kotlin/com/rodiz/arch2/core/session/domain/SessionRepository.kt` — `current().userId` to know which uid is "me".
- `core/ui/components/PrimaryButton.kt` — reused for the white "Say hello" CTA via color overrides.
- `feature/login/presentation/.../LoginScreen.kt:LightStatusBarIconsWhileShown` — copy the pattern (10 lines) or extract into `:core:ui`.

# Plan — "That's everyone for now" empty-deck screen

> **NOTE on plan location:** project convention is to save plans under `plans/` in the Arch2.0 repo. Plan mode pins the working copy here. **Step 0 of implementation: copy this file to `plans/empty-deck-screen.md`** so it gets versioned with the code.

## 1. Context

Today, when the user has swiped through every pet the deck has to offer, `DeckScreen.kt:143-150` renders a generic `DeckEmptyState` composable — a centered 96dp paw icon, the headline string `deck_empty_no_more_title`, and a one-liner body. No actions, no path forward. The user is stuck looking at a dead screen until either:
- they realise they can tap the Filters icon in the header to widen the radius, or
- they leave the app and a new pet happens to be created.

The mock (`/Users/jrodiz/Desktop/no-pets-discover.png`) reimagines that state as a **hero illustration + 3-card action list**, giving the user three concrete ways out:
1. **Widen your distance** — open Filters, recover with a bigger radius.
2. **Review who you passed** — undo today's passes in bulk so the deck re-populates.
3. **Add another pet** — open AddPet (each pet gets its own deck filter).

This plan keeps the work inside `:feature:deck:presentation` (no new module). The empty-state surface is still part of the Deck route, just a richer rendering of `DeckState.EXHAUSTED`. Filters + AddPet routes are already wired in `DeckNavModule.kt:9-10,26-27`.

## 2. Final flow

```
DeckScreen with state.state == EXHAUSTED
  ├─ Hero: paw centerpiece + dashed sniff-zone illustration
  ├─ Headline: "That's everyone for now"
  ├─ Subtitle: "You've seen every pet within <maxDistanceKm> km.
  │             New pets join every day — we'll ping you when
  │             there's a new match."         (no pet name, per user)
  └─ Action cards (Column, spacing 12.dp):
      ├─ [Widen your distance]   → onOpenFilters()      (existing)
      │     "More pets within <maxDistanceKm * 2> km"   (approximate, no count)
      ├─ [Review who you passed] → onReviewPasses()     NEW
      │     "Take another look at the pets you skipped today"
      └─ [Add another pet]       → onAddPet()           (existing)
            "Each pet gets its own fresh deck"
```

The header (`Discover`, `Pets near you`, `25 km` chips, filter icon, bell) stays — the empty body just replaces the cards/skeleton area.

The next-tier suffix on Widen: `nextTierKm = (maxDistanceKm * 2).coerceAtMost(200)`. If already at 200km, the suffix reads "Across the whole map" and the card still navigates to Filters so the user can adjust species/intents instead.

## 3. Module layout

Everything stays inside `:feature:deck:{domain, data, presentation}`.

- `:feature:deck:domain` gains `ReviewPassedPetsUseCase` that delegates to a new `DeckRepository.clearTodayPasses(): Int` (returns count of cleared passes; the screen will surface it in a snackbar so the user knows the deck moved).
- `:feature:deck:data` (`FirestoreDeckRepository`) implements `clearTodayPasses` as a batched delete of `passes` docs where `ownerId == me && createdAt >= startOfToday()`.
- `:feature:deck:presentation` gains a new `DeckExhaustedState` composable (with hero illustration + 3 action cards) and a new `ActionRow` private component. `DeckViewModel` gains `reviewPasses()` + `clearReviewMessage()`.

No new feature modules, no new `:core/*` modules.

## 4. Screen design (from the mock)

Layout, top to bottom **inside** the existing Deck `Box` body (below the header strip), on the existing `BrandColors.WarmSnow`/whatever-the-deck-uses background:

- 40dp top spacer (header strip already provides its own padding).
- **Hero illustration**, ~280×180dp, centered:
  - A rounded-rect dashed border around the whole illustration (`drawBehind { drawRoundRect(... pathEffect = PathEffect.dashPathEffect) }`).
  - 4 small scattered marks inside (alternating tiny dots + 'z' chip + 'x' chip — see mock); render with `Icons.Filled.Close` rotated small, `Text("z")` in a tiny white pill, and `Box(size = 6.dp, background = Coral)` dots.
  - Centerpiece: ~160dp circle in `BrandColors.CoralLight.copy(alpha=0.45f)` with `Icons.Outlined.Pets` 56dp in `BrandColors.CoralDeep` inside.
- 32dp spacer.
- Headline `"That's everyone for now"` — `headlineSmall ExtraBold`, centered, `semantics { heading() }`.
- 8dp spacer.
- Subtitle: `"You've seen every pet within <X> km. New pets join every day — we'll ping you when there's a new match."` — `bodyMedium`, 70% on-surface, centered, max-width preserved by horizontal padding. `<X>` is `state.maxDistanceKm` (already in `DeckUiState`).
- 24dp spacer.
- **Three action cards** (Column with `Arrangement.spacedBy(12.dp)`, horizontal padding 16dp):
  - Each card = `Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp)` containing a `Row { LeadingIconBubble; Spacer(12.dp); Column(title, body); Spacer(weight=1f); TrailingSlot }`.
  - **LeadingIconBubble**: 44dp circle, soft-tinted background per card (coral / orange / sage).
  - **TrailingSlot**: card 1 = `Text("Expand", color = BrandColors.CoralDeep)`; cards 2 & 3 = `Icons.AutoMirrored.Filled.ChevronRight`.
  - Whole card is `clickable { ... }`.
- 24dp bottom spacer (the bottom nav bar already pads itself).

A loading/empty fallback for the existing minimal `DeckEmptyState` (used when `cards` is empty but state != EXHAUSTED, e.g. mid-load) is **kept as-is** — only the EXHAUSTED branch swaps to the new composable. This keeps the "deck is still loading" state stable.

## 5. State / behavior

### 5.1 DeckUiState

`maxDistanceKm: Int` is already there (set by the filter-prefs collector). Add one more field for the post-review feedback:

```kotlin
// Non-null right after the user taps "Review who you passed" — the screen
// shows a snackbar ("Restored 14 pets") then clears via clearReviewMessage().
// Plural-aware string built in the composable so we don't put copy in VM.
val reviewedPassesCount: Int? = null,
```

### 5.2 DeckViewModel

Add:

```kotlin
fun reviewPasses() {
    viewModelScope.launch {
        val count = runCatching { reviewPassedPets() }
            .getOrElse { e ->
                _uiState.update { it.copy(errorMessage = e.message ?: "Couldn't restore passes") }
                return@launch
            }
        // Drop in-memory swipe filter so the just-restored pets reappear immediately.
        recentlySwiped.clear()
        _uiState.update { it.copy(reviewedPassesCount = count) }
    }
}

fun clearReviewMessage() = _uiState.update { it.copy(reviewedPassesCount = null) }
```

Constructor gets a new dep: `private val reviewPassedPets: ReviewPassedPetsUseCase`.

### 5.3 Domain

`feature/deck/domain/.../usecase/ReviewPassedPetsUseCase.kt`:

```kotlin
class ReviewPassedPetsUseCase @Inject constructor(
    private val deckRepo: DeckRepository,
) {
    suspend operator fun invoke(): Int = deckRepo.clearTodayPasses()
}
```

`DeckRepository` interface gains `suspend fun clearTodayPasses(): Int`.

### 5.4 Data

`FirestoreDeckRepository.clearTodayPasses(): Int`:

```kotlin
override suspend fun clearTodayPasses(): Int = withContext(io) {
    val me = sessionRepo.current()?.userId ?: return@withContext 0
    val startOfDay = startOfTodayMillis()  // Clock.System.now() truncated to local midnight
    val snap = passesCol
        .whereEqualTo("ownerId", me)
        .whereGreaterThanOrEqualTo("createdAt", Timestamp(startOfDay / 1000, 0))
        .get()
        .await()
    if (snap.isEmpty) return@withContext 0
    val batch = firestore.batch()
    snap.documents.forEach { batch.delete(it.reference) }
    batch.commit().await()
    snap.size()
}
```

Notes:
- Reuses the existing `passesCol`, `sessionRepo`, `io` dispatcher already in this repo.
- "Today" = local-midnight. `kotlinx.datetime.Clock.System.now()` → `toLocalDateTime(TimeZone.currentSystemDefault()).date.atStartOfDayIn(...)` → epoch ms.
- Returns the count so the VM can show "Restored 14 pets" (or "Restored a pet" / "Restored N pets" — plural handled in the composable via `pluralStringResource`).

### 5.5 DeckScreen wiring

Replace the EXHAUSTED branch:

```kotlin
state.state == DeckState.EXHAUSTED -> {
    DeckExhaustedState(
        maxDistanceKm = state.maxDistanceKm,
        onWidenDistance = onOpenFilters,
        onReviewPasses = viewModel::reviewPasses,
        onAddAnotherPet = onAddPet,
    )
}
```

`DeckRoute` gains no new callback params — `onOpenFilters` + `onAddPet` already exist; `reviewPasses` is a VM method called directly from the screen-level state.

Snackbar for the post-review count: a new `LaunchedEffect(state.reviewedPassesCount)` block fires `snackbarHostState.showSnackbar("Restored $count pet${if (count==1) "" else "s"}")` then `viewModel.clearReviewMessage()`. Use the existing snackbar host that the route already wires (the same one the existing error snackbar uses — see how it consumes `state.errorMessage`).

## 6. Files to add / modify

### Add
- `feature/deck/domain/src/main/kotlin/com/rodiz/arch2/feature/deck/domain/usecase/ReviewPassedPetsUseCase.kt`
- `feature/deck/presentation/src/main/kotlin/com/rodiz/arch2/feature/deck/presentation/components/DeckExhaustedState.kt` (the new composable + private `ActionCard` and `HeroIllustration`)
- `feature/deck/presentation/src/test/kotlin/com/rodiz/arch2/feature/deck/presentation/ReviewPassesTest.kt` (one tests in the existing test class is fine too — see §8)

### Modify
- `feature/deck/domain/src/main/kotlin/com/rodiz/arch2/feature/deck/domain/repository/DeckRepository.kt` — add `suspend fun clearTodayPasses(): Int`.
- `feature/deck/data/src/main/kotlin/com/rodiz/arch2/feature/deck/data/FirestoreDeckRepository.kt` — implement `clearTodayPasses`.
- `feature/deck/data/src/test/kotlin/com/rodiz/arch2/feature/deck/data/FakeDeckRepository.kt` (if it exists) — implement the same method returning a settable count.
- `feature/deck/presentation/src/main/kotlin/com/rodiz/arch2/feature/deck/presentation/DeckViewModel.kt` — new constructor dep, `reviewPasses()`, `clearReviewMessage()`, `reviewedPassesCount` field on `DeckUiState`.
- `feature/deck/presentation/src/main/kotlin/com/rodiz/arch2/feature/deck/presentation/DeckScreen.kt` — swap the EXHAUSTED branch to `DeckExhaustedState`; new snackbar `LaunchedEffect`.
- `feature/deck/presentation/src/test/kotlin/com/rodiz/arch2/feature/deck/presentation/DeckViewModelTest.kt` — see §8.
- `feature/deck/presentation/src/main/res/values/strings.xml` + `values-es/strings.xml` — new strings (headline, subtitle, three card titles + bodies, snackbar plurals).

### Do NOT modify
- `DeckNavModule.kt` — no new routes (Filters + AddPet already wired).
- `core/filters/domain` — we read existing prefs; we don't write the radius from this screen.
- `firestore.rules` — `passes` already has `allow delete: if … resource.data.ownerId == request.auth.uid`. Bulk delete works under the existing rule.

## 7. Critical recipes

1. **Local-midnight "today" boundary**. `kotlinx.datetime.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()`. Use this — not UTC midnight — so "today's passes" matches the user's wall-clock day. The data module already pulls `kotlinx-datetime`.
2. **Plural snackbar string**. `<plurals name="deck_review_restored_count"><item quantity="one">Restored one pet</item><item quantity="other">Restored %d pets</item></plurals>` + `pluralStringResource(..., count, count)` in the composable.
3. **Path-effect dashed border**. `Modifier.drawBehind { drawRoundRect(color = BrandColors.CoralLight, style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))), cornerRadius = CornerRadius(28.dp.toPx())) }`. No third-party dep.
4. **`recentlySwiped.clear()` after restore**. Without this, the just-restored pets would be silently filtered out by the in-memory swipe set in `DeckViewModel.swipe(...)`. Critical for "the deck visibly re-populates" to work.
5. **Next-tier suffix logic**. `(maxDistanceKm * 2).coerceAtMost(200)`. At 100km → "within 200 km"; at 200km → "Across the whole map". Keep this in the composable, not the VM.

## 8. Tests

Add to `DeckViewModelTest`:

1. **`reviewPasses success → snackbar count set, recentlySwiped cleared, deck not affected`** — fake use case returns 14; assert `vm.uiState.value.reviewedPassesCount == 14`; assert a subsequent same-pet swap doesn't get filtered (i.e. the in-memory set is empty).
2. **`reviewPasses failure → errorMessage set, no count`** — fake throws; assert `errorMessage` populated and `reviewedPassesCount == null`.
3. **`clearReviewMessage clears the count`** — set count, call clear, assert null.

No new test infrastructure — `MainDispatcherExtension` + hand-rolled fakes (same pattern as the existing `DeckViewModelTest`). `FakeDeckRepository` (the in-memory test double already in the file) gains `var clearTodayCount: Int = 0` and a counter for invocations.

`ReviewPassedPetsUseCase` is a one-line passthrough; no unit test needed.

Composable previews:
- `DeckExhaustedState` preview at `maxDistanceKm = 25` (suffix reads "within 50 km") and one at `maxDistanceKm = 200` (suffix reads "Across the whole map") to catch the edge.

## 9. Verification

End-to-end on `emulator-5554` (re-seed first via `curl "https://us-central1-arch2-cac87.cloudfunctions.net/seedTestData?secret=tinpet-seed-2026-05-25"`):

1. Sign in as Lena. Swipe through every pet in the default 25km radius (pass all). The empty screen should render with the headline, the dashed-border hero, and three action cards.
2. **Widen your distance** → opens Filters. Tap back. The cards reappear.
3. **Review who you passed** → snackbar shows "Restored N pets" (N matches how many passes Lena made today). Deck re-populates with those pets within ~1 second.
4. **Add another pet** → opens AddPet. Tap back. Cards still present.
5. **Reseed** mid-session to verify the live `maxDistanceKm` reflows the subtitle ("within 50 km" if user moves the filter to 50km).
6. **Locale** — `adb shell cmd locale set-app-locales com.rodiz.arch2.debug --locales es-MX`; verify Spanish strings render (headline + subtitle + three card titles).

Capture screenshots: (a) the empty state populated, (b) post-review snackbar with deck reflowing, (c) Spanish locale render.

## 10. Out of scope

- A separate browsable list of passed pets (per the user's pick: one-shot bulk-restore wins over a list screen).
- Accurate "+N pets within Mkm" counts (deferred — the card uses the approximate suffix only).
- Per-pet name in the subtitle (deferred — copy is pet-agnostic for v1).
- Custom illustration assets (the v1 hero is drawn with Compose primitives + the `Pets` icon; if design wants a richer SVG later, swap in an `Image(painter = …)` keeping the dashed border).
- Notifications hookup ("we'll ping you" is aspirational copy — the actual nudge push is a separate piece of work).

## 11. Risk & rollback

- **Risk:** `clearTodayPasses` deletes more or fewer docs than expected if device clock is wrong. Mitigated by trusting the device's local time (same convention the rest of the app uses for "today").
- **Risk:** the Firestore query `passes where ownerId == me && createdAt >= startOfToday` needs a composite index. The existing `firestore.indexes.json` already has `passes (ownerId ASC, createdAt DESC)` for the deck filter; this query reuses it. If the deploy says a new index is missing, the use case will fail loudly — handle by adding the index to `firestore.indexes.json` and re-deploying.
- **Risk:** the in-memory `recentlySwiped` set in `DeckViewModel` getting cleared mid-swipe (race between `reviewPasses` and `swipe`). The set is single-thread on the VM scope; clearing it after the suspend-then-resume of `reviewPasses` is safe.
- **Rollback:** revert the `DeckScreen` EXHAUSTED branch to call the old `DeckEmptyState`. The new `DeckExhaustedState` composable becomes dormant. ~5-line revert.

## 12. Implementation order

0. Copy this plan to `plans/empty-deck-screen.md`.
1. Add `clearTodayPasses` to `DeckRepository` interface + `FirestoreDeckRepository` impl + fake. Build.
2. Add `ReviewPassedPetsUseCase`. Build.
3. Wire `DeckViewModel` (new dep + `reviewPasses` + `clearReviewMessage` + `reviewedPassesCount`). Update `DeckViewModelTest`. Run tests.
4. Write `DeckExhaustedState` composable (hero + 3 cards) with two previews. Render in Android Studio preview to sanity-check the dashed border + sprinkles.
5. Swap the EXHAUSTED branch in `DeckScreen` + add the post-review snackbar `LaunchedEffect`.
6. Strings: add EN + ES + plurals.
7. Build + install on `emulator-5554`. Walk §9 verification end-to-end. Capture screenshots.
8. Single local commit. No push without explicit ask. No `Co-Authored-By` trailer. No Anthropic references in copy or commit message.

## 13. Critical files

- `feature/deck/presentation/src/main/kotlin/com/rodiz/arch2/feature/deck/presentation/DeckScreen.kt:143-150` — current EXHAUSTED branch to swap.
- `feature/deck/presentation/src/main/kotlin/com/rodiz/arch2/feature/deck/presentation/DeckScreen.kt:476-515` — existing `DeckEmptyState` to keep around for the non-EXHAUSTED-empty case.
- `feature/deck/presentation/src/main/kotlin/com/rodiz/arch2/feature/deck/presentation/DeckViewModel.kt` — new dep + methods + field on `DeckUiState`.
- `feature/deck/domain/src/main/kotlin/com/rodiz/arch2/feature/deck/domain/repository/DeckRepository.kt` — add `clearTodayPasses(): Int`.
- `feature/deck/data/src/main/kotlin/com/rodiz/arch2/feature/deck/data/FirestoreDeckRepository.kt:47-48,192-201,247-260` — existing `passesCol` + write/undo paths to mirror for the bulk delete.
- `feature/deck/presentation/src/main/kotlin/com/rodiz/arch2/feature/deck/presentation/DeckNavModule.kt:9-10,26-27` — `AddPet` + `SettingsFilters` already wired; nothing to change here.
- `core/designsystem/src/main/kotlin/com/rodiz/arch2/core/designsystem/theme/BrandColors.kt` — `Coral`, `CoralLight`, `CoralDeep` for the hero + icon-bubble tints.

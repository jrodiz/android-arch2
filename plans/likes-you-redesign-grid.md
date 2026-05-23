# Plan: Likes you screen — grid redesign

> Parent feature spec: [`plans/likes-you.md`](./likes-you.md). Brand vocabulary & shell: [`plans/floating-chip-bottom-nav.md`](./floating-chip-bottom-nav.md), [`plans/deck-redesign-discover.md`](./deck-redesign-discover.md), [`plans/deck-pet-details.md`](./deck-pet-details.md).
>
> This plan is a **visual redesign** of the existing `LikesYouRoute` in `:feature:likes:presentation`. It does **not** change the domain, data, navigation contract, or matching semantics — every wire still goes through `ObserveLikesYouUseCase`, `PassLikeUseCase`, `LikeBackUseCase`. The screen is reshaped from a plain card grid with a `TopAppBar` + tap-to-open-bottom-sheet into a TinPet-branded grid: warm header with eyebrow + headline + small count pill, filter-chip row, and bigger image-forward tiles with relative-time/NEW/intent pills and a name/age + distance overlay.

## 1. Context

### What's changing
- Replace the `TopAppBar` + `LazyVerticalGrid` shell with the brand header pattern used elsewhere (eyebrow label + bold headline) plus a small inline coral count pill ("♥ 6 new").
- Add a horizontally-scrollable filter chip row directly below the sub-headline: **All** (default, dark pill) + one chip per `Intent` (Playdate, Adoption, Friendship). Filtering is purely client-side — it narrows the visible grid items by `anchorPet.intents`.
- Redesign `LikeCard` to match the mock: full-bleed pet photo, top-left dark "relative time" pill, top-right coral "NEW" pill on unseen likes, bottom-left intent pill colored by `Intent` (Playdate=coral, Friendship=mint, Adoption=coral-deep — same palette as the deck detail's `IntentChip`), and below the photo overlay the pet name + age in bold white plus a small pin icon with the synthetic distance bucket ("< 5 km" etc.).
- Drop the old bottom-sheet that opened on card tap. Tap on a card → navigate to `DeckPetDetail(anchorPet.id)` — the just-shipped pet detail screen owns the "view + like/pass" surface already. This eliminates a duplicate pet-summary surface and keeps the like-back action exactly where it lives for the deck.
- Restyle the empty / loading states to fit the new shell (header stays, body becomes the empty card art).

### Why
- The current screen renders as a plain Material grid with a `TopAppBar` — it visually disconnects from the rest of the app (deck, signup, login) which use the bold warm header pattern.
- The card content currently shows the owner's truncated UID ("Abc12345… liked your pet") instead of presenting like a Tinder-style "Likes you" grid. The mock leans into the photo + meta overlay convention which is more inviting and more honest to what users care about.
- The tap → custom bottom sheet path duplicates `DeckPetDetail`. Both surfaces show a pet's photo + name/age + species + intent chips + bio + Like/Pass action. Sharing one detail screen is consistent and one fewer surface to maintain.

### Non-goals
- No changes to `:feature:likes:domain` or `:feature:likes:data` semantics (filtering remains client-side, anchor-pet selection unchanged).
- No real distance plumbing through the data layer — `IncomingLike` does not currently carry a distance bucket and adding that would mean threading owner locations through Firestore queries (see §2 decision D3). Visual stand-in only for v1.
- No "unread"/seen tracking persisted to Firestore — the `NEW` pill uses an in-memory `seenKeys` set inside the ViewModel that resets on process death (see §2 D2).
- No bottom-nav changes — the screen is hosted under the floating chip nav already.
- No animated card-removed choreography beyond what the existing list animation gives us.

## 2. Confirmed decisions / inferred decisions

| # | Decision | Source |
|---|---|---|
| D1 | **Tap on a card opens `DeckPetDetail(anchorPet.id)`** rather than calling `likeBack` directly or opening a bottom sheet. | Inferred. Parent brief surfaced both options. Rationale: `DeckPetDetail` is the canonical "view + Like/Pass" surface; reusing it gives the user a full photo, bio, and intent picture before committing — more careful than a one-tap like-back. The mock's sub-headline ("Tap any pet to like back and start a match") is still honored: from the detail screen, tap the coral "Like <name>" CTA → reciprocity is satisfied → match. `:feature:likes:presentation` already depends on `:feature:deck:nav`, so this is a no-op for module wiring. |
| D2 | **"NEW" pill** uses a presentation-only `seenKeys: MutableSet<String>` inside the ViewModel. Every key absent from the set is "new"; opening the detail (or scrolling the card past) adds it to the set; set lives in `viewModelScope`, lost on process death. | Inferred. Firestore-side `seen` flag is a v2 concern (parent likes plan §11 lists this kind of work as future-fcm). Keeping it in-memory unblocks the visual without invading the data layer. |
| D3 | **Distance bucket** is a deterministic synthetic value derived from `like.key.value.hashCode().mod(4)` mapped onto the four `DistanceBucket` enum cases. Stand-in only — labels still come from `DistanceBucket.label` so when real distance lands, swapping the source is a one-line change. | Inferred. Plumbing real distance through `IncomingLike` would require: (a) reading my own location and the liker's location in `FirestoreLikesYouRepository`, (b) adding a `distanceBucket` field to the domain model, (c) updating the Firestore query path. Out of scope for a visual redesign. Captured as follow-up in §10. |
| D4 | **Filter chip row** is rendered as 4 pills (All, Playdate, Adoption, Friendship) ordered exactly as in the mock. Selected = filled dark pill (`BrandColors.NavSurface`, white text). Unselected = transparent pill with `onSurfaceVariant` border + onSurface text. Single-select (radio-style), default = All. | Mock. Order: All, Playdate, Adoption, Friendship — matches the mock left-to-right. |
| D5 | **Filter chips do not affect the visible "X new" count** — the count pill always reflects the unfiltered "unseen" total, so filtering doesn't make the badge fluctuate as the user explores. | Inferred. Filtering is a view of the data; the count is a status indicator. |
| D6 | **`like.likedAt` relative-time** is rendered with a small local helper: "just now" (<60s), "5m", "2h", "1d", "5d", "12w" — same family as common social-feed shorthands. No `kotlinx.datetime.format` dependency; just an `Instant` diff. | Inferred from the mock's "just now", "2h", "1d", "2d", "3d", "4d" labels. |
| D7 | **No more in-screen bottom sheet, no more snackbar-based "Pass" path.** Pass is no longer a primary action on this surface — it lives only on `DeckPetDetail` where the user has full context. This removes the only call site of `PassLikeUseCase` from `LikesYouViewModel`. The use case + repo method stay (no behavior regression — they're still useful later for swipe-to-dismiss). | Inferred. Mock has no Pass affordance on the grid. |

## 3. Visual spec

All measurements approximate from the mock; tune on emulator. Colors come from `BrandColors` (already defined — do not add new ones).

### 3.1 Background + safe areas
- Root `Scaffold(containerColor = MaterialTheme.colorScheme.background)` — warm off-white `#FFFBFA`, consistent with Deck.
- Content uses `.statusBarsPadding()` (no insets via Scaffold, since the floating nav bar handles bottom inset itself).
- Bottom of `LazyVerticalGrid` adds `contentPadding(bottom = 120.dp)` so the last row doesn't sit under the floating chip nav.

### 3.2 Header (above the grid; non-scrolling)
```
Column(start=20dp, end=20dp, top=4dp)
├─ "Incoming"                  labelMedium, letterSpacing 0.6sp, color = onSurfaceVariant
├─ Spacer 2dp
├─ Row(verticalAlignment = CenterVertically)
│   ├─ "Likes you"             displaySmall, weight = ExtraBold, color = onSurface
│   ├─ Spacer 12dp
│   └─ CountPill("♥ 6 new")    coral filled pill, only shown when unseenCount > 0
├─ Spacer 6dp
└─ "Tap any pet to like back and start a match"   bodyMedium, color = onSurfaceVariant
```

`CountPill` is a small `Surface` with `BrandColors.Coral`, `RoundedCornerShape(percent = 50)`, padding `(start=10dp, end=12dp, vertical=6dp)`, white `Icons.Filled.Favorite` 12dp + `Text("$count new", labelMedium SemiBold, color = white)`.

### 3.3 Filter chip row
```
LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), spacedBy = 10.dp)
└─ items(All, Playdate, Adoption, Friendship) { FilterPill(label, selected, onClick) }
```

`FilterPill`:
- Selected: `Surface(shape = CircleShape, color = BrandColors.NavSurface)` + text `Color.White`.
- Unselected: `Surface(shape = CircleShape, color = Transparent, border = onSurfaceVariant.copy(alpha = 0.4f), 1.dp)` + text `onSurface`.
- Both: `padding(horizontal = 18.dp, vertical = 10.dp)`, `titleSmall` weight SemiBold.
- Spacer 16dp under the row before the grid starts.

### 3.4 Grid
```
LazyVerticalGrid(
  columns = GridCells.Fixed(2),
  contentPadding = PaddingValues(horizontal = 16.dp, top = 4.dp, bottom = 120.dp),
  verticalArrangement = Arrangement.spacedBy(16.dp),
  horizontalArrangement = Arrangement.spacedBy(12.dp),
)
└─ items(filteredLikes, key = { it.key.value }) { LikeCard(...) }
```

### 3.5 `LikeCard`
Card shape: `RoundedCornerShape(20.dp)`, `Modifier.aspectRatio(3f/4f)`, no Material elevation (shadow is too cluttered in a 2-col grid — flat with subtle outline reads cleaner). Clickable → `onCardTap(anchorPet.id)`.

```
Box(fillMaxSize().clip(RoundedCornerShape(20.dp)))
├─ AsyncImage(anchorPet.photos[0])              // contentScale = Crop, fillMaxSize
│   └─ if null: surfaceVariant fill + Icons.Outlined.Pets 48dp tinted onSurfaceVariant
├─ Box(BottomStart, fillMaxWidth, h=auto)       // gradient scrim (top transparent → bottom black 0.55)
└─ Row top-row chips (top=10dp, start=10dp, end=10dp)
    ├─ RelativeTimePill                         // dark
    ├─ Spacer(1f).weight(1f)
    └─ NewPill (if unseen)                      // coral
└─ IntentPill (Bottom-Start of photo, start=10dp, bottom=64dp)
└─ Column overlay (Bottom-Start, start=12dp, bottom=10dp)
    ├─ Text("${name}, ${ageYears}")             titleMedium SemiBold, white
    └─ Row { Icon(Place, 12dp, white@0.8) + Spacer 4dp + Text(distance label, labelSmall, white@0.8) }
```

#### Pills
- `RelativeTimePill`: `Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.55f))`, content `padding(horizontal = 10.dp, vertical = 5.dp)`, text `labelSmall` SemiBold white.
- `NewPill`: `Surface(shape = CircleShape, color = BrandColors.Coral)`, content `padding(horizontal = 8.dp, vertical = 4.dp)`, text `"NEW"` `labelSmall` Bold letterSpacing 0.8sp white.
- `IntentPill`: reuses the visual recipe from `DeckPetDetailScreen.IntentChip` but the call site lives in `LikesScreen.kt` (no cross-feature presentation import). Colors per intent:
  - `PLAYDATE` → `BrandColors.Coral` + `Icons.Outlined.Pets`
  - `FRIENDSHIP` → `BrandColors.MintLeaf` + `Icons.AutoMirrored.Outlined.Chat`
  - `ADOPTION` → `BrandColors.CoralDeep` + `Icons.Outlined.Favorite`
  - `padding(horizontal = 10.dp, vertical = 5.dp)`, icon 11.dp, text `labelSmall` SemiBold white. (Smaller than the deck detail's chip so it fits comfortably on a 2-col tile.)
- A card has at most one intent chip rendered; if `anchorPet.intents` has multiple we pick the first present in the canonical order Playdate > Friendship > Adoption (same precedence the deck detail uses for its intent row).

### 3.6 Empty / loading
- Loading (`isLoading == true && likes.isEmpty()`): the header is still visible; below it a `Box(fillMaxSize, contentAlignment = Center)` with `CircularProgressIndicator(color = BrandColors.Coral)`.
- Empty (`likes.isEmpty() && !isLoading`): the header stays; below it the existing `EmptyTabState(icon = Favorite, headline = "No one yet", body = "When someone likes one of your pets, they'll show up here.", cta = "Go to Deck", onCta = onGoToDeck)`.
- "Filter has no matches" (`likes.isNotEmpty() && filteredLikes.isEmpty()`): small inline empty state — a `Column` centered in the grid area with `Icons.Outlined.SearchOff` 56dp tinted onSurfaceVariant + "No <intent> likes yet" body. No CTA — clearing the filter is one tap away.

## 4. Component changes

### `:core:ui` — none

### `:core:designsystem` — none
- `EmptyTabState` reused as-is.
- No new tokens. The mint, coral, coralDeep, coralLight, navSurface, and onPattern colors are all already in `BrandColors`.

### Local to `:feature:likes:presentation`
- New private composables in `LikesScreen.kt`: `LikesHeader`, `CountPill`, `FilterChipRow`, `FilterPill`, `IntentPill` (local — small enough not to share, and the deck detail's chip lives in `DeckPetDetailScreen.kt`'s private scope anyway).
- New private helpers in `LikesScreen.kt`: `relativeTimeLabel(now, then)`, `syntheticDistanceBucket(key)` (D3 stand-in), `displayIntent(intents)` (selects the canonical chip per card).

## 5. State / behavior changes

### 5.1 `LikesYouUiState`
Add fields; do not remove existing ones (data-layer-driven `likes`, `expandedKey`, `matchMessage`, `errorMessage` stay — `expandedKey` becomes unused by the new UI but is harmless to leave for now, and removing it would require touching the data layer's expectations from the existing tests).

```kotlin
internal data class LikesYouUiState(
    val likes: List<IncomingLike> = emptyList(),
    val expandedKey: LikeKey? = null,
    val matchMessage: String? = null,
    val errorMessage: String? = null,
    // NEW
    val isLoading: Boolean = true,
    val activeFilter: LikesFilter = LikesFilter.All,
    val seenKeys: Set<String> = emptySet(),
)

internal sealed interface LikesFilter {
    data object All : LikesFilter
    data class ByIntent(val intent: com.rodiz.arch2.feature.pet.domain.model.Intent) : LikesFilter
}
```

### 5.2 ViewModel
- Flip `isLoading = false` on the first emission of `observeLikesYou()`.
- Add `onFilterSelected(filter: LikesFilter)` → updates `activeFilter`.
- Add `markSeen(key: LikeKey)` → adds `key.value` to `seenKeys` (immutable `+` copy).
- Keep `onCardTap(key)` — but it becomes "mark seen + navigate" plumbed through the composable. (`expandedKey` plumbing stays in the ViewModel as dead code for this PR.)
- Add a `filteredLikes` derivation in the composable (cheap; not in state).

### 5.3 Route signature
```kotlin
@Composable
internal fun LikesYouRoute(
    onGoToDeck: () -> Unit,
    onOpenPetDetail: (PetId) -> Unit,        // NEW
    viewModel: LikesYouViewModel = hiltViewModel(),
)
```

### 5.4 `LikesNavModule`
Wire `onOpenPetDetail = { petId -> navigator.goTo(DeckPetDetail(petId.value)) }`. `:feature:likes:presentation` already declares `implementation(project(":feature:deck:nav"))` so no Gradle changes.

### 5.5 Strings
New file: `feature/likes/presentation/src/main/res/values/strings.xml`. Currently the screen hard-codes English; this PR adds the resource file and migrates the user-visible strings.

```xml
<string name="likes_eyebrow">Incoming</string>
<string name="likes_headline">Likes you</string>
<string name="likes_subtitle">Tap any pet to like back and start a match</string>
<string name="likes_count_format">%1$d new</string>
<string name="likes_filter_all">All</string>
<string name="likes_filter_playdate">Playdate</string>
<string name="likes_filter_adoption">Adoption</string>
<string name="likes_filter_friendship">Friendship</string>
<string name="likes_intent_playdate">Playdate</string>
<string name="likes_intent_friendship">Friendship</string>
<string name="likes_intent_adoption">Adoption</string>
<string name="likes_card_new">NEW</string>
<string name="likes_empty_headline">No one yet</string>
<string name="likes_empty_body">When someone likes one of your pets, they\'ll show up here.</string>
<string name="likes_empty_cta">Go to Deck</string>
<string name="likes_filtered_empty_format">No %1$s likes yet</string>
<string name="likes_time_just_now">just now</string>
<string name="likes_time_minutes_format">%1$dm</string>
<string name="likes_time_hours_format">%1$dh</string>
<string name="likes_time_days_format">%1$dd</string>
<string name="likes_time_weeks_format">%1$dw</string>
<string name="likes_distance_under_5">&lt; 5 km</string>
<string name="likes_distance_5_15">5–15 km</string>
<string name="likes_distance_15_50">15–50 km</string>
<string name="likes_distance_over_50">50+ km</string>
```

## 6. Files to add / modify / NOT modify

### Add
- `feature/likes/presentation/src/main/res/values/strings.xml` — new strings file (the module currently has none).

### Modify
- `feature/likes/presentation/src/main/kotlin/com/rodiz/arch2/feature/likes/presentation/LikesScreen.kt` — full rewrite of the composable tree (header + filter row + redesigned cards), keep file name.
- `feature/likes/presentation/src/main/kotlin/com/rodiz/arch2/feature/likes/presentation/LikesYouViewModel.kt` — add `isLoading`, `activeFilter`, `seenKeys`, `onFilterSelected`, `markSeen`.
- `feature/likes/presentation/src/main/kotlin/com/rodiz/arch2/feature/likes/presentation/LikesNavModule.kt` — wire `onOpenPetDetail`.

### Explicitly NOT modified
- `feature/likes/domain/**` — domain model unchanged.
- `feature/likes/data/**` — repository implementation unchanged.
- `feature/likes/nav/**` — route contract unchanged.
- `core/designsystem/**`, `core/ui/**` — no new shared components or tokens.
- `feature/deck/**` — pet detail screen reused as-is.
- `app/src/main/kotlin/com/rodiz/arch2/ui/FloatingChipNavBar.kt` — bottom nav unchanged.

## 7. Critical recipes

- **`statusBarsPadding()` on the header `Column`**, not on the `Scaffold`. The screen is hosted inside `MainActivity`'s `Scaffold` which sets `contentWindowInsets = WindowInsets(0)` — so the inner content owns top inset handling. Match how `DeckScreen` does it.
- **Bottom padding for the grid:** `contentPadding = PaddingValues(bottom = 120.dp)` keeps the last row above the floating chip nav. (The nav is ~92dp visually + nav-bar inset; 120dp is comfortable.)
- **`LazyVerticalGrid` items keyed by `like.key.value`** so position-stable across emissions and filter changes.
- **Intent precedence helper** must be a `when` over the canonical order, not iteration on `Set<Intent>` (whose order is undefined):
  ```kotlin
  private fun displayIntent(intents: Set<Intent>): Intent? = when {
      Intent.PLAYDATE in intents -> Intent.PLAYDATE
      Intent.FRIENDSHIP in intents -> Intent.FRIENDSHIP
      Intent.ADOPTION in intents -> Intent.ADOPTION
      else -> null
  }
  ```
- **`relativeTimeLabel` uses `Clock.System.now()`** captured once per recomposition via `remember(like.likedAt)`. Avoid recomputing on every frame.
- **Synthetic distance must be deterministic per like** so the same card always shows the same bucket across recompositions: hash `like.key.value`. (Captured as a TODO with a `// TODO(distance):` comment pointing to D3 above.)
- **Status bar icons** stay default (dark on cream) on this screen — do NOT call the deck-detail's `LightStatusBarIconsWhileShown`. Light icons only belong on the full-bleed dark photo of the detail.
- **Empty state CTA** continues to navigate `onGoToDeck()`; the user can still reach the deck.

## 8. Verification checklist

1. Build clean:
   ```bash
   JAVA_HOME=/Users/jrodiz/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home ./gradlew :app:installDebug
   ```
2. Launch app, sign in to a test owner that has incoming likes (the previous test owners used by `deck-pet-details.md` work). Navigate to the Likes tab via the floating chip.
3. Visual checks against the mock:
   - Header reads "Incoming" eyebrow + "Likes you" bold headline + coral count pill if any new likes.
   - Sub-headline copy matches.
   - Filter row scrolls horizontally, All is dark-pill by default.
   - Cards are 2-col, rounded, full-bleed photo, dark time pill top-left, coral NEW pill top-right on unseen, intent pill bottom-left, name + age + distance on the photo.
4. Tap a card → opens `DeckPetDetail` for the anchor pet (no in-place bottom sheet).
5. Tap a filter (e.g. Playdate) → grid narrows to only cards whose `anchorPet.intents` contains Playdate. Tap All → restored.
6. Force-stop + relaunch — `seenKeys` resets, NEW pills come back (D2 acknowledged behavior).
7. Empty owner: navigate as an owner with no incoming likes → header stays, `EmptyTabState` shown.
8. Screenshot: `adb -s emulator-5556 exec-out screencap -p > /tmp/likes-after.png`; read with the Read tool to confirm against `/Users/jrodiz/Desktop/likes.png`.

## 9. Out of scope

- Real per-card distance (D3 — needs data-layer plumbing).
- Persisted "seen" tracking (D2 — needs Firestore field + write on view).
- Swipe-to-dismiss / pass on grid tiles (the parent likes plan explicitly declined this).
- Pull-to-refresh (the listener emits continuously already).
- Pagination (count is capped at 200 in the repo; v1 doesn't need pagination).
- Bulk actions (parent plan §10 explicitly declined).

## 10. Risks / rollback

- **Risk:** the synthetic distance bucket reads as misinformation if a user puzzles over it. Mitigation: stand-in only; we keep it consistent per like, and the label "< 5 km" is intentionally a bucket (not a precise number) which softens the falseness. Add a follow-up TODO at the call site to swap to real distance when `IncomingLike.distanceBucket` is plumbed.
- **Risk:** removing the in-screen bottom sheet breaks any user muscle memory for the old surface — but the surface is brand-new (parent likes plan just shipped) so muscle memory is minimal. The detail screen has the same Like/Pass actions.
- **Rollback:** the change is contained to three files in `:feature:likes:presentation` plus a new `strings.xml`. Revert the commit and the old bottom-sheet flow returns intact.

## 11. Implementation order

1. Add `feature/likes/presentation/src/main/res/values/strings.xml` with all strings from §5.5.
2. Extend `LikesYouViewModel` with `isLoading`, `activeFilter`, `seenKeys`, `onFilterSelected`, `markSeen`. Flip `isLoading` on first emission.
3. Rewrite `LikesScreen.kt`: new `LikesYouRoute` signature, new `LikesHeader`, `CountPill`, `FilterChipRow`, `FilterPill`, `LikeCard`, `RelativeTimePill`, `NewPill`, `IntentPill`, helpers (`displayIntent`, `relativeTimeLabel`, `syntheticDistanceBucket`).
4. Update `LikesNavModule` to pass `onOpenPetDetail = { petId -> navigator.goTo(DeckPetDetail(petId.value)) }`.
5. Build → `:app:installDebug`, fix any compile errors.
6. Launch + screenshot + compare to mock, iterate.
7. Single commit.

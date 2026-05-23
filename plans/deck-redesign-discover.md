# Plan — Deck / Discover screen redesign

> **Parent spec:** [`ANDROID_APP_SCAFFOLD_PROMPT.md`](../ANDROID_APP_SCAFFOLD_PROMPT.md). **Architecture rules** same as the rest of the project.
>
> **Feature plan it builds on:** [`plans/deck-swipe.md`](./deck-swipe.md). This plan does **not** change deck behavior, data layer, or domain — it's purely a presentation-layer reskin.
>
> **Bottom nav:** the floating chip nav visible in the mock is already shipped per [`plans/floating-chip-bottom-nav.md`](./floating-chip-bottom-nav.md). Do **not** modify it here.

## 1. Context

Today `:feature:deck:presentation` renders an austere "Deck" screen: a stock `TopAppBar` with a back arrow, a single full-bleed image card with a dark gradient + name overlay at the bottom, and three Material `FilledIconButton` actions (pass, rewind, like).

The mock recasts this surface as **"Discover" — a magazine-style card** with:
- A two-line headline header (`Discover` overline + `Pets near you` H4).
- A meta row above the card showing the user's active filters at a glance (radius, species categories as icon strip, intent count).
- A floating dark distance pill and a coral intent pill that sit **above** the card's photo (overlapping the top edge of the card surface).
- An owner chip ("with Maya" + avatar) anchored to the bottom-left of the photo, straddling the photo/info-pane boundary.
- A white info pane below the photo (not gradient-overlaid) with: name + inline age badge, species chip (outlined), intent chip (filled tonal), a short bio quoted in italic.
- A four-button action row: large gray Pass, small amber Rewind, small coral Boost (deferred no-op for now), large coral Like.

The floating chip bottom nav is unchanged — same coral "Deck" pill + three muted siblings.

## 2. Confirmed / inferred decisions

1. **Header icons** = filter (`Icons.Outlined.Tune`) and notifications (`Icons.Outlined.NotificationsNone`). The filter icon navigates to Settings → Filters (existing route). Notifications opens the Settings notifications screen for now. *Inferred — the mock shows them but doesn't label destinations. Tapping is non-destructive in both cases.*
2. **The "3 intents" meta** comes from `FilterPrefs.intents.size`. It always renders as `${n} intent` / `${n} intents` (no hard-coded `3`). *Confirmed by the data already on hand.*
3. **The "25 km" meta** comes from `FilterPrefs.maxDistanceKm`. Always uses km (we don't yet ship miles). *Confirmed — matches the rest of the deck spec.*
4. **The species icon strip** shows one tiny outlined icon per enabled `SpeciesCategory` (DOGS = paw, CATS = pets, SMALL_MAMMALS = pets — Compose's Material Icons set is thin here; we'll reuse `Icons.Outlined.Pets` for all three with subtle alpha differences, since the strip is decorative). *Inferred — accept that the icons are loose stand-ins; the strip's purpose is "filters are narrow".*
5. **The small chip floating at the bottom-left of the photo** shows the owner avatar + `"with ${firstName}"` text. When the owner doc hasn't loaded yet, we render a generic `with someone` placeholder. *Inferred — matches the mock and keeps the layout stable.*
6. **The boost / star button** in the action row has no behavior assigned in our spec (no "boost" or "super-like" per [`plans/deck-swipe.md`](./deck-swipe.md) §11). Render it disabled (50% alpha) so the visual matches the mock but tapping is a no-op. *Inferred — a quick visual nod to the mock without committing to a paid feature.*
7. **The age "3" badge** beside the pet name renders as a small outlined pill: `${ageYears}` (with `~` prefix when `ageIsApproximate`). *Inferred — matches typographic balance in the mock.*
8. **Card width** sits inset 16dp from the screen edges, occupying ~88% of viewport width. The photo block is roughly square (`aspectRatio(1.05f)`). The info pane below adapts to content. *Inferred from mock proportions.*
9. **Distance + intent chips** float above the photo with a small downward offset so they appear to "sit on top of" the rounded card top. *Inferred from mock.*
10. **No top app-bar** — the headline + icons are inline in the screen content (the floating chip nav is the only bottom chrome). Removing the `Scaffold(topBar=...)` lets the card breathe and matches the mock's airy top. *Inferred.*

## 3. Visual spec

### 3.1 Screen frame
- Background: `MaterialTheme.colorScheme.background` (the warm off-white already in the theme — `#FFFBFA`).
- Outer padding: `horizontal = 20.dp`, `top = 16.dp + status bar inset`, `bottom = floating-nav inset` (handled by Scaffold contentPadding which already provides bottom inset for the nav).
- Column layout, fills the viewport.

### 3.2 Header block
- Row 1: a column with two `Text`s.
  - Overline: `"Discover"`, `labelMedium`, `onSurfaceVariant`, `letterSpacing = 0.5.sp`.
  - Headline: `"Pets near you"`, `headlineSmall.copy(fontWeight = ExtraBold)`, `onSurface`.
- Row 1 right side: two `IconButton`s in a horizontal row.
  - `Tune` (filter) → invokes `onOpenFilters`.
  - `NotificationsNone` (bell) → invokes `onOpenNotifications`.
  - Both 40dp, `MaterialTheme.colorScheme.onSurface` tint.
- Spacer `12.dp`.
- Row 2 (meta strip):
  - Pin icon (`Icons.Outlined.Place`, 16dp, `onSurfaceVariant`) + `"${maxDistanceKm} km"` (labelMedium).
  - Spacer `12.dp`.
  - Species icon strip: a `Row(horizontalArrangement = spacedBy(2.dp))` of one 16dp `Icons.Outlined.Pets` per enabled `SpeciesCategory`, tinted at `0.5f` alpha to read as a single visual token (matches the mock's compact food/bone icon cluster).
  - Spacer `12.dp`.
  - Bullet `"•"` separator at `onSurfaceVariant`.
  - Spacer `12.dp`.
  - `Icons.Outlined.AutoAwesome` (16dp) + `"${n} intents"` / `"1 intent"`.

### 3.3 Floating chips row (above the card)
A `Row` with horizontal arrangement = `SpaceBetween`:
- **Distance chip (left):** dark filled pill.
  - Background `Color(0xFF1F1B1A)` (reuse `onSurface = #211B1A` from the theme — close enough).
  - Content padding `horizontal = 10.dp, vertical = 6.dp`, `RoundedCornerShape(percent = 50)`.
  - `Icons.Outlined.Place` 14dp + `" "` + distance bucket label (`card.distanceBucket?.label ?: "Nearby"`).
  - Text color `Color.White`, `labelMedium`, `FontWeight.SemiBold`.
- **Intent chip (right):** coral filled pill.
  - Background `BrandColors.Coral`, same shape/padding.
  - Text: first intent label, capitalized — `Playdate` / `Adoption` / `Friendship`.
  - Text color `Color.White`, `labelMedium`, `FontWeight.SemiBold`.

Both chips have `Modifier.offset(y = 8.dp)` so they sit at the visual top edge of the card and look like they're sitting on it (z-order via `Box` overlap or by giving the card a top padding equal to half the chip height — we use the simpler approach of stacking with `Box` and aligning the chips to `TopStart`/`TopEnd`).

Implementation: wrap the card + chips in a `Box`. Card is the base layer with `Modifier.padding(top = 16.dp)`. Chips align to `TopStart` / `TopEnd` of the Box.

### 3.4 The card

`Surface(shape = RoundedCornerShape(24.dp), shadowElevation = 6.dp, color = MaterialTheme.colorScheme.surface)` wrapping a `Column`:

#### 3.4.1 Photo block
- `Box(Modifier.fillMaxWidth().aspectRatio(1.05f))`
- Background: gray placeholder for missing photo.
- `AsyncImage` fills the box with `ContentScale.Crop`.
- Top of the photo is clipped to match the card's top corners (24dp).
- Overlaid at `BottomStart` with `Modifier.padding(start = 12.dp).offset(y = 18.dp)`: the owner chip.

#### 3.4.2 Owner chip (the floating "with Maya" pill)
- `Surface(shape = RoundedCornerShape(percent = 50), shadowElevation = 4.dp, color = Color.White)`.
- Inner row, `Modifier.padding(end = 12.dp, vertical = 4.dp).padding(start = 4.dp)`, `verticalAlignment = CenterVertically`, `horizontalArrangement = spacedBy(6.dp)`.
- 24dp `AsyncImage` clipped to a circle for the avatar. Fallback: `Icons.Outlined.Person` over `BrandColors.CoralLight` background.
- `Text("with ${owner.firstName}", labelMedium, FontWeight.SemiBold, onSurface)`.

#### 3.4.3 Info pane
- `Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 18.dp))`.
- Row 1: pet name + age badge.
  - `Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = spacedBy(8.dp))`.
  - `Text(pet.name, headlineMedium.copy(fontWeight = ExtraBold), onSurface)`.
  - `Surface(shape = RoundedCornerShape(percent = 50), color = BrandColors.CoralLight.copy(alpha = 0.35f))` containing `Text("${if (approx) "~" else ""}${ageYears}", labelLarge.copy(fontWeight = Bold), color = BrandColors.CoralDeep, padding 8/2)`.
- Spacer `8.dp`.
- Row 2: chips.
  - `Row(horizontalArrangement = spacedBy(6.dp))`.
  - Species chip (outlined): `Surface(shape = RoundedCornerShape(50%), border = 1.dp onSurfaceVariant.copy(0.4f), color = surface, padding 12/4)` + `Text(speciesLabel, labelMedium, onSurface)`.
  - Intent chip (filled tonal — friendship in mock): `Surface(shape = RoundedCornerShape(50%), color = BrandColors.CoralLight.copy(alpha = 0.25f), padding 12/4)` containing `Row(spacedBy(4.dp))` of `Icons.Outlined.AutoAwesome` 12dp tinted CoralDeep + `Text(intentLabel, labelMedium, CoralDeep, SemiBold)`.
- Spacer `12.dp`.
- Bio quote (when present): `Text("“${bio}”", bodyMedium.copy(fontStyle = Italic), color = onSurfaceVariant)`. When the pet has no bio, skip this `Text` entirely.

### 3.5 Action row
- `Spacer(20.dp)` above the row.
- `Row(Modifier.fillMaxWidth().padding(horizontal = 32.dp), horizontalArrangement = SpaceBetween, verticalAlignment = CenterVertically)`.
- Four buttons, in order:
  1. **Pass (big):** circular `Surface` 56dp, `color = Color(0xFFF2F2F2)` (soft gray), `Icons.Outlined.Close` 24dp, `onSurface` tint.
  2. **Rewind (small):** circular `Surface` 40dp, transparent background, border 1.5dp `Color(0xFFE7B486)` (warm amber), `Icons.Outlined.Replay` 18dp, tinted `Color(0xFFE7B486)`.
  3. **Boost (small, disabled):** circular `Surface` 40dp, transparent background, border 1.5dp `BrandColors.Coral.copy(alpha = 0.5f)`, `Icons.Outlined.AutoAwesome` 18dp, tint coral at 0.5 alpha. `clickable(enabled = false)`.
  4. **Like (big):** circular `Surface` 56dp, `color = BrandColors.Coral`, shadow 4dp, `Icons.Filled.Favorite` 28dp, white tint.

Bottom padding of the action row: `24.dp` so it clears the floating nav's `8 + 16dp` outer margin.

### 3.6 Loading / empty / petless states
Keep the existing visual behavior — `CircularProgressIndicator`, `DeckEmptyState`, the petless-owner banner — but render them **inside** the redesigned screen frame (i.e. still show the new header at the top so the screen doesn't visually swap shells when there are no cards).

The petless-owner banner moves directly under the header strip and uses the same rounded surface treatment as the rest of the screen (12.dp corner radius, `BrandColors.CoralLight.copy(alpha = 0.25f)` background).

## 4. Files to add / modify

### Modify
- `feature/deck/presentation/src/main/kotlin/com/rodiz/arch2/feature/deck/presentation/DeckScreen.kt` — replace `DeckRoute` body and `DeckStack`/`DeckActionBar` with the new layout. Inject `FilterPrefsRepository` flow via the ViewModel (already collects it) — see §5.
- `feature/deck/presentation/src/main/kotlin/com/rodiz/arch2/feature/deck/presentation/DeckCardView.kt` — replace the gradient-overlay rendering with the new photo + owner chip + info pane structure. Keep the drag/swipe gesture wrapper intact.
- `feature/deck/presentation/src/main/kotlin/com/rodiz/arch2/feature/deck/presentation/DeckViewModel.kt` — surface `maxDistanceKm` and `intentsCount` from the filter prefs flow on `DeckUiState` (purely additive; no behavior change).
- `feature/deck/presentation/src/main/kotlin/com/rodiz/arch2/feature/deck/presentation/DeckNavModule.kt` — pass two new lambdas: `onOpenFilters` → `navigator.goTo(SettingsHome)` (already a route), `onOpenNotifications` → `navigator.goTo(NotificationsSettings)` (existing route).
- `feature/deck/presentation/build.gradle.kts` — add `implementation(project(":feature:settings:nav"))` so the nav module can reference the routes.

### Add
- `feature/deck/presentation/src/main/res/values/strings.xml` — new file with these string resources:
  - `deck_overline` → `Discover`
  - `deck_headline` → `Pets near you`
  - `deck_filter_action` → `Filters`
  - `deck_notifications_action` → `Notifications`
  - `deck_distance_km_format` → `%1$d km`
  - `deck_intents_count_one` → `1 intent`
  - `deck_intents_count_other` → `%1$d intents`
  - `deck_owner_with_format` → `with %1$s`
  - `deck_owner_with_unknown` → `with someone`
  - `deck_action_pass` → `Pass`
  - `deck_action_rewind` → `Rewind`
  - `deck_action_boost` → `Boost`
  - `deck_action_like` → `Like`
  - `deck_bio_quote_format` → `“%1$s”`
  - `deck_distance_unknown` → `Nearby`

Use plurals where appropriate:
```xml
<plurals name="deck_intents_count">
    <item quantity="one">1 intent</item>
    <item quantity="other">%1$d intents</item>
</plurals>
```

### Do NOT modify
- `app/src/main/kotlin/com/rodiz/arch2/ui/FloatingChipNavBar.kt` — bottom nav is already correct.
- `app/src/main/kotlin/com/rodiz/arch2/MainActivity.kt` — nav wiring unchanged.
- `:feature:deck:domain` / `:feature:deck:data` — no behavioral changes.
- `:core:designsystem` — every color/shape we need already exists (`BrandColors.Coral`, `CoralDeep`, `CoralLight`, `onSurface`, `onSurfaceVariant`).

## 5. State / data wiring

`DeckViewModel` already collects `filterPrefsRepo.observePrefs()` internally. Surface those two scalar values on `DeckUiState`:

```kotlin
internal data class DeckUiState(
    val cards: List<DeckCard> = emptyList(),
    val state: DeckState = DeckState.EXHAUSTED,
    val hasOwnPet: Boolean = true,
    val maxDistanceKm: Int = 25,             // NEW — drives the header meta
    val intentsCount: Int = Intent.entries.size,  // NEW — drives the header meta
    val speciesCount: Int = SpeciesCategory.entries.size, // NEW — drives the icon strip
    val matchMessage: String? = null,
    val requiresPetMessage: String? = null,
    val errorMessage: String? = null,
)
```

In `init { ... }` extend the existing `filterPrefsRepo.observePrefs()` chain so we both fan out to the deck observation **and** keep the latest scalar values in `_uiState`. The simplest patch: split into two collectors — one as today driving `observeDeck`, a second one that just `update`s the three new fields whenever prefs change. Both subscribe to the same hot flow so no extra Firestore reads.

The species icon strip uses `speciesCount` (an integer 1..3) to render the right number of decorative paw icons; we don't need the actual `SpeciesCategory` enum values at the presentation layer.

## 6. Critical recipes

1. **Chips overlapping the card top.** Wrap the card + the two chips in a `Box` and align the chips with `Modifier.align(Alignment.TopStart)` and `Modifier.align(Alignment.TopEnd)`. Give the card `Modifier.padding(top = 18.dp)` so the chips sit on the visual top edge. **Do not** give the chips negative offsets — that can clip them outside the Box's bounds and prevent input from reaching them.

2. **Owner chip straddling the photo / info-pane boundary.** Use a `Box` over the photo, with the chip aligned to `BottomStart` and `Modifier.offset(x = 12.dp, y = 14.dp)`. The Box itself is inside the card column, *not* outside it — keeps the chip clipped by the card's rounded corners on the left. The bottom of the chip sits inside the info pane.

3. **`pointerInput` on the draggable card.** The existing drag gesture in `DeckCardView` runs on the whole card. Keep that wrapper as the root composable; just rebuild the *inside* (photo + info pane) with the new structure. Don't put `clickable` modifiers on the owner chip or info-pane chips — they'd steal the drag pointer.

4. **`PetlessOwnerBanner` rendered inside the redesigned shell.** The existing screen renders the banner *above* the deck stack inside the same Column. Keep that — just restyle the banner to use `RoundedCornerShape(16.dp)` and the `CoralLight` background so it harmonizes with the new chips.

5. **`stringResource` calls and `pluralStringResource`.** The intent count needs the plurals API:
   ```kotlin
   val intentsText = pluralStringResource(R.plurals.deck_intents_count, count = state.intentsCount, state.intentsCount)
   ```
   Don't try to do this with `%d intent(s)` — it reads as broken English in the singular.

6. **`Modifier.fillMaxSize()` inside a `Column` with a `weight(1f)`.** The card needs a fixed aspect ratio for the photo block and *content height* for the info pane — `Modifier.fillMaxWidth().wrapContentHeight()`. Don't fill height or the card stretches when there's no bio.

7. **`Settings → Filters` and `Settings → Notifications` are existing routes**, verify both route keys exist in `:feature:settings:nav` before wiring. If `NotificationsSettings` doesn't exist as a dedicated route, fall back to `SettingsHome` for the bell icon and note it as a TODO.

## 7. Verification

1. **Build:**
   ```bash
   JAVA_HOME=/Users/jrodiz/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home ./gradlew :app:installDebug
   ```
2. **JVM isolation** — no change expected, but sanity check:
   ```bash
   ./gradlew :feature:deck:nav:dependencies --configuration runtimeClasspath
   ./gradlew :feature:deck:domain:dependencies --configuration runtimeClasspath
   ```
   Both must remain free of `androidx.*` and Firebase.
3. **Emulator (`emulator-5556`):**
   - Force-stop + start the debug app.
   - Land on Deck (default for signed-in user).
   - Header: "Discover" overline, "Pets near you" headline, filter + bell icons top-right.
   - Meta row: distance + species strip + intents count visible.
   - Card has the dark distance chip top-left and coral intent chip top-right, both overlapping the card's top edge.
   - Owner chip floats over bottom-left of the photo.
   - Info pane shows name + age badge + species chip + intent chip + bio (italicized) when present.
   - Action row: gray X, amber undo, faded coral boost (no-op), coral heart.
   - Drag the card left/right — swipe still commits.
   - Tap filter icon → Settings opens.
4. **Screenshot** to `/tmp/deck-after.png`; compare visually to the mock.

## 8. Out of scope

- Photo cycling within a card (`PhotoSegmentedIndicator`) — deferred per `deck-swipe.md` §6.3 (still unimplemented).
- The expanded bottom sheet on card tap — deferred per `deck-swipe.md` §6.3.
- Boost / super-like behavior — deferred per `deck-swipe.md` §11.
- Match celebration overlay — separate plan.
- Per-species icon variety — the icon strip is a decorative stand-in until Material Icons ships distinct paw/cat icons we want to commit to.
- The deck *stack* (showing 2–3 cards peeking behind the top) — current implementation shows only the top card; this plan doesn't change that.

## 9. Risk / rollback

- **Risk:** removing the `Scaffold(topBar = ...)` changes how the system status bar interacts with the screen. Mitigation: the floating-nav `Scaffold` in `MainActivity` already provides edge-to-edge inset handling; the new top header uses `Modifier.statusBarsPadding()` to leave room for the status bar icons.
- **Risk:** the floating chips with `align(TopStart)` may overlap the card's top corners and look clipped. Mitigation: chip backgrounds are full-pill so corner clipping is harmless; verify on emulator.
- **Risk:** adding two new routes to the deck presentation module bumps its dependency on `:feature:settings:nav`. Verify that `:feature:settings:nav` exists as a pure JVM module before depending on it. (It does — it's part of the settings feature already in the codebase.)
- **Rollback:** revert the single commit. No data-layer or domain changes to undo.

## 10. Implementation order

1. Add `feature/deck/presentation/src/main/res/values/strings.xml` with all strings + the `deck_intents_count` plural.
2. Extend `DeckUiState` with `maxDistanceKm`, `intentsCount`, `speciesCount` and wire them in `DeckViewModel.init`.
3. Rewrite `DeckCardView` to render the new photo + owner chip + info pane structure (preserving the existing drag/swipe wrapper).
4. Rewrite `DeckScreen.kt` (header + meta row + chips above the card + action row), preserving the existing `LaunchedEffect`s for snackbars and the empty/petless states.
5. Update `DeckNavModule` to wire `onOpenFilters` + `onOpenNotifications` to the existing settings routes. Add `:feature:settings:nav` dependency in the build script.
6. `:app:installDebug`, force-stop + start, screenshot.
7. Iterate on spacing / chip overlaps until they match the mock; re-screenshot.
8. Single commit: `feat(deck): redesign Discover screen with magazine card layout`.

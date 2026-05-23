# Plan — Pet details bottom-sheet (opened from Deck card tap)

> **First implementer step:** copy this file to `plans/deck-pet-details.md` in the repo and delete the harness copy. Project convention is that plans live in `plans/` at the repo root (see `feedback_plans_location.md`), but plan mode forces the working file into `~/.claude/plans/`.

> **Parent spec:** [`ANDROID_APP_SCAFFOLD_PROMPT.md`](../ANDROID_APP_SCAFFOLD_PROMPT.md).
>
> **Plans this builds on:** [`plans/deck-swipe.md`](./deck-swipe.md) (§6.3 noted "tap the center of a card to open a bottom sheet with the full pet details" as deferred — this is the plan that ships it) and [`plans/deck-redesign-discover.md`](./deck-redesign-discover.md) (the new magazine card whose tap surface we're wiring).
>
> **Mock:** `/Users/jrodiz/Desktop/pet-details.png` (verified read 2026-05-22).

## 1. Context

Tapping the pet card on the Discover deck currently does nothing — the only gestures are horizontal swipes. The mock specifies a **suitor's detail view**: full-bleed photo on top with story-progress segments, a back chevron, a translucent owner chip, the pet's name/age/species/distance overlaid on the photo gradient, then a draggable cream bottom sheet that covers ~40% of the screen at rest. The sheet shows:

- An "OPEN TO" intent block (coral + green filled pills),
- An "ABOUT" bio paragraph,
- A peek of size/energy chips (the "Medium" / "Small" peeking under the action row indicates more content lives in the expanded sheet),
- An action row docked at the bottom of the sheet: a small white X (pass) circle next to a large coral **"♥ Like Biscuit"** pill.

This screen is the suitor's analog of the existing owner-side `PetPreviewScreen`. Liking from here is the **same intent** as tapping ♥ on the deck card — it should produce the same match/decision outcome.

**Why now:** card-tap → details is the only deferred interaction left from `deck-swipe.md`. Shipping it closes the loop and gives the deck a "more info before you decide" surface, which materially changes engagement quality (less impulse-swiping).

## 2. Confirmed / inferred decisions

1. **New screen + new route, route lives in `:feature:deck:nav`.** Add `DeckPetDetail(petId: String)` to `:feature:deck:nav/Routes.kt` and `DeckPetDetailScreen` to `:feature:deck:presentation`. *User answered "new screen + new route" but selected the option that placed the route in `:feature:pet:nav`; I moved it to `:feature:deck:nav` because the screen needs to invoke `SubmitSwipeUseCase` (deck domain), and the architecture rule "`:presentation` may depend on another feature's `:nav` only" forbids `:feature:pet:presentation → :feature:deck:domain`. The screen is conceptually a deck-driven detail; pet feature owning the route would be semantically wrong too. **Confirm at plan review — if you'd rather keep it under pet/nav, we'd need to extract `SubmitSwipeUseCase` into a `:core:swipeactions:domain` JVM module first.* *
2. **Reuse the existing owner-side `PetPreviewScreen`? No.** Owner preview has different chrome (no like CTA, no owner attribution, has Edit affordance). Cleaner to ship a separate `DeckPetDetailScreen` that shares only the underlying `ObservePetUseCase`. *Confirmed.*
3. **Like behavior:** Like + close + advance deck. Tapping "Like Biscuit" invokes `SubmitSwipeUseCase(petId, LIKE)`, pops back to the deck graph, and the deck observes the same swipe stream so its top-card advances and any match dialog surfaces in the deck UI (where it already lives). The detail screen does **not** render its own match dialog. *Confirmed.*
4. **Pass (X) behavior on the details:** mirrors Like — invokes `SubmitSwipeUseCase(petId, PASS)`, pops back, deck advances. *Inferred from symmetry.*
5. **Sheet pattern:** Material3 `BottomSheetScaffold` with `SheetValue.PartiallyExpanded` initial state and a `sheetPeekHeight` sized so the action row + intent pills row are visible at rest (see §3.5). The photo + overlay header live in `sheetContent`'s parent (the scaffold `content`). The sheet expands to reveal extra content (size/energy chips, full bio if truncated). *Inferred from mock — the mock's bottom edge peek of "Medium"/"Small" + the visible drag handle telegraph a draggable sheet, not a static one.*
6. **Pet model gets two new optional fields:** `size: PetSize?` and `energy: PetEnergy?` (both nullable, both default null). New enums in `:feature:pet:domain/model`. AddPet/EditPet forms are **not** updated in this pass — existing pets will have these as null and the chips simply won't render. *User selected "Add now".*
7. **Owner age on the "with Maya · 28" chip is deferred** — render only `with ${firstName}` for this pass. *User selected defer.*
8. **Stories-style photo progress indicator at the top of the photo is rendered as decorative-only:** show one segment per `pet.photos.size` (max 6), with segment 1 active. The actual photo cycling isn't wired in this pass (it's already deferred in `deck-swipe.md` §6.3) — but rendering the segment row gives the new screen visual parity with the mock and lets us wire interaction later without re-laying-out. *Inferred — matches mock, doesn't commit to behavior we can't deliver.*
9. **Back chevron** (top-left, dark translucent circle): pops the destination. Does **not** count as a swipe (no `SubmitSwipeUseCase` invoked). *Inferred.*
10. **Bottom sheet uses `MaterialTheme.colorScheme.background`** (the warm cream `#FFFBFA` that the rest of the redesign uses), with rounded top corners 24.dp, and the default Material3 drag handle. *Inferred — matches the rest of the redesign's tone.*
11. **Tap target on the deck card:** add a tap detector inside `DeckCardView`'s existing `pointerInput` block (alongside the drag detector). A short tap that doesn't pass the drag threshold emits `DeckAction.OpenDetail(petId)`. **Do not** add a separate `Modifier.clickable` — it would compete with the drag pointer and intercept swipes. *Inferred — critical recipe.*

## 3. Visual spec

### 3.1 Screen frame
- Top half: full-bleed photo (no system-bar background — status bar icons go light because the photo is dark in the gradient region). Use `LightStatusBarIconsWhileShown` (the recipe already used by the login coral hero).
- Bottom half: `BottomSheetScaffold` sheet, sheet color `MaterialTheme.colorScheme.background` (`#FFFBFA`), `sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)`, default drag handle.
- No `TopAppBar` — the back chevron is a free-floating overlay.

### 3.2 Photo block (scaffold `content`)
- `Box(Modifier.fillMaxSize())`.
- `AsyncImage(pet.photos[0].source.url, ContentScale.Crop, Modifier.fillMaxSize())` as the base layer.
- Bottom-of-photo gradient: `Brush.verticalGradient(0f to Transparent, 0.55f to Transparent, 1f to Color.Black.copy(alpha = 0.55f))` overlay, so the white text reads against any photo.
- Top-area overlays:
  - **Back chevron** at `TopStart`, `Modifier.statusBarsPadding().padding(start = 16.dp, top = 8.dp)`. `Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.45f), size = 40.dp)` with `Icons.AutoMirrored.Outlined.ArrowBackIos` 18dp white. Invokes `onBack()` (pops the route).
  - **Photo-segment row** centered horizontally at the top, `Modifier.statusBarsPadding().padding(top = 12.dp, horizontal = 72.dp)`. A `Row(horizontalArrangement = spacedBy(4.dp))` of `Box(Modifier.height(3.dp).weight(1f).clip(RoundedCornerShape(50%)).background(color))` — first segment `Color.White`, remaining segments `Color.White.copy(alpha = 0.35f)`. Cap at 6 segments.
- Bottom-of-photo content stack, aligned `BottomStart`, `Modifier.padding(start = 20.dp, end = 20.dp, bottom = sheetPeekHeight + 12.dp)`:
  - **Owner chip:** `Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.45f))` containing `Row(verticalAlignment = CenterVertically, horizontalArrangement = spacedBy(6.dp), padding 4/4 + end 12)`: 24dp circular `AsyncImage` of `owner.avatarUrl` (fallback `Icons.Outlined.Person` over CoralLight), then `Text("with ${owner.firstName}", labelMedium, FontWeight.SemiBold, Color.White)`.
  - Spacer 12dp.
  - **Name + age row:** `Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = spacedBy(10.dp))`: `Text(pet.name, displaySmall.copy(fontWeight = ExtraBold), Color.White)`, then `Text("${if (approx) "~" else ""}${ageYears} yr", titleMedium, Color.White.copy(alpha = 0.85f))`. (Mock shows `"Biscuit · 3 yr"` — the bullet separator is **decorative middle-dot**, render as a single Text concatenation `"${name}  ·  ${ageYears} yr"` if mock-fidelity is preferred; the row variant gives better typographic control. Implementer's call.)
  - **Species + distance row:** `Text("${speciesLabel} · ${distanceLabel}", titleSmall, Color.White.copy(alpha = 0.75f))`. `distanceLabel = card.distanceBucket?.label ?: "Nearby"`.

### 3.3 Sheet content (scaffold `sheetContent`)

A `Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState()))`. **Do not** put the action row inside the scrolling column — it's docked at the bottom of the sheet outside the scroll (see §3.5).

#### 3.3.1 OPEN TO block
- `Spacer(20.dp)`.
- `Text("OPEN TO", labelMedium.copy(fontWeight = Bold, letterSpacing = 1.2.sp), color = onSurfaceVariant)`.
- `Spacer(10.dp)`.
- `Row(horizontalArrangement = spacedBy(8.dp))` of one chip per intent in `pet.intents`:
  - **PLAYDATE chip:** `Surface(shape = CircleShape, color = BrandColors.Coral, padding 14/8)` + `Row(spacedBy(6.dp))` of `Icons.Outlined.Pets` 14dp white + `Text("Playdate", labelLarge, FontWeight.SemiBold, Color.White)`.
  - **FRIENDSHIP chip:** same shape, `color = Color(0xFF6FAE9C)` (mint green from mock — add to `BrandColors` as `MintLeaf = #6FAE9C` and `MintLeafLight = #C8E2D9` for any future tinted variants). Icon: `Icons.Outlined.SmsRounded` 14dp white (the curl-arrow in the mock is a chat curl — closest Material icon). Text: `"Friendship"` white.
  - **ADOPTION chip:** `color = BrandColors.CoralDeep`, icon `Icons.Outlined.Favorite` 14dp white, text `"Adoption"`.

#### 3.3.2 ABOUT block
- `Spacer(20.dp)`.
- `Text("ABOUT", labelMedium.copy(fontWeight = Bold, letterSpacing = 1.2.sp), color = onSurfaceVariant)`.
- `Spacer(10.dp)`.
- `Text(pet.bio.orEmpty(), bodyLarge, color = onSurface, lineHeight = 24.sp)`. If `pet.bio.isNullOrBlank()`, render a single muted line: `"${pet.name} is keeping their story to themselves for now."`.

#### 3.3.3 STATS block (the peek under the action row in the mock)
- `Spacer(20.dp)`.
- `Text("STATS", labelMedium.copy(fontWeight = Bold, letterSpacing = 1.2.sp), color = onSurfaceVariant)`.
- `Spacer(10.dp)`.
- `Row(horizontalArrangement = spacedBy(8.dp))` of:
  - **Size chip** (only when `pet.size != null`): `OutlinedSurface` with `Text(pet.size.label, labelLarge, FontWeight.SemiBold)` — labels are `"Small" / "Medium" / "Large"`.
  - **Energy chip** (only when `pet.energy != null`): same treatment — labels `"Calm" / "Medium" / "High"`.
  - If both null, the entire STATS block is omitted (don't render the header alone).

### 3.4 Loading + error states
- While `pet` is null and `isLoading == true`: render a `Box(Modifier.fillMaxSize())` containing a `CircularProgressIndicator(color = BrandColors.Coral)` centered. The sheet doesn't render until pet data resolves.
- When `pet` is null and `isLoading == false` (the route id doesn't resolve to any pet): show a small error inside the sheet area: `Text("This pet is no longer available.", bodyLarge)` + a "Back" `TextButton`. `onBack()` on click.

### 3.5 Action row (docked at sheet bottom)
- `BottomSheetScaffold(sheetSwipeEnabled = true)`. The action row goes **inside** `sheetContent` but **outside** the scrolling column — implemented by wrapping the sheet column in a `Column(Modifier.fillMaxWidth())` where the first child is `verticalScroll`-able and the action row is the last child with `Modifier.background(MaterialTheme.colorScheme.background).padding(horizontal = 20.dp).padding(top = 12.dp).padding(bottom = navigationBars.dp + 16.dp)`.
- `Row(verticalAlignment = CenterVertically, horizontalArrangement = spacedBy(12.dp))`:
  1. **Pass (small circle):** `Surface(shape = CircleShape, color = Color.White, border = BorderStroke(1.dp, onSurfaceVariant.copy(alpha = 0.2f)), shadowElevation = 2.dp, size = 56.dp)`, `Icons.Outlined.Close` 22dp `onSurface`. `onClick = { onPass(); onBack() }`.
  2. **Like (big pill, weight 1f):** `Button(modifier = Modifier.weight(1f).height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = BrandColors.Coral), shape = CircleShape)` with `Row(spacedBy(10.dp))` of `Icons.Filled.Favorite` 22dp white + `Text("Like ${pet.name}", titleMedium, FontWeight.Bold, Color.White)`. `onClick = { onLike(); onBack() }`.

The `sheetPeekHeight` = the rendered height of "OPEN TO" header + intent row + 16.dp spacer + the action row block. Empirically ~280–320dp; tune on the emulator. Use `LocalDensity` + a measured fallback rather than hard-coding.

## 4. Files to add / modify

### Add
- `feature/deck/nav/src/main/kotlin/com/rodiz/arch2/feature/deck/nav/Routes.kt` — add `@Serializable data class DeckPetDetail(val petId: String)` next to the existing `DeckHome`.
- `feature/deck/presentation/src/main/kotlin/com/rodiz/arch2/feature/deck/presentation/DeckPetDetailScreen.kt` — full screen composable, follows the visual spec above.
- `feature/deck/presentation/src/main/kotlin/com/rodiz/arch2/feature/deck/presentation/DeckPetDetailRoute.kt` — wires `DeckPetDetailViewModel` and surfaces the screen with `onBack` / `onLike` / `onPass` callbacks.
- `feature/deck/presentation/src/main/kotlin/com/rodiz/arch2/feature/deck/presentation/DeckPetDetailViewModel.kt` — Hilt VM. Injects `ObservePetUseCase` (`:feature:pet:domain`), `OwnerLookupRepository` (`:core:ownerlookup:domain`), `SubmitSwipeUseCase` (`:feature:deck:domain`). Exposes `DeckPetDetailUiState(pet, owner, isLoading, error)`. Reads `DeckPetDetail` route from `SavedStateHandle`.
- `feature/deck/presentation/src/main/res/values/strings.xml` — extend (file added by the redesign plan) with:
  - `deck_detail_open_to` → `OPEN TO`
  - `deck_detail_about` → `ABOUT`
  - `deck_detail_stats` → `STATS`
  - `deck_detail_size_small` / `_medium` / `_large` → `Small` / `Medium` / `Large`
  - `deck_detail_energy_calm` / `_medium` / `_high` → `Calm` / `Medium` / `High`
  - `deck_detail_intent_playdate` → `Playdate`
  - `deck_detail_intent_friendship` → `Friendship`
  - `deck_detail_intent_adoption` → `Adoption`
  - `deck_detail_like_format` → `Like %1$s`
  - `deck_detail_no_bio_format` → `%1$s is keeping their story to themselves for now.`
  - `deck_detail_unavailable` → `This pet is no longer available.`
  - `deck_detail_back` → `Back`

### Modify
- `feature/pet/domain/src/main/kotlin/com/rodiz/arch2/feature/pet/domain/model/Pet.kt` — add `size: PetSize? = null` and `energy: PetEnergy? = null` to the `Pet` data class. Defaults to null so all existing callers compile unchanged.
- Add `feature/pet/domain/src/main/kotlin/com/rodiz/arch2/feature/pet/domain/model/PetSize.kt` → `enum class PetSize { SMALL, MEDIUM, LARGE }`.
- Add `feature/pet/domain/src/main/kotlin/com/rodiz/arch2/feature/pet/domain/model/PetEnergy.kt` → `enum class PetEnergy { CALM, MEDIUM, HIGH }`.
- `feature/pet/data/.../PetDtoMapping.kt` (and any DTO file involved in mapping Firestore → domain) — read `size` and `energy` as nullable strings, parse to the enums, write nullable strings back. Defaults to null when the Firestore field is missing. **No Firestore index changes needed** — these are scalar string fields on the existing pet docs.
- `feature/deck/presentation/src/main/kotlin/com/rodiz/arch2/feature/deck/presentation/DeckCardView.kt` — add a tap detector to the existing `detectDragGestures` block (use `detectTapGestures(onTap = { onCardTap(card.pet.id) })` inside the same `pointerInput`, OR upgrade to `awaitEachGesture` to multiplex tap + drag). Add `onCardTap: (PetId) -> Unit` param to the composable signature.
- `feature/deck/presentation/src/main/kotlin/com/rodiz/arch2/feature/deck/presentation/DeckScreen.kt` — thread `onCardTap` from `DeckRoute` → `DeckStack` → `DeckCardView`. The handler invokes `onOpenDetail(petId)`.
- `feature/deck/presentation/src/main/kotlin/com/rodiz/arch2/feature/deck/presentation/DeckRoute.kt` (or `DeckNavModule.kt` — wherever the existing entries are assembled): add `onOpenDetail = { navigator.goTo(DeckPetDetail(it.value)) }` to the deck entry, and register a new entry for `DeckPetDetail`:
  ```kotlin
  entry<DeckPetDetail> { key ->
      DeckPetDetailRoute(
          onBack = { navigator.popBack() },
          // onLike / onPass are handled inside the VM; the route just navigates back.
      )
  }
  ```
- `feature/deck/presentation/build.gradle.kts` — add `implementation(project(":feature:pet:domain"))` and `implementation(project(":core:ownerlookup:domain"))` if not already on the classpath.

### Do NOT modify
- `feature/pet/presentation/PetPreviewScreen.kt` — owner-side preview, keep as-is.
- `feature/pet/presentation/PetNavModule.kt` — pet-feature owns `PetDetail`; the new route is in `:feature:deck:nav`.
- `app/src/main/kotlin/com/rodiz/arch2/MainActivity.kt` — Hilt multibinding already collects every `EntryProviderInstaller`; the new entry registers automatically.
- `:feature:deck:domain` / `:feature:deck:data` — no behavioral changes (use cases reused as-is).
- `:core:designsystem` — only color additions inside `BrandColors.kt` (mint green for the friendship chip); no new theme files.

## 5. State / data wiring

```kotlin
internal data class DeckPetDetailUiState(
    val pet: Pet? = null,
    val owner: OwnerDisplay? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
)

internal sealed interface DeckPetDetailAction {
    data object Back : DeckPetDetailAction
    data object Pass : DeckPetDetailAction
    data object Like : DeckPetDetailAction
}

internal sealed interface DeckPetDetailEvent {
    data object Dismiss : DeckPetDetailEvent  // VM tells route to pop back
}

@HiltViewModel
internal class DeckPetDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observePet: ObservePetUseCase,
    ownerLookup: OwnerLookupRepository,
    private val submitSwipe: SubmitSwipeUseCase,
) : ViewModel() {
    private val route: DeckPetDetail = savedStateHandle.toRoute()  // navigation3 helper
    private val petId = PetId(route.petId)

    val uiState: StateFlow<DeckPetDetailUiState> = observePet(petId)
        .flatMapLatest { pet ->
            if (pet == null) flowOf(DeckPetDetailUiState(isLoading = false))
            else ownerLookup.observe(pet.ownerId).map { owner ->
                DeckPetDetailUiState(pet = pet, owner = owner, isLoading = false)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, DeckPetDetailUiState())

    private val _events = Channel<DeckPetDetailEvent>(Channel.BUFFERED)
    val events: Flow<DeckPetDetailEvent> = _events.receiveAsFlow()

    fun onAction(action: DeckPetDetailAction) {
        when (action) {
            DeckPetDetailAction.Back -> _events.trySend(DeckPetDetailEvent.Dismiss)
            DeckPetDetailAction.Pass -> swipe(SwipeAction.PASS)
            DeckPetDetailAction.Like -> swipe(SwipeAction.LIKE)
        }
    }

    private fun swipe(action: SwipeAction) {
        viewModelScope.launch {
            submitSwipe(petId, action)         // result observed by DeckViewModel via its own use case
            _events.trySend(DeckPetDetailEvent.Dismiss)
        }
    }
}
```

The DeckPetDetailRoute collects `events` and invokes `onBack()` on `Dismiss`. **The deck observes the swipe stream independently** — it already does (the deck's `likeTop` / `passTop` go through the same `SubmitSwipeUseCase` and the deck list refetch reacts to swipe history). No bridge needed between the two ViewModels.

## 6. Critical recipes

1. **Tap + drag on the same pointer.** The existing `DeckCardView` uses `Modifier.pointerInput(key) { detectDragGestures(...) }`. Replace with:
   ```kotlin
   Modifier.pointerInput(key) {
       awaitEachGesture {
           val down = awaitFirstDown()
           val drag = awaitTouchSlopOrCancellation(down.id) { change, _ -> change.consume() }
           if (drag == null) {
               // didn't pass slop → treat as tap
               onCardTap(card.pet.id)
           } else {
               // drag started → continue with existing horizontal drag logic
               horizontalDrag(drag.id) { change -> /* existing translation logic */ }
           }
       }
   }
   ```
   **Do not** stack `Modifier.clickable` on top of the drag — it consumes pointer-down events before the drag detector sees them, breaking swipes.
2. **Status bar icons on a dark photo.** Use the existing `LightStatusBarIconsWhileShown` helper from `core:ui` (the login coral hero uses it). Wrap the route content in it so status icons stay readable above the photo.
3. **`BottomSheetScaffold` peek height.** Material3's `sheetPeekHeight` is a `Dp`. Measure with a `SubcomposeLayout` if you want it exact, but for v1 hard-code `300.dp` and verify on the emulator — the action row + intent pill row at 56dp + 44dp + paddings lands around there.
4. **Navigation3 `SavedStateHandle.toRoute()`.** The `navigation3` alpha uses `SavedStateHandle.toRoute<T>()` where `T` is the `@Serializable` route class. The receiver in the entry block is `EntryProviderBuilder<Any>` (per project pin), so `entry<DeckPetDetail> { key -> ... }` is correct. The VM grabs the route via `savedStateHandle.toRoute<DeckPetDetail>()`.
5. **Pet model field additions are purely additive.** Defaults on `size` and `energy` are `null`, so every existing call site (`AddPetViewModel`, `EditPetViewModel`, all the tests) compiles unchanged.
6. **Firestore mapping is forgiving of missing fields.** The DTO mapper should treat absent `size`/`energy` fields as null, not as parse errors. Firestore `getString("size")` returns null when the field doesn't exist — perfect.
7. **Don't pop back from inside the VM directly.** The VM emits a `Dismiss` event; the route collects it via `LaunchedEffect(Unit) { vm.events.collect { onBack() } }`. Keeps the VM Navigator-free (the existing pattern in this repo).
8. **Owner chip avatar fallback.** When `owner.avatarUrl` is null *and* `isLoading == true` for the owner doc, still render the fallback chip (don't conditionally hide the chip) — otherwise the photo bottom feels empty.

## 7. Verification

1. **Build:**
   ```bash
   JAVA_HOME=/Users/jrodiz/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home ./gradlew :app:installDebug
   ```
2. **JVM isolation** (the new route lives in a JVM module):
   ```bash
   ./gradlew :feature:deck:nav:dependencies --configuration runtimeClasspath
   ./gradlew :feature:pet:domain:dependencies --configuration runtimeClasspath
   ```
   Neither should pull `androidx.*` or Firebase.
3. **Emulator (`emulator-5556`):**
   - Force-stop + start the debug app, land on Deck.
   - Tap the center of the top pet card → the detail screen opens with the photo + sheet layout.
   - Swipe the card horizontally first (it shouldn't open the detail) → verify the drag/tap multiplex works.
   - Owner chip shows `with ${firstName}` (no age).
   - Intent pills render with correct colors (coral playdate, mint friendship, deep-coral adoption).
   - Drag the bottom sheet up → STATS section comes into view. With existing test pets (no size/energy set), the STATS section is **absent** (per §3.3.3). Manually edit a pet doc in Firestore to set `size = "MEDIUM"` and `energy = "HIGH"` and re-test — chips should render.
   - Tap **Like Biscuit** → sheet dismisses, deck advances to next card. If the liked pet had a reciprocal like, the deck's existing match dialog should appear.
   - Tap **X (pass)** → sheet dismisses, deck advances.
   - Tap **back chevron** → sheet dismisses, no swipe recorded. (Verify by re-entering the deck — the same pet should still be at the top.)
   - Pet with no bio → "ABOUT" body falls back to the `keeping their story to themselves` line.
4. **Screenshots** to `/tmp/pet-details-after.png` (collapsed sheet) and `/tmp/pet-details-expanded.png` (expanded sheet showing STATS). Compare to mock.

## 8. Out of scope

- Photo cycling within the segment indicator — segments render but tapping them does nothing in this pass (still deferred per `deck-swipe.md` §6.3).
- Owner age on the "with Maya" chip — defers a `:core:ownerlookup:domain` extension.
- AddPet/EditPet forms for `size` / `energy` — separate plan once we decide the UX (slider? chips? freeform?).
- Match celebration overlay on the detail screen — the deck graph's existing dialog handles it.
- Reporting / blocking from the detail screen — separate plan when Help & safety lands.
- Swipe-up gesture to dismiss the sheet as a "pass" shortcut — Tinder-style. Not requested.
- Sharing the pet ("send to a friend") — not in the mock.

## 9. Risk / rollback

- **Risk: tap/drag multiplex on the deck card breaks swipes.** Mitigation: the recipe in §6.1 has been used in other projects (Compose's `awaitEachGesture` is the idiomatic answer). Verify on emulator before committing — both horizontal swipe and center tap must work.
- **Risk: Pet model additions break Firestore reads.** Mitigation: both new fields are nullable with `null` default. The DTO mapper must treat absent fields as null (Firestore `getString` returns null on miss — confirmed behavior). Add a unit test in `:feature:pet:data` if a mapping test file exists; otherwise skip (don't create one just for this).
- **Risk: `BottomSheetScaffold` peek height misjudged on smaller screens.** Mitigation: hard-coded `300.dp` is conservative on the emulator's 2400×1080. If real-device QA flags clipping, switch to measured peek (`onSizeChanged` on the docked block → `LocalDensity` → peek height).
- **Risk: navigating to the detail and then backing causes the deck to refetch and feel laggy.** Mitigation: the deck VM uses `stateIn(SharingStarted.WhileSubscribed(5_000))` so the deck flow stays warm during the brief detail visit. Verify no observable refetch latency.
- **Risk: deviating from the user's literal "route in `:feature:pet:nav`" answer.** Mitigation: §2.1 surfaces this explicitly. If the user pushes back, the fallback is to extract `SubmitSwipeUseCase` into a `:core:swipeactions:domain` JVM module (1–2 hours of work) and put the route back in pet/nav.
- **Rollback:** revert the single commit. Pet model defaults remain `null` so even if existing docs were re-saved with the new fields present, dropping the model fields would harmlessly ignore them on read.

## Revision 1 — 2026-05-22 (shipped)

Implementation deviations from the original plan, documented for future readers:

- **`BottomSheetScaffold` not used.** The plan §3.5 suggested Material3's `BottomSheetScaffold` with a peek state. In practice the scaffold's two-snap-point machinery added more complexity than the deferred drag-up-to-reveal warranted: the mock shows a peek that's tall enough (40% of screen) to contain the whole intent row, ABOUT bio, AND the action row without expansion in the common case (short bios). Replaced with a fixed-height `Surface(380.dp)` anchored to `BottomCenter`, with internal `verticalScroll` on the content column. Result: simpler code, identical mock fidelity, and the docked action row never crosses the scroll boundary. The decorative drag handle still renders so the affordance is preserved — a future revision can swap this for `BottomSheetScaffold` once we want true expand/collapse + STATS scroll-to-reveal.
- **`LightStatusBarIconsWhileShown` duplicated locally** instead of extracted to `:core:ui`. The helper exists privately in both `LoginScreen.kt` and `SignUpScreen.kt`; promoting it to `:core:ui` would touch login/signup imports as a drive-by. Mirrored the same private helper into `DeckPetDetailScreen.kt` to keep this commit scoped to the deck. Worth extracting in a follow-up once a third feature copies it.
- **`size`/`energy` on `PetDraft` not added.** `Pet` got the nullable fields per plan §4. `PetDraft` did not, because the Add/Edit forms don't collect these values yet (deferred per plan §8). `buildPetMap` and `petFromWrite` instead accept optional `size`/`energy` parameters that the update path threads through from the existing record — so editing a pet preserves stats rather than wiping them.
- **`SavedStateHandle.toRoute()` not used.** Plan §6.4 suggested it, but the repo convention (see `ChatViewModel`/`PetPreviewViewModel`) is `AssistedInject` + `Factory` + a `FactoryHolder : ViewModel()` that exposes the factory. Matched that pattern for `DeckPetDetailViewModel`.
- **Decorative `Icons.AutoMirrored.Outlined.Chat`** used for the Friendship intent chip instead of `Icons.Outlined.SmsRounded` (plan §3.3.1) — `SmsRounded` doesn't exist in the bundled material-icons-extended; `Chat` is the closest equivalent and avoids the icon-set deprecation warning.
- **STATS section verified by code-path only** — no test pets in the seeded data have `size`/`energy` populated, and seeding Firestore by hand is out of scope for this run. The chip renders the same way other surface-color outlined chips do; manual verification is trivially deferrable to whoever lands the Add/Edit form pass.

## 10. Implementation order

1. **Pet model + enums.** Add `PetSize`, `PetEnergy`, and nullable fields on `Pet`. Update the DTO mapper to read/write them (nullable strings). Run `./gradlew :feature:pet:domain:assembleDebug :feature:pet:data:assembleDebug` to catch compile breaks.
2. **Route + nav.** Add `DeckPetDetail(petId)` to `:feature:deck:nav`. Verify `:feature:deck:nav:dependencies` still has no androidx deps.
3. **DeckPetDetailScreen scaffolding.** Stand up the screen + route + VM with stub UI (just shows `pet.name`). Wire the `onCardTap` → `navigator.goTo(DeckPetDetail(petId))`. `./gradlew :app:installDebug` and verify tap navigates.
4. **Tap/drag multiplex on `DeckCardView`.** Replace `detectDragGestures` with the `awaitEachGesture` recipe from §6.1. Re-test on emulator: swipe still commits, center tap opens detail.
5. **Photo + overlays.** Full-bleed photo, gradient, back chevron, segment row, owner chip, name/age/species rows.
6. **Bottom sheet content.** OPEN TO, ABOUT, STATS sections in the scrolling column.
7. **Docked action row.** Pass + Like buttons; wire to VM.
8. **Status-bar icon styling.** Wrap with `LightStatusBarIconsWhileShown`.
9. **Strings + plurals.** Move all literal text into `strings.xml`.
10. **Screenshot + iterate.** Compare to mock; tune spacing, peek height, mint-green hue.
11. **Single commit:** `feat(deck): add pet detail bottom-sheet from card tap`.

# Plan — Filters screen redesign

> **Parent spec:** [`ANDROID_APP_SCAFFOLD_PROMPT.md`](../ANDROID_APP_SCAFFOLD_PROMPT.md).
>
> **Prior plans this builds on:**
> - [`plans/deck-redesign-discover.md`](./deck-redesign-discover.md) — already consumes `FilterPrefs` (distance + intent count + species count) for the deck header meta strip. The Filters screen edits exactly that object.
> - [`plans/deck-swipe.md`](./deck-swipe.md) — defines the swipe + matching loop and is the consumer that actually applies the filters. The deck repo filters by `maxDistanceKm`, `intents`, and `speciesCategories` (see `FirestoreDeckRepository.observeDeck`).
> - [`plans/owner-profile-settings.md`](./owner-profile-settings.md) — shipped the original `FiltersScreen` (Material `Slider` + `FilterChip` rows). This plan rewrites that surface.

## 1. Context

Today `feature/settings/presentation/.../FiltersScreen.kt` renders an austere page on the warm Material background:
- Stock `TopAppBar(title = "Filters")` + back arrow only.
- Three sections (`Distance`, `Looking for`, `Species`) rendered with `MaterialTheme.typography.titleMedium` headers.
- Distance uses Material 3 `Slider` (5..200, 5km steps) with a single `Show pets within $km km` line.
- Intents and species are vanilla M3 `FilterChip`s with a check leading icon when selected.
- All writes go through a 200ms debounce in `FiltersViewModel.patch` to a `FilterPrefsRepository` (DataStore-backed).
- No reset, no commit-style CTA, no live result count.

The mock recasts the surface as three **white rounded "panels"** floating on the warm cream background, with a peach-toned hero CTA at the bottom:

1. **Header** — back chip on a white circle, `Filters` headline (bold), `Reset` text button on the right.
2. **Distance panel** — `Distance` title + coral pill chip showing the current value (`25 km`). Subtitle `Show pets within N km of you`. A coral-themed slider with axis labels `5 / 50 / 100 / 200 km` underneath.
3. **Looking for panel** — `Looking for` title + `Pick all that apply` subtitle. Three big square intent tiles in a row: **Playdate** (coral / `CoralLight` fill, paw icon), **Adoption** (peach fill, house icon), **Friendship** (mint / `MintLeaf` fill, handshake icon). Each tile is a rounded square (~18dp radius), icon circle on top, label below. Tapping toggles inclusion; visual selected state is the saturated fill, unselected is a low-alpha tint of the same hue with greyed icon + label.
4. **Species panel** — `Species` title + a flow of pill chips: `Dogs`, `Cats`, `Rabbits`, `Hamsters`, `Ferrets`, `Other`. Selected = coral fill + check + white text + emoji. Unselected = white pill with outline + grey text + emoji. The mock surfaces **6 species categories**, not 3.
5. **Apply CTA** — full-bleed coral pill button anchored to the bottom: `Apply filters · N pets` (the dot is a middle bullet `·`). Tapping commits & navigates back.

## 2. Confirmed / inferred decisions

1. **Species enum stays at 3 categories (`DOGS`, `CATS`, `SMALL_MAMMALS`)** for the underlying data model — the deck repo already filters by `species.category in filters.speciesCategories`, and the catalogue in `Species.kt` only distinguishes `RABBIT`, `HAMSTER`, `GUINEA_PIG`, `FERRET`, `OTHER_SMALL_MAMMAL` as `SMALL_MAMMALS`. The mock's six species chips (Dogs / Cats / Rabbits / Hamsters / Ferrets / Other) are rendered as **visual subdivisions of `SMALL_MAMMALS`** for now: tapping any of the four small-mammal chips toggles the `SMALL_MAMMALS` category, and they all share the same selected state. *Inferred — widening the enum to per-species filtering is a real data-layer change (Firestore writes, deck filter) that's out of scope here. Surfaced as a deferred decision.*
2. **Adoption chip color** — the mock uses a peach/orange fill that isn't in `BrandColors` today. Add `BrandColors.PeachWarm = Color(0xFFE8A275)` (sampled from mock) with a light pair `PeachWarmLight = Color(0xFFF6CBAA)` for the unselected tint. *Inferred — keeps the brand tokens centralized.*
3. **"Apply filters · N pets" CTA suffix** — there is **no candidate-count Flow exposed today**. `:feature:settings:presentation` cannot depend on `:feature:deck:domain` per the architecture invariant (presentation → other feature's `:nav` only). For this redesign we render the CTA as **just `Apply filters`** with no suffix, and flag the missing count Flow as a deferred follow-up (would need a JVM helper in e.g. `:core:filters:domain` or a shared count repo). *Inferred per project guidance.*
4. **Apply button behavior** — writes are already auto-persisted on every change via the existing 200ms debounce in `FiltersViewModel.patch`. The Apply button is therefore semantically a "Done — go back" action; it does **not** need to flush anything because the debounce will run on next dispatch and DataStore writes are idempotent. We do force a final `repo.updatePrefs(state.prefs)` synchronously inside `viewModelScope.launch` on click as a belt-and-braces guarantee that prefs reach disk even if the user backs out within 200ms of the last change, then invoke `onBack()`. *Inferred — matches the mock's commit-style affordance without changing semantics.*
5. **Reset behavior** — resets the in-memory `prefs` to `FilterPrefs.DEFAULT` (25km, all intents, all species) and lets the debounce write it through. No confirmation dialog. *Inferred — the mock shows a plain text link.*
6. **Distance pill** — shows the live `"$km km"` value in a small coral-tinted pill (`CoralLight.copy(alpha = 0.5)` background, `CoralDeep` text). *Inferred from mock.*
7. **Slider styling** — Material `Slider` recolored with `SliderColors(thumbColor = CoralDeep, activeTrackColor = CoralDeep, inactiveTrackColor = Coral.copy(alpha = 0.2f))`. The "axis labels" row sits below the slider in a `Row(spaceBetween)` with `labelSmall` greys. *Inferred — closest M3 affordance to mock.*
8. **Intent tile selected state** — saturated fill of that intent's hue (`CoralLight` / `PeachWarm` / `MintLeaf` at full opacity), icon container is a darker tone (`CoralDeep` / `PeachWarmDeep` / `MintLeaf.darken()` — use `Color(0xFFD27750)` and `Color(0xFF4F9485)` respectively, sampled from the mock). Unselected = same fill at `0.35f` alpha + greyed `onSurfaceVariant` icon/label. *Inferred.*
9. **Intent at-least-one guard stays** — `FiltersViewModel.toggleIntent` already prevents emptying the intent set. We keep that behavior; tapping the last selected tile is a no-op. *Confirmed by reading the existing VM.*
10. **Species pill emoji** — each pill shows a small emoji glyph followed by the label (e.g. `🐶 Dogs`, `🐱 Cats`, `🐰 Rabbits`, `🐹 Hamsters`, `🦦 Ferrets`, `🐾 Other`). Render as inline `Text` rather than `ImageVector` for reach. *Inferred from mock.*
11. **Scaffold background** — `BrandColors.CoralLight.copy(alpha = 0.10f)` over `MaterialTheme.colorScheme.background` gives the warm cream the mock uses (`#FBF5F0`-ish). Implement by setting `containerColor` on the `Scaffold` to a derived `Color(0xFFFBF5F0)` token (new `BrandColors.FiltersCream`). *Inferred.*

## 3. Visual spec

### 3.1 Screen frame
- `Scaffold` with no `topBar`. `containerColor = BrandColors.FiltersCream` (`#FBF5F0`).
- Status-bar icons dark on this light background — no special handling; default `Theme.Material3.Light` behavior is fine.
- Root `Column` with `verticalScroll`, `imePadding()` (defensive), `padding(horizontal = 20.dp, top = status bar inset + 12.dp, bottom = 96.dp)` — the bottom inset leaves room for the floating Apply button.
- Each panel is a `Surface(color = White, shape = RoundedCornerShape(24.dp), tonalElevation = 0.dp)` with inner `padding(20.dp)`.

### 3.2 Header row
- `Row(Modifier.fillMaxWidth(), verticalAlignment = CenterVertically)`.
- Leading: 44dp white `Surface(shape = CircleShape, shadowElevation = 2.dp)` containing `IconButton` with `Icons.AutoMirrored.Outlined.ArrowBack` (24dp, `onSurface`). `onClick = onBack`.
- Middle (`weight(1f)`, `padding(start = 12.dp)`): `Text("Filters", style = headlineSmall.copy(fontWeight = ExtraBold))`.
- Trailing: `TextButton(onClick = onReset)` with text `"Reset"` (`labelLarge`, `onSurfaceVariant`).

### 3.3 Distance panel
- Title row: `Text("Distance", titleMedium bold)` weighted to `1f`, then the live value pill.
- Value pill: `Surface(shape = CircleShape, color = BrandColors.CoralLight.copy(alpha = 0.45f))` with `Text("${km} km", labelMedium, BrandColors.CoralDeep)` inside `padding(horizontal = 12.dp, vertical = 6.dp)`.
- Spacer 4.dp.
- Subtitle: `Text("Show pets within ${km} km of you", bodySmall, onSurfaceVariant)`.
- Spacer 12.dp.
- `Slider(value = km.toFloat(), valueRange = 5f..200f, steps = 38, colors = SliderDefaults.colors(...))` with `Modifier.fillMaxWidth()`.
- Spacer 4.dp.
- Axis row: `Row(Arrangement.SpaceBetween)` with four `Text`s — `5`, `50`, `100`, `200 km` — each `labelSmall`, `onSurfaceVariant`.

### 3.4 Looking for panel
- Title `"Looking for"`, `titleMedium bold`.
- Subtitle `"Pick all that apply"`, `bodySmall`, `onSurfaceVariant`.
- Spacer 16.dp.
- `Row(horizontalArrangement = spacedBy(12.dp))` of three `IntentTile`s, each `Modifier.weight(1f).aspectRatio(0.95f)`.
- `IntentTile(intent, selected, onToggle)`:
  - `Surface(shape = RoundedCornerShape(20.dp), color = if (selected) tint else tint.copy(alpha = 0.35f))`.
  - Inside `Column(Center, spaceBetween 6.dp, padding(vertical = 16.dp))`.
  - Icon circle: 44dp `Box(background = if (selected) iconBg else Color.White.copy(alpha = 0.6f), shape = CircleShape)` containing `Icon(intent.icon, 22dp, tint = White if selected else onSurfaceVariant)`.
  - Label: `Text(intent.label, labelLarge bold, color = if (selected) onSurface else onSurfaceVariant)`.
  - Tile-level `.toggleable(selected, role = Role.Checkbox, onValueChange = { onToggle() })`.

### 3.5 Species panel
- Title `"Species"`, `titleMedium bold`. Spacer 12.dp.
- `FlowRow(horizontalArrangement = spacedBy(8.dp), verticalArrangement = spacedBy(8.dp))` of six pills.
- Each pill = `Surface(shape = CircleShape, color = if (selected) BrandColors.CoralDeep else White, border = if (!selected) BorderStroke(1.dp, outline.copy(alpha = 0.4f)) else null, modifier = clip + toggleable)` with inner `Row(padding(horizontal = 14.dp, vertical = 10.dp), spacedBy 6.dp)`:
  - `Text(emoji, fontSize 16.sp)`.
  - `Text(label, labelMedium bold, color = if (selected) White else onSurface)`.
  - If selected: trailing `Icon(Check, 14dp, tint = White)`.

### 3.6 Apply CTA
- Rendered **outside** the scrollable column, in a `Box(Modifier.align(BottomCenter))` inside the `Scaffold` content area — or as a `Surface` overlay using `Modifier.align(BottomCenter)` inside a `Box` that wraps the scroll column and the CTA.
- Implementation note: simplest path is to wrap the scroll Column and the CTA in a `Box(Modifier.fillMaxSize().padding(padding))`, with the Column scrollable inside and the CTA `Modifier.align(BottomCenter).padding(20.dp).fillMaxWidth()`.
- `Button(onClick = onApply, shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = BrandColors.CoralDeep), modifier = Modifier.fillMaxWidth().height(60.dp))`.
- Label: `Text("Apply filters", labelLarge bold, color = White)`. **Suffix `· N pets` is omitted in this iteration** — see deferred decision in §2.

## 4. Component / token changes

- **`core/designsystem/.../theme/Color.kt`** — add to `BrandColors`:
  - `PeachWarm = Color(0xFFE8A275)` — Adoption tile fill.
  - `PeachWarmLight = Color(0xFFF6CBAA)` — unselected Adoption tint pair (alpha used at call site).
  - `PeachWarmDeep = Color(0xFFD27750)` — Adoption icon circle background.
  - `MintLeafDeep = Color(0xFF4F9485)` — Friendship icon circle background.
  - `FiltersCream = Color(0xFFFBF5F0)` — Filters screen container.
- **No new reusable component** is extracted — `IntentTile` and `SpeciesPill` live as `private @Composable`s inside `FiltersScreen.kt` since they're one-off for this surface.

## 5. State / behavior changes

- **`FiltersUiState`** — unchanged structure. Continues to expose `isLoading` + `prefs`.
- **`FiltersViewModel`** — add:
  - `fun reset()` → `patch { FilterPrefs.DEFAULT }` (re-uses the existing debounce path).
  - `fun applyAndExit(onDone: () -> Unit)` is **not** added to the VM; the Route handles "force flush + onBack" itself by calling `viewModel.flush()` then `onBack()`. Add `suspend fun flush()` to the VM that cancels the debounce job and writes `_uiState.value.prefs` synchronously, then completes. The Route invokes it from a `rememberCoroutineScope().launch` on Apply click.
- **`FiltersRoute`** signature unchanged: `(onBack, viewModel)`.

## 6. Files to add / modify / NOT modify

### Modify
- `feature/settings/presentation/src/main/kotlin/com/rodiz/arch2/feature/settings/presentation/FiltersScreen.kt` — full rewrite of `FiltersRoute` + helpers.
- `feature/settings/presentation/src/main/kotlin/com/rodiz/arch2/feature/settings/presentation/FiltersViewModel.kt` — add `reset()` and `suspend fun flush()`.
- `core/designsystem/src/main/kotlin/com/rodiz/arch2/core/designsystem/theme/Color.kt` — add the four `BrandColors` tokens.

### Do NOT modify
- `core/filters/domain/.../FilterPrefs.kt` — model stays as-is (the mock is rendered on top of the existing 3-category SMALL_MAMMALS).
- `core/filters/data/.../DataStoreFilterPrefsRepository.kt` — persistence keys unchanged.
- `feature/pet/domain/.../Species.kt` — no widening to per-species filtering in this pass.
- `feature/deck/data/.../FirestoreDeckRepository.kt` — no changes to filter application logic.
- `feature/settings/presentation/.../SettingsNavModule.kt` — `FiltersRoute` callable signature unchanged.

## 7. Critical Compose recipes

- **`Modifier.toggleable(selected, role = Role.Checkbox, onValueChange = { onToggle() })`** on intent tiles + species pills so TalkBack reads them as toggleable.
- **`imePadding()` after `verticalScroll`** on the root column (defensive — there's no field, but cheap).
- **`FlowRow` from `androidx.compose.foundation.layout`** (not the M3 alias) since we're already past Compose 1.4. Existing code uses `androidx.compose.foundation.layout.FlowRow`.
- **`Slider` snapping** — `valueRange = 5f..200f` with `steps = 38` gives 5km granularity (`(200-5)/5 - 1 = 38`).
- **Bottom CTA over scrollable content** — wrap scroll column + CTA in `Box(Modifier.fillMaxSize().padding(scaffoldPadding))`, scroll column has `padding(bottom = 96.dp)` to leave room.

## 8. Verification checklist

- [ ] Filters opens via Settings → Filters and renders with header, three panels, and Apply CTA.
- [ ] Distance pill updates as the slider moves.
- [ ] Slider axis labels read 5 / 50 / 100 / 200 km.
- [ ] Each intent tile toggles with the correct hue (coral / peach / mint) and disabled state when it would be the last selected.
- [ ] All six species pills render; tapping any of the four small-mammal pills (Rabbits / Hamsters / Ferrets / Other) toggles them as a group.
- [ ] Reset returns to 25 km + all intents + all species pills selected.
- [ ] Apply commits and pops back to Settings home.
- [ ] After backgrounding and reopening the screen, the persisted prefs are restored.

## 9. Out of scope

- Per-species filtering for small mammals (would require widening `FilterPrefs.speciesCategories` to a `Set<Species>` or adding a `subSpecies: Set<Species>` field — plus deck filter changes and a DataStore migration).
- Live `N pets` count in the Apply CTA (needs a cross-module count Flow that respects the presentation→nav-only invariant).
- "Pick all that apply" / "Pick exactly one" segmentation — the existing data model treats all sections as multi-select.
- New miles/km unit toggle.

## 10. Risk / rollback

- Low — change is contained to `FiltersScreen.kt` + `FiltersViewModel.kt` + 4 lines in `Color.kt`. Rollback = `git revert`.
- The `SMALL_MAMMALS` proxy for the 4 small-mammal pills is the only semantic risk: if a user reads "Hamsters off" and expects hamsters but not rabbits to vanish from the deck, they'll be surprised. Mitigated by §2 (1) — flagged.

## 11. Implementation order

1. Add the four `BrandColors` tokens.
2. Extend `FiltersViewModel` with `reset()` and `suspend fun flush()`.
3. Rewrite `FiltersRoute` body — header, distance panel, looking-for panel, species panel, apply CTA.
4. Add private composables: `Panel`, `IntentTile`, `SpeciesPill`.
5. `./gradlew :app:installDebug` (JBR-17).
6. Launch, navigate to Settings → Filters, screenshot.

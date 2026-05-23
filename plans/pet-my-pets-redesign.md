# Plan: My Pets screen redesign

> **Parent spec:** [`plans/pet-profile.md`](./pet-profile.md). **Architecture rules:** [`ANDROID_APP_SCAFFOLD_PROMPT.md`](../ANDROID_APP_SCAFFOLD_PROMPT.md).

## 1. Context

The existing `MyPetsScreen.kt` (in `:feature:pet:presentation`) uses a stock Material 3 `TopAppBar` + `LazyVerticalGrid` of `PetThumbnailCard` items. It's functional but visually generic: small thumbnails, no brand presence, no surfacing of intents/status, archived block is an OutlinedButton expander.

The mock reskins the screen into a warm, on-brand pet management surface:

- A floating header (no surface) with a white rounded-square back button, big "My Pets" title, and a coral rounded-square "+" action.
- A coral "active pets" banner that doubles as a quota meter (up to 5 per account).
- A 2-column grid of richer pet cards (status pill, edit overlay, name + age/species subtitle, intent chips).
- A dashed empty "Add another pet" tile occupying the next free grid slot, until the user has 5 pets.

Non-goals: any change to add/edit/preview flows, domain model, repositories, navigation contract.

## 2. Confirmed decisions / inferred from mock

- **"3 yr · Corgi"** in the mock implies a `breed` field. The pet domain has no `breed` (see `pet-profile.md` §10 — explicitly deferred). The subtitle becomes **`"{age} yr · {speciesLabel}"`** to match what the model knows. When `ageIsApproximate` is true, render `~3 yr`. Deferred: revisit if/when the breed field lands.
- **"ACTIVE" status pill** maps to `Pet.enabled == true` (the owner-controlled visibility toggle from the domain). When `enabled == false`, the pill flips to **"PAUSED"** in muted gray so the affordance remains. `PetState.ARCHIVED` rows already render in a separate archived band beneath the active grid (existing behavior preserved, restyled).
- **Tags / intents:** the existing `Intent` enum (PLAYDATE / ADOPTION / FRIENDSHIP) maps 1:1 to the three chip styles in the mock (coral filled / sage outlined / peach filled). No new domain values.
- **Quota = 5 pets per account:** the mock says "Up to 5 per account." There is no hard server enforcement of this today, but we surface the limit in the banner and hide the "Add another" tile + fade the header "+" once `activePets.size >= MAX_ACTIVE_PETS` (constant in presentation). Server-side enforcement is out of scope.
- **Plurals:** "1 active pet" vs "N active pets". Android plurals don't exist in this module yet; we add a `plurals` resource entry in `:feature:pet:presentation`'s new `strings.xml`.
- **Empty state** (no active pets): keep the dedicated `EmptyMyPets` view (large paw icon + "Add your first pet" CTA) — restyled to the coral palette so it doesn't feel orphaned. The dashed "Add another pet" tile only renders inside the populated-grid layout.

## 3. Visual spec

### 3.1 Background

- Whole screen sits on `MaterialTheme.colorScheme.surface` (`#FFFBFA`). No top app bar; no surface color overlays.

### 3.2 Header (floating, 16.dp horizontal pad, 12.dp top after status bar inset, ~64.dp content)

Row with `Arrangement.SpaceBetween`, `verticalAlignment = CenterVertically`:

- **Back button** — 44.dp square `Surface` with `RoundedCornerShape(14.dp)`, `color = Color.White`, `shadowElevation = 1.dp`. Centered `Icons.AutoMirrored.Outlined.ArrowBack`, 20.dp, `tint = onSurface`. `Modifier.clickable` for tap.
- **Title row** (`Modifier.weight(1f)`, padded 12.dp start): `"My Pets"` — `MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold)`, color `onSurface`. Single line.
- **Add button** — 44.dp square `Surface`, `RoundedCornerShape(14.dp)`, `color = BrandColors.CoralDeep` (or `.copy(alpha = 0.45f)` when at quota), `shadowElevation = 2.dp`. Centered `Icons.Outlined.Add`, 24.dp, white. Disabled (no ripple) when at quota.

### 3.3 Active-count banner (16.dp side margin, 12.dp top spacing)

`Surface` — `RoundedCornerShape(20.dp)`, `color = BrandColors.Coral.copy(alpha = 0.14f)`, no border, no elevation.

Inner `Row` (padding 16.dp horizontal, 14.dp vertical, `spacedBy(14.dp)`):

- Decorative paw cluster: a `Box` (40.dp), background `BrandColors.Coral.copy(alpha = 0.25f)`, `CircleShape`, containing `Icons.Outlined.Pets`, 22.dp, tint `BrandColors.CoralDeep`. Single paw — keeps the visual budget small and works at all densities.
- `Column(verticalArrangement = spacedBy(2.dp))`:
  - Title: `quantityString(R.plurals.pet_active_count, n, n)` → e.g. "2 active pets" — `titleMedium.copy(fontWeight = ExtraBold)`, color `BrandColors.CoralDeep`.
  - Subtitle: when below quota → `"More pets = more matches. Up to $MAX per account."` When at quota → `"You've reached the $MAX-pet limit."`. `bodySmall`, color `BrandColors.CoralDeep.copy(alpha = 0.85f)`.

### 3.4 Pet card (2-column grid cell, `aspectRatio = 0.72f` approx so card fits photo + chip area)

`Surface` — `RoundedCornerShape(20.dp)`, `color = Color.White`, `shadowElevation = 1.dp`, `Modifier.clickable { onOpenPet(pet.id) }`.

Internal `Column`:

1. **Photo Box** — `fillMaxWidth().aspectRatio(1f)`, clipped to top corners 20.dp, background `surfaceVariant`. Loads via `AsyncImage(contentScale = Crop)`. When pet has no photo, falls back to the existing paw icon. When `pet.enabled = false`, photo is rendered at `alpha = 0.45f`.
   - **Top-left overlay (10.dp inset):** status pill — `Surface(shape = RoundedCornerShape(50), color = activeColor, shadowElevation = 0.dp)`. Active: `Color(0xFF7CC384)` with white text. Paused (`!pet.enabled`): `Color(0xFF9E9E9E)` with white. Inner padding 8.dp horizontal, 4.dp vertical. Text uppercase, `labelSmall.copy(fontWeight = Bold, letterSpacing = 0.6.sp)`.
   - **Top-right overlay (10.dp inset):** `Box(30.dp, CircleShape, color = Color.Black.copy(alpha = 0.35f))` with `Icons.Outlined.Edit`, 16.dp, white. `Modifier.clickable` → same destination as card click (route to `EditPet`).
2. **Body Column** (`padding(horizontal = 14.dp, vertical = 12.dp)`, `spacedBy(6.dp)`):
   - Name: `titleMedium.copy(fontWeight = ExtraBold)`, `onSurface`, single-line, ellipsis.
   - Subtitle: `"${pet.ageYears} yr · ${pet.species.label()}"` (prefix `~` to age when approximate). `bodySmall`, `onSurfaceVariant`.
   - **Intent chips** in a `FlowRow(horizontalArrangement = spacedBy(6.dp), verticalArrangement = spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp))`. One chip per `Intent` in `pet.intents`, styled per §3.5.

### 3.5 Intent chip styling (used by pet cards)

Composable `IntentChip(intent: Intent)`:

| Intent | Fill | Text color | Border | Leading icon |
|---|---|---|---|---|
| PLAYDATE | `BrandColors.Coral.copy(alpha = 0.22f)` | `BrandColors.CoralDeep` | none | `Icons.Outlined.Pets` (14.dp) |
| FRIENDSHIP | `Color.Transparent` | `Color(0xFF5B9B66)` (deep sage) | 1.dp `Color(0xFF7CC384)` | `Icons.Outlined.Favorite` (14.dp) |
| ADOPTION | `BrandColors.CoralLight.copy(alpha = 0.42f)` | `Color(0xFF8B5E3C)` (warm brown) | none | `Icons.Outlined.Home` (14.dp) |

Container: `Surface(shape = RoundedCornerShape(50), color = fill, border = optional BorderStroke)`. Inner `Row(verticalAlignment = CenterVertically, horizontalArrangement = spacedBy(4.dp), padding = horizontal 10.dp vertical 5.dp)`. Text `labelMedium.copy(fontWeight = SemiBold)`.

### 3.6 Add-another-pet tile (one grid cell, same `aspectRatio` as a pet card)

- Outer `Box(Modifier.clickable { onAddPet() })`. Background transparent.
- Dashed border drawn in a `Modifier.drawBehind` block: `Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())))`, color `MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)`. Rounded corners 20.dp (`drawRoundRect`).
- Centered `Column(horizontalAlignment = CenterHorizontally, verticalArrangement = Center)`:
  - `Box(64.dp, CircleShape, color = BrandColors.Coral.copy(alpha = 0.15f))` containing `Icons.Outlined.Add`, 28.dp, tint `BrandColors.CoralDeep`.
  - `Spacer(10.dp)`.
  - `"Add another pet"` — `bodyLarge.copy(fontWeight = SemiBold)`, `onSurface`.

### 3.7 Grid layout & spacing

- `LazyVerticalGrid(GridCells.Fixed(2), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp), horizontalArrangement = spacedBy(14.dp), verticalArrangement = spacedBy(14.dp))`.
- Sticky-ish (regular) `item(span = GridItemSpan(2))` at top: the active-count banner. Then `items(activePets)` → pet cards. Then `item` (single cell, not full span): the dashed add tile (omitted when `activePets.size >= MAX_ACTIVE_PETS`). Then if any archived: full-span "Archived (N)" header pill (existing toggle, restyled) and archived cards.

### 3.8 Archived band (light restyle, not the focus of the mock)

- Full-span header: `Row` with `"Archived"` (`titleMedium.copy(fontWeight = SemiBold)`) on the left, archived count badge `(N)` in `onSurfaceVariant`, and a `TextButton("Show" / "Hide")` toggle on the right.
- Archived pet cards use the same `PetThumbnailCard` rendered at `alpha = 0.6f` with a small `OutlinedButton("Restore")` directly below.

## 4. Component changes

All new composables live in `:feature:pet:presentation` (the screen is a one-off; no need to promote anything to `:core:ui` yet).

- Rename existing `PetThumbnailCard.kt` body to remain the canonical pet card. Heavy edit — overlays, intent chips, new typography.
- New helper composables inside the same file or a new `MyPetsComponents.kt`:
  - `PetStatusPill(active: Boolean)`
  - `IntentChip(intent: Intent)`
  - `AddAnotherPetTile(onClick: () -> Unit)`
  - `MyPetsHeader(onBack, onAdd, addEnabled)`
  - `ActivePetsBanner(activeCount: Int, max: Int)`

No edits to `:core:designsystem` or `:core:ui` — every brand color already exists (`BrandColors.Coral`, `CoralDeep`, `CoralLight`). The mint/sage and brown colors are screen-local hex constants since they only appear here.

## 5. State / behavior changes

- `MyPetsUiState` gains nothing new — existing `activePets`, `archivedPets`, `isLoading`, `errorMessage` cover the screen. Computed in the composable layer:
  - `addEnabled = activePets.size < MAX_ACTIVE_PETS`.
- `MAX_ACTIVE_PETS` constant (= 5) in `MyPetsScreen.kt` (private).
- Tap-handler wiring: card tap and pencil overlay both call `onOpenPet(pet.id)` → existing `PetDetail` route. There's no separate edit-from-list shortcut today; routing to the preview is fine because the preview offers Edit. (If the user wants a direct-to-edit shortcut from the pencil later, that's a follow-up.)
- New strings in `feature/pet/presentation/src/main/res/values/strings.xml` (file created):
  - `pet_screen_title` → "My Pets"
  - `pet_a11y_back` → "Back"
  - `pet_a11y_add` → "Add a pet"
  - `pet_a11y_edit_pet` → "Edit %1$s"
  - `pet_banner_subtitle_below_quota` → "More pets = more matches. Up to %1$d per account."
  - `pet_banner_subtitle_at_quota` → "You've reached the %1$d-pet limit."
  - `pet_status_active` → "ACTIVE"
  - `pet_status_paused` → "PAUSED"
  - `pet_intent_playdate` → "Playdate"
  - `pet_intent_friendship` → "Friendship"
  - `pet_intent_adoption` → "Adoption"
  - `pet_add_another_tile` → "Add another pet"
  - `pet_archived_header` → "Archived"
  - `pet_archived_show` → "Show"
  - `pet_archived_hide` → "Hide"
  - `pet_restore` → "Restore"
  - `pet_empty_title` → "No pets yet"
  - `pet_empty_subtitle` → "Add your first pet to start matching."
  - `pet_empty_cta` → "Add your first pet"
  - `pet_subtitle_format` → "%1$s · %2$s" (age string built in code; species label localized in code)
  - plurals `pet_active_count` → one: "%d active pet", other: "%d active pets".

Existing `PetUiHelpers.label()` provides species/intent labels; reuse those.

## 6. Files to add / modify / NOT modify

**Modify:**
- `feature/pet/presentation/src/main/kotlin/com/rodiz/arch2/feature/pet/presentation/MyPetsScreen.kt` — full rewrite of `MyPetsRoute`, `PetsGrid`, `EmptyMyPets`. Removes the `TopAppBar` + `Scaffold` chrome. Keeps the ViewModel wiring and `Restore` callback.
- `feature/pet/presentation/src/main/kotlin/com/rodiz/arch2/feature/pet/presentation/PetThumbnailCard.kt` — rewrite to match the new spec (status pill, edit overlay, intent chip column).

**Add:**
- `feature/pet/presentation/src/main/res/values/strings.xml` — new file (this module had none).

**Do NOT modify:**
- `MyPetsViewModel.kt` — state shape is sufficient.
- Domain / data / nav modules.
- `PetNavModule.kt`.
- Any other screen in the pet feature (Add/Edit/Preview).
- `BrandColors` (already has what we need).

## 7. Critical Compose recipes

- `LazyVerticalGrid` with mixed cell spans: use `item(span = { GridItemSpan(2) }) { ... }` for the banner and the archived header.
- Density-aware dashed border for the add tile:
  ```kotlin
  val density = LocalDensity.current
  val stroke = with(density) {
      Stroke(width = 2.dp.toPx(),
             pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())))
  }
  Modifier.drawBehind {
      drawRoundRect(color = strokeColor, style = stroke,
                    cornerRadius = CornerRadius(20.dp.toPx()))
  }
  ```
- Status-bar inset: wrap the root `Column` in `Modifier.statusBarsPadding()` (we're not using a Scaffold).
- Don't bake casing into strings: keep `"ACTIVE"` as the literal in the resource since it's a fixed-style label; alternatively `text.uppercase()` at the call site. Choose the latter so locales can still render lowercased forms — but since these are brand chips, baking uppercase is acceptable. Pick `.uppercase()` for safety.
- Pluralized count: `LocalContext.current.resources.getQuantityString(R.plurals.pet_active_count, n, n)`.

## 8. Verification checklist

- [ ] Build with JBR-17.
- [ ] Install on `emulator-5556`.
- [ ] From Profile tab, tap "My Pets" → land on the new screen.
- [ ] No pets → restyled empty state with coral CTA.
- [ ] With 1+ pets → header floats, banner shows correct count, grid shows pet cards with status pill, edit overlay, name+subtitle, intent chips. Dashed "Add another pet" tile sits after the last card.
- [ ] At 5 pets → dashed tile disappears; header "+" looks faded.
- [ ] Tap card → open Pet Preview. Tap pencil → also open Pet Preview.
- [ ] Tap header "+" → open Add Pet.
- [ ] Tap back → returns to Profile.

## 9. Out of scope

- Edit-from-pencil shortcut bypassing the preview screen.
- Pet card drag-to-reorder.
- Filter / search bar.
- Re-arranging the archived band layout meaningfully.
- Adding a `breed` field to the domain (deferred per `pet-profile.md`).
- Server-side enforcement of the 5-pet cap.

## 10. Risk / rollback

Low risk — screen-local change inside a single feature module's presentation layer. Rollback is `git revert` of the single commit. ViewModel + use cases + repository untouched.

## 11. Implementation order

1. Add `feature/pet/presentation/src/main/res/values/strings.xml` with all string + plural resources.
2. Rewrite `PetThumbnailCard.kt` with the new card layout and helper composables (`PetStatusPill`, `IntentChip`).
3. Rewrite `MyPetsScreen.kt` with the floating header, banner, grid (incl. dashed add tile), restyled empty state, restyled archived band.
4. `:app:installDebug`, navigate via Profile → My Pets, screenshot, diff, iterate.
5. Single local commit.

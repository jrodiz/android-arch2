# Plan: Add a pet — Step 1 redesign

> **Parent spec:** [`plans/pet-profile.md`](./pet-profile.md), [`plans/pet-my-pets-redesign.md`](./pet-my-pets-redesign.md). **Architecture rules:** [`ANDROID_APP_SCAFFOLD_PROMPT.md`](../ANDROID_APP_SCAFFOLD_PROMPT.md).

## 1. Context

The existing `AddPetRoute` (in `:feature:pet:presentation`) renders the legacy `PetForm`: stock `Scaffold`/`TopAppBar`, a horizontal lazy row of photo squares, an outlined `Pet name` field, an `OutlinedTextField` for bio, a slider for age, a row of M3 `FilterChip`s for species, and a `FilledTonalButton`. It works but isn't on-brand and treats the whole creation flow as one screen.

The mock reframes pet creation as a **3-step wizard** and ships **Step 1** (basics: photos, name, age, species, intents) on the warm cream surface used by My Pets / Inbox / Discover, with:

- A bespoke header (white rounded back button, big "Add a pet" title, "Step 1 / 3" coral label on the right).
- A 3-segment progress bar under the header (first segment filled coral, the other two pale).
- A large dashed coral rounded-rect hero "dropzone" with a circular coral chip + camera icon + "Add up to 6 photos" + helper text.
- A row of six 1:1 thumbnail slots beneath the hero (filled coral-bordered tile for the selected hero photo, plus 5 pale tiles with `+`).
- Uppercase `NAME` / `AGE` / `SPECIES` labels above white rounded text fields (white containers, soft shadow). Species shows an inline species emoji + label + chevron, acting as a selector.
- Uppercase `OPEN TO (PICK AT LEAST ONE)` label, then horizontally scrolling pill chips: selected = coral fill + white text + leading icon, unselected = white fill + coral border + coral icon.
- A bottom action bar floating above the content: outlined `Save draft` pill on the left, primary coral `Continue` pill (with chevron-right icon) on the right.

Non-goals for this turn:

- Steps 2 & 3 (no scaffolding for them).
- Actually saving the draft to the repo / Firestore.
- Wiring `Continue` to a destination beyond a stub (Step 2 navigation lands later).
- Bio field (deferred to a later step).
- Species picker bottom sheet — the chevron is wired to a no-op stub plus snackbar so the affordance is visible; the full picker UI ships with the species sheet (out of scope here, see §9).

## 2. Confirmed / inferred decisions

- **Module placement:** stays in `:feature:pet:presentation`. There is already a `feature/pet/{nav,domain,data,presentation}` quad and `AddPetRoute`/`AddPetViewModel`/`PetFormState` carry the field plumbing we need. Spinning up `:feature:addpet` is unwarranted — the addpet flow is logically part of the pet feature.
- **Reuse existing state machinery.** `PetFormUiState` / `PetFormEvent` / `handlePetFormEvent` (the ViewModel-agnostic reducer) already model name, age (years + approximate), species, intents, and a 6-photo list. We reuse them verbatim. The new screen reads `state.draft.*` and dispatches the same events. The redesign is presentation-only.
- **Continue button:** for this turn, `Continue` fires a `viewModel.attemptContinue()` that runs a step-1 validation (name non-empty, photos ≥ 1, species set, ≥1 intent) and either emits a `StepCompleted` one-shot event (route navigates to a placeholder destination — currently calls `onContinue()` which is wired in `PetNavModule` to a snackbar/no-op) **or** populates `state.step1Errors` for inline display. Persistence and Step 2 navigation are TODOs.
- **Save draft:** wired to `viewModel.saveDraft()` which currently emits a `DraftSaved` one-shot for the snackbar. No repository call yet — the existing `AddPetUseCase` is only invoked when the full 3-step flow finishes (out of scope).
- **Age input:** the mock shows a free-text "3 years" field, not a slider. We render a `FilledPillTextField`-style white pill that accepts digits only (KeyboardType.Number) and writes through `PetFormEvent.AgeYearsChanged`. Trailing " years" / " year" suffix is rendered as a placeholder/suffix inside the field, not stored in the value.
- **Species selector:** displayed as a white rounded "selector" pill (matching the AGE field height/style) that shows a species emoji + species label + trailing chevron. Tapping is a no-op stub for now (snackbar "Species picker coming soon"). Default selection if `state.draft.species == null`: render placeholder text "Pick species" in muted color, no emoji. When the user already has a species set (e.g. from a previous draft restore), the emoji + label render.
- **Intent chips:** 3 enum values (`PLAYDATE`, `ADOPTION`, `FRIENDSHIP`). The mock displays Playdate (selected) and Adoption (unselected) plus a small green sliver peeking — that maps cleanly to the third chip Friendship rendered in sage. We render all three in a horizontally scrollable `Row` (`horizontalScroll(rememberScrollState())`) so the mock's "more options below" hint is real, not painted. Each chip toggles `PetFormEvent.IntentToggled`.
- **Photo dropzone behavior:** tapping the big dashed hero or tapping any `+` slot opens the same `PickVisualMedia` activity-result launcher. Tapping a filled tile selects it as the hero (we render a coral 2.dp border + shadow on the first photo automatically since `photos[0]` is always the hero in `PetDraft`). For this turn, we don't yet support reordering — the hero is always index 0. Long-press / drag is out of scope.
- **Progress bar:** purely visual for Step 1; the three segments are a static `StepProgressBar(current = 1, total = 3)` so future steps can reuse it.
- **Cream surface:** `MaterialTheme.colorScheme.surface` (`#FFFBFA`) — the exact tone the rest of the redesigned screens already use. Don't introduce a new color token.
- **Step label color:** the "Step 1 / 3" text on the right uses `BrandColors.CoralDeep` to match other coral accents.

## 3. Visual spec

### 3.1 Background

- Root `Box` fills the screen, background = `MaterialTheme.colorScheme.surface`. Status bar inset honored via `Modifier.statusBarsPadding()` at the column top. No Scaffold.
- A bottom `Surface` floats over the scroll (the action bar), so the scroll content reserves enough bottom padding (~120.dp) for it.

### 3.2 Header (16.dp horizontal, 12.dp top after status bar inset)

`Row(verticalAlignment = CenterVertically)`:

- **Back button** — 44.dp square `Surface(shape = RoundedCornerShape(14.dp), color = Color.White, shadowElevation = 1.dp)` with centered `Icons.AutoMirrored.Outlined.ArrowBack`, 20.dp, `tint = onSurface`. `Modifier.clickable { onBack() }`. (Same recipe as `MyPetsScreen.HeaderSquareButton`.)
- **Title** `"Add a pet"` — `Modifier.weight(1f).padding(start = 12.dp)`, `headlineLarge.copy(fontWeight = ExtraBold)`, color `onSurface`. Single line.
- **Step indicator** `"Step 1 / 3"` — `bodyMedium.copy(fontWeight = SemiBold)`, color `BrandColors.CoralDeep`. Right-aligned.

### 3.3 Step progress bar (16.dp horizontal, 14.dp top spacing from header)

`Row(horizontalArrangement = spacedBy(8.dp))` with 3 expanded segments (`Modifier.weight(1f)`). Each segment is a `Box(Modifier.height(4.dp).clip(RoundedCornerShape(50)))`, background = `BrandColors.CoralDeep` for the active segment, `BrandColors.Coral.copy(alpha = 0.18f)` for the inactive ones. Generic enough to render later steps.

### 3.4 Photo dropzone hero (16.dp side margin, 20.dp top spacing from progress bar)

- `Box(Modifier.fillMaxWidth().height(176.dp).clip(RoundedCornerShape(24.dp)).background(Color.White).clickable { launchPhotoPicker() }).drawBehind { dashed coral border }`.
- Dashed border: `Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 7.dp.toPx())))`, color = `BrandColors.CoralDeep.copy(alpha = 0.6f)`, drawn via `drawRoundRect(cornerRadius = CornerRadius(24.dp.toPx()))` with density-aware px conversions (LocalDensity).
- Center column (`horizontalAlignment = CenterHorizontally, verticalArrangement = Center`):
  - 64.dp circle, background `BrandColors.Coral.copy(alpha = 0.18f)`, centered `Icons.Outlined.CameraAlt` 28.dp tinted `BrandColors.CoralDeep`.
  - `Spacer(12.dp)`.
  - `"Add up to 6 photos"` — `titleMedium.copy(fontWeight = SemiBold)`, color `BrandColors.CoralDeep`.
  - `Spacer(4.dp)`.
  - `"The first photo is the hero of the card"` — `bodySmall`, color `onSurfaceVariant.copy(alpha = 0.85f)`.

### 3.5 Thumbnail row (16.dp side margin, 14.dp top)

- `Row(horizontalArrangement = spacedBy(8.dp))` with 6 cells, each `Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(14.dp))`.
- For each index `i` in 0..5:
  - If `state.draft.photos.getOrNull(i) != null`:
    - Render `AsyncImage` (contentScale Crop) filling the cell. If `i == 0` (hero), wrap in a `Box` with a 2.dp `BorderStroke(color = BrandColors.CoralDeep)` (via `Modifier.border` after the clip) and `shadowElevation = 2.dp` (Surface).
    - Tiny close affordance on the cell: 20.dp circle, top-end aligned, black-35% bg, white `Icons.Outlined.Close` 12.dp, `clickable { onEvent(PhotoRemoved(i)) }`. Skipped for this turn? — keep it; the user must be able to remove a photo. (One small piece of polish beyond the strict mock — but the mock implicitly requires it once a photo is added.)
  - Else: pale tile — background `BrandColors.Coral.copy(alpha = 0.08f)`, centered `Icons.Outlined.Add` 22.dp tinted `onSurfaceVariant.copy(alpha = 0.65f)`. `clickable { launchPhotoPicker() }` (which appends via `PhotoAdded`). Disabled visually & functionally when `photos.size >= 6`.

### 3.6 Form section labels + fields (16.dp side margin, 20.dp top)

Each label is uppercase, `labelMedium.copy(fontWeight = SemiBold, letterSpacing = 0.8.sp)`, color `onSurfaceVariant`. 8.dp below label sits the field.

#### NAME

A single-row white pill field. We could reuse `FilledPillTextField` but it requires a leadingIcon — instead, render a thin local wrapper around `TextField` for the form fields:

- `Surface(shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 1.dp)` containing a `TextField` with no leading icon, transparent indicator, single-line, `value = state.draft.name`, `placeholder = "Pet name"`, container colors all white. Height 56.dp.

#### Two-column row (AGE + SPECIES) — `Row(horizontalArrangement = spacedBy(12.dp))`, both children `Modifier.weight(1f)`.

- **AGE field**: same white pill, `value = if (state.draft.ageYears > 0) state.draft.ageYears.toString() else ""`, placeholder `"e.g. 3"`, KeyboardType.Number, single line. After the field, a tiny suffix isn't natively supported on TextField — we render the unit inline by reading the field value and showing `"$n year(s)"` only when the field is empty (placeholder route). Decision: placeholder is the simplest, on-brand approach; we keep the field's actual text as digits only and leave the unit to the placeholder. **Trade-off:** the mock literally shows "3 years" inside the field; we follow it by setting placeholder to `"e.g. 3 years"` and storing only the integer, parsing leading digits on every change. This keeps the user from having to type the word "years" themselves.

- **SPECIES selector**: visually a white pill the same height as the AGE field. Inside: `Row(verticalAlignment = CenterVertically, horizontalArrangement = SpaceBetween, padding 16dp horizontal)`. Leading content: emoji+label (or placeholder), trailing `Icons.AutoMirrored.Outlined.KeyboardArrowRight` (or `Icons.Default.ChevronRight`) tinted muted. Whole pill is clickable → `onSpeciesClick()` (no-op snackbar stub for this turn). Emoji map (string built in code):

  | Species | Emoji |
  |---|---|
  | DOG | 🐕 |
  | CAT | 🐈 |
  | RABBIT | 🐇 |
  | HAMSTER | 🐹 |
  | GUINEA_PIG | 🐹 (fallback) |
  | FERRET | 🦦 (closest match available) |
  | OTHER_SMALL_MAMMAL | 🐾 |

  When `species == null`, render placeholder `"Pick species"` in `onSurfaceVariant.copy(alpha = 0.7f)`, no emoji.

### 3.7 OPEN TO (16.dp side margin, 20.dp top)

- Label uppercase `"OPEN TO (PICK AT LEAST ONE)"`, same label style.
- `Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = spacedBy(10.dp))` with one `IntentChoiceChip` per `Intent` enum entry.

`IntentChoiceChip(intent: Intent, selected: Boolean, onClick: () -> Unit)`:

- `Surface(shape = RoundedCornerShape(50))`. Container, border, content color depend on selection:
  - **Selected** (any intent): `color = BrandColors.CoralDeep`, `border = null`, `shadowElevation = 1.dp`, content (icon + label + leading check) all white. Leading content = `Row(Icons.Outlined.Check 14.dp, Spacer 4.dp, leadingIcon 14.dp, Spacer 6.dp, label)` per the mock's `✓ 🐾 Playdate` ordering.
  - **Unselected**: `color = Color.White`, `border = BorderStroke(1.5.dp, BrandColors.CoralDeep)`, `shadowElevation = 0.dp`, content = `Row(leadingIcon 14.dp, Spacer 6.dp, label)`. Icon tint + label color = `BrandColors.CoralDeep`.
- Padding 14.dp horizontal, 9.dp vertical. Label = `labelLarge.copy(fontWeight = SemiBold)`.
- Per-intent leading icon:

  | Intent | Icon |
  |---|---|
  | PLAYDATE | `Icons.Outlined.Pets` |
  | ADOPTION | `Icons.Outlined.Home` |
  | FRIENDSHIP | `Icons.Outlined.Favorite` |

  These intentionally match the mini chips on the `PetThumbnailCard`. The check mark only appears when selected.

### 3.8 Bottom action bar (floats above scroll content)

- A `Surface(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shadowElevation = 0.dp)` with `Modifier.padding(horizontal = 16.dp).navigationBarsPadding().padding(top = 12.dp, bottom = 16.dp)`. We want it to **not** look like a separate surface — the cream surface bleeds in. To prevent the scroll content from disappearing under the bar visually, we add a subtle top gradient fade — implemented via a thin `Spacer(8.dp)` and the surface itself sits flat. (No fancy shadow; the mock shows the buttons floating, not a divider.)
- `Row(horizontalArrangement = spacedBy(12.dp), verticalAlignment = CenterVertically)`:
  - **Save draft** button — `OutlinedButton` styled to brand: `shape = RoundedCornerShape(50)`, `border = BorderStroke(1.5.dp, BrandColors.CoralDeep.copy(alpha = 0.5f))`, `colors = ButtonDefaults.outlinedButtonColors(contentColor = onSurface, containerColor = Color.White)`, padding inside = horizontal 20.dp / vertical 14.dp. Label `"Save draft"` `titleSmall.copy(fontWeight = SemiBold)`. Width = `wrapContent` (don't weight) — sits flush left.
  - **Continue** button — primary pill with chevron. Surface(`RoundedCornerShape(50)`, color = `BrandColors.CoralDeep`, shadowElevation = 2.dp, modifier `Modifier.weight(1f).clickable { onContinue() }`). Inside: centered `Row(verticalAlignment = CenterVertically, horizontalArrangement = Center, spacedBy(8.dp), padding vertical 14.dp)`. Leading `Icons.AutoMirrored.Outlined.KeyboardArrowRight` 18.dp white + label `"Continue"` `titleMedium.copy(fontWeight = Bold)` white. (Mock shows chevron *before* the label.)

### 3.9 Scroll container

- The whole content (header → progress → dropzone → thumbnails → form → intents) lives in a `Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 120.dp))` so it never sits under the action bar.
- Between sections we use `Spacer` of variable heights as called out above.

## 4. Component changes

All new composables live in `:feature:pet:presentation` (one-off, no need to promote to `:core:ui` yet):

- `AddPetStep1Screen.kt` — new file. Contains `AddPetStep1Route` + `AddPetStep1Screen` + the internal helpers (`StepProgressBar`, `PhotoDropzone`, `PhotoStrip`, `LabeledField`, `WhiteTextPill`, `SpeciesSelectorPill`, `IntentChoiceChip`, `Step1ActionBar`).
- `AddPetScreen.kt` — slimmed: `AddPetRoute` now delegates to `AddPetStep1Route` (preserves the same `onDone` lambda for back, adds an `onContinue` no-op for now). Keeps the existing nav entry intact.
- `AddPetViewModel.kt` — augmented:
  - Add `data object DraftSaved : Step1Event` / `data object ContinueRequested : Step1Event` (or simpler: two `SharedFlow`s `draftSaved` and `continueRequested`).
  - Add `fun saveDraft()` — emits `draftSaved`. No persistence.
  - Add `fun attemptContinue()` — validates Step 1 fields (name non-empty, ≥1 photo, species set, ≥1 intent). Emits `continueRequested` if OK; otherwise sets `state.step1Errors`.
  - Add `step1Errors: Set<Step1Error>` to `PetFormUiState` (additive). `Step1Error = { NameMissing, PhotosMissing, SpeciesMissing, IntentMissing }`.
- `PetFormState.kt` — add `step1Errors` field (default empty).
- `PetNavModule.kt` — `AddPetRoute(onDone = ..., onContinue = { /* TODO step 2 */ })` — onContinue currently calls `navigator.goBack()` plus a snackbar would be nice but Navigator surface lacks one; for now we make `onContinue` identical to `onDone` so the user sees they advanced. (Marked TODO in code.)

No edits to `:core:designsystem` or `:core:ui`. Every brand color already exists (`BrandColors.Coral`, `CoralDeep`, `CoralLight`). The dashed-border recipe is the same one in `MyPetsScreen.AddAnotherPetTile`.

## 5. State / behavior changes

- `PetFormUiState`:
  - Add `val step1Errors: Set<Step1Error> = emptySet()`.
- `Step1Error` sealed interface (presentation-only):
  - `NameMissing`, `PhotosMissing`, `SpeciesMissing`, `IntentMissing`.
- `AddPetViewModel`:
  - `private val _step1Events = MutableSharedFlow<Step1Event>(extraBufferCapacity = 1)`; expose as `SharedFlow`.
  - `sealed interface Step1Event { data object DraftSaved : Step1Event; data object ContinueRequested : Step1Event }`.
  - `fun saveDraft() { _step1Events.tryEmit(Step1Event.DraftSaved) }` (stub).
  - `fun attemptContinue() { val errs = validateStep1(state); if errs.isEmpty() then emit ContinueRequested else update step1Errors }`.
  - Any field-edit event clears the corresponding step1Error.
- New strings in `feature/pet/presentation/src/main/res/values/strings.xml`:
  - `addpet_title` → "Add a pet"
  - `addpet_step_indicator` → "Step %1$d / %2$d"
  - `addpet_a11y_back` → reuse existing `pet_a11y_back`
  - `addpet_dropzone_title` → "Add up to 6 photos"
  - `addpet_dropzone_hint` → "The first photo is the hero of the card"
  - `addpet_label_name` → "Name"
  - `addpet_label_age` → "Age"
  - `addpet_label_species` → "Species"
  - `addpet_label_open_to` → "Open to (pick at least one)"
  - `addpet_placeholder_name` → "e.g. Biscuit"
  - `addpet_placeholder_age` → "e.g. 3 years"
  - `addpet_placeholder_species` → "Pick species"
  - `addpet_action_save_draft` → "Save draft"
  - `addpet_action_continue` → "Continue"
  - `addpet_snackbar_draft_saved` → "Draft saved"
  - `addpet_snackbar_species_soon` → "Species picker coming soon"
  - `addpet_error_name_missing` → "Add your pet's name"
  - `addpet_error_photos_missing` → "Add at least one photo"
  - `addpet_error_species_missing` → "Pick a species"
  - `addpet_error_intent_missing` → "Pick at least one intent"
  - `addpet_a11y_remove_photo` → "Remove photo"

Existing `Intent.label()` / `Species.label()` reused for the chip labels and species pill.

## 6. Files to add / modify / NOT modify

**Modify:**
- `feature/pet/presentation/src/main/kotlin/com/rodiz/arch2/feature/pet/presentation/AddPetScreen.kt` — replace with a delegating `AddPetRoute` (forwards to `AddPetStep1Route`). Keeps the nav contract identical.
- `feature/pet/presentation/src/main/kotlin/com/rodiz/arch2/feature/pet/presentation/AddPetViewModel.kt` — add `step1Events`, `saveDraft()`, `attemptContinue()`, `Step1Event`, validation.
- `feature/pet/presentation/src/main/kotlin/com/rodiz/arch2/feature/pet/presentation/PetFormState.kt` — add `step1Errors: Set<Step1Error>` + `Step1Error` sealed interface.
- `feature/pet/presentation/src/main/kotlin/com/rodiz/arch2/feature/pet/presentation/PetNavModule.kt` — pass `onContinue` lambda (currently same as `onDone`).
- `feature/pet/presentation/src/main/res/values/strings.xml` — append new `addpet_*` strings.

**Add:**
- `feature/pet/presentation/src/main/kotlin/com/rodiz/arch2/feature/pet/presentation/AddPetStep1Screen.kt`.

**Do NOT modify:**
- `:feature:pet:nav` / `:domain` / `:data`.
- `:core:designsystem` / `:core:ui`.
- `EditPetScreen.kt`, `PetForm.kt` (still used by EditPet — leave intact for now).
- Other features.
- `BrandColors`.

## 7. Critical Compose recipes

- Density-aware dashed border (same as `AddAnotherPetTile`):
  ```kotlin
  val density = LocalDensity.current
  val strokeWidthPx = with(density) { 2.dp.toPx() }
  val dashOn = with(density) { 10.dp.toPx() }
  val dashOff = with(density) { 7.dp.toPx() }
  val cornerPx = with(density) { 24.dp.toPx() }
  val stroke = remember(strokeWidthPx, dashOn, dashOff) {
      Stroke(width = strokeWidthPx,
             pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashOn, dashOff)))
  }
  Modifier.drawBehind {
      val inset = strokeWidthPx / 2f
      drawRoundRect(color = coral,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - strokeWidthPx, size.height - strokeWidthPx),
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                    style = stroke)
  }
  ```
- Don't bake uppercase casing into XML strings — render via `.uppercase()` at the call site for labels.
- `Modifier.verticalScroll` must come **before** `.imePadding()` if we later add keyboard insets; for this screen we use `.padding(bottom = 120.dp)` plus rely on the scroll to expose hidden fields when the keyboard opens. We add `Modifier.imePadding()` to the scroll Column to be safe.
- One-shot events: collect from the new SharedFlow inside a `LaunchedEffect(Unit)` and call `snackbarHostState.showSnackbar` for `DraftSaved` / `ContinueRequested` (stubbed copy).
- The action bar `Surface` floats — set `Modifier.align(Alignment.BottomCenter)` on it inside an outer `Box`. The scroll Column is the other child.
- Number-only AGE field: `KeyboardOptions(keyboardType = KeyboardType.Number)` + filter digits in the `onValueChange` so the reducer sees a clean Int.
- Status bar icons: this screen sits on light cream, so the default dark icons are correct — we **don't** need `LightStatusBarIconsWhileShown` (that's the coral-hero recipe).

## 8. Verification checklist

- [ ] Build with JBR-17 (`./gradlew :app:installDebug`).
- [ ] Launch app; sign in (Firebase or test path); navigate Profile → My Pets → "+" header button → land on the redesigned Add a pet screen.
- [ ] Screenshot matches the mock for: header + step indicator, progress bar, photo dropzone (coral dashed, camera chip, copy), 6 thumbnail slots, NAME/AGE/SPECIES labels and pills, OPEN TO chips, bottom action bar.
- [ ] Tap a + thumbnail → photo picker opens; pick → first slot fills with image + coral border. Subsequent picks fill subsequent slots up to 6.
- [ ] Type in NAME → state updates.
- [ ] Type digits in AGE → state updates; non-digits ignored.
- [ ] Tap SPECIES pill → snackbar "Species picker coming soon" appears.
- [ ] Tap PLAYDATE / ADOPTION / FRIENDSHIP → toggles; selected = coral fill + white check, unselected = white + coral border.
- [ ] Tap Save draft → snackbar "Draft saved".
- [ ] Tap Continue with empty fields → no nav; inline error chips/banners surface (text in red under the relevant section).
- [ ] Tap Continue with all required fields → currently routes back (placeholder for Step 2).
- [ ] Back chevron returns to My Pets.

## 9. Out of scope

- Steps 2 & 3 of the wizard.
- Persisting the in-progress draft across process death.
- Photo reorder / hero-swap drag.
- Bio / size / energy fields.
- Species picker bottom sheet UI.
- Add Pet entry from places other than the existing My Pets "+" header button.
- Server-side cap of 5 active pets (covered in `pet-my-pets-redesign.md`).
- Hooking `AddPetUseCase` — fires only when the 3-step flow completes.

## 10. Risk / rollback

Low risk — change is confined to `:feature:pet:presentation`. Rollback = `git revert` of the single commit. `AddPetUseCase`, repository, nav contract, and domain are untouched.

## 11. Implementation order

1. Append new `addpet_*` strings to `feature/pet/presentation/src/main/res/values/strings.xml`.
2. Extend `PetFormState.kt` with `step1Errors` + `Step1Error`.
3. Extend `AddPetViewModel.kt` with `Step1Event`, `step1Events` SharedFlow, `saveDraft()`, `attemptContinue()`, validation, and reducer hooks to clear errors on field edits.
4. Create `AddPetStep1Screen.kt` with the new UI (header, progress, dropzone, thumbnails, labeled fields, intents row, action bar) wired to existing events + the new `attemptContinue`/`saveDraft`.
5. Rewrite `AddPetScreen.kt` to delegate `AddPetRoute` to `AddPetStep1Route`.
6. Pass an `onContinue` lambda through `PetNavModule` (currently same as `onDone`).
7. Build, install, screenshot, iterate, commit.

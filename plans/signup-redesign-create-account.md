# Plan — Sign Up redesign: "Create account" hero + dashed avatar + Terms gate

> **NOTE on plan location:** project convention ([[feedback_plans_location]]) is to save plans under `plans/` in the Arch2.0 repo. Plan-mode constraints put the working copy at `~/.claude/plans/zippy-dreaming-moth.md`. **Step 0 of implementation is to copy this file to `plans/signup-redesign-create-account.md`** so it gets versioned with the code.

## 1. Context

The Login screen was just rebranded with the "Welcome back" hero (coral block + radar rings + pet tiles + filled pill fields). The Sign Up screen, reached from Login's "Sign up" tab, still uses the old visual language: a 160dp coral hero with a centered "Sign up" title and outlined Material fields. The new mockup brings Sign Up into the same brand family but with its own personality: a **gradient hero** (coral → peach) with a **scalloped bottom**, a **dashed-box avatar picker** with a "+" badge, **WHITE pill fields** with uppercase labels, and a new **Terms & Privacy checkbox** that gates the Create-account CTA.

**Goal:** match the mockup, keep all existing auth wiring (Firebase, validation, avatar upload, savedStateHandle persistence) intact. **Non-goal:** restyle the avatar source bottom sheet, change the registration network flow, or wire real Terms / Privacy destinations.

## 2. Confirmed decisions

1. **Terms / Privacy link taps** → show a "Coming soon" snackbar. Bold spans are tappable via `LinkAnnotation.Clickable`, but no navigation happens yet.
2. **Terms checkbox is REQUIRED** to enable the Create account button. `canSubmit &= termsAccepted`.

## 3. Visual spec (target)

### 3.1 Hero (~280dp)
- **Background**: `Brush.verticalGradient(BrandColors.Coral → lighter peach)`. Add a new `BrandColors.CoralLight` (e.g. `#F6B5A0`) to `Color.kt` so the gradient stops are named, not inline.
- **Bottom edge**: clipped with the existing `WaveBottomShape` (12% depth). Same shape Login's old hero used; visually verified to read as a soft scallop.
- **Decorative ellipses**: 3 faint white ovals drawn via `Modifier.drawBehind { drawOval(...) }`. Positions / sizes relative to a `BoxWithConstraints`' `maxWidth`/`maxHeight` so they scale. Alpha ~0.20. Approximate placement:
  - top-left ellipse: center `(0.20 * w, 0.30 * h)`, size `(180dp, 80dp)`
  - top-right ellipse: center `(0.78 * w, 0.30 * h)`, size `(150dp, 70dp)`
  - mid-right ellipse: center `(0.55 * w, 0.65 * h)`, size `(200dp, 80dp)`
- **Inline back-arrow + headline (Row)**:
  - `IconButton(onClick = onBack)` rendering `Icons.AutoMirrored.Outlined.ArrowBackIosNew` (or `ArrowBack`), tint `Color.White`.
  - `Text("Create account", style = headlineLarge.copy(fontWeight = ExtraBold), color = Color.White, modifier = Modifier.semantics { heading() })`.
- **Subtitle (below the Row)**: `Text("Two minutes to start matching your pets with new friends nearby.", style = bodyMedium, color = Color.White)`.
- **Insets**: hero applies `Modifier.statusBarsPadding()` so the back arrow + headline don't collide with the system bar.

### 3.2 Avatar picker (~160dp, replaces current 96dp circle)
- **Outer Box**: `size(160.dp)`, `clip(RoundedCornerShape(28.dp))`, fill `BrandColors.Coral.copy(alpha = 0.08f)`.
- **Dashed border**: `Modifier.drawBehind { drawRoundRect(color = BrandColors.Coral, style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 12f))), cornerRadius = CornerRadius(28.dp.toPx())) }`.
- **Center**: 48dp camera icon (`Icons.Outlined.PhotoCamera`), tint coral, OR an `AsyncImage` of `state.avatarUri` when set (clipped to the same RoundedCornerShape).
- **"+" badge** (bottom-end, only when avatarUri is null): coral `Color`-filled `Box(size = 36.dp).clip(CircleShape)` with a white `Add` icon, offset `(-8.dp, -8.dp)` from the picker's bottom-right corner via `Modifier.align(Alignment.BottomEnd).padding(8.dp)`.
- **"Remove" affordance** (when avatarUri is set): small `IconButton(onClick = AvatarCleared)` at top-right of the picker with `Icons.Outlined.Close`, white-on-coral background.
- **Caption** below: single string `signup_avatar_picker_label = "Add a photo (optional) — owners love a face"`. Style `bodySmall`, `onSurfaceVariant`.
- **Tappable area**: the whole picker `clickable(onClick = onAction(PickAvatarTapped))`.

### 3.3 Form fields (5)
- Each field is a stacked **Column**: an **uppercase label** above + a **white pill field** below.
  - Label: `Text(label.uppercase(), style = labelMedium.copy(fontWeight = Bold, letterSpacing = 1.sp), color = onSurfaceVariant)`. Render uppercased at the screen level — strings.xml stays mixed-case for translation cleanliness.
  - Field: `FilledPillTextField` (with the new optional `containerColor = Color.White` + `shadowElevation = 2.dp` params — see §4).
- **Five fields** in order: First name (Person), Last name (Person), Email (Mail, via `EmailFieldPill`), Password (Lock + eye, via `PasswordFieldPill`), Confirm password (Lock + eye, via `PasswordFieldPill`, IME `Done` → `Submit`).

### 3.4 Terms checkbox row
- `Row.toggleable(value = termsAccepted, onValueChange = { onAction(ToggleTerms) }, role = Role.Checkbox)`.
- `Checkbox(checked = termsAccepted, ...)` with `CheckboxDefaults.colors(checkedColor = BrandColors.Coral, uncheckedColor = onSurfaceVariant)` and `RoundedCornerShape(4.dp)` (custom checkbox uses Material default; tweak only if visually off).
- Label `Text(AnnotatedString)` with `LinkAnnotation.Clickable` on "Terms" and "Privacy" spans (bold, coral). Tap dispatches `SignUpAction.TermsLinkTapped` / `PrivacyLinkTapped`.

### 3.5 Primary CTA
- Inline (not sticky) `PrimaryButton(text = "Create account", loading = state.isSubmitting, enabled = state.canSubmit, onClick = { onAction(Submit) }, testTag = "signup_submit")`.

### 3.6 Snackbar plumbing
- Wrap the screen content in `Scaffold(containerColor = Color.Transparent, snackbarHost = { SnackbarHost(snackbarHostState) })` so the hero continues to paint under the status bar.
- Existing `ErrorBanner` (sticky, dismissible, for `transientError`) stays inline at the top of the form area — different UX intent from snackbar.
- New `SnackbarHostState` collects a one-shot flow of `SignUpEvent.ShowComingSoon` strings emitted by the ViewModel.

## 4. Component changes

### `core/ui/components/FilledPillTextField.kt` — extend, additive only
Add two new optional params, defaults preserve the current Login behavior:
- `containerColor: Color? = null` — when non-null, overrides the default coral-peach 10% alpha. SignUp passes `Color.White`.
- `shadowElevation: Dp = 0.dp` — when > 0, wrap the `TextField` in a `Surface(shape = CircleShape, shadowElevation = ...)`. SignUp passes `2.dp`.

The `colors = TextFieldDefaults.colors(...)` block resolves `containerColor ?: defaultPeach` for the four container slots.

### `feature/login/presentation/.../screen/AvatarPicker.kt` — replace wholesale
The current 96dp circular picker is only used by SignUp. Rewrite it as the dashed 160dp picker described in §3.2. Existing API (`onTap: () -> Unit, onClear: () -> Unit, avatarUri: String?`) stays — only the visual changes.

### New file: `core/designsystem/theme/Color.kt` — add `CoralLight`
```
val CoralLight: Color = Color(0xFFF6B5A0)
```
Sits in the `BrandColors` object next to `Coral` and `CoralDeep`. Used as the bottom stop of the Sign Up hero gradient.

## 5. State / behavior changes

| File | Change |
|---|---|
| `SignUpUiState.kt` | Add `val termsAccepted: Boolean = false`. Update `canSubmit` getter to require `&& termsAccepted`. |
| `SignUpAction.kt` | Add `data object ToggleTerms`, `data object TermsLinkTapped`, `data object PrivacyLinkTapped`. |
| `SignUpViewModel.kt` | Handle `ToggleTerms` → flip `termsAccepted`. Handle `TermsLinkTapped` / `PrivacyLinkTapped` → emit a new `SignUpEvent.ShowComingSoon(message)` on a one-shot flow. |
| `SignUpEvent.kt` (or wherever events live) | Add `data class ShowComingSoon(val resId: Int)`. (Use a string-resource id so the route can resolve it at render time.) |
| `SignUpRoute.kt` | Collect `ShowComingSoon` events and call `snackbarHostState.showSnackbar(stringResource(...))`. |
| `SignUpScreen.kt` | Full rewrite per §3. |
| `feature/login/presentation/src/main/res/values/strings.xml` | Rename value of `signup_title` from "Sign up" to "Create account". Add `signup_subtitle` ("Two minutes to start matching your pets with new friends nearby."). Replace `signup_avatar_picker_label` value with "Add a photo (optional) — owners love a face". Add `signup_terms_prefix` ("I agree to TinPet's "), `signup_terms_link` ("Terms"), `signup_terms_conjunction` (" & "), `signup_privacy_link` ("Privacy"). Add `signup_coming_soon` ("Coming soon"). Change `signup_submit` value from "Register" to "Create account". |
| `feature/login/presentation/src/test/.../SignUpViewModelTest.kt` | Add `termsAccepted=true` to fixtures that already assert `canSubmit==true`. Add a new test asserting `canSubmit==false` when `termsAccepted==false` and all other fields valid. |

## 6. Files to add / modify

### Add
- (none new) — `CoralLight` is added to existing `Color.kt`.

### Modify
- `core/ui/components/FilledPillTextField.kt` — add `containerColor` + `shadowElevation` params (defaults preserve current Login look).
- `core/designsystem/theme/Color.kt` — add `BrandColors.CoralLight`.
- `feature/login/presentation/.../screen/SignUpScreen.kt` — full rewrite of the screen composable + new private `SignUpHero`, `AvatarPickerDashed` (replacing the current AvatarPicker.kt), `LabeledField`, `TermsRow` helpers.
- `feature/login/presentation/.../screen/AvatarPicker.kt` — replaced wholesale (or deleted if it's now inlined in SignUpScreen; recommend keep as separate file for testability).
- `feature/login/presentation/.../screen/SignUpRoute.kt` — collect the new `ShowComingSoon` event and call `snackbarHostState.showSnackbar(...)`.
- `feature/login/presentation/.../state/SignUpUiState.kt` — `termsAccepted` field + updated `canSubmit`.
- `feature/login/presentation/.../state/SignUpAction.kt` — 3 new objects.
- `feature/login/presentation/.../state/SignUpEvent.kt` — `ShowComingSoon` data class.
- `feature/login/presentation/.../viewmodel/SignUpViewModel.kt` — new handlers, one-shot snackbar emission.
- `feature/login/presentation/src/main/res/values/strings.xml` — rename + 6 new strings per §5.
- `feature/login/presentation/src/test/.../SignUpViewModelTest.kt` — fixture updates + 1 new test.

### Do NOT modify
- `core/ui/components/{BrandHeader,EmailField,PasswordField,BrandTextField,PrimaryButton,ErrorBanner}.kt`. SignUp stops using `BrandHeader` (builds its own hero); the outlined field components stay for any future caller. `EmailFieldPill` / `PasswordFieldPill` already accept the new `containerColor`/`shadowElevation` transitively because they delegate to `FilledPillTextField`.
- `core/designsystem/theme/Shapes.kt` — `WaveBottomShape` stays; SignUp reuses it for the scalloped bottom.
- `feature/login/presentation/.../screen/AvatarSourceSheet.kt` — out of scope.
- `:feature:login:data` / `:domain` — auth wiring untouched.

## 7. Critical recipes (from Plan agent verdict)

1. **`LinkAnnotation.Clickable` is available** in Compose UI 1.7.4 (matches the BOM `2024.10.01`). Build the AnnotatedString with:
   ```
   buildAnnotatedString {
     append("I agree to TinPet's ")
     withLink(LinkAnnotation.Clickable("terms", styles = TextLinkStyles(SpanStyle(fontWeight = Bold, color = Coral))) { onAction(TermsLinkTapped) }) {
       append("Terms")
     }
     append(" & ")
     withLink(LinkAnnotation.Clickable("privacy", ...) { onAction(PrivacyLinkTapped) }) {
       append("Privacy")
     }
   }
   ```
   Compose absorbs link clicks so the surrounding `Row.toggleable` won't double-fire.
2. **Whole-row toggleable** wraps Checkbox + label so users can tap anywhere outside the bold link spans to flip the checkbox. Use `Role.Checkbox` for a11y. Don't put `clickable` on Checkbox AND toggleable on Row — Compose will fight focus.
3. **`Modifier.imePadding()` must come AFTER `Modifier.verticalScroll`** on the outer Column. Otherwise the IME shrinks the scrollable area's content instead of its viewport; the hero would jump.
4. **Status bar**: copy Login's `LightStatusBarIconsWhileShown` modifier to SignUp (or extract it to `:core:ui`). Without it, the white back arrow + title clash with the system's default dark status-bar icons.
5. **Dashed border pixel snapping**: `dashPathEffect(floatArrayOf(20f, 12f))` is in pixels. On low-DPI emulators dashes can look uneven; use `with(LocalDensity.current) { 8.dp.toPx() to 6.dp.toPx() }` for density-aware dashes.
6. **AnnotatedString a11y**: TalkBack announces `LinkAnnotation` spans as links automatically. Don't add manual `clickable` modifiers around the spans.
7. **Snackbar vs ErrorBanner**: keep `ErrorBanner` for sticky auth errors (`transientError`), use Snackbar for transient one-shot hints ("Coming soon"). Two surfaces, two UX intents — don't merge.
8. **CoralLight value pick**: `#F6B5A0` reads close to the mockup's peach. If it skews too pink, drop to `#F8C7B2`. Test in the Compose preview before iterating.

## 8. Verification

1. **Build**: `JAVA_HOME=…/jbr-17.0.14 ./gradlew :feature:login:presentation:assembleDebug && :app:assembleDebug`.
2. **Unit tests**: `./gradlew :feature:login:presentation:testDebugUnitTest`. Expected: existing `SignUpViewModelTest` passes after `termsAccepted=true` is added to the canSubmit fixtures; the new "Terms unchecked blocks submit" test passes.
3. **Compose previews** — add three:
   - `SignUpScreenPreviewEmpty` — fresh state, Create account disabled, Terms unchecked.
   - `SignUpScreenPreviewFilled` — all fields valid + Terms checked, Create account enabled.
   - `SignUpScreenPreviewError` — `transientError = AuthError.EmailInUse`, fields filled.
4. **Emulator** (`emulator-5556`, 1080×2400):
   - From Login, tap the "Sign up" pill → lands on Sign Up.
   - Hero gradient + 3 white ellipses + scalloped bottom render.
   - White back arrow + "Create account" headline + subtitle render inside the hero.
   - Tap back → returns to Login.
   - Dashed-box avatar picker visible with "+" badge; tap → existing avatar source sheet appears; pick from gallery; image fills the box; "+" badge swaps for a Remove affordance.
   - All 5 fields are filled WHITE pills with uppercase labels above; leading icons (person/mail/lock) render; password eye toggle works.
   - Terms checkbox tap toggles state. Tapping "Terms" or "Privacy" shows a "Coming soon" snackbar.
   - Create account button is disabled until Terms is checked + all fields valid.
   - With all valid + Terms checked, tap Create account → existing registration flow runs (validation + Firebase). Successful sign-up lands on Deck.
   - IME open: focused field stays above keyboard; hero scrolls off — acceptable.
   - Dark mode: white pill fields read as white on a dark background (acceptable contrast); ellipses still visible on the hero.
5. **Screenshot** to `/tmp/signup-redesign.png` via `adb -s emulator-5556 exec-out screencap -p`.

## 9. Out of scope

- Real Terms / Privacy destinations (snackbar stub for now; future plan).
- Avatar source bottom sheet restyle (current Material style stays).
- Login screen changes.
- Server-side / data layer changes.
- Forgot password screen.

## 10. Risk / rollback

- Risk: `LinkAnnotation.Clickable` rendering quirks on older API levels. Mitigation: visual smoke on emulator-5556 (API 35).
- Risk: dashed border looks janky on low-DPI screens. Mitigation: density-aware dash sizing per §7.5.
- Rollback: revert the single `feat(signup): create-account redesign` commit. The only files touched in `:core:*` are the additive `FilledPillTextField` params and one new `BrandColors.CoralLight` — both backwards-compatible.

## 11. Implementation order

When the user accepts and we exit plan mode:

0. Copy this plan to `plans/signup-redesign-create-account.md` in the repo root.
1. Add `containerColor` + `shadowElevation` params to `FilledPillTextField`. Add `BrandColors.CoralLight`.
2. Update `SignUpUiState` + `SignUpAction` + `SignUpEvent` + `SignUpViewModel` + `SignUpViewModelTest` for Terms gating + ShowComingSoon snackbar.
3. Update strings.xml per §5.
4. Replace `AvatarPicker.kt` with the dashed 160dp version.
5. Rewrite `SignUpScreen.kt`: new `SignUpHero` (gradient + ellipses + back/headline/subtitle inside), Scaffold wrapper for the Snackbar, the `LabeledField` helper for uppercase-label + pill stack, the new `TermsRow` with `LinkAnnotation.Clickable`, inline CTA.
6. Update `SignUpRoute.kt` to collect `ShowComingSoon` and call `snackbarHostState.showSnackbar(...)`.
7. Build, run previews, install on emulator-5556, screenshot, compare against the mockup.
8. Single commit: `feat(signup): create account redesign with gradient hero, dashed avatar, Terms gate`.

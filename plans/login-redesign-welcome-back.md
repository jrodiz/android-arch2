# Plan — Login redesign: "Welcome back" hero

> **NOTE on plan location:** the project convention ([[feedback_plans_location]]) is to save plans under `plans/` in the Arch2.0 repo. Plan-mode constraints put the working copy at `~/.claude/plans/zippy-dreaming-moth.md`. **Step 0 of implementation is to copy this file to `plans/login-redesign-welcome-back.md` in the repo** so it gets versioned with the code, then continue from there.

## 1. Context

The current Login screen ships a coral wave-bottom hero, big text "Sign In / Sign Up" tabs with an underline indicator, and a progressive-disclosure "Login with email" CTA that animates an outlined-Material form open. We have a new mockup that swaps in a friendlier brand surface — three tilted pet thumbnails on a radar-ring backdrop, a white card overlapping the hero with "Welcome back" copy, pill-segmented tabs, and filled-pill input fields with leading icons. The form is always visible.

**Goal:** match the mockup pixel-for-spirit while keeping the existing auth wiring (Firebase email/password, Google sign-in, biometric prompt) and validation behavior intact. **Non-goal:** refactor `:core:ui` form components beyond what the new style requires; rebrand the SignUp screen (it still uses `BrandHeader` + outlined fields and is fine).

## 2. Confirmed decisions

1. **Drop the email-reveal animation.** Form is always rendered; remove `LoginUiState.emailFormExpanded`, `LoginAction.ShowEmailForm`, and the `R.string.login_email_form_show` resource.
2. **Bundle 3 placeholder vector tiles** under `feature/login/presentation/res/drawable/`: `pet_tile_puppies.xml`, `pet_tile_cat.xml`, `pet_tile_rabbit.xml`. Each is a `surfaceVariant`/coral-tinted rounded square with the existing Pets paw icon centered. The white border + corner radius live in Compose (`Modifier.border` + `Modifier.clip`), not in the vector. User swaps to photos later by replacing the drawables — no Compose change.
3. **Keep biometric button** below "Sign in" when `state.biometricAvailable`. Not rendered in the mockup, but the affordance is too useful to drop.

## 3. Visual spec (target)

### 3.1 Hero (top ~360dp)
- Background: flat `BrandColors.Coral` rectangle, no wave clip.
- **Radar rings**: 4 concentric circles drawn via `Modifier.drawBehind` on the hero `Box`. Center anchored at ~`(0.35 * maxWidth, 0.45 * maxHeight)`. Radii at `0.25f / 0.40f / 0.55f / 0.70f * maxWidth`. Stroke 2dp, color `White.copy(alpha = 0.20f)`.
- **Pet tiles**: 3 `Image`s using the new vector drawables, sized `110.dp`, clipped with `RoundedCornerShape(20.dp)`, bordered with `4.dp` `Color.White` of the same shape, slight `Modifier.rotate(±5–8°)`. Positions (BoxWithConstraints-relative so they scale):
  - Puppies — top-left, `offset(x = 0.05 * maxWidth, y = 0.20 * maxHeight)`, rotation `-6°`.
  - Cat — right side, `offset(x = 0.55 * maxWidth, y = 0.40 * maxHeight)`, rotation `+8°`.
  - Rabbit — bottom-center, straddles the hero/card boundary. Placed as a **sibling** of hero and card (not a child of hero) inside the outer `Box`, with `Modifier.align(BottomCenter).offset(y = -(cardOverlap + 36.dp))`. Rotation `-4°`.

### 3.2 White card
- `Surface(shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp, shadowElevation = 0.dp)` — flat, no shadow per mockup.
- Vertical overlap with hero: 50dp (rabbit's tile sits on this boundary).
- Inner padding: `horizontal = 24.dp`, `top = 32.dp`, `bottom = 32.dp`.
- Inner column gets `Modifier.imePadding()` so focused fields stay visible above the keyboard.

### 3.3 Card contents (top → bottom)
1. **"Welcome back"** — `MaterialTheme.typography.headlineLarge` (or 28sp), `FontWeight.ExtraBold`, `onSurface`.
2. **"Let's find a new pal for your pet"** — `bodyMedium`, `onSurfaceVariant`. 8dp gap below headline. 24dp gap before tabs.
3. **Pill-segmented tabs** ("Sign in" active, "Sign up" launcher).
4. **Email field** — filled pill, mail leading icon, peach container. Placeholder only ("rodiz@tinpet.com"-style); no floating label.
5. **Password field** — filled pill, lock leading icon, eye toggle trailing. Placeholder ("••••••••" via password masking).
6. **"Forgot password?"** — `TextButton`, right-aligned `Row`, primary color, semibold.
7. **"Sign in"** primary button — existing `PrimaryButton` (already a pill via `MaterialTheme.shapes.large` = 28dp). Coral filled.
8. **"or continue with"** divider — keep existing `OrDivider`, text just changes from "or" to "or continue with" (string update).
9. **"Continue with Google"** — keep existing outlined pill with G icon.
10. **Biometric** (conditional, when `state.biometricAvailable`) — keep existing outlined pill, label "Use biometric".

### 3.4 Pill-segmented tabs spec
- Outer track: `Surface(shape = RoundedCornerShape(28.dp), color = BrandColors.Coral.copy(alpha = 0.12f))` — coral-derived peach so it works in both light and dark mode (don't use `surfaceVariant` — disappears in dark).
- Track padding: 4dp.
- Row inside: two `Box`es with `weight(1f)`, height 48dp, clipped to `RoundedCornerShape(24.dp)`.
- Active pill: background `MaterialTheme.colorScheme.surface` (white in light mode). 1dp `tonalElevation` via a nested `Surface` (no `Modifier.shadow` — renders muddy on tinted surfaces).
- Inactive pill: transparent background. Text color `onSurfaceVariant`.
- Selection a11y: `Modifier.selectableGroup()` on the Row; each pill `Modifier.selectable(selected, role = Role.Tab)`.

### 3.5 Filled-pill text field spec
- New composable `FilledPillTextField` in `:core:ui/components/`.
- Uses M3 `TextField` (not `OutlinedTextField`) with:
  - `shape = CircleShape`
  - `colors = TextFieldDefaults.colors(focusedContainerColor = BrandColors.Coral.copy(alpha = 0.10f), unfocusedContainerColor = BrandColors.Coral.copy(alpha = 0.10f), focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, ...)`
  - `leadingIcon = { Icon(...) }`
  - `placeholder = { Text(...) }` — no `label =`, label sits as a regular `Text` above the field if needed (mockup has none on Login, so omit).
  - Height fixed via `Modifier.height(56.dp)` to match button heights.
- Two thin wrappers around it: `EmailFieldPill` (mail icon + email IME options) and `PasswordFieldPill` (lock icon + eye toggle + password masking) — keeps icon imports out of the screen.

## 4. State / behavior changes

| File | Change |
|---|---|
| `LoginUiState.kt` | Remove `emailFormExpanded: Boolean`. |
| `LoginAction.kt` | Remove `ShowEmailForm`. |
| `LoginViewModel.kt` | Remove `ShowEmailForm` handler; remove init logic that depends on `emailFormExpanded`. |
| `feature/login/presentation/src/main/res/values/strings.xml` | Remove `login_email_form_show`; change `login_or_divider` from "or" to "or continue with"; add `login_welcome_title` ("Welcome back") and `login_welcome_subtitle` ("Let's find a new pal for your pet"); keep `login_submit` text as "Sign in" (currently "Login" — also update). |
| `LoginViewModelTest.kt`, `LoginScreenHappyPathTest.kt` (if they reference the dropped state) | Remove assertions on `emailFormExpanded` / `ShowEmailForm`; ensure the form-always-visible flow still validates. |

## 5. Files to add / modify

### Add
- `feature/login/presentation/src/main/res/drawable/pet_tile_puppies.xml`
- `feature/login/presentation/src/main/res/drawable/pet_tile_cat.xml`
- `feature/login/presentation/src/main/res/drawable/pet_tile_rabbit.xml`
- `core/ui/src/main/kotlin/com/rodiz/arch2/core/ui/components/FilledPillTextField.kt` (base composable)
- `core/ui/src/main/kotlin/com/rodiz/arch2/core/ui/components/EmailFieldPill.kt` (thin wrapper)
- `core/ui/src/main/kotlin/com/rodiz/arch2/core/ui/components/PasswordFieldPill.kt` (thin wrapper)

### Modify
- `feature/login/presentation/.../screen/LoginScreen.kt` — rewrite hero, drop `BrandHeader` call, drop `AnimatedVisibility` blocks, swap `EmailField`/`PasswordField` for the new pill variants, replace `ModeTabs` with the new pill-segmented tabs, add "Welcome back" + subtitle, add hero+card overlap layout, keep `LightStatusBarIconsWhileShown` + `OrDivider` + biometric block + Google block.
- `feature/login/presentation/.../state/LoginUiState.kt` — drop `emailFormExpanded`.
- `feature/login/presentation/.../state/LoginAction.kt` — drop `ShowEmailForm`.
- `feature/login/presentation/.../viewmodel/LoginViewModel.kt` — drop the corresponding handler.
- `feature/login/presentation/src/main/res/values/strings.xml` — string updates per §4.
- `feature/login/presentation/src/test/.../LoginViewModelTest.kt` (and any screen test) — update fixtures.

### Do NOT modify
- `core/ui/components/{BrandHeader,EmailField,PasswordField,PrimaryButton,ErrorBanner}.kt` — still used by SignUp and other screens. Leave alone.
- `core/designsystem/theme/Shapes.kt` — `WaveBottomShape` stays (BrandHeader still uses it).
- Anything in `:feature:login:data` / `:domain`.

## 6. Critical design recipes (from Plan agent verdict)

These are the non-obvious choices to apply when writing the screen:

1. **Hero+card+rabbit composition** — outer `Column(Modifier.fillMaxSize().verticalScroll(...))` containing a single `Box(Modifier.fillMaxWidth())`. Inside that Box: (a) the hero `Box` pinned to TopCenter with fixed `height = 360.dp`; (b) the card `Surface` aligned to `BottomCenter` with `padding(top = 310.dp)` so it overlaps by 50dp; (c) the rabbit `Image` aligned to BottomCenter with a negative offset so it sits on the boundary. **Rabbit must be the last child of the outer Box** so it draws on top — no `Modifier.zIndex` needed.
2. **Radar rings** — `Modifier.drawBehind` on the hero Box, not a child `Canvas`. Saves a layout node.
3. **Pet tiles use `BoxWithConstraints`** so offsets scale with `maxWidth`. Hard dp offsets drift on narrow screens.
4. **Active pill lift** — 1dp `tonalElevation` on a nested `Surface` (or just rely on white-on-peach contrast). **Do not** `Modifier.shadow` on a tinted surface — renders muddy.
5. **IME** — apply `Modifier.imePadding()` to the inner card `Column` only, not the outer scroll Column (otherwise the hero jumps too).
6. **Theme colors** — pill track and field containers use `BrandColors.Coral.copy(alpha = 0.12f)` (coral-derived), not `surfaceVariant`, so dark mode renders correctly.
7. **A11y** — pet tile images get `contentDescription = null` and the hero `Box` gets `Modifier.semantics(mergeDescendants = true) { invisibleToUser() }` so TalkBack skips them. Pill tabs get `selectableGroup` + `selectable(role = Role.Tab)`.
8. **Status bar** — keep `LightStatusBarIconsWhileShown` (already correct).
9. **Landscape fallback** — accept that the hero scrolls off in landscape. Don't try to shrink/scale.

## 7. Verification

1. **Lint + build**: `JAVA_HOME=…/jbr-17.0.14 ./gradlew :feature:login:presentation:assembleDebug` then `:app:assembleDebug`.
2. **Unit tests**: `JAVA_HOME=… ./gradlew :feature:login:presentation:testDebugUnitTest` — the existing `LoginViewModelTest.kt` will need updates (see §5). All existing tests should pass after the fixture changes.
3. **Compose previews** — `LoginScreenPreviewCollapsed` should be deleted (no collapsed state anymore). Keep `LoginScreenPreviewExpanded` (rename to `LoginScreenPreview`) and `LoginScreenPreviewError`. Add `LoginScreenPreviewBiometric` to cover the biometric-available variant.
4. **Emulator**: install on `emulator-5554` and visually compare against the mockup. Specifically check:
   - Hero rings + pet tiles render at the right offsets on a 1080×2400 screen (Pixel 6 emulator).
   - Rabbit tile straddles the hero/card boundary cleanly.
   - "Welcome back" + subtitle copy + spacing matches.
   - Pill tabs swap state correctly when "Sign up" is tapped → navigates to the existing SignUp screen.
   - Filled pill fields show leading icons + peach background; password eye toggle works.
   - "Forgot password?" link is reachable + tappable.
   - Sign in submits with real credentials (`rodiz@tinpet.com`-style) and lands on the Deck.
   - Continue with Google still launches the Google sign-in flow.
   - Biometric button still renders when the device reports biometric available (force via `state.biometricAvailable = true` in preview to verify visual at least).
   - Dark mode: peach pill track and field containers still visible; coral hero unchanged.
   - IME open: focused fields stay above keyboard, hero is allowed to scroll off.
5. Capture a screenshot via `adb -s emulator-5554 exec-out screencap -p > /tmp/login-redesign.png` and surface the path (per [[feedback_post_plan_emulator]]).

## 8. Out of scope

- SignUp screen restyle. (Still uses `BrandHeader` + outlined fields; intentionally untouched.)
- Forgot password destination screen. (`LoginEvent.NavigateForgot` is still a stub — separate plan.)
- Server-side anything. (Auth flows are unchanged.)
- Pet hero photos. (Placeholder vectors ship now; real photos swapped later by replacing the 3 drawables.)
- Reusable pill text field outside Login. (SignUp still wants the outlined variant.)

## 9. Risk / rollback

- Risk: removing `emailFormExpanded` and `ShowEmailForm` may break instrumented tests. Mitigation: update fixtures as part of §5, run `:feature:login:presentation:testDebugUnitTest` before committing.
- Risk: pill text fields don't expose all the props `BrandTextField` did. Mitigation: the new wrappers are scoped to Login; SignUp keeps using the existing fields. No call-site change outside Login.
- Rollback: revert the Login screen commit. Hero/card/tab redesign is local to one file plus three new drawables and one new component family in `:core:ui` — no schema changes, no data layer changes, no Firestore rules changes. Clean to revert.

## 10. Implementation order

When the user accepts and we exit plan mode:

0. Copy this plan to `plans/login-redesign-welcome-back.md` in the repo root.
1. Add `FilledPillTextField` + `EmailFieldPill` + `PasswordFieldPill` in `:core:ui`.
2. Add the 3 pet tile vector drawables.
3. Drop `emailFormExpanded` / `ShowEmailForm` from state + VM + tests.
4. Rewrite `LoginScreen.kt` per §3 + §6. Update string resources.
5. Build, run previews, install on emulator-5554, screenshot, compare.
6. Single commit: `feat(login): welcome-back redesign with pet hero and pill fields`.

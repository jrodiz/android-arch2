# Plan — LoginScreen visual restyle (coral / topographic mockup)

## Context

The current `LoginScreen` is functional but generic (Material 3 defaults, indigo primary, `OutlinedTextField`s, centered column). The mockup the user supplied is a custom-branded login: coral/salmon header with a topographic line pattern, a curved-wave separator, an oversized bold "Sign in" title, underline-style fields with leading icons, a coral pill "Login" button, and a "Sign up" footer.

This plan applies that visual design to the existing `LoginScreen` while preserving the architecture and behavior settled in `plans/login-feature.md` (always-remember session, biometric flow, validation, error mapping, etc.).

### Confirmed decisions

- **Remember Me checkbox:** skip it. The row becomes a single right-aligned "Forgot Password?" link. Always-remember semantics stay as designed.
- **Theme scope:** update the global `Arch2Theme` to a coral brand palette. Material You dynamic color is disabled (would otherwise override coral on S+).
- **Topographic pattern:** vector drawable asset (`ic_login_topographic.xml`) under `:feature:login:presentation/res/drawable/`. Crisp at any size; easy to art-direct later.

---

## 1. Design tokens (`:core:designsystem`)

### 1.1 Color palette

Replace the current indigo-based palette in `core/designsystem/.../theme/Color.kt` with a coral brand scheme:

| Role | Light | Dark |
|---|---|---|
| `primary` | `#E97A7A` (coral 500) | `#FFB3AC` |
| `onPrimary` | `#FFFFFF` | `#5C1212` |
| `primaryContainer` | `#FFDAD5` | `#7D2A2A` |
| `onPrimaryContainer` | `#410001` | `#FFDAD5` |
| `secondary` | `#775653` (warm taupe) | `#E7BDB8` |
| `surface` | `#FFFBFA` | `#1A1110` |
| `onSurface` | `#211B1A` | `#F1DEDB` |
| `surfaceVariant` | `#F5DDDA` | `#534340` |
| `onSurfaceVariant` | `#534340` | `#D8C2BE` |
| `outline` | `#857370` | `#A08C89` |
| `background` | `#FFFBFA` | `#1A1110` |
| `error` | `#BA1A1A` | `#FFB4AB` |

Add named brand color extensions for the topographic header (`BrandCoral = #F08A8A`, `BrandCoralDeep = #E97A7A`) since the colored hero needs a fixed value that doesn't depend on dark/light scheme.

### 1.2 Disable dynamic color

`Theme.kt`: change `dynamicColor` default to `false` and remove the dynamic branch. Keep the parameter for future opt-in but route to the static `LightScheme`/`DarkScheme` by default.

### 1.3 Typography

Add a tuned `Typography` instance in a new `theme/Type.kt`:

- `displayMedium` — used for "Sign in": `fontWeight = ExtraBold`, `fontSize = 44.sp`, `lineHeight = 52.sp`, `letterSpacing = (-1).sp`.
- Keep other text styles at M3 defaults.

Wire it into `MaterialTheme(typography = AppTypography, ...)` in `Arch2Theme`.

### 1.4 Shapes

Add `theme/Shapes.kt`:

- `WaveBottomShape` — a `Shape` whose `createOutline` draws a rounded-rectangle top with the bottom edge replaced by a smooth quadratic bezier curve (`size.height` peak ~12% above the bottom). Used to clip the coral header box so the bottom is the wavy divider in the mockup.
- `PillButtonShape` = `RoundedCornerShape(28.dp)`.

Wire into `MaterialTheme(shapes = AppShapes, ...)`.

---

## 2. Reusable components (`:core:ui`)

### 2.1 New: `BrandTextField`

`core/ui/src/main/kotlin/.../components/BrandTextField.kt` — the underline-style field used by the mockup:

- Uses `TextField` (not `OutlinedTextField`) so the indicator (underline) is the visual emphasis.
- `colors = TextFieldDefaults.colors(...)` with `unfocusedContainerColor = Color.Transparent`, `focusedContainerColor = Color.Transparent`, `unfocusedIndicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)`, `focusedIndicatorColor = MaterialTheme.colorScheme.primary`, `disabledIndicatorColor = ...`.
- `leadingIcon` slot — receives an `ImageVector` (the email/lock icons in the mockup).
- `trailingIcon` slot — used by the password variant for the eye toggle.
- `label` slot — rendered **above** the field as a separate bold `Text` (not as the floating label) to match the mockup. The composable renders a `Column { Text(label, …); TextField(…) }`.
- `placeholder` slot — `demo@email.com`, `enter your password` etc.
- `errorMessage: String?` — rendered as a small caption below when non-null.

Add `EmailField` and `PasswordField` thin wrappers around `BrandTextField` so existing call sites keep working with no parameter changes (just visually upgraded). The legacy `OutlinedTextField`-based bodies of those files are replaced.

### 2.2 Update: `PrimaryButton`

`core/ui/.../components/PrimaryButton.kt`:

- Apply `shape = MaterialTheme.shapes.large` (which `AppShapes` sets to `PillButtonShape`).
- Default `Modifier.height(56.dp)` so the pill matches the mockup proportions.
- Loading indicator stays as-is.

### 2.3 New: `BrandHeader`

`core/ui/.../components/BrandHeader.kt` — the coral hero block:

```
@Composable
fun BrandHeader(
    @DrawableRes patternRes: Int,
    modifier: Modifier = Modifier,
    heightFraction: Float = 0.42f,
    content: @Composable BoxScope.() -> Unit = {},
)
```

Renders a `Box` filling the parent up to `heightFraction` of the height, background = `BrandCoral`, clipped by `WaveBottomShape`. Inside it: an `Image` painted from `patternRes` with `alpha = 0.25f` and `contentScale = ContentScale.Crop` to give the topographic look. The `content` slot lets the screen overlay anything (in this case nothing — the title sits below the wave).

Kept in `:core:ui` (not `:core:designsystem`) because it composes — `:core:designsystem` is for tokens only.

---

## 3. Login assets (`:feature:login:presentation/res`)

- `res/drawable/ic_login_topographic.xml` — a vector drawable approximating the contour-line pattern: roughly 8–12 nested closed paths with bezier curves, strokeColor=white, strokeAlpha=0.4, fillColor=transparent, stroke width ~1.2dp. Hand-tuned to evoke topography without being a literal map.
- `res/drawable/ic_field_email.xml` — outlined mail icon (vector). Could also reuse `Icons.Outlined.Email` from `material-icons-extended`; we already depend on it, so prefer the existing icon to avoid asset bloat.
- `res/drawable/ic_field_lock.xml` — same logic: reuse `Icons.Outlined.LockOutline`.

(Net new asset = the topographic pattern only. Field icons come from the material-icons-extended dependency already in `:core:ui`.)

---

## 4. LoginScreen layout (`:feature:login:presentation`)

Replace the body of `screen/LoginScreen.kt`:

```
Box (fillMaxSize)
├── BrandHeader (top, heightFraction = 0.42f, pattern asset)
└── Column (zIndex above header, top-aligned with statusBars + 0.42f offset)
    ├── Spacer matching header height - 24.dp (lets title overlap the curve slightly)
    ├── Text "Sign in"  (displayMedium, ExtraBold, onSurface)
    │   └── Box below: 32.dp wide, 3.dp tall, BrandCoralDeep, RoundedCornerShape(2.dp)   ← accent line
    ├── Spacer(28.dp)
    ├── EmailField   (uses BrandTextField under the hood, leadingIcon = Email)
    ├── Spacer(20.dp)
    ├── PasswordField (BrandTextField + lock leading + visibility toggle trailing)
    ├── Spacer(16.dp)
    ├── Row (fillMaxWidth, end-aligned)
    │   └── TextButton "Forgot Password?" → onAction(ForgotPasswordTapped)
    ├── Spacer(40.dp)
    ├── PrimaryButton "Login"   (full width, pill, height 56)
    ├── if (state.biometricAvailable) Spacer + OutlinedButton "Sign in with biometric"
    ├── Spacer(20.dp)
    └── Row (fillMaxWidth, center)
        ├── Text "Don't have an Account?"
        ├── Spacer(4.dp)
        └── TextButton "Sign up" (primary color) → onAction(CreateAccountTapped)
```

- The whole screen sits on top of the system status bar (edge-to-edge — `MainActivity` already calls `enableEdgeToEdge()`). The coral header naturally bleeds under the status bar. Add `WindowInsets.systemBars.asPaddingValues()` to the form column's horizontal/bottom padding only — the top is intentionally untouched so the wave reaches the screen top.
- Form column horizontal padding: 28.dp.
- Error banner (existing `ErrorBanner`) still renders inside the form column when `state.transientError != null`, between the password field and the Forgot row. Style update: round the corners (`RoundedCornerShape(12.dp)`), keep semantics.

---

## 5. Strings (`res/values/strings.xml`)

Updates / additions:

| Key | Value |
|---|---|
| `login_email_placeholder` | `demo@email.com` |
| `login_password_placeholder` | `enter your password` |
| `login_submit` | change `Sign in` → `Login` (matches button text in mockup) |
| `login_no_account_prefix` | `Don't have an Account?` |
| `login_sign_up` | `Sign up` |

The existing `login_title`, `login_email_label`, `login_password_label`, `login_forgot`, error / validation strings are unchanged.

---

## 6. Files to modify / add

```
core/designsystem/src/main/kotlin/.../theme/Color.kt        (rewrite — coral scheme + brand extras)
core/designsystem/src/main/kotlin/.../theme/Theme.kt        (disable dynamic color, wire Typography + Shapes)
core/designsystem/src/main/kotlin/.../theme/Type.kt         (new — displayMedium override)
core/designsystem/src/main/kotlin/.../theme/Shapes.kt       (new — WaveBottomShape, PillButtonShape, AppShapes)

core/ui/src/main/kotlin/.../components/BrandTextField.kt    (new — underline style, leading icon, external label)
core/ui/src/main/kotlin/.../components/BrandHeader.kt       (new — coral box clipped by wave + pattern image)
core/ui/src/main/kotlin/.../components/EmailField.kt        (rewrite as BrandTextField wrapper)
core/ui/src/main/kotlin/.../components/PasswordField.kt     (rewrite as BrandTextField wrapper)
core/ui/src/main/kotlin/.../components/PrimaryButton.kt     (pill shape + 56dp height)
core/ui/src/main/kotlin/.../components/ErrorBanner.kt       (rounded corners; minor)

feature/login/presentation/src/main/kotlin/.../screen/LoginScreen.kt   (rewrite layout)
feature/login/presentation/src/main/res/drawable/ic_login_topographic.xml   (new vector)
feature/login/presentation/src/main/res/values/strings.xml   (placeholders + footer strings)
```

No changes to the ViewModel, UseCases, Repository, or any module's `build.gradle.kts` — purely a UI/theme update.

---

## 7. Verification

1. `./gradlew assembleDebug` — must still succeed.
2. `./gradlew :feature:login:presentation:test` — `LoginViewModelTest` is untouched and must stay green (no behavioral changes).
3. `./gradlew :feature:login:presentation:connectedDebugAndroidTest` — `LoginScreenHappyPathTest` references test tags `email_field`, `password_field`, `login_submit`. **Preserve these test tags** on the new field/button composables so the UI test keeps passing.
4. Manual visual diff against the mockup on an emulator:
   - Coral header with topographic pattern + wave divider visible.
   - "Sign in" bold title with the small coral accent underline.
   - Underline-style fields with mail / lock leading icons.
   - Eye toggle on the password field.
   - Right-aligned "Forgot Password?".
   - Full-width coral pill "Login" button.
   - "Don't have an Account? Sign up" centered footer.
   - Status bar icons readable against coral (status bar should remain transparent — `enableEdgeToEdge` keeps the default light icons in dark mode and dark icons in light mode; if visibility is an issue, override status bar icon color to light in `MainActivity`).
5. Rotate the device — form state should still survive (covered by the unchanged VM).

---

## 8. Out of scope (intentional)

- Animations / motion (pattern shimmer, button press, field focus). Static visual parity first.
- Dark-mode tuning of the topographic pattern asset (it's drawn with white-on-coral; in dark mode the coral surface variant still reads OK). Revisit if it looks off.
- Re-skinning the `HomeScreen` and stub screens. They remain on Material 3 defaults; will get a separate visual pass in a future plan.
- A `BrandCheckbox` component — not needed since the Remember Me row is dropped.
- Icon asset audit / replacement — we keep using `material-icons-extended` and don't add custom vectors beyond the topographic pattern.

---

## 9. Risks

- **Status bar contrast.** With edge-to-edge, the system status bar overlays the coral header. On light mode the default dark status-bar icons might disappear against coral. Mitigation: in `MainActivity`, set `WindowInsetsControllerCompat(window, decorView).isAppearanceLightStatusBars = false` so the icons are light, then make sure the dark-mode behavior is the inverse. Will verify and tweak during the manual check.
- **`OutlinedTextField` → `TextField` swap** changes `supportingText` rendering slightly (less padding). The validation message rendering may shift; will inspect during the manual check and adjust if needed.
- **Vector topographic pattern fidelity.** A hand-drawn approximation won't match the exact mockup pixel-for-pixel. Will produce a close visual stand-in; user can replace the SVG later with a designer-supplied one without code changes.

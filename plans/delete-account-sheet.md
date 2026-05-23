# Plan — Delete Account confirmation as a bottom sheet

> **NOTE on plan location:** project convention ([[feedback_plans_location]]) is to save plans under `plans/` in the Arch2.0 repo. Plan mode pins the working copy here. **Step 0 of implementation: copy this file to `plans/delete-account-sheet.md`** so it gets versioned with the code.

## 1. Context

Today the Delete Account flow is a **full-screen route**:

- Privacy → "Delete account" row → `navigator.goTo(SettingsAccount)` (see `feature/settings/presentation/.../SettingsNavModule.kt:51`).
- `AccountScreen.kt` renders the user's email, a "Delete account" red button, an in-screen `AlertDialog` (`DeleteAccountModal`) for the typed-`DELETE` confirmation, and — when a deletion is pending — a red banner with a days-remaining counter and a "Cancel deletion" button. It owns three responsibilities (email display, confirm flow, grace-state UI) that are awkward at full-screen scale and inconsistent with the rest of the Privacy surface.

The mock replaces it with a **dedicated `ModalBottomSheet`** opened in place from Privacy:

- Drag handle + coral icon tile + bold "Delete your account?" headline + body copy with "**30 days**" emphasis.
- A peach summary card titled "THIS WILL REMOVE" listing three rows: pets ("N pets — Name1, Name2, …"), matches ("N matches and all chats"), and a static "Your profile and photos" row.
- A "TYPE DELETE TO CONFIRM" eyebrow above a peach-bordered pill text field; the red "Delete account" CTA is disabled until the user types `DELETE`.
- A "Keep my account" text-link dismisses the sheet.

**Confirmed decisions (this turn):**

1. **AccountScreen + `SettingsAccount` route are deleted.** A single sheet handles both states — confirm UI when no deletion is pending, and a cancel-deletion variant when grace is active. Privacy row optionally surfaces "Scheduled · N days" when grace is active so the user has a visible hook.
2. **Summary counts are read once on sheet open** (`.first()` on the existing `ObserveMyPetsUseCase` and `ObserveInboxUseCase` Flows). No live listeners attached for the lifetime of the sheet.

**Non-goals:** changing the underlying delete UseCases, the Firestore document layout, the 30-day grace policy, the sign-out behavior on success, the Privacy screen's other rows, or any other Settings surface.

## 2. Visual spec

### 2.1 Sheet container
- `ModalBottomSheet` with `rememberModalBottomSheetState(skipPartiallyExpanded = true)` — matches `AvatarSourceSheet.kt` and `ReportSheet` already in the repo.
- `shape = MaterialTheme.shapes.large` (default).
- `containerColor = MaterialTheme.colorScheme.surface` (cream/off-white from `LightScheme`).
- Default Material drag handle is fine — the mock's peach handle tint is the default surface-variant rendering; no override.
- `Modifier.padding(horizontal = 24.dp).padding(bottom = 16.dp)` on the content column. `imePadding()` after `verticalScroll(rememberScrollState())` so the keyboard pushes content up when the DELETE field focuses.

### 2.2 Header tile + headline (confirm state)
- 64.dp `RoundedCornerShape(20.dp)` tile, `BrandColors.CoralLight.copy(alpha = 0.35f)` background, centered horizontally with 16.dp top spacing.
- Inside: `Icons.Outlined.Block` icon, 32.dp, tint `BrandColors.CoralDeep` (matches the red-on-pink coral pattern already used on the existing Privacy "Delete account" row).
- Spacer 16.dp.
- Headline: "Delete your account?" — `headlineSmall.copy(fontWeight = FontWeight.ExtraBold)`, `onSurface`, centered, `semantics { heading() }`.
- Spacer 8.dp.
- Body: "We'll keep your data for **30 days**. Sign in again any time before then to restore everything." Render via `buildAnnotatedString` so "30 days" gets `SpanStyle(fontWeight = FontWeight.Bold)`. Style `bodyMedium`, `onSurfaceVariant`, centered.

### 2.3 "THIS WILL REMOVE" summary card
- `Surface(shape = RoundedCornerShape(16.dp), color = BrandColors.PeachWarmLight)` (reuse the token added in the Filters redesign).
- Inner column padding 16.dp.
- Eyebrow: "THIS WILL REMOVE" — `labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp)`, `BrandColors.PeachWarmDeep.copy(alpha = 0.85f)`, uppercase.
- Spacer 8.dp.
- 3 rows in a `Column(verticalArrangement = Arrangement.spacedBy(8.dp))`. Each row: `Row(verticalAlignment = CenterVertically, horizontalArrangement = spacedBy(10.dp))` → 20.dp icon tinted `BrandColors.CoralDeep` + `bodyMedium` text in `onSurface`.
  - Row 1 — `Icons.Outlined.Pets` (or the existing paw used elsewhere) + `pluralResource(R.plurals.delete_summary_pets, n, n, names.joinToString(", "))`. Truncate the names string at 60 chars with an ellipsis if needed (avoid the row wrapping past 2 lines).
  - Row 2 — `Icons.Outlined.ChatBubbleOutline` + `pluralResource(R.plurals.delete_summary_matches, n, n)`.
  - Row 3 — `Icons.Outlined.Person` + static `R.string.delete_summary_profile` ("Your profile and photos").
- Skip rows whose count is `0` (don't render "0 pets — " or "0 matches"). If both pet and match counts are zero, still render at least the static profile row.

### 2.4 Typed-DELETE field
- Eyebrow: "TYPE DELETE TO CONFIRM" — same labelMedium uppercase pattern, `onSurfaceVariant`.
- Field: reuse `FilledPillTextField` from `core/ui/components/`, passing `containerColor = BrandColors.PeachWarmLight` and `shadowElevation = 0.dp`. Placeholder `DELETE` rendered in `BrandColors.CoralDeep`, `FontWeight.Bold`, all-caps — no special placeholder API needed because `FilledPillTextField`'s `placeholder` parameter already accepts a String which the underlying `TextField` renders with `onSurfaceVariant`. To get the bold-coral placeholder shown in the mock, pass a custom `placeholder = { Text("DELETE", ...) }` overload **if** `FilledPillTextField` accepts a `@Composable` placeholder slot today; if not, fall back to setting `value`'s text style and skipping the styled placeholder. *Verify the placeholder API at wire time.*
- `KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done)`. On `KeyboardActions(onDone = { if (canSubmit) onConfirm() })`.

### 2.5 Primary CTA + dismiss
- `PrimaryButton(text = stringResource(R.string.delete_account_cta), enabled = state.typed == "DELETE" && !state.isSubmitting, loading = state.isSubmitting, onClick = { onConfirm() })`. The existing `PrimaryButton` uses `BrandColors.Coral`/`CoralDeep`; the mock's CTA reads as a darker brick red. Pass an optional `containerColor` if `PrimaryButton` supports it today; otherwise add a `containerColor` parameter (default unchanged) so the Delete sheet can use `Color(0xFFB23A3A)` — a single brand-internal token. **If the simpler path is acceptable, pass `BrandColors.CoralDeep` and accept the slightly less-saturated red.** Default the plan to that simpler path and call out the deeper red as a polish item.
- Spacer 8.dp.
- `TextButton(onClick = { onDismiss() }) { Text("Keep my account", style = titleSmall, fontWeight = FontWeight.Bold, color = onSurface) }`.

### 2.6 Grace-state variant
When `state.pendingDeletion != null`, the sheet renders a different content body in place of §2.2–§2.5:
- Same coral-tile + icon header (but icon = `Icons.Outlined.Schedule`).
- Headline: "Deletion scheduled".
- Body: "Your account will be permanently deleted in **N days**. Sign in any time before then to keep it." (`N` from `state.daysRemaining`).
- No summary card, no DELETE field.
- Primary CTA: "Cancel deletion" using the same `PrimaryButton` (this one stays coral — destructive verb isn't needed because it's the recovery action). `enabled = !state.isSubmitting`.
- Dismiss link: "Close" (or reuse "Keep my account" — pick the more familiar one). Default to **"Close"** since the user has already chosen to delete; "Keep my account" reads wrong here.

The sheet auto-switches between variants based on `state.pendingDeletion` — no separate route or sheet instance.

### 2.7 Privacy row affordance
- In `PrivacyScreen.kt`, the "Delete account" row in the Danger Zone card stays. When `state.pendingDeletion != null`, append a small coral pill ("Scheduled · N days") to the row's trailing content (next to the chevron). This is purely cosmetic — the tap target still opens the sheet.

## 3. State / behavior

A new VM, hoisted at the Privacy route so the sheet shares lifecycle with the Privacy screen.

```kotlin
@HiltViewModel
class DeleteAccountSheetViewModel @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val observePendingDeletion: ObservePendingDeletionUseCase,
    private val requestDeletion: RequestAccountDeletionUseCase,
    private val cancelDeletion: CancelAccountDeletionUseCase,
    private val observeMyPets: ObserveMyPetsUseCase,
    private val observeInbox: ObserveInboxUseCase,
) : ViewModel() {

    data class UiState(
        val pendingDeletion: AccountDeletion? = null,
        val daysRemaining: Long = 0,
        val petNames: List<String> = emptyList(),
        val matchCount: Int = 0,
        val typed: String = "",
        val isSubmitting: Boolean = false,
        val errorRes: Int? = null,
        val completed: Boolean = false,
    ) {
        val canSubmit: Boolean get() = typed == "DELETE" && !isSubmitting
    }

    val state: StateFlow<UiState> = ... // pendingDeletion + daysRemaining wired to observePendingDeletion()

    fun onSheetOpen() {
        viewModelScope.launch {
            val uid = sessionRepository.observe().firstOrNull()?.userId ?: return@launch
            val pets = observeMyPets().first()
            val inbox = observeInbox(uid).first()
            _state.update {
                it.copy(
                    petNames = pets.map(Pet::name),
                    matchCount = inbox.newMatches.size + inbox.conversations.size,
                )
            }
        }
    }

    fun onTypedChanged(value: String) = _state.update { it.copy(typed = value.uppercase()) }
    fun onConfirmDelete() { /* mirrors AccountViewModel.requestDeletion() */ }
    fun onCancelDeletion() { /* mirrors AccountViewModel.cancelDeletion() */ }
    fun onDismiss() = _state.update { it.copy(typed = "", errorRes = null) }
    fun onErrorShown() = _state.update { it.copy(errorRes = null) }
}
```

- `state.completed = true` triggers the host (Privacy route) to call `navigator.replaceAll(LoginHome)` — mirrors `AccountRoute`'s `onDeleted` callback today (`SettingsNavModule.kt:60`).
- `state.errorRes` is a string-res `Int` (not raw text) so the route can surface it via a snackbar with the proper `stringResource(...)` resolution.
- All deletion methods reuse the exact UseCase calls that `AccountViewModel` does today (lines 64 and 77 of `AccountViewModel.kt`) — no behavior change.

## 4. Files to add / modify / delete

### Add
- `feature/settings/presentation/src/main/kotlin/com/rodiz/arch2/feature/settings/presentation/DeleteAccountSheet.kt` — the new composable + private subcomposables (`ConfirmContent`, `GraceContent`, `SummaryCard`, `SummaryRow`). ~280 LOC.
- `feature/settings/presentation/src/main/kotlin/com/rodiz/arch2/feature/settings/presentation/DeleteAccountSheetViewModel.kt` — Hilt VM described in §3. ~120 LOC.

### Modify
- `feature/settings/presentation/src/main/kotlin/com/rodiz/arch2/feature/settings/presentation/PrivacyScreen.kt`:
  - Inject `DeleteAccountSheetViewModel` via `hiltViewModel()` at `PrivacyRoute`.
  - Hold sheet open state (`var sheetOpen by rememberSaveable { mutableStateOf(false) }`).
  - On `onOpenDeleteAccount` (currently the route prop), set `sheetOpen = true` and call `viewModel.onSheetOpen()`.
  - When `sheetOpen`, render `DeleteAccountSheet(state = vmState, onTypedChanged = ..., onConfirm = ..., onCancel = ..., onDismiss = { sheetOpen = false; viewModel.onDismiss() })`.
  - When `vmState.completed`, call `onCompleted()` and dismiss.
  - In `DangerZone`'s "Delete account" row, when `vmState.pendingDeletion != null`, render a trailing coral pill "Scheduled · N days".
  - Use `LaunchedEffect(vmState.errorRes)` to show errors via the existing `SnackbarHostState`.
- `feature/settings/presentation/src/main/kotlin/com/rodiz/arch2/feature/settings/presentation/SettingsNavModule.kt`:
  - Change `onOpenDeleteAccount = { navigator.goTo(SettingsAccount) }` (line 51) to a no-op or a new `onCompleted = { navigator.replaceAll(LoginHome) }` if we hoist via the route.
  - Remove the `entry<SettingsAccount> { AccountRoute(...) }` block (lines 57–62).
- `feature/settings/nav/src/main/kotlin/com/rodiz/arch2/feature/settings/nav/Routes.kt`:
  - Remove `data object SettingsAccount` (line 18).
- `feature/settings/presentation/src/main/res/values/strings.xml`:
  - Add: `delete_sheet_headline = "Delete your account?"`, `delete_sheet_body_30_days = "30 days"`, `delete_sheet_body_format = "We'll keep your data for %1$s. Sign in again any time before then to restore everything."`, `delete_sheet_will_remove = "This will remove"`, `delete_sheet_profile_row = "Your profile and photos"`, `delete_sheet_confirm_eyebrow = "Type DELETE to confirm"`, `delete_sheet_placeholder = "DELETE"`, `delete_account_cta = "Delete account"`, `delete_sheet_keep = "Keep my account"`, `delete_sheet_close = "Close"`, `delete_sheet_grace_headline = "Deletion scheduled"`, `delete_sheet_grace_body_format = "Your account will be permanently deleted in %1$s. Sign in any time before then to keep it."`, `delete_sheet_grace_days_format = "%1$d days"`, `delete_sheet_cancel_cta = "Cancel deletion"`, `delete_sheet_pill_format = "Scheduled · %1$d days"`.
  - Add plurals:
    - `<plurals name="delete_summary_pets">` — one: "%1$d pet — %2$s"; other: "%1$d pets — %2$s".
    - `<plurals name="delete_summary_matches">` — one: "%1$d match and all chats"; other: "%1$d matches and all chats".
- `feature/settings/presentation/build.gradle.kts`:
  - Add `implementation(project(":feature:pet:domain"))` and `implementation(project(":feature:match:domain"))` if not already present (verify before editing).

### Delete
- `feature/settings/presentation/src/main/kotlin/com/rodiz/arch2/feature/settings/presentation/AccountScreen.kt` (entire file).
- `feature/settings/presentation/src/main/kotlin/com/rodiz/arch2/feature/settings/presentation/AccountViewModel.kt` (entire file).
- Any `AccountRoute(...)` reference (only one, in `SettingsNavModule.kt`).

### Do NOT modify
- `feature/settings/domain/.../SettingsUseCases.kt` — three UseCases reused as-is.
- `feature/settings/domain/.../AccountDeletion.kt` model.
- `feature/settings/data/.../FirestoreAccountDeletionRepository.kt` — Firestore layout unchanged.
- `feature/login/nav/.../LoginHome` route — same destination for post-deletion sign-out.
- `core/ui/components/PrimaryButton.kt` unless the deeper-red CTA is taken on (see §2.5).

## 5. Critical recipes

1. **`ModalBottomSheet` recipe** (mirror `AvatarSourceSheet.kt:36-95`): `rememberModalBottomSheetState(skipPartiallyExpanded = true)`, `onDismissRequest = onDismiss`, default shape + drag handle. Wrap content in a `Column` with `verticalScroll(rememberScrollState()).imePadding()` because the DELETE field can summon the keyboard.
2. **`ImeAction.Done` + `KeyboardCapitalization.Characters`** on the DELETE field so the user can type lowercase and the VM still validates against `"DELETE"`. Normalize via `value.uppercase()` in `onTypedChanged`.
3. **`buildAnnotatedString` for the bold "30 days"** — don't ship two separate `Text` composables; one annotated string keeps wrapping correct.
4. **`pluralResource` / `pluralStringResource`** for "N pets" and "N matches" — required for clean i18n. Don't `if (n == 1) "1 pet" else "$n pets"`.
5. **Hoist VM at the route**, not at the sheet composable, so the sheet's content recomposes during the open/close animation without losing state.
6. **Don't preload counts in `init`** — call `onSheetOpen()` on first open so Firestore reads don't happen for users who never tap Delete.
7. **One-shot counts use `Flow.first()`** (not `.firstOrNull()` — both are fine, but `first()` matches the rest of the codebase's style). Wrap in a `try/catch` so a Firestore error doesn't kill the sheet; on failure, render the rows with placeholder counts and log the throwable.
8. **`Scaffold(snackbarHost = ...)` on Privacy already exists** — reuse it for `errorRes` instead of giving the sheet its own host. The sheet sits above the Privacy `Scaffold`, but the snackbar from the parent still surfaces when the sheet dismisses.

## 6. Verification

1. **Build:**
   ```bash
   JAVA_HOME=/Users/jrodiz/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home ./gradlew :app:installDebug
   ```
2. **Emulator (`emulator-5556`):**
   - Sign in. Navigate Settings → Privacy.
   - Tap "Delete account" → sheet animates up. Header + icon + "Delete your account?" + the 30-day body render.
   - Summary card shows N pets (with names truncated past 60 chars), N matches, profile row. If account has 0 pets and 0 matches, only the profile row renders.
   - Field starts empty; CTA disabled. Type "delete" lowercase → field shows "DELETE" upper; CTA enables. Tap → loading spinner → sheet dismisses → `LoginHome` reached.
   - Re-sign in within the grace window: open Delete sheet again. It renders the grace variant with "N days remaining" + "Cancel deletion" + "Close". Tap "Cancel deletion" → spinner → sheet dismisses → Privacy row pill ("Scheduled · N days") disappears.
   - Privacy row pill renders when grace is active and hides when canceled.
   - Open sheet, tap outside / drag down → sheet dismisses without side effects.
   - Toggle airplane mode and try to confirm → error snackbar appears on Privacy after sheet dismisses.
3. **Screenshot** the confirm variant to `/tmp/delete-sheet-confirm.png` and the grace variant to `/tmp/delete-sheet-grace.png` via `adb -s emulator-5556 exec-out screencap -p`.
4. **Confirm AccountScreen is gone:** `grep -r AccountScreen feature/settings/` returns nothing; `grep -r SettingsAccount` returns nothing outside route deletion diffs.

## 7. Out of scope

- Restyling the rest of the Privacy screen.
- Changing the deletion UseCases, Firestore layout, or 30-day policy.
- A separate "Why are you leaving?" survey step.
- A toggle on the sheet to choose immediate vs. graced deletion.
- Localization of the new strings beyond English.
- Tests for the new VM (no existing VM tests in `:feature:settings:presentation`; if test scaffolding exists, add a simple "canSubmit gates on typed=='DELETE'" unit test — but that's optional polish).

## 8. Risk / rollback

- **Risk:** removing `SettingsAccount` breaks deep links if any exist. Mitigation: grep `tinpet://account` and any `goTo(SettingsAccount)` callers across the repo — only `SettingsNavModule.kt:51` should match. Confirm during implementation.
- **Risk:** users mid-flow on the existing `AccountScreen` when they update will see the old route briefly missing. Mitigation: low — no production users yet (debug build only). Acceptable.
- **Risk:** loading pet/match counts via `.first()` blocks the sheet content for one Firestore round-trip. Mitigation: render the card with placeholder dashes ("— pets · — matches") for the first ~200ms while the suspend resolves; replace on emission. Or just render the card empty for the first frame and let the values fill in — both are fine.
- **Rollback:** revert the single `feat(privacy): delete account confirmation as a bottom sheet` commit. The Firestore layout is untouched, so existing graced accounts continue to work after rollback.

## 9. Implementation order

0. Copy this plan to `plans/delete-account-sheet.md`.
1. Verify `feature/settings/presentation/build.gradle.kts` has `:feature:pet:domain` + `:feature:match:domain`. Add what's missing.
2. Add the new strings + plurals to `strings.xml`.
3. Write `DeleteAccountSheetViewModel`. Build (`:app:assembleDebug`) to confirm Hilt graph.
4. Write `DeleteAccountSheet` composable with previews for [confirm, empty counts], [confirm, 2 pets + 14 matches], [grace, 27 days].
5. Modify `PrivacyScreen.kt` + `SettingsNavModule.kt` to host the sheet and drop the `SettingsAccount` entry.
6. Delete `AccountScreen.kt`, `AccountViewModel.kt`, and the `SettingsAccount` route declaration.
7. Build + install. Walk both states (confirm + grace) on emulator-5556, screenshot both.
8. Single commit: `feat(privacy): delete account confirmation as a bottom sheet`. Local only, no push, no `Co-Authored-By`.

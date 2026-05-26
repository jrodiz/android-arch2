# Testing the TinPet dev project

Everything in this doc points at the **dev** Firebase project (`arch2-cac87`)
running under your personal account. Don't re-use these credentials, secrets,
or URLs in any production-aligned environment.

## Test accounts

The `seedTestData` Cloud Function provisions five owners + ten pets so you
can sign in as any of them and exercise multi-account flows (matching,
chatting, blocking, reporting, filtering) without leaving the emulator.

**Shared password for every account:** `TinPetTest2026!`

| Email | Owner | Pets | Notes |
|---|---|---|---|
| `lena@tinpet.dev` | Lena | **Mochi** (Shiba Inu, 3 yr) · **Sprout** (Holland Lop rabbit, 1 yr) | Midtown, vet tech, playdate + friendship |
| `marcus@tinpet.dev` | Marcus | **Otto** (Pug, 5 yr) · **Pumpkin** (Maine Coon cat, 2 yr) | Midtown, pug + cat |
| `ayaan@tinpet.dev` | Ayaan | **Biscuit** (Corgi, 1 yr) | Queens, single pet, playdate-only |
| `sofia@tinpet.dev` | Sofia | **Cloud** (British Shorthair cat, 4 yr, **Adoption**) · **Pearl** (Mini Rex rabbit, 2 yr) · **Bandit** (Ferret, 3 yr) | Three pets, mixed intents — good for testing the rail |
| `noor@tinpet.dev` | Noor | **Rex** (Golden Retriever, 7 yr) · **Saffron** (Syrian hamster, 5 yr) | Older pets, mixed species |

All locations are scattered around midtown Manhattan so every pair falls
inside the default 25 km filter. Photos are public Unsplash CDN URLs (no
Firebase Storage uploads) so re-seeds don't accumulate orphaned files.

## Common test flows

1. **Sign in as two owners on two devices/emulators** — both can swipe on
   the same deck and trigger a real mutual-like → match → chat round-trip.
   The `onLikeCreate` function deploys the match doc with both `petAId` and
   `petBId` populated; the inbox and chat header should both read
   `"Owner & Pet"` (e.g. "Sofia & Cloud").
2. **Filter regression** — sign in as Lena, open Filters, toggle only
   `Rabbits` + `Friendship`. Deck should narrow to Sprout / Pearl. The
   Apply CTA suffix should drop to a small pet count.
3. **Block + unblock** — from any chat, tap the overflow → Block. The other
   participant disappears from your deck. Settings → Privacy → Blocked
   owners shows the row; tap Unblock to restore.
4. **Delete account grace** — Settings → Privacy → Delete account → type
   `DELETE` → publish. Sign back in within 30 days and the account is
   restored; after 30 days `purgeDeletedAccounts` (scheduled, every 24 h)
   wipes everything.
5. **Data export** — Privacy → Download my data. Snackbar "Generating…"
   then "Your data export is ready" with an Open action. The JSON contains
   profile + pets + matches + messages.

## Resetting the test set

Re-running the seed function is idempotent — it resets each account's
password and wipes their pets before re-creating:

```bash
curl "https://us-central1-arch2-cac87.cloudfunctions.net/seedTestData?secret=tinpet-seed-2026-05-25"
```

Use this when you've mangled state via live testing and want a clean slate.

## Inspecting Firestore

If the UI looks wrong but the data is unclear, two HTTPS endpoints help:

- **Match inspector** — dump every match doc with participant emails
  resolved. Useful when the inbox is unexpectedly empty / extra.
  ```bash
  curl "https://us-central1-arch2-cac87.cloudfunctions.net/inspectMatches?secret=tinpet-inspect-2026-05-25"
  ```
- **Match-pet backfill** — re-runs the `petAId`/`petBId` resolver against
  every match. Idempotent; reports `alreadyComplete` vs `updated` vs
  `failed` counts.
  ```bash
  curl "https://us-central1-arch2-cac87.cloudfunctions.net/backfillMatchPetIds?secret=pet-id-backfill-2026-05-23"
  ```

For everything else, the Firebase Console
([overview](https://console.firebase.google.com/project/arch2-cac87/overview))
is the source of truth — collections live at `/owners`, `/pets`, `/matches`,
`/likes`, `/passes`, `/blocks`, `/dataExports`, `/fcmTokens`,
`/accountDeletions`.

## Logs

Tail Cloud Function logs from any terminal:

```bash
npx firebase-tools functions:log --lines 50
# or per-function:
npx firebase-tools functions:log --only onLikeCreate --lines 20
```

Logcat for the Android app on the emulator:

```bash
adb -s emulator-5556 logcat -d -t 200 | grep -iE 'TinPet|FATAL|ANR'
```

## Standing emulator + JDK

- **Emulator**: `emulator-5556` (1080×2400 phone profile)
- **JDK** (must be set for every Gradle invocation):
  ```bash
  JAVA_HOME=/Users/jrodiz/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home
  ```
- **Install + launch**:
  ```bash
  JAVA_HOME=... ./gradlew :app:installDebug && \
    adb -s emulator-5556 shell am start -n com.rodiz.arch2.debug/com.rodiz.arch2.MainActivity
  ```

## CI

GitHub Actions runs on every push to `master` and every PR targeting `master`
(`.github/workflows/ci.yml`):

- **Android job** — `./gradlew ktlintCheck testDebugUnitTest :app:assembleDebug`.
  ktlint runs first (cheap, fails fast); run `./gradlew ktlintFormat`
  locally to auto-fix style violations. Rule overrides live in the
  `.editorconfig` at the repo root — see the inline comments there for
  which formatters we've muted and why. Unit test reports are uploaded
  as an artifact on every run (success or fail) so you can confirm
  tests actually executed — open
  `android-unit-test-reports/.../build/reports/tests/.../index.html` and
  check the count > 0 under `<div class="counter">`. The
  `tinpet.android.test` convention plugin (now applied transitively by the
  feature plugin) is the safeguard against the "0 tests silently passing"
  regression we hit pre-CI.
- **Functions job** — `npm ci && npm run build` inside `functions/`. Catches
  TS compile errors before a deploy attempt.

Both jobs use Gradle's configuration cache + the GitHub-Actions Gradle
cache so warm runs are typically under 2 minutes. Concurrency-grouped on
`workflow + ref` so a rebased PR cancels the in-flight build.

## Removing the test data

There's no `unseedTestData` endpoint. To purge:

1. **Pets** — Firebase Console → Firestore → `pets` collection → filter by
   `ownerId == <test uid>` and delete in bulk.
2. **Owner profile** — delete `/owners/{uid}`.
3. **Auth user** — Firebase Console → Authentication → find by email →
   Delete user.

Or wait for `purgeDeletedAccounts` to do its thing if you put each test
account through the delete-account flow first.

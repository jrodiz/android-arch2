# Plan — Full-app QA test run (multi-user, 2 emulators)

> **NOTE on plan location:** project convention is plans under `plans/` in the Arch2.0 repo. **Step 0 of execution: copy this file to `plans/qa-full-app-test-plan.md`** so it versions with the code. (The previous content of this working file, the empty-deck plan, is already committed as `plans/empty-deck-screen.md`.)

## 1. Context

The app (TinPet) now has its full v1 surface implemented: auth/sign-up, permissions onboarding, deck with filters + empty state, likes-you, matching + celebration, chat with block/report, pet wizard/management, profile, settings (notifications, filters, featured pets, privacy, blocked users, legal), data export, account deletion with 30-day grace, FCM push, deep links, and es-MX strings. It has only ever been spot-tested per feature. This run plays the QA-tester role: create new users with pets **through the real UI**, exercise every flow per screen/feature (SDD-style Given/When/Then cases), and validate all cross-user interactions on two emulators against the live `arch2-cac87` Firebase backend.

**User-mandated rules of engagement:**
1. **All new QA users + their pets are created exclusively via the in-app sign-up / add-pet wizard UI** (this doubles as the onboarding test). Seeded users (Lena/Marcus/Ayaan/Sofia/Noor) serve the interaction matrix.
2. **Fix every defect as found**: stop → root-cause → fix → `ktlintFormat && ktlintCheck && testDebugUnitTest && :app:assembleDebug` → reinstall both emulators → retest → **one local commit per fix** → resume. Never push; no AI attribution; Firebase Functions/rules fixes get deployed immediately after their commit (standing rule).
3. Full scope: matches, inbox/messages, likes, UI/animation review, account deletion + grace (request / sign-out / re-sign-in restore — NOT the 30-day purge job), data export (download + validate JSON), push notifications (delivery, deep link, channel toggles, quiet hours), Spanish locale spot-check.
4. **Pause after EVERY phase** (including Phase 0) for user go-ahead. Each phase ends with a pass/fail table + screenshots + defect-log update in `qa/QA_RUN_2026-06-10.md`.

**Git:** create branch `feature/jrc--qa.full.app.sweep` at Phase 0 (we're on master; standing rule says branch before committing). All QA commits (fixes + QA doc updates) land there. At run end, write `PR_MESSAGE_QA_SWEEP.md` locally; no remote PR/push without explicit ask.

**QA identities** (all password `TinPetTest2026!`):
| User | Role | Created |
|---|---|---|
| qa.iris@tinpet.dev | Primary QA user — emulator-5554 | Phase 1, via sign-up UI |
| qa.diego@tinpet.dev | Counterpart QA user — emulator-5556 | Phase 1, via sign-up UI |
| qa.zoe@tinpet.dev | Disposable (account-deletion target) | Phase 8, via sign-up UI |
| lena/marcus/ayaan/sofia/noor @tinpet.dev | Seeded interaction matrix | seedTestData |

**Pre-known open defects carried into this run:** `D-pre1` MatchCelebration "my pet" tile shows paw fallback (myPetId resolution); `D-pre2` like via Likes-You / PetDetail path never shows celebration (only deck swipe wires `pendingMatchId`). Both have explicit retest cases in Phase 4.

## 2. Phase overview & state-dependency chain

| Phase | Name | Est. | emulator-5554 | emulator-5556 |
|---|---|---|---|---|
| 0 | Environment prep | 30–45 min | — | — |
| 1 | Auth, sign-up, onboarding | 45–60 min | qa.iris (created) | qa.diego (created) |
| 2 | Deck pre-pet (no own pet) + filters | 45–60 min | qa.iris (0 pets) | idle |
| 3 | Pet wizard & management | 60–90 min | qa.iris | qa.diego |
| 4 | Likes & matching (D-pre1/D-pre2 retests) | 60–90 min | qa.iris | qa.diego → marcus@ |
| 5 | Chat & inbox | 60–90 min | qa.iris | qa.diego ↔ marcus@ |
| 6 | Push channels, quiet hours, deep links | 60–90 min | qa.iris | qa.diego / noor@ |
| 7 | Profile, settings, featured, privacy, export | 60–90 min | qa.iris | qa.diego (observer) |
| 8 | Account deletion + grace, es-MX, final UX sweep | 45–75 min | qa.iris (locale) | qa.zoe (disposable) |

Dependency chain: 1 creates users → **2 must run while iris has zero pets** (RequiresPetDialog) → 3 gives iris 2 pets ("Nova" + "Pixel", the future purge target) / diego 1 pet ("Koda") → 4 creates likes + matches (iris↔diego, iris↔marcus) → 5 consumes them (block confined to iris↔marcus so iris↔diego survives) → 6 reuses iris↔diego chat for push → 7 reuses Phase-5 block for BlockedUsers and run history for export validation → 8 self-contained.

## 3. Phase 0 — Environment prep (gate checklist, no scored cases)

1. Create branch `feature/jrc--qa.full.app.sweep`. Copy this plan to `plans/qa-full-app-test-plan.md`.
2. Boot both emulators (5554 with `-no-snapshot-load` per its flaky history); `adb devices` shows both; verify Play services present (`pm list packages | grep com.google.android.gms` — required for FCM) and network up.
3. Re-seed backend: `curl ".../seedTestData?secret=tinpet-seed-2026-05-25"` → idempotent OK (5 owners, 10 pets). Sanity: `inspectMatches?secret=tinpet-inspect-2026-05-25`.
4. Build + install both: `JAVA_HOME=<jbr-17> ./gradlew :app:installDebug` (per-device via `ANDROID_SERIAL` or `adb install -r`). Baseline green: `ktlintCheck testDebugUnitTest`.
5. Push 6 generated JPEGs per emulator to `/sdcard/Pictures/` + media-scan (`cmd media_scanner scan`, fallback `MEDIA_SCANNER_SCAN_FILE` broadcast, last resort reboot); smoke-open the Photo Picker to confirm visibility.
6. `pm clear com.rodiz.arch2.debug` on both so Phase 1 starts signed-out at LoginHome.
7. Create `qa/QA_RUN_2026-06-10.md`: run header (build SHA, emulator images), per-phase results tables (ID | Case | Result | Screenshot | Notes), defect log (ID | Sev | Case | Root cause | Fix commit | Retest), screenshot index `qa/screenshots/phaseN/<CASE-ID>_<n>.png`. Log D-pre1/D-pre2 as OPEN.

**Exit gate:** both emulators responsive, app at LoginHome on both, seed confirmed, photos visible in picker. → pause for go.

## 4. Test cases per phase (SDD: Given/When/Then, stable IDs)

### Phase 1 — Auth, sign-up, onboarding
- **AUTH-01** Fresh signed-out install → launch → start destination LoginHome, featured-pets hero carousel rotating with seeded pets.
- **AUTH-02** Unknown email + any password → login → error surfaced, stays on LoginHome.
- **AUTH-03** `lena@tinpet.dev` + wrong password → login → auth error, no navigation.
- **AUTH-04** Blank email or password → submit → blocked (disabled button or validation error).
- **AUTH-05** Tap forgot-password link → ForgotPassword stub renders, no crash; back → LoginHome.
- **AUTH-06** Tap sign-up link → SignUpHome opens.
- **AUTH-07** Malformed email (`qa.iris@`) → validation error.
- **AUTH-08** Password ≠ confirm → mismatch error, submit blocked.
- **AUTH-09** Too-weak/short password → validation error (assert against actual rule in login domain validators).
- **AUTH-10** Valid fields, terms unchecked → create blocked.
- **AUTH-11** Tap terms / privacy links → legal screens open; back preserves entered form state.
- **AUTH-12** Avatar picker sheet → select photo → selection reflected in form.
- **AUTH-13** Sign up with existing `lena@tinpet.dev` → email-in-use error.
- **AUTH-14** Valid form + terms → create `qa.iris@tinpet.dev` → success → PermissionsOnboarding.
- **NOTIF-01** PermissionsOnboarding → grant location via system dialog → UI shows granted.
- **NOTIF-02** Grant notifications + tap Done → lands on DeckHome.
- **AUTH-15** Repeat full happy-path sign-up on 5556 for `qa.diego@tinpet.dev` → DeckHome.
- **AUTH-16** iris signed in → force-stop + relaunch → session persists, starts at DeckHome.
- **AUTH-17** diego signs out (Profile → Sign out → confirm, smoke) → log back in → DeckHome.
- **UX-01** Login/sign-up with IME open → no clipped text, fields not obscured, carousel smooth.

### Phase 2 — Deck with no own pet (iris = 0 pets; MUST precede Phase 3)
- **DECK-01** Fresh iris on DeckHome → LOADING spinner → READY stack of seeded pets.
- **DECK-02** Header meta strip → km / intents / species counts match default filters.
- **DECK-03** Tap Pass → card dismissed, next card shown.
- **DECK-04** Left-swipe gesture → pass animation, next card.
- **DECK-05** Tap Rewind after a pass → previous card restored on top.
- **DECK-06** Like with no own pet → RequiresPetDialog; dismiss stays on deck; "Add pet" CTA opens wizard step 1; back exits without creating.
- **DECK-07** Tap card → DeckPetDetail (photos, bio); back returns to same deck position.
- **DECK-08** Pass from detail → returns to deck advanced past that pet.
- **DECK-09** Like from detail with no own pet → RequiresPetDialog (detail path too).
- **LIKE-01** Likes tab with no incoming likes → empty state + Go-to-Deck CTA works; no badge.
- **MATCH-01** Matches tab with no matches → empty state; no badge.
- **DECK-15** Tap header bell → SettingsNotifications opens; back to deck.
- **SET-03a** Tap header filter icon → SettingsFilters: slider bounds 5–200 km, intent + species chips toggle, auto-applied (no Apply button).
- **DECK-10** Narrow filters (e.g. species = Hamster only) → deck restricted, meta strip updates.
- **DECK-11** Pass through all remaining cards → EXHAUSTED "That's everyone for now" with 3 action cards.
- **DECK-12** EXHAUSTED → "Widen distance" → SettingsFilters opens.
- **DECK-13** EXHAUSTED with today's passes → "Review who you passed" → snackbar with count; passed cards reappear.
- **DECK-14** EXHAUSTED → "Add another pet" → wizard opens; back out without saving.
- **SET-01** Changed filters → force-stop + relaunch → filters persisted. Reset to defaults afterward.
- **UX-02** Mid-gesture screenshots of like/pass → card rotation/affordances render, no jank or ghost cards.

### Phase 3 — Pet wizard & management
- **PET-01** Step 1 blank name → Next blocked with validation.
- **PET-02** Species sheet → pick species → shown on step 1.
- **PET-03** Photo via system Photo Picker (tap field → screencap → tap pushed `qa1.jpg` → confirm) → thumbnail appears.
- **PET-04** Back from step 2 → step 1 retains name/species/photo.
- **PET-05** Step 2 size/energy/intents → advance → step 3; re-entering step 2 shows persisted selections.
- **PET-06** Step 3 bio → Publish → wizard closes; MyPets shows iris pet #1 "Nova".
- **PET-14** diego full wizard happy path → "Koda" published (visible to iris in Phase 4).
- **PET-07** iris adds minimal pets to 5 active → at-quota banner, Add entry points disabled (`MAX_ACTIVE_PETS = 5`).
- **PET-08** 6th pet created directly via Firestore REST with iris's idToken → rules deny OR `enforcePetCap` deletes within ~30 s (verify via REST); record which.
- **PET-09** MyPets → tap pet → PetDetail owner-preview with Edit + Delete.
- **PET-10** EditPet → change bio/energy → save → PetDetail reflects immediately and after relaunch.
- **PET-13** Delete → Cancel in confirm → pet retained.
- **PET-11** Delete → confirm → pet gone from MyPets (ARCHIVED server-side).
- **PET-12** Deleted iris pet never appears in diego's deck on 5556.
- **Cleanup:** trim iris to 2 active pets — "Nova" + "Pixel" (Pixel = Phase-5 purge-test target).
- **DECK-16** iris (now owning pets) likes a seeded pet → no dialog; `likes/{iris_petId}` doc exists (REST).
- **UX-03** Wizard transitions/step indicator correct each step, no layout shift; screenshots of all 3 steps.

### Phase 4 — Likes & matching (D-pre1 / D-pre2 retests)
Pairings: iris↔diego (likes-you path), iris↔marcus (deck-swipe celebration, future block). Switch 5556 between diego and marcus via sign-out/login.
- **LIKE-02** iris likes Koda via deck swipe → diego's Likes tab shows tile + badge = 1.
- **NOTIF-03** diego backgrounded when like lands → push "Someone liked your pet"; tap deep-links to LikesHome.
- **LIKE-03** Tap like tile → DeckPetDetail of iris's pet.
- **LIKE-04 [D-pre2 retest]** diego like-backs from Likes-You/detail → match doc created (inspectMatches) AND MatchCelebration fires on this path. Expected FAIL → fix `pendingMatchId` wiring for the likes/detail paths, commit, retest.
- **MATCH-02 [D-pre1 retest]** Celebration "my pet" tile shows the real pet photo, not paw fallback. Expected FAIL → fix myPetId resolution in `MatchCelebrationViewModel`, commit, retest.
- **MATCH-03** Celebration renders both pet tiles + both owner chips correctly.
- **MATCH-04** "Keep swiping" → back on Deck; system-back does NOT re-show celebration (popped).
- **MATCH-05** marcus likes Nova first; iris deck-swipes Otto → iris (second liker) sees same-device celebration within ~2 s poll window.
- **NOTIF-04** First-liker marcus gets "New match!" push on 5556 → tap deep-links to ChatRoute.
- **MATCH-06** "Say hello" → ChatRoute; back from chat lands on **Deck** (celebration popped before push).
- **MATCH-07** New matches (no messages) appear in "new matches" row, not conversations; Matches badge set.
- **LIKE-05** Extra incoming like (sofia@ likes a diego pet) → recipient taps Pass on Likes-You → tile removed, badge decrements, `passedLikes/{...}` doc exists.
- **LIKE-06** All incoming likes acted on → empty state returns, badge cleared.
- **DECK-17** Liked/matched/passed pets never reappear in iris's deck.
- **DECK-18** Fresh reciprocal pair → like from DeckPetDetail → celebration fires from detail path (post-fix regression).
- **UX-04** Celebration animation frames → no clipping, completes, buttons immediately tappable.

End state (record matchIds in QA doc via inspectMatches): iris↔diego and iris↔marcus matches exist.

### Phase 5 — Chat & inbox (messaging iris↔diego; purge + block on iris↔marcus)
- **CHAT-01** Tap iris↔diego in new-match row → ChatRoute with banner "You matched on <PetA> & <PetB>".
- **CHAT-02** iris sends "hola from iris" → optimistic append on 5554; appears on 5556 within ~2 s.
- **CHAT-03** diego opens chat → readBy updates → read receipt shows on iris's bubble.
- **CHAT-05** Empty/whitespace draft → send disabled/no-op.
- **CHAT-04** 2100-char input (chunked `input text`) → draft truncates at exactly 2000 (`take(2000)`), sends fine at 2000.
- **CHAT-06** After first message → match moves to conversations with preview; unread badge on recipient; opening chat clears it.
- **NOTIF-05** diego backgrounded; iris sends → push to diego ONLY (assert none on 5554); tap opens that ChatRoute.
- **CHAT-07** Menu → Report → reason sheet → submit → confirmation snackbar; chat unaffected.
- **CHAT-11** iris↔marcus chat whose `initiatingLike.toPetId` = Pixel: set Pixel `state=PURGED` via REST → system message "Pixel is no longer on TinPet." appears (onPetUpdate).
- **CHAT-09** iris has iris↔marcus chat open; marcus blocks from chat menu → iris's open chat flips to unmatched state, composer disabled.
- **CHAT-08** Post-block: match gone from both inboxes, `blocks/{marcus_iris}` exists, messages cascade-deleted (onMatchDelete; verify via inspectMatches/REST).
- **CHAT-10** Post-block decks: iris's pets absent from marcus's deck AND vice versa.
- **CHAT-12** Force-stop + relaunch → iris↔diego history persists in order with timestamps.
- **UX-05** Chat with IME open → composer above keyboard, bubbles aligned by sender, auto-scroll on send, banner not overlapping.

End state: iris↔diego match alive (Phase 6 needs it); **marcus blocked iris** — keep until PRIV-02 unblocks from marcus's device in Phase 7.

### Phase 6 — Push channels, quiet hours, deep links
- **NOTIF-06** iris toggles Messages channel off → `owners/{iris}.notifications.newMessage == false` (REST).
- **NOTIF-07** Channel off + iris backgrounded; diego sends → NO push on 5554 within 30 s (dumpsys absence) but message visible in-app.
- **NOTIF-08** Channel re-enabled; diego sends → push delivered (only-explicit-false semantics).
- **NOTIF-09** iris Likes channel off; noor@ (on 5556) likes Nova → no like push, but Likes grid/badge update in-app; re-enable after.
- **NOTIF-10** Quiet hours spanning now (verify stored minutes + IANA tz via REST) → push suppressed; disable → next message pushes.
- **NOTIF-11** Revoke POST_NOTIFICATIONS (`pm revoke`) → SettingsNotifications permission button → rationale flow recovers (re-grant; `pm grant` fallback).
- **NOTIF-12** `am start -a VIEW -d tinpet://deck|likes|matches|profile` → each opens correct tab, sane back behavior, no duplicate screens.
- **NOTIF-13** App force-stopped → cold-launch `tinpet://chat/{irisDiegoMatchId}` → ChatRoute opens with data; back lands on sane parent, not blank stack.
- **NOTIF-14** `tinpet://notify` → notification rationale/notifications surface opens.
- **NOTIF-15** iris foregrounded on Profile; message push tapped from shade → navigates in existing task (no second activity instance; `dumpsys activity activities`).
- **UX-06a** Shade screenshots → titles/bodies/channel names sensible, correct app icon.

### Phase 7 — Profile, settings, featured pets, privacy, export
- **PROF-01** Change avatar via sheet → persists after relaunch.
- **PROF-02** EditProfile name/bio/location → save → ProfileHome updates; diego's view of iris (owner chip) reflects new name.
- **PROF-03** ProfileHome entries (My pets / Add pet / Settings / Help&Safety) all navigate; back returns.
- **PROF-04** Sign out → Cancel keeps session; Confirm → LoginHome; relaunch stays signed out; log back in as iris.
- **SET-02** SettingsHome hub → every section opens (EditProfile, Notifications, Filters, FeaturedPets, Privacy, BlockedUsers, PrivacyPolicy, Terms, HelpSafety).
- **SET-03** Filters slider extremes clamp at 5/200 km; chips multi-select; persist across relaunch.
- **SET-04** Pin 3 featured pets → 4th pin blocked; signed-out LoginHome hero on 5556 shows the pinned pets.
- **SET-05** Quiet-hours picker stores correct minutes + tz in `owners/{iris}` (cross-check NOTIF-10).
- **SET-06** PrivacyPolicy/Terms/HelpSafety render, scroll, back works.
- **PRIV-01** iris Pause on → diego's deck + Likes-You show no iris pets anywhere; off → reappear after refresh.
- **PRIV-02** marcus's BlockedUsers lists iris (from CHAT-08) → Unblock → confirm → list empties; pets mutually reappear in decks.
- **PRIV-03** Data export → `dataExports/{iris}` pending→ready with downloadUrl (poll ≤2 min) → curl JSON → contains owner, pets, matches+messages, likes, passes; spot-check counts match this run.
- **PRIV-04** downloadUrl is a signed Storage URL (HTTP 200, no auth header; note 7-day expiry params).
- **UX-06** Settings screens sweep → toggles/sliders aligned, no truncated labels, consistent top bars.

### Phase 8 — Account deletion + grace, es-MX locale, final UX sweep
- **AUTH-18** 5556 signed out → full UI sign-up `qa.zoe@tinpet.dev` (avatar, terms, permissions) → DeckHome; add 1 quick pet so deletion-hides-pets is observable.
- **PRIV-05** Delete-account sheet: wrong confirmation text (`delete`/blank) keeps confirm disabled; typing `DELETE` enables.
- **PRIV-06** Confirm deletion → 30-day-grace messaging → signed out to LoginHome.
- **PRIV-07** Backend: `accountDeletions/{zoe}` has `requestedAt` + `hardDeleteAt` ≈ +30 d (REST); zoe's pet hidden from iris's deck while pending. (30-day purge job itself NOT tested.)
- **PRIV-08** zoe re-signs-in within grace → restore completes: account active, `accountDeletions/{zoe}` removed/cancelled (REST), pet visible in iris's deck again.
- **LOC-01** `cmd locale set-app-locales com.rodiz.arch2.debug --user 0 --locales es-MX` on 5554 → LoginHome (sign out first), Deck, Likes, Matches, Profile, Settings all Spanish, no English leaks (screenshot each).
- **LOC-02** Formatted strings in Spanish (deck meta km, pet-quota banner `%d`, sign-out confirm, RequiresPetDialog if reachable) → placeholders/plurals render correctly.
- **LOC-03** Spanish dialog + snackbar (report flow, pass-restore) → translated and unclipped (es strings run longer — watch truncation).
- **LOC-04** Reset locale (`--locales ""`) → relaunch → English restored.
- **UX-07** Final sweep: tab transitions, deck physics, celebration, bottom sheets, scroll behaviors with screenshots → any jank/clipping logged + fixed per the defect rule.
- **Wrap-up:** defect log complete (all D-xxx closed with commit SHAs), full-run roll-up table in `qa/QA_RUN_2026-06-10.md`, `PR_MESSAGE_QA_SWEEP.md` written locally, list of commits surfaced for review.

## 5. Execution mechanics

**adb driving** (always `-s emulator-5554|5556`): `input tap X Y`, `input swipe x1 y1 x2 y2 [ms]` (like ≈ `200 1200 950 1100 200`, pass mirrored); `input text 'foo%sbar'` (ASCII only, chunk long inputs; never type Spanish — assert displayed strings only); `exec-out screencap -p > qa/screenshots/phaseN/<CASE>_<n>.png`; `am start -n com.rodiz.arch2.debug/com.rodiz.arch2.MainActivity`, `am force-stop`, `pm clear`; deep links via `am start -a android.intent.action.VIEW -d "tinpet://..." com.rodiz.arch2.debug`. **Screencap before computing tap targets every time — never reuse coordinates after a fix/reinstall.** Photo Picker: tap field → screencap → tap thumbnail → confirm (layout varies; read from the screenshot).

**Backend verification:** `inspectMatches?secret=tinpet-inspect-2026-05-25` for matches; arbitrary docs via Firestore REST with a user idToken — sign in through `identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=<apiKey from app/google-services.json>`, then `firestore.googleapis.com/v1/projects/arch2-cac87/databases/(default)/documents/<path>` with `Authorization: Bearer`. If rules deny a cross-user read, fall back to `gcloud auth print-access-token`. Direct writes (PET-08 6th pet, CHAT-11 purge) use PATCH with `updateMask`. Confirm functions region once via `firebase functions:list --project arch2-cac87`.

**Push assertions:** `adb shell dumpsys notification --noredact | grep -B2 -A8 com.rodiz.arch2.debug`; visual fallback `cmd statusbar expand-notifications` → screencap → tap → `collapse`. Negative cases: poll dumpsys 30 s, require zero records — and only score a push-negative after a push-positive has succeeded on that device this session.

**Defect workflow:** IDs `D-001…` (D-pre1/D-pre2 keep theirs). Severity P0 crash/data-loss, P1 broken flow, P2 workaroundable, P3 cosmetic. Per defect: log (case, repro, screenshot, logcat) → root-cause → fix → ktlint + unit tests (add/adjust when VM logic changes) + assemble → reinstall both → retest failing + adjacent cases → commit `fix(<feature>): <summary> [D-00X]` → resume. P3 cosmetics from UX sweeps may batch at phase end, still one commit each. Functions/rules fixes also get `firebase deploy --only <target>` right after the commit.

## 6. Risks & gotchas

1. **emulator-5554 flakiness:** if unresponsive → `emu kill`, relaunch `-no-snapshot-load`; persists → `-wipe-data` (safe: all QA state is server-side; reinstall APK, re-login, re-push photos, re-grant permissions).
2. **FCM on emulators:** needs Play-services image + network; token registration can lag minutes after install/pm-clear — warm up with one push-positive before scoring any NOTIF case.
3. **Seed mutation mid-run:** re-seed rewrites only the 5 seeded owners/pets but can orphan QA↔seeded matches/likes (iris↔marcus). Safe before Phase 4; after that, only if prepared to rebuild state (verify with inspectMatches).
4. **Silent rules denials:** a tap that "does nothing" may be PERMISSION_DENIED — check `adb logcat -d -s Firestore` before filing a UI defect; rules failures are defects too, different root cause.
5. **~2 s celebration poll window:** slow emulator can legitimately miss same-device celebration (push covers it). Only a defect if celebration missing AND no push; rerun once before filing.
6. **`input text` limits:** no unicode; `%s` for spaces; chunk CHAT-04's 2100 chars; remember the field truncates rather than blocking send.
7. **Permission dialogs:** API 33+ double-deny of POST_NOTIFICATIONS is permanent — recover via `pm grant`; never deny twice except in NOTIF-11.
8. **Block-direction bookkeeping:** marcus-blocks-iris must survive Phases 5→7 (PRIV-02's precondition); don't unblock early.

## 7. Coverage map (screen/feature → phases → cases)

| Screen / feature | Phase(s) | Case IDs |
|---|---|---|
| LoginHome + hero carousel | 1, 7, 8 | AUTH-01..06, AUTH-17, SET-04, LOC-01, UX-01 |
| SignUpHome (validation/avatar/terms) | 1, 8 | AUTH-07..15, AUTH-18, UX-01 |
| ForgotPassword stub | 1 | AUTH-05 |
| PermissionsOnboarding | 1 | NOTIF-01..02 |
| Session / start destination | 1, 7 | AUTH-16, PROF-04 |
| Deck states (loading/ready/exhausted) | 2 | DECK-01..02, DECK-11..14 |
| Deck like/pass/swipe/rewind | 2–4 | DECK-03..05, DECK-16..17, UX-02 |
| RequiresPetDialog | 2, 8 | DECK-06, DECK-09, LOC-02 |
| DeckPetDetail | 2, 4 | DECK-07..09, DECK-18, LIKE-03 |
| Deck header (filter/bell/meta) | 2 | DECK-02, DECK-10, DECK-15, SET-03a |
| LikesHome (grid/empty/badge/pass) | 2, 4 | LIKE-01..06 |
| MatchesHome (new vs conversations/badges) | 2, 4, 5 | MATCH-01, MATCH-07, CHAT-06 |
| MatchCelebration + back-stack pop | 4 | MATCH-02..06, DECK-18, UX-04 (D-pre1: MATCH-02; D-pre2: LIKE-04) |
| ChatRoute (send/receipts/truncate/banner) | 5 | CHAT-01..06, CHAT-12, UX-05 |
| Block / report / unmatched-open | 5, 7 | CHAT-07..10, PRIV-02 |
| Pet-purged system message | 5 | CHAT-11 |
| MyPets / wizard / PetDetail / EditPet / delete | 3 | PET-01..14, UX-03 |
| Pet cap (client + enforcePetCap) | 3 | PET-07..08 |
| ProfileHome (avatar/entries/sign-out) | 7 | PROF-01..04 |
| SettingsHome / EditProfile | 7 | SET-02, PROF-02 |
| SettingsFilters | 2, 7 | SET-01, SET-03, SET-03a, DECK-10/12 |
| SettingsNotifications (+rationale) | 6, 7 | NOTIF-06..11, SET-05 |
| FeaturedPets → login hero | 7 | SET-04 |
| Privacy: pause / export / delete+grace | 7, 8 | PRIV-01, PRIV-03..08 |
| BlockedUsers | 7 | PRIV-02 |
| Legal docs | 1, 7 | AUTH-11, SET-06 |
| Push delivery + filtering | 4–6 | NOTIF-03..10 |
| Deep links (6 routes, cold/warm) | 4–6 | NOTIF-03..05, NOTIF-12..15 |
| Bottom-nav badges | 4, 5 | LIKE-02, LIKE-06, MATCH-07, CHAT-06 |
| es-MX locale | 8 | LOC-01..04 |
| Animations / visual QA | 1–8 | UX-01..07, UX-06a |

**Accepted gaps:** 30-day hard-purge + 7-day archived-pet purge scheduled jobs (time-based; CHAT-11 covers the PURGED trigger directly); `backfillMatchPetIds` (maintenance-only).

## 8. Verification & reporting

Every phase produces: updated `qa/QA_RUN_2026-06-10.md` results table (pass/fail per case), indexed screenshots under `qa/screenshots/phaseN/`, defect-log delta, and a chat summary — then **stops for your go-ahead**. Defect fixes are verified by retesting the failing case plus adjacent cases on both emulators before the run resumes. Final wrap-up: roll-up table, closed defect log with commit SHAs, `PR_MESSAGE_QA_SWEEP.md` for review (no push, no remote PR).

## 9. Critical files (defect-fix likely targets)

- `feature/match/presentation/.../MatchCelebrationViewModel.kt` — D-pre1 (my-pet tile fallback)
- `feature/likes/presentation/.../LikesYouViewModel.kt` + `feature/deck/presentation/.../DeckPetDetailViewModel.kt` — D-pre2 (celebration on like-back/detail paths; reference wiring: `DeckViewModel.pendingMatchId`)
- `feature/chat/presentation/.../ChatViewModel.kt` — 2000-char truncation, block/report flows
- `functions/src/triggers/*.ts`, `firestore.rules` — backend-side fixes (deploy immediately after commit)
- `functions/src/admin/seedTestData.ts` — re-seed semantics governing Risk 3

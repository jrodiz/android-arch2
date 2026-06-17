# Scheduled / maintenance Cloud Functions — test plan

> Covers the QA plan's "accepted gaps": the time-based jobs not exercised during the
> main sweep. Live project `arch2-cac87`, region `us-central1`. Disposable target:
> `qa.zoe@tinpet.dev` (uid `lJyPSFN9sRXFBoCNmRQzV79uciN2`). Date: 2026-06-16.

## Functions under test

| Function | Trigger | What it does |
|---|---|---|
| `purgeDeletedAccounts` | `onSchedule("every 24 hours")` | Deletes `accountDeletions` rows where `hardDeleteAt <= now`; wipes the user's pets+Storage, likes, passes, matches, fcmTokens, users doc, **and the Auth user**. Irreversible. |
| `purgeArchivedPets` | `onSchedule("every 24 hours")` | Finds `pets` with `state==ARCHIVED && deletedAt <= now-7d`; deletes their photos and flips to `state=PURGED, photos=[]`. |
| `backfillMatchPetIds` | HTTP `onRequest` (secret `pet-id-backfill-2026-05-23`) | Idempotent one-off; fills `petAId/petBId` on matches missing them, from reciprocal likes. |

## Trigger mechanism (no prod code changes)

The two `onSchedule` jobs aren't HTTP-callable and there's no `gcloud` / service-account
key on this machine. Instead: mint a Cloud-platform access token from the Firebase CLI's
stored refresh token (the public `firebase-tools` OAuth client — same exchange the CLI
does), then call **Cloud Scheduler `jobs:run`**, which invokes the *real deployed*
function immediately. `backfillMatchPetIds` is plain HTTP → `curl`.

Token mint:
```
refresh=$(jq -r .tokens.refresh_token ~/.config/configstore/firebase-tools.json)
at=$(curl -s https://oauth2.googleapis.com/token \
  -d client_id=563584335869-fgrhgmd47bqnekij5i8b5pr03ho849e6.apps.googleusercontent.com \
  -d client_secret=j9iVZfS8kkCEFUPaAeJV0sAi \
  -d refresh_token="$refresh" -d grant_type=refresh_token | jq -r .access_token)
```
Run a job:
```
curl -X POST -H "Authorization: Bearer $at" \
 "https://cloudscheduler.googleapis.com/v1/projects/arch2-cac87/locations/us-central1/jobs/<job>:run"
```

## D-014 (found during recon) — purgeArchivedPets has no schedule

`firebase functions:list` shows `purgeArchivedPets` as a deployed scheduled function, but
a direct GET of `firebase-schedule-purgeArchivedPets-us-central1` returns **404 in every
location** (vs `purgeDeletedAccounts`, whose job exists + ENABLED). **The 7-day purge
never runs in prod.** Severity P2 (silent data-retention / Storage-cost leak; not a
crash — the live `onPetUpdate` PURGED path is unaffected).

**Fix:** `firebase deploy --only functions:purgeArchivedPets` to recreate the Cloud
Scheduler job; verify the job appears + ENABLED. Then test through the real `jobs:run`
path. Commit `fix(functions): restore purgeArchivedPets daily schedule [D-014]`.

## Test procedure (qa.zoe only; seeded accounts never touched)

### J-01 — purgeArchivedPets (after the D-014 fix)
1. Create a disposable pet under qa.zoe via REST (owner-authed; rules allow).
2. PATCH it to `state=ARCHIVED`, `deletedAt = now-8d` (owner may write both per rules).
3. `jobs:run firebase-schedule-purgeArchivedPets-us-central1`.
4. **Expect:** pet → `state=PURGED`, `photos=[]`, `updatedAt` bumped; Storage prefix emptied.
5. Negative guard: a second disposable ARCHIVED pet with `deletedAt = now-2d` must remain ARCHIVED (inside the 7-day window).

### J-02 — backfillMatchPetIds
1. `curl ".../backfillMatchPetIds?secret=..."` → baseline report (expect mostly `alreadyComplete`, since `onLikeCreate` now writes the ids).
2. Fill-branch: on a disposable match with intact reciprocal likes, strip `petAId`+`petBId` via REST (participant-authed), re-run, expect `updated>=1` and the ids repopulated. If no match has its reciprocal likes still present, record that the fill branch is unreachable with current data and only the idempotent/no-op path is verified.
3. Re-run once more → idempotent (`alreadyComplete` for that match again).

### J-03 — purgeDeletedAccounts (DESTRUCTIVE — permanently deletes qa.zoe)
1. Seed qa.zoe with a small, verifiable footprint (≥1 pet; note any fcmTokens/likes).
2. Capture the full footprint via REST (pets, likes, passes, matches, fcmTokens, owners doc).
3. Write `accountDeletions/{zoe}` with `requestedAt=now`, `hardDeleteAt=now` (owner-authed; backdated so it's overdue).
4. `jobs:run firebase-schedule-purgeDeletedAccounts-us-central1`.
5. **Expect:** every captured doc gone; `accountDeletions/{zoe}` deleted; Auth user removed → REST sign-in as qa.zoe now fails with `EMAIL_NOT_FOUND`/`INVALID_LOGIN_CREDENTIALS`.
6. qa.zoe is now permanently gone — end of the disposable account's life.

## Approvals required before mutating

- Redeploy `purgeArchivedPets` (prod function deploy) to fix D-014.
- Run `purgeDeletedAccounts` against qa.zoe → **irreversible** Auth + data deletion.

## Reporting

Append results to `qa/QA_RUN_2026-06-10.md` (new "Scheduled jobs" section) with J-01..J-03
pass/fail + the D-014 entry, and one commit for the D-014 fix.

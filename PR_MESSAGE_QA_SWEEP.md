# Full-app QA sweep + 14 defect fixes

Multi-user, two-emulator QA pass over the entire TinPet v1 surface against the live `arch2-cac87` backend. All QA users and pets were created through the real in-app UI; cross-user flows were exercised on two devices and verified against Firestore/Storage/Functions. Full run log: `qa/QA_RUN_2026-06-10.md`.

## Scope covered
Auth/sign-up/onboarding · deck (states, filters, swipe/rewind, empty state) · pet wizard & management + pet cap · likes & matching + celebration · chat (send, read receipts, truncation, report, persistence) · pet-purge system note · block→unmatch cascade · push (delivery, per-channel opt-out, quiet hours, deep links, permission recovery) · profile/settings · featured pets · pause · blocked users · data export · account deletion + 30-day grace + restore · es-MX locale.

## Defects fixed (14 — zero open)
Each is one commit with a regression test where a VM seam existed; on-device retested on both emulators.

| ID | Sev | Fix | Commit |
|---|---|---|---|
| D-001 | P2 | Rewind restores the passed card | `3af8135` |
| D-002 | P3 | Rejected like keeps its card on the deck | `b0badf1` |
| D-003 | P1 | "Review who you passed" no longer hangs on a spinner | `b0badf1` |
| D-004 | P2 | Detail-screen swipes propagate back to the deck | `e6c330a` |
| D-pre2 | P1 | Match celebration fires from the Likes-You / detail like-back | `667aa18` |
| D-005 | P1 | Deck excludes already liked/passed pets after relaunch | `5fd3d3a` |
| D-006 | P2 | Matched owners' likes drop out of "Likes you" | `a8d4b64` |
| D-007 | P2 | Decline ✕ on each Likes-you card dismisses an incoming like | `01276bb` |
| D-008 | P2 | "Read" receipt renders under the last read outgoing message | `9bd21ca` |
| D-009 | P3 | "system" sender notes render as a centered system note | `4e3029e` |
| D-010 | P1 | Inbox + Profile + Likes re-key reactively on account switch | `5a64af5`, `aed91f1` |
| D-011 | P3 | Back from a deep-linked chat returns to the inbox, not the launcher | `9c5e04d` |
| D-012 | P1 | Push taps honor the deep link (route to the right surface) | `d9a9c81` |
| D-013 | P3 | Inbox/chat day + month labels follow the app locale (es-MX) | `b233ad4` |

D-pre1 was confirmed **not reproducing**. Every scored QA case now passes.

## Notes on the bigger fixes
- **D-010** (session re-keying) — the inbox, Profile header/pets, pending-deletion banner, and Likes-you grid all read the signed-in uid at the data layer; they now `session.observe().flatMapLatest` so an in-app account switch (sign out → sign in a different user, no restart) re-keys to the new user instead of showing the previous one. Verified on-device.
- **D-007** (decline path) — reuses the existing-but-dead `passLike`/`passedLikes` path behind a new ✕ on each Likes-you card; this also unblocks the `LIKE-06` empty-state case.
- **D-013** — swapped hardcoded English `when`-blocks for `getDisplayName(TextStyle, Locale.getDefault())`.

## Notes
- Backend data correct throughout; no Firebase rules/functions changes were needed.
- es-MX strings are complete across the main surfaces (one minor day-abbrev leak, D-013).
- Disposable `qa.zoe` was used for the destructive deletion test and left active (deletion cancelled); the five seeded accounts were never mutated destructively.

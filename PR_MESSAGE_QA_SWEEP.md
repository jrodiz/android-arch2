# Full-app QA sweep + 12 defect fixes

Multi-user, two-emulator QA pass over the entire TinPet v1 surface against the live `arch2-cac87` backend. All QA users and pets were created through the real in-app UI; cross-user flows were exercised on two devices and verified against Firestore/Storage/Functions. Full run log: `qa/QA_RUN_2026-06-10.md`.

## Scope covered
Auth/sign-up/onboarding · deck (states, filters, swipe/rewind, empty state) · pet wizard & management + pet cap · likes & matching + celebration · chat (send, read receipts, truncation, report, persistence) · pet-purge system note · block→unmatch cascade · push (delivery, per-channel opt-out, quiet hours, deep links, permission recovery) · profile/settings · featured pets · pause · blocked users · data export · account deletion + 30-day grace + restore · es-MX locale.

## Defects fixed (12)
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
| D-008 | P2 | "Read" receipt renders under the last read outgoing message | `9bd21ca` |
| D-009 | P3 | "system" sender notes render as a centered system note | `4e3029e` |
| D-010 | P1 | Inbox re-keys reactively on account switch (no cross-user leak) | `5a64af5` |
| D-011 | P3 | Back from a deep-linked chat returns to the inbox, not the launcher | `9c5e04d` |
| D-012 | P1 | Push taps honor the deep link (route to the right surface) | `d9a9c81` |

## Open items / decisions for you
- **D-007 (P2)** — the Likes-you grid has no way to *decline* an incoming like (only matching clears a tile). Deferred per your call; fix approach is a design choice (filter the deck-pass vs. a dedicated decline writing `passedLikes`). This is the only thing blocking the `LIKE-06` empty-state case.
- **D-010 residual** — the same non-reactive `sessionRepo.current()` pattern remains in other data-layer listeners (profile header, accountDeletions, likes); only visible on an in-app account switch (sign out → sign in a different user, no restart), self-corrects on restart. A comprehensive fix is a data-layer session-reactive refactor.
- **D-013 (P3)** — in es-MX the conversation timestamp shows English day abbreviations ("Thu" vs "jue"); date formatter isn't locale-aware.

See `qa/QA_RUN_2026-06-10.md` → "Decisions for you" for the options.

## Notes
- Backend data correct throughout; no Firebase rules/functions changes were needed.
- es-MX strings are complete across the main surfaces (one minor day-abbrev leak, D-013).
- Disposable `qa.zoe` was used for the destructive deletion test and left active (deletion cancelled); the five seeded accounts were never mutated destructively.

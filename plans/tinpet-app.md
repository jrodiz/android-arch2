# Plan: TinPet — the full product spec

> **Architecture reference:** all module structure, dependency rules, and tech-stack choices are governed by [`plans/ANDROID_APP_SCAFFOLD_PROMPT.md`](./ANDROID_APP_SCAFFOLD_PROMPT.md). This plan defines the **product** (features, flows, decisions) — it does not duplicate the scaffold spec. When a feature is implemented, it gets its own plan file (`plans/<feature-name>.md`) that references this one.

## 1. Context

TinPet is a swipe-based matching app for pets. It is the Tinder model adapted to a free, three-intent product:

- **Playdate** — owners coordinate IRL meetups for their pets.
- **Adoption** — peer-to-peer (no shelters in v1).
- **Owner-friendship** — humans bond via their pets.

There is **no paid tier**. All Tinder mechanics that exist primarily to gate behind a paywall (super-likes, boosts, passport, daily picks, swipe quotas, see-who-liked-you teasing) are removed, because there's no reason to gate anything from free users.

**What's already built (do not re-plan):**

- App is branded "TinPet" ([`plans/rebrand-tinpet.md`](./rebrand-tinpet.md)).
- Auth: Login + Sign Up screens with email/password and avatar ([`plans/login-feature.md`](./login-feature.md), [`plans/sign-up-screen.md`](./sign-up-screen.md), [`plans/firebase-google-signin.md`](./firebase-google-signin.md)).
- Bottom-nav scaffold + a placeholder Facebook-style feed on the Home tab ([`plans/home-bottom-navigation.md`](./home-bottom-navigation.md), [`plans/home-feed.md`](./home-feed.md)). **The feed will be replaced by the TinPet swipe deck.** The bottom-nav structure will be reshaped to the 5-tab layout below.

## 2. Three intents

A **pet profile** declares which intents it participates in. Intent is rendered as a chip on every card so swipers always see the context. The deck is unified — Playdate / Adoption / Friendship cards interleave, filtered by the user's multi-select intent preference.

| Intent | What "match" means |
|---|---|
| Playdate | Owners chat to coordinate a meetup for their pets. |
| Adoption | Owner is rehoming the pet; the other side is interested in taking the pet. |
| Owner-friendship | Pet is the icebreaker; humans want to befriend each other. |

## 3. Scope summary

### In scope

| Area | Decision |
|---|---|
| Account model | One owner account, many pets per owner. Individuals only — no shelter/rescue account type. |
| Pet profile fields | photo, name, age, species, intent(s). (Owner contributes name + photo on their account.) |
| Species (v1) | Dogs, cats, small mammals (rabbit, hamster, guinea pig, ferret, other small mammal). |
| Intent model | Unified deck with intent chip per card. |
| Deck UI | Pet primary on card front + small owner chip (first name + tiny avatar). Tap to expand for full details. |
| Swipe gestures | Left = pass, right = like. No super-like, no down gesture. **Rewind** (undo last swipe) available as a button. |
| Empty deck | Auto-expand search radius with a visible banner ("now showing pets up to 50 km away"). |
| Match | Mutual right swipe opens a 1:1 chat. No application form, no pending inbox. |
| Chat | Text + emoji only. No photos, no voice notes, no scheduler, no GIFs. |
| Match expiration | Never. Matches stay until someone unmatches. |
| Filters (deck-affecting) | Distance, species, intent (multi-select). |
| Filter UX | Set during onboarding; edited in **Settings** only. No in-deck filter bar or button. |
| 'Likes you' | Dedicated tab with **full reveal** — every profile that liked one of your pets is visible. |
| Notifications | Granular per-event toggles in Settings (defaults: matches ON, messages ON, likes ON, weekly digest OFF). |
| Verification | Email verification only (built on existing auth). |
| Location precision shown | Bucketed: `< 5 km`, `5–15 km`, `15–50 km`, `50+ km`. Exact distance never exposed. |
| Profile pause | Manual toggle in Settings — hides all of an owner's pets from decks until un-paused. |
| Reporting | Generic block + report with reasons (spam, fake, harassment, animal welfare concern, other). |
| Pet removal | When an owner deletes a pet, existing chats with that pet's matches **stay fully active**. Pet is removed from the deck only. |
| Account deletion | Soft delete (30-day grace), then hard delete. Configurable later if needed. |
| Onboarding (first pet) | Owner lands in the app after sign-up; an empty **My Pets** state + a persistent banner pushes them to add one. Not blocking. |
| Home navigation | 4 bottom tabs: **Deck / Likes you / Matches / Profile**. The **Matches** tab is a combined inbox with two sections — 'New matches' (matched but no messages yet) and 'Conversations' (active chats). Chat detail is pushed onto the stack when an entry is tapped. |
| **Backend** | **Cloud Firestore** for all data tables; **Firebase Storage** for image uploads. Authenticated via the existing Firebase Auth (login feature). Firestore's local cache is on by default on Android so the app survives brief network drops; no separate Room cache. |

### Out of scope (deliberately)

- Paid tier of any kind (no super-likes, boosts, passport, daily picks, swipe quotas, see-who-liked-you tease).
- Photo verification, government-ID verification.
- Shelter / rescue accounts and listings.
- Adoption application forms.
- Photo sharing, voice notes, GIFs, video, or meeting-scheduler inside chat.
- Match expiration timers.
- Welfare-specific reporting flow (rolled into the generic report's reason list).
- Birds, reptiles, fish, exotics, large livestock (revisit when there's user demand).
- In-deck filter UI (button or chip bar).
- Owner-level filters (age, gender of the human).

## 4. Per-area details

### 4.1 Onboarding & profile

After the existing Sign Up flow completes:

1. New account lands directly in the app, on the **Deck** tab.
2. Deck shows an empty state: **"Add your first pet to start matching"** with a primary button → Add Pet flow.
3. A persistent (dismissable per-session) banner appears at the top of Deck, Likes-you, Matches, and Chats tabs until the owner adds at least one pet.
4. **Add Pet** is a single-screen form: photo picker (camera + gallery), name, age (years), species (Dogs / Cats / Small mammals — sub-pickers reveal once species chosen), intent multi-select chips, short bio (optional).
5. **Filter preferences** are captured at the same moment as the first pet (since they're meaningless without one). Defaults: all intents on, all species on, distance 25 km.
6. Subsequent pets are added from the **Profile** tab → "My Pets" section → "Add another pet".

### 4.2 Deck (Discovery)

- Card content (front, before tap): pet photo (hero), pet name + age, species, intent chip, bucketed distance, small "with `<owner-first-name>`" + avatar chip.
- Tap card → expanded view: full photo gallery, owner name + avatar, bio, full details.
- Gestures: left/right swipe with spring physics + button equivalents (X, ❤). Rewind button (↺) restores the last swiped card.
- When the local pool is exhausted at the user's current radius, the radius auto-doubles (25 → 50 → 100 km) and a banner explains: "Showing pets up to 50 km away — change in Settings". Radius cap configurable; suggested cap 200 km.
- Petless owners (no pet added yet) can browse the deck but the swipe-right gesture / Like button surface an inline "Add a pet to start liking" prompt that opens the Add Pet flow. **Petless owners cannot like.** (Open question 6.1.)

### 4.3 Match & chat

- Match = both sides have liked at least one of each other's pets. (One owner's dog liked, the other owner's two cats liked — counts as a match keyed on the owner-pair, not the pet-pair.)
- On match, both owners see a celebratory match screen and the match appears in the **Matches** tab (no message yet) and slides into **Chats** once either side sends the first message.
- Chat: text + emoji. Unmatch and block are available from the chat header overflow.
- If a pet involved in the match is removed, the chat is unaffected — message: "`<pet-name>` is no longer on TinPet" appears in chat as a system line; both parties can still message.

### 4.4 Filters (Settings only)

- Distance slider (5 km – 200 km, default 25 km).
- Species multi-select (Dogs, Cats, Small mammals — and sub-categories of small mammals).
- Intent multi-select chips (Playdate, Adoption, Friendship; all on by default).
- Saving filters in Settings refreshes the deck immediately on next visit.

### 4.5 Safety

- Email verification gate: account cannot enter the app until the verification link is clicked (built on existing Firebase auth).
- Block: removes the other user from every surface (deck, likes-you, matches, chats) and prevents future re-pairing.
- Report: modal with reasons (Spam / Fake profile / Harassment / Animal welfare concern / Other) + optional free text. Submission writes to a moderation queue (back-end TBD — flagged for the data plan).
- Pause: Settings toggle — owner's pets stop appearing in others' decks; owner can still see the app, browse, and chat.

### 4.6 Notifications

Granular per-event toggles in Settings → Notifications. Defaults:

| Event | Default |
|---|---|
| New match | ON |
| New message in a chat | ON |
| Someone liked one of your pets | ON |
| Weekly digest of new pets nearby | OFF |

In-app notification surface: bell icon on the home top bar (when implemented) — not required for v1.

### 4.7 Account

- **4 bottom tabs:** Deck / Likes you / Matches / Profile. The **Matches** tab is a combined inbox with two sections — 'New matches' (no messages exchanged yet) and 'Conversations' (active chats). Tapping a New matches entry opens the match detail screen; tapping a Conversations entry opens chat detail directly.
- Profile tab contents: owner avatar + name (editable), "My Pets" list (add / edit / delete pet), Settings entry, Sign out, Delete account.
- Delete account: soft delete with 30-day grace period — account hidden, pets removed from deck, chats marked as "this user left TinPet". Hard delete runs as a scheduled job (or on next sign-in if the user comes back, the deletion is cancelled).

## 5. Module plan (high level)

Following the 4-module-per-feature convention in [`plans/ANDROID_APP_SCAFFOLD_PROMPT.md`](./ANDROID_APP_SCAFFOLD_PROMPT.md):

```
:feature
  :feature:deck             // Discovery / swipe surface
    :nav :domain :data :presentation
  :feature:pet              // Pet profile CRUD (add / edit / delete pets)
    :nav :domain :data :presentation
  :feature:likes            // 'Likes you' tab
    :nav :domain :data :presentation
  :feature:match            // Combined inbox (New matches + Conversations) + match detail screen
    :nav :domain :data :presentation
  :feature:chat             // 1:1 text chat detail screen (reached from the Matches tab)
    :nav :domain :data :presentation
  :feature:profile          // Owner's own profile, My Pets section
    :nav :domain :data :presentation
  :feature:settings         // Filters, notifications, safety, pause, sign out, delete account
    :nav :domain :data :presentation
```

Already in place:

```
  :feature:login            // Email/password + Google sign-in
  :feature:home             // Bottom-nav host (to be reshaped to 5 tabs)
  :feature:signup           // Account creation
```

Shared concepts (`:core`):

- `:core:session` (already split: `:domain` JVM + `:data` Android) — current user.
- `:core:designsystem` — extend with pet-card components, intent chips, distance buckets.
- New: `:core:location` (split `:domain` / `:data`) — fetches user location for distance calculations. (Open question 6.3.)

## 6. Open questions (resolve before/during implementation)

1. **Petless-owner swiping.** Plan above: petless owners can browse but cannot like; swipe-right prompts the Add Pet flow. Alternative: queue their likes and activate them once they add a pet. Pick one before implementing Deck.
2. **Multiple intents per pet.** Plan above assumes a pet can declare more than one intent (multi-select). Alternative: one intent per pet (a pet that's both playdate and adoption needs two separate pet profiles). Pick one before implementing the Add Pet form.
3. **Distance computation.** Bucketed display, but the underlying distance still needs to be computed — requires either client-side geolocation + server-side filtering, or server-side filtering only. Decide as part of the backend plan.
4. **Backend.** Resolved: **Cloud Firestore** for all data tables and **Firebase Storage** for binary assets (images). All `:feature:*:data` modules are wired against Firebase from day one — no fake/in-memory phase. Existing Firebase Auth (login feature) supplies the user identity for security rules. The `:core:firebase` module owns the singletons; each feature's `:data` module depends on it. **Why Firestore over Realtime DB?** The existing project scaffold is already wired for Firestore; Firestore's rich queries (`where`/`array-contains`/compound) eliminate the need for denormalized indices that RTDB would have required (no `/likedYouBy`, no `/blockedBy` mirrors — both replaced by direct queries).
5. **Moderation queue.** Reports write to the `reports` collection in Firestore with `allow read: if false` for clients (admin reads via Firebase console only, or via a Cloud Function with custom claims). Manual review via the Firebase console for v1, no in-app admin UI.
6. **Notification delivery.** FCM is already in the project. Server-side triggers for match / message / like events use **Firebase Cloud Functions** with Firestore document triggers (e.g. `onDocumentCreated('matches/{matchId}')` fans out to both owners' FCM tokens). Covered in `plans/notifications-fcm.md`.

## 7. Suggested implementation order

Each step gets its own plan file (`plans/<step>.md`) before code is written.

1. **`plans/pet-profile.md`** — `:feature:pet` (Add / edit / delete pets). Foundational; everything else needs pets to exist.
2. **`plans/deck-swipe.md`** — `:feature:deck` with in-memory fakes. The headline feature; validates the card UI, swipe physics, and intent chips.
3. **`plans/bottom-nav-reshape.md`** — Replace the current Home (Facebook feed) with the 5-tab structure. Touches `:feature:home` and adds skeletons for Likes-you / Matches / Chats / Profile / Settings.
4. **`plans/match-and-chat.md`** — `:feature:match` + `:feature:chat`. Mutual-swipe detection, match list, text chat.
5. **`plans/likes-you.md`** — `:feature:likes` (full-reveal incoming likes).
6. **`plans/owner-profile-settings.md`** — `:feature:profile` + `:feature:settings`. Includes filters, notifications, pause, block/report, delete account.
7. **`plans/notifications-fcm.md`** — Firebase Cloud Functions on Realtime DB paths to fan out FCM push for matches, messages, and likes-you; in-app FCM token registration; scheduled function for any background jobs (e.g. server-side pet purge).

## 8. Out-of-scope for v1, parking lot for v2

- Photo / voice / video in chat
- Meeting-scheduler in chat (for Playdate intent)
- Adoption application form (for Adoption intent)
- Shelter / rescue account type
- Photo verification badge
- Broader species (birds, reptiles, fish)
- Owner-side filters (age, gender)
- Paid tier
- Welfare-specific report routing
- In-deck filter UI

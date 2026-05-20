# Plan: Push notifications + Cloud Functions (`functions/`, `:feature:notifications`)

> **Parent spec:** [`plans/tinpet-app.md`](./tinpet-app.md). **Architecture rules:** [`plans/ANDROID_APP_SCAFFOLD_PROMPT.md`](./ANDROID_APP_SCAFFOLD_PROMPT.md).
>
> **Prereq features:** every prior plan. This is the closing plan that introduces server-side Firebase Cloud Functions (Node.js / TypeScript), wires FCM push delivery, and resolves the deferred "this needs a Cloud Function" items flagged in every earlier plan.
>
> **Scope:** introduces a new top-level `functions/` directory (server-side TypeScript project) and a new client-side `:feature:notifications` module for the permission rationale screen + FCM token sync.

## 1. Goal

Stand up server-side push notification delivery via Firebase Cloud Functions, FCM token registration on the client, the notification permission UX (rationale-first), three notification channels (Matches / Messages / Likes), and deep-link routing from a tapped notification straight into the relevant detail screen.

Bundled into this plan: the server-side fixes for race conditions and cross-feature atomicity that earlier plans deferred — `/likedYouBy` index maintenance, match-creation atomicity, bidirectional block enforcement, pet-deletion system-note propagation, pet hard-purge, account hard-purge, and a write-only moderation triage hook on `/reports` create.

## 2. User stories

### Permission + onboarding

1. As a brand-new owner who just finished sign-up, I'm taken to a **notification rationale screen** before the first deck load: a single screen explaining "Get notified when someone likes your pet, when you match, and when you get messages." with [Allow notifications] (primary) and [Skip for now] (text button).
2. As an owner who tapped Allow, the system POST_NOTIFICATIONS prompt fires; granting permission registers my FCM token in `/owners/{uid}/fcmTokens/{tokenId}` and proceeds to the deck.
3. As an owner who tapped Skip, I proceed to the deck without permission requested. The next time I'd otherwise receive a high-value push (someone likes my pet, I match with someone), the rationale screen reappears as a one-time bottom sheet.

### Push delivery

4. As an owner, when someone likes one of my pets, within ~2 seconds I receive an Android push on the **Likes** channel: title "Sarah liked Buddy", body "Tap to see who's interested." Tapping opens the Likes you tab with Sarah's entry pre-expanded as a bottom sheet.
5. As an owner, when I get a new match, within ~2 seconds I receive a push on the **Matches** channel: title "New match with Sarah!", body "Say hello to Sarah." Tapping opens the match detail screen for that match.
6. As an owner, when I get a new message in a chat I'm not currently viewing, I receive a push on the **Messages** channel: title "Sarah", body the first 100 chars of the message. Tapping opens chat detail with the new message visible.
7. As an owner, if my app is in the foreground in the relevant surface (e.g., I'm already in the chat that received a message), the push is suppressed in favor of the in-app realtime update.
8. As an owner, I can mute any individual channel from Android system settings (long-press a notification → channel settings). The in-app per-event toggles in Settings → Notifications still work alongside this — both gate delivery.

### Behind-the-scenes correctness fixes

9. As an owner who likes a pet via the deck, the `/likedYouBy` index entry is created server-side from the `/likes` create event — even if my client crashed between writes — so the other owner never misses a like-you entry.
10. As an owner, when I mutually-like someone and the deck creates the match, the match record is created exactly once even if both clients fire simultaneously (server-side reconciliation via a transactional Function).
11. As an owner who blocks someone, the bidirectional enforcement happens server-side: a `/blockedBy/{them}/{me} = true` marker is written by a Function, so they can't see me even though they can't read my `/blocks` subtree.
12. As an owner whose pet was the `initiatingLike` for a match and who later deletes that pet, all my active chats from that match automatically get a system-note message ("Buddy is no longer on TinPet") synthesized server-side.
13. As an owner, when my 7-day pet-archive grace expires, the pet's photos are deleted from Storage and the record is purged server-side — even if I never reopen the app.
14. As an owner who requested account deletion 30 days ago, all my data (pets, profile, likes, passes, matches, messages, fcmTokens) is hard-deleted server-side, with chats showing "This user left TinPet" to my former matches.

## 3. Cloud Functions inventory

A new top-level `functions/` directory at the repo root, using Firebase Functions for Node.js (TypeScript). All RTDB triggers use the v2 SDK.

### 3.1 Firestore document triggers

| Function | Trigger | Purpose |
|---|---|---|
| `onLikeCreate` | `onDocumentCreated('likes/{likeId}')` | Send "Someone liked your pet" push to `toOwnerId`; check reciprocity (`likes where fromOwnerId == toOwnerId and toOwnerId == fromOwnerId, limit 1`); if mutual, atomically write `matches/{deterministicId}` (idempotent set + merge). |
| `onMatchCreate` | `onDocumentCreated('matches/{matchId}')` | Send "New match" push to both `participants`. |
| `onMatchDelete` | `onDocumentDeleted('matches/{matchId}')` | **Recursive delete** of the `matches/{matchId}/messages` subcollection (Firestore does not auto-delete subcollections; client deletes are intentionally shallow). |
| `onMessageCreate` | `onDocumentCreated('matches/{matchId}/messages/{messageId}')` | Send "New message" push to the recipient (the participant that isn't `fromOwnerId`). The client already writes `lastMessage*` fields atomically via `runBatch`, so no DB updates needed here. |
| `onPetUpdate` | `onDocumentUpdated('pets/{petId}')` | If `state` flipped to `PURGED`, find every match referencing this pet via `initiatingLike.toPetId`; insert a synthesized system-note message into each related chat. |
| `onReportCreate` | `onDocumentCreated('reports/{reportId}')` | Send a Slack webhook (or email) to the moderation channel. Stub for v1 — log to Functions stdout if no webhook URL configured. |

**What we lost vs. the RTDB design:**

| Removed | Reason |
|---|---|
| `onLikedYouByCreate` | The `/likedYouBy` mirror was an index workaround. Firestore answers "who liked my pets" directly via `likes where toOwnerId == me`, so the Likes-you push is moved into `onLikeCreate` itself. |
| `onLikeDelete` | No mirror to clean up. |
| `onBlockCreate` / `onBlockDelete` | The `/blockedBy` mirror was an index workaround. Firestore answers "who blocked me" directly via `blocks where blockedOwnerId == me`. |

### 3.2 Scheduled functions

| Function | Schedule | Purpose |
|---|---|---|
| `purgeArchivedPets` | `every 24 hours` | Query `pets where state == 'ARCHIVED' and deletedAt <= now - 7d`. For each: delete photos from Storage, update doc to `state = 'PURGED'` with `photos = []`. Supersedes the client-side `PetPurgeWorker` — the WorkManager job is removed in this PR. |
| `purgeDeletedAccounts` | `every 24 hours` | Query `accountDeletions where hardDeleteAt <= now`. For each `uid`: delete `pets where ownerId == uid`, `likes where fromOwnerId == uid OR toOwnerId == uid`, `passes where ownerId == uid`, `passedLikes where toOwnerId == uid`, `blocks where ownerId == uid OR blockedOwnerId == uid`, `matches where participants array-contains uid` (plus each match's `messages` subcollection), `owners/{uid}`, `fcmTokens where ownerId == uid`, `accountDeletions/{uid}`. Delete avatar from Storage. Auth account also `auth().deleteUser(uid)`. |
| `sendWeeklyDigest` | `every monday 09:00` | For each owner with `notifications.weeklyDigest == true` and `location != null`: count active pets within their `filters.maxDistanceKm` that they haven't swiped on; if > 0, push "<N> new pets to meet near you" on the **Matches** channel (low priority). |

### 3.3 Function structure (TypeScript)

```
functions/
├── package.json
├── tsconfig.json
├── .eslintrc.js
└── src/
    ├── index.ts                          // re-exports every function
    ├── triggers/
    │   ├── onLikeCreate.ts
    │   ├── onLikeDelete.ts
    │   ├── onLikedYouByCreate.ts
    │   ├── onMatchCreate.ts
    │   ├── onMessageCreate.ts
    │   ├── onBlockCreate.ts
    │   ├── onBlockDelete.ts
    │   ├── onPetStateChange.ts
    │   └── onReportCreate.ts
    ├── scheduled/
    │   ├── purgeArchivedPets.ts
    │   ├── purgeDeletedAccounts.ts
    │   └── sendWeeklyDigest.ts
    └── shared/
        ├── fcm.ts                        // sendToUser(uid, channel, payload)
        ├── database.ts                   // typed Refs for /pets, /matches, /messages, etc.
        ├── matching.ts                   // match-id computation, reciprocity check
        └── prefs.ts                      // notification-pref lookup
```

Each trigger function under ~50 lines; logic delegated to `shared/`.

### 3.4 FCM payload shape

```typescript
// shared/fcm.ts
type Channel = "matches" | "messages" | "likes";

interface Payload {
    title: string;
    body: string;
    channel: Channel;
    deepLink: string;             // e.g. "tinpet://chat/abc123"
    data?: Record<string, string>; // for in-app routing
}

async function sendToUser(uid: string, payload: Payload): Promise<void> {
    // 1. Read /owners/{uid}/notificationPrefs; bail if this event type is disabled.
    // 2. Read /owners/{uid}/fcmTokens/* — collect all tokens.
    // 3. For each token, send via FCM Admin SDK with:
    //    {
    //      notification: { title, body },
    //      android: {
    //        notification: {
    //          channelId: payload.channel,                  // "matches" | "messages" | "likes"
    //          clickAction: "OPEN_TINPET",                  // intent filter on MainActivity
    //        },
    //      },
    //      data: {
    //        deepLink: payload.deepLink,
    //        ...payload.data,
    //      },
    //    }
    // 4. On UNREGISTERED / INVALID_ARGUMENT errors, delete the stale token from /owners/{uid}/fcmTokens.
}
```

### 3.5 Push title/body templates

| Event | Channel | Title | Body |
|---|---|---|---|
| New match | matches | "New match with `<first name>`!" | "Say hello to `<first name>`." |
| New message | messages | "`<first name>`" | first 100 chars of the message |
| Someone liked your pet | likes | "`<liker first name>` liked `<my pet name>`" | "Tap to see who's interested." |
| Weekly digest | matches (low priority) | "`<N>` new pets to meet near you" | "Open the deck to find your next match." |

Names come from `/owners/{uid}/profile.firstName`; if absent, fall back to "Someone". Pet names from `/pets/{petId}.name`; if absent, fall back to "your pet".

## 4. FCM token sync (client side)

### 4.1 Schema

```
/owners/{uid}/fcmTokens/{tokenId} = {
  token:     <fcm token>,
  platform:  "android",
  createdAt: <epoch ms>,
  updatedAt: <epoch ms>,
}
```

`{tokenId}` = the FCM token's hash (so re-registering the same token is idempotent — overwrites rather than duplicates).

Multiple tokens per user supported (multi-device). Cloud Functions fan out to every token in the subtree.

### 4.2 Sync rules

| Event | Action |
|---|---|
| App launches & user signed in | Read current FCM token; write to `/owners/{uid}/fcmTokens/{hash}`. |
| FCM token refresh (`FirebaseMessagingService.onNewToken`) | Read new token; delete old entry (best effort); write new entry. |
| User signs out | Delete `/owners/{uid}/fcmTokens/{hash}` for THIS device's token (other devices stay registered). |
| User taps Skip on permission prompt | No token write attempted (FCM still issues a token, but we don't register it server-side until the user grants permission). |
| Cloud Function detects stale token (FCM error) | Function deletes the entry. Client will re-write on next launch. |

### 4.3 Module: `:core:firebase` additions

A new `FcmTokenSync` class injected at the `:app` level and invoked from `MainActivity.onCreate` (or a `LifecycleObserver` keyed to the process):

```kotlin
@Singleton
class FcmTokenSync @Inject constructor(
    private val sessionRepo: SessionRepository,
    private val database: FirebaseDatabase,
    private val messaging: FirebaseMessaging,
    private val clock: Clock,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    suspend fun syncForSignedInUser() = withContext(io) {
        val uid = sessionRepo.currentUidOrNull() ?: return@withContext
        if (!hasNotificationPermission()) return@withContext
        val token = messaging.token.await()
        val hash = token.sha256().take(40)
        val now = clock.now().toEpochMilliseconds()
        database.reference.child("owners/$uid/fcmTokens/$hash").setValue(mapOf(
            "token"     to token,
            "platform"  to "android",
            "createdAt" to ServerValue.TIMESTAMP,   // honored as create-time only via security rule
            "updatedAt" to now,
        )).await()
    }

    suspend fun clearForSignOut(uid: String, token: String) = withContext(io) {
        val hash = token.sha256().take(40)
        database.reference.child("owners/$uid/fcmTokens/$hash").removeValue().await()
    }
}
```

A small `TinPetMessagingService : FirebaseMessagingService` lives in `:app` (since it needs to be declared in `AndroidManifest.xml`):

```kotlin
class TinPetMessagingService : FirebaseMessagingService() {
    @Inject lateinit var fcmTokenSync: FcmTokenSync
    @Inject lateinit var sessionRepo: SessionRepository
    @Inject lateinit var notificationRenderer: NotificationRenderer  // §5.3

    override fun onNewToken(token: String) {
        // Sync via FcmTokenSync (background coroutine launched on a small CoroutineScope).
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // Functions send via `notification` payload, so Android auto-renders when the app is backgrounded.
        // For foreground delivery, we receive the data payload and decide:
        //   - If the user is currently on the relevant surface (e.g., already in this chat), suppress.
        //   - Otherwise, hand off to NotificationRenderer to display in-app.
    }
}
```

### 4.4 Security rules for `fcmTokens`

Top-level collection (so the `purgeDeletedAccounts` function can query across all tokens for a user). Document id is the SHA-256 token hash; an `ownerId` field links the token to its user.

```
match /fcmTokens/{tokenId} {
  allow read:   if false;                                              // never read from clients
  allow create: if request.auth != null
                && request.resource.data.ownerId == request.auth.uid;
  allow update: if request.auth != null
                && resource.data.ownerId == request.auth.uid;
  allow delete: if request.auth != null
                && resource.data.ownerId == request.auth.uid;
}
```

Tokens are write-only from clients. Cloud Functions read with admin privileges.

## 5. Notification permission flow + channels

### 5.1 Rationale screen (post-signup onboarding step)

New route `NotificationRationale` in `:feature:notifications:nav`. Inserted into the post-signup flow: after a new account is created and the owner profile is written, the navigator pushes `NotificationRationale` instead of going directly to `DeckHome`. Existing signed-in users skip this step.

Screen content:

```
Hero illustration (notification bell or pet wave)
Headline: "Stay in the loop"
Body:
    "Get notified when:
    • Someone likes your pet
    • You match with another owner
    • You get a new message"
PrimaryButton("Allow notifications")  → request system permission
TextButton("Skip for now")            → navigate to DeckHome
```

Tap "Allow notifications" → `ActivityResultContracts.RequestPermission(POST_NOTIFICATIONS)` →

- Granted → `FcmTokenSync.syncForSignedInUser()` → `navigator.replaceAll(DeckHome)`
- Denied → snackbar "You can enable notifications anytime in Settings → Notifications" → `navigator.replaceAll(DeckHome)` after dismiss

Tap "Skip for now" → `navigator.replaceAll(DeckHome)` without permission request.

### 5.2 Re-prompt heuristic

If the user skipped at onboarding, the rationale appears once more as a `ModalBottomSheet` the first time any of these happens:

- They receive an in-app like (visible because RTDB observed the new `/likedYouBy` entry while the app was open) without permission granted.
- They get a match (deck celebration shows; no push since permission missing).

The sheet is shown at most **once total** post-skip (tracked in `:core:datastore` via a `PromptedAgain: Bool` preference). After that, opt-in lives only in Settings → Notifications (which links to system settings if app-level permission is denied).

### 5.3 Notification rendering (NotificationRenderer in `:app`)

Three Android channels created on `Application.onCreate` (Android 8+):

```kotlin
class TinPetApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService<NotificationManager>()!!
        manager.createNotificationChannel(NotificationChannel(
            "matches", "Matches", NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = "New matches and weekly highlights" })
        manager.createNotificationChannel(NotificationChannel(
            "messages", "Messages", NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = "New chat messages" })
        manager.createNotificationChannel(NotificationChannel(
            "likes", "Likes", NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = "Someone liked one of your pets" })
    }
}
```

For background delivery, Android's FCM SDK uses the `channelId` from the push payload automatically — no in-app rendering code needed. For **foreground** delivery (when the user is in the app but not in the relevant surface), `TinPetMessagingService.onMessageReceived` constructs a `NotificationCompat.Builder` manually with the same channel id and posts it.

**Foreground suppression rule:** before posting in-foreground, check if the current top route is the relevant surface for this notification's `deepLink`:

| Notification | Suppress if current route is |
|---|---|
| New message (chat X) | `ChatRoute(matchId == X)` |
| New match (match X) | `MatchDetail(matchId == X)` OR `MatchesHome` |
| Someone liked | `LikesHome` |

The check uses `navigator.backStack.lastOrNull()` injected via Hilt.

## 6. Deep link routing

### 6.1 URI scheme

Custom scheme `tinpet://` registered in `AndroidManifest.xml`:

```xml
<activity android:name=".MainActivity" ...>
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data android:scheme="tinpet" />
    </intent-filter>
</activity>
```

URI patterns:

| URI | Destination |
|---|---|
| `tinpet://chat/{matchId}` | `ChatRoute(matchId)` |
| `tinpet://match/{matchId}` | `MatchDetail(matchId)` |
| `tinpet://likes` | `LikesHome` |
| `tinpet://deck` | `DeckHome` |

### 6.2 Routing implementation

`MainActivity.onCreate` (and `onNewIntent` for warm starts):

```kotlin
private fun handleDeepLink(intent: Intent?) {
    val uri = intent?.data ?: return
    if (uri.scheme != "tinpet") return
    val route: Any = when (uri.host) {
        "chat"  -> ChatRoute(uri.lastPathSegment ?: return)
        "match" -> MatchDetail(uri.lastPathSegment ?: return)
        "likes" -> LikesHome
        "deck"  -> DeckHome
        else    -> return
    }
    navigator.replaceAll(route)
}
```

This is invoked **after** the existing `StartDestination` resolution — so an unauthenticated tap still routes to `LoginHome` first; the intent is consumed but doesn't override auth state.

For the `LikesHome` deep link, the ViewModel reads an optional `prefocusKey: LikeKey?` from the `Intent.extras` and immediately opens the corresponding bottom sheet on first composition.

### 6.3 FCM `notification` vs `data`-only payloads

Cloud Functions send **both** a `notification` block (so the system tray handles backgrounded apps with zero client code) and a `data` block carrying the `deepLink`. When the user taps:

- Backgrounded: Android launches `MainActivity` with the URI in `intent.data`.
- Foregrounded: our handler in `onMessageReceived` constructs the notification with a `PendingIntent` carrying the URI.

## 7. Cross-plan integration — what this PR removes / supersedes

This plan moves several pieces of logic from client to server. The corresponding client-side code is **removed** in this PR:

| Removed | Reason |
|---|---|
| Client-side match-reciprocity check + match write in `FirestoreDeckRepository.submitSwipe` ([`plans/deck-swipe.md`](./deck-swipe.md) §5.4) | Replaced by `onLikeCreate` Function. Client now only writes the `likes` doc; it learns about a match via a one-shot Firestore read on `matches/{deterministicId}` after the local right-swipe (with a short timeout) — if the Function created the match within ~500ms, the deck shows the celebration; otherwise, the push delivers it. |
| Client-side recursive delete of `messages` subcollection in `unmatch` ([`plans/match-and-chat.md`](./match-and-chat.md) §5.1) | Replaced by `onMatchDelete` Function. Client only deletes the `matches/{matchId}` document. |
| Client-side system-note synthesis in chat for pet-deletion ([`plans/match-and-chat.md`](./match-and-chat.md) §9 open question 3) | Replaced by `onPetUpdate` Function which inserts a SystemNote message directly into `matches/{matchId}/messages` for relevant matches. |
| `PetPurgeWorker` (WorkManager) in `:feature:pet:data` ([`plans/pet-profile.md`](./pet-profile.md) §7) | Replaced by `purgeArchivedPets` scheduled Function. Worker class + scheduling code in `:app` removed. |

Each removal is a small, surgical change. Document them in the PR description so a reviewer can connect the dots.

## 8. New client modules

### 8.1 `:feature:notifications`

```
:feature:notifications
  :nav            // route key NotificationRationale (pure JVM)
  :presentation   // NotificationRationaleScreen + ViewModel + EntryProviderInstaller
```

No `:domain` / `:data` — permission state is system-side; FCM token sync lives in `:core:firebase`.

Dependencies:

```
:feature:notifications:nav
  └─► (nothing or :core:navigation — pure JVM, no Android, no Compose)

:feature:notifications:presentation
  ├─► :feature:notifications:nav
  ├─► :feature:deck:nav            (post-rationale destination)
  ├─► :core:designsystem
  ├─► :core:ui
  ├─► :core:navigation
  └─► :core:firebase               (FcmTokenSync)
```

Wire into `:app/build.gradle.kts`. Register the route via Hilt multibinding.

### 8.2 `:core:firebase` additions

- `FcmTokenSync` class + Hilt provider for `FirebaseMessaging`.
- Small `String.sha256()` extension in `:core:common` (pure JVM).

No new module needed — extends what's already there.

### 8.3 `:app` additions

- `TinPetMessagingService : FirebaseMessagingService` (declared in `AndroidManifest.xml`).
- `NotificationRenderer` for foreground rendering with channel + deep-link wiring.
- `Application.onCreate` creates the three channels.
- `MainActivity.onCreate` + `onNewIntent` parse `tinpet://` URIs and route via `Navigator.replaceAll`.

## 9. Server project setup

### 9.1 Firebase Functions init

From the repo root:

```bash
firebase login
firebase init functions
# Pick: TypeScript, ESLint enabled, install deps
```

This creates `functions/` with `package.json`, `tsconfig.json`, `.eslintrc.js`, and `src/index.ts`.

### 9.2 `package.json` essentials

```json
{
  "engines": { "node": "20" },
  "main": "lib/index.js",
  "scripts": {
    "build": "tsc",
    "serve": "npm run build && firebase emulators:start --only functions,database",
    "deploy": "firebase deploy --only functions",
    "logs": "firebase functions:log"
  },
  "dependencies": {
    "firebase-admin": "^12",
    "firebase-functions": "^5"
  }
}
```

### 9.3 Deployment

```bash
cd functions
npm run build
npm run deploy
```

Cloud Functions run in the project's Firebase region (set in `firebase.json`). Logs available via `npm run logs` or the Firebase console.

### 9.4 Local development

The Firebase emulator suite (`firebase emulators:start`) runs Functions + RTDB locally. The Android app's debug build can be pointed at the emulator via `FirebaseDatabase.getInstance().useEmulator("10.0.2.2", 9000)` gated on `BuildConfig.DEBUG`.

## 10. Security & cost considerations

- **Cloud Function read fan-out cost.** `onLikeCreate` reads `/likes/{targetOwner}` for reciprocity — small per-owner subtree, cheap. `purgeArchivedPets` scans `/pets` daily — keep query indexed on `state` and `deletedAt` to avoid full-tree scans.
- **FCM send cost.** Free up to ~600/sec sustained; matches/messages/likes volume in v1 is comfortably within that.
- **Function cold starts.** ~1–2s for v1 traffic levels. Acceptable for non-critical paths (push delivery within ~3s end-to-end is fine). For latency-sensitive ones (match celebration), client-side fast-path (the one-shot `/matches/{deterministicId}` lookup mentioned in §7) handles the common case.
- **Function permission model.** Functions use `firebase-admin` SDK — full DB and Storage access. No security rule enforcement applies to Functions; rules only apply to client SDKs. All trigger logic must validate inputs (e.g., reject like writes where `fromOwnerId != auth.uid` — though the security rule already prevents this from the client, defense in depth).
- **`/blockedBy` privacy.** The denormalized index is read-only for the blocked party (so they can hide themselves from blocked relationships), but they can read who blocked them, which leaks info. Acceptable for v1; v2 should keep `/blockedBy` private and have the Function maintain views server-side.

## 11. Open questions / future work

1. **Email / web push.** Out of scope — Android push only in v1.
2. **iOS APNs.** Out of scope — Android-only project.
3. **Notification action buttons** ("Like back" inline on a like push) — possible with Android Action buttons + a small `BroadcastReceiver`. Defer to v2 polish.
4. **Notification translations / locale-specific phrasing.** v1 English-only. Move title/body templates to localized resources when adding language support.
5. **Cloud Function unit tests.** Add `firebase-functions-test` for trigger unit testing. Initial PR ships e2e tests against the emulator; pure unit tests follow.
6. **Function-side rate-limiting / abuse prevention.** A user spamming `/likes` could trigger many `onLikeCreate` invocations. v1 acceptable; v2 add per-uid rate counters in a `/rateLimit/{uid}` subtree.
7. **Backfill of `/blockedBy` and `/likedYouBy` for pre-existing data.** If this PR ships after some users already have data, a one-off backfill function reads `/blocks/*` and `/likes/*` and writes the indices. Trivial Function; document in the PR.
8. **Reverse-engineer detection of low-quality matches.** Out of scope — server-side ML for ranking comes much later.

## 12. Out of scope

- Real moderation tooling (admin web UI, role-based assignment, audit log).
- Notification scheduling / quiet hours / Do-Not-Disturb integration beyond Android system defaults.
- Push to multiple platforms (iOS, web, desktop).
- In-app notification center (a bell icon listing recent activity). The push tray is the surface in v1.
- Notification analytics (open rate, conversion). Add when there's product signal worth measuring.

## 13. Verification

### 13.1 Cloud Functions

1. Local emulator round-trip:
   ```bash
   cd functions && npm run serve
   ```
   In another terminal, run the Android app pointed at the emulator. Trigger each event and verify:
   - Create a `/likes` entry as Owner A → see `/likedYouBy/{B}/{...}` appear → see B's app receive a push (via emulator/Firebase console).
   - Send a chat message → see `/matches/{matchId}/lastMessage*` update → see the other side receive a push.
   - Create a `/blocks` entry → see `/blockedBy` mirror appear.
   - Change a pet's `state` to `PURGED` → see SystemNote messages appear in relevant `/messages/{matchId}` chats.
2. Scheduled functions:
   ```bash
   firebase emulators:start --only functions,database
   # In another shell:
   firebase functions:shell
   # Then in the shell:
   purgeArchivedPets()
   purgeDeletedAccounts()
   sendWeeklyDigest()
   ```
   Verify each completes without errors and writes the expected outputs.
3. Deploy to staging Firebase project:
   ```bash
   firebase use staging
   firebase deploy --only functions
   firebase functions:log
   ```

### 13.2 Android client

1. Build:
   ```bash
   JAVA_HOME=/Users/jrodiz/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home ./gradlew assembleDebug
   ```
2. JVM isolation:
   ```bash
   ./gradlew :feature:notifications:nav:dependencies --configuration runtimeClasspath
   ```
3. Manual on emulator (Android 13+ to exercise POST_NOTIFICATIONS):
   - Fresh sign-up → see the **Notification rationale** screen → tap Allow → system prompt appears → grant.
   - Sign in on a second emulator as another user. Have them like one of your pets via the deck.
   - First emulator: receive a push on the **Likes** channel. Long-press → see channel name "Likes" → tap → opens Likes you tab with that entry pre-expanded.
   - Mutually like back → receive a push on the **Matches** channel for both sides. Tap → opens match detail.
   - Send a message in chat → other side receives push on the **Messages** channel. Tap → opens chat detail with the new message visible.
   - Mute the Likes channel in system settings → trigger another like → verify no push (but the in-app realtime update still works).
   - Skip the rationale at onboarding → receive a like → verify the re-prompt sheet appears exactly once.
   - Sign out → verify your FCM token entry is removed from `/owners/{uid}/fcmTokens/{hash}` (check Firebase console).
4. Cross-removal regression:
   - Delete a pet → verify the 7-day grace runs; trigger `purgeArchivedPets` manually (emulator) before 7d to confirm photos delete from Storage and `/pets/{petId}.state` flips to `PURGED`.
   - Schedule account deletion → run `purgeDeletedAccounts` immediately after (override `hardDeleteAt` to past) → verify all data subtrees are gone.
5. Capture screenshots of: the rationale screen, a backgrounded push on the lockscreen, a foreground in-app notification, the three channels in system Settings, and a deep-link tap successfully opening chat detail.

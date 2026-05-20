# Plan: Match feature (combined inbox) + Chat feature (1:1 text chat)

> **Parent spec:** [`plans/tinpet-app.md`](./tinpet-app.md). **Architecture rules:** [`plans/ANDROID_APP_SCAFFOLD_PROMPT.md`](./ANDROID_APP_SCAFFOLD_PROMPT.md).
>
> **Prereq features:** [`plans/pet-profile.md`](./pet-profile.md), [`plans/deck-swipe.md`](./deck-swipe.md), [`plans/bottom-nav-reshape.md`](./bottom-nav-reshape.md). The deck already writes `/matches` records when mutual likes are detected; this plan adds the screens that consume those records and the chat schema layered on top.
>
> **Backend:** Cloud Firestore — extends the existing `matches` collection (from deck-swipe.md) with chat-related fields and adds a `messages` subcollection under each match. No new Storage usage. No Cloud Functions in this PR — that's deferred to `plans/notifications-fcm.md`.

## 1. Goal

Build the **Matches** tab and the chat detail screen on top of the existing match data that the deck already produces.

The Matches tab is a **combined inbox** with two sections:

- **New matches** — matches with no messages yet. Tapping opens the match detail screen with "who liked which pet" context and a "Say hello" CTA that opens chat.
- **Conversations** — matches with at least one message exchanged. Tapping opens chat detail directly.

The Chat feature provides 1:1 text-and-emoji messaging between two matched owners, with **read receipts** (sent ✓ / read ✓✓). No photos, no voice notes, no typing indicators, no match expiration. Unmatch lives only inside chat detail (overflow menu).

## 2. User stories

### Match (Matches tab)

1. As an owner, when I tap the Matches tab and have at least one match, I see two clearly labeled sections: "New matches" at the top, "Conversations" below.
2. As an owner, each New matches entry shows the other owner's primary pet photo, the other owner's first name, when the match happened ("Matched 2h ago"), and the match's first intent chip. Tapping opens the match detail screen.
3. As an owner, each Conversations entry shows the other owner's primary pet photo, the other owner's first name, the last message preview (first ~60 chars), and the last message timestamp ("9:42 PM" today, "Tue" this week, date otherwise). Tapping opens chat detail.
4. As an owner with no matches at all, I see the empty-state mockup from the bottom-nav reshape plan ("No matches yet").
5. As an owner with matches but none in one section, I see the populated section with its header and a short hint where the other section would be ("No conversations yet — say hi to your new matches!" or "All your matches are in conversations").
6. As an owner viewing match detail, I see: (a) my pet(s) that the other owner liked, (b) the other owner's pet(s) that I liked, each with the timestamp of the like. A primary "Say hello" button opens chat detail for this match.

### Chat (chat detail)

7. As an owner in chat detail, I see the other owner's matched pet's photo + first name in the header, the message history below (oldest at top, newest at bottom), and a text input pinned to the bottom.
8. As an owner, I can type a message (text + emoji via system keyboard) and send it. My message appears in the list immediately; the other side receives it within ~1s.
9. As an owner, when the other side reads my message, my outgoing message's ✓ becomes ✓✓ (or the second check tints to the primary color).
10. As an owner, when I open a chat, all unread incoming messages become "read" — their sender sees the ✓✓ update.
11. As an owner, I can scroll up to load older messages (lazy pagination, ~50 messages per page).
12. As an owner, when I open the overflow menu in chat detail, I see **Unmatch** — tapping it shows a confirmation modal explaining "This deletes your conversation with `<first name>` for both of you." Confirming unmatches and returns me to the Matches tab.
13. As an owner, if the other owner unmatches me while I'm in the chat, the screen shows a non-blocking banner "`<first name>` is no longer matched with you", the input field disables, and a back button is the only action.
14. As an owner, if the other owner deletes the pet involved in our match, the chat continues unaffected — a system line appears in the message stream: "`<pet name>` is no longer on TinPet" (per the parent plan §4.3).

## 3. Domain model

### 3.1 In `:feature:match:domain` (pure JVM)

```kotlin
@JvmInline value class MatchId(val value: String)

data class Match(
    val id: MatchId,
    val ownerAId: OwnerId,           // sorted ascending by uid
    val ownerBId: OwnerId,
    val createdAt: Instant,
    val lastMessageAt: Instant?,     // null = no messages yet → "New matches" section
    val lastMessagePreview: String?, // null or first ~60 chars
    val lastMessageFromOwnerId: OwnerId?,
) {
    fun otherOwnerId(me: OwnerId): OwnerId = if (ownerAId == me) ownerBId else ownerAId
    val hasMessages: Boolean get() = lastMessageAt != null
}

data class MatchSummary(
    val match: Match,
    val otherOwner: OwnerSummary,        // from :core:session:domain or :feature:profile:domain
    val anchorPet: Pet,                  // the other owner's pet to show in the row (their most-recently-liked-by-me pet)
)

data class MatchDetail(
    val match: Match,
    val otherOwner: OwnerSummary,
    val myPetsLikedByOther: List<PetLike>,    // their likes targeting my pets, newest first
    val theirPetsLikedByMe: List<PetLike>,    // my likes targeting their pets, newest first
)

data class PetLike(
    val pet: Pet,                         // from :feature:pet:domain
    val likedAt: Instant,
)

interface MatchRepository {
    /** All matches for the current owner, sorted by max(createdAt, lastMessageAt) desc. Continuous stream. */
    fun observeInbox(): Flow<List<MatchSummary>>

    /** A single match with its full like-history context. */
    fun observeMatchDetail(id: MatchId): Flow<MatchDetail?>

    /** Removes a match + all messages + both /matchesByOwner index entries. */
    suspend fun unmatch(id: MatchId)
}
```

UseCases:

- `ObserveInboxUseCase` — emits `InboxState(newMatches: List<MatchSummary>, conversations: List<MatchSummary>)` derived from `Match.hasMessages`.
- `ObserveMatchDetailUseCase`
- `UnmatchUseCase`

### 3.2 In `:feature:chat:domain` (pure JVM)

```kotlin
@JvmInline value class MessageId(val value: String)

data class Message(
    val id: MessageId,
    val matchId: MatchId,                    // from :feature:match:domain
    val fromOwnerId: OwnerId,
    val text: String,                        // 1..2000 chars after trim
    val createdAt: Instant,
    val readBy: Map<OwnerId, Instant>,       // who has read it, and when
)

sealed interface ChatEntry {
    data class UserMessage(val msg: Message) : ChatEntry
    data class SystemNote(val text: String, val at: Instant) : ChatEntry   // e.g. "<pet> is no longer on TinPet"
}

interface ChatRepository {
    /** Continuous stream of chat entries (messages + synthesized system notes), oldest first. */
    fun observeChat(matchId: MatchId, pageSize: Int = 50): Flow<List<ChatEntry>>

    /** Load older entries before the oldest currently loaded; returns true if more exist. */
    suspend fun loadOlder(matchId: MatchId, beforeMessageId: MessageId, pageSize: Int = 50): Boolean

    /** Send a text message. Validates non-empty, ≤ 2000 chars after trim. */
    suspend fun sendMessage(matchId: MatchId, text: String): Message

    /** Mark all currently-unread incoming messages in this chat as read by me. */
    suspend fun markAllRead(matchId: MatchId)
}
```

UseCases: `ObserveChatUseCase`, `SendMessageUseCase`, `MarkAllReadUseCase`, `LoadOlderMessagesUseCase`.

## 4. Firestore schema additions

The deck already created `matches/{matchId}` per [`plans/deck-swipe.md`](./deck-swipe.md) §5.1. This plan **extends** that document with three new fields and adds a `messages` subcollection.

### 4.1 Extended `matches/{matchId}` document

```
matches/{matchId} = {
  // existing (from deck):
  ownerAId, ownerBId, participants, createdAt, initiatingLike,

  // new (added by chat on first message; absent until then):
  lastMessageAt:         Timestamp?,
  lastMessagePreview:    string?,          // first 60 chars, trimmed
  lastMessageFromOwnerId: string?,
}
```

The inbox query orders by `max(createdAt, lastMessageAt)`. Since Firestore can't compute this in a query, the client fetches a small page sorted by `lastMessageAt desc` (composite index `(participants array-contains, lastMessageAt desc)`) and re-sorts in memory to factor in matches without messages — fine for inbox sizes < ~500 entries.

### 4.2 New `matches/{matchId}/messages/{messageId}` subcollection

```
matches/{matchId}/messages/{messageId} = {
  fromOwnerId: string,
  text:        string,
  createdAt:   Timestamp,
  readBy:      map<string, Timestamp>,    // who-read → when (sender included on send)
}
```

`{messageId}` is Firestore-auto-generated, so it sorts chronologically by default.

**Composite index:** `messages` (collection-group, optional for v1): `(createdAt asc)` — handles chat pagination.

### 4.3 Security rules

```
match /matches/{matchId} {
  // (read/write rules from deck-swipe.md §5.2 still apply)
}

match /matches/{matchId}/messages/{messageId} {
  allow read:   if request.auth != null
                && request.auth.uid in get(/databases/$(database)/documents/matches/$(matchId)).data.participants;
  allow create: if request.auth != null
                && request.resource.data.fromOwnerId == request.auth.uid
                && request.auth.uid in get(/databases/$(database)/documents/matches/$(matchId)).data.participants;
  allow update: if request.auth != null
                && request.auth.uid in get(/databases/$(database)/documents/matches/$(matchId)).data.participants
                && request.resource.data.diff(resource.data).affectedKeys().hasOnly(['readBy']);  // only readBy can be patched after create
  allow delete: if false;                                                                          // no message deletion in v1
}
```

Only the two participants can read or write messages. Senders set `fromOwnerId` to themselves on create. After create, only the `readBy` map can be updated (no edit/delete) — enforced by the `diff().affectedKeys()` check.

> The repeated `get(...)` inside rules incurs a billed document read per evaluation. For higher write volume we'd cache participants by denormalizing onto each message; v1 accepts the cost.

## 5. Data layer

### 5.1 `:feature:match:data`

Depends on `:feature:match:domain`, `:feature:pet:domain` (for `Pet`), `:core:firebase`, `:core:session:domain`.

```kotlin
@Singleton
class FirebaseMatchRepository @Inject constructor(
    private val sessionRepo: SessionRepository,
    private val database: FirebaseDatabase,
    private val clock: Clock,
    @IoDispatcher private val io: CoroutineDispatcher,
) : MatchRepository {

    override fun observeInbox(): Flow<List<MatchSummary>> = callbackFlow {
        // Listen on /matchesByOwner/{uid}, fan out to /matches/{matchId} for each.
        // For each match: resolve otherOwnerId, fetch /owners/{otherOwner}/profile for OwnerSummary,
        // pick the "anchor pet" = the most-recently-liked-by-me pet owned by the other side
        // (read /likes/{me}/* filtered by toOwnerId == other) → fetch /pets/{petId}.
        // Combine into MatchSummary; sort by max(createdAt, lastMessageAt) desc.
    }

    override fun observeMatchDetail(id: MatchId): Flow<MatchDetail?> = callbackFlow {
        // Listen on /matches/{id}; null on removal.
        // Resolve myPetsLikedByOther by reading /likes/{other}/* where toOwnerId == me.
        // Resolve theirPetsLikedByMe by reading /likes/{me}/* where toOwnerId == other.
        // Fan out pet reads, attach likedAt timestamps.
    }

    override suspend fun unmatch(id: MatchId) = withContext(io) {
        // Firestore can't recursively delete subcollections client-side.
        // v1: client deletes the matches/{matchId} doc; a Firestore-trigger
        // Cloud Function (see notifications-fcm.md) recursively deletes the
        // messages subcollection. The match-list snapshot listener on both
        // sides removes the row as soon as the parent doc is gone.
        firestore.collection("matches").document(id.value).delete().await()
    }
}
```

Hilt:

```kotlin
@Module @InstallIn(SingletonComponent::class)
abstract class MatchDataModule {
    @Binds @Singleton
    abstract fun bindMatchRepository(impl: FirebaseMatchRepository): MatchRepository
}
```

### 5.2 `:feature:chat:data`

Depends on `:feature:chat:domain`, `:feature:match:domain` (for `MatchId`), `:feature:pet:domain` (for system-note triggers like "<pet> is no longer on TinPet"), `:core:firebase`, `:core:session:domain`.

```kotlin
@Singleton
class FirebaseChatRepository @Inject constructor(
    private val sessionRepo: SessionRepository,
    private val database: FirebaseDatabase,
    private val clock: Clock,
    @IoDispatcher private val io: CoroutineDispatcher,
) : ChatRepository {

    override fun observeChat(matchId: MatchId, pageSize: Int): Flow<List<ChatEntry>> = callbackFlow {
        // Query /messages/{matchId} orderByChild("createdAt") limitToLast(pageSize).
        // Listen for child added/changed/removed.
        // Merge in synthesized SystemNote entries (e.g., when /pets/{petId}.state changes to PURGED
        // for a pet that was an initiatingLike for this match).
    }

    override suspend fun loadOlder(matchId: MatchId, beforeMessageId: MessageId, pageSize: Int): Boolean = withContext(io) {
        // Query endBefore(beforeMessageId) limitToLast(pageSize). Cache results so observeChat appends them.
    }

    override suspend fun sendMessage(matchId: MatchId, text: String): Message = withContext(io) {
        val trimmed = text.trim()
        require(trimmed.isNotEmpty() && trimmed.length <= 2000)
        val me = currentUid()
        val matchRef = firestore.collection("matches").document(matchId.value)
        val messageRef = matchRef.collection("messages").document()              // auto-id

        firestore.runBatch { batch ->
            batch.set(messageRef, mapOf(
                "fromOwnerId" to me,
                "text"        to trimmed,
                "createdAt"   to FieldValue.serverTimestamp(),
                "readBy"      to mapOf(me to FieldValue.serverTimestamp()),
            ))
            batch.update(matchRef, mapOf(
                "lastMessageAt"          to FieldValue.serverTimestamp(),
                "lastMessagePreview"     to trimmed.take(60),
                "lastMessageFromOwnerId" to me,
            ))
        }.await()
        // Read the just-written message back (or build locally with a near-now timestamp).
    }

    override suspend fun markAllRead(matchId: MatchId) = withContext(io) {
        val me = sessionRepo.currentUid()
        // Query /messages/{matchId} where !readBy.contains(me) and fromOwnerId != me, last 50.
        // Batch updateChildren: /messages/{matchId}/{messageId}/readBy/{me} = now for each.
    }
}
```

Hilt:

```kotlin
@Module @InstallIn(SingletonComponent::class)
abstract class ChatDataModule {
    @Binds @Singleton
    abstract fun bindChatRepository(impl: FirebaseChatRepository): ChatRepository
}
```

**Cross-feature writes:** `sendMessage` writes to `/matches/{matchId}/lastMessage*`, which is conceptually owned by `:feature:match`. This is intentional — atomic in a single `updateChildren` call, and the alternative (a separate Cloud Function) is deferred to `plans/notifications-fcm.md`. The write is permitted by the `/matches` security rule because the sender is one of the match participants.

### 5.3 Read-receipt strategy

Tinder/Bumble compute read state per-message. We do the same:

- Sender's outgoing message in the UI shows `✓` if `readBy` contains only the sender's uid (i.e., the recipient hasn't seen it), or `✓✓` once the recipient's uid appears in `readBy`.
- When the user opens chat detail, `markAllRead(matchId)` runs after the screen settles (debounced by ~300ms to avoid spam on quick-tab-flicks). Subsequent incoming messages are marked read on receipt while the screen is foregrounded.
- No "online/typing" presence — not in scope. Typing indicators were explicitly declined.

## 6. Presentation layer

### 6.1 `:feature:match:presentation`

Depends on `:feature:match:domain`, `:feature:match:nav`, `:feature:chat:nav` (to navigate to chat detail), `:feature:pet:domain` (for `Pet` model used in MatchDetail), `:core:designsystem`, `:core:ui`, `:core:navigation`.

#### Screens

| Screen | Route key (`:nav`) | Purpose |
|---|---|---|
| Inbox (Matches tab) | `MatchesHome` | Combined inbox: "New matches" + "Conversations" sections. |
| Match detail | `MatchDetail(matchId)` | Pets + who-liked-which context + "Say hello" CTA. |

`Routes.kt`:

```kotlin
@Serializable data object MatchesHome
@Serializable data class MatchDetail(val matchId: String)
```

#### InboxScreen layout

```
TopBar             // "Matches"
LazyColumn
├─ if newMatches.isNotEmpty():
│   ├─ SectionHeader("New matches", count)
│   └─ items(newMatches) { MatchRow(...) }                  // tap → MatchDetail(matchId)
├─ else: SectionHint("Match with someone to get started")
│
├─ if conversations.isNotEmpty():
│   ├─ SectionHeader("Conversations", count)
│   └─ items(conversations) { ConversationRow(...) }       // tap → ChatRoute(matchId)
└─ else if newMatches.isNotEmpty(): SectionHint("Say hi to your new matches above!")
```

When both lists are empty: the bottom-nav-reshape empty state ("No matches yet") replaces the LazyColumn entirely.

- `MatchRow`: avatar (other owner's pet primary photo, 56dp circle) + first name + "Matched 2h ago" + small intent chip → trailing chevron.
- `ConversationRow`: same avatar layout + first name + last message preview (1 line, ellipsized) + last message timestamp (right-aligned).

#### MatchDetailScreen layout

```
TopBar             // back arrow + other owner's first name
Column (scrollable)
├─ Header
│   ├─ Other owner's primary pet photo (large, centered)
│   ├─ "<other first name>" (headline)
│   └─ "Matched <time-ago>"
├─ Section: "They liked your pets"
│   └─ for each myPetsLikedByOther:
│       PetRowCompact(pet) + "liked <time-ago>"
├─ Section: "You liked their pets"
│   └─ for each theirPetsLikedByMe:
│       PetRowCompact(pet) + "liked <time-ago>"
└─ BottomBar (sticky)
    └─ PrimaryButton("Say hello") → navigator.goTo(ChatRoute(matchId))
```

#### EntryProviderInstaller

```kotlin
@Module @InstallIn(ActivityRetainedComponent::class)
object MatchNavModule {
    @IntoSet @Provides
    fun provideMatchEntries(navigator: Navigator): EntryProviderInstaller = {
        entry<MatchesHome>  { InboxScreen(
            onOpenMatch = { id -> navigator.goTo(MatchDetail(id.value)) },
            onOpenChat  = { id -> navigator.goTo(ChatRoute(id.value)) },
        ) }
        entry<MatchDetail> { key -> MatchDetailScreen(
            matchId = MatchId(key.matchId),
            onSayHello = { navigator.goTo(ChatRoute(key.matchId)) },
            onBack     = { navigator.back() },
        ) }
    }
}
```

### 6.2 `:feature:chat:presentation`

Depends on `:feature:chat:domain`, `:feature:chat:nav`, `:feature:match:domain` (for `MatchId`), `:feature:match:nav` (to navigate back after unmatch), `:core:designsystem`, `:core:ui`, `:core:navigation`.

#### Screens

| Screen | Route key (`:nav`) | Purpose |
|---|---|---|
| Chat detail | `ChatRoute(matchId)` | 1:1 conversation. |

`Routes.kt`:

```kotlin
@Serializable data class ChatRoute(val matchId: String)
```

#### ChatScreen layout

```
TopBar
├─ Back arrow
├─ Avatar (other owner's pet, 32dp circle) + first name + (optional) "matched X ago" subtitle
└─ Overflow menu: [Unmatch]
LazyColumn (reverseLayout = true → newest at bottom, scrolls up to load older)
├─ MessageBubble (incoming or outgoing, with ✓ / ✓✓ for outgoing)
├─ SystemNoteRow (e.g., "Buddy is no longer on TinPet")
└─ if more older messages available: LoadOlderTrigger (auto-fires when in view)
Composer (pinned bottom)
├─ TextField (multiline up to 4 visible lines, max 2000 chars)
└─ Send button (disabled when text is blank or whitespace-only)
Banner (conditional, top of message list)
└─ if other side unmatched: "<first name> is no longer matched with you" — input disables
```

#### MessageBubble

```
Row (start- or end-aligned based on fromOwnerId)
└─ Box
    ├─ Background: primaryContainer (outgoing) or surfaceVariant (incoming), rounded 16dp
    ├─ Text (with selectable copy)
    └─ Footer row (timestamp + read-receipt for outgoing)
        ├─ Timestamp ("9:42 PM")
        └─ if outgoing: ✓ (sent) or ✓✓ (read) — primary color when read
```

#### Unmatch flow

Overflow menu → `Unmatch` →

```
AlertDialog(
    title = "Unmatch <first name>?",
    text  = "This deletes your conversation with <first name> for both of you. You won't see each other in the deck again.",
    confirm = "Unmatch",
    dismiss = "Cancel",
)
```

On confirm: call `UnmatchUseCase(matchId)` → on success, `navigator.replaceAll(MatchesHome)` (pop the chat off the stack and return to inbox).

#### Real-time + lifecycle behavior

- `observeChat(matchId)` is collected via `repeatOnLifecycle(STARTED)` so the listener tears down when the chat is backgrounded.
- `markAllRead(matchId)` fires on first composition and on every new incoming message while the screen is in the foreground (debounced 300ms).
- If the match disappears from RTDB while the user is in chat (other side unmatched), the screen shows the banner and disables input.

#### EntryProviderInstaller

```kotlin
@Module @InstallIn(ActivityRetainedComponent::class)
object ChatNavModule {
    @IntoSet @Provides
    fun provideChatEntries(navigator: Navigator): EntryProviderInstaller = {
        entry<ChatRoute> { key -> ChatScreen(
            matchId = MatchId(key.matchId),
            onBack  = { navigator.back() },
            onUnmatched = { navigator.replaceAll(MatchesHome) },
        ) }
    }
}
```

### 6.3 Deck integration (small follow-up)

The deck's match-celebration overlay currently dismisses without navigation (per [`plans/deck-swipe.md`](./deck-swipe.md) §6.3). Once `:feature:chat:nav` exists, wire the celebration's "Say hello" button:

```kotlin
// In DeckScreen / DeckViewModel:
onSayHello = { matchId -> navigator.goTo(ChatRoute(matchId)) }
```

`:feature:deck:presentation` adds a `:feature:chat:nav` dependency. Trivial — handled as part of this PR.

## 7. Module structure

```
:feature:match
  :nav            // Pure JVM. Route keys: MatchesHome, MatchDetail(matchId).
  :domain         // Pure JVM. Match, MatchSummary, MatchDetail, PetLike, MatchRepository, UseCases.
  :data           // Android. FirebaseMatchRepository.
  :presentation   // Android/Compose. InboxScreen, MatchDetailScreen, EntryProviderInstaller.

:feature:chat
  :nav            // Pure JVM. Route key: ChatRoute(matchId).
  :domain         // Pure JVM. Message, ChatEntry, ChatRepository, UseCases.
  :data           // Android. FirebaseChatRepository.
  :presentation   // Android/Compose. ChatScreen, MessageBubble, Composer, EntryProviderInstaller.
```

Dependency edges:

```
:feature:match:domain
  ├─► :feature:pet:domain        (Pet)
  ├─► :core:session:domain       (OwnerId)
  └─► :core:common

:feature:match:data
  ├─► :feature:match:domain
  ├─► :feature:pet:domain
  ├─► :core:firebase
  ├─► :core:session:domain
  └─► :core:common

:feature:match:presentation
  ├─► :feature:match:domain
  ├─► :feature:match:nav
  ├─► :feature:chat:nav          (navigate to chat detail)
  ├─► :feature:pet:domain        (Pet model for MatchDetail rendering)
  ├─► :core:designsystem
  ├─► :core:ui
  ├─► :core:navigation
  └─► :core:session:domain

:feature:chat:domain
  ├─► :feature:match:domain      (MatchId)
  ├─► :core:session:domain
  └─► :core:common

:feature:chat:data
  ├─► :feature:chat:domain
  ├─► :feature:match:domain
  ├─► :feature:pet:domain        (for system-note synthesis)
  ├─► :core:firebase
  ├─► :core:session:domain
  └─► :core:common

:feature:chat:presentation
  ├─► :feature:chat:domain
  ├─► :feature:chat:nav
  ├─► :feature:match:domain      (MatchId)
  ├─► :feature:match:nav         (navigate back to MatchesHome after unmatch)
  ├─► :core:designsystem
  ├─► :core:ui
  ├─► :core:navigation
  └─► :core:session:domain
```

Convention plugins:
- `:nav`, `:domain` → `tinpet.jvm.library` (+ `tinpet.kotlin.serialization` for `:nav`).
- `:data` → `tinpet.android.library` + `tinpet.hilt`.
- `:presentation` → `tinpet.android.feature`.

Register `:feature:match:presentation` and `:feature:chat:presentation` in `:app/build.gradle.kts` so Hilt multibinding picks up both `EntryProviderInstaller`s.

## 8. Out of scope

- Cloud Functions for atomic message bookkeeping, push fanout, and server-side unmatch cleanup — covered in `plans/notifications-fcm.md`.
- Typing indicators — explicitly declined.
- Photos, voice notes, GIFs, video, link previews, meeting scheduler — explicitly declined by the parent plan.
- Message reactions, edit, delete-for-me, delete-for-everyone.
- Message search.
- Group chats / multi-party matches (every match is 2 owners).
- Per-tab unread badge counts in the bottom nav — captured in this plan's open questions; landed as a small follow-up.
- Match list filtering / sorting beyond "newest activity first".
- Block from chat (Block is the Profile / Safety feature's concern — `plans/owner-profile-settings.md`).
- Report user from chat (same — Safety feature).

## 9. Open questions / future work

1. **Read-receipt write contention.** Two participants writing `readBy` simultaneously on the same message is fine (RTDB merges different map keys), but if both sides scroll quickly through 50+ messages, that's 50+ writes from each. Acceptable for v1; future optimization: client-side batched single-write per chat session.
2. **Unmatch race.** If A unmatches while B is composing a message, B's `sendMessage` will hit a security-rule rejection because the match record is gone. Handle gracefully in the UI: detect the failure, show the "no longer matched" banner, discard the draft.
3. **Pet-deletion system notes.** Synthesizing "`<pet name>` is no longer on TinPet" requires the chat data layer to know about pet state changes. v1: observe `/pets/{petId}.state` for any pet referenced by the match's `initiatingLike`; emit a `SystemNote` when state flips to `PURGED`. Doesn't cover pets that *weren't* part of the initiating like but were also matched. Server-side fanout (Cloud Function on `/pets/{petId}.state` change) is the long-term fix.
4. **Inbox-row anchor pet selection.** Current rule: "most-recently-liked-by-me pet owned by the other side". If the user later removes that pet, the row falls back to "any active pet of theirs"; if none, fall back to a generic avatar. Document the cascade in the impl.
5. **Bottom-nav badge on Matches tab.** Out of scope for this PR (per bottom-nav-reshape decision). Trivial to add later: extend `BottomTab` with `badgeFlow: Flow<Int>?` and wire `ObserveInboxUseCase().map { it.newMatches.size + it.conversations.count { hasUnread(it) } }`.

## 10. Verification

1. Build:
   ```bash
   JAVA_HOME=/Users/jrodiz/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home ./gradlew assembleDebug
   ```
2. JVM isolation for the four pure-Kotlin modules:
   ```bash
   ./gradlew :feature:match:nav:dependencies     --configuration runtimeClasspath
   ./gradlew :feature:match:domain:dependencies  --configuration runtimeClasspath
   ./gradlew :feature:chat:nav:dependencies      --configuration runtimeClasspath
   ./gradlew :feature:chat:domain:dependencies   --configuration runtimeClasspath
   ```
   None should pull in `androidx.*`, Compose, Room, Retrofit, Hilt, or Firebase SDKs.
3. Unit tests (in `:feature:match:domain`, `:feature:chat:domain`, and `:data` with Firebase emulator):
   - `Match.otherOwnerId(me)` returns the correct side for both A and B.
   - `Match.hasMessages` reflects `lastMessageAt` nullness.
   - `ObserveInboxUseCase` partitions matches into `newMatches` (no messages) and `conversations` (has messages) and sorts each by max(createdAt, lastMessageAt) desc.
   - `sendMessage` writes both the message node AND `/matches/{id}/lastMessage*` atomically.
   - `markAllRead` only updates messages NOT sent by me and NOT already containing my uid in `readBy`.
   - `unmatch` removes the match record, both index entries, and all messages — verify the message subtree is empty after.
4. Manual on two emulators (or one emulator + Firebase console as the "other user"):
   - Match with another test user via the deck.
   - Open Matches tab → see "New matches" section with the match. Tap → MatchDetail shows correct "they liked your pets" / "you liked theirs" content.
   - Tap "Say hello" → ChatScreen opens. Send a message.
   - Observe on the other side: message arrives within ~1s; the row moves from "New matches" → "Conversations" with preview + timestamp.
   - Reply from the other side; verify the first ✓ becomes ✓✓ after the original sender opens the chat.
   - Overflow → Unmatch → confirm. Verify the match row disappears from both sides' inboxes, the chat history is gone, and the deck no longer pairs the two owners.
   - Have one side delete the pet that was the `initiatingLike`. Verify a system-note row appears in the existing chat for the other side: "`<pet name>` is no longer on TinPet". Chat continues to work.
   - Capture screenshots of (a) populated inbox with both sections, (b) MatchDetail, (c) ChatScreen mid-conversation with read receipts visible, and surface their paths.

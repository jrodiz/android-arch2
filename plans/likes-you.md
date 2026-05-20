# Plan: Likes you feature (`:feature:likes`)

> **Parent spec:** [`plans/tinpet-app.md`](./tinpet-app.md). **Architecture rules:** [`plans/ANDROID_APP_SCAFFOLD_PROMPT.md`](./ANDROID_APP_SCAFFOLD_PROMPT.md).
>
> **Prereq features:** [`plans/pet-profile.md`](./pet-profile.md), [`plans/deck-swipe.md`](./deck-swipe.md), [`plans/bottom-nav-reshape.md`](./bottom-nav-reshape.md), [`plans/match-and-chat.md`](./match-and-chat.md). This plan **replaces** the empty-state placeholder created by `bottom-nav-reshape.md` with the full Likes you implementation.
>
> **Backend:** adds one new Firestore collection (`passedLikes`) layered on the existing `likes` collection from the deck plan. **No denormalized index is required** — Firestore answers "who liked my pets" directly with `likes where toOwnerId == me`. This is a key simplification over the Realtime-DB-era version of this plan.

## 1. Goal

Build the **Likes you** tab — the surface where an owner sees every other owner who has liked one of their pets. The parent plan committed to **full reveal** (no paywall, every liker visible), so the design is generous: a 2-column grid of pet cards, tap to expand with full info, two-button action (Like back / Pass) in a bottom sheet.

"Like back" performs an ordinary like on the liker's anchor pet, which — because they've already liked one of mine — immediately satisfies the deck's reciprocity check and produces a match. "Pass" hides the entry from this tab without affecting the deck (the liker can still appear in my deck independently).

## 2. User stories

1. As an owner, when I tap the **Likes you** tab and at least one person has liked one of my pets, I see a **2-column grid** of pet cards — each card represents one like.
2. As an owner, each card shows: a representative pet photo of the liker, the liker's first name, the intent chip of that pet, and a small footer "liked `<my pet name>` · `<time-ago>`" so I know which of my pets was liked.
3. As an owner with multiple pets and multiple likers, I see **one entry per like** (most granular): if Sarah liked Buddy AND Sarah liked Whiskers, I see two cards from Sarah, each footnoted with the respective pet.
4. As an owner, the grid is sorted **newest first** (most recent like at top-left).
5. As an owner, tapping a card opens a **bottom sheet** showing the liker's anchor pet in full (photo pager, name/age/species, intent chips, bio, distance bucket, owner first name). The sheet has two prominent buttons at the bottom: **Like back** and **Pass**.
6. As an owner, tapping **Like back** records my like on the liker's anchor pet, which produces a match (since they've already liked one of mine). The bottom sheet dismisses, the card animates out of the grid, and the match-celebration overlay (same one as the deck) appears. The match now appears in the Matches tab.
7. As an owner, tapping **Pass** records the dismissal, the bottom sheet closes, and the card animates out of the grid. The liker can still appear in my main deck (Pass on a like is not the same as a deck-pass).
8. As an owner with no likes, I see the empty-state mockup from [`plans/bottom-nav-reshape.md`](./bottom-nav-reshape.md) ("No one yet").
9. As an owner whose liker has deleted the pet they liked me with (or whose liker has deleted **all** their pets so there's no anchor pet to show), that entry disappears from my list automatically (it was conditional on both sides having something to show).
10. As an owner whose liker has unmatched / blocked me after liking, that entry disappears.

## 3. Domain model

In `:feature:likes:domain` — pure Kotlin/JVM.

```kotlin
@JvmInline value class LikeKey(val value: String)   // "${fromOwnerId}_${toPetId}"

data class IncomingLike(
    val key: LikeKey,
    val fromOwnerId: OwnerId,
    val fromOwner: OwnerSummary,            // first name + avatar
    val anchorPet: Pet,                      // pet of the liker shown on the card; pick rule in §5.3
    val toPetId: PetId,                      // which of MY pets was liked
    val toPetName: String,                   // denormalized for the card footer
    val likedAt: Instant,
)

interface LikesYouRepository {
    /** All incoming likes for the current owner, sorted by likedAt desc. Continuous stream. */
    fun observeLikesYou(): Flow<List<IncomingLike>>

    /** Records the recipient passing on this like — entry hides from the list. Liker can still appear in my deck. */
    suspend fun pass(key: LikeKey)

    /** "Like back" — performs a like on the liker's anchor pet, returning the result (Pending or Match). */
    suspend fun likeBack(key: LikeKey): SwipeResult        // SwipeResult from :feature:deck:domain
}
```

UseCases:

- `ObserveLikesYouUseCase`
- `PassLikeUseCase`
- `LikeBackUseCase`

> `LikeBackUseCase` delegates to `:feature:deck:domain`'s `SubmitSwipeUseCase` internally (Like Back is just a like on the liker's anchor pet — same reciprocity logic, same match-creation, same `/matches` write). That means `:feature:likes:domain` depends on `:feature:deck:domain`.

## 4. Firestore schema additions

One new top-level collection on top of the existing `likes` collection from the deck plan.

### 4.1 Existing `likes` collection (no change)

The deck plan already created `likes/{likeId} = { fromOwnerId, toOwnerId, toPetId, createdAt }` with the composite index `(toOwnerId asc, createdAt desc)`. That index — declared in `firestore.indexes.json` — is precisely what makes "find every like targeting me" a single query:

```kotlin
firestore.collection("likes")
    .whereEqualTo("toOwnerId", uid)
    .orderBy("createdAt", Query.Direction.DESCENDING)
    .limit(500)                                  // safety cap; v2 paginates
```

No `/likedYouBy` mirror, no maintenance burden.

### 4.2 `passedLikes` — recipient passes

```
passedLikes/{passId} = {                         // passId = "${toOwnerId}_${fromOwnerId}_${toPetId}"
  toOwnerId:    string,
  fromOwnerId:  string,
  toPetId:      string,
  passedAt:     Timestamp,
}
```

Written by the recipient when they tap **Pass** in the Likes you bottom sheet. The Likes you query filters out entries whose key appears in this set (client-side filter against the small set returned by `passedLikes where toOwnerId == me`).

Not written when the recipient **likes back** — the like-back generates a match, which the deck/match flow handles, and the entry naturally disappears.

**Composite index:** `(toOwnerId asc, passedAt desc)` — drives the recipient-side fetch.

### 4.3 Security rules

```
match /passedLikes/{passId} {
  allow read:   if request.auth != null
                && resource.data.toOwnerId == request.auth.uid;
  allow create: if request.auth != null
                && request.resource.data.toOwnerId == request.auth.uid;
  allow delete: if request.auth != null
                && resource.data.toOwnerId == request.auth.uid;
}
```

Only the recipient (`toOwnerId`) reads, creates, or deletes their own passed-like entries.

## 5. Data layer

In `:feature:likes:data` (Android module). Depends on `:feature:likes:domain`, `:feature:pet:domain` (for `Pet`), `:feature:deck:domain` (for delegating `Like Back` to the deck's `submitSwipe`), `:core:firebase`, `:core:session:domain`.

### 5.1 Implementation

```kotlin
@Singleton
class FirebaseLikesYouRepository @Inject constructor(
    private val sessionRepo: SessionRepository,
    private val database: FirebaseDatabase,
    private val petRepo: PetRepository,                // for fetching anchor pet + my pet name
    private val deckRepo: DeckRepository,              // for delegating likeBack
    private val clock: Clock,
    @IoDispatcher private val io: CoroutineDispatcher,
) : LikesYouRepository {

    override fun observeLikesYou(): Flow<List<IncomingLike>> = callbackFlow {
        // 1. Listen on /likedYouBy/{me} orderByChild("createdAt").
        // 2. Listen on /passedLikes/{me} for the exclusion set.
        // 3. For each surviving entry:
        //    a. Resolve fromOwner profile from /owners/{fromOwnerId}/profile.
        //    b. Resolve my toPet name from /pets/{toPetId}.
        //    c. Resolve anchor pet (see §5.3) from /pets/* indexed by ownerId == fromOwnerId.
        // 4. Drop entries where any required piece is missing (liker has no active pets, or my pet was purged, etc.).
        // 5. Emit sorted list (newest first).
    }

    override suspend fun pass(key: LikeKey) = withContext(io) {
        val me = sessionRepo.currentUid()
        database.reference.child("passedLikes/$me/${key.value}")
            .setValue(clock.now().toEpochMilliseconds()).await()
    }

    override suspend fun likeBack(key: LikeKey): SwipeResult = withContext(io) {
        val anchorPetId = resolveAnchorPetIdForKey(key)
            ?: return@withContext SwipeResult.Pending     // liker has no pet to like back; treat as no-op
        deckRepo.submitSwipe(anchorPetId, SwipeAction.LIKE)
    }
}
```

Hilt:

```kotlin
@Module @InstallIn(SingletonComponent::class)
abstract class LikesDataModule {
    @Binds @Singleton
    abstract fun bindLikesYouRepository(impl: FirebaseLikesYouRepository): LikesYouRepository
}
```

### 5.2 What "anchor pet" means

Each card represents one like. The like targets one of MY pets (clear) but originates from an owner who may have multiple pets. The card shows ONE of the liker's pets — the "anchor pet" — so the card has a face.

Anchor-pet selection rule (deterministic, refreshes when the liker's roster changes):

1. Pick the liker's `ACTIVE` pet whose `intents` overlap with the intent of the liked-toPet (matching context). If multiple, pick the most-recently-`updatedAt`.
2. Fallback: any `ACTIVE` pet of the liker, most-recently-updated.
3. Fallback: if the liker has zero active pets, **drop the entry from the grid** (user story #9 — there's nothing to show).

Recomputed on each emission of `observeLikesYou()`. Cheap enough — the liker's pet count is small.

### 5.3 Like-back atomicity

Calling `deckRepo.submitSwipe(anchorPetId, LIKE)` will:

1. Write `/likes/{me}/{anchorPetId}` (with the index write per §7).
2. Check reciprocity — find at least one existing like from the anchor pet's owner toward any of my pets. **Always succeeds** here because the entry only exists in my likes-you because the other side has liked one of my pets.
3. Write the match record + both `/matchesByOwner` indices.
4. Return `SwipeResult.Match(matchId)`.

The Likes you UI then:
- Animates the card out of the grid.
- Shows the deck's match-celebration overlay (same composable, hoisted into a `:core:designsystem` slot so both features can use it).
- The match appears in the Matches tab on next emission of `observeInbox()`.

## 6. Presentation layer

In `:feature:likes:presentation` (Android, Compose). Depends on `:feature:likes:domain`, `:feature:likes:nav`, `:feature:pet:domain`, `:feature:deck:nav` (for the empty-state CTA "Go to Deck"), `:feature:deck:domain` (for `SwipeResult`), `:core:designsystem`, `:core:ui`, `:core:navigation`.

> The `LikesScreen` and `LikesNavModule` created in `bottom-nav-reshape.md` are **replaced** by this PR's full implementation. The empty-state composable (`EmptyTabState`) added to `:core:designsystem` in that PR is reused for the no-likes case.

### 6.1 Screen

| Screen | Route key (`:nav`) | Purpose |
|---|---|---|
| Likes you grid | `LikesHome` (existing from bottom-nav-reshape) | 2-column grid of incoming likes; tap → bottom sheet. |

(No new route; the bottom sheet is hosted inside `LikesScreen`.)

### 6.2 ViewModel

```kotlin
data class LikesUiState(
    val likes: List<IncomingLike> = emptyList(),
    val isLoading: Boolean = true,
    val expandedKey: LikeKey? = null,                 // non-null → bottom sheet open
    val pendingAction: PendingAction? = null,         // optimistic UI guard
    val matchCelebration: MatchId? = null,
)

enum class PendingAction { LIKING_BACK, PASSING }

sealed interface LikesEvent {
    data class CardTapped(val key: LikeKey) : LikesEvent
    data object SheetDismissed                : LikesEvent
    data object LikeBackTapped                : LikesEvent       // applies to expandedKey
    data object PassTapped                    : LikesEvent       // applies to expandedKey
    data object MatchCelebrationDismissed     : LikesEvent
    data object GoToDeck                      : LikesEvent       // empty-state CTA
}
```

The ViewModel collects `ObserveLikesYouUseCase` into `likes`, exposes events, and applies optimistic updates: when the user taps Like back or Pass, the card is removed from the visible list immediately; on backend failure (rare), it re-appears with a snackbar.

### 6.3 Composable structure

```
LikesScreen
├─ if isLoading → centered CircularProgressIndicator
├─ else if likes.isEmpty() → EmptyTabState(icon = Favorite, headline = "No one yet", body = "...", cta = "Go to Deck")
├─ else
│   ├─ TopBar           // "Likes you" + small subtitle "<N> waiting"
│   └─ LazyVerticalGrid(columns = Fixed(2), contentPadding = 12.dp, spacing = 12.dp)
│       └─ items(likes) { like -> LikeCard(like, onTap = { CardTapped(like.key) }) }
├─ if expandedKey != null → ModalBottomSheet { LikeBottomSheet(...) }
└─ if matchCelebration != null → MatchCelebrationOverlay(...)
```

#### LikeCard (grid tile)

```
Card (aspectRatio 3:4, rounded 16dp, elevation 2dp)
├─ AsyncImage(anchorPet.photos[0])
├─ IntentChip(anchorPet.intents.first(), top-end)
├─ BottomGradient + Column
│   ├─ Text("<liker first name>", titleSmall)
│   └─ Text("liked <toPetName> · <time-ago>", labelSmall, alpha 0.85)
```

Tap on the card → fire `CardTapped(like.key)`.

#### LikeBottomSheet

`ModalBottomSheet`, 90% screen height, content:

```
HorizontalPager(anchorPet.photos)
└─ AsyncImage per page + page indicator dots
Column (scrollable)
├─ Pet name, age, species
├─ Intent chips (all of anchorPet.intents)
├─ Distance bucket
├─ Bio (if present)
└─ OwnerChip ("<liker first name>" + avatar)
ActionBar (pinned to bottom of the sheet)
├─ OutlinedButton("Pass", weight 1f) → PassTapped
└─ FilledButton("Like back", weight 1f) → LikeBackTapped
```

#### MatchCelebrationOverlay

Reuses the same composable that the deck shows on a mutual match (hoisted into `:core:designsystem` as part of this PR — small refactor noted in §7). Auto-dismisses after 5s; tap-to-dismiss works too. No "Say hello" navigation in v1 (consistent with deck's celebration overlay; chat integration is the same small follow-up captured in `plans/match-and-chat.md` §6.3).

### 6.4 EntryProviderInstaller

Replaces the placeholder from `bottom-nav-reshape.md`:

```kotlin
@Module @InstallIn(ActivityRetainedComponent::class)
object LikesNavModule {
    @IntoSet @Provides
    fun provideLikesEntries(navigator: Navigator): EntryProviderInstaller = {
        entry<LikesHome> { LikesScreen(
            onGoToDeck = { navigator.replaceAll(DeckHome) },
        ) }
    }
}
```

## 7. Deck integration — none required

In the Firestore version of the spec, the Likes you feature reads directly from the `likes` collection via the `(toOwnerId, createdAt)` composite index that the deck plan already declared. **No edits to `FirestoreDeckRepository.submitSwipe` are required.** This is a notable simplification — the original RTDB design needed the deck to maintain a `/likedYouBy` mirror written atomically with each like; Firestore's query layer eliminates that.

## 8. Other small refactors bundled into this PR

- **`MatchCelebrationOverlay` hoisted to `:core:designsystem`.** Currently defined in `:feature:deck:presentation`. The Likes you feature needs the same composable on `likeBack` success, so move it to a shared location and update the deck to import from there. Pure code move, no behavior change.
- **`IntentChip` hoisted to `:core:designsystem`** if not already there. The deck card, match detail, likes-you card, and likes-you bottom sheet all render it identically.

## 9. Module structure & dependencies

```
:feature:likes
  :nav            // Pure JVM. Existing: LikesHome route (from bottom-nav-reshape).
  :domain         // NEW (this PR). LikeKey, IncomingLike, LikesYouRepository, UseCases.
  :data           // NEW. FirebaseLikesYouRepository.
  :presentation   // REPLACED (was placeholder). LikesScreen + ViewModel + bottom sheet + EntryProviderInstaller.
```

Dependencies:

```
:feature:likes:domain
  ├─► :feature:pet:domain
  ├─► :feature:deck:domain        (SwipeResult + delegation to SubmitSwipeUseCase)
  ├─► :core:session:domain
  └─► :core:common

:feature:likes:data
  ├─► :feature:likes:domain
  ├─► :feature:pet:domain
  ├─► :feature:deck:domain        (DeckRepository for likeBack delegation)
  ├─► :core:firebase
  ├─► :core:session:domain
  └─► :core:common

:feature:likes:presentation
  ├─► :feature:likes:domain
  ├─► :feature:likes:nav
  ├─► :feature:deck:nav           (empty-state CTA "Go to Deck")
  ├─► :feature:deck:domain        (SwipeResult — for the LikeBackUseCase return type)
  ├─► :feature:pet:domain
  ├─► :core:designsystem
  ├─► :core:ui
  ├─► :core:navigation
  └─► :core:session:domain
```

Convention plugins:
- `:nav`, `:domain` → `tinpet.jvm.library`.
- `:data` → `tinpet.android.library` + `tinpet.hilt`.
- `:presentation` → `tinpet.android.feature`.

## 10. Out of scope

- Tab badge / unread count on the bottom-nav Likes you tab — captured as a small follow-up in §11.
- Filters inside Likes you (by intent or by liked pet) — explicitly declined.
- Swipe-to-dismiss on grid tiles — bottom sheet only; keeps interaction symmetrical with deck card expansion.
- Animated card-disappears choreography — fade out is enough for v1.
- "Liked you most recently" vs "liked you with multiple pets" sorting — strict chronological newest-first for v1.
- Pagination — assumes likes < ~500 per user (realistic for v1). Add `limitToLast(500)` as a guard; full pagination is a v2 concern.
- Bulk actions ("Pass all", "Like back all") — never (encouraging thoughtless mutuals is bad product).

## 11. Open questions / future work

1. **Likes you tab badge.** Trivially derivable from `observeLikesYou().map { it.size }` once `BottomTab` gains a `badgeFlow` slot. Bundle with the broader "tab badges" work the parent plan defers.
2. **Server-side index maintenance.** The `/likedYouBy` index is currently written by the liker's client. A misbehaving client could fail to write it, causing missed entries. A Cloud Function on `/likes/*/*` create/delete that mirrors the index is the proper fix — captured in `plans/notifications-fcm.md`.
3. **Cleanup when a my-pet is deleted.** When I delete one of my pets (state → ARCHIVED, then PURGED via the 7-day worker), incoming likes targeting that pet should disappear from my Likes you grid. The view layer can filter "drop if `toPetId` no longer corresponds to any of my pets"; the proper cleanup (delete `/likedYouBy/{me}/*_petId` entries) belongs in `PetPurgeWorker` (`:feature:pet:data` §7). Add to the worker's responsibilities when this plan lands.
4. **Pass undo.** No undo in v1 — passes are silently committed. If user feedback shows accidental passes are common, a 5-second snackbar with "Undo" is the standard fix.
5. **Block/report integration.** When a liker is blocked from Settings (`plans/owner-profile-settings.md`), their incoming like entries must vanish. View layer filter against `/blocks/{me}/*`.

## 12. Verification

1. Build:
   ```bash
   JAVA_HOME=/Users/jrodiz/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home ./gradlew assembleDebug
   ```
2. JVM isolation:
   ```bash
   ./gradlew :feature:likes:nav:dependencies     --configuration runtimeClasspath
   ./gradlew :feature:likes:domain:dependencies  --configuration runtimeClasspath
   ```
   No `androidx.*`, Compose, Room, Retrofit, Hilt, or Firebase SDKs.
3. Unit tests (in `:feature:likes:domain` and `:data` with Firebase emulator):
   - `LikeKey` round-trips through `compoundKey = "${fromOwnerId}_${toPetId}"` correctly even when uids contain `_` (use the **last** underscore as the split point if needed; assert with test cases).
   - Anchor-pet selection: prefers a pet whose intent overlaps with the toPet intent; falls back to most-recently-updated; drops the entry if no active pets.
   - `pass(key)` writes to `/passedLikes/{me}/{key}` and the next `observeLikesYou` emission omits the entry.
   - `likeBack(key)` delegates to `deckRepo.submitSwipe(anchorPetId, LIKE)` and propagates `SwipeResult.Match`.
   - Updated deck `submitSwipe`: writes both `/likes` and `/likedYouBy` atomically; partial-failure paths.
4. Manual on two emulators (or emulator + Firebase console):
   - Sign in as User A; have them like one of User B's pets via the deck.
   - Open User B's app, tap Likes you → see exactly one grid card representing User A's like, with the correct anchor pet, intent chip, "liked `<my pet name>` · just now" footer.
   - Tap the card → bottom sheet opens with User A's anchor pet in full + Pass / Like back buttons.
   - Tap Pass → sheet closes, card animates out, list now shows empty state.
   - Repeat: have User A like a different pet of User B. Verify a new card appears.
   - Tap Like back → sheet closes, match-celebration overlay appears, match appears in Matches tab for both users.
   - Have User A delete the pet they originally used (or delete all their pets) — verify the corresponding Likes you entry disappears from User B's grid.
   - Capture a screenshot of the populated Likes you grid and the open bottom sheet with both action buttons visible, and surface their paths.

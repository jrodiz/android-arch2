# Plan: Swipe Deck feature (`:feature:deck`)

> **Parent spec:** [`plans/tinpet-app.md`](./tinpet-app.md). **Architecture rules:** [`plans/ANDROID_APP_SCAFFOLD_PROMPT.md`](./ANDROID_APP_SCAFFOLD_PROMPT.md). **Prereq feature:** [`plans/pet-profile.md`](./pet-profile.md) — the deck reads pets created by that feature.
>
> **Backend:** Cloud Firestore + Firebase Storage (per the parent spec). No in-memory or fake-data layer.

## 1. Goal

Implement the **Deck** tab — TinPet's headline surface. Owners swipe through pet cards from other owners, like or pass, and trigger matches on mutual likes. The deck is a unified feed: Playdate / Adoption / Friendship cards interleave with an intent chip on each card.

This feature owns the swipe mechanics, the card stack, the photo cycling on each card, the expanded-detail bottom sheet, the like/pass storage, and the on-device match detection that fires when a swipe completes a mutual like. The downstream **Match** feature (separate plan) consumes the resulting `/matches` records.

## 2. User stories

1. As an owner with at least one published pet, when I open the Deck tab I see a stack of 2–3 pet cards with the topmost one fully visible and the next 1–2 peeking behind with a slight depth offset.
2. As a user, I can **swipe right** to like the top card or **swipe left** to pass; the card animates off-screen with a small rotation; the next card slides up into focus.
3. As a user, I can tap the **❤ Like** or **✕ Pass** button below the deck to perform the same action without swiping.
4. As a user, I can tap **↺ Rewind** to restore the most recently swiped card to the top of the stack; my prior swipe is undone in the backend.
5. As a user, I can tap the left/right edges of a card to cycle through that pet's photos; small segmented indicators at the top of the card show progress.
6. As a user, I can tap the center of a card to open a bottom sheet with the full pet details — photo pager, intent chips, bio, distance bucket, owner chip, and a Like shortcut button.
7. As a user, every card visibly carries an **intent chip** (Playdate / Adoption / Friendship) and a bucketed distance label ("< 5 km", "5–15 km", "15–50 km", "50+ km"). The owner's first name + tiny avatar appear as a small chip on the card front.
8. As a user, if my local pool (within my chosen distance radius) is exhausted, the deck **auto-expands** to the next radius tier and shows a banner: "Showing pets up to N km away".
9. As a user, if even the maximum radius (200 km) yields no cards, I see an empty state with a button to adjust filters in Settings.
10. As an owner with **no published pets**, I can browse the deck but the Like button is disabled (and a right-swipe shows a snackbar) — tapping the disabled Like surfaces "Add a pet to start liking" with an "Add" action.
11. As a user, when one of my swipes completes a mutual like with another owner, a match is created in the backend; the deck shows a brief "It's a match!" celebration overlay; the match also appears in my Matches tab (handled by the Match feature).
12. As a user, I never see a pet I've already swiped on (until the rewind window is used) and I never see my own pets.

## 3. Domain model

In `:feature:deck:domain` — pure Kotlin/JVM.

```kotlin
data class DeckCard(
    val pet: Pet,                      // from :feature:pet:domain
    val owner: OwnerSummary,           // minimal — just what the card needs
    val distanceBucket: DistanceBucket,
)

data class OwnerSummary(
    val id: OwnerId,
    val firstName: String,
    val avatarUrl: String?,
)

enum class DistanceBucket(val label: String) {
    UNDER_5_KM("< 5 km"),
    BUCKET_5_15_KM("5–15 km"),
    BUCKET_15_50_KM("15–50 km"),
    OVER_50_KM("50+ km");

    companion object {
        fun fromKm(km: Double): DistanceBucket = when {
            km < 5 -> UNDER_5_KM
            km < 15 -> BUCKET_5_15_KM
            km < 50 -> BUCKET_15_50_KM
            else   -> OVER_50_KM
        }
    }
}

data class FilterPrefs(
    val maxDistanceKm: Int,                       // 25, 50, 100, 200
    val intents: Set<Intent>,                     // non-empty subset
    val speciesCategories: Set<SpeciesCategory>,  // non-empty subset
) {
    companion object {
        val DEFAULT = FilterPrefs(
            maxDistanceKm = 25,
            intents = Intent.values().toSet(),
            speciesCategories = SpeciesCategory.values().toSet(),
        )
    }
}

enum class SwipeAction { LIKE, PASS }

data class DeckSnapshot(
    val cards: List<DeckCard>,         // up to 10-ish; the UI only shows the top 2-3
    val effectiveRadiusKm: Int,        // banner text when > user's max
    val state: DeckState,
)

enum class DeckState {
    LOADING,
    READY,
    EXPANDED,        // showing pets beyond the user's chosen radius (auto-widened)
    EXHAUSTED,       // even max radius yielded nothing
    REQUIRES_PET,    // owner has no published pets; browse-only, Like disabled
}

sealed interface SwipeResult {
    data object Pending : SwipeResult                // recorded, no match
    data class  Match(val matchId: String) : SwipeResult
    data object RequiresPet : SwipeResult            // tried to like without a pet
}
```

> `FilterPrefs` lives in a new shared `:core:filters:domain` (JVM) module, not in `:feature:deck:domain`, because `:feature:settings` also reads/writes it. See §8. The deck imports it.

## 4. Repository contract

In `:feature:deck:domain`:

```kotlin
interface DeckRepository {
    /** Continuous stream — re-emits when filters change, new likes are committed, or new pets appear. */
    fun observeDeck(filters: Flow<FilterPrefs>): Flow<DeckSnapshot>

    /** Persist a swipe. Detects reciprocity for LIKE; returns Match if a mutual like is found. */
    suspend fun submitSwipe(petId: PetId, action: SwipeAction): SwipeResult

    /** Undo the most recent swipe. No-op if there isn't one or it's too old. Returns the un-swiped pet for re-insertion. */
    suspend fun undoLastSwipe(): Pet?
}
```

UseCases (in `:feature:deck:domain`):

- `ObserveDeckUseCase(filterPrefsRepo, deckRepo)` — combines filter flow with deck flow.
- `SubmitSwipeUseCase`
- `UndoLastSwipeUseCase`

## 5. Data layer (Firestore)

In `:feature:deck:data` (Android module). Depends on `:feature:deck:domain`, `:feature:pet:domain`, `:core:firebase`, `:core:session:domain`, `:core:filters:domain`.

### 5.1 Firestore schema

This feature reads the `pets` collection (created by the Pet feature) and writes three new top-level collections.

```
likes/{likeId} = {                          // likeId = "${fromOwnerId}_${toPetId}" (deterministic)
  fromOwnerId:  string,
  toOwnerId:    string,                     // denormalized so reciprocity is one query
  toPetId:      string,
  createdAt:    Timestamp,
}

passes/{passId} = {                         // passId = "${ownerId}_${toPetId}"
  ownerId:      string,
  toPetId:      string,
  createdAt:    Timestamp,
}

matches/{matchId} = {                       // matchId = "${minUid}_${maxUid}"
  ownerAId:     string,                     // = minUid (lexicographic)
  ownerBId:     string,                     // = maxUid
  participants: [ownerAId, ownerBId],       // array — enables `array-contains` queries
  createdAt:    Timestamp,
  initiatingLike: { fromOwnerId: string, toPetId: string },
  // Fields written later by chat (see match-and-chat.md):
  lastMessageAt:         Timestamp?,
  lastMessagePreview:    string?,
  lastMessageFromOwnerId: string?,
}
```

The deterministic `matchId` makes mutual-like writes idempotent — concurrent races resolve to the same doc, the second `set` overwrites with identical data.

**Composite indexes** (`firestore.indexes.json`):
- `likes`: `(toOwnerId asc, createdAt desc)` — drives the Likes-you tab and reciprocity reads.
- `likes`: `(fromOwnerId asc, createdAt desc)` — drives "my outgoing likes" reads for rewind.
- `passes`: `(ownerId asc, toPetId asc)` — drives the deck's "skip already-passed" filter.
- `matches`: `(participants array-contains, createdAt desc)` — drives "my matches" list.

### 5.2 Security rules

```
match /likes/{likeId} {
  allow read:   if request.auth != null;                                                // public read; needed for reciprocity
  allow create: if request.auth != null
                && request.resource.data.fromOwnerId == request.auth.uid;
  allow delete: if request.auth != null
                && resource.data.fromOwnerId == request.auth.uid;
}

match /passes/{passId} {
  allow read, create, delete: if request.auth != null
                && request.resource.data.ownerId == request.auth.uid;
  // (only the owner reads/writes their own passes)
}

match /matches/{matchId} {
  allow read:   if request.auth != null
                && request.auth.uid in resource.data.participants;
  allow create: if request.auth != null
                && request.auth.uid in request.resource.data.participants;
  allow update: if request.auth != null
                && request.auth.uid in resource.data.participants;
  allow delete: if request.auth != null
                && request.auth.uid in resource.data.participants;
}
```

`likes` is world-readable for signed-in users — required for client-side reciprocity (Owner A queries `likes where toOwnerId == B and fromOwnerId == ...` to detect mutuals). Server-side detection via Cloud Function is captured in `plans/notifications-fcm.md` and removes this concession.

### 5.3 Distance computation (v1 strategy)

Owner location lives at `owners/{uid}.location = GeoPoint(lat, lng)` (Firestore native type) — set during `:feature:profile` onboarding. The deck reads it.

For v1, given expected low user count (< 10k pets globally), the deck:

1. Queries `pets` with `whereEqualTo("state", "ACTIVE")` (and `whereIn("speciesCategory", filters.species)` when narrowed) — fetches up to N (say 500) active pets.
2. Filters client-side: drop my own pets, drop pets I've already swiped on (read `likes where fromOwnerId == me` + `passes where ownerId == me`), drop pets whose owner location is missing, drop pets that don't match `FilterPrefs.intents`.
3. Computes Haversine distance for each remaining pet.
4. Filters by `effectiveRadiusKm` (starts at `FilterPrefs.maxDistanceKm`).
5. Sorts by distance ascending; returns the top batch (~20).

When the result is empty and `effectiveRadiusKm < 200`, double the radius and try again — emit `DeckState.EXPANDED` with the new value. At 200 km with no results: `DeckState.EXHAUSTED`.

**v2 / scaling:** add a `geohash` field to each owner profile + a corresponding `pets.geohash` denormalized field; use range queries on geohash prefix to bucket the initial fetch.

### 5.4 Match detection on like submit

```kotlin
override suspend fun submitSwipe(petId: PetId, action: SwipeAction): SwipeResult = withContext(io) {
    val me = currentUid()
    val likeId = "${me}_${petId.value}"
    val passId = "${me}_${petId.value}"

    if (action == SwipeAction.PASS) {
        firestore.collection("passes").document(passId).set(mapOf(
            "ownerId"   to me,
            "toPetId"   to petId.value,
            "createdAt" to FieldValue.serverTimestamp(),
        )).await()
        return@withContext SwipeResult.Pending
    }

    // LIKE path — requires the user to have at least one published pet.
    if (!ownerHasActivePet(me)) return@withContext SwipeResult.RequiresPet

    val targetPet = firestore.collection("pets").document(petId.value).get().await().toPetOrNull()
        ?: error("Pet ${petId.value} not found")
    val targetOwner = targetPet.ownerId

    firestore.collection("likes").document(likeId).set(mapOf(
        "fromOwnerId" to me,
        "toOwnerId"   to targetOwner,
        "toPetId"     to petId.value,
        "createdAt"   to FieldValue.serverTimestamp(),
    )).await()

    // Reciprocity: has the target owner ever liked any of my pets?
    val theirLikes = firestore.collection("likes")
        .whereEqualTo("fromOwnerId", targetOwner)
        .whereEqualTo("toOwnerId", me)
        .limit(1)
        .get().await()
    if (theirLikes.isEmpty) return@withContext SwipeResult.Pending

    // Mutual like → write match in a transaction (idempotent via deterministic id).
    val matchId = listOf(me, targetOwner).sorted().joinToString("_")
    val (a, b) = matchId.split("_")
    firestore.collection("matches").document(matchId).set(mapOf(
        "ownerAId"     to a,
        "ownerBId"     to b,
        "participants" to listOf(a, b),
        "createdAt"    to FieldValue.serverTimestamp(),
        "initiatingLike" to mapOf("fromOwnerId" to me, "toPetId" to petId.value),
    ), SetOptions.merge()).await()                   // merge so we never overwrite chat-set fields

    SwipeResult.Match(matchId)
}
```

### 5.5 Rewind

`undoLastSwipe()` is in-memory — `:feature:deck:data` keeps a `LastSwipe(petId, action, timestamp)` reference. On rewind:

1. Delete the corresponding `likes/{me}_{petId}` or `passes/{me}_{petId}` doc.
2. If the swipe created a match, delete `matches/{matchId}` (clients will lose access via the rule, the other side's snapshot listener removes the row).
3. Return the un-swiped `Pet` so presentation can splice it back to the top of the stack.

Rewind expires after 60 seconds or one swipe (whichever comes first). Beyond that, the button is disabled.

### 5.6 Hilt

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class DeckDataModule {
    @Binds @Singleton
    abstract fun bindDeckRepository(impl: FirestoreDeckRepository): DeckRepository
}
```

## 6. Presentation layer

In `:feature:deck:presentation` (Android, Compose).

### 6.1 Screens

| Screen | Route key (`:nav`) | Purpose |
|---|---|---|
| Deck | `DeckHome` | The card stack + action buttons + filter banner. The Deck tab's only route. |

(The expanded pet view is a bottom sheet inside `DeckHome`, not a separate route.)

### 6.2 ViewModel

```kotlin
data class DeckUiState(
    val cards: List<DeckCard> = emptyList(),     // top of list = top of stack
    val effectiveRadiusKm: Int = 25,
    val state: DeckState = DeckState.LOADING,
    val canRewind: Boolean = false,
    val photoIndexByPetId: Map<PetId, Int> = emptyMap(),  // which photo each card is currently showing
    val expandedPetId: PetId? = null,             // non-null → bottom sheet open
    val matchCelebration: SwipeResult.Match? = null,
)

sealed interface DeckEvent {
    data object SwipeLikeRequested : DeckEvent
    data object SwipePassRequested : DeckEvent
    data object RewindRequested    : DeckEvent
    data class  PhotoCycle(val petId: PetId, val direction: CycleDir) : DeckEvent
    data class  CardTapped(val petId: PetId) : DeckEvent
    data object SheetDismissed     : DeckEvent
    data object MatchCelebrationDismissed : DeckEvent
    data object AddPetRequested    : DeckEvent              // from disabled-Like snackbar
}

enum class CycleDir { PREV, NEXT }
```

The ViewModel:
- Collects `ObserveDeckUseCase(filterPrefsFlow)` into `cards`.
- On `SwipeLikeRequested`/`SwipePassRequested`: pop the top card, call `SubmitSwipeUseCase`. On `SwipeResult.Match`, set `matchCelebration`. On `RequiresPet`, emit a snackbar event.
- On `RewindRequested`: call `UndoLastSwipeUseCase`, splice the returned pet to the top.
- Tracks per-card photo index in `photoIndexByPetId`.

### 6.3 Composables

```
DeckScreen
├─ DeckTopBar              // app name + small filter-summary chip ("25km · 3 intents")
├─ Box (the card stack)
│   ├─ DeckCard(index = 2, ...) // back card, scale 0.9, offset +16dp, no interaction
│   ├─ DeckCard(index = 1, ...) // middle, scale 0.95, offset +8dp, no interaction
│   └─ DeckCard(index = 0, ...) // top, full scale, draggable, tappable
├─ ActionBar               // [✕ Pass]  [↺ Rewind]  [❤ Like]
└─ Banner (conditional)    // "Showing pets up to 50 km away" when state == EXPANDED
└─ EmptyState (conditional) // when state == EXHAUSTED
└─ ModalBottomSheet (conditional) // when expandedPetId != null
└─ MatchCelebrationOverlay (conditional) // when matchCelebration != null
```

#### DeckCard composable

```
Card (rounded 24.dp, elevation 4.dp, fill 90% of screen height)
├─ Photo (AsyncImage from PetPhoto.source — Local or Remote handled uniformly)
├─ PhotoSegmentedIndicator (top, one segment per photo, current highlighted)
├─ IntentChip (top-right corner)
├─ DistanceBadge (top-left corner)
└─ BottomGradient + InfoOverlay
    ├─ Pet name, age (with "~" if approximate), species
    └─ OwnerChip (avatar + first name, small)
```

Modifiers on the **top card only**:
- `pointerInput { detectDragGestures(...) }` for swipe (translation + rotation = `dragX / cardWidth * 15deg`).
- `pointerInput { detectTapGestures(onTap = { offset -> ... }) }` — region check:
  - x < 1/3 width → `PhotoCycle(PREV)`
  - x > 2/3 width → `PhotoCycle(NEXT)`
  - else → `CardTapped` (opens sheet)
- Release threshold: if `|dragX| > 40% of cardWidth`, animate off and fire swipe; else spring back.

#### Bottom sheet (expanded pet view)

`ModalBottomSheet`, 90% screen height:

```
HorizontalPager(photos)
├─ AsyncImage per page
└─ Page indicator dots
Column (scrollable)
├─ Pet name, age, species (large)
├─ IntentChip row (all chips, since multi-select per pet)
├─ DistanceBadge
├─ Bio (if present)
├─ OwnerChip (avatar + first name, "Member since…" if useful)
└─ Like button (full-width, primary) — same effect as right-swiping
```

Dismiss: down-drag or back gesture. Tapping Like inside the sheet performs the swipe and closes the sheet.

#### Match celebration overlay

Fullscreen translucent scrim with both pets' primary photos in a "matched!" composition, "Say hello" button → navigates to chat (route exists when `:feature:chat` lands; until then, button just dismisses). Auto-dismiss after 5s.

#### Petless-owner Like gating

When `state == DeckState.REQUIRES_PET`:
- ❤ Like button is rendered disabled (50% alpha) with a tooltip-like helper.
- Right-swipe is still gesturally accepted but doesn't commit — snackbar appears: "Add a pet to start liking" with action "Add" → navigates to `AddPet` route from `:feature:pet:nav`.
- Pass button and pass-swipe still work fully (pets the user passes on while petless are recorded in `/passes` so they don't reappear when they do add a pet).

### 6.4 EntryProviderInstaller

```kotlin
@Module
@InstallIn(ActivityRetainedComponent::class)
object DeckNavModule {
    @IntoSet @Provides
    fun provideDeckEntries(navigator: Navigator): EntryProviderInstaller = {
        entry<DeckHome> { DeckScreen(
            onAddPet = { navigator.goTo(AddPet) },         // from :feature:pet:nav
            onOpenMatchChat = { matchId -> navigator.goTo(ChatRoute(matchId)) }, // from :feature:chat:nav (when it exists)
        ) }
    }
}
```

## 7. Background / lifecycle considerations

- **No background workers** in this feature. The deck is foreground-only; nothing to schedule.
- **Realtime DB listeners** for the visible cards are torn down when the Deck tab is not active (lifecycle-aware via `repeatOnLifecycle(STARTED)`).
- **Prefetch policy**: when the visible stack shrinks to ≤ 3 cards, the data layer prefetches the next batch in the background.

## 8. New shared module: `:core:filters`

`FilterPrefs` is read by `:feature:deck` and written by `:feature:settings`. To avoid either feature depending on the other beyond `:nav`, introduce a split core module:

```
:core:filters
  :domain      // JVM. FilterPrefs data class, FilterPrefsRepository interface, validation.
  :data        // Android. FirebaseFilterPrefsRepository — reads/writes /owners/{uid}/filters in RTDB.
```

RTDB schema:

```
/owners/{uid}/filters = {
  maxDistanceKm: <int>,
  intents: { PLAYDATE: true, ... },
  speciesCategories: { DOGS: true, ... },
  updatedAt: <epoch ms>,
}
```

Security: owner can read+write their own; nobody else can read.

Default values applied when the node is missing (first-time deck open): `FilterPrefs.DEFAULT`.

## 9. Module structure & dependencies

```
:feature:deck
  :nav            // Pure JVM. Route key DeckHome (no parameters).
  :domain         // Pure JVM. DeckCard, FilterPrefs (re-export from :core:filters), DistanceBucket, DeckRepository, UseCases.
  :data           // Android. FirebaseDeckRepository, distance math (Haversine), rewind state.
  :presentation   // Android/Compose. DeckScreen, card composables, bottom sheet, match celebration, EntryProviderInstaller.
```

Dependency edges:

```
:feature:deck:domain
  ├─► :feature:pet:domain        (Pet, Intent, Species, PetPhoto)
  ├─► :core:filters:domain       (FilterPrefs)
  ├─► :core:session:domain       (OwnerId)
  └─► :core:common

:feature:deck:data
  ├─► :feature:deck:domain
  ├─► :feature:pet:domain
  ├─► :core:filters:domain
  ├─► :core:firebase
  ├─► :core:session:domain
  └─► :core:common

:feature:deck:presentation
  ├─► :feature:deck:domain
  ├─► :feature:deck:nav
  ├─► :feature:pet:nav           (AddPet route for petless-owner gate)
  ├─► :feature:chat:nav          (when it exists — for match celebration "Say hello" button)
  ├─► :core:designsystem
  ├─► :core:ui
  ├─► :core:navigation
  └─► :core:session:domain
```

Convention plugins:
- `:nav`, `:domain` apply only `tinpet.jvm.library`.
- `:data` applies `tinpet.android.library` + `tinpet.hilt`.
- `:presentation` applies `tinpet.android.feature`.

Wire `:feature:deck:presentation` into `:app` so its routes register with `NavDisplay`.

## 10. Open questions / future work

1. **Geohash-bucketed queries.** v1 fetches all `ACTIVE` pets and filters client-side. This breaks past ~1000s of active pets globally. Migrate to a manually-implemented geohash bucketing scheme (write `/pets/{petId}.geohash` on create, query by prefix) before scaling. Out of scope here.
2. **Match detection race.** Client-side reciprocity check + non-transactional write can produce a duplicate match record under simultaneous mutual likes. Deterministic match id mitigates duplication (idempotent overwrite) but the "initiatingLike" field can flip-flop briefly. Cloud-Function-based detection is the proper fix — see `plans/notifications-fcm.md`.
3. **Owner location source.** This plan assumes `/owners/{uid}/profile.location` exists. The `:feature:profile` plan must capture location (foreground permission, last known location via `FusedLocationProviderClient`) during onboarding. Add a guard in the deck data layer: if the current owner has no location, surface a "Set your location to start matching" empty state instead of an exhausted deck.
4. **Pet visibility while owner is paused.** When an owner toggles "Pause" in Settings (`:feature:settings`), all their pets must be hidden from other owners' decks. Implementation: add an `/owners/{uid}/paused` boolean and skip pets whose owner is paused during the client-side filter. Cross-reference `:feature:settings` when implemented.
5. **Block/report effects on deck.** When an owner blocks another, the deck must permanently exclude the blocked owner's pets. Implementation: read `/blocks/{me}/*` during filter and exclude. Cross-reference `:feature:settings`.
6. **Reads of `/likes/{otherOwner}/*` for reciprocity.** Current plan loosens the `/likes` read rule to any signed-in user. Document the trade-off in the rules file; revisit when match detection moves server-side.

## 11. Out of scope

- Super-like, boost, daily picks, passport — paid features dropped per parent spec.
- "Likes you" tab (separate feature `:feature:likes`).
- Match list & chat (separate features `:feature:match`, `:feature:chat`).
- Onboarding tour / first-time deck coach-marks.
- Animations beyond the standard swipe + spring (no card-flip, no Lottie celebrations beyond the simple match overlay).
- Multi-step undo (only the most recent swipe is rewindable).
- Server-side match detection (Cloud Function — covered in notifications plan).

## 12. Verification

1. Build:
   ```bash
   JAVA_HOME=/Users/jrodiz/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home ./gradlew assembleDebug
   ```
2. JVM isolation:
   ```bash
   ./gradlew :feature:deck:nav:dependencies --configuration runtimeClasspath
   ./gradlew :feature:deck:domain:dependencies --configuration runtimeClasspath
   ./gradlew :core:filters:domain:dependencies --configuration runtimeClasspath
   ```
   None should pull in `androidx.*`, Compose, Room, Retrofit, Hilt, or Firebase SDKs.
3. Unit tests (in `:feature:deck:domain` and `:feature:deck:data` with Firebase emulator suite):
   - `DistanceBucket.fromKm` boundary cases.
   - Filter pipeline: my own pets excluded, already-swiped pets excluded, intent/species filters applied.
   - Radius auto-expansion: 25 → 50 → 100 → 200; `EXPANDED` and `EXHAUSTED` states.
   - `submitSwipe` reciprocity: when target owner has not liked me, returns `Pending`; when they have, writes a match and returns `Match`.
   - Match-id determinism: A→B and B→A produce the same `matchId`.
   - `undoLastSwipe` removes the like/pass and (if applicable) the match.
4. UI smoke (manual on emulator, with the Firebase emulator suite running):
   - Add at least 2 fake owners + their pets via Firebase console; verify they appear in the deck for the test user.
   - Swipe left and right; verify `/likes` and `/passes` populate under the right owner.
   - Configure two test users to mutually like each other's pets; verify `/matches` is created and the celebration overlay shows.
   - Disable the user's pets (set state = ARCHIVED for all their pets); verify the Like button gates and the snackbar appears with "Add" action.
   - Run the deck dry within the default radius; verify auto-expansion banner appears at 50 km.
   - Cycle photos via left/right edge taps; verify the segmented indicator advances and the photo changes.
   - Tap card center; verify the bottom sheet opens with the photo pager + full details.

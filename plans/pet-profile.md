# Plan: Pet Profile feature (`:feature:pet`)

> **Parent spec:** [`plans/tinpet-app.md`](./tinpet-app.md). **Architecture rules:** [`plans/ANDROID_APP_SCAFFOLD_PROMPT.md`](./ANDROID_APP_SCAFFOLD_PROMPT.md) — every module dependency invariant there applies here.
>
> **Prerequisite reads:** `CLAUDE.md` at the repo root, the parent spec above, and the existing `plans/home-feed.md` (for the fake-data + repository pattern this plan reuses).

## 1. Goal

Implement the `:feature:pet` feature module — the system by which an owner creates, edits, previews, and removes pet profiles. This is the foundational feature: every downstream feature (Deck, Likes-you, Match, Chat) consumes pets, so it must land first.

Data lives in **Cloud Firestore**; pet photos are uploaded to **Firebase Storage**. Both are wired in from day one — no in-memory or fake-data phase. Firebase Auth (already configured for login) supplies the user identity for security rules. Firestore's local cache is enabled by default on Android so the My Pets list survives a brief network drop.

Pets are scoped to the current owner. The current owner is read from `:core:session`.

## 2. User stories

1. As an owner with no pets, I see an empty **My Pets** state with a primary "Add a pet" button.
2. As an owner, I can add a new pet by providing photos, a name, age, species, intent(s), and an optional bio. The pet appears in My Pets and (if all required fields are present) in others' decks.
3. As an owner, when I tap one of my pets, I see a **preview** of how that pet appears to swipers — same card layout, full photo gallery, intent chips, bio. An "Edit" button opens the form.
4. As an owner, I can edit any field on a published pet. Saving immediately reflects in the deck.
5. As an owner, I can delete a pet. The delete is **soft for 7 days** — the pet vanishes from the deck and My Pets main list, but I can restore it from an "Archived" section within that window. After 7 days it's auto-purged.
6. As an owner who deleted a pet, I can restore it during the 7-day grace window; the pet returns to active state with all data intact.
7. As an owner, if I try to publish a pet missing any required field, the form blocks submission and highlights what's missing.
8. As an owner with multiple pets, My Pets shows them as a list/grid with a primary photo thumbnail, name, and intent chips. Tapping a row opens the preview.

## 3. Domain model

In `:feature:pet:domain` — pure Kotlin/JVM, no Android.

```kotlin
@JvmInline value class PetId(val value: String)

data class Pet(
    val id: PetId,
    val ownerId: OwnerId,                    // from :core:session:domain
    val name: String,                        // 1..30 chars, trimmed
    val ageYears: Int,                       // 0..25
    val ageIsApproximate: Boolean,           // true for rescues with unknown DOB
    val species: Species,
    val intents: Set<Intent>,                // non-empty when ACTIVE
    val photos: List<PetPhoto>,              // 1..6 when ACTIVE; index 0 = primary
    val bio: String?,                        // null or 1..300 chars (trimmed)
    val state: PetState,
    val createdAt: Instant,                  // kotlinx.datetime.Instant
    val updatedAt: Instant,
    val deletedAt: Instant?,                 // set when state moves to ARCHIVED
)

data class PetPhoto(
    val id: PhotoId,
    val source: PhotoSource,                 // see Data Layer §5 for v1 representation
)

@JvmInline value class PhotoId(val value: String)

enum class Intent { PLAYDATE, ADOPTION, FRIENDSHIP }

enum class Species(val category: SpeciesCategory) {
    DOG(SpeciesCategory.DOGS),
    CAT(SpeciesCategory.CATS),
    RABBIT(SpeciesCategory.SMALL_MAMMALS),
    HAMSTER(SpeciesCategory.SMALL_MAMMALS),
    GUINEA_PIG(SpeciesCategory.SMALL_MAMMALS),
    FERRET(SpeciesCategory.SMALL_MAMMALS),
    OTHER_SMALL_MAMMAL(SpeciesCategory.SMALL_MAMMALS),
}

enum class SpeciesCategory { DOGS, CATS, SMALL_MAMMALS }

enum class PetState {
    ACTIVE,          // visible in deck, visible in My Pets
    ARCHIVED,        // soft-deleted; hidden from deck and My Pets main list, restorable for 7 days
    PURGED,          // hard-deleted; record retained as a tombstone for chat references
}
```

`PhotoSource` is defined in §5 (Data Layer) because its concrete representation is implementation-specific.

### 3.1 Drafts

Adding/editing a pet uses a separate draft type so validation can run before producing a `Pet`:

```kotlin
data class PetDraft(
    val name: String,
    val ageYears: Int,
    val ageIsApproximate: Boolean,
    val species: Species,
    val intents: Set<Intent>,
    val photos: List<PetPhoto>,
    val bio: String?,
)

sealed interface PetValidationError {
    data object NameMissing : PetValidationError
    data object NameTooLong : PetValidationError
    data object AgeOutOfRange : PetValidationError
    data object NoPhotos : PetValidationError
    data object TooManyPhotos : PetValidationError
    data object NoIntent : PetValidationError
    data object BioTooLong : PetValidationError
}
```

Validation:
- `name`: trimmed, must be 1..30 chars
- `ageYears`: must be in 0..25
- `intents`: must be non-empty
- `photos`: must be 1..6
- `bio`: nullable; if non-null, trimmed length 1..300

A `PetDraft.validate(): Result<PetDraft, List<PetValidationError>>` extension (or equivalent `Try`) returns all errors together so the UI can show every problem at once, not field-by-field.

## 4. Repository contract

In `:feature:pet:domain`:

```kotlin
interface PetRepository {
    /** All pets for the current owner, ACTIVE + ARCHIVED. UI filters by state for the My Pets list / Archived section. */
    fun observeMyPets(): Flow<List<Pet>>

    /** Single pet by id, observed (null after PURGED). */
    fun observePet(id: PetId): Flow<Pet?>

    /** Creates a new ACTIVE pet from a valid draft. Throws on validation failure — caller must validate first. */
    suspend fun addPet(draft: PetDraft): Pet

    /** Updates a pet's editable fields. Throws on validation failure. */
    suspend fun updatePet(id: PetId, draft: PetDraft): Pet

    /** Soft-deletes a pet: state → ARCHIVED, deletedAt = now. */
    suspend fun archivePet(id: PetId)

    /** Restores an ARCHIVED pet within its 7-day grace window. No-op if already PURGED. */
    suspend fun restorePet(id: PetId)
}
```

UseCases in `:feature:pet:domain` (one per business operation, each with `suspend operator fun invoke`):

- `ObserveMyPetsUseCase` (with a `state` filter parameter — defaults to `ACTIVE`)
- `ObservePetUseCase`
- `AddPetUseCase` (validates the draft, returns `Result<Pet, List<PetValidationError>>`)
- `UpdatePetUseCase` (same)
- `ArchivePetUseCase`
- `RestorePetUseCase`

Background purge is **not** a UseCase — it's a scheduled job (§7).

## 5. Data layer (Cloud Firestore + Firebase Storage)

In `:feature:pet:data` (Android module). Depends on `:core:firebase` for the `FirebaseFirestore` and `FirebaseStorage` singletons.

### 5.1 Firestore schema

Single top-level collection. Rich queries replace the need for any denormalized index.

```
pets/{petId} = {
  ownerId:          string,                          // uid of the owner
  name:             string,
  ageYears:         number (0..25),
  ageIsApproximate: boolean,
  species:          string (DOG | CAT | RABBIT | HAMSTER | GUINEA_PIG | FERRET | OTHER_SMALL_MAMMAL),
  speciesCategory:  string (DOGS | CATS | SMALL_MAMMALS),       // denormalized for cheap deck filter
  intents:          string[] (subset of [PLAYDATE, ADOPTION, FRIENDSHIP]),
  photos:           [{ id: string, storagePath: string, downloadUrl: string }],
  bio:              string?,
  state:            string (ACTIVE | ARCHIVED | PURGED),
  createdAt:        Timestamp,
  updatedAt:        Timestamp,
  deletedAt:        Timestamp?,
}
```

**Indexes** (composite, declared in `firestore.indexes.json`):
- `(ownerId asc, state asc, updatedAt desc)` — drives the "my pets" query and the archived-section sort.
- `(state asc, speciesCategory asc, updatedAt desc)` — used by the deck (see deck-swipe plan).

### 5.2 Security rules (`firestore.rules`)

```
rules_version = '2';
service cloud.firestore {
  match /databases/{db}/documents {
    match /pets/{petId} {
      allow read:   if request.auth != null;
      allow create: if request.auth != null
                    && request.resource.data.ownerId == request.auth.uid;
      allow update: if request.auth != null
                    && resource.data.ownerId == request.auth.uid
                    && request.resource.data.ownerId == request.auth.uid;  // can't reassign owner
      allow delete: if false;  // soft-delete only — state goes to ARCHIVED then PURGED via update
    }
  }
}
```

Any signed-in user can read pets (the deck needs to see other owners' pets). Only the pet's owner can create or update; nobody can hard-delete from a client.

### 5.3 Firebase Storage layout & rules

```
gs://<bucket>/pets/{petId}/photos/{photoId}.jpg
```

`storage.rules`:

```
service firebase.storage {
  match /b/{bucket}/o {
    match /pets/{petId}/photos/{photoName} {
      allow read:  if request.auth != null;
      allow write: if request.auth != null
        && firestore.get(/databases/(default)/documents/pets/$(petId)).data.ownerId == request.auth.uid
        && request.resource.size < 5 * 1024 * 1024
        && request.resource.contentType.matches('image/.*');
    }
  }
}
```

5 MB cap + image content-type guard are baseline anti-abuse.

### 5.4 Photo representation (in `:domain`)

```kotlin
sealed interface PhotoSource {
    /** Local image selected from gallery/camera, not yet uploaded. Used inside the Add/Edit form before submit. */
    data class Local(val uri: String) : PhotoSource

    /** Uploaded to Firebase Storage; readable by any authenticated user. */
    data class Remote(val storagePath: String, val downloadUrl: String) : PhotoSource
}
```

Both variants are pure strings → keeps `:domain` JVM-only. Coil 3 loads each via a single `AsyncImage` — `Local` resolves to a `content://` URI, `Remote` to the Storage download URL.

### 5.5 Repository implementation

```kotlin
@Singleton
class FirestorePetRepository @Inject constructor(
    private val sessionRepo: SessionRepository,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val clock: Clock,
    @IoDispatcher private val io: CoroutineDispatcher,
) : PetRepository {

    private val petsCol get() = firestore.collection("pets")

    override fun observeMyPets(): Flow<List<Pet>> = callbackFlow {
        val uid = sessionRepo.current()?.userId
        if (uid == null) { trySend(emptyList()); close(); return@callbackFlow }
        val reg = petsCol
            .whereEqualTo("ownerId", uid)
            .whereIn("state", listOf("ACTIVE", "ARCHIVED"))
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) { close(err); return@addSnapshotListener }
                trySend(snap?.documents.orEmpty().mapNotNull { it.toPetOrNull() })
            }
        awaitClose { reg.remove() }
    }.flowOn(io)

    override fun observePet(id: PetId): Flow<Pet?> = callbackFlow {
        val reg = petsCol.document(id.value).addSnapshotListener { snap, err ->
            if (err != null) { close(err); return@addSnapshotListener }
            trySend(snap?.toPetOrNull())
        }
        awaitClose { reg.remove() }
    }.flowOn(io)

    override suspend fun addPet(draft: PetDraft): Pet = withContext(io) {
        val uid = currentUid()
        val docRef = petsCol.document()
        val petId = docRef.id
        val uploaded = try {
            draft.photos.map { uploadPhotoOrPassThrough(petId, it) }
        } catch (t: Throwable) { cleanupStorageBestEffort(petId); throw t }
        val now = clock.now()
        val finalDraft = draft.copy(photos = uploaded)
        docRef.set(buildPetMap(uid, finalDraft, PetState.ACTIVE, now, now, null)).await()
        petFromWrite(PetId(petId), uid, finalDraft, PetState.ACTIVE, now, now, null)
    }

    override suspend fun updatePet(id: PetId, draft: PetDraft): Pet = withContext(io) {
        // Read once, diff photos, upload new, delete removed, then docRef.set(...).
    }

    override suspend fun archivePet(id: PetId) {
        val now = clock.now()
        petsCol.document(id.value).update(mapOf(
            "state" to PetState.ARCHIVED.name,
            "deletedAt" to Timestamp(now.toJavaInstant()),
            "updatedAt" to Timestamp(now.toJavaInstant()),
        )).await()
    }

    override suspend fun restorePet(id: PetId) {
        val now = clock.now()
        petsCol.document(id.value).update(mapOf(
            "state" to PetState.ACTIVE.name,
            "deletedAt" to FieldValue.delete(),
            "updatedAt" to Timestamp(now.toJavaInstant()),
        )).await()
    }
}
```

**Partial-failure semantics for Add:** if any photo upload throws, the submit fails atomically — successfully-uploaded photos for this `petId` are deleted best-effort, no pet doc is written, and the user is shown a retry-able error.

**Hilt binding:**

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class PetDataModule {
    @Binds @Singleton
    abstract fun bindPetRepository(impl: FirestorePetRepository): PetRepository
}
```

Firestore's local cache is on by default — no `setPersistenceEnabled` call needed at app startup.

## 6. Presentation layer

In `:feature:pet:presentation` (Android, depends on `:feature:pet:domain`, `:feature:pet:nav`, `:core:designsystem`, `:core:ui`, `:core:navigation`).

### 6.1 Screens

| Screen | Route key (`:nav`) | Purpose |
|---|---|---|
| My Pets list | `MyPets` | Grid/list of owner's ACTIVE pets + an "Archived" section toggle. Primary CTA: "Add a pet". |
| Add Pet | `AddPet` | Form for new pet (all fields, validation, submit). |
| Pet Preview | `PetDetail(petId)` | Card-style preview matching the deck representation. "Edit" button → `EditPet`. "Delete" (overflow) → confirmation → archive. |
| Edit Pet | `EditPet(petId)` | Same form as Add, prefilled. Submit updates the pet. |
| Archived section | nested under `MyPets` | Lists ARCHIVED pets with "Restore" action; shows time remaining ("Auto-deletes in 4 days"). |

### 6.2 ViewModels

One per screen. Each exposes `StateFlow<UiState>` and accepts `UiEvent`s. Error/event channels are `SharedFlow` (replay 0).

`AddPetViewModel` example shape:

```kotlin
data class AddPetUiState(
    val draft: PetDraft = PetDraft.empty(),
    val errors: List<PetValidationError> = emptyList(),
    val isSubmitting: Boolean = false,
)

sealed interface AddPetEvent {
    data class NameChanged(val v: String) : AddPetEvent
    data class AgeChanged(val years: Int, val approximate: Boolean) : AddPetEvent
    data class SpeciesChanged(val v: Species) : AddPetEvent
    data class IntentToggled(val intent: Intent) : AddPetEvent
    data class PhotoAdded(val source: PhotoSource) : AddPetEvent
    data class PhotoRemoved(val id: PhotoId) : AddPetEvent
    data class PhotosReordered(val newOrder: List<PhotoId>) : AddPetEvent
    data class BioChanged(val v: String) : AddPetEvent
    data object Submit : AddPetEvent
}
```

Submit calls `AddPetUseCase` → on success navigates back to `MyPets`; on validation error updates `errors` in state.

### 6.3 Add/Edit form layout (single scrollable screen)

1. **Photos** (top) — horizontal pager / grid (up to 6 slots). Tap empty slot → camera/gallery picker. Long-press to reorder. First slot is the primary photo. Show "Add up to 6 photos" hint.
2. **Name** — text field, 30-char counter.
3. **Species** — segmented picker (Dogs / Cats / Small mammals). Selecting "Small mammals" reveals a sub-picker (Rabbit / Hamster / Guinea pig / Ferret / Other).
4. **Age** — number picker (0–25) + checkbox "Age is approximate".
5. **Intent** — three filter chips (Playdate / Adoption / Friendship), multi-select, at least one required.
6. **Bio** — multi-line text field, 300-char counter, helper text "Optional".
7. **Submit** — pinned to bottom. Disabled until all required fields are filled; tap shows aggregated validation errors if any remain.

### 6.4 Pet Preview screen

Renders the same card composable used by the Deck (defined in `:core:designsystem`) so previewing is WYSIWYG:

- Photo pager (swipe through up to 6)
- Pet name, age (`"3 years"` or `"~3 years"` when approximate), species
- Intent chips
- Bio
- Owner chip ("with <first name>", small avatar) — read from `:core:session`

Top bar: Back / Edit / overflow ▾ (Delete).

### 6.5 My Pets screen

- Header: "My Pets" + "+" icon → `AddPet`.
- Grid (2 columns) of `PetThumbnailCard` (primary photo, name, intent chips).
- Footer: collapsible "Archived (N)" expander → grid of archived pets with "Restore" action and remaining-time text.
- Empty state (no active pets): illustration + "Add your first pet" primary button.

### 6.6 EntryProviderInstaller

```kotlin
@Module
@InstallIn(ActivityRetainedComponent::class)
object PetNavModule {
    @IntoSet @Provides
    fun providePetEntries(navigator: Navigator): EntryProviderInstaller = {
        entry<MyPets> { MyPetsScreen(onAddPet = { navigator.goTo(AddPet) }, ...) }
        entry<AddPet> { AddPetScreen(onSaved = { navigator.back() }) }
        entry<PetDetail> { key -> PetPreviewScreen(key.petId, ...) }
        entry<EditPet>   { key -> EditPetScreen(key.petId, onSaved = { navigator.back() }) }
    }
}
```

## 7. Background: 7-day auto-purge

A WorkManager periodic worker (`PetPurgeWorker`, daily) queries Firestore for the current user's ARCHIVED pets where `deletedAt + 7d <= now` (`whereEqualTo("ownerId", uid).whereEqualTo("state", "ARCHIVED").whereLessThanOrEqualTo("deletedAt", cutoff)`). For each, it deletes the pet's images from Firebase Storage (best effort), then writes `state = PURGED` and clears the `photos` array. The `pets/{petId}` document stays as a tombstone so chats can render "this pet is no longer on TinPet" (handled in `:feature:chat`).

**Caveat:** WorkManager only runs while the device is online and the app is registered, so a malicious owner could leave the app uninstalled and never purge. The proper long-term home for this is a scheduled **Firebase Cloud Function** that runs server-side — captured in `plans/notifications-fcm.md`. For v1, the client-side worker is acceptable because: (a) the photos count against the owner's RTDB/Storage quota only, and (b) the owner has to open the app within the grace window for any restore to happen anyway.

The worker is registered in `:feature:pet:data` and scheduled from `:app` on cold start.

## 8. Module structure

Follows the 4-module convention:

```
:feature:pet
  :feature:pet:nav            // route keys: MyPets, AddPet, PetDetail(petId), EditPet(petId). Pure JVM.
  :feature:pet:domain         // Pet, PetDraft, Intent, Species, PetRepository, UseCases. Pure JVM.
  :feature:pet:data           // InMemoryPetRepository, PhotoSource impls, PetPurgeWorker, Hilt bindings. Android.
  :feature:pet:presentation   // ViewModels, screens, EntryProviderInstaller. Android (Compose).
```

Build files:
- `:nav`, `:domain` — apply only `tinpet.jvm.library` (per `plans/rebrand-tinpet.md` — plugin IDs are `tinpet.*`).
- `:data` — apply `tinpet.android.library`, `tinpet.hilt`. Depends on `:domain`, `:core:common`, `:core:session:domain`.
- `:presentation` — apply `tinpet.android.feature`. Depends on `:domain`, `:nav`, `:core:designsystem`, `:core:ui`, `:core:navigation`, `:core:session:domain`.

Wire `:feature:pet:presentation` into `:app` via Hilt multibinding so its routes register with the `NavDisplay`.

## 9. Required vs optional fields summary

| Field | Required to publish? | Validation |
|---|---|---|
| Photos | Yes (1–6) | At least 1; max 6 |
| Name | Yes | 1–30 chars, trimmed |
| Species | Yes | One of `Species` |
| Age | Yes | 0–25 |
| Age is approximate | No (default false) | — |
| Intent(s) | Yes (≥1) | Non-empty subset of `Intent` |
| Bio | No | If present, 1–300 chars |

## 10. Out of scope

- Breed field (deferred per parent plan).
- Photo verification badge.
- Crop / filter / edit tools beyond what the system picker provides.
- Drag-to-reorder *between sessions* via cloud — local reorder only in v1.
- Pet onboarding wizard (multi-step) — single-screen form is the v1 UX.
- Bulk import of pets.
- Co-owners (multiple owners sharing one pet).

## 11. Verification

1. Build:
   ```bash
   JAVA_HOME=/Users/jrodiz/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home ./gradlew assembleDebug
   ```
2. JVM isolation checks for the pure-Kotlin modules:
   ```bash
   ./gradlew :feature:pet:nav:dependencies --configuration runtimeClasspath
   ./gradlew :feature:pet:domain:dependencies --configuration runtimeClasspath
   ```
   Neither should pull in `androidx.*`, Compose, Room, Retrofit, or Hilt.
3. Unit tests (in `:feature:pet:domain`):
   - `PetDraft.validate()` returns every applicable error simultaneously.
   - `AddPetUseCase` rejects invalid drafts and accepts valid ones.
   - `ArchivePetUseCase` followed by `RestorePetUseCase` restores state.
4. UI smoke (manual on emulator):
   - Add a pet end-to-end; see it appear in My Pets.
   - Tap a pet → see Preview with photo gallery, intent chips, bio, owner chip.
   - Edit and save; preview reflects the change.
   - Delete a pet; see it move to Archived with countdown; restore it; see it return to active.

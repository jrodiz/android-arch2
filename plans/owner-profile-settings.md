# Plan: Owner Profile + Settings (`:feature:profile`, `:feature:settings`)

> **Parent spec:** [`plans/tinpet-app.md`](./tinpet-app.md). **Architecture rules:** [`plans/ANDROID_APP_SCAFFOLD_PROMPT.md`](./ANDROID_APP_SCAFFOLD_PROMPT.md).
>
> **Prereq features:** [`plans/pet-profile.md`](./pet-profile.md), [`plans/deck-swipe.md`](./deck-swipe.md), [`plans/bottom-nav-reshape.md`](./bottom-nav-reshape.md), [`plans/match-and-chat.md`](./match-and-chat.md). This plan expands the existing skeletal `:feature:profile` (which currently only signs out) and introduces a new `:feature:settings` module.
>
> **Backend:** adds four new Firestore collections (`owners`, `blocks`, `reports`, `accountDeletions`). Notification prefs and filters are fields on the `owners` doc (or a tiny single-doc subcollection) — see §4.1. No Firebase Storage additions beyond what the existing avatar upload (from sign-up) already uses.

## 1. Goal

Build out the **Profile tab** as the owner's home base — a small, focused screen with the owner's avatar + name, their pet roster, and a single Settings entry — and stand up a full **Settings** hub for everything else: edit profile, notification preferences, filter preferences, privacy (pause + blocked owners + report management), and account (delete account with 30-day grace).

This plan also adds **Block** and **Report** actions to the chat overflow menu (a small extension to [`plans/match-and-chat.md`](./match-and-chat.md) §6.2) so users have a path to block/report without leaving the conversation surface.

## 2. User stories

### Profile tab

1. As an owner, when I tap the Profile tab I see my avatar + first name at the top (tap → opens Settings → Edit profile).
2. As an owner, below the header I see my **My Pets** grid (same `PetThumbnailCard` from `:feature:pet`) with an "Add a pet" tile at the end.
3. As an owner, below My Pets I see two menu rows: **Settings** (chevron → Settings home) and **Sign out** (signs me out and returns to Login).

### Settings (categorized hub)

4. As an owner, when I tap Settings I see a vertical list of categories: **Profile**, **Notifications**, **Filters**, **Privacy**, **Account**.
5. As an owner, **Profile** (sub-screen) lets me edit my first name, change my avatar, and set/update my location. Saving validates and writes to RTDB; location update requests foreground location permission and uses `FusedLocationProviderClient`.
6. As an owner, **Notifications** (sub-screen) shows four toggles: New match / New message / Someone liked your pet / Weekly digest. Each writes to `/owners/{uid}/notificationPrefs` instantly on toggle (no Save button).
7. As an owner, **Filters** (sub-screen) lets me adjust max distance (slider 5–200 km), intent multi-select chips, and species multi-select chips. Changes persist via `:core:filters:data` and the deck reflects them on next observation.
8. As an owner, **Privacy** (sub-screen) lets me toggle "Pause profile" (hides my pets from others' decks) and tap into "Blocked users".
9. As an owner, **Privacy → Blocked users** shows each blocked owner (avatar + first name + Unblock button). Tapping Unblock removes the entry from `/blocks` and the owner can appear in my deck again.
10. As an owner, **Account** (sub-screen) shows my email (read-only, from Firebase Auth), and a destructive "Delete account" action at the bottom.
11. As an owner, tapping Delete account opens a modal explaining the **30-day grace period**. I must type the word `DELETE` (literal) into a text field for the destructive button to enable. Confirming writes `/accountDeletions/{uid}`, pauses my profile, and signs me out.
12. As an owner who deleted my account and then signs back in within 30 days, I see a "Welcome back — cancel deletion?" banner; tapping Cancel deletion removes the record and reactivates my profile.

### Block / Report from chat

13. As an owner in a chat, the overflow menu now shows **Unmatch** (existing), **Block** (new), and **Report** (new). Block confirms with "Block `<first name>`? You won't see each other again." → on confirm, writes `/blocks/{me}/{them}`, removes the match, deletes the chat, returns to the Matches tab.
14. As an owner, tapping Report opens a modal with reason chips (Spam / Fake profile / Harassment / Animal welfare concern / Other) and an optional free-text field. Submitting writes a `/reports/{reportId}` record. The chat stays open after submit; a snackbar confirms "Report submitted".

## 3. Domain model

### 3.1 In `:feature:profile:domain` (pure JVM)

```kotlin
data class OwnerProfile(
    val id: OwnerId,                            // from :core:session:domain
    val firstName: String,                       // 1..30 chars, trimmed
    val avatarUrl: String?,                      // Firebase Storage download URL
    val location: GeoPoint?,                     // null = not yet captured
    val paused: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class GeoPoint(
    val lat: Double,
    val lng: Double,
    val geohash: String,                         // computed client-side; deck-scaling prep
)

interface OwnerProfileRepository {
    /** Live stream of my profile. Emits null if not yet created. */
    fun observeMyProfile(): Flow<OwnerProfile?>

    /** Read another owner's profile (public; used by deck, match, chat, likes-you for display). */
    fun observeOwnerProfile(id: OwnerId): Flow<OwnerProfile?>

    /** Edit-profile mutations. Each does its own validation. */
    suspend fun updateFirstName(name: String)
    suspend fun updateAvatar(localUri: String)               // uploads to Storage, then writes URL
    suspend fun updateLocation(point: GeoPoint)
    suspend fun setPaused(paused: Boolean)
}
```

### 3.2 In `:feature:settings:domain` (pure JVM)

```kotlin
data class NotificationPrefs(
    val newMatch: Boolean,
    val newMessage: Boolean,
    val someoneLiked: Boolean,
    val weeklyDigest: Boolean,
) {
    companion object {
        val DEFAULT = NotificationPrefs(
            newMatch     = true,
            newMessage   = true,
            someoneLiked = true,
            weeklyDigest = false,
        )
    }
}

interface NotificationPrefsRepository {
    fun observePrefs(): Flow<NotificationPrefs>
    suspend fun updatePrefs(prefs: NotificationPrefs)
}

data class BlockedOwner(
    val id: OwnerId,
    val firstName: String,
    val avatarUrl: String?,
    val blockedAt: Instant,
)

interface BlockRepository {
    fun observeBlockedOwners(): Flow<List<BlockedOwner>>
    suspend fun block(otherOwnerId: OwnerId)             // also tears down any existing match
    suspend fun unblock(otherOwnerId: OwnerId)
    fun isBlocked(otherOwnerId: OwnerId): Flow<Boolean>  // used by deck/likes-you/match filters
}

enum class ReportReason { SPAM, FAKE_PROFILE, HARASSMENT, ANIMAL_WELFARE_CONCERN, OTHER }

data class ReportDraft(
    val reportedOwnerId: OwnerId,
    val reason: ReportReason,
    val freeText: String?,                       // null or 1..500 chars
    val context: ReportContext?,                  // optional — e.g. matchId or petId at point of report
)

sealed interface ReportContext {
    data class FromChat(val matchId: MatchId) : ReportContext
    data class FromDeck(val petId: PetId) : ReportContext
    data class FromLikes(val likeKey: LikeKey) : ReportContext
}

interface ReportRepository {
    suspend fun submit(draft: ReportDraft)               // writes /reports/{auto-id}
}

data class AccountDeletion(
    val requestedAt: Instant,
    val hardDeleteAt: Instant,                   // requestedAt + 30 days
)

interface AccountDeletionRepository {
    /** Returns the pending deletion if one exists. */
    fun observePendingDeletion(): Flow<AccountDeletion?>

    /** Schedules deletion: writes /accountDeletions/{me}, pauses the profile, signs out. */
    suspend fun requestDeletion()

    /** Cancels a pending deletion: removes /accountDeletions/{me}, unpauses the profile. */
    suspend fun cancelDeletion()
}
```

UseCases:

- Profile: `ObserveMyProfileUseCase`, `UpdateFirstNameUseCase`, `UpdateAvatarUseCase`, `UpdateLocationUseCase`, `SetPausedUseCase`
- Notifications: `ObserveNotificationPrefsUseCase`, `UpdateNotificationPrefsUseCase`
- Blocks: `ObserveBlockedOwnersUseCase`, `BlockOwnerUseCase`, `UnblockOwnerUseCase`, `IsBlockedUseCase`
- Reports: `SubmitReportUseCase`
- Account: `ObservePendingDeletionUseCase`, `RequestAccountDeletionUseCase`, `CancelAccountDeletionUseCase`

## 4. Firestore schema additions

### 4.1 `owners/{uid}` document

```
owners/{uid} = {
  firstName:    string,
  avatarUrl:    string?,                       // Firebase Storage download URL
  location:     GeoPoint?,                      // Firestore native; null = not yet set
  paused:       boolean,                        // default false
  notifications: {
    newMatch:     boolean,                      // default true
    newMessage:   boolean,                      // default true
    someoneLiked: boolean,                      // default true
    weeklyDigest: boolean,                      // default false
  },
  createdAt:    Timestamp,
  updatedAt:    Timestamp,
}
```

Notification prefs are folded into the owner doc as a sub-map (instead of a separate subcollection) — they're small, always loaded together, and toggling one is a single `update` on a nested field path.

Created by the sign-up flow on first auth (existing scaffold, extended to also write `paused: false`, `notifications: {...defaults}`, and `location: null`). Edited via Settings → Profile.

> The owner's `location` is a native Firestore `GeoPoint`. v2 can add a `geohash` field denormalized for range-prefix queries; v1 fetches and filters client-side (see deck-swipe.md §5.3).

### 4.2 `owners/{uid}/filters/current` (subcollection)

Already defined in `:core:filters:data` (per [`plans/deck-swipe.md`](./deck-swipe.md) §8). Settings → Filters reads/writes via that repository — no new schema.

### 4.3 `blocks` collection (top-level)

```
blocks/{blockId} = {                            // blockId = "${ownerId}_${blockedOwnerId}"
  ownerId:          string,                     // the user doing the blocking
  blockedOwnerId:   string,                     // the blocked party
  blockedAt:        Timestamp,
}
```

Top-level (not a subcollection) so **bidirectional enforcement is a direct query**:

- "Who have I blocked?" → `blocks.where("ownerId", "==", me)`
- "Who has blocked me?" → `blocks.where("blockedOwnerId", "==", me)`

No mirror collection, no Cloud-Function index maintenance. This is a clean win over the original RTDB shape, which required a `/blockedBy` denormalization to answer the second question.

**Composite indexes:** `(ownerId asc)` and `(blockedOwnerId asc)` — both single-field, auto-indexed.

### 4.4 `reports/{reportId}` collection

```
reports/{reportId} = {                          // Firestore auto-id
  reporter:        string,
  reportedOwnerId: string,
  reason:          string (SPAM | FAKE_PROFILE | HARASSMENT | ANIMAL_WELFARE_CONCERN | OTHER),
  freeText:        string?,
  context:         {                            // optional
    type:     'chat' | 'deck' | 'likes',
    matchId:  string?,
    petId:    string?,
    likeKey:  string?,
  },
  createdAt:       Timestamp,
}
```

Write-only from clients — `allow read: if false`. Admins read via the Firebase console or via a Cloud Function with custom claims (`plans/notifications-fcm.md` adds a thin moderation Slack hook).

### 4.5 `accountDeletions/{uid}` collection

```
accountDeletions/{uid} = {
  requestedAt:  Timestamp,
  hardDeleteAt: Timestamp,                      // requestedAt + 30 days
}
```

Presence indicates a pending soft delete. Hard delete is a scheduled Cloud Function — deferred to `plans/notifications-fcm.md`. For v1, hard delete is documented but not yet implemented; the soft-delete UX works in full (profile paused, sign-out, welcome-back banner on re-sign-in).

### 4.6 Security rules

```
match /owners/{ownerId} {
  allow read:   if request.auth != null;                                            // public profiles
  allow create: if request.auth != null && ownerId == request.auth.uid;
  allow update: if request.auth != null && ownerId == request.auth.uid;
  allow delete: if false;
}

match /blocks/{blockId} {
  allow read:   if request.auth != null
                && (resource.data.ownerId == request.auth.uid
                    || resource.data.blockedOwnerId == request.auth.uid);
  allow create: if request.auth != null && request.resource.data.ownerId == request.auth.uid;
  allow delete: if request.auth != null && resource.data.ownerId == request.auth.uid;
}

match /reports/{reportId} {
  allow read:   if false;                                                            // admin-only via console
  allow create: if request.auth != null && request.resource.data.reporter == request.auth.uid;
}

match /accountDeletions/{uid} {
  allow read:   if request.auth != null && uid == request.auth.uid;
  allow write:  if request.auth != null && uid == request.auth.uid;
}
```

`owners/{*}` is world-readable so the deck/match/chat/likes screens can render names + avatars. Notification prefs sit inside that doc but the client only reads its own field via the existing query (no separate access control needed since the doc is already readable). `blocks/{blockId}` is readable by **either** participant of the block — that's safe because we use the read access only for view-side filtering, not for leaking information ("you can know who blocked you" is a non-issue since the result of being blocked is just not seeing them; the metadata is benign).

## 5. Data layer

### 5.1 `:feature:profile:data`

```kotlin
@Singleton
class FirebaseOwnerProfileRepository @Inject constructor(
    private val sessionRepo: SessionRepository,
    private val database: FirebaseDatabase,
    private val storage: FirebaseStorage,
    private val locationClient: FusedLocationProviderClient,
    private val clock: Clock,
    @IoDispatcher private val io: CoroutineDispatcher,
) : OwnerProfileRepository {

    override fun observeMyProfile() = observeOwnerProfile(OwnerId(sessionRepo.currentUid()))
    override fun observeOwnerProfile(id: OwnerId): Flow<OwnerProfile?> = callbackFlow { ... }

    override suspend fun updateFirstName(name: String) = withContext(io) {
        val trimmed = name.trim()
        require(trimmed.length in 1..30) { "Name must be 1..30 chars" }
        update(mapOf("firstName" to trimmed))
    }

    override suspend fun updateAvatar(localUri: String) = withContext(io) {
        val me = sessionRepo.currentUid()
        val path = "owners/$me/avatar.jpg"
        val ref = storage.reference.child(path)
        ref.putFile(Uri.parse(localUri)).await()
        val url = ref.downloadUrl.await().toString()
        update(mapOf("avatarUrl" to url))
    }

    override suspend fun updateLocation(point: GeoPoint) = withContext(io) {
        update(mapOf(
            "location" to mapOf("lat" to point.lat, "lng" to point.lng, "geohash" to point.geohash),
        ))
    }

    override suspend fun setPaused(paused: Boolean) = withContext(io) {
        update(mapOf("paused" to paused))
    }

    private suspend fun update(fields: Map<String, Any?>) {
        val me = sessionRepo.currentUid()
        val patch = fields + ("updatedAt" to clock.now().toEpochMilliseconds())
        database.reference.child("owners/$me/profile").updateChildren(patch).await()
    }
}
```

Location capture flow (in presentation):

```kotlin
val location = locationClient.lastLocation.await()
val point = GeoPoint(
    lat     = location.latitude,
    lng     = location.longitude,
    geohash = Geohash.encode(location.latitude, location.longitude, precision = 6),
)
profileRepo.updateLocation(point)
```

`Geohash.encode` is a small pure-JVM utility in `:core:common` (no library dependency required — ~50 lines).

### 5.2 `:feature:settings:data`

Four repositories, all thin Firebase wrappers:

- `FirebaseNotificationPrefsRepository` — reads/writes `/owners/{me}/notificationPrefs`; emits `NotificationPrefs.DEFAULT` when the node is missing.
- `FirebaseBlockRepository` — reads/writes `/blocks/{me}`; `block(other)` is an atomic `updateChildren` that writes the block AND tears down any existing match/messages between the two (delegates to `MatchRepository.unmatch` + direct deletes).
- `FirebaseReportRepository` — `push()` + `setValue()` on `/reports/{auto-id}`.
- `FirebaseAccountDeletionRepository` — `requestDeletion()` runs three writes atomically: `/accountDeletions/{me}` set, `/owners/{me}/profile/paused` set to true, then triggers `signOut()` via Firebase Auth. `cancelDeletion()` reverses the first two.

Hilt bindings co-located in each `:data` module.

## 6. Presentation layer

### 6.1 `:feature:profile:presentation` — Profile tab (full impl)

Replaces the existing sign-out-only screen. Depends on `:feature:profile:domain`, `:feature:profile:nav`, `:feature:pet:domain` (My Pets list), `:feature:pet:nav` (Add/Edit pet routes), `:feature:settings:nav` (link to Settings home), `:feature:login:nav` (sign-out destination), `:core:designsystem`, `:core:ui`, `:core:navigation`, `:core:session:domain`.

```
ProfileScreen
├─ ProfileHeader (clickable → SettingsEditProfile)
│   ├─ Avatar (large, 96dp circle)
│   └─ FirstName (titleLarge) + email (bodySmall, muted)
├─ SectionHeader("My Pets")
├─ LazyVerticalGrid(columns = Fixed(2))
│   ├─ items(myPets) { PetThumbnailCard(...) onTap → PetDetail(petId) }
│   └─ AddPetTile (always at the end) → AddPet
├─ Divider
├─ MenuRow("Settings", icon = Settings, trailing = chevron) → SettingsHome
└─ MenuRow("Sign out", icon = Logout, destructive = true) → SignOutUseCase → LoginHome
```

`Routes.kt` (`:feature:profile:nav`) — unchanged from skeleton; still just `ProfileHome`.

### 6.2 `:feature:settings:presentation` — Settings hub + sub-screens

`Routes.kt` (`:feature:settings:nav`):

```kotlin
@Serializable data object SettingsHome
@Serializable data object SettingsEditProfile
@Serializable data object SettingsNotifications
@Serializable data object SettingsFilters
@Serializable data object SettingsPrivacy
@Serializable data object SettingsBlockedUsers
@Serializable data object SettingsAccount
```

#### SettingsHomeScreen

Categorized list:

```
TopBar("Settings", back arrow)
LazyColumn
├─ CategoryRow(icon, "Profile",       subtitle = "Name, avatar, location")        → SettingsEditProfile
├─ CategoryRow(icon, "Notifications", subtitle = "Matches, messages, likes")      → SettingsNotifications
├─ CategoryRow(icon, "Filters",       subtitle = "Distance, intent, species")     → SettingsFilters
├─ CategoryRow(icon, "Privacy",       subtitle = "Pause, blocked users")          → SettingsPrivacy
└─ CategoryRow(icon, "Account",       subtitle = "Email, delete account")         → SettingsAccount
```

#### SettingsEditProfileScreen

```
TopBar("Profile", back arrow)
Column (scrollable)
├─ AvatarPicker (current avatar + "Change photo" button → camera/gallery → upload)
├─ TextField("First name", value = currentName, 30-char counter)
├─ LocationRow
│   ├─ if location != null: "Set: <city-ish from reverse-geocode or just coords>" + "Update" button
│   └─ if location == null: "Set your location to start matching" + primary "Set location" button
└─ Save button (pinned bottom, enabled when fields are valid AND dirty)
```

Save triggers `UpdateFirstNameUseCase` and/or `UpdateAvatarUseCase` and/or `UpdateLocationUseCase` based on which fields changed. Each is its own write; partial failure is surfaced per-field.

#### SettingsNotificationsScreen

```
TopBar("Notifications", back arrow)
LazyColumn
├─ SwitchRow("New match",             default true,  prefs.newMatch)
├─ SwitchRow("New message",           default true,  prefs.newMessage)
├─ SwitchRow("Someone liked your pet", default true, prefs.someoneLiked)
└─ SwitchRow("Weekly digest",         default false, prefs.weeklyDigest)
```

Each switch debounces 200ms and writes the entire `NotificationPrefs` (since RTDB writes are cheap and the four bools are tightly coupled).

#### SettingsFiltersScreen

```
TopBar("Filters", back arrow)
Column (scrollable)
├─ DistanceSection
│   ├─ Slider(5..200 km, current = filters.maxDistanceKm, snap to 5, 25, 50, 100, 200)
│   └─ Label("Show pets within <N> km")
├─ IntentSection
│   └─ FilterChipRow(Playdate / Adoption / Friendship, multi-select)
└─ SpeciesSection
    └─ FilterChipRow(Dogs / Cats / Small mammals, multi-select)
```

No Save button — writes via `:core:filters:data` on each change (debounced 200ms).

#### SettingsPrivacyScreen

```
TopBar("Privacy", back arrow)
LazyColumn
├─ SwitchRow("Pause profile", subtitle = "Hide your pets from other owners' decks", profile.paused)
└─ MenuRow("Blocked users", trailing = "<count> blocked", chevron) → SettingsBlockedUsers
```

#### SettingsBlockedUsersScreen

```
TopBar("Blocked users", back arrow)
if blockedOwners.isEmpty():
    EmptyTabState(icon = Block, headline = "Nobody blocked", body = "You can block someone from any chat overflow menu.")
else:
    LazyColumn
    └─ items(blockedOwners) { blocked ->
        Row {
            Avatar(blocked.avatarUrl)
            Column { Text(blocked.firstName); Text("Blocked <time-ago>", labelSmall) }
            Spacer(weight = 1f)
            OutlinedButton("Unblock") → UnblockOwnerUseCase(blocked.id)
        }
    }
```

#### SettingsAccountScreen

```
TopBar("Account", back arrow)
Column
├─ InfoRow("Email", email from Firebase Auth, read-only)
├─ if pendingDeletion != null:
│   ├─ Banner("Account deletion scheduled — <N> days remaining")
│   └─ OutlinedButton("Cancel deletion") → CancelAccountDeletionUseCase
└─ Spacer + Destructive section
    └─ DestructiveButton("Delete account") → DeleteAccountModal
```

#### DeleteAccountModal

```
AlertDialog(
    title = "Delete your account?",
    text = """
        This will:
        • Hide your profile and all your pets immediately.
        • Schedule permanent deletion in 30 days. Your data is gone after that.
        • Sign you out.

        You can cancel within 30 days by signing back in.

        To confirm, type DELETE below.
    """,
    content = TextField(value, placeholder = "Type DELETE"),
    confirm = "Delete account" (enabled only when value.trim() == "DELETE"),
    dismiss = "Cancel",
)
```

On confirm → `RequestAccountDeletionUseCase` → sign out → land on Login.

#### Welcome-back banner

`StartDestinationModule` (in `:app/di/`) checks `/accountDeletions/{me}` on cold start when a session exists. If present:

- Land on `DeckHome` as usual.
- Set a one-shot `UiEvent.WelcomeBackBanner(daysRemaining)` consumed by `MainActivity` and displayed as a top-of-screen banner with "Cancel deletion" action.

(Implementation detail: store the one-shot flag in a `SharedFlow` on a `WelcomeBackEvents` class injected at `:app` level.)

#### EntryProviderInstaller

```kotlin
@Module @InstallIn(ActivityRetainedComponent::class)
object SettingsNavModule {
    @IntoSet @Provides
    fun provideSettingsEntries(navigator: Navigator): EntryProviderInstaller = {
        entry<SettingsHome>          { SettingsHomeScreen(onCategory = { route -> navigator.goTo(route) }) }
        entry<SettingsEditProfile>   { EditProfileScreen(onBack = { navigator.back() }) }
        entry<SettingsNotifications> { NotificationsScreen(onBack = { navigator.back() }) }
        entry<SettingsFilters>       { FiltersScreen(onBack = { navigator.back() }) }
        entry<SettingsPrivacy>       { PrivacyScreen(onBack = { navigator.back() }, onBlocked = { navigator.goTo(SettingsBlockedUsers) }) }
        entry<SettingsBlockedUsers>  { BlockedUsersScreen(onBack = { navigator.back() }) }
        entry<SettingsAccount>       { AccountScreen(onBack = { navigator.back() }, onSignedOut = { navigator.replaceAll(LoginHome) }) }
    }
}
```

### 6.3 Chat overflow extension

Extend `:feature:chat:presentation`'s overflow menu (currently just **Unmatch** per `match-and-chat.md` §6.2):

```kotlin
DropdownMenu {
    DropdownMenuItem("Unmatch", icon = HeartBroken)  { ... existing ... }
    DropdownMenuItem("Block",   icon = Block)        { showBlockConfirm = true }
    DropdownMenuItem("Report",  icon = Report)       { showReportSheet = true }
}
```

**Block confirm dialog:**

```
AlertDialog(
    title = "Block <first name>?",
    text = "You won't see each other in the deck or matches again. This also deletes your conversation.",
    confirm = "Block",
    dismiss = "Cancel",
)
```

On confirm → `BlockOwnerUseCase(otherOwnerId)` → on success, `navigator.replaceAll(MatchesHome)`.

**Report sheet:**

```
ModalBottomSheet
├─ Headline ("Report <first name>")
├─ FilterChipRow (reasons, single-select required: Spam / Fake profile / Harassment / Animal welfare concern / Other)
├─ TextField (optional "Additional details", 500-char counter)
└─ PrimaryButton("Submit report", enabled when a reason is selected)
```

On submit → `SubmitReportUseCase(ReportDraft(otherOwnerId, reason, freeText, context = FromChat(matchId)))` → snackbar "Report submitted".

`:feature:chat:presentation` gains dependencies on `:feature:settings:domain` and `:feature:settings:nav` (just for the use cases — no UI imports).

## 7. Existing-feature integrations / cross-plan edits

Bundled into this PR because they touch features already implemented:

1. **Sign-up flow extension.** When a new account is created, write the initial `/owners/{uid}/profile` record (firstName from sign-up, avatarUrl from sign-up, location = null, paused = false). Currently the sign-up flow doesn't write this. Update `:feature:signup:data` (or wherever sign-up wiring lives) to do the atomic write.
2. **Deck view filters extended for blocks + pause.** The deck's filter pipeline (per `plans/deck-swipe.md` §5.3) gets two more exclusions: drop pets whose `ownerId` is in `/blocks/{me}/*`, drop pets whose owner has `profile.paused == true`. The bidirectional block check (also exclude owners who blocked me) is captured in §9 open question 1.
3. **Likes you view filters extended for blocks + pause.** Same exclusions applied to the `/likedYouBy/{me}` iteration.
4. **Inbox view filters extended for blocks.** Matches involving a blocked owner are hidden from the inbox (the underlying match record stays until the block actually tears it down via `BlockOwnerUseCase`).
5. **Welcome-back check.** Edit `StartDestinationModule.provideStartDestination` to also check `/accountDeletions/{me}` and emit a one-shot welcome-back event on app start.

Each is a small surgical change; collectively they keep the cross-cutting concerns visible to a reader of this plan rather than scattered across multiple PRs.

## 8. Module structure & dependencies

```
:feature:profile
  :nav            // Existing. ProfileHome route.
  :domain         // NEW. OwnerProfile, GeoPoint, OwnerProfileRepository, UseCases.
  :data           // NEW. FirebaseOwnerProfileRepository.
  :presentation   // REPLACED. Full ProfileScreen with My Pets and Settings menu.

:feature:settings
  :nav            // NEW. 7 route keys (SettingsHome and 6 sub-screen routes).
  :domain         // NEW. NotificationPrefs, BlockedOwner, ReportDraft, AccountDeletion, repositories, UseCases.
  :data           // NEW. Four Firebase repository impls.
  :presentation   // NEW. Settings hub + 6 sub-screens.
```

Dependencies:

```
:feature:profile:domain
  ├─► :core:session:domain
  └─► :core:common

:feature:profile:data
  ├─► :feature:profile:domain
  ├─► :core:firebase
  ├─► :core:session:domain
  └─► :core:common
  (+ runtime dep on Play Services Location for FusedLocationProviderClient — wired in this :data module)

:feature:profile:presentation
  ├─► :feature:profile:domain
  ├─► :feature:profile:nav
  ├─► :feature:pet:domain         (My Pets grid)
  ├─► :feature:pet:nav            (AddPet / PetDetail routes)
  ├─► :feature:settings:nav       (SettingsHome route)
  ├─► :feature:login:nav          (LoginHome for sign-out)
  ├─► :core:designsystem
  ├─► :core:ui
  ├─► :core:navigation
  └─► :core:session:domain

:feature:settings:domain
  ├─► :feature:match:domain       (MatchId for ReportContext.FromChat)
  ├─► :feature:pet:domain         (PetId for ReportContext.FromDeck)
  ├─► :feature:likes:domain       (LikeKey for ReportContext.FromLikes)
  ├─► :core:filters:domain        (FilterPrefs used by Filters screen)
  ├─► :core:session:domain
  └─► :core:common

:feature:settings:data
  ├─► :feature:settings:domain
  ├─► :feature:match:domain        (delegating block-teardown to MatchRepository.unmatch)
  ├─► :feature:match:data          (yes — for direct unmatch call; this is the one allowed exception, see scaffold §2.1 note)
  ├─► :core:firebase
  ├─► :core:session:domain
  └─► :core:common

:feature:settings:presentation
  ├─► :feature:settings:domain
  ├─► :feature:settings:nav
  ├─► :feature:login:nav           (sign-out destination)
  ├─► :core:filters:domain         (FilterPrefs)
  ├─► :core:designsystem
  ├─► :core:ui
  ├─► :core:navigation
  └─► :core:session:domain
```

> **Note on `:feature:settings:data → :feature:match:data`:** the scaffold rules forbid `:feature:X:data → :feature:Y:data`. We resolve this by adding a `MatchTeardownService` interface in `:feature:match:domain` (which `MatchRepository` already implements), so `:feature:settings:data` only depends on `:feature:match:domain`. Document the interface explicitly.

Convention plugins:
- All `:nav` and `:domain` → `tinpet.jvm.library` (+ `tinpet.kotlin.serialization` for `:nav`).
- All `:data` → `tinpet.android.library` + `tinpet.hilt`.
- All `:presentation` → `tinpet.android.feature`.

Register `:feature:profile:presentation` (already in `:app`) and add `:feature:settings:presentation` to `:app/build.gradle.kts`.

## 9. Open questions / future work

1. **Bidirectional block check.** Resolved by the Firestore data model — top-level `blocks` collection with read access for either participant lets the client query both "people I blocked" and "people who blocked me" with the same collection. No denormalized mirror needed.
2. **Account deletion hard-purge.** Soft delete + 30-day grace works end-to-end on the client. Actual purge (delete all `/pets/{*}` where `ownerId == uid`, `/owners/{uid}/*`, `/likes/{uid}/*`, `/messages/{matchId}/*` for matches involving uid, etc.) is **a scheduled Cloud Function** — captured in `plans/notifications-fcm.md`.
3. **Reverse geocoding for location display.** Showing "Brooklyn, NY" instead of raw coordinates needs reverse geocoding. v1 shows just "Location set" with a small map preview or no preview. Add reverse geocoding when it becomes user-facing painful.
4. **Avatar upload progress.** Settings → Profile → "Change photo" runs an upload that may take a few seconds. v1: show an inline `CircularProgressIndicator` and disable Save during upload. Cancellation is not supported (small file, short upload).
5. **Report rate-limiting.** A malicious user could spam `/reports`. v1: client-side throttle (max 5 reports per day per reporter, tracked locally). Server-side enforcement is a Cloud Function concern.
6. **Pause toggle UX placement.** Currently buried under Settings → Privacy. Could surface in Profile tab header for one-tap pause. Defer until user feedback.
7. **Sign out from chat / deck overflow.** Currently sign-out only lives in Profile. Acceptable for v1 — adding more entry points is trivial later.

## 10. Out of scope

- Custom notification quiet hours.
- Per-pet notification preferences (e.g., "only notify me about likes for Buddy").
- Account recovery flows (e.g., "I forgot my email"). Handled by Firebase Auth UI.
- Multi-language support / locale-driven distance units (km only in v1).
- Tablet / split-screen Settings layout.
- In-app moderation tooling (no admin role, no review UI).
- Email change.
- Password change (handled by Firebase Auth's built-in flows, accessed via deep link).
- "Why was I blocked?" explanations for the blocked party (they just stop seeing the blocker).

## 11. Verification

1. Build:
   ```bash
   JAVA_HOME=/Users/jrodiz/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home ./gradlew assembleDebug
   ```
2. JVM isolation:
   ```bash
   ./gradlew :feature:profile:nav:dependencies      --configuration runtimeClasspath
   ./gradlew :feature:profile:domain:dependencies   --configuration runtimeClasspath
   ./gradlew :feature:settings:nav:dependencies     --configuration runtimeClasspath
   ./gradlew :feature:settings:domain:dependencies  --configuration runtimeClasspath
   ```
   None should pull in `androidx.*`, Compose, Room, Retrofit, Hilt, or Firebase SDKs.
3. Unit tests (with Firebase emulator):
   - `UpdateFirstNameUseCase` rejects empty / > 30-char names.
   - `BlockOwnerUseCase` removes any existing match between the two owners and writes `/blocks/{me}/{other}`.
   - `UnblockOwnerUseCase` removes `/blocks/{me}/{other}` and does not auto-recreate matches.
   - `NotificationPrefs.DEFAULT` is emitted when `/owners/{me}/notificationPrefs` is missing.
   - `RequestAccountDeletionUseCase` writes `/accountDeletions/{me}`, sets `profile.paused = true`, and triggers sign-out.
   - `CancelAccountDeletionUseCase` removes `/accountDeletions/{me}` and sets `profile.paused = false`.
   - `Geohash.encode` against known reference values.
4. Manual on emulator:
   - Profile tab: avatar + name visible, My Pets grid renders, "Settings" and "Sign out" rows present.
   - Tap Settings → see categorized hub.
   - Tap each category → sub-screen pushes correctly.
   - Edit profile → change first name → save → see new name on Profile tab.
   - Edit profile → grant location permission → location captured → deck now shows pets (vs. previous "Set your location" empty state).
   - Notifications → toggle each switch → verify `/owners/{me}/notificationPrefs` updates in Firebase console.
   - Filters → adjust distance slider, intent chips, species chips → return to Deck tab → verify pool reflects.
   - Privacy → toggle Pause → log in as another user → verify my pets no longer appear in their deck.
   - Privacy → Blocked users → empty state initially → block someone from a chat → return to the list → see them → tap Unblock → list empties.
   - Account → tap Delete account → modal opens → typing "delete" leaves button disabled, typing "DELETE" enables it → confirm → sign-out happens → log back in → see "Welcome back" banner with Cancel deletion CTA → tap Cancel → banner clears.
   - Chat overflow → Block → confirm → match disappears for both sides; blocked party can't see me in their deck.
   - Chat overflow → Report → pick reason → submit → snackbar confirms → verify a `/reports/{auto-id}` record exists in Firebase console.
   - Capture screenshots of: Profile tab, Settings home, Edit profile, Notifications, Filters, Privacy, Blocked users (populated), Account with pending deletion banner, the Delete-account modal mid-input, and a fresh Report submission.

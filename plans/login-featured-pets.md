# Plan — Featured pets on the Login screen

> **NOTE on plan location:** project convention ([[feedback_plans_location]]) is to save plans under `plans/` in the Arch2.0 repo. Plan mode pins the working copy here. **Step 0 of implementation: copy this file to `plans/login-featured-pets.md`** so it gets versioned with the code.

## 1. Context

The Login hero today renders three decorative PNG pet tiles (`pet_tile_puppies`, `pet_tile_cat`, `pet_tile_rabbit`) — see `feature/login/presentation/src/main/kotlin/com/rodiz/arch2/feature/login/presentation/screen/LoginScreen.kt:79-206`. The user wants those replaced with up to 3 pets they've selected ("featured"), persisted across sign-out, and wiped when a *different* user signs in.

**User-approved scope (this session):**

1. Selection UI is **both** a pin icon on every My Pets card AND a Settings entry. Same toggle; shared state via the new repo.
2. Empty slots on the Login hero are filled with the existing decorative paw PNGs. 0 selected → original 3-PNG layout (today's behavior). 1 → 1 real tile + 2 paws. 2 → 2 real + 1 paw. 3 → all real.

**Critical constraint:** Firestore rules block reads from unauthenticated clients (`firestore.rules:44-52`, `allow read: if request.auth != null`). The login screen renders pre-auth, so the cached pet metadata (id, name, species, avatarUrl) must live in DataStore. Coil's HTTP cache handles the image bytes; if Coil evicts, we fall back to the decorative paw graphic for that slot.

**Cache wipe trigger:** when a session is established for a userId different from the previously-cached `featured_pets_last_user_id`. NOT on `SessionRepository.clear()` — sign-out must preserve the cache.

## 2. Module layout

Two new modules, mirroring `:core:filters:{domain,data}`:

**`:core:featuredpets:domain`** (pure JVM, `tinpet.jvm.library`)
- No Android deps. Safe for `:feature:login:presentation` to consume directly (presentation may depend on a `:core:*:domain` JVM module — see [[feedback_cross_feature_display_pattern]]).
- Files:
  - `FeaturedPet.kt` — `data class FeaturedPet(id: String, name: String, species: String?, avatarUrl: String?)`.
  - `FeaturedPetsState.kt` — `data class FeaturedPetsState(featured: List<FeaturedPet>)`.
  - `FeaturedPetsRepository.kt` — interface (below).
  - Top-level `const val MAX_FEATURED_PETS = 3`, `enum class UserChangeResult { SAME_USER, FIRST_USER, USER_CHANGED }`.
- Deps: `kotlinx.coroutines.core`, `javax.inject`. **Do NOT** depend on `:core:petlookup:domain` — keeps the JVM module standalone; we accept a `PetDisplay`-shaped subset via the local `FeaturedPet` model.

**`:core:featuredpets:data`** (Android, Hilt + serialization)
- Apply `tinpet.android.library`, `tinpet.android.hilt`, `tinpet.kotlin.serialization`.
- Depends on `:core:featuredpets:domain`, `:core:datastore`.
- Files:
  - `DataStoreFeaturedPetsRepository.kt` — `@Singleton` impl.
  - `FeaturedPetCacheEntry.kt` — `@Serializable` DTO (id/name/species/avatarUrl).
  - `FeaturedPetsSessionGate.kt` — runs the user-change wipe logic from App.kt.
  - `di/FeaturedPetsModule.kt` — `@Binds` impl → interface.

`settings.gradle.kts` adds both. `app/build.gradle.kts` adds `implementation(project(":core:featuredpets:data"))` so the Hilt module is in the graph.

### Repository interface

```kotlin
interface FeaturedPetsRepository {
    fun observe(): Flow<FeaturedPetsState>
    suspend fun current(): FeaturedPetsState

    /** Add a pet. Returns false (and is a no-op) when already at MAX_FEATURED_PETS. */
    suspend fun pin(pet: FeaturedPet): Boolean

    /** Remove a pet. No-op if not present. */
    suspend fun unpin(petId: String)

    /**
     * Reconcile cached metadata against the authoritative list from My Pets.
     * Drops entries for pets that no longer exist; refreshes name/species/url
     * for ones that do; preserves order.
     */
    suspend fun refreshFrom(authoritative: Map<String, FeaturedPet>)

    /** Wipes featured set but preserves the last-user marker. Called only by the gate. */
    suspend fun wipe()

    /**
     * Records the currently-signed-in uid and returns whether it differs from
     * the previously-recorded one. Atomic via a single DataStore edit().
     */
    suspend fun onUserActive(userId: String): UserChangeResult
}
```

## 3. DataStore schema

Add three keys to the existing shared `app_prefs` `DataStore<Preferences>` (`core/datastore/.../DataStoreModule.kt:24-29`). Namespaced with `featured_pets_*` to coexist with `session_*` and `filters_*`.

| Key | Type | Notes |
|---|---|---|
| `featured_pets_json` | `stringPreferencesKey` | JSON-encoded `List<FeaturedPetCacheEntry>`, order preserved, capped at 3. Single string preferred over `stringSetPreferencesKey` because Set is unordered (slot stability) and one JSON string avoids parallel-keys parsing. |
| `featured_pets_last_user_id` | `stringPreferencesKey` | The uid that wrote the current cache. `onUserActive` reads + updates this to detect user-switch. |
| `featured_pets_schema_version` | `intPreferencesKey` | Currently `1`. On read, if value > known → treat cache as empty (forward-compat, no crash). |

```kotlin
@Serializable
internal data class FeaturedPetCacheEntry(
    val id: String,
    val name: String,
    val species: String? = null,
    val avatarUrl: String? = null,
)

private val FeaturedPetsJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
}
```

Read path wraps decoding in `runCatching { ... }` and falls back to `emptyList()` so corrupted JSON doesn't crash startup.

## 4. User-change gate

App startup, in the supervisor coroutine that already collects `sessionRepository.observe()` for Crashlytics user attribution (`App.kt:72-78`). Same `distinctUntilChanged { a, b -> a?.userId == b?.userId }`, expand the `onEach` to call both `crashReporter.setUserId(...)` and the new gate.

```kotlin
// App.kt
@Inject lateinit var featuredPetsGate: FeaturedPetsSessionGate

private fun observeSession() {
    val scope = CoroutineScope(SupervisorJob() + io)
    sessionRepository.observe()
        .distinctUntilChanged { a, b -> a?.userId == b?.userId }
        .onEach { session ->
            crashReporter.setUserId(session?.userId)
            featuredPetsGate.onSessionChanged(session?.userId)
        }
        .launchIn(scope)
}
```

The gate:

```kotlin
@Singleton
class FeaturedPetsSessionGate @Inject constructor(
    private val repo: FeaturedPetsRepository,
) {
    suspend fun onSessionChanged(userId: String?) {
        // Sign-out (null) preserves the cache — that's the whole point.
        if (userId == null) return
        when (repo.onUserActive(userId)) {
            UserChangeResult.USER_CHANGED -> repo.wipe()
            UserChangeResult.FIRST_USER, UserChangeResult.SAME_USER -> Unit
        }
    }
}
```

Modifying `SessionRepositoryImpl.clear()` was the wrong place: it would conflate sign-out (preserve) with user-switch (wipe). The gate keeps the two concerns separate.

## 5. Pin UI on My Pets cards

Modify `feature/pet/presentation/.../PetThumbnailCard.kt`:

1. Add params: `isFeatured: Boolean = false`, `onToggleFeature: () -> Unit = {}`, `showPin: Boolean = true`.
2. Inside `PetPhotoBox`, add a pin overlay alongside the existing edit pencil at `Alignment.TopEnd`, vertically stacked with `Arrangement.spacedBy(6.dp)`. Keep the existing 30dp circle pattern.
   - Icon: `Icons.Filled.PushPin` (active) / `Icons.Outlined.PushPin` (inactive).
   - Active: white icon on `BrandColors.CoralDeep` circle.
   - Inactive: white icon on `Color.Black.copy(alpha = 0.35f)` — matches the edit affordance.
3. `showPin = false` for archived/disabled pets so they can't be featured (archived pets are hidden from the deck and Login).

`MyPetsViewModel` gets a new constructor param `private val featuredRepo: FeaturedPetsRepository`. Exposes:

- `featuredIds: StateFlow<Set<String>>` via `featuredRepo.observe().map { it.featured.map(FeaturedPet::id).toSet() }.stateIn(...)`.
- `fun onTogglePin(pet: Pet)`:
  - If `pet.id.value in featuredIds.value` → `featuredRepo.unpin(pet.id.value)`.
  - Else → build `FeaturedPet(pet.id.value, pet.name, pet.species.name, pet.photos.firstOrNull()?.remoteUrl())` and call `pin(...)`. If `pin` returns `false`, set `errorMessage = "You can feature up to 3 pets"`.

Pin a 4th attempt → **block + snackbar**. Auto-demoting the oldest pinned pet would silently desync the Settings screen and surprise the user.

`MyPetsRoute` folds `featuredIds` into `MyPetsUiState` (via `combine` in the VM) so the card already knows its pin state — no extra state in the route. Passes `isFeatured = pet.id.value in state.featuredIds` and `onToggleFeature = { vm.onTogglePin(pet) }` down to each `PetThumbnailCard`.

## 6. Featured-pets Settings screen

**Route** in `feature/settings/nav/.../Routes.kt`:

```kotlin
@Serializable
data object SettingsFeaturedPets
```

**Settings home row** in `SettingsHomeScreen.kt`, added to the MATCHING section between Filters and Notifications:

```kotlin
SettingsRow(
    icon = Icons.Outlined.PushPin,
    iconBackground = BrandColors.CoralTint,
    iconTint = BrandColors.CoralDeep,
    title = stringResource(R.string.settings_row_featured_title),         // "Featured on login"
    subtitle = stringResource(R.string.settings_row_featured_subtitle),   // "Choose up to 3 pets"
    onClick = onOpenFeatured,
    trailing = {
        Row(...) {
            if (state.featuredCount > 0) CountPill(count = state.featuredCount)
            ChevronTrailing()
        }
    },
)
```

`SettingsHomeViewModel` collects `featuredRepo.observe()` for `featuredCount`. `SettingsNavModule` registers the `SettingsFeaturedPets` entry and wires `onOpenFeatured = { navigator.goTo(SettingsFeaturedPets) }`.

**New screen + VM** in `:feature:settings:presentation`:

`FeaturedPetsScreen.kt` + `FeaturedPetsRoute.kt` + `FeaturedPetsViewModel.kt`.

```kotlin
@HiltViewModel
internal class FeaturedPetsViewModel @Inject constructor(
    observeMyPets: ObserveMyPetsUseCase,
    private val featuredRepo: FeaturedPetsRepository,
) : ViewModel() {
    val uiState: StateFlow<FeaturedPetsUiState> = combine(
        observeMyPets(setOf(PetState.ACTIVE)),
        featuredRepo.observe(),
    ) { pets, featuredState ->
        val featuredIds = featuredState.featured.map { it.id }.toSet()
        FeaturedPetsUiState(
            pets = pets.map { PetRow(it, isFeatured = it.id.value in featuredIds) },
            featuredCount = featuredIds.size,
            atLimit = featuredIds.size >= MAX_FEATURED_PETS,
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, FeaturedPetsUiState())

    fun onToggle(pet: Pet) { /* same as MyPetsViewModel.onTogglePin */ }
}
```

`feature/settings/presentation/build.gradle.kts` adds `implementation(project(":feature:pet:domain"))` and `implementation(project(":core:featuredpets:domain"))`.

**State sharing answer:** both the Settings VM and the MyPets VM observe the same `FeaturedPetsRepository`. No draft state, no confirm/cancel — every toggle writes immediately and propagates live to the other surface. Matches `FilterPrefsRepository` precedent.

UI layout: quota banner at top ("2 of 3 selected"), vertical list of active pets with a trailing pin toggle per row. Empty-state when the user has no pets ("Add a pet to feature it" → CTA navigating to AddPet).

## 7. Login hero changes

`feature/login/presentation/build.gradle.kts` adds `implementation(project(":core:featuredpets:domain"))` (Coil already declared).

**Hoist state** — `LoginRoute` collects featured pets via a small new VM (kept separate from `LoginViewModel` to leave its 12 existing tests untouched):

```kotlin
@HiltViewModel
internal class LoginFeaturedPetsViewModel @Inject constructor(
    repo: FeaturedPetsRepository,
) : ViewModel() {
    val featured: StateFlow<List<FeaturedPet>> = repo.observe()
        .map { it.featured }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
}
```

`LoginRoute` injects it via `hiltViewModel()` and forwards `featured` into `LoginScreen(state, featured, onAction)`.

**Slot rendering** — `Hero` and `HeroAndCard` are refactored to use a per-slot composable. Important caveat from the existing code: the **rabbit tile is rendered in `HeroAndCard`, not `Hero`** (line 126) — it sits at the hero/card boundary. To keep visuals identical when slot 2 is empty, leave the rabbit position rendering inside `HeroAndCard`; just extract:

```kotlin
@Composable
private fun HeroPetSlot(
    pet: FeaturedPet?,
    @DrawableRes fallbackPawRes: Int,
    modifier: Modifier,  // call site builds offset + size + rotate + clip + border
) {
    if (pet?.avatarUrl != null) {
        AsyncImage(
            model = pet.avatarUrl,
            contentDescription = null, // hero merges semantics
            contentScale = ContentScale.Crop,
            modifier = modifier,
            placeholder = painterResource(fallbackPawRes),
            error = painterResource(fallbackPawRes),
            fallback = painterResource(fallbackPawRes),
        )
    } else {
        Image(
            painter = painterResource(fallbackPawRes),
            contentDescription = null,
            modifier = modifier,
        )
    }
}
```

Three call sites (TopLeft puppies, RightMid cat, BottomCenter rabbit), each passing `pet = featured.getOrNull(slotIndex)` and its own decorative paw drawable as fallback. The rotation, white rounded border (20dp), and positioning all stay in `modifier`, applied uniformly whether the child is `AsyncImage` or `Image`.

Accessibility: keep the hero's `.semantics(mergeDescendants = true) { }` (line 146) and each tile's `.clearAndSetSemantics {}`. Pet names are NOT exposed via the Login hero — they're decorative on this surface.

Compose previews: add a new preview pass with `featured = listOf(FeaturedPet("1","Rex",null,null), FeaturedPet("2","Mochi",null,null))` to validate the mixed-state layout.

## 8. Post-auth refresh

When and where to call `refreshFrom`:

- **MyPetsViewModel.init** — already subscribes to `ObserveMyPetsUseCase`. After first emission, call `featuredRepo.refreshFrom(pets.associateBy { it.id.value })` once per VM instance (guard with a private `Boolean`).
- **FeaturedPetsViewModel.init** — same pattern, so opening Settings → Featured triggers a refresh on the first emission too.

Implementation sketch (in both VMs):

```kotlin
private var refreshed = false
init {
    viewModelScope.launch {
        observeMyPets(setOf(PetState.ACTIVE)).collect { pets ->
            if (!refreshed) {
                refreshed = true
                featuredRepo.refreshFrom(pets.associate { p ->
                    p.id.value to FeaturedPet(
                        id = p.id.value,
                        name = p.name,
                        species = p.species.name,
                        avatarUrl = p.photos.firstOrNull()?.remoteUrl(),
                    )
                })
            }
            // existing handling
        }
    }
}

private fun PetPhoto.remoteUrl(): String? =
    (source as? PhotoSource.Remote)?.downloadUrl
```

An account that signs in but never opens My Pets / Settings keeps slightly stale cache until next session — acceptable because the cached fields are low-churn (name + url). If we want stricter freshness later, hook into the App-level session observer post-auth.

## 9. Tests

JUnit5 + `MainDispatcherExtension` + hand-rolled fakes (matches `LoginViewModelTest` / `DeckViewModelTest` style).

1. **`DataStoreFeaturedPetsRepositoryTest`** (`:core:featuredpets:data`) — drive a real `PreferenceDataStoreFactory.create` against a `tempDir`.
   - `pin` adds to ordered list, returns true.
   - `pin` at limit returns false, no mutation.
   - `unpin` removes by id, no-op when absent.
   - `refreshFrom` drops absent ids, updates name/url for present ids, preserves order.
   - `observe` emits initial empty, then mutations.
   - `wipe` clears `featured_pets_json` but retains `featured_pets_last_user_id`.
   - `onUserActive`: `FIRST_USER` when no last id, `SAME_USER` on match, `USER_CHANGED` otherwise.
   - Corrupt JSON in the store → reads as empty list (no crash).

2. **`FeaturedPetsSessionGateTest`** (`:core:featuredpets:data`) — fake repo records call sequence.
   - `onSessionChanged(null)` → `repo.wipe` NOT called, `onUserActive` NOT called.
   - `onSessionChanged("a")` first time → no wipe (FIRST_USER).
   - `onSessionChanged("a")` then `"a"` again → no wipe.
   - `onSessionChanged("a")` then `"b"` → `repo.wipe` called once.
   - Sign-out between sign-ins preserves cache: `"a"` → `null` → `"a"` causes no wipe.

3. **`LoginFeaturedPetsViewModelTest`** (`:feature:login:presentation`) — minimal.
   - Fake repo emits `[]` → VM state empty.
   - Emit `[p1, p2]` → VM state has both, in order.

4. **`MyPetsViewModelTest`** — extend the existing test (or add one if missing).
   - `onTogglePin` for unfeatured pet → `repo.pin` called with mapped `FeaturedPet`.
   - `onTogglePin` for featured pet → `repo.unpin` called.
   - `pin` returns false → `errorMessage` set with the limit string.
   - `featuredIds` reflects repo state across emissions (Turbine).
   - First emission triggers `refreshFrom`; subsequent emissions do not.

5. **`FeaturedPetsViewModelTest`** (`:feature:settings:presentation`).
   - `pets` list reflects `myPets × featured` intersection (`isFeatured` correct per row).
   - `atLimit` true at 3, false at 2.
   - Toggle behavior + snackbar same as MyPets.

No Compose UI tests required — the rendering is straightforward and hero geometry is preview-validated.

## 10. Files

### Add
- `core/featuredpets/domain/build.gradle.kts`
- `core/featuredpets/domain/src/main/kotlin/com/rodiz/arch2/core/featuredpets/domain/FeaturedPet.kt`
- `core/featuredpets/domain/src/main/kotlin/com/rodiz/arch2/core/featuredpets/domain/FeaturedPetsState.kt`
- `core/featuredpets/domain/src/main/kotlin/com/rodiz/arch2/core/featuredpets/domain/FeaturedPetsRepository.kt`
- `core/featuredpets/data/build.gradle.kts`
- `core/featuredpets/data/src/main/AndroidManifest.xml`
- `core/featuredpets/data/src/main/kotlin/com/rodiz/arch2/core/featuredpets/data/DataStoreFeaturedPetsRepository.kt`
- `core/featuredpets/data/src/main/kotlin/com/rodiz/arch2/core/featuredpets/data/FeaturedPetCacheEntry.kt`
- `core/featuredpets/data/src/main/kotlin/com/rodiz/arch2/core/featuredpets/data/FeaturedPetsSessionGate.kt`
- `core/featuredpets/data/src/main/kotlin/com/rodiz/arch2/core/featuredpets/data/di/FeaturedPetsModule.kt`
- `core/featuredpets/data/src/test/kotlin/com/rodiz/arch2/core/featuredpets/data/DataStoreFeaturedPetsRepositoryTest.kt`
- `core/featuredpets/data/src/test/kotlin/com/rodiz/arch2/core/featuredpets/data/FeaturedPetsSessionGateTest.kt`
- `feature/login/presentation/src/main/kotlin/com/rodiz/arch2/feature/login/presentation/viewmodel/LoginFeaturedPetsViewModel.kt`
- `feature/login/presentation/src/test/kotlin/com/rodiz/arch2/feature/login/presentation/LoginFeaturedPetsViewModelTest.kt`
- `feature/settings/presentation/src/main/kotlin/com/rodiz/arch2/feature/settings/presentation/FeaturedPetsScreen.kt`
- `feature/settings/presentation/src/main/kotlin/com/rodiz/arch2/feature/settings/presentation/FeaturedPetsRoute.kt`
- `feature/settings/presentation/src/main/kotlin/com/rodiz/arch2/feature/settings/presentation/FeaturedPetsViewModel.kt`
- `feature/settings/presentation/src/test/kotlin/com/rodiz/arch2/feature/settings/presentation/FeaturedPetsViewModelTest.kt`

### Modify
- `settings.gradle.kts` — include the two new `:core:featuredpets:*` modules.
- `app/build.gradle.kts` — `implementation(project(":core:featuredpets:data"))`.
- `app/src/main/kotlin/com/rodiz/arch2/App.kt` — inject `FeaturedPetsSessionGate`, fold into the existing session observer.
- `feature/login/presentation/build.gradle.kts` — `implementation(project(":core:featuredpets:domain"))`.
- `feature/login/presentation/src/main/kotlin/com/rodiz/arch2/feature/login/presentation/screen/LoginScreen.kt` — add `featured: List<FeaturedPet>` param, refactor `Hero` + `HeroAndCard` to use the slot composable.
- `feature/login/presentation/src/main/kotlin/com/rodiz/arch2/feature/login/presentation/screen/LoginRoute.kt` — inject `LoginFeaturedPetsViewModel`, pass `featured` down.
- `feature/pet/presentation/build.gradle.kts` — `implementation(project(":core:featuredpets:domain"))`.
- `feature/pet/presentation/src/main/kotlin/com/rodiz/arch2/feature/pet/presentation/PetThumbnailCard.kt` — add `isFeatured` / `onToggleFeature` / `showPin` params; pin overlay.
- `feature/pet/presentation/src/main/kotlin/com/rodiz/arch2/feature/pet/presentation/MyPetsViewModel.kt` — inject repo, expose `featuredIds`, add `onTogglePin`, hook `refreshFrom`.
- `feature/pet/presentation/src/main/kotlin/com/rodiz/arch2/feature/pet/presentation/MyPetsScreen.kt` — thread params; surface pin-limit snackbar.
- `feature/pet/presentation/src/test/kotlin/.../MyPetsViewModelTest.kt` — extend.
- `feature/settings/nav/src/main/kotlin/com/rodiz/arch2/feature/settings/nav/Routes.kt` — add `SettingsFeaturedPets`.
- `feature/settings/presentation/build.gradle.kts` — add `:core:featuredpets:domain` + `:feature:pet:domain`.
- `feature/settings/presentation/src/main/kotlin/com/rodiz/arch2/feature/settings/presentation/SettingsNavModule.kt` — register entry, wire `onOpenFeatured`.
- `feature/settings/presentation/src/main/kotlin/com/rodiz/arch2/feature/settings/presentation/SettingsHomeScreen.kt` — add the row.
- `feature/settings/presentation/src/main/kotlin/com/rodiz/arch2/feature/settings/presentation/SettingsHomeViewModel.kt` — expose `featuredCount`.
- String resources in `feature/pet/presentation/src/main/res/values/strings.xml` (pin a11y label, "max featured" snackbar) and `feature/settings/presentation/src/main/res/values/strings.xml` (row title/subtitle, screen title, banner, empty state).

### Delete
None. The decorative paw drawables (`pet_tile_*`) stay — they're the fallback art.

### Do NOT modify
- `core/session/data/.../SessionRepositoryImpl.kt`. Sign-out must preserve the cache; the gate handles wipe.
- `firestore.rules`. We deliberately keep pets read-gated by auth and cache locally instead.
- `feature/pet/domain/.../model/Pet.kt`. No `featured` field on the Pet model — featuring is purely client-side and avoids a Firestore schema migration.

## 11. Critical recipes

1. **AsyncImage swap requires all three fallbacks.** `AsyncImage` needs `placeholder`, `error`, AND `fallback` set explicitly. Without `fallback`, a null `model` (URL) renders nothing. Without `error`, a Coil load failure shows nothing. Without `placeholder`, the slot is blank during load.
2. **Single `dataStore.edit { }` per mutation.** Never two consecutive `edit` calls — opens a race window where another collector sees half-updated state. `wipe()` + last-user-id update should be one block.
3. **Module-scope `Json` instance.** Define once with `ignoreUnknownKeys = true`, `encodeDefaults = false`. Use `Json.encodeToString(ListSerializer(FeaturedPetCacheEntry.serializer()), entries)` for the write path. Wrap decode in `runCatching` → fallback to `emptyList()`.
4. **`distinctUntilChanged` already in App.kt's session observer.** Reuse it; don't add a parallel collector. One `onEach` calls both `crashReporter.setUserId` and `featuredPetsGate.onSessionChanged`.
5. **Pet → FeaturedPet mapping.** The remote URL comes from `pet.photos.firstOrNull()?.remoteUrl()` where `remoteUrl()` is a `(source as? PhotoSource.Remote)?.downloadUrl` cast. Local-only photos (e.g. mid-upload) map to `null` and the Login hero shows the decorative paw.
6. **Shared state, no draft.** Settings + My Pets both observe the same repo; toggles propagate live. No draft / commit-cancel UI on the Settings screen.

## 12. Verification

End-to-end on `emulator-5556`:

1. Fresh install. Login hero shows the original 3 paws.
2. Sign in as User A. Open My Pets, pin 2 pets. Sign out → Login hero shows those 2 real pets + 1 paw.
3. Force-stop the app, relaunch → still those 2 pets (cache survives process death).
4. Sign in as User A again → still 2 pinned (`SAME_USER` path).
5. Sign out → cache preserved. Sign in as User B → Login hero reverts to original 3 paws (`USER_CHANGED` triggered, cache wiped). Pin a different pet as User B → only User B's pet shows. Sign out → User B's pet still shown.
6. Sign in as A again → `USER_CHANGED` triggers again (B → A), cache wiped, hero back to paws. A has to re-pin.
7. From My Pets, attempt to pin a 4th pet → snackbar "You can feature up to 3 pets", count stays at 3.
8. Open Settings → Featured on login. Unpin a pet via the Settings toggle → return to My Pets, that pet's pin reflects unpinned (shared repo).
9. Externally delete a pinned pet in Firestore (via the Console). Next open of My Pets → `refreshFrom` runs → cache loses that id → Login hero now shows one fewer.
10. Airplane mode + relaunch → Login still renders cached pets if Coil disk cache has them; if it doesn't, paw fallback shows. No crash.
11. Take a screenshot for each of (0, 1, 2, 3) featured count and surface paths in the implementation final report.

## 13. Out of scope

- Firestore `featured` field on `pets/{id}` — deliberately avoided. Adding it requires a schema migration + rules update and breaks the "preserve across sign-out" property (Firestore is unreachable pre-auth).
- Cross-device sync of featured selection. Different device = different cache, fine.
- Reordering featured pets — pin order is "first pinned, first shown". A drag-to-reorder UI is future work.
- Showing the pet's name on the Login hero. Decorative-only for now.
- Animated tile entrance / exit.

## 14. Risk & rollback

- **Risk:** Coil disk cache evicted on low-storage devices → all-paws fallback. Mitigated by Coil's default ~2% free-space allocation being generous for 3 small images. If observed in QA, bump the `OkHttpClient` disk cache size or pre-warm via `ImageLoader.enqueue` post-pin.
- **Risk:** Settings + My Pets writing the same key concurrently. Mitigated because DataStore serializes writes per-instance and `edit { }` is atomic.
- **Risk:** Manual app-data clear with a session token still alive elsewhere → Login renders empty hero. Acceptable degraded state.
- **Rollback (UI only):** revert `LoginScreen.kt` + `LoginRoute.kt` to the original three `Image` calls; the cache + Settings + pin overlay remain dormant until a future revisit.
- **Rollback (full):** revert the App.kt gate hookup so the cache never accumulates; the unused code paths can stay or be removed in a follow-up.

## 15. Implementation order

0. Copy this plan to `plans/login-featured-pets.md`.
1. **Domain module skeleton** — create `:core:featuredpets:domain`, register in `settings.gradle.kts`, add the 4 files (`FeaturedPet`, `FeaturedPetsState`, `FeaturedPetsRepository`, `UserChangeResult` + `MAX_FEATURED_PETS`). Compile.
2. **Data module skeleton** — create `:core:featuredpets:data` with build file, empty manifest, `FeaturedPetCacheEntry`, `DataStoreFeaturedPetsRepository` stub (returns empty/no-ops), `di/FeaturedPetsModule.kt`, `FeaturedPetsSessionGate`. Compile.
3. **Repo impl + tests** — implement DataStore reads/writes, JSON serialization, user-change tracking. Write `DataStoreFeaturedPetsRepositoryTest` against a tempDir DataStore. Run.
4. **Session gate test** — write `FeaturedPetsSessionGateTest` against a fake repo. Run.
5. **App.kt integration** — inject gate, fold into the session observer. Manual smoke: sign in, force-stop, sign in different user, verify cache wipe via logcat.
6. **Login hero refactor (preview-only first)** — add `featured` param to `LoginScreen`, extract `HeroPetSlot`, wire the `Image` / `AsyncImage` choice. Update previews to cover 0 / 1 / 2 / 3 cases. No VM yet.
7. **`LoginFeaturedPetsViewModel`** — add VM + test. Wire into `LoginRoute`. Manual launch: hero still shows paws on fresh install.
8. **`PetThumbnailCard` pin overlay** — add params, preview both states. Don't wire the VM yet.
9. **`MyPetsViewModel` toggle + refresh** — inject repo, expose `featuredIds`, implement `onTogglePin`, hook `refreshFrom`. Update `MyPetsRoute` + `MyPetsScreen`. Update `MyPetsViewModelTest`. Manual: pin 2 pets, sign out, confirm they appear on Login.
10. **Settings nav + home row** — add `SettingsFeaturedPets` route, register in `SettingsNavModule`, add row in `SettingsHomeScreen`, expose `featuredCount` in `SettingsHomeViewModel`. Manual: row navigates; count pill correct.
11. **`FeaturedPetsViewModel` + screen** — implement combined-state VM, the screen UI + route. Wire `refreshFrom`. Test. Manual: toggle here, see My Pets pin update live and vice versa.
12. **Snackbar for pin-limit** — verify both surfaces show it on the 4th attempt.
13. **QA pass** — walk the verification checklist (§12) end-to-end on `emulator-5556`. Capture screenshots for 0/1/2/3 featured counts. Single local commit per [[feedback_post_plan_emulator]].

## 16. Critical files

- `feature/login/presentation/src/main/kotlin/com/rodiz/arch2/feature/login/presentation/screen/LoginScreen.kt` — hero refactor lives here (lines 79-206 for the Hero, line 126 for the rabbit tile in HeroAndCard).
- `feature/login/presentation/src/main/kotlin/com/rodiz/arch2/feature/login/presentation/screen/LoginRoute.kt` — adds the second `hiltViewModel()`.
- `feature/pet/presentation/src/main/kotlin/com/rodiz/arch2/feature/pet/presentation/PetThumbnailCard.kt` — pin overlay.
- `feature/pet/presentation/src/main/kotlin/com/rodiz/arch2/feature/pet/presentation/MyPetsViewModel.kt` — toggle + refresh.
- `app/src/main/kotlin/com/rodiz/arch2/App.kt` — session gate hookup (lines 72-78).
- `core/filters/data/src/main/kotlin/com/rodiz/arch2/core/filters/data/DataStoreFilterPrefsRepository.kt` — reference pattern for the new repo.
- `core/datastore/src/main/kotlin/com/rodiz/arch2/core/datastore/DataStoreModule.kt` — provides the shared `app_prefs` DataStore the new keys live in.
- `feature/settings/presentation/src/main/kotlin/com/rodiz/arch2/feature/settings/presentation/SettingsHomeScreen.kt` — where the new row goes.

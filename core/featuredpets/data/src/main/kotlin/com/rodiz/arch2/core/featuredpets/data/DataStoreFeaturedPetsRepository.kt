package com.rodiz.arch2.core.featuredpets.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.rodiz.arch2.core.featuredpets.domain.FeaturedPet
import com.rodiz.arch2.core.featuredpets.domain.FeaturedPetsRepository
import com.rodiz.arch2.core.featuredpets.domain.FeaturedPetsState
import com.rodiz.arch2.core.featuredpets.domain.MAX_FEATURED_PETS
import com.rodiz.arch2.core.featuredpets.domain.UserChangeResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DataStoreFeaturedPetsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : FeaturedPetsRepository {

    override fun observe(): Flow<FeaturedPetsState> = dataStore.data.map { prefs ->
        FeaturedPetsState(featured = prefs.readEntries().toDomain())
    }

    override suspend fun current(): FeaturedPetsState =
        FeaturedPetsState(featured = dataStore.data.first().readEntries().toDomain())

    override suspend fun pin(pet: FeaturedPet): Boolean {
        var accepted = false
        dataStore.edit { editor ->
            val existing = editor.readEntries()
            if (existing.any { it.id == pet.id }) {
                // Already pinned — treat as success so the UI doesn't show a
                // misleading "limit reached" error on a no-op double-tap.
                accepted = true
                return@edit
            }
            if (existing.size >= MAX_FEATURED_PETS) return@edit
            editor.writeEntries(existing + pet.toEntry())
            accepted = true
        }
        return accepted
    }

    override suspend fun unpin(petId: String) {
        dataStore.edit { editor ->
            val existing = editor.readEntries()
            val filtered = existing.filterNot { it.id == petId }
            if (filtered.size != existing.size) editor.writeEntries(filtered)
        }
    }

    override suspend fun refreshFrom(authoritative: Map<String, FeaturedPet>) {
        dataStore.edit { editor ->
            val existing = editor.readEntries()
            // Preserve order; drop cache entries whose id is no longer in the
            // authoritative set; refresh in-place for ids that ARE present.
            val refreshed = existing.mapNotNull { entry ->
                authoritative[entry.id]?.toEntry()
            }
            if (refreshed != existing) editor.writeEntries(refreshed)
        }
    }

    override suspend fun wipe() {
        // Only clear the featured set + schema version; preserve
        // featured_pets_last_user_id so onUserActive can keep distinguishing
        // SAME_USER from USER_CHANGED on future sign-ins.
        dataStore.edit { editor ->
            editor.remove(KEY_FEATURED_JSON)
            editor[KEY_SCHEMA_VERSION] = CURRENT_SCHEMA_VERSION
        }
    }

    override suspend fun onUserActive(userId: String): UserChangeResult {
        var result: UserChangeResult = UserChangeResult.SAME_USER
        dataStore.edit { editor ->
            val previous = editor[KEY_LAST_USER_ID]
            result = when (previous) {
                null -> UserChangeResult.FIRST_USER
                userId -> UserChangeResult.SAME_USER
                else -> UserChangeResult.USER_CHANGED
            }
            editor[KEY_LAST_USER_ID] = userId
        }
        return result
    }

    // --- helpers ---

    private fun Preferences.readEntries(): List<FeaturedPetCacheEntry> {
        // Forward-compat: a future install that wrote a higher schema version
        // is treated as empty rather than crashing the current app version.
        val version = this[KEY_SCHEMA_VERSION] ?: CURRENT_SCHEMA_VERSION
        if (version > CURRENT_SCHEMA_VERSION) return emptyList()
        val raw = this[KEY_FEATURED_JSON] ?: return emptyList()
        return runCatching { FeaturedPetsJson.decodeFromString(EntryListSerializer, raw) }
            .getOrElse { emptyList() }
    }

    private fun androidx.datastore.preferences.core.MutablePreferences.writeEntries(
        entries: List<FeaturedPetCacheEntry>,
    ) {
        this[KEY_FEATURED_JSON] = FeaturedPetsJson.encodeToString(EntryListSerializer, entries)
        this[KEY_SCHEMA_VERSION] = CURRENT_SCHEMA_VERSION
    }

    private fun List<FeaturedPetCacheEntry>.toDomain(): List<FeaturedPet> =
        map { FeaturedPet(it.id, it.name, it.species, it.avatarUrl) }

    private fun FeaturedPet.toEntry(): FeaturedPetCacheEntry =
        FeaturedPetCacheEntry(id, name, species, avatarUrl)

    private companion object {
        const val CURRENT_SCHEMA_VERSION = 1
        val KEY_FEATURED_JSON = stringPreferencesKey("featured_pets_json")
        val KEY_LAST_USER_ID = stringPreferencesKey("featured_pets_last_user_id")
        val KEY_SCHEMA_VERSION = intPreferencesKey("featured_pets_schema_version")

        val FeaturedPetsJson = Json {
            ignoreUnknownKeys = true
            encodeDefaults = false
        }

        val EntryListSerializer = ListSerializer(FeaturedPetCacheEntry.serializer())
    }
}

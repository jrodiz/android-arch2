package com.rodiz.arch2.core.filters.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.rodiz.arch2.core.filters.domain.FilterPrefs
import com.rodiz.arch2.core.filters.domain.FilterPrefsRepository
import com.rodiz.arch2.feature.pet.domain.model.Intent
import com.rodiz.arch2.feature.pet.domain.model.Species
import com.rodiz.arch2.feature.pet.domain.model.SpeciesCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DataStoreFilterPrefsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : FilterPrefsRepository {

    override fun observePrefs(): Flow<FilterPrefs> = dataStore.data.map { prefs ->
        FilterPrefs(
            maxDistanceKm = prefs[KEY_MAX_DISTANCE_KM] ?: FilterPrefs.DEFAULT.maxDistanceKm,
            intents = prefs[KEY_INTENTS]?.mapNotNullTo(mutableSetOf()) { name ->
                Intent.entries.firstOrNull { it.name == name }
            }?.takeIf { it.isNotEmpty() } ?: FilterPrefs.DEFAULT.intents,
            species = prefs[KEY_SPECIES]?.let(::parseSpeciesSet)
                ?.takeIf { it.isNotEmpty() }
                ?: FilterPrefs.DEFAULT.species,
        )
    }

    override suspend fun updatePrefs(prefs: FilterPrefs) {
        dataStore.edit { editor ->
            editor[KEY_MAX_DISTANCE_KM] = prefs.maxDistanceKm
            editor[KEY_INTENTS] = prefs.intents.mapTo(mutableSetOf()) { it.name }
            editor[KEY_SPECIES] = prefs.species.mapTo(mutableSetOf()) { it.name }
        }
    }

    /**
     * Stored species names may use either the current [Species] enum (DOG, CAT, RABBIT, …)
     * or the legacy [SpeciesCategory] enum (DOGS, CATS, SMALL_MAMMALS) from prefs written
     * before the per-species widening landed. Resolve each name against [Species] first;
     * any leftover that matches a legacy category is expanded into the full set of species
     * that category contained.
     */
    private fun parseSpeciesSet(names: Set<String>): Set<Species> {
        val result = mutableSetOf<Species>()
        for (name in names) {
            val direct = Species.entries.firstOrNull { it.name == name }
            if (direct != null) {
                result += direct
                continue
            }
            val legacy = SpeciesCategory.entries.firstOrNull { it.name == name }
            if (legacy != null) {
                Species.entries.filterTo(result) { it.category == legacy }
            }
        }
        return result
    }

    private companion object {
        val KEY_MAX_DISTANCE_KM = intPreferencesKey("filters_max_distance_km")
        val KEY_INTENTS = stringSetPreferencesKey("filters_intents")
        val KEY_SPECIES = stringSetPreferencesKey("filters_species")
    }
}

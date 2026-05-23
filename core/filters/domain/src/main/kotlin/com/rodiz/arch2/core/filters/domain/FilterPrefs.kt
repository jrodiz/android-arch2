package com.rodiz.arch2.core.filters.domain

import com.rodiz.arch2.feature.pet.domain.model.Intent
import com.rodiz.arch2.feature.pet.domain.model.Species

data class FilterPrefs(
    val maxDistanceKm: Int,
    val intents: Set<Intent>,
    /**
     * Species the user wants to see in the deck. Granular at the [Species] level
     * (DOG, CAT, RABBIT, …) so the Filters UI can offer per-species pills like the
     * mock instead of the coarser DOGS/CATS/SMALL_MAMMALS categories. Earlier
     * persisted prefs that used [com.rodiz.arch2.feature.pet.domain.model.SpeciesCategory]
     * keys are migrated on read (see DataStoreFilterPrefsRepository).
     */
    val species: Set<Species>,
) {
    companion object {
        val DEFAULT = FilterPrefs(
            maxDistanceKm = 25,
            intents = Intent.entries.toSet(),
            species = Species.entries.toSet(),
        )
    }
}

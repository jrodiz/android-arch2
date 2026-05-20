package com.rodiz.arch2.feature.deck.domain.model

import com.rodiz.arch2.feature.pet.domain.model.Intent
import com.rodiz.arch2.feature.pet.domain.model.SpeciesCategory

data class FilterPrefs(
    val maxDistanceKm: Int,
    val intents: Set<Intent>,
    val speciesCategories: Set<SpeciesCategory>,
) {
    companion object {
        val DEFAULT = FilterPrefs(
            maxDistanceKm = 25,
            intents = Intent.entries.toSet(),
            speciesCategories = SpeciesCategory.entries.toSet(),
        )
    }
}

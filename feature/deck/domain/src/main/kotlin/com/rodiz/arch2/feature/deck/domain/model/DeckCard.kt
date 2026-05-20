package com.rodiz.arch2.feature.deck.domain.model

import com.rodiz.arch2.feature.pet.domain.model.Pet

data class DeckCard(
    val pet: Pet,
    val distanceBucket: DistanceBucket? = null,  // null when owner location is missing
)

package com.rodiz.arch2.feature.deck.domain.model

import com.rodiz.arch2.core.ownerlookup.domain.OwnerDisplay
import com.rodiz.arch2.feature.pet.domain.model.Pet

data class DeckCard(
    val pet: Pet,
    val owner: OwnerDisplay? = null,             // null while the owner doc hasn't loaded yet
    val distanceBucket: DistanceBucket? = null,  // null when owner location is missing
)

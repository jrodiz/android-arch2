package com.rodiz.arch2.feature.deck.domain.repository

import com.rodiz.arch2.core.filters.domain.FilterPrefs
import com.rodiz.arch2.feature.deck.domain.model.DeckSnapshot
import com.rodiz.arch2.feature.deck.domain.model.SwipeAction
import com.rodiz.arch2.feature.deck.domain.model.SwipeResult
import com.rodiz.arch2.feature.pet.domain.model.Pet
import com.rodiz.arch2.feature.pet.domain.model.PetId
import kotlinx.coroutines.flow.Flow

interface DeckRepository {
    /** Continuous stream of cards the current user can swipe on, filtered by [filters]. */
    fun observeDeck(filters: FilterPrefs): Flow<DeckSnapshot>

    /** Persist a swipe. On LIKE, checks reciprocity and atomically creates a match if mutual. */
    suspend fun submitSwipe(petId: PetId, action: SwipeAction): SwipeResult

    /** Undo the most recent swipe within a short grace window. Returns the un-swiped pet, or null. */
    suspend fun undoLastSwipe(): Pet?

    /**
     * Bulk-delete every pass the current user made since local midnight. Returns the count of
     * deleted docs so callers can surface a "Restored N pets" confirmation. Powers the
     * "Review who you passed" action on the empty-deck state.
     */
    suspend fun clearTodayPasses(): Int
}

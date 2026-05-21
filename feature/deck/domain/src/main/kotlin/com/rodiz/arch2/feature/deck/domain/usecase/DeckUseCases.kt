package com.rodiz.arch2.feature.deck.domain.usecase

import com.rodiz.arch2.feature.deck.domain.model.DeckSnapshot
import com.rodiz.arch2.core.filters.domain.FilterPrefs
import com.rodiz.arch2.feature.deck.domain.model.SwipeAction
import com.rodiz.arch2.feature.deck.domain.model.SwipeResult
import com.rodiz.arch2.feature.deck.domain.repository.DeckRepository
import com.rodiz.arch2.feature.pet.domain.model.Pet
import com.rodiz.arch2.feature.pet.domain.model.PetId
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveDeckUseCase @Inject constructor(
    private val repo: DeckRepository,
) {
    operator fun invoke(filters: FilterPrefs = FilterPrefs.DEFAULT): Flow<DeckSnapshot> =
        repo.observeDeck(filters)
}

class SubmitSwipeUseCase @Inject constructor(
    private val repo: DeckRepository,
) {
    suspend operator fun invoke(petId: PetId, action: SwipeAction): SwipeResult =
        repo.submitSwipe(petId, action)
}

class UndoLastSwipeUseCase @Inject constructor(
    private val repo: DeckRepository,
) {
    suspend operator fun invoke(): Pet? = repo.undoLastSwipe()
}

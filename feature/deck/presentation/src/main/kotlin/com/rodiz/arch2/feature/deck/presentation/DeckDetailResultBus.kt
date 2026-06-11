package com.rodiz.arch2.feature.deck.presentation

import com.rodiz.arch2.feature.deck.domain.model.SwipeResult
import com.rodiz.arch2.feature.pet.domain.model.PetId
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

/**
 * Bridges a swipe performed on the [DeckPetDetailViewModel] (a per-detail instance pushed
 * on top of the back stack) back to the retained [DeckViewModel] underneath it.
 *
 * The deck snapshot is driven by `observeAllActivePets`, which does NOT react to pass/like
 * writes — so without this bridge a pass from the detail would leave the pet on top of the
 * deck, and a like with no pet would surface no "add a pet" dialog. Scoped to the
 * ActivityRetained component so the detail and deck ViewModels share a single instance.
 */
@ActivityRetainedScoped
class DeckDetailResultBus @Inject constructor() {

    data class Outcome(val petId: PetId, val result: SwipeResult)

    private val _outcomes = MutableSharedFlow<Outcome>(extraBufferCapacity = 4)
    val outcomes: SharedFlow<Outcome> = _outcomes.asSharedFlow()

    fun publish(petId: PetId, result: SwipeResult) {
        _outcomes.tryEmit(Outcome(petId, result))
    }
}

package com.rodiz.arch2.feature.deck.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodiz.arch2.core.ownerlookup.domain.OwnerDisplay
import com.rodiz.arch2.core.ownerlookup.domain.OwnerLookupRepository
import com.rodiz.arch2.feature.deck.domain.model.SwipeAction
import com.rodiz.arch2.feature.deck.domain.model.SwipeResult
import com.rodiz.arch2.feature.deck.domain.usecase.SubmitSwipeUseCase
import com.rodiz.arch2.feature.pet.domain.model.Pet
import com.rodiz.arch2.feature.pet.domain.model.PetId
import com.rodiz.arch2.feature.pet.domain.usecase.ObservePetUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class DeckPetDetailUiState(
    val pet: Pet? = null,
    val owner: OwnerDisplay? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

/**
 * Powers the suitor-side pet detail bottom-sheet (see `plans/deck-pet-details.md`).
 *
 * Reuses [SubmitSwipeUseCase] from `:feature:deck:domain` so liking / passing from the
 * detail produces the same downstream effects (match creation) as liking from the card
 * itself. The deck snapshot is NOT reactive to swipe writes, so we hand the swipe result
 * to the retained [DeckViewModel] through [DeckDetailResultBus] before popping — that's
 * what makes the deck advance past the pet, surface the "add a pet" dialog, or fire the
 * match celebration after a swipe performed here.
 */
internal class DeckPetDetailViewModel @AssistedInject constructor(
    @Assisted petIdValue: String,
    observePet: ObservePetUseCase,
    ownerLookup: OwnerLookupRepository,
    private val submitSwipe: SubmitSwipeUseCase,
    private val resultBus: DeckDetailResultBus,
) : ViewModel() {

    private val petId: PetId = PetId(petIdValue)

    @AssistedFactory
    interface Factory {
        fun create(petIdValue: String): DeckPetDetailViewModel
    }

    private val _uiState = MutableStateFlow(DeckPetDetailUiState())
    val uiState: StateFlow<DeckPetDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<DeckPetDetailEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<DeckPetDetailEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            observePet(petId)
                .flatMapLatest { pet ->
                    if (pet == null) {
                        flowOf(DeckPetDetailUiState(pet = null, owner = null, isLoading = false))
                    } else {
                        ownerLookup.observe(pet.ownerId)
                            .catch { emit(null) }
                            .map { owner ->
                                DeckPetDetailUiState(
                                    pet = pet,
                                    owner = owner,
                                    isLoading = false,
                                )
                            }
                    }
                }
                .catch { e ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = e.message)
                    }
                }
                .collect { state -> _uiState.value = state }
        }
    }

    fun like() = swipe(SwipeAction.LIKE)
    fun pass() = swipe(SwipeAction.PASS)

    private fun swipe(action: SwipeAction) {
        viewModelScope.launch {
            // Hand the outcome to the deck so it can advance past this pet or show the
            // add-a-pet dialog (rejected like). On error we just dismiss; the deck's
            // snackbars own the "swipe failed" feedback.
            val result = runCatching { submitSwipe(petId, action) }.getOrNull()
            if (result != null) resultBus.publish(petId, result)
            // A match navigates this route straight to the celebration (which pops the
            // detail first). This is what makes the celebration fire from the Likes-You
            // detail too, not just the deck — both reuse this same route. Anything else
            // just dismisses back to wherever the detail was opened from.
            val event = (result as? SwipeResult.Match)
                ?.let { DeckPetDetailEvent.MatchOccurred(it.matchId) }
                ?: DeckPetDetailEvent.Dismiss
            _events.tryEmit(event)
        }
    }
}

internal sealed interface DeckPetDetailEvent {
    /** Tells the route to pop back to wherever the detail was opened from. */
    data object Dismiss : DeckPetDetailEvent

    /** A like from this detail produced a match — pop the detail and show the celebration. */
    data class MatchOccurred(val matchId: String) : DeckPetDetailEvent
}

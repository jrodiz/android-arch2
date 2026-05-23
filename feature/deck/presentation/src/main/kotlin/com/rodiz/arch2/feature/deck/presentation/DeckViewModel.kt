package com.rodiz.arch2.feature.deck.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodiz.arch2.core.filters.domain.FilterPrefsRepository
import com.rodiz.arch2.feature.deck.domain.model.DeckCard
import com.rodiz.arch2.feature.deck.domain.model.DeckState
import com.rodiz.arch2.feature.deck.domain.model.SwipeAction
import com.rodiz.arch2.feature.deck.domain.model.SwipeResult
import com.rodiz.arch2.feature.deck.domain.usecase.ObserveDeckUseCase
import com.rodiz.arch2.feature.deck.domain.usecase.SubmitSwipeUseCase
import com.rodiz.arch2.feature.deck.domain.usecase.UndoLastSwipeUseCase
import com.rodiz.arch2.feature.pet.domain.model.Intent
import com.rodiz.arch2.feature.pet.domain.model.PetId
import com.rodiz.arch2.feature.pet.domain.model.PetState
import com.rodiz.arch2.feature.pet.domain.model.SpeciesCategory
import com.rodiz.arch2.feature.pet.domain.usecase.ObserveMyPetsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class DeckUiState(
    val cards: List<DeckCard> = emptyList(),
    // Initial state is EXHAUSTED so the empty state renders immediately rather than
    // a spinner-forever if the snapshot listener takes a while to fire (e.g. cold cache
    // with no network round-trip yet). When the listener does fire, state moves to READY.
    val state: DeckState = DeckState.EXHAUSTED,
    // Default true so we don't flash the "add a pet" banner before the My Pets
    // snapshot has fired. Becomes false as soon as we know the user has zero active pets.
    val hasOwnPet: Boolean = true,
    // Filter prefs surfaced to the header meta row. Default to the canonical defaults
    // so the strip reads sanely on cold start before the prefs flow has emitted.
    val maxDistanceKm: Int = 25,
    val intentsCount: Int = Intent.entries.size,
    val speciesCount: Int = SpeciesCategory.entries.size,
    val matchMessage: String? = null,
    val requiresPetMessage: String? = null,
    val errorMessage: String? = null,
)

@HiltViewModel
internal class DeckViewModel @Inject constructor(
    observeDeck: ObserveDeckUseCase,
    observeMyPets: ObserveMyPetsUseCase,
    filterPrefsRepo: FilterPrefsRepository,
    private val submitSwipe: SubmitSwipeUseCase,
    private val undoLastSwipe: UndoLastSwipeUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeckUiState())
    val uiState: StateFlow<DeckUiState> = _uiState.asStateFlow()

    // Pets the user just swiped on this session — filter them out of incoming snapshots
    // so the UI doesn't briefly show a card that's about to disappear.
    private val recentlySwiped = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            filterPrefsRepo.observePrefs()
                .flatMapLatest { prefs -> observeDeck(prefs) }
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                .collect { snapshot ->
                    val filtered = snapshot.cards.filterNot { it.pet.id.value in recentlySwiped }
                    _uiState.update {
                        it.copy(
                            cards = filtered,
                            state = if (filtered.isEmpty() && snapshot.state == DeckState.READY) {
                                DeckState.EXHAUSTED
                            } else {
                                snapshot.state
                            },
                        )
                    }
                }
        }
        // Mirror the scalar filter values into UiState so the header meta strip
        // (radius / intents / species count) reflects what the user has configured.
        // Hot-shares the same flow as observeDeck above; no extra reads.
        viewModelScope.launch {
            filterPrefsRepo.observePrefs()
                .catch { /* swallow — meta strip falls back to UiState defaults */ }
                .collect { prefs ->
                    _uiState.update {
                        it.copy(
                            maxDistanceKm = prefs.maxDistanceKm,
                            intentsCount = prefs.intents.size,
                            // Derive a category count for the header's paw-icon strip so
                            // it stays at 1–3 visual paws even after the underlying species
                            // set widened to 7 granular options. The visual is "how broad
                            // is your filter," not "exact count."
                            speciesCount = prefs.species.map { it.category }.toSet().size,
                        )
                    }
                }
        }
        // Reactive "do I have a pet?" — drives the persistent Add-a-pet banner above
        // the deck. Also catches the case where the user adds a pet via the banner CTA
        // and comes back: the banner disappears as soon as the My Pets snapshot updates.
        viewModelScope.launch {
            observeMyPets(filter = setOf(PetState.ACTIVE))
                .catch { /* swallow — banner stays in its default-true state */ }
                .collect { activePets ->
                    _uiState.update { it.copy(hasOwnPet = activePets.isNotEmpty()) }
                }
        }
    }

    fun likeTop() {
        val top = _uiState.value.cards.firstOrNull() ?: return
        swipe(top.pet.id, SwipeAction.LIKE)
    }

    fun passTop() {
        val top = _uiState.value.cards.firstOrNull() ?: return
        swipe(top.pet.id, SwipeAction.PASS)
    }

    fun rewind() {
        viewModelScope.launch {
            val pet = runCatching { undoLastSwipe() }.getOrNull() ?: return@launch
            recentlySwiped.remove(pet.id.value)
            _uiState.update { it.copy(cards = listOf(DeckCard(pet)) + it.cards) }
        }
    }

    fun clearMatch() = _uiState.update { it.copy(matchMessage = null) }
    fun clearRequiresPet() = _uiState.update { it.copy(requiresPetMessage = null) }
    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    private fun swipe(petId: PetId, action: SwipeAction) {
        // Optimistic remove of the top card.
        recentlySwiped.add(petId.value)
        _uiState.update { it.copy(cards = it.cards.drop(1)) }
        viewModelScope.launch {
            val result = runCatching { submitSwipe(petId, action) }
                .getOrElse { e ->
                    _uiState.update { it.copy(errorMessage = e.message ?: "Swipe failed") }
                    return@launch
                }
            when (result) {
                SwipeResult.Pending -> Unit
                SwipeResult.RequiresPet -> _uiState.update {
                    it.copy(requiresPetMessage = "Add a pet to start matching")
                }
                is SwipeResult.Match -> _uiState.update {
                    it.copy(matchMessage = "It's a match!")
                }
            }
        }
    }
}

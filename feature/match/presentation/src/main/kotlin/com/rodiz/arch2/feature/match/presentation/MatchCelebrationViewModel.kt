package com.rodiz.arch2.feature.match.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodiz.arch2.core.ownerlookup.domain.OwnerDisplay
import com.rodiz.arch2.core.ownerlookup.domain.OwnerLookupRepository
import com.rodiz.arch2.core.petlookup.domain.PetDisplay
import com.rodiz.arch2.core.petlookup.domain.PetLookupRepository
import com.rodiz.arch2.core.session.domain.SessionRepository
import com.rodiz.arch2.feature.match.domain.model.Match
import com.rodiz.arch2.feature.match.domain.model.MatchId
import com.rodiz.arch2.feature.match.domain.usecase.ObserveMatchUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Snapshot for the "It's a match!" celebration screen.
 *
 * Owners are not blocking — the screen renders the pet tiles + headline as
 * soon as the two pets resolve, then fades owner chips in when their lookups
 * arrive. [isReady] gates "show real content vs. skeleton".
 */
internal data class MatchCelebrationUiState(
    val match: Match? = null,
    val myPet: PetDisplay? = null,
    val theirPet: PetDisplay? = null,
    val me: OwnerDisplay? = null,
    val them: OwnerDisplay? = null,
    val errorMessage: String? = null,
) {
    val isReady: Boolean = match != null && myPet != null && theirPet != null
}

internal class MatchCelebrationViewModel @AssistedInject constructor(
    @Assisted matchIdValue: String,
    private val sessionRepo: SessionRepository,
    private val observeMatch: ObserveMatchUseCase,
    private val petLookup: PetLookupRepository,
    private val ownerLookup: OwnerLookupRepository,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(matchIdValue: String): MatchCelebrationViewModel
    }

    private val matchId: MatchId = MatchId(matchIdValue)

    private val _uiState = MutableStateFlow(MatchCelebrationUiState())
    val uiState: StateFlow<MatchCelebrationUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val me = sessionRepo.current()?.userId
            if (me == null) {
                _uiState.update { it.copy(errorMessage = "Sign-in required") }
                return@launch
            }
            // observeMatch resolves first; the four lookups are launched off the
            // first non-null match emission. Keeping them as separate collectors
            // (vs. a single combine of 5 hot Flows) makes the partial-loading
            // story easier to read: each collector only ever writes its own
            // slice of UiState.
            launch {
                observeMatch(matchId)
                    .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                    .collect { match ->
                        _uiState.update { it.copy(match = match) }
                        if (match == null) return@collect
                        val myPetId = match.myPetId(me)
                        val theirPetId = match.otherPetId(me)
                        launchPetCollector(myPetId, isMine = true)
                        launchPetCollector(theirPetId, isMine = false)
                        launchOwnerCollector(me, isMine = true)
                        launchOwnerCollector(match.otherOwnerId(me), isMine = false)
                    }
            }
        }
    }

    private fun launchPetCollector(petId: String?, isMine: Boolean) {
        viewModelScope.launch {
            val source = if (petId != null) petLookup.observe(petId) else flowOf(null)
            source
                .catch { /* keep the tile in its fallback state */ }
                .collect { pet ->
                    _uiState.update {
                        if (isMine) it.copy(myPet = pet) else it.copy(theirPet = pet)
                    }
                }
        }
    }

    private fun launchOwnerCollector(ownerId: String, isMine: Boolean) {
        viewModelScope.launch {
            ownerLookup.observe(ownerId)
                .catch { /* owner chip stays in its fallback state */ }
                .collect { owner ->
                    _uiState.update {
                        if (isMine) it.copy(me = owner) else it.copy(them = owner)
                    }
                }
        }
    }
}

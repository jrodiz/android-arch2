package com.rodiz.arch2.feature.likes.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodiz.arch2.feature.deck.domain.model.SwipeResult
import com.rodiz.arch2.feature.likes.domain.model.IncomingLike
import com.rodiz.arch2.feature.likes.domain.model.LikeKey
import com.rodiz.arch2.feature.likes.domain.usecase.LikeBackUseCase
import com.rodiz.arch2.feature.likes.domain.usecase.ObserveLikesYouUseCase
import com.rodiz.arch2.feature.likes.domain.usecase.PassLikeUseCase
import com.rodiz.arch2.feature.pet.domain.model.Intent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Filter applied to the Likes-you grid. View-side only — the data layer always returns
 * the full unfiltered list.
 */
internal sealed interface LikesFilter {
    data object All : LikesFilter
    data class ByIntent(val intent: Intent) : LikesFilter
}

internal data class LikesYouUiState(
    val likes: List<IncomingLike> = emptyList(),
    val expandedKey: LikeKey? = null, // unused by the redesigned UI; kept to preserve VM API
    val matchMessage: String? = null,
    val errorMessage: String? = null,
    val isLoading: Boolean = true,
    val activeFilter: LikesFilter = LikesFilter.All,
    /**
     * Keys the user has already opened/seen during this session. Drives the "NEW" pill.
     * In-memory only — resets on process death; persistence is a future concern (see
     * [`plans/likes-you-redesign-grid.md`] decision D2).
     */
    val seenKeys: Set<String> = emptySet(),
)

@HiltViewModel
internal class LikesYouViewModel @Inject constructor(
    observeLikesYou: ObserveLikesYouUseCase,
    private val passLike: PassLikeUseCase,
    private val likeBack: LikeBackUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LikesYouUiState())
    val uiState: StateFlow<LikesYouUiState> = _uiState.asStateFlow()

    private val locallyHidden = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            observeLikesYou()
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                }
                .collect { incoming ->
                    val visible = incoming.filterNot { it.key.value in locallyHidden }
                    _uiState.update { it.copy(isLoading = false, likes = visible) }
                }
        }
    }

    fun onFilterSelected(filter: LikesFilter) {
        _uiState.update { it.copy(activeFilter = filter) }
    }

    /** Mark a like as seen so its "NEW" pill clears (in-memory only). */
    fun markSeen(key: LikeKey) {
        _uiState.update { it.copy(seenKeys = it.seenKeys + key.value) }
    }

    fun onCardTap(key: LikeKey) = _uiState.update { it.copy(expandedKey = key) }
    fun dismissSheet() = _uiState.update { it.copy(expandedKey = null) }
    fun clearMatch() = _uiState.update { it.copy(matchMessage = null) }
    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    fun likeBackExpanded() {
        val key = _uiState.value.expandedKey ?: return
        hideOptimistic(key)
        viewModelScope.launch {
            runCatching { likeBack(key) }
                .onSuccess { result ->
                    val msg = when (result) {
                        is SwipeResult.Match -> "It's a match!"
                        else -> null
                    }
                    _uiState.update { it.copy(expandedKey = null, matchMessage = msg) }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(expandedKey = null, errorMessage = e.message ?: "Couldn't like back")
                    }
                }
        }
    }

    fun passExpanded() {
        val key = _uiState.value.expandedKey ?: return
        pass(key)
        _uiState.update { it.copy(expandedKey = null) }
    }

    /**
     * Decline an incoming like straight from the grid card. Writes a `passedLikes` doc so the
     * tile stays gone (the data layer filters on it), plus an optimistic local hide. This is
     * the only way to decline a like — tapping a tile opens the shared pet detail whose "Pass"
     * is a *deck* pass and never touched the incoming like. [D-007]
     */
    fun pass(key: LikeKey) {
        hideOptimistic(key)
        viewModelScope.launch {
            runCatching { passLike(key) }
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.message ?: "Couldn't pass") }
                }
        }
    }

    private fun hideOptimistic(key: LikeKey) {
        locallyHidden.add(key.value)
        _uiState.update { it.copy(likes = it.likes.filterNot { l -> l.key == key }) }
    }
}

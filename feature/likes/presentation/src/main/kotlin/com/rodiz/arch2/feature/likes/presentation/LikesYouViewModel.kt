package com.rodiz.arch2.feature.likes.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodiz.arch2.feature.deck.domain.model.SwipeResult
import com.rodiz.arch2.feature.likes.domain.model.IncomingLike
import com.rodiz.arch2.feature.likes.domain.model.LikeKey
import com.rodiz.arch2.feature.likes.domain.usecase.LikeBackUseCase
import com.rodiz.arch2.feature.likes.domain.usecase.ObserveLikesYouUseCase
import com.rodiz.arch2.feature.likes.domain.usecase.PassLikeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class LikesYouUiState(
    val likes: List<IncomingLike> = emptyList(),
    val expandedKey: LikeKey? = null,
    val matchMessage: String? = null,
    val errorMessage: String? = null,
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
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                .collect { incoming ->
                    val visible = incoming.filterNot { it.key.value in locallyHidden }
                    _uiState.update { it.copy(likes = visible) }
                }
        }
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
        hideOptimistic(key)
        viewModelScope.launch {
            runCatching { passLike(key) }
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.message ?: "Couldn't pass") }
                }
        }
        _uiState.update { it.copy(expandedKey = null) }
    }

    private fun hideOptimistic(key: LikeKey) {
        locallyHidden.add(key.value)
        _uiState.update { it.copy(likes = it.likes.filterNot { l -> l.key == key }) }
    }
}

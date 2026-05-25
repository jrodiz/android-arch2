package com.rodiz.arch2.feature.match.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodiz.arch2.core.session.domain.SessionRepository
import com.rodiz.arch2.feature.match.domain.model.InboxSnapshot
import com.rodiz.arch2.feature.match.domain.usecase.ObserveInboxUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class InboxUiState(
    val snapshot: InboxSnapshot = InboxSnapshot(emptyList(), emptyList()),
    /**
     * False until the first snapshot lands (or until we know there's no signed-in
     * user to query). The screen distinguishes this from `isReady && snapshot
     * empty` so a cold start no longer flashes "No matches yet" before the
     * snapshot listener has had a chance to fire.
     */
    val isReady: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
internal class InboxViewModel @Inject constructor(
    observeInbox: ObserveInboxUseCase,
    private val sessionRepo: SessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InboxUiState())
    val uiState: StateFlow<InboxUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val uid = sessionRepo.current()?.userId
            if (uid == null) {
                // No signed-in user — flip ready so the empty state renders instead
                // of hanging forever on the loading spinner.
                _uiState.update { it.copy(isReady = true) }
                return@launch
            }
            observeInbox(uid)
                .catch { e ->
                    _uiState.update { it.copy(isReady = true, errorMessage = e.message) }
                }
                .collect { snap ->
                    _uiState.update { it.copy(isReady = true, snapshot = snap) }
                }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

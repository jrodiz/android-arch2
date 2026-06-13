package com.rodiz.arch2.feature.match.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodiz.arch2.core.session.domain.SessionRepository
import com.rodiz.arch2.feature.match.domain.model.InboxSnapshot
import com.rodiz.arch2.feature.match.domain.usecase.ObserveInboxUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
internal class InboxViewModel @Inject constructor(
    observeInbox: ObserveInboxUseCase,
    private val sessionRepo: SessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InboxUiState())
    val uiState: StateFlow<InboxUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Observe the session reactively so an in-app account switch (sign out →
            // sign in as a different user, no process restart) re-keys the inbox to the
            // new uid. A one-shot `current()` read left this VM bound to the previous
            // user — leaking their conversations + PERMISSION_DENIED on their docs. [D-010]
            sessionRepo.observe()
                .flatMapLatest { session ->
                    val uid = session?.userId
                    // No signed-in user → emit an empty snapshot (also clears any stale
                    // data from the previous session instead of leaving it on screen).
                    if (uid == null) flowOf(InboxSnapshot(emptyList(), emptyList())) else observeInbox(uid)
                }
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

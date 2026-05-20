package com.rodiz.arch2.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodiz.arch2.core.session.domain.SessionRepository
import com.rodiz.arch2.feature.settings.domain.model.AccountDeletion
import com.rodiz.arch2.feature.settings.domain.usecase.CancelAccountDeletionUseCase
import com.rodiz.arch2.feature.settings.domain.usecase.ObservePendingDeletionUseCase
import com.rodiz.arch2.feature.settings.domain.usecase.RequestAccountDeletionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject

internal data class AccountUiState(
    val email: String = "",
    val pendingDeletion: AccountDeletion? = null,
    val daysRemaining: Long = 0,
    val errorMessage: String? = null,
    val completedDeletion: Boolean = false,
)

@HiltViewModel
internal class AccountViewModel @Inject constructor(
    observePendingDeletion: ObservePendingDeletionUseCase,
    private val requestDeletionUseCase: RequestAccountDeletionUseCase,
    private val cancelDeletionUseCase: CancelAccountDeletionUseCase,
    private val sessionRepo: SessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Session display name isn't email, but we don't have email exposed yet — use userId as a stand-in.
            val session = sessionRepo.current()
            _uiState.update { it.copy(email = session?.displayName ?: session?.userId.orEmpty()) }
        }
        viewModelScope.launch {
            observePendingDeletion()
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                .collect { deletion ->
                    val daysLeft = if (deletion != null) {
                        val secondsLeft = deletion.hardDeleteAt.epochSeconds - Clock.System.now().epochSeconds
                        (secondsLeft / 86_400L).coerceAtLeast(0)
                    } else {
                        0L
                    }
                    _uiState.update {
                        it.copy(pendingDeletion = deletion, daysRemaining = daysLeft)
                    }
                }
        }
    }

    fun requestDeletion() {
        viewModelScope.launch {
            runCatching { requestDeletionUseCase() }
                .onSuccess {
                    sessionRepo.clear()
                    _uiState.update { it.copy(completedDeletion = true) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.message ?: "Couldn't schedule deletion") }
                }
        }
    }

    fun cancelDeletion() {
        viewModelScope.launch {
            runCatching { cancelDeletionUseCase() }
                .onFailure { e -> _uiState.update { it.copy(errorMessage = e.message) } }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

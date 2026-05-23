package com.rodiz.arch2.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodiz.arch2.core.session.domain.SessionRepository
import com.rodiz.arch2.feature.match.domain.usecase.ObserveInboxUseCase
import com.rodiz.arch2.feature.pet.domain.usecase.ObserveMyPetsUseCase
import com.rodiz.arch2.feature.settings.domain.model.AccountDeletion
import com.rodiz.arch2.feature.settings.domain.usecase.CancelAccountDeletionUseCase
import com.rodiz.arch2.feature.settings.domain.usecase.ObservePendingDeletionUseCase
import com.rodiz.arch2.feature.settings.domain.usecase.RequestAccountDeletionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject

internal data class DeleteAccountSheetUiState(
    val pendingDeletion: AccountDeletion? = null,
    val daysRemaining: Long = 0,
    val petNames: List<String> = emptyList(),
    val matchCount: Int = 0,
    val typed: String = "",
    val isSubmitting: Boolean = false,
    val errorRes: Int? = null,
    val completed: Boolean = false,
) {
    val canSubmit: Boolean get() = typed == "DELETE" && !isSubmitting
}

@HiltViewModel
internal class DeleteAccountSheetViewModel @Inject constructor(
    observePendingDeletion: ObservePendingDeletionUseCase,
    private val requestDeletion: RequestAccountDeletionUseCase,
    private val cancelDeletion: CancelAccountDeletionUseCase,
    private val sessionRepository: SessionRepository,
    private val observeMyPets: ObserveMyPetsUseCase,
    private val observeInbox: ObserveInboxUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(DeleteAccountSheetUiState())
    val state: StateFlow<DeleteAccountSheetUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            observePendingDeletion()
                .catch { _state.update { it.copy(errorRes = R.string.delete_sheet_error_generic) } }
                .collect { deletion ->
                    val daysLeft = if (deletion != null) {
                        val secondsLeft =
                            deletion.hardDeleteAt.epochSeconds - Clock.System.now().epochSeconds
                        (secondsLeft / 86_400L).coerceAtLeast(0)
                    } else {
                        0L
                    }
                    _state.update {
                        it.copy(pendingDeletion = deletion, daysRemaining = daysLeft)
                    }
                }
        }
    }

    /** Read pet names + match count once, on sheet open. Safe to call repeatedly. */
    fun onSheetOpen() {
        viewModelScope.launch {
            runCatching {
                val pets = observeMyPets().first()
                val session = sessionRepository.current()
                val matches = session?.userId?.let { uid -> observeInbox(uid).first() }
                _state.update {
                    it.copy(
                        petNames = pets.map { pet -> pet.name },
                        matchCount = matches?.let { snap -> snap.newMatches.size + snap.conversations.size }
                            ?: 0,
                    )
                }
            }
            // Snapshot errors are non-fatal — the sheet still works without the summary counts.
        }
    }

    fun onTypedChanged(value: String) {
        _state.update { it.copy(typed = value.uppercase()) }
    }

    fun onConfirmDelete() {
        if (!_state.value.canSubmit) return
        _state.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            runCatching { requestDeletion() }
                .onSuccess {
                    sessionRepository.clear()
                    _state.update { it.copy(isSubmitting = false, completed = true) }
                }
                .onFailure {
                    _state.update {
                        it.copy(isSubmitting = false, errorRes = R.string.delete_sheet_error_generic)
                    }
                }
        }
    }

    fun onCancelDeletion() {
        _state.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            runCatching { cancelDeletion() }
                .onSuccess { _state.update { it.copy(isSubmitting = false) } }
                .onFailure {
                    _state.update {
                        it.copy(isSubmitting = false, errorRes = R.string.delete_sheet_error_cancel)
                    }
                }
        }
    }

    fun onDismiss() {
        _state.update { it.copy(typed = "", errorRes = null) }
    }

    fun onErrorShown() {
        _state.update { it.copy(errorRes = null) }
    }

    fun onCompletedHandled() {
        _state.update { it.copy(completed = false) }
    }
}

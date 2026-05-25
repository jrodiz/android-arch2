package com.rodiz.arch2.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodiz.arch2.feature.profile.domain.usecase.ObserveMyProfileUseCase
import com.rodiz.arch2.feature.profile.domain.usecase.SetPausedUseCase
import com.rodiz.arch2.feature.settings.domain.model.DataExportStatus
import com.rodiz.arch2.feature.settings.domain.repository.DataExportRepository
import com.rodiz.arch2.feature.settings.domain.usecase.ObserveBlockedOwnersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class PrivacyUiState(
    val isLoading: Boolean = true,
    val paused: Boolean = false,
    val blockedCount: Int = 0,
    val isWriting: Boolean = false,
    val errorMessage: String? = null,
    val transientMessage: String? = null,
)

@HiltViewModel
internal class PrivacyViewModel @Inject constructor(
    observeMyProfile: ObserveMyProfileUseCase,
    observeBlockedOwners: ObserveBlockedOwnersUseCase,
    private val setPaused: SetPausedUseCase,
    private val dataExportRepository: DataExportRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrivacyUiState())
    val uiState: StateFlow<PrivacyUiState> = _uiState.asStateFlow()

    /**
     * Current state of the "Download my data" request. Idle → Pending after the
     * user taps the row → Ready (with downloadUrl) once the Cloud Function
     * finishes. The Privacy screen reads this and shows the right snackbar /
     * download CTA.
     */
    val exportStatus: StateFlow<DataExportStatus> = dataExportRepository
        .observeMyExport()
        .catch { /* surface via errorMessage; status stays at last emitted */ }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), DataExportStatus.Idle)

    init {
        viewModelScope.launch {
            val profileFlow = observeMyProfile()
                .onStart { emit(null) }
                .catch { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                    emit(null)
                }
            val blockedFlow = observeBlockedOwners()
                .onStart { emit(emptyList()) }
                .catch { e ->
                    _uiState.update { it.copy(errorMessage = e.message) }
                    emit(emptyList())
                }
            combine(profileFlow, blockedFlow) { profile, blocked ->
                profile to blocked
            }.collect { (profile, blocked) ->
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        // Don't clobber an in-flight optimistic pause toggle.
                        paused = if (current.isWriting) current.paused else profile?.paused ?: false,
                        blockedCount = blocked.size,
                    )
                }
            }
        }
    }

    fun onTogglePause(value: Boolean) {
        if (_uiState.value.isWriting) return
        _uiState.update { it.copy(paused = value, isWriting = true) }
        viewModelScope.launch {
            runCatching { setPaused(value) }
                .onFailure { e -> _uiState.update { it.copy(errorMessage = e.message ?: "Could not save") } }
            _uiState.update { it.copy(isWriting = false) }
        }
    }

    /** Kick off a data export. Idempotent — re-tapping while pending is a no-op. */
    fun onRequestDataExport() {
        if (exportStatus.value is DataExportStatus.Pending) return
        viewModelScope.launch {
            runCatching { dataExportRepository.requestExport() }
                .onFailure { e ->
                    _uiState.update { it.copy(errorMessage = e.message ?: "Could not start export") }
                }
        }
    }

    /** Called after the user opens (or dismisses) the Ready snackbar. */
    fun onExportAcknowledged() {
        viewModelScope.launch {
            runCatching { dataExportRepository.acknowledge() }
        }
    }

    fun notifyComingSoon(message: String) {
        _uiState.update { it.copy(transientMessage = message) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearTransient() {
        _uiState.update { it.copy(transientMessage = null) }
    }
}

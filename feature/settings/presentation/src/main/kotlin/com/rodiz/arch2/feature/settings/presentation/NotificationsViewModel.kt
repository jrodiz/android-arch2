package com.rodiz.arch2.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodiz.arch2.feature.settings.domain.model.NotificationPrefs
import com.rodiz.arch2.feature.settings.domain.usecase.ObserveNotificationPrefsUseCase
import com.rodiz.arch2.feature.settings.domain.usecase.UpdateNotificationPrefsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class NotificationsUiState(
    val isLoading: Boolean = true,
    val prefs: NotificationPrefs = NotificationPrefs.DEFAULT,
    val errorMessage: String? = null,
)

@HiltViewModel
internal class NotificationsViewModel @Inject constructor(
    observePrefs: ObserveNotificationPrefsUseCase,
    private val updatePrefs: UpdateNotificationPrefsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    private var debounceJob: Job? = null

    init {
        viewModelScope.launch {
            observePrefs()
                .catch { e -> _uiState.update { it.copy(isLoading = false, errorMessage = e.message) } }
                .collect { prefs ->
                    // Only refresh server state when no pending local edit is in flight,
                    // otherwise we'd snap the UI back to the pre-write value.
                    if (debounceJob?.isActive != true) {
                        _uiState.update { it.copy(isLoading = false, prefs = prefs) }
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
        }
    }

    fun onToggleNewMatch(value: Boolean) = patch { it.copy(newMatch = value) }
    fun onToggleNewMessage(value: Boolean) = patch { it.copy(newMessage = value) }
    fun onToggleSomeoneLiked(value: Boolean) = patch { it.copy(someoneLiked = value) }
    fun onToggleWeeklyDigest(value: Boolean) = patch { it.copy(weeklyDigest = value) }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun patch(mutate: (NotificationPrefs) -> NotificationPrefs) {
        val next = mutate(_uiState.value.prefs)
        _uiState.update { it.copy(prefs = next) }
        debounceJob?.cancel()
        // 200ms debounce — multiple toggles in a row coalesce into one write.
        debounceJob = viewModelScope.launch {
            delay(200)
            runCatching { updatePrefs(next) }
                .onFailure { e -> _uiState.update { it.copy(errorMessage = e.message ?: "Save failed") } }
        }
    }
}

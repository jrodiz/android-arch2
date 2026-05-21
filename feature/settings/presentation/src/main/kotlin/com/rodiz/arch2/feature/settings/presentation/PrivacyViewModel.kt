package com.rodiz.arch2.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodiz.arch2.feature.profile.domain.usecase.ObserveMyProfileUseCase
import com.rodiz.arch2.feature.profile.domain.usecase.SetPausedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class PrivacyUiState(
    val isLoading: Boolean = true,
    val paused: Boolean = false,
    val isWriting: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
internal class PrivacyViewModel @Inject constructor(
    observeMyProfile: ObserveMyProfileUseCase,
    private val setPaused: SetPausedUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrivacyUiState())
    val uiState: StateFlow<PrivacyUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeMyProfile()
                .catch { e -> _uiState.update { it.copy(isLoading = false, errorMessage = e.message) } }
                .collect { profile ->
                    if (!_uiState.value.isWriting) {
                        _uiState.update { it.copy(isLoading = false, paused = profile?.paused ?: false) }
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
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

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

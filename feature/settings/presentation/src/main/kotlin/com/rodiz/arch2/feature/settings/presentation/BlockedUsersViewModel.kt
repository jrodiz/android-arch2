package com.rodiz.arch2.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodiz.arch2.feature.settings.domain.model.BlockedOwner
import com.rodiz.arch2.feature.settings.domain.usecase.ObserveBlockedOwnersUseCase
import com.rodiz.arch2.feature.settings.domain.usecase.UnblockOwnerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

internal data class BlockedUsersUiState(
    val isLoading: Boolean = true,
    val blocked: List<BlockedOwner> = emptyList(),
    val pendingUnblock: Set<String> = emptySet(),
    val errorMessage: String? = null,
)

@HiltViewModel
internal class BlockedUsersViewModel @Inject constructor(
    observeBlockedOwners: ObserveBlockedOwnersUseCase,
    private val unblockOwner: UnblockOwnerUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BlockedUsersUiState())
    val uiState: StateFlow<BlockedUsersUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeBlockedOwners()
                .catch { e -> _uiState.update { it.copy(isLoading = false, errorMessage = e.message) } }
                .collect { list ->
                    _uiState.update { it.copy(isLoading = false, blocked = list) }
                }
        }
    }

    fun unblock(otherOwnerId: String) {
        if (otherOwnerId in _uiState.value.pendingUnblock) return
        _uiState.update { it.copy(pendingUnblock = it.pendingUnblock + otherOwnerId) }
        viewModelScope.launch {
            runCatching { unblockOwner(otherOwnerId) }
                .onFailure { e -> _uiState.update { it.copy(errorMessage = e.message ?: "Could not unblock") } }
            _uiState.update { it.copy(pendingUnblock = it.pendingUnblock - otherOwnerId) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

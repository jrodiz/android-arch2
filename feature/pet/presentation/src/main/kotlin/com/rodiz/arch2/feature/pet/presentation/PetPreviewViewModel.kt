package com.rodiz.arch2.feature.pet.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodiz.arch2.feature.pet.domain.model.Pet
import com.rodiz.arch2.feature.pet.domain.model.PetId
import com.rodiz.arch2.feature.pet.domain.usecase.ArchivePetUseCase
import com.rodiz.arch2.feature.pet.domain.usecase.ObservePetUseCase
import com.rodiz.arch2.feature.pet.domain.usecase.SetPetEnabledUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class PetPreviewUiState(
    val pet: Pet? = null,
    val isLoading: Boolean = true,
    val isUpdatingEnabled: Boolean = false,
    val errorMessage: String? = null,
)

internal class PetPreviewViewModel @AssistedInject constructor(
    @Assisted petIdValue: String,
    observePet: ObservePetUseCase,
    private val archivePet: ArchivePetUseCase,
    private val setPetEnabled: SetPetEnabledUseCase,
) : ViewModel() {

    private val petId: PetId = PetId(petIdValue)

    @AssistedFactory
    interface Factory {
        // String, not PetId — see EditPetViewModel for why.
        fun create(petId: String): PetPreviewViewModel
    }

    private val _uiState = MutableStateFlow(PetPreviewUiState())
    val uiState: StateFlow<PetPreviewUiState> = _uiState.asStateFlow()

    private val _archived = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val archived: SharedFlow<Unit> = _archived.asSharedFlow()

    init {
        viewModelScope.launch {
            observePet(petId)
                .catch { e -> _uiState.update { it.copy(isLoading = false, errorMessage = e.message) } }
                .collect { pet ->
                    _uiState.update { it.copy(pet = pet, isLoading = false) }
                }
        }
    }

    fun archive() {
        viewModelScope.launch {
            runCatching { archivePet(petId) }
                .onSuccess { _archived.tryEmit(Unit) }
                .onFailure { e -> _uiState.update { it.copy(errorMessage = e.message) } }
        }
    }

    fun toggleEnabled() {
        val current = _uiState.value.pet ?: return
        if (_uiState.value.isUpdatingEnabled) return
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingEnabled = true) }
            runCatching { setPetEnabled(petId, !current.enabled) }
                .onFailure { e -> _uiState.update { it.copy(errorMessage = e.message) } }
            _uiState.update { it.copy(isUpdatingEnabled = false) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}

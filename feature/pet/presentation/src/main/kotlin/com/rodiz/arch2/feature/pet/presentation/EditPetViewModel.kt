package com.rodiz.arch2.feature.pet.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodiz.arch2.feature.pet.domain.model.Pet
import com.rodiz.arch2.feature.pet.domain.model.PetDraft
import com.rodiz.arch2.feature.pet.domain.model.PetId
import com.rodiz.arch2.feature.pet.domain.usecase.ArchivePetUseCase
import com.rodiz.arch2.feature.pet.domain.usecase.ObservePetUseCase
import com.rodiz.arch2.feature.pet.domain.usecase.UpdatePetUseCase
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

internal class EditPetViewModel @AssistedInject constructor(
    @Assisted petIdValue: String,
    observePet: ObservePetUseCase,
    private val updatePet: UpdatePetUseCase,
    private val archivePet: ArchivePetUseCase,
) : ViewModel() {

    private val petId: PetId = PetId(petIdValue)

    @AssistedFactory
    interface Factory {
        // String, not PetId — Dagger's JavaPoet can't generate code for
        // factory methods whose param type is a Kotlin value class
        // (the synthetic method name gets mangled, e.g., create-XYZ).
        fun create(petId: String): EditPetViewModel
    }

    private val _uiState = MutableStateFlow(PetFormUiState())
    val uiState: StateFlow<PetFormUiState> = _uiState.asStateFlow()

    private val _completed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val completed: SharedFlow<Unit> = _completed.asSharedFlow()

    private var loaded = false

    init {
        viewModelScope.launch {
            observePet(petId)
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                .collect { pet ->
                    if (pet != null && !loaded) {
                        _uiState.update { it.copy(draft = pet.toDraft()) }
                        loaded = true
                    }
                }
        }
    }

    fun onEvent(event: PetFormEvent) = handlePetFormEvent(_uiState, event)

    fun submit() {
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, errors = emptyList()) }
            val result = runCatching { updatePet(petId, _uiState.value.draft) }
                .getOrElse { e ->
                    _uiState.update {
                        it.copy(isSubmitting = false, errorMessage = e.message ?: "Failed to save")
                    }
                    return@launch
                }
            when (result) {
                is UpdatePetUseCase.Result.Invalid ->
                    _uiState.update { it.copy(isSubmitting = false, errors = result.errors) }
                is UpdatePetUseCase.Result.Success -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _completed.tryEmit(Unit)
                }
            }
        }
    }

    fun archive(onArchived: () -> Unit) {
        viewModelScope.launch {
            runCatching { archivePet(petId) }
                .onSuccess { onArchived() }
                .onFailure { e -> _uiState.update { it.copy(errorMessage = e.message) } }
        }
    }
}

private fun Pet.toDraft(): PetDraft = PetDraft(
    name = name,
    ageYears = ageYears,
    ageIsApproximate = ageIsApproximate,
    species = species,
    intents = intents,
    photos = photos,
    bio = bio,
)

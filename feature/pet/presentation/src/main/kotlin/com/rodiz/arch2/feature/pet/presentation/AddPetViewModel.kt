package com.rodiz.arch2.feature.pet.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodiz.arch2.feature.pet.domain.model.PetDraft
import com.rodiz.arch2.feature.pet.domain.usecase.AddPetUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class AddPetViewModel @Inject constructor(
    private val addPet: AddPetUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(PetFormUiState())
    val uiState: StateFlow<PetFormUiState> = _uiState.asStateFlow()

    private val _completed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val completed: SharedFlow<Unit> = _completed.asSharedFlow()

    fun onEvent(event: PetFormEvent) = handlePetFormEvent(_uiState, event)

    fun submit() {
        if (_uiState.value.isSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, errors = emptyList()) }
            val result = runCatching { addPet(_uiState.value.draft) }
                .getOrElse { e ->
                    _uiState.update {
                        it.copy(isSubmitting = false, errorMessage = e.message ?: "Failed to save")
                    }
                    return@launch
                }
            when (result) {
                is AddPetUseCase.Result.Invalid -> {
                    _uiState.update { it.copy(isSubmitting = false, errors = result.errors) }
                }
                is AddPetUseCase.Result.Success -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    _completed.tryEmit(Unit)
                }
            }
        }
    }
}

internal fun handlePetFormEvent(state: MutableStateFlow<PetFormUiState>, event: PetFormEvent) {
    state.update { current ->
        val draft = current.draft
        val newDraft = when (event) {
            is PetFormEvent.NameChanged -> draft.copy(name = event.value.take(PetDraft.NAME_MAX_LEN))
            is PetFormEvent.AgeYearsChanged -> draft.copy(
                ageYears = event.years.coerceIn(PetDraft.AGE_MIN, PetDraft.AGE_MAX),
            )
            is PetFormEvent.AgeApproximateChanged -> draft.copy(ageIsApproximate = event.approximate)
            is PetFormEvent.SpeciesChanged -> draft.copy(species = event.species)
            is PetFormEvent.IntentToggled -> draft.copy(
                intents = if (event.intent in draft.intents) draft.intents - event.intent
                else draft.intents + event.intent,
            )
            is PetFormEvent.PhotoAdded -> if (draft.photos.size < PetDraft.PHOTOS_MAX) {
                draft.copy(photos = draft.photos + event.photo)
            } else {
                draft
            }
            is PetFormEvent.PhotoRemoved -> draft.copy(
                photos = draft.photos.filterIndexed { idx, _ -> idx != event.index },
            )
            is PetFormEvent.BioChanged -> draft.copy(bio = event.value.take(PetDraft.BIO_MAX_LEN).ifEmpty { null })
            PetFormEvent.DismissError -> draft
        }
        when (event) {
            PetFormEvent.DismissError -> current.copy(errorMessage = null)
            else -> current.copy(draft = newDraft)
        }
    }
}

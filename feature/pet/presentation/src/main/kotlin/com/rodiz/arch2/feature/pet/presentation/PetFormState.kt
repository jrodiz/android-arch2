package com.rodiz.arch2.feature.pet.presentation

import com.rodiz.arch2.feature.pet.domain.model.Intent
import com.rodiz.arch2.feature.pet.domain.model.PetDraft
import com.rodiz.arch2.feature.pet.domain.model.PetPhoto
import com.rodiz.arch2.feature.pet.domain.model.PetValidationError
import com.rodiz.arch2.feature.pet.domain.model.Species

internal data class PetFormUiState(
    val draft: PetDraft = PetDraft.EMPTY,
    val errors: List<PetValidationError> = emptyList(),
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
)

internal sealed interface PetFormEvent {
    data class NameChanged(val value: String) : PetFormEvent
    data class AgeYearsChanged(val years: Int) : PetFormEvent
    data class AgeApproximateChanged(val approximate: Boolean) : PetFormEvent
    data class SpeciesChanged(val species: Species) : PetFormEvent
    data class IntentToggled(val intent: Intent) : PetFormEvent
    data class PhotoAdded(val photo: PetPhoto) : PetFormEvent
    data class PhotoRemoved(val index: Int) : PetFormEvent
    data class BioChanged(val value: String) : PetFormEvent
    data object DismissError : PetFormEvent
}

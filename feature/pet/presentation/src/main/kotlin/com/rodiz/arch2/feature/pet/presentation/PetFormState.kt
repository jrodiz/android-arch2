package com.rodiz.arch2.feature.pet.presentation

import com.rodiz.arch2.feature.pet.domain.model.Intent
import com.rodiz.arch2.feature.pet.domain.model.PetDraft
import com.rodiz.arch2.feature.pet.domain.model.PetEnergy
import com.rodiz.arch2.feature.pet.domain.model.PetPhoto
import com.rodiz.arch2.feature.pet.domain.model.PetSize
import com.rodiz.arch2.feature.pet.domain.model.PetValidationError
import com.rodiz.arch2.feature.pet.domain.model.Species

internal data class PetFormUiState(
    val draft: PetDraft = PetDraft.EMPTY,
    val errors: List<PetValidationError> = emptyList(),
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    /** Step-1 validation errors surfaced inline under each labeled section. */
    val step1Errors: Set<Step1Error> = emptySet(),
    /** Which wizard step is on screen (1, 2, or 3). Drives the AddPet wrapper. */
    val currentStep: Int = 1,
)

/**
 * Presentation-only validation flags for the redesigned Add a pet — Step 1 screen.
 * Lets the UI render an inline error under the right section without re-running the
 * full domain `validate()` (which checks photo-count caps, bio length, etc. that are
 * not Step-1 concerns).
 */
internal sealed interface Step1Error {
    data object NameMissing : Step1Error
    data object PhotosMissing : Step1Error
    data object SpeciesMissing : Step1Error
    data object IntentMissing : Step1Error
}

internal sealed interface PetFormEvent {
    data class NameChanged(val value: String) : PetFormEvent
    data class AgeYearsChanged(val years: Int) : PetFormEvent
    data class AgeApproximateChanged(val approximate: Boolean) : PetFormEvent
    data class SpeciesChanged(val species: Species) : PetFormEvent
    data class IntentToggled(val intent: Intent) : PetFormEvent
    data class PhotoAdded(val photo: PetPhoto) : PetFormEvent
    data class PhotoRemoved(val index: Int) : PetFormEvent
    data class BioChanged(val value: String) : PetFormEvent
    /** Pass null to clear the selection (tapping an already-selected pill). */
    data class SizeChanged(val size: PetSize?) : PetFormEvent
    /** Pass null to clear the selection. */
    data class EnergyChanged(val energy: PetEnergy?) : PetFormEvent
    data object DismissError : PetFormEvent
}

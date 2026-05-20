package com.rodiz.arch2.feature.pet.domain.usecase

import com.rodiz.arch2.feature.pet.domain.model.Pet
import com.rodiz.arch2.feature.pet.domain.model.PetDraft
import com.rodiz.arch2.feature.pet.domain.model.PetDraftValidation
import com.rodiz.arch2.feature.pet.domain.model.PetValidationError
import com.rodiz.arch2.feature.pet.domain.model.validate
import com.rodiz.arch2.feature.pet.domain.repository.PetRepository
import javax.inject.Inject

class AddPetUseCase @Inject constructor(
    private val repo: PetRepository,
) {
    sealed interface Result {
        data class Success(val pet: Pet) : Result
        data class Invalid(val errors: List<PetValidationError>) : Result
    }

    suspend operator fun invoke(draft: PetDraft): Result =
        when (val v = draft.validate()) {
            is PetDraftValidation.Invalid -> Result.Invalid(v.errors)
            is PetDraftValidation.Valid -> Result.Success(repo.addPet(v.draft))
        }
}

package com.rodiz.arch2.feature.pet.domain.usecase

import com.rodiz.arch2.feature.pet.domain.model.Pet
import com.rodiz.arch2.feature.pet.domain.model.PetId
import com.rodiz.arch2.feature.pet.domain.repository.PetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObservePetUseCase @Inject constructor(
    private val repo: PetRepository,
) {
    operator fun invoke(id: PetId): Flow<Pet?> = repo.observePet(id)
}

package com.rodiz.arch2.feature.pet.domain.usecase

import com.rodiz.arch2.feature.pet.domain.model.Pet
import com.rodiz.arch2.feature.pet.domain.model.PetState
import com.rodiz.arch2.feature.pet.domain.repository.PetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObserveMyPetsUseCase @Inject constructor(
    private val repo: PetRepository,
) {
    /** Returns pets whose state matches [filter]. Defaults to ACTIVE only. */
    operator fun invoke(filter: Set<PetState> = setOf(PetState.ACTIVE)): Flow<List<Pet>> =
        repo.observeMyPets().map { pets -> pets.filter { it.state in filter } }
}

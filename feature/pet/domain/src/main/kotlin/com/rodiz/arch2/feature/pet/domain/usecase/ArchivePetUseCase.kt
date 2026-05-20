package com.rodiz.arch2.feature.pet.domain.usecase

import com.rodiz.arch2.feature.pet.domain.model.PetId
import com.rodiz.arch2.feature.pet.domain.repository.PetRepository
import javax.inject.Inject

class ArchivePetUseCase @Inject constructor(
    private val repo: PetRepository,
) {
    suspend operator fun invoke(id: PetId) = repo.archivePet(id)
}

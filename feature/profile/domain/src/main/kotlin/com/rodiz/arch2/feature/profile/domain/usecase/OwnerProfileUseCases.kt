package com.rodiz.arch2.feature.profile.domain.usecase

import com.rodiz.arch2.feature.profile.domain.model.OwnerProfile
import com.rodiz.arch2.feature.profile.domain.repository.OwnerProfileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveMyProfileUseCase @Inject constructor(
    private val repo: OwnerProfileRepository,
) {
    operator fun invoke(): Flow<OwnerProfile?> = repo.observeMyProfile()
}

class UpdateFirstNameUseCase @Inject constructor(
    private val repo: OwnerProfileRepository,
) {
    suspend operator fun invoke(name: String) {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "First name cannot be empty" }
        require(trimmed.length <= 30) { "First name cannot exceed 30 characters" }
        repo.updateFirstName(trimmed)
    }
}

class UpdateAvatarUseCase @Inject constructor(
    private val repo: OwnerProfileRepository,
) {
    suspend operator fun invoke(localUri: String) = repo.updateAvatar(localUri)
}

class SetPausedUseCase @Inject constructor(
    private val repo: OwnerProfileRepository,
) {
    suspend operator fun invoke(paused: Boolean) = repo.setPaused(paused)
}

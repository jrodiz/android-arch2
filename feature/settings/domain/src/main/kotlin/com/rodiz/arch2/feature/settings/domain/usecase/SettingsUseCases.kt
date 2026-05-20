package com.rodiz.arch2.feature.settings.domain.usecase

import com.rodiz.arch2.feature.settings.domain.model.AccountDeletion
import com.rodiz.arch2.feature.settings.domain.repository.AccountDeletionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObservePendingDeletionUseCase @Inject constructor(
    private val repo: AccountDeletionRepository,
) {
    operator fun invoke(): Flow<AccountDeletion?> = repo.observePendingDeletion()
}

class RequestAccountDeletionUseCase @Inject constructor(
    private val repo: AccountDeletionRepository,
) {
    suspend operator fun invoke(): AccountDeletion = repo.requestDeletion()
}

class CancelAccountDeletionUseCase @Inject constructor(
    private val repo: AccountDeletionRepository,
) {
    suspend operator fun invoke() = repo.cancelDeletion()
}

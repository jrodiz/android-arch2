package com.rodiz.arch2.feature.settings.domain.usecase

import com.rodiz.arch2.feature.settings.domain.model.AccountDeletion
import com.rodiz.arch2.feature.settings.domain.model.NotificationPrefs
import com.rodiz.arch2.feature.settings.domain.repository.AccountDeletionRepository
import com.rodiz.arch2.feature.settings.domain.repository.NotificationPrefsRepository
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

class ObserveNotificationPrefsUseCase @Inject constructor(
    private val repo: NotificationPrefsRepository,
) {
    operator fun invoke(): Flow<NotificationPrefs> = repo.observePrefs()
}

class UpdateNotificationPrefsUseCase @Inject constructor(
    private val repo: NotificationPrefsRepository,
) {
    suspend operator fun invoke(prefs: NotificationPrefs) = repo.updatePrefs(prefs)
}

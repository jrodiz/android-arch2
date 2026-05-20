package com.rodiz.arch2.feature.settings.domain.repository

import com.rodiz.arch2.feature.settings.domain.model.AccountDeletion
import kotlinx.coroutines.flow.Flow

interface AccountDeletionRepository {
    /** Pending deletion for the current user, or null if none scheduled. */
    fun observePendingDeletion(): Flow<AccountDeletion?>

    /** Writes /accountDeletions/{me} with hardDeleteAt = now + 30d. Returns the scheduled deletion. */
    suspend fun requestDeletion(): AccountDeletion

    /** Removes the pending deletion (user came back during the grace window). */
    suspend fun cancelDeletion()
}

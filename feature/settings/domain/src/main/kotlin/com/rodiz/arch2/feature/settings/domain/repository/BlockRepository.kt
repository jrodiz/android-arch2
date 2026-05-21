package com.rodiz.arch2.feature.settings.domain.repository

import com.rodiz.arch2.feature.settings.domain.model.BlockedOwner
import kotlinx.coroutines.flow.Flow

interface BlockRepository {
    /** Live list of owners blocked by the current user (joined with their display info). */
    fun observeBlockedOwners(): Flow<List<BlockedOwner>>

    /** Block another owner. Idempotent — re-blocking the same owner is a no-op. */
    suspend fun block(otherOwnerId: String)

    /** Remove an existing block. No-op if no such block exists. */
    suspend fun unblock(otherOwnerId: String)
}

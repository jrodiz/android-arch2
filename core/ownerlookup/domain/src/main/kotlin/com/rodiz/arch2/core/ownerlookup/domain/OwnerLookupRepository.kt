package com.rodiz.arch2.core.ownerlookup.domain

import kotlinx.coroutines.flow.Flow

/**
 * Read-only owner directory for surfacing display info (name + avatar) outside the
 * owner profile feature. Backed by /owners; reactive to changes.
 */
interface OwnerLookupRepository {
    /** Live snapshot of every owner with a profile doc, keyed by owner id. */
    fun observeAll(): Flow<Map<String, OwnerDisplay>>

    /** Live single-owner lookup. Emits null when the doc doesn't exist. */
    fun observe(ownerId: String): Flow<OwnerDisplay?>
}

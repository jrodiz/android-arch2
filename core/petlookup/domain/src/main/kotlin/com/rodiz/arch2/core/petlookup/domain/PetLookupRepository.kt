package com.rodiz.arch2.core.petlookup.domain

import kotlinx.coroutines.flow.Flow

/**
 * Read-only pet directory for surfacing pet display info (name, species, photo)
 * outside the pet feature itself. Backed by the `pets` collection; reactive so
 * cross-feature surfaces (inbox row, chat header) re-render when a pet's name or
 * photo changes.
 */
interface PetLookupRepository {
    /** Live snapshot of every pet doc, keyed by pet id. */
    fun observeAll(): Flow<Map<String, PetDisplay>>

    /** Live single-pet lookup. Emits null when the doc doesn't exist. */
    fun observe(petId: String): Flow<PetDisplay?>
}

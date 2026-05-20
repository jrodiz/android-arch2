package com.rodiz.arch2.feature.pet.domain.repository

import com.rodiz.arch2.feature.pet.domain.model.Pet
import com.rodiz.arch2.feature.pet.domain.model.PetDraft
import com.rodiz.arch2.feature.pet.domain.model.PetId
import kotlinx.coroutines.flow.Flow

interface PetRepository {
    /** All pets for the current owner (ACTIVE + ARCHIVED). The UI filters by state. */
    fun observeMyPets(): Flow<List<Pet>>

    /** All ACTIVE pets globally, capped at [limit]. The deck reads this then filters client-side. */
    fun observeAllActivePets(limit: Long = 500): Flow<List<Pet>>

    /** Returns true iff the current owner has at least one ACTIVE pet. Used by the deck's petless guard. */
    suspend fun currentOwnerHasActivePet(): Boolean

    /** A single pet by id; emits null after PURGED. */
    fun observePet(id: PetId): Flow<Pet?>

    /** Creates a new ACTIVE pet from a pre-validated draft. */
    suspend fun addPet(draft: PetDraft): Pet

    /** Updates a pet's editable fields from a pre-validated draft. */
    suspend fun updatePet(id: PetId, draft: PetDraft): Pet

    /** Soft-deletes a pet: state -> ARCHIVED, deletedAt = now. */
    suspend fun archivePet(id: PetId)

    /** Restores an ARCHIVED pet within its 7-day grace window. */
    suspend fun restorePet(id: PetId)

    /** Owner-controlled visibility toggle. Disabled pets stay in My Pets but are hidden from the deck. */
    suspend fun setPetEnabled(id: PetId, enabled: Boolean)
}

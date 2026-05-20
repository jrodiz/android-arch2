package com.rodiz.arch2.feature.pet.domain.model

import kotlinx.datetime.Instant

data class Pet(
    val id: PetId,
    val ownerId: String,
    val name: String,
    val ageYears: Int,
    val ageIsApproximate: Boolean,
    val species: Species,
    val intents: Set<Intent>,
    val photos: List<PetPhoto>,
    val bio: String?,
    val state: PetState,
    /**
     * Owner-controlled visibility toggle. When `false`, the pet stays in the owner's
     * roster (My Pets) but is hidden from every other surface — most importantly the
     * deck other users see. Reversible: flipping back to `true` restores normal
     * visibility. Separate from [PetState] which models the permanent lifecycle
     * (ACTIVE → ARCHIVED → PURGED) for deletes.
     */
    val enabled: Boolean = true,
    val createdAt: Instant,
    val updatedAt: Instant,
    val deletedAt: Instant?,
)

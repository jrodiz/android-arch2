package com.rodiz.arch2.feature.pet.domain.model

import kotlinx.datetime.Instant

data class Pet(
    val id: PetId,
    val ownerId: String,
    val name: String,
    val ageYears: Int,
    val ageIsApproximate: Boolean,
    val species: Species,
    /**
     * Optional free-text breed (e.g. "Corgi", "Maine Coon"). When set, display
     * surfaces prefer the breed over the broader species label — e.g. the My Pets
     * card subtitle reads "3 yr · Corgi" instead of "3 yr · Dog". Null when the
     * owner hasn't filled it in (or for pets created before this field existed).
     */
    val breed: String? = null,
    val intents: Set<Intent>,
    val photos: List<PetPhoto>,
    val bio: String?,
    /**
     * Owner-reported size band. Optional — pets created before this field existed
     * (or owners who haven't filled it in) leave it null and the detail UI hides
     * the size chip rather than rendering a fallback.
     */
    val size: PetSize? = null,
    /**
     * Owner-reported energy level. Same nullability story as [size].
     */
    val energy: PetEnergy? = null,
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

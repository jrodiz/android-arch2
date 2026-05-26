package com.rodiz.arch2.core.featuredpets.domain

/**
 * Minimal pet-display data the Login hero needs to render a featured pet
 * tile before the user is signed in. Mirrors a subset of
 * [com.rodiz.arch2.core.petlookup.domain.PetDisplay] so this module can stay
 * pure JVM and not depend on petlookup's Firestore-side types.
 */
data class FeaturedPet(
    val id: String,
    val name: String,
    val species: String? = null,
    val avatarUrl: String? = null,
)

/** Hard limit on how many pets a user can pin to the Login hero. */
const val MAX_FEATURED_PETS: Int = 3

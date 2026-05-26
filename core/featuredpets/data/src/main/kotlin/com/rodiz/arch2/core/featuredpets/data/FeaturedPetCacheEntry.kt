package com.rodiz.arch2.core.featuredpets.data

import kotlinx.serialization.Serializable

/**
 * DataStore-serialized shape of a featured pet. Kept internal so callers go
 * through the [com.rodiz.arch2.core.featuredpets.domain.FeaturedPet] domain
 * model — the serialized form may evolve without breaking the API.
 */
@Serializable
internal data class FeaturedPetCacheEntry(
    val id: String,
    val name: String,
    val species: String? = null,
    val avatarUrl: String? = null,
)

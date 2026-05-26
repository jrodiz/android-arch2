package com.rodiz.arch2.core.featuredpets.domain

/**
 * Snapshot of the user's pinned set, ordered so the Login hero's three slots
 * are stable across recompositions (slot 0 = first pinned, etc.). An empty
 * list means "render the decorative paw default" — same as no user ever
 * having signed in.
 */
data class FeaturedPetsState(
    val featured: List<FeaturedPet> = emptyList(),
)

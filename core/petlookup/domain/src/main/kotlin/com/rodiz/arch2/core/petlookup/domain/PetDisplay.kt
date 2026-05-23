package com.rodiz.arch2.core.petlookup.domain

/**
 * Minimal pet identity for cross-feature display (inbox row title, chat header,
 * deck card subtitle). Mirrors [com.rodiz.arch2.core.ownerlookup.domain.OwnerDisplay]
 * for parallel "look up another user's pet by id" use cases — keeping this in a
 * shared JVM module avoids `:presentation` ever depending on another feature's
 * `:domain`.
 */
data class PetDisplay(
    val id: String,
    val ownerId: String,
    val name: String,
    val species: String?,
    val avatarUrl: String?,
)

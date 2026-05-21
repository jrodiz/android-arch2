package com.rodiz.arch2.core.ownerlookup.domain

/** Minimal owner identity for cross-feature display (deck card, match row, chat header). */
data class OwnerDisplay(
    val id: String,
    val firstName: String,
    val avatarUrl: String?,
)

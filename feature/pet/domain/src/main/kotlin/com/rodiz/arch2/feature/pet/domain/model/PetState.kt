package com.rodiz.arch2.feature.pet.domain.model

enum class PetState {
    /** Visible in deck, visible in My Pets. */
    ACTIVE,

    /** Soft-deleted; hidden from deck and the My Pets main list, restorable for 7 days. */
    ARCHIVED,

    /** Hard-deleted; record retained as a tombstone for chat references. */
    PURGED,
}

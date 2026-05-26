package com.rodiz.arch2.feature.deck.domain.model

enum class DeckState {
    LOADING,
    READY,
    EXHAUSTED,
    REQUIRES_PET, // owner has no active pet; browse-only, Like disabled
}

data class DeckSnapshot(
    val cards: List<DeckCard>,
    val state: DeckState,
)

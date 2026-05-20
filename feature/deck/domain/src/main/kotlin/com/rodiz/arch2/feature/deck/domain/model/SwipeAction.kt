package com.rodiz.arch2.feature.deck.domain.model

enum class SwipeAction { LIKE, PASS }

sealed interface SwipeResult {
    data object Pending : SwipeResult
    data class  Match(val matchId: String) : SwipeResult
    data object RequiresPet : SwipeResult
}

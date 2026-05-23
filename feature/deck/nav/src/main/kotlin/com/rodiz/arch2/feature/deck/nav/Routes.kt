package com.rodiz.arch2.feature.deck.nav

import kotlinx.serialization.Serializable

@Serializable
data object DeckHome

/**
 * Suitor-side pet detail opened from tapping a card on the Discover deck.
 *
 * Lives in `:feature:deck:nav` (not `:feature:pet:nav`) because the screen drives
 * deck swipe actions — keeping the route in the deck-feature lets `:feature:deck:presentation`
 * own the detail composable without violating the
 * "`:presentation` may depend on another feature's `:nav` only" rule.
 */
@Serializable
data class DeckPetDetail(val petId: String)

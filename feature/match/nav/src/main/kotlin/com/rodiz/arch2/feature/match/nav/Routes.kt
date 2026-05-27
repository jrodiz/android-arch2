package com.rodiz.arch2.feature.match.nav

import kotlinx.serialization.Serializable

@Serializable
data object MatchesHome

/**
 * One-shot celebration screen shown right after the user's like completes a
 * mutual match. Reached from `DeckRoute.onMatchHappened` via
 * `navigator.goTo(MatchCelebration(matchId))`. "Say hello" pops this entry
 * first and then pushes [com.rodiz.arch2.feature.chat.nav.ChatRoute], so the
 * celebration never sits in the user's back history.
 */
@Serializable
data class MatchCelebration(val matchId: String)

package com.rodiz.arch2.feature.match.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
internal fun MatchCelebrationRoute(
    onSayHello: () -> Unit,
    onKeepSwiping: () -> Unit,
    viewModel: MatchCelebrationViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // System back gesture mirrors "Keep swiping" — pop the celebration off the
    // back stack and land on Deck. Without this, the gesture would still pop,
    // but the symmetric handler keeps the contract explicit + survives future
    // changes to the host Scaffold's gesture handling.
    BackHandler { onKeepSwiping() }
    MatchCelebrationScreen(
        state = state,
        onSayHello = onSayHello,
        onKeepSwiping = onKeepSwiping,
    )
}

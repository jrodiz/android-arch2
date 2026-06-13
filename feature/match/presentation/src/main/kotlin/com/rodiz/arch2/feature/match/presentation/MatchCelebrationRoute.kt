package com.rodiz.arch2.feature.match.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
internal class MatchCelebrationFactoryHolder @Inject constructor(
    val factory: MatchCelebrationViewModel.Factory,
) : ViewModel()

@Composable
internal fun MatchCelebrationRoute(
    matchId: String,
    onSayHello: () -> Unit,
    onKeepSwiping: () -> Unit,
    holder: MatchCelebrationFactoryHolder = hiltViewModel(),
) {
    val viewModel = remember(matchId) { holder.factory.create(matchId) }
    val state by viewModel.uiState.collectAsState()
    BackHandler { onKeepSwiping() }
    MatchCelebrationScreen(
        state = state,
        onSayHello = onSayHello,
        onKeepSwiping = onKeepSwiping,
    )
}

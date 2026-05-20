package com.rodiz.arch2.feature.deck.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rodiz.arch2.feature.deck.domain.model.DeckState
import com.rodiz.arch2.feature.deck.domain.model.SwipeAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DeckRoute(
    onBack: () -> Unit,
    onAddPet: () -> Unit,
    viewModel: DeckViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.matchMessage) {
        state.matchMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMatch()
        }
    }
    LaunchedEffect(state.requiresPetMessage) {
        state.requiresPetMessage?.let {
            val result = snackbarHostState.showSnackbar(
                message = it,
                actionLabel = "Add",
                duration = androidx.compose.material3.SnackbarDuration.Short,
            )
            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                onAddPet()
            }
            viewModel.clearRequiresPet()
        }
    }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Deck") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            // Persistent banner: shows whenever the user has no active pet of their own.
            // Stays out of the way (small, above the deck) while still surfacing the
            // requirement upfront — before the user discovers it the hard way by tapping
            // Like and getting a snackbar. Hides as soon as ObserveMyPetsUseCase reports
            // at least one ACTIVE pet (e.g., user just added one).
            if (!state.hasOwnPet && state.state != DeckState.LOADING && state.state != DeckState.REQUIRES_PET) {
                PetlessOwnerBanner(onAddPet = onAddPet)
            }
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.state == DeckState.LOADING -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    state.state == DeckState.REQUIRES_PET && state.cards.isEmpty() -> {
                        DeckEmptyState(
                            title = "Add a pet to start swiping",
                            body = "Pets on TinPet are owned by you. Tap below to add your first one.",
                            actionLabel = "Add a pet",
                            onAction = onAddPet,
                        )
                    }
                    state.state == DeckState.EXHAUSTED -> {
                        DeckEmptyState(
                            title = "No more pets nearby",
                            body = "Check back later — new pets join all the time.",
                            actionLabel = null,
                            onAction = null,
                        )
                    }
                    else -> {
                        DeckStack(
                            cards = state.cards,
                            onSwipeLike = viewModel::likeTop,
                            onSwipePass = viewModel::passTop,
                            onRewind = viewModel::rewind,
                            onSwipeFromGesture = { action ->
                                when (action) {
                                    SwipeAction.LIKE -> viewModel.likeTop()
                                    SwipeAction.PASS -> viewModel.passTop()
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PetlessOwnerBanner(onAddPet: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Pets,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.size(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Add your own pet to like back",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                    text = "Browse freely, but liking requires at least one pet of your own.",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            TextButton(onClick = onAddPet) { Text("Add") }
        }
    }
}

@Composable
private fun DeckStack(
    cards: List<com.rodiz.arch2.feature.deck.domain.model.DeckCard>,
    onSwipeLike: () -> Unit,
    onSwipePass: () -> Unit,
    onRewind: () -> Unit,
    onSwipeFromGesture: (SwipeAction) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .weight(1f)
                .padding(vertical = 12.dp),
        ) {
            val top = cards.firstOrNull()
            if (top != null) {
                DeckCardView(card = top, onSwipe = onSwipeFromGesture)
            } else {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
        DeckActionBar(onSwipePass, onRewind, onSwipeLike)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun DeckActionBar(
    onPass: () -> Unit,
    onRewind: () -> Unit,
    onLike: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledIconButton(
            onClick = onPass,
            modifier = Modifier.size(64.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
        ) {
            Icon(Icons.Outlined.Close, contentDescription = "Pass", modifier = Modifier.size(32.dp))
        }
        FilledIconButton(
            onClick = onRewind,
            modifier = Modifier.size(56.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            ),
        ) {
            Icon(Icons.Outlined.Replay, contentDescription = "Rewind")
        }
        FilledIconButton(
            onClick = onLike,
            modifier = Modifier.size(64.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
            ),
        ) {
            Icon(Icons.Outlined.Favorite, contentDescription = "Like", modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
private fun DeckEmptyState(
    title: String,
    body: String,
    actionLabel: String?,
    onAction: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.Pets,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(24.dp))
            FilledTonalButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

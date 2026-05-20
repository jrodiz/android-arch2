package com.rodiz.arch2.feature.likes.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.rodiz.arch2.core.designsystem.component.EmptyTabState
import com.rodiz.arch2.feature.likes.domain.model.IncomingLike
import com.rodiz.arch2.feature.pet.domain.model.PhotoSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LikesYouRoute(
    onGoToDeck: () -> Unit,
    viewModel: LikesYouViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.clearError()
        }
    }
    LaunchedEffect(state.matchMessage) {
        state.matchMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMatch()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Likes you") }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.likes.isEmpty()) {
                EmptyTabState(
                    icon = Icons.Outlined.Favorite,
                    headline = "No one yet",
                    body = "When someone likes one of your pets, they'll appear here. Keep swiping in the deck.",
                    cta = "Go to Deck",
                    onCta = onGoToDeck,
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.likes, key = { it.key.value }) { like ->
                        LikeCard(like = like, onClick = { viewModel.onCardTap(like.key) })
                    }
                }
            }

            state.expandedKey?.let { key ->
                val sheet = state.likes.firstOrNull { it.key == key }
                if (sheet != null) {
                    LikeBottomSheet(
                        like = sheet,
                        onPass = viewModel::passExpanded,
                        onLikeBack = viewModel::likeBackExpanded,
                        onDismiss = viewModel::dismissSheet,
                    )
                }
            }
        }
    }
}

@Composable
private fun LikeCard(like: IncomingLike, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f),
    ) {
        Box(Modifier.fillMaxSize()) {
            val primaryUrl = like.anchorPet.photos.firstOrNull()?.let {
                (it.source as? PhotoSource.Remote)?.downloadUrl
            }
            if (primaryUrl != null) {
                AsyncImage(
                    model = primaryUrl,
                    contentDescription = like.anchorPet.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Pets,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.7f),
                        ),
                    )
                    .padding(12.dp),
            ) {
                Column {
                    Text(
                        text = like.anchorPet.name,
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${like.fromOwnerId.take(8)}… liked your pet",
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LikeBottomSheet(
    like: IncomingLike,
    onPass: () -> Unit,
    onLikeBack: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val primaryUrl = like.anchorPet.photos.firstOrNull()?.let {
                (it.source as? PhotoSource.Remote)?.downloadUrl
            }
            if (primaryUrl != null) {
                AsyncImage(
                    model = primaryUrl,
                    contentDescription = like.anchorPet.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(20.dp)),
                )
            }
            Text(
                text = "${like.anchorPet.name}, ${like.anchorPet.ageYears}",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            )
            Text(
                text = like.anchorPet.species.name.lowercase().replaceFirstChar { it.uppercase() },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (like.anchorPet.intents.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    like.anchorPet.intents.forEach { intent ->
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(intent.name.lowercase().replaceFirstChar { it.uppercase() })
                            },
                        )
                    }
                }
            }
            if (!like.anchorPet.bio.isNullOrBlank()) {
                Text(text = like.anchorPet.bio!!, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onPass,
                    modifier = Modifier.weight(1f),
                ) { Text("Pass") }
                FilledTonalButton(
                    onClick = onLikeBack,
                    modifier = Modifier.weight(1f),
                ) { Text("Like back") }
            }
        }
    }
}


package com.rodiz.arch2.feature.deck.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rodiz.arch2.core.designsystem.theme.BrandColors
import com.rodiz.arch2.feature.deck.domain.model.DeckState
import com.rodiz.arch2.feature.deck.domain.model.SwipeAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DeckRoute(
    onAddPet: () -> Unit,
    onOpenFilters: () -> Unit,
    onOpenNotifications: () -> Unit,
    viewModel: DeckViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

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
                actionLabel = context.getString(R.string.deck_add_pet_action),
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
        ) {
            DeckHeader(
                onOpenFilters = onOpenFilters,
                onOpenNotifications = onOpenNotifications,
            )
            Spacer(Modifier.height(12.dp))
            DeckMetaStrip(
                maxDistanceKm = state.maxDistanceKm,
                intentsCount = state.intentsCount,
                speciesCount = state.speciesCount,
            )
            Spacer(Modifier.height(16.dp))
            // Petless banner moves under the header but above the card.
            if (!state.hasOwnPet && state.state != DeckState.LOADING && state.state != DeckState.REQUIRES_PET) {
                PetlessOwnerBanner(onAddPet = onAddPet)
                Spacer(Modifier.height(12.dp))
            }
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.state == DeckState.LOADING -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    state.state == DeckState.REQUIRES_PET && state.cards.isEmpty() -> {
                        DeckEmptyState(
                            title = stringResource(R.string.deck_empty_petless_title),
                            body = stringResource(R.string.deck_empty_petless_body),
                            actionLabel = stringResource(R.string.deck_empty_petless_action),
                            onAction = onAddPet,
                        )
                    }
                    state.state == DeckState.EXHAUSTED -> {
                        DeckEmptyState(
                            title = stringResource(R.string.deck_empty_no_more_title),
                            body = stringResource(R.string.deck_empty_no_more_body),
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
private fun DeckHeader(
    onOpenFilters: () -> Unit,
    onOpenNotifications: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.deck_overline),
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.6.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.deck_headline),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        HeaderIconButton(
            onClick = onOpenFilters,
            icon = Icons.Outlined.Tune,
            contentDescription = stringResource(R.string.deck_filter_action),
        )
        Spacer(Modifier.width(4.dp))
        HeaderIconButton(
            onClick = onOpenNotifications,
            icon = Icons.Outlined.NotificationsNone,
            contentDescription = stringResource(R.string.deck_notifications_action),
        )
    }
}

@Composable
private fun HeaderIconButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
) {
    Surface(
        modifier = Modifier.size(40.dp),
        shape = RoundedCornerShape(percent = 30),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
        ),
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun DeckMetaStrip(
    maxDistanceKm: Int,
    intentsCount: Int,
    speciesCount: Int,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Distance
        Icon(
            imageVector = Icons.Outlined.Place,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.deck_distance_km_format, maxDistanceKm),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(14.dp))
        // Species icon strip — decorative stand-in (paw per category).
        repeat(speciesCount.coerceAtLeast(1)) { idx ->
            Icon(
                imageVector = Icons.Outlined.Pets,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.size(14.dp),
            )
            if (idx < speciesCount - 1) Spacer(Modifier.width(2.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = "•",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
        Spacer(Modifier.width(14.dp))
        Icon(
            imageVector = Icons.Outlined.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = pluralStringResource(
                id = R.plurals.deck_intents_count,
                count = intentsCount,
                intentsCount,
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PetlessOwnerBanner(onAddPet: () -> Unit) {
    Surface(
        color = BrandColors.CoralLight.copy(alpha = 0.25f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Pets,
                contentDescription = null,
                tint = BrandColors.CoralDeep,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.deck_petless_title),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                    text = stringResource(R.string.deck_petless_body),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onAddPet) {
                Text(stringResource(R.string.deck_add_pet_action))
            }
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
                .fillMaxWidth(),
            contentAlignment = Alignment.TopCenter,
        ) {
            val top = cards.firstOrNull()
            if (top != null) {
                DeckCardView(card = top, onSwipe = onSwipeFromGesture)
            } else {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
        Spacer(Modifier.height(16.dp))
        DeckActionBar(
            onPass = onSwipePass,
            onRewind = onRewind,
            onLike = onSwipeLike,
        )
        Spacer(Modifier.height(16.dp))
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
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Pass — large gray circle
        CircularActionButton(
            onClick = onPass,
            size = 56.dp,
            background = Color(0xFFF2F2F2),
            contentColor = MaterialTheme.colorScheme.onSurface,
            icon = Icons.Outlined.Close,
            contentDescription = stringResource(R.string.deck_action_pass),
            iconSize = 24.dp,
            elevation = 0.dp,
        )
        // Rewind — small amber outlined circle
        CircularActionButton(
            onClick = onRewind,
            size = 44.dp,
            background = Color.White,
            contentColor = Color(0xFFE7A05B),
            icon = Icons.Outlined.Replay,
            contentDescription = stringResource(R.string.deck_action_rewind),
            iconSize = 20.dp,
            borderColor = Color(0xFFE7A05B).copy(alpha = 0.6f),
            elevation = 0.dp,
        )
        // Boost — small coral outlined circle (disabled no-op)
        CircularActionButton(
            onClick = { /* deferred — see plan §2.6 */ },
            size = 44.dp,
            background = Color.White,
            contentColor = BrandColors.Coral.copy(alpha = 0.6f),
            icon = Icons.Outlined.AutoAwesome,
            contentDescription = stringResource(R.string.deck_action_boost),
            iconSize = 20.dp,
            borderColor = BrandColors.Coral.copy(alpha = 0.4f),
            enabled = false,
            elevation = 0.dp,
        )
        // Like — large coral filled circle
        CircularActionButton(
            onClick = onLike,
            size = 60.dp,
            background = BrandColors.Coral,
            contentColor = Color.White,
            icon = Icons.Filled.Favorite,
            contentDescription = stringResource(R.string.deck_action_like),
            iconSize = 28.dp,
            elevation = 6.dp,
        )
    }
}

@Composable
private fun CircularActionButton(
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp,
    background: Color,
    contentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    iconSize: androidx.compose.ui.unit.Dp,
    borderColor: Color? = null,
    enabled: Boolean = true,
    elevation: androidx.compose.ui.unit.Dp = 2.dp,
) {
    val baseModifier = Modifier.size(size)
    val shadowModifier = if (elevation > 0.dp) {
        baseModifier.shadow(elevation = elevation, shape = CircleShape, clip = false)
    } else {
        baseModifier
    }
    val finalModifier = (if (borderModifierNeeded(borderColor)) {
        shadowModifier.border(width = 1.5.dp, color = borderColor!!, shape = CircleShape)
    } else {
        shadowModifier
    })
        .background(color = background, shape = CircleShape)
        .clickable(enabled = enabled, onClick = onClick)
    Box(
        modifier = finalModifier,
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(iconSize),
        )
    }
}

private fun borderModifierNeeded(color: Color?): Boolean = color != null

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

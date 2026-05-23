package com.rodiz.arch2.feature.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.rodiz.arch2.core.designsystem.theme.BrandColors
import com.rodiz.arch2.core.designsystem.theme.TinPetTheme
import com.rodiz.arch2.feature.settings.domain.model.BlockedOwner

@Composable
internal fun BlockedUsersRoute(
    onBack: () -> Unit,
    viewModel: BlockedUsersViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    BlockedUsersScreen(
        state = state,
        onBack = onBack,
        onUnblock = viewModel::unblock,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
private fun BlockedUsersScreen(
    state: BlockedUsersUiState,
    onBack: () -> Unit,
    onUnblock: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    Scaffold(
        containerColor = BrandColors.Cream,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            BlockedHeader(onBack = onBack)

            when {
                state.isLoading -> BlockedLoading()
                state.blocked.isEmpty() -> BlockedEmptyState(modifier = Modifier.weight(1f))
                else -> BlockedList(
                    state = state,
                    onUnblock = onUnblock,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

// ----- Header --------------------------------------------------------------

@Composable
private fun BlockedHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        BackTile(onBack = onBack)
        Text(
            text = stringResource(R.string.blocked_title),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun BackTile(onBack: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier
            .size(44.dp)
            .clickable(onClick = onBack)
            .testTag("blocked_back"),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.settings_back_cd),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

// ----- Loading -------------------------------------------------------------

@Composable
private fun BlockedLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = BrandColors.CoralDeep)
    }
}

// ----- Empty state ---------------------------------------------------------

@Composable
private fun BlockedEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(BrandColors.CoralTint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Block,
                contentDescription = stringResource(R.string.blocked_chip_cd),
                tint = BrandColors.CoralDeep,
                modifier = Modifier.size(48.dp),
            )
        }

        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.blocked_empty_headline),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.blocked_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 300.dp),
        )

        Spacer(Modifier.weight(1.6f))
    }
}

// ----- Populated list -----------------------------------------------------

@Composable
private fun BlockedList(
    state: BlockedUsersUiState,
    onUnblock: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        items(state.blocked, key = { it.id }) { owner ->
            BlockedRow(
                owner = owner,
                isUnblocking = owner.id in state.pendingUnblock,
                onUnblock = { onUnblock(owner.id) },
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f),
                modifier = Modifier.padding(start = 72.dp),
            )
        }
    }
}

@Composable
private fun BlockedRow(
    owner: BlockedOwner,
    isUnblocking: Boolean,
    onUnblock: () -> Unit,
) {
    ListItem(
        colors = ListItemDefaults.colors(containerColor = BrandColors.Cream),
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                if (owner.avatarUrl != null) {
                    AsyncImage(
                        model = owner.avatarUrl,
                        contentDescription = owner.firstName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        headlineContent = {
            Text(
                text = owner.firstName,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        },
        trailingContent = {
            TextButton(onClick = onUnblock, enabled = !isUnblocking) {
                Text(
                    text = if (isUnblocking) "Unblocking…" else "Unblock",
                    color = BrandColors.CoralDeep,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
        },
    )
}

// ----- Previews ------------------------------------------------------------

@Preview(name = "Blocked owners — empty", showBackground = true, heightDp = 900)
@Composable
private fun BlockedUsersScreenEmptyPreview() {
    TinPetTheme {
        BlockedUsersScreen(
            state = BlockedUsersUiState(isLoading = false, blocked = emptyList()),
            onBack = {},
            onUnblock = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

@Preview(name = "Blocked owners — loading", showBackground = true, heightDp = 900)
@Composable
private fun BlockedUsersScreenLoadingPreview() {
    TinPetTheme {
        BlockedUsersScreen(
            state = BlockedUsersUiState(isLoading = true),
            onBack = {},
            onUnblock = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

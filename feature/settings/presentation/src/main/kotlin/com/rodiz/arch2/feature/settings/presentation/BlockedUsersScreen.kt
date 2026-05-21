package com.rodiz.arch2.feature.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.rodiz.arch2.core.designsystem.component.EmptyTabState
import com.rodiz.arch2.feature.settings.domain.model.BlockedOwner

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blocked users") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.blocked.isEmpty() -> {
                EmptyTabState(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    icon = Icons.Outlined.Block,
                    headline = "No blocked users",
                    body = "When you block someone from chat, they'll show up here so you can unblock them.",
                )
            }
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                    items(state.blocked, key = { it.id }) { owner ->
                        BlockedRow(
                            owner = owner,
                            isUnblocking = owner.id in state.pendingUnblock,
                            onUnblock = { viewModel.unblock(owner.id) },
                        )
                        HorizontalDivider()
                    }
                }
            }
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
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
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
        headlineContent = { Text(owner.firstName) },
        trailingContent = {
            TextButton(onClick = onUnblock, enabled = !isUnblocking) {
                Text(if (isUnblocking) "Unblocking…" else "Unblock")
            }
        },
    )
}

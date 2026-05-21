package com.rodiz.arch2.feature.settings.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PrivacyRoute(
    onBack: () -> Unit,
    onOpenBlockedUsers: () -> Unit,
    viewModel: PrivacyViewModel = hiltViewModel(),
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
                title = { Text("Privacy") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ListItem(
                leadingContent = { Icon(Icons.Outlined.PauseCircle, contentDescription = null) },
                headlineContent = { Text("Pause profile") },
                supportingContent = { Text("Hide your pets from other owners' decks.") },
                trailingContent = {
                    Switch(checked = state.paused, onCheckedChange = viewModel::onTogglePause)
                },
            )
            HorizontalDivider()
            ListItem(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenBlockedUsers),
                leadingContent = { Icon(Icons.Outlined.Block, contentDescription = null) },
                headlineContent = { Text("Blocked users") },
                supportingContent = { Text("See and unblock owners you've blocked.") },
                trailingContent = {
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null)
                },
            )
            HorizontalDivider()
        }
    }
}

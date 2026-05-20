package com.rodiz.arch2.feature.match.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rodiz.arch2.core.designsystem.component.EmptyTabState
import com.rodiz.arch2.feature.match.domain.model.MatchSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InboxRoute(
    onOpenMatch: (matchId: String) -> Unit,
    onGoToDeck: () -> Unit,
    viewModel: InboxViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Matches") }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val snap = state.snapshot
        val empty = snap.newMatches.isEmpty() && snap.conversations.isEmpty()
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (empty) {
                EmptyTabState(
                    icon = Icons.Outlined.Bolt,
                    headline = "No matches yet",
                    body = "When you and another owner both like each other's pets, your match shows up here.",
                    cta = "Go to Deck",
                    onCta = onGoToDeck,
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (snap.newMatches.isNotEmpty()) {
                        item { SectionHeader("New matches (${snap.newMatches.size})") }
                        items(snap.newMatches, key = { "new-${it.match.id.value}" }) { row ->
                            MatchRow(row = row, onClick = { onOpenMatch(row.match.id.value) })
                            HorizontalDivider()
                        }
                    }
                    if (snap.conversations.isNotEmpty()) {
                        item { SectionHeader("Conversations (${snap.conversations.size})") }
                        items(snap.conversations, key = { "conv-${it.match.id.value}" }) { row ->
                            ConversationRow(row = row, onClick = { onOpenMatch(row.match.id.value) })
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun MatchRow(row: MatchSummary, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(row.otherOwnerId.take(12) + "…") },
        supportingContent = { Text("Matched • say hello") },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ConversationRow(row: MatchSummary, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(row.otherOwnerId.take(12) + "…") },
        supportingContent = {
            Text(row.match.lastMessagePreview ?: "…", maxLines = 1)
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

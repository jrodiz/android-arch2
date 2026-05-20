package com.rodiz.arch2.feature.chat.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.HeartBroken
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rodiz.arch2.feature.chat.domain.model.Message
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
internal class ChatFactoryHolder @Inject constructor(
    val factory: ChatViewModel.Factory,
) : ViewModel()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatScreen(
    matchId: String,
    onBack: () -> Unit,
    onUnmatched: () -> Unit,
    holder: ChatFactoryHolder = hiltViewModel(),
) {
    val viewModel = remember(matchId) { holder.factory.create(matchId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showUnmatchDialog by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.clearError()
        }
    }
    LaunchedEffect(Unit) {
        viewModel.exited.collect { onUnmatched() }
    }
    LaunchedEffect(state.unmatched) {
        if (state.unmatched) onUnmatched()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Unmatch") },
                                leadingIcon = {
                                    Icon(Icons.Outlined.HeartBroken, contentDescription = null)
                                },
                                onClick = {
                                    menuOpen = false
                                    showUnmatchDialog = true
                                },
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Composer(
                draft = state.draft,
                isSending = state.isSending,
                onDraftChange = viewModel::draftChanged,
                onSend = viewModel::send,
            )
        },
    ) { padding ->
        MessageList(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            messages = state.messages,
            currentUid = state.currentUid,
        )
    }

    if (showUnmatchDialog) {
        AlertDialog(
            onDismissRequest = { showUnmatchDialog = false },
            title = { Text("Unmatch?") },
            text = { Text("This deletes your conversation for both of you. You won't see each other again.") },
            confirmButton = {
                TextButton(onClick = {
                    showUnmatchDialog = false
                    viewModel.unmatchAndExit()
                }) { Text("Unmatch") }
            },
            dismissButton = {
                TextButton(onClick = { showUnmatchDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun MessageList(
    modifier: Modifier,
    messages: List<Message>,
    currentUid: String,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }
    LazyColumn(
        state = listState,
        modifier = modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(messages, key = { it.id.value }) { msg ->
            MessageBubble(message = msg, isMine = msg.fromOwnerId == currentUid)
        }
    }
}

@Composable
private fun MessageBubble(message: Message, isMine: Boolean) {
    val arrangement = if (isMine) Arrangement.End else Arrangement.Start
    val bg = if (isMine) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (isMine) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = arrangement,
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = bg,
            modifier = Modifier.widthIn(max = 280.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                Text(text = message.text, color = fg, style = MaterialTheme.typography.bodyMedium)
                if (isMine) {
                    val readByOther = message.readBy.keys.any { it != message.fromOwnerId }
                    Text(
                        text = if (readByOther) "✓✓" else "✓",
                        color = if (readByOther) MaterialTheme.colorScheme.primary else fg.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.End),
                    )
                }
            }
        }
    }
}

@Composable
private fun Composer(
    draft: String,
    isSending: Boolean,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Surface(tonalElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                placeholder = { Text("Type a message") },
                maxLines = 4,
            )
            IconButton(
                onClick = onSend,
                enabled = !isSending && draft.isNotBlank(),
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primary),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Send,
                    contentDescription = "Send",
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

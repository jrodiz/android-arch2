package com.rodiz.arch2.feature.chat.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.HeartBroken
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.rodiz.arch2.core.designsystem.theme.BrandColors
import com.rodiz.arch2.core.ownerlookup.domain.OwnerDisplay
import com.rodiz.arch2.feature.chat.domain.model.Message
import com.rodiz.arch2.feature.chat.domain.model.ReportReason
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime

@HiltViewModel
internal class ChatFactoryHolder @Inject constructor(
    val factory: ChatViewModel.Factory,
) : ViewModel()

private val TopBarHeight = 72.dp
private val MessageMaxWidth = 280.dp
private val ActiveDotColor = Color(0xFF22C55E)

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
    var showBlockDialog by remember { mutableStateOf(false) }
    var showReportSheet by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.clearError()
        }
    }
    LaunchedEffect(state.reportSubmittedAtMillis) {
        if (state.reportSubmittedAtMillis != null) {
            snackbar.showSnackbar("Report submitted")
            viewModel.clearReportSubmitted()
        }
    }
    LaunchedEffect(Unit) {
        viewModel.exited.collect { onUnmatched() }
    }
    LaunchedEffect(state.unmatched) {
        if (state.unmatched) onUnmatched()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ChatTopBar(
                other = state.other,
                otherPetName = state.otherPet?.name,
                onBack = onBack,
                menuOpen = menuOpen,
                onMenuTap = { menuOpen = true },
                onMenuDismiss = { menuOpen = false },
                onUnmatch = {
                    menuOpen = false
                    showUnmatchDialog = true
                },
                onBlock = {
                    menuOpen = false
                    showBlockDialog = true
                },
                onReport = {
                    menuOpen = false
                    showReportSheet = true
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Composer(
                draft = state.draft,
                isSending = state.isSending,
                otherFirstName = state.other?.firstName,
                onDraftChange = viewModel::draftChanged,
                onSend = viewModel::send,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            state.match?.let { match ->
                MatchContextBanner(createdAt = match.createdAt)
            }
            MessageList(
                modifier = Modifier.fillMaxSize(),
                messages = state.messages,
                currentUid = state.currentUid,
            )
        }
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

    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            title = { Text("Block this person?") },
            text = {
                Text(
                    "Blocking deletes the conversation and stops their pets from showing up in your " +
                        "deck. You can unblock from Settings → Privacy → Blocked users.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showBlockDialog = false
                    viewModel.blockAndExit()
                }) { Text("Block") }
            },
            dismissButton = {
                TextButton(onClick = { showBlockDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showReportSheet) {
        ReportSheet(
            isSubmitting = state.isReporting,
            onDismiss = { showReportSheet = false },
            onSubmit = { reason, freeText ->
                showReportSheet = false
                viewModel.submitReport(reason, freeText)
            },
        )
    }
}

// -----------------------------------------------------------------------------
// Top bar — white, no shadow, square-rounded avatar with coral mini-badge,
// stacked title / presence row, sliders icon on the right.
// -----------------------------------------------------------------------------

@Composable
private fun ChatTopBar(
    other: OwnerDisplay?,
    otherPetName: String?,
    onBack: () -> Unit,
    menuOpen: Boolean,
    onMenuTap: () -> Unit,
    onMenuDismiss: () -> Unit,
    onUnmatch: () -> Unit,
    onBlock: () -> Unit,
    onReport: () -> Unit,
) {
    Surface(
        color = Color.White,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .heightIn(min = TopBarHeight)
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.chat_back_cd),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            HeaderAvatar(avatarUrl = other?.avatarUrl)
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)) {
                Text(
                    text = chatHeaderTitle(
                        ownerFirstName = other?.firstName,
                        petName = otherPetName,
                    ),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                Spacer(Modifier.size(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(ActiveDotColor),
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        // TODO presence + distance: static placeholder until we
                        // have a presence channel and shared-location flag.
                        text = stringResource(R.string.chat_active_now),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            Box {
                IconButton(onClick = onMenuTap) {
                    Icon(
                        imageVector = Icons.Outlined.Tune,
                        contentDescription = stringResource(R.string.chat_options_cd),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = onMenuDismiss) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.chat_options_unmatch)) },
                        leadingIcon = {
                            Icon(Icons.Outlined.HeartBroken, contentDescription = null)
                        },
                        onClick = onUnmatch,
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.chat_options_block)) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Block, contentDescription = null)
                        },
                        onClick = onBlock,
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.chat_options_report)) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Flag, contentDescription = null)
                        },
                        onClick = onReport,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderAvatar(avatarUrl: String?) {
    Box(modifier = Modifier.size(44.dp)) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (avatarUrl != null) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Pets,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        // Coral mini-badge — placeholder for current user's primary pet avatar.
        // TODO pet-mini: thread the current user's pet avatar through and render
        // it inside this badge instead of the paw glyph.
        BadgeOverlay()
    }
}

@Composable
private fun BoxScope.BadgeOverlay() {
    Box(
        modifier = Modifier
            .size(18.dp)
            .align(Alignment.BottomEnd)
            .offset(x = 4.dp, y = 4.dp)
            .clip(CircleShape)
            .background(BrandColors.Coral)
            .border(2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Pets,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(10.dp),
        )
    }
}

// -----------------------------------------------------------------------------
// Match-context banner — soft coral wash, heart roundel, "You matched on X".
// -----------------------------------------------------------------------------

@Composable
private fun MatchContextBanner(createdAt: Instant) {
    val phrase = formatMatchDay(createdAt)
    Surface(
        color = BrandColors.Coral.copy(alpha = 0.12f),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(BrandColors.Coral),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.size(14.dp))
            Column {
                Text(
                    text = stringResource(R.string.chat_banner_title_template, phrase),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = BrandColors.CoralDeep,
                )
                Spacer(Modifier.size(2.dp))
                Text(
                    text = stringResource(R.string.chat_banner_subtitle),
                    style = MaterialTheme.typography.labelMedium,
                    color = BrandColors.CoralDeep.copy(alpha = 0.85f),
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Message list — bubble-less, generous spacing, single "Today" separator.
// -----------------------------------------------------------------------------

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
    val tz = remember { TimeZone.currentSystemDefault() }
    val today = remember { Clock.System.now().toLocalDateTime(tz).date }
    // Index of the first message landing on the local "today" date. We surface
    // a single "Today" separator above it. Older messages flow continuously
    // without per-day buckets (intentional — see plan §2.5).
    val todayStartIndex = remember(messages, tz) {
        messages.indexOfFirst { it.createdAt.toLocalDateTime(tz).date == today }
    }
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        itemsIndexed(messages, key = { _, m -> m.id.value }) { index, msg ->
            if (todayStartIndex >= 0 && index == todayStartIndex) {
                DateSeparator(label = stringResource(R.string.chat_date_today))
            }
            MessageRow(message = msg, isMine = msg.fromOwnerId == currentUid, tz = tz)
        }
    }
}

@Composable
private fun DateSeparator(label: String) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun MessageRow(message: Message, isMine: Boolean, tz: TimeZone) {
    // Brand-coral for outgoing messages — readable on the cream background (AA Large
    // for body text, much better than the prior pure-white at ~1:1) while keeping
    // the brand differentiation between mine (coral, right-aligned) and theirs
    // (dark, left-aligned). Replaces the documented contrast compromise from the
    // chat-redesign-conversation.md §10 ghost-white fallback.
    val textColor = if (isMine) BrandColors.CoralDeep else MaterialTheme.colorScheme.onSurface
    val timestampColor = if (isMine) {
        BrandColors.CoralDeep.copy(alpha = 0.75f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
    ) {
        Text(
            text = message.text,
            color = textColor,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = if (isMine) TextAlign.End else TextAlign.Start,
            modifier = Modifier.widthIn(max = MessageMaxWidth),
        )
        Spacer(Modifier.size(4.dp))
        Text(
            text = formatTime(message.createdAt, tz),
            color = timestampColor,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

// -----------------------------------------------------------------------------
// Composer — white rounded pill (placeholder + emoji icon) + coral send circle.
// -----------------------------------------------------------------------------

@Composable
private fun Composer(
    draft: String,
    isSending: Boolean,
    otherFirstName: String?,
    onDraftChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    val placeholder = if (!otherFirstName.isNullOrBlank()) {
        stringResource(R.string.chat_composer_placeholder_template, otherFirstName)
    } else {
        stringResource(R.string.chat_composer_placeholder_default)
    }
    val canSend = draft.isNotBlank() && !isSending
    val sendBg = if (canSend) BrandColors.Coral else BrandColors.Coral.copy(alpha = 0.5f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(28.dp),
            shadowElevation = 0.dp,
            tonalElevation = 0.dp,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 52.dp),
        ) {
            Row(
                modifier = Modifier.padding(start = 18.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    BasicTextField(
                        value = draft,
                        onValueChange = onDraftChange,
                        textStyle = LocalTextStyle.current.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                        ),
                        cursorBrush = SolidColor(BrandColors.Coral),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 14.dp),
                        decorationBox = { inner ->
                            if (draft.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                )
                            }
                            inner()
                        },
                    )
                }
                IconButton(
                    onClick = { /* TODO emoji picker — decorative for now */ },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SentimentSatisfied,
                        contentDescription = stringResource(R.string.chat_emoji_cd),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Spacer(Modifier.size(10.dp))
        Surface(
            shape = CircleShape,
            color = sendBg,
            shadowElevation = 0.dp,
            modifier = Modifier
                .size(52.dp)
                .clickable(enabled = canSend, onClick = onSend),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Send,
                    contentDescription = stringResource(R.string.chat_send_cd),
                    tint = Color.White,
                    modifier = Modifier
                        .size(22.dp)
                        .padding(start = 2.dp), // optical centering of the plane icon
                )
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Report sheet — unchanged from the previous design (out of scope).
// -----------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportSheet(
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (ReportReason, String?) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedReason by remember { mutableStateOf<ReportReason?>(null) }
    var freeText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Report this person",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "Pick a reason. Reports go to our moderation team.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ReportReason.entries.forEach { reason ->
                    FilterChip(
                        selected = selectedReason == reason,
                        onClick = { selectedReason = reason },
                        label = { Text(reason.label()) },
                    )
                }
            }
            OutlinedTextField(
                value = freeText,
                onValueChange = { if (it.length <= 500) freeText = it },
                placeholder = { Text("Add details (optional)") },
                supportingText = { Text("${freeText.length}/500") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )
            Button(
                onClick = {
                    val reason = selectedReason ?: return@Button
                    onSubmit(reason, freeText.takeIf { it.isNotBlank() })
                },
                enabled = selectedReason != null && !isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isSubmitting) "Submitting…" else "Submit")
            }
            Spacer(Modifier.size(8.dp))
        }
    }
}

private fun ReportReason.label(): String = when (this) {
    ReportReason.SPAM -> "Spam"
    ReportReason.FAKE_PROFILE -> "Fake profile"
    ReportReason.HARASSMENT -> "Harassment"
    ReportReason.ANIMAL_WELFARE_CONCERN -> "Animal welfare concern"
    ReportReason.OTHER -> "Other"
}

// -----------------------------------------------------------------------------
// Date / time helpers — kotlinx.datetime so the JVM domain stays clean.
// -----------------------------------------------------------------------------

/**
 * Header title combining the other owner and pet — "Leah & Mochi" when both are
 * resolved, owner-only for pre-plumbing matches, pet-only as a backstop, and
 * "Chat" when nothing has loaded yet. Same pattern as MatchSummary.displayTitle()
 * on the inbox row so the two surfaces stay in sync.
 */
private fun chatHeaderTitle(ownerFirstName: String?, petName: String?): String {
    val owner = ownerFirstName?.takeIf { it.isNotBlank() }
    val pet = petName?.takeIf { it.isNotBlank() }
    return when {
        owner != null && pet != null -> "$owner & $pet"
        owner != null -> owner
        pet != null -> pet
        else -> "Chat"
    }
}

/**
 * Returns the trailing portion of "You matched ___" — e.g. "today", "yesterday",
 * "on tuesday", or "on May 12". Uses the device's local timezone.
 */
private fun formatMatchDay(createdAt: Instant): String {
    val tz = TimeZone.currentSystemDefault()
    val today = Clock.System.now().toLocalDateTime(tz).date
    val createdDate = createdAt.toLocalDateTime(tz).date
    val daysAgo = createdDate.daysUntil(today)
    return when {
        daysAgo == 0 -> "today"
        daysAgo == 1 -> "yesterday"
        daysAgo in 2..6 -> "on ${createdDate.dayOfWeek.titlecased()}"
        else -> "on ${createdDate.monthAbbrev()} ${createdDate.dayOfMonth}"
    }
}

private fun DayOfWeek.titlecased(): String =
    name.lowercase().replaceFirstChar { it.titlecase() }

private fun LocalDate.monthAbbrev(): String =
    month.name.take(3).lowercase().replaceFirstChar { it.titlecase() }

/** "HH:mm" in the device's local time. */
private fun formatTime(instant: Instant, tz: TimeZone): String {
    val ldt = instant.toLocalDateTime(tz)
    val hh = ldt.hour.toString().padStart(2, '0')
    val mm = ldt.minute.toString().padStart(2, '0')
    return "$hh:$mm"
}


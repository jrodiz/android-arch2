package com.rodiz.arch2.feature.settings.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.PauseCircleOutline
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rodiz.arch2.core.designsystem.theme.BrandColors
import com.rodiz.arch2.core.designsystem.theme.TinPetTheme

@Composable
internal fun PrivacyRoute(
    onBack: () -> Unit,
    onOpenBlockedOwners: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onAccountDeleted: () -> Unit,
    viewModel: PrivacyViewModel = hiltViewModel(),
    deleteSheetViewModel: DeleteAccountSheetViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val deleteState by deleteSheetViewModel.state.collectAsStateWithLifecycle()
    val exportStatus by viewModel.exportStatus.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val comingSoonMessage = stringResource(R.string.privacy_coming_soon)
    val exportStartedMsg = stringResource(R.string.privacy_export_started)
    val exportReadyMsg = stringResource(R.string.privacy_export_ready)
    val exportOpenLabel = stringResource(R.string.privacy_export_open)
    val exportFailedMsg = stringResource(R.string.privacy_export_failed)
    val context = androidx.compose.ui.platform.LocalContext.current
    var sheetOpen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    LaunchedEffect(state.transientMessage) {
        state.transientMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearTransient()
        }
    }
    LaunchedEffect(exportStatus) {
        when (val status = exportStatus) {
            is com.rodiz.arch2.feature.settings.domain.model.DataExportStatus.Ready -> {
                val result = snackbarHostState.showSnackbar(
                    message = exportReadyMsg,
                    actionLabel = exportOpenLabel,
                    duration = androidx.compose.material3.SnackbarDuration.Long,
                )
                if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                    runCatching {
                        context.startActivity(
                            android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse(status.downloadUrl),
                            ),
                        )
                    }
                }
                viewModel.onExportAcknowledged()
            }
            is com.rodiz.arch2.feature.settings.domain.model.DataExportStatus.Failed -> {
                snackbarHostState.showSnackbar(status.reason ?: exportFailedMsg)
                viewModel.onExportAcknowledged()
            }
            else -> Unit
        }
    }
    val deleteErrorMessage = deleteState.errorRes?.let { stringResource(it) }
    LaunchedEffect(deleteErrorMessage) {
        deleteErrorMessage?.let {
            // Dismiss the sheet so the snackbar isn't hidden underneath it.
            sheetOpen = false
            snackbarHostState.showSnackbar(it)
            deleteSheetViewModel.onErrorShown()
        }
    }
    LaunchedEffect(deleteState.completed) {
        if (deleteState.completed) {
            sheetOpen = false
            deleteSheetViewModel.onCompletedHandled()
            onAccountDeleted()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BrandColors.Cream,
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = BrandColors.CoralDeep)
            }
            return@Scaffold
        }
        PrivacyContent(
            paddingValues = padding,
            paused = state.paused,
            blockedCount = state.blockedCount,
            deletionDaysRemaining = deleteState.pendingDeletion?.let { deleteState.daysRemaining.toInt() },
            isExportPending = exportStatus is com.rodiz.arch2.feature.settings.domain.model.DataExportStatus.Pending,
            onBack = onBack,
            onTogglePause = viewModel::onTogglePause,
            onOpenBlockedOwners = onOpenBlockedOwners,
            onOpenDeleteAccount = {
                deleteSheetViewModel.onSheetOpen()
                sheetOpen = true
            },
            onDownloadData = {
                viewModel.onRequestDataExport()
                viewModel.notifyComingSoon(exportStartedMsg)
            },
            onOpenPrivacyPolicy = onOpenPrivacyPolicy,
            onComingSoon = { viewModel.notifyComingSoon(comingSoonMessage) },
        )

        if (sheetOpen) {
            DeleteAccountSheet(
                state = deleteState,
                onTypedChanged = deleteSheetViewModel::onTypedChanged,
                onConfirm = deleteSheetViewModel::onConfirmDelete,
                onCancelDeletion = deleteSheetViewModel::onCancelDeletion,
                onDismiss = {
                    sheetOpen = false
                    deleteSheetViewModel.onDismiss()
                },
            )
        }
    }
}

@Composable
private fun PrivacyContent(
    paddingValues: androidx.compose.foundation.layout.PaddingValues,
    paused: Boolean,
    blockedCount: Int,
    deletionDaysRemaining: Int?,
    isExportPending: Boolean,
    onBack: () -> Unit,
    onTogglePause: (Boolean) -> Unit,
    onOpenBlockedOwners: () -> Unit,
    onOpenDeleteAccount: () -> Unit,
    onDownloadData: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onComingSoon: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .windowInsetsPadding(WindowInsets.statusBars)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        PrivacyHeader(onBack = onBack)

        // Top card: Pause + Blocked owners (no eyebrow, matches the mock).
        SettingsCard {
            PauseRow(
                paused = paused,
                onTogglePause = onTogglePause,
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Outlined.Block,
                iconBackground = BrandColors.CoralTint,
                iconTint = BrandColors.CoralDeep,
                title = stringResource(R.string.settings_row_blocked_title),
                subtitle = pluralStringResource(
                    id = R.plurals.settings_row_blocked_subtitle,
                    count = blockedCount,
                    blockedCount,
                ),
                titleColor = MaterialTheme.colorScheme.onSurface,
                onClick = onOpenBlockedOwners,
                trailing = { ChevronTrailing() },
                testTag = "privacy_row_blocked",
            )
        }

        SectionEyebrow(text = stringResource(R.string.privacy_section_data))
        SettingsCard {
            SettingsRow(
                icon = Icons.Outlined.MailOutline,
                iconBackground = BrandColors.MintTint,
                iconTint = BrandColors.MintLeaf,
                title = stringResource(R.string.privacy_download_title),
                subtitle = if (isExportPending) {
                    stringResource(R.string.privacy_download_subtitle_pending)
                } else {
                    stringResource(R.string.privacy_download_subtitle)
                },
                titleColor = MaterialTheme.colorScheme.onSurface,
                onClick = onDownloadData,
                trailing = {
                    if (isExportPending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = BrandColors.CoralDeep,
                        )
                    } else {
                        ChevronTrailing()
                    }
                },
                testTag = "privacy_row_download",
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Outlined.Shield,
                iconBackground = BrandColors.LavenderTint,
                iconTint = BrandColors.LavenderInk,
                title = stringResource(R.string.privacy_policy_title),
                subtitle = stringResource(R.string.privacy_policy_subtitle),
                titleColor = MaterialTheme.colorScheme.onSurface,
                onClick = onOpenPrivacyPolicy,
                trailing = { ChevronTrailing() },
                testTag = "privacy_row_policy",
            )
        }

        SectionEyebrow(text = stringResource(R.string.privacy_section_danger_zone))
        SettingsCard(
            border = BorderStroke(1.dp, BrandColors.CoralDeep.copy(alpha = 0.5f)),
            elevation = 0.dp,
        ) {
            SettingsRow(
                icon = Icons.Outlined.Block,
                iconBackground = BrandColors.CoralTint,
                iconTint = BrandColors.CoralDeep,
                title = stringResource(R.string.privacy_delete_title),
                subtitle = stringResource(R.string.privacy_delete_subtitle),
                titleColor = MaterialTheme.colorScheme.error,
                onClick = onOpenDeleteAccount,
                trailing = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (deletionDaysRemaining != null) {
                            ScheduledPill(daysRemaining = deletionDaysRemaining)
                        }
                        ChevronTrailing()
                    }
                },
                testTag = "privacy_row_delete",
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}

// ----- Header --------------------------------------------------------------

@Composable
private fun PrivacyHeader(onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp,
            modifier = Modifier
                .size(44.dp)
                .clickable(onClick = onBack)
                .testTag("privacy_back"),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.privacy_back_cd),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
        Text(
            text = stringResource(R.string.privacy_title),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ----- Section eyebrow + card ----------------------------------------------

@Composable
private fun SectionEyebrow(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.5.sp,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 6.dp, bottom = 0.dp),
    )
}

@Composable
private fun SettingsCard(
    border: BorderStroke? = null,
    elevation: androidx.compose.ui.unit.Dp = 1.dp,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = elevation,
        border = border,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column { content() }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f),
        modifier = Modifier.padding(start = 72.dp),
    )
}

// ----- Rows ----------------------------------------------------------------

@Composable
private fun SettingsRow(
    icon: ImageVector,
    iconBackground: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    titleColor: Color,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit,
    testTag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconTile(icon = icon, background = iconBackground, tint = iconTint)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        trailing()
    }
}

@Composable
private fun PauseRow(
    paused: Boolean,
    onTogglePause: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = paused,
                role = Role.Switch,
                onValueChange = onTogglePause,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("privacy_row_pause"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconTile(
            icon = Icons.Outlined.PauseCircleOutline,
            background = BrandColors.NeutralTint,
            tint = BrandColors.NeutralInk,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.privacy_pause_title),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.privacy_pause_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Switch(
            checked = paused,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = BrandColors.CoralDeep,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = MaterialTheme.colorScheme
                    .onSurfaceVariant.copy(alpha = 0.35f),
                uncheckedBorderColor = Color.Transparent,
            ),
            modifier = Modifier.testTag("privacy_pause_switch"),
        )
    }
}

@Composable
private fun IconTile(
    icon: ImageVector,
    background: Color,
    tint: Color,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun ChevronTrailing() {
    Icon(
        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ScheduledPill(daysRemaining: Int) {
    Surface(
        shape = RoundedCornerShape(50),
        color = BrandColors.CoralTint,
    ) {
        Text(
            text = stringResource(R.string.delete_sheet_pill_format, daysRemaining),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = BrandColors.CoralDeep,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

// ----- Previews ------------------------------------------------------------

@Preview(name = "Privacy — default", showBackground = true, heightDp = 1100)
@Composable
private fun PrivacyScreenPreviewDefault() {
    TinPetTheme {
        Scaffold(containerColor = BrandColors.Cream) { padding ->
            PrivacyContent(
                paddingValues = padding,
                paused = false,
                blockedCount = 0,
                deletionDaysRemaining = null,
                isExportPending = false,
                onBack = {},
                onTogglePause = {},
                onOpenBlockedOwners = {},
                onOpenDeleteAccount = {},
                onDownloadData = {},
                onOpenPrivacyPolicy = {},
                onComingSoon = {},
            )
        }
    }
}

@Preview(name = "Privacy — paused + blocked", showBackground = true, heightDp = 1100)
@Composable
private fun PrivacyScreenPreviewPopulated() {
    TinPetTheme {
        Scaffold(containerColor = BrandColors.Cream) { padding ->
            PrivacyContent(
                paddingValues = padding,
                paused = true,
                blockedCount = 3,
                deletionDaysRemaining = 27,
                isExportPending = false,
                onBack = {},
                onTogglePause = {},
                onOpenBlockedOwners = {},
                onOpenDeleteAccount = {},
                onDownloadData = {},
                onOpenPrivacyPolicy = {},
                onComingSoon = {},
            )
        }
    }
}

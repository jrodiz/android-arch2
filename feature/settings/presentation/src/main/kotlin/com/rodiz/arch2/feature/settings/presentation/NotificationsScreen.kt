package com.rodiz.arch2.feature.settings.presentation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rodiz.arch2.core.designsystem.theme.BrandColors
import com.rodiz.arch2.core.designsystem.theme.TinPetTheme
import com.rodiz.arch2.feature.settings.domain.model.NotificationPrefs

@Composable
internal fun NotificationsRoute(
    onBack: () -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var pickerOpen by remember { mutableStateOf(false) }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    NotificationsScaffold(
        prefs = state.prefs,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onToggleNewMatch = viewModel::onToggleNewMatch,
        onToggleNewMessage = viewModel::onToggleNewMessage,
        onToggleSomeoneLiked = viewModel::onToggleSomeoneLiked,
        onToggleWeeklyDigest = viewModel::onToggleWeeklyDigest,
        onToggleQuietHours = viewModel::onToggleQuietHoursEnabled,
        onPickQuietHours = { pickerOpen = true },
    )

    if (pickerOpen) {
        QuietHoursPickerDialog(
            initialStartMinutes = state.prefs.quietHoursStartMinutes,
            initialEndMinutes = state.prefs.quietHoursEndMinutes,
            onDismiss = { pickerOpen = false },
            onSave = { start, end ->
                viewModel.onQuietHoursChanged(start, end)
                pickerOpen = false
            },
        )
    }
}

@Composable
private fun NotificationsScaffold(
    prefs: NotificationPrefs,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onToggleNewMatch: (Boolean) -> Unit,
    onToggleNewMessage: (Boolean) -> Unit,
    onToggleSomeoneLiked: (Boolean) -> Unit,
    onToggleWeeklyDigest: (Boolean) -> Unit,
    onToggleQuietHours: (Boolean) -> Unit,
    onPickQuietHours: () -> Unit,
) {
    val context = LocalContext.current
    val granted by rememberPushPermissionGranted()
    val openSystemSettings: () -> Unit = remember(context) {
        {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
            context.startActivity(intent)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BrandColors.Cream,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .windowInsetsPadding(WindowInsets.statusBars)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            NotificationsHeader(onBack = onBack)

            PushPermissionBanner(
                granted = granted,
                onTurnOn = openSystemSettings,
            )

            SectionEyebrow(stringResource(R.string.notifications_section_activity))

            SettingsCard {
                NotificationToggleRow(
                    icon = Icons.Outlined.FavoriteBorder,
                    iconBackground = BrandColors.CoralTint,
                    iconTint = BrandColors.CoralDeep,
                    title = stringResource(R.string.notifications_row_new_match_title),
                    subtitle = stringResource(R.string.notifications_row_new_match_subtitle),
                    checked = prefs.newMatch,
                    onCheckedChange = onToggleNewMatch,
                    testTag = "notifications_row_new_match",
                )
                RowDivider()
                NotificationToggleRow(
                    icon = Icons.AutoMirrored.Outlined.Message,
                    iconBackground = BrandColors.LavenderTint,
                    iconTint = BrandColors.LavenderInk,
                    title = stringResource(R.string.notifications_row_new_message_title),
                    subtitle = stringResource(R.string.notifications_row_new_message_subtitle),
                    checked = prefs.newMessage,
                    onCheckedChange = onToggleNewMessage,
                    testTag = "notifications_row_new_message",
                )
                RowDivider()
                NotificationToggleRow(
                    icon = Icons.Outlined.AutoAwesome,
                    iconBackground = BrandColors.PeachTint,
                    iconTint = BrandColors.PeachInk,
                    title = stringResource(R.string.notifications_row_liked_title),
                    subtitle = stringResource(R.string.notifications_row_liked_subtitle),
                    checked = prefs.someoneLiked,
                    onCheckedChange = onToggleSomeoneLiked,
                    testTag = "notifications_row_liked",
                )
                RowDivider()
                NotificationToggleRow(
                    icon = Icons.Outlined.Notifications,
                    iconBackground = BrandColors.MintTint,
                    iconTint = BrandColors.MintLeaf,
                    title = stringResource(R.string.notifications_row_digest_title),
                    subtitle = stringResource(R.string.notifications_row_digest_subtitle),
                    checked = prefs.weeklyDigest,
                    onCheckedChange = onToggleWeeklyDigest,
                    testTag = "notifications_row_digest",
                )
            }

            SectionEyebrow(stringResource(R.string.notifications_section_quiet_hours))

            SettingsCard {
                QuietHoursRow(
                    startMinutes = prefs.quietHoursStartMinutes,
                    endMinutes = prefs.quietHoursEndMinutes,
                    checked = prefs.quietHoursEnabled,
                    onCheckedChange = onToggleQuietHours,
                    onRowClick = onPickQuietHours,
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ----- Header --------------------------------------------------------------

@Composable
private fun NotificationsHeader(onBack: () -> Unit) {
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
                .testTag("notifications_back"),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.notifications_back_cd),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
        Text(
            text = stringResource(R.string.notifications_title),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ----- Banner --------------------------------------------------------------

@Composable
private fun PushPermissionBanner(
    granted: Boolean,
    onTurnOn: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = BrandColors.Coral.copy(alpha = 0.20f),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (granted) Modifier else Modifier.clickable(onClick = onTurnOn),
            )
            .testTag("notifications_banner"),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BrandColors.Coral),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = stringResource(R.string.notifications_banner_icon_cd),
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        if (granted) R.string.notifications_banner_on_title
                        else R.string.notifications_banner_off_title,
                    ),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = BrandColors.CoralDeep,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(
                        if (granted) R.string.notifications_banner_on_subtitle
                        else R.string.notifications_banner_off_subtitle,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandColors.CoralDeep.copy(alpha = 0.75f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!granted) {
                Surface(
                    shape = CircleShape,
                    color = BrandColors.CoralDeep,
                    modifier = Modifier.testTag("notifications_banner_turn_on"),
                ) {
                    Text(
                        text = stringResource(R.string.notifications_banner_turn_on),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

// ----- Sections + cards (mirror SettingsHomeScreen) -----------------------

@Composable
private fun SectionEyebrow(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.5.sp,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 6.dp, bottom = 0.dp, top = 0.dp),
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
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
private fun NotificationToggleRow(
    icon: ImageVector,
    iconBackground: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
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
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = brandedSwitchColors(),
        )
    }
}

@Composable
private fun QuietHoursRow(
    startMinutes: Int,
    endMinutes: Int,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onRowClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onRowClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag("notifications_row_quiet_hours"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BrandColors.MoonTint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Bedtime,
                contentDescription = null,
                tint = BrandColors.MoonInk,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(
                    R.string.notifications_quiet_hours_window_template,
                    formatMinutesAsHHmm(startMinutes),
                    formatMinutesAsHHmm(endMinutes),
                ),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.notifications_quiet_hours_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = brandedSwitchColors(),
            modifier = Modifier.testTag("notifications_quiet_hours_switch"),
        )
    }
}

@Composable
private fun brandedSwitchColors(): SwitchColors = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = BrandColors.CoralDeep,
    uncheckedThumbColor = Color.White,
    uncheckedTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
    uncheckedBorderColor = Color.Transparent,
)

private fun formatMinutesAsHHmm(minutes: Int): String {
    val safe = minutes.coerceIn(0, 24 * 60 - 1)
    val h = safe / 60
    val m = safe % 60
    return "%02d:%02d".format(h, m)
}

// ----- Permission state ----------------------------------------------------

@Composable
private fun rememberPushPermissionGranted(): State<Boolean> {
    val context = LocalContext.current
    val granted = remember { mutableStateOf(checkPushGranted(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                granted.value = checkPushGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return granted
}

private fun checkPushGranted(context: Context): Boolean =
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        true
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

// ----- Previews ------------------------------------------------------------

@Preview(name = "Notifications — granted", showBackground = true, heightDp = 1100)
@Composable
private fun NotificationsScreenPreviewGranted() {
    TinPetTheme {
        NotificationsPreviewScaffold(
            prefs = NotificationPrefs.DEFAULT.copy(quietHoursEnabled = true),
            granted = true,
        )
    }
}

@Preview(name = "Notifications — denied", showBackground = true, heightDp = 1100)
@Composable
private fun NotificationsScreenPreviewDenied() {
    TinPetTheme {
        NotificationsPreviewScaffold(
            prefs = NotificationPrefs.DEFAULT,
            granted = false,
        )
    }
}

@Composable
private fun NotificationsPreviewScaffold(prefs: NotificationPrefs, granted: Boolean) {
    Scaffold(containerColor = BrandColors.Cream) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            NotificationsHeader(onBack = {})
            PushPermissionBanner(granted = granted, onTurnOn = {})
            SectionEyebrow(stringResource(R.string.notifications_section_activity))
            SettingsCard {
                NotificationToggleRow(
                    icon = Icons.Outlined.FavoriteBorder,
                    iconBackground = BrandColors.CoralTint,
                    iconTint = BrandColors.CoralDeep,
                    title = stringResource(R.string.notifications_row_new_match_title),
                    subtitle = stringResource(R.string.notifications_row_new_match_subtitle),
                    checked = prefs.newMatch,
                    onCheckedChange = {},
                    testTag = "preview_new_match",
                )
                RowDivider()
                NotificationToggleRow(
                    icon = Icons.AutoMirrored.Outlined.Message,
                    iconBackground = BrandColors.LavenderTint,
                    iconTint = BrandColors.LavenderInk,
                    title = stringResource(R.string.notifications_row_new_message_title),
                    subtitle = stringResource(R.string.notifications_row_new_message_subtitle),
                    checked = prefs.newMessage,
                    onCheckedChange = {},
                    testTag = "preview_new_message",
                )
                RowDivider()
                NotificationToggleRow(
                    icon = Icons.Outlined.AutoAwesome,
                    iconBackground = BrandColors.PeachTint,
                    iconTint = BrandColors.PeachInk,
                    title = stringResource(R.string.notifications_row_liked_title),
                    subtitle = stringResource(R.string.notifications_row_liked_subtitle),
                    checked = prefs.someoneLiked,
                    onCheckedChange = {},
                    testTag = "preview_liked",
                )
                RowDivider()
                NotificationToggleRow(
                    icon = Icons.Outlined.Notifications,
                    iconBackground = BrandColors.MintTint,
                    iconTint = BrandColors.MintLeaf,
                    title = stringResource(R.string.notifications_row_digest_title),
                    subtitle = stringResource(R.string.notifications_row_digest_subtitle),
                    checked = prefs.weeklyDigest,
                    onCheckedChange = {},
                    testTag = "preview_digest",
                )
            }
            SectionEyebrow(stringResource(R.string.notifications_section_quiet_hours))
            SettingsCard {
                QuietHoursRow(
                    startMinutes = prefs.quietHoursStartMinutes,
                    endMinutes = prefs.quietHoursEndMinutes,
                    checked = prefs.quietHoursEnabled,
                    onCheckedChange = {},
                    onRowClick = {},
                )
            }
        }
    }
}

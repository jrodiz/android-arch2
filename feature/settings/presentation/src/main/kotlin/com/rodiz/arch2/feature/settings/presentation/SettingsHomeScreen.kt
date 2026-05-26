package com.rodiz.arch2.feature.settings.presentation

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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PauseCircleOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Tune
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rodiz.arch2.core.designsystem.theme.BrandColors
import com.rodiz.arch2.core.designsystem.theme.TinPetTheme

@Composable
internal fun SettingsHomeRoute(
    onBack: () -> Unit,
    onOpenEditProfile: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenFilters: () -> Unit,
    onOpenFeaturedPets: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenBlockedOwners: () -> Unit,
    onSignedOut: () -> Unit,
    viewModel: SettingsHomeViewModel = hiltViewModel(),
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
            SettingsHeader(onBack = onBack)

            SettingsSection(title = stringResource(R.string.settings_section_account)) {
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Outlined.Person,
                        iconBackground = BrandColors.MintTint,
                        iconTint = BrandColors.MintLeaf,
                        title = stringResource(R.string.settings_row_profile_title),
                        subtitle = stringResource(R.string.settings_row_profile_subtitle),
                        onClick = onOpenEditProfile,
                        trailing = { ChevronTrailing() },
                        testTag = "settings_row_profile",
                    )
                }
            }

            SettingsSection(title = stringResource(R.string.settings_section_matching)) {
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Outlined.Tune,
                        iconBackground = BrandColors.CoralTint,
                        iconTint = BrandColors.CoralDeep,
                        title = stringResource(R.string.settings_row_filters_title),
                        subtitle = stringResource(R.string.settings_row_filters_subtitle),
                        onClick = onOpenFilters,
                        trailing = { ChevronTrailing() },
                        testTag = "settings_row_filters",
                    )
                    RowDivider()
                    SettingsRow(
                        icon = Icons.Outlined.PushPin,
                        iconBackground = BrandColors.CoralTint,
                        iconTint = BrandColors.CoralDeep,
                        title = stringResource(R.string.settings_row_featured_title),
                        subtitle = stringResource(R.string.settings_row_featured_subtitle),
                        onClick = onOpenFeaturedPets,
                        trailing = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                if (state.featuredCount > 0) {
                                    CountPill(count = state.featuredCount)
                                }
                                ChevronTrailing()
                            }
                        },
                        testTag = "settings_row_featured",
                    )
                    RowDivider()
                    SettingsRow(
                        icon = Icons.Outlined.Notifications,
                        iconBackground = BrandColors.LavenderTint,
                        iconTint = BrandColors.LavenderInk,
                        title = stringResource(R.string.settings_row_notifications_title),
                        subtitle = stringResource(R.string.settings_row_notifications_subtitle),
                        onClick = onOpenNotifications,
                        trailing = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                if (state.notifEnabledCount > 0) {
                                    CountPill(count = state.notifEnabledCount)
                                }
                                ChevronTrailing()
                            }
                        },
                        testTag = "settings_row_notifications",
                    )
                    RowDivider()
                    SettingsRow(
                        icon = Icons.Outlined.PauseCircleOutline,
                        iconBackground = BrandColors.NeutralTint,
                        iconTint = BrandColors.NeutralInk,
                        title = stringResource(R.string.settings_row_pause_title),
                        subtitle = stringResource(R.string.settings_row_pause_subtitle),
                        onClick = onOpenPrivacy,
                        trailing = {
                            Switch(
                                checked = state.paused,
                                onCheckedChange = viewModel::onTogglePause,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = BrandColors.CoralDeep,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = MaterialTheme.colorScheme
                                        .onSurfaceVariant.copy(alpha = 0.35f),
                                    uncheckedBorderColor = Color.Transparent,
                                ),
                                modifier = Modifier.testTag("settings_pause_switch"),
                            )
                        },
                        testTag = "settings_row_pause",
                    )
                }
            }

            SettingsSection(title = stringResource(R.string.settings_section_safety)) {
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Outlined.Shield,
                        iconBackground = BrandColors.MintTint,
                        iconTint = BrandColors.MintLeaf,
                        title = stringResource(R.string.settings_row_privacy_title),
                        subtitle = stringResource(R.string.settings_row_privacy_subtitle),
                        onClick = onOpenPrivacy,
                        trailing = { ChevronTrailing() },
                        testTag = "settings_row_privacy",
                    )
                    RowDivider()
                    SettingsRow(
                        icon = Icons.Outlined.Block,
                        iconBackground = BrandColors.CoralTint,
                        iconTint = BrandColors.CoralDeep,
                        title = stringResource(R.string.settings_row_blocked_title),
                        subtitle = pluralStringResource(
                            id = R.plurals.settings_row_blocked_subtitle,
                            count = state.blockedCount,
                            state.blockedCount,
                        ),
                        onClick = onOpenBlockedOwners,
                        trailing = { ChevronTrailing() },
                        testTag = "settings_row_blocked",
                    )
                }
            }

            SignOutPill(onClick = { viewModel.signOut(onSignedOut) })

            VersionFooter()

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ----- Header ---------------------------------------------------------------

@Composable
private fun SettingsHeader(onBack: () -> Unit) {
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
                .testTag("settings_back"),
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
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.ExtraBold,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ----- Sections + cards ----------------------------------------------------

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp, bottom = 10.dp),
        )
        content()
    }
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

// ----- Row -----------------------------------------------------------------

@Composable
private fun SettingsRow(
    icon: ImageVector,
    iconBackground: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        trailing()
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
private fun CountPill(count: Int) {
    Surface(
        shape = CircleShape,
        color = BrandColors.CoralDeep,
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
            ),
            color = Color.White,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

// ----- Sign out + footer ---------------------------------------------------

@Composable
private fun SignOutPill(onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("settings_sign_out"),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Logout,
                contentDescription = null,
                tint = BrandColors.CoralDeep,
            )
            Spacer(Modifier.size(12.dp))
            Text(
                text = stringResource(R.string.settings_sign_out),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = BrandColors.CoralDeep,
            )
        }
    }
}

@Composable
private fun VersionFooter() {
    val version = BuildConfig.VERSION_NAME.ifBlank { "–" }
    Text(
        text = stringResource(R.string.settings_footer_template, version),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
    )
}

// ----- Previews ------------------------------------------------------------

@Preview(name = "Settings — populated", showBackground = true, heightDp = 1100)
@Composable
private fun SettingsHomeScreenPreviewPopulated() {
    TinPetTheme {
        SettingsHomeScaffoldPreview(
            notifEnabledCount = 4,
            blockedCount = 3,
            paused = false,
        )
    }
}

@Preview(name = "Settings — zero state", showBackground = true, heightDp = 1100)
@Composable
private fun SettingsHomeScreenPreviewZero() {
    TinPetTheme {
        SettingsHomeScaffoldPreview(
            notifEnabledCount = 0,
            blockedCount = 0,
            paused = false,
        )
    }
}

@Composable
private fun SettingsHomeScaffoldPreview(
    notifEnabledCount: Int,
    blockedCount: Int,
    paused: Boolean,
) {
    // Minimal stand-in for SettingsHomeRoute that lets previews render without Hilt.
    Scaffold(containerColor = BrandColors.Cream) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SettingsHeader(onBack = {})

            SettingsSection(title = stringResource(R.string.settings_section_account)) {
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Outlined.Person,
                        iconBackground = BrandColors.MintTint,
                        iconTint = BrandColors.MintLeaf,
                        title = stringResource(R.string.settings_row_profile_title),
                        subtitle = stringResource(R.string.settings_row_profile_subtitle),
                        onClick = {},
                        trailing = { ChevronTrailing() },
                        testTag = "preview_profile",
                    )
                }
            }

            SettingsSection(title = stringResource(R.string.settings_section_matching)) {
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Outlined.Tune,
                        iconBackground = BrandColors.CoralTint,
                        iconTint = BrandColors.CoralDeep,
                        title = stringResource(R.string.settings_row_filters_title),
                        subtitle = stringResource(R.string.settings_row_filters_subtitle),
                        onClick = {},
                        trailing = { ChevronTrailing() },
                        testTag = "preview_filters",
                    )
                    RowDivider()
                    SettingsRow(
                        icon = Icons.Outlined.Notifications,
                        iconBackground = BrandColors.LavenderTint,
                        iconTint = BrandColors.LavenderInk,
                        title = stringResource(R.string.settings_row_notifications_title),
                        subtitle = stringResource(R.string.settings_row_notifications_subtitle),
                        onClick = {},
                        trailing = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                if (notifEnabledCount > 0) CountPill(count = notifEnabledCount)
                                ChevronTrailing()
                            }
                        },
                        testTag = "preview_notifications",
                    )
                    RowDivider()
                    SettingsRow(
                        icon = Icons.Outlined.PauseCircleOutline,
                        iconBackground = BrandColors.NeutralTint,
                        iconTint = BrandColors.NeutralInk,
                        title = stringResource(R.string.settings_row_pause_title),
                        subtitle = stringResource(R.string.settings_row_pause_subtitle),
                        onClick = {},
                        trailing = {
                            Switch(
                                checked = paused,
                                onCheckedChange = {},
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = BrandColors.CoralDeep,
                                    uncheckedThumbColor = Color.White,
                                    uncheckedTrackColor = MaterialTheme.colorScheme
                                        .onSurfaceVariant.copy(alpha = 0.35f),
                                    uncheckedBorderColor = Color.Transparent,
                                ),
                            )
                        },
                        testTag = "preview_pause",
                    )
                }
            }

            SettingsSection(title = stringResource(R.string.settings_section_safety)) {
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Outlined.Shield,
                        iconBackground = BrandColors.MintTint,
                        iconTint = BrandColors.MintLeaf,
                        title = stringResource(R.string.settings_row_privacy_title),
                        subtitle = stringResource(R.string.settings_row_privacy_subtitle),
                        onClick = {},
                        trailing = { ChevronTrailing() },
                        testTag = "preview_privacy",
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
                        onClick = {},
                        trailing = { ChevronTrailing() },
                        testTag = "preview_blocked",
                    )
                }
            }

            SignOutPill(onClick = {})
            VersionFooter()
        }
    }
}

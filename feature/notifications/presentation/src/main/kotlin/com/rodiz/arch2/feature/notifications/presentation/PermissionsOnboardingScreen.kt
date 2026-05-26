package com.rodiz.arch2.feature.notifications.presentation

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rodiz.arch2.core.designsystem.theme.BrandColors
import com.rodiz.arch2.core.designsystem.theme.TinPetTheme
import com.rodiz.arch2.core.ui.components.PrimaryButton

@Composable
internal fun PermissionsOnboardingScreen(
    state: PermissionsOnboardingUiState,
    onTapLocation: () -> Unit,
    onTapNotifications: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars),
        color = BrandColors.Cream,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))
            HeaderIconTile()
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.permissions_onboarding_headline),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.permissions_onboarding_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))

            PermissionRow(
                icon = Icons.Outlined.LocationOn,
                iconBackground = BrandColors.CoralLight.copy(alpha = 0.35f),
                iconTint = BrandColors.CoralDeep,
                title = stringResource(R.string.permissions_onboarding_location_title),
                subtitle = locationSubtitle(state.location),
                status = state.location,
                onClick = onTapLocation,
                actionLabel = stringResource(R.string.permissions_onboarding_action_allow),
            )

            Spacer(Modifier.height(12.dp))

            PermissionRow(
                icon = Icons.Outlined.Notifications,
                iconBackground = BrandColors.LavenderTint,
                iconTint = BrandColors.LavenderInk,
                title = stringResource(R.string.permissions_onboarding_notifications_title),
                subtitle = notificationsSubtitle(state.notifications),
                status = state.notifications,
                onClick = onTapNotifications,
                actionLabel = stringResource(R.string.permissions_onboarding_action_allow),
            )

            Spacer(Modifier.weight(1f))

            PrimaryButton(
                text = stringResource(R.string.permissions_onboarding_continue),
                onClick = onDone,
                enabled = true,
                loading = false,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.permissions_onboarding_maybe_later),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
        }
    }
}

@Composable
private fun HeaderIconTile() {
    Box(
        modifier = Modifier
            .size(64.dp)
            .background(
                color = BrandColors.CoralLight.copy(alpha = 0.35f),
                shape = RoundedCornerShape(20.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Tune,
            contentDescription = null,
            tint = BrandColors.CoralDeep,
            modifier = Modifier.size(32.dp),
        )
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    iconBackground: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    status: PermissionStatus,
    onClick: () -> Unit,
    actionLabel: String,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = BrandColors.PeachWarmLight,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Icon tile
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(iconBackground, CircleShape),
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
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Trailing affordance reflects status.
            when (status) {
                PermissionStatus.NotRequested, PermissionStatus.Denied -> {
                    OutlinedButton(
                        onClick = onClick,
                        shape = RoundedCornerShape(50),
                    ) { Text(actionLabel) }
                }
                PermissionStatus.Working -> {
                    CircularProgressIndicator(
                        color = BrandColors.CoralDeep,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                    )
                }
                is PermissionStatus.Allowed -> {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = BrandColors.MintLeaf,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun locationSubtitle(status: PermissionStatus): String = when (status) {
    PermissionStatus.NotRequested -> stringResource(R.string.permissions_onboarding_location_subtitle_default)
    PermissionStatus.Working -> stringResource(R.string.permissions_onboarding_location_subtitle_working)
    is PermissionStatus.Allowed -> status.cityLabel?.let {
        stringResource(R.string.permissions_onboarding_location_subtitle_allowed_with_city, it)
    } ?: stringResource(R.string.permissions_onboarding_location_subtitle_allowed)
    PermissionStatus.Denied -> stringResource(R.string.permissions_onboarding_location_subtitle_denied)
}

@Composable
private fun notificationsSubtitle(status: PermissionStatus): String = when (status) {
    PermissionStatus.NotRequested -> stringResource(R.string.permissions_onboarding_notifications_subtitle_default)
    PermissionStatus.Working -> stringResource(R.string.permissions_onboarding_notifications_subtitle_default)
    is PermissionStatus.Allowed -> stringResource(R.string.permissions_onboarding_notifications_subtitle_allowed)
    PermissionStatus.Denied -> stringResource(R.string.permissions_onboarding_notifications_subtitle_denied)
}

// ----- Previews ------------------------------------------------------------

@Preview(name = "Permissions — fresh", showBackground = true, heightDp = 720)
@Composable
private fun PreviewPermissionsBlank() {
    TinPetTheme {
        PermissionsOnboardingScreen(
            state = PermissionsOnboardingUiState(),
            onTapLocation = {},
            onTapNotifications = {},
            onDone = {},
        )
    }
}

@Preview(name = "Permissions — both allowed", showBackground = true, heightDp = 720)
@Composable
private fun PreviewPermissionsBothAllowed() {
    TinPetTheme {
        PermissionsOnboardingScreen(
            state = PermissionsOnboardingUiState(
                location = PermissionStatus.Allowed("Mexico City"),
                notifications = PermissionStatus.Allowed(),
            ),
            onTapLocation = {},
            onTapNotifications = {},
            onDone = {},
        )
    }
}

@Preview(name = "Permissions — partial", showBackground = true, heightDp = 720)
@Composable
private fun PreviewPermissionsPartial() {
    TinPetTheme {
        PermissionsOnboardingScreen(
            state = PermissionsOnboardingUiState(
                location = PermissionStatus.Working,
                notifications = PermissionStatus.Denied,
            ),
            onTapLocation = {},
            onTapNotifications = {},
            onDone = {},
        )
    }
}

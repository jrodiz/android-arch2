package com.rodiz.arch2.feature.settings.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

private data class SettingsCategory(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsHomeRoute(
    onBack: () -> Unit,
    onOpenEditProfile: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenAccount: () -> Unit,
) {
    val categories = listOf(
        SettingsCategory("Profile", "Name and avatar", Icons.Outlined.Person, onOpenEditProfile),
        SettingsCategory("Notifications", "Matches, messages, likes", Icons.Outlined.Notifications, onOpenNotifications),
        SettingsCategory("Filters", "Distance, intent, species", Icons.Outlined.Tune, {}, enabled = false),
        SettingsCategory("Privacy", "Pause, blocked users", Icons.Outlined.Lock, onOpenPrivacy),
        SettingsCategory("Account", "Email, delete account", Icons.Outlined.Settings, onOpenAccount),
    )
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            items(categories) { cat ->
                val rowModifier = if (cat.enabled) Modifier.clickable { cat.onClick() } else Modifier
                ListItem(
                    modifier = rowModifier,
                    leadingContent = { Icon(cat.icon, contentDescription = null) },
                    headlineContent = { Text(cat.title) },
                    supportingContent = {
                        Text(if (cat.enabled) cat.subtitle else "${cat.subtitle} (coming soon)")
                    },
                    trailingContent = {
                        if (cat.enabled) {
                            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null)
                        }
                    },
                )
                HorizontalDivider()
            }
        }
    }
}

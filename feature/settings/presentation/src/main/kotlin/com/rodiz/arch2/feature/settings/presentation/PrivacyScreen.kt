package com.rodiz.arch2.feature.settings.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.rodiz.arch2.core.designsystem.component.EmptyTabState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PrivacyRoute(
    onBack: () -> Unit,
) {
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
    ) { padding ->
        EmptyTabState(
            modifier = Modifier.fillMaxSize().padding(padding),
            icon = Icons.Outlined.Block,
            headline = "Privacy controls coming soon",
            body = "Pause your profile and manage blocked users will land in a follow-up build.",
        )
    }
}

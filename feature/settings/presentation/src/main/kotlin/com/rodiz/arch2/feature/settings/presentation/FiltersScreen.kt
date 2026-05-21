package com.rodiz.arch2.feature.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rodiz.arch2.feature.pet.domain.model.Intent
import com.rodiz.arch2.feature.pet.domain.model.SpeciesCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FiltersRoute(
    onBack: () -> Unit,
    viewModel: FiltersViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Filters") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            DistanceSection(
                km = state.prefs.maxDistanceKm,
                onChange = viewModel::setDistance,
            )
            Section(title = "Looking for") {
                ChipRow {
                    Intent.entries.forEach { intent ->
                        FilterChip(
                            selected = intent in state.prefs.intents,
                            onClick = { viewModel.toggleIntent(intent) },
                            label = { Text(intent.label()) },
                            leadingIcon = if (intent in state.prefs.intents) {
                                { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else null,
                        )
                    }
                }
            }
            Section(title = "Species") {
                ChipRow {
                    SpeciesCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = cat in state.prefs.speciesCategories,
                            onClick = { viewModel.toggleSpecies(cat) },
                            label = { Text(cat.label()) },
                            leadingIcon = if (cat in state.prefs.speciesCategories) {
                                { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else null,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DistanceSection(km: Int, onChange: (Int) -> Unit) {
    Section(title = "Distance") {
        Text(
            text = "Show pets within $km km",
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
            value = km.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = 5f..200f,
            // Snap to plan's preset stops by stepping in 5km increments.
            steps = ((200 - 5) / 5) - 1,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChipRow(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        content()
    }
}

private fun Intent.label(): String = when (this) {
    Intent.PLAYDATE -> "Playdate"
    Intent.ADOPTION -> "Adoption"
    Intent.FRIENDSHIP -> "Friendship"
}

private fun SpeciesCategory.label(): String = when (this) {
    SpeciesCategory.DOGS -> "Dogs"
    SpeciesCategory.CATS -> "Cats"
    SpeciesCategory.SMALL_MAMMALS -> "Small mammals"
}

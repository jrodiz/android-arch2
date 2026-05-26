package com.rodiz.arch2.feature.settings.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rodiz.arch2.core.designsystem.theme.BrandColors
import com.rodiz.arch2.feature.pet.domain.model.Intent
import com.rodiz.arch2.feature.pet.domain.model.Species
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FiltersRoute(
    onBack: () -> Unit,
    viewModel: FiltersViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = BrandColors.FiltersCream,
    ) { padding ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Box(Modifier.fillMaxSize().padding(padding)) {
            val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = topInset + 12.dp, bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                HeaderRow(onBack = onBack, onReset = viewModel::reset)
                DistancePanel(
                    km = state.prefs.maxDistanceKm,
                    onChange = viewModel::setDistance,
                )
                LookingForPanel(
                    selected = state.prefs.intents,
                    onToggle = viewModel::toggleIntent,
                )
                SpeciesPanel(
                    selectedSpecies = state.prefs.species,
                    onToggleGroup = viewModel::toggleSpeciesGroup,
                )
            }

            ApplyCta(
                count = state.matchingPetCount,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                onClick = {
                    scope.launch {
                        viewModel.flush()
                        onBack()
                    }
                },
            )
        }
    }
}

@Composable
private fun HeaderRow(onBack: () -> Unit, onReset: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 2.dp,
            modifier = Modifier.size(44.dp),
        ) {
            IconButton(onClick = onBack, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Text(
            text = "Filters",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 12.dp).weight(1f),
        )
        TextButton(onClick = onReset) {
            Text(
                text = "Reset",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Panel(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            content()
        }
    }
}

@Composable
private fun DistancePanel(km: Int, onChange: (Int) -> Unit) {
    Panel {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Distance",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Surface(
                shape = CircleShape,
                color = BrandColors.CoralLight.copy(alpha = 0.45f),
            ) {
                Text(
                    text = "$km km",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = BrandColors.CoralDeep,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Show pets within $km km of you",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Slider(
            value = km.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = 5f..200f,
            // 5km step granularity over the 5..200 range.
            steps = ((200 - 5) / 5) - 1,
            colors = SliderDefaults.colors(
                thumbColor = BrandColors.CoralDeep,
                activeTrackColor = BrandColors.CoralDeep,
                inactiveTrackColor = BrandColors.Coral.copy(alpha = 0.2f),
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(2.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            listOf("5", "50", "100", "200 km").forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LookingForPanel(
    selected: Set<Intent>,
    onToggle: (Intent) -> Unit,
) {
    Panel {
        Text(
            text = "Looking for",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Pick all that apply",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Intent.entries.forEach { intent ->
                IntentTile(
                    intent = intent,
                    selected = intent in selected,
                    onToggle = { onToggle(intent) },
                    modifier = Modifier.weight(1f).aspectRatio(0.95f),
                )
            }
        }
    }
}

private data class IntentVisuals(
    val label: String,
    val icon: ImageVector,
    val fill: Color,
    val iconBg: Color,
)

private fun Intent.visuals(): IntentVisuals = when (this) {
    Intent.PLAYDATE -> IntentVisuals(
        label = "Playdate",
        icon = Icons.Outlined.Pets,
        fill = BrandColors.CoralLight,
        iconBg = BrandColors.CoralDeep,
    )
    Intent.ADOPTION -> IntentVisuals(
        label = "Adoption",
        icon = Icons.Outlined.Home,
        fill = BrandColors.PeachWarm,
        iconBg = BrandColors.PeachWarmDeep,
    )
    Intent.FRIENDSHIP -> IntentVisuals(
        label = "Friendship",
        icon = Icons.Outlined.Favorite,
        fill = BrandColors.MintLeaf,
        iconBg = BrandColors.MintLeafDeep,
    )
}

@Composable
private fun IntentTile(
    intent: Intent,
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val v = intent.visuals()
    val fill = if (selected) v.fill.copy(alpha = 0.45f) else v.fill.copy(alpha = 0.18f)
    val iconBg = if (selected) v.iconBg else Color.White
    val iconTint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    val labelColor = if (selected) MaterialTheme.colorScheme.onSurface
    else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = fill,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .toggleable(
                value = selected,
                role = Role.Checkbox,
                onValueChange = { onToggle() },
            ),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(iconBg, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = v.icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp),
                )
            }
            Text(
                text = v.label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = labelColor,
            )
        }
    }
}

/**
 * Each pill represents one or more granular [Species] values. Most pills map 1:1
 * (Dogs → DOG); "Other" buckets the two species the mock doesn't surface explicitly
 * (Guinea pigs + an open-ended Other small mammal slot) so the pill row stays at 6.
 */
private enum class SpeciesPill(
    val label: String,
    val emoji: String,
    val species: Set<Species>,
) {
    DOGS("Dogs", "🐶", setOf(Species.DOG)),
    CATS("Cats", "🐱", setOf(Species.CAT)),
    RABBITS("Rabbits", "🐰", setOf(Species.RABBIT)),
    HAMSTERS("Hamsters", "🐹", setOf(Species.HAMSTER)),
    FERRETS("Ferrets", "🦊", setOf(Species.FERRET)),
    OTHER("Other", "🐾", setOf(Species.GUINEA_PIG, Species.OTHER_SMALL_MAMMAL)),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeciesPanel(
    selectedSpecies: Set<Species>,
    onToggleGroup: (Set<Species>) -> Unit,
) {
    Panel {
        Text(
            text = "Species",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(12.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SpeciesPill.entries.forEach { pill ->
                SpeciesChip(
                    pill = pill,
                    selected = pill.species.all { it in selectedSpecies },
                    onToggle = { onToggleGroup(pill.species) },
                )
            }
        }
    }
}

@Composable
private fun SpeciesChip(
    pill: SpeciesPill,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    val bg = if (selected) BrandColors.CoralDeep else Color.White
    val labelColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
    val border = if (selected) null else BorderStroke(
        1.dp,
        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
    )
    Surface(
        shape = CircleShape,
        color = bg,
        border = border,
        modifier = Modifier
            .clip(CircleShape)
            .toggleable(
                value = selected,
                role = Role.Checkbox,
                onValueChange = { onToggle() },
            ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(text = pill.emoji, fontSize = 16.sp)
            Text(
                text = pill.label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = labelColor,
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun ApplyCta(
    count: Int?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = BrandColors.CoralDeep,
            contentColor = Color.White,
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
    ) {
        val label = if (count != null) {
            val petsLabel = androidx.compose.ui.res.pluralStringResource(
                id = R.plurals.filters_apply_cta_pets,
                count = count,
                count,
            )
            "Apply filters · $petsLabel"
        } else {
            "Apply filters"
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = Color.White,
        )
    }
}

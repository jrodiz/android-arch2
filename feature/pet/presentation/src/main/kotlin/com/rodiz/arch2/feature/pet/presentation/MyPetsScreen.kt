package com.rodiz.arch2.feature.pet.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rodiz.arch2.core.designsystem.theme.BrandColors
import com.rodiz.arch2.feature.pet.domain.model.Pet
import com.rodiz.arch2.feature.pet.domain.model.PetId

private const val MAX_ACTIVE_PETS = 5

@Composable
internal fun MyPetsRoute(
    onAddPet: () -> Unit,
    onOpenPet: (PetId) -> Unit,
    onEditPet: (PetId) -> Unit,
    onBack: () -> Unit,
    viewModel: MyPetsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            val addEnabled = state.activePets.size < MAX_ACTIVE_PETS
            MyPetsHeader(
                onBack = onBack,
                onAdd = onAddPet,
                addEnabled = addEnabled,
            )
            when {
                state.isLoading -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = BrandColors.CoralDeep)
                }
                state.activePets.isEmpty() && state.archivedPets.isEmpty() -> EmptyMyPets(onAddPet = onAddPet)
                else -> PetsGrid(
                    active = state.activePets,
                    archived = state.archivedPets,
                    onAddPet = onAddPet,
                    onOpenPet = onOpenPet,
                    onEditPet = onEditPet,
                    onRestore = viewModel::restore,
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        ) { data -> Snackbar(snackbarData = data) }
    }
}

@Composable
private fun MyPetsHeader(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    addEnabled: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeaderSquareButton(
            background = Color.White,
            elevation = 1.dp,
            onClick = onBack,
            enabled = true,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.pet_a11y_back),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = stringResource(R.string.pet_screen_title),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.ExtraBold,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            maxLines = 1,
        )
        val addColor = if (addEnabled) BrandColors.CoralDeep
        else BrandColors.CoralDeep.copy(alpha = 0.4f)
        HeaderSquareButton(
            background = addColor,
            elevation = if (addEnabled) 2.dp else 0.dp,
            onClick = onAdd,
            enabled = addEnabled,
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = stringResource(R.string.pet_a11y_add),
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun HeaderSquareButton(
    background: Color,
    elevation: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    Surface(
        color = background,
        shape = RoundedCornerShape(14.dp),
        shadowElevation = elevation,
        modifier = Modifier
            .size(44.dp)
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
private fun ActivePetsBanner(activeCount: Int) {
    val context = LocalContext.current
    val title = remember(activeCount) {
        context.resources.getQuantityString(R.plurals.pet_active_count, activeCount, activeCount)
    }
    val subtitle = if (activeCount >= MAX_ACTIVE_PETS) {
        stringResource(R.string.pet_banner_subtitle_at_quota, MAX_ACTIVE_PETS)
    } else {
        stringResource(R.string.pet_banner_subtitle_below_quota, MAX_ACTIVE_PETS)
    }
    Surface(
        color = BrandColors.Coral.copy(alpha = 0.14f),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(BrandColors.Coral.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Pets,
                    contentDescription = null,
                    tint = BrandColors.CoralDeep,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = BrandColors.CoralDeep,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandColors.CoralDeep.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@Composable
private fun AddAnotherPetTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val strokeColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    val strokeWidthPx = with(density) { 2.dp.toPx() }
    val dashOnPx = with(density) { 8.dp.toPx() }
    val dashOffPx = with(density) { 6.dp.toPx() }
    val cornerPx = with(density) { 20.dp.toPx() }
    val stroke = remember(strokeWidthPx, dashOnPx, dashOffPx) {
        Stroke(
            width = strokeWidthPx,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashOnPx, dashOffPx)),
        )
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.72f)
            .clickable(onClick = onClick)
            .drawBehind {
                val inset = strokeWidthPx / 2f
                drawRoundRect(
                    color = strokeColor,
                    topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                    size = Size(size.width - strokeWidthPx, size.height - strokeWidthPx),
                    cornerRadius = CornerRadius(cornerPx, cornerPx),
                    style = stroke,
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(BrandColors.Coral.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    tint = BrandColors.CoralDeep,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.pet_add_another_tile),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun PetsGrid(
    active: List<Pet>,
    archived: List<Pet>,
    onAddPet: () -> Unit,
    onOpenPet: (PetId) -> Unit,
    onEditPet: (PetId) -> Unit,
    onRestore: (Pet) -> Unit,
) {
    var showArchived by remember { mutableStateOf(false) }
    val showAddTile = active.size < MAX_ACTIVE_PETS
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(2) }) {
            ActivePetsBanner(activeCount = active.size)
        }
        items(active, key = { it.id.value }) { pet ->
            PetThumbnailCard(
                pet = pet,
                onClick = { onOpenPet(pet.id) },
                onEdit = { onEditPet(pet.id) },
            )
        }
        if (showAddTile) {
            item(key = "add-tile") {
                AddAnotherPetTile(onClick = onAddPet)
            }
        }
        if (archived.isNotEmpty()) {
            item(span = { GridItemSpan(2) }, key = "archived-header") {
                ArchivedHeader(
                    count = archived.size,
                    expanded = showArchived,
                    onToggle = { showArchived = !showArchived },
                )
            }
            if (showArchived) {
                items(archived, key = { "archived-${it.id.value}" }) { pet ->
                    Column(modifier = Modifier.alpha(0.85f)) {
                        PetThumbnailCard(
                            pet = pet,
                            onClick = { onOpenPet(pet.id) },
                            onEdit = { onEditPet(pet.id) },
                            modifier = Modifier.alpha(0.7f),
                        )
                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = { onRestore(pet) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.pet_restore))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchivedHeader(
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.pet_archived_header),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = "($count)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onToggle) {
            Text(
                text = if (expanded) stringResource(R.string.pet_archived_hide)
                else stringResource(R.string.pet_archived_show),
                color = BrandColors.CoralDeep,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            )
        }
    }
}

@Composable
private fun EmptyMyPets(onAddPet: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(BrandColors.Coral.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Pets,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = BrandColors.CoralDeep,
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.pet_empty_title),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.pet_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Surface(
            color = BrandColors.CoralDeep,
            shape = RoundedCornerShape(50),
            shadowElevation = 2.dp,
            modifier = Modifier.clickable(onClick = onAddPet),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = stringResource(R.string.pet_empty_cta),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
            }
        }
    }
}

package com.rodiz.arch2.feature.deck.presentation

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBackIos
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.rodiz.arch2.core.designsystem.theme.BrandColors
import com.rodiz.arch2.core.ownerlookup.domain.OwnerDisplay
import com.rodiz.arch2.feature.pet.domain.model.Intent as PetIntent
import com.rodiz.arch2.feature.pet.domain.model.Pet
import com.rodiz.arch2.feature.pet.domain.model.PetEnergy
import com.rodiz.arch2.feature.pet.domain.model.PetSize
import com.rodiz.arch2.feature.pet.domain.model.PhotoSource
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
internal class DeckPetDetailFactoryHolder @Inject constructor(
    val factory: DeckPetDetailViewModel.Factory,
) : ViewModel()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DeckPetDetailRoute(
    petIdValue: String,
    onBack: () -> Unit,
    holder: DeckPetDetailFactoryHolder = hiltViewModel(),
) {
    val viewModel = remember(petIdValue) { holder.factory.create(petIdValue) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                DeckPetDetailEvent.Dismiss -> onBack()
            }
        }
    }

    DeckPetDetailScreen(
        state = state,
        onBack = onBack,
        onLike = viewModel::like,
        onPass = viewModel::pass,
    )
}

@Composable
internal fun DeckPetDetailScreen(
    state: DeckPetDetailUiState,
    onBack: () -> Unit,
    onLike: () -> Unit,
    onPass: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Status-bar icons turn white while this dark-photo screen is on top; they go
    // back to the default (dark on cream) on dispose.
    LightStatusBarIconsWhileShown()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        val pet = state.pet
        when {
            state.isLoading && pet == null -> {
                CircularProgressIndicator(
                    color = BrandColors.Coral,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            pet == null -> {
                UnavailablePet(onBack = onBack, modifier = Modifier.align(Alignment.Center))
            }
            else -> {
                DetailContent(
                    pet = pet,
                    owner = state.owner,
                    onBack = onBack,
                    onLike = onLike,
                    onPass = onPass,
                )
            }
        }
    }
}

@Composable
private fun DetailContent(
    pet: Pet,
    owner: OwnerDisplay?,
    onBack: () -> Unit,
    onLike: () -> Unit,
    onPass: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Photo + dark scrim live below the sheet.
        PhotoBlock(
            pet = pet,
            owner = owner,
            onBack = onBack,
            modifier = Modifier.fillMaxSize(),
        )

        // The bottom sheet is rendered as a fixed-height surface anchored to the
        // bottom edge — using a plain Surface instead of BottomSheetScaffold avoids
        // pulling the experimental scaffold-state machinery for a sheet that doesn't
        // animate between two snap points in this pass. Tap-friendly drag is deferred;
        // the sheet content is internally scrollable.
        BottomSheetCard(
            pet = pet,
            onLike = onLike,
            onPass = onPass,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        )
    }
}

private val SheetHeight: Dp = 380.dp
private val PhotoOverlayBottomPad: Dp = SheetHeight + 16.dp

@Composable
private fun PhotoBlock(
    pet: Pet,
    owner: OwnerDisplay?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        val photoUrl = pet.photos.firstOrNull()?.let { p ->
            (p.source as? PhotoSource.Remote)?.downloadUrl
                ?: (p.source as? PhotoSource.Local)?.uri
        }
        if (photoUrl != null) {
            AsyncImage(
                model = photoUrl,
                contentDescription = pet.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Pets,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(120.dp),
                )
            }
        }

        // Soft black gradient over the lower half so the white text reads on any photo.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.45f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.65f),
                    ),
                ),
        )

        // Back chevron (translucent circle, top-left).
        Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.45f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 16.dp, top = 8.dp)
                .size(40.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBackIos,
                    contentDescription = stringResource(R.string.deck_detail_back_action),
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        // Story segment row (decorative — photo cycling is deferred per plan §3).
        val segmentCount = pet.photos.size.coerceIn(1, 6)
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 14.dp, start = 72.dp, end = 72.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            repeat(segmentCount) { index ->
                Box(
                    modifier = Modifier
                        .height(3.dp)
                        .weight(1f)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(
                            if (index == 0) Color.White else Color.White.copy(alpha = 0.35f),
                        ),
                )
            }
        }

        // Bottom-of-photo content stack: owner chip, name+age, species+distance.
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, end = 20.dp, bottom = PhotoOverlayBottomPad),
        ) {
            OwnerOverlayChip(firstName = owner?.firstName, avatarUrl = owner?.avatarUrl)
            Spacer(Modifier.height(12.dp))
            Text(
                text = if (pet.ageIsApproximate) {
                    stringResource(R.string.deck_detail_age_year_approx_format, pet.name, pet.ageYears)
                } else {
                    stringResource(R.string.deck_detail_age_year_format, pet.name, pet.ageYears)
                },
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                color = Color.White,
            )
            Spacer(Modifier.height(4.dp))
            val speciesLabel = pet.species.name.toDisplayCase()
            Text(
                text = speciesLabel,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun OwnerOverlayChip(firstName: String?, avatarUrl: String?) {
    Surface(
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.45f),
    ) {
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(BrandColors.CoralLight.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center,
            ) {
                if (!avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            val label = firstName?.takeIf { it.isNotBlank() }
                ?.let { stringResource(R.string.deck_owner_with_format, it) }
                ?: stringResource(R.string.deck_owner_with_unknown)
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
            )
        }
    }
}

@Composable
private fun BottomSheetCard(
    pet: Pet,
    onLike: () -> Unit,
    onPass: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.height(SheetHeight),
        color = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        shadowElevation = 10.dp,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Drag handle — visual only; the sheet doesn't drag in this pass.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)),
                )
            }

            // Scrolling content fills available space; action row docks below it.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(Modifier.height(10.dp))
                SectionLabel(stringResource(R.string.deck_detail_open_to))
                Spacer(Modifier.height(10.dp))
                IntentRow(intents = pet.intents)
                Spacer(Modifier.height(20.dp))
                SectionLabel(stringResource(R.string.deck_detail_about))
                Spacer(Modifier.height(10.dp))
                Text(
                    text = if (pet.bio.isNullOrBlank()) {
                        stringResource(R.string.deck_detail_no_bio_format, pet.name)
                    } else {
                        pet.bio!!
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (pet.size != null || pet.energy != null) {
                    Spacer(Modifier.height(20.dp))
                    SectionLabel(stringResource(R.string.deck_detail_stats))
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pet.size?.let { StatsChip(label = sizeLabel(it)) }
                        pet.energy?.let { StatsChip(label = energyLabel(it)) }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            ActionRow(
                petName = pet.name,
                onPass = onPass,
                onLike = onLike,
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun IntentRow(intents: Set<PetIntent>) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // Render in a stable order matching the mock: Playdate, Friendship, Adoption.
        listOf(PetIntent.PLAYDATE, PetIntent.FRIENDSHIP, PetIntent.ADOPTION)
            .filter { it in intents }
            .forEach { intent -> IntentChip(intent = intent) }
    }
}

@Composable
private fun IntentChip(intent: PetIntent) {
    val (label, color, icon) = when (intent) {
        PetIntent.PLAYDATE -> Triple(
            stringResource(R.string.deck_detail_intent_playdate),
            BrandColors.Coral,
            Icons.Outlined.Pets,
        )
        PetIntent.FRIENDSHIP -> Triple(
            stringResource(R.string.deck_detail_intent_friendship),
            BrandColors.MintLeaf,
            Icons.AutoMirrored.Outlined.Chat,
        )
        PetIntent.ADOPTION -> Triple(
            stringResource(R.string.deck_detail_intent_adoption),
            BrandColors.CoralDeep,
            Icons.Outlined.Favorite,
        )
    }
    Surface(
        shape = CircleShape,
        color = color,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
            )
        }
    }
}

@Composable
private fun StatsChip(label: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
        ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun ActionRow(
    petName: String,
    onPass: () -> Unit,
    onLike: () -> Unit,
) {
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = (bottomInset.value + 12).dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Pass — small white circle with subtle border.
        Surface(
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 2.dp,
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
            ),
            modifier = Modifier.size(56.dp),
        ) {
            IconButton(onClick = onPass) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.deck_detail_pass_action),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        // Like — large coral pill with the pet's name.
        Button(
            onClick = onLike,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = BrandColors.Coral),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = stringResource(R.string.deck_detail_like_format, petName),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun UnavailablePet(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.deck_detail_unavailable),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onBack) {
            Text(stringResource(R.string.deck_detail_back_action))
        }
    }
}

/**
 * Forces light status-bar icons (white on the dark photo) while the detail is on top,
 * and restores the previous setting on dispose. Mirrors the helper in the login coral
 * hero — kept local to avoid pulling cross-feature presentation deps.
 */
@Composable
private fun LightStatusBarIconsWhileShown() {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(Unit) {
        val window = (view.context as Activity).window
        val controller = WindowInsetsControllerCompat(window, view)
        val previous = controller.isAppearanceLightStatusBars
        controller.isAppearanceLightStatusBars = false
        onDispose { controller.isAppearanceLightStatusBars = previous }
    }
}

@Composable
private fun sizeLabel(size: PetSize): String = when (size) {
    PetSize.SMALL -> stringResource(R.string.deck_detail_size_small)
    PetSize.MEDIUM -> stringResource(R.string.deck_detail_size_medium)
    PetSize.LARGE -> stringResource(R.string.deck_detail_size_large)
}

@Composable
private fun energyLabel(energy: PetEnergy): String = when (energy) {
    PetEnergy.CALM -> stringResource(R.string.deck_detail_energy_calm)
    PetEnergy.MEDIUM -> stringResource(R.string.deck_detail_energy_medium)
    PetEnergy.HIGH -> stringResource(R.string.deck_detail_energy_high)
}

private fun String.toDisplayCase(): String = lowercase()
    .replace('_', ' ')
    .replaceFirstChar { it.uppercase() }

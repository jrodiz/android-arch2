package com.rodiz.arch2.feature.pet.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.rodiz.arch2.core.designsystem.theme.BrandColors
import com.rodiz.arch2.feature.pet.domain.model.Intent
import com.rodiz.arch2.feature.pet.domain.model.PetDraft
import com.rodiz.arch2.feature.pet.domain.model.PetPhoto
import com.rodiz.arch2.feature.pet.domain.model.PhotoSource

@Composable
internal fun AddPetStep3Route(
    onBack: () -> Unit,
    onPublished: () -> Unit,
    viewModel: AddPetViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.completed.collect { onPublished() }
    }

    AddPetStep3Screen(
        state = state,
        onBack = onBack,
        onPublish = viewModel::submit,
    )
}

@Composable
private fun AddPetStep3Screen(
    state: PetFormUiState,
    onBack: () -> Unit,
    onPublish: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 124.dp),
        ) {
            AddPetWizardHeader(currentStep = 3, onBack = onBack)
            Spacer(Modifier.height(14.dp))
            StepProgressBar(
                current = 3,
                total = ADD_PET_STEP_TOTAL,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.addpet3_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(20.dp))

            ReviewPhotos(
                photos = state.draft.photos,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(22.dp))
            BasicsCard(
                draft = state.draft,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(14.dp))
            PersonalityCard(
                draft = state.draft,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            if (state.errorMessage != null) {
                Spacer(Modifier.height(14.dp))
                InlineError(
                    text = state.errorMessage,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        WizardActionBar(
            secondaryLabel = stringResource(R.string.addpet_action_back),
            primaryLabel = stringResource(R.string.addpet3_action_publish),
            onSecondary = onBack,
            onPrimary = onPublish,
            primaryLoading = state.isSubmitting,
            primaryEnabled = !state.isSubmitting,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun ReviewPhotos(
    photos: List<PetPhoto>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SectionLabel(text = stringResource(R.string.addpet3_section_photos))
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            photos.forEachIndexed { index, photo ->
                PhotoTile(photo = photo, isHero = index == 0)
            }
        }
    }
}

@Composable
private fun PhotoTile(photo: PetPhoto, isHero: Boolean) {
    val source = photo.source
    val model: Any = when (source) {
        is PhotoSource.Local -> source.uri
        is PhotoSource.Remote -> source.downloadUrl
    }
    Box(
        modifier = Modifier
            .size(width = 96.dp, height = 120.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (isHero) {
            Surface(
                color = BrandColors.CoralDeep,
                shape = RoundedCornerShape(topStart = 18.dp, bottomEnd = 12.dp),
                modifier = Modifier.align(Alignment.TopStart),
            ) {
                Text(
                    text = "HERO",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun BasicsCard(draft: PetDraft, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("addpet3_basics_card"),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            SectionLabel(text = stringResource(R.string.addpet3_section_basics))
            Spacer(Modifier.height(10.dp))
            Text(
                text = draft.name.ifBlank { stringResource(R.string.addpet3_value_empty) },
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            val species = draft.species
            val subtitle = if (species != null) {
                val format = if (draft.ageIsApproximate) R.string.addpet3_subtitle_approx
                else R.string.addpet3_subtitle_exact
                // Prefer breed over species in the subtitle when set — same convention
                // as PetThumbnailCard so My Pets and the wizard review stay aligned.
                val speciesPortion = draft.breed?.takeIf { it.isNotBlank() } ?: species.label()
                stringResource(format, draft.ageYears, speciesPortion)
            } else {
                stringResource(R.string.addpet3_value_empty)
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))
            ReviewRowLabel(text = stringResource(R.string.addpet3_field_intents))
            Spacer(Modifier.height(6.dp))
            if (draft.intents.isEmpty()) {
                Text(
                    text = stringResource(R.string.addpet3_value_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    draft.intents.forEach { intent ->
                        IntentBadge(intent = intent)
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonalityCard(draft: PetDraft, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("addpet3_personality_card"),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            SectionLabel(text = stringResource(R.string.addpet3_section_personality))
            Spacer(Modifier.height(12.dp))
            FieldRow(
                label = stringResource(R.string.addpet3_field_bio),
                value = draft.bio?.takeIf { it.isNotBlank() }
                    ?: stringResource(R.string.addpet3_value_empty),
                valueMaxLines = 4,
            )
            Spacer(Modifier.height(10.dp))
            FieldRow(
                label = stringResource(R.string.addpet3_field_size),
                value = draft.size?.label() ?: stringResource(R.string.addpet3_value_empty),
            )
            Spacer(Modifier.height(10.dp))
            FieldRow(
                label = stringResource(R.string.addpet3_field_energy),
                value = draft.energy?.label() ?: stringResource(R.string.addpet3_value_empty),
            )
        }
    }
}

@Composable
private fun FieldRow(label: String, value: String, valueMaxLines: Int = 1) {
    Column {
        ReviewRowLabel(text = label)
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = valueMaxLines,
        )
    }
}

@Composable
private fun ReviewRowLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun IntentBadge(intent: Intent) {
    val icon: ImageVector = when (intent) {
        Intent.PLAYDATE -> Icons.Outlined.Pets
        Intent.ADOPTION -> Icons.Outlined.Home
        Intent.FRIENDSHIP -> Icons.Outlined.Favorite
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = BrandColors.CoralDeep.copy(alpha = 0.12f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BrandColors.CoralDeep,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = intent.label(),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = BrandColors.CoralDeep,
            )
        }
    }
}

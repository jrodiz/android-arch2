package com.rodiz.arch2.feature.pet.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rodiz.arch2.core.designsystem.theme.BrandColors
import com.rodiz.arch2.feature.pet.domain.model.PetDraft
import com.rodiz.arch2.feature.pet.domain.model.PetEnergy
import com.rodiz.arch2.feature.pet.domain.model.PetSize

@Composable
internal fun AddPetStep2Route(
    onBack: () -> Unit,
    viewModel: AddPetViewModel,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AddPetStep2Screen(
        state = state,
        onBack = onBack,
        onEvent = viewModel::onEvent,
        onContinue = viewModel::goToStep3,
    )
}

@Composable
private fun AddPetStep2Screen(
    state: PetFormUiState,
    onBack: () -> Unit,
    onEvent: (PetFormEvent) -> Unit,
    onContinue: () -> Unit,
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
                .imePadding()
                .padding(bottom = 124.dp),
        ) {
            AddPetWizardHeader(currentStep = 2, onBack = onBack)
            Spacer(Modifier.height(14.dp))
            StepProgressBar(
                current = 2,
                total = ADD_PET_STEP_TOTAL,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.addpet2_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(20.dp))

            LabeledField(
                label = stringResource(R.string.addpet2_label_breed),
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                val placeholder = state.draft.species
                    ?.let { stringResource(R.string.addpet2_breed_placeholder, it.label()) }
                    ?: stringResource(R.string.addpet2_breed_placeholder_generic)
                WhitePillTextField(
                    value = state.draft.breed.orEmpty(),
                    onValueChange = { onEvent(PetFormEvent.BreedChanged(it)) },
                    placeholder = placeholder,
                )
            }

            Spacer(Modifier.height(22.dp))
            BioField(
                petName = state.draft.name,
                bio = state.draft.bio.orEmpty(),
                onBioChange = { onEvent(PetFormEvent.BioChanged(it)) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(22.dp))
            LabeledField(
                label = stringResource(R.string.addpet2_label_size),
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                EnumChipRow(
                    options = PetSize.entries,
                    selected = state.draft.size,
                    labelFor = { it.label() },
                    onToggle = { next ->
                        onEvent(PetFormEvent.SizeChanged(if (next == state.draft.size) null else next))
                    },
                )
            }

            Spacer(Modifier.height(22.dp))
            LabeledField(
                label = stringResource(R.string.addpet2_label_energy),
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                EnumChipRow(
                    options = PetEnergy.entries,
                    selected = state.draft.energy,
                    labelFor = { it.label() },
                    onToggle = { next ->
                        onEvent(PetFormEvent.EnergyChanged(if (next == state.draft.energy) null else next))
                    },
                )
            }
        }

        WizardActionBar(
            secondaryLabel = stringResource(R.string.addpet_action_back),
            primaryLabel = stringResource(R.string.addpet_action_continue),
            onSecondary = onBack,
            onPrimary = onContinue,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun BioField(
    petName: String,
    bio: String,
    onBioChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SectionLabel(
                text = stringResource(R.string.addpet2_label_bio),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.addpet2_bio_counter, bio.length, PetDraft.BIO_MAX_LEN),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            val placeholder = if (petName.isNotBlank()) {
                stringResource(R.string.addpet2_bio_placeholder, petName)
            } else {
                stringResource(R.string.addpet2_bio_placeholder_generic)
            }
            TextField(
                value = bio,
                onValueChange = onBioChange,
                placeholder = {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                },
                singleLine = false,
                minLines = 4,
                maxLines = 8,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    disabledContainerColor = Color.White,
                    errorContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent,
                    cursorColor = BrandColors.CoralDeep,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
                    .testTag("addpet2_bio_field"),
            )
        }
    }
}

@Composable
private fun <T> EnumChipRow(
    options: List<T>,
    selected: T?,
    labelFor: (T) -> String,
    onToggle: (T) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val container = if (isSelected) BrandColors.CoralDeep else Color.White
            val content = if (isSelected) Color.White else BrandColors.CoralDeep
            val border = if (isSelected) null else BorderStroke(1.5.dp, BrandColors.CoralDeep)
            Surface(
                shape = RoundedCornerShape(50),
                color = container,
                contentColor = content,
                border = border,
                shadowElevation = if (isSelected) 1.dp else 0.dp,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clickable { onToggle(option) },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = labelFor(option),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = content,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/**
 * Two-button bottom bar for Steps 2 and 3 — outlined back/secondary on the left,
 * filled coral primary on the right. Shares the same look as Step 1's action bar so
 * the wizard reads as one continuous strip.
 */
@Composable
internal fun WizardActionBar(
    secondaryLabel: String,
    primaryLabel: String,
    onSecondary: () -> Unit,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    primaryLoading: Boolean = false,
    primaryEnabled: Boolean = true,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.White,
                border = BorderStroke(1.5.dp, BrandColors.CoralDeep.copy(alpha = 0.55f)),
                modifier = Modifier
                    .height(52.dp)
                    .clickable(onClick = onSecondary),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 22.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = secondaryLabel,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            val primaryColor = if (primaryEnabled && !primaryLoading) BrandColors.CoralDeep
            else BrandColors.CoralDeep.copy(alpha = 0.45f)
            Surface(
                shape = RoundedCornerShape(50),
                color = primaryColor,
                shadowElevation = if (primaryEnabled) 2.dp else 0.dp,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clickable(enabled = primaryEnabled && !primaryLoading, onClick = onPrimary),
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = primaryLabel,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )
                }
            }
        }
    }
}

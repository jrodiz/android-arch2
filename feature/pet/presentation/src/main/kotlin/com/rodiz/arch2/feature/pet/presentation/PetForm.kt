package com.rodiz.arch2.feature.pet.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.rodiz.arch2.feature.pet.domain.model.Intent
import com.rodiz.arch2.feature.pet.domain.model.PetDraft
import com.rodiz.arch2.feature.pet.domain.model.PetPhoto
import com.rodiz.arch2.feature.pet.domain.model.PhotoId
import com.rodiz.arch2.feature.pet.domain.model.PhotoSource
import com.rodiz.arch2.feature.pet.domain.model.Species

@Composable
internal fun PetForm(
    state: PetFormUiState,
    onEvent: (PetFormEvent) -> Unit,
    submitLabel: String,
    onSubmit: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(16.dp),
) {
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            onEvent(
                PetFormEvent.PhotoAdded(
                    PetPhoto(
                        id = PhotoId(""),
                        source = PhotoSource.Local(uri.toString()),
                    ),
                ),
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        SectionLabel("Photos (${state.draft.photos.size}/${PetDraft.PHOTOS_MAX})")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(state.draft.photos) { index, photo ->
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    val model = when (val src = photo.source) {
                        is PhotoSource.Local -> src.uri
                        is PhotoSource.Remote -> src.downloadUrl
                    }
                    AsyncImage(
                        model = model,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    IconButton(
                        onClick = { onEvent(PetFormEvent.PhotoRemoved(index)) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(2.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f)),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Remove photo",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
            if (state.draft.photos.size < PetDraft.PHOTOS_MAX) {
                items(1) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        IconButton(
                            onClick = {
                                photoPicker.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly,
                                    ),
                                )
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = "Add photo",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider()

        OutlinedTextField(
            value = state.draft.name,
            onValueChange = { onEvent(PetFormEvent.NameChanged(it)) },
            label = { Text("Pet name") },
            singleLine = true,
            supportingText = { Text("${state.draft.name.length}/${PetDraft.NAME_MAX_LEN}") },
            modifier = Modifier.fillMaxWidth(),
        )

        SectionLabel("Species")
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            Species.entries.forEach { species ->
                FilterChip(
                    selected = state.draft.species == species,
                    onClick = { onEvent(PetFormEvent.SpeciesChanged(species)) },
                    label = { Text(species.label()) },
                )
            }
        }

        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Age: ${state.draft.ageYears} ${if (state.draft.ageYears == 1) "yr" else "yrs"}")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Approx.", style = MaterialTheme.typography.bodySmall)
                    Checkbox(
                        checked = state.draft.ageIsApproximate,
                        onCheckedChange = { onEvent(PetFormEvent.AgeApproximateChanged(it)) },
                    )
                }
            }
            Slider(
                value = state.draft.ageYears.toFloat(),
                onValueChange = { onEvent(PetFormEvent.AgeYearsChanged(it.toInt())) },
                valueRange = PetDraft.AGE_MIN.toFloat()..PetDraft.AGE_MAX.toFloat(),
                steps = PetDraft.AGE_MAX - PetDraft.AGE_MIN - 1,
            )
        }

        SectionLabel("Open to")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Intent.entries.forEach { intent ->
                FilterChip(
                    selected = intent in state.draft.intents,
                    onClick = { onEvent(PetFormEvent.IntentToggled(intent)) },
                    label = { Text(intent.label()) },
                )
            }
        }

        OutlinedTextField(
            value = state.draft.bio.orEmpty(),
            onValueChange = { onEvent(PetFormEvent.BioChanged(it)) },
            label = { Text("Bio (optional)") },
            supportingText = { Text("${(state.draft.bio?.length ?: 0)}/${PetDraft.BIO_MAX_LEN}") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            minLines = 3,
            maxLines = 6,
            modifier = Modifier.fillMaxWidth(),
        )

        if (state.errors.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                state.errors.forEach {
                    Text(
                        text = "• ${it.message()}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        state.errorMessage?.let { msg ->
            Text(
                text = msg,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(Modifier.height(8.dp))

        FilledTonalButton(
            onClick = onSubmit,
            enabled = !state.isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (state.isSubmitting) "Saving…" else submitLabel)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

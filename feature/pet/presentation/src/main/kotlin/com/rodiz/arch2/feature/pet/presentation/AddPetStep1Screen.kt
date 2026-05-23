package com.rodiz.arch2.feature.pet.presentation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.rodiz.arch2.core.designsystem.theme.BrandColors
import kotlinx.coroutines.launch
import com.rodiz.arch2.feature.pet.domain.model.Intent
import com.rodiz.arch2.feature.pet.domain.model.PetDraft
import com.rodiz.arch2.feature.pet.domain.model.PetPhoto
import com.rodiz.arch2.feature.pet.domain.model.PhotoId
import com.rodiz.arch2.feature.pet.domain.model.PhotoSource
import com.rodiz.arch2.feature.pet.domain.model.Species

private const val STEP_TOTAL = 3
private const val STEP_CURRENT = 1
private const val PHOTO_SLOT_COUNT = PetDraft.PHOTOS_MAX

@Composable
internal fun AddPetStep1Route(
    onBack: () -> Unit,
    onContinue: () -> Unit,
    viewModel: AddPetViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.step1Events.collect { event ->
            when (event) {
                Step1Event.DraftSaved ->
                    snackbar.showSnackbar(context.getString(R.string.addpet_snackbar_draft_saved))
                Step1Event.ContinueRequested -> {
                    snackbar.showSnackbar(context.getString(R.string.addpet_snackbar_continue_stub))
                    onContinue()
                }
            }
        }
    }

    AddPetStep1Screen(
        state = state,
        onBack = onBack,
        onEvent = viewModel::onEvent,
        onSaveDraft = viewModel::saveDraft,
        onContinue = viewModel::attemptContinue,
        onSpeciesClick = {
            // Species picker bottom-sheet is out of scope for this turn.
            // Surface a snackbar so the affordance is visible and not silently broken.
            val msg = context.getString(R.string.addpet_snackbar_species_soon)
            scope.launch { snackbar.showSnackbar(msg) }
        },
        snackbarHostState = snackbar,
    )
}

@Composable
private fun AddPetStep1Screen(
    state: PetFormUiState,
    onBack: () -> Unit,
    onEvent: (PetFormEvent) -> Unit,
    onSaveDraft: () -> Unit,
    onContinue: () -> Unit,
    onSpeciesClick: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
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
    val launchPicker: () -> Unit = remember(photoPicker) {
        {
            photoPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        }
    }

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
            Step1Header(onBack = onBack)
            Spacer(Modifier.height(14.dp))
            StepProgressBar(
                current = STEP_CURRENT,
                total = STEP_TOTAL,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(20.dp))

            PhotoDropzone(
                onClick = {
                    if (state.draft.photos.size < PHOTO_SLOT_COUNT) launchPicker()
                },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(14.dp))
            PhotoStrip(
                photos = state.draft.photos,
                onAddPhoto = launchPicker,
                onRemovePhoto = { idx -> onEvent(PetFormEvent.PhotoRemoved(idx)) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            if (Step1Error.PhotosMissing in state.step1Errors) {
                Spacer(Modifier.height(8.dp))
                InlineError(
                    text = stringResource(R.string.addpet_error_photos_missing),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            Spacer(Modifier.height(22.dp))
            LabeledField(
                label = stringResource(R.string.addpet_label_name),
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                WhitePillTextField(
                    value = state.draft.name,
                    onValueChange = { onEvent(PetFormEvent.NameChanged(it)) },
                    placeholder = stringResource(R.string.addpet_placeholder_name),
                )
            }
            if (Step1Error.NameMissing in state.step1Errors) {
                Spacer(Modifier.height(6.dp))
                InlineError(
                    text = stringResource(R.string.addpet_error_name_missing),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LabeledField(
                    label = stringResource(R.string.addpet_label_age),
                    modifier = Modifier.weight(1f),
                ) {
                    val ageText = if (state.draft.ageYears > 0) state.draft.ageYears.toString() else ""
                    WhitePillTextField(
                        value = ageText,
                        onValueChange = { raw ->
                            val digits = raw.filter { it.isDigit() }.take(2)
                            val years = digits.toIntOrNull() ?: 0
                            onEvent(PetFormEvent.AgeYearsChanged(years))
                        },
                        placeholder = stringResource(R.string.addpet_placeholder_age),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
                LabeledField(
                    label = stringResource(R.string.addpet_label_species),
                    modifier = Modifier.weight(1f),
                ) {
                    SpeciesSelectorPill(
                        species = state.draft.species,
                        placeholder = stringResource(R.string.addpet_placeholder_species),
                        onClick = onSpeciesClick,
                    )
                }
            }
            if (Step1Error.SpeciesMissing in state.step1Errors) {
                Spacer(Modifier.height(6.dp))
                InlineError(
                    text = stringResource(R.string.addpet_error_species_missing),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel(
                text = stringResource(R.string.addpet_label_open_to),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Intent.entries.forEach { intent ->
                    IntentChoiceChip(
                        intent = intent,
                        selected = intent in state.draft.intents,
                        onClick = { onEvent(PetFormEvent.IntentToggled(intent)) },
                    )
                }
            }
            if (Step1Error.IntentMissing in state.step1Errors) {
                Spacer(Modifier.height(8.dp))
                InlineError(
                    text = stringResource(R.string.addpet_error_intent_missing),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        Step1ActionBar(
            onSaveDraft = onSaveDraft,
            onContinue = onContinue,
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp, start = 16.dp, end = 16.dp),
        ) { data -> Snackbar(snackbarData = data) }
    }
}

@Composable
private fun Step1Header(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(14.dp),
            shadowElevation = 1.dp,
            modifier = Modifier
                .size(44.dp)
                .clickable(onClick = onBack),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.pet_a11y_back),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Text(
            text = stringResource(R.string.addpet_title),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.ExtraBold,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = stringResource(R.string.addpet_step_indicator, STEP_CURRENT, STEP_TOTAL),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = BrandColors.CoralDeep,
        )
    }
}

@Composable
private fun StepProgressBar(
    current: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(total) { i ->
            val isActive = i < current
            val color = if (isActive) BrandColors.CoralDeep
            else BrandColors.Coral.copy(alpha = 0.18f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color),
            )
        }
    }
}

@Composable
private fun PhotoDropzone(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val strokeWidthPx = with(density) { 2.dp.toPx() }
    val dashOn = with(density) { 10.dp.toPx() }
    val dashOff = with(density) { 7.dp.toPx() }
    val cornerPx = with(density) { 24.dp.toPx() }
    val strokeColor = BrandColors.CoralDeep.copy(alpha = 0.65f)
    val stroke = remember(strokeWidthPx, dashOn, dashOff) {
        Stroke(
            width = strokeWidthPx,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashOn, dashOff)),
        )
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(176.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .drawBehind {
                val inset = strokeWidthPx / 2f
                drawRoundRect(
                    color = strokeColor,
                    topLeft = Offset(inset, inset),
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
                    .background(BrandColors.Coral.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.CameraAlt,
                    contentDescription = null,
                    tint = BrandColors.CoralDeep,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.addpet_dropzone_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = BrandColors.CoralDeep,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.addpet_dropzone_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun PhotoStrip(
    photos: List<PetPhoto>,
    onAddPhoto: () -> Unit,
    onRemovePhoto: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(PHOTO_SLOT_COUNT) { i ->
            val photo = photos.getOrNull(i)
            val isHero = i == 0 && photo != null
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center,
            ) {
                if (photo != null) {
                    val model = when (val src = photo.source) {
                        is PhotoSource.Local -> src.uri
                        is PhotoSource.Remote -> src.downloadUrl
                    }
                    val borderMod = if (isHero) {
                        Modifier.border(
                            BorderStroke(2.dp, BrandColors.CoralDeep),
                            RoundedCornerShape(14.dp),
                        )
                    } else {
                        Modifier
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shadowElevation = if (isHero) 2.dp else 0.dp,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(borderMod),
                    ) {
                        AsyncImage(
                            model = model,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(2.dp)
                            .size(20.dp)
                            .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                            .clickable { onRemovePhoto(i) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.addpet_a11y_remove_photo),
                            tint = Color.White,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(14.dp))
                            .background(BrandColors.Coral.copy(alpha = 0.10f))
                            .clickable(enabled = photos.size < PHOTO_SLOT_COUNT, onClick = onAddPhoto),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LabeledField(
    label: String,
    modifier: Modifier = Modifier,
    field: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
        SectionLabel(text = label)
        Spacer(Modifier.height(8.dp))
        field()
    }
}

@Composable
private fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
private fun WhitePillTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            ),
            keyboardOptions = keyboardOptions,
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
                .height(56.dp),
        )
    }
}

@Composable
private fun SpeciesSelectorPill(
    species: Species?,
    placeholder: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (species == null) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = species.emoji(),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = species.label(),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun IntentChoiceChip(
    intent: Intent,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val icon: ImageVector = when (intent) {
        Intent.PLAYDATE -> Icons.Outlined.Pets
        Intent.ADOPTION -> Icons.Outlined.Home
        Intent.FRIENDSHIP -> Icons.Outlined.Favorite
    }
    val container = if (selected) BrandColors.CoralDeep else Color.White
    val content = if (selected) Color.White else BrandColors.CoralDeep
    val border = if (selected) null else BorderStroke(1.5.dp, BrandColors.CoralDeep)
    Surface(
        shape = RoundedCornerShape(50),
        color = container,
        contentColor = content,
        border = border,
        shadowElevation = if (selected) 1.dp else 0.dp,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(14.dp),
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = intent.label(),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = content,
            )
        }
    }
}

@Composable
private fun Step1ActionBar(
    onSaveDraft: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
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
                    .clickable(onClick = onSaveDraft),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 22.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.addpet_action_save_draft),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = BrandColors.CoralDeep,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clickable(onClick = onContinue),
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
                        text = stringResource(R.string.addpet_action_continue),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun InlineError(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Start,
        modifier = modifier,
    )
}

private fun Species.emoji(): String = when (this) {
    Species.DOG -> "🐕"          // 🐕
    Species.CAT -> "🐈"          // 🐈
    Species.RABBIT -> "🐇"       // 🐇
    Species.HAMSTER -> "🐹"      // 🐹
    Species.GUINEA_PIG -> "🐹"   // 🐹 (closest match)
    Species.FERRET -> "🦝"       // 🦝 (closest small mammal)
    Species.OTHER_SMALL_MAMMAL -> "🐾" // 🐾
}

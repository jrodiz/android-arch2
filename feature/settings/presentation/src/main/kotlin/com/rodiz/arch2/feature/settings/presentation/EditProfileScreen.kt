package com.rodiz.arch2.feature.settings.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationServices
import com.rodiz.arch2.core.designsystem.theme.BrandColors
import com.rodiz.arch2.core.designsystem.theme.TinPetTheme
import com.rodiz.arch2.core.ui.components.FilledPillTextField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Composable
internal fun EditProfileRoute(
    onBack: () -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val savedMessage = stringResource(R.string.edit_profile_saved)

    val ctx = LocalContext.current
    val avatarSavedMessage = stringResource(R.string.edit_profile_avatar_saved)
    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    LaunchedEffect(state.errorMessageRes) {
        state.errorMessageRes?.let { resId ->
            snackbarHostState.showSnackbar(ctx.getString(resId))
            viewModel.onErrorMessageResShown()
        }
    }
    LaunchedEffect(state.avatarSavedAtMillis) {
        if (state.avatarSavedAtMillis != null) {
            snackbarHostState.showSnackbar(avatarSavedMessage)
            viewModel.onAvatarSavedShown()
        }
    }
    LaunchedEffect(state.savedAtMillis) {
        if (state.savedAtMillis != null) {
            snackbarHostState.showSnackbar(savedMessage)
            viewModel.onSavedShown()
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let { viewModel.onAvatarPicked(it.toString()) }
    }

    val scope = rememberCoroutineScope()
    val locationClient = remember { LocationServices.getFusedLocationProviderClient(ctx) }
    val locationUnavailable = stringResource(R.string.edit_profile_location_unavailable)
    val locationFailed = stringResource(R.string.edit_profile_location_fetch_failed)
    val permissionDenied = stringResource(R.string.edit_profile_location_permission_denied)

    fun fetchAndSaveLocation() {
        scope.launch {
            runCatching {
                @Suppress("MissingPermission")
                val loc = locationClient.lastLocation.await()
                if (loc != null) {
                    val city = reverseGeocode(ctx, loc.latitude, loc.longitude)
                    viewModel.onLocationFetched(loc.latitude, loc.longitude, city)
                } else {
                    viewModel.onLocationError(locationUnavailable)
                }
            }.onFailure { e ->
                viewModel.onLocationError(e.message ?: locationFailed)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            fetchAndSaveLocation()
        } else {
            viewModel.onLocationError(permissionDenied)
        }
    }

    fun onSetLocationClicked() {
        val granted = ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            fetchAndSaveLocation()
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    var showSourceSheet by rememberSaveable { mutableStateOf(false) }

    fun launchGallery() {
        showSourceSheet = false
        photoPicker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
        )
    }

    // Dismiss the IME when Save is tapped — otherwise the snackbar lives
    // behind the keyboard on small phones and the user has to manually
    // collapse it to see the confirmation.
    val keyboardController = LocalSoftwareKeyboardController.current
    EditProfileScaffold(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onSave = {
            keyboardController?.hide()
            viewModel.save()
        },
        onFirstNameChange = viewModel::onFirstNameChange,
        onBioChange = viewModel::onBioChange,
        onChangePhoto = { showSourceSheet = true },
        onUpdateLocation = ::onSetLocationClicked,
    )

    if (showSourceSheet) {
        AvatarSourceSheet(
            onDismiss = { showSourceSheet = false },
            onGallery = { launchGallery() },
            // Camera capture isn't wired yet — falls back to the gallery launcher so the
            // user always lands somewhere useful. TODO: wire TakePicture + FileProvider.
            onCamera = { launchGallery() },
        )
    }
}

@Composable
private fun EditProfileScaffold(
    state: EditProfileUiState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onFirstNameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onChangePhoto: () -> Unit,
    onUpdateLocation: () -> Unit,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BrandColors.Cream,
    ) { padding ->
        if (state.isLoading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = BrandColors.CoralDeep)
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .windowInsetsPadding(WindowInsets.statusBars)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            EditProfileHeader(
                onBack = onBack,
                saveEnabled = state.canSave,
                isSaving = state.isSaving,
                onSave = onSave,
            )

            AvatarBlock(
                avatarUrl = state.avatarUrl,
                isUploading = state.isUploadingAvatar,
                onChangePhoto = onChangePhoto,
            )

            LabeledPillField(
                label = stringResource(R.string.edit_profile_label_first_name),
            ) {
                FilledPillTextField(
                    value = state.firstNameField,
                    onValueChange = onFirstNameChange,
                    placeholder = stringResource(R.string.edit_profile_placeholder_first_name),
                    errorMessage = null,
                    containerColor = MaterialTheme.colorScheme.surface,
                    shadowElevation = 1.dp,
                    fieldModifier = Modifier.testTag("edit_profile_first_name"),
                )
            }

            LabeledPillField(
                label = stringResource(R.string.edit_profile_label_bio),
            ) {
                FilledPillTextField(
                    value = state.bioField,
                    onValueChange = onBioChange,
                    placeholder = stringResource(R.string.edit_profile_placeholder_bio),
                    errorMessage = null,
                    containerColor = MaterialTheme.colorScheme.surface,
                    shadowElevation = 1.dp,
                    singleLine = false,
                    minLines = 3,
                    shape = RoundedCornerShape(24.dp),
                    fieldModifier = Modifier.testTag("edit_profile_bio"),
                )
                BioCounter(length = state.bioField.length, max = EditProfileUiState.MAX_BIO_LENGTH)
            }

            LocationCard(
                cityLabel = state.location?.cityLabel,
                isUpdating = state.isUpdatingLocation,
                onUpdate = onUpdateLocation,
            )

            EmailCard(
                email = state.email,
                isVerified = state.isEmailVerified,
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ----- Header --------------------------------------------------------------

@Composable
private fun EditProfileHeader(
    onBack: () -> Unit,
    saveEnabled: Boolean,
    isSaving: Boolean,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp,
            modifier = Modifier
                .size(44.dp)
                .clickable(onClick = onBack)
                .testTag("edit_profile_back"),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.edit_profile_back_cd),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
        Text(
            text = stringResource(R.string.edit_profile_title),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.ExtraBold,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(start = 14.dp)
                .weight(1f),
        )
        TextButton(
            onClick = onSave,
            enabled = saveEnabled,
            modifier = Modifier.testTag("edit_profile_save"),
        ) {
            Text(
                text = if (isSaving) {
                    stringResource(R.string.edit_profile_saving)
                } else {
                    stringResource(R.string.edit_profile_save)
                },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = if (saveEnabled) {
                    BrandColors.CoralDeep
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                },
            )
        }
    }
}

// ----- Avatar block --------------------------------------------------------

@Composable
private fun AvatarBlock(
    avatarUrl: String?,
    isUploading: Boolean,
    onChangePhoto: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(140.dp),
            contentAlignment = Alignment.BottomEnd,
        ) {
            // Avatar tile
            Box(
                modifier = Modifier
                    .size(124.dp)
                    .align(Alignment.TopCenter)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(3.dp, Color.White, RoundedCornerShape(28.dp))
                    .clickable(onClick = onChangePhoto)
                    .testTag("edit_profile_avatar_tile"),
                contentAlignment = Alignment.Center,
            ) {
                if (avatarUrl != null) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = stringResource(R.string.edit_profile_avatar_cd),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(124.dp)
                            .clip(RoundedCornerShape(28.dp)),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (isUploading) {
                    Box(
                        modifier = Modifier
                            .size(124.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }
            // Camera badge — positioned in the outer Box's BottomEnd, the outer Box is
            // wider than the tile so the badge sits flush against the tile's lower-right
            // corner with a small overlap onto the cream background.
            Box(
                modifier = Modifier
                    .offset(x = (-4).dp, y = (-2).dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(BrandColors.CoralDeep)
                    .border(3.dp, BrandColors.Cream, CircleShape)
                    .clickable(onClick = onChangePhoto)
                    .testTag("edit_profile_avatar_badge"),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.PhotoCamera,
                    contentDescription = stringResource(R.string.edit_profile_avatar_badge_cd),
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        TextButton(
            onClick = onChangePhoto,
            enabled = !isUploading,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier.testTag("edit_profile_change_photo"),
        ) {
            // Flip the label to "Uploading…" while the upload is in flight so the
            // user gets explicit textual confirmation that something is happening
            // — the spinner overlay alone reads as ambiguous on small displays.
            Text(
                text = stringResource(
                    if (isUploading) R.string.edit_profile_avatar_uploading
                    else R.string.edit_profile_change_photo,
                ),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = if (isUploading) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    BrandColors.CoralDeep
                },
            )
        }
    }
}

// ----- Field block ---------------------------------------------------------

@Composable
private fun LabeledPillField(
    label: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp, bottom = 6.dp),
        )
        content()
    }
}

@Composable
private fun BioCounter(length: Int, max: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, end = 16.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        Text(
            text = stringResource(R.string.edit_profile_bio_counter, length, max),
            style = MaterialTheme.typography.labelSmall,
            color = if (length > max) {
                BrandColors.CoralDeep
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

// ----- Location card -------------------------------------------------------

@Composable
private fun LocationCard(
    cityLabel: String?,
    isUpdating: Boolean,
    onUpdate: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = BrandColors.CoralTint,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onUpdate)
            .testTag("edit_profile_location_card"),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BrandColors.CoralDeep),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cityLabel ?: stringResource(R.string.edit_profile_location_empty_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = BrandColors.CoralDeep,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (cityLabel == null) {
                        stringResource(R.string.edit_profile_location_subtitle_empty)
                    } else {
                        stringResource(R.string.edit_profile_location_subtitle_set)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandColors.CoralDeep.copy(alpha = 0.8f),
                )
            }
            TextButton(
                onClick = onUpdate,
                enabled = !isUpdating,
                modifier = Modifier.testTag("edit_profile_location_update"),
            ) {
                Text(
                    text = if (isUpdating) {
                        stringResource(R.string.edit_profile_location_updating)
                    } else {
                        stringResource(R.string.edit_profile_location_update)
                    },
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = BrandColors.CoralDeep,
                )
            }
        }
    }
}

// ----- Email card ----------------------------------------------------------

@Composable
private fun EmailCard(
    email: String?,
    isVerified: Boolean,
) {
    val tileBg = if (isVerified) BrandColors.MintTint else BrandColors.PeachTint
    val tileTint = if (isVerified) BrandColors.MintLeaf else BrandColors.PeachInk
    val icon: ImageVector = if (isVerified) Icons.Outlined.Check else Icons.Outlined.ErrorOutline
    val titleRes = if (isVerified) {
        R.string.edit_profile_email_verified_title
    } else {
        R.string.edit_profile_email_unverified_title
    }
    val titleColor = if (isVerified) {
        MaterialTheme.colorScheme.onSurface
    } else {
        BrandColors.CoralDeep
    }
    val subtitle = email?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.edit_profile_email_missing)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("edit_profile_email_card"),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tileBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tileTint,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(titleRes),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = titleColor,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ----- Helpers -------------------------------------------------------------

// Best-effort reverse geocode. Returns null if the Geocoder is unavailable, the
// network fails, or no locality is resolved — the caller falls back to "Add your city".
private suspend fun reverseGeocode(ctx: Context, lat: Double, lng: Double): String? =
    withContext(Dispatchers.IO) {
        runCatching {
            @Suppress("DEPRECATION")
            val results = Geocoder(ctx).getFromLocation(lat, lng, 1)
            results?.firstOrNull()?.let { addr ->
                listOfNotNull(addr.locality, addr.adminArea ?: addr.countryCode)
                    .joinToString(", ")
                    .ifEmpty { null }
            }
        }.getOrNull()
    }

// ----- Previews ------------------------------------------------------------

@Preview(name = "Edit profile — populated", showBackground = true, heightDp = 1100)
@Composable
private fun EditProfilePreviewPopulated() {
    TinPetTheme {
        EditProfileScaffold(
            state = EditProfileUiState(
                isLoading = false,
                firstNameField = "Maya",
                bioField = "Dog mom of one bossy corgi. Park regular at Riverside.",
                avatarUrl = null,
                email = "maya@tinpet.com",
                location = com.rodiz.arch2.feature.profile.domain.model.GeoPoint(
                    lat = 40.6782,
                    lng = -73.9442,
                    geohash = "dr5rs",
                    cityLabel = "Brooklyn, NY",
                ),
                original = com.rodiz.arch2.feature.profile.domain.model.OwnerProfile(
                    id = "preview",
                    firstName = "Maya",
                    avatarUrl = null,
                    email = "maya@tinpet.com",
                    paused = false,
                    location = null,
                    createdAt = kotlinx.datetime.Clock.System.now(),
                    updatedAt = kotlinx.datetime.Clock.System.now(),
                    bio = "Dog mom of one bossy corgi. Park regular at Riverside.",
                ),
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onSave = {},
            onFirstNameChange = {},
            onBioChange = {},
            onChangePhoto = {},
            onUpdateLocation = {},
        )
    }
}

@Preview(name = "Edit profile — empty", showBackground = true, heightDp = 1100)
@Composable
private fun EditProfilePreviewEmpty() {
    TinPetTheme {
        EditProfileScaffold(
            state = EditProfileUiState(
                isLoading = false,
                firstNameField = "",
                bioField = "",
                avatarUrl = null,
                email = null,
                location = null,
                original = com.rodiz.arch2.feature.profile.domain.model.OwnerProfile(
                    id = "preview",
                    firstName = "",
                    avatarUrl = null,
                    email = null,
                    paused = false,
                    location = null,
                    createdAt = kotlinx.datetime.Clock.System.now(),
                    updatedAt = kotlinx.datetime.Clock.System.now(),
                    bio = "",
                ),
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onSave = {},
            onFirstNameChange = {},
            onBioChange = {},
            onChangePhoto = {},
            onUpdateLocation = {},
        )
    }
}

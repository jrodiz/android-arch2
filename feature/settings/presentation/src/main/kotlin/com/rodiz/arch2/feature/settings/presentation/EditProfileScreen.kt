package com.rodiz.arch2.feature.settings.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationServices
import com.rodiz.arch2.feature.profile.domain.model.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EditProfileRoute(
    onBack: () -> Unit,
    viewModel: EditProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    LaunchedEffect(state.savedAtMillis) {
        if (state.savedAtMillis != null) {
            snackbarHostState.showSnackbar("Profile saved")
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let { viewModel.onAvatarPicked(it.toString()) }
    }

    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationClient = remember { LocationServices.getFusedLocationProviderClient(ctx) }

    fun fetchAndSaveLocation() {
        scope.launch {
            runCatching {
                @Suppress("MissingPermission")
                val loc = locationClient.lastLocation.await()
                if (loc != null) {
                    val city = reverseGeocode(ctx, loc.latitude, loc.longitude)
                    viewModel.onLocationFetched(loc.latitude, loc.longitude, city)
                } else {
                    viewModel.onLocationError("Location unavailable — try outdoors with GPS on.")
                }
            }.onFailure { e ->
                viewModel.onLocationError(e.message ?: "Could not get location")
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            fetchAndSaveLocation()
        } else {
            viewModel.onLocationError("Location permission denied")
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                val url = state.avatarUrl
                if (url != null) {
                    AsyncImage(
                        model = url,
                        contentDescription = "Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (state.isUploadingAvatar) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = {
                    photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                enabled = !state.isUploadingAvatar,
            ) {
                Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(if (state.avatarUrl == null) "Add a photo" else "Change photo")
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = state.firstNameField,
                onValueChange = viewModel::onFirstNameChange,
                label = { Text("First name") },
                supportingText = {
                    Text(
                        text = "${state.firstNameField.length}/30",
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                isError = state.firstNameField.isNotEmpty() && !state.isNameValid,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            LocationRow(
                location = state.location,
                isUpdating = state.isUpdatingLocation,
                onSetLocation = ::onSetLocationClicked,
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = viewModel::save,
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.isSaving) "Saving…" else "Save")
            }
        }
    }
}

@Composable
private fun LocationRow(
    location: GeoPoint?,
    isUpdating: Boolean,
    onSetLocation: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (location != null) {
                    Text(
                        text = "Location",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = location.cityLabel
                            ?: "%.4f, %.4f".format(location.lat, location.lng),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    Text(
                        text = "Set your location to start matching",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Spacer(Modifier.size(8.dp))
            if (location == null) {
                FilledTonalButton(onClick = onSetLocation, enabled = !isUpdating) {
                    Text(if (isUpdating) "…" else "Set")
                }
            } else {
                OutlinedButton(onClick = onSetLocation, enabled = !isUpdating) {
                    Text(if (isUpdating) "…" else "Update")
                }
            }
        }
    }
}

// Best-effort reverse geocode. Returns null if the Geocoder is unavailable, the
// network fails, or no locality is resolved — the caller falls back to raw coords.
private suspend fun reverseGeocode(ctx: Context, lat: Double, lng: Double): String? =
    withContext(Dispatchers.IO) {
        runCatching {
            @Suppress("DEPRECATION")
            val results = Geocoder(ctx).getFromLocation(lat, lng, 1)
            results?.firstOrNull()?.let { addr ->
                listOfNotNull(addr.locality, addr.countryCode)
                    .joinToString(", ")
                    .ifEmpty { null }
            }
        }.getOrNull()
    }

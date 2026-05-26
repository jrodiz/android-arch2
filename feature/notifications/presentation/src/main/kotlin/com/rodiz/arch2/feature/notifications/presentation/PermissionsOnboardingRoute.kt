package com.rodiz.arch2.feature.notifications.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Composable
internal fun PermissionsOnboardingRoute(
    onDone: () -> Unit,
    viewModel: PermissionsOnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationClient = remember { LocationServices.getFusedLocationProviderClient(ctx) }

    // System back gesture lands on Deck, not Sign Up. The screen is reached via
    // replaceAll so the back stack is already empty — without this, the gesture
    // is a no-op (worse UX than landing on the next sensible screen).
    BackHandler { onDone() }

    fun fetchAndPersistLocation() {
        viewModel.onLocationWorking()
        scope.launch {
            runCatching {
                @Suppress("MissingPermission")
                val loc = locationClient.lastLocation.await()
                if (loc == null) {
                    viewModel.onLocationFetchFailed()
                } else {
                    val city = reverseGeocode(ctx, loc.latitude, loc.longitude)
                    viewModel.onLocationGranted(loc.latitude, loc.longitude, city)
                }
            }.onFailure {
                viewModel.onLocationFetchFailed()
            }
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) fetchAndPersistLocation() else viewModel.onLocationDenied()
    }

    val notificationsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) viewModel.onNotificationsGranted() else viewModel.onNotificationsDenied()
    }

    // Pre-Android 13 there's no POST_NOTIFICATIONS runtime permission; treat as
    // implicitly granted on first composition so the row reads "Allowed" without
    // the user having to tap. The notification CHANNEL is configured at app
    // start regardless of this flow.
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            viewModel.onNotificationsGranted()
        }
    }

    PermissionsOnboardingScreen(
        state = state,
        onTapLocation = {
            // If already granted from a prior session (re-installing the APK over
            // a previously-granted state), skip the system dialog and go straight
            // to the fetch.
            val granted = ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) fetchAndPersistLocation()
            else locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        },
        onTapNotifications = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    ctx,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
                if (granted) viewModel.onNotificationsGranted()
                else notificationsPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.onNotificationsGranted()
            }
        },
        onDone = onDone,
    )
}

// Best-effort reverse geocode — copied from EditProfileScreen to avoid a tiny
// shared-utility module for one 10-line helper. Returns null if the Geocoder
// is unavailable, the network fails, or no locality is resolved.
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

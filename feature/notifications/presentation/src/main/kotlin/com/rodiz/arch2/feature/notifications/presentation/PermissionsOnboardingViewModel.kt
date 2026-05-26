package com.rodiz.arch2.feature.notifications.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodiz.arch2.core.common.logging.CrashReporter
import com.rodiz.arch2.core.firebase.FcmTokenSync
import com.rodiz.arch2.core.firebase.OwnerLocationWriter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Per-permission status surface for the combined post-signup permissions screen.
 *
 * Both rows are independent; either can be in any state regardless of the other.
 * `Working` covers the brief window between the system permission grant and the
 * follow-on side effect completing (location fetch + Firestore write, or FCM
 * token sync). `Denied` is set whenever the user dismisses the system dialog OR
 * the follow-on side effect fails irrecoverably — the user still has Settings →
 * EditProfile or Settings → Notifications to retry.
 */
internal sealed interface PermissionStatus {
    data object NotRequested : PermissionStatus
    data object Working : PermissionStatus
    data class Allowed(val cityLabel: String? = null) : PermissionStatus
    data object Denied : PermissionStatus
}

internal data class PermissionsOnboardingUiState(
    val location: PermissionStatus = PermissionStatus.NotRequested,
    val notifications: PermissionStatus = PermissionStatus.NotRequested,
)

@HiltViewModel
internal class PermissionsOnboardingViewModel @Inject constructor(
    private val locationWriter: OwnerLocationWriter,
    private val fcmTokenSync: FcmTokenSync,
    private val crashReporter: CrashReporter,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PermissionsOnboardingUiState())
    val uiState: StateFlow<PermissionsOnboardingUiState> = _uiState.asStateFlow()

    /** Called by the route the moment the system permission grant comes back. */
    fun onLocationWorking() {
        _uiState.update { it.copy(location = PermissionStatus.Working) }
    }

    /**
     * Called by the route once it has resolved a lat/lng + best-effort city label.
     * Persists the location to Firestore; on Firestore failure, the row still
     * surfaces as Allowed (the user IS giving us permission — we'll retry on
     * next sign-in via Settings) and we log the throwable as a non-fatal.
     */
    fun onLocationGranted(lat: Double, lng: Double, cityLabel: String?) {
        _uiState.update { it.copy(location = PermissionStatus.Allowed(cityLabel)) }
        viewModelScope.launch {
            runCatching { locationWriter.updateLocation(lat, lng, cityLabel) }
                .onFailure {
                    crashReporter.recordException(
                        it,
                        "PermissionsOnboarding location write failed",
                    )
                }
        }
    }

    fun onLocationDenied() {
        _uiState.update { it.copy(location = PermissionStatus.Denied) }
    }

    /**
     * Called when permission was granted but the fetch itself failed
     * (no last-known location, GPS off, etc.). We surface as Denied so the
     * subtitle nudges the user to retry in EditProfile.
     */
    fun onLocationFetchFailed() {
        _uiState.update { it.copy(location = PermissionStatus.Denied) }
    }

    fun onNotificationsGranted() {
        _uiState.update { it.copy(notifications = PermissionStatus.Allowed()) }
        viewModelScope.launch {
            runCatching { fcmTokenSync.syncForSignedInUser() }
                .onFailure {
                    crashReporter.recordException(
                        it,
                        "PermissionsOnboarding FCM token sync failed",
                    )
                }
        }
    }

    fun onNotificationsDenied() {
        _uiState.update { it.copy(notifications = PermissionStatus.Denied) }
    }
}

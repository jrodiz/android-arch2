package com.rodiz.arch2.feature.notifications.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodiz.arch2.core.common.logging.CrashReporter
import com.rodiz.arch2.core.firebase.FcmTokenSync
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class NotificationRationaleViewModel @Inject constructor(
    private val fcmTokenSync: FcmTokenSync,
    private val crashReporter: CrashReporter,
) : ViewModel() {
    fun onPermissionGranted() {
        viewModelScope.launch {
            runCatching { fcmTokenSync.syncForSignedInUser() }
                .onFailure {
                    crashReporter.recordException(it, "FCM token sync after permission grant failed")
                }
        }
    }
}

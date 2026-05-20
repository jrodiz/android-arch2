package com.rodiz.arch2.feature.notifications.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodiz.arch2.core.firebase.FcmTokenSync
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class NotificationRationaleViewModel @Inject constructor(
    private val fcmTokenSync: FcmTokenSync,
) : ViewModel() {
    fun onPermissionGranted() {
        viewModelScope.launch {
            runCatching { fcmTokenSync.syncForSignedInUser() }
        }
    }
}

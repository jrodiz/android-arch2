package com.rodiz.arch2.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.rodiz.arch2.core.firebase.FcmTokenSync
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AppFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var fcmTokenSync: FcmTokenSync

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        scope.launch { runCatching { fcmTokenSync.syncForSignedInUser() } }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        // For backgrounded apps, FCM auto-renders push from the notification payload using
        // the channelId we set server-side. For foreground delivery, render manually here.
        // v1: just log; richer in-foreground UX (suppression when on the relevant surface)
        // lands when the Cloud Functions plan is implemented.
        Log.d(TAG, "FCM message: ${message.notification?.title} / ${message.data}")
    }

    companion object {
        private const val TAG = "TinPet.FCM"
    }
}

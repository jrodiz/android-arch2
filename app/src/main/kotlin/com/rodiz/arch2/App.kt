package com.rodiz.arch2

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.content.getSystemService
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService<NotificationManager>() ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MATCHES,
                "Matches",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = "New matches and weekly highlights" },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MESSAGES,
                "Messages",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = "New chat messages" },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_LIKES,
                "Likes",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = "Someone liked one of your pets" },
        )
    }

    companion object {
        const val CHANNEL_MATCHES = "matches"
        const val CHANNEL_MESSAGES = "messages"
        const val CHANNEL_LIKES = "likes"
    }
}

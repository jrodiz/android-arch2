package com.rodiz.arch2

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.ApplicationInfo
import androidx.core.content.getSystemService
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.rodiz.arch2.core.common.coroutine.IoDispatcher
import com.rodiz.arch2.core.common.logging.CrashReporter
import com.rodiz.arch2.core.session.domain.SessionRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {

    @Inject lateinit var sessionRepository: SessionRepository
    @Inject lateinit var crashReporter: CrashReporter

    @Inject @IoDispatcher lateinit var io: CoroutineDispatcher

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        configureCrashlyticsCollection()
        observeSessionForCrashAttribution()
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

    // Keep dev installs out of the prod Crashlytics dashboard. Using the
    // FLAG_DEBUGGABLE manifest flag avoids needing buildConfig=true just to
    // read BuildConfig.DEBUG.
    private fun configureCrashlyticsCollection() {
        val debuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = !debuggable
    }

    // Forwards uid changes (sign-in, account switch, sign-out) to Crashlytics so
    // every report carries the active user, and so sign-out detaches the previous
    // identity instead of attributing a later crash to a stale account.
    private fun observeSessionForCrashAttribution() {
        val scope = CoroutineScope(SupervisorJob() + io)
        sessionRepository.observe()
            .distinctUntilChanged { a, b -> a?.userId == b?.userId }
            .onEach { crashReporter.setUserId(it?.userId) }
            .launchIn(scope)
    }

    companion object {
        const val CHANNEL_MATCHES = "matches"
        const val CHANNEL_MESSAGES = "messages"
        const val CHANNEL_LIKES = "likes"
    }
}

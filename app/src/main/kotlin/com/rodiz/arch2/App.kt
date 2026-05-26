package com.rodiz.arch2

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.ApplicationInfo
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.getSystemService
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.rodiz.arch2.core.common.coroutine.IoDispatcher
import com.rodiz.arch2.core.common.logging.CrashReporter
import com.rodiz.arch2.core.featuredpets.data.FeaturedPetsSessionGate
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
    @Inject lateinit var featuredPetsGate: FeaturedPetsSessionGate

    @Inject @IoDispatcher lateinit var io: CoroutineDispatcher

    override fun onCreate() {
        super.onCreate()
        forceLightTheme()
        createNotificationChannels()
        configureCrashlyticsCollection()
        observeSession()
    }

    // Compose composables paint via TinPetTheme (forced to LightScheme), but
    // AppCompat-rendered surfaces — system dialogs, DatePicker / TimePicker
    // for quiet hours, the action bar in third-party activities — still
    // respect the system night mode. Pinning AppCompat to MODE_NIGHT_NO keeps
    // those consistent until we color-audit the dark palette for v2.
    private fun forceLightTheme() {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
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

    // Single uid-change observer that drives multiple downstream concerns:
    //   1. Crashlytics user attribution (so reports carry the active user, and
    //      sign-out detaches the prior identity).
    //   2. Featured-pets cache lifecycle (a different uid signing in wipes the
    //      previous user's Login-screen pet selection; sign-out preserves it
    //      so the Login hero can still render those pets).
    private fun observeSession() {
        val scope = CoroutineScope(SupervisorJob() + io)
        sessionRepository.observe()
            .distinctUntilChanged { a, b -> a?.userId == b?.userId }
            .onEach { session ->
                crashReporter.setUserId(session?.userId)
                featuredPetsGate.onSessionChanged(session?.userId)
            }
            .launchIn(scope)
    }

    companion object {
        const val CHANNEL_MATCHES = "matches"
        const val CHANNEL_MESSAGES = "messages"
        const val CHANNEL_LIKES = "likes"
    }
}

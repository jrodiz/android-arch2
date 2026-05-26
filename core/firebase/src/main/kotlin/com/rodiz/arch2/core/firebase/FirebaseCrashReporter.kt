package com.rodiz.arch2.core.firebase

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.rodiz.arch2.core.common.logging.CrashReporter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

internal class FirebaseCrashReporter(
    private val crashlytics: FirebaseCrashlytics,
) : CrashReporter {

    override fun setUserId(uid: String?) {
        // Empty string detaches the previous identity on Crashlytics' side.
        crashlytics.setUserId(uid.orEmpty())
    }

    override fun log(message: String) {
        crashlytics.log(message)
    }

    override fun recordException(throwable: Throwable, message: String?) {
        if (message != null) crashlytics.log(message)
        crashlytics.recordException(throwable)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object CrashReporterModule {
    @Provides
    @Singleton
    fun provideCrashReporter(): CrashReporter =
        FirebaseCrashReporter(FirebaseCrashlytics.getInstance())
}

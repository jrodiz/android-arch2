package com.rodiz.arch2.di

import com.rodiz.arch2.core.navigation.StartDestination
import com.rodiz.arch2.core.session.domain.SessionRepository
import com.rodiz.arch2.feature.home.nav.HomeHome
import com.rodiz.arch2.feature.login.nav.LoginHome
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.runBlocking

@Module
@InstallIn(ActivityRetainedComponent::class)
object StartDestinationModule {

    @Provides
    @StartDestination
    @ActivityRetainedScoped
    fun provideStartDestination(sessionRepository: SessionRepository): Any {
        // Startup-only: one-shot DataStore read on Activity creation. The Hilt
        // Navigator binding needs the start route up front; we cannot suspend
        // here. This is the single intentional runBlocking call in production
        // code and stays isolated to startup.
        val session = runBlocking { sessionRepository.current() }
        return if (session != null) HomeHome else LoginHome
    }
}

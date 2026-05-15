package com.rodiz.arch2.feature.home.presentation

import androidx.navigation3.runtime.entry
import com.rodiz.arch2.core.navigation.EntryProviderInstaller
import com.rodiz.arch2.core.navigation.Navigator
import com.rodiz.arch2.feature.home.nav.HomeHome
import com.rodiz.arch2.feature.login.nav.LoginHome
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(ActivityRetainedComponent::class)
internal object HomeNavModule {
    @Provides
    @IntoSet
    fun provideHomeEntries(navigator: Navigator): EntryProviderInstaller = {
        entry<HomeHome> {
            HomeRoute(onSignedOut = { navigator.replaceAll(LoginHome) })
        }
    }
}

package com.rodiz.arch2.feature.discover.presentation

import androidx.navigation3.runtime.entry
import com.rodiz.arch2.core.navigation.EntryProviderInstaller
import com.rodiz.arch2.feature.discover.nav.DiscoverHome
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(ActivityRetainedComponent::class)
internal object DiscoverNavModule {
    @Provides
    @IntoSet
    fun provideDiscoverEntries(): EntryProviderInstaller = {
        entry<DiscoverHome> {
            DiscoverRoute()
        }
    }
}

package com.rodiz.arch2.feature.likes.presentation

import androidx.navigation3.runtime.entry
import com.rodiz.arch2.core.navigation.EntryProviderInstaller
import com.rodiz.arch2.core.navigation.Navigator
import com.rodiz.arch2.feature.deck.nav.DeckHome
import com.rodiz.arch2.feature.likes.nav.LikesHome
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(ActivityRetainedComponent::class)
internal object LikesNavModule {
    @Provides
    @IntoSet
    fun provideLikesEntries(navigator: Navigator): EntryProviderInstaller = {
        entry<LikesHome> {
            LikesYouRoute(onGoToDeck = { navigator.replaceAll(DeckHome) })
        }
    }
}

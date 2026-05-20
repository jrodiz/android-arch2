package com.rodiz.arch2.feature.deck.presentation

import androidx.navigation3.runtime.entry
import com.rodiz.arch2.core.navigation.EntryProviderInstaller
import com.rodiz.arch2.core.navigation.Navigator
import com.rodiz.arch2.feature.deck.nav.DeckHome
import com.rodiz.arch2.feature.pet.nav.AddPet
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(ActivityRetainedComponent::class)
internal object DeckNavModule {
    @Provides
    @IntoSet
    fun provideDeckEntries(navigator: Navigator): EntryProviderInstaller = {
        entry<DeckHome> {
            DeckRoute(
                onBack = { navigator.goBack() },
                onAddPet = { navigator.goTo(AddPet) },
            )
        }
    }
}

package com.rodiz.arch2.feature.deck.presentation

import androidx.navigation3.runtime.entry
import com.rodiz.arch2.core.navigation.EntryProviderInstaller
import com.rodiz.arch2.core.navigation.Navigator
import com.rodiz.arch2.feature.deck.nav.DeckHome
import com.rodiz.arch2.feature.deck.nav.DeckPetDetail
import com.rodiz.arch2.feature.match.nav.MatchCelebration
import com.rodiz.arch2.feature.pet.nav.AddPet
import com.rodiz.arch2.feature.settings.nav.SettingsFilters
import com.rodiz.arch2.feature.settings.nav.SettingsNotifications
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
                onAddPet = { navigator.goTo(AddPet) },
                onOpenFilters = { navigator.goTo(SettingsFilters) },
                onOpenNotifications = { navigator.goTo(SettingsNotifications) },
                onOpenPetDetail = { petId -> navigator.goTo(DeckPetDetail(petId.value)) },
                onMatchHappened = { matchId -> navigator.goTo(MatchCelebration(matchId)) },
            )
        }
        entry<DeckPetDetail> { key ->
            DeckPetDetailRoute(
                petIdValue = key.petId,
                onBack = { navigator.goBack() },
                // Pop the detail, then show the celebration — so "Keep swiping" / back
                // from the celebration returns to the deck (or Likes), not the detail.
                // This route is shared by the deck and the Likes-You tab, so wiring it
                // here fixes the celebration for both like-back paths.
                onMatchHappened = { matchId ->
                    navigator.goBack()
                    navigator.goTo(MatchCelebration(matchId))
                },
            )
        }
    }
}

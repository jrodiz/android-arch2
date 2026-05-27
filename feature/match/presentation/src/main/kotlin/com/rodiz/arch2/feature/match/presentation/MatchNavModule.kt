package com.rodiz.arch2.feature.match.presentation

import androidx.navigation3.runtime.entry
import com.rodiz.arch2.core.navigation.EntryProviderInstaller
import com.rodiz.arch2.core.navigation.Navigator
import com.rodiz.arch2.feature.chat.nav.ChatRoute
import com.rodiz.arch2.feature.deck.nav.DeckHome
import com.rodiz.arch2.feature.match.nav.MatchCelebration
import com.rodiz.arch2.feature.match.nav.MatchesHome
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(ActivityRetainedComponent::class)
internal object MatchNavModule {
    @Provides
    @IntoSet
    fun provideMatchEntries(navigator: Navigator): EntryProviderInstaller = {
        entry<MatchesHome> {
            InboxRoute(
                onOpenMatch = { matchId -> navigator.goTo(ChatRoute(matchId)) },
                onGoToDeck = { navigator.replaceAll(DeckHome) },
            )
        }
        entry<MatchCelebration> { key ->
            MatchCelebrationRoute(
                onSayHello = {
                    // Pop the celebration off first so back from Chat lands on
                    // Deck, not on this screen. See plans/match-celebration-screen.md
                    // §2 for the rationale.
                    navigator.goBack()
                    navigator.goTo(ChatRoute(key.matchId))
                },
                onKeepSwiping = { navigator.goBack() },
            )
        }
    }
}

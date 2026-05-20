package com.rodiz.arch2.feature.chat.presentation

import androidx.navigation3.runtime.entry
import com.rodiz.arch2.core.navigation.EntryProviderInstaller
import com.rodiz.arch2.core.navigation.Navigator
import com.rodiz.arch2.feature.chat.nav.ChatRoute
import com.rodiz.arch2.feature.match.nav.MatchesHome
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(ActivityRetainedComponent::class)
internal object ChatNavModule {
    @Provides
    @IntoSet
    fun provideChatEntries(navigator: Navigator): EntryProviderInstaller = {
        entry<ChatRoute> { key ->
            ChatScreen(
                matchId = key.matchId,
                onBack = { navigator.goBack() },
                onUnmatched = { navigator.replaceAll(MatchesHome) },
            )
        }
    }
}

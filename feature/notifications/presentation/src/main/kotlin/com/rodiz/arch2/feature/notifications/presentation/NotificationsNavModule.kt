package com.rodiz.arch2.feature.notifications.presentation

import androidx.navigation3.runtime.entry
import com.rodiz.arch2.core.navigation.EntryProviderInstaller
import com.rodiz.arch2.core.navigation.Navigator
import com.rodiz.arch2.feature.deck.nav.DeckHome
import com.rodiz.arch2.feature.notifications.nav.NotificationRationale
import com.rodiz.arch2.feature.notifications.nav.NotificationRationaleOnboarding
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(ActivityRetainedComponent::class)
internal object NotificationsNavModule {
    @Provides
    @IntoSet
    fun provideNotificationsEntries(navigator: Navigator): EntryProviderInstaller = {
        entry<NotificationRationale> {
            // Settings entry — bounce back to wherever the user came from.
            NotificationRationaleRoute(onDone = { navigator.goBack() })
        }
        entry<NotificationRationaleOnboarding> {
            // Post-signup entry — clear the back stack and land on Deck regardless of choice.
            NotificationRationaleRoute(onDone = { navigator.replaceAll(DeckHome) })
        }
    }
}

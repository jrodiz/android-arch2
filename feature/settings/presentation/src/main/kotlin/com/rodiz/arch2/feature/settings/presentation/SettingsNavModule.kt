package com.rodiz.arch2.feature.settings.presentation

import androidx.navigation3.runtime.entry
import com.rodiz.arch2.core.navigation.EntryProviderInstaller
import com.rodiz.arch2.core.navigation.Navigator
import com.rodiz.arch2.feature.login.nav.LoginHome
import com.rodiz.arch2.feature.notifications.nav.NotificationRationale
import com.rodiz.arch2.feature.settings.nav.SettingsAccount
import com.rodiz.arch2.feature.settings.nav.SettingsHome
import com.rodiz.arch2.feature.settings.nav.SettingsPrivacy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(ActivityRetainedComponent::class)
internal object SettingsNavModule {
    @Provides
    @IntoSet
    fun provideSettingsEntries(navigator: Navigator): EntryProviderInstaller = {
        entry<SettingsHome> {
            SettingsHomeRoute(
                onBack = { navigator.goBack() },
                onOpenNotifications = { navigator.goTo(NotificationRationale) },
                onOpenPrivacy = { navigator.goTo(SettingsPrivacy) },
                onOpenAccount = { navigator.goTo(SettingsAccount) },
            )
        }
        entry<SettingsPrivacy> {
            PrivacyRoute(onBack = { navigator.goBack() })
        }
        entry<SettingsAccount> {
            AccountRoute(
                onBack = { navigator.goBack() },
                onDeleted = { navigator.replaceAll(LoginHome) },
            )
        }
    }
}

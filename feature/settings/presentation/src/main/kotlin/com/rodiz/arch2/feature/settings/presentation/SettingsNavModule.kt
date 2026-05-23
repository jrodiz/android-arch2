package com.rodiz.arch2.feature.settings.presentation

import androidx.navigation3.runtime.entry
import com.rodiz.arch2.core.navigation.EntryProviderInstaller
import com.rodiz.arch2.core.navigation.Navigator
import com.rodiz.arch2.feature.login.nav.LoginHome
import com.rodiz.arch2.feature.settings.nav.SettingsAccount
import com.rodiz.arch2.feature.settings.nav.SettingsBlockedUsers
import com.rodiz.arch2.feature.settings.nav.SettingsEditProfile
import com.rodiz.arch2.feature.settings.nav.SettingsFilters
import com.rodiz.arch2.feature.settings.nav.SettingsHome
import com.rodiz.arch2.feature.settings.nav.SettingsNotifications
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
                onOpenEditProfile = { navigator.goTo(SettingsEditProfile) },
                onOpenNotifications = { navigator.goTo(SettingsNotifications) },
                onOpenFilters = { navigator.goTo(SettingsFilters) },
                onOpenPrivacy = { navigator.goTo(SettingsPrivacy) },
                onOpenBlockedOwners = { navigator.goTo(SettingsBlockedUsers) },
                onOpenAccount = { navigator.goTo(SettingsAccount) },
                onSignedOut = { navigator.replaceAll(LoginHome) },
            )
        }
        entry<SettingsEditProfile> {
            EditProfileRoute(onBack = { navigator.goBack() })
        }
        entry<SettingsNotifications> {
            NotificationsRoute(onBack = { navigator.goBack() })
        }
        entry<SettingsFilters> {
            FiltersRoute(onBack = { navigator.goBack() })
        }
        entry<SettingsPrivacy> {
            PrivacyRoute(
                onBack = { navigator.goBack() },
                onOpenBlockedUsers = { navigator.goTo(SettingsBlockedUsers) },
            )
        }
        entry<SettingsBlockedUsers> {
            BlockedUsersRoute(onBack = { navigator.goBack() })
        }
        entry<SettingsAccount> {
            AccountRoute(
                onBack = { navigator.goBack() },
                onDeleted = { navigator.replaceAll(LoginHome) },
            )
        }
    }
}

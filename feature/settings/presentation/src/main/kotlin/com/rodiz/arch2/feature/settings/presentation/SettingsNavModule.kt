package com.rodiz.arch2.feature.settings.presentation

import androidx.navigation3.runtime.entry
import com.rodiz.arch2.core.navigation.EntryProviderInstaller
import com.rodiz.arch2.core.navigation.Navigator
import com.rodiz.arch2.feature.login.nav.LoginHome
import com.rodiz.arch2.feature.settings.nav.SettingsBlockedUsers
import com.rodiz.arch2.feature.settings.nav.SettingsEditProfile
import com.rodiz.arch2.feature.settings.nav.SettingsFeaturedPets
import com.rodiz.arch2.feature.settings.nav.SettingsFilters
import com.rodiz.arch2.feature.settings.nav.SettingsHelpSafety
import com.rodiz.arch2.feature.settings.nav.SettingsHome
import com.rodiz.arch2.feature.settings.nav.SettingsNotifications
import com.rodiz.arch2.feature.settings.nav.SettingsPrivacy
import com.rodiz.arch2.feature.settings.nav.SettingsPrivacyPolicy
import com.rodiz.arch2.feature.settings.nav.SettingsTerms
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
                onOpenFeaturedPets = { navigator.goTo(SettingsFeaturedPets) },
                onOpenPrivacy = { navigator.goTo(SettingsPrivacy) },
                onOpenBlockedOwners = { navigator.goTo(SettingsBlockedUsers) },
                onSignedOut = { navigator.replaceAll(LoginHome) },
            )
        }
        entry<SettingsFeaturedPets> {
            FeaturedPetsRoute(onBack = { navigator.goBack() })
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
                onOpenBlockedOwners = { navigator.goTo(SettingsBlockedUsers) },
                onOpenPrivacyPolicy = { navigator.goTo(SettingsPrivacyPolicy) },
                onAccountDeleted = { navigator.replaceAll(LoginHome) },
            )
        }
        entry<SettingsBlockedUsers> {
            BlockedUsersRoute(onBack = { navigator.goBack() })
        }
        entry<SettingsPrivacyPolicy> {
            PrivacyPolicyRoute(onBack = { navigator.goBack() })
        }
        entry<SettingsTerms> {
            TermsRoute(onBack = { navigator.goBack() })
        }
        entry<SettingsHelpSafety> {
            HelpSafetyRoute(onBack = { navigator.goBack() })
        }
    }
}

package com.rodiz.arch2.feature.profile.presentation

import androidx.navigation3.runtime.entry
import com.rodiz.arch2.core.navigation.EntryProviderInstaller
import com.rodiz.arch2.core.navigation.Navigator
import com.rodiz.arch2.feature.login.nav.LoginHome
import com.rodiz.arch2.feature.pet.nav.MyPets
import com.rodiz.arch2.feature.profile.nav.ProfileHome
import com.rodiz.arch2.feature.settings.nav.SettingsEditProfile
import com.rodiz.arch2.feature.settings.nav.SettingsHome
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(ActivityRetainedComponent::class)
internal object ProfileNavModule {
    @Provides
    @IntoSet
    fun provideProfileEntries(navigator: Navigator): EntryProviderInstaller = {
        entry<ProfileHome> {
            ProfileRoute(
                onSignedOut = { navigator.replaceAll(LoginHome) },
                onOpenMyPets = { navigator.goTo(MyPets) },
                onOpenSettings = { navigator.goTo(SettingsHome) },
                onEditProfile = { navigator.goTo(SettingsEditProfile) },
            )
        }
    }
}

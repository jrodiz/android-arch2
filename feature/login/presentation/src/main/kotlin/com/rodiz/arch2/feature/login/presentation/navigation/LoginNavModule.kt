package com.rodiz.arch2.feature.login.presentation.navigation

import androidx.navigation3.runtime.entry
import com.rodiz.arch2.core.navigation.EntryProviderInstaller
import com.rodiz.arch2.core.navigation.Navigator
import com.rodiz.arch2.feature.deck.nav.DeckHome
import com.rodiz.arch2.feature.login.nav.ForgotPassword
import com.rodiz.arch2.feature.login.nav.LoginHome
import com.rodiz.arch2.feature.login.nav.SignUpHome
import com.rodiz.arch2.feature.login.presentation.screen.ForgotPasswordStubScreen
import com.rodiz.arch2.feature.login.presentation.screen.LoginRoute
import com.rodiz.arch2.feature.login.presentation.screen.SignUpRoute
import com.rodiz.arch2.feature.notifications.nav.NotificationRationaleOnboarding
import com.rodiz.arch2.feature.settings.nav.SettingsPrivacyPolicy
import com.rodiz.arch2.feature.settings.nav.SettingsTerms
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(ActivityRetainedComponent::class)
internal object LoginNavModule {

    @Provides
    @IntoSet
    fun provideLoginEntries(navigator: Navigator): EntryProviderInstaller = {
        entry<LoginHome> {
            // Existing user signing back in — straight to Deck.
            LoginRoute(
                onNavigateHome = { navigator.replaceAll(DeckHome) },
                onForgot = { navigator.goTo(ForgotPassword) },
                onSignUp = { navigator.goTo(SignUpHome) },
            )
        }
        entry<ForgotPassword> {
            ForgotPasswordStubScreen(onBack = { navigator.goBack() })
        }
        entry<SignUpHome> {
            // New account — route through the notification rationale before landing on Deck.
            SignUpRoute(
                onNavigateHome = { navigator.replaceAll(NotificationRationaleOnboarding) },
                onNavigateBack = { navigator.goBack() },
                onOpenTerms = { navigator.goTo(SettingsTerms) },
                onOpenPrivacyPolicy = { navigator.goTo(SettingsPrivacyPolicy) },
            )
        }
    }
}

package com.rodiz.arch2.feature.login.presentation.navigation

import androidx.navigation3.runtime.entry
import com.rodiz.arch2.core.navigation.EntryProviderInstaller
import com.rodiz.arch2.core.navigation.Navigator
import com.rodiz.arch2.feature.home.nav.HomeHome
import com.rodiz.arch2.feature.login.nav.ForgotPassword
import com.rodiz.arch2.feature.login.nav.LoginHome
import com.rodiz.arch2.feature.login.nav.SignUpHome
import com.rodiz.arch2.feature.login.presentation.screen.ForgotPasswordStubScreen
import com.rodiz.arch2.feature.login.presentation.screen.LoginRoute
import com.rodiz.arch2.feature.login.presentation.screen.SignUpRoute
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
            LoginRoute(
                onNavigateHome = { navigator.replaceAll(HomeHome) },
                onForgot = { navigator.goTo(ForgotPassword) },
                onSignUp = { navigator.goTo(SignUpHome) },
            )
        }
        entry<ForgotPassword> {
            ForgotPasswordStubScreen(onBack = { navigator.goBack() })
        }
        entry<SignUpHome> {
            SignUpRoute(
                onNavigateHome = { navigator.replaceAll(HomeHome) },
                onNavigateBack = { navigator.goBack() },
            )
        }
    }
}

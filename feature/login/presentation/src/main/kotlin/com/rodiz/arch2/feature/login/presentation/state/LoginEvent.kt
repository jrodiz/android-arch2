package com.rodiz.arch2.feature.login.presentation.state

sealed interface LoginEvent {
    data object NavigateHome : LoginEvent
    data object NavigateForgot : LoginEvent
    data object PromptBiometric : LoginEvent
    data object PromptGoogleSignIn : LoginEvent
    data object ShowRegisterComingSoon : LoginEvent
}

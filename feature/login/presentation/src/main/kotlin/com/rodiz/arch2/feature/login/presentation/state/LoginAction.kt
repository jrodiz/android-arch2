package com.rodiz.arch2.feature.login.presentation.state

sealed interface LoginAction {
    data class ModeSelected(val mode: LoginMode) : LoginAction
    data class EmailChanged(val value: String) : LoginAction
    data class PasswordChanged(val value: String) : LoginAction
    data object TogglePasswordVisibility : LoginAction
    data object Submit : LoginAction
    data object BiometricRequested : LoginAction
    data object BiometricSucceeded : LoginAction
    data object GoogleSignInRequested : LoginAction
    data object ForgotPasswordTapped : LoginAction
    data object DismissError : LoginAction
}

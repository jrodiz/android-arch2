package com.rodiz.arch2.feature.login.presentation.state

import com.rodiz.arch2.feature.login.domain.model.AuthError
import com.rodiz.arch2.feature.login.domain.model.ValidationError

data class LoginUiState(
    val mode: LoginMode = LoginMode.SignIn,
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val emailError: ValidationError? = null,
    val passwordError: ValidationError? = null,
    val isSubmitting: Boolean = false,
    val transientError: AuthError? = null,
    val biometricAvailable: Boolean = false,
    val emailFormExpanded: Boolean = false,
) {
    val canSubmit: Boolean
        get() = email.isNotBlank() &&
            password.isNotBlank() &&
            emailError == null &&
            passwordError == null &&
            !isSubmitting
}

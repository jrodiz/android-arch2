package com.rodiz.arch2.feature.login.presentation.state

import com.rodiz.arch2.feature.login.domain.model.AuthError
import com.rodiz.arch2.feature.login.domain.model.ValidationError

data class SignUpUiState(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val passwordVisible: Boolean = false,
    val confirmPasswordVisible: Boolean = false,
    val avatarUri: String? = null,
    val firstNameError: ValidationError? = null,
    val lastNameError: ValidationError? = null,
    val emailError: ValidationError? = null,
    val passwordError: ValidationError? = null,
    val confirmPasswordError: ValidationError? = null,
    val isSubmitting: Boolean = false,
    val transientError: AuthError? = null,
    val showAvatarSourceSheet: Boolean = false,
    val termsAccepted: Boolean = false,
) {
    val canSubmit: Boolean
        get() = firstName.isNotBlank() &&
            lastName.isNotBlank() &&
            email.isNotBlank() &&
            password.isNotBlank() &&
            confirmPassword.isNotBlank() &&
            firstNameError == null &&
            lastNameError == null &&
            emailError == null &&
            passwordError == null &&
            confirmPasswordError == null &&
            termsAccepted &&
            !isSubmitting
}

package com.rodiz.arch2.feature.login.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rodiz.arch2.feature.login.domain.model.AuthError
import com.rodiz.arch2.feature.login.domain.model.ValidationError
import com.rodiz.arch2.feature.login.presentation.R

@Composable
internal fun AuthError.localized(): String = when (this) {
    AuthError.InvalidCredentials -> stringResource(R.string.login_error_invalid_credentials)
    AuthError.NoNetwork -> stringResource(R.string.login_error_no_network)
    AuthError.Timeout -> stringResource(R.string.login_error_timeout)
    is AuthError.Server -> stringResource(R.string.login_error_server, code)
    AuthError.GoogleSignInCancelled -> stringResource(R.string.login_google_cancelled)
    AuthError.GoogleSignInFailed -> stringResource(R.string.login_google_failed)
    AuthError.Unknown -> stringResource(R.string.login_error_unknown)
}

@Composable
internal fun ValidationError.localized(): String = when (this) {
    ValidationError.EmailEmpty -> stringResource(R.string.login_validation_email_empty)
    ValidationError.EmailMalformed -> stringResource(R.string.login_validation_email_malformed)
    ValidationError.PasswordEmpty -> stringResource(R.string.login_validation_password_empty)
    is ValidationError.PasswordTooShort -> stringResource(R.string.login_validation_password_short, minLength)
}

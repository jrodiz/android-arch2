package com.rodiz.arch2.feature.login.domain.model

import com.rodiz.arch2.core.common.result.AppError

sealed interface AuthError : AppError {
    data object InvalidCredentials : AuthError
    data object NoNetwork : AuthError
    data object Timeout : AuthError
    data class Server(val code: Int) : AuthError
    data object GoogleSignInCancelled : AuthError

    /**
     * Credential Manager opened but had no Google account to surface — the
     * device hasn't been signed into Google. Distinct from
     * [GoogleSignInCancelled] so the UI can tell the user *what* to do
     * instead of the generic "try again".
     */
    data object GoogleNoAccount : AuthError
    data object GoogleSignInFailed : AuthError
    data object EmailAlreadyInUse : AuthError
    data object WeakPassword : AuthError
    data object AvatarUploadFailed : AuthError
    data object Unknown : AuthError
}

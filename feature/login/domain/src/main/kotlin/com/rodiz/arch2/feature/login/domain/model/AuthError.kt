package com.rodiz.arch2.feature.login.domain.model

import com.rodiz.arch2.core.common.result.AppError

sealed interface AuthError : AppError {
    data object InvalidCredentials : AuthError
    data object NoNetwork : AuthError
    data object Timeout : AuthError
    data class Server(val code: Int) : AuthError
    data object GoogleSignInCancelled : AuthError
    data object GoogleSignInFailed : AuthError
    data object Unknown : AuthError
}

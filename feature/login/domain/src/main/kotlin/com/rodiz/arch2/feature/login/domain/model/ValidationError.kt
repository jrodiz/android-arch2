package com.rodiz.arch2.feature.login.domain.model

sealed interface ValidationError {
    data object EmailEmpty : ValidationError
    data object EmailMalformed : ValidationError
    data object PasswordEmpty : ValidationError
    data class PasswordTooShort(val minLength: Int) : ValidationError
}

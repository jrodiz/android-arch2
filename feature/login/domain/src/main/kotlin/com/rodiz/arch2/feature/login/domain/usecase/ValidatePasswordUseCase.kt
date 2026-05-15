package com.rodiz.arch2.feature.login.domain.usecase

import com.rodiz.arch2.feature.login.domain.model.ValidationError

class ValidatePasswordUseCase {
    operator fun invoke(password: String): ValidationError? = when {
        password.isEmpty() -> ValidationError.PasswordEmpty
        password.length < MIN_LENGTH -> ValidationError.PasswordTooShort(MIN_LENGTH)
        else -> null
    }

    companion object {
        const val MIN_LENGTH = 8
    }
}

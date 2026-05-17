package com.rodiz.arch2.feature.login.domain.usecase

import com.rodiz.arch2.feature.login.domain.model.ValidationError

class ValidateConfirmPasswordUseCase {
    operator fun invoke(password: String, confirm: String): ValidationError? =
        if (confirm != password) ValidationError.ConfirmPasswordMismatch else null
}

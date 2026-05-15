package com.rodiz.arch2.feature.login.domain.usecase

import com.rodiz.arch2.feature.login.domain.model.ValidationError

class ValidateEmailUseCase {
    operator fun invoke(email: String): ValidationError? = when {
        email.isBlank() -> ValidationError.EmailEmpty
        !EmailRegex.matches(email) -> ValidationError.EmailMalformed
        else -> null
    }

    private companion object {
        // RFC 5322 simplified — local@domain.tld
        val EmailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}

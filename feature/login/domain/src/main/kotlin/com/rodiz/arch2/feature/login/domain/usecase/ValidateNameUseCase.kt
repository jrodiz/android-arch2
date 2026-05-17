package com.rodiz.arch2.feature.login.domain.usecase

import com.rodiz.arch2.feature.login.domain.model.ValidationError

enum class NameField { First, Last }

class ValidateNameUseCase {
    operator fun invoke(value: String, field: NameField): ValidationError? {
        val trimmed = value.trim()
        return when {
            trimmed.isEmpty() -> when (field) {
                NameField.First -> ValidationError.FirstNameEmpty
                NameField.Last -> ValidationError.LastNameEmpty
            }
            trimmed.length > MAX_LENGTH -> ValidationError.NameTooLong(MAX_LENGTH)
            else -> null
        }
    }

    companion object {
        const val MAX_LENGTH = 50
    }
}

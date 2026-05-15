package com.rodiz.arch2.feature.login.domain

import com.rodiz.arch2.feature.login.domain.model.ValidationError
import com.rodiz.arch2.feature.login.domain.usecase.ValidatePasswordUseCase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ValidatePasswordUseCaseTest {

    private val validate = ValidatePasswordUseCase()

    @Test
    fun `empty password returns PasswordEmpty`() {
        assertEquals(ValidationError.PasswordEmpty, validate(""))
    }

    @Test
    fun `password shorter than min returns PasswordTooShort`() {
        val result = validate("1234567")
        assertEquals(ValidationError.PasswordTooShort(ValidatePasswordUseCase.MIN_LENGTH), result)
    }

    @Test
    fun `password equal to or longer than min returns null`() {
        assertNull(validate("12345678"))
        assertNull(validate("a-much-longer-password"))
    }
}

package com.rodiz.arch2.feature.login.domain

import com.rodiz.arch2.feature.login.domain.model.ValidationError
import com.rodiz.arch2.feature.login.domain.usecase.ValidateConfirmPasswordUseCase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ValidateConfirmPasswordUseCaseTest {

    private val validate = ValidateConfirmPasswordUseCase()

    @Test
    fun `matching passwords return null`() {
        assertNull(validate("password1", "password1"))
    }

    @Test
    fun `mismatched passwords return ConfirmPasswordMismatch`() {
        assertEquals(ValidationError.ConfirmPasswordMismatch, validate("password1", "password2"))
    }

    @Test
    fun `both empty returns null since they trivially match`() {
        assertNull(validate("", ""))
    }

    @Test
    fun `case-sensitive mismatch is rejected`() {
        assertEquals(ValidationError.ConfirmPasswordMismatch, validate("Password1", "password1"))
    }
}

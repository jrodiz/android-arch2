package com.rodiz.arch2.feature.login.domain

import com.rodiz.arch2.feature.login.domain.model.ValidationError
import com.rodiz.arch2.feature.login.domain.usecase.ValidateEmailUseCase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ValidateEmailUseCaseTest {

    private val validate = ValidateEmailUseCase()

    @Test
    fun `empty email returns EmailEmpty`() {
        assertEquals(ValidationError.EmailEmpty, validate(""))
        assertEquals(ValidationError.EmailEmpty, validate("   "))
    }

    @Test
    fun `malformed email returns EmailMalformed`() {
        assertEquals(ValidationError.EmailMalformed, validate("not-an-email"))
        assertEquals(ValidationError.EmailMalformed, validate("missing@tld"))
        assertEquals(ValidationError.EmailMalformed, validate("@no-local.com"))
        assertEquals(ValidationError.EmailMalformed, validate("spaces are@bad.com"))
    }

    @Test
    fun `valid email returns null`() {
        assertNull(validate("user@example.com"))
        assertNull(validate("first.last+tag@sub.example.co"))
    }
}

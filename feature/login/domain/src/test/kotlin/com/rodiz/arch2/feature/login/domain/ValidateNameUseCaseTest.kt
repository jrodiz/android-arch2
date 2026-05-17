package com.rodiz.arch2.feature.login.domain

import com.rodiz.arch2.feature.login.domain.model.ValidationError
import com.rodiz.arch2.feature.login.domain.usecase.NameField
import com.rodiz.arch2.feature.login.domain.usecase.ValidateNameUseCase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ValidateNameUseCaseTest {

    private val validate = ValidateNameUseCase()

    @Test
    fun `empty first name returns FirstNameEmpty`() {
        assertEquals(ValidationError.FirstNameEmpty, validate("", NameField.First))
    }

    @Test
    fun `blank first name returns FirstNameEmpty`() {
        assertEquals(ValidationError.FirstNameEmpty, validate("   ", NameField.First))
    }

    @Test
    fun `empty last name returns LastNameEmpty`() {
        assertEquals(ValidationError.LastNameEmpty, validate("", NameField.Last))
    }

    @Test
    fun `name longer than max returns NameTooLong`() {
        val tooLong = "a".repeat(ValidateNameUseCase.MAX_LENGTH + 1)
        val result = validate(tooLong, NameField.First)
        assertEquals(ValidationError.NameTooLong(ValidateNameUseCase.MAX_LENGTH), result)
    }

    @Test
    fun `valid name returns null`() {
        assertNull(validate("Steve", NameField.First))
        assertNull(validate("Rogers", NameField.Last))
        assertNull(validate("  Steve  ", NameField.First))
    }
}

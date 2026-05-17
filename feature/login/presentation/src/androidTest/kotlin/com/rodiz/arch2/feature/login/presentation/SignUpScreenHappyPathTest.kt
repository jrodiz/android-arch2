package com.rodiz.arch2.feature.login.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.rodiz.arch2.feature.login.domain.model.ValidationError
import com.rodiz.arch2.feature.login.presentation.screen.SignUpScreen
import com.rodiz.arch2.feature.login.presentation.state.SignUpAction
import com.rodiz.arch2.feature.login.presentation.state.SignUpUiState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI happy-path: fill every field with valid input and assert the Register
 * button enables + dispatches Submit when clicked. Validation logic mirrors the real
 * use cases just enough to drive the screen — full validator coverage lives in
 * the domain test module.
 */
class SignUpScreenHappyPathTest {

    @get:Rule val compose = createComposeRule()

    @Test
    fun typingAllValidInputs_enablesRegister_and_dispatchesSubmit() {
        var submitDispatched = false
        compose.setContent {
            var state by androidx.compose.runtime.remember { mutableStateOf(SignUpUiState()) }
            SignUpScreen(
                state = state,
                onAction = { action ->
                    state = state.applyForTest(action)
                    if (action is SignUpAction.Submit) submitDispatched = true
                },
            )
        }

        compose.onNodeWithTag("signup_submit").assertIsNotEnabled()
        compose.onNodeWithTag("signup_first_name_field").performTextInput("Steve")
        compose.onNodeWithTag("signup_last_name_field").performTextInput("Rogers")
        compose.onNodeWithTag("email_field").performTextInput("steve@example.com")
        compose.onNodeWithTag("signup_password_field").performTextInput("password1")
        compose.onNodeWithTag("signup_confirm_password_field").performTextInput("password1")
        compose.onNodeWithTag("signup_submit").assertIsEnabled()
        compose.onNodeWithTag("signup_submit").performClick()
        assertTrue(submitDispatched)
    }

    @Test
    fun mismatchedConfirmPassword_keepsRegisterDisabled() {
        compose.setContent {
            var state by androidx.compose.runtime.remember { mutableStateOf(SignUpUiState()) }
            SignUpScreen(
                state = state,
                onAction = { action -> state = state.applyForTest(action) },
            )
        }

        compose.onNodeWithTag("signup_first_name_field").performTextInput("Steve")
        compose.onNodeWithTag("signup_last_name_field").performTextInput("Rogers")
        compose.onNodeWithTag("email_field").performTextInput("steve@example.com")
        compose.onNodeWithTag("signup_password_field").performTextInput("password1")
        compose.onNodeWithTag("signup_confirm_password_field").performTextInput("password2")
        compose.onNodeWithTag("signup_submit").assertIsNotEnabled()
    }
}

private fun SignUpUiState.applyForTest(action: SignUpAction): SignUpUiState = when (action) {
    is SignUpAction.FirstNameChanged -> copy(
        firstName = action.value,
        firstNameError = if (action.value.isBlank()) ValidationError.FirstNameEmpty else null,
    )
    is SignUpAction.LastNameChanged -> copy(
        lastName = action.value,
        lastNameError = if (action.value.isBlank()) ValidationError.LastNameEmpty else null,
    )
    is SignUpAction.EmailChanged -> copy(
        email = action.value,
        emailError = if (action.value.contains('@') && action.value.contains('.')) {
            null
        } else {
            ValidationError.EmailMalformed
        },
    )
    is SignUpAction.PasswordChanged -> copy(
        password = action.value,
        passwordError = if (action.value.length >= 8) null else ValidationError.PasswordTooShort(8),
        confirmPasswordError = when {
            confirmPassword.isEmpty() -> confirmPasswordError
            confirmPassword == action.value -> null
            else -> ValidationError.ConfirmPasswordMismatch
        },
    )
    is SignUpAction.ConfirmPasswordChanged -> copy(
        confirmPassword = action.value,
        confirmPasswordError = if (action.value == password) {
            null
        } else {
            ValidationError.ConfirmPasswordMismatch
        },
    )
    SignUpAction.TogglePasswordVisibility -> copy(passwordVisible = !passwordVisible)
    SignUpAction.ToggleConfirmPasswordVisibility ->
        copy(confirmPasswordVisible = !confirmPasswordVisible)
    else -> this
}

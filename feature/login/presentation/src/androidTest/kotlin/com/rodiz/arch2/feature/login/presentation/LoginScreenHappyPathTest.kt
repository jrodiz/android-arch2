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
import com.rodiz.arch2.feature.login.presentation.screen.LoginScreen
import com.rodiz.arch2.feature.login.presentation.state.LoginAction
import com.rodiz.arch2.feature.login.presentation.state.LoginUiState
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI happy-path test. Stateless screen is driven by a mutable UiState the
 * test owns. We type into the fields, then assert the submit button becomes enabled
 * and dispatches Submit when clicked.
 */
class LoginScreenHappyPathTest {

    @get:Rule val compose = createComposeRule()

    @Test
    fun typingValidInputs_enablesSubmit_and_dispatchesSubmit() {
        var submitDispatched = false
        compose.setContent {
            var state by androidx.compose.runtime.remember { mutableStateOf(LoginUiState()) }
            LoginScreen(
                state = state,
                onAction = { action ->
                    when (action) {
                        is LoginAction.EmailChanged -> state = state.copy(
                            email = action.value,
                            emailError = if (action.value.contains('@') && action.value.contains('.')) null
                            else ValidationError.EmailMalformed,
                        )
                        is LoginAction.PasswordChanged -> state = state.copy(
                            password = action.value,
                            passwordError = if (action.value.length >= 8) null else ValidationError.PasswordTooShort(8),
                        )
                        LoginAction.Submit -> submitDispatched = true
                        LoginAction.TogglePasswordVisibility -> state = state.copy(passwordVisible = !state.passwordVisible)
                        LoginAction.ShowEmailForm -> state = state.copy(emailFormExpanded = true)
                        else -> Unit
                    }
                },
            )
        }

        compose.onNodeWithTag("login_email_form_show").performClick()
        compose.onNodeWithTag("login_submit").assertIsNotEnabled()
        compose.onNodeWithTag("email_field").performTextInput("user@example.com")
        compose.onNodeWithTag("password_field").performTextInput("password1")
        compose.onNodeWithTag("login_submit").assertIsEnabled()
        compose.onNodeWithTag("login_submit").performClick()
        assertTrue(submitDispatched)
    }
}

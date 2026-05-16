package com.rodiz.arch2.feature.login.presentation

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.rodiz.arch2.core.common.result.Try
import com.rodiz.arch2.core.session.domain.Session
import com.rodiz.arch2.core.testing.MainDispatcherExtension
import com.rodiz.arch2.feature.login.domain.model.AuthError
import com.rodiz.arch2.feature.login.domain.model.Credentials
import com.rodiz.arch2.feature.login.domain.model.ValidationError
import com.rodiz.arch2.feature.login.domain.repository.AuthRepository
import com.rodiz.arch2.feature.login.domain.usecase.LoginUseCase
import com.rodiz.arch2.feature.login.domain.usecase.LoginWithBiometricUseCase
import com.rodiz.arch2.feature.login.domain.usecase.SignInWithGoogleUseCase
import com.rodiz.arch2.feature.login.domain.usecase.ValidateEmailUseCase
import com.rodiz.arch2.feature.login.domain.usecase.ValidatePasswordUseCase
import com.rodiz.arch2.feature.login.presentation.state.LoginAction
import com.rodiz.arch2.feature.login.presentation.state.LoginEvent
import com.rodiz.arch2.feature.login.presentation.state.LoginMode
import com.rodiz.arch2.feature.login.presentation.viewmodel.LoginViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcherExt = MainDispatcherExtension()

    private val testDispatcher get() = mainDispatcherExt.dispatcher

    @Test
    fun `valid inputs flip canSubmit to true`() = runTest(testDispatcher) {
        val vm = newViewModel()
        vm.state.test {
            assertFalse(awaitItem().canSubmit)
            vm.onAction(LoginAction.EmailChanged("user@example.com"))
            assertFalse(awaitItem().canSubmit)
            vm.onAction(LoginAction.PasswordChanged("password1"))
            val updated = awaitItem()
            assertTrue(updated.canSubmit)
            assertNull(updated.emailError)
            assertNull(updated.passwordError)
        }
    }

    @Test
    fun `malformed email surfaces EmailMalformed validation error`() = runTest(testDispatcher) {
        val vm = newViewModel()
        vm.onAction(LoginAction.EmailChanged("not-email"))
        vm.state.test {
            val state = awaitItem()
            assertEquals(ValidationError.EmailMalformed, state.emailError)
            assertFalse(state.canSubmit)
        }
    }

    @Test
    fun `submit success emits NavigateHome event and clears isSubmitting`() = runTest(testDispatcher) {
        val repo = FakeAuthRepo(loginResult = Try.Success(Session("u", "t")))
        val vm = newViewModel(repo = repo)
        vm.onAction(LoginAction.EmailChanged("user@example.com"))
        vm.onAction(LoginAction.PasswordChanged("password1"))

        vm.events.test {
            vm.onAction(LoginAction.Submit)
            advanceUntilIdle()
            assertEquals(LoginEvent.NavigateHome, awaitItem())
        }
        assertFalse(vm.state.value.isSubmitting)
    }

    @Test
    fun `each AuthError sets transientError and clears isSubmitting`() = runTest(testDispatcher) {
        val errors: List<AuthError> = listOf(
            AuthError.InvalidCredentials,
            AuthError.NoNetwork,
            AuthError.Timeout,
            AuthError.Server(500),
            AuthError.Unknown,
        )
        errors.forEach { error ->
            val repo = FakeAuthRepo(loginResult = Try.Failure(error))
            val vm = newViewModel(repo = repo)
            vm.onAction(LoginAction.EmailChanged("user@example.com"))
            vm.onAction(LoginAction.PasswordChanged("password1"))
            vm.onAction(LoginAction.Submit)
            advanceUntilIdle()
            val state = vm.state.value
            assertEquals(error, state.transientError)
            assertFalse(state.isSubmitting, "isSubmitting should be false for ${'$'}error")
        }
    }

    @Test
    fun `GoogleSignInRequested emits PromptGoogleSignIn event`() = runTest(testDispatcher) {
        val vm = newViewModel()
        vm.events.test {
            vm.onAction(LoginAction.GoogleSignInRequested)
            assertEquals(LoginEvent.PromptGoogleSignIn, awaitItem())
        }
    }

    @Test
    fun `onGoogleIdToken success emits NavigateHome and clears isSubmitting`() = runTest(testDispatcher) {
        val session = Session(userId = "google-uid", token = "t", displayName = "Joe Demo")
        val repo = FakeAuthRepo(googleResult = Try.Success(session))
        val vm = newViewModel(repo = repo)

        vm.events.test {
            vm.onGoogleIdToken("fake.idtoken")
            advanceUntilIdle()
            assertEquals(LoginEvent.NavigateHome, awaitItem())
        }
        assertFalse(vm.state.value.isSubmitting)
    }

    @Test
    fun `onGoogleIdToken failure sets transientError and clears isSubmitting`() = runTest(testDispatcher) {
        val errors: List<AuthError> = listOf(
            AuthError.GoogleSignInCancelled,
            AuthError.GoogleSignInFailed,
            AuthError.NoNetwork,
            AuthError.Unknown,
        )
        errors.forEach { error ->
            val repo = FakeAuthRepo(googleResult = Try.Failure(error))
            val vm = newViewModel(repo = repo)
            vm.onGoogleIdToken("fake.idtoken")
            advanceUntilIdle()
            val state = vm.state.value
            assertEquals(error, state.transientError)
            assertFalse(state.isSubmitting, "isSubmitting should be false for ${'$'}error")
        }
    }

    @Test
    fun `onGoogleSignInFailed surfaces error without contacting the use case`() = runTest(testDispatcher) {
        val vm = newViewModel()
        vm.onGoogleSignInFailed(AuthError.GoogleSignInCancelled)
        val state = vm.state.value
        assertEquals(AuthError.GoogleSignInCancelled, state.transientError)
        assertFalse(state.isSubmitting)
    }

    @Test
    fun `password visibility toggles`() = runTest(testDispatcher) {
        val vm = newViewModel()
        assertFalse(vm.state.value.passwordVisible)
        vm.onAction(LoginAction.TogglePasswordVisibility)
        assertTrue(vm.state.value.passwordVisible)
        vm.onAction(LoginAction.TogglePasswordVisibility)
        assertFalse(vm.state.value.passwordVisible)
    }

    @Test
    fun `email persisted to SavedStateHandle survives ViewModel recreation`() = runTest(testDispatcher) {
        val savedState = SavedStateHandle()
        val first = newViewModel(savedStateHandle = savedState)
        first.onAction(LoginAction.EmailChanged("user@example.com"))
        val recreated = newViewModel(savedStateHandle = savedState)
        assertEquals("user@example.com", recreated.state.value.email)
    }

    @Test
    fun `ModeSelected updates state mode and clears transientError`() = runTest(testDispatcher) {
        val repo = FakeAuthRepo(loginResult = Try.Failure(AuthError.NoNetwork))
        val vm = newViewModel(repo = repo)
        vm.onAction(LoginAction.EmailChanged("user@example.com"))
        vm.onAction(LoginAction.PasswordChanged("password1"))
        vm.onAction(LoginAction.Submit)
        advanceUntilIdle()
        assertNotNull(vm.state.value.transientError)

        vm.onAction(LoginAction.ModeSelected(LoginMode.SignUp))
        assertEquals(LoginMode.SignUp, vm.state.value.mode)
        assertNull(vm.state.value.transientError)
    }

    @Test
    fun `submit in SignUp mode emits ShowRegisterComingSoon and skips loginUseCase`() = runTest(testDispatcher) {
        val repo = FakeAuthRepo(loginResult = Try.Success(Session("u", "t")))
        val vm = newViewModel(repo = repo)
        vm.onAction(LoginAction.ModeSelected(LoginMode.SignUp))
        vm.onAction(LoginAction.EmailChanged("user@example.com"))
        vm.onAction(LoginAction.PasswordChanged("password1"))

        vm.events.test {
            vm.onAction(LoginAction.Submit)
            advanceUntilIdle()
            assertEquals(LoginEvent.ShowRegisterComingSoon, awaitItem())
        }
        assertFalse(vm.state.value.isSubmitting)
    }

    @Test
    fun `dismiss error clears transientError`() = runTest(testDispatcher) {
        val repo = FakeAuthRepo(loginResult = Try.Failure(AuthError.NoNetwork))
        val vm = newViewModel(repo = repo)
        vm.onAction(LoginAction.EmailChanged("user@example.com"))
        vm.onAction(LoginAction.PasswordChanged("password1"))
        vm.onAction(LoginAction.Submit)
        advanceUntilIdle()
        assertNotNull(vm.state.value.transientError)
        vm.onAction(LoginAction.DismissError)
        assertNull(vm.state.value.transientError)
    }

    private fun newViewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        repo: AuthRepository = FakeAuthRepo(),
    ): LoginViewModel = LoginViewModel(
        savedStateHandle = savedStateHandle,
        loginUseCase = LoginUseCase(repo),
        loginWithBiometricUseCase = LoginWithBiometricUseCase(repo),
        signInWithGoogleUseCase = SignInWithGoogleUseCase(repo),
        validateEmail = ValidateEmailUseCase(),
        validatePassword = ValidatePasswordUseCase(),
        authRepository = repo,
    )

    private class FakeAuthRepo(
        val loginResult: Try<Session, AuthError> = Try.Success(Session("u", "t")),
        val googleResult: Try<Session, AuthError> = loginResult,
        val hasStored: Boolean = false,
    ) : AuthRepository {
        override suspend fun login(credentials: Credentials) = loginResult
        override suspend fun loginWithStoredCredentials() = loginResult
        override suspend fun signInWithGoogle(idToken: String) = googleResult
        override suspend fun hasStoredCredentials() = hasStored
    }
}

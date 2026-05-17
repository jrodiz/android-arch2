package com.rodiz.arch2.feature.login.presentation

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.rodiz.arch2.core.common.result.Try
import com.rodiz.arch2.core.session.domain.Session
import com.rodiz.arch2.core.testing.MainDispatcherExtension
import com.rodiz.arch2.feature.login.domain.model.AuthError
import com.rodiz.arch2.feature.login.domain.model.Credentials
import com.rodiz.arch2.feature.login.domain.model.SignUpRequest
import com.rodiz.arch2.feature.login.domain.model.ValidationError
import com.rodiz.arch2.feature.login.domain.repository.AuthRepository
import com.rodiz.arch2.feature.login.domain.usecase.RegisterUseCase
import com.rodiz.arch2.feature.login.domain.usecase.ValidateConfirmPasswordUseCase
import com.rodiz.arch2.feature.login.domain.usecase.ValidateEmailUseCase
import com.rodiz.arch2.feature.login.domain.usecase.ValidateNameUseCase
import com.rodiz.arch2.feature.login.domain.usecase.ValidatePasswordUseCase
import com.rodiz.arch2.feature.login.presentation.state.SignUpAction
import com.rodiz.arch2.feature.login.presentation.state.SignUpEvent
import com.rodiz.arch2.feature.login.presentation.state.SoftWarning
import com.rodiz.arch2.feature.login.presentation.viewmodel.SignUpViewModel
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
class SignUpViewModelTest {

    @JvmField
    @RegisterExtension
    val mainDispatcherExt = MainDispatcherExtension()

    private val testDispatcher get() = mainDispatcherExt.dispatcher

    @Test
    fun `submit with all-blank fields surfaces per-field validation errors`() = runTest(testDispatcher) {
        val vm = newViewModel()
        vm.onAction(SignUpAction.Submit)
        val state = vm.state.value
        assertEquals(ValidationError.FirstNameEmpty, state.firstNameError)
        assertEquals(ValidationError.LastNameEmpty, state.lastNameError)
        assertEquals(ValidationError.EmailEmpty, state.emailError)
        assertEquals(ValidationError.PasswordEmpty, state.passwordError)
        // Confirm matches password (both empty) so no mismatch is flagged.
        assertNull(state.confirmPasswordError)
        assertFalse(state.isSubmitting)
    }

    @Test
    fun `mismatched confirm password surfaces ConfirmPasswordMismatch on submit`() = runTest(testDispatcher) {
        val vm = newViewModel()
        fillValidExceptConfirm(vm)
        vm.onAction(SignUpAction.ConfirmPasswordChanged("differentpassword"))
        vm.onAction(SignUpAction.Submit)
        assertEquals(ValidationError.ConfirmPasswordMismatch, vm.state.value.confirmPasswordError)
        assertFalse(vm.state.value.isSubmitting)
    }

    @Test
    fun `valid submit success emits NavigateHome and clears isSubmitting`() = runTest(testDispatcher) {
        val repo = FakeAuthRepo(registerResult = Try.Success(Session("u1", "t1", displayName = "Steve Rogers")))
        val vm = newViewModel(repo = repo)
        fillValid(vm)

        vm.events.test {
            vm.onAction(SignUpAction.Submit)
            advanceUntilIdle()
            assertEquals(SignUpEvent.NavigateHome, awaitItem())
        }
        assertFalse(vm.state.value.isSubmitting)
    }

    @Test
    fun `each AuthError populates transientError`() = runTest(testDispatcher) {
        val errors: List<AuthError> = listOf(
            AuthError.EmailAlreadyInUse,
            AuthError.WeakPassword,
            AuthError.NoNetwork,
            AuthError.Unknown,
        )
        errors.forEach { error ->
            val repo = FakeAuthRepo(registerResult = Try.Failure(error))
            val vm = newViewModel(repo = repo)
            fillValid(vm)
            vm.onAction(SignUpAction.Submit)
            advanceUntilIdle()
            val state = vm.state.value
            assertEquals(error, state.transientError, "AuthError ${'$'}error should populate transientError")
            assertFalse(state.isSubmitting)
        }
    }

    @Test
    fun `AvatarUploadFailed emits both ShowSoftWarning and NavigateHome`() = runTest(testDispatcher) {
        val repo = FakeAuthRepo(registerResult = Try.Failure(AuthError.AvatarUploadFailed))
        val vm = newViewModel(repo = repo)
        fillValid(vm)

        vm.events.test {
            vm.onAction(SignUpAction.Submit)
            advanceUntilIdle()
            assertEquals(SignUpEvent.ShowSoftWarning(SoftWarning.AvatarUploadFailed), awaitItem())
            assertEquals(SignUpEvent.NavigateHome, awaitItem())
        }
        assertFalse(vm.state.value.isSubmitting)
        // The avatar-failed path does NOT set transientError — the account exists.
        assertNull(vm.state.value.transientError)
    }

    @Test
    fun `AvatarSelected populates avatarUri and dismisses sheet`() = runTest(testDispatcher) {
        val vm = newViewModel()
        vm.onAction(SignUpAction.PickAvatarTapped)
        assertTrue(vm.state.value.showAvatarSourceSheet)

        vm.onAction(SignUpAction.AvatarSelected("content://media/picker/img1"))
        val state = vm.state.value
        assertEquals("content://media/picker/img1", state.avatarUri)
        assertFalse(state.showAvatarSourceSheet)
    }

    @Test
    fun `AvatarCleared resets avatar to null`() = runTest(testDispatcher) {
        val vm = newViewModel()
        vm.onAction(SignUpAction.AvatarSelected("content://media/picker/img1"))
        assertNotNull(vm.state.value.avatarUri)
        vm.onAction(SignUpAction.AvatarCleared)
        assertNull(vm.state.value.avatarUri)
    }

    @Test
    fun `PickFromGallery emits LaunchGalleryPicker and dismisses sheet`() = runTest(testDispatcher) {
        val vm = newViewModel()
        vm.onAction(SignUpAction.PickAvatarTapped)
        vm.events.test {
            vm.onAction(SignUpAction.PickFromGallery)
            assertEquals(SignUpEvent.LaunchGalleryPicker, awaitItem())
        }
        assertFalse(vm.state.value.showAvatarSourceSheet)
    }

    @Test
    fun `PickFromCamera emits LaunchCamera and dismisses sheet`() = runTest(testDispatcher) {
        val vm = newViewModel()
        vm.onAction(SignUpAction.PickAvatarTapped)
        vm.events.test {
            vm.onAction(SignUpAction.PickFromCamera)
            assertEquals(SignUpEvent.LaunchCamera, awaitItem())
        }
        assertFalse(vm.state.value.showAvatarSourceSheet)
    }

    @Test
    fun `password change re-runs confirm validation`() = runTest(testDispatcher) {
        val vm = newViewModel()
        vm.onAction(SignUpAction.PasswordChanged("password1"))
        vm.onAction(SignUpAction.ConfirmPasswordChanged("password2"))
        assertEquals(ValidationError.ConfirmPasswordMismatch, vm.state.value.confirmPasswordError)

        // Edit password to match confirm — error should clear.
        vm.onAction(SignUpAction.PasswordChanged("password2"))
        assertNull(vm.state.value.confirmPasswordError)
    }

    @Test
    fun `non-sensitive fields persist to SavedStateHandle, passwords do not`() = runTest(testDispatcher) {
        val savedState = SavedStateHandle()
        val first = newViewModel(savedStateHandle = savedState)
        first.onAction(SignUpAction.FirstNameChanged("Steve"))
        first.onAction(SignUpAction.LastNameChanged("Rogers"))
        first.onAction(SignUpAction.EmailChanged("steve@example.com"))
        first.onAction(SignUpAction.PasswordChanged("password1"))
        first.onAction(SignUpAction.ConfirmPasswordChanged("password1"))
        first.onAction(SignUpAction.AvatarSelected("content://media/picker/img1"))

        val recreated = newViewModel(savedStateHandle = savedState)
        val state = recreated.state.value
        assertEquals("Steve", state.firstName)
        assertEquals("Rogers", state.lastName)
        assertEquals("steve@example.com", state.email)
        assertEquals("content://media/picker/img1", state.avatarUri)
        // Passwords are deliberately NOT persisted.
        assertEquals("", state.password)
        assertEquals("", state.confirmPassword)
    }

    @Test
    fun `BackTapped emits NavigateBack`() = runTest(testDispatcher) {
        val vm = newViewModel()
        vm.events.test {
            vm.onAction(SignUpAction.BackTapped)
            assertEquals(SignUpEvent.NavigateBack, awaitItem())
        }
    }

    private fun fillValid(vm: SignUpViewModel) {
        fillValidExceptConfirm(vm)
        vm.onAction(SignUpAction.ConfirmPasswordChanged("password1"))
    }

    private fun fillValidExceptConfirm(vm: SignUpViewModel) {
        vm.onAction(SignUpAction.FirstNameChanged("Steve"))
        vm.onAction(SignUpAction.LastNameChanged("Rogers"))
        vm.onAction(SignUpAction.EmailChanged("steve@example.com"))
        vm.onAction(SignUpAction.PasswordChanged("password1"))
    }

    private fun newViewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        repo: AuthRepository = FakeAuthRepo(),
    ): SignUpViewModel = SignUpViewModel(
        savedStateHandle = savedStateHandle,
        registerUseCase = RegisterUseCase(repo),
        validateName = ValidateNameUseCase(),
        validateEmail = ValidateEmailUseCase(),
        validatePassword = ValidatePasswordUseCase(),
        validateConfirmPassword = ValidateConfirmPasswordUseCase(),
    )

    private class FakeAuthRepo(
        val registerResult: Try<Session, AuthError> = Try.Success(Session("u", "t")),
    ) : AuthRepository {
        override suspend fun login(credentials: Credentials) = error("not used in sign-up test")
        override suspend fun loginWithStoredCredentials() = error("not used in sign-up test")
        override suspend fun signInWithGoogle(idToken: String) = error("not used in sign-up test")
        override suspend fun register(request: SignUpRequest) = registerResult
        override suspend fun hasStoredCredentials() = false
    }
}

package com.rodiz.arch2.feature.login.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodiz.arch2.core.common.result.Try
import com.rodiz.arch2.core.session.domain.Session
import com.rodiz.arch2.feature.login.domain.model.AuthError
import com.rodiz.arch2.feature.login.domain.model.SignUpRequest
import com.rodiz.arch2.feature.login.domain.usecase.NameField
import com.rodiz.arch2.feature.login.domain.usecase.RegisterUseCase
import com.rodiz.arch2.feature.login.domain.usecase.ValidateConfirmPasswordUseCase
import com.rodiz.arch2.feature.login.domain.usecase.ValidateEmailUseCase
import com.rodiz.arch2.feature.login.domain.usecase.ValidateNameUseCase
import com.rodiz.arch2.feature.login.domain.usecase.ValidatePasswordUseCase
import com.rodiz.arch2.feature.login.presentation.R
import com.rodiz.arch2.feature.login.presentation.state.SignUpAction
import com.rodiz.arch2.feature.login.presentation.state.SignUpEvent
import com.rodiz.arch2.feature.login.presentation.state.SignUpUiState
import com.rodiz.arch2.feature.login.presentation.state.SoftWarning
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val registerUseCase: RegisterUseCase,
    private val validateName: ValidateNameUseCase,
    private val validateEmail: ValidateEmailUseCase,
    private val validatePassword: ValidatePasswordUseCase,
    private val validateConfirmPassword: ValidateConfirmPasswordUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(
        SignUpUiState(
            firstName = savedStateHandle.get<String>(KEY_FIRST_NAME).orEmpty(),
            lastName = savedStateHandle.get<String>(KEY_LAST_NAME).orEmpty(),
            email = savedStateHandle.get<String>(KEY_EMAIL).orEmpty(),
            avatarUri = savedStateHandle.get<String>(KEY_AVATAR_URI),
        ),
    )
    val state: StateFlow<SignUpUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<SignUpEvent>(
        extraBufferCapacity = 2,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<SignUpEvent> = _events.asSharedFlow()

    fun onAction(action: SignUpAction) {
        when (action) {
            is SignUpAction.FirstNameChanged -> onFirstNameChanged(action.value)
            is SignUpAction.LastNameChanged -> onLastNameChanged(action.value)
            is SignUpAction.EmailChanged -> onEmailChanged(action.value)
            is SignUpAction.PasswordChanged -> onPasswordChanged(action.value)
            is SignUpAction.ConfirmPasswordChanged -> onConfirmChanged(action.value)
            SignUpAction.TogglePasswordVisibility ->
                _state.update { it.copy(passwordVisible = !it.passwordVisible) }
            SignUpAction.ToggleConfirmPasswordVisibility ->
                _state.update { it.copy(confirmPasswordVisible = !it.confirmPasswordVisible) }
            SignUpAction.PickAvatarTapped ->
                _state.update { it.copy(showAvatarSourceSheet = true) }
            SignUpAction.DismissAvatarSheet ->
                _state.update { it.copy(showAvatarSourceSheet = false) }
            SignUpAction.PickFromGallery -> {
                _state.update { it.copy(showAvatarSourceSheet = false) }
                emit(SignUpEvent.LaunchGalleryPicker)
            }
            SignUpAction.PickFromCamera -> {
                _state.update { it.copy(showAvatarSourceSheet = false) }
                emit(SignUpEvent.LaunchCamera)
            }
            is SignUpAction.AvatarSelected -> {
                savedStateHandle[KEY_AVATAR_URI] = action.uri
                _state.update { it.copy(avatarUri = action.uri, showAvatarSourceSheet = false) }
            }
            SignUpAction.AvatarCleared -> {
                savedStateHandle[KEY_AVATAR_URI] = null
                _state.update { it.copy(avatarUri = null) }
            }
            SignUpAction.Submit -> submit()
            SignUpAction.BackTapped -> emit(SignUpEvent.NavigateBack)
            SignUpAction.DismissError -> _state.update { it.copy(transientError = null) }
            SignUpAction.ToggleTerms ->
                _state.update { it.copy(termsAccepted = !it.termsAccepted) }
            SignUpAction.TermsLinkTapped,
            SignUpAction.PrivacyLinkTapped -> emit(SignUpEvent.ShowComingSoon(R.string.signup_coming_soon))
        }
    }

    private fun submit() {
        val current = _state.value
        val firstErr = validateName(current.firstName, NameField.First)
        val lastErr = validateName(current.lastName, NameField.Last)
        val emailErr = validateEmail(current.email)
        val passwordErr = validatePassword(current.password)
        val confirmErr = validateConfirmPassword(current.password, current.confirmPassword)

        if (firstErr != null || lastErr != null || emailErr != null ||
            passwordErr != null || confirmErr != null
        ) {
            _state.update {
                it.copy(
                    firstNameError = firstErr,
                    lastNameError = lastErr,
                    emailError = emailErr,
                    passwordError = passwordErr,
                    confirmPasswordError = confirmErr,
                )
            }
            return
        }

        _state.update { it.copy(isSubmitting = true, transientError = null) }
        viewModelScope.launch {
            val result = registerUseCase(
                SignUpRequest(
                    firstName = current.firstName.trim(),
                    lastName = current.lastName.trim(),
                    email = current.email.trim(),
                    password = current.password,
                    avatarUri = current.avatarUri,
                ),
            )
            handleResult(result)
        }
    }

    private fun handleResult(result: Try<Session, AuthError>) {
        when (result) {
            is Try.Success -> {
                _state.update { it.copy(isSubmitting = false) }
                emit(SignUpEvent.NavigateHome)
            }
            is Try.Failure -> when (result.error) {
                AuthError.AvatarUploadFailed -> {
                    _state.update { it.copy(isSubmitting = false) }
                    emit(SignUpEvent.ShowSoftWarning(SoftWarning.AvatarUploadFailed))
                    emit(SignUpEvent.NavigateHome)
                }
                else -> _state.update {
                    it.copy(isSubmitting = false, transientError = result.error)
                }
            }
        }
    }

    private fun onFirstNameChanged(value: String) {
        savedStateHandle[KEY_FIRST_NAME] = value
        _state.update {
            it.copy(
                firstName = value,
                firstNameError = if (value.isEmpty()) null else validateName(value, NameField.First),
            )
        }
    }

    private fun onLastNameChanged(value: String) {
        savedStateHandle[KEY_LAST_NAME] = value
        _state.update {
            it.copy(
                lastName = value,
                lastNameError = if (value.isEmpty()) null else validateName(value, NameField.Last),
            )
        }
    }

    private fun onEmailChanged(value: String) {
        savedStateHandle[KEY_EMAIL] = value
        _state.update {
            it.copy(
                email = value,
                emailError = if (value.isEmpty()) null else validateEmail(value),
                transientError = null,
            )
        }
    }

    private fun onPasswordChanged(value: String) {
        _state.update {
            val passwordErr = if (value.isEmpty()) null else validatePassword(value)
            // Re-run confirm check against the new password so the mismatch error
            // clears (or appears) the moment the password edit makes them agree.
            val confirmErr = if (it.confirmPassword.isEmpty()) {
                it.confirmPasswordError
            } else {
                validateConfirmPassword(value, it.confirmPassword)
            }
            it.copy(
                password = value,
                passwordError = passwordErr,
                confirmPasswordError = confirmErr,
                transientError = null,
            )
        }
    }

    private fun onConfirmChanged(value: String) {
        _state.update {
            it.copy(
                confirmPassword = value,
                confirmPasswordError = if (value.isEmpty()) {
                    null
                } else {
                    validateConfirmPassword(it.password, value)
                },
            )
        }
    }

    private fun emit(event: SignUpEvent) {
        _events.tryEmit(event)
    }

    private companion object {
        const val KEY_FIRST_NAME = "signup.first_name"
        const val KEY_LAST_NAME = "signup.last_name"
        const val KEY_EMAIL = "signup.email"
        const val KEY_AVATAR_URI = "signup.avatar_uri"
    }
}

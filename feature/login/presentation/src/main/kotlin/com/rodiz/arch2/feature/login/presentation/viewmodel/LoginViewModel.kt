package com.rodiz.arch2.feature.login.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rodiz.arch2.core.common.result.Try
import com.rodiz.arch2.feature.login.domain.model.AuthError
import com.rodiz.arch2.feature.login.domain.model.Credentials
import com.rodiz.arch2.feature.login.domain.repository.AuthRepository
import com.rodiz.arch2.feature.login.domain.usecase.LoginUseCase
import com.rodiz.arch2.feature.login.domain.usecase.LoginWithBiometricUseCase
import com.rodiz.arch2.feature.login.domain.usecase.SignInWithGoogleUseCase
import com.rodiz.arch2.feature.login.domain.usecase.ValidateEmailUseCase
import com.rodiz.arch2.feature.login.domain.usecase.ValidatePasswordUseCase
import com.rodiz.arch2.feature.login.presentation.BuildConfig
import com.rodiz.arch2.feature.login.presentation.DebugConstants
import com.rodiz.arch2.feature.login.presentation.state.LoginAction
import com.rodiz.arch2.feature.login.presentation.state.LoginEvent
import com.rodiz.arch2.feature.login.presentation.state.LoginUiState
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
class LoginViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val loginUseCase: LoginUseCase,
    private val loginWithBiometricUseCase: LoginWithBiometricUseCase,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val validateEmail: ValidateEmailUseCase,
    private val validatePassword: ValidatePasswordUseCase,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(
        LoginUiState(
            // TODO: Remove pre-filled debug credentials before production
            email = savedStateHandle.get<String>(KEY_EMAIL) ?: if (BuildConfig.DEBUG) {
                DebugConstants.PREFILLED_EMAIL
            } else {
                ""
            },
            password = if (BuildConfig.DEBUG) DebugConstants.PREFILLED_PASSWORD else "",
        ),
    )
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<LoginEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<LoginEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            _state.update { it.copy(biometricAvailable = authRepository.hasStoredCredentials()) }
        }
    }

    fun onAction(action: LoginAction) {
        when (action) {
            is LoginAction.EmailChanged -> onEmailChanged(action.value)
            is LoginAction.PasswordChanged -> onPasswordChanged(action.value)
            LoginAction.TogglePasswordVisibility -> _state.update { it.copy(passwordVisible = !it.passwordVisible) }
            LoginAction.Submit -> submit()
            LoginAction.BiometricRequested -> tryEmit(LoginEvent.PromptBiometric)
            LoginAction.BiometricSucceeded -> loginWithBiometric()
            LoginAction.GoogleSignInRequested -> tryEmit(LoginEvent.PromptGoogleSignIn)
            LoginAction.ForgotPasswordTapped -> tryEmit(LoginEvent.NavigateForgot)
            LoginAction.CreateAccountTapped -> tryEmit(LoginEvent.NavigateCreate)
            LoginAction.DismissError -> _state.update { it.copy(transientError = null) }
        }
    }

    /** Called by the route after Credential Manager returns a Google ID token. */
    fun onGoogleIdToken(idToken: String) {
        _state.update { it.copy(isSubmitting = true, transientError = null) }
        viewModelScope.launch {
            val result = signInWithGoogleUseCase(idToken)
            handleResult(result)
        }
    }

    /** Called by the route when Credential Manager fails or is cancelled. */
    fun onGoogleSignInFailed(error: AuthError) {
        _state.update { it.copy(isSubmitting = false, transientError = error) }
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
            it.copy(
                password = value,
                passwordError = if (value.isEmpty()) null else validatePassword(value),
                transientError = null,
            )
        }
    }

    private fun submit() {
        val current = _state.value
        val emailError = validateEmail(current.email)
        val passwordError = validatePassword(current.password)
        if (emailError != null || passwordError != null) {
            _state.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }
        _state.update { it.copy(isSubmitting = true, transientError = null) }
        viewModelScope.launch {
            val result = loginUseCase(Credentials(current.email, current.password))
            handleResult(result)
        }
    }

    private fun loginWithBiometric() {
        _state.update { it.copy(isSubmitting = true, transientError = null) }
        viewModelScope.launch {
            val result = loginWithBiometricUseCase()
            handleResult(result)
        }
    }

    private fun handleResult(result: Try<*, AuthError>) {
        when (result) {
            is Try.Success -> {
                _state.update { it.copy(isSubmitting = false) }
                tryEmit(LoginEvent.NavigateHome)
            }
            is Try.Failure -> _state.update {
                it.copy(isSubmitting = false, transientError = result.error)
            }
        }
    }

    private fun tryEmit(event: LoginEvent) {
        _events.tryEmit(event)
    }

    private companion object {
        const val KEY_EMAIL = "login.email"
    }
}

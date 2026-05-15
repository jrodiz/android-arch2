package com.rodiz.arch2.feature.login.presentation.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.rodiz.arch2.feature.login.presentation.R
import com.rodiz.arch2.feature.login.presentation.biometric.BiometricPromptManager
import com.rodiz.arch2.feature.login.presentation.biometric.BiometricResult
import com.rodiz.arch2.feature.login.presentation.state.LoginAction
import com.rodiz.arch2.feature.login.presentation.state.LoginEvent
import com.rodiz.arch2.feature.login.presentation.viewmodel.LoginViewModel
import kotlinx.coroutines.launch

@Composable
fun LoginRoute(
    onNavigateHome: () -> Unit,
    onForgot: () -> Unit,
    onCreate: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val biometricTitle = stringResource(R.string.login_biometric_title)
    val biometricSubtitle = stringResource(R.string.login_biometric_subtitle)
    val biometricCancel = stringResource(R.string.login_biometric_cancel)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                LoginEvent.NavigateHome -> onNavigateHome()
                LoginEvent.NavigateForgot -> onForgot()
                LoginEvent.NavigateCreate -> onCreate()
                LoginEvent.PromptBiometric -> {
                    val activity = context as? FragmentActivity ?: return@collect
                    coroutineScope.launch {
                        val result = BiometricPromptManager.authenticate(
                            activity = activity,
                            title = biometricTitle,
                            subtitle = biometricSubtitle,
                            negativeButton = biometricCancel,
                        )
                        if (result is BiometricResult.Success) {
                            viewModel.onAction(LoginAction.BiometricSucceeded)
                        }
                    }
                }
            }
        }
    }

    LoginScreen(state = state, onAction = viewModel::onAction)
}

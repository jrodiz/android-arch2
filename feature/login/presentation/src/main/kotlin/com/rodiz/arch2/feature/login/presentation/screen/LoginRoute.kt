package com.rodiz.arch2.feature.login.presentation.screen

import android.app.Activity
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rodiz.arch2.core.common.result.Try
import com.rodiz.arch2.feature.login.domain.model.AuthError
import com.rodiz.arch2.feature.login.presentation.BuildConfig
import com.rodiz.arch2.feature.login.presentation.R
import com.rodiz.arch2.feature.login.presentation.biometric.BiometricPromptManager
import com.rodiz.arch2.feature.login.presentation.biometric.BiometricResult
import com.rodiz.arch2.feature.login.presentation.googlesignin.GoogleSignInLauncher
import com.rodiz.arch2.feature.login.presentation.state.LoginAction
import com.rodiz.arch2.feature.login.presentation.state.LoginEvent
import com.rodiz.arch2.feature.login.presentation.viewmodel.LoginViewModel
import kotlinx.coroutines.launch

@Composable
fun LoginRoute(
    onNavigateHome: () -> Unit,
    onForgot: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val biometricTitle = stringResource(R.string.login_biometric_title)
    val biometricSubtitle = stringResource(R.string.login_biometric_subtitle)
    val biometricCancel = stringResource(R.string.login_biometric_cancel)
    val registerComingSoon = stringResource(R.string.register_coming_soon)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                LoginEvent.NavigateHome -> onNavigateHome()
                LoginEvent.NavigateForgot -> onForgot()
                LoginEvent.ShowRegisterComingSoon ->
                    Toast.makeText(context, registerComingSoon, Toast.LENGTH_SHORT).show()
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
                LoginEvent.PromptGoogleSignIn -> {
                    val activity = context as? Activity ?: return@collect
                    val webClientId = BuildConfig.GCP_WEB_CLIENT_ID
                    if (webClientId.isBlank()) {
                        // Fail fast — Firebase Console setup hasn't been completed.
                        // Set firebase.web.client.id in gradle.properties to enable.
                        viewModel.onGoogleSignInFailed(AuthError.GoogleSignInFailed)
                        return@collect
                    }
                    coroutineScope.launch {
                        when (val result = GoogleSignInLauncher.launch(activity, webClientId)) {
                            is Try.Success -> viewModel.onGoogleIdToken(result.value)
                            is Try.Failure -> viewModel.onGoogleSignInFailed(result.error)
                        }
                    }
                }
            }
        }
    }

    LoginScreen(state = state, onAction = viewModel::onAction)
}

package com.rodiz.arch2.feature.login.presentation.screen

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsControllerCompat
import com.rodiz.arch2.core.designsystem.theme.TinPetTheme
import com.rodiz.arch2.core.ui.components.BrandHeader
import com.rodiz.arch2.core.ui.components.EmailField
import com.rodiz.arch2.core.ui.components.ErrorBanner
import com.rodiz.arch2.core.ui.components.PasswordField
import com.rodiz.arch2.core.ui.components.PrimaryButton
import com.rodiz.arch2.feature.login.domain.model.AuthError
import com.rodiz.arch2.feature.login.domain.model.ValidationError
import com.rodiz.arch2.feature.login.presentation.R
import com.rodiz.arch2.feature.login.presentation.state.LoginAction
import com.rodiz.arch2.feature.login.presentation.state.LoginUiState

@Composable
fun LoginScreen(
    state: LoginUiState,
    onAction: (LoginAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    LightStatusBarIconsWhileShown()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        BrandHeader(
            patternRes = R.drawable.ic_login_topographic,
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .padding(bottom = 16.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .padding(top = 16.dp, bottom = 32.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            verticalArrangement = Arrangement.Top,
        ) {
            ModeTabs(onSignUpTapped = { onAction(LoginAction.SignUpTabTapped) })

            Spacer(Modifier.height(28.dp))

            state.transientError?.let { error ->
                ErrorBanner(
                    message = error.localized(),
                    onDismiss = { onAction(LoginAction.DismissError) },
                    dismissContentDescription = stringResource(R.string.login_error_dismiss),
                )
                Spacer(Modifier.height(16.dp))
            }

            AnimatedVisibility(
                visible = !state.emailFormExpanded,
                enter = expandVertically(animationSpec = tween(durationMillis = 250)) +
                    fadeIn(animationSpec = tween(durationMillis = 200)),
                exit = shrinkVertically(animationSpec = tween(durationMillis = 200)) +
                    fadeOut(animationSpec = tween(durationMillis = 150)),
            ) {
                PrimaryButton(
                    text = stringResource(R.string.login_email_form_show),
                    loading = false,
                    enabled = !state.isSubmitting,
                    onClick = { onAction(LoginAction.ShowEmailForm) },
                    testTag = "login_email_form_show",
                )
            }

            AnimatedVisibility(
                visible = state.emailFormExpanded,
                enter = expandVertically(animationSpec = tween(durationMillis = 300)) +
                    fadeIn(animationSpec = tween(durationMillis = 300)),
                exit = shrinkVertically(animationSpec = tween(durationMillis = 250)) +
                    fadeOut(animationSpec = tween(durationMillis = 200)),
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    EmailField(
                        value = state.email,
                        onValueChange = { onAction(LoginAction.EmailChanged(it)) },
                        label = stringResource(R.string.login_email_label),
                        placeholder = stringResource(R.string.login_email_placeholder),
                        errorMessage = state.emailError?.localized(),
                    )

                    Spacer(Modifier.height(20.dp))

                    PasswordField(
                        value = state.password,
                        onValueChange = { onAction(LoginAction.PasswordChanged(it)) },
                        label = stringResource(R.string.login_password_label),
                        placeholder = stringResource(R.string.login_password_placeholder),
                        visible = state.passwordVisible,
                        onToggleVisibility = { onAction(LoginAction.TogglePasswordVisibility) },
                        onImeDone = { onAction(LoginAction.Submit) },
                        errorMessage = state.passwordError?.localized(),
                        toggleContentDescription = stringResource(
                            if (state.passwordVisible) R.string.login_password_hide else R.string.login_password_show,
                        ),
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = { onAction(LoginAction.ForgotPasswordTapped) }) {
                            Text(
                                text = stringResource(R.string.login_forgot),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            )
                        }
                    }

                    Spacer(Modifier.height(28.dp))

                    PrimaryButton(
                        text = stringResource(R.string.login_submit),
                        loading = state.isSubmitting,
                        enabled = state.canSubmit,
                        onClick = { onAction(LoginAction.Submit) },
                        testTag = "login_submit",
                    )

                    if (state.biometricAvailable) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { onAction(LoginAction.BiometricRequested) },
                            enabled = !state.isSubmitting,
                            shape = MaterialTheme.shapes.large,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("login_biometric"),
                        ) {
                            Text(stringResource(R.string.login_biometric))
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            OrDivider(text = stringResource(R.string.login_or_divider))
            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = { onAction(LoginAction.GoogleSignInRequested) },
                enabled = !state.isSubmitting,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("login_google"),
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_google),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.login_continue_google),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}

@Composable
private fun ModeTabs(onSignUpTapped: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(28.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Sign In is always the active tab on this screen — the active styling lives here.
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .padding(vertical = 4.dp)
                .testTag("login_tab_sign_in"),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.login_tab_sign_in),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .size(width = 96.dp, height = 3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }

        // Sign Up is a launcher — taps navigate to the full Sign Up screen.
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onSignUpTapped)
                .padding(vertical = 4.dp)
                .testTag("login_tab_sign_up"),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.login_tab_sign_up),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .size(width = 96.dp, height = 3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.Transparent),
            )
        }
    }
}

@Composable
private fun OrDivider(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
        )
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
        )
    }
}

/**
 * Forces light status-bar icons (white on coral) while this screen is on top, and
 * restores the default on dispose so light-background screens (Home) get dark icons.
 */
@Composable
private fun LightStatusBarIconsWhileShown() {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(Unit) {
        val window = (view.context as Activity).window
        val controller = WindowInsetsControllerCompat(window, view)
        val previous = controller.isAppearanceLightStatusBars
        controller.isAppearanceLightStatusBars = false
        onDispose { controller.isAppearanceLightStatusBars = previous }
    }
}

@Preview(name = "Login — collapsed", showBackground = true, heightDp = 900)
@Composable
private fun LoginScreenPreviewCollapsed() {
    TinPetTheme {
        LoginScreen(state = LoginUiState(), onAction = {})
    }
}

@Preview(name = "Login — expanded", showBackground = true, heightDp = 900)
@Composable
private fun LoginScreenPreviewExpanded() {
    TinPetTheme {
        LoginScreen(
            state = LoginUiState(
                email = "demo@email.com",
                password = "password1",
                emailFormExpanded = true,
                biometricAvailable = true,
            ),
            onAction = {},
        )
    }
}

@Preview(name = "Login — error", showBackground = true, heightDp = 900)
@Composable
private fun LoginScreenPreviewError() {
    TinPetTheme {
        LoginScreen(
            state = LoginUiState(
                email = "demo@email.com",
                password = "shrt",
                passwordError = ValidationError.PasswordTooShort(8),
                transientError = AuthError.NoNetwork,
                emailFormExpanded = true,
            ),
            onAction = {},
        )
    }
}

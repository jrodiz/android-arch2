package com.rodiz.arch2.feature.login.presentation.screen

import android.app.Activity
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsControllerCompat
import com.rodiz.arch2.core.designsystem.theme.BrandColors
import com.rodiz.arch2.core.designsystem.theme.TinPetTheme
import com.rodiz.arch2.core.ui.components.BrandHeader
import com.rodiz.arch2.core.ui.components.BrandTextField
import com.rodiz.arch2.core.ui.components.EmailField
import com.rodiz.arch2.core.ui.components.ErrorBanner
import com.rodiz.arch2.core.ui.components.PasswordField
import com.rodiz.arch2.core.ui.components.PrimaryButton
import com.rodiz.arch2.feature.login.presentation.R
import com.rodiz.arch2.feature.login.presentation.state.SignUpAction
import com.rodiz.arch2.feature.login.presentation.state.SignUpUiState

@Composable
fun SignUpScreen(
    state: SignUpUiState,
    onAction: (SignUpAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    LightStatusBarIconsWhileShown()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        SignUpHero(onBack = { onAction(SignUpAction.BackTapped) })

        Spacer(Modifier.height(20.dp))

        AvatarPicker(
            avatarUri = state.avatarUri,
            onPickTapped = { onAction(SignUpAction.PickAvatarTapped) },
            onCleared = { onAction(SignUpAction.AvatarCleared) },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Spacer(Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .padding(bottom = 32.dp)
                .windowInsetsPadding(WindowInsets.navigationBars),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            state.transientError?.let { error ->
                ErrorBanner(
                    message = error.localized(),
                    onDismiss = { onAction(SignUpAction.DismissError) },
                    dismissContentDescription = stringResource(R.string.login_error_dismiss),
                )
            }

            BrandTextField(
                value = state.firstName,
                onValueChange = { onAction(SignUpAction.FirstNameChanged(it)) },
                label = stringResource(R.string.signup_first_name),
                placeholder = stringResource(R.string.signup_first_name_placeholder),
                leadingIcon = Icons.Outlined.Person,
                errorMessage = state.firstNameError?.localized(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
                fieldModifier = Modifier.testTag("signup_first_name_field"),
            )

            BrandTextField(
                value = state.lastName,
                onValueChange = { onAction(SignUpAction.LastNameChanged(it)) },
                label = stringResource(R.string.signup_last_name),
                placeholder = stringResource(R.string.signup_last_name_placeholder),
                leadingIcon = Icons.Outlined.Person,
                errorMessage = state.lastNameError?.localized(),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
                fieldModifier = Modifier.testTag("signup_last_name_field"),
            )

            EmailField(
                value = state.email,
                onValueChange = { onAction(SignUpAction.EmailChanged(it)) },
                label = stringResource(R.string.login_email_label),
                placeholder = stringResource(R.string.login_email_placeholder),
                errorMessage = state.emailError?.localized(),
            )

            PasswordField(
                value = state.password,
                onValueChange = { onAction(SignUpAction.PasswordChanged(it)) },
                label = stringResource(R.string.login_password_label),
                placeholder = stringResource(R.string.login_password_placeholder),
                visible = state.passwordVisible,
                onToggleVisibility = { onAction(SignUpAction.TogglePasswordVisibility) },
                onImeDone = {},
                errorMessage = state.passwordError?.localized(),
                toggleContentDescription = stringResource(
                    if (state.passwordVisible) R.string.login_password_hide else R.string.login_password_show,
                ),
                imeAction = ImeAction.Next,
                testTag = "signup_password_field",
            )

            PasswordField(
                value = state.confirmPassword,
                onValueChange = { onAction(SignUpAction.ConfirmPasswordChanged(it)) },
                label = stringResource(R.string.signup_confirm_password),
                placeholder = stringResource(R.string.login_password_placeholder),
                visible = state.confirmPasswordVisible,
                onToggleVisibility = { onAction(SignUpAction.ToggleConfirmPasswordVisibility) },
                onImeDone = { onAction(SignUpAction.Submit) },
                errorMessage = state.confirmPasswordError?.localized(),
                toggleContentDescription = stringResource(
                    if (state.confirmPasswordVisible) R.string.login_password_hide else R.string.login_password_show,
                ),
                imeAction = ImeAction.Done,
                testTag = "signup_confirm_password_field",
            )

            Spacer(Modifier.height(4.dp))

            PrimaryButton(
                text = stringResource(R.string.signup_submit),
                loading = state.isSubmitting,
                enabled = state.canSubmit,
                onClick = { onAction(SignUpAction.Submit) },
                testTag = "signup_submit",
            )
        }
    }

    if (state.showAvatarSourceSheet) {
        AvatarSourceSheet(
            onDismiss = { onAction(SignUpAction.DismissAvatarSheet) },
            onGallery = { onAction(SignUpAction.PickFromGallery) },
            onCamera = { onAction(SignUpAction.PickFromCamera) },
        )
    }
}

@Composable
private fun SignUpHero(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
    ) {
        BrandHeader(
            patternRes = R.drawable.ic_login_topographic,
            modifier = Modifier.fillMaxSize(),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("signup_back"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.common_back),
                    tint = BrandColors.CoralOnPattern,
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.signup_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = BrandColors.CoralOnPattern,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.weight(1f))
            // Balance the back icon visually so the title is truly centered.
            Spacer(Modifier.width(48.dp))
        }
    }
}

/**
 * Forces light status-bar icons (white on coral) while this screen is on top, and
 * restores the default on dispose. Mirrors LoginScreen's helper.
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

@Preview(name = "Sign up — empty", showBackground = true, heightDp = 900)
@Composable
private fun SignUpScreenPreviewEmpty() {
    TinPetTheme {
        SignUpScreen(state = SignUpUiState(), onAction = {})
    }
}

@Preview(name = "Sign up — filled", showBackground = true, heightDp = 900)
@Composable
private fun SignUpScreenPreviewFilled() {
    TinPetTheme {
        SignUpScreen(
            state = SignUpUiState(
                firstName = "Steve",
                lastName = "Rogers",
                email = "steve@example.com",
                password = "password1",
                confirmPassword = "password1",
            ),
            onAction = {},
        )
    }
}

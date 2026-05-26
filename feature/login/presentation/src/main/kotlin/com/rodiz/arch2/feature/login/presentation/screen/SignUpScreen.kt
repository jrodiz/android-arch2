package com.rodiz.arch2.feature.login.presentation.screen

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat
import com.rodiz.arch2.core.designsystem.theme.BrandColors
import com.rodiz.arch2.core.designsystem.theme.TinPetTheme
import com.rodiz.arch2.core.designsystem.theme.WaveBottomShape
import com.rodiz.arch2.core.ui.components.EmailFieldPill
import com.rodiz.arch2.core.ui.components.ErrorBanner
import com.rodiz.arch2.core.ui.components.FilledPillTextField
import com.rodiz.arch2.core.ui.components.PasswordFieldPill
import com.rodiz.arch2.core.ui.components.PrimaryButton
import com.rodiz.arch2.feature.login.presentation.R
import com.rodiz.arch2.feature.login.presentation.state.SignUpAction
import com.rodiz.arch2.feature.login.presentation.state.SignUpUiState

private val HeroHeight = 280.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    state: SignUpUiState,
    onAction: (SignUpAction) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    LightStatusBarIconsWhileShown()
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        // Opt the Scaffold out of top status-bar inset so the coral hero can paint
        // edge-to-edge under the status bar (the hero itself reapplies
        // statusBarsPadding() so the back arrow + headline clear the system clock).
        // Without this, the Column starts below the status bar and the system fills
        // that strip with the default white window background.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .imePadding(),
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
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp)
                    .windowInsetsPadding(WindowInsets.navigationBars),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                state.transientError?.let { error ->
                    ErrorBanner(
                        message = error.localized(),
                        onDismiss = { onAction(SignUpAction.DismissError) },
                        dismissContentDescription = stringResource(R.string.login_error_dismiss),
                    )
                }

                LabeledField(label = stringResource(R.string.signup_first_name)) {
                    FilledPillTextField(
                        value = state.firstName,
                        onValueChange = { onAction(SignUpAction.FirstNameChanged(it)) },
                        placeholder = stringResource(R.string.signup_first_name_placeholder),
                        leadingIcon = Icons.Outlined.Person,
                        errorMessage = state.firstNameError?.localized(),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next,
                        ),
                        containerColor = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp,
                        fieldModifier = Modifier.testTag("signup_first_name_field"),
                    )
                }

                LabeledField(label = stringResource(R.string.signup_last_name)) {
                    FilledPillTextField(
                        value = state.lastName,
                        onValueChange = { onAction(SignUpAction.LastNameChanged(it)) },
                        placeholder = stringResource(R.string.signup_last_name_placeholder),
                        leadingIcon = Icons.Outlined.Person,
                        errorMessage = state.lastNameError?.localized(),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next,
                        ),
                        containerColor = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp,
                        fieldModifier = Modifier.testTag("signup_last_name_field"),
                    )
                }

                LabeledField(label = stringResource(R.string.login_email_label)) {
                    EmailFieldPill(
                        value = state.email,
                        onValueChange = { onAction(SignUpAction.EmailChanged(it)) },
                        placeholder = stringResource(R.string.login_email_placeholder),
                        errorMessage = state.emailError?.localized(),
                        containerColor = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp,
                    )
                }

                LabeledField(label = stringResource(R.string.login_password_label)) {
                    PasswordFieldPill(
                        value = state.password,
                        onValueChange = { onAction(SignUpAction.PasswordChanged(it)) },
                        placeholder = stringResource(R.string.login_password_placeholder),
                        visible = state.passwordVisible,
                        onToggleVisibility = { onAction(SignUpAction.TogglePasswordVisibility) },
                        onImeDone = {},
                        errorMessage = state.passwordError?.localized(),
                        toggleContentDescription = stringResource(
                            if (state.passwordVisible) R.string.login_password_hide else R.string.login_password_show,
                        ),
                        imeAction = ImeAction.Next,
                        containerColor = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp,
                        testTag = "signup_password_field",
                    )
                }

                LabeledField(label = stringResource(R.string.signup_confirm_password)) {
                    PasswordFieldPill(
                        value = state.confirmPassword,
                        onValueChange = { onAction(SignUpAction.ConfirmPasswordChanged(it)) },
                        placeholder = stringResource(R.string.login_password_placeholder),
                        visible = state.confirmPasswordVisible,
                        onToggleVisibility = { onAction(SignUpAction.ToggleConfirmPasswordVisibility) },
                        onImeDone = { onAction(SignUpAction.Submit) },
                        errorMessage = state.confirmPasswordError?.localized(),
                        toggleContentDescription = stringResource(
                            if (state.confirmPasswordVisible) R.string.login_password_hide else R.string.login_password_show,
                        ),
                        imeAction = ImeAction.Done,
                        containerColor = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp,
                        testTag = "signup_confirm_password_field",
                    )
                }

                Spacer(Modifier.height(4.dp))

                TermsRow(
                    checked = state.termsAccepted,
                    onToggle = { onAction(SignUpAction.ToggleTerms) },
                    onTermsTapped = { onAction(SignUpAction.TermsLinkTapped) },
                    onPrivacyTapped = { onAction(SignUpAction.PrivacyLinkTapped) },
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
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(HeroHeight)
            .background(
                brush = Brush.verticalGradient(
                    listOf(BrandColors.Coral, BrandColors.CoralLight),
                ),
                shape = WaveBottomShape,
            )
            .clip(WaveBottomShape)
            .drawBehind {
                // Decorative ellipses — scaled to hero size so they read on every
                // form factor. White at 18% alpha sits behind the headline without
                // competing for attention.
                val ringColor = Color.White.copy(alpha = 0.18f)
                drawOval(
                    color = ringColor,
                    topLeft = Offset(size.width * 0.10f, size.height * 0.20f),
                    size = Size(size.width * 0.30f, size.height * 0.22f),
                )
                drawOval(
                    color = ringColor,
                    topLeft = Offset(size.width * 0.65f, size.height * 0.18f),
                    size = Size(size.width * 0.28f, size.height * 0.18f),
                )
                drawOval(
                    color = ringColor,
                    topLeft = Offset(size.width * 0.40f, size.height * 0.55f),
                    size = Size(size.width * 0.36f, size.height * 0.22f),
                )
            }
            .statusBarsPadding()
            .padding(horizontal = 8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.Top,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                Text(
                    text = stringResource(R.string.signup_title),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = BrandColors.CoralOnPattern,
                    modifier = Modifier.semantics { heading() },
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.signup_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = BrandColors.CoralOnPattern,
                modifier = Modifier.padding(start = 16.dp, end = 32.dp),
            )
        }
    }
}

@Composable
private fun LabeledField(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

@Composable
private fun TermsRow(
    checked: Boolean,
    onToggle: () -> Unit,
    onTermsTapped: () -> Unit,
    onPrivacyTapped: () -> Unit,
) {
    val linkStyles = TextLinkStyles(
        style = SpanStyle(fontWeight = FontWeight.Bold, color = BrandColors.CoralDeep),
    )
    val labelText = buildAnnotatedString {
        append(stringResource(R.string.signup_terms_prefix))
        withLink(LinkAnnotation.Clickable(tag = "terms", styles = linkStyles) { onTermsTapped() }) {
            append(stringResource(R.string.signup_terms_link))
        }
        append(stringResource(R.string.signup_terms_conjunction))
        withLink(LinkAnnotation.Clickable(tag = "privacy", styles = linkStyles) { onPrivacyTapped() }) {
            append(stringResource(R.string.signup_privacy_link))
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = { onToggle() },
            )
            .testTag("signup_terms_row"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null,
            colors = CheckboxDefaults.colors(checkedColor = BrandColors.CoralDeep),
            modifier = Modifier.testTag("signup_terms_checkbox"),
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = labelText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
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

@Preview(name = "Sign up — empty", showBackground = true, heightDp = 1000)
@Composable
private fun SignUpScreenPreviewEmpty() {
    TinPetTheme {
        SignUpScreen(
            state = SignUpUiState(),
            onAction = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

@Preview(name = "Sign up — filled + terms", showBackground = true, heightDp = 1000)
@Composable
private fun SignUpScreenPreviewFilled() {
    TinPetTheme {
        SignUpScreen(
            state = SignUpUiState(
                firstName = "Joseph",
                lastName = "Godiz",
                email = "joseph@tinpet.com",
                password = "password1",
                confirmPassword = "password1",
                termsAccepted = true,
            ),
            onAction = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

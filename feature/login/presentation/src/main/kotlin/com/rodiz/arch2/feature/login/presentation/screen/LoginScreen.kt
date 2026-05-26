package com.rodiz.arch2.feature.login.presentation.screen

import android.app.Activity
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import com.rodiz.arch2.core.designsystem.theme.BrandColors
import com.rodiz.arch2.core.designsystem.theme.TinPetTheme
import com.rodiz.arch2.core.featuredpets.domain.FeaturedPet
import com.rodiz.arch2.core.ui.components.EmailFieldPill
import com.rodiz.arch2.core.ui.components.ErrorBanner
import com.rodiz.arch2.core.ui.components.PasswordFieldPill
import com.rodiz.arch2.core.ui.components.PrimaryButton
import com.rodiz.arch2.feature.login.domain.model.AuthError
import com.rodiz.arch2.feature.login.domain.model.ValidationError
import com.rodiz.arch2.feature.login.presentation.R
import com.rodiz.arch2.feature.login.presentation.state.LoginAction
import com.rodiz.arch2.feature.login.presentation.state.LoginUiState

private val HeroHeight: Dp = 360.dp
private val CardOverlap: Dp = 50.dp
private val TileSize: Dp = 110.dp

@Composable
fun LoginScreen(
    state: LoginUiState,
    onAction: (LoginAction) -> Unit,
    modifier: Modifier = Modifier,
    featured: List<FeaturedPet> = emptyList(),
) {
    LightStatusBarIconsWhileShown()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        HeroAndCard(state = state, onAction = onAction, featured = featured)
    }
}

@Composable
private fun HeroAndCard(
    state: LoginUiState,
    onAction: (LoginAction) -> Unit,
    featured: List<FeaturedPet>,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Hero(
            featured = featured,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(HeroHeight),
        )
        // Card overlaps the hero by CardOverlap. Top padding pushes it down so the
        // hero is still visible above it.
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = HeroHeight - CardOverlap),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        ) {
            CardContent(state = state, onAction = onAction)
        }
        // Rabbit slot sits on the boundary; last child = on top of both hero and card.
        // About 60% of the tile stays above the hero/card boundary, 40% dips into the
        // card — matches the mockup. CardContent's top padding clears the dipped part.
        // Slot index 2 of the featured list lands here.
        HeroPetSlot(
            pet = featured.getOrNull(2),
            fallbackPawRes = R.drawable.pet_tile_rabbit,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = HeroHeight - TileSize + 20.dp)
                .size(TileSize)
                .rotate(-4f)
                .clip(RoundedCornerShape(20.dp))
                .border(4.dp, Color.White, RoundedCornerShape(20.dp))
                .clearAndSetSemantics {},
        )
    }
}

/**
 * Single decorative-or-photo tile in the Login hero. When [pet] has a non-null
 * avatarUrl, renders the photo via Coil; otherwise (or on load failure) shows
 * [fallbackPawRes] so the hero composition is preserved even pre-cache or
 * after a Coil eviction. `modifier` carries the slot's offset / size / rotate /
 * clip / border so both the photo and the fallback paint identically.
 */
@Composable
private fun HeroPetSlot(
    pet: FeaturedPet?,
    @DrawableRes fallbackPawRes: Int,
    modifier: Modifier,
) {
    if (pet?.avatarUrl != null) {
        AsyncImage(
            model = pet.avatarUrl,
            contentDescription = null, // hero merges semantics
            contentScale = ContentScale.Crop,
            modifier = modifier,
            placeholder = painterResource(fallbackPawRes),
            error = painterResource(fallbackPawRes),
            fallback = painterResource(fallbackPawRes),
        )
    } else {
        Image(
            painter = painterResource(fallbackPawRes),
            contentDescription = null,
            modifier = modifier,
        )
    }
}

@Composable
private fun Hero(
    featured: List<FeaturedPet>,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .background(BrandColors.Coral)
            .semantics(mergeDescendants = true) { }
            .drawBehind {
                // Concentric radar rings, faint white. Center pinned at (35% w, 45% h)
                // so the rings sit behind the puppies tile (top-left) and reach toward
                // the cat tile (right).
                val cx = size.width * 0.35f
                val cy = size.height * 0.45f
                val ringColor = Color.White.copy(alpha = 0.20f)
                val strokeWidthPx = 2.dp.toPx()
                val stroke = Stroke(width = strokeWidthPx)
                listOf(0.25f, 0.40f, 0.55f, 0.70f).forEach { factor ->
                    drawCircle(
                        color = ringColor,
                        radius = size.width * factor,
                        center = Offset(cx, cy),
                        style = stroke,
                    )
                }
            }
            .statusBarsPadding(),
    ) {
        val w = maxWidth
        val h = maxHeight
        val density = LocalDensity.current
        val tilePx: Float = with(density) { TileSize.toPx() }
        val widthPx: Float = with(density) { w.toPx() }
        val heightPx: Float = with(density) { h.toPx() }

        // Slot 0 — top-left, slight CCW tilt. Falls back to the puppies PNG.
        HeroPetSlot(
            pet = featured.getOrNull(0),
            fallbackPawRes = R.drawable.pet_tile_puppies,
            modifier = Modifier
                .offset(
                    x = with(density) { (widthPx * 0.05f).toDp() },
                    y = with(density) { (heightPx * 0.18f).toDp() },
                )
                .size(TileSize)
                .rotate(-6f)
                .clip(RoundedCornerShape(20.dp))
                .border(4.dp, Color.White, RoundedCornerShape(20.dp))
                .clearAndSetSemantics {},
        )

        // Slot 1 — right side, slight CW tilt. Falls back to the cat PNG.
        HeroPetSlot(
            pet = featured.getOrNull(1),
            fallbackPawRes = R.drawable.pet_tile_cat,
            modifier = Modifier
                .offset(
                    x = with(density) { (widthPx - tilePx - widthPx * 0.06f).toDp() },
                    y = with(density) { (heightPx * 0.38f).toDp() },
                )
                .size(TileSize)
                .rotate(8f)
                .clip(RoundedCornerShape(20.dp))
                .border(4.dp, Color.White, RoundedCornerShape(20.dp))
                .clearAndSetSemantics {},
        )
    }
}

@Composable
private fun CardContent(state: LoginUiState, onAction: (LoginAction) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            // Clears the rabbit tile that dips into the top of the card (~70dp dip with
            // CardOverlap=50, TileSize=110, top offset HeroHeight-TileSize+20: bottom of
            // rabbit lands at CardOverlap+10 = 60dp below the card top edge).
            .padding(top = 90.dp, bottom = 32.dp)
            .imePadding()
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        Text(
            text = stringResource(R.string.login_welcome_title),
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.login_welcome_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))

        PillSegmentedTabs(
            signInLabel = stringResource(R.string.login_tab_sign_in),
            signUpLabel = stringResource(R.string.login_tab_sign_up),
            onSignUpTapped = { onAction(LoginAction.SignUpTabTapped) },
        )

        Spacer(Modifier.height(20.dp))

        state.transientError?.let { error ->
            ErrorBanner(
                message = error.localized(),
                onDismiss = { onAction(LoginAction.DismissError) },
                dismissContentDescription = stringResource(R.string.login_error_dismiss),
            )
            Spacer(Modifier.height(16.dp))
        }

        // Collapsed entry: a single "Login with email" CTA. Tapping expands the form.
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

        // Expanded form: email + password + forgot link + Sign in CTA + (optional)
        // biometric. AnimatedVisibility wraps the whole block so the height
        // animation matches the original UX.
        AnimatedVisibility(
            visible = state.emailFormExpanded,
            enter = expandVertically(animationSpec = tween(durationMillis = 300)) +
                fadeIn(animationSpec = tween(durationMillis = 300)),
            exit = shrinkVertically(animationSpec = tween(durationMillis = 250)) +
                fadeOut(animationSpec = tween(durationMillis = 200)),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                EmailFieldPill(
                    value = state.email,
                    onValueChange = { onAction(LoginAction.EmailChanged(it)) },
                    placeholder = stringResource(R.string.login_email_placeholder),
                    errorMessage = state.emailError?.localized(),
                )

                Spacer(Modifier.height(12.dp))

                PasswordFieldPill(
                    value = state.password,
                    onValueChange = { onAction(LoginAction.PasswordChanged(it)) },
                    placeholder = stringResource(R.string.login_password_placeholder),
                    visible = state.passwordVisible,
                    onToggleVisibility = { onAction(LoginAction.TogglePasswordVisibility) },
                    onImeDone = { onAction(LoginAction.Submit) },
                    errorMessage = state.passwordError?.localized(),
                    toggleContentDescription = stringResource(
                        if (state.passwordVisible) R.string.login_password_hide else R.string.login_password_show,
                    ),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = { onAction(LoginAction.ForgotPasswordTapped) },
                        contentPadding = PaddingValues(horizontal = 4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.login_forgot),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

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

        Spacer(Modifier.height(24.dp))
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

@Composable
private fun PillSegmentedTabs(
    signInLabel: String,
    signUpLabel: String,
    onSignUpTapped: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = BrandColors.Coral.copy(alpha = 0.12f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Sign in — active pill. White on peach. 1dp tonalElevation reads as a
            // subtle lift without the muddy drop-shadow that Modifier.shadow gives.
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .selectable(
                        selected = true,
                        onClick = {},
                        role = Role.Tab,
                    )
                    .testTag("login_tab_sign_in"),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = signInLabel,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            // Sign up — inactive pill (transparent), tap launches the SignUp screen.
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .selectable(
                        selected = false,
                        onClick = onSignUpTapped,
                        role = Role.Tab,
                    )
                    .testTag("login_tab_sign_up"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = signUpLabel,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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

@Preview(name = "Login — expanded", showBackground = true, heightDp = 980)
@Composable
private fun LoginScreenPreviewExpanded() {
    TinPetTheme {
        LoginScreen(
            state = LoginUiState(
                email = "rodiz@tinpet.com",
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

/**
 * Mixed-state preview: two pets pinned, third slot still uses the rabbit PNG
 * fallback. Avatars are null so even pre-render the slots fall back to the
 * decorative paws — proves the hero composition stays intact when Coil can't
 * resolve URLs in the preview environment.
 */
@Preview(name = "Login — featured (2 pets)", showBackground = true, heightDp = 900)
@Composable
private fun LoginScreenPreviewFeaturedMixed() {
    TinPetTheme {
        LoginScreen(
            state = LoginUiState(),
            onAction = {},
            featured = listOf(
                FeaturedPet(id = "p1", name = "Rex", species = "DOG", avatarUrl = null),
                FeaturedPet(id = "p2", name = "Mochi", species = "CAT", avatarUrl = null),
            ),
        )
    }
}

package com.rodiz.arch2.feature.match.presentation

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import com.rodiz.arch2.core.designsystem.theme.BrandColors
import com.rodiz.arch2.core.designsystem.theme.TinPetTheme
import com.rodiz.arch2.core.ownerlookup.domain.OwnerDisplay
import com.rodiz.arch2.core.petlookup.domain.PetDisplay

@Composable
internal fun MatchCelebrationScreen(
    state: MatchCelebrationUiState,
    onSayHello: () -> Unit,
    onKeepSwiping: () -> Unit,
) {
    LightStatusBarIconsWhileShown()
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars),
        color = BrandColors.Coral,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(56.dp))
            Text(
                text = stringResource(R.string.match_celebration_headline),
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = subtitleText(state),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(36.dp))

            PetTilesRow(
                myPet = state.myPet,
                theirPet = state.theirPet,
                me = state.me,
                them = state.them,
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = onSayHello,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = BrandColors.CoralDeep,
                ),
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.match_celebration_say_hello),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onKeepSwiping,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
            ) {
                Text(
                    text = stringResource(R.string.match_celebration_keep_swiping),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(vertical = 6.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun subtitleText(state: MatchCelebrationUiState): String {
    val mine = state.myPet?.name ?: stringResource(R.string.match_celebration_fallback_my_pet)
    val theirs = state.theirPet?.name ?: stringResource(R.string.match_celebration_fallback_their_pet)
    return stringResource(R.string.match_celebration_subtitle_format, mine, theirs)
}

@Composable
private fun PetTilesRow(
    myPet: PetDisplay?,
    theirPet: PetDisplay?,
    me: OwnerDisplay?,
    them: OwnerDisplay?,
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PetTile(
                pet = myPet,
                owner = me,
                rotationDeg = -6f,
            )
            PetTile(
                pet = theirPet,
                owner = them,
                rotationDeg = 6f,
            )
        }
        // Heart centerpiece — sits between the two tiles, white circle, coral icon.
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null,
                tint = BrandColors.CoralDeep,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Composable
private fun PetTile(
    pet: PetDisplay?,
    owner: OwnerDisplay?,
    rotationDeg: Float,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(width = 140.dp, height = 180.dp)
                .rotate(rotationDeg)
                .clip(RoundedCornerShape(20.dp))
                .border(4.dp, Color.White, RoundedCornerShape(20.dp))
                .background(BrandColors.CoralLight.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center,
        ) {
            if (pet?.avatarUrl != null) {
                AsyncImage(
                    model = pet.avatarUrl,
                    contentDescription = pet.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Pets,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        OwnerChip(owner = owner)
    }
}

@Composable
private fun OwnerChip(owner: OwnerDisplay?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center,
        ) {
            if (owner?.avatarUrl != null) {
                AsyncImage(
                    model = owner.avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Text(
            text = owner?.firstName.orEmpty(),
            color = Color.White,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

/**
 * Forces light (white) status-bar icons while this screen is visible so they
 * contrast with the coral hero, and restores the previous value on dispose.
 * Mirrors `LoginScreen.LightStatusBarIconsWhileShown` — copied (not extracted)
 * since the helper is 10 lines and lives in a different feature module.
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

// ----- Previews ------------------------------------------------------------

@Preview(name = "Match — populated", showBackground = true, heightDp = 760)
@Composable
private fun PreviewPopulated() {
    TinPetTheme {
        MatchCelebrationScreen(
            state = MatchCelebrationUiState(
                myPet = PetDisplay("p1", "ownerA", "Biscuit", "DOG", null),
                theirPet = PetDisplay("p2", "ownerB", "Mochi", "CAT", null),
                me = OwnerDisplay("ownerA", "Maya", null),
                them = OwnerDisplay("ownerB", "Leah", null),
                match = null, // not checked by previews
            ),
            onSayHello = {},
            onKeepSwiping = {},
        )
    }
}

@Preview(name = "Match — loading", showBackground = true, heightDp = 760)
@Composable
private fun PreviewLoading() {
    TinPetTheme {
        MatchCelebrationScreen(
            state = MatchCelebrationUiState(),
            onSayHello = {},
            onKeepSwiping = {},
        )
    }
}

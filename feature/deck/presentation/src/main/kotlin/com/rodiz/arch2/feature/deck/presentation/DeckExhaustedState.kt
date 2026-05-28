package com.rodiz.arch2.feature.deck.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rodiz.arch2.core.designsystem.theme.BrandColors
import com.rodiz.arch2.core.designsystem.theme.TinPetTheme

@Composable
internal fun DeckExhaustedState(
    maxDistanceKm: Int,
    onWidenDistance: () -> Unit,
    onReviewPasses: () -> Unit,
    onAddAnotherPet: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nextTierKm = (maxDistanceKm * 2).coerceAtMost(MAX_RADIUS_KM)
    val atMaxRadius = maxDistanceKm >= MAX_RADIUS_KM
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        HeroIllustration()
        Spacer(Modifier.height(28.dp))
        Text(
            text = stringResource(R.string.deck_exhausted_headline),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.deck_exhausted_subtitle_format, maxDistanceKm),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        Spacer(Modifier.height(24.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ActionCard(
                icon = Icons.Outlined.Tune,
                iconBubbleColor = BrandColors.CoralTint,
                iconTint = BrandColors.CoralDeep,
                title = stringResource(R.string.deck_exhausted_widen_title),
                body = if (atMaxRadius) {
                    stringResource(R.string.deck_exhausted_widen_body_max)
                } else {
                    stringResource(R.string.deck_exhausted_widen_body_format, nextTierKm)
                },
                trailing = {
                    Text(
                        text = stringResource(R.string.deck_exhausted_widen_cta),
                        color = BrandColors.CoralDeep,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    )
                },
                onClick = onWidenDistance,
            )
            ActionCard(
                icon = Icons.Outlined.Replay,
                iconBubbleColor = BrandColors.PeachTint,
                iconTint = BrandColors.PeachInk,
                title = stringResource(R.string.deck_exhausted_review_title),
                body = stringResource(R.string.deck_exhausted_review_body),
                trailing = { ChevronRight() },
                onClick = onReviewPasses,
            )
            ActionCard(
                icon = Icons.Outlined.Pets,
                iconBubbleColor = BrandColors.MintTint,
                iconTint = BrandColors.MintLeafDeep,
                title = stringResource(R.string.deck_exhausted_add_pet_title),
                body = stringResource(R.string.deck_exhausted_add_pet_body),
                trailing = { ChevronRight() },
                onClick = onAddAnotherPet,
            )
        }
    }
}

@Composable
private fun ActionCard(
    icon: ImageVector,
    iconBubbleColor: Color,
    iconTint: Color,
    title: String,
    body: String,
    trailing: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(iconBubbleColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                )
            }
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
private fun ChevronRight() {
    Icon(
        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
    )
}

/**
 * Hero "sniff zone" — a dashed rounded-rect frame with a few scattered
 * decorative marks and a coral paw centerpiece. All Compose primitives,
 * no SVG asset, so the layout adapts to dark/light without a swap.
 */
@Composable
private fun HeroIllustration() {
    val frameColor = BrandColors.CoralLight.copy(alpha = 0.55f)
    Box(
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .height(220.dp)
            .drawBehind {
                drawRoundRect(
                    color = frameColor,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f)),
                    ),
                    cornerRadius = CornerRadius(28.dp.toPx(), 28.dp.toPx()),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        // Decorative sprinkles around the frame. Positions are approximate to the mock —
        // dots + "z" chips + small "x" marks. Kept lightweight (no extra layout).
        Dot(offsetX = (-110).dp, offsetY = (-70).dp)
        Dot(offsetX = 120.dp, offsetY = 60.dp)
        Mark(text = "x", offsetX = (-130).dp, offsetY = 10.dp)
        Mark(text = "x", offsetX = 130.dp, offsetY = (-80).dp)
        ZChip(offsetX = 36.dp, offsetY = (-72).dp, scale = 0.85f)
        ZChip(offsetX = (-20).dp, offsetY = (-88).dp, scale = 0.7f)

        // Centerpiece — large coral-tinted circle with a paw icon.
        Box(
            modifier = Modifier
                .size(160.dp)
                .background(BrandColors.CoralTint, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Pets,
                contentDescription = null,
                tint = BrandColors.CoralDeep,
                modifier = Modifier.size(64.dp),
            )
        }
    }
}

@Composable
private fun Dot(offsetX: androidx.compose.ui.unit.Dp, offsetY: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .offset(x = offsetX, y = offsetY)
            .size(8.dp)
            .background(BrandColors.CoralLight, CircleShape),
    )
}

@Composable
private fun Mark(
    text: String,
    offsetX: androidx.compose.ui.unit.Dp,
    offsetY: androidx.compose.ui.unit.Dp,
) {
    Text(
        text = text,
        color = BrandColors.CoralLight,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.offset(x = offsetX, y = offsetY),
    )
}

@Composable
private fun ZChip(
    offsetX: androidx.compose.ui.unit.Dp,
    offsetY: androidx.compose.ui.unit.Dp,
    scale: Float,
) {
    Box(
        modifier = Modifier
            .offset(x = offsetX, y = offsetY)
            .size(width = (28 * scale).dp, height = (28 * scale).dp)
            .background(MaterialTheme.colorScheme.surface, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "z",
            color = BrandColors.CoralDeep,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
        )
    }
}

@Suppress("UnusedPrivateMember")
@Composable
private fun PreviewRestoredPlural(count: Int) {
    // Sanity check that pluralStringResource resolves; used only in IDE inspection.
    pluralStringResource(R.plurals.deck_review_restored_count, count, count)
}

private const val MAX_RADIUS_KM = 200

@Preview(name = "Exhausted — 25 km", showBackground = true, heightDp = 900)
@Composable
private fun PreviewExhausted25() {
    TinPetTheme {
        DeckExhaustedState(
            maxDistanceKm = 25,
            onWidenDistance = {},
            onReviewPasses = {},
            onAddAnotherPet = {},
        )
    }
}

@Preview(name = "Exhausted — at max radius", showBackground = true, heightDp = 900)
@Composable
private fun PreviewExhaustedMax() {
    TinPetTheme {
        DeckExhaustedState(
            maxDistanceKm = 200,
            onWidenDistance = {},
            onReviewPasses = {},
            onAddAnotherPet = {},
        )
    }
}

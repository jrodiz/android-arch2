package com.rodiz.arch2.feature.deck.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.rodiz.arch2.core.designsystem.theme.BrandColors
import com.rodiz.arch2.feature.deck.domain.model.DeckCard
import com.rodiz.arch2.feature.deck.domain.model.SwipeAction
import com.rodiz.arch2.feature.pet.domain.model.PetId
import com.rodiz.arch2.feature.pet.domain.model.PhotoSource
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * The redesigned Discover card. Wraps a `Box` that stacks the photo + info pane
 * (inside a rounded `Surface`) with two floating chips overlapping the top edge.
 * The whole box carries the drag/swipe gesture so the user can fling either chip
 * along with the card body.
 */
@Composable
internal fun DeckCardView(
    card: DeckCard,
    onSwipe: (SwipeAction) -> Unit,
    onCardTap: (PetId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val offsetX = remember(card.pet.id.value) { Animatable(0f) }
    val offsetY = remember(card.pet.id.value) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val thresholdPx = with(density) { 140.dp.toPx() }
    val cardWidthPx = with(density) { 360.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .offset { IntOffset(offsetX.value.toInt(), offsetY.value.toInt()) }
            .graphicsLayer { rotationZ = (offsetX.value / cardWidthPx) * 15f }
            .pointerInput(card.pet.id.value) {
                // Multiplex tap + drag on the same pointer: a pointer that doesn't
                // pass the touch slop within its lifetime is treated as a tap and
                // opens the detail sheet. A pointer that DOES pass slop continues
                // into the horizontal swipe path. Critically, do NOT stack
                // Modifier.clickable on top — it would consume pointer-down events
                // before the gesture detector runs, breaking swipes.
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val dragChange = awaitTouchSlopOrCancellation(down.id) { change, _ ->
                        change.consume()
                    }
                    if (dragChange == null) {
                        // Pointer was released before crossing slop → tap.
                        onCardTap(card.pet.id)
                    } else {
                        // Apply the initial slop-breaking delta, then continue with
                        // the existing horizontal-drag accumulation.
                        scope.launch {
                            offsetX.snapTo(offsetX.value + dragChange.positionChange().x)
                            offsetY.snapTo(offsetY.value + dragChange.positionChange().y * 0.3f)
                        }
                        val dragSucceeded = horizontalDrag(dragChange.id) { change ->
                            change.consume()
                            scope.launch {
                                offsetX.snapTo(offsetX.value + change.positionChange().x)
                                offsetY.snapTo(offsetY.value + change.positionChange().y * 0.3f)
                            }
                        }
                        scope.launch {
                            if (dragSucceeded && abs(offsetX.value) > thresholdPx) {
                                val direction = if (offsetX.value > 0) 1 else -1
                                val target = direction * cardWidthPx * 2f
                                offsetX.animateTo(target, tween(durationMillis = 280))
                                onSwipe(if (direction > 0) SwipeAction.LIKE else SwipeAction.PASS)
                                offsetX.snapTo(0f)
                                offsetY.snapTo(0f)
                            } else {
                                offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                                offsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                            }
                        }
                    }
                }
            },
    ) {
        // The card body sits below the chips with top padding so the chips can
        // overlap its top edge without being clipped by the parent Box.
        DeckCardBody(
            card = card,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp),
        )

        // Distance chip — dark filled pill, top-left, overlapping the card edge.
        DistanceChip(
            label = card.distanceBucket?.label ?: stringResource(R.string.deck_distance_unknown),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 14.dp),
        )

        // Intent chip — coral filled pill, top-right, overlapping the card edge.
        val primaryIntent = card.pet.intents.firstOrNull()
        if (primaryIntent != null) {
            IntentChip(
                label = primaryIntent.name.displayCase(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 14.dp),
            )
        }
    }
}

@Composable
private fun DeckCardBody(
    card: DeckCard,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            PhotoBlock(card = card)
            InfoPane(card = card)
        }
    }
}

@Composable
private fun PhotoBlock(card: DeckCard) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.05f),
    ) {
        val primaryUrl = card.pet.photos.firstOrNull()?.let {
            (it.source as? PhotoSource.Remote)?.downloadUrl
                ?: (it.source as? PhotoSource.Local)?.uri
        }
        if (primaryUrl != null) {
            AsyncImage(
                model = primaryUrl,
                contentDescription = card.pet.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(
                        RoundedCornerShape(
                            topStart = 24.dp,
                            topEnd = 24.dp,
                            bottomStart = 0.dp,
                            bottomEnd = 0.dp,
                        ),
                    ),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(
                        RoundedCornerShape(
                            topStart = 24.dp,
                            topEnd = 24.dp,
                            bottomStart = 0.dp,
                            bottomEnd = 0.dp,
                        ),
                    )
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Pets,
                    contentDescription = null,
                    modifier = Modifier.size(96.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Owner chip — straddles the photo's bottom-left edge into the info pane.
        OwnerChip(
            firstName = card.owner?.firstName,
            avatarUrl = card.owner?.avatarUrl,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 14.dp, y = 16.dp),
        )
    }
}

@Composable
private fun OwnerChip(
    firstName: String?,
    avatarUrl: String?,
    modifier: Modifier = Modifier,
) {
    val labelText = firstName
        ?.takeIf { it.isNotBlank() }
        ?.let { stringResource(R.string.deck_owner_with_format, it) }
        ?: stringResource(R.string.deck_owner_with_unknown)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(percent = 50),
        color = Color.White,
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(BrandColors.CoralLight.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                if (!avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.Person,
                        contentDescription = null,
                        tint = BrandColors.CoralDeep,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            Text(
                text = labelText,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun InfoPane(card: DeckCard) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 18.dp),
    ) {
        // Pet name + age badge.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = card.pet.name,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            AgeBadge(
                ageYears = card.pet.ageYears,
                approximate = card.pet.ageIsApproximate,
            )
        }
        Spacer(Modifier.height(10.dp))
        // Species + intent chips.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SpeciesChip(label = card.pet.species.name.displayCase())
            val secondaryIntent = card.pet.intents.elementAtOrNull(1)
                ?: card.pet.intents.firstOrNull()
            if (secondaryIntent != null) {
                IntentTagChip(label = secondaryIntent.name.displayCase())
            }
        }
        if (!card.pet.bio.isNullOrBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.deck_bio_quote_format, card.pet.bio!!),
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AgeBadge(ageYears: Int, approximate: Boolean) {
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = BrandColors.CoralLight.copy(alpha = 0.35f),
    ) {
        Text(
            text = "${if (approximate) "~" else ""}$ageYears",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = BrandColors.CoralDeep,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun SpeciesChip(label: String) {
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
            shape = RoundedCornerShape(percent = 50),
        ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun IntentTagChip(label: String) {
    Surface(
        shape = RoundedCornerShape(percent = 50),
        color = BrandColors.CoralLight.copy(alpha = 0.25f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = BrandColors.CoralDeep,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = BrandColors.CoralDeep,
            )
        }
    }
}

@Composable
private fun DistanceChip(label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(percent = 50),
        color = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Place,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
            )
        }
    }
}

@Composable
private fun IntentChip(label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(percent = 50),
        color = BrandColors.Coral,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
            )
        }
    }
}

private fun String.displayCase(): String = lowercase()
    .replace('_', ' ')
    .replaceFirstChar { it.uppercase() }

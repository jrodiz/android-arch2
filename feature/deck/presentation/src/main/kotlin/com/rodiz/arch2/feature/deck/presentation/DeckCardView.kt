package com.rodiz.arch2.feature.deck.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.rodiz.arch2.feature.deck.domain.model.DeckCard
import com.rodiz.arch2.feature.deck.domain.model.SwipeAction
import com.rodiz.arch2.feature.pet.domain.model.PhotoSource
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
internal fun DeckCardView(
    card: DeckCard,
    onSwipe: (SwipeAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val offsetX = remember(card.pet.id.value) { Animatable(0f) }
    val offsetY = remember(card.pet.id.value) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val thresholdPx = with(density) { 140.dp.toPx() }
    val cardWidthPx = with(density) { 360.dp.toPx() }

    Card(
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = modifier
            .fillMaxSize()
            .offset { IntOffset(offsetX.value.toInt(), offsetY.value.toInt()) }
            .graphicsLayer { rotationZ = (offsetX.value / cardWidthPx) * 15f }
            .pointerInput(card.pet.id.value) {
                detectDragGestures(
                    onDrag = { change, drag ->
                        change.consume()
                        scope.launch {
                            offsetX.snapTo(offsetX.value + drag.x)
                            offsetY.snapTo(offsetY.value + drag.y * 0.3f)
                        }
                    },
                    onDragEnd = {
                        scope.launch {
                            if (abs(offsetX.value) > thresholdPx) {
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
                    },
                )
            },
    ) {
        Box(Modifier.fillMaxSize()) {
            val primaryUrl = card.pet.photos.firstOrNull()?.let {
                (it.source as? PhotoSource.Remote)?.downloadUrl
                    ?: (it.source as? PhotoSource.Local)?.uri
            }
            if (primaryUrl != null) {
                AsyncImage(
                    model = primaryUrl,
                    contentDescription = card.pet.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
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

            // Bottom gradient + info overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.75f),
                        ),
                    )
                    .padding(20.dp),
            ) {
                Column {
                    Text(
                        text = "${card.pet.name}, ${if (card.pet.ageIsApproximate) "~" else ""}${card.pet.ageYears}",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Text(
                        text = card.pet.species.name.lowercase().replaceFirstChar { it.uppercase() },
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (card.pet.intents.isNotEmpty()) {
                        Spacer(Modifier.size(8.dp))
                        Row {
                            card.pet.intents.take(3).forEach { intent ->
                                AssistChip(
                                    onClick = {},
                                    label = { Text(intent.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

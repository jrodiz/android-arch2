package com.rodiz.arch2.feature.pet.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.rodiz.arch2.core.designsystem.theme.BrandColors
import com.rodiz.arch2.feature.pet.domain.model.Intent
import com.rodiz.arch2.feature.pet.domain.model.Pet
import com.rodiz.arch2.feature.pet.domain.model.PhotoSource

private val SageGreen = Color(0xFF7CC384)
private val SageDeep = Color(0xFF4F8F5C)
private val PausedGray = Color(0xFF9E9E9E)
private val AdoptionBrown = Color(0xFF8B5E3C)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PetThumbnailCard(
    pet: Pet,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            PetPhotoBox(pet = pet, onEdit = onEdit)
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = pet.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val subtitleRes = if (pet.ageIsApproximate) {
                    R.string.pet_subtitle_approx_age
                } else {
                    R.string.pet_subtitle_exact_age
                }
                Text(
                    text = stringResource(subtitleRes, pet.ageYears, pet.species.label()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (pet.intents.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 2.dp),
                    ) {
                        // Render in a stable order (PLAYDATE, FRIENDSHIP, ADOPTION) for visual consistency.
                        Intent.entries
                            .filter { it in pet.intents }
                            .forEach { intent -> IntentChip(intent = intent) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PetPhotoBox(pet: Pet, onEdit: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        val primaryUrl = pet.photos.firstOrNull()?.let {
            (it.source as? PhotoSource.Remote)?.downloadUrl
                ?: (it.source as? PhotoSource.Local)?.uri
        }
        val photoModifier = if (pet.enabled) Modifier.fillMaxSize()
        else Modifier.fillMaxSize().alpha(0.45f)
        if (primaryUrl != null) {
            AsyncImage(
                model = primaryUrl,
                contentDescription = pet.name,
                contentScale = ContentScale.Crop,
                modifier = photoModifier,
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Pets,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        PetStatusPill(
            active = pet.enabled,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp),
        )

        val editLabel = stringResource(R.string.pet_a11y_edit_pet, pet.name)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .size(30.dp)
                .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                .clickable(onClick = onEdit),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = editLabel,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
internal fun PetStatusPill(
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    val color = if (active) SageGreen else PausedGray
    val labelRes = if (active) R.string.pet_status_active else R.string.pet_status_paused
    Surface(
        color = color,
        shape = RoundedCornerShape(50),
        shadowElevation = 0.dp,
        modifier = modifier,
    ) {
        Text(
            text = stringResource(labelRes).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
            ),
            color = Color.White,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
internal fun IntentChip(
    intent: Intent,
    modifier: Modifier = Modifier,
) {
    data class Style(
        val fill: Color,
        val content: Color,
        val border: BorderStroke?,
        val labelRes: Int,
        val icon: androidx.compose.ui.graphics.vector.ImageVector,
    )
    val style = when (intent) {
        Intent.PLAYDATE -> Style(
            fill = BrandColors.Coral.copy(alpha = 0.22f),
            content = BrandColors.CoralDeep,
            border = null,
            labelRes = R.string.pet_intent_playdate,
            icon = Icons.Outlined.Pets,
        )
        Intent.FRIENDSHIP -> Style(
            fill = Color.Transparent,
            content = SageDeep,
            border = BorderStroke(1.dp, SageGreen),
            labelRes = R.string.pet_intent_friendship,
            icon = Icons.Outlined.Favorite,
        )
        Intent.ADOPTION -> Style(
            fill = BrandColors.CoralLight.copy(alpha = 0.42f),
            content = AdoptionBrown,
            border = null,
            labelRes = R.string.pet_intent_adoption,
            icon = Icons.Outlined.Home,
        )
    }
    Surface(
        color = style.fill,
        contentColor = style.content,
        shape = RoundedCornerShape(50),
        border = style.border,
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        ) {
            Icon(
                imageVector = style.icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = style.content,
            )
            Text(
                text = stringResource(style.labelRes),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = style.content,
            )
            Spacer(Modifier.size(0.dp))
        }
    }
}

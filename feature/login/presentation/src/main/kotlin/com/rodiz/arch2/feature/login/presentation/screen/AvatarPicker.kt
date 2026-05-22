package com.rodiz.arch2.feature.login.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.rodiz.arch2.core.designsystem.theme.BrandColors
import com.rodiz.arch2.feature.login.presentation.R

private val PickerSize = 160.dp
private val PickerCornerRadius = 28.dp

/**
 * Sign-up avatar picker. 160dp rounded-square with a dashed coral border and a soft
 * pink fill. Empty state shows a centered camera icon plus a coral "+" badge in the
 * bottom-right; filled state shows the picked image clipped to the same shape with a
 * Close affordance in the top-right.
 */
@Composable
fun AvatarPicker(
    avatarUri: String?,
    onPickTapped: () -> Unit,
    onCleared: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pickerCd = stringResource(R.string.signup_avatar_picker_cd)
    val density = LocalDensity.current
    val dashOn = with(density) { 8.dp.toPx() }
    val dashOff = with(density) { 6.dp.toPx() }
    val strokePx = with(density) { 2.dp.toPx() }
    val cornerPx = with(density) { PickerCornerRadius.toPx() }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(PickerSize)
                .clip(RoundedCornerShape(PickerCornerRadius))
                .background(BrandColors.Coral.copy(alpha = 0.08f))
                .drawBehind {
                    drawRoundRect(
                        color = BrandColors.Coral,
                        cornerRadius = CornerRadius(cornerPx, cornerPx),
                        style = Stroke(
                            width = strokePx,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashOn, dashOff)),
                        ),
                    )
                }
                .clickable(onClick = onPickTapped)
                .semantics { contentDescription = pickerCd }
                .testTag("signup_avatar_picker"),
            contentAlignment = Alignment.Center,
        ) {
            if (avatarUri != null) {
                AsyncImage(
                    model = avatarUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(PickerSize)
                        .clip(RoundedCornerShape(PickerCornerRadius)),
                )
                // Close affordance in the top-right replaces the "+" badge when set.
                IconButton(
                    onClick = onCleared,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .testTag("signup_avatar_clear"),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.signup_avatar_clear),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Outlined.PhotoCamera,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp),
                )
                // "+" badge bottom-right hints that the empty box is a picker.
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
        if (avatarUri == null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.signup_avatar_picker_label),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        } else {
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = onCleared,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text(
                    text = stringResource(R.string.signup_avatar_clear),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
    // Unused import suppressor (Arrangement) — kept for parity if callers add it later.
    @Suppress("UNUSED_EXPRESSION") Arrangement
}

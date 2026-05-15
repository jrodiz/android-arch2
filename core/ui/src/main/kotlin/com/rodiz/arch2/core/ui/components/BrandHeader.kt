package com.rodiz.arch2.core.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.rodiz.arch2.core.designsystem.theme.BrandColors
import com.rodiz.arch2.core.designsystem.theme.WaveBottomShape

/**
 * Coral hero block clipped by the WaveBottomShape, with a topographic-style
 * pattern lightly overlaid. Fills whatever size its parent allocates — caller
 * controls the height with `Modifier.height(...)` or `Modifier.weight(...)`.
 *
 * The pattern asset belongs to the feature using the header (so brand-neutral
 * code stays in :core:*). Pass it via [patternRes].
 */
@Composable
fun BrandHeader(
    @DrawableRes patternRes: Int,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit = {},
) {
    Box(
        modifier = modifier
            .clip(WaveBottomShape)
            .background(BrandColors.Coral),
    ) {
        Image(
            painter = painterResource(patternRes),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.28f),
        )
        content()
    }
}

package com.rodiz.arch2.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Dark mode is intentionally disabled for the v1 launch — the brand identity
 * (coral hero, peach tiles, cream surfaces) was designed for a light surface
 * only and the dark scheme hasn't been color-audited yet. The unused
 * [DarkScheme] is left in place so we can revive support later without
 * re-discovering the palette.
 *
 * `App.kt` also forces `AppCompatDelegate.MODE_NIGHT_NO` so AppCompat-rendered
 * dialogs / pickers (DatePicker for quiet hours, etc.) match — without that
 * those surfaces would still respect the system dark setting.
 */
@Composable
fun TinPetTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LightScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}

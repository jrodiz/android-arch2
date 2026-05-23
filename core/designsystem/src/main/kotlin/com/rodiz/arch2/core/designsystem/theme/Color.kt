package com.rodiz.arch2.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

internal val LightScheme = lightColorScheme(
    primary = Color(0xFFE97A7A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD5),
    onPrimaryContainer = Color(0xFF410001),
    secondary = Color(0xFF775653),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDAD5),
    onSecondaryContainer = Color(0xFF2C1513),
    surface = Color(0xFFFFFBFA),
    onSurface = Color(0xFF211B1A),
    surfaceVariant = Color(0xFFF5DDDA),
    onSurfaceVariant = Color(0xFF534340),
    outline = Color(0xFF857370),
    background = Color(0xFFFFFBFA),
    onBackground = Color(0xFF211B1A),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

internal val DarkScheme = darkColorScheme(
    primary = Color(0xFFFFB3AC),
    onPrimary = Color(0xFF5C1212),
    primaryContainer = Color(0xFF7D2A2A),
    onPrimaryContainer = Color(0xFFFFDAD5),
    secondary = Color(0xFFE7BDB8),
    onSecondary = Color(0xFF442927),
    secondaryContainer = Color(0xFF5D3F3C),
    onSecondaryContainer = Color(0xFFFFDAD5),
    surface = Color(0xFF1A1110),
    onSurface = Color(0xFFF1DEDB),
    surfaceVariant = Color(0xFF534340),
    onSurfaceVariant = Color(0xFFD8C2BE),
    outline = Color(0xFFA08C89),
    background = Color(0xFF1A1110),
    onBackground = Color(0xFFF1DEDB),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

/**
 * Brand colors that don't switch with the color scheme — used by the login hero
 * to keep the coral header consistent across light/dark.
 */
object BrandColors {
    val Coral: Color = Color(0xFFF08A8A)
    val CoralDeep: Color = Color(0xFFE97A7A)
    /** Lighter peach used as the bottom stop of the SignUp hero's vertical gradient. */
    val CoralLight: Color = Color(0xFFF6B5A0)
    val CoralOnPattern: Color = Color.White
    /** Warm dark surface for the floating chip nav capsule — sits in the same family as onSurface but reads as a deliberate surface. */
    val NavSurface: Color = Color(0xFF2A1F1D)
    /** Mint green used on the deck pet-detail "Friendship" intent chip. */
    val MintLeaf: Color = Color(0xFF6FAE9C)
    /** Darker mint used as the icon-circle background on the Filters "Friendship" tile. */
    val MintLeafDeep: Color = Color(0xFF4F9485)
    /** Warm peach used as the saturated fill of the Filters "Adoption" intent tile. */
    val PeachWarm: Color = Color(0xFFE8A275)
    /** Light peach pair for the unselected state of the Adoption tile (alpha applied at call site). */
    val PeachWarmLight: Color = Color(0xFFF6CBAA)
    /** Darker peach used as the icon-circle background on the Filters "Adoption" tile. */
    val PeachWarmDeep: Color = Color(0xFFD27750)
    /** Off-white cream used as the Filters screen container background. */
    val FiltersCream: Color = Color(0xFFFBF5F0)

    // ----- Settings-home tinted icon palette -----------------------------------
    // Tinted icon squares (44dp, RoundedCornerShape(12.dp)) read against the cream
    // screen background. Each tint comes with a darker "ink" foreground so the
    // glyph carries enough contrast.

    /** Cream screen background used across the Settings redesign (lighter than surface). */
    val Cream: Color = Color(0xFFFBF1E9)

    /** Mint icon tile background — Profile, Privacy. Foreground = [MintLeaf]. */
    val MintTint: Color = Color(0xFFDDEFE9)
    /** Peach icon tile background — Account. Foreground = [PeachInk]. */
    val PeachTint: Color = Color(0xFFFCE3D6)
    /** Bright coral icon tile background — Filters, Blocked owners. Foreground = [CoralDeep]. */
    val CoralTint: Color = Color(0xFFF7DAD3)
    /** Lavender icon tile background — Notifications. Foreground = [LavenderInk]. */
    val LavenderTint: Color = Color(0xFFE5DDF6)
    /** Neutral grey icon tile background — Pause profile. Foreground = [NeutralInk]. */
    val NeutralTint: Color = Color(0xFFE9E4E0)

    /** Foreground glyph color for [PeachTint] tiles. */
    val PeachInk: Color = Color(0xFFC4724F)
    /** Foreground glyph color for [LavenderTint] tiles. */
    val LavenderInk: Color = Color(0xFF6E5DB8)
    /** Foreground glyph color for [NeutralTint] tiles. */
    val NeutralInk: Color = Color(0xFF8B807A)

    /** Soft yellow icon tile background — Notifications quiet-hours row. Foreground = [MoonInk]. */
    val MoonTint: Color = Color(0xFFFFF1C2)
    /** Warm yellow crescent-moon glyph color for [MoonTint] tiles. */
    val MoonInk: Color = Color(0xFFE5B73B)
}

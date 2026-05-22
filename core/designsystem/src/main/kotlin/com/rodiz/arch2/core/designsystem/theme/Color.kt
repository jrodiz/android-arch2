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
}

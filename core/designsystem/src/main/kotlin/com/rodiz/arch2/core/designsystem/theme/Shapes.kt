package com.rodiz.arch2.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Clips the bottom edge of a box into a soft wave. Use to mask the coral hero so
 * its lower edge curves into the form area like the mockup. The wave is built with
 * a single quadratic bezier whose control point sits below the bottom edge.
 */
val WaveBottomShape: Shape = object : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val waveDepth = size.height * 0.12f
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height - waveDepth)
            quadraticBezierTo(
                size.width * 0.55f,
                size.height + waveDepth,
                0f,
                size.height - waveDepth * 0.4f,
            )
            close()
        }
        return Outline.Generic(path)
    }
}

internal val AppShapes: Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

package com.rodiz.arch2.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

internal val AppTypography: Typography = Typography().run {
    copy(
        displayMedium = displayMedium.copy(
            fontWeight = FontWeight.ExtraBold,
            fontSize = 44.sp,
            lineHeight = 52.sp,
            letterSpacing = (-1).sp,
        ),
        titleMedium = titleMedium.copy(
            fontWeight = FontWeight.SemiBold,
        ),
        labelLarge = labelLarge.copy(
            fontWeight = FontWeight.SemiBold,
        ),
    )
}

internal val BrandFieldLabelStyle: TextStyle = TextStyle(
    fontWeight = FontWeight.Bold,
    fontSize = 16.sp,
    lineHeight = 20.sp,
)

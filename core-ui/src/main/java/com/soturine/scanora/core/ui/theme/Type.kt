package com.soturine.scanora.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun scanoraTextStyle(weight: FontWeight, size: Int, lineHeight: Int) = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
)

val ScanoraTypography = Typography(
    headlineLarge = scanoraTextStyle(FontWeight.Bold, 30, 36),
    headlineMedium = scanoraTextStyle(FontWeight.Bold, 25, 31),
    headlineSmall = scanoraTextStyle(FontWeight.SemiBold, 22, 28),
    titleLarge = scanoraTextStyle(FontWeight.SemiBold, 21, 27),
    titleMedium = scanoraTextStyle(FontWeight.SemiBold, 18, 24),
    titleSmall = scanoraTextStyle(FontWeight.SemiBold, 16, 22),
    bodyLarge = scanoraTextStyle(FontWeight.Normal, 17, 26),
    bodyMedium = scanoraTextStyle(FontWeight.Normal, 15, 23),
    bodySmall = scanoraTextStyle(FontWeight.Normal, 14, 20),
    labelLarge = scanoraTextStyle(FontWeight.SemiBold, 16, 22),
    labelMedium = scanoraTextStyle(FontWeight.Medium, 14, 19),
    labelSmall = scanoraTextStyle(FontWeight.Medium, 12, 17),
)

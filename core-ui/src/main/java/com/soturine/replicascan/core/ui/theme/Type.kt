package com.soturine.replicascan.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun replicascanTextStyle(weight: FontWeight, size: Int, lineHeight: Int) = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
)

val ReplicaScanTypography = Typography(
    headlineLarge = replicascanTextStyle(FontWeight.Medium, 32, 40).copy(fontFamily = FontFamily.Serif),
    headlineMedium = replicascanTextStyle(FontWeight.Medium, 28, 36).copy(fontFamily = FontFamily.Serif),
    headlineSmall = replicascanTextStyle(FontWeight.Medium, 24, 32).copy(fontFamily = FontFamily.Serif),
    titleLarge = replicascanTextStyle(FontWeight.SemiBold, 21, 27),
    titleMedium = replicascanTextStyle(FontWeight.SemiBold, 18, 24),
    titleSmall = replicascanTextStyle(FontWeight.SemiBold, 16, 22),
    bodyLarge = replicascanTextStyle(FontWeight.Normal, 17, 26),
    bodyMedium = replicascanTextStyle(FontWeight.Normal, 15, 23),
    bodySmall = replicascanTextStyle(FontWeight.Normal, 14, 20),
    labelLarge = replicascanTextStyle(FontWeight.SemiBold, 16, 22),
    labelMedium = replicascanTextStyle(FontWeight.Medium, 14, 19),
    labelSmall = replicascanTextStyle(FontWeight.Medium, 12, 17),
)

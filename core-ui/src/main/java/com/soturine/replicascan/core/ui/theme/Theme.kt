package com.soturine.replicascan.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = BrandOrange,
    onPrimary = Color.White,
    primaryContainer = BrandOrangeContainer,
    onPrimaryContainer = Navy,
    secondary = ReplicaScanTeal,
    onSecondary = Color.White,
    secondaryContainer = ReplicaScanTealContainer,
    onSecondaryContainer = Navy,
    tertiary = ReplicaScanTeal,
    onTertiary = Color.White,
    background = Cream,
    onBackground = Navy,
    surface = WarmWhite,
    onSurface = Navy,
    surfaceVariant = WarmSurface,
    onSurfaceVariant = NavySoft,
    surfaceContainer = WarmSurface,
    surfaceContainerLow = WarmWhite,
    surfaceContainerHigh = Color(0xFFF2E8DC),
    surfaceContainerHighest = Color(0xFFECE0D2),
    outline = Color(0xFF776D65),
    outlineVariant = WarmOutline,
    error = ReplicaScanError,
    inverseSurface = Navy,
    inverseOnSurface = Cream,
)

private val DarkColors = darkColorScheme(
    primary = BrandOrangeDark,
    onPrimary = Color(0xFF3C1600),
    primaryContainer = BrandOrangeContainerDark,
    onPrimaryContainer = Color(0xFFFFDBC8),
    secondary = ReplicaScanTealDark,
    onSecondary = Color(0xFF00373A),
    secondaryContainer = ReplicaScanTealContainerDark,
    onSecondaryContainer = Color(0xFFA4EFF2),
    tertiary = ReplicaScanTealDark,
    onTertiary = Color(0xFF00373A),
    background = Night,
    onBackground = Color(0xFFF6EEE5),
    surface = NightSurface,
    onSurface = Color(0xFFF6EEE5),
    surfaceVariant = NightSurfaceHigh,
    onSurfaceVariant = Color(0xFFD3DEE0),
    surfaceContainer = NightSurface,
    surfaceContainerLow = Color(0xFF101E24),
    surfaceContainerHigh = NightSurfaceHigh,
    surfaceContainerHighest = Color(0xFF28434D),
    outline = Color(0xFF91AAB0),
    outlineVariant = NightOutline,
    error = Color(0xFFFFB4AB),
    inverseSurface = Cream,
    inverseOnSurface = Night,
)

@Composable
fun ReplicaScanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = ReplicaScanTypography,
        shapes = ReplicaScanShapes,
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = colors.background,
            contentColor = colors.onBackground,
            content = content,
        )
    }
}

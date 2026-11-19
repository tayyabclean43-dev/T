package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.core.ThemeColorPalette
import com.example.core.ThemePreference

val RankPulseShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun RankPulseTheme(
    themePreference: ThemePreference = ThemePreference.DARK,
    colorPalette: ThemeColorPalette = ThemeColorPalette.EMERALD,
    content: @Composable () -> Unit
) {
    val isDark = when (themePreference) {
        ThemePreference.DARK -> true
        ThemePreference.LIGHT -> false
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = colorSchemeFor(colorPalette, isDark)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = RankPulseShapes,
        content = content
    )
}

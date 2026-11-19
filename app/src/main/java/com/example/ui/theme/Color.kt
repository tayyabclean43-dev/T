package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.example.core.ThemeColorPalette

// Shared semantic status colors
val StatusError = Color(0xFFEF4444)
val StatusWarning = Color(0xFFF59E0B)
val StatusSuccess = Color(0xFF10B981)

// 1. Emerald & Amber Palette
val EmeraldPrimaryDark = Color(0xFF34D399)
val OnEmeraldDark = Color(0xFF022C22)
val EmeraldContainerDark = Color(0xFF064E3B)
val OnEmeraldContainerDark = Color(0xFFA7F3D0)

val AmberSecondaryDark = Color(0xFFFBBF24)
val OnAmberDark = Color(0xFF451A03)
val AmberContainerDark = Color(0xFF78350F)
val OnAmberContainerDark = Color(0xFFFDE68A)

val CyanTertiaryDark = Color(0xFF38BDF8)
val OnCyanDark = Color(0xFF082F49)
val CyanContainerDark = Color(0xFF0C4A6E)
val OnCyanContainerDark = Color(0xFFBAE6FD)

val DarkBackground = Color(0xFF07110E)
val DarkSurface = Color(0xFF0C1814)
val DarkSurfaceVariant = Color(0xFF132620)
val DarkSurfaceElevated = Color(0xFF183129)
val DarkOutline = Color(0xFF28473D)
val DarkTextPrimary = Color(0xFFECFDF5)
val DarkTextSecondary = Color(0xFF9CA3AF)

val EmeraldPrimaryLight = Color(0xFF047857)
val OnEmeraldLight = Color(0xFFFFFFFF)
val EmeraldContainerLight = Color(0xFFD1FAE5)
val OnEmeraldContainerLight = Color(0xFF022C22)

val AmberSecondaryLight = Color(0xFFB45309)
val OnAmberLight = Color(0xFFFFFFFF)
val AmberContainerLight = Color(0xFFFEF3C7)
val OnAmberContainerLight = Color(0xFF451A03)

val CyanTertiaryLight = Color(0xFF0369A1)
val OnCyanLight = Color(0xFFFFFFFF)
val CyanContainerLight = Color(0xFFE0F2FE)
val OnCyanContainerLight = Color(0xFF082F49)

val LightBackground = Color(0xFFF6FAF8)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE8F2EE)
val LightOutline = Color(0xFFC5DBD3)
val LightTextPrimary = Color(0xFF0B1F1A)
val LightTextSecondary = Color(0xFF475569)

fun colorSchemeFor(palette: ThemeColorPalette, isDark: Boolean): ColorScheme = when (palette) {
    ThemeColorPalette.EMERALD -> if (isDark) {
        darkColorScheme(
            primary = EmeraldPrimaryDark,
            onPrimary = OnEmeraldDark,
            primaryContainer = EmeraldContainerDark,
            onPrimaryContainer = OnEmeraldContainerDark,
            secondary = AmberSecondaryDark,
            onSecondary = OnAmberDark,
            secondaryContainer = AmberContainerDark,
            onSecondaryContainer = OnAmberContainerDark,
            tertiary = CyanTertiaryDark,
            onTertiary = OnCyanDark,
            tertiaryContainer = CyanContainerDark,
            onTertiaryContainer = OnCyanContainerDark,
            background = DarkBackground,
            onBackground = DarkTextPrimary,
            surface = DarkSurface,
            onSurface = DarkTextPrimary,
            surfaceVariant = DarkSurfaceVariant,
            onSurfaceVariant = DarkTextSecondary,
            surfaceContainerLow = DarkSurface,
            surfaceContainer = DarkSurfaceVariant,
            surfaceContainerHigh = DarkSurfaceElevated,
            outline = DarkOutline,
            error = StatusError
        )
    } else {
        lightColorScheme(
            primary = EmeraldPrimaryLight,
            onPrimary = OnEmeraldLight,
            primaryContainer = EmeraldContainerLight,
            onPrimaryContainer = OnEmeraldContainerLight,
            secondary = AmberSecondaryLight,
            onSecondary = OnAmberLight,
            secondaryContainer = AmberContainerLight,
            onSecondaryContainer = OnAmberContainerLight,
            tertiary = CyanTertiaryLight,
            onTertiary = OnCyanLight,
            tertiaryContainer = CyanContainerLight,
            onTertiaryContainer = OnCyanContainerLight,
            background = LightBackground,
            onBackground = LightTextPrimary,
            surface = LightSurface,
            onSurface = LightTextPrimary,
            surfaceVariant = LightSurfaceVariant,
            onSurfaceVariant = LightTextSecondary,
            surfaceContainerLow = LightSurface,
            surfaceContainer = LightSurfaceVariant,
            surfaceContainerHigh = LightSurfaceVariant,
            outline = LightOutline,
            error = StatusError
        )
    }

    ThemeColorPalette.OCEAN_BLUE -> if (isDark) {
        darkColorScheme(
            primary = Color(0xFF60A5FA),
            onPrimary = Color(0xFF0A2540),
            primaryContainer = Color(0xFF1E3A8A),
            onPrimaryContainer = Color(0xFFDBEAFE),
            secondary = Color(0xFFFBBF24),
            onSecondary = Color(0xFF451A03),
            secondaryContainer = Color(0xFF78350F),
            onSecondaryContainer = Color(0xFFFDE68A),
            tertiary = Color(0xFF2DD4BF),
            onTertiary = Color(0xFF042F2E),
            tertiaryContainer = Color(0xFF134E4A),
            onTertiaryContainer = Color(0xFFCCFBF1),
            background = Color(0xFF070D19),
            onBackground = Color(0xFFEFF6FF),
            surface = Color(0xFF0D1526),
            onSurface = Color(0xFFEFF6FF),
            surfaceVariant = Color(0xFF17223B),
            onSurfaceVariant = Color(0xFF94A3B8),
            surfaceContainerLow = Color(0xFF0D1526),
            surfaceContainer = Color(0xFF17223B),
            surfaceContainerHigh = Color(0xFF1E2D4D),
            outline = Color(0xFF2B3F6B),
            error = StatusError
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF1D4ED8),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFDBEAFE),
            onPrimaryContainer = Color(0xFF1E3A8A),
            secondary = Color(0xFFD97706),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFFEF3C7),
            onSecondaryContainer = Color(0xFF78350F),
            tertiary = Color(0xFF0F766E),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFCCFBF1),
            onTertiaryContainer = Color(0xFF134E4A),
            background = Color(0xFFF4F7FC),
            onBackground = Color(0xFF0F172A),
            surface = Color.White,
            onSurface = Color(0xFF0F172A),
            surfaceVariant = Color(0xFFE2E8F0),
            onSurfaceVariant = Color(0xFF475569),
            outline = Color(0xFFCBD5E1),
            error = StatusError
        )
    }

    ThemeColorPalette.SAFFRON_CORAL -> if (isDark) {
        darkColorScheme(
            primary = Color(0xFFFB923C),
            onPrimary = Color(0xFF431407),
            primaryContainer = Color(0xFF7C2D12),
            onPrimaryContainer = Color(0xFFFFEDD5),
            secondary = Color(0xFF2DD4BF),
            onSecondary = Color(0xFF042F2E),
            secondaryContainer = Color(0xFF115E59),
            onSecondaryContainer = Color(0xFFCCFBF1),
            tertiary = Color(0xFFFACC15),
            onTertiary = Color(0xFF422006),
            tertiaryContainer = Color(0xFF713F12),
            onTertiaryContainer = Color(0xFFFEF9C3),
            background = Color(0xFF140C08),
            onBackground = Color(0xFFFFF7ED),
            surface = Color(0xFF1C120D),
            onSurface = Color(0xFFFFF7ED),
            surfaceVariant = Color(0xFF291B14),
            onSurfaceVariant = Color(0xFFD6B9A9),
            surfaceContainerLow = Color(0xFF1C120D),
            surfaceContainer = Color(0xFF291B14),
            surfaceContainerHigh = Color(0xFF36241B),
            outline = Color(0xFF523628),
            error = StatusError
        )
    } else {
        lightColorScheme(
            primary = Color(0xFFC2410C),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFFEDD5),
            onPrimaryContainer = Color(0xFF7C2D12),
            secondary = Color(0xFF0F766E),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFCCFBF1),
            onSecondaryContainer = Color(0xFF134E4A),
            tertiary = Color(0xFFCA8A04),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFFEF9C3),
            onTertiaryContainer = Color(0xFF713F12),
            background = Color(0xFFFFFBF7),
            onBackground = Color(0xFF1F120B),
            surface = Color.White,
            onSurface = Color(0xFF1F120B),
            surfaceVariant = Color(0xFFF5EAE2),
            onSurfaceVariant = Color(0xFF5C473C),
            outline = Color(0xFFE2CEC2),
            error = StatusError
        )
    }

    ThemeColorPalette.ROYAL_VIOLET -> if (isDark) {
        darkColorScheme(
            primary = Color(0xFFA78BFA),
            onPrimary = Color(0xFF2E1065),
            primaryContainer = Color(0xFF4C1D95),
            onPrimaryContainer = Color(0xFFEDE9FE),
            secondary = Color(0xFFFBBF24),
            onSecondary = Color(0xFF451A03),
            secondaryContainer = Color(0xFF78350F),
            onSecondaryContainer = Color(0xFFFDE68A),
            tertiary = Color(0xFF34D399),
            onTertiary = Color(0xFF022C22),
            tertiaryContainer = Color(0xFF064E3B),
            onTertiaryContainer = Color(0xFFA7F3D0),
            background = Color(0xFF0E091A),
            onBackground = Color(0xFFF5F3FF),
            surface = Color(0xFF150F26),
            onSurface = Color(0xFFF5F3FF),
            surfaceVariant = Color(0xFF21183B),
            onSurfaceVariant = Color(0xFFB4A9D6),
            surfaceContainerLow = Color(0xFF150F26),
            surfaceContainer = Color(0xFF21183B),
            surfaceContainerHigh = Color(0xFF2B204D),
            outline = Color(0xFF433475),
            error = StatusError
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF6D28D9),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFEDE9FE),
            onPrimaryContainer = Color(0xFF4C1D95),
            secondary = Color(0xFFD97706),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFFEF3C7),
            onSecondaryContainer = Color(0xFF78350F),
            tertiary = Color(0xFF047857),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFD1FAE5),
            onTertiaryContainer = Color(0xFF064E3B),
            background = Color(0xFFFAF8FF),
            onBackground = Color(0xFF181029),
            surface = Color.White,
            onSurface = Color(0xFF181029),
            surfaceVariant = Color(0xFFEDE9F7),
            onSurfaceVariant = Color(0xFF52496E),
            outline = Color(0xFFD4CCEB),
            error = StatusError
        )
    }

    ThemeColorPalette.ROSE_CRIMSON -> if (isDark) {
        darkColorScheme(
            primary = Color(0xFFFB7185),
            onPrimary = Color(0xFF4C0519),
            primaryContainer = Color(0xFF881337),
            onPrimaryContainer = Color(0xFFFFE4E6),
            secondary = Color(0xFF38BDF8),
            onSecondary = Color(0xFF082F49),
            secondaryContainer = Color(0xFF0C4A6E),
            onSecondaryContainer = Color(0xFFBAE6FD),
            tertiary = Color(0xFFFBBF24),
            onTertiary = Color(0xFF451A03),
            tertiaryContainer = Color(0xFF78350F),
            onTertiaryContainer = Color(0xFFFDE68A),
            background = Color(0xFF14080C),
            onBackground = Color(0xFFFFF1F2),
            surface = Color(0xFF1D0D13),
            onSurface = Color(0xFFFFF1F2),
            surfaceVariant = Color(0xFF2D141D),
            onSurfaceVariant = Color(0xFFD6A8B6),
            surfaceContainerLow = Color(0xFF1D0D13),
            surfaceContainer = Color(0xFF2D141D),
            surfaceContainerHigh = Color(0xFF3D1C28),
            outline = Color(0xFF5E2B3D),
            error = Color(0xFFFCA5A5)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFFBE123C),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFFE4E6),
            onPrimaryContainer = Color(0xFF881337),
            secondary = Color(0xFF0369A1),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFE0F2FE),
            onSecondaryContainer = Color(0xFF0C4A6E),
            tertiary = Color(0xFFB45309),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFFEF3C7),
            onTertiaryContainer = Color(0xFF78350F),
            background = Color(0xFFFFF8F9),
            onBackground = Color(0xFF260911),
            surface = Color.White,
            onSurface = Color(0xFF260911),
            surfaceVariant = Color(0xFFF7E6EA),
            onSurfaceVariant = Color(0xFF6B434E),
            outline = Color(0xFFE8C8D0),
            error = StatusError
        )
    }

    ThemeColorPalette.TEAL_SLATE -> if (isDark) {
        darkColorScheme(
            primary = Color(0xFF2DD4BF),
            onPrimary = Color(0xFF042F2E),
            primaryContainer = Color(0xFF115E59),
            onPrimaryContainer = Color(0xFFCCFBF1),
            secondary = Color(0xFFF472B6),
            onSecondary = Color(0xFF500724),
            secondaryContainer = Color(0xFF831843),
            onSecondaryContainer = Color(0xFFFCE7F3),
            tertiary = Color(0xFF60A5FA),
            onTertiary = Color(0xFF0A2540),
            tertiaryContainer = Color(0xFF1E3A8A),
            onTertiaryContainer = Color(0xFFDBEAFE),
            background = Color(0xFF081014),
            onBackground = Color(0xFFF0FDFA),
            surface = Color(0xFF0E1A1F),
            onSurface = Color(0xFFF0FDFA),
            surfaceVariant = Color(0xFF162830),
            onSurfaceVariant = Color(0xFF94B4BC),
            surfaceContainerLow = Color(0xFF0E1A1F),
            surfaceContainer = Color(0xFF162830),
            surfaceContainerHigh = Color(0xFF1E3640),
            outline = Color(0xFF2C4D59),
            error = StatusError
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF0F766E),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFCCFBF1),
            onPrimaryContainer = Color(0xFF134E4A),
            secondary = Color(0xFFBE185D),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFFCE7F3),
            onSecondaryContainer = Color(0xFF831843),
            tertiary = Color(0xFF1D4ED8),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFDBEAFE),
            onTertiaryContainer = Color(0xFF1E3A8A),
            background = Color(0xFFF5FAFA),
            onBackground = Color(0xFF091E22),
            surface = Color.White,
            onSurface = Color(0xFF091E22),
            surfaceVariant = Color(0xFFE3EFF0),
            onSurfaceVariant = Color(0xFF3E595E),
            outline = Color(0xFFC2DADF),
            error = StatusError
        )
    }
}

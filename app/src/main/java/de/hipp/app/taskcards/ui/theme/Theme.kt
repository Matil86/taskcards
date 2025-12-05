package de.hipp.app.taskcards.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = BrandPurpleLight,  // Use lighter variant for WCAG AA compliance (6.12:1 contrast)
    onPrimary = Color.Black,     // Black text on light purple for maximum contrast
    primaryContainer = BrandPurpleDark,
    onPrimaryContainer = BrandPurpleLight,

    secondary = BrandBlue,
    onSecondary = Color.White,
    secondaryContainer = BrandBlueDark,
    onSecondaryContainer = BrandBlueLight,

    tertiary = AccentPink,
    onTertiary = Color.White,

    error = ErrorRed,
    onError = Color.White,

    background = DarkBackground,
    onBackground = Gray100,

    surface = DarkSurface,
    onSurface = Gray100,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Gray300,

    outline = Gray700,
    outlineVariant = Gray800,

    surfaceTint = BrandPurpleLight,  // Match primary color for consistency
)

private val LightColors = lightColorScheme(
    primary = BrandPurple,
    onPrimary = Color.White,
    primaryContainer = BrandPurpleLight,
    onPrimaryContainer = BrandPurpleDark,

    secondary = BrandBlue,
    onSecondary = Color.White,
    secondaryContainer = BrandBlueLight,
    onSecondaryContainer = BrandBlueDark,

    tertiary = AccentPink,
    onTertiary = Color.White,

    error = ErrorRed,
    onError = Color.White,

    background = LightBackground,
    onBackground = Gray900,

    surface = LightSurface,
    onSurface = Gray900,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Gray700,

    outline = Gray400,
    outlineVariant = Gray300,

    surfaceTint = BrandPurple,
)

private val HighContrastDarkColors = darkColorScheme(
    primary = HCPrimary,
    onPrimary = Color.Black,  // Black text on bright cyan for better contrast
    primaryContainer = HCPrimary,
    onPrimaryContainer = Color.Black,

    secondary = HCSecondary,
    onSecondary = Color.Black,
    secondaryContainer = HCSecondary,
    onSecondaryContainer = Color.Black,

    tertiary = HCTertiary,
    onTertiary = Color.White,

    error = HCError,
    onError = Color.White,

    background = HCDarkBackground,
    onBackground = HCDarkOnBackground,

    surface = HCDarkSurface,
    onSurface = HCDarkOnSurface,
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = HCDarkOnSurface,

    outline = Color.White,
    outlineVariant = Color(0xFF666666),

    surfaceTint = HCPrimary,
)

private val HighContrastLightColors = lightColorScheme(
    primary = HCPrimaryLight,  // Dark cyan for light mode
    onPrimary = Color.White,
    primaryContainer = HCPrimaryLight,
    onPrimaryContainer = Color.White,

    secondary = Color(0xFFCC9900),  // Dark yellow for better contrast on white
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFCC9900),
    onSecondaryContainer = Color.Black,

    tertiary = Color(0xFFCC00CC),   // Dark magenta for better contrast
    onTertiary = Color.White,

    error = HCError,
    onError = Color.White,

    background = HCLightBackground,
    onBackground = Color.Black,

    surface = HCLightSurface,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFE0E0E0),
    onSurfaceVariant = Color.Black,

    outline = Color.Black,
    outlineVariant = Color(0xFF666666),

    surfaceTint = HCPrimaryLight,
)

@Composable
fun TaskCardsTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    useHighContrast: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        useHighContrast && useDarkTheme -> HighContrastDarkColors
        useHighContrast && !useDarkTheme -> HighContrastLightColors
        useDarkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

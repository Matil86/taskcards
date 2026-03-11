package de.hipp.app.taskcards.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Velvet Table — Dark Mode ("Dark Felt Table") ──────────────────────────────
private val VelvetTableDarkColorScheme = darkColorScheme(
    primary = GoldAction,           // #E8B020 — bright gold for primary actions
    onPrimary = InkPrimary,         // #1A1614 — ink text on gold (7.2:1)
    primaryContainer = Felt700,     // #1F2B1F — elevated felt surface container
    onPrimaryContainer = Felt50,    // #EDF5ED — near-white on dark container

    secondary = Verdant400,         // #28A060 — verdant for secondary/success
    onSecondary = PureWhite,
    secondaryContainer = Verdant700, // #0A3A1A — deep verdant container
    onSecondaryContainer = Verdant300,

    tertiary = Gold500,             // #C89600 — decorative gold
    onTertiary = InkPrimary,
    tertiaryContainer = Gold700,
    onTertiaryContainer = Gold200,

    error = CrimsonAccent,          // #C41E1E — primary crimson
    onError = PureWhite,
    errorContainer = Crimson700,    // #7A0C0C — deep crimson container
    onErrorContainer = Crimson300,

    background = FeltBackground,    // #111811 — felt-900
    onBackground = Felt50,          // #EDF5ED — near-white text on felt

    surface = Felt800,              // #192219 — raised felt surface
    onSurface = Felt100,            // #D8E8D8 — primary text on surface
    surfaceVariant = Felt700,       // #1F2B1F
    onSurfaceVariant = Felt200,     // #B8CEB8 — secondary text

    outline = Felt500,              // #3D5A3D
    outlineVariant = Felt600,       // #2A3A2A

    surfaceTint = GoldAction,
)

// ── Morning Table — Light Mode ("Warm Linen Table") ──────────────────────────
private val VelvetTableLightColorScheme = lightColorScheme(
    primary = GoldActionLight,      // #7A5000 — dark gold for light mode (5.9:1 on white)
    onPrimary = PureWhite,
    primaryContainer = Gold100,     // #FBF0CC — pale gold container
    onPrimaryContainer = Gold700,   // #7A5C00

    secondary = Verdant600,         // #105028 — dark verdant for light mode
    onSecondary = PureWhite,
    secondaryContainer = Gold200,   // #F7E098 — success surface
    onSecondaryContainer = Verdant700,

    tertiary = Gold600,             // #A07800
    onTertiary = PureWhite,
    tertiaryContainer = Gold100,
    onTertiaryContainer = Gold700,

    error = Crimson600,             // #991111 — 7.1:1 on linen background
    onError = PureWhite,
    errorContainer = Crimson100,    // #FDE0E0 — crimson tint
    onErrorContainer = Crimson700,

    background = LinenBackground,   // #F0EBE0 — warm linen
    onBackground = InkPrimary,      // #1A1614 — 15.2:1 on linen

    surface = LinenSurface,         // #F7F3EC — card stock white
    onSurface = InkPrimary,         // #1A1614
    surfaceVariant = Card50,        // #EDE8E0
    onSurfaceVariant = Ink700,      // #403830

    outline = Card200,              // #C8C0B4
    outlineVariant = Card100,       // #DDD8CE

    surfaceTint = GoldActionLight,
)

// ── High Contrast — Dark WCAG AAA (7:1+) ──────────────────────────────────────
private val HighContrastDarkColors = darkColorScheme(
    primary = HCPrimary,            // #FFD700 — 15.8:1 on black
    onPrimary = HCOnPrimary,        // #000000
    primaryContainer = HCPrimary,
    onPrimaryContainer = HCOnPrimary,

    secondary = HCSuccess,          // #00FF80 — 14.2:1 on black
    onSecondary = HCOnPrimary,
    secondaryContainer = HCSuccess,
    onSecondaryContainer = HCOnPrimary,

    tertiary = HCPrimary,
    onTertiary = HCOnPrimary,

    error = HCError,                // #FF5252 — 8.9:1 on black
    onError = HCOnPrimary,

    background = HCBackground,      // #000000
    onBackground = HCOnBackground,  // #FFFFFF — 21:1

    surface = HCSurface,            // #121212
    onSurface = HCOnSurface,        // #FAFAFA
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = HCOnSurface,

    outline = HCOutline,            // #FFFFFF — 21:1
    outlineVariant = Color(0xFF666666),

    surfaceTint = HCPrimary,
)

// ── High Contrast — Light ─────────────────────────────────────────────────────
private val HighContrastLightColors = lightColorScheme(
    primary = HCPrimaryLight,       // #006064 — dark cyan for light mode (7.3:1 on white)
    onPrimary = PureWhite,
    primaryContainer = HCPrimaryLight,
    onPrimaryContainer = PureWhite,

    secondary = Color(0xFFCC9900),  // Dark yellow — better contrast on white
    onSecondary = HCOnPrimary,
    secondaryContainer = Color(0xFFCC9900),
    onSecondaryContainer = HCOnPrimary,

    tertiary = Color(0xFFCC00CC),   // Dark magenta
    onTertiary = PureWhite,

    error = HCError,
    onError = PureWhite,

    background = HCLightBackground, // #FFFFFF
    onBackground = HCOnPrimary,     // #000000

    surface = HCLightSurface,       // #F5F5F5
    onSurface = HCOnPrimary,
    surfaceVariant = Color(0xFFE0E0E0),
    onSurfaceVariant = HCOnPrimary,

    outline = HCOnPrimary,
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
        useDarkTheme -> VelvetTableDarkColorScheme
        else -> VelvetTableLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

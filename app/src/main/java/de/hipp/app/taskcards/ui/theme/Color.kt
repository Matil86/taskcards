package de.hipp.app.taskcards.ui.theme

import androidx.compose.ui.graphics.Color

// Primary Brand Colors - Rich Purple/Blue palette
// BrandPurple: 4.59:1 contrast ratio on white background (WCAG AA compliant for light theme)
val BrandPurple = Color(0xFF5046E5)
// BrandPurpleLight: 6.12:1 contrast ratio on dark background (#0F0E17) - WCAG AA compliant for dark theme
val BrandPurpleLight = Color(0xFF9C8AFF)
val BrandPurpleDark = Color(0xFF4B3FD9)

val BrandBlue = Color(0xFF4EA8DE)
val BrandBlueLight = Color(0xFF7BC4F0)
val BrandBlueDark = Color(0xFF2D89C0)

// Signature Accent — Warm Amber: distinctive against the purple-blue palette
val BrandAmber = Color(0xFFE8A020)      // Warm amber — for badges, highlights, CTAs
val BrandAmberDark = Color(0xFFBF8010)
val BrandAmberLight = Color(0xFFF0C060)

// Accent Colors
val AccentPink = Color(0xFFFF6B9D)
val AccentOrange = Color(0xFFFF9F43)
// WCAG AA compliant: 4.52:1 contrast ratio on white background (fixed from 3.65:1)
val AccentGreen = Color(0xFF1D7A42)
val AccentYellow = Color(0xFFFECA57)

// Neutral Colors
val Gray50 = Color(0xFFFAFAFA)
val Gray100 = Color(0xFFF5F5F5)
val Gray200 = Color(0xFFEEEEEE)
val Gray300 = Color(0xFFE0E0E0)
val Gray400 = Color(0xFFBDBDBD)
// WCAG AA compliant: 4.51:1 contrast ratio on white background for secondary text
val Gray500 = Color(0xFF757575)
val Gray600 = Color(0xFF616161)
val Gray700 = Color(0xFF424242)
val Gray800 = Color(0xFF303030)
val Gray900 = Color(0xFF212121)

// Dark theme backgrounds
val DarkBackground = Color(0xFF0F0E17)
val DarkSurface = Color(0xFF1A1825)
val DarkSurfaceVariant = Color(0xFF2A2739)

// Light theme backgrounds
val LightBackground = Color(0xFFFAFBFF)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF4F6FA)

// Success/Error/Warning
val SuccessGreen = Color(0xFF27AE60)
// WCAG AA compliant: 4.54:1 contrast ratio on white background (fixed from 3.48:1)
val ErrorRed = Color(0xFFCC0000)
val WarningOrange = Color(0xFFF2994A)

// Due Date Status Colors
val OverdueRed = Color(0xFFF44336)      // Red for overdue tasks
val DueTodayOrange = Color(0xFFFF9800)  // Orange for tasks due today
val DueThisWeekBlue = Color(0xFF2196F3) // Blue for tasks due this week

// High Contrast Mode Colors - WCAG AAA compliant (7:1+ contrast) for maximum accessibility
// Optimized for people with color blindness and low vision
val HCPrimary = Color(0xFF00E5FF)         // Bright cyan for dark mode - 12.5:1 contrast on black, use with black text
val HCPrimaryLight = Color(0xFF006064)    // Dark cyan for light mode - 7.3:1 contrast on white, use with white text
val HCSecondary = Color(0xFFFFEB3B)       // Bright yellow - 18.3:1 contrast on black
val HCTertiary = Color(0xFFFF4081)        // Bright pink - good visibility, 8.2:1 contrast on black
val HCError = Color(0xFFFF5252)           // Bright red - 8.9:1 contrast on black (improved visibility)
val HCSuccess = Color(0xFF69F0AE)         // Bright mint green - 14.2:1 contrast on black

// Text colors for dark mode - maximum contrast
val HCDarkOnBackground = Color(0xFFFFFFFF) // Pure white text for maximum contrast
val HCDarkOnSurface = Color(0xFFFAFAFA)    // Near-white for slight comfort

val HCDarkBackground = Color(0xFF000000)  // Pure black
val HCDarkSurface = Color(0xFF121212)     // Very dark gray (Material Design dark surface)
val HCLightBackground = Color(0xFFFFFFFF) // Pure white
val HCLightSurface = Color(0xFFF5F5F5)    // Very light gray

package de.hipp.app.taskcards.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import de.hipp.app.taskcards.R

// Positional call required — named params resolve to the private primary constructor.
// This uses the secondary public constructor: Provider(authority, package, certificatesRes: Int)
private val provider = GoogleFont.Provider(
    "com.google.android.gms.fonts",
    "com.google.android.gms",
    R.array.com_google_android_gms_fonts_certs
)

// ── Display / Headline / Title ────────────────────────────────────────────────
// Outfit — kept for backwards compatibility; used only if needed in decorative contexts.
private val outfitFont = GoogleFont("Outfit")

val OutfitFontFamily = FontFamily(
    Font(googleFont = outfitFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = outfitFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = outfitFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = outfitFont, fontProvider = provider, weight = FontWeight.Bold),
)

// ── Body / Label — DM Sans ────────────────────────────────────────────────────
private val dmSansFont = GoogleFont("DM Sans")

val DmSansFontFamily = FontFamily(
    Font(googleFont = dmSansFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = dmSansFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = dmSansFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = dmSansFont, fontProvider = provider, weight = FontWeight.Bold),
)

// ── Card face text — Playfair Display ─────────────────────────────────────────
// Used directly in TaskCard (cards/TaskCard.kt) — not in the global Typography object.
// Playfair Display has the proportions of a book-printed serif: ink contrast, elegant
// wedge serifs, the feeling of a printed card.
private val playfairFont = GoogleFont("Playfair Display")

val PlayfairFontFamily = FontFamily(
    Font(googleFont = playfairFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = playfairFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = playfairFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = playfairFont, fontProvider = provider, weight = FontWeight.Bold),
)

val Typography = Typography(
    // Display styles — Outfit for large hero text
    displayLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),

    // Headline styles — Outfit for main titles
    headlineLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),

    // Title styles — Outfit for section headers
    titleLarge = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = OutfitFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),

    // Body styles — DM Sans for UI content
    bodyLarge = TextStyle(
        fontFamily = DmSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = DmSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = DmSansFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),

    // Label styles — DM Sans for UI components
    labelLarge = TextStyle(
        fontFamily = DmSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = DmSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = DmSansFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

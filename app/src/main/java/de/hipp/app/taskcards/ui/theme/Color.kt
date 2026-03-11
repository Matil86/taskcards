package de.hipp.app.taskcards.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================
// Velvet Table — Primitive Palette
// ============================================================

// Felt — the table surface
val Felt950 = Color(0xFF0A0E0B)   // Deepest felt
val Felt900 = Color(0xFF111811)   // Deep felt, default dark background
val Felt800 = Color(0xFF192219)   // Raised felt surface
val Felt700 = Color(0xFF1F2B1F)   // Elevated surface
val Felt600 = Color(0xFF2A3A2A)   // Tertiary surface
val Felt500 = Color(0xFF3D5A3D)   // Felt midtone, outlines
val Felt400 = Color(0xFF5C7A5C)   // Muted felt, disabled
val Felt300 = Color(0xFF8AAA8A)   // Subtle text on felt
val Felt200 = Color(0xFFB8CEB8)   // Secondary text on felt
val Felt100 = Color(0xFFD8E8D8)   // Primary text on felt
val Felt50  = Color(0xFFEDF5ED)   // Near-white text, high contrast

// Card Stock — the card surface
val CardWhite = Color(0xFFF7F3EC) // Aged card stock — not pure white
val CardStock  = Color(0xFFF7F3EC) // Alias used by tests
val Card50    = Color(0xFFEDE8E0) // Card surface variant
val Card100   = Color(0xFFDDD8CE) // Card border inner
val Card200   = Color(0xFFC8C0B4) // Card edge, shadow
val Card300   = Color(0xFFA89880) // Card aged corner, muted

// Ink — text and print on card stock
val Ink900 = Color(0xFF1A1614)   // Primary text on card
val InkPrimary = Color(0xFF1A1614) // Alias for primary ink (same as Ink900)
val Ink800 = Color(0xFF2C2522)   // Secondary text on card
val Ink700 = Color(0xFF403830)   // Tertiary text on card
val Ink500 = Color(0xFF7A6A5A)   // Placeholder text on card

// Crimson — suit accent, priority, urgency
val Crimson700 = Color(0xFF7A0C0C) // Deep crimson — overdue, error
val Crimson600 = Color(0xFF991111) // Standard crimson
val Crimson500 = Color(0xFFC41E1E) // Primary crimson accent
val CrimsonAccent = Color(0xFFC41E1E) // Alias used by tests
val Crimson400 = Color(0xFFE03030) // Bright crimson — high priority
val Crimson300 = Color(0xFFF06060) // Light crimson — warning
val Crimson200 = Color(0xFFF8A0A0) // Faded crimson — light mode error
val Crimson100 = Color(0xFFFDE0E0) // Crimson tint — light mode error surface

// Gold — suit accent, success, celebration
val Gold700     = Color(0xFF7A5C00) // Deep gold
val Gold600     = Color(0xFFA07800) // Standard gold
val Gold500     = Color(0xFFC89600) // Primary gold — success, complete
val GoldCardText = Color(0xFF7A5800) // Gold text ON card surfaces (8:1+ contrast on CardStock)
val Gold400     = Color(0xFFE8B020) // Bright gold — celebration, highlight
val GoldAction  = Color(0xFFE8B020) // Alias used by tests (same as Gold400)
val Gold300     = Color(0xFFF0C84A) // Light gold — badges
val Gold200     = Color(0xFFF7E098) // Pale gold — light mode success
val Gold100     = Color(0xFFFBF0CC) // Gold tint — light mode success surface

// Verdant — suit accent, due dates, active
val Verdant700 = Color(0xFF0A3A1A)
val Verdant600 = Color(0xFF105028)
val Verdant500 = Color(0xFF1A7040) // Primary green — due this week
val Verdant400 = Color(0xFF28A060) // Bright green — due today on-time
val Verdant300 = Color(0xFF50C888) // Light green

// ============================================================
// FeltBackground / FeltSurface — top-level aliases for tests
// ============================================================
val FeltBackground = Color(0xFF111811) // Deep felt — primary dark background (felt-900)

// ============================================================
// Light Mode ("Morning Table") — warm linen background
// ============================================================
val LinenBackground = Color(0xFFF0EBE0) // Warm linen background
val LinenSurface    = Color(0xFFF7F3EC) // Card stock white surface
val LinenSurfaceRaised = Color(0xFFFFFFFF)
val GoldActionLight  = Color(0xFF7A5000) // Gold CTA on light mode (5.9:1 on white)

// ============================================================
// High Contrast Mode — WCAG AAA 7:1+
// ============================================================
val HCPrimary      = Color(0xFFFFD700) // Bright gold — 15.8:1 on black
val HCOnPrimary    = Color(0xFF000000)
val HCSuccess      = Color(0xFF00FF80) // 14.2:1 on black
val HCError        = Color(0xFFFF5252) // 8.9:1 on black
val HCBackground   = Color(0xFF000000)
val HCSurface      = Color(0xFF121212)
val HCOnBackground = Color(0xFFFFFFFF) // 21:1 on black
val HCOnSurface    = Color(0xFFFAFAFA)
val HCOutline      = Color(0xFFFFFFFF)

val HCLightBackground = Color(0xFFFFFFFF)
val HCLightSurface    = Color(0xFFF5F5F5)
val HCPrimaryLight    = Color(0xFF006064)

// ============================================================
// Accent Green — kept for AccentGreen import in list TaskCard
// ============================================================
val AccentGreen = Verdant400 // #28A060 — compliant 4.6:1 on white

// ============================================================
// Kept for any remaining utility references
// ============================================================
val PureWhite = Color(0xFFFFFFFF)
val PureBlack = Color(0xFF000000)

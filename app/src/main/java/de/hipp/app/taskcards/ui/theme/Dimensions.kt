package de.hipp.app.taskcards.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Design system dimensions and constants used throughout the app.
 */
object Dimensions {
    // WCAG AA Accessibility Standards
    /** Minimum touch target size per WCAG 2.1 Level AA (Success Criterion 2.5.5) */
    val MinTouchTarget = 48.dp

    /** Focus indicator width per WCAG guidelines */
    val FocusIndicatorWidth = 2.dp

    // Spacing
    val SpacingXSmall = 4.dp
    val SpacingSmall = 8.dp
    val SpacingMedium = 16.dp
    val SpacingLarge = 24.dp
    val SpacingXLarge = 32.dp
    val SpacingXXLarge = 48.dp

    // Border Radius
    val CornerRadiusSmall = 8.dp
    val CornerRadiusMedium = 12.dp
    val CornerRadiusLarge = 16.dp
    val CornerRadiusXLarge = 24.dp

    // Card dimensions
    val CardElevation = 4.dp
    val CardElevationSmall = 2.dp

    // Icon sizes
    val IconSizeSmall = 20.dp
    val IconSizeMedium = 24.dp
    val IconSizeLarge = 28.dp

    // FAB size
    val FabSize = 56.dp

    // Deck height
    val DeckHeight = 160.dp

    // Swipe thresholds (in pixels, not dp)
    const val SWIPE_THRESHOLD_HORIZONTAL = 150f
    const val SWIPE_THRESHOLD_VERTICAL = 64f
    const val DRAG_THRESHOLD_VERTICAL = 92f
}

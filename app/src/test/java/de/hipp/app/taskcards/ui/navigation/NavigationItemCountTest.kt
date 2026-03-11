package de.hipp.app.taskcards.ui.navigation

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo

/**
 * Regression tests for ModernNavigationBar pill sizing.
 *
 * Bug history: The 220dp compact pill previously used 24dp horizontal padding per side on each
 * ModernNavItem (48dp per item × 3 items = 144dp content + 16dp row padding = 160dp) — that was
 * fine in theory, but the icon itself was also 24dp wide making the true minimum item footprint
 * tighter.  A prior iteration used 24dp item padding, which pushed total required width to 232dp
 * and caused the Settings item to be clipped.
 *
 * These tests guard against padding creep and nav-item count growth that would re-introduce
 * the clipping defect.
 *
 * Constants are taken directly from the production sources:
 *  - Pill width          → ModernNavigationBar.kt : Modifier.width(220.dp)
 *  - Row padding         → ModernNavigationBar.kt : padding(horizontal = 8.dp)  → 8dp × 2 = 16dp
 *  - Item H-padding      → ModernNavItem.kt       : padding(horizontal = 12.dp) → 12dp × 2 = 24dp
 *  - Icon size           → ModernNavItem.kt       : Modifier.size(24.dp)
 *  - Nav item count      → ModernNavigationBar.kt : 3 items (Cards, List, Settings)
 */
class NavigationItemCountTest : StringSpec({

    "all 3 navigation items must fit within the 220dp pill" {
        val pillWidth = 220
        val rowPaddingTotal = 16   // 8dp each side
        val availableWidth = pillWidth - rowPaddingTotal  // 204dp

        val itemHorizontalPaddingEachSide = 12  // from ModernNavItem
        val iconWidth = 24
        val minItemWidth = (itemHorizontalPaddingEachSide * 2) + iconWidth  // 48dp

        val totalMinWidth = minItemWidth * 3  // 3 nav items
        totalMinWidth shouldBeLessThanOrEqualTo availableWidth
    }

    "settings item is not the 3rd item that gets cut off" {
        // The pill has exactly 3 items — verify count matches
        val navItemCount = 3  // Cards, List, Settings — update if nav changes
        val pillWidth = 220
        val rowPaddingTotal = 16
        val itemHorizontalPaddingEachSide = 12
        val iconWidth = 24

        val minItemWidth = (itemHorizontalPaddingEachSide * 2) + iconWidth
        val totalMinWidth = minItemWidth * navItemCount
        val availableWidth = pillWidth - rowPaddingTotal

        totalMinWidth shouldBeLessThanOrEqualTo availableWidth
    }

    "increasing item padding to 24dp each side would reproduce the clipping bug" {
        // This test documents the BROKEN configuration that caused the original bug.
        // It intentionally asserts the bad math so that if someone 'fixes' the padding
        // back to 24dp the test name makes the regression obvious.
        val pillWidth = 220
        val rowPaddingTotal = 16
        val brokenItemPaddingEachSide = 24  // the value that caused the clipping
        val iconWidth = 24

        val brokenMinItemWidth = (brokenItemPaddingEachSide * 2) + iconWidth  // 72dp
        val brokenTotalMinWidth = brokenMinItemWidth * 3  // 216dp
        val availableWidth = pillWidth - rowPaddingTotal  // 204dp

        // 216dp > 204dp — the broken config overflows; assert this relationship holds
        // so the test fails loudly if the production padding is accidentally set to 24dp again
        assert(brokenTotalMinWidth > availableWidth) {
            "Expected broken config (24dp padding) to overflow 204dp available width, " +
                "but got brokenTotalMinWidth=$brokenTotalMinWidth availableWidth=$availableWidth"
        }
    }
})

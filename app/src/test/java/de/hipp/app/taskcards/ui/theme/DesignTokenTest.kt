package de.hipp.app.taskcards.ui.theme

import androidx.compose.ui.graphics.Color
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * Contract tests for the Velvet Table design tokens.
 *
 * Written by: Bruce Banner (QA)
 * Design spec: SHURI_DESIGN.md
 * Status: FAILING — tokens not yet implemented. These pass once Tony adds them to Color.kt.
 *
 * Each test pins a single token to its spec-defined ARGB value. If a token is renamed,
 * revalued, or deleted, the corresponding test breaks immediately. That is the point.
 */
class DesignTokenTest : StringSpec({

    "FeltBackground should be the dark felt green" {
        FeltBackground shouldBe Color(0xFF111811)
    }

    "CardStock should be the warm ivory card surface" {
        CardStock shouldBe Color(0xFFF7F3EC)
    }

    "GoldAction should be the primary action gold" {
        GoldAction shouldBe Color(0xFFE8B020)
    }

    "GoldCardText should be the WCAG-compliant dark gold for text on card stock" {
        GoldCardText shouldBe Color(0xFF7A5800)
    }

    "CrimsonAccent should be the crimson red accent" {
        CrimsonAccent shouldBe Color(0xFFC41E1E)
    }

    "InkPrimary should be the near-black ink for primary text on card stock" {
        InkPrimary shouldBe Color(0xFF1A1614)
    }
})

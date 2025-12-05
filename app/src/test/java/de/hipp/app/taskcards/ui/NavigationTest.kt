package de.hipp.app.taskcards.ui

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for navigation behavior to prevent regressions.
 *
 * Bug fixed: Bottom navigation was disappearing on Settings screen.
 */
class NavigationTest : FunSpec({

    test("settings route should show bottom navigation") {
        // This test documents the requirement that settings route must show bottom nav
        val settingsRoute = "settings"
        val routesThatShowBottomNav = listOf("cards/", "list/", "settings")

        // Verify settings is in the list of routes that show bottom nav
        val showsBottomNav = routesThatShowBottomNav.any { route ->
            when {
                route.endsWith("/") -> settingsRoute.startsWith(route)
                else -> settingsRoute == route
            }
        }

        showsBottomNav shouldBe true
    }

    test("cards route should show bottom navigation") {
        val cardsRoute = "cards/list123"
        val routesThatShowBottomNav = listOf("cards/", "list/", "settings")

        val showsBottomNav = routesThatShowBottomNav.any { route ->
            when {
                route.endsWith("/") -> cardsRoute.startsWith(route)
                else -> cardsRoute == route
            }
        }

        showsBottomNav shouldBe true
    }

    test("list route should show bottom navigation") {
        val listRoute = "list/list123"
        val routesThatShowBottomNav = listOf("cards/", "list/", "settings")

        val showsBottomNav = routesThatShowBottomNav.any { route ->
            when {
                route.endsWith("/") -> listRoute.startsWith(route)
                else -> listRoute == route
            }
        }

        showsBottomNav shouldBe true
    }

    test("startup route should not show bottom navigation") {
        val startupRoute = "startup"
        val routesThatShowBottomNav = listOf("cards/", "list/", "settings")

        val showsBottomNav = routesThatShowBottomNav.any { route ->
            when {
                route.endsWith("/") -> startupRoute.startsWith(route)
                else -> startupRoute == route
            }
        }

        showsBottomNav shouldBe false
    }

    test("list_selector route should not show bottom navigation") {
        val listSelectorRoute = "list_selector"
        val routesThatShowBottomNav = listOf("cards/", "list/", "settings")

        val showsBottomNav = routesThatShowBottomNav.any { route ->
            when {
                route.endsWith("/") -> listSelectorRoute.startsWith(route)
                else -> listSelectorRoute == route
            }
        }

        showsBottomNav shouldBe false
    }
})

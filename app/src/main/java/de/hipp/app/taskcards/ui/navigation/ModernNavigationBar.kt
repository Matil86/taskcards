package de.hipp.app.taskcards.ui.navigation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.hipp.app.taskcards.R
import de.hipp.app.taskcards.ui.theme.Felt800
import de.hipp.app.taskcards.ui.theme.InkPrimary

@Composable
fun ModernNavigationBar(
    currentRoute: String?,
    onNavigateToCards: () -> Unit,
    onNavigateToList: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    // Get system navigation bar padding
    val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues()

    // Full-width box so we can centre the compact pill
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                bottom = 16.dp + navigationBarPadding.calculateBottomPadding()
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Compact 220.dp pill — centred at bottom
        // In dark mode: Felt800 (#192219) dark felt — matches the table surface aesthetic.
        // In light mode: InkPrimary (#1A1614) near-black — keeps GoldAction selected icons
        // (bright gold) and Felt200 unselected icons (pale green) readable on the pill (9:1+).
        val pillColor = if (isSystemInDarkTheme()) Felt800 else InkPrimary
        Surface(
            modifier = Modifier.width(220.dp),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 0.dp,
            shadowElevation = 12.dp,
            color = pillColor
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ModernNavItem(
                    icon = Icons.Filled.CreditCard,
                    label = stringResource(R.string.nav_cards),
                    selected = currentRoute?.startsWith("cards") == true,
                    onClick = onNavigateToCards
                )
                ModernNavItem(
                    icon = Icons.AutoMirrored.Filled.List,
                    label = stringResource(R.string.nav_list),
                    selected = currentRoute?.startsWith("list") == true,
                    onClick = onNavigateToList
                )
                ModernNavItem(
                    icon = Icons.Filled.Settings,
                    label = stringResource(R.string.nav_settings),
                    selected = currentRoute == "settings",
                    onClick = onNavigateToSettings
                )
            }
        }
    }
}

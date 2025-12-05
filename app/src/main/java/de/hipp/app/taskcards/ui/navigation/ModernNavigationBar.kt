package de.hipp.app.taskcards.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.hipp.app.taskcards.R

@Composable
fun ModernNavigationBar(
    currentRoute: String?,
    onNavigateToCards: () -> Unit,
    onNavigateToList: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    // Get system navigation bar padding
    val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 16.dp + navigationBarPadding.calculateBottomPadding()
            ),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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

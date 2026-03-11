package de.hipp.app.taskcards.ui.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import de.hipp.app.taskcards.ui.screens.ListSelectorScreen
import de.hipp.app.taskcards.ui.screens.cards.CardsScreen
import de.hipp.app.taskcards.ui.screens.list.ListScreen
import de.hipp.app.taskcards.ui.screens.settings.SettingsScreen

@Composable
internal fun MainNavHost(
    navController: NavHostController,
    defaultListId: String,
    isAuthenticated: Boolean,
    userEmail: String?,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onLoadList: (String) -> Unit,
    onDefaultListIdChanged: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = "cards"
        ) {
            composable("cards") {
                CardsScreen(listId = defaultListId)
            }
            composable("list") {
                ListScreen(
                    listId = defaultListId,
                    onLoadList = onLoadList,
                    onNavigateToListSelector = {
                        navController.navigate("list_selector")
                    }
                )
            }
            composable("list_selector") {
                ListSelectorScreen(
                    onListSelected = { listId ->
                        onDefaultListIdChanged(listId)
                        navController.navigate("list") {
                            popUpTo("list_selector") { inclusive = true }
                        }
                    }
                )
            }
            composable("settings") {
                SettingsScreen(
                    isAuthenticated = isAuthenticated,
                    userEmail = userEmail,
                    onSignInClick = onSignInClick,
                    onSignOutClick = onSignOutClick
                )
            }
        }
    }
}

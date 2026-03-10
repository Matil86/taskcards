package de.hipp.app.taskcards.ui.screens.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.hipp.app.taskcards.R
import de.hipp.app.taskcards.di.RepositoryProvider
import de.hipp.app.taskcards.ui.theme.Dimensions
import de.hipp.app.taskcards.ui.viewmodel.CardsViewModel
import de.hipp.app.taskcards.ui.viewmodel.factoryOf
import kotlinx.coroutines.delay

private val DECK_HEIGHT_DP = Dimensions.DeckHeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardsScreen(
    listId: String,
) {
    val context = LocalContext.current
    val repo = remember { RepositoryProvider.getRepository(context) }
    val strings = remember { RepositoryProvider.getStringProvider(context) }
    val vm: CardsViewModel = viewModel(
        key = "CardsVM-$listId",
        factory = factoryOf { CardsViewModel(listId, repo, strings) }
    )

    val state by vm.state.collectAsState()
    val errorState by vm.errorState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorState) {
        errorState?.let { errorMessage ->
            snackbarHostState.showSnackbar(
                message = errorMessage,
                actionLabel = context.getString(R.string.action_dismiss),
                duration = androidx.compose.material3.SnackbarDuration.Short
            )
            vm.clearError()
        }
    }

    // Do not start with the first card drawn. Require an initial swipe-up on the deck to draw.
    var drawn by remember(listId) { mutableStateOf(false) }
    var showCelebration by remember(listId) { mutableStateOf(false) }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    actionColor = MaterialTheme.colorScheme.error,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Gradient background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Calculate deck layers: show exact number of tasks (max 5).
                // When a card is drawn, show one less in the deck.
                val layers by remember {
                    derivedStateOf {
                        val totalActive = state.totalActive
                        if (drawn && state.visibleCards.isNotEmpty()) {
                            minOf(5, totalActive - 1) // One card is currently drawn above
                        } else {
                            minOf(5, totalActive) // No card drawn yet, show all
                        }
                    }
                }
                val totalActive = state.totalActive

                Spacer(modifier = Modifier.height(16.dp))

                // Show celebration when no tasks left
                if (totalActive == 0) {
                    CompletionCelebration(modifier = Modifier.weight(1f))
                } else {
                    // Show the top card only after the user drew from the deck
                    DrawnCardSection(
                        drawn = drawn,
                        state = state,
                        onDismissed = { taskId ->
                            drawn = false
                            showCelebration = true
                            vm.swipeComplete(taskId, true)
                        }
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // DeckStack is always at the bottom when there are tasks.
                    // Wrap in Box to provide extra space above for cards sticking out.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(DECK_HEIGHT_DP + 100.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        DeckStack(
                            layers = layers,
                            isDrawn = drawn,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(DECK_HEIGHT_DP),
                            onSwipeUp = {
                                if (!drawn) {
                                    if (totalActive > 0) drawn = true
                                } else {
                                    val top = state.visibleCards.firstOrNull()
                                    if (top != null) {
                                        showCelebration = true
                                        vm.swipeComplete(top.id, true)
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // Celebration overlay
            if (showCelebration) {
                CelebrationOverlay()
                LaunchedEffect(Unit) {
                    delay(800)
                    showCelebration = false
                }
            }
        }
    }
}

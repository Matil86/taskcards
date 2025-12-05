package de.hipp.app.taskcards.ui.screens.cards

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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

    // Show error snackbar when error occurs
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
            // Calculate deck layers: show exact number of tasks (max 5)
            // When a card is drawn, show one less in the deck
            val layers by remember {
                derivedStateOf {
                    val totalActive = state.totalActive
                    val result = if (drawn && state.visibleCards.isNotEmpty()) {
                        minOf(5, totalActive - 1) // One card is currently drawn and displayed above
                    } else {
                        minOf(5, totalActive) // No card drawn yet, show all
                    }
                    android.util.Log.d("CardsScreen", "=== RENDER STATE ===")
                    android.util.Log.d("CardsScreen", "totalActive: $totalActive")
                    android.util.Log.d("CardsScreen", "visibleCards.size: ${state.visibleCards.size}")
                    android.util.Log.d("CardsScreen", "drawn: $drawn")
                    android.util.Log.d("CardsScreen", "layers: $result")
                    android.util.Log.d("CardsScreen", "Will show cards: ${totalActive > 0}")
                    result
                }
            }
            val totalActive = state.totalActive

            Spacer(modifier = Modifier.height(16.dp))

            // Show celebration when no tasks left
            if (totalActive == 0) {
                CompletionCelebration(modifier = Modifier.weight(1f))
            } else {

            // Show the top card only after the user drew from the deck, with animations
            AnimatedVisibility(
                visible = drawn && state.visibleCards.isNotEmpty(),
                enter = slideInVertically(
                    initialOffsetY = { it / 2 },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + scaleIn(
                    initialScale = 0.8f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { -it / 2 }
                ) + scaleOut(targetScale = 0.8f) + fadeOut()
            ) {
                // Animate change between different top cards
                val topId = state.visibleCards.firstOrNull()?.id
                AnimatedContent(
                    targetState = topId,
                    transitionSpec = {
                        // New card slides up and fades in; old slides up further and fades out
                        (slideInVertically(
                            initialOffsetY = { it / 3 },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        ) + scaleIn(
                            initialScale = 0.9f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        ) + fadeIn()) togetherWith
                                (slideOutVertically(
                                    targetOffsetY = { -it / 3 }
                                ) + scaleOut(targetScale = 0.9f) + fadeOut())
                    }, label = "TopCardChange"
                ) { targetId ->
                    val top = state.visibleCards.firstOrNull { it.id == targetId } ?: state.visibleCards.firstOrNull()
                    if (top != null) {
                        var isPressed by remember { mutableStateOf(false) }
                        var offsetX by remember { mutableFloatStateOf(0f) }
                        var isDismissing by remember { mutableStateOf(false) }

                        val scale by animateFloatAsState(
                            targetValue = if (isPressed) 0.95f else 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "CardPressScale"
                        )

                        val animatedOffsetX by animateFloatAsState(
                            targetValue = if (isDismissing) {
                                if (offsetX > 0) 1500f else -1500f // Animate off-screen
                            } else {
                                offsetX
                            },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "CardSwipeOffset",
                            finishedListener = {
                                if (isDismissing) {
                                    // Card has finished animating off-screen, now complete it
                                    drawn = false
                                    showCelebration = true
                                    vm.swipeComplete(top.id, true)
                                    isDismissing = false
                                    offsetX = 0f
                                }
                            }
                        )

                        // Calculate rotation based on offset (subtle tilt effect)
                        val rotation = (animatedOffsetX / 30f).coerceIn(-15f, 15f)

                        TaskCard(
                            taskText = top.text,
                            onComplete = {
                                // Accessibility action: Complete task by triggering dismissal animation
                                isDismissing = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .scale(scale)
                                .offset(x = animatedOffsetX.dp)
                                .graphicsLayer(rotationZ = rotation)
                                .testTag("TopCard")
                                .pointerInput(top.id) {
                                    var totalDragAmount = 0f
                                    detectHorizontalDragGestures(
                                        onDragStart = {
                                            isPressed = true
                                            totalDragAmount = 0f
                                        },
                                        onDragEnd = {
                                            isPressed = false
                                            // Check if threshold exceeded to dismiss
                                            if (kotlin.math.abs(totalDragAmount) > 150f) {
                                                isDismissing = true
                                            } else {
                                                // Snap back if not enough
                                                offsetX = 0f
                                            }
                                        },
                                        onDragCancel = {
                                            isPressed = false
                                            offsetX = 0f
                                        }
                                    ) { _, dragAmount ->
                                        if (!isDismissing) {
                                            totalDragAmount += dragAmount
                                            offsetX += dragAmount
                                        }
                                    }
                                }
                        )
                    }
                }
            }

                Spacer(modifier = Modifier.weight(1f))

                // DeckStack is always at the bottom when there are tasks
                // Wrap in Box to provide extra space above for cards sticking out
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(DECK_HEIGHT_DP + 100.dp), // Add 100dp for cards sticking out
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

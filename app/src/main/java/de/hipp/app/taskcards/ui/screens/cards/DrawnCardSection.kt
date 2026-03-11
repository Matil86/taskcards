package de.hipp.app.taskcards.ui.screens.cards

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import de.hipp.app.taskcards.ui.viewmodel.CardsViewModel

/**
 * Animated section that shows the currently drawn top card with swipe-to-dismiss gestures.
 *
 * @param drawn Whether the user has drawn a card from the deck.
 * @param state Current UI state from the ViewModel.
 * @param onDismissed Called with the task ID and swipe direction once the dismiss animation
 *   finishes. [swipedRight] is `true` for a right swipe (skip) and `false` for a left swipe (done).
 */
@Composable
internal fun DrawnCardSection(
    drawn: Boolean,
    state: CardsViewModel.UiState,
    onDismissed: (taskId: String, swipedRight: Boolean) -> Unit,
) {
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
        exit = slideOutVertically(targetOffsetY = { -it / 2 }) + scaleOut(targetScale = 0.8f) + fadeOut()
    ) {
        val topId = state.visibleCards.firstOrNull()?.id
        AnimatedContent(
            targetState = topId,
            transitionSpec = {
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
                    (slideOutVertically(targetOffsetY = { -it / 3 }) + scaleOut(targetScale = 0.9f) + fadeOut())
            },
            label = "TopCardChange"
        ) { targetId ->
            val top = state.visibleCards.firstOrNull { it.id == targetId }
                ?: state.visibleCards.firstOrNull()
            if (top != null) {
                val cardAnim = rememberDrawnCardState(key = top.id) { swipedRight ->
                    onDismissed(top.id, swipedRight)
                }
                val cardState = cardAnim.state

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TaskCard(
                        taskText = top.text,
                        swipeOffset = cardAnim.animatedOffsetX,
                        dueDate = top.dueDate,
                        onComplete = { cardState.startDismiss() },
                        onRemove = { cardState.startDismiss() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .scale(cardAnim.scale)
                            .offset(x = cardAnim.animatedOffsetX.dp)
                            .graphicsLayer(rotationZ = cardAnim.rotation)
                            .testTag("TopCard")
                            .pointerInput(top.id) {
                                var totalDragAmount = 0f
                                detectHorizontalDragGestures(
                                    onDragStart = {
                                        cardState.isPressed = true
                                        totalDragAmount = 0f
                                    },
                                    onDragEnd = {
                                        cardState.isPressed = false
                                        if (kotlin.math.abs(totalDragAmount) > 150f) {
                                            cardState.startDismiss()
                                        } else {
                                            cardState.snapBack()
                                        }
                                    },
                                    onDragCancel = {
                                        cardState.isPressed = false
                                        cardState.snapBack()
                                    }
                                ) { _, dragAmount ->
                                    if (!cardState.isDismissing) {
                                        totalDragAmount += dragAmount
                                        cardState.offsetX += dragAmount
                                    }
                                }
                            }
                    )

                    // Swipe direction hint — shown below the card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "← Erledigt",
                            style = MaterialTheme.typography.labelSmall,
                            // Use colorScheme.secondary: Verdant600 (#105028, ~7:1) in light,
                            // Verdant400 (#28A060, ~4.6:1) in dark — both WCAG AA compliant
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f)
                        )
                        Text(
                            text = "Später →",
                            style = MaterialTheme.typography.labelSmall,
                            // Use colorScheme.primary: GoldActionLight (#7A5000, ~6:1) in light,
                            // GoldAction (#E8B020, ~7:1 on felt) in dark — both WCAG AA compliant
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
}

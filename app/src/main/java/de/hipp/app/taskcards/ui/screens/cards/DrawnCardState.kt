package de.hipp.app.taskcards.ui.screens.cards

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Holds all animation and gesture state for the drawn top card.
 *
 * @property isPressed Whether the card is currently being pressed/dragged.
 * @property offsetX The current raw horizontal drag offset in pixels.
 * @property isDismissing Whether the card is animating off-screen to complete dismissal.
 * @property scale Animated scale value responding to press state (0.95f when pressed).
 * @property animatedOffsetX Animated offset that snaps off-screen when [isDismissing] is true.
 * @property rotation Subtle tilt rotation derived from [animatedOffsetX].
 */
class DrawnCardState(
    initialOffsetX: Float = 0f,
) {
    var isPressed by mutableStateOf(false)
    var offsetX by mutableFloatStateOf(initialOffsetX)
    var isDismissing by mutableStateOf(false)

    /** Reset drag/dismiss state so the card snaps back to neutral. */
    fun snapBack() {
        offsetX = 0f
        isPressed = false
    }

    /** Begin the off-screen dismiss animation. */
    fun startDismiss() {
        isDismissing = true
    }

    /** Called by [animatedOffsetX]'s finishedListener once the card has left the screen. */
    fun onDismissAnimationFinished() {
        isDismissing = false
        offsetX = 0f
    }
}

/**
 * Creates and remembers a [DrawnCardState] whose animated values are wired up via
 * [animateFloatAsState] so the composable re-renders whenever they change.
 *
 * @param key Resets state whenever this key changes (e.g. the top card's ID).
 * @param onDismissed Called once the dismiss animation has fully completed, passing
 *   a positive [offsetX] direction flag so the caller knows which direction the card flew.
 */
@Composable
fun rememberDrawnCardState(
    key: Any?,
    onDismissed: (positiveDirection: Boolean) -> Unit,
): DrawnCardStateWithAnimations {
    val state = remember(key) { DrawnCardState() }

    val scale by animateFloatAsState(
        targetValue = if (state.isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "CardPressScale"
    )

    val positiveDirection = state.offsetX > 0
    val animatedOffsetX by animateFloatAsState(
        targetValue = if (state.isDismissing) {
            if (positiveDirection) 1500f else -1500f
        } else {
            state.offsetX
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "CardSwipeOffset",
        finishedListener = {
            if (state.isDismissing) {
                state.onDismissAnimationFinished()
                onDismissed(positiveDirection)
            }
        }
    )

    val rotation = (animatedOffsetX / 30f).coerceIn(-15f, 15f)

    return remember(state) {
        DrawnCardStateWithAnimations(state)
    }.also { wrapper ->
        wrapper.scale = scale
        wrapper.animatedOffsetX = animatedOffsetX
        wrapper.rotation = rotation
    }
}

/**
 * Wrapper that exposes the mutable gesture [state] together with the derived animated values
 * produced by [rememberDrawnCardState].
 */
class DrawnCardStateWithAnimations(val state: DrawnCardState) {
    var scale: Float = 1f
    var animatedOffsetX: Float = 0f
    var rotation: Float = 0f
}

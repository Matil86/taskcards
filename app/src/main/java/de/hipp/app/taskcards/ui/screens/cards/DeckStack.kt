package de.hipp.app.taskcards.ui.screens.cards

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.hipp.app.taskcards.R
import de.hipp.app.taskcards.ui.theme.CardStock
import de.hipp.app.taskcards.ui.theme.GoldAction
import de.hipp.app.taskcards.ui.theme.InkPrimary

private const val V_SWIPE_UP_THRESHOLD = 64f

@Composable
fun DeckStack(
    layers: Int,
    isDrawn: Boolean = false,
    modifier: Modifier = Modifier,
    onSwipeUp: () -> Unit = {}
) {
    // Show exact number of cards (max 5) sticking out of the box
    val clamped = layers.coerceIn(0, 5)
    var isPressed by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val deckScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "DeckPressScale"
    )

    Box(
        modifier = modifier
            .testTag("DeckStack")
            .scale(deckScale)
            // Gesture on outer Box so the whole area (card backs + box) responds to swipe-up
            .pointerInput(layers) {
                var totalDy = 0f
                detectDragGestures(
                    onDragStart = { totalDy = 0f; isPressed = true },
                    onDragCancel = { totalDy = 0f; isPressed = false },
                    onDragEnd = {
                        if (totalDy < -V_SWIPE_UP_THRESHOLD) onSwipeUp()
                        totalDy = 0f
                        isPressed = false
                    },
                ) { _, dragAmount -> totalDy += dragAmount.y }
            }
            .semantics {
                role = Role.Button
                liveRegion = LiveRegionMode.Polite
                contentDescription = when {
                    !isDrawn -> context.getString(
                        R.string.cards_deck_count_first_accessibility,
                        clamped
                    )
                    else -> context.getString(
                        R.string.cards_deck_count_accessibility,
                        clamped
                    )
                }
                // Custom accessibility action: Draw card without swiping up gesture
                customActions = listOf(
                    CustomAccessibilityAction(
                        label = when {
                            !isDrawn -> context.getString(R.string.cards_draw_first)
                            else -> context.getString(R.string.cards_draw_next)
                        }
                    ) {
                        onSwipeUp()
                        true
                    }
                )
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        // ── Cards emerging from the box ─────────────────────────────────────────
        // Cards are rendered FIRST so the box renders ON TOP, hiding the bottom
        // portion of each card — creating the illusion they slide out of the slot.
        if (clamped > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .align(Alignment.TopCenter),
                contentAlignment = Alignment.TopCenter
            ) {
                repeat(clamped) { index ->
                    val backIndex = (clamped - index - 1)
                    // Smaller offset = more card hidden inside the box → emerge effect
                    val verticalOffset = -28 - (backIndex * 7)
                    val horizontalInset = backIndex * 5
                    val alpha = 1f - (backIndex * 0.08f)

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .padding(
                                start = (16 + horizontalInset).dp,
                                end = (16 + horizontalInset).dp
                            )
                            .offset(y = verticalOffset.dp)
                            .alpha(alpha)
                            // Performance: drawWithCache pre-computes all diagonal line coordinates
                            // once when size changes and caches them. Only the onDrawBehind lambda
                            // re-executes on subsequent recompositions, avoiding repeated allocation
                            // of Offset pairs on every Firestore-triggered recomposition.
                            .drawWithCache {
                                // Capture size in CacheDrawScope before entering buildList
                                // (buildList changes `this` receiver, losing DrawScope.size access)
                                val w = size.width
                                val h = size.height
                                val lineSpacing = 8.dp.toPx()
                                val lineColor = InkPrimary.copy(alpha = 0.08f)
                                val strokeWidth = 0.8.dp.toPx()
                                val lines = buildList {
                                    var x = -h
                                    while (x < w + h) {
                                        add(Offset(x, 0f) to Offset(x + h, h))
                                        x += lineSpacing
                                    }
                                    var x2 = w + h
                                    while (x2 > -h) {
                                        add(Offset(x2, 0f) to Offset(x2 - h, h))
                                        x2 -= lineSpacing
                                    }
                                }
                                onDrawBehind {
                                    lines.forEach { (start, end) ->
                                        drawLine(color = lineColor, start = start, end = end, strokeWidth = strokeWidth)
                                    }
                                }
                            }
                            .testTag("DeckLayer"),
                        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
                        color = CardStock,
                        tonalElevation = 0.dp,
                        shadowElevation = (4 + backIndex).dp,
                        border = BorderStroke(
                            width = 1.dp,
                            color = InkPrimary.copy(alpha = 0.18f)
                        )
                    ) {
                        // Line drawing moved to drawWithCache on the Surface modifier above
                    }
                }
            }
        }

        // ── The Box — renders ON TOP of card bottoms to hide them inside ────────
        // Gold border makes it clearly visible on the dark felt background.
        // The box "swallows" the lower portion of each stacked card.
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.72f)          // Box takes 72% of height; top 28% shows cards emerging
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1A251A),          // Felt750 — slightly lighter than felt bg for contrast
            tonalElevation = 0.dp,
            shadowElevation = 20.dp,
            border = BorderStroke(
                width = 1.5.dp,
                color = GoldAction.copy(alpha = 0.35f)  // Gold border — clearly visible on dark felt
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Subtle inner gradient — lighter at top where cards emerge
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color(0xFF233023),    // Slightly lighter at the slot opening
                                0.25f to Color(0xFF1A251A), // Settles into box colour
                                1f to Color(0xFF141E14)     // Darker at bottom = depth
                            )
                        )
                )

                // Slot shadow at top — the opening where cards slide out
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(6.dp)
                        .align(Alignment.TopCenter)
                        .offset(y = (-3).dp)
                        .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.5f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Gold draw hint arrow
                if (clamped > 0 && !isPressed) {
                    Text(
                        text = stringResource(R.string.cards_draw_up_arrow),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Normal,
                        color = GoldAction.copy(alpha = 0.55f),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = 16.dp)
                    )
                }
            }
        }
    }
}

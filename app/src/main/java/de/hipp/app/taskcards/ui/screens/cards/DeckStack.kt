package de.hipp.app.taskcards.ui.screens.cards

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
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
import de.hipp.app.taskcards.ui.theme.BrandBlue
import de.hipp.app.taskcards.ui.theme.BrandPurple

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
        // Cards sticking out of the box at the top
        if (clamped > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .align(Alignment.TopCenter),
                contentAlignment = Alignment.TopCenter
            ) {
                // Stack of cards sticking out
                repeat(clamped) { index ->
                    val backIndex = (clamped - index - 1)
                    val verticalOffset = -60 - (backIndex * 8) // Stick out above the box
                    val horizontalInset = backIndex * 4
                    val alpha = 1f - (backIndex * 0.1f)

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .padding(
                                start = (20 + horizontalInset).dp,
                                end = (20 + horizontalInset).dp
                            )
                            .offset(y = verticalOffset.dp)
                            .alpha(alpha)
                            .testTag("DeckLayer"),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = (3 + backIndex * 2).dp,
                        shadowElevation = (4 + backIndex * 2).dp,
                        border = BorderStroke(
                            width = 2.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    BrandPurple.copy(alpha = 0.8f),
                                    BrandBlue.copy(alpha = 0.8f)
                                )
                            )
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(
                                                alpha = 0.2f
                                            ),
                                            MaterialTheme.colorScheme.primaryContainer,
                                            MaterialTheme.colorScheme.primary.copy(
                                                alpha = 0.15f
                                            )
                                        )
                                    )
                                )
                        )
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val lineSpacing = 12.dp.toPx()
                            // alpha is already applied to the parent Surface via .alpha(alpha);
                            // using a fixed value here avoids doubling the alpha multiplication
                            // and prevents the Canvas from being re-executed just because alpha
                            // changes during the deckScale animation.
                            val lineColor = BrandPurple.copy(alpha = 0.15f)
                            val strokeWidth = 1.dp.toPx()

                            // Draw diagonal lines at 45 degrees
                            var x = -size.height
                            while (x < size.width + size.height) {
                                drawLine(
                                    color = lineColor,
                                    start = Offset(x, 0f),
                                    end = Offset(x + size.height, size.height),
                                    strokeWidth = strokeWidth
                                )
                                x += lineSpacing
                            }
                        }
                    }
                }
            }
        }

        // Box container - the outer card box
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
            border = BorderStroke(
                width = 3.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        BrandPurple.copy(alpha = 0.6f),
                        BrandBlue.copy(alpha = 0.6f)
                    )
                )
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                            )
                        )
                    )
                    .pointerInput(layers) {
                        var totalDy = 0f
                        detectDragGestures(
                            onDragStart = {
                                totalDy = 0f
                                isPressed = true
                            },
                            onDragCancel = {
                                totalDy = 0f
                                isPressed = false
                            },
                            onDragEnd = {
                                // Trigger a single action per gesture if overall movement was upward
                                if (totalDy < -V_SWIPE_UP_THRESHOLD) {
                                    onSwipeUp()
                                }
                                totalDy = 0f
                                isPressed = false
                            },
                        ) { _, dragAmount ->
                            totalDy += dragAmount.y
                        }
                    }
            ) {
                // Inner shadow effect to create depth
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    // Opening/slot at the top where cards come out
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(16.dp)
                            .align(Alignment.TopCenter)
                            .offset(y = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.4f),
                                        Color.Black.copy(alpha = 0.15f)
                                    )
                                )
                            )
                    )
                }

                // Interactive hint text
                if (clamped > 0 && !isPressed) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.cards_draw_up_arrow),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = stringResource(R.string.cards_draw_card_label),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

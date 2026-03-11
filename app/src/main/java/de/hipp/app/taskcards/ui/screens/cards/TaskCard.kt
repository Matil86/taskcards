package de.hipp.app.taskcards.ui.screens.cards

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.hipp.app.taskcards.R
import de.hipp.app.taskcards.ui.theme.CardStock
import de.hipp.app.taskcards.ui.theme.GoldAction
import de.hipp.app.taskcards.ui.theme.InkPrimary
import de.hipp.app.taskcards.ui.theme.PlayfairFontFamily
import de.hipp.app.taskcards.ui.theme.Card200
import androidx.compose.runtime.remember
import de.hipp.app.taskcards.ui.theme.CrimsonAccent
import de.hipp.app.taskcards.ui.theme.GoldCardText
import de.hipp.app.taskcards.ui.theme.Verdant400
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * The drawn task card — playing card anatomy with aged card-stock feel.
 *
 * @param taskText The task text to display on the card face.
 * @param swipeOffset Current horizontal drag offset in pixels; drives swipe label visibility.
 * @param dueDate Optional due date timestamp in milliseconds; shows a badge on the card if set.
 * @param modifier Modifier applied to the outer Box (scale/rotation are applied here by the parent).
 * @param onComplete Called when the user triggers the complete action.
 * @param onRemove Called when the user triggers the remove action.
 */
@Composable
fun TaskCard(
    taskText: String,
    modifier: Modifier = Modifier,
    swipeOffset: Float = 0f,
    dueDate: Long? = null,
    onComplete: () -> Unit = {},
    onRemove: () -> Unit = {},
) {
    val context = LocalContext.current
    val cardShape = RoundedCornerShape(8.dp)

    // Swipe label alpha — fade in after ~40px drag, fully visible at 150px
    val swipeThreshold = 40f
    val swipeFull = 150f
    val rawAlpha = ((abs(swipeOffset) - swipeThreshold) / (swipeFull - swipeThreshold))
        .coerceIn(0f, 1f)
    val swipeLabelAlpha by animateFloatAsState(
        targetValue = rawAlpha,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "SwipeLabelAlpha"
    )
    val isDraggingRight = swipeOffset > 0f

    Box(modifier = modifier) {
        // Main card surface — aged card stock
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2.5f / 3.5f)
                .shadow(
                    elevation = 10.dp,
                    shape = cardShape,
                    ambientColor = InkPrimary.copy(alpha = 0.18f),
                    spotColor = InkPrimary.copy(alpha = 0.22f)
                ),
            shape = cardShape,
            color = CardStock,
            border = BorderStroke(
                width = 1.dp,
                color = InkPrimary.copy(alpha = 0.2f)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
                    .semantics {
                        role = Role.Button
                        contentDescription = context.getString(
                            R.string.cards_task_card_description,
                            taskText
                        )
                        // Accessibility: Skip task (right) or Mark done (left) without swiping
                        customActions = listOf(
                            CustomAccessibilityAction(
                                context.getString(R.string.cards_action_skip)
                            ) {
                                onComplete()
                                true
                            },
                            CustomAccessibilityAction(
                                context.getString(R.string.cards_action_mark_done)
                            ) {
                                onRemove()
                                true
                            }
                        )
                    }
            ) {
                // Corner index — top-left (playing card anatomy)
                Text(
                    text = "✦",
                    style = MaterialTheme.typography.labelSmall,
                    color = Card200,
                    modifier = Modifier.align(Alignment.TopStart)
                )

                // Corner index — bottom-right (rotated 180°, mirrored)
                Text(
                    text = "✦",
                    style = MaterialTheme.typography.labelSmall,
                    color = Card200,
                    modifier = Modifier.align(Alignment.BottomEnd)
                )

                // Task text — centre of card, Playfair Display for printed-card feel
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = taskText,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = PlayfairFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 22.sp,
                            lineHeight = 30.sp,
                            fontStyle = FontStyle.Normal
                        ),
                        color = InkPrimary,
                        textAlign = TextAlign.Center
                    )

                    // Due date badge — mirrors list card badge, adapted for card-stock surface
                    dueDate?.let { dueDateMs ->
                        val today = System.currentTimeMillis()
                        val dayInMs = 86_400_000L
                        val badgeColor = when {
                            dueDateMs < today -> CrimsonAccent
                            dueDateMs < today + dayInMs -> Verdant400
                            else -> GoldCardText
                        }
                        val dateText = if (dueDateMs < today + dayInMs && dueDateMs >= today) {
                            "Today"
                        } else {
                            remember(dueDateMs) {
                                SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(dueDateMs))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = badgeColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = dateText,
                                style = MaterialTheme.typography.labelSmall,
                                color = badgeColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Swipe label: "Later ↩" when swiping right (skip — recurring)
                    if (swipeLabelAlpha > 0f && isDraggingRight) {
                        Text(
                            text = stringResource(R.string.cards_swipe_later_label),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            ),
                            color = GoldAction.copy(alpha = swipeLabelAlpha),
                            textAlign = TextAlign.Center
                        )
                    }

                    // Swipe label: "Done ✓" when swiping left (mark complete)
                    if (swipeLabelAlpha > 0f && !isDraggingRight) {
                        Text(
                            text = stringResource(R.string.cards_swipe_done_label),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            ),
                            color = Verdant400.copy(alpha = swipeLabelAlpha),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

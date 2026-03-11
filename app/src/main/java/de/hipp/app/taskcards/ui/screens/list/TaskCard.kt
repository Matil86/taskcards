package de.hipp.app.taskcards.ui.screens.list

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.hipp.app.taskcards.R
import de.hipp.app.taskcards.ui.theme.AccentGreen
import de.hipp.app.taskcards.ui.theme.CrimsonAccent
import de.hipp.app.taskcards.ui.theme.Dimensions
import de.hipp.app.taskcards.ui.theme.Felt400
import de.hipp.app.taskcards.ui.theme.Felt50
import de.hipp.app.taskcards.ui.theme.Felt700
import de.hipp.app.taskcards.ui.theme.GoldAction
import de.hipp.app.taskcards.ui.theme.GoldCardText
import de.hipp.app.taskcards.ui.theme.Verdant400
import de.hipp.app.taskcards.ui.theme.focusIndicator

@Composable
fun TaskCard(
    text: String,
    done: Boolean,
    removed: Boolean,
    elevation: Dp,
    scale: Float = 1f,
    isDragging: Boolean = false,
    dragOffset: Float = 0f,
    dueDate: Long? = null,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    taskId: String = "",
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val alpha = if (removed) 0.5f else if (isDragging) 0.95f else 1f
    val removedScale by animateFloatAsState(
        targetValue = if (removed) 0.95f else 1f,
        label = "removed-scale",
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )
    )

    // Determine state description
    val stateStr = when {
        removed -> context.getString(R.string.list_task_removed_state)
        done -> context.getString(R.string.list_task_completed_state)
        else -> context.getString(R.string.list_task_active_state)
    }

    // Build custom actions list
    val customActionsList = buildList {
        onMoveUp?.let { moveUp ->
            add(CustomAccessibilityAction(
                label = context.getString(R.string.list_move_up_action)
            ) {
                moveUp()
                true
            })
        }
        onMoveDown?.let { moveDown ->
            add(CustomAccessibilityAction(
                label = context.getString(R.string.list_move_down_action)
            ) {
                moveDown()
                true
            })
        }
    }

    Card(
        modifier = modifier
            .scale(scale * removedScale)
            .alpha(alpha)
            .semantics {
                contentDescription = context.getString(
                    R.string.list_task_full_description,
                    text,
                    stateStr
                )
                stateDescription = stateStr
                role = Role.Button
                if (customActionsList.isNotEmpty()) {
                    customActions = customActionsList
                }
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging) {
                Felt700.copy(alpha = 0.85f)
            } else {
                Felt700
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = elevation
        ),
        border = BorderStroke(
            width = if (isDragging) 2.dp else 1.dp,
            brush = if (isDragging) {
                Brush.linearGradient(
                    colors = listOf(
                        GoldAction,
                        GoldAction.copy(alpha = 0.6f)
                    )
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(
                        when {
                            done -> AccentGreen.copy(alpha = 0.25f)
                            removed -> MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                        },
                        when {
                            done -> AccentGreen.copy(alpha = 0.25f)
                            removed -> MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                        }
                    )
                )
            }
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Custom checkbox with icon - WCAG AA compliant touch target (48dp minimum)
            Surface(
                shape = CircleShape,
                color = if (done) AccentGreen else Color.Transparent,
                border = if (!done) BorderStroke(2.dp, MaterialTheme.colorScheme.outline) else null,
                modifier = Modifier
                    .size(28.dp)
                    .sizeIn(minWidth = Dimensions.MinTouchTarget, minHeight = Dimensions.MinTouchTarget) // WCAG AA: Ensures 48dp touch target
                    .clip(CircleShape)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (done) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = stringResource(R.string.list_task_completed_description),
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    } else {
                        Checkbox(
                            checked = done,
                            onCheckedChange = onCheckedChange,
                            colors = CheckboxDefaults.colors(
                                checkedColor = AccentGreen,
                                checkmarkColor = Color.White
                            ),
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("TaskCheckbox-$taskId")
                                .focusIndicator(shape = CircleShape)
                        )
                    }
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (done) FontWeight.Normal else FontWeight.Medium,
                    textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (done) {
                        Felt400
                    } else {
                        Felt50
                    }
                )
                // Due date badge — show only if task has a due date
                dueDate?.let { dueDateMs ->
                    val today = System.currentTimeMillis()
                    val dayInMs = 86_400_000L
                    val badgeColor = when {
                        dueDateMs < today -> CrimsonAccent
                        dueDateMs < today + dayInMs -> Verdant400
                        else -> GoldCardText
                    }
                    val dateText = when {
                        dueDateMs < today + dayInMs && dueDateMs >= today -> "Today"
                        else -> remember(dueDateMs) {
                            SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(dueDateMs))
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = badgeColor.copy(alpha = 0.12f),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            if (removed) {
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = stringResource(R.string.list_removed_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

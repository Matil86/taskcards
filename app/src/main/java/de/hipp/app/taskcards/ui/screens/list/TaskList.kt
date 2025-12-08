package de.hipp.app.taskcards.ui.screens.list

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import de.hipp.app.taskcards.R
import de.hipp.app.taskcards.model.TaskItem
import de.hipp.app.taskcards.ui.viewmodel.ListViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TaskList(
    displayTasks: List<TaskItem>,
    vm: ListViewModel
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    // Track dragging state locally
    var draggingTaskId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    var targetDropIndex by remember { mutableStateOf<Int?>(null) }

    // Get active tasks for drag calculations
    val activeTasks = remember(displayTasks) {
        displayTasks.filter { !it.removed }
    }

    // Create live-reordered list during drag
    val reorderedTasks = remember(displayTasks, draggingTaskId, targetDropIndex) {
        if (draggingTaskId != null && targetDropIndex != null) {
            val draggedTask = activeTasks.find { it.id == draggingTaskId }
            val draggedIndex = activeTasks.indexOf(draggedTask)

            if (draggedTask != null && draggedIndex >= 0 && targetDropIndex != draggedIndex) {
                // Create new list with items reordered
                val mutableActive = activeTasks.toMutableList()
                mutableActive.removeAt(draggedIndex)
                mutableActive.add(targetDropIndex!!, draggedTask)

                // Combine with removed tasks at the end
                mutableActive + displayTasks.filter { it.removed }
            } else {
                displayTasks
            }
        } else {
            displayTasks
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(reorderedTasks, key = { item -> item.id }) { item ->
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = { value ->
                    when (value) {
                        SwipeToDismissBoxValue.StartToEnd -> {
                            vm.remove(item.id); true
                        }
                        SwipeToDismissBoxValue.EndToStart -> {
                            vm.restore(item.id); true
                        }
                        else -> false
                    }
                }
            )

            // Calculate active index from reordered list for correct positioning
            val reorderedActiveTasks = reorderedTasks.filter { !it.removed }
            val activeIndex = reorderedActiveTasks.indexOf(item).takeIf { it >= 0 }
            val activeCount = reorderedActiveTasks.size
            val isDragging = draggingTaskId == item.id

            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = {
                    SwipeBackground(dismissState.dismissDirection)
                },
                modifier = Modifier
                    .animateItem(
                        fadeInSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        placementSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        fadeOutSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    )
                    .pointerInput(item.id, activeTasks) {
                        if (item.removed) return@pointerInput

                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                if (activeIndex != null) {
                                    draggingTaskId = item.id
                                    dragOffset = 0f
                                    targetDropIndex = activeIndex
                                }
                            },
                            onDragEnd = {
                                if (activeIndex != null && draggingTaskId == item.id && targetDropIndex != null) {
                                    // Persist the move
                                    if (targetDropIndex != activeIndex) {
                                        vm.move(item.id, targetDropIndex!!)
                                    }
                                }
                                draggingTaskId = null
                                dragOffset = 0f
                                targetDropIndex = null
                            },
                            onDragCancel = {
                                draggingTaskId = null
                                dragOffset = 0f
                                targetDropIndex = null
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            dragOffset += dragAmount.y

                            // Calculate target drop index during drag
                            if (activeIndex != null) {
                                val cardHeightPx = with(density) { (80.dp + 12.dp).toPx() }
                                val positionsMoved = (dragOffset / cardHeightPx).toInt()
                                targetDropIndex = (activeIndex + positionsMoved).coerceIn(0, activeCount - 1)
                            }
                        }
                    },
                enableDismissFromStartToEnd = true,
                enableDismissFromEndToStart = true
            ) {
                TaskCard(
                    text = item.text,
                    done = item.done,
                    removed = item.removed,
                    elevation = if (isDragging) 8.dp else 2.dp,
                    scale = if (isDragging) 1.05f else 1f,
                    isDragging = isDragging,
                    dragOffset = 0f, // No visual offset, just rely on animateItemPlacement
                    onCheckedChange = { vm.toggleDone(item.id, it) },
                    taskId = item.id,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = context.getString(R.string.list_task_description, item.text)
                            if (activeIndex != null) {
                                val actionsList = mutableListOf<CustomAccessibilityAction>()

                                if (activeIndex > 0) {
                                    actionsList.add(
                                        CustomAccessibilityAction(context.getString(R.string.list_move_up_action)) {
                                            vm.move(item.id, activeIndex - 1)
                                            true
                                        }
                                    )
                                }

                                if (activeIndex < activeCount - 1) {
                                    actionsList.add(
                                        CustomAccessibilityAction(context.getString(R.string.list_move_down_action)) {
                                            vm.move(item.id, activeIndex + 1)
                                            true
                                        }
                                    )
                                }

                                if (actionsList.isNotEmpty()) {
                                    customActions = actionsList
                                }
                            }
                        }
                )
            }
        }
    }
}

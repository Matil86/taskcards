package de.hipp.app.taskcards.ui.screens.list

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import de.hipp.app.taskcards.ui.theme.CrimsonAccent

@Composable
fun SwipeBackground(direction: SwipeToDismissBoxValue?) {
    val color by animateColorAsState(
        targetValue = when (direction) {
            SwipeToDismissBoxValue.StartToEnd -> CrimsonAccent.copy(alpha = 0.8f) // Swipe right = remove (crimson)
            SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f) // Swipe left = restore (green)
            else -> Color.Transparent
        },
        label = "swipe-bg-color",
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    val icon = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Delete // Swipe right = delete icon
        SwipeToDismissBoxValue.EndToStart -> Icons.Default.RestoreFromTrash // Swipe left = restore icon
        else -> null
    }

    val alignment = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
        SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
        else -> Alignment.Center
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color, RoundedCornerShape(16.dp))
            .padding(horizontal = 24.dp),
        contentAlignment = alignment
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

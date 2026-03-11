package de.hipp.app.taskcards.ui.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.hipp.app.taskcards.ui.theme.Dimensions
import de.hipp.app.taskcards.ui.theme.Felt200
import de.hipp.app.taskcards.ui.theme.GoldAction
import de.hipp.app.taskcards.ui.theme.focusIndicator

@Composable
fun ModernNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "nav-scale"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier
            .sizeIn(minWidth = Dimensions.MinTouchTarget, minHeight = Dimensions.MinTouchTarget) // WCAG AA compliant: 48dp minimum touch target
            .scale(scale)
            .clip(RoundedCornerShape(Dimensions.CornerRadiusLarge))
            .focusIndicator(shape = RoundedCornerShape(Dimensions.CornerRadiusLarge))
            .semantics {
                role = Role.Tab
                stateDescription = if (selected) {
                    "Selected"
                } else {
                    "Not selected"
                }
            },
        shape = RoundedCornerShape(Dimensions.CornerRadiusLarge),
        color = if (selected) {
            GoldAction.copy(alpha = 0.15f)
        } else {
            Color.Transparent
        }
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (selected) GoldAction else Felt200,
                    modifier = Modifier.size(24.dp)
                )
                if (selected) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = GoldAction,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
        }
    }
}

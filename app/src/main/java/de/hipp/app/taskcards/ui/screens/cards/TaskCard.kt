package de.hipp.app.taskcards.ui.screens.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.hipp.app.taskcards.R
import de.hipp.app.taskcards.ui.theme.BrandBlue
import de.hipp.app.taskcards.ui.theme.BrandPurple
import de.hipp.app.taskcards.ui.theme.Dimensions

@Composable
fun TaskCard(
    taskText: String,
    modifier: Modifier = Modifier,
    onComplete: () -> Unit = {},
) {
    val context = LocalContext.current
    val cornerShape = RoundedCornerShape(24.dp)

    Box(modifier = modifier) {
        // Glow effect background
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 4.dp)
                .blur(16.dp)
                .clip(cornerShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            BrandPurple.copy(alpha = 0.3f),
                            BrandBlue.copy(alpha = 0.3f)
                        )
                    )
                )
        )

        // Main card surface with gradient
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 12.dp,
                    shape = cornerShape,
                    ambientColor = BrandPurple.copy(alpha = 0.2f),
                    spotColor = BrandPurple.copy(alpha = 0.2f)
                ),
            shape = cornerShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                width = 2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        BrandPurple.copy(alpha = 0.5f),
                        BrandBlue.copy(alpha = 0.5f)
                    )
                )
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimensions.DeckHeight * 3 / 2)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        )
                    )
                    .padding(32.dp)
                    .semantics {
                        role = Role.Button
                        contentDescription = context.getString(
                            R.string.cards_task_card_description,
                            taskText
                        )
                        // Custom accessibility action: Complete task without swiping
                        customActions = listOf(
                            CustomAccessibilityAction(
                                context.getString(
                                    R.string.cards_complete_task_action
                                )
                            ) {
                                onComplete()
                                true
                            }
                        )
                    }
            ) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = taskText,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

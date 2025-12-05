package de.hipp.app.taskcards.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

/**
 * Adds WCAG AA compliant focus indication for keyboard navigation.
 *
 * Displays a 2dp border when the element receives keyboard focus,
 * ensuring keyboard-only users can see which element is active.
 *
 * @param focusColor Color of the focus border (defaults to primary color)
 * @param focusWidth Width of the focus border (2dp per WCAG guidelines)
 * @param shape Shape of the focus border (matches element shape)
 */
@Composable
fun Modifier.focusIndicator(
    focusColor: Color = MaterialTheme.colorScheme.primary,
    focusWidth: Dp = Dimensions.FocusIndicatorWidth,
    shape: Shape = RoundedCornerShape(Dimensions.CornerRadiusSmall)
): Modifier {
    var isFocused by remember { mutableStateOf(false) }

    return this
        .onFocusChanged { focusState ->
            isFocused = focusState.isFocused
        }
        .then(
            if (isFocused) {
                Modifier.border(
                    border = BorderStroke(focusWidth, focusColor),
                    shape = shape
                )
            } else {
                Modifier
            }
        )
}

package de.hipp.app.taskcards.ui.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.hipp.app.taskcards.ui.navigation.ModernNavigationBar
import de.hipp.app.taskcards.ui.screens.cards.DeckStack
import de.hipp.app.taskcards.ui.screens.cards.TaskCard as DrawnTaskCard
import de.hipp.app.taskcards.ui.screens.list.TaskCard as ListTaskCard
import de.hipp.app.taskcards.ui.screens.settings.cards.NotificationSettingsCard
import de.hipp.app.taskcards.ui.theme.FeltBackground
import de.hipp.app.taskcards.ui.theme.LinenBackground
import de.hipp.app.taskcards.ui.theme.TaskCardsTheme

// ─────────────────────────────────────────────────────────────
// SECTION 1 — DeckStack
// ─────────────────────────────────────────────────────────────

@Preview(
    name = "DeckStack — 5 cards (Dark)",
    showBackground = true,
    backgroundColor = 0xFF111811,
    widthDp = 360,
    heightDp = 640
)
@Composable
private fun DeckStack5CardsPreview() {
    TaskCardsTheme(useDarkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FeltBackground)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            DeckStack(
                layers = 5,
                isDrawn = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )
        }
    }
}

@Preview(
    name = "DeckStack — 2 cards, drawn (Dark)",
    showBackground = true,
    backgroundColor = 0xFF111811,
    widthDp = 360,
    heightDp = 640
)
@Composable
private fun DeckStack2CardsPreview() {
    TaskCardsTheme(useDarkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FeltBackground)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            DeckStack(
                layers = 2,
                isDrawn = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )
        }
    }
}

@Preview(
    name = "DeckStack — Empty (Dark)",
    showBackground = true,
    backgroundColor = 0xFF111811,
    widthDp = 360,
    heightDp = 640
)
@Composable
private fun DeckStackEmptyPreview() {
    TaskCardsTheme(useDarkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FeltBackground)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            DeckStack(
                layers = 0,
                isDrawn = false,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// SECTION 2 — TaskCard (drawn card / cards screen)
// ─────────────────────────────────────────────────────────────

@Preview(
    name = "TaskCard — Default, no swipe (Dark)",
    showBackground = true,
    backgroundColor = 0xFF111811,
    widthDp = 360,
    heightDp = 640
)
@Composable
private fun TaskCardDefaultPreview() {
    TaskCardsTheme(useDarkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FeltBackground)
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            DrawnTaskCard(
                taskText = "Review quarterly budget report",
                swipeOffset = 0f,
                onComplete = {},
                onRemove = {},
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(
    name = "TaskCard — Default, no swipe (Light)",
    showBackground = true,
    backgroundColor = 0xFFF0EBE0,
    widthDp = 360,
    heightDp = 640
)
@Composable
private fun TaskCardDefaultLightPreview() {
    TaskCardsTheme(useDarkTheme = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LinenBackground)
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            DrawnTaskCard(
                taskText = "Review quarterly budget report",
                swipeOffset = 0f,
                onComplete = {},
                onRemove = {},
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(
    name = "TaskCard — Long text (Dark)",
    showBackground = true,
    backgroundColor = 0xFF111811,
    widthDp = 360,
    heightDp = 640
)
@Composable
private fun TaskCardLongTextPreview() {
    TaskCardsTheme(useDarkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FeltBackground)
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            DrawnTaskCard(
                taskText = "Schedule a follow-up meeting with the design team to align on the revised component library and discuss accessibility requirements",
                swipeOffset = 0f,
                onComplete = {},
                onRemove = {},
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(
    name = "TaskCard — Swipe right: Later ↩ (Dark)",
    showBackground = true,
    backgroundColor = 0xFF111811,
    widthDp = 360,
    heightDp = 640
)
@Composable
private fun TaskCardSwipeRightPreview() {
    TaskCardsTheme(useDarkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FeltBackground)
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            DrawnTaskCard(
                taskText = "Review quarterly budget report",
                swipeOffset = 120f,
                onComplete = {},
                onRemove = {},
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(
    name = "TaskCard — Swipe left: Done ✓ (Dark)",
    showBackground = true,
    backgroundColor = 0xFF111811,
    widthDp = 360,
    heightDp = 640
)
@Composable
private fun TaskCardSwipeLeftPreview() {
    TaskCardsTheme(useDarkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FeltBackground)
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            DrawnTaskCard(
                taskText = "Review quarterly budget report",
                swipeOffset = -120f,
                onComplete = {},
                onRemove = {},
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// SECTION 3 — ModernNavigationBar
// ─────────────────────────────────────────────────────────────

@Preview(
    name = "NavBar — Cards selected (Dark)",
    showBackground = true,
    backgroundColor = 0xFF111811,
    widthDp = 360,
    heightDp = 120
)
@Composable
private fun NavBarCardsSelectedPreview() {
    TaskCardsTheme(useDarkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FeltBackground),
            contentAlignment = Alignment.BottomCenter
        ) {
            ModernNavigationBar(
                currentRoute = "cards",
                onNavigateToCards = {},
                onNavigateToList = {},
                onNavigateToSettings = {}
            )
        }
    }
}

@Preview(
    name = "NavBar — Settings selected (Dark)",
    showBackground = true,
    backgroundColor = 0xFF111811,
    widthDp = 360,
    heightDp = 120
)
@Composable
private fun NavBarSettingsSelectedPreview() {
    TaskCardsTheme(useDarkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FeltBackground),
            contentAlignment = Alignment.BottomCenter
        ) {
            ModernNavigationBar(
                currentRoute = "settings",
                onNavigateToCards = {},
                onNavigateToList = {},
                onNavigateToSettings = {}
            )
        }
    }
}

@Preview(
    name = "NavBar — Cards selected (Light)",
    showBackground = true,
    backgroundColor = 0xFFF0EBE0,
    widthDp = 360,
    heightDp = 120
)
@Composable
private fun NavBarCardsSelectedLightPreview() {
    TaskCardsTheme(useDarkTheme = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LinenBackground),
            contentAlignment = Alignment.BottomCenter
        ) {
            ModernNavigationBar(
                currentRoute = "cards",
                onNavigateToCards = {},
                onNavigateToList = {},
                onNavigateToSettings = {}
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// SECTION 4 — ListTaskCard (list screen)
// ─────────────────────────────────────────────────────────────

@Preview(
    name = "ListTaskCard — Active (Dark)",
    showBackground = true,
    backgroundColor = 0xFF111811,
    widthDp = 360,
    heightDp = 120
)
@Composable
private fun ListTaskCardActivePreview() {
    TaskCardsTheme(useDarkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FeltBackground)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            ListTaskCard(
                text = "Prepare slide deck for Monday stand-up",
                done = false,
                removed = false,
                elevation = 4.dp,
                onCheckedChange = {}
            )
        }
    }
}

@Preview(
    name = "ListTaskCard — Done / strikethrough (Dark)",
    showBackground = true,
    backgroundColor = 0xFF111811,
    widthDp = 360,
    heightDp = 120
)
@Composable
private fun ListTaskCardDonePreview() {
    TaskCardsTheme(useDarkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FeltBackground)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            ListTaskCard(
                text = "Prepare slide deck for Monday stand-up",
                done = true,
                removed = false,
                elevation = 4.dp,
                onCheckedChange = {}
            )
        }
    }
}

@Preview(
    name = "ListTaskCard — Active (Light)",
    showBackground = true,
    backgroundColor = 0xFFF0EBE0,
    widthDp = 360,
    heightDp = 120
)
@Composable
private fun ListTaskCardActiveLightPreview() {
    TaskCardsTheme(useDarkTheme = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LinenBackground)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            ListTaskCard(
                text = "Prepare slide deck for Monday stand-up",
                done = false,
                removed = false,
                elevation = 4.dp,
                onCheckedChange = {}
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// SECTION 5 — NotificationSettingsCard
// ─────────────────────────────────────────────────────────────

@Preview(
    name = "NotificationCard — Permission granted (Dark)",
    showBackground = true,
    backgroundColor = 0xFF111811,
    widthDp = 360,
    heightDp = 480
)
@Composable
private fun NotificationCardPermissionGrantedPreview() {
    TaskCardsTheme(useDarkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FeltBackground)
                .padding(16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            NotificationSettingsCard(
                remindersEnabled = true,
                reminderHour = 8,
                reminderMinute = 30,
                notificationSound = true,
                notificationVibration = false,
                onRemindersToggle = {},
                onReminderTimeClick = {},
                onNotificationSoundToggle = {},
                onNotificationVibrationToggle = {},
                canScheduleExactAlarms = true
            )
        }
    }
}

@Preview(
    name = "NotificationCard — No permission (Dark)",
    showBackground = true,
    backgroundColor = 0xFF111811,
    widthDp = 360,
    heightDp = 320
)
@Composable
private fun NotificationCardNoPermissionPreview() {
    TaskCardsTheme(useDarkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FeltBackground)
                .padding(16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            NotificationSettingsCard(
                remindersEnabled = false,
                reminderHour = 9,
                reminderMinute = 0,
                notificationSound = false,
                notificationVibration = false,
                onRemindersToggle = {},
                onReminderTimeClick = {},
                onNotificationSoundToggle = {},
                onNotificationVibrationToggle = {},
                canScheduleExactAlarms = false
            )
        }
    }
}

@Preview(
    name = "NotificationCard — Permission granted (Light)",
    showBackground = true,
    backgroundColor = 0xFFF0EBE0,
    widthDp = 360,
    heightDp = 480
)
@Composable
private fun NotificationCardPermissionGrantedLightPreview() {
    TaskCardsTheme(useDarkTheme = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LinenBackground)
                .padding(16.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            NotificationSettingsCard(
                remindersEnabled = true,
                reminderHour = 8,
                reminderMinute = 30,
                notificationSound = true,
                notificationVibration = false,
                onRemindersToggle = {},
                onReminderTimeClick = {},
                onNotificationSoundToggle = {},
                onNotificationVibrationToggle = {},
                canScheduleExactAlarms = true
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// SECTION 6 — Full CardsScreen layout preview (static, no VM)
// ─────────────────────────────────────────────────────────────

@Preview(
    name = "CardsScreen — Full layout, 5 cards (Dark)",
    showBackground = true,
    backgroundColor = 0xFF111811,
    widthDp = 360,
    heightDp = 760
)
@Composable
private fun CardsScreenFullPreview() {
    TaskCardsTheme(useDarkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FeltBackground)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Drawn card — centred in the upper portion
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    DrawnTaskCard(
                        taskText = "Review quarterly budget report",
                        swipeOffset = 0f,
                        onComplete = {},
                        onRemove = {},
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Deck box at the bottom — extra headroom so card tops are visible
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    DeckStack(
                        layers = 4,
                        isDrawn = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    )
                }
            }

            // Navigation pill pinned to bottom
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 8.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                ModernNavigationBar(
                    currentRoute = "cards",
                    onNavigateToCards = {},
                    onNavigateToList = {},
                    onNavigateToSettings = {}
                )
            }
        }
    }
}

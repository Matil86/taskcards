# TaskCards — Expected App Behaviour

Use this document to verify that the app behaves correctly after any code change.
Each section describes what the user should experience and what must not happen.

---

## Navigation structure

The app has three main screens reachable via the bottom navigation bar:

| Tab | Screen | Purpose |
|---|---|---|
| Cards (leftmost) | CardsScreen | Draw and swipe tasks one at a time |
| List (centre) | ListScreen | Full task list with add/edit/delete |
| Settings (rightmost) | SettingsScreen | Account, notifications, appearance |

The app always opens on the **Cards screen**. The bottom bar is only visible on Cards,
List, and Settings — not on the List Selector screen.

---

## Authentication state

### Unauthenticated

- App is fully functional offline using Room (local SQLite).
- The default local list (`default-list`) is used for both Cards and List screens.
- No Firestore read or write is performed.
- The List screen shows the local Room list. It may be empty on first launch.
- Settings shows a "Sign In" option; no email is displayed.

### After sign-in

- The active list switches from Room (`default-list`) to the user's Firestore list.
- `defaultListId` resolves automatically — the user must **not** need to tap anything.
- Resolution completes within ~5 s on a normal connection (instant for returning users).
- **No new Firestore list is created** if the user already has lists.
- A new list is created only when the server confirms zero lists exist for this account.
- Both Cards and List screens update to show the Firestore list once resolved.
- Settings shows the signed-in email address.

### After sign-out

- The active list reverts to the local Room list (`default-list`).
- Firestore data is no longer visible.
- The app continues to work offline with local data.

---

## Cards screen

### Initial state (deck not yet drawn)

- The deck stack is shown at the bottom of the screen (up to 5 layered card shapes).
- No card is displayed above the deck yet.
- The user must **swipe up on the deck** to draw the first card.

### Drawing a card

- Swipe up on the deck → the top task card slides up and is shown above the deck.
- The deck visually shrinks by one layer.
- If there is only one task in the deck and it is drawn, the deck shows zero layers.

### Card gestures

| Gesture | Result |
|---|---|
| Swipe **left** on drawn card | Task marked **done**; card dismissed; celebration animation plays |
| Swipe **right** on drawn card | Task **skipped** — moved to bottom of deck; card dismissed; deck unchanged count |
| Swipe **up** on deck (when card already drawn) | Top task of the visible window marked done |

Skipping a card places it at the true bottom of the full active deck
(not just the 5-card visible window).

### Empty deck state

- When all tasks are done (`totalActive == 0`), the celebration/completion animation is shown.
- No deck stack is rendered.
- Adding a new task via the List screen restores the deck.

### Visible window

- The deck shows at most **5 active tasks** visually.
- All active tasks in the list are counted for ordering; skip moves to the true end.

### Error handling

- Repository errors appear as a snackbar at the bottom of the screen.
- Errors are dismissed automatically after a short duration or via the "Dismiss" action.

---

## List screen

### Header

- Displays the list name and a `X completed / Y total` summary.
- Summary updates live as tasks are added, completed, or removed.
- Three icon buttons:
  - **Switch list** (⇄) — navigates to the List Selector; only visible when a selector is available.
  - **Share** — opens the Share dialog.
  - **Filter** (funnel) — opens the Filter bottom sheet; shows a gold badge when filters are active.

### Task list

- Shows all tasks for the active list (respects current filter).
- Tasks are ordered by their `order` field (ascending).
- Done tasks are visually distinct (strikethrough or dimmed, depending on theme).
- Removed tasks are not shown.

### Adding a task

- Text field at the bottom of the screen.
- Submitting a non-blank string creates a new task and clears the field.
- Blank input is ignored (no task created, no error).

### Swipe actions

| Swipe direction | Result |
|---|---|
| Swipe **left** | Task **removed** (soft-deleted; hidden from list and deck) |
| Swipe **right** | *(not used on List screen; left is remove only)* |

### Task actions (tap)

- Tapping a task toggles its **done** state.
- Long press may reveal a reorder handle (drag to reorder).

### Drag to reorder

- Tasks can be dragged to change their position.
- The new position is persisted to the repository.

### Filtering

- Filter bottom sheet supports: text search, status filter (active/done/all), due-date range.
- Active filters are shown as chips below the header.
- Clearing all filters restores the full list.
- Saved searches can be named and recalled.

### Last-used list persistence

- When the List screen opens with a given `listId`, that ID is saved as `lastUsedListId`.
- On next sign-in, the app navigates back to the last-used list automatically.

---

## List Selector screen

- Shows all Firestore lists the signed-in user has access to (via `contributors` array).
- Each list card shows the list name and a three-dot menu with Rename and Delete.
- Tapping a list card navigates to the List screen for that list and saves the selection.
- **Create list** button creates a new Firestore list and navigates to it.
- Empty state is shown when the user has no lists yet.
- Only reachable via the ⇄ button in the List header; not in the bottom navigation bar.

---

## Settings screen

### Authentication card

- Unauthenticated: shows "Sign In with Google" button.
- Authenticated: shows the signed-in email and a "Sign Out" button.

### Notifications card

- Toggle to enable/disable daily reminders.
- Time picker to choose the reminder time.
- Reminder is rescheduled immediately when the time is changed.

### High contrast mode card

- Toggle switches the app theme to a high-contrast variant.
- Takes effect immediately without restart.

### Language card

- Allows selecting the app language independently of the system locale.
- App language preference is persisted.

---

## Deep links / QR codes

- The app handles deep links to join a shared list.
- Scanning a QR code on the List screen opens a confirmation dialog.
- Accepting the dialog adds the scanned list to the user's accessible lists and navigates to it.
- The joined list ID is saved as `lastUsedListId`.

---

## Home screen widgets

Three widget types are available:

| Widget | Description |
|---|---|
| **TaskList** | Shows up to 5 tasks from the chosen list |
| **QuickAdd** | Opens the app directly to the add-task flow |
| **DueToday** | Shows all tasks with a due date of today |

Widgets are updated by `WidgetUpdateWorker`, which runs periodically and on data changes.

---

## Due dates and reminders

- Tasks may have an optional `dueDate` (milliseconds since epoch).
- Due-date badges on List screen task cards are colour-coded:
  - **Red** (`CrimsonAccent`) — overdue
  - **Green** (`Verdant400`) — due today
  - **Gold** (`GoldCardText`) — upcoming
- Daily reminder fires at the user-configured time if reminders are enabled.
- Per-task reminders can be set via `reminderType`; managed by `ReminderScheduler`.

---

## Data storage

| Scope | Storage |
|---|---|
| Unauthenticated tasks | Room (local SQLite) |
| Authenticated tasks and list metadata | Firestore |
| User preferences (last list, reminder settings, language, high contrast) | DataStore |
| Widget data | Read from the active repository at widget update time |

Firestore data is scoped per user via the `contributors` array on each list document.
A user can only read or write lists where their UID appears in `contributors`.

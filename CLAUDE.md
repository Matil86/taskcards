# CLAUDE.md — TaskCards

Quick reference for developers and AI assistants working in this codebase.

---

## Project Overview

TaskCards is an Android task management app with a card-deck UI built entirely with Jetpack Compose. Tasks are stored locally via Room when the user is unauthenticated and synced to Firestore after Google Sign-In.

- **minSdk**: 34
- **targetSdk**: 36
- **compileSdk**: 36
- **versionName**: 1.0.0
- **Language**: Kotlin 2.3.x (Compose plugin 2.3.10)

---

## Build & Run

```bash
# Debug build
./gradlew assembleDebug

# Run unit tests (Kotest, JUnit 5 platform)
./gradlew test

# Run connected/instrumented tests
./gradlew connectedAndroidTest

# Static analysis
./gradlew detekt
./gradlew ktlintCheck

# Security scan (OWASP dependency check)
./gradlew dependencyCheckAnalyze

# Code coverage
./gradlew koverHtmlReport
```

Signing for release builds requires environment variables: `RELEASE_KEYSTORE_PATH`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`.

---

## Architecture

MVVM + Repository pattern with unidirectional data flow.

```
UI Layer (Compose Screens)
        |  observes StateFlow
        v
ViewModel Layer (CardsViewModel, ListViewModel, SettingsViewModel, ...)
        |  calls suspend functions / collects Flow
        v
Repository Layer (TaskListRepository, PreferencesRepository, TaskListMetadataRepository)
        |  persists / fetches
        v
Data Sources (Room SQLite, Firebase Firestore, DataStore)
```

Key principles:
- **Local-first**: app fully functional offline via Room.
- **Optional cloud sync**: Firestore enabled after Google Sign-In.
- **Reactive by default**: all data streams are `Flow<T>`.
- **Dependency inversion**: ViewModels depend on repository interfaces, not implementations.

---

## Key Files

| Concern | Path |
|---|---|
| Compose screens | `app/src/main/java/de/hipp/app/taskcards/ui/screens/` |
| ViewModels | `app/src/main/java/de/hipp/app/taskcards/ui/viewmodel/` |
| Repository interfaces | `app/src/main/java/de/hipp/app/taskcards/data/` |
| Room impl | `data/RoomTaskListRepository.kt`, `data/AppDatabase.kt` |
| Firestore impl | `data/FirestoreTaskListRepository.kt`, `data/FirestoreTaskListMetadataRepository.kt` |
| Firestore rules | `firestore.rules` (deploy via Firebase CLI) |
| Koin modules | `app/src/main/java/de/hipp/app/taskcards/di/KoinModules.kt` |
| Auth-aware provider | `app/src/main/java/de/hipp/app/taskcards/di/RepositoryProvider.kt` |
| Reminder pipeline | `app/src/main/java/de/hipp/app/taskcards/worker/ReminderScheduler.kt` |
| Auth service | `app/src/main/java/de/hipp/app/taskcards/auth/` |
| Unit tests | `app/src/test/java/de/hipp/app/taskcards/` |

---

## Dependency Injection

Koin **4.1.1** is used throughout. Hilt was removed and replaced with Koin.

Three Koin modules are defined in `KoinModules.kt`:

- **`appModule`** — repositories, analytics, string provider, `ReminderScheduler`, `DeepLinkHandler`, `QRCodeGenerator`.
- **`viewModelModule`** — all ViewModels.
- **`workerModule`** — `ReminderWorker`, `DailyReminderWorker` (via `workerOf`).

Inject in Compose with `koinInject<T>()` or `koinViewModel()`.

---

## Important Patterns

### Auth-aware repository switching

`RepositoryProvider` is the single source of truth for auth state. Call `RepositoryProvider.setAuthenticated(true/false)` when auth state changes (done in `TaskCardsApp`). It exposes `isAuthenticated()` which the Koin factory lambdas consult on each injection.

`TaskListRepository` and `TaskListMetadataRepository` are registered as **`factory<>`** (not `single<>`) in `appModule`. This ensures every injection resolves the current auth state rather than caching the instance from startup:

- Authenticated → `FirestoreTaskListRepository` / `FirestoreTaskListMetadataRepository`
- Unauthenticated → `RoomTaskListRepository` / `InMemoryTaskListMetadataRepository`

Both `FirestoreTaskListRepository` and `RoomTaskListRepository` receive `ReminderScheduler` via Koin so that `updateTaskDueDate()` can schedule notifications.

### Koin injection in Compose screens

In composable screens, use `koinInject<TaskListRepository>()` (not `remember { RepositoryProvider.getRepository(...) }`). The latter bypasses Koin and captures a stale instance for the lifetime of the composition.

### ListViewModel secondary constructor

`ListViewModel` has a primary constructor that takes `SavedStateHandle` (used by the Koin `viewModel` block) and a secondary constructor that takes a plain `listId: String` (used by `ListScreen` via `factoryOf { ListViewModel(listId, repo, prefsRepo, strings) }`). The secondary constructor wraps the string in a `SavedStateHandle` internally and supplies a no-op `Analytics`.

### ReminderScheduler

`ReminderScheduler` is a singleton in Koin. It wraps WorkManager to schedule and cancel per-task reminders. It is injected into both `RoomTaskListRepository` and `FirestoreTaskListRepository` so both paths schedule notifications consistently.

---

## Testing

- **Framework**: Kotest 5.9.1, JUnit 5 platform
- **Coroutines**: `kotlinx-coroutines-test`
- **Run**: `./gradlew test`
- **Parallelism**: up to 3 test classes concurrently, `maxParallelForks` = CPU count
- **Instrumented**: `./gradlew connectedAndroidTest` (requires emulator or device)
- Repository implementations are testable via `RepositoryProvider.setRepository(...)` / `RepositoryProvider.setMetadataRepository(...)`.

---

## Design Identity

TaskCards uses the **Velvet Table** visual theme — a warm, tactile aesthetic inspired by felt game tables and aged card stock.

### Color Palette

| Token | Hex | Role |
|---|---|---|
| `FeltBackground` | `#111811` | App background (dark green felt) |
| `CardStock` | `#F7F3EC` | Card surface (warm off-white) |
| `GoldAction` | `#E8B020` | Primary actions, highlights; Settings switches accent |
| `GoldCardText` | — | Due date badge: upcoming tasks |
| `CrimsonAccent` | `#C41E1E` | Destructive actions, urgent indicators; due date badge: overdue tasks |
| `Verdant400` | — | Due date badge: tasks due today |

### Component Notes

- **Empty states**: `EmptyListsState` uses the Velvet Table style — a card outline illustration rendered on the felt background, consistent with the overall tactile theme.
- **Due date badges on TaskCards**: displayed on list-view `TaskCard` items with color-coded backgrounds — `CrimsonAccent` (overdue), `Verdant400` (due today), `GoldCardText` (upcoming).
- **Settings switches**: use `GoldAction` as the thumb/track accent color.

### Typography

| Font | Usage |
|---|---|
| **Outfit** | Display / Headline (app name, screen titles) |
| **Playfair Display** | Card face text (task content on cards) |
| **DM Sans** | Body / Label (supporting text, chips, captions) |

Outfit and DM Sans are loaded via the existing GMS Downloadable Fonts provider. Playfair Display is also served through the same provider — no additional dependencies required.

---

## Swipe Behavior (Card Deck)

The card deck screen uses gesture-based task actions:

- Swipe RIGHT → "Later ↩" — card returns to bottom of deck, task stays active
- Swipe LEFT → "Done ✓" — task marked as complete
- Swipe UP on deck box → draw next card from deck

---

## Widget Architecture

Three home screen widget types live in the `widget/` package:

- **TaskList** — displays up to 5 tasks from a chosen list
- **QuickAdd** — one-tap task entry without opening the app
- **DueToday** — shows all tasks due on the current day

All three are built with Jetpack Glance and update automatically via `WidgetUpdateWorker`.

---

## Firestore Data Model

### `/lists/{listId}`

| Field | Type | Notes |
|---|---|---|
| `creatorUid` | `string` | UID of the user who created the list |
| `contributors` | `array<string>` | UIDs of all users with access (always includes `creatorUid`); used for `whereArrayContains` queries |
| `name` | `string` | 1–100 characters |
| `createdAt` | `int` | Unix timestamp (ms) |
| `lastModifiedAt` | `int` | Unix timestamp (ms) |

- **Contributors model**: a list is accessible to any UID present in the `contributors` array. The creator is always added to `contributors` on creation.
- **Join via QR code**: unauthenticated-to-contributor join is handled by a Firestore update that only adds the joining user's UID to `contributors` (enforced in `firestore.rules`).
- **Queries**: `FirestoreTaskListMetadataRepository` uses `whereArrayContains("contributors", uid)` to fetch all lists the signed-in user can access.

### `/lists/{listId}/tasks/{taskId}`

Standard task fields: `text`, `order`, `done`, `removed`, `timestamp`, plus optional `dueDate` (int ms) and `reminderType` (string).

---

## Known Issues / Conventions

- Kotlin **2.3.x** is used; note that some Compose compiler and KSP behaviours differ from the 1.x line.
- **Hilt was removed** in favour of Koin; do not re-introduce Hilt annotations.
- Never use `runBlocking` in production code or tests — use `runTest` / coroutine test utilities.
- Never use fully-qualified names (FQNs) inline — always add an `import`.
- Auth state is set via `RepositoryProvider.setAuthenticated()` inside `TaskCardsApp`; avoid calling it elsewhere to prevent unintended repository invalidation.

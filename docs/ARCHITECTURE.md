# TaskCards Technical Architecture

**Version**: 1.0
**Last Updated**: November 11, 2025
**Target Audience**: Developers, Contributors, Technical Reviewers

This document provides an in-depth technical reference for the TaskCards architecture, implementation details, and design decisions. For high-level project overview, see [README.md](../README.md). For Claude Code-specific guidance, see [CLAUDE.md](../CLAUDE.md).

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Data Layer](#data-layer)
3. [Domain Models](#domain-models)
4. [State Management](#state-management)
5. [Dependency Injection](#dependency-injection)
6. [Database & Persistence](#database--persistence)
7. [Background Processing](#background-processing)
8. [Widget Architecture](#widget-architecture)
9. [Internationalization System](#internationalization-system)
10. [Build Configuration](#build-configuration)
11. [ProGuard & Code Optimization](#proguard--code-optimization)
12. [Testing Architecture](#testing-architecture)
13. [Performance Considerations](#performance-considerations)
14. [Security & Privacy](#security--privacy)
15. [Deep Dive: Key Implementations](#deep-dive-key-implementations)

---

## Architecture Overview

### Core Pattern: MVVM + Repository

TaskCards implements a clean MVVM (Model-View-ViewModel) architecture with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────┐
│                    UI Layer (Compose)                    │
│  CardsScreen, ListScreen, SettingsScreen, etc.          │
└────────────────┬────────────────────────────────────────┘
                 │ observes StateFlow
                 ▼
┌─────────────────────────────────────────────────────────┐
│               ViewModel Layer                            │
│  CardsViewModel, ListViewModel, SettingsViewModel       │
│  - Exposes StateFlow<UiState>                           │
│  - Handles user actions                                 │
│  - Transforms repository emissions                       │
└────────────────┬────────────────────────────────────────┘
                 │ calls repository methods
                 ▼
┌─────────────────────────────────────────────────────────┐
│              Repository Layer                            │
│  TaskListRepository, PreferencesRepository              │
│  - Abstracts data sources                               │
│  - Multiple implementations (InMemory, Room, Firestore) │
│  - Exposes Flow<T> for reactive updates                │
└────────────────┬────────────────────────────────────────┘
                 │ persists/fetches
                 ▼
┌─────────────────────────────────────────────────────────┐
│                 Data Sources                             │
│  Room Database, Firebase Firestore, DataStore           │
└─────────────────────────────────────────────────────────┘
```

### Key Architectural Principles

1. **Unidirectional Data Flow**: Data flows down (State), events flow up (Actions)
2. **Single Source of Truth**: Repository layer is authoritative
3. **Reactive by Default**: Kotlin Flow powers all data streams
4. **Local-First**: App fully functional offline with Room persistence
5. **Optional Cloud Sync**: Firestore sync enabled via authentication
6. **Dependency Inversion**: ViewModels depend on repository interfaces, not implementations

---

## Data Layer

### Repository Pattern Implementation

The app uses three primary repository interfaces:

#### 1. TaskListRepository

**Interface**: `data/TaskListRepository.kt`

**Implementations**:
- `InMemoryTaskListRepository` - Ephemeral storage (testing only)
- `RoomTaskListRepository` - Local SQLite persistence via Room (unauthenticated state, production default)
- `FirestoreTaskListRepository` - Cloud sync via Firebase Firestore (authenticated state)

**Core Operations**:
```kotlin
interface TaskListRepository {
    // Reactive observation
    fun observeTasks(listId: String): Flow<List<TaskItem>>

    // CRUD operations
    suspend fun addTask(listId: String, text: String): String
    suspend fun removeTask(listId: String, taskId: String)
    suspend fun restoreTask(listId: String, taskId: String)
    suspend fun markDone(listId: String, taskId: String, done: Boolean)

    // Task ordering (lower order = higher priority)
    suspend fun moveTask(listId: String, taskId: String, toIndex: Int)

    // Due Dates
    suspend fun updateTaskDueDate(listId: String, taskId: String, dueDate: Long?, reminderType: ReminderType)

    // Search & Filtering
    suspend fun searchTasks(listId: String, query: String): List<TaskItem>
    suspend fun filterTasks(listId: String, filter: SearchFilter): List<TaskItem>

    // Memory management (production)
    suspend fun clearList(listId: String)
    suspend fun getActiveListCount(): Int
}
```

**Design Decisions**:
- **Flow-based observation**: `observeTasks()` returns `Flow<List<TaskItem>>` for reactive UI updates
- **Suspend functions**: All mutation operations are suspending for coroutine integration
- **List ID parameter**: All operations require explicit listId (currently always `DEFAULT_LIST_ID`)
- **Order compaction**: `moveTask()` automatically reorders all tasks to prevent integer overflow

#### 2. PreferencesRepository

**Implementation**: DataStore-based (Preferences or Proto)

**Responsibilities**:
- High contrast mode preference
- Language selection (English, German, Japanese, or "system")
- Widget preferences (selected list ID per widget instance)
- Saved search filters
- Reminder notification settings (sound, vibration)

**Design Pattern**:
```kotlin
class PreferencesRepository(private val dataStore: DataStore<Preferences>) {
    val highContrastMode: Flow<Boolean> = dataStore.data
        .map { prefs -> prefs[HIGH_CONTRAST_KEY] ?: false }

    suspend fun setHighContrastMode(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[HIGH_CONTRAST_KEY] = enabled
        }
    }
}
```

---

## Domain Models

### Core Data Classes

#### TaskItem
```kotlin
data class TaskItem(
    val id: String,
    val text: String,
    val order: Int,              // Lower = higher priority
    val done: Boolean,
    val removed: Boolean,
    val dueDate: Long?,           // Epoch milliseconds
    val reminderType: ReminderType,
    val createdAt: Long,
    val updatedAt: Long
)
```

**Design Notes**:
- `order`: Integer-based ordering where lower values = higher priority
- New tasks receive `minOfOrNull(order) - 1` to appear at top
- Order compaction prevents integer overflow in long-running lists

#### SearchFilter
```kotlin
data class SearchFilter(
    val query: String = "",
    val statusFilter: StatusFilter = StatusFilter.ALL,
    val dueDateRange: DueDateRange = DueDateRange.ALL,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null
)

enum class StatusFilter { ALL, ACTIVE, DONE, REMOVED }
enum class DueDateRange { ALL, OVERDUE, TODAY, THIS_WEEK, CUSTOM }
```

**Filter Logic**: Filters combine with AND logic
- Query AND Status AND DueDate

#### Language
```kotlin
data class Language(
    val code: String,        // "en", "de", "ja", "system"
    val displayName: String, // "English", "Deutsch", "日本語", "System Default"
    val flagEmoji: String    // "🇺🇸", "🇩🇪", "🇯🇵", "🌐"
)
```

---

## State Management

### ViewModel State Pattern

ViewModels expose immutable `StateFlow<UiState>` to the UI layer:

```kotlin
class ListViewModel(
    private val repo: TaskListRepository
) : ViewModel() {

    // Public state exposed to UI (read-only)
    val state: StateFlow<UiState> = combine(
        repo.observeTasks(DEFAULT_LIST_ID),
        searchQueryFlow,
        statusFilterFlow
    ) { tasks, query, statusFilter ->
        // Transform and filter tasks based on current filters
        UiState(
            tasks = applyFilters(tasks, query, statusFilter),
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UiState()
    )

    data class UiState(
        val tasks: List<TaskItem> = emptyList(),
        val isLoading: Boolean = true
    )
}
```

**Key Techniques**:
- **`combine()`**: Merge multiple Flow sources into single UiState
- **`stateIn()`**: Convert cold Flow to hot StateFlow
- **`WhileSubscribed(5000)`**: Keep flow active 5s after last subscriber (optimization)
- **Derived State**: UI state computed from repository emissions, not stored

### Error Handling Pattern

All ViewModels implement production-grade error handling:

```kotlin
private val _errorState = MutableStateFlow<String?>(null)
val errorState: StateFlow<String?> = _errorState.asStateFlow()

fun add(text: String) {
    viewModelScope.launch {
        try {
            repo.addTask(DEFAULT_LIST_ID, text.trim())
            _errorState.value = null  // Clear previous error on success
        } catch (e: Exception) {
            Log.e(TAG, "Error adding task", e)
            _errorState.value = "Failed to add task: ${e.message}"
        }
    }
}

fun clearError() {
    _errorState.value = null
}
```

**Benefits**:
- No app crashes from repository failures
- User-friendly error messages
- Errors can be dismissed by UI
- Logging for developer debugging

---

## Dependency Injection

### Koin Configuration

The app uses Koin 4.1.1 for dependency injection. Repository bindings delegate to `RepositoryProvider`, which is the single source of truth for auth-aware repository switching (Room for offline/unauthenticated, Firestore once authenticated).

**appModule** (`di/KoinModules.kt`):
```kotlin
val appModule = module {
    single<PreferencesRepository> { RepositoryProvider.getPreferencesRepository(androidContext()) }
    single<TaskListRepository> { RepositoryProvider.getRepository(androidContext()) }
    single { ReminderScheduler(androidContext()) }
    single { QRCodeGenerator() }
}
```

**ViewModel Injection**:
```kotlin
val viewModelModule = module {
    viewModel { parameters ->
        ListViewModel(
            savedStateHandle = parameters.get(),
            repo = get(),
            prefsRepo = get()
        )
    }
}
```

**Legacy Pattern** (still in use for some components):
```kotlin
object RepositoryProvider {
    private var taskRepo: TaskListRepository? = null

    fun setAuthenticated(authenticated: Boolean) {
        // Clear repository to force recreation
        taskRepo = null
    }

    fun getRepository(context: Context): TaskListRepository {
        return taskRepo ?: run {
            if (isAuthenticated) {
                FirestoreTaskListRepository(FirebaseFirestore.getInstance())
            } else {
                RoomTaskListRepository(AppDatabase.getInstance(context).taskDao())
            }
        }.also { taskRepo = it }
    }
}
```

**Worker Injection**: Both `ReminderWorker` and `DailyReminderWorker` are registered via `workerOf(::ReminderWorker)` and `workerOf(::DailyReminderWorker)` in the Koin `workerModule`, receiving their dependencies through Koin rather than `RepositoryProvider`.

---

## Database & Persistence

### Room Database

**Entity Definition** (`data/TaskEntity.kt`):
```kotlin
@Entity(
    tableName = "tasks",
    indices = [Index(value = ["listId", "order"])]
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val listId: String,
    val text: String,
    val order: Int,
    val done: Boolean,
    val removed: Boolean,
    val dueDate: Long?,
    val reminderType: String, // Enum.name
    val createdAt: Long,
    val updatedAt: Long
)
```

**DAO** (`data/TaskDao.kt`):
```kotlin
@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE listId = :listId ORDER BY order ASC")
    fun observeTasks(listId: String): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: String)

    @Query("UPDATE tasks SET `order` = :order WHERE id = :taskId")
    suspend fun updateOrder(taskId: String, order: Int)
}
```

**Database** (`data/AppDatabase.kt`):
```kotlin
@Database(
    entities = [TaskEntity::class],
    version = 1,
    exportSchema = true  // Exports schema to app/schemas/
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "taskcards_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
```

**Schema Export**: Room schemas exported to `app/schemas/` for migration tracking (configured in `build.gradle.kts`):
```kotlin
javaCompileOptions {
    annotationProcessorOptions {
        arguments += mapOf("room.schemaLocation" to "$projectDir/schemas")
    }
}
```

### DataStore for Preferences

**Preference Keys**:
```kotlin
private val HIGH_CONTRAST_KEY = booleanPreferencesKey("high_contrast_mode")
private val LANGUAGE_KEY = stringPreferencesKey("language")
private val DEFAULT_LIST_KEY = stringPreferencesKey("default_list_id")
```

**Flow-based Access**:
```kotlin
val languageFlow: Flow<String> = dataStore.data
    .catch { exception ->
        if (exception is IOException) {
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }
    .map { prefs -> prefs[LANGUAGE_KEY] ?: "system" }
```

---

## Background Processing

### WorkManager Architecture

TaskCards uses WorkManager for two primary background tasks:

#### 1. Task Reminders

**ReminderScheduler** (`worker/ReminderScheduler.kt`):
```kotlin
object ReminderScheduler {
    fun scheduleReminder(
        context: Context,
        taskId: String,
        taskText: String,
        dueDate: Long,
        reminderType: ReminderType
    ) {
        val reminderTime = calculateReminderTime(dueDate, reminderType)
        val delay = reminderTime - System.currentTimeMillis()

        if (delay <= 0) return  // Don't schedule past reminders

        val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    "taskId" to taskId,
                    "taskText" to taskText,
                    "dueDate" to dueDate
                )
            )
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "reminder_$taskId",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
    }

    fun cancelReminder(context: Context, taskId: String) {
        WorkManager.getInstance(context)
            .cancelUniqueWork("reminder_$taskId")
    }
}
```

**ReminderWorker** (`worker/ReminderWorker.kt`):
```kotlin
class ReminderWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    override fun doWork(): Result {
        val taskId = inputData.getString("taskId") ?: return Result.failure()
        val taskText = inputData.getString("taskText") ?: return Result.failure()
        val dueDate = inputData.getLong("dueDate", 0L)

        showNotification(taskId, taskText, dueDate)

        return Result.success()
    }

    private fun showNotification(taskId: String, taskText: String, dueDate: Long) {
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("taskcards://task/$taskId")
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, REMINDERS_CHANNEL_ID)
            .setContentTitle("Task Reminder")
            .setContentText(taskText)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(taskId.hashCode(), notification)
    }
}
```

**Notification Channels** (`worker/NotificationChannels.kt`):
```kotlin
object NotificationChannels {
    const val REMINDERS_CHANNEL_ID = "task_reminders"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                REMINDERS_CHANNEL_ID,
                "Task Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for task due dates and reminders"
                enableLights(true)
                enableVibration(true)
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
```

#### 2. Daily Reminders

Users can schedule a daily reminder at a specific time to be notified about their active tasks.

**DailyReminderScheduler** (`worker/DailyReminderScheduler.kt`):

```kotlin
object DailyReminderScheduler {
    const val WORK_NAME = "daily_reminder_work"

    fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
        val currentTime = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= currentTime) {
                add(Calendar.DAY_OF_MONTH, 1)  // Schedule for tomorrow
            }
        }
        val initialDelay = calendar.timeInMillis - currentTime

        val dailyWorkRequest = PeriodicWorkRequestBuilder<DailyReminderWorker>(
            24, TimeUnit.HOURS,
            15, TimeUnit.MINUTES  // 15-minute flex interval
        )
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                dailyWorkRequest
            )
    }

    fun cancelDailyReminder(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
```

**Key Implementation Details**:
- Uses `PeriodicWorkRequestBuilder` with 24-hour interval
- Calculates initial delay to reach target time (schedules next day if already passed)
- Uses `ExistingPeriodicWorkPolicy.UPDATE` to reschedule when time changes
- 15-minute flex interval allows WorkManager to batch with other periodic work

**DailyReminderWorker** (`worker/DailyReminderWorker.kt`):

```kotlin
class DailyReminderWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params), KoinComponent {

    private val preferencesRepository: PreferencesRepository by inject()
    private val taskListRepository: TaskListRepository by inject()

    override suspend fun doWork(): Result {
        val settings = prefsRepo.getSettings().first()

        if (!settings.remindersEnabled) {
            return Result.success()
        }
        val tasks = taskRepo.observeTasks(Constants.DEFAULT_LIST_ID).first()
        val activeTasks = tasks.count { !it.isDone && !it.isRemoved }

        if (activeTasks == 0) {
            return Result.success()  // No notification if no active tasks
        }

        showNotification(activeTasks, settings)
        return Result.success()
    }

    private fun showNotification(count: Int, settings: Settings) {
        val notification = NotificationCompat.Builder(
            applicationContext,
            NotificationChannels.REMINDERS_CHANNEL_ID
        )
            .setContentTitle("Daily Task Reminder")
            .setContentText("You have $count active task${if (count > 1) "s" else ""}")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .apply {
                if (settings.notificationSound) {
                    setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                }
                if (settings.notificationVibration) {
                    setVibrate(longArrayOf(0, 500, 200, 500))
                }
            }
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val WORK_NAME = "daily_reminder_work"
        const val NOTIFICATION_ID = 1000
    }
}
```

**Key Features**:
- Only shows notification if reminders enabled AND active tasks exist
- Respects user preferences for sound and vibration
- Uses distinct notification ID (1000) from task-specific reminders
- Counts active tasks (excludes done and removed)
- Falls back gracefully if repository operations fail

#### 3. Widget Updates

**WidgetUpdateWorker** (`worker/WidgetUpdateWorker.kt`):
```kotlin
class WidgetUpdateWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        // Update all widget instances
        updateTaskListWidgets()
        updateDueTodayWidgets()

        return Result.success()
    }

    private fun updateTaskListWidgets() {
        val glanceIds = GlanceAppWidgetManager(applicationContext)
            .getGlanceIds(TaskListWidget::class.java)

        glanceIds.forEach { glanceId ->
            TaskListWidget().update(applicationContext, glanceId)
        }
    }
}
```

**Periodic Scheduling**:
```kotlin
// Schedule periodic widget updates every 30 minutes
val workRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
    repeatInterval = 30,
    repeatIntervalTimeUnit = TimeUnit.MINUTES
)
    .setConstraints(
        Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()
    )
    .build()

WorkManager.getInstance(context)
    .enqueueUniquePeriodicWork(
        "widget_updates",
        ExistingPeriodicWorkPolicy.KEEP,
        workRequest
    )
```

---

## Widget Architecture

### Jetpack Glance Widgets

TaskCards implements three home screen widgets using Jetpack Glance (Compose for Widgets):

#### TaskListWidget

**Implementation** (`widget/TaskListWidget.kt`):
```kotlin
class TaskListWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<Preferences>()
            val listId = prefs[PreferencesKey("selected_list_id")] ?: DEFAULT_LIST_ID
            val tasks = loadTasks(listId).take(5)  // Show max 5 tasks

            TaskListWidgetContent(tasks)
        }
    }

    @Composable
    private fun TaskListWidgetContent(tasks: List<TaskItem>) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color.White)
                .padding(16.dp)
        ) {
            Text(
                text = "Tasks",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            tasks.forEach { task ->
                TaskWidgetItem(task)
            }
        }
    }

    @Composable
    private fun TaskWidgetItem(task: TaskItem) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable(
                    onClick = actionStartActivity(
                        Intent(context, MainActivity::class.java).apply {
                            data = Uri.parse("taskcards://task/${task.id}")
                        }
                    )
                )
        ) {
            CheckBox(
                checked = task.done,
                onCheckedChange = actionRunCallback<ToggleTaskAction>(
                    actionParametersOf(
                        ActionParameters.Key<String>("taskId") to task.id
                    )
                )
            )
            Text(text = task.text)
        }
    }
}
```

**Widget Interaction**:
- Widget actions are handled inline within widget composables using Glance action modifiers
- Task completion toggles trigger repository updates
- Widgets refresh automatically via GlanceAppWidget state management
- No separate WidgetActions file needed - actions integrated into widget components

#### QuickAddWidget

**Simplified Implementation**:
- Single text input field
- "Add" button
- Adds task to default list
- No configuration required

#### DueTodayWidget

**Data Source**:
```kotlin
suspend fun loadDueTodayTasks(): List<TaskItem> {
    val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000
    val endOfDay = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toEpochSecond() * 1000

    return repo.filterTasks(
        DEFAULT_LIST_ID,
        SearchFilter(
            dueDateRange = DueDateRange.TODAY,
            customStartDate = startOfDay,
            customEndDate = endOfDay
        )
    )
}
```

---

## Internationalization System

### Language Support Architecture

**Supported Languages**:
- English (en) - Default fallback
- German (de) - Deutsch
- Japanese (ja) - 日本語

**String Resources Structure**:
```
app/src/main/res/
├── values/strings.xml           # English (default)
├── values-de/strings.xml        # German translations
└── values-ja/strings.xml        # Japanese translations
```

**Total Strings**: 278+ fully translated across all languages

### Runtime Locale Switching

**LocaleHelper** (`util/LocaleHelper.kt`):
```kotlin
object LocaleHelper {
    fun setLocale(context: Context, languageCode: String): Context {
        val locale = getLocaleFromCode(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        return context.createConfigurationContext(config)
    }

    fun getLocaleFromCode(languageCode: String): Locale {
        return when (languageCode) {
            "en" -> Locale.ENGLISH
            "de" -> Locale.GERMAN
            "ja" -> Locale.JAPANESE
            "system" -> Resources.getSystem().configuration.locales[0]
            else -> Locale.ENGLISH  // Fallback
        }
    }

    fun getCurrentLanguageCode(context: Context): String {
        val currentLocale = context.resources.configuration.locales[0]
        return when (currentLocale.language) {
            "de" -> "de"
            "ja" -> "ja"
            "en" -> "en"
            else -> "system"
        }
    }
}
```

**MainActivity Integration**:
```kotlin
class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        // Apply locale before activity creation
        val prefs = PreferencesRepository(newBase)
        val languageCode = runBlocking { prefs.languageFlow.first() }
        val context = LocaleHelper.setLocale(newBase, languageCode)
        super.attachBaseContext(context)
    }
}
```

**Language Change Flow**:
1. User selects language in SettingsScreen
2. SettingsViewModel saves to DataStore: `prefsRepo.setLanguage(languageCode)`
3. ViewModel recreates activity: `activity.recreate()`
4. MainActivity.attachBaseContext() applies new locale
5. All strings display in new language immediately

**Locale Resolution Priority**:
1. User preference from DataStore (if set)
2. Device system locale (if "system" selected and supported)
3. English fallback (for unsupported locales)

---

## Build Configuration

### Gradle Configuration

**SDK Requirements** (`app/build.gradle.kts`):
```kotlin
android {
    compileSdk = 36

    defaultConfig {
        minSdk = 34        // Android 14+
        targetSdk = 36     // Latest Android version
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
}
```

**Note**: minSdk 34 is intentionally high to leverage latest Android APIs and simplify development. For wider distribution, consider lowering to minSdk 28 (Android 9).

### Build Variants

**Release Build** (optimized):
```kotlin
buildTypes {
    release {
        isMinifyEnabled = true           // Enable R8/ProGuard
        isShrinkResources = true         // Remove unused resources
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
        signingConfig = signingConfigs.getByName("release")

        // Firebase Crashlytics mapping file upload
        configure<CrashlyticsExtension> {
            mappingFileUploadEnabled = true
        }
    }
}
```

**Debug Build** (unobfuscated):
```kotlin
buildTypes {
    debug {
        isMinifyEnabled = false
        isDebuggable = true
        applicationIdSuffix = ".debug"  // Allows debug and release side-by-side
    }
}
```

### Signing Configuration

**Environment Variable-Based Signing**:
```kotlin
signingConfigs {
    create("release") {
        storeFile = file(System.getenv("RELEASE_KEYSTORE_PATH") ?: "../release.keystore")
        storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
        keyAlias = System.getenv("RELEASE_KEY_ALIAS")
        keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
    }
}
```

**GitHub Actions Usage**:
```yaml
env:
  RELEASE_KEYSTORE_PATH: ${{ github.workspace }}/release.keystore
  RELEASE_KEYSTORE_PASSWORD: ${{ secrets.RELEASE_KEYSTORE_PASSWORD }}
  RELEASE_KEY_ALIAS: ${{ secrets.RELEASE_KEY_ALIAS }}
  RELEASE_KEY_PASSWORD: ${{ secrets.RELEASE_KEY_PASSWORD }}
```

### Dependencies (Key Libraries)

**Compose BOM** (Bill of Materials):
```kotlin
implementation(platform("androidx.compose:compose-bom:2024.10.01"))
```
This ensures all Compose libraries use compatible versions.

**Critical Dependencies**:
- **Kotlin**: 2.0.21 with Compose Compiler Plugin
- **Navigation**: navigation-compose 2.8.3
- **Coroutines**: 1.9.0
- **Room**: room-runtime-ktx 2.6.1
- **Koin**: koin-android 4.1.1, koin-androidx-compose, koin-androidx-workmanager
- **WorkManager**: work-runtime-ktx 2.9.0
- **Glance**: glance-appwidget 1.1.0, glance-material3 1.1.0
- **Firebase BOM**: firebase-bom 33.5.1
- **ZXing**: core 3.5.3
- **Kotest**: 5.9.1 (testing)

---

## ProGuard & Code Optimization

### ProGuard Rules Rationale

The `app/proguard-rules.pro` file contains 159 lines of carefully crafted rules to prevent R8/ProGuard from breaking the app in release builds.

#### Data Classes Preservation

**Why**: Reflection, serialization, and Firestore require class structure intact.

```proguard
-keep class de.hipp.app.taskcards.model.** { *; }
-keep class de.hipp.android.taskcards.model.** { *; }  # Legacy package support
```

**Affected Classes**: TaskItem, SearchFilter, Language, ReminderType, DueDateStatus, etc.

#### Compose Preservation

**Why**: Compose runtime uses reflection to invoke @Composable functions.

```proguard
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.material3.** { *; }
```

**Impact**: Without these rules, all @Composable functions would be removed → blank screens.

#### Coroutines & Flow

**Why**: Coroutines internals use reflection and volatile fields for thread safety.

```proguard
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keep class kotlinx.coroutines.flow.** { *; }
-keep class kotlinx.coroutines.flow.StateFlow { *; }
-keep class kotlinx.coroutines.flow.SharedFlow { *; }
```

**Impact**: Without these rules, Flow operators might be removed → data not flowing to UI.

#### WorkManager

**Why**: WorkManager uses reflection to instantiate Worker classes.

```proguard
-keep class androidx.work.** { *; }
-keep class de.hipp.app.taskcards.worker.** { *; }
```

**Affected Classes**: ReminderWorker, WidgetUpdateWorker

#### Room Database

**Why**: Room generates code at compile-time that references entities and DAOs by name.

```proguard
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep class de.hipp.android.taskcards.data.TaskEntity { *; }
-keep class de.hipp.android.taskcards.data.TaskDao { *; }
-keep class de.hipp.android.taskcards.data.AppDatabase { *; }
```

#### Glance Widgets

**Why**: Glance uses reflection to discover and instantiate widgets.

```proguard
-keep class androidx.glance.** { *; }
-keep class de.hipp.app.taskcards.widget.** { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidget { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }
```

#### Logging Removal

**Why**: Remove debug logging in release builds for performance and security.

```proguard
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
```

**Keeps**: `Log.e()` and `Log.w()` for production error tracking.

### R8 Full Mode

The app uses R8 with "full mode" optimization (`proguard-android-optimize.txt`):
- More aggressive optimization than default ProGuard
- Inlines methods, removes unused code paths
- Smaller APK size (~30% reduction typical)
- Requires comprehensive keep rules to prevent breakage

---

## Testing Architecture

### Test Organization

**JVM Tests** (`app/src/test/`):
- Pure Kotlin logic (no Android dependencies)
- ViewModels, repositories, integration tests
- Fast execution (~2-3 seconds for all tests)

**Instrumented Tests** (`app/src/androidTest/`):
- Requires Android Context
- Room database, DataStore, Compose UI
- Slower execution (30-60 seconds)

### Kotest Framework

**Why Kotest over JUnit4**:
- More expressive test names (strings instead of function names)
- Better readability with StringSpec style
- Idiomatic Kotlin assertions
- Test lifecycle hooks (beforeTest, afterTest)

**Example Test**:
```kotlin
class InMemoryTaskListRepositoryTest : StringSpec({
    lateinit var repo: InMemoryTaskListRepository

    beforeTest {
        repo = InMemoryTaskListRepository()
    }

    "adding a task should emit it in observeTasks flow" {
        val listId = "test-list"
        val tasks = mutableListOf<List<TaskItem>>()

        val job = launch {
            repo.observeTasks(listId).collect { tasks.add(it) }
        }

        repo.addTask(listId, "Test task")
        advanceUntilIdle()

        tasks.last() shouldHaveSize 1
        tasks.last().first().text shouldBe "Test task"

        job.cancel()
    }

    "moving task to invalid index should ignore the move" {
        val listId = "test-list"
        repo.addTask(listId, "Task 1")
        val taskId = repo.observeTasks(listId).first().first().id

        repo.moveTask(listId, taskId, -1)  // Invalid index
        repo.moveTask(listId, taskId, 999) // Out of bounds

        // Task should remain at original position
        val tasks = repo.observeTasks(listId).first()
        tasks.first().id shouldBe taskId
    }
})
```

### Test Coverage

**Repository Tests** (19 tests):
- InMemoryTaskListRepositoryTest.kt (19 tests)
- RoomTaskListRepositoryTest.kt (17 instrumented tests)
- PreferencesRepositoryTest.kt (5 instrumented tests)

**ViewModel Tests** (46 tests):
- CardsViewModelTest.kt (2 tests)
- CardsViewModelErrorHandlingTest.kt (6 tests)
- ListViewModelTest.kt (2 tests)
- ListViewModelErrorHandlingTest.kt (8 tests)
- ListViewModelUnicodeTest.kt (6 tests)
- SettingsViewModelTest.kt (14 tests)
- SettingsViewModelErrorHandlingTest.kt (10 tests)

**UI Tests** (6 instrumented tests):
- CardsScreenBasicTest.kt (2 tests)
- ListScreenBasicTest.kt (3 tests)
- SettingsScreenBasicTest.kt (1 test)

**Integration Tests**:
- MultiListIntegrationTest.kt
- TaskLifecycleIntegrationTest.kt

**Total**: 63+ tests with comprehensive coverage

### Coroutine Testing

**Test Dispatcher Injection**:
```kotlin
class ListViewModel(
    private val repo: TaskListRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : ViewModel() {

    fun add(text: String) {
        viewModelScope.launch(dispatcher) {
            repo.addTask(DEFAULT_LIST_ID, text.trim())
        }
    }
}

// In tests:
val testDispatcher = StandardTestDispatcher()
val viewModel = ListViewModel(mockRepo, testDispatcher)
viewModel.add("Test")
advanceUntilIdle()  // Process all pending coroutines
```

---

## Performance Considerations

### Recomposition Optimization

**Derived State with `derivedStateOf`**:
```kotlin
@Composable
fun TaskList(tasks: List<TaskItem>) {
    // Only recompute filteredTasks if tasks list changes
    val filteredTasks by remember(tasks) {
        derivedStateOf {
            tasks.filter { !it.removed }
        }
    }

    LazyColumn {
        items(filteredTasks, key = { it.id }) { task ->
            TaskItem(task)
        }
    }
}
```

**Key Usage in LazyColumn**:
- Always provide `key` parameter to preserve scroll position
- Enables efficient item reordering animations
- Prevents unnecessary recompositions

### Flow Optimization

**WhileSubscribed with Timeout**:
```kotlin
val state = repo.observeTasks(DEFAULT_LIST_ID)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),  // 5s timeout
        initialValue = emptyList()
    )
```

**Benefit**: Flow stays active 5 seconds after last subscriber, avoiding repeated cold starts.

### Memory Management

**List Cleanup**:
```kotlin
// Repository interface
suspend fun clearList(listId: String)  // Free memory for unused lists
suspend fun getActiveListCount(): Int  // Debug memory usage
```

**Usage**: Call `clearList()` when user navigates away from list, or after syncing to Firestore.

---

## Security & Privacy

### Data Security

**Local Storage**:
- Room database: Unencrypted (standard Android practice)
- DataStore preferences: Unencrypted
- For sensitive data, consider [EncryptedSharedPreferences](https://developer.android.com/reference/androidx/security/crypto/EncryptedSharedPreferences)

**Cloud Storage (Firestore)**:
- Data encrypted in transit (TLS)
- Data encrypted at rest (Google-managed keys)
- Security rules enforce authentication requirement

### Firestore Security Rules

**Current Rules** (`firestore.rules`):
```javascript
rules_version = '2';

service cloud.firestore {
  match /databases/{database}/documents {

    function isAuthenticated() {
      return request.auth != null;
    }

    match /lists/{listId} {
      allow read, write: if isAuthenticated();

      match /tasks/{taskId} {
        allow read, write: if isAuthenticated();
      }

      // Recursive wildcard for any subcollection
      match /{document=**} {
        allow read, write: if isAuthenticated();
      }
    }

    // Deny all other access
    match /{document=**} {
      allow read, write: if false;
    }
  }
}
```

**Security Model**: Collaborative lists (any authenticated user can access any list). For user isolation, see [FIRESTORE_RULES_DEPLOYMENT.md](FIRESTORE_RULES_DEPLOYMENT.md).

### Google Sign-In

**Configuration Requirements**:
- SHA-1 fingerprint added to Firebase Console
- OAuth clients: Android (type 1) AND Web (type 3)
- `google-services.json` with real Firebase credentials

**Common Issues**:
- "Unknown calling package name" → SHA-1 fingerprint missing
- "Sign-in failed" → Check OAuth client configuration

---

## Deep Dive: Key Implementations

### Task Ordering System

**Problem**: How to efficiently reorder tasks without renumbering entire list?

**Solution**: Integer-based ordering where lower values = higher priority.

**Implementation**:
```kotlin
// Adding new task at top
val currentTasks = repo.observeTasks(listId).first()
val minOrder = currentTasks.minOfOrNull { it.order } ?: 0
val newOrder = minOrder - 1

repo.addTask(listId, text, order = newOrder)
```

**Order Compaction** (prevents integer overflow):
```kotlin
suspend fun moveTask(listId: String, taskId: String, toIndex: Int) {
    val tasks = observeTasks(listId).first().toMutableList()
    val taskIndex = tasks.indexOfFirst { it.id == taskId }

    if (taskIndex < 0 || toIndex < 0 || toIndex >= tasks.size) return

    // Move task in list
    val task = tasks.removeAt(taskIndex)
    tasks.add(toIndex, task)

    // Compact orders: reassign sequential values
    tasks.forEachIndexed { index, t ->
        val newOrder = -index  // 0, -1, -2, -3, ...
        if (t.order != newOrder) {
            updateTask(t.copy(order = newOrder))
        }
    }
}
```

**Benefit**: Moves remain O(1) database writes, no cascading updates.

### Search & Filtering Implementation

**Filter Combination Logic**:
```kotlin
suspend fun filterTasks(listId: String, filter: SearchFilter): List<TaskItem> {
    var tasks = observeTasks(listId).first()

    // 1. Text search
    if (filter.query.isNotBlank()) {
        tasks = tasks.filter {
            it.text.contains(filter.query, ignoreCase = true)
        }
    }

    // 2. Status filter
    tasks = when (filter.statusFilter) {
        StatusFilter.ACTIVE -> tasks.filter { !it.done && !it.removed }
        StatusFilter.DONE -> tasks.filter { it.done }
        StatusFilter.REMOVED -> tasks.filter { it.removed }
        StatusFilter.ALL -> tasks
    }

    // 3. Due date filter
    tasks = when (filter.dueDateRange) {
        DueDateRange.OVERDUE -> tasks.filter {
            it.dueDate != null && it.dueDate < System.currentTimeMillis()
        }
        DueDateRange.TODAY -> tasks.filter {
            it.dueDate?.isToday() == true
        }
        DueDateRange.THIS_WEEK -> tasks.filter {
            it.dueDate?.isThisWeek() == true
        }
        DueDateRange.CUSTOM -> tasks.filter {
            it.dueDate != null
                && it.dueDate >= filter.customStartDate!!
                && it.dueDate <= filter.customEndDate!!
        }
        DueDateRange.ALL -> tasks
    }

    return tasks
}
```

**Filter Persistence**:
```kotlin
// Save filter as saved search
data class SavedSearch(
    val id: String,
    val name: String,
    val filter: SearchFilter,
    val createdAt: Long
)

suspend fun saveSearch(name: String, filter: SearchFilter) {
    val savedSearch = SavedSearch(
        id = UUID.randomUUID().toString(),
        name = name,
        filter = filter,
        createdAt = System.currentTimeMillis()
    )
    prefsRepo.addSavedSearch(savedSearch)
}
```

---

## Accessibility Implementation

For complete accessibility documentation, see [ACCESSIBILITY.md](../ACCESSIBILITY.md).

### Key Accessibility Features

**Semantic Properties**:
```kotlin
// CardsScreen.kt - Task card with custom action
Card(
    modifier = Modifier.semantics {
        contentDescription = "Task card: ${task.text}"
        role = Role.Button
        customActions = listOf(
            CustomAccessibilityAction("Complete task") {
                onComplete()
                true
            }
        )
    }
)
```

**Focus Indicators** (2dp border, WCAG compliant):
```kotlin
fun Modifier.focusIndicator(
    focusColor: Color = MaterialTheme.colorScheme.primary,
    focusWidth: Dp = 2.dp,
    shape: Shape = RoundedCornerShape(8.dp)
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    this
        .focusable(interactionSource = interactionSource)
        .then(
            if (isFocused) {
                Modifier.border(focusWidth, focusColor, shape)
            } else {
                Modifier
            }
        )
}
```

**Touch Targets** (48×48 dp minimum):
```kotlin
// Dimensions.kt
object Dimensions {
    val MinTouchTarget = 48.dp  // WCAG 2.1 Level AA compliant
}

// Usage
Checkbox(
    modifier = Modifier.sizeIn(
        minWidth = Dimensions.MinTouchTarget,
        minHeight = Dimensions.MinTouchTarget
    )
)
```

---

## Release Process

For complete release documentation, see:
- [docs/release-setup.md](release-setup.md) - Full setup guide
- [docs/release-quick-start.md](release-quick-start.md) - Quick start

### Release Workflow Summary

1. **Local Build**: `./gradlew assembleRelease`
2. **GitHub Actions**: Manual workflow trigger
3. **Dry Run Mode**: Test build without publishing (default: enabled)
4. **Publish**: Disable dry run to publish to Play Store
5. **Tracks**: internal → alpha → beta → production

**Version Management**:
- Version Code: Integer, must increment (1, 2, 3, ...)
- Version Name: Semantic version (1.0.0, 1.1.0, 2.0.0)

---

## Future Enhancements

### Potential Improvements

1. **Subtasks**: Nested task hierarchy
2. **Recurring Tasks**: Daily, weekly, monthly recurrence
3. **Task Templates**: Reusable task structures
4. **Collaboration**: Real-time multi-user editing
5. **Attachment Support**: Images, files, notes
6. **Dark Mode Improvements**: More granular theme controls
7. **Sync Conflict Resolution**: Better offline→online merge
8. **Export/Import**: Backup to JSON or CSV
9. **Task Statistics**: Completion rates, time tracking
10. **Calendar Integration**: Sync with Google Calendar

### Architecture Evolution

1. **Multi-Module**: Split into :app, :data, :domain, :ui modules
2. **Clean Architecture**: Strict use cases layer
3. **RepositoryProvider Cleanup**: Remove legacy RepositoryProvider and move repository lifecycle fully into Koin modules
4. **Offline-First with Sync Queue**: Better offline handling
5. **Pagination**: For very large task lists (1000+ tasks)

---

## Additional Resources

### Official Documentation
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
- [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
- [Jetpack Glance](https://developer.android.com/jetpack/compose/glance)
- [Firebase Firestore](https://firebase.google.com/docs/firestore)
- [Koin Dependency Injection](https://insert-koin.io/docs/quickstart/android)

### Testing Resources
- [Kotest Documentation](https://kotest.io/)
- [Compose Testing](https://developer.android.com/jetpack/compose/testing)
- [Coroutine Testing](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-test/)

### Design Resources
- [Material Design 3](https://m3.material.io/)
- [Android Accessibility](https://developer.android.com/guide/topics/ui/accessibility)
- [WCAG 2.1 Guidelines](https://www.w3.org/WAI/WCAG21/quickref/)

---

**Document Maintenance**: This document should be updated whenever architectural changes are made. See [CLAUDE.md Documentation Maintenance](../CLAUDE.md#documentation-maintenance) section for guidelines.

**Contact**: For technical questions or clarifications, please open a GitHub issue with the "documentation" label.

---

*This document is part of the TaskCards project documentation suite. Last updated: November 11, 2025.*

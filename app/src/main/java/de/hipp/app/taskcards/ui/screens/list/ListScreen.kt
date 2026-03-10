package de.hipp.app.taskcards.ui.screens.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.hipp.app.taskcards.R
import de.hipp.app.taskcards.data.TaskListRepository
import de.hipp.app.taskcards.data.preferences.PreferencesRepository
import de.hipp.app.taskcards.di.StringProvider
import de.hipp.app.taskcards.model.StatusFilter
import de.hipp.app.taskcards.ui.screens.ActiveFilterChips
import de.hipp.app.taskcards.ui.screens.ShareDialog
import de.hipp.app.taskcards.ui.screens.filter.FilterBottomSheet
import de.hipp.app.taskcards.ui.viewmodel.ListViewModel
import de.hipp.app.taskcards.ui.viewmodel.ShareViewModel
import de.hipp.app.taskcards.ui.viewmodel.factoryOf
import de.hipp.app.taskcards.ui.viewmodel.list.clearAllFilters
import de.hipp.app.taskcards.ui.viewmodel.list.deleteSavedSearch
import de.hipp.app.taskcards.ui.viewmodel.list.saveCurrentSearch
import de.hipp.app.taskcards.ui.viewmodel.list.setDueDateRange
import de.hipp.app.taskcards.ui.viewmodel.list.setSearchQuery
import de.hipp.app.taskcards.ui.viewmodel.list.setStatusFilter
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ListScreen(
    listId: String,
    onLoadList: (String) -> Unit,
    onNavigateToListSelector: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val repo: TaskListRepository = koinInject()
    val prefsRepo: PreferencesRepository = koinInject()
    val strings: StringProvider = koinInject()
    val vm: ListViewModel = viewModel(
        key = "ListVM-$listId",
        factory = factoryOf { ListViewModel(listId, repo, prefsRepo, strings) }
    )
    val shareVm: ShareViewModel = koinViewModel()

    val state by vm.state.collectAsState()
    val errorState by vm.errorState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val shareState by shareVm.state.collectAsState()

    var showFilterSheet by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }

    // Show error snackbar when error occurs
    LaunchedEffect(errorState) {
        errorState?.let { errorMessage ->
            snackbarHostState.showSnackbar(
                message = errorMessage,
                actionLabel = context.getString(R.string.action_dismiss),
                duration = SnackbarDuration.Short
            )
            vm.clearError()
        }
    }

    var showQRScanner by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    actionColor = MaterialTheme.colorScheme.error,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    ) { paddingValues ->
    Box(modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)) {
        // Gradient background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Modern header with stats
            ListHeader(
                listId = listId,
                taskCount = state.tasks.count { !it.removed },
                doneCount = state.tasks.count { it.done && !it.removed },
                hasActiveFilters = !state.searchFilter.isEmpty(),
                onOpenFilter = { showFilterSheet = true },
                onNavigateToListSelector = onNavigateToListSelector,
                onShareList = {
                    shareVm.generateListIdShareUrl(listId)
                    showShareDialog = true
                }
            )

            // Active filter chips
            ActiveFilterChips(
                filter = state.searchFilter,
                onClearDueDateRange = { vm.setDueDateRange(null) },
                onClearStatusFilter = { vm.setStatusFilter(StatusFilter.ACTIVE_ONLY) },
                onClearAll = vm::clearAllFilters
            )

            Spacer(Modifier.height(16.dp))

            // Enhanced add item input
            AddItemRow(
                onAdd = vm::add,
                onLoadList = { showQRScanner = true }
            )

            Spacer(Modifier.height(16.dp))

            TaskList(
                displayTasks = state.tasks,
                vm = vm
            )
        }
    }
    }

    if (showQRScanner) {
        QRScannerScreen(
            onQRCodeScanned = { scannedValue ->
                showQRScanner = false
                // Pass the full scanned URL to be processed by deep link handler
                // Supported formats:
                // - taskcards://list/{listId} (new, path-based)
                // - taskcards://list?data=... (legacy, query-based)
                if (scannedValue.isNotBlank()) {
                    onLoadList(scannedValue)
                }
            },
            onDismiss = { showQRScanner = false }
        )
    }

    if (showFilterSheet) {
        FilterBottomSheet(
            currentFilter = state.searchFilter,
            savedSearches = state.savedSearches,
            onFilterChanged = { newFilter ->
                vm.setSearchQuery(newFilter.textQuery)
                vm.setStatusFilter(newFilter.statusFilter)
                vm.setDueDateRange(newFilter.dueDateRange)
            },
            onSaveSearch = vm::saveCurrentSearch,
            onApplySavedSearch = { saved ->
                vm.setSearchQuery(saved.filter.textQuery)
                vm.setStatusFilter(saved.filter.statusFilter)
                vm.setDueDateRange(saved.filter.dueDateRange)
            },
            onDeleteSavedSearch = { searchId ->
                vm.deleteSavedSearch(searchId)
            },
            onDismiss = { showFilterSheet = false }
        )
    }

    if (showShareDialog) {
        ShareDialog(
            shareUrl = shareState.shareUrl,
            qrCodeBitmap = shareState.qrCodeBitmap,
            isGeneratingQR = shareState.isGeneratingQR,
            onGenerateQR = shareVm::generateQRCode,
            onDismiss = {
                showShareDialog = false
                shareVm.clearShareState()
            }
        )
    }
}

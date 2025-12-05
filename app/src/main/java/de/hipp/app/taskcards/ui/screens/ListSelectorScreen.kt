package de.hipp.app.taskcards.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.hipp.app.taskcards.R
import de.hipp.app.taskcards.di.RepositoryProvider
import de.hipp.app.taskcards.model.TaskList
import de.hipp.app.taskcards.ui.screens.listselector.EmptyListsState
import de.hipp.app.taskcards.ui.screens.listselector.ListCard
import de.hipp.app.taskcards.ui.screens.listselector.dialogs.CreateListDialog
import de.hipp.app.taskcards.ui.screens.listselector.dialogs.DeleteListDialog
import de.hipp.app.taskcards.ui.screens.listselector.dialogs.RenameListDialog
import de.hipp.app.taskcards.ui.theme.BrandPurple
import de.hipp.app.taskcards.ui.viewmodel.ListSelectorViewModel
import de.hipp.app.taskcards.ui.viewmodel.factoryOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListSelectorScreen(
    onListSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val metadataRepo = remember { RepositoryProvider.getMetadataRepository(context) }
    val taskRepo = remember { RepositoryProvider.getRepository(context) }

    val vm: ListSelectorViewModel = viewModel(
        factory = factoryOf { ListSelectorViewModel(metadataRepo, taskRepo) }
    )

    val state by vm.state.collectAsState()
    val errorState by vm.errorState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showCreateDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf<TaskList?>(null) }
    var showDeleteDialog by remember { mutableStateOf<TaskList?>(null) }

    // Show error snackbar
    LaunchedEffect(errorState) {
        errorState?.let { error ->
            snackbarHostState.showSnackbar(error)
            vm.clearError()
        }
    }

    // Gradient background
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surface
        )
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = BrandPurple,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.semantics {
                    contentDescription = context.getString(R.string.list_selector_create_fab_description)
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.list_selector_create_button)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
                .padding(paddingValues)
        ) {
            if (state.lists.isEmpty()) {
                EmptyListsState()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(state.lists, key = { it.list.id }) { listWithCount ->
                        ListCard(
                            list = listWithCount.list,
                            taskCount = listWithCount.taskCount,
                            onClick = { onListSelected(listWithCount.list.id) },
                            onLongClick = { showRenameDialog = listWithCount.list },
                            onDelete = { showDeleteDialog = listWithCount.list }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp)) // Space for FAB
                    }
                }
            }
        }
    }

    // Create List Dialog
    if (showCreateDialog) {
        CreateListDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                vm.createList(name) { listId ->
                    showCreateDialog = false
                    onListSelected(listId)
                }
            }
        )
    }

    // Rename List Dialog
    showRenameDialog?.let { list ->
        RenameListDialog(
            currentName = list.name,
            onDismiss = { showRenameDialog = null },
            onRename = { newName ->
                vm.renameList(list.id, newName)
                showRenameDialog = null
            }
        )
    }

    // Delete List Dialog
    showDeleteDialog?.let { list ->
        DeleteListDialog(
            listName = list.name,
            onDismiss = { showDeleteDialog = null },
            onConfirm = {
                vm.deleteList(list.id)
                showDeleteDialog = null
            }
        )
    }
}

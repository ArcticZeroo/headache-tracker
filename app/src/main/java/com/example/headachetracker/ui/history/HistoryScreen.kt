package com.example.headachetracker.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.headachetracker.ui.components.HeadacheEntryCard
import com.example.headachetracker.ui.entry.EditEntryScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onEditEntry: (Long) -> Unit,
    isExpanded: Boolean = false,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val groups by viewModel.groupedEntries.collectAsState()
    var selectedEntryId by remember { mutableStateOf<Long?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.undoEvent.collect { deletedEntry ->
            val result = snackbarHostState.showSnackbar(
                message = "Entry deleted",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoDelete(deletedEntry)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { scaffoldPadding ->
        Box(modifier = Modifier.padding(scaffoldPadding)) {
            if (groups.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No entries yet",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Use the widget or Quick Log tab\nto record your first headache",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (isExpanded) {
                Row(modifier = Modifier.fillMaxSize()) {
                    EntryList(
                        groups = groups,
                        onEdit = { id -> selectedEntryId = id },
                        onDelete = { viewModel.deleteEntry(it) },
                        modifier = Modifier
                            .weight(0.45f)
                            .fillMaxHeight()
                    )
                    VerticalDivider(modifier = Modifier.fillMaxHeight())
                    Box(
                        modifier = Modifier
                            .weight(0.55f)
                            .fillMaxHeight()
                    ) {
                        val entryId = selectedEntryId
                        if (entryId != null) {
                            EditEntryScreen(
                                entryId = entryId,
                                onNavigateBack = { selectedEntryId = null }
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Select an entry to edit",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else {
                EntryList(
                    groups = groups,
                    onEdit = onEditEntry,
                    onDelete = { viewModel.deleteEntry(it) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryList(
    groups: List<DayGroup>,
    onEdit: (Long) -> Unit,
    onDelete: (com.example.headachetracker.data.local.HeadacheEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        groups.forEach { group ->
            item(key = "header_${group.dateLabel}") {
                Text(
                    text = group.dateLabel,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(
                items = group.entries,
                key = { it.id }
            ) { entry ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { dismissValue ->
                        if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                            onDelete(entry)
                            true
                        } else false
                    }
                )

                SwipeToDismissBox(
                    state = dismissState,
                    enableDismissFromStartToEnd = false,
                    backgroundContent = {},
                    content = {
                        HeadacheEntryCard(
                            entry = entry,
                            onEdit = { onEdit(entry.id) },
                            onDelete = { onDelete(entry) }
                        )
                    }
                )
            }
        }
    }
}

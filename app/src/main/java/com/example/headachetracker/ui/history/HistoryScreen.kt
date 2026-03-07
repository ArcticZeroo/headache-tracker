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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.headachetracker.data.local.HeadacheEntry
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
    val dayContexts by viewModel.dayContexts.collectAsState()
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
                        dayContexts = dayContexts,
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
                    dayContexts = dayContexts,
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
    dayContexts: Map<String, DayContext>,
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
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        text = group.dateLabel,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    val context = dayContexts[group.dateLabel]
                    if (context != null) {
                        DayContextRow(context)
                    }
                }
            }

            items(
                items = group.entries,
                key = { it.id }
            ) { entry ->
                SwipeableEntryItem(
                    entry = entry,
                    onEdit = { onEdit(entry.id) },
                    onDelete = { onDelete(entry) }
                )
            }
        }
    }
}

@Composable
private fun DayContextRow(context: DayContext) {
    val hasSomething = context.highTemp != null || context.rainMm != null ||
            context.steps != null || context.sleepHours != null
    if (!hasSomething) return

    Row(
        modifier = Modifier.padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (context.highTemp != null && context.lowTemp != null) {
            val highF = context.highTemp * 9.0 / 5.0 + 32.0
            val lowF = context.lowTemp * 9.0 / 5.0 + 32.0
            ContextItem(icon = Icons.Default.Thermostat, label = "${lowF.toInt()}–${highF.toInt()}°F")
        }
        if (context.rainMm != null && context.rainMm > 0) {
            ContextItem(icon = Icons.Default.WaterDrop, label = "${String.format("%.1f", context.rainMm)}mm")
        }
        if (context.steps != null && context.steps > 0) {
            ContextItem(icon = Icons.AutoMirrored.Filled.DirectionsWalk, label = "${String.format("%,d", context.steps)} steps")
        }
        if (context.sleepHours != null && context.sleepHours > 0) {
            ContextItem(icon = Icons.Default.Bedtime, label = "${String.format("%.1f", context.sleepHours)}h sleep")
        }
    }
}

@Composable
private fun ContextItem(icon: ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableEntryItem(
    entry: HeadacheEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {},
        content = {
            HeadacheEntryCard(entry = entry, onEdit = onEdit, onDelete = onDelete)
        }
    )
}

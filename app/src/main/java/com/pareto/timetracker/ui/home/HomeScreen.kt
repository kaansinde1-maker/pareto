package com.pareto.timetracker.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pareto.timetracker.R
import com.pareto.timetracker.ui.timer.DiscardTimerDialog
import com.pareto.timetracker.ui.timer.rememberTimerStartGate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenArchive: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val cards by viewModel.cards.collectAsStateWithLifecycle()
    val activeTimer by viewModel.activeTimer.collectAsStateWithLifecycle()
    val editor by viewModel.editor.collectAsStateWithLifecycle()
    val pendingRating by viewModel.pendingRating.collectAsStateWithLifecycle()

    var editMode by rememberSaveable { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    val startGate = rememberTimerStartGate { viewModel.toggleTimer(it) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenArchive) {
                        Icon(
                            imageVector = Icons.Outlined.Archive,
                            contentDescription = stringResource(R.string.desc_archive),
                        )
                    }
                    TextButton(onClick = { editMode = !editMode }) {
                        Text(stringResource(if (editMode) R.string.home_done else R.string.home_edit))
                    }
                },
            )
        },
        bottomBar = {
            AnimatedVisibility(visible = pendingRating != null) {
                FocusRatingBar(selected = pendingRating?.selected, onRate = viewModel::rate)
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::openNew) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.desc_add_category),
                )
            }
        },
    ) { padding ->
        val list = cards
        when {
            list == null -> Box(Modifier.fillMaxSize().padding(padding))
            list.isEmpty() -> EmptyState(Modifier.fillMaxSize().padding(padding))
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(list, key = { it.category.id }) { ui ->
                    val running = activeTimer?.takeIf { it.categoryId == ui.category.id }
                    CategoryCard(
                        category = ui.category,
                        todayMs = ui.todayMs,
                        runningStartedAt = running?.startedAt,
                        editMode = editMode,
                        onClick = {
                            when {
                                editMode -> viewModel.openEdit(ui.category)
                                running != null -> viewModel.toggleTimer(ui.category.id) // durdur
                                else -> startGate(ui.category.id)
                            }
                        },
                        onDiscard = { showDiscardDialog = true },
                    )
                }
            }
        }
    }

    editor?.let { state ->
        CategoryEditorSheet(
            state = state,
            onNameChange = viewModel::onNameChange,
            onTagChange = viewModel::onTagChange,
            onColorChange = viewModel::onColorChange,
            onSave = viewModel::save,
            onRestoreConflicting = viewModel::restoreConflicting,
            onArchive = viewModel::archiveEditing,
            onDismiss = viewModel::dismissEditor,
        )
    }

    if (showDiscardDialog) {
        DiscardTimerDialog(
            onConfirm = {
                viewModel.discardTimer()
                showDiscardDialog = false
            },
            onDismiss = { showDiscardDialog = false },
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.home_empty_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.home_empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

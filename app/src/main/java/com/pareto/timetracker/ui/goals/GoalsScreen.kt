package com.pareto.timetracker.ui.goals

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pareto.timetracker.R
import com.pareto.timetracker.domain.GoalPeriod

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(viewModel: GoalsViewModel = hiltViewModel()) {
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val editor by viewModel.editor.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_goals)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::openNew) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.desc_add_goal))
            }
        },
    ) { padding ->
        val list = goals
        when {
            list == null -> Box(Modifier.fillMaxSize().padding(padding))
            list.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.goals_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.goals_empty_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                GoalPeriod.entries.forEach { period ->
                    val inPeriod = list.filter { it.goal.period == period }
                    if (inPeriod.isNotEmpty()) {
                        item(key = "header-${period.name}") {
                            Text(
                                text = stringResource(period.labelRes()),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                        items(inPeriod, key = { it.goal.id }) { ui ->
                            GoalCard(ui = ui, onClick = { viewModel.openEdit(ui) })
                        }
                    }
                }
            }
        }
    }

    editor?.let { state ->
        // Düzenlenen hedefin kategorisi arşivdeyse de seçili görünsün; yeni hedefte sadece aktifler.
        val choices = remember(allCategories, state.categoryId) {
            allCategories
                .filter { !it.isArchived || it.id == state.categoryId }
                .sortedBy { it.createdAt }
        }
        GoalEditorSheet(
            state = state,
            categories = choices,
            onScopeType = viewModel::onScopeType,
            onTag = viewModel::onTag,
            onCategory = viewModel::onCategory,
            onPeriod = viewModel::onPeriod,
            onType = viewModel::onType,
            onHours = viewModel::onHours,
            onMinutes = viewModel::onMinutes,
            onSave = viewModel::save,
            onDelete = viewModel::deleteEditing,
            onDismiss = viewModel::dismissEditor,
        )
    }
}

private fun GoalPeriod.labelRes(): Int = when (this) {
    GoalPeriod.DAILY -> R.string.filter_daily
    GoalPeriod.WEEKLY -> R.string.filter_weekly
    GoalPeriod.MONTHLY -> R.string.filter_monthly
}

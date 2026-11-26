package com.pareto.timetracker.ui.archive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pareto.timetracker.R
import com.pareto.timetracker.data.db.CategoryEntity
import com.pareto.timetracker.domain.CategoryPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    onBack: () -> Unit,
    viewModel: ArchiveViewModel = hiltViewModel(),
) {
    val items by viewModel.archived.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<CategoryEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.archive_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.desc_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        val list = items
        when {
            list == null -> Box(Modifier.fillMaxSize().padding(padding))
            list.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.archive_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                items(list, key = { it.id }) { category ->
                    ListItem(
                        headlineContent = { Text(category.name) },
                        supportingContent = { Text(stringResource(category.tag.labelRes)) },
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color(CategoryPalette.color(category.tag, category.colorIndex))),
                            )
                        },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { viewModel.restore(category.id) }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Unarchive,
                                        contentDescription = stringResource(R.string.archive_restore),
                                    )
                                }
                                IconButton(onClick = { pendingDelete = category }) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteForever,
                                        contentDescription = stringResource(R.string.archive_delete),
                                    )
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    pendingDelete?.let { category ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_message, category.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePermanently(category.id)
                        pendingDelete = null
                    },
                ) { Text(stringResource(R.string.delete_confirm_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.dialog_dismiss))
                }
            },
        )
    }
}

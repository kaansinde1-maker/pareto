package com.pareto.timetracker.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.pareto.timetracker.R
import com.pareto.timetracker.domain.CategoryPalette
import com.pareto.timetracker.domain.Tag

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryEditorSheet(
    state: EditorState,
    onNameChange: (String) -> Unit,
    onTagChange: (Tag) -> Unit,
    onColorChange: (Int) -> Unit,
    onSave: () -> Unit,
    onRestoreConflicting: () -> Unit,
    onArchive: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var confirmArchive by remember { mutableStateOf(false) }
    val conflict = state.nameConflict

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(
                    if (state.isNew) R.string.editor_title_new else R.string.editor_title_edit,
                ),
                style = MaterialTheme.typography.titleLarge,
            )

            Column {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = onNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.editor_name_label)) },
                    singleLine = true,
                    isError = conflict != null,
                    supportingText = conflict?.let {
                        {
                            Text(
                                stringResource(
                                    if (it.existingIsArchived) {
                                        R.string.editor_name_taken_archived
                                    } else {
                                        R.string.editor_name_taken
                                    },
                                ),
                            )
                        }
                    },
                )
                if (conflict?.existingIsArchived == true) {
                    TextButton(onClick = onRestoreConflicting) {
                        Text(stringResource(R.string.editor_restore))
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.editor_tag_label),
                    style = MaterialTheme.typography.labelLarge,
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    Tag.entries.forEachIndexed { index, tag ->
                        SegmentedButton(
                            selected = state.tag == tag,
                            onClick = { onTagChange(tag) },
                            shape = SegmentedButtonDefaults.itemShape(index, Tag.entries.size),
                        ) {
                            Text(stringResource(tag.labelRes))
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.editor_color_label),
                    style = MaterialTheme.typography.labelLarge,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CategoryPalette.colors(state.tag).forEachIndexed { index, argb ->
                        ColorSwatch(
                            color = Color(argb),
                            selected = state.colorIndex == index,
                            onClick = { onColorChange(index) },
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (!state.isNew) {
                    TextButton(onClick = { confirmArchive = true }) {
                        Text(stringResource(R.string.editor_archive))
                    }
                } else {
                    Box {}
                }
                Button(onClick = onSave, enabled = state.name.isNotBlank()) {
                    Text(stringResource(R.string.editor_save))
                }
            }
        }
    }

    if (confirmArchive) {
        AlertDialog(
            onDismissRequest = { confirmArchive = false },
            title = { Text(stringResource(R.string.archive_confirm_title)) },
            text = { Text(stringResource(R.string.archive_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmArchive = false
                        onArchive()
                    },
                ) { Text(stringResource(R.string.archive_confirm_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmArchive = false }) {
                    Text(stringResource(R.string.dialog_dismiss))
                }
            },
        )
    }
}

@Composable
private fun ColorSwatch(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (selected) {
                    Modifier.border(BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface), CircleShape)
                } else {
                    Modifier
                },
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
            )
        }
    }
}

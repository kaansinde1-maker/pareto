package com.pareto.timetracker.ui.goals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pareto.timetracker.R
import com.pareto.timetracker.data.db.CategoryEntity
import com.pareto.timetracker.domain.CategoryPalette
import com.pareto.timetracker.domain.GoalPeriod
import com.pareto.timetracker.domain.GoalType
import com.pareto.timetracker.domain.Tag
import com.pareto.timetracker.ui.common.tagColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalEditorSheet(
    state: GoalEditorState,
    categories: List<CategoryEntity>,
    onScopeType: (GoalScopeType) -> Unit,
    onTag: (Tag) -> Unit,
    onCategory: (Long) -> Unit,
    onPeriod: (GoalPeriod) -> Unit,
    onType: (GoalType) -> Unit,
    onHours: (String) -> Unit,
    onMinutes: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var confirmDelete by remember { mutableStateOf(false) }

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
                    if (state.isNew) R.string.goal_editor_title_new else R.string.goal_editor_title_edit,
                ),
                style = MaterialTheme.typography.titleLarge,
            )

            // Kapsam: Etiket veya tek kategori
            Section(R.string.goal_scope_label) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = state.scopeType == GoalScopeType.TAG,
                        onClick = { onScopeType(GoalScopeType.TAG) },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                    ) { Text(stringResource(R.string.grouping_tag)) }
                    SegmentedButton(
                        selected = state.scopeType == GoalScopeType.CATEGORY,
                        onClick = { onScopeType(GoalScopeType.CATEGORY) },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                    ) { Text(stringResource(R.string.grouping_category)) }
                }
                SubOptions {
                    if (state.scopeType == GoalScopeType.TAG) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Tag.entries.forEach { tag ->
                                TagChoice(
                                    tag = tag,
                                    selected = state.tag == tag,
                                    onClick = { onTag(tag) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    } else if (categories.isEmpty()) {
                        Text(
                            text = stringResource(R.string.goal_no_categories),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            categories.forEach { category ->
                                FilterChip(
                                    selected = state.categoryId == category.id,
                                    onClick = { onCategory(category.id) },
                                    label = { Text(category.name) },
                                    leadingIcon = {
                                        Box(
                                            Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(Color(CategoryPalette.color(category.tag, category.colorIndex))),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }

            Section(R.string.goal_period_label) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val periods = listOf(
                        GoalPeriod.DAILY to R.string.filter_daily,
                        GoalPeriod.WEEKLY to R.string.filter_weekly,
                        GoalPeriod.MONTHLY to R.string.filter_monthly,
                    )
                    periods.forEachIndexed { index, (period, label) ->
                        SegmentedButton(
                            selected = state.period == period,
                            onClick = { onPeriod(period) },
                            shape = SegmentedButtonDefaults.itemShape(index, periods.size),
                        ) { Text(stringResource(label)) }
                    }
                }
            }

            Section(R.string.goal_type_label) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = state.type == GoalType.MIN,
                        onClick = { onType(GoalType.MIN) },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                    ) { Text(stringResource(R.string.goal_type_min)) }
                    SegmentedButton(
                        selected = state.type == GoalType.MAX,
                        onClick = { onType(GoalType.MAX) },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                    ) { Text(stringResource(R.string.goal_type_max)) }
                }
            }

            Section(R.string.goal_target_label) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = state.hours,
                        onValueChange = onHours,
                        modifier = Modifier.weight(1f),
                        label = { Text(stringResource(R.string.goal_hours)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    OutlinedTextField(
                        value = state.minutes,
                        onValueChange = onMinutes,
                        modifier = Modifier.weight(1f),
                        label = { Text(stringResource(R.string.goal_minutes)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
                if (state.duplicate) {
                    Text(
                        text = stringResource(R.string.goal_duplicate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (!state.isNew) {
                    TextButton(onClick = { confirmDelete = true }) {
                        Text(stringResource(R.string.goal_delete))
                    }
                } else {
                    Box {}
                }
                Button(onClick = onSave, enabled = state.canSave) {
                    Text(stringResource(R.string.editor_save))
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.goal_delete_confirm_title)) },
            text = { Text(stringResource(R.string.goal_delete_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    },
                ) { Text(stringResource(R.string.goal_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.dialog_dismiss))
                }
            },
        )
    }
}

@Composable
private fun Section(labelRes: Int, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(labelRes), style = MaterialTheme.typography.labelLarge)
        content()
    }
}

/** Üst seçeneğin alt seçenekleri: solda ince bağlantı çizgisi ve girinti (üst menüyle karışmasın). */
@Composable
private fun SubOptions(content: @Composable () -> Unit) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp)
            .drawBehind {
                drawRoundRect(
                    color = lineColor,
                    topLeft = Offset.Zero,
                    size = Size(2.dp.toPx(), size.height),
                    cornerRadius = CornerRadius(1.dp.toPx()),
                )
            }
            .padding(start = 14.dp),
    ) {
        content()
    }
}

/** Yatırım / Tüketim seçimi: segment düğmesi yerine yeşil/kırmızı renkli seçim kartı. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagChoice(
    tag: Tag,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = tagColor(tag)
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) accent.copy(alpha = 0.18f) else Color.Transparent,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) accent else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(accent))
            Text(
                text = stringResource(tag.labelRes),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

package com.pareto.timetracker.ui.charts

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.pareto.timetracker.data.obsidian.ObsidianSyncResult
import com.pareto.timetracker.domain.CategoryPalette
import com.pareto.timetracker.domain.Tag
import com.pareto.timetracker.domain.TimeFilter
import com.pareto.timetracker.ui.common.durationText
import com.pareto.timetracker.ui.common.percentText
import com.pareto.timetracker.ui.common.tagColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartsScreen(viewModel: ChartsViewModel = hiltViewModel()) {
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val periodLabel by viewModel.periodLabel.collectAsStateWithLifecycle()
    val data by viewModel.data.collectAsStateWithLifecycle()

    var grouping by rememberSaveable { mutableStateOf(Grouping.TAG) }
    var chartType by rememberSaveable { mutableStateOf(ChartType.PIE) }
    var tagFilter by rememberSaveable { mutableStateOf<Tag?>(null) }
    var showRangePicker by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val exportDone = stringResource(R.string.charts_export_done)
    val exportFailed = stringResource(R.string.charts_export_failed)
    LaunchedEffect(Unit) {
        viewModel.exportResult.collect { ok ->
            snackbarHostState.showSnackbar(if (ok) exportDone else exportFailed)
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri -> uri?.let(viewModel::export) }

    val obsidianConfigured by viewModel.obsidianConfigured.collectAsStateWithLifecycle()
    val obsidianDone = stringResource(R.string.obsidian_done)
    val obsidianFailed = stringResource(R.string.obsidian_failed)
    val obsidianNotConfigured = stringResource(R.string.obsidian_not_configured)
    LaunchedEffect(Unit) {
        viewModel.obsidianResults.collect { result ->
            snackbarHostState.showSnackbar(
                when (result) {
                    ObsidianSyncResult.Success -> obsidianDone
                    ObsidianSyncResult.NotConfigured -> obsidianNotConfigured
                    is ObsidianSyncResult.Failure -> obsidianFailed
                },
            )
        }
    }
    val obsidianFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> uri?.let(viewModel::setObsidianFolder) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_charts)) },
                actions = {
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.charts_menu))
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.charts_export)) },
                                onClick = {
                                    menuOpen = false
                                    exportLauncher.launch(viewModel.exportFileName())
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.obsidian_pick_folder)) },
                                onClick = {
                                    menuOpen = false
                                    obsidianFolderLauncher.launch(null)
                                },
                            )
                            if (obsidianConfigured) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.obsidian_sync_now)) },
                                    onClick = {
                                        menuOpen = false
                                        viewModel.obsidianSyncNow()
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.obsidian_rewrite_all)) },
                                    onClick = {
                                        menuOpen = false
                                        viewModel.obsidianRewriteAll()
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val current = data
        val total = current?.totalMs ?: 0L

        // Gösterilen küme: Kategori görünümünde etiket filtresi uygulanır.
        val visible = remember(current, grouping, tagFilter) {
            (current?.totals ?: emptyList())
                .filter { grouping == Grouping.TAG || tagFilter == null || it.category.tag == tagFilter }
                .sortedByDescending { it.durationMs }
        }
        val investmentLabel = stringResource(Tag.INVESTMENT.labelRes)
        val consumptionLabel = stringResource(Tag.CONSUMPTION.labelRes)
        val slices = remember(current, grouping, visible, investmentLabel, consumptionLabel) {
            if (grouping == Grouping.TAG && current != null) {
                listOf(
                    ChartSlice(investmentLabel, current.tagMs(Tag.INVESTMENT), tagColor(Tag.INVESTMENT)),
                    ChartSlice(consumptionLabel, current.tagMs(Tag.CONSUMPTION), tagColor(Tag.CONSUMPTION)),
                ).filter { it.value > 0 }
            } else {
                visible.map {
                    ChartSlice(
                        label = it.category.name,
                        value = it.durationMs,
                        color = Color(CategoryPalette.color(it.category.tag, it.category.colorIndex)),
                        isArchived = it.category.isArchived,
                    )
                }
            }
        }
        val slicesTotal = slices.sumOf { it.value }
        val listTotal = visible.sumOf { it.durationMs }
        val showList = !(grouping == Grouping.CATEGORY && chartType == ChartType.BAR)

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterOption(R.string.filter_daily, filter.type == TimeFilter.DAILY) {
                        viewModel.selectFilter(TimeFilter.DAILY)
                    }
                    FilterOption(R.string.filter_weekly, filter.type == TimeFilter.WEEKLY) {
                        viewModel.selectFilter(TimeFilter.WEEKLY)
                    }
                    FilterOption(R.string.filter_monthly, filter.type == TimeFilter.MONTHLY) {
                        viewModel.selectFilter(TimeFilter.MONTHLY)
                    }
                    FilterOption(R.string.filter_all_time, filter.type == TimeFilter.ALL_TIME) {
                        viewModel.selectFilter(TimeFilter.ALL_TIME)
                    }
                    FilterOption(R.string.filter_custom, filter.type == TimeFilter.CUSTOM) {
                        showRangePicker = true
                    }
                }
            }

            if (filter.type != TimeFilter.ALL_TIME) {
                item {
                    PeriodNavigator(
                        label = periodLabel,
                        showArrows = filter.type == TimeFilter.DAILY || filter.type == TimeFilter.WEEKLY || filter.type == TimeFilter.MONTHLY,
                        canGoNext = filter.offset < 0,
                        onPrevious = viewModel::previousPeriod,
                        onNext = viewModel::nextPeriod,
                    )
                }
            }

            item { SummaryCard(current) }

            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = grouping == Grouping.TAG,
                        onClick = { grouping = Grouping.TAG },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                    ) { Text(stringResource(R.string.grouping_tag)) }
                    SegmentedButton(
                        selected = grouping == Grouping.CATEGORY,
                        onClick = { grouping = Grouping.CATEGORY },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                    ) { Text(stringResource(R.string.grouping_category)) }
                }
            }

            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = chartType == ChartType.PIE,
                        onClick = { chartType = ChartType.PIE },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                    ) { Text(stringResource(R.string.chart_type_pie)) }
                    SegmentedButton(
                        selected = chartType == ChartType.BAR,
                        onClick = { chartType = ChartType.BAR },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                    ) { Text(stringResource(R.string.chart_type_bar)) }
                }
            }

            if (grouping == Grouping.CATEGORY) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterOption(R.string.tag_filter_all, tagFilter == null) { tagFilter = null }
                        FilterOption(Tag.INVESTMENT.labelRes, tagFilter == Tag.INVESTMENT) {
                            tagFilter = Tag.INVESTMENT
                        }
                        FilterOption(Tag.CONSUMPTION.labelRes, tagFilter == Tag.CONSUMPTION) {
                            tagFilter = Tag.CONSUMPTION
                        }
                    }
                }
            }

            when {
                current == null -> Unit
                slicesTotal <= 0 -> item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.charts_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                else -> {
                    item {
                        when (chartType) {
                            ChartType.PIE -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                PieChart(
                                    slices = slices,
                                    centerText = durationText(slicesTotal),
                                    modifier = Modifier.fillMaxWidth(0.7f),
                                )
                            }
                            ChartType.BAR -> BarChart(slices = slices)
                        }
                    }
                    if (showList) {
                        items(visible, key = { it.category.id }) { row ->
                            CategoryRow(
                                name = row.category.name,
                                isArchived = row.category.isArchived,
                                color = Color(CategoryPalette.color(row.category.tag, row.category.colorIndex)),
                                durationMs = row.durationMs,
                                totalMs = listTotal,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showRangePicker) {
        CustomRangeDialog(
            onConfirm = { start, end ->
                viewModel.setCustomRange(start, end)
                showRangePicker = false
            },
            onDismiss = { showRangePicker = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterOption(labelRes: Int, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(stringResource(labelRes)) },
    )
}

@Composable
private fun PeriodNavigator(
    label: String,
    showArrows: Boolean,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (showArrows) Arrangement.SpaceBetween else Arrangement.Center,
    ) {
        if (showArrows) {
            IconButton(onClick = onPrevious) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.desc_previous_period),
                )
            }
        }
        Text(text = label, style = MaterialTheme.typography.titleMedium)
        if (showArrows) {
            IconButton(onClick = onNext, enabled = canGoNext) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.desc_next_period),
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(data: ChartData?) {
    val total = data?.totalMs ?: 0L
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column {
                Text(
                    text = stringResource(R.string.charts_total_time),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(text = durationText(total), style = MaterialTheme.typography.headlineMedium)
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                Tag.entries.forEach { tag ->
                    TagSummary(
                        tag = tag,
                        ms = data?.tagMs(tag) ?: 0L,
                        totalMs = total,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun TagSummary(tag: Tag, ms: Long, totalMs: Long, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(tagColor(tag)))
            Text(
                text = stringResource(tag.labelRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(text = durationText(ms), style = MaterialTheme.typography.titleMedium)
        Text(
            text = percentText(ms, totalMs),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CategoryRow(
    name: String,
    isArchived: Boolean,
    color: Color,
    durationMs: Long,
    totalMs: Long,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(12.dp).clip(CircleShape).background(color))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
            if (isArchived) {
                Text(
                    text = stringResource(R.string.charts_archived_suffix),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(text = durationText(durationMs), style = MaterialTheme.typography.bodyMedium)
        Text(
            text = percentText(durationMs, totalMs),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

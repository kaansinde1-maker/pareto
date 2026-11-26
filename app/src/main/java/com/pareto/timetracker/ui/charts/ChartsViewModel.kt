package com.pareto.timetracker.ui.charts

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pareto.timetracker.data.obsidian.ObsidianExporter
import com.pareto.timetracker.data.obsidian.ObsidianSettings
import com.pareto.timetracker.data.obsidian.ObsidianSyncResult
import com.pareto.timetracker.data.repo.ExportRepository
import com.pareto.timetracker.data.repo.StatsRepository
import com.pareto.timetracker.domain.FilterState
import com.pareto.timetracker.domain.Period
import com.pareto.timetracker.domain.TimeFilter
import com.pareto.timetracker.domain.resolvePeriod
import com.pareto.timetracker.domain.todayRangeFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChartsViewModel @Inject constructor(
    statsRepository: StatsRepository,
    private val exportRepository: ExportRepository,
    private val obsidianExporter: ObsidianExporter,
    private val obsidianSettings: ObsidianSettings,
) : ViewModel() {

    private val _filter = MutableStateFlow(FilterState())
    val filter: StateFlow<FilterState> = _filter.asStateFlow()

    /** Filtre veya gün değişince (gece yarısı) yeniden çözülür. */
    private val period: Flow<Period> = combine(_filter, todayRangeFlow()) { filter, _ ->
        val zone = ZoneId.systemDefault()
        resolvePeriod(filter, LocalDate.now(zone), zone)
    }

    val periodLabel: StateFlow<String> = period
        .map(::formatPeriod)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    /** null: henüz yüklenmedi. */
    val data: StateFlow<ChartData?> = period
        .flatMapLatest { p ->
            statsRepository.observeCategoryTotals(p.range.fromMs, p.range.toMs)
                .map { ChartData(it) }
        }
        .stateIn<ChartData?>(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _exportResult = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val exportResult: SharedFlow<Boolean> = _exportResult.asSharedFlow()

    /** Obsidian klasörü seçili mi (menüdeki "şimdi yaz" maddelerini göstermek için). */
    val obsidianConfigured: StateFlow<Boolean> = obsidianSettings.treeUri
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Menüden elle tetiklenen Obsidian yazmalarının sonucu. */
    val obsidianResults: SharedFlow<ObsidianSyncResult> = obsidianExporter.manualResults

    fun setObsidianFolder(uri: Uri) {
        viewModelScope.launch {
            obsidianSettings.setTreeUri(uri)
            obsidianExporter.rewriteAll()
        }
    }

    fun obsidianSyncNow() = obsidianExporter.syncNow()

    fun obsidianRewriteAll() = obsidianExporter.rewriteAll()

    fun selectFilter(type: TimeFilter) {
        _filter.update { it.copy(type = type, offset = 0) }
    }

    fun setCustomRange(start: LocalDate, end: LocalDate) {
        _filter.value = FilterState(
            type = TimeFilter.CUSTOM,
            customStart = start,
            customEnd = end,
        )
    }

    fun previousPeriod() = _filter.update { it.copy(offset = it.offset - 1) }

    /** Gelecek dönem yok: offset en fazla 0. */
    fun nextPeriod() = _filter.update { it.copy(offset = (it.offset + 1).coerceAtMost(0)) }

    fun exportFileName(): String = exportRepository.suggestedFileName()

    fun export(uri: Uri) {
        viewModelScope.launch { _exportResult.emit(exportRepository.writeTo(uri)) }
    }

    private fun formatPeriod(period: Period): String {
        val start = period.start
        val end = period.endInclusive
        return when {
            start == null || end == null -> ""
            period.type == TimeFilter.DAILY -> start.format(DAY_FORMAT)
            period.type == TimeFilter.MONTHLY -> start.format(MONTH_FORMAT)
            period.type == TimeFilter.WEEKLY ->
                "${start.format(SHORT_FORMAT)} – ${end.format(DAY_FORMAT)}"
            else -> "${start.format(DAY_FORMAT)} – ${end.format(DAY_FORMAT)}"
        }
    }

    private companion object {
        val TR: Locale = Locale.forLanguageTag("tr-TR")
        val DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", TR)
        val SHORT_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", TR)
        val MONTH_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", TR)
    }
}

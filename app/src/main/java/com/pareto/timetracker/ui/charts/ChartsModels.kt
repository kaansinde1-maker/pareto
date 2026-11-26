package com.pareto.timetracker.ui.charts

import androidx.compose.ui.graphics.Color
import com.pareto.timetracker.data.repo.CategoryTotal
import com.pareto.timetracker.domain.Tag

enum class Grouping { TAG, CATEGORY }

enum class ChartType { PIE, BAR }

/** Seçili dönemde süresi > 0 olan kategoriler (arşivliler dahil). */
data class ChartData(val totals: List<CategoryTotal>) {
    val totalMs: Long = totals.sumOf { it.durationMs }

    fun tagMs(tag: Tag): Long = totals.filter { it.category.tag == tag }.sumOf { it.durationMs }
}

data class ChartSlice(
    val label: String,
    val value: Long,
    val color: Color,
    val isArchived: Boolean = false,
)

package com.pareto.timetracker.data.repo

import com.pareto.timetracker.data.db.CategoryDao
import com.pareto.timetracker.data.db.CategoryEntity
import com.pareto.timetracker.data.db.SessionDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

data class CategoryTotal(
    val category: CategoryEntity,
    val durationMs: Long,
)

/**
 * Süre toplamları. Aralık [fromMs, toMs) ve milisaniye cinsindendir;
 * "Tüm Zamanlar" için from = 0, to = Long.MAX_VALUE verilebilir.
 * Henüz durdurulmamış (çalışan) timer toplamlara dahil değildir.
 */
@Singleton
class StatsRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val categoryDao: CategoryDao,
) {
    /** Arşivli kategoriler dahil, süresi > 0 olan kategoriler. */
    fun observeCategoryTotals(fromMs: Long, toMs: Long): Flow<List<CategoryTotal>> =
        combine(
            sessionDao.observeDurationsByCategory(fromMs, toMs),
            categoryDao.observeAll(),
        ) { durations, categories ->
            val byId = categories.associateBy { it.id }
            durations.mapNotNull { d ->
                byId[d.categoryId]?.let { CategoryTotal(it, d.durationMs) }
            }.filter { it.durationMs > 0 }
        }
}

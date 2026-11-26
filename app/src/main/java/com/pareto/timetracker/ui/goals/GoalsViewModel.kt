package com.pareto.timetracker.ui.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pareto.timetracker.data.db.CategoryEntity
import com.pareto.timetracker.data.db.GoalEntity
import com.pareto.timetracker.data.repo.CategoryRepository
import com.pareto.timetracker.data.repo.CategoryTotal
import com.pareto.timetracker.data.repo.GoalRepository
import com.pareto.timetracker.data.repo.GoalSaveResult
import com.pareto.timetracker.data.repo.GoalScope
import com.pareto.timetracker.data.repo.StatsRepository
import com.pareto.timetracker.domain.FilterState
import com.pareto.timetracker.domain.GoalPeriod
import com.pareto.timetracker.domain.GoalType
import com.pareto.timetracker.domain.Tag
import com.pareto.timetracker.domain.resolvePeriod
import com.pareto.timetracker.domain.toTimeFilter
import com.pareto.timetracker.domain.todayRangeFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    categoryRepository: CategoryRepository,
    statsRepository: StatsRepository,
) : ViewModel() {

    /** Günlük / haftalık / aylık: her takvim döneminin güncel toplamları. Gece yarısı yenilenir. */
    private val totalsByPeriod: Flow<Map<GoalPeriod, List<CategoryTotal>>> =
        todayRangeFlow().flatMapLatest {
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val flows = GoalPeriod.entries.map { period ->
                val range = resolvePeriod(FilterState(type = period.toTimeFilter()), today, zone).range
                statsRepository.observeCategoryTotals(range.fromMs, range.toMs).map { period to it }
            }
            combine(flows) { pairs -> pairs.toMap() }
        }

    val allCategories: StateFlow<List<CategoryEntity>> = categoryRepository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** null: henüz yüklenmedi. */
    val goals: StateFlow<List<GoalUi>?> =
        combine(goalRepository.goals, categoryRepository.allCategories, totalsByPeriod) { goals, categories, totals ->
            val byId = categories.associateBy { it.id }
            goals.map { goal -> toUi(goal, byId[goal.categoryId], totals[goal.period].orEmpty()) }
        }.stateIn<List<GoalUi>?>(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _editor = MutableStateFlow<GoalEditorState?>(null)
    val editor: StateFlow<GoalEditorState?> = _editor.asStateFlow()

    private fun toUi(goal: GoalEntity, category: CategoryEntity?, totals: List<CategoryTotal>): GoalUi {
        val progress = if (goal.categoryId != null) {
            totals.firstOrNull { it.category.id == goal.categoryId }?.durationMs ?: 0L
        } else {
            totals.filter { it.category.tag == goal.tag }.sumOf { it.durationMs }
        }
        return GoalUi(goal = goal, category = category, progressMs = progress)
    }

    fun openNew() {
        _editor.value = GoalEditorState()
    }

    fun openEdit(ui: GoalUi) {
        val goal = ui.goal
        _editor.value = GoalEditorState(
            goalId = goal.id,
            scopeType = if (goal.categoryId != null) GoalScopeType.CATEGORY else GoalScopeType.TAG,
            tag = goal.tag ?: Tag.INVESTMENT,
            categoryId = goal.categoryId,
            period = goal.period,
            type = goal.goalType,
            hours = (goal.targetSeconds / 3600).takeIf { it > 0 }?.toString().orEmpty(),
            minutes = ((goal.targetSeconds % 3600) / 60).takeIf { it > 0 }?.toString().orEmpty(),
        )
    }

    fun dismissEditor() {
        _editor.value = null
    }

    private fun edit(transform: (GoalEditorState) -> GoalEditorState) =
        _editor.update { state -> state?.let { transform(it).copy(duplicate = false) } }

    fun onScopeType(type: GoalScopeType) = edit { state ->
        val firstCategory = allCategories.value.firstOrNull { !it.isArchived }?.id
        state.copy(
            scopeType = type,
            categoryId = if (type == GoalScopeType.CATEGORY) state.categoryId ?: firstCategory else state.categoryId,
        )
    }

    fun onTag(tag: Tag) = edit { it.copy(tag = tag) }
    fun onCategory(id: Long) = edit { it.copy(categoryId = id) }
    fun onPeriod(period: GoalPeriod) = edit { it.copy(period = period) }
    fun onType(type: GoalType) = edit { it.copy(type = type) }
    fun onHours(value: String) = edit { it.copy(hours = value.filter(Char::isDigit).take(4)) }
    fun onMinutes(value: String) = edit { it.copy(minutes = value.filter(Char::isDigit).take(2)) }

    fun save() {
        val state = _editor.value ?: return
        if (!state.canSave) return
        val scope = when (state.scopeType) {
            GoalScopeType.TAG -> GoalScope.ByTag(state.tag)
            GoalScopeType.CATEGORY -> GoalScope.ByCategory(state.categoryId ?: return)
        }
        viewModelScope.launch {
            val result = if (state.goalId == null) {
                goalRepository.add(scope, state.period, state.type, state.targetSeconds)
            } else {
                goalRepository.update(state.goalId, scope, state.period, state.type, state.targetSeconds)
            }
            when (result) {
                GoalSaveResult.Success -> _editor.value = null
                GoalSaveResult.Duplicate -> _editor.update { it?.copy(duplicate = true) }
                GoalSaveResult.InvalidTarget -> Unit
            }
        }
    }

    fun deleteEditing() {
        val id = _editor.value?.goalId ?: return
        viewModelScope.launch {
            goalRepository.delete(id)
            _editor.value = null
        }
    }
}

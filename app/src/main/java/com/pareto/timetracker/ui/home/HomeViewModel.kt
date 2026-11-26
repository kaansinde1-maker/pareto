package com.pareto.timetracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pareto.timetracker.data.db.ActiveTimerEntity
import com.pareto.timetracker.data.db.CategoryEntity
import com.pareto.timetracker.data.pool.PoolRepository
import com.pareto.timetracker.data.repo.CategoryRepository
import com.pareto.timetracker.data.repo.SaveResult
import com.pareto.timetracker.data.repo.StatsRepository
import com.pareto.timetracker.data.repo.TimerRepository
import com.pareto.timetracker.domain.Tag
import com.pareto.timetracker.domain.TimerRules
import com.pareto.timetracker.domain.todayRangeFlow
import com.pareto.timetracker.timer.TimerController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
import javax.inject.Inject

data class CategoryCardUi(
    val category: CategoryEntity,
    /** Bugün kaydedilmiş toplam (çalışan oturum hariç). */
    val todayMs: Long,
)

/** Odak puanı çubuğu: [selected] boşsa henüz dokunulmadı. */
data class PendingRating(val sessionId: Long, val selected: Int? = null)

data class NameConflict(val existingId: Long, val existingIsArchived: Boolean)

data class EditorState(
    /** null ise yeni kategori. */
    val categoryId: Long? = null,
    val name: String = "",
    val tag: Tag = Tag.INVESTMENT,
    val colorIndex: Int = 0,
    val nameConflict: NameConflict? = null,
) {
    val isNew: Boolean get() = categoryId == null
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    statsRepository: StatsRepository,
    timerRepository: TimerRepository,
    private val poolRepository: PoolRepository,
    private val controller: TimerController,
) : ViewModel() {

    private val todayTotals: Flow<Map<Long, Long>> = todayRangeFlow().flatMapLatest { range ->
        statsRepository.observeCategoryTotals(range.fromMs, range.toMs)
            .map { totals -> totals.associate { it.category.id to it.durationMs } }
    }

    /** null: henüz yüklenmedi (boş durum yanlışlıkla çakmasın). */
    val cards: StateFlow<List<CategoryCardUi>?> =
        combine(categoryRepository.activeCategories, todayTotals) { categories, totals ->
            categories.map { CategoryCardUi(it, totals[it.id] ?: 0L) }
        }.stateIn<List<CategoryCardUi>?>(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val activeTimer: StateFlow<ActiveTimerEntity?> = timerRepository.activeTimer
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _editor = MutableStateFlow<EditorState?>(null)
    val editor: StateFlow<EditorState?> = _editor.asStateFlow()

    private val _pendingRating = MutableStateFlow<PendingRating?>(null)
    val pendingRating: StateFlow<PendingRating?> = _pendingRating.asStateFlow()
    private var ratingTimeoutJob: Job? = null

    init {
        // Odak puanı çubuğu ~8 sn görünür; dokunulmazsa puan boş kalır.
        viewModelScope.launch {
            controller.focusPromptSessions.collect { session ->
                _pendingRating.value = PendingRating(session.sessionId)
                ratingTimeoutJob?.cancel()
                ratingTimeoutJob = launch {
                    delay(TimerRules.FOCUS_PROMPT_VISIBLE_MS)
                    _pendingRating.value = null
                }
            }
        }
    }

    fun rate(value: Int) {
        val pending = _pendingRating.value ?: return
        _pendingRating.value = pending.copy(selected = value)
        viewModelScope.launch { poolRepository.setFocusRating(pending.sessionId, value) }
    }

    fun toggleTimer(categoryId: Long) = controller.toggle(categoryId)
    fun discardTimer() = controller.cancel()

    fun openNew() {
        _editor.value = EditorState()
    }

    fun openEdit(category: CategoryEntity) {
        _editor.value = EditorState(
            categoryId = category.id,
            name = category.name,
            tag = category.tag,
            colorIndex = category.colorIndex,
        )
    }

    fun dismissEditor() {
        _editor.value = null
    }

    fun onNameChange(value: String) = _editor.update {
        it?.copy(name = value.take(CategoryRepository.MAX_NAME_LENGTH), nameConflict = null)
    }

    /** Ton sırası korunur: yeni Tag paletinde aynı sıradaki tona eşlenir. */
    fun onTagChange(tag: Tag) = _editor.update { it?.copy(tag = tag) }

    fun onColorChange(index: Int) = _editor.update { it?.copy(colorIndex = index) }

    fun save() {
        val state = _editor.value ?: return
        viewModelScope.launch {
            val result = if (state.categoryId == null) {
                categoryRepository.add(state.name, state.tag, state.colorIndex)
            } else {
                categoryRepository.update(state.categoryId, state.name, state.tag, state.colorIndex)
            }
            when (result) {
                is SaveResult.Success -> _editor.value = null
                is SaveResult.NameTaken -> _editor.update {
                    it?.copy(nameConflict = NameConflict(result.existingId, result.existingIsArchived))
                }
                SaveResult.BlankName -> Unit
            }
        }
    }

    /** İsim çakışması arşivdeki bir kategoriyle ise onu geri yükler. */
    fun restoreConflicting() {
        val conflict = _editor.value?.nameConflict ?: return
        viewModelScope.launch {
            categoryRepository.restore(conflict.existingId)
            _editor.value = null
        }
    }

    fun archiveEditing() {
        val id = _editor.value?.categoryId ?: return
        viewModelScope.launch {
            categoryRepository.archive(id)
            _editor.value = null
        }
    }
}

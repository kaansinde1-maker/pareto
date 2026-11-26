package com.pareto.timetracker.ui.goals

import com.pareto.timetracker.data.db.CategoryEntity
import com.pareto.timetracker.data.db.GoalEntity
import com.pareto.timetracker.domain.GoalPeriod
import com.pareto.timetracker.domain.GoalType
import com.pareto.timetracker.domain.Tag

enum class GoalScopeType { TAG, CATEGORY }

data class GoalUi(
    val goal: GoalEntity,
    /** Kategori kapsamlı hedeflerde ilgili kategori; etiket kapsamlıda null. */
    val category: CategoryEntity?,
    /** Mevcut takvim döneminde kaydedilmiş süre (çalışan timer hariç), ms. */
    val progressMs: Long,
) {
    /** Bağlı kategori arşivdeyse hedef pasiftir; geri yüklenince tekrar aktifleşir. */
    val isActive: Boolean get() = category?.isArchived != true
    val targetMs: Long get() = goal.targetSeconds * 1000
}

data class GoalEditorState(
    /** null ise yeni hedef. */
    val goalId: Long? = null,
    val scopeType: GoalScopeType = GoalScopeType.TAG,
    val tag: Tag = Tag.INVESTMENT,
    val categoryId: Long? = null,
    val period: GoalPeriod = GoalPeriod.WEEKLY,
    val type: GoalType = GoalType.MIN,
    val hours: String = "",
    val minutes: String = "",
    val duplicate: Boolean = false,
) {
    val isNew: Boolean get() = goalId == null

    val targetSeconds: Long
        get() = (hours.toLongOrNull() ?: 0L) * 3600L + (minutes.toLongOrNull() ?: 0L) * 60L

    val canSave: Boolean
        get() = targetSeconds > 0 && (scopeType == GoalScopeType.TAG || categoryId != null)
}

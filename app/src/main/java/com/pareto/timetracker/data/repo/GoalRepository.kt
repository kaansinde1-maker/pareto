package com.pareto.timetracker.data.repo

import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import com.pareto.timetracker.data.db.AppDatabase
import com.pareto.timetracker.data.db.GoalDao
import com.pareto.timetracker.data.db.GoalEntity
import com.pareto.timetracker.data.pool.EventLogger
import com.pareto.timetracker.data.pool.PoolRepository
import com.pareto.timetracker.data.pool.jsonOf
import com.pareto.timetracker.data.pool.toEventJson
import com.pareto.timetracker.domain.EventType
import com.pareto.timetracker.domain.GoalPeriod
import com.pareto.timetracker.domain.GoalType
import com.pareto.timetracker.domain.Tag
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface GoalScope {
    val key: String

    data class ByCategory(val categoryId: Long) : GoalScope {
        override val key = "C:$categoryId"
    }

    data class ByTag(val tag: Tag) : GoalScope {
        override val key = "T:${tag.name}"
    }
}

sealed interface GoalSaveResult {
    data object Success : GoalSaveResult
    /** Aynı kapsam + dönem + tip için zaten hedef var. */
    data object Duplicate : GoalSaveResult
    data object InvalidTarget : GoalSaveResult
}

@Singleton
class GoalRepository @Inject constructor(
    private val db: AppDatabase,
    private val dao: GoalDao,
    private val poolRepository: PoolRepository,
    private val eventLogger: EventLogger,
) {
    val goals: Flow<List<GoalEntity>> = dao.observeAll()

    suspend fun add(
        scope: GoalScope,
        period: GoalPeriod,
        type: GoalType,
        targetSeconds: Long,
    ): GoalSaveResult {
        if (targetSeconds <= 0) return GoalSaveResult.InvalidTarget
        if (dao.findByKey(scope.key, period, type) != null) return GoalSaveResult.Duplicate
        val entity = newEntity(0L, scope, period, type, targetSeconds, System.currentTimeMillis())
        return try {
            db.withTransaction {
                val id = dao.insert(entity)
                eventLogger.log(
                    type = EventType.GOAL_CREATED,
                    categoryId = entity.categoryId,
                    goalId = id,
                    payload = entity.copy(id = id).toEventJson(),
                )
            }
            GoalSaveResult.Success
        } catch (e: SQLiteConstraintException) {
            GoalSaveResult.Duplicate
        }
    }

    suspend fun update(
        id: Long,
        scope: GoalScope,
        period: GoalPeriod,
        type: GoalType,
        targetSeconds: Long,
    ): GoalSaveResult {
        if (targetSeconds <= 0) return GoalSaveResult.InvalidTarget
        val current = dao.getById(id) ?: return GoalSaveResult.InvalidTarget
        val existing = dao.findByKey(scope.key, period, type)
        if (existing != null && existing.id != id) return GoalSaveResult.Duplicate

        val updated = newEntity(id, scope, period, type, targetSeconds, current.createdAt)
        if (updated == current) return GoalSaveResult.Success

        // Geçmiş günler eski hedef tanımıyla dondurulsun, sonra değişiklik uygulansın.
        poolRepository.freezeCompletedDays()
        return try {
            db.withTransaction {
                dao.update(updated)
                eventLogger.log(
                    type = EventType.GOAL_UPDATED,
                    categoryId = updated.categoryId ?: current.categoryId,
                    goalId = id,
                    payload = jsonOf("old" to current.toEventJson(), "new" to updated.toEventJson()),
                )
            }
            GoalSaveResult.Success
        } catch (e: SQLiteConstraintException) {
            GoalSaveResult.Duplicate
        }
    }

    suspend fun delete(id: Long) {
        val goal = dao.getById(id) ?: return
        poolRepository.freezeCompletedDays()
        db.withTransaction {
            dao.deleteById(id)
            eventLogger.log(
                type = EventType.GOAL_DELETED,
                categoryId = goal.categoryId,
                goalId = id,
                payload = goal.toEventJson(),
            )
        }
    }

    private fun newEntity(
        id: Long,
        scope: GoalScope,
        period: GoalPeriod,
        type: GoalType,
        targetSeconds: Long,
        createdAt: Long,
    ) = GoalEntity(
        id = id,
        scopeKey = scope.key,
        categoryId = (scope as? GoalScope.ByCategory)?.categoryId,
        tag = (scope as? GoalScope.ByTag)?.tag,
        period = period,
        goalType = type,
        targetSeconds = targetSeconds,
        createdAt = createdAt,
    )
}

package com.pareto.timetracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pareto.timetracker.domain.GoalPeriod
import com.pareto.timetracker.domain.GoalType
import kotlinx.coroutines.flow.Flow

/** Bir kategorinin seçilen aralığa kırpılmış toplam süresi. */
data class CategoryDuration(
    val categoryId: Long,
    val durationMs: Long,
)

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE is_archived = 0 ORDER BY created_at ASC, id ASC")
    fun observeActive(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE is_archived = 1 ORDER BY name_key ASC")
    fun observeArchived(): Flow<List<CategoryEntity>>

    /** Arşivliler dahil tüm kategoriler (istatistik ve dışa aktarma için). */
    @Query("SELECT * FROM categories")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories")
    suspend fun getAll(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): CategoryEntity?

    @Query("SELECT * FROM categories WHERE name_key = :key LIMIT 1")
    suspend fun findByNameKey(key: String): CategoryEntity?

    @Insert
    suspend fun insert(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnoringConflicts(categories: List<CategoryEntity>)

    @Update
    suspend fun update(category: CategoryEntity)

    @Query("UPDATE categories SET is_archived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface SessionDao {
    @Insert
    suspend fun insert(session: SessionEntity): Long

    /**
     * [from, to) aralığıyla kesişen oturumların süresini aralığa kırparak toplar.
     * Gece yarısını aşan oturumlar böylece günlere doğru bölünür.
     */
    @Query(DURATIONS_BY_CATEGORY_QUERY)
    fun observeDurationsByCategory(from: Long, to: Long): Flow<List<CategoryDuration>>

    /** [observeDurationsByCategory] ile aynı sorgu, tek seferlik. */
    @Query(DURATIONS_BY_CATEGORY_QUERY)
    suspend fun getDurationsByCategory(from: Long, to: Long): List<CategoryDuration>

    @Query("UPDATE sessions SET focus_rating = :rating WHERE id = :sessionId")
    suspend fun setFocusRating(sessionId: Long, rating: Int?)
}

internal const val DURATIONS_BY_CATEGORY_QUERY = """
    SELECT category_id AS categoryId,
           SUM(MIN(ended_at, :to) - MAX(started_at, :from)) AS durationMs
    FROM sessions
    WHERE ended_at > :from AND started_at < :to
    GROUP BY category_id
"""

@Dao
interface ActiveTimerDao {
    @Query("SELECT * FROM active_timer WHERE id = 1")
    fun observe(): Flow<ActiveTimerEntity?>

    @Query("SELECT * FROM active_timer WHERE id = 1")
    suspend fun get(): ActiveTimerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun set(timer: ActiveTimerEntity)

    @Query("DELETE FROM active_timer")
    suspend fun clear()
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY created_at ASC, id ASC")
    fun observeAll(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getById(id: Long): GoalEntity?

    @Query("SELECT * FROM goals WHERE period = :period")
    suspend fun getByPeriod(period: GoalPeriod): List<GoalEntity>

    @Query(
        "SELECT * FROM goals WHERE scope_key = :scopeKey AND period = :period AND goal_type = :goalType LIMIT 1"
    )
    suspend fun findByKey(scopeKey: String, period: GoalPeriod, goalType: GoalType): GoalEntity?

    /** Aynı kapsam + dönem + tip varsa unique indeks nedeniyle exception fırlatır. */
    @Insert
    suspend fun insert(goal: GoalEntity): Long

    @Update
    suspend fun update(goal: GoalEntity)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteById(id: Long)
}

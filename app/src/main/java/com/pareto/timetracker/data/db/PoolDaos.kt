package com.pareto.timetracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pareto.timetracker.domain.EndReason
import com.pareto.timetracker.domain.StartSource
import com.pareto.timetracker.domain.Tag
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Insert
    suspend fun insert(event: EventEntity): Long
}

@Dao
interface DailyGoalStatusDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(rows: List<DailyGoalStatusEntity>)

    @Query("SELECT MAX(day) FROM daily_goal_status WHERE goal_id = :goalId")
    suspend fun lastDay(goalId: Long): String?
}

@Dao
interface PoolMetaDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun putIfAbsent(entry: PoolMetaEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entry: PoolMetaEntity)
}

/**
 * Havuz çıktıları için bir oturum: oturumun kendi alanları (snapshot* = oturum anındaki kategori görüntüsü)
 * ve kategorinin GÜNCEL ad/etiketi/arşiv durumu.
 */
data class PoolSessionRow(
    val sessionId: Long,
    val categoryId: Long,
    val uuid: String,
    val startedAt: Long,
    val endedAt: Long,
    val endReason: EndReason,
    val startSource: StartSource,
    val focusRating: Int?,
    val snapshotName: String,
    val snapshotTag: Tag,
    val snapshotColorIndex: Int,
    val tzOffsetSeconds: Int,
    val appVersion: String,
    val categoryName: String,
    val tag: Tag,
    val isArchived: Boolean,
)

@Dao
interface PoolReadDao {
    @Query(
        """
        SELECT s.id AS sessionId,
               s.category_id AS categoryId,
               s.uuid AS uuid,
               s.started_at AS startedAt,
               s.ended_at AS endedAt,
               s.end_reason AS endReason,
               s.start_source AS startSource,
               s.focus_rating AS focusRating,
               s.category_name AS snapshotName,
               s.category_tag AS snapshotTag,
               s.category_color_index AS snapshotColorIndex,
               s.tz_offset_seconds AS tzOffsetSeconds,
               s.app_version AS appVersion,
               c.name AS categoryName,
               c.tag AS tag,
               c.is_archived AS isArchived
        FROM sessions s
        INNER JOIN categories c ON c.id = s.category_id
        ORDER BY s.started_at ASC, s.id ASC
        """
    )
    suspend fun getSessions(): List<PoolSessionRow>

    @Query("SELECT * FROM events ORDER BY occurred_at ASC, id ASC")
    suspend fun getEvents(): List<EventEntity>

    @Query("SELECT * FROM daily_goal_status ORDER BY day ASC, goal_id ASC")
    suspend fun getDailyGoalStatuses(): List<DailyGoalStatusEntity>

    @Query("SELECT * FROM goals ORDER BY id ASC")
    suspend fun getGoals(): List<GoalEntity>

    @Query("SELECT * FROM categories ORDER BY id ASC")
    suspend fun getCategories(): List<CategoryEntity>

    @Query("SELECT * FROM pool_meta ORDER BY meta_key ASC")
    suspend fun getMeta(): List<PoolMetaEntity>

    /**
     * Havuzu etkileyen tablolardan herhangi biri değişince yeniden yayılır (değerin kendisi önemsiz).
     * Obsidian yazıcısı bunu tetikleyici olarak kullanır.
     */
    @Query(
        """
        SELECT (SELECT COUNT(*) FROM sessions)
             + (SELECT COUNT(*) FROM categories)
             + (SELECT COUNT(*) FROM goals)
             + (SELECT COUNT(*) FROM events)
             + (SELECT COUNT(*) FROM daily_goal_status)
        """
    )
    fun observeChanges(): Flow<Int>
}

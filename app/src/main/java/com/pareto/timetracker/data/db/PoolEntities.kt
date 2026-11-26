package com.pareto.timetracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pareto.timetracker.domain.EventType
import com.pareto.timetracker.domain.GoalType
import com.pareto.timetracker.domain.Tag

/**
 * Sadece eklenen olay günlüğü. category_id doluysa kategori kalıcı silinince olay da silinir.
 * goal_id'ye bilerek FK yok: "hedef silindi" olayı hedefin kendisinden uzun yaşar.
 */
@Entity(
    tableName = "events",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["uuid"], unique = true), Index("category_id")],
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String,
    @ColumnInfo(name = "occurred_at") val occurredAt: Long,
    @ColumnInfo(name = "tz_offset_seconds") val tzOffsetSeconds: Int,
    @ColumnInfo(name = "app_version") val appVersion: String,
    val type: EventType,
    @ColumnInfo(name = "category_id") val categoryId: Long? = null,
    @ColumnInfo(name = "goal_id") val goalId: Long? = null,
    /** JSON; içerik olay türüne göre değişir. */
    val payload: String? = null,
)

/** Günü bitmiş günlük hedeflerin dondurulmuş durumu (boş günler dahil). */
@Entity(
    tableName = "daily_goal_status",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["day", "goal_id"], unique = true), Index("category_id")],
)
data class DailyGoalStatusEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Yerel gün, ISO biçimi (2026-11-24). */
    val day: String,
    @ColumnInfo(name = "goal_id") val goalId: Long,
    @ColumnInfo(name = "scope_key") val scopeKey: String,
    @ColumnInfo(name = "category_id") val categoryId: Long? = null,
    val tag: Tag? = null,
    @ColumnInfo(name = "goal_type") val goalType: GoalType,
    @ColumnInfo(name = "target_seconds") val targetSeconds: Long,
    @ColumnInfo(name = "actual_seconds") val actualSeconds: Long,
    /** MIN: gerçekleşen >= hedef. MAX: gerçekleşen <= hedef. */
    val achieved: Boolean,
    /** Bağlı kategori o gün arşivdeyse false (kategori kapsamlı hedeflerde). */
    @ColumnInfo(name = "goal_active") val goalActive: Boolean,
    @ColumnInfo(name = "frozen_at") val frozenAt: Long,
)

/** Anahtar-değer: cihaz kimliği, cihaz modeli, Android sürümü, ilk çalıştırma vb. */
@Entity(tableName = "pool_meta")
data class PoolMetaEntity(
    @PrimaryKey @ColumnInfo(name = "meta_key") val key: String,
    @ColumnInfo(name = "meta_value") val value: String,
)

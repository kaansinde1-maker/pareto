package com.pareto.timetracker.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pareto.timetracker.domain.EndReason
import com.pareto.timetracker.domain.GoalPeriod
import com.pareto.timetracker.domain.GoalType
import com.pareto.timetracker.domain.StartSource
import com.pareto.timetracker.domain.Tag

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name_key"], unique = true)],
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Büyük/küçük harf duyarsız (tr) benzersizlik için normalize edilmiş ad. */
    @ColumnInfo(name = "name_key") val nameKey: String,
    val tag: Tag,
    /** Tag paletindeki ton sırası (0..3). */
    @ColumnInfo(name = "color_index") val colorIndex: Int,
    @ColumnInfo(name = "is_archived") val isArchived: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("category_id"),
        Index("started_at"),
        Index(value = ["uuid"], unique = true),
    ],
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    /** Benzersiz kayıt kimliği (CSV/Obsidian çıktılarında oturumu tanımlar). */
    val uuid: String,
    /** Epoch milisaniye. */
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "ended_at") val endedAt: Long,
    @ColumnInfo(name = "end_reason") val endReason: EndReason,
    @ColumnInfo(name = "start_source") val startSource: StartSource,
    /** 1..5 veya boş (sorulmadı / dokunulmadı). */
    @ColumnInfo(name = "focus_rating") val focusRating: Int? = null,
    /** Oturum anındaki kategori görüntüsü (sonradan adı/etiketi değişse de geçmiş korunur). */
    @ColumnInfo(name = "category_name") val categoryName: String,
    @ColumnInfo(name = "category_tag") val categoryTag: Tag,
    @ColumnInfo(name = "category_color_index") val categoryColorIndex: Int,
    /** Oturum başlangıcındaki UTC ofseti. */
    @ColumnInfo(name = "tz_offset_seconds") val tzOffsetSeconds: Int,
    @ColumnInfo(name = "app_version") val appVersion: String,
)

/** Çalışan timer. Tek satırlık tablo (id her zaman 1). */
@Entity(
    tableName = "active_timer",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("category_id")],
)
data class ActiveTimerEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    @ColumnInfo(name = "started_at") val startedAt: Long,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}

@Entity(
    tableName = "goals",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("category_id"),
        Index(value = ["scope_key", "period", "goal_type"], unique = true),
    ],
)
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** "C:<categoryId>" veya "T:<TAG>" — NULL'lar benzersiz indekste sorun çıkarmasın diye. */
    @ColumnInfo(name = "scope_key") val scopeKey: String,
    @ColumnInfo(name = "category_id") val categoryId: Long? = null,
    val tag: Tag? = null,
    val period: GoalPeriod,
    @ColumnInfo(name = "goal_type") val goalType: GoalType,
    @ColumnInfo(name = "target_seconds") val targetSeconds: Long,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

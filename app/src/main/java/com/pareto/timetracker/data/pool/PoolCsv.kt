package com.pareto.timetracker.data.pool

import com.pareto.timetracker.data.db.CategoryEntity
import com.pareto.timetracker.data.db.DailyGoalStatusEntity
import com.pareto.timetracker.data.db.EventEntity
import com.pareto.timetracker.data.db.GoalEntity
import com.pareto.timetracker.data.db.PoolMetaEntity
import com.pareto.timetracker.data.db.PoolSessionRow
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Veri havuzunun CSV biçimi. Hem Obsidian klasörüne yazılan dosyalar hem de Grafik ekranındaki
 * "Verileri dışa aktar" aynı üreticiyi kullanır. Virgül ayraçlı, UTF-8, BOM yok, boş değer = bilinmiyor,
 * satır sonu "\n", zamanlar ISO 8601 + saat dilimi ofseti, etiketler INVESTMENT / CONSUMPTION.
 */
object PoolCsv {

    fun sessions(rows: List<PoolSessionRow>, zone: ZoneId): String = csv(
        listOf(
            "session_id", "session_uuid", "start", "end", "duration_seconds", "category_id", "category", "tag",
            "archived", "end_reason", "start_source", "focus_rating", "category_snapshot", "tag_snapshot",
            "color_index_snapshot", "tz_offset_seconds", "app_version",
        ),
        rows.map { r ->
            listOf(
                r.sessionId.toString(),
                r.uuid,
                iso(r.startedAt, r.tzOffsetSeconds, zone),
                iso(r.endedAt, r.tzOffsetSeconds, zone),
                ((r.endedAt - r.startedAt) / 1000).toString(),
                r.categoryId.toString(),
                r.categoryName,
                r.tag.name,
                r.isArchived.toString(),
                r.endReason.name,
                r.startSource.name,
                r.focusRating?.toString(),
                r.snapshotName,
                r.snapshotTag.name,
                r.snapshotColorIndex.toString(),
                r.tzOffsetSeconds.toString(),
                r.appVersion,
            )
        },
    )

    fun events(events: List<EventEntity>, zone: ZoneId): String = csv(
        listOf("event_uuid", "occurred_at", "type", "category_id", "goal_id", "payload", "tz_offset_seconds", "app_version"),
        events.map { e ->
            listOf(
                e.uuid,
                iso(e.occurredAt, e.tzOffsetSeconds, zone),
                e.type.name,
                e.categoryId?.toString(),
                e.goalId?.toString(),
                e.payload,
                e.tzOffsetSeconds.toString(),
                e.appVersion,
            )
        },
    )

    fun dailyGoalStatus(rows: List<DailyGoalStatusEntity>, zone: ZoneId): String = csv(
        listOf(
            "day", "goal_id", "scope_key", "category_id", "tag", "goal_type", "target_seconds", "actual_seconds",
            "achieved", "goal_active", "frozen_at",
        ),
        rows.map { r ->
            listOf(
                r.day,
                r.goalId.toString(),
                r.scopeKey,
                r.categoryId?.toString(),
                r.tag?.name,
                r.goalType.name,
                r.targetSeconds.toString(),
                r.actualSeconds.toString(),
                r.achieved.toString(),
                r.goalActive.toString(),
                iso(r.frozenAt, null, zone),
            )
        },
    )

    fun categories(categories: List<CategoryEntity>, zone: ZoneId): String = csv(
        listOf("category_id", "name", "tag", "color_index", "archived", "created_at"),
        categories.map { c ->
            listOf(
                c.id.toString(),
                c.name,
                c.tag.name,
                c.colorIndex.toString(),
                c.isArchived.toString(),
                iso(c.createdAt, null, zone),
            )
        },
    )

    fun goals(goals: List<GoalEntity>, zone: ZoneId): String = csv(
        listOf("goal_id", "scope_key", "category_id", "tag", "period", "goal_type", "target_seconds", "created_at"),
        goals.map { g ->
            listOf(
                g.id.toString(),
                g.scopeKey,
                g.categoryId?.toString(),
                g.tag?.name,
                g.period.name,
                g.goalType.name,
                g.targetSeconds.toString(),
                iso(g.createdAt, null, zone),
            )
        },
    )

    fun meta(meta: List<PoolMetaEntity>): String = csv(
        listOf("key", "value"),
        meta.map { listOf(it.key, it.value) },
    )

    private fun csv(header: List<String>, rows: List<List<String?>>): String {
        val sb = StringBuilder()
        sb.append(header.joinToString(",")).append('\n')
        rows.forEach { row -> sb.append(row.joinToString(",") { field(it) }).append('\n') }
        return sb.toString()
    }

    /** RFC 4180: virgül, tırnak veya satır sonu içeren alanlar tırnaklanır; null boş alan olur. */
    private fun field(value: String?): String {
        if (value == null) return ""
        return if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    /** ISO 8601 + ofset. Ofset kayıtlıysa onu, değilse cihazın o andaki ofsetini kullanır. */
    private fun iso(epochMs: Long, offsetSeconds: Int?, zone: ZoneId): String {
        val instant = Instant.ofEpochMilli(epochMs)
        val offset = offsetSeconds?.let { ZoneOffset.ofTotalSeconds(it) } ?: zone.rules.getOffset(instant)
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(instant.atOffset(offset))
    }
}

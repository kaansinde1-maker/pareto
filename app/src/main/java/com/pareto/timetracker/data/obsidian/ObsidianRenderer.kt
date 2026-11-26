package com.pareto.timetracker.data.obsidian

import com.pareto.timetracker.data.db.PoolSessionRow
import com.pareto.timetracker.domain.Tag
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Günlük dosyadaki bir günlük-hedef satırı. */
data class GoalLine(
    val scope: String, // "kategori" | "etiket"
    val name: String,
    val type: String, // "min" | "max"
    val targetSeconds: Long,
    val actualSeconds: Long,
    val achieved: Boolean,
    val active: Boolean,
)

/** Bir yerel günün toplamları. Süreler gün sınırına kırpılmıştır; sayaç ve odak puanı başlangıç gününe yazılır. */
class DayAggregate {
    var investmentMs = 0L
    var consumptionMs = 0L
    val categoryMs = HashMap<String, Long>()
    val categoryIdMs = HashMap<Long, Long>()
    var sessionCount = 0
    var firstStartMs: Long? = null
    var lastEndMs: Long? = null
    var longestMs = 0L
    var focusSum = 0
    var focusCount = 0

    val totalMs: Long get() = investmentMs + consumptionMs
}

/** Saf (Android'siz) üretici fonksiyonlar: günlük markdown dosyaları ve günlük toplamlar. CSV için bkz. PoolCsv. */
object ObsidianRenderer {

    private val HHMM = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)

    // ---------- toplama ----------

    fun aggregate(rows: List<PoolSessionRow>, zone: ZoneId): Map<LocalDate, DayAggregate> {
        val days = HashMap<LocalDate, DayAggregate>()
        for (row in rows) {
            if (row.endedAt <= row.startedAt) continue
            val startDay = Instant.ofEpochMilli(row.startedAt).atZone(zone).toLocalDate()

            val startAgg = days.getOrPut(startDay) { DayAggregate() }
            startAgg.sessionCount++
            row.focusRating?.let {
                startAgg.focusSum += it
                startAgg.focusCount++
            }

            var day = startDay
            while (true) {
                val dayStart = day.atStartOfDay(zone).toInstant().toEpochMilli()
                val dayEnd = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val segStart = maxOf(row.startedAt, dayStart)
                val segEnd = minOf(row.endedAt, dayEnd)
                if (segEnd > segStart) {
                    val agg = days.getOrPut(day) { DayAggregate() }
                    val ms = segEnd - segStart
                    when (row.tag) {
                        Tag.INVESTMENT -> agg.investmentMs += ms
                        Tag.CONSUMPTION -> agg.consumptionMs += ms
                    }
                    agg.categoryMs.merge(row.categoryName, ms, Long::plus)
                    agg.categoryIdMs.merge(row.categoryId, ms, Long::plus)
                    if (ms > agg.longestMs) agg.longestMs = ms
                    agg.firstStartMs = agg.firstStartMs?.let { minOf(it, segStart) } ?: segStart
                    agg.lastEndMs = agg.lastEndMs?.let { maxOf(it, segEnd) } ?: segEnd
                }
                if (row.endedAt <= dayEnd) break
                day = day.plusDays(1)
            }
        }
        return days
    }

    // ---------- günlük markdown ----------

    fun renderDay(
        day: LocalDate,
        agg: DayAggregate?,
        goals: List<GoalLine>,
        finalized: Boolean,
        zone: ZoneId,
    ): String {
        val a = agg ?: DayAggregate()
        val dayEndMs = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val sb = StringBuilder()
        sb.appendLine("---")
        sb.appendLine("date: $day")
        sb.appendLine("yatirim_dk: ${minutes(a.investmentMs)}")
        sb.appendLine("tuketim_dk: ${minutes(a.consumptionMs)}")
        sb.appendLine("toplam_dk: ${minutes(a.totalMs)}")
        sb.appendLine("oturum_sayisi: ${a.sessionCount}")
        a.firstStartMs?.let { sb.appendLine("ilk_oturum: ${yaml(clock(it, zone, dayEndMs))}") }
        a.lastEndMs?.let { sb.appendLine("son_oturum: ${yaml(clock(it, zone, dayEndMs))}") }
        if (a.longestMs > 0) sb.appendLine("en_uzun_oturum_dk: ${minutes(a.longestMs)}")
        if (a.focusCount > 0) {
            sb.appendLine("ort_odak: ${String.format(Locale.ROOT, "%.2f", a.focusSum.toDouble() / a.focusCount)}")
        }

        if (a.categoryMs.isEmpty()) {
            sb.appendLine("kategoriler: {}")
        } else {
            sb.appendLine("kategoriler:")
            a.categoryMs.entries.sortedByDescending { it.value }.forEach { (name, ms) ->
                sb.appendLine("  ${yaml(name)}: ${minutes(ms)}")
            }
        }

        if (goals.isEmpty()) {
            sb.appendLine("hedefler: []")
        } else {
            sb.appendLine("hedefler:")
            goals.forEach { g ->
                sb.appendLine("  - kapsam: ${yaml(g.scope)}")
                sb.appendLine("    hedef: ${yaml(g.name)}")
                sb.appendLine("    tip: ${yaml(g.type)}")
                sb.appendLine("    hedef_dk: ${minutes(g.targetSeconds * 1000)}")
                sb.appendLine("    gerceklesen_dk: ${minutes(g.actualSeconds * 1000)}")
                sb.appendLine("    tamam: ${g.achieved}")
                sb.appendLine("    aktif: ${g.active}")
            }
        }
        sb.appendLine("kesinlesti: $finalized")
        sb.appendLine("---")
        sb.appendLine()
        sb.appendLine("%% 80/20 uygulaması tarafından üretilir; elle düzenleme, üzerine yazılır. %%")
        return sb.toString()
    }

    // ---------- yardımcılar ----------

    private fun clock(epochMs: Long, zone: ZoneId, dayEndMs: Long): String =
        if (epochMs >= dayEndMs) "24:00" else HHMM.format(Instant.ofEpochMilli(epochMs).atZone(zone))

    /** Dakika, tek ondalık ("95.5"). Ondalık ayracı her zaman nokta. */
    private fun minutes(ms: Long): String = String.format(Locale.ROOT, "%.1f", ms / 60_000.0)

    /** YAML çift tırnaklı metin (zaman gibi değerler yanlış tipe çevrilmesin diye hep tırnaklı). */
    private fun yaml(value: String): String =
        "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ") + "\""
}

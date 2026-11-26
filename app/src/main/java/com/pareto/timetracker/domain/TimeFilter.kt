package com.pareto.timetracker.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

enum class TimeFilter { DAILY, WEEKLY, MONTHLY, ALL_TIME, CUSTOM }

/**
 * @param offset 0 = bu dönem, -1 = önceki dönem... (gelecek yok, hep <= 0)
 */
data class FilterState(
    val type: TimeFilter = TimeFilter.DAILY,
    val offset: Int = 0,
    val customStart: LocalDate? = null,
    val customEnd: LocalDate? = null,
)

/** Çözülmüş dönem. ALL_TIME için tarihler null. [endInclusive] gün olarak dahildir. */
data class Period(
    val type: TimeFilter,
    val range: TimeRangeMs,
    val start: LocalDate?,
    val endInclusive: LocalDate?,
)

/** Takvim bazlı dönemler: hafta Pazartesi başlar, ay ayın 1'i ile başlar. */
fun resolvePeriod(state: FilterState, today: LocalDate, zone: ZoneId): Period {
    fun ms(date: LocalDate) = date.atStartOfDay(zone).toInstant().toEpochMilli()
    fun period(start: LocalDate, endInclusive: LocalDate) = Period(
        type = state.type,
        range = TimeRangeMs(ms(start), ms(endInclusive.plusDays(1))),
        start = start,
        endInclusive = endInclusive,
    )

    return when (state.type) {
        TimeFilter.DAILY -> {
            val day = today.plusDays(state.offset.toLong())
            period(day, day)
        }
        TimeFilter.WEEKLY -> {
            val start = today
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .plusWeeks(state.offset.toLong())
            period(start, start.plusDays(6))
        }
        TimeFilter.MONTHLY -> {
            val start = today.withDayOfMonth(1).plusMonths(state.offset.toLong())
            period(start, start.plusMonths(1).minusDays(1))
        }
        TimeFilter.ALL_TIME -> Period(
            type = state.type,
            range = TimeRangeMs(0L, Long.MAX_VALUE),
            start = null,
            endInclusive = null,
        )
        TimeFilter.CUSTOM -> {
            val start = state.customStart ?: today
            val end = state.customEnd?.takeIf { !it.isBefore(start) } ?: start
            period(start, end)
        }
    }
}

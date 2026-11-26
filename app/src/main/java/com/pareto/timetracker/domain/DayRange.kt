package com.pareto.timetracker.domain

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import java.time.ZoneId

/** [başlangıç, bitiş) epoch ms. */
data class TimeRangeMs(val fromMs: Long, val toMs: Long)

private const val CHECK_INTERVAL_MS = 30_000L

/**
 * Bugünün aralığını yayar; gün (veya saat dilimi) değişince yenisini yayar.
 * Tek uzun bir gecikme yerine kısa aralıklarla kontrol eder: coroutine gecikmeleri cihaz derin uykudayken
 * ilerlemediği için gece boyunca açık kalan uygulamada "bugün" sabaha kadar eski gün kalabilirdi.
 */
fun todayRangeFlow(): Flow<TimeRangeMs> = flow {
    var last: TimeRangeMs? = null
    while (true) {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val range = TimeRangeMs(
            fromMs = today.atStartOfDay(zone).toInstant().toEpochMilli(),
            toMs = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),
        )
        if (range != last) {
            emit(range)
            last = range
        }
        delay(CHECK_INTERVAL_MS)
    }
}

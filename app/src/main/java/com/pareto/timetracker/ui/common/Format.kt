package com.pareto.timetracker.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.pareto.timetracker.R
import java.util.Locale

/** "2 sa 5 dk", "45 dk", "30 sn" biçiminde okunur süre. */
@Composable
fun durationText(ms: Long): String {
    val totalSeconds = ms.coerceAtLeast(0) / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 && minutes > 0 -> stringResource(R.string.duration_hm, hours, minutes)
        hours > 0 -> stringResource(R.string.duration_h, hours)
        minutes > 0 -> stringResource(R.string.duration_m, minutes)
        else -> stringResource(R.string.duration_s, seconds)
    }
}

/** Canlı sayaç için "00:12:05". */
fun formatClock(ms: Long): String {
    val total = ms.coerceAtLeast(0) / 1000
    return String.format(Locale.ROOT, "%02d:%02d:%02d", total / 3600, (total % 3600) / 60, total % 60)
}

private val TR = Locale.forLanguageTag("tr-TR")

/** Gerçek yüzde ("65,3%"). Toplam yoksa "–". */
fun percentText(part: Long, total: Long): String =
    if (total <= 0) "–" else String.format(TR, "%.1f%%", part * 100.0 / total)

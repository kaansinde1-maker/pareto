package com.pareto.timetracker.ui.charts

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pareto.timetracker.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Özel tarih aralığı: gelecek tarihler seçilemez. Tek gün seçilirse başlangıç = bitiş. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomRangeDialog(
    onConfirm: (start: LocalDate, end: LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    // Takvim bileşeni günleri UTC gece yarısı olarak verir.
    val today = remember { LocalDate.now() }
    val todayUtcMs = remember(today) {
        today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }
    val state = rememberDateRangePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= todayUtcMs
            override fun isSelectableYear(year: Int): Boolean = year <= today.year
        },
    )

    fun toDate(utcMs: Long): LocalDate = Instant.ofEpochMilli(utcMs).atZone(ZoneOffset.UTC).toLocalDate()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = state.selectedStartDateMillis != null,
                onClick = {
                    val start = toDate(state.selectedStartDateMillis ?: return@TextButton)
                    val end = state.selectedEndDateMillis?.let(::toDate) ?: start
                    onConfirm(start, end)
                },
            ) { Text(stringResource(R.string.charts_pick_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_dismiss)) }
        },
    ) {
        DateRangePicker(
            state = state,
            modifier = Modifier.height(520.dp),
            title = {
                Text(
                    text = stringResource(R.string.charts_pick_range),
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                )
            },
            showModeToggle = false,
        )
    }
}

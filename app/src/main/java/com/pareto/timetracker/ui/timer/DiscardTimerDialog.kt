package com.pareto.timetracker.ui.timer

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.pareto.timetracker.R

/**
 * Çalışan timer'ı kaydetmeden silme onayı (uygulama içi ve bildirimden gelen akışın ortak diyaloğu).
 * "Durdur" süreyi kaydeder; "Sil" kaydetmez. Diyaloğu kapatmak timer'ı çalışır halde bırakır.
 */
@Composable
fun DiscardTimerDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.discard_dialog_title)) },
        text = { Text(stringResource(R.string.discard_dialog_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.discard_dialog_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.discard_dialog_keep)) }
        },
    )
}

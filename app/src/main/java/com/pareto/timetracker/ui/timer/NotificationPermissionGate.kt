package com.pareto.timetracker.ui.timer

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Bildirim iznini (Android 13+) ilk timer başlatılırken ister.
 * İzin verilse de verilmese de timer başlar; reddedilirse sadece bildirim görünmez.
 * Dönen fonksiyonu kategori kartının tıklamasında çağırın.
 */
@Composable
fun rememberTimerStartGate(onStart: (categoryId: Long) -> Unit): (Long) -> Unit {
    val context = LocalContext.current
    val currentOnStart by rememberUpdatedState(onStart)
    var pendingCategoryId by remember { mutableStateOf<Long?>(null) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        pendingCategoryId?.let(currentOnStart)
        pendingCategoryId = null
    }

    return { categoryId ->
        val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (needsPermission) {
            pendingCategoryId = categoryId
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            currentOnStart(categoryId)
        }
    }
}

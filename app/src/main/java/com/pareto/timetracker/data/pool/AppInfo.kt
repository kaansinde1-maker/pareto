package com.pareto.timetracker.data.pool

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** Her havuz kaydına yazılan uygulama sürümü ve saat dilimi ofseti. */
@Singleton
class AppInfo @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val versionName: String by lazy {
        runCatching {
            val pm = context.packageManager
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(context.packageName, 0)
            }
            info.versionName
        }.getOrNull() ?: "unknown"
    }

    fun tzOffsetSeconds(atMs: Long = System.currentTimeMillis()): Int =
        ZoneId.systemDefault().rules.getOffset(Instant.ofEpochMilli(atMs)).totalSeconds
}

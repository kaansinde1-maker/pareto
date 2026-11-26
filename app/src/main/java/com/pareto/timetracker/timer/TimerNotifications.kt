package com.pareto.timetracker.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.pareto.timetracker.MainActivity
import com.pareto.timetracker.R
import com.pareto.timetracker.data.db.CategoryEntity
import com.pareto.timetracker.domain.CategoryPalette

object TimerNotifications {
    private const val CHANNEL_ID = "timer"
    const val NOTIFICATION_ID = 1001

    private const val PI_FLAGS = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

    /** Sessiz kanal: ses ve titreşim yok (titreşimi TimerHaptics verir, çifte titreşim olmasın). */
    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            enableVibration(false)
            setSound(null, null)
            setShowBadge(false)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** Servis başlar başlamaz (5 sn kuralı) gösterilir; veri gelince [running] ile değişir. */
    fun placeholder(context: Context): Notification =
        base(context)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.notification_running))
            .build()

    fun running(context: Context, startedAtMs: Long, category: CategoryEntity): Notification =
        base(context)
            .setContentTitle(category.name)
            .setContentText(context.getString(category.tag.labelRes))
            .setColor(CategoryPalette.color(category.tag, category.colorIndex))
            .setWhen(startedAtMs)
            .setShowWhen(true)
            .setUsesChronometer(true)
            .addAction(0, context.getString(R.string.action_stop), stopIntent(context))
            .addAction(0, context.getString(R.string.action_discard), discardIntent(context))
            .build()

    private fun base(context: Context): NotificationCompat.Builder =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(openAppIntent(context))

    private fun openAppIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PI_FLAGS,
        )

    private fun stopIntent(context: Context): PendingIntent =
        PendingIntent.getService(
            context,
            1,
            Intent(context, TimerService::class.java).setAction(TimerService.ACTION_STOP),
            PI_FLAGS,
        )

    /** İptal doğrudan uygulanmaz; önce onay diyaloğunu gösteren activity açılır. */
    private fun discardIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            2,
            Intent(context, DiscardTimerActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PI_FLAGS,
        )
}

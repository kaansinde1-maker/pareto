package com.pareto.timetracker.timer

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.pareto.timetracker.data.repo.CategoryRepository
import com.pareto.timetracker.data.repo.TimerRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Sadece bildirimi yaşatır. Timer'ın kendisi veritabanındaki zaman damgasıdır:
 * servis ölse bile süre doğru kalır. Çalışan timer kalmayınca servis kendini kapatır.
 */
@AndroidEntryPoint
class TimerService : Service() {

    @Inject lateinit var timerRepository: TimerRepository
    @Inject lateinit var categoryRepository: CategoryRepository
    @Inject lateinit var controller: TimerController

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observeJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        TimerNotifications.ensureChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // startForegroundService sonrası 5 sn içinde foreground'a geçmek zorunlu.
        enterForeground(TimerNotifications.placeholder(this))

        if (intent?.action == ACTION_STOP) controller.stopFromNotification()

        if (observeJob == null) {
            observeJob = scope.launch {
                combine(
                    timerRepository.activeTimer,
                    categoryRepository.allCategories,
                ) { timer, categories ->
                    timer to categories.firstOrNull { it.id == timer?.categoryId }
                }.collect { (timer, category) ->
                    when {
                        timer == null -> {
                            ServiceCompat.stopForeground(this@TimerService, ServiceCompat.STOP_FOREGROUND_REMOVE)
                            stopSelf()
                        }
                        category != null -> enterForeground(
                            TimerNotifications.running(this@TimerService, timer.startedAt, category),
                        )
                    }
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun enterForeground(notification: Notification) {
        ServiceCompat.startForeground(
            this,
            TimerNotifications.NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MANIFEST,
        )
    }

    companion object {
        const val ACTION_STOP = "com.pareto.timetracker.action.STOP"
    }
}

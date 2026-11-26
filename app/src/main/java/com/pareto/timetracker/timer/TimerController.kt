package com.pareto.timetracker.timer

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.pareto.timetracker.data.repo.SavedSession
import com.pareto.timetracker.data.repo.TimerRepository
import com.pareto.timetracker.data.repo.TimerTransition
import com.pareto.timetracker.domain.EndReason
import com.pareto.timetracker.domain.TimerRules
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Timer işlemlerinin tek giriş noktası (arayüz ve bildirim aynı yolu kullanır).
 * Kendi uygulama kapsamında çalışır; servis kapansa bile titreşim ve kayıt tamamlanır.
 */
@Singleton
class TimerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: TimerRepository,
    private val haptics: TimerHaptics,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _focusPromptSessions = MutableSharedFlow<SavedSession>(extraBufferCapacity = 1)

    /**
     * Odak puanı sorulacak oturumlar: sadece uygulama içinden (karta dokunarak) durdurulan
     * ve en az 5 dakika süren oturumlar. Bildirimden durdurma, iptal ve otomatik durmada yayılmaz.
     */
    val focusPromptSessions: SharedFlow<SavedSession> = _focusPromptSessions.asSharedFlow()

    /** Kategori kartına tek dokunuş: çalışıyorsa durdurur, değilse başlatır. */
    fun toggle(categoryId: Long) {
        scope.launch {
            val result = repository.toggle(categoryId)
            handle(result.transition)
            result.savedSession
                ?.takeIf { result.transition == TimerTransition.STOPPED }
                ?.takeIf { it.durationMs >= TimerRules.FOCUS_PROMPT_MIN_MS }
                ?.let { _focusPromptSessions.tryEmit(it) }
        }
    }

    /** Bildirimdeki Durdur butonu. */
    fun stopFromNotification() {
        scope.launch { handle(repository.stop(EndReason.NOTIFICATION_STOP).transition) }
    }

    /** Çalışan süreyi kaydetmeden siler ("Süreyi sil"). Onay diyaloğu çağıranın sorumluluğundadır. */
    fun cancel() {
        scope.launch { handle(repository.cancel().transition) }
    }

    /** Uygulama açılırken: DB'de çalışan timer varsa ama servis yoksa (ör. yeniden başlatma) servisi başlatır. */
    fun resumeServiceIfRunning() {
        scope.launch {
            if (repository.activeTimer.first() != null) startService()
        }
    }

    private fun handle(transition: TimerTransition) {
        when (transition) {
            TimerTransition.STARTED -> {
                startService()
                haptics.playStart()
            }
            TimerTransition.STOPPED -> haptics.playStop()
            TimerTransition.CANCELLED, TimerTransition.NONE -> Unit
        }
    }

    private fun startService() {
        try {
            ContextCompat.startForegroundService(context, Intent(context, TimerService::class.java))
        } catch (e: Exception) {
            // Arka plan kısıtlaması vb. Timer verisi DB'de güvende; bildirim sonra açılışta yenilenir.
            Log.w(TAG, "Could not start the timer service", e)
        }
    }

    private companion object {
        const val TAG = "TimerController"
    }
}

package com.pareto.timetracker.data.repo

import androidx.room.withTransaction
import com.pareto.timetracker.data.db.ActiveTimerDao
import com.pareto.timetracker.data.db.ActiveTimerEntity
import com.pareto.timetracker.data.db.AppDatabase
import com.pareto.timetracker.data.db.CategoryDao
import com.pareto.timetracker.data.db.SessionDao
import com.pareto.timetracker.data.db.SessionEntity
import com.pareto.timetracker.data.pool.AppInfo
import com.pareto.timetracker.data.pool.EventLogger
import com.pareto.timetracker.data.pool.jsonOf
import com.pareto.timetracker.domain.EndReason
import com.pareto.timetracker.domain.EventType
import com.pareto.timetracker.domain.StartSource
import com.pareto.timetracker.domain.TimerRules
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Timer geçişinin sonucu. Titreşim/bildirim katmanı buna göre tepki verir. */
enum class TimerTransition { STARTED, STOPPED, CANCELLED, NONE }

/** Kaydedilen (>= 10 sn) bir oturum. */
data class SavedSession(val sessionId: Long, val categoryId: Long, val durationMs: Long)

data class TimerResult(
    val transition: TimerTransition,
    /** Sadece bir oturum kaydedildiyse dolu. */
    val savedSession: SavedSession? = null,
)

/**
 * Timer durumu veritabanında zaman damgası olarak tutulur; sayaç yoktur.
 * Geçen süre = şimdi - startedAt, bu yüzden uygulama kapansa da doğru kalır.
 * Atılan (10 sn altı) ve iptal edilen oturumlar için olay günlüğüne kayıt yazılır.
 */
@Singleton
class TimerRepository @Inject constructor(
    private val db: AppDatabase,
    private val timerDao: ActiveTimerDao,
    private val sessionDao: SessionDao,
    private val categoryDao: CategoryDao,
    private val eventLogger: EventLogger,
    private val appInfo: AppInfo,
) {
    val activeTimer: Flow<ActiveTimerEntity?> = timerDao.observe()

    /** Karta tek dokunuş: o kategori çalışıyorsa durdurur, değilse başlatır (varsa öncekini durdurarak). */
    suspend fun toggle(categoryId: Long): TimerResult = db.withTransaction {
        val running = timerDao.get()
        val now = now()
        if (running != null && running.categoryId == categoryId) {
            TimerResult(TimerTransition.STOPPED, finish(running, now, EndReason.MANUAL_STOP))
        } else {
            running?.let { finish(it, now, EndReason.SWITCHED) }
            timerDao.set(ActiveTimerEntity(categoryId = categoryId, startedAt = now))
            TimerResult(TimerTransition.STARTED)
        }
    }

    /** Çalışan timer'ı durdurur ve (>= 10 sn ise) oturum olarak kaydeder. */
    suspend fun stop(reason: EndReason = EndReason.MANUAL_STOP): TimerResult = db.withTransaction {
        val running = timerDao.get() ?: return@withTransaction TimerResult(TimerTransition.NONE)
        TimerResult(TimerTransition.STOPPED, finish(running, now(), reason))
    }

    /** Çalışan timer'ı oturum kaydetmeden siler (kullanıcıya "Süreyi sil"); olay günlüğüne SESSION_CANCELLED yazılır. */
    suspend fun cancel(): TimerResult = db.withTransaction {
        val running = timerDao.get() ?: return@withTransaction TimerResult(TimerTransition.NONE)
        val now = now()
        timerDao.clear()
        eventLogger.log(
            type = EventType.SESSION_CANCELLED,
            categoryId = running.categoryId,
            payload = jsonOf(
                "started_at" to running.startedAt,
                "ended_at" to now,
                "duration_ms" to (now - running.startedAt).coerceAtLeast(0L),
            ),
            occurredAt = now,
        )
        TimerResult(TimerTransition.CANCELLED)
    }

    /** Verilen kategori çalışıyorsa durdurup kaydeder (arşivleme öncesi kullanılır). */
    suspend fun stopIfRunningFor(categoryId: Long) {
        db.withTransaction {
            val running = timerDao.get()
            if (running != null && running.categoryId == categoryId) {
                finish(running, now(), EndReason.ARCHIVED)
            }
        }
    }

    private suspend fun finish(running: ActiveTimerEntity, endedAt: Long, reason: EndReason): SavedSession? {
        timerDao.clear()
        val durationMs = endedAt - running.startedAt

        if (durationMs < TimerRules.MIN_SESSION_MS) {
            eventLogger.log(
                type = EventType.SESSION_DISCARDED,
                categoryId = running.categoryId,
                payload = jsonOf(
                    "started_at" to running.startedAt,
                    "ended_at" to endedAt,
                    "duration_ms" to durationMs.coerceAtLeast(0L),
                    "end_reason" to reason.name,
                ),
                occurredAt = endedAt,
            )
            return null
        }

        val category = categoryDao.getById(running.categoryId) ?: return null
        val sessionId = sessionDao.insert(
            SessionEntity(
                categoryId = running.categoryId,
                uuid = UUID.randomUUID().toString(),
                startedAt = running.startedAt,
                endedAt = endedAt,
                endReason = reason,
                startSource = StartSource.CARD,
                categoryName = category.name,
                categoryTag = category.tag,
                categoryColorIndex = category.colorIndex,
                tzOffsetSeconds = appInfo.tzOffsetSeconds(running.startedAt),
                appVersion = appInfo.versionName,
            ),
        )
        return SavedSession(sessionId, running.categoryId, durationMs)
    }

    private fun now() = System.currentTimeMillis()
}

package com.pareto.timetracker.data.pool

import android.os.Build
import androidx.room.withTransaction
import com.pareto.timetracker.data.db.AppDatabase
import com.pareto.timetracker.data.db.CategoryDao
import com.pareto.timetracker.data.db.DailyGoalStatusDao
import com.pareto.timetracker.data.db.DailyGoalStatusEntity
import com.pareto.timetracker.data.db.GoalDao
import com.pareto.timetracker.data.db.PoolMetaDao
import com.pareto.timetracker.data.db.PoolMetaEntity
import com.pareto.timetracker.data.db.SessionDao
import com.pareto.timetracker.domain.GoalPeriod
import com.pareto.timetracker.domain.GoalType
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Sessiz veri havuzunun yazma tarafı: cihaz meta verisi, odak puanı, günlük hedef durumlarını dondurma. */
@Singleton
class PoolRepository @Inject constructor(
    private val db: AppDatabase,
    private val goalDao: GoalDao,
    private val categoryDao: CategoryDao,
    private val sessionDao: SessionDao,
    private val statusDao: DailyGoalStatusDao,
    private val metaDao: PoolMetaDao,
    private val appInfo: AppInfo,
) {
    private val freezeMutex = Mutex()

    /** Uygulama açılışında: cihaz kimliği (bir kez) ve güncel cihaz/sürüm bilgisi. */
    suspend fun ensureMeta() {
        metaDao.putIfAbsent(PoolMetaEntity("device_id", UUID.randomUUID().toString()))
        metaDao.putIfAbsent(PoolMetaEntity("first_run_at", System.currentTimeMillis().toString()))
        metaDao.put(PoolMetaEntity("device_model", "${Build.MANUFACTURER} ${Build.MODEL}"))
        metaDao.put(PoolMetaEntity("android_version", "${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"))
        metaDao.put(PoolMetaEntity("app_version", appInfo.versionName))
        metaDao.put(PoolMetaEntity("pool_schema_version", POOL_SCHEMA_VERSION))
    }

    suspend fun setFocusRating(sessionId: Long, rating: Int) {
        sessionDao.setFocusRating(sessionId, rating.coerceIn(1, 5))
    }

    /**
     * Günü bitmiş (dün ve öncesi) günlük hedefler için durumu dondurur; boş günler dahil her gün için satır yazar.
     * Hedef, kategori adı/etiketi veya arşiv durumu DEĞİŞMEDEN hemen önce çağrılmalıdır; böylece geçmiş günler
     * eski tanımla dondurulur ve sonradan yapılan değişiklikler onları etkilemez.
     * Zaten yazılmış günler atlanır (tekrar çağırmak güvenlidir).
     */
    suspend fun freezeCompletedDays() {
        freezeMutex.withLock {
            val zone = ZoneId.systemDefault()
            val yesterday = LocalDate.now(zone).minusDays(1)
            val goals = goalDao.getByPeriod(GoalPeriod.DAILY)
            if (goals.isEmpty()) return

            val categories = categoryDao.getAll().associateBy { it.id }
            val durationsByDay = HashMap<LocalDate, Map<Long, Long>>()
            val frozenAt = System.currentTimeMillis()
            val rows = mutableListOf<DailyGoalStatusEntity>()

            for (goal in goals) {
                val createdDay = Instant.ofEpochMilli(goal.createdAt).atZone(zone).toLocalDate()
                val lastFrozen = statusDao.lastDay(goal.id)?.let(LocalDate::parse)
                var day = if (lastFrozen != null && !lastFrozen.plusDays(1).isBefore(createdDay)) {
                    lastFrozen.plusDays(1)
                } else {
                    createdDay
                }

                while (!day.isAfter(yesterday)) {
                    val byCategory = durationsByDay.getOrPut(day) {
                        val from = day.atStartOfDay(zone).toInstant().toEpochMilli()
                        val to = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
                        sessionDao.getDurationsByCategory(from, to).associate { it.categoryId to it.durationMs }
                    }
                    val actualMs = if (goal.categoryId != null) {
                        byCategory[goal.categoryId] ?: 0L
                    } else {
                        byCategory.filterKeys { categories[it]?.tag == goal.tag }.values.sum()
                    }
                    val actualSeconds = actualMs / 1000
                    rows += DailyGoalStatusEntity(
                        day = day.toString(),
                        goalId = goal.id,
                        scopeKey = goal.scopeKey,
                        categoryId = goal.categoryId,
                        tag = goal.tag,
                        goalType = goal.goalType,
                        targetSeconds = goal.targetSeconds,
                        actualSeconds = actualSeconds,
                        achieved = when (goal.goalType) {
                            GoalType.MIN -> actualSeconds >= goal.targetSeconds
                            GoalType.MAX -> actualSeconds <= goal.targetSeconds
                        },
                        goalActive = goal.categoryId?.let { categories[it]?.isArchived != true } ?: true,
                        frozenAt = frozenAt,
                    )
                    day = day.plusDays(1)
                }
            }
            if (rows.isNotEmpty()) db.withTransaction { statusDao.insertAll(rows) }
        }
    }

    private companion object {
        const val POOL_SCHEMA_VERSION = "1"
    }
}

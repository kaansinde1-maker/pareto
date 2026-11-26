package com.pareto.timetracker.data.pool

import com.pareto.timetracker.data.db.EventDao
import com.pareto.timetracker.data.db.EventEntity
import com.pareto.timetracker.domain.EventType
import org.json.JSONObject
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Olay günlüğüne ekleme yapar. Kayıtlar asla güncellenmez veya silinmez (kategori kalıcı silinmesi hariç). */
@Singleton
class EventLogger @Inject constructor(
    private val dao: EventDao,
    private val appInfo: AppInfo,
) {
    suspend fun log(
        type: EventType,
        categoryId: Long? = null,
        goalId: Long? = null,
        payload: JSONObject? = null,
        occurredAt: Long = System.currentTimeMillis(),
    ) {
        dao.insert(
            EventEntity(
                uuid = UUID.randomUUID().toString(),
                occurredAt = occurredAt,
                tzOffsetSeconds = appInfo.tzOffsetSeconds(occurredAt),
                appVersion = appInfo.versionName,
                type = type,
                categoryId = categoryId,
                goalId = goalId,
                payload = payload?.toString(),
            ),
        )
    }
}

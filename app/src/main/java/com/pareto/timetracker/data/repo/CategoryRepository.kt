package com.pareto.timetracker.data.repo

import androidx.room.withTransaction
import com.pareto.timetracker.data.db.AppDatabase
import com.pareto.timetracker.data.db.CategoryDao
import com.pareto.timetracker.data.db.CategoryEntity
import com.pareto.timetracker.data.pool.EventLogger
import com.pareto.timetracker.data.pool.PoolRepository
import com.pareto.timetracker.data.pool.jsonOf
import com.pareto.timetracker.data.pool.toEventJson
import com.pareto.timetracker.domain.CategoryPalette
import com.pareto.timetracker.domain.EventType
import com.pareto.timetracker.domain.Tag
import kotlinx.coroutines.flow.Flow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

sealed interface SaveResult {
    data class Success(val id: Long) : SaveResult
    data object BlankName : SaveResult

    /** Aynı ad zaten var. Arşivdeyse arayüz "geri yükle?" önerebilir. */
    data class NameTaken(val existingId: Long, val existingIsArchived: Boolean) : SaveResult
}

@Singleton
class CategoryRepository @Inject constructor(
    private val db: AppDatabase,
    private val dao: CategoryDao,
    private val timerRepository: TimerRepository,
    private val poolRepository: PoolRepository,
    private val eventLogger: EventLogger,
) {
    val activeCategories: Flow<List<CategoryEntity>> = dao.observeActive()
    val archivedCategories: Flow<List<CategoryEntity>> = dao.observeArchived()
    val allCategories: Flow<List<CategoryEntity>> = dao.observeAll()

    suspend fun add(name: String, tag: Tag, colorIndex: Int): SaveResult {
        val trimmed = name.trim().take(MAX_NAME_LENGTH)
        if (trimmed.isEmpty()) return SaveResult.BlankName
        val key = nameKey(trimmed)
        dao.findByNameKey(key)?.let { return SaveResult.NameTaken(it.id, it.isArchived) }

        val entity = CategoryEntity(
            name = trimmed,
            nameKey = key,
            tag = tag,
            colorIndex = colorIndex.coerceIn(0, CategoryPalette.TONE_COUNT - 1),
            createdAt = System.currentTimeMillis(),
        )
        val id = db.withTransaction {
            val newId = dao.insert(entity)
            eventLogger.log(
                type = EventType.CATEGORY_CREATED,
                categoryId = newId,
                payload = entity.copy(id = newId).toEventJson(),
            )
            newId
        }
        return SaveResult.Success(id)
    }

    /**
     * Tag değişimi geriye dönüktür: Tag kategoriye aittir, oturuma değil.
     * Ton sırası korunur; yeni Tag paletinde aynı sıradaki tona eşlenir.
     */
    suspend fun update(id: Long, name: String, tag: Tag, colorIndex: Int): SaveResult {
        val current = dao.getById(id) ?: return SaveResult.BlankName
        val trimmed = name.trim().take(MAX_NAME_LENGTH)
        if (trimmed.isEmpty()) return SaveResult.BlankName
        val key = nameKey(trimmed)
        dao.findByNameKey(key)?.let {
            if (it.id != id) return SaveResult.NameTaken(it.id, it.isArchived)
        }

        val updated = current.copy(
            name = trimmed,
            nameKey = key,
            tag = tag,
            colorIndex = colorIndex.coerceIn(0, CategoryPalette.TONE_COUNT - 1),
        )
        if (updated == current) return SaveResult.Success(id)

        // Etiket tag kapsamlı günlük hedeflerin gerçekleşenini değiştirir: geçmiş günler eski etiketle dondurulsun.
        if (updated.tag != current.tag) poolRepository.freezeCompletedDays()
        db.withTransaction {
            dao.update(updated)
            eventLogger.log(
                type = EventType.CATEGORY_UPDATED,
                categoryId = id,
                payload = jsonOf("old" to current.toEventJson(), "new" to updated.toEventJson()),
            )
        }
        return SaveResult.Success(id)
    }

    /** Çalışıyorsa önce oturumu kaydeder, sonra arşivler. Geçmiş veriler grafiklerde kalır. */
    suspend fun archive(id: Long) {
        poolRepository.freezeCompletedDays()
        timerRepository.stopIfRunningFor(id)
        db.withTransaction {
            dao.setArchived(id, true)
            eventLogger.log(type = EventType.CATEGORY_ARCHIVED, categoryId = id)
        }
    }

    suspend fun restore(id: Long) {
        // Arşivde geçen günler "pasif" olarak dondurulsun.
        poolRepository.freezeCompletedDays()
        db.withTransaction {
            dao.setArchived(id, false)
            eventLogger.log(type = EventType.CATEGORY_RESTORED, categoryId = id)
        }
    }

    /**
     * Kalıcı silme: oturumlar, oturum ayrıntıları, hedefler, günlük hedef durumları ve bu kategoriye bağlı
     * olaylar (FK cascade) havuzdan da silinir; geride iz kalmaz.
     */
    suspend fun deletePermanently(id: Long) = dao.deleteById(id)

    companion object {
        const val MAX_NAME_LENGTH = 40

        private val TR = Locale.forLanguageTag("tr-TR")

        fun nameKey(name: String): String = name.trim().lowercase(TR)
    }
}

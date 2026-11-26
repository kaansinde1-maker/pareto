package com.pareto.timetracker.data.obsidian

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.pareto.timetracker.R
import com.pareto.timetracker.data.db.CategoryEntity
import com.pareto.timetracker.data.db.PoolReadDao
import com.pareto.timetracker.data.pool.PoolCsv
import com.pareto.timetracker.data.pool.PoolRepository
import com.pareto.timetracker.domain.GoalPeriod
import com.pareto.timetracker.domain.GoalType
import com.pareto.timetracker.domain.Tag
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

sealed interface ObsidianSyncResult {
    data object Success : ObsidianSyncResult
    data object NotConfigured : ObsidianSyncResult
    data class Failure(val message: String?) : ObsidianSyncResult
}

/**
 * Veri havuzunu seçilen vault klasörüne yazar (tek yön: uygulama -> Obsidian).
 *
 * Tetikleyiciler: veritabanındaki havuz tabloları değişince (2 sn bekleyerek), uygulama açılırken ve menüden elle.
 * İçeriği değişmeyen dosyalar yeniden yazılmaz (SHA-256 karşılaştırması + dosyanın varlık kontrolü).
 */
@Singleton
class ObsidianExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: ObsidianSettings,
    private val poolRepository: PoolRepository,
    private val readDao: PoolReadDao,
) {
    private data class Request(val manual: Boolean, val force: Boolean)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val requests = Channel<Request>(Channel.UNLIMITED)
    private val _manualResults = MutableSharedFlow<ObsidianSyncResult>(extraBufferCapacity = 1)
    private var started = false

    /** Sadece elle tetiklenen yazmaların sonucu (arayüzde mesaj göstermek için). */
    val manualResults: SharedFlow<ObsidianSyncResult> = _manualResults.asSharedFlow()

    @OptIn(FlowPreview::class)
    @Synchronized
    fun start() {
        if (started) return
        started = true

        scope.launch {
            for (first in requests) {
                var manual = first.manual
                var force = first.force
                // Birikmiş istekleri tek çalıştırmada birleştir.
                while (true) {
                    val next = requests.tryReceive().getOrNull() ?: break
                    manual = manual || next.manual
                    force = force || next.force
                }
                val result = try {
                    syncOnce(force)
                } catch (e: Exception) {
                    ObsidianSyncResult.Failure(e.message)
                }
                if (manual) _manualResults.tryEmit(result)
            }
        }
        scope.launch {
            readDao.observeChanges().debounce(DEBOUNCE_MS).collect {
                requests.trySend(Request(manual = false, force = false))
            }
        }
    }

    fun syncNow() {
        requests.trySend(Request(manual = true, force = false))
    }

    fun rewriteAll() {
        requests.trySend(Request(manual = true, force = true))
    }

    private suspend fun syncOnce(force: Boolean): ObsidianSyncResult {
        val treeUri = settings.currentTreeUri() ?: return ObsidianSyncResult.NotConfigured
        val root = DocumentFile.fromTreeUri(context, treeUri)
        if (root == null || !root.exists() || !root.canWrite()) {
            return ObsidianSyncResult.Failure("Vault folder is not accessible")
        }

        // Günü bitmiş günlük hedef durumlarını önce dondur ki dosyalara eksiksiz girsin.
        poolRepository.freezeCompletedDays()

        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val sessions = readDao.getSessions()
        val categories = readDao.getCategories()
        val goals = readDao.getGoals()
        val statuses = readDao.getDailyGoalStatuses()
        val events = readDao.getEvents()
        val meta = readDao.getMeta()

        val aggregates = ObsidianRenderer.aggregate(sessions, zone)
        val categoriesById = categories.associateBy { it.id }
        val statusesByDay = statuses.groupBy { it.day }
        val firstDay = sessions.minOfOrNull { it.startedAt }
            ?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
            ?: today

        // yol -> içerik (sırayla yazılır)
        val files = LinkedHashMap<String, String>()

        var day = firstDay
        while (!day.isAfter(today)) {
            val isToday = day == today
            val lines = if (isToday) {
                liveGoalLines(goals.filter { it.period == GoalPeriod.DAILY }, categoriesById, aggregates[day])
            } else {
                statusesByDay[day.toString()].orEmpty().map { s ->
                    val category = s.categoryId?.let { categoriesById[it] }
                    GoalLine(
                        scope = if (s.categoryId != null) "kategori" else "etiket",
                        name = category?.name ?: s.tag?.let { context.getString(it.labelRes) }.orEmpty(),
                        type = s.goalType.name.lowercase(),
                        targetSeconds = s.targetSeconds,
                        actualSeconds = s.actualSeconds,
                        achieved = s.achieved,
                        active = s.goalActive,
                    )
                }
            }
            val month = day.monthValue.toString().padStart(2, '0')
            files["$BASE_DIR/${day.year}/$month/$day.md"] =
                ObsidianRenderer.renderDay(day, aggregates[day], lines, finalized = !isToday, zone = zone)
            day = day.plusDays(1)
        }

        files["$DATA_DIR/oturumlar.csv"] = PoolCsv.sessions(sessions, zone)
        files["$DATA_DIR/olaylar.csv"] = PoolCsv.events(events, zone)
        files["$DATA_DIR/gunluk_hedef_durumu.csv"] = PoolCsv.dailyGoalStatus(statuses, zone)
        files["$DATA_DIR/kategoriler.csv"] = PoolCsv.categories(categories, zone)
        files["$DATA_DIR/hedefler.csv"] = PoolCsv.goals(goals, zone)
        files["$DATA_DIR/cihaz.csv"] = PoolCsv.meta(meta)
        files["$DATA_DIR/SEMA.md"] = context.resources.openRawResource(R.raw.veri_sema)
            .bufferedReader(Charsets.UTF_8).use { it.readText() }

        val hashes = HashStore(File(context.filesDir, HASH_FILE), treeUri)
        val writer = TreeWriter(context, root)
        try {
            for ((path, content) in files) {
                val segments = path.split('/')
                val dirPath = segments.dropLast(1)
                val fileName = segments.last()
                val hash = sha256(content)
                val unchanged = !force && hashes[path] == hash && writer.fileExists(dirPath, fileName)
                if (unchanged) continue
                writer.write(dirPath, fileName, content)
                hashes[path] = hash
            }
        } finally {
            hashes.save()
        }
        return ObsidianSyncResult.Success
    }

    /** Bugün henüz bitmediği için hedef durumu canlı hesaplanır (kesinlesti: false). */
    private fun liveGoalLines(
        dailyGoals: List<com.pareto.timetracker.data.db.GoalEntity>,
        categoriesById: Map<Long, CategoryEntity>,
        todayAgg: DayAggregate?,
    ): List<GoalLine> = dailyGoals.map { goal ->
        val category = goal.categoryId?.let { categoriesById[it] }
        val actualMs = when {
            goal.categoryId != null -> todayAgg?.categoryIdMs?.get(goal.categoryId) ?: 0L
            goal.tag == Tag.INVESTMENT -> todayAgg?.investmentMs ?: 0L
            goal.tag == Tag.CONSUMPTION -> todayAgg?.consumptionMs ?: 0L
            else -> 0L
        }
        val actualSeconds = actualMs / 1000
        GoalLine(
            scope = if (goal.categoryId != null) "kategori" else "etiket",
            name = category?.name ?: goal.tag?.let { context.getString(it.labelRes) }.orEmpty(),
            type = goal.goalType.name.lowercase(),
            targetSeconds = goal.targetSeconds,
            actualSeconds = actualSeconds,
            achieved = when (goal.goalType) {
                GoalType.MIN -> actualSeconds >= goal.targetSeconds
                GoalType.MAX -> actualSeconds <= goal.targetSeconds
            },
            active = category?.isArchived != true,
        )
    }

    private fun sha256(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    /** Yazılan dosyaların içerik özetleri (klasör değişince sıfırlanır). */
    private class HashStore(private val file: File, private val tree: Uri) {
        private val map = HashMap<String, String>()

        init {
            try {
                val json = JSONObject(file.readText())
                if (json.optString("tree") == tree.toString()) {
                    val hashes = json.getJSONObject("hashes")
                    hashes.keys().forEach { key -> map[key] = hashes.getString(key) }
                }
            } catch (e: Exception) {
                // dosya yok/bozuk: boş başla, her şey yeniden yazılır
            }
        }

        operator fun get(path: String): String? = map[path]
        operator fun set(path: String, hash: String) {
            map[path] = hash
        }

        fun save() {
            val json = JSONObject()
                .put("tree", tree.toString())
                .put("hashes", JSONObject(map as Map<*, *>))
            file.writeText(json.toString())
        }
    }

    /** SAF ağacına yazar. Dizin listeleri çalıştırma boyunca önbelleğe alınır (SAF sorguları yavaştır). */
    private class TreeWriter(private val context: Context, private val root: DocumentFile) {
        private val listings = HashMap<String, MutableMap<String, DocumentFile>>()
        private val dirs = HashMap<String, DocumentFile>()

        fun fileExists(dirPath: List<String>, name: String): Boolean {
            val dir = findDir(dirPath) ?: return false
            return listing(dir).containsKey(name)
        }

        fun write(dirPath: List<String>, name: String, content: String) {
            val dir = ensureDir(dirPath)
            val children = listing(dir)
            val file = children[name]
                ?: dir.createFile(MIME, name)?.also { children[name] = it }
                ?: throw IOException("Could not create file: $name")
            val stream = context.contentResolver.openOutputStream(file.uri, "wt")
                ?: throw IOException("Could not open file: $name")
            stream.use { it.write(content.toByteArray(Charsets.UTF_8)) }
        }

        private fun listing(dir: DocumentFile): MutableMap<String, DocumentFile> =
            listings.getOrPut(dir.uri.toString()) {
                val map = HashMap<String, DocumentFile>()
                dir.listFiles().forEach { child -> child.name?.let { map[it] = child } }
                map
            }

        private fun findDir(path: List<String>): DocumentFile? {
            var current = root
            val key = StringBuilder()
            for (segment in path) {
                key.append('/').append(segment)
                val cached = dirs[key.toString()]
                current = cached ?: listing(current)[segment]?.takeIf { it.isDirectory }
                    ?.also { dirs[key.toString()] = it }
                    ?: return null
            }
            return current
        }

        private fun ensureDir(path: List<String>): DocumentFile {
            var current = root
            val key = StringBuilder()
            for (segment in path) {
                key.append('/').append(segment)
                val cached = dirs[key.toString()]
                current = cached ?: run {
                    val children = listing(current)
                    val existing = children[segment]?.takeIf { it.isDirectory }
                    existing ?: current.createDirectory(segment)?.also { children[segment] = it }
                        ?: throw IOException("Could not create folder: $segment")
                }.also { dirs[key.toString()] = it }
            }
            return current
        }
    }

    private companion object {
        const val BASE_DIR = "80-20"
        const val DATA_DIR = "80-20/_veri"
        const val HASH_FILE = "obsidian_hashes.json"
        const val DEBOUNCE_MS = 2_000L

        // Sağlayıcı dosya adına .txt/.md gibi uzantı eklemesin diye genel tür kullanılır.
        const val MIME = "application/octet-stream"
    }
}

package com.pareto.timetracker.data.repo

import android.content.Context
import android.net.Uri
import com.pareto.timetracker.data.db.PoolReadDao
import com.pareto.timetracker.data.pool.PoolCsv
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Grafik ekranındaki "Verileri dışa aktar": tüm oturumları, Obsidian veri havuzundaki `oturumlar.csv`
 * ile birebir aynı biçimde (bkz. [PoolCsv.sessions]) seçilen konuma yazar.
 */
@Singleton
class ExportRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val readDao: PoolReadDao,
) {
    fun suggestedFileName(): String = "pareto_oturumlar_${LocalDate.now()}.csv"

    /** @return başarılı mı */
    suspend fun writeTo(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val csv = PoolCsv.sessions(readDao.getSessions(), ZoneId.systemDefault())
            val stream = context.contentResolver.openOutputStream(uri, "wt") ?: return@withContext false
            stream.bufferedWriter(Charsets.UTF_8).use { it.write(csv) }
            true
        } catch (e: IOException) {
            false
        } catch (e: SecurityException) {
            false
        }
    }
}

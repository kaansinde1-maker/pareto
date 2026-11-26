package com.pareto.timetracker

import android.app.Application
import android.util.Log
import com.pareto.timetracker.data.obsidian.ObsidianExporter
import com.pareto.timetracker.data.pool.PoolRepository
import com.pareto.timetracker.data.repo.DefaultDataSeeder
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class ParetoApp : Application() {

    @Inject lateinit var seeder: DefaultDataSeeder
    @Inject lateinit var poolRepository: PoolRepository
    @Inject lateinit var obsidianExporter: ObsidianExporter

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            startupStep("seed default categories") { seeder.seedIfNeeded() }
            startupStep("write pool meta") { poolRepository.ensureMeta() }
            // Günü bitmiş günlük hedeflerin durumunu dondurur (eksik günleri tamamlar).
            startupStep("freeze completed days") { poolRepository.freezeCompletedDays() }
            // Obsidian klasörü seçilmişse havuz değişikliklerini dinleyip yazar (seçilmediyse hiçbir şey yapmaz).
            startupStep("start Obsidian exporter") { obsidianExporter.start() }
        }
    }

    /** Bir başlangıç adımı hata verse bile sonrakiler çalışsın; hata günlüğe yazılır. */
    private suspend fun startupStep(name: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Startup step failed: $name", e)
        }
    }

    private companion object {
        const val TAG = "ParetoApp"
    }
}

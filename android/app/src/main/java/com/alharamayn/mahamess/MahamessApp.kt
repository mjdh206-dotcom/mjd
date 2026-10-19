package com.alharamayn.mahamess

import android.app.Application
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.alharamayn.mahamess.data.Repo
import com.alharamayn.mahamess.data.SettingsStore
import com.alharamayn.mahamess.util.Dates
import kotlinx.coroutines.flow.first
import java.io.File
import java.util.concurrent.TimeUnit

class MahamessApp : Application() {
    override fun onCreate() {
        super.onCreate()
        scheduleBackup()
    }

    private fun scheduleBackup() {
        try {
            val request = PeriodicWorkRequestBuilder<BackupWorker>(1, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .build()
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "daily_backup",
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        } catch (e: Exception) {
            // لا تُفشل بدء التطبيق بسبب النسخ الاحتياطي
        }
    }
}

/** نسخة احتياطية يومية محلية داخل مساحة التطبيق (آخر 7 نسخ) */
class BackupWorker(appContext: android.content.Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val settingsStore = SettingsStore(applicationContext)
            val settings = settingsStore.flow.first()
            if (!settings.autoBackup) return Result.success()
            val repo = Repo(applicationContext, settingsStore)
            val json = repo.exportJson()
            val dir = File(applicationContext.filesDir, "backups").apply { mkdirs() }
            val file = File(dir, "backup-${Dates.today()}.json")
            file.writeText(json)
            dir.listFiles()?.sortedByDescending { it.name }?.drop(7)?.forEach { it.delete() }
            settingsStore.update { it.copy(lastBackup = System.currentTimeMillis()) }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

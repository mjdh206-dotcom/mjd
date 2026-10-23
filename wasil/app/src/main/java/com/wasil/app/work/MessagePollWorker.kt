package com.wasil.app.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.ListenableWorker.Result
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.wasil.app.data.ApiClient
import com.wasil.app.data.AppMode
import com.wasil.app.data.ConfigStore
import com.wasil.app.notify.MessageNotifier
import java.util.concurrent.TimeUnit

/**
 * مزامنة دورية كل 15 دقيقة (أقل حد يسمح به أندرويد) لجلب الرسائل الجديدة
 * وإظهار إشعار محلي بها حتى لو كان التطبيق مغلقاً.
 */
class MessagePollWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val store = ConfigStore(applicationContext)
        val config = store.loadConfig()
        if (config.mode != AppMode.LIVE || config.serverUrl.isBlank()) return Result.success()

        val api = ApiClient()
        val numberResult = api.fetchNumber(config.serverUrl, config.token)
        if (numberResult is ApiClient.Result.Success) {
            store.saveNumber(numberResult.value)
        }

        return when (val result = api.fetchMessages(config.serverUrl, config.token)) {
            is ApiClient.Result.Failure -> Result.retry()
            is ApiClient.Result.Success -> {
                val incoming = result.value
                val known = store.loadNotifiedIds()
                val fresh = incoming.filter { it.id !in known }
                if (config.notificationsEnabled) {
                    val notifier = MessageNotifier(applicationContext)
                    fresh.forEach { notifier.show(it) }
                }
                if (fresh.isNotEmpty()) {
                    store.saveNotifiedIds(known + fresh.map { it.id })
                }
                store.saveMessages(incoming)
                Result.success()
            }
        }
    }

    companion object {
        private const val UNIQUE_WORK = "wasil_periodic_poll"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<MessagePollWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK)
        }
    }
}

package com.mjd.videoforge

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class VideoForgeApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            // Video processing channel
            val processingChannel = NotificationChannel(
                CHANNEL_PROCESSING,
                "معالجة الفيديو",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعارات تقدم معالجة الفيديو"
            }
            manager.createNotificationChannel(processingChannel)

            // Queue completion channel
            val completionChannel = NotificationChannel(
                CHANNEL_COMPLETION,
                "اكتمال المشاريع",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "إشعارات عند اكتمال إنتاج فيديو"
            }
            manager.createNotificationChannel(completionChannel)

            // Quota warning channel
            val quotaChannel = NotificationChannel(
                CHANNEL_QUOTA,
                "تحذيرات الحصص",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات عند نفاد حصص محركات الذكاء الاصطناعي"
            }
            manager.createNotificationChannel(quotaChannel)
        }
    }

    companion object {
        const val CHANNEL_PROCESSING = "video_processing"
        const val CHANNEL_COMPLETION = "queue_completion"
        const val CHANNEL_QUOTA = "quota_warnings"
    }
}

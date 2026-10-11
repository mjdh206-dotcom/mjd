package com.mjd.videoforge.workers

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.mjd.videoforge.MainActivity
import com.mjd.videoforge.R
import com.mjd.videoforge.VideoForgeApp

/**
 * خدمة أمامية لمعالجة الفيديو
 * تضمن أن النظام لا يوقف المعالجة حتى لو went التطبيق للخلفية
 */
class VideoProcessingService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification("جاري إنتاج الفيديو...", 0)
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    private fun createNotification(text: String, progress: Int): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, VideoForgeApp.CHANNEL_PROCESSING)
            .setContentTitle("مصنع الفيديو")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setProgress(100, progress, progress == 0)
            .build()
    }

    fun updateProgress(text: String, progress: Int) {
        val notification = createNotification(text, progress)
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    companion object {
        const val NOTIFICATION_ID = 1001
    }
}

package com.alharamain.store

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class OrderMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        createOrderChannel(this, getSharedPreferences("haramain_store", Context.MODE_PRIVATE).getBoolean("sound_enabled", true))
        val id = message.data["orderId"]?.takeLast(8)?.uppercase().orEmpty()
        val notification = NotificationCompat.Builder(this, ORDER_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(message.notification?.title ?: "طلب جديد — محامص الحرمين")
            .setContentText(message.notification?.body ?: "وصلك طلب جديد${if (id.isNotBlank()) " رقم $id" else ""}")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(this).notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification) }
    }

    override fun onNewToken(token: String) {
        // The app uses a shared FCM topic. No device token or service credential is stored in the client.
    }
}

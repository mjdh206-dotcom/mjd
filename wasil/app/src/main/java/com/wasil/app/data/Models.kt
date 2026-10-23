package com.wasil.app.data

import org.json.JSONObject

/** رسالة SMS واردة إلى الرقم الأمريكي */
data class SmsMessage(
    val id: String,
    val from: String,
    val body: String,
    val ts: Long,
    val code: String? = CodeDetector.findCode(body),
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("from", from)
        put("body", body)
        put("ts", ts)
    }

    companion object {
        fun fromJson(o: JSONObject): SmsMessage = SmsMessage(
            id = o.optString("id"),
            from = o.optString("from"),
            body = o.optString("body"),
            ts = o.optLong("ts", 0L),
        )
    }
}

/** وضع تشغيل التطبيق */
enum class AppMode { DEMO, LIVE }

/** حالة الاتصال بالخادم */
enum class ConnectStatus { IDLE, CONNECTING, OK, ERROR }

/** إعدادات الاتصال المحفوظة محلياً */
data class AppConfig(
    val mode: AppMode = AppMode.DEMO,
    val serverUrl: String = "",
    val token: String = "",
    val notificationsEnabled: Boolean = true,
) {
    val isLive: Boolean get() = mode == AppMode.LIVE
}

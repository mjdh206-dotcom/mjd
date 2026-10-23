package com.wasil.app.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/** تخزين محلي بسيط لكل إعدادات التطبيق وآخر الرسائل (لا تُرسل لأي طرف ثالث). */
class ConfigStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun loadConfig(): AppConfig = AppConfig(
        mode = if (prefs.getString(KEY_MODE, "DEMO") == "LIVE") AppMode.LIVE else AppMode.DEMO,
        serverUrl = prefs.getString(KEY_URL, "").orEmpty(),
        token = prefs.getString(KEY_TOKEN, "").orEmpty(),
        notificationsEnabled = prefs.getBoolean(KEY_NOTIFY, true),
    )

    fun isLaunchedBefore(): Boolean = prefs.getBoolean(KEY_LAUNCHED, false)

    fun setLaunchedBefore() = prefs.edit().putBoolean(KEY_LAUNCHED, true).apply()

    fun saveConfig(config: AppConfig) {
        prefs.edit()
            .putString(KEY_MODE, if (config.mode == AppMode.LIVE) "LIVE" else "DEMO")
            .putString(KEY_URL, config.serverUrl.trim())
            .putString(KEY_TOKEN, config.token.trim())
            .putBoolean(KEY_NOTIFY, config.notificationsEnabled)
            .apply()
    }

    fun saveNumber(number: String) = prefs.edit().putString(KEY_NUMBER, number).apply()
    fun loadNumber(): String = prefs.getString(KEY_NUMBER, "").orEmpty()

    fun saveMessages(messages: List<SmsMessage>) {
        val arr = JSONArray()
        messages.take(MAX_CACHED).forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY_MESSAGES, arr.toString()).apply()
    }

    fun loadMessages(): List<SmsMessage> {
        val raw = prefs.getString(KEY_MESSAGES, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { SmsMessage.fromJson(arr.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }

    fun clearMessages() = prefs.edit().remove(KEY_MESSAGES).apply()

    /** معرّفات الرسائل التي سبق إشعار المستخدم بها */
    fun loadNotifiedIds(): Set<String> =
        prefs.getStringSet(KEY_NOTIFIED, emptySet()).orEmpty().toSet()

    fun saveNotifiedIds(ids: Set<String>) =
        prefs.edit().putStringSet(KEY_NOTIFIED, ids.take(MAX_CACHED).toSet()).apply()

    companion object {
        private const val PREFS = "wasil_prefs"
        private const val KEY_MODE = "mode"
        private const val KEY_URL = "server_url"
        private const val KEY_TOKEN = "token"
        private const val KEY_NOTIFY = "notifications"
        private const val KEY_NUMBER = "number"
        private const val KEY_MESSAGES = "messages_cache"
        private const val KEY_NOTIFIED = "notified_ids"
        private const val KEY_LAUNCHED = "launched_before"
        private const val MAX_CACHED = 200

        /** تنسيق رسالة واحدة كما يتوقعها خادم واصل (موثّق في README) */
        fun messageJson(m: SmsMessage): String = JSONObject().apply {
            put("id", m.id)
            put("from", m.from)
            put("body", m.body)
            put("ts", m.ts)
        }.toString()
    }
}

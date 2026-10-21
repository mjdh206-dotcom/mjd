package com.haris.wifi.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class DeviceStore(context: Context) {

    private val prefs = context.getSharedPreferences("haris_wifi_store", Context.MODE_PRIVATE)

    fun loadAllMeta(): Map<String, SavedDeviceMeta> {
        val raw = prefs.getString(KEY_DEVICES, null) ?: return emptyMap()
        return runCatching {
            val arr = JSONArray(raw)
            val map = mutableMapOf<String, SavedDeviceMeta>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val key = obj.optString("key")
                if (key.isNotBlank()) {
                    val until = obj.optLong("blockedUntilMillis", -1L).takeIf { it > 0L }
                    // إذا انتهى وقت الحظر المؤقت، نعيد الجهاز متصلاً تلقائياً
                    val expired = until != null && System.currentTimeMillis() > until
                    map[key] = SavedDeviceMeta(
                        key = key,
                        customName = obj.optString("customName", ""),
                        deviceTypeId = obj.optString("deviceTypeId", ""),
                        isTrusted = obj.optBoolean("isTrusted", false),
                        isBlocked = if (expired) false else obj.optBoolean("isBlocked", false),
                        blockedUntilMillis = if (expired) null else until,
                        blockMethodAr = if (expired) "" else obj.optString("blockMethodAr", ""),
                        lastIp = obj.optString("lastIp", ""),
                        lastMac = obj.optString("lastMac", ""),
                        vendorAr = obj.optString("vendorAr", ""),
                        defaultNameAr = obj.optString("defaultNameAr", ""),
                    )
                }
            }
            map
        }.getOrDefault(emptyMap())
    }

    fun saveMeta(meta: SavedDeviceMeta) {
        val all = loadAllMeta().toMutableMap()
        all[meta.key] = meta
        persistMap(all)
    }

    fun saveAllMeta(map: Map<String, SavedDeviceMeta>) {
        persistMap(map)
    }

    private fun persistMap(map: Map<String, SavedDeviceMeta>) {
        val arr = JSONArray()
        map.values.forEach { item ->
            val obj = JSONObject().apply {
                put("key", item.key)
                put("customName", item.customName)
                put("deviceTypeId", item.deviceTypeId)
                put("isTrusted", item.isTrusted)
                put("isBlocked", item.isBlocked)
                put("blockedUntilMillis", item.blockedUntilMillis ?: -1L)
                put("blockMethodAr", item.blockMethodAr)
                put("lastIp", item.lastIp)
                put("lastMac", item.lastMac)
                put("vendorAr", item.vendorAr)
                put("defaultNameAr", item.defaultNameAr)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_DEVICES, arr.toString()).apply()
    }

    var vibrateEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATE, true)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATE, value).apply()

    var autoCutStrangers: Boolean
        get() = prefs.getBoolean(KEY_AUTO_CUT_STRANGERS, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_CUT_STRANGERS, value).apply()

    companion object {
        private const val KEY_DEVICES = "devices_meta_json"
        private const val KEY_VIBRATE = "vibrate_enabled"
        private const val KEY_AUTO_CUT_STRANGERS = "auto_cut_strangers"
    }
}

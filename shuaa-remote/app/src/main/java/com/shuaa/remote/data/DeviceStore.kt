package com.shuaa.remote.data

import android.content.Context
import org.json.JSONArray

/** حفظ الأجهزة والمفضلة محلياً */
class DeviceStore(context: Context) {

    private val prefs = context.getSharedPreferences("shuaa_devices", Context.MODE_PRIVATE)

    private fun readArray(): JSONArray = try {
        JSONArray(prefs.getString(KEY, "[]") ?: "[]")
    } catch (_: Exception) {
        JSONArray()
    }

    fun devices(): List<SavedDevice> {
        val arr = readArray()
        val list = mutableListOf<SavedDevice>()
        for (i in 0 until arr.length()) {
            arr.optJSONObject(i)?.let { SavedDevice.fromJson(it)?.let(list::add) }
        }
        return list.sortedByDescending { it.createdAt }
    }

    fun primary(): SavedDevice? = devices().firstOrNull { it.isPrimary } ?: devices().firstOrNull()

    private fun write(list: List<SavedDevice>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    fun add(device: SavedDevice): SavedDevice {
        val current = devices().toMutableList()
        // أول جهاز يصبح الافتراضي تلقائياً
        val makePrimary = current.isEmpty() || device.isPrimary
        current.removeAll { it.id == device.id }
        if (makePrimary) {
            for (i in current.indices) current[i] = current[i].copy(isPrimary = false)
        }
        current.add(device.copy(isPrimary = makePrimary))
        write(current)
        return current.first { it.id == device.id }
    }

    fun setPrimary(id: String) {
        val current = devices().map { it.copy(isPrimary = it.id == id) }
        write(current)
    }

    fun rename(id: String, name: String) {
        val current = devices().map { if (it.id == id) it.copy(name = name) else it }
        write(current)
    }

    fun delete(id: String) {
        val remaining = devices().filterNot { it.id == id }
        if (remaining.isNotEmpty() && remaining.none { it.isPrimary }) {
            write(listOf(remaining.first().copy(isPrimary = true)) + remaining.drop(1))
        } else {
            write(remaining)
        }
    }

    fun clear() = prefs.edit().remove(KEY).apply()

    companion object {
        private const val KEY = "devices_json"
    }
}

package com.shuaa.remote.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** إعدادات التطبيق (اهتزاز، صوت، وضع المعاينة) */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("shuaa_settings", Context.MODE_PRIVATE)

    var hapticsEnabled by mutableStateOf(prefs.getBoolean(KEY_HAPTICS, true))
        private set

    var soundEnabled by mutableStateOf(prefs.getBoolean(KEY_SOUND, true))
        private set

    var bigButtons by mutableStateOf(prefs.getBoolean(KEY_BIG, false))
        private set

    var demoMode by mutableStateOf(prefs.getBoolean(KEY_DEMO, false))
        private set

    fun setHaptics(value: Boolean) {
        hapticsEnabled = value
        prefs.edit().putBoolean(KEY_HAPTICS, value).apply()
    }

    fun setSound(value: Boolean) {
        soundEnabled = value
        prefs.edit().putBoolean(KEY_SOUND, value).apply()
    }

    fun setBigButtons(value: Boolean) {
        bigButtons = value
        prefs.edit().putBoolean(KEY_BIG, value).apply()
    }

    fun setDemoMode(value: Boolean) {
        demoMode = value
        prefs.edit().putBoolean(KEY_DEMO, value).apply()
    }

    private companion object {
        const val KEY_HAPTICS = "haptics"
        const val KEY_SOUND = "sound"
        const val KEY_BIG = "big_buttons"
        const val KEY_DEMO = "demo_mode"
    }
}

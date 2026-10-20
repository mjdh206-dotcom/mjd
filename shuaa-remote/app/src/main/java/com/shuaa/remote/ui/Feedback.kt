package com.shuaa.remote.ui

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.SoundEffectConstants
import android.view.View
import com.shuaa.remote.data.SettingsStore

/** اهتزاز خفيف + صوت ضغطة زر */
class Feedback(context: Context, private val settings: SettingsStore) {

    private val appContext = context.applicationContext
    private val vibrator: Vibrator? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    } catch (_: Exception) {
        null
    }

    private val audioManager: AudioManager? =
        appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    fun tap(view: View? = null) {
        if (settings.hapticsEnabled) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(18, 90))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(18)
                }
            } catch (_: Exception) {
            }
        }
        if (settings.soundEnabled) {
            try {
                if (view != null) {
                    view.playSoundEffect(SoundEffectConstants.CLICK)
                } else {
                    audioManager?.playSoundEffect(SoundEffectConstants.CLICK, 0.35f)
                }
            } catch (_: Exception) {
            }
        }
    }

    fun success() {
        if (!settings.hapticsEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 25, 60, 35), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 25, 60, 35), -1)
            }
        } catch (_: Exception) {
        }
    }
}

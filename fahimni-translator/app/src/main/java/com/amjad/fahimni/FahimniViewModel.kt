package com.amjad.fahimni

import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.amjad.fahimni.data.AppStorage
import com.amjad.fahimni.data.ChatTurn
import com.amjad.fahimni.data.LanguageCatalog
import com.amjad.fahimni.data.SpokenLanguage
import com.amjad.fahimni.data.TurnSide
import com.amjad.fahimni.engine.OnlineTranslatorService
import com.amjad.fahimni.speech.LiveSpeechRecognizer
import com.amjad.fahimni.speech.VoiceSpeaker
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SimpleUiState(
    val detectedPartnerLanguage: SpokenLanguage? = null,
    val activeListeningSide: TurnSide? = null,
    val isTranslating: Boolean = false,
    val autoSpeak: Boolean = true,
    val turns: List<ChatTurn> = emptyList(),
    val toastMessage: String? = null,
)

class FahimniViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val storage = AppStorage(appContext)
    private val translator = OnlineTranslatorService()
    val voiceSpeaker = VoiceSpeaker(appContext, translator.httpClient)
    val speechRecognizer = LiveSpeechRecognizer(appContext)

    private var toastJob: Job? = null
    var onLaunchSystemVoicePopup: ((Intent, TurnSide) -> Unit)? = null

    private val _uiState = MutableStateFlow(
        SimpleUiState(
            detectedPartnerLanguage = storage.detectedPartnerLanguage,
            autoSpeak = storage.autoSpeak,
            turns = storage.loadTurns(),
        )
    )
    val uiState: StateFlow<SimpleUiState> = _uiState.asStateFlow()

    /**
     * الزر 1: كبسة «اعرف شو لغتو وترجم للعربي»
     */
    fun startDetectOtherPerson() {
        voiceSpeaker.stop()
        vibrateShort()

        val knownLocale = _uiState.value.detectedPartnerLanguage?.speechLocale
        _uiState.update { it.copy(activeListeningSide = TurnSide.OTHER_PERSON, toastMessage = null) }

        speechRecognizer.startListening(
            autoDetectOtherPerson = true,
            knownPartnerLocale = knownLocale,
            onResult = { spokenText, acousticLocale ->
                _uiState.update { it.copy(activeListeningSide = null) }
                handleOtherPersonSpeech(spokenText, acousticLocale)
            },
            onError = { msg ->
                _uiState.update { it.copy(activeListeningSide = null) }
                showToast(msg)
            },
            onFallbackPopup = { intent ->
                _uiState.update { it.copy(activeListeningSide = null) }
                onLaunchSystemVoicePopup?.invoke(intent, TurnSide.OTHER_PERSON)
            }
        )
    }

    /**
     * الزر 2: كبسة «احكي بالعربي — يترجملو بناءً على لغتو»
     */
    fun startSpeakMyArabic() {
        voiceSpeaker.stop()
        vibrateShort()

        _uiState.update { it.copy(activeListeningSide = TurnSide.ME_ARABIC, toastMessage = null) }

        speechRecognizer.startListening(
            autoDetectOtherPerson = false,
            knownPartnerLocale = null,
            onResult = { spokenArabic, _ ->
                _uiState.update { it.copy(activeListeningSide = null) }
                handleMyArabicSpeech(spokenArabic)
            },
            onError = { msg ->
                _uiState.update { it.copy(activeListeningSide = null) }
                showToast(msg)
            },
            onFallbackPopup = { intent ->
                _uiState.update { it.copy(activeListeningSide = null) }
                onLaunchSystemVoicePopup?.invoke(intent, TurnSide.ME_ARABIC)
            }
        )
    }

    fun finishListeningNow() {
        speechRecognizer.finishAndCommitNow()
    }

    fun cancelListening() {
        speechRecognizer.cancel()
        _uiState.update { it.copy(activeListeningSide = null) }
    }

    fun handleOtherPersonSpeech(rawText: String, acousticLocale: String? = null) {
        val clean = rawText.trim()
        if (clean.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isTranslating = true) }

            val (detectedLang, arabicTranslation) = translator.detectLanguageAndTranslateToArabic(
                rawSpokenText = clean,
                acousticLocaleHint = acousticLocale,
            )

            storage.detectedPartnerLanguage = detectedLang

            val turn = ChatTurn(
                id = "t_${System.currentTimeMillis()}",
                side = TurnSide.OTHER_PERSON,
                originalText = clean,
                translatedText = arabicTranslation,
                partnerLanguage = detectedLang,
                timestamp = System.currentTimeMillis(),
            )

            val updated = (_uiState.value.turns + turn).takeLast(40)
            storage.saveTurns(updated)

            _uiState.update {
                it.copy(
                    isTranslating = false,
                    detectedPartnerLanguage = detectedLang,
                    turns = updated,
                    toastMessage = "🎯 لغته: ${detectedLang.flag} ${detectedLang.nameAr}",
                )
            }

            if (_uiState.value.autoSpeak) {
                voiceSpeaker.speak(
                    text = arabicTranslation,
                    ttsLangCode = "ar",
                    messageId = turn.id,
                )
            }
        }
    }

    fun handleMyArabicSpeech(rawArabic: String) {
        val clean = rawArabic.trim()
        if (clean.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isTranslating = true) }

            val targetLang = _uiState.value.detectedPartnerLanguage
                ?: LanguageCatalog.defaultFallbackTarget

            val translatedForHim = translator.translateMyArabicToPartner(
                mySpokenArabic = clean,
                partnerLanguage = targetLang,
            )

            val turn = ChatTurn(
                id = "t_${System.currentTimeMillis()}",
                side = TurnSide.ME_ARABIC,
                originalText = clean,
                translatedText = translatedForHim,
                partnerLanguage = targetLang,
                timestamp = System.currentTimeMillis(),
            )

            val updated = (_uiState.value.turns + turn).takeLast(40)
            storage.saveTurns(updated)

            _uiState.update {
                it.copy(
                    isTranslating = false,
                    turns = updated,
                )
            }

            if (_uiState.value.autoSpeak) {
                voiceSpeaker.speak(
                    text = translatedForHim,
                    ttsLangCode = targetLang.ttsLang,
                    messageId = turn.id,
                )
            }
        }
    }

    fun replayTurnAudio(turn: ChatTurn) {
        vibrateShort()
        val langCode = if (turn.side == TurnSide.OTHER_PERSON) {
            "ar"
        } else {
            turn.partnerLanguage.ttsLang
        }
        voiceSpeaker.speak(
            text = turn.translatedText,
            ttsLangCode = langCode,
            messageId = turn.id,
        )
    }

    fun resetToAutoDetect() {
        vibrateShort()
        storage.detectedPartnerLanguage = null
        _uiState.update { it.copy(detectedPartnerLanguage = null) }
        showToast("🎯 تم التصفير — جاهز لكشف لغة شخص جديد تلقائياً")
    }

    fun setPartnerLanguageManually(lang: SpokenLanguage?) {
        storage.detectedPartnerLanguage = lang
        _uiState.update { it.copy(detectedPartnerLanguage = lang) }
        if (lang != null) {
            showToast("تم اختيار لغة الطرف المقابل: ${lang.flag} ${lang.nameAr}")
        } else {
            showToast("🎯 تم تفعيل الكشف التلقائي")
        }
    }

    fun toggleAutoSpeak() {
        val next = !_uiState.value.autoSpeak
        storage.autoSpeak = next
        if (!next) voiceSpeaker.stop()
        _uiState.update { it.copy(autoSpeak = next) }
        showToast(if (next) "🔊 النطق التلقائي مفعّل" else "🔇 تم كتم النطق التلقائي")
    }

    fun clearScreen() {
        voiceSpeaker.stop()
        storage.saveTurns(emptyList())
        _uiState.update { it.copy(turns = emptyList()) }
    }

    fun showToast(msg: String) {
        toastJob?.cancel()
        _uiState.update { it.copy(toastMessage = msg) }
        toastJob = viewModelScope.launch {
            delay(3200L)
            _uiState.update { st -> if (st.toastMessage == msg) st.copy(toastMessage = null) else st }
        }
    }

    private fun vibrateShort() {
        runCatching {
            val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = appContext.getSystemService(Application.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                appContext.getSystemService(Application.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createOneShot(20L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(20L)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer.shutdown()
        voiceSpeaker.shutdown()
    }
}

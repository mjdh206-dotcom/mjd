package com.amjad.fahimni

import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.amjad.fahimni.data.AppPreferences
import com.amjad.fahimni.data.ConversationMessage
import com.amjad.fahimni.data.DialectComparisonItem
import com.amjad.fahimni.data.DialectProfile
import com.amjad.fahimni.data.SpeakerSide
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

enum class ActiveTab {
    CONVERSATION,
    ALL_DIALECTS,
    PHRASEBOOK,
    SETTINGS,
}

data class FahimniUiState(
    val activeTab: ActiveTab = ActiveTab.CONVERSATION,
    val myDialect: DialectProfile = DialectProfile.AR_SY,
    val partnerDialect: DialectProfile = DialectProfile.AUTO_DETECT,
    val lastDetectedPartner: DialectProfile? = null,
    val partnerListenLocaleHint: String = "ar",
    val autoSyncToDetected: Boolean = true,
    val autoSpeak: Boolean = true,
    val speechSpeed: Float = 1.0f,
    val handsFreeLoop: Boolean = false,
    val deepCloudAi: Boolean = true,
    val faceToFaceFlipped: Boolean = false,
    val isOnline: Boolean = true,
    val isTranslating: Boolean = false,
    val activeListeningSide: SpeakerSide? = null,
    val messages: List<ConversationMessage> = emptyList(),
    val statusBanner: String? = null,
    val comparisonInput: String = "يعطيك العافية يا أخي شو بدك نعمل اليوم؟",
    val comparisonResults: List<DialectComparisonItem> = emptyList(),
    val isComparing: Boolean = false,
)

class FahimniViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val prefs = AppPreferences(appContext)
    private val translator = OnlineTranslatorService(appContext)
    val voiceSpeaker = VoiceSpeaker(appContext, translator.httpClient)
    val speechRecognizer = LiveSpeechRecognizer(appContext)

    private var autoLoopJob: Job? = null
    private var bannerClearJob: Job? = null

    var onLaunchSystemVoicePopup: ((Intent, SpeakerSide) -> Unit)? = null

    private val _uiState = MutableStateFlow(
        FahimniUiState(
            myDialect = prefs.myDialect,
            partnerDialect = prefs.partnerDialect,
            lastDetectedPartner = prefs.lastDetectedPartnerDialect,
            partnerListenLocaleHint = prefs.partnerSpeechHintLocale,
            autoSyncToDetected = prefs.autoSyncTargetToDetected,
            autoSpeak = prefs.autoSpeak,
            speechSpeed = prefs.speechSpeed,
            handsFreeLoop = prefs.handsFreeLoop,
            deepCloudAi = prefs.deepCloudAiEnabled,
            isOnline = translator.isOnline(),
            messages = prefs.loadMessages(),
        )
    )
    val uiState: StateFlow<FahimniUiState> = _uiState.asStateFlow()

    init {
        refreshDialectComparison(_uiState.value.comparisonInput)
    }

    fun selectTab(tab: ActiveTab) {
        _uiState.update { it.copy(activeTab = tab, isOnline = translator.isOnline()) }
    }

    fun setMyDialect(profile: DialectProfile) {
        val valid = if (profile == DialectProfile.AUTO_DETECT) DialectProfile.AR_SIMPLE else profile
        prefs.myDialect = valid
        _uiState.update { it.copy(myDialect = valid) }
        showBanner("تم ضبط لهجتك: ${valid.flag} ${valid.shortNameAr}")
    }

    fun setPartnerDialect(profile: DialectProfile) {
        prefs.partnerDialect = profile
        if (profile != DialectProfile.AUTO_DETECT) {
            val hint = if (profile.isArabicDialect) "ar" else profile.speechLocale
            prefs.partnerSpeechHintLocale = hint
            _uiState.update {
                it.copy(partnerDialect = profile, partnerListenLocaleHint = hint)
            }
            showBanner("تم تحديد طرف المحادثة: ${profile.flag} ${profile.shortNameAr}")
        } else {
            prefs.partnerSpeechHintLocale = "ar"
            _uiState.update {
                it.copy(partnerDialect = DialectProfile.AUTO_DETECT, partnerListenLocaleHint = "ar")
            }
            showBanner("🎯 تم تفعيل الكشف التلقائي الذكي للهجة المتحدث")
        }
    }

    fun setPartnerListenEarLocale(locale: String, label: String) {
        prefs.partnerSpeechHintLocale = locale
        _uiState.update { it.copy(partnerListenLocaleHint = locale) }
        showBanner("🎙️ رادار الاستماع مضبوط على: $label")
    }

    fun swapDialects() {
        val st = _uiState.value
        val currentPartnerResolved = when {
            st.partnerDialect != DialectProfile.AUTO_DETECT -> st.partnerDialect
            st.lastDetectedPartner != null -> st.lastDetectedPartner
            else -> DialectProfile.AR_SIMPLE
        }
        val oldMine = st.myDialect
        setMyDialect(currentPartnerResolved)
        setPartnerDialect(oldMine)
        hapticTick()
    }

    fun toggleFaceToFace() {
        hapticTick()
        _uiState.update { it.copy(faceToFaceFlipped = !it.faceToFaceFlipped) }
    }

    fun setHandsFreeLoop(enabled: Boolean) {
        prefs.handsFreeLoop = enabled
        if (!enabled) autoLoopJob?.cancel()
        _uiState.update { it.copy(handsFreeLoop = enabled) }
        showBanner(
            if (enabled) "🔁 الحوار المستمر التلقائي مفعّل (يتناوب الاستماع تلقائياً)"
            else "تم إيقاف التناوب التلقائي"
        )
    }

    fun setAutoSpeak(enabled: Boolean) {
        prefs.autoSpeak = enabled
        _uiState.update { it.copy(autoSpeak = enabled) }
    }

    fun setAutoSyncToDetected(enabled: Boolean) {
        prefs.autoSyncTargetToDetected = enabled
        _uiState.update { it.copy(autoSyncToDetected = enabled) }
    }

    fun setSpeechSpeed(speed: Float) {
        prefs.speechSpeed = speed
        _uiState.update { it.copy(speechSpeed = speed) }
    }

    fun setDeepCloudAi(enabled: Boolean) {
        prefs.deepCloudAiEnabled = enabled
        _uiState.update { it.copy(deepCloudAi = enabled) }
    }

    fun startVoiceCapture(side: SpeakerSide) {
        autoLoopJob?.cancel()
        voiceSpeaker.stop()
        hapticTick()

        val st = _uiState.value
        val localeTag = when (side) {
            SpeakerSide.ME -> if (st.myDialect.isArabicDialect) "ar" else st.myDialect.speechLocale
            SpeakerSide.PARTNER -> {
                if (st.partnerDialect == DialectProfile.AUTO_DETECT) {
                    st.partnerListenLocaleHint
                } else if (st.partnerDialect.isArabicDialect) {
                    "ar"
                } else {
                    st.partnerDialect.speechLocale
                }
            }
        }

        _uiState.update {
            it.copy(
                activeListeningSide = side,
                isOnline = translator.isOnline(),
                statusBanner = null,
            )
        }

        speechRecognizer.startListening(
            localeTag = localeTag,
            onResult = { spokenText ->
                _uiState.update { it.copy(activeListeningSide = null) }
                processSpokenOrTypedInput(spokenText, side)
            },
            onError = { errMsg ->
                _uiState.update { it.copy(activeListeningSide = null) }
                showBanner(errMsg)
            },
            onFallbackPopup = { intent ->
                _uiState.update { it.copy(activeListeningSide = null) }
                onLaunchSystemVoicePopup?.invoke(intent, side)
            }
        )
    }

    fun commitActiveListeningNow() {
        speechRecognizer.finishAndCommitNow()
    }

    fun cancelActiveListening() {
        autoLoopJob?.cancel()
        speechRecognizer.cancel()
        _uiState.update { it.copy(activeListeningSide = null) }
    }

    fun processSpokenOrTypedInput(
        rawText: String,
        side: SpeakerSide,
    ) {
        val text = rawText.trim()
        if (text.isBlank()) return

        viewModelScope.launch {
            val st = _uiState.value
            _uiState.update {
                it.copy(
                    isTranslating = true,
                    isOnline = translator.isOnline(),
                )
            }

            val isPartnerSpeaking = (side == SpeakerSide.PARTNER)
            val sourceHint = if (isPartnerSpeaking) {
                st.partnerDialect
            } else {
                DialectProfile.AUTO_DETECT
            }

            val effectiveTarget = if (isPartnerSpeaking) {
                // الطرف المقابل يتكلم -> أترجم كلامه إلى لهجتي أنا لأفهمه فوراً!
                st.myDialect
            } else {
                // أنا أتكلم بأي لهجة -> أترجم كلامي إلى لهجة/لغة الطرف المقابل (سواء المحددة أو المكتشفة تلقائياً)!
                when {
                    st.partnerDialect != DialectProfile.AUTO_DETECT -> st.partnerDialect
                    st.autoSyncToDetected && st.lastDetectedPartner != null -> st.lastDetectedPartner
                    else -> DialectProfile.AR_SIMPLE
                }
            }

            val outcome = translator.translateSmart(
                rawText = text,
                sourceHint = sourceHint,
                targetDialect = effectiveTarget,
                deepCloudAi = st.deepCloudAi,
            )

            val autoDetectedFlag = isPartnerSpeaking && st.partnerDialect == DialectProfile.AUTO_DETECT
            if (isPartnerSpeaking) {
                prefs.lastDetectedPartnerDialect = outcome.detectedDialect
            }

            val msg = ConversationMessage(
                id = "m_${System.currentTimeMillis()}_${(100..999).random()}",
                side = side,
                originalText = outcome.originalText,
                translatedText = outcome.translatedText,
                clearArabicSubtitle = outcome.clearArabicSubtitle,
                detectedDialect = outcome.detectedDialect,
                targetDialect = outcome.targetDialect,
                confidence = outcome.confidence,
                slangNotes = outcome.slangNotes,
                latencyMs = outcome.latencyMs,
                timestamp = System.currentTimeMillis(),
                isAutoDetected = autoDetectedFlag,
            )

            val updatedMessages = (_uiState.value.messages + msg).takeLast(60)
            prefs.saveMessages(updatedMessages)

            _uiState.update { state ->
                state.copy(
                    isTranslating = false,
                    messages = updatedMessages,
                    lastDetectedPartner = if (isPartnerSpeaking) outcome.detectedDialect else state.lastDetectedPartner,
                    statusBanner = if (autoDetectedFlag) {
                        "🎯 تم اكتشاف اللهجة تلقائياً: ${outcome.detectedDialect.flag} ${outcome.detectedDialect.nameAr} (${outcome.confidence}%)"
                    } else state.statusBanner,
                )
            }

            if (_uiState.value.autoSpeak) {
                val nextSide = if (side == SpeakerSide.ME) SpeakerSide.PARTNER else SpeakerSide.ME
                voiceSpeaker.speak(
                    text = msg.translatedText,
                    ttsLangCode = msg.targetDialect.ttsLang,
                    messageId = msg.id,
                    speed = _uiState.value.speechSpeed,
                    onComplete = {
                        if (_uiState.value.handsFreeLoop &&
                            _uiState.value.activeTab == ActiveTab.CONVERSATION &&
                            _uiState.value.activeListeningSide == null
                        ) {
                            autoLoopJob?.cancel()
                            autoLoopJob = viewModelScope.launch {
                                delay(550L)
                                if (_uiState.value.handsFreeLoop) {
                                    startVoiceCapture(nextSide)
                                }
                            }
                        }
                    }
                )
            }
        }
    }

    fun speakMessage(msg: ConversationMessage) {
        hapticTick()
        voiceSpeaker.speak(
            text = msg.translatedText,
            ttsLangCode = msg.targetDialect.ttsLang,
            messageId = msg.id,
            speed = _uiState.value.speechSpeed,
        )
    }

    fun speakArbitraryText(text: String, dialect: DialectProfile, tagId: String) {
        hapticTick()
        voiceSpeaker.speak(
            text = text,
            ttsLangCode = dialect.ttsLang,
            messageId = tagId,
            speed = _uiState.value.speechSpeed,
        )
    }

    fun sendToAllDialectsStudio(text: String) {
        _uiState.update {
            it.copy(
                activeTab = ActiveTab.ALL_DIALECTS,
                comparisonInput = text,
            )
        }
        refreshDialectComparison(text)
    }

    fun refreshDialectComparison(input: String) {
        _uiState.update { it.copy(comparisonInput = input, isComparing = true) }
        viewModelScope.launch {
            val list = translator.compareAcrossDialectsOnline(input)
            _uiState.update { it.copy(comparisonResults = list, isComparing = false) }
        }
    }

    fun speakQuickPhraseToPartner(arabicPhrase: String) {
        _uiState.update { it.copy(activeTab = ActiveTab.CONVERSATION) }
        processSpokenOrTypedInput(arabicPhrase, SpeakerSide.ME)
    }

    fun clearHistory() {
        voiceSpeaker.stop()
        prefs.saveMessages(emptyList())
        _uiState.update { it.copy(messages = emptyList()) }
        showBanner("تم مسح سجل المحادثة")
    }

    fun showBanner(message: String) {
        bannerClearJob?.cancel()
        _uiState.update { it.copy(statusBanner = message) }
        bannerClearJob = viewModelScope.launch {
            delay(4000L)
            _uiState.update { state ->
                if (state.statusBanner == message) state.copy(statusBanner = null) else state
            }
        }
    }

    private fun hapticTick() {
        runCatching {
            val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = appContext.getSystemService(Application.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                appContext.getSystemService(Application.VIBRATOR_SERVICE) as Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createOneShot(18L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(18L)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer.shutdown()
        voiceSpeaker.shutdown()
    }
}

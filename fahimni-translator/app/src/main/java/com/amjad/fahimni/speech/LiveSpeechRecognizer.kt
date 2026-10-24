package com.amjad.fahimni.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * محرك الالتقاط الصوتي المباشر بوضعين مبسطين:
 * 1) وضع كشف لغة الشخص المقابل تلقائياً (ENABLE_LANGUAGE_DETECTION + ENABLE_LANGUAGE_SWITCH)
 * 2) وضع التحدث بالعربية بأي لهجة
 */
class LiveSpeechRecognizer(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private var lastPartial: String = ""
    private var detectedAcousticLocale: String? = null
    private var resultDelivered: Boolean = false
    private var onFinalCallback: ((String, String?) -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null

    fun startListening(
        autoDetectOtherPerson: Boolean,
        knownPartnerLocale: String?,
        onResult: (text: String, acousticLocale: String?) -> Unit,
        onError: (String) -> Unit,
        onFallbackPopup: (Intent) -> Unit,
    ) {
        mainHandler.post {
            destroyInternal()

            onFinalCallback = onResult
            onErrorCallback = onError
            lastPartial = ""
            detectedAcousticLocale = null
            resultDelivered = false
            _partialTranscript.value = ""
            _audioLevel.value = 0.12f

            val intent = buildIntent(autoDetectOtherPerson, knownPartnerLocale)

            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                _isListening.value = false
                onFallbackPopup(intent)
                return@post
            }

            runCatching {
                val sr = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
                recognizer = sr
                sr.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                    }

                    override fun onBeginningOfSpeech() {
                        _isListening.value = true
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        _audioLevel.value = ((rmsdB + 2f) / 12f).coerceIn(0.08f, 1.0f)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _audioLevel.value = 0.08f
                    }

                    override fun onLanguageDetection(results: Bundle) {
                        val lang = results.getString("android.speech.extra.DETECTED_LANGUAGE")
                        if (!lang.isNullOrBlank()) {
                            detectedAcousticLocale = lang
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val best = partialResults
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                            ?.trim()
                            .orEmpty()
                        if (best.isNotBlank()) {
                            lastPartial = best
                            _partialTranscript.value = best
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        if (resultDelivered) return
                        resultDelivered = true
                        _isListening.value = false
                        _audioLevel.value = 0f

                        val finalBest = results
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()
                            ?.trim()
                            .takeUnless { it.isNullOrBlank() }
                            ?: lastPartial.trim()

                        _partialTranscript.value = ""
                        if (finalBest.isNotBlank()) {
                            onFinalCallback?.invoke(finalBest, detectedAcousticLocale)
                        } else {
                            onErrorCallback?.invoke("لم يُسمع صوت واضح، اضغط وتكلم قريباً من الهاتف")
                        }
                    }

                    override fun onError(error: Int) {
                        if (resultDelivered) return
                        _isListening.value = false
                        _audioLevel.value = 0f

                        if (lastPartial.isNotBlank() &&
                            (error == SpeechRecognizer.ERROR_NO_MATCH ||
                                error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                                error == SpeechRecognizer.ERROR_CLIENT)
                        ) {
                            resultDelivered = true
                            val captured = lastPartial.trim()
                            _partialTranscript.value = ""
                            onFinalCallback?.invoke(captured, detectedAcousticLocale)
                            return
                        }

                        _partialTranscript.value = ""
                        if (error == SpeechRecognizer.ERROR_CLIENT) return

                        val msg = when (error) {
                            SpeechRecognizer.ERROR_NETWORK,
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                                "تأكد من اتصالك بالإنترنت ليعمل التطبيق بأقصى سرعة"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                                "يرجى السماح بصلاحية المايكروفون"
                            SpeechRecognizer.ERROR_NO_MATCH,
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                                "لم ألتقط الصوت، اضغط الزر وتكلم بوضوح"
                            else -> "تعذر التقاط الصوت، اضغط مرة أخرى"
                        }
                        onErrorCallback?.invoke(msg)
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                _isListening.value = true
                sr.startListening(intent)
            }.onFailure {
                _isListening.value = false
                onFallbackPopup(intent)
            }
        }
    }

    fun finishAndCommitNow() {
        mainHandler.post {
            if (!_isListening.value) return@post
            runCatching { recognizer?.stopListening() }
            mainHandler.postDelayed({
                if (!resultDelivered && lastPartial.isNotBlank()) {
                    resultDelivered = true
                    _isListening.value = false
                    _audioLevel.value = 0f
                    val captured = lastPartial.trim()
                    val lang = detectedAcousticLocale
                    _partialTranscript.value = ""
                    destroyInternal()
                    onFinalCallback?.invoke(captured, lang)
                }
            }, 400L)
        }
    }

    fun cancel() {
        mainHandler.post {
            resultDelivered = true
            _isListening.value = false
            _audioLevel.value = 0f
            _partialTranscript.value = ""
            lastPartial = ""
            destroyInternal()
        }
    }

    fun shutdown() {
        cancel()
    }

    private fun buildIntent(
        autoDetectOtherPerson: Boolean,
        knownPartnerLocale: String?,
    ): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1600L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1300L)

            if (autoDetectOtherPerson) {
                // كشف تلقائي ذكي لأي لغة يتكلمها الشخص المقابل
                val seedLocale = knownPartnerLocale?.takeIf { it.isNotBlank() } ?: "en-US"
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, seedLocale)
                putExtra("android.speech.extra.ENABLE_LANGUAGE_DETECTION", true)
                putExtra(
                    "android.speech.extra.ENABLE_LANGUAGE_SWITCH",
                    if (knownPartnerLocale == null) "high_precision" else "quick_response"
                )
            } else {
                // أنا أحكي عربي بأي لهجة
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar")
            }
        }
    }

    private fun destroyInternal() {
        runCatching { recognizer?.cancel() }
        runCatching { recognizer?.destroy() }
        recognizer = null
    }
}

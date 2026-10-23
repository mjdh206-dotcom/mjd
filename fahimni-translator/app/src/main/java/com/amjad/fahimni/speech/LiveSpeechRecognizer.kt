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
 * محرك الاستماع الصوتي المباشر عبر الإنترنت:
 * يلتقط الكلام لحظة بلحظة بأي لهجة عربية مع عرض الكلمات فوراً أثناء الحديث.
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
    private var resultDelivered: Boolean = false
    private var onFinalTextCallback: ((String) -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null
    private var onNeedSystemPopupCallback: ((Intent) -> Unit)? = null

    fun startListening(
        localeTag: String,
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
        onFallbackPopup: (Intent) -> Unit,
    ) {
        mainHandler.post {
            destroyInternal()

            onFinalTextCallback = onResult
            onErrorCallback = onError
            onNeedSystemPopupCallback = onFallbackPopup
            lastPartial = ""
            resultDelivered = false
            _partialTranscript.value = ""
            _audioLevel.value = 0.1f

            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                _isListening.value = false
                onFallbackPopup(buildRecognizerIntent(localeTag))
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
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.06f, 1.0f)
                        _audioLevel.value = normalized
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _audioLevel.value = 0.08f
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            .orEmpty()
                        val best = matches.firstOrNull()?.trim().orEmpty()
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
                        val matches = results
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            .orEmpty()
                        val finalBest = matches.firstOrNull()?.trim().takeUnless { it.isNullOrBlank() }
                            ?: lastPartial.trim()
                        _partialTranscript.value = ""
                        if (finalBest.isNotBlank()) {
                            onFinalTextCallback?.invoke(finalBest)
                        } else {
                            onErrorCallback?.invoke("لم ألتقط كلاماً واضحاً، تحدث قريباً من المايكروفون")
                        }
                    }

                    override fun onError(error: Int) {
                        if (resultDelivered) return
                        _isListening.value = false
                        _audioLevel.value = 0f

                        // إن كان المستخدم قد نطق جملة والتقطناها جزئياً قبل الصمت، نترجمها فوراً ولا نضيعها!
                        if (lastPartial.isNotBlank() &&
                            (error == SpeechRecognizer.ERROR_NO_MATCH ||
                                error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
                                error == SpeechRecognizer.ERROR_CLIENT)
                        ) {
                            resultDelivered = true
                            val captured = lastPartial.trim()
                            _partialTranscript.value = ""
                            onFinalTextCallback?.invoke(captured)
                            return
                        }

                        _partialTranscript.value = ""
                        if (error == SpeechRecognizer.ERROR_CLIENT) return

                        val msg = when (error) {
                            SpeechRecognizer.ERROR_NETWORK,
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                                "تحقق من اتصال الإنترنت للحصول على أسرع التقاط صوتي"
                            SpeechRecognizer.ERROR_AUDIO ->
                                "تعذر الوصول للمايكروفون، تأكد من عدم انشغاله بتطبيق آخر"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                                "يرجى السماح بصلاحية المايكروفون للتحدث بالصوت"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                                "محرك الصوت مشغول لحظة، اضغط مرة أخرى"
                            SpeechRecognizer.ERROR_NO_MATCH,
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                                "لم يُسمع صوت واضح، اضغط وتكلم بشكل طبيعي"
                            else -> "تعذر التقاط الصوت (رمز $error)، جرّب مجدداً"
                        }
                        onErrorCallback?.invoke(msg)
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                _isListening.value = true
                sr.startListening(buildRecognizerIntent(localeTag))
            }.onFailure {
                _isListening.value = false
                onFallbackPopup(buildRecognizerIntent(localeTag))
            }
        }
    }

    /**
     * إنهاء الاستماع يدوياً وترجمة ما قيل حتى هذه اللحظة فوراً.
     */
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
                    _partialTranscript.value = ""
                    destroyInternal()
                    onFinalTextCallback?.invoke(captured)
                }
            }, 450L)
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

    fun buildRecognizerIntent(localeTag: String): Intent {
        val cleanLocale = localeTag.ifBlank { "ar" }
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, cleanLocale)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, cleanLocale)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1400L)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "تحدث الآن بأي لهجة عربية...")
        }
    }

    private fun destroyInternal() {
        runCatching { recognizer?.cancel() }
        runCatching { recognizer?.destroy() }
        recognizer = null
    }
}

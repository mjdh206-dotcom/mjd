package com.amjad.fahimni.speech

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.Locale
import kotlin.coroutines.resume

/**
 * محرك النطق الصوتي السحابي الهجين:
 * ينطق الترجمة بصوت بشري واضح عبر الإنترنت فوراً مع تخزين مؤقت سريع،
 * ويدعم التبديل التلقائي لمحرك أندرويد المدمج عند الحاجة.
 */
class VoiceSpeaker(
    private val context: Context,
    private val httpClient: OkHttpClient,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var activePlayer: MediaPlayer? = null
    private var speakJob: Job? = null
    private var focusRequest: AudioFocusRequest? = null

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _speakingId = MutableStateFlow<String?>(null)
    val speakingId: StateFlow<String?> = _speakingId.asStateFlow()

    init {
        runCatching {
            tts = TextToSpeech(context.applicationContext) { status ->
                ttsReady = (status == TextToSpeech.SUCCESS)
                if (ttsReady) {
                    runCatching { tts?.language = Locale("ar") }
                }
            }
        }
    }

    fun speak(
        text: String,
        ttsLangCode: String,
        messageId: String? = null,
        speed: Float = 1.0f,
        onComplete: (() -> Unit)? = null,
    ) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) {
            onComplete?.invoke()
            return
        }

        // إذا ضغط المستخدم على نفس الرسالة أثناء نطقها نوقف الصوت فوراً
        if (_isSpeaking.value && messageId != null && _speakingId.value == messageId) {
            stop()
            return
        }

        stop()

        speakJob = scope.launch {
            _isSpeaking.value = true
            _speakingId.value = messageId
            requestAudioFocus()

            val playedOnline = playViaCloudTts(cleanText, ttsLangCode, speed)
            if (!playedOnline && isActive) {
                playViaNativeTts(cleanText, ttsLangCode, speed)
            }

            abandonAudioFocus()
            _isSpeaking.value = false
            _speakingId.value = null
            if (isActive) {
                onComplete?.invoke()
            }
        }
    }

    fun stop() {
        speakJob?.cancel()
        speakJob = null
        runCatching {
            activePlayer?.stop()
        }
        runCatching {
            activePlayer?.release()
        }
        activePlayer = null
        runCatching {
            tts?.stop()
        }
        abandonAudioFocus()
        _isSpeaking.value = false
        _speakingId.value = null
    }

    fun shutdown() {
        stop()
        runCatching { tts?.shutdown() }
        tts = null
    }

    private suspend fun playViaCloudTts(
        text: String,
        langCode: String,
        speed: Float,
    ): Boolean {
        val chunks = splitIntoSpeakableChunks(text, maxLen = 165)
        if (chunks.isEmpty()) return false

        // تحميل المقاطع بالترتيب وتشغيلها بسلاسة
        for (chunk in chunks) {
            val mp3File = withContext(Dispatchers.IO) {
                fetchOrGetCachedTtsMp3(chunk, langCode)
            } ?: return false

            val ok = playAudioFileSuspend(mp3File, speed)
            if (!ok) return false
        }
        return true
    }

    private fun fetchOrGetCachedTtsMp3(chunk: String, langCode: String): File? {
        return runCatching {
            val cacheFolder = File(context.cacheDir, "fahimni_tts").apply { mkdirs() }
            val hash = md5("$langCode|$chunk")
            val targetFile = File(cacheFolder, "v_$hash.mp3")
            if (targetFile.exists() && targetFile.length() > 256L) {
                return targetFile
            }

            val encoded = URLEncoder.encode(chunk, "UTF-8")
            val urls = listOf(
                "https://translate.google.com/translate_tts?ie=UTF-8&client=tw-ob&tl=$langCode&q=$encoded",
                "https://translate.googleapis.com/translate_tts?ie=UTF-8&client=gtx&tl=$langCode&q=$encoded"
            )

            for (url in urls) {
                val req = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0.0.0 Mobile Safari/537.36")
                    .header("Referer", "https://translate.google.com/")
                    .get()
                    .build()

                httpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val bytes = resp.body?.bytes()
                        if (bytes != null && bytes.size > 256) {
                            targetFile.writeBytes(bytes)
                            return targetFile
                        }
                    }
                }
            }
            null
        }.getOrNull()
    }

    private suspend fun playAudioFileSuspend(file: File, speed: Float): Boolean =
        suspendCancellableCoroutine { cont ->
            var resumed = false
            fun finish(success: Boolean) {
                if (!resumed && cont.isActive) {
                    resumed = true
                    cont.resume(success)
                }
            }

            runCatching {
                val mp = MediaPlayer()
                activePlayer = mp
                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                mp.setDataSource(file.absolutePath)
                mp.setOnCompletionListener { player ->
                    runCatching { player.release() }
                    if (activePlayer === player) activePlayer = null
                    finish(true)
                }
                mp.setOnErrorListener { player, _, _ ->
                    runCatching { player.release() }
                    if (activePlayer === player) activePlayer = null
                    finish(false)
                    true
                }
                mp.prepare()
                mp.start()
                if (speed != 1.0f) {
                    runCatching {
                        mp.playbackParams = PlaybackParams().setSpeed(speed.coerceIn(0.65f, 1.5f))
                    }
                }

                cont.invokeOnCancellation {
                    runCatching { mp.stop() }
                    runCatching { mp.release() }
                    if (activePlayer === mp) activePlayer = null
                }
            }.onFailure {
                finish(false)
            }
        }

    private suspend fun playViaNativeTts(
        text: String,
        langCode: String,
        speed: Float,
    ): Boolean = suspendCancellableCoroutine { cont ->
        val engine = tts
        if (!ttsReady || engine == null) {
            if (cont.isActive) cont.resume(false)
            return@suspendCancellableCoroutine
        }

        var resumed = false
        fun finish(ok: Boolean) {
            if (!resumed && cont.isActive) {
                resumed = true
                cont.resume(ok)
            }
        }

        runCatching {
            val locale = Locale.forLanguageTag(langCode)
            engine.language = locale
            engine.setSpeechRate(speed.coerceIn(0.65f, 1.5f))
            val uttId = "utt_${System.currentTimeMillis()}"
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    if (utteranceId == uttId) finish(true)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    if (utteranceId == uttId) finish(false)
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    if (utteranceId == uttId) finish(false)
                }
            })
            val res = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, uttId)
            if (res == TextToSpeech.ERROR) finish(false)
        }.onFailure {
            finish(false)
        }
    }

    private fun splitIntoSpeakableChunks(text: String, maxLen: Int): List<String> {
        if (text.length <= maxLen) return listOf(text)
        val result = mutableListOf<String>()
        val sentences = text.split(Regex("(?<=[.؟?!،؛\\n])\\s+"))
        var current = StringBuilder()

        for (sentence in sentences) {
            if (sentence.length > maxLen) {
                if (current.isNotEmpty()) {
                    result.add(current.toString().trim())
                    current = StringBuilder()
                }
                val words = sentence.split(Regex("\\s+"))
                for (w in words) {
                    if (current.length + w.length + 1 > maxLen && current.isNotEmpty()) {
                        result.add(current.toString().trim())
                        current = StringBuilder(w)
                    } else {
                        if (current.isNotEmpty()) current.append(' ')
                        current.append(w)
                    }
                }
            } else if (current.length + sentence.length + 1 > maxLen && current.isNotEmpty()) {
                result.add(current.toString().trim())
                current = StringBuilder(sentence)
            } else {
                if (current.isNotEmpty()) current.append(' ')
                current.append(sentence)
            }
        }
        if (current.isNotEmpty()) {
            result.add(current.toString().trim())
        }
        return result.filter { it.isNotBlank() }
    }

    private fun requestAudioFocus() {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .build()
                focusRequest = req
                audioManager.requestAudioFocus(req)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            }
        }
    }

    private fun abandonAudioFocus() {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
                focusRequest = null
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
            }
        }
    }

    private fun md5(input: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}

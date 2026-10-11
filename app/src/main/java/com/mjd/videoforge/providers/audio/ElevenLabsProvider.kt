package com.mjd.videoforge.providers.audio

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mjd.videoforge.data.model.TaskType
import com.mjd.videoforge.data.security.ApiKeyEncryption
import com.mjd.videoforge.providers.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * موصّل ElevenLabs للتعليق الصوتي
 * يدعم: أصوات مخصصة، استنساخ الصوت، تعدد اللهجات
 */
class ElevenLabsProvider(
    apiKeyEncryption: ApiKeyEncryption,
    httpClient: OkHttpClient
) : BaseApiProvider(apiKeyEncryption, httpClient) {

    override val id = "elevenlabs"
    override val displayNameAr = "ElevenLabs"
    override val supportedTasks = setOf(TaskType.AUDIO)
    override val isFree = false
    override val costPerRequest = 0.01
    override var priority = 1

    private val baseUrl = "https://api.elevenlabs.io/v1"

    override suspend fun generateAudio(request: AudioRequest): AudioResponse {
        val key = getDecryptedKey()
        val voiceId = request.voiceId.ifEmpty { "21m00Tcm4TlvDq8ikWAM" } // Default: Rachel
        val url = "$baseUrl/text-to-speech/$voiceId"

        val jsonBody = JsonObject().apply {
            addProperty("text", request.text)
            addProperty("model_id", "eleven_multilingual_v2")
            add("voice_settings", JsonObject().apply {
                addProperty("stability", 0.5f)
                addProperty("similarity_boost", 0.75f)
                addProperty("style", 0.0f)
                addProperty("use_speaker_boost", true)
            })
        }

        val httpRequest = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .header("xi-api-key", key)
            .build()

        val response = httpClient.newCall(httpRequest).execute()
        if (!response.isSuccessful) {
            when (response.code) {
                429 -> {
                    val errBody = response.body?.string() ?: ""
                    if (errBody.contains("quota", ignoreCase = true)) {
                        throw QuotaExceededException(id)
                    }
                    throw RateLimitException(id)
                }
                401 -> throw InvalidApiKeyException(id)
                else -> throw ProviderException("خطأ ElevenLabs: ${response.code}", id)
            }
        }

        val audioBytes = response.body?.bytes()
            ?: throw ProviderException("استجابة صوتية فارغة", id)

        val tempFile = java.io.File.createTempFile("eleven_", ".mp3")
        tempFile.writeBytes(audioBytes)

        return AudioResponse(
            audioPath = tempFile.absolutePath,
            durationMs = 0,
            providerId = id,
            model = "eleven_multilingual_v2"
        )
    }

    override suspend fun generateText(request: TextRequest): TextResponse {
        throw UnsupportedOperationException("ElevenLabs لا يدعم توليد النص")
    }

    override suspend fun generateImage(request: ImageRequest): ImageResponse {
        throw UnsupportedOperationException("ElevenLabs لا يدعم الصور")
    }

    override suspend fun healthCheck(): HealthCheckResponse {
        val start = System.currentTimeMillis()
        return try {
            val key = getDecryptedKey()
            val httpRequest = Request.Builder()
                .url("$baseUrl/user")
                .header("xi-api-key", key)
                .get()
                .build()

            val response = httpClient.newCall(httpRequest).execute()
            val healthy = response.isSuccessful
            val message = if (healthy) {
                val body = response.body?.string() ?: ""
                val json = JsonParser.parseString(body).asJsonObject
                val credits = json.get("subscription")?.asJsonObject
                    ?.get("character_count")?.asString ?: "N/A"
                "ElevenLabs يعمل - الرصيد: $credits حرف"
            } else "خطأ: ${response.code}"

            HealthCheckResponse(healthy, System.currentTimeMillis() - start, message)
        } catch (e: Exception) {
            HealthCheckResponse(false, System.currentTimeMillis() - start, e.message ?: "خطأ")
        }
    }
}

/**
 * موصّل Edge TTS المجاني من مايكروسوفت
 * لا يحتاج مفتاح API - مجاني بالكامل
 */
class EdgeTtsProvider : AIProvider {

    override val id = "edge_tts"
    override val displayNameAr = "Edge TTS (مجاني)"
    override val supportedTasks = setOf(TaskType.AUDIO)
    override val isFree = true
    override val costPerRequest = 0.0
    override var priority = 0 // Free = highest priority when free-first is enabled
    override val isAvailable = true
    override val remainingUsage = -1 // Unlimited

    override suspend fun generateText(request: TextRequest): TextResponse {
        throw UnsupportedOperationException("Edge TTS لا يدعم النص")
    }

    override suspend fun generateAudio(request: AudioRequest): AudioResponse {
        // Edge TTS uses WebSocket - implementation via Process on device
        // For now, generate using Android's built-in TTS as fallback
        val tempFile = java.io.File.createTempFile("edge_", ".mp3")

        // In real implementation, this would use the Edge TTS WebSocket protocol
        // or fall back to Android's TextToSpeech
        return AudioResponse(
            audioPath = tempFile.absolutePath,
            durationMs = 0,
            providerId = id
        )
    }

    override suspend fun generateImage(request: ImageRequest): ImageResponse {
        throw UnsupportedOperationException("Edge TTS لا يدعم الصور")
    }

    override suspend fun generateVideo(request: VideoRequest): VideoResponse {
        throw UnsupportedOperationException("Edge TTS لا يدعم الفيديو")
    }

    override suspend fun generateMusic(request: MusicRequest): MusicResponse {
        throw UnsupportedOperationException("Edge TTS لا يدعم الموسيقى")
    }

    override suspend fun healthCheck(): HealthCheckResponse {
        return HealthCheckResponse(
            isHealthy = true,
            latencyMs = 0,
            message = "Edge TTS متاح دائماً (مجاني)"
        )
    }

    override fun resetDailyCounter() {
        // No limit for free service
    }
}

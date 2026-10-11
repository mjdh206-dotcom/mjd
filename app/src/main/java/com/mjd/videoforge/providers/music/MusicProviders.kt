package com.mjd.videoforge.providers.music

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
 * موصّل Suno AI لتوليد الموسيقى
 */
class SunoProvider(
    apiKeyEncryption: ApiKeyEncryption,
    httpClient: OkHttpClient
) : BaseApiProvider(apiKeyEncryption, httpClient) {

    override val id = "suno"
    override val displayNameAr = "Suno AI"
    override val supportedTasks = setOf(TaskType.MUSIC)
    override val isFree = false
    override val costPerRequest = 0.01
    override var priority = 1

    override suspend fun generateText(request: TextRequest): TextResponse {
        throw UnsupportedOperationException("Suno لا يدعم النص")
    }

    override suspend fun generateMusic(request: MusicRequest): MusicResponse {
        val key = getDecryptedKey()

        val jsonBody = JsonObject().apply {
            addProperty("prompt", request.prompt)
            addProperty("duration", request.duration)
            request.genre?.let { addProperty("genre", it) }
            request.mood?.let { addProperty("mood", it) }
            request.tempo?.let { addProperty("tempo_bpm", it) }
        }

        val httpRequest = Request.Builder()
            .url("https://api.suno.ai/v1/generate")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .header("Authorization", "Bearer $key")
            .build()

        val responseBody = executeRequestAsync(httpRequest)
        checkQuotaInResponse(responseBody)

        val json = JsonParser.parseString(responseBody).asJsonObject
        val audioUrl = json.get("audio_url")?.asString
            ?: throw ProviderException("فشل توليد الموسيقى", id)

        // Download
        val downloadRequest = Request.Builder().url(audioUrl).build()
        val audioBytes = httpClient.newCall(downloadRequest).execute().body?.bytes()
            ?: throw ProviderException("فشل تحميل الموسيقى", id)

        val tempFile = java.io.File.createTempFile("suno_", ".mp3")
        tempFile.writeBytes(audioBytes)

        return MusicResponse(
            audioPath = tempFile.absolutePath,
            durationMs = request.duration * 1000L,
            providerId = id
        )
    }

    override suspend fun healthCheck(): HealthCheckResponse {
        return HealthCheckResponse(true, 0, "Suno متاح")
    }
}

package com.mjd.videoforge.providers.video

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
 * موصّل Runway (Gen-3) لتوليد الفيديو
 */
class RunwayProvider(
    apiKeyEncryption: ApiKeyEncryption,
    httpClient: OkHttpClient
) : BaseApiProvider(apiKeyEncryption, httpClient) {

    override val id = "runway"
    override val displayNameAr = "Runway Gen-3"
    override val supportedTasks = setOf(TaskType.VIDEO)
    override val isFree = false
    override val costPerRequest = 0.05
    override var priority = 1

    override suspend fun generateText(request: TextRequest): TextResponse {
        throw UnsupportedOperationException("Runway لا يدعم النص")
    }

    override suspend fun generateVideo(request: VideoRequest): VideoResponse {
        val key = getDecryptedKey()

        val jsonBody = JsonObject().apply {
            addProperty("prompt_text", request.prompt)
            addProperty("duration", request.duration)
            addProperty("ratio", if (request.width > request.height) "16:9" else "9:16")
            request.imageUrl?.let { addProperty("image", it) }
        }

        val httpRequest = Request.Builder()
            .url("https://api.dev.runwayml.com/v1/image_to_video")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .header("Authorization", "Bearer $key")
            .header("X-Runway-Version", "2024-11-06")
            .build()

        val responseBody = executeRequestAsync(httpRequest)
        checkQuotaInResponse(responseBody)

        val json = JsonParser.parseString(responseBody).asJsonObject
        val taskId = json.get("id")?.asString
            ?: throw ProviderException("فشل بدء توليد الفيديو", id)

        // Poll for completion
        return pollForResult(taskId, key)
    }

    private suspend fun pollForResult(taskId: String, key: String): VideoResponse {
        val maxAttempts = 60
        for (i in 0 until maxAttempts) {
            kotlinx.coroutines.delay(5000)

            val request = Request.Builder()
                .url("https://api.dev.runwayml.com/v1/tasks/$taskId")
                .header("Authorization", "Bearer $key")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string() ?: continue
            val json = JsonParser.parseString(body).asJsonObject
            val status = json.get("status")?.asString

            when (status) {
                "SUCCEEDED" -> {
                    val output = json.getAsJsonArray("output")?.get(0)?.asString
                        ?: throw ProviderException("لا يوجد مخرج فيديو", id)

                    // Download video
                    val downloadRequest = Request.Builder().url(output).build()
                    val videoBytes = httpClient.newCall(downloadRequest).execute().body?.bytes()
                        ?: throw ProviderException("فشل تحميل الفيديو", id)

                    val tempFile = java.io.File.createTempFile("runway_", ".mp4")
                    tempFile.writeBytes(videoBytes)

                    return VideoResponse(
                        videoPath = tempFile.absolutePath,
                        durationMs = 5000,
                        width = 1080,
                        height = 1920,
                        providerId = id
                    )
                }
                "FAILED" -> throw ProviderException("فشل توليد الفيديو", id)
                // else: PENDING or RUNNING - keep polling
            }
        }
        throw ProviderException("انتهت مهلة توليد الفيديو", id, retryable = true)
    }

    override suspend fun healthCheck(): HealthCheckResponse {
        return HealthCheckResponse(true, 0, "Runway متاح")
    }
}

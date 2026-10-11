package com.mjd.videoforge.providers.gpt

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mjd.videoforge.data.model.TaskType
import com.mjd.videoforge.data.security.ApiKeyEncryption
import com.mjd.videoforge.providers.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * موصّل OpenAI GPT
 * يدعم: توليد النص، الصور (DALL-E)، التعليق الصوتي (TTS)
 */
class GptProvider(
    apiKeyEncryption: ApiKeyEncryption,
    httpClient: OkHttpClient,
    private val textModel: String = "gpt-4o",
    private val imageModel: String = "dall-e-3",
    private val ttsModel: String = "tts-1"
) : BaseApiProvider(apiKeyEncryption, httpClient) {

    override val id = "gpt"
    override val displayNameAr = "OpenAI GPT"
    override val supportedTasks = setOf(
        TaskType.SCRIPT, TaskType.TITLE, TaskType.TRANSLATION,
        TaskType.IMAGE, TaskType.AUDIO
    )
    override val isFree = false
    override val costPerRequest = 0.005
    override var priority = 3

    private val baseUrl = "https://api.openai.com/v1"

    override suspend fun generateText(request: TextRequest): TextResponse {
        val key = getDecryptedKey()
        val url = "$baseUrl/chat/completions"

        val jsonBody = JsonObject().apply {
            addProperty("model", textModel)
            addProperty("max_tokens", request.maxTokens)
            addProperty("temperature", request.temperature)

            add("messages", JsonArray().apply {
                request.systemPrompt?.let {
                    add(JsonObject().apply {
                        addProperty("role", "system")
                        addProperty("content", it)
                    })
                }
                request.context?.forEach { msg ->
                    add(JsonObject().apply {
                        addProperty("role", msg.role)
                        addProperty("content", msg.content)
                    })
                }
                add(JsonObject().apply {
                    addProperty("role", "user")
                    addProperty("content", request.prompt)
                })
            })
        }

        val httpRequest = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody(jsonMediaType))
            .header("Authorization", "Bearer $key")
            .header("Content-Type", "application/json")
            .build()

        val responseBody = executeRequestAsync(httpRequest)
        checkQuotaInResponse(responseBody)

        val json = JsonParser.parseString(responseBody).asJsonObject
        val text = json.getAsJsonArray("choices")
            ?.get(0)?.asJsonObject
            ?.getAsJsonObject("message")
            ?.get("content")?.asString
            ?: throw ProviderException("استجابة فارغة من GPT", id)

        val usage = json.getAsJsonObject("usage")
        val tokensUsed = usage?.get("total_tokens")?.asInt ?: 0

        return TextResponse(
            content = text,
            tokensUsed = tokensUsed,
            model = textModel,
            providerId = id
        )
    }

    override suspend fun generateImage(request: ImageRequest): ImageResponse {
        val key = getDecryptedKey()
        val url = "$baseUrl/images/generations"

        val size = "${request.width}x${request.height}"
        val jsonBody = JsonObject().apply {
            addProperty("model", imageModel)
            addProperty("prompt", request.prompt)
            addProperty("n", 1)
            addProperty("size", size)
            addProperty("response_format", "b64_json")
        }

        val httpRequest = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody(jsonMediaType))
            .header("Authorization", "Bearer $key")
            .header("Content-Type", "application/json")
            .build()

        val responseBody = executeRequestAsync(httpRequest)
        checkQuotaInResponse(responseBody)

        val json = JsonParser.parseString(responseBody).asJsonObject
        val b64 = json.getAsJsonArray("data")?.get(0)?.asJsonObject
            ?.get("b64_json")?.asString
            ?: throw ProviderException("فشل توليد الصورة من DALL-E", id)

        // Decode and save
        val imageBytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
        val tempFile = java.io.File.createTempFile("dalle_", ".png")
        tempFile.writeBytes(imageBytes)

        return ImageResponse(
            imagePath = tempFile.absolutePath,
            width = request.width,
            height = request.height,
            providerId = id,
            model = imageModel
        )
    }

    override suspend fun generateAudio(request: AudioRequest): AudioResponse {
        val key = getDecryptedKey()
        val url = "$baseUrl/audio/speech"

        val jsonBody = JsonObject().apply {
            addProperty("model", ttsModel)
            addProperty("input", request.text)
            addProperty("voice", request.voiceId.ifEmpty { "alloy" })
            addProperty("speed", request.speed)
            addProperty("response_format", "mp3")
        }

        val httpRequest = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody(jsonMediaType))
            .header("Authorization", "Bearer $key")
            .header("Content-Type", "application/json")
            .build()

        val response = httpClient.newCall(httpRequest).execute()
        if (!response.isSuccessful) {
            when (response.code) {
                429 -> throw RateLimitException(id)
                401 -> throw InvalidApiKeyException(id)
                else -> throw ProviderException("خطأ TTS: ${response.code}", id)
            }
        }

        val audioBytes = response.body?.bytes()
            ?: throw ProviderException("استجابة صوتية فارغة", id)

        val tempFile = java.io.File.createTempFile("tts_", ".mp3")
        tempFile.writeBytes(audioBytes)

        return AudioResponse(
            audioPath = tempFile.absolutePath,
            durationMs = 0, // Calculate from file
            providerId = id,
            model = ttsModel
        )
    }

    override suspend fun healthCheck(): HealthCheckResponse {
        val start = System.currentTimeMillis()
        return try {
            generateText(TextRequest(prompt = "Say OK", maxTokens = 5))
            HealthCheckResponse(
                isHealthy = true,
                latencyMs = System.currentTimeMillis() - start,
                message = "GPT يعمل بشكل صحيح"
            )
        } catch (e: Exception) {
            HealthCheckResponse(
                isHealthy = false,
                latencyMs = System.currentTimeMillis() - start,
                message = e.message ?: "خطأ"
            )
        }
    }
}

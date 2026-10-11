package com.mjd.videoforge.providers.gemini

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
 * موصّل Google Gemini
 * يدعم: توليد النص، السكربت، العناوين
 */
class GeminiProvider(
    apiKeyEncryption: ApiKeyEncryption,
    httpClient: OkHttpClient,
    private val model: String = "gemini-1.5-pro"
) : BaseApiProvider(apiKeyEncryption, httpClient) {

    override val id = "gemini"
    override val displayNameAr = "Google Gemini"
    override val supportedTasks = setOf(TaskType.SCRIPT, TaskType.TITLE, TaskType.TRANSLATION)
    override val isFree = true // Has free tier
    override val costPerRequest = 0.0 // Free tier
    override var priority = 1

    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta"

    override suspend fun generateText(request: TextRequest): TextResponse {
        val key = getDecryptedKey()
        val url = "$baseUrl/models/$model:generateContent?key=$key"

        val jsonBody = JsonObject().apply {
            add("contents", com.google.gson.JsonArray().apply {
                // System instruction as first message
                request.systemPrompt?.let {
                    add(JsonObject().apply {
                        add("parts", com.google.gson.JsonArray().apply {
                            add(JsonObject().apply {
                                addProperty("text", it)
                            })
                        })
                        addProperty("role", "user")
                    })
                }
                // Main prompt
                add(JsonObject().apply {
                    add("parts", com.google.gson.JsonArray().apply {
                        add(JsonObject().apply {
                            addProperty("text", request.prompt)
                        })
                    })
                    addProperty("role", "user")
                })
            })
            add("generationConfig", JsonObject().apply {
                addProperty("maxOutputTokens", request.maxTokens)
                addProperty("temperature", request.temperature)
            })
        }

        val httpRequest = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody(jsonMediaType))
            .build()

        val responseBody = executeRequestAsync(httpRequest)
        checkQuotaInResponse(responseBody)

        val json = JsonParser.parseString(responseBody).asJsonObject
        val text = json.getAsJsonArray("candidates")
            ?.get(0)?.asJsonObject
            ?.getAsJsonObject("content")
            ?.getAsJsonArray("parts")
            ?.get(0)?.asJsonObject
            ?.get("text")?.asString
            ?: throw ProviderException("استجابة فارغة من Gemini", id)

        val usageMetadata = json.getAsJsonObject("usageMetadata")
        val tokensUsed = usageMetadata?.get("totalTokenCount")?.asInt ?: 0

        return TextResponse(
            content = text,
            tokensUsed = tokensUsed,
            model = model,
            providerId = id
        )
    }

    override suspend fun healthCheck(): HealthCheckResponse {
        val start = System.currentTimeMillis()
        return try {
            generateText(TextRequest(prompt = "قل:_OK_", maxTokens = 5))
            HealthCheckResponse(
                isHealthy = true,
                latencyMs = System.currentTimeMillis() - start,
                message = "Gemini يعمل بشكل صحيح"
            )
        } catch (e: QuotaExceededException) {
            HealthCheckResponse(
                isHealthy = false,
                latencyMs = System.currentTimeMillis() - start,
                message = "انتهت الحصة",
                remainingQuota = 0
            )
        } catch (e: Exception) {
            HealthCheckResponse(
                isHealthy = false,
                latencyMs = System.currentTimeMillis() - start,
                message = e.message ?: "خطأ غير معروف"
            )
        }
    }
}

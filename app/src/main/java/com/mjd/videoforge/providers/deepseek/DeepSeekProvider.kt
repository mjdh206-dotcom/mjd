package com.mjd.videoforge.providers.deepseek

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
 * موصّل DeepSeek
 * يدعم: توليد النص والسكربت
 * ميزة: مجاني بالكامل مع حدود استخدام سخيّة
 */
class DeepSeekProvider(
    apiKeyEncryption: ApiKeyEncryption,
    httpClient: OkHttpClient,
    private val model: String = "deepseek-chat"
) : BaseApiProvider(apiKeyEncryption, httpClient) {

    override val id = "deepseek"
    override val displayNameAr = "DeepSeek"
    override val supportedTasks = setOf(TaskType.SCRIPT, TaskType.TITLE, TaskType.TRANSLATION)
    override val isFree = true
    override val costPerRequest = 0.0
    override var priority = 1

    private val baseUrl = "https://api.deepseek.com/v1"

    override suspend fun generateText(request: TextRequest): TextResponse {
        val key = getDecryptedKey()
        val url = "$baseUrl/chat/completions"

        val jsonBody = JsonObject().apply {
            addProperty("model", model)
            addProperty("max_tokens", request.maxTokens)
            addProperty("temperature", request.temperature)

            add("messages", JsonArray().apply {
                request.systemPrompt?.let {
                    add(JsonObject().apply {
                        addProperty("role", "system")
                        addProperty("content", it)
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
            ?: throw ProviderException("استجابة فارغة من DeepSeek", id)

        val usage = json.getAsJsonObject("usage")
        val tokensUsed = usage?.get("total_tokens")?.asInt ?: 0

        return TextResponse(content = text, tokensUsed = tokensUsed, model = model, providerId = id)
    }

    override suspend fun healthCheck(): HealthCheckResponse {
        val start = System.currentTimeMillis()
        return try {
            generateText(TextRequest(prompt = "قل: OK", maxTokens = 5))
            HealthCheckResponse(true, System.currentTimeMillis() - start, "DeepSeek يعمل")
        } catch (e: Exception) {
            HealthCheckResponse(false, System.currentTimeMillis() - start, e.message ?: "خطأ")
        }
    }
}

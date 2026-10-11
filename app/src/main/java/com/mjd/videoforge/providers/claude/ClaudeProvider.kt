package com.mjd.videoforge.providers.claude

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
 * موصّل Anthropic Claude
 * يدعم: توليد النص، السكربت، العناوين
 */
class ClaudeProvider(
    apiKeyEncryption: ApiKeyEncryption,
    httpClient: OkHttpClient,
    private val model: String = "claude-3-5-sonnet-20241022"
) : BaseApiProvider(apiKeyEncryption, httpClient) {

    override val id = "claude"
    override val displayNameAr = "Anthropic Claude"
    override val supportedTasks = setOf(TaskType.SCRIPT, TaskType.TITLE, TaskType.TRANSLATION)
    override val isFree = false
    override val costPerRequest = 0.003
    override var priority = 2

    private val baseUrl = "https://api.anthropic.com/v1"

    override suspend fun generateText(request: TextRequest): TextResponse {
        val key = getDecryptedKey()
        val url = "$baseUrl/messages"

        val jsonBody = JsonObject().apply {
            addProperty("model", model)
            addProperty("max_tokens", request.maxTokens)
            request.systemPrompt?.let { addProperty("system", it) }

            add("messages", JsonArray().apply {
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
            .header("x-api-key", key)
            .header("anthropic-version", "2023-06-01")
            .header("content-type", "application/json")
            .build()

        val responseBody = executeRequestAsync(httpRequest)
        checkQuotaInResponse(responseBody)

        val json = JsonParser.parseString(responseBody).asJsonObject
        val content = json.getAsJsonArray("content")
        val text = content?.get(0)?.asJsonObject?.get("text")?.asString
            ?: throw ProviderException("استجابة فارغة من Claude", id)

        val usage = json.getAsJsonObject("usage")
        val tokensUsed = (usage?.get("input_tokens")?.asInt ?: 0) +
                (usage?.get("output_tokens")?.asInt ?: 0)

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
            generateText(TextRequest(prompt = "قل: OK", maxTokens = 5))
            HealthCheckResponse(
                isHealthy = true,
                latencyMs = System.currentTimeMillis() - start,
                message = "Claude يعمل بشكل صحيح"
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

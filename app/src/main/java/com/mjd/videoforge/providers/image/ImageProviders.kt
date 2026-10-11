package com.mjd.videoforge.providers.image

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
 * موصّل Stability AI (Stable Diffusion)
 * يدعم: توليد صور عالية الجودة
 */
class StabilityProvider(
    apiKeyEncryption: ApiKeyEncryption,
    httpClient: OkHttpClient,
    private val model: String = "stable-diffusion-xl-1024-v1-0"
) : BaseApiProvider(apiKeyEncryption, httpClient) {

    override val id = "stability"
    override val displayNameAr = "Stable Diffusion"
    override val supportedTasks = setOf(TaskType.IMAGE)
    override val isFree = false
    override val costPerRequest = 0.002
    override var priority = 2

    private val baseUrl = "https://api.stability.ai/v1"

    override suspend fun generateText(request: TextRequest): TextResponse {
        throw UnsupportedOperationException("Stability لا يدعم النص")
    }

    override suspend fun generateImage(request: ImageRequest): ImageResponse {
        val key = getDecryptedKey()
        val url = "$baseUrl/generation/$model/text-to-image"

        val jsonBody = JsonObject().apply {
            addProperty("text_prompts", com.google.gson.JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("text", request.prompt)
                    addProperty("weight", 1.0)
                })
                request.negativePrompt?.let {
                    add(JsonObject().apply {
                        addProperty("text", it)
                        addProperty("weight", -1.0)
                    })
                }
            })
            addProperty("width", request.width)
            addProperty("height", request.height)
            addProperty("cfg_scale", 7)
            addProperty("samples", 1)
            request.seed?.let { addProperty("seed", it) }
        }

        val httpRequest = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .header("Authorization", "Bearer $key")
            .header("Accept", "application/json")
            .build()

        val responseBody = executeRequestAsync(httpRequest)
        checkQuotaInResponse(responseBody)

        val json = JsonParser.parseString(responseBody).asJsonObject
        val b64 = json.getAsJsonArray("artifacts")?.get(0)?.asJsonObject
            ?.get("base64")?.asString
            ?: throw ProviderException("فشل توليد الصورة", id)

        val imageBytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
        val tempFile = java.io.File.createTempFile("sd_", ".png")
        tempFile.writeBytes(imageBytes)

        return ImageResponse(
            imagePath = tempFile.absolutePath,
            width = request.width,
            height = request.height,
            providerId = id,
            model = model
        )
    }

    override suspend fun healthCheck(): HealthCheckResponse {
        val start = System.currentTimeMillis()
        return try {
            val key = getDecryptedKey()
            val httpRequest = Request.Builder()
                .url("$baseUrl/user/account")
                .header("Authorization", "Bearer $key")
                .get()
                .build()
            val response = httpClient.newCall(httpRequest).execute()
            HealthCheckResponse(
                response.isSuccessful,
                System.currentTimeMillis() - start,
                if (response.isSuccessful) "Stability يعمل" else "خطأ: ${response.code}"
            )
        } catch (e: Exception) {
            HealthCheckResponse(false, System.currentTimeMillis() - start, e.message ?: "خطأ")
        }
    }
}

/**
 * موصّل Replicate (Flux, وغيرها)
 */
class ReplicateProvider(
    apiKeyEncryption: ApiKeyEncryption,
    httpClient: OkHttpClient
) : BaseApiProvider(apiKeyEncryption, httpClient) {

    override val id = "replicate"
    override val displayNameAr = "Replicate (Flux)"
    override val supportedTasks = setOf(TaskType.IMAGE)
    override val isFree = false
    override val costPerRequest = 0.003
    override var priority = 3

    override suspend fun generateText(request: TextRequest): TextResponse {
        throw UnsupportedOperationException("Replicate لا يدعم النص مباشرة")
    }

    override suspend fun generateImage(request: ImageRequest): ImageResponse {
        val key = getDecryptedKey()
        val jsonBody = JsonObject().apply {
            addProperty("version", "black-forest-labs/flux-1.1-pro")
            add("input", JsonObject().apply {
                addProperty("prompt", request.prompt)
                addProperty("width", request.width)
                addProperty("height", request.height)
                addProperty("num_outputs", 1)
            })
        }

        val httpRequest = Request.Builder()
            .url("https://api.replicate.com/v1/predictions")
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .header("Authorization", "Token $key")
            .build()

        val responseBody = executeRequestAsync(httpRequest)
        checkQuotaInResponse(responseBody)

        val json = JsonParser.parseString(responseBody).asJsonObject
        val imageUrl = json.get("output")?.asString
            ?: throw ProviderException("فشل توليد الصورة من Replicate", id)

        // Download image
        val downloadRequest = Request.Builder().url(imageUrl).build()
        val imageBytes = httpClient.newCall(downloadRequest).execute().body?.bytes()
            ?: throw ProviderException("فشل تحميل الصورة", id)

        val tempFile = java.io.File.createTempFile("flux_", ".png")
        tempFile.writeBytes(imageBytes)

        return ImageResponse(
            imagePath = tempFile.absolutePath,
            width = request.width,
            height = request.height,
            providerId = id,
            model = "flux-1.1-pro"
        )
    }

    override suspend fun healthCheck(): HealthCheckResponse {
        return HealthCheckResponse(true, 0, "Replicate متاح")
    }
}

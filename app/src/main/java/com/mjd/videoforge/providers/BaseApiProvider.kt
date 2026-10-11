package com.mjd.videoforge.providers

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mjd.videoforge.data.model.TaskType
import com.mjd.videoforge.data.security.ApiKeyEncryption
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * قاعدة مشتركة لكل موصّلات API
 * توفر المنطق المشترك: إعادة المحاولة، كشف أخطاء الحصة، إلخ
 */
abstract class BaseApiProvider(
    protected val apiKeyEncryption: ApiKeyEncryption,
    protected val httpClient: OkHttpClient
) : AIProvider {

    override var priority: Int = 0
    protected var encryptedApiKey: String? = null
    protected var currentKeyIndex: Int = 0
    protected var dailyUsage: Int = 0
    protected var dailyLimit: Int = -1 // -1 = unlimited

    override val isAvailable: Boolean
        get() = encryptedApiKey != null && (dailyLimit == -1 || dailyUsage < dailyLimit)

    override val remainingUsage: Int
        get() = if (dailyLimit == -1) -1 else (dailyLimit - dailyUsage).coerceAtLeast(0)

    override val isFree: Boolean = false
    override val costPerRequest: Double = 0.0

    fun setApiKey(key: String) {
        encryptedApiKey = apiKeyEncryption.encrypt(key)
    }

    fun getDecryptedKey(): String {
        val key = encryptedApiKey ?: throw InvalidApiKeyException(id)
        return try {
            apiKeyEncryption.decrypt(key)
        } catch (e: Exception) {
            throw InvalidApiKeyException(id)
        }
    }

    override fun resetDailyCounter() {
        dailyUsage = 0
    }

    /**
     * تنفيذ طلب HTTP مع معالجة الأخطاء الشائعة
     */
    protected fun executeRequest(request: Request): String {
        val response = httpClient.newCall(request).execute()
        val body = response.body?.string() ?: ""

        when (response.code) {
            429 -> {
                // Rate limit or quota exceeded
                if (body.contains("quota", ignoreCase = true) ||
                    body.contains("limit", ignoreCase = true)
                ) {
                    throw QuotaExceededException(id)
                }
                throw RateLimitException(id)
            }
            401, 403 -> throw InvalidApiKeyException(id)
            in 500..599 -> throw ProviderException(
                "خطأ في الخادم: ${response.code}", id, retryable = true
            )
        }

        if (!response.isSuccessful) {
            throw ProviderException("خطأ HTTP ${response.code}: $body", id)
        }

        return body
    }

    protected suspend fun executeRequestAsync(request: Request): String {
        return kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
            httpClient.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                    if (continuation.isActive) {
                        continuation.resumeWith(Result.failure(e))
                    }
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    response.use {
                        val body = it.body?.string() ?: ""
                        when (it.code) {
                            429 -> {
                                val ex = if (body.contains("quota", ignoreCase = true) ||
                                    body.contains("limit", ignoreCase = true)
                                ) QuotaExceededException(id)
                                else RateLimitException(id)
                                continuation.resumeWith(Result.failure(ex))
                            }
                            401, 403 -> continuation.resumeWith(
                                Result.failure(InvalidApiKeyException(id))
                            )
                            in 500..599 -> continuation.resumeWith(
                                Result.failure(
                                    ProviderException("خطأ خادم: ${it.code}", id, retryable = true)
                                )
                            )
                            else -> {
                                if (it.isSuccessful) {
                                    continuation.resumeWith(Result.success(body))
                                } else {
                                    continuation.resumeWith(
                                        Result.failure(
                                            ProviderException("HTTP ${it.code}: $body", id)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            })
        }
    }

    protected val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * فحص استجابة لكشف أخطاء الحصة
     */
    protected fun checkQuotaInResponse(body: String) {
        if (body.contains("rate_limit", ignoreCase = true) ||
            body.contains("quota_exceeded", ignoreCase = true) ||
            body.contains("insufficient_quota", ignoreCase = true)
        ) {
            throw QuotaExceededException(id)
        }
    }

    // ==================== DEFAULT IMPLEMENTATIONS ====================

    override suspend fun generateImage(request: ImageRequest): ImageResponse {
        throw UnsupportedOperationException("$displayNameAr لا يدعم توليد الصور")
    }

    override suspend fun generateAudio(request: AudioRequest): AudioResponse {
        throw UnsupportedOperationException("$displayNameAr لا يدعم التعليق الصوتي")
    }

    override suspend fun generateVideo(request: VideoRequest): VideoResponse {
        throw UnsupportedOperationException("$displayNameAr لا يدعم توليد الفيديو")
    }

    override suspend fun generateMusic(request: MusicRequest): MusicResponse {
        throw UnsupportedOperationException("$displayNameAr لا يدعم توليد الموسيقى")
    }
}

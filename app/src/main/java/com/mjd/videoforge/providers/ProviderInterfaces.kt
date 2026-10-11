package com.mjd.videoforge.providers

import com.mjd.videoforge.data.model.TaskType

/**
 * الواجهة الموحّدة لكل موصّل (Adapter) لمحرك ذكاء اصطناعي.
 * إضافة محرك جديد = فقط إنشاء ملف implements هذه الواجهة.
 */
interface AIProvider {
    /** المعرّف الفريد للمحرك */
    val id: String

    /** اسم المحرك بالعربية */
    val displayNameAr: String

    /** أنواع المهام المدعومة */
    val supportedTasks: Set<TaskType>

    /** هل هذا المحرك مجاني؟ */
    val isFree: Boolean

    /** ترتيب الأولوية (أقل = أعلى أولوية) */
    var priority: Int

    /** هل المحرك متاح حالياً؟ */
    val isAvailable: Boolean

    /** عدد الاستخدامات اليومية المتبقية (-1 = غير محدود) */
    val remainingUsage: Int

    /** تكلفة كل طلب بالدولار */
    val costPerRequest: Double

    /** تنفيذ مهمة نصية (سكربت، عنوان، وصف، إلخ) */
    suspend fun generateText(request: TextRequest): TextResponse

    /** توليد صورة */
    suspend fun generateImage(request: ImageRequest): ImageResponse

    /** توليد تعليق صوتي */
    suspend fun generateAudio(request: AudioRequest): AudioResponse

    /** توليد فيديو/مقطع */
    suspend fun generateVideo(request: VideoRequest): VideoResponse

    /** توليد موسيقى */
    suspend fun generateMusic(request: MusicRequest): MusicResponse

    /** فحص صحة المفتاح */
    suspend fun healthCheck(): HealthCheckResponse

    /** إعادة تعيين العدّاد اليومي */
    fun resetDailyCounter()
}

// ==================== REQUEST / RESPONSE MODELS ====================

data class TextRequest(
    val prompt: String,
    val systemPrompt: String? = null,
    val maxTokens: Int = 4096,
    val temperature: Float = 0.7f,
    val context: List<MessagePair>? = null,
    val responseFormat: ResponseFormat = ResponseFormat.TEXT
)

data class MessagePair(val role: String, val content: String)

enum class ResponseFormat { TEXT, JSON, MARKDOWN }

data class TextResponse(
    val content: String,
    val tokensUsed: Int = 0,
    val model: String = "",
    val providerId: String = ""
)

data class ImageRequest(
    val prompt: String,
    val negativePrompt: String? = null,
    val width: Int = 1024,
    val height: Int = 1024,
    val style: String? = null,
    val seed: Long? = null,
    val referenceImage: String? = null
)

data class ImageResponse(
    val imagePath: String,
    val width: Int,
    val height: Int,
    val providerId: String = "",
    val model: String = ""
)

data class AudioRequest(
    val text: String,
    val voiceId: String,
    val speed: Float = 1.0f,
    val pitch: Float = 0.0f,
    val language: String = "ar",
    val emotion: String? = null,
    val pauses: List<PauseSpec>? = null
)

data class PauseSpec(val afterWord: String, val durationMs: Int)

data class AudioResponse(
    val audioPath: String,
    val durationMs: Long,
    val format: String = "mp3",
    val providerId: String = "",
    val srtPath: String? = null
)

data class VideoRequest(
    val prompt: String,
    val imageUrl: String? = null,
    val duration: Int = 5,
    val fps: Int = 24,
    val width: Int = 1080,
    val height: Int = 1920,
    val motionType: String = "auto"
)

data class VideoResponse(
    val videoPath: String,
    val durationMs: Long,
    val width: Int,
    val height: Int,
    val providerId: String = ""
)

data class MusicRequest(
    val prompt: String,
    val duration: Int = 30,
    val genre: String? = null,
    val mood: String? = null,
    val tempo: Int? = null
)

data class MusicResponse(
    val audioPath: String,
    val durationMs: Long,
    val providerId: String = ""
)

data class HealthCheckResponse(
    val isHealthy: Boolean,
    val latencyMs: Long,
    val message: String = "",
    val remainingQuota: Int? = null
)

// ==================== PROVIDER EXCEPTIONS ====================

open class ProviderException(
    message: String,
    val providerId: String,
    val isQuotaError: Boolean = false,
    val retryable: Boolean = true
) : Exception(message)

class QuotaExceededException(providerId: String) :
    ProviderException("انتهت الحصة اليومية", providerId, isQuotaError = true, retryable = false)

class RateLimitException(providerId: String) :
    ProviderException("تجاوز حد المعدل", providerId, isQuotaError = true, retryable = true)

class InvalidApiKeyException(providerId: String) :
    ProviderException("مفتاح API غير صالح", providerId, isQuotaError = false, retryable = false)

class ContentFilteredException(providerId: String) :
    ProviderException("تم حجب المحتوى بواسطة فلتر الأمان", providerId, isQuotaError = false, retryable = true)

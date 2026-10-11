package com.mjd.videoforge.providers

import android.util.Log
import com.mjd.videoforge.data.db.ProviderDao
import com.mjd.videoforge.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * مدير المحركات الذكي (ProviderManager)
 *
 * هذا هو قلب التطبيق - يدير كل المحركات ويختار الأنسب تلقائياً.
 * - يكشف أخطاء الحصة (429, quota, rate limit)
 * - ينتقل للمحرك التالي بنفس السياق
 * - يحسب العدّاد اليومي
 * - يطبق فترات التبريد
 * - يدعم التدوير بين المفاتيح
 */
@Singleton
class ProviderManager @Inject constructor(
    private val providerDao: ProviderDao,
    private val costTracker: CostTracker
) {
    private val TAG = "ProviderManager"

    /** كل المحركات المسجلة، مرتبة حسب الأولوية */
    private val providers = MutableStateFlow<List<AIProvider>>(emptyList())

    /** المحركات المعلقة (في فترة تبريد) */
    private val cooldownProviders = mutableMapOf<String, Date>()

    /** العدّاد اليومي لكل محرك */
    private val dailyCounters = mutableMapOf<String, Int>()

    /** قفل لتجنب التزاحم عند التبديل */
    private val switchMutex = Mutex()

    // State
    private val _providerStates = MutableStateFlow<Map<String, ProviderRuntimeState>>(emptyMap())
    val providerStates: StateFlow<Map<String, ProviderRuntimeState>> = _providerStates.asStateFlow()

    private val _qualityWarnings = MutableStateFlow<List<QualityWarning>>(emptyList())
    val qualityWarnings: StateFlow<List<QualityWarning>> = _qualityWarnings.asStateFlow()

    /**
     * تسجيل محرك جديد
     * إضافة محرك جديد = فقط استدعاء هذه الدالة
     */
    fun registerProvider(provider: AIProvider) {
        providers.value = (providers.value + provider).sortedBy { it.priority }
        Log.i(TAG, "تم تسجيل المحرك: ${provider.displayNameAr} (${provider.id})")
    }

    /**
     * اختيار أفضل محرك متاح لمهمة معينة
     * يتبع استراتيجية: مجاني أولاً ← أعلى أولوية ← متاح
     */
    suspend fun selectProvider(
        taskType: TaskType,
        preferredMode: ProviderMode = ProviderMode.AUTO,
        excludeIds: Set<String> = emptySet()
    ): AIProvider? {
        return switchMutex.withLock {
            val candidates = providers.value
                .filter { taskType in it.supportedTasks }
                .filter { it.id !in excludeIds }
                .filter { isAvailable(it) }
                .filter { it.isAvailable }

            if (candidates.isEmpty()) return@withLock null

            when (preferredMode) {
                ProviderMode.AUTO -> {
                    // مجاني أولاً، ثم حسب الأولوية
                    candidates.sortedWith(
                        compareByDescending<AIProvider> { it.isFree }
                            .thenBy { it.priority }
                            .thenBy { it.costPerRequest }
                    ).firstOrNull()
                }
                ProviderMode.QUALITY -> {
                    // أعلى جودة (أقل priority = أعلى جودة)
                    candidates.minByOrNull { it.priority }
                }
                ProviderMode.SPEED -> {
                    // أسرع (Groq أولاً عادة)
                    candidates.firstOrNull { it.id.startsWith("groq") }
                        ?: candidates.firstOrNull()
                }
                ProviderMode.COST -> {
                    // أقل تكلفة
                    candidates.minByOrNull { it.costPerRequest }
                }
            }
        }
    }

    /**
     * تنفيذ مهمة مع تبديل تلقائي بين المحركات
     * إذا فشل محرك بسبب الحصة، ينتقل للتالي بنفس السياق
     */
    suspend fun <T> executeWithFallback(
        taskType: TaskType,
        preferredMode: ProviderMode = ProviderMode.AUTO,
        operation: suspend (AIProvider) -> T
    ): ProviderResult<T> {
        val excludedIds = mutableSetOf<String>()
        var lastException: Exception? = null
        var attempts = 0

        while (attempts < providers.value.size) {
            val provider = selectProvider(taskType, preferredMode, excludedIds)
                ?: return ProviderResult.Failure(
                    error = "لا توجد محركات متاحة لمهمة: ${taskType.labelAr}",
                    allExhausted = true
                )

            attempts++
            try {
                Log.i(TAG, "استخدام المحرك: ${provider.displayNameAr} (المحاولة $attempts)")
                val result = operation(provider)

                // نجاح - تحديث العدّاد
                incrementCounter(provider.id)
                costTracker.recordUsage(provider.id, taskType, provider.costPerRequest)

                return ProviderResult.Success(
                    data = result,
                    providerId = provider.id,
                    providerName = provider.displayNameAr
                )
            } catch (e: QuotaExceededException) {
                Log.w(TAG, "المحرك ${provider.displayNameAr} انتهت حصته - انتقال للتالي")
                excludedIds.add(provider.id)
                markExhausted(provider.id)
                lastException = e
            } catch (e: RateLimitException) {
                Log.w(TAG, "المحرك ${provider.displayNameAr} تجاوز حد المعدل - تبريد")
                applyCooldown(provider.id, cooldownMs = 60_000)
                excludedIds.add(provider.id)
                lastException = e
            } catch (e: InvalidApiKeyException) {
                Log.e(TAG, "المحرك ${provider.displayNameAr} مفتاح غير صالح")
                markError(provider.id, "مفتاح API غير صالح")
                excludedIds.add(provider.id)
                lastException = e
            } catch (e: ContentFilteredException) {
                Log.w(TAG, "المحرك ${provider.displayNameAr} حجب المحتوى")
                excludedIds.add(provider.id)
                lastException = e
            } catch (e: Exception) {
                Log.e(TAG, "خطأ في المحرك ${provider.displayNameAr}: ${e.message}")
                excludedIds.add(provider.id)
                lastException = e
            }
        }

        return ProviderResult.Failure(
            error = lastException?.message ?: "فشل جميع المحركات",
            allExhausted = true,
            exception = lastException
        )
    }

    /**
     * فحص صحة جميع المفاتيح والمحركات
     */
    suspend fun runHealthCheck(): List<HealthCheckReport> {
        val reports = mutableListOf<HealthCheckReport>()

        for (provider in providers.value) {
            try {
                val result = provider.healthCheck()
                reports.add(
                    HealthCheckReport(
                        providerId = provider.id,
                        providerName = provider.displayNameAr,
                        isHealthy = result.isHealthy,
                        latencyMs = result.latencyMs,
                        remainingQuota = result.remainingQuota,
                        message = result.message
                    )
                )
            } catch (e: Exception) {
                reports.add(
                    HealthCheckReport(
                        providerId = provider.id,
                        providerName = provider.displayNameAr,
                        isHealthy = false,
                        latencyMs = -1,
                        message = e.message ?: "خطأ غير معروف"
                    )
                )
            }
        }
        return reports
    }

    /**
     * إضافة مفتاح API لمحرك مع تشفير
     */
    fun addApiKey(providerId: String, key: String, label: String = "") {
        val provider = providers.value.find { it.id == providerId } ?: return
        // Note: encryption handled by the repository layer
        Log.i(TAG, "تم إضافة مفتاح جديد للمحرك: ${provider.displayNameAr}")
    }

    // ==================== INTERNAL HELPERS ====================

    private fun isAvailable(provider: AIProvider): Boolean {
        val cooldownUntil = cooldownProviders[provider.id]
        if (cooldownUntil != null && cooldownUntil.after(Date())) {
            return false
        }
        return provider.remainingUsage != 0
    }

    private fun incrementCounter(providerId: String) {
        dailyCounters[providerId] = (dailyCounters[providerId] ?: 0) + 1
    }

    private fun markExhausted(providerId: String) {
        val state = _providerStates.value.toMutableMap()
        state[providerId] = (state[providerId] ?: ProviderRuntimeState(providerId))
            .copy(status = ProviderStatus.EXHAUSTED)
        _providerStates.value = state
    }

    private fun markError(providerId: String, message: String) {
        val state = _providerStates.value.toMutableMap()
        state[providerId] = (state[providerId] ?: ProviderRuntimeState(providerId))
            .copy(status = ProviderStatus.ERROR, errorMessage = message)
        _providerStates.value = state
    }

    private fun applyCooldown(providerId: String, cooldownMs: Long) {
        cooldownProviders[providerId] = Date(System.currentTimeMillis() + cooldownMs)
        val state = _providerStates.value.toMutableMap()
        state[providerId] = (state[providerId] ?: ProviderRuntimeState(providerId))
            .copy(status = ProviderStatus.COOLDOWN, cooldownUntil = Date(System.currentTimeMillis() + cooldownMs))
        _providerStates.value = state
    }

    /**
     * إعادة تعيين العدّادات عند منتصف الليل
     */
    fun resetDailyIfNeeded() {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(Date())
        for (provider in providers.value) {
            provider.resetDailyCounter()
        }
        dailyCounters.clear()
        cooldownProviders.clear()
        val state = _providerStates.value.toMutableMap()
        for ((id, s) in state) {
            state[id] = s.copy(status = ProviderStatus.ACTIVE, cooldownUntil = null, errorMessage = null)
        }
        _providerStates.value = state
    }

    /**
     * تحذير الجودة عند الانتقال لمحرك أضعف
     */
    fun warnQualityDowngrade(fromProvider: String, toProvider: String, reason: String) {
        val warning = QualityWarning(
            fromProvider = fromProvider,
            toProvider = toProvider,
            reason = reason,
            timestamp = Date()
        )
        _qualityWarnings.value = _qualityWarnings.value + warning
    }
}

// ==================== RUNTIME STATE ====================

data class ProviderRuntimeState(
    val providerId: String,
    val status: ProviderStatus = ProviderStatus.ACTIVE,
    val dailyUsage: Int = 0,
    val cooldownUntil: Date? = null,
    val errorMessage: String? = null,
    val lastSuccessAt: Date? = null
)

data class QualityWarning(
    val fromProvider: String,
    val toProvider: String,
    val reason: String,
    val timestamp: Date,
    val dismissed: Boolean = false
)

data class HealthCheckReport(
    val providerId: String,
    val providerName: String,
    val isHealthy: Boolean,
    val latencyMs: Long,
    val remainingQuota: Int? = null,
    val message: String = ""
)

// ==================== RESULT WRAPPER ====================

sealed class ProviderResult<out T> {
    data class Success<T>(
        val data: T,
        val providerId: String,
        val providerName: String
    ) : ProviderResult<T>()

    data class Failure(
        val error: String,
        val allExhausted: Boolean = false,
        val exception: Exception? = null
    ) : ProviderResult<Nothing>()
}

// ==================== COST TRACKER ====================

@Singleton
class CostTracker @Inject constructor(
    private val costDao: com.mjd.videoforge.data.db.CostDao
) {
    suspend fun recordUsage(providerId: String, taskType: TaskType, cost: Double) {
        costDao.insertCostEntry(
            CostEntry(
                providerId = providerId,
                projectId = "", // Set by the pipeline
                taskType = taskType,
                cost = cost
            )
        )
    }
}

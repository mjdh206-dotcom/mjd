package com.mjd.videoforge.data.repository

import com.mjd.videoforge.data.db.*
import com.mjd.videoforge.data.model.*
import com.mjd.videoforge.data.security.ApiKeyEncryption
import kotlinx.coroutines.flow.Flow
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * مستودع المشاريع
 */
@Singleton
class ProjectRepository @Inject constructor(
    private val projectDao: ProjectDao,
    private val sceneDao: SceneDao
) {
    fun getAllProjects(): Flow<List<Project>> = projectDao.getAllProjects()

    fun getDrafts(): Flow<List<Project>> = projectDao.getDrafts()

    suspend fun getProjectById(id: String): Project? = projectDao.getProjectById(id)

    suspend fun createProject(idea: String): Project {
        val project = Project(
            idea = idea,
            status = ProjectStatus.DRAFT,
            updatedAt = Date()
        )
        projectDao.insertProject(project)
        return project
    }

    suspend fun updateProject(project: Project) {
        projectDao.updateProject(project.copy(updatedAt = Date()))
    }

    suspend fun deleteProject(id: String) {
        sceneDao.deleteScenesForProject(id)
        projectDao.deleteProjectById(id)
    }

    fun getScenes(projectId: String): Flow<List<Scene>> = sceneDao.getScenesForProject(projectId)

    suspend fun saveScenes(scenes: List<Scene>) {
        sceneDao.insertScenes(scenes)
    }

    suspend fun updateScene(scene: Scene) {
        sceneDao.updateScene(scene)
    }
}

/**
 * مستودع إعدادات المحركات
 */
@Singleton
class ProviderRepository @Inject constructor(
    private val providerDao: ProviderDao,
    private val apiKeyEncryption: ApiKeyEncryption
) {
    fun getAllProviders(): Flow<List<ProviderConfig>> = providerDao.getAllProviders()

    suspend fun getProviderById(id: String): ProviderConfig? = providerDao.getProviderById(id)

    suspend fun saveProvider(config: ProviderConfig) {
        providerDao.insertProvider(config)
    }

    /**
     * إضافة مفتاح API مشفر لمحرك
     */
    suspend fun addApiKey(providerId: String, plainKey: String, label: String) {
        val provider = providerDao.getProviderById(providerId) ?: return
        val encryptedKey = apiKeyEncryption.encrypt(plainKey)
        val newKey = EncryptedApiKey(
            encryptedKey = encryptedKey,
            label = label
        )
        providerDao.updateProvider(
            provider.copy(
                apiKeys = provider.apiKeys + newKey,
                currentKeyIndex = provider.apiKeys.size
            )
        )
    }

    /**
     * حذف مفتاح API
     */
    suspend fun removeApiKey(providerId: String, keyIndex: Int) {
        val provider = providerDao.getProviderById(providerId) ?: return
        val newKeys = provider.apiKeys.toMutableList().also { it.removeAt(keyIndex) }
        providerDao.updateProvider(
            provider.copy(
                apiKeys = newKeys,
                currentKeyIndex = (provider.currentKeyIndex - 1).coerceAtLeast(0)
            )
        )
    }

    /**
     * الحصول على المفتاح الحالي المفكوك
     */
    suspend fun getCurrentKey(providerId: String): String? {
        val provider = providerDao.getProviderById(providerId) ?: return null
        if (provider.apiKeys.isEmpty()) return null
        return try {
            apiKeyEncryption.decrypt(provider.apiKeys[provider.currentKeyIndex].encryptedKey)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * تبديل للمفتاح التالي (تدوير المفاتيح)
     */
    suspend fun rotateKey(providerId: String) {
        val provider = providerDao.getProviderById(providerId) ?: return
        if (provider.apiKeys.size <= 1) return
        val nextIndex = (provider.currentKeyIndex + 1) % provider.apiKeys.size
        providerDao.updateProvider(provider.copy(currentKeyIndex = nextIndex))
    }

    suspend fun resetDailyUsage() {
        providerDao.resetDailyUsage()
    }

    suspend fun updateProviderStatus(id: String, status: ProviderStatus) {
        providerDao.updateStatus(id, status)
    }
}

/**
 * مستودع قائمة الانتظار
 */
@Singleton
class QueueRepository @Inject constructor(
    private val queueDao: QueueDao
) {
    fun getAllItems(): Flow<List<QueueItem>> = queueDao.getAllQueueItems()

    suspend fun getNextItem(): QueueItem? = queueDao.getNextQueueItem()

    suspend fun addItem(projectId: String) {
        val items = mutableListOf<QueueItem>()
        queueDao.getAllQueueItems().collect { list ->
            items.clear()
            items.addAll(list)
        }
        val nextPosition = items.maxOfOrNull { it.position }?.plus(1) ?: 0

        queueDao.insertQueueItem(
            QueueItem(
                projectId = projectId,
                position = nextPosition
            )
        )
    }

    suspend fun updateItem(item: QueueItem) {
        queueDao.updateQueueItem(item)
    }

    suspend fun clearCompleted() {
        queueDao.clearCompleted()
    }

    suspend fun clearAll() {
        queueDao.clearAll()
    }
}

/**
 * مستودع التكلفة
 */
@Singleton
class CostRepository @Inject constructor(
    private val costDao: CostDao
) {
    fun getAllCosts(): Flow<List<CostEntry>> = costDao.getAllCosts()

    fun getTodayCost(): Flow<Double?> = costDao.getTodayCost()

    fun getTotalCost(): Flow<Double?> = costDao.getTotalCost()

    fun getTodayUsageByProvider(): Flow<List<ProviderDailyUsage>> = costDao.getTodayUsageByProvider()

    suspend fun recordCost(projectId: String, providerId: String, taskType: TaskType, cost: Double) {
        costDao.insertCostEntry(
            CostEntry(
                projectId = projectId,
                providerId = providerId,
                taskType = taskType,
                cost = cost
            )
        )
    }
}

/**
 * مستودع القوالب الجاهزة
 */
@Singleton
class TemplateRepository @Inject constructor(
    private val templateDao: TemplateDao
) {
    fun getAllTemplates(): Flow<List<VideoTemplate>> = templateDao.getAllTemplates()

    fun getTemplatesByCategory(category: TemplateCategory): Flow<List<VideoTemplate>> =
        templateDao.getTemplatesByCategory(category)

    suspend fun getTemplateById(id: String): VideoTemplate? = templateDao.getTemplateById(id)

    suspend fun initializeBuiltInTemplates() {
        val templates = listOf(
            VideoTemplate(
                id = "science_facts",
                nameAr = "حقائق علمية",
                descriptionAr = "معلومات مذهلة عن الكون والطبيعة",
                category = TemplateCategory.SCIENCE,
                promptTemplate = "اكتب سكربت فيديو قصير عن أغرب الحقائق العلمية عن: {topic}"
            ),
            VideoTemplate(
                id = "history",
                nameAr = "تاريخ",
                descriptionAr = "قصص وأحداث تاريخية مشوقة",
                category = TemplateCategory.HISTORY,
                promptTemplate = "اكتب سكربت فيديو قصير يحكي قصة تاريخية عن: {topic}"
            ),
            VideoTemplate(
                id = "stories",
                nameAr = "قصص",
                descriptionAr = "قصص قصيرة مؤثرة",
                category = TemplateCategory.STORIES,
                promptTemplate = "اكتب سكربت فيديو قصير يحكي قصة عن: {topic}"
            ),
            VideoTemplate(
                id = "motivation",
                nameAr = "تحفيز",
                descriptionAr = "كلمات ملهمة ودروس حياة",
                category = TemplateCategory.MOTIVATION,
                promptTemplate = "اكتب سكربت فيديو تحفيزي عن: {topic}"
            ),
            VideoTemplate(
                id = "tech",
                nameAr = "تقنية",
                descriptionAr = "آخر الأخبار والاكتشافات التقنية",
                category = TemplateCategory.TECH,
                promptTemplate = "اكتب سكربت فيديو تقني مبسط عن: {topic}"
            ),
            VideoTemplate(
                id = "health",
                nameAr = "صحة",
                descriptionAr = "نصائح طبية ومعلومات صحية",
                category = TemplateCategory.HEALTH,
                promptTemplate = "اكتب سكربت فيديو صحي مبسط عن: {topic}"
            )
        )
        templates.forEach { templateDao.insertTemplate(it) }
    }
}

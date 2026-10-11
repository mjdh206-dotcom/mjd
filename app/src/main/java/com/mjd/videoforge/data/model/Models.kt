package com.mjd.videoforge.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.mjd.videoforge.data.db.Converters
import java.util.Date

// ==================== PROJECT ====================

@Entity(tableName = "projects")
@TypeConverters(Converters::class)
data class Project(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "",
    val idea: String = "",
    val dialect: Dialect = Dialect.FUSHA,
    val duration: VideoDuration = VideoDuration.SEC_30,
    val aspectRatio: AspectRatio = AspectRatio.VERTICAL,
    val imageStyle: ImageStyle = ImageStyle.CINEMATIC,
    val voiceProviderId: String = "",
    val voiceId: String = "",
    val voiceSpeed: Float = 1.0f,
    val includeMusic: Boolean = true,
    val includeSubtitles: Boolean = true,
    val loopMode: Boolean = false,
    val script: String = "",
    val generatedTitle: String = "",
    val generatedDescription: String = "",
    val generatedHashtags: List<String> = emptyList(),
    val sources: List<String> = emptyList(),
    val outputVideoPath: String? = null,
    val thumbnailPath: String? = null,
    val status: ProjectStatus = ProjectStatus.DRAFT,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date(),
    val completedAt: Date? = null,
    val estimatedCost: Double = 0.0,
    val actualCost: Double = 0.0
)

enum class ProjectStatus {
    DRAFT,
    QUESTIONING,
    SCRIPT_GENERATED,
    STORYBOARD_READY,
    GENERATING,
    RENDERING,
    EXPORTING,
    COMPLETED,
    FAILED,
    PAUSED
}

enum class VideoDuration(val seconds: Int, val labelAr: String) {
    SEC_15(15, "١٥ ثانية"),
    SEC_25(25, "٢٥ ثانية"),
    SEC_45(45, "٤٥ ثانية"),
    SEC_60(60, "٦٠ ثانية")
}

enum class AspectRatio(val ratio: Float, val width: Int, val height: Int, val labelAr: String) {
    VERTICAL(9f / 16f, 1080, 1920, "عمودي 9:16"),
    HORIZONTAL(16f / 9f, 1920, 1080, "أفقي 16:9")
}

enum class Dialect(val code: String, val labelAr: String) {
    FUSHA("ar-MSA", "فصحى"),
    SHAMI("ar-SY", "شامي"),
    EGYPTIAN("ar-EG", "مصري"),
    GULF("ar-SA", "خليجي"),
    MAGHREB("ar-MA", "مغربي"),
    WHITE("ar-WH", "بيضاء")
}

enum class ImageStyle(val code: String, val labelAr: String, val promptSuffix: String) {
    CINEMATIC("cinematic", "سينمائي واقعي", "cinematic lighting, photorealistic, 8k, film grain"),
    ANIME("anime", "أنمي", "anime style, Studio Ghibli inspired, vibrant colors"),
    CARTOON("cartoon", "رسوم متحركة", "cartoon style, colorful, clean lines"),
    DOCUMENTARY("documentary", "وثائقي", "documentary photography, natural lighting, editorial"),
    THREE_D("3d", "ثلاثي الأبعاد", "3D rendered, octane render, subsurface scattering"),
    WATERCOLOR("watercolor", "ألوان مائية", "watercolor painting, soft edges, artistic")
}

// ==================== SCENE / STORYBOARD ====================

@Entity(tableName = "scenes")
data class Scene(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val projectId: String,
    val sceneNumber: Int,
    val visualDescription: String = "",
    val visualDescriptionAr: String = "",
    val narrationText: String = "",
    val narrationTextAr: String = "",
    val generatedImagePath: String? = null,
    val generatedAudioPath: String? = null,
    val srtPath: String? = null,
    val duration: Float = 5.0f,
    val cameraMovement: CameraMovement = CameraMovement.STATIC,
    val transition: Transition = Transition.CUT,
    val isGenerated: Boolean = false,
    val order: Int = 0,
    val promptUsed: String? = null,
    val providerUsed: String? = null,
    val cost: Double = 0.0
)

enum class CameraMovement(val labelAr: String) {
    STATIC("ثابت"),
    ZOOM_IN("تقريب"),
    ZOOM_OUT("إبعاد"),
    PAN_LEFT("تحريك يسار"),
    PAN_RIGHT("تحريك يمين"),
    KEN_BURNS("كين بيرنز"),
    DOLLY_IN("دوليه تقريب"),
    DRONE_UP("ارتفاع درون")
}

enum class Transition(val labelAr: String) {
    CUT("قطع مباشر"),
    FADE("تلاشي"),
    DISSOLVE("ذوبان"),
    WIPE("مسح"),
    SLIDE("انزلاق"),
    ZOOM("تكبير")
}

// ==================== AI PROVIDERS ====================

@Entity(tableName = "provider_configs")
@TypeConverters(Converters::class)
data class ProviderConfig(
    @PrimaryKey
    val id: String,
    val name: String,
    val displayNameAr: String,
    val type: ProviderType,
    val supportedTasks: List<TaskType>,
    val priority: Int = 0,
    val isFreeFirst: Boolean = true,
    val isEnabled: Boolean = true,
    val apiKeys: List<EncryptedApiKey> = emptyList(),
    val currentKeyIndex: Int = 0,
    val dailyUsage: Int = 0,
    val dailyLimit: Int = -1, // -1 = unlimited
    val lastUsageDate: String = "", // yyyy-MM-dd
    val status: ProviderStatus = ProviderStatus.ACTIVE,
    val cooldownUntil: Date? = null,
    val errorMessage: String? = null,
    val lastHealthCheck: Date? = null,
    val healthCheckResult: HealthCheckResult? = null,
    val costPerRequest: Double = 0.0,
    val maxTokensPerRequest: Int = 4096
)

enum class ProviderType(val labelAr: String) {
    GEMINI("Gemini"),
    CLAUDE("Claude"),
    GPT("OpenAI GPT"),
    DEEPSEEK("DeepSeek"),
    MISTRAL("Mistral"),
    GROQ("Groq"),
    GROK("Grok (xAI)"),
    ELEVENLABS("ElevenLabs"),
    OPENAI_TTS("OpenAI TTS"),
    GOOGLE_TTS("Google TTS"),
    AZURE_TTS("Azure Speech"),
    EDGE_TTS("Edge TTS"),
    DALLE("DALL-E"),
    STABLE_DIFFUSION("Stable Diffusion"),
    FLUX("Flux"),
    RUNWAY("Runway"),
    SUNO("Suno AI"),
    MUSICGEN("MusicGen")
}

enum class TaskType(val labelAr: String) {
    SCRIPT("توليد السكربت"),
    IMAGE("توليد الصور"),
    VIDEO("توليد الفيديو"),
    AUDIO("التعليق الصوتي"),
    MUSIC("الموسيقى"),
    TRANSLATION("الترجمة"),
    TITLE("العناوين والوصف")
}

enum class ProviderStatus {
    ACTIVE,
    EXHAUSTED,
    ERROR,
    COOLDOWN,
    HEALTH_CHECK_RUNNING
}

enum class HealthCheckResult {
    SUCCESS,
    FAILURE,
    RATE_LIMITED,
    INVALID_KEY,
    TIMEOUT
}

data class EncryptedApiKey(
    val encryptedKey: String,
    val label: String = "",
    val addedAt: Date = Date(),
    val usageCount: Int = 0
)

// ==================== QUEUE ====================

@Entity(tableName = "queue_items")
@TypeConverters(Converters::class)
data class QueueItem(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val projectId: String,
    val position: Int = 0,
    val status: QueueStatus = QueueStatus.WAITING,
    val progress: Float = 0.0f,
    val currentStep: String = "",
    val startedAt: Date? = null,
    val completedAt: Date? = null,
    val errorMessage: String? = null,
    val retryCount: Int = 0
)

enum class QueueStatus {
    WAITING,
    PROCESSING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

// ==================== BRAND KIT ====================

@Entity(tableName = "brand_kits")
data class BrandKit(
    @PrimaryKey
    val id: String = "default",
    val channelName: String = "",
    val primaryColor: String = "#1B6B93",
    val secondaryColor: String = "#FC9D4A",
    val fontFamily: String = "default",
    val logoPath: String? = null,
    val watermarkEnabled: Boolean = false,
    val watermarkPosition: WatermarkPosition = WatermarkPosition.BOTTOM_RIGHT,
    val defaultVoiceProvider: String = "",
    val defaultVoiceId: String = "",
    val defaultStyle: ImageStyle = ImageStyle.CINEMATIC,
    val defaultDialect: Dialect = Dialect.FUSHA
)

enum class WatermarkPosition {
    TOP_RIGHT, TOP_LEFT, BOTTOM_RIGHT, BOTTOM_LEFT, CENTER
}

// ==================== SETTINGS ====================

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey
    val id: String = "main",
    val darkModeEnabled: Boolean = false,
    val autoSave: Boolean = true,
    val dataSaverMode: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val qualityWarningEnabled: Boolean = true,
    val autoBackupEnabled: Boolean = false,
    val backupDriveFolderId: String? = null,
    val defaultExportQuality: Int = 1080,
    val defaultExportFormat: String = "mp4",
    val preferredProviderMode: ProviderMode = ProviderMode.AUTO,
    val freeFirstEnabled: Boolean = true
)

enum class ProviderMode(val labelAr: String) {
    AUTO("تلقائي (الأفضل)"),
    QUALITY("أعلى جودة"),
    SPEED("أسرع"),
    COST("أقل تكلفة")
}

// ==================== COST TRACKING ====================

@Entity(tableName = "cost_entries")
data class CostEntry(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val projectId: String,
    val providerId: String,
    val taskType: TaskType,
    val cost: Double,
    val timestamp: Date = Date(),
    val description: String = ""
)

// ==================== TEMPLATES ====================

@Entity(tableName = "templates")
@TypeConverters(Converters::class)
data class VideoTemplate(
    @PrimaryKey
    val id: String,
    val nameAr: String,
    val descriptionAr: String,
    val category: TemplateCategory,
    val defaultDuration: VideoDuration = VideoDuration.SEC_30,
    val defaultStyle: ImageStyle = ImageStyle.CINEMATIC,
    val promptTemplate: String,
    val sampleScenes: List<Scene> = emptyList(),
    val isBuiltIn: Boolean = true
)

enum class TemplateCategory(val labelAr: String) {
    SCIENCE("حقائق علمية"),
    HISTORY("تاريخ"),
    STORIES("قصص"),
    MOTIVATION("تحفيز"),
    TECH("تقنية"),
    HEALTH("صحة"),
    CUSTOM("مخصص")
}

// ==================== ASSET LIBRARY ====================

@Entity(tableName = "assets")
data class Asset(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val type: AssetType,
    val filePath: String,
    val category: String = "",
    val tags: List<String> = emptyList(),
    val createdAt: Date = Date(),
    val usageCount: Int = 0
)

enum class AssetType {
    IMAGE, AUDIO, MUSIC, VIDEO, FONT, OVERLAY
}

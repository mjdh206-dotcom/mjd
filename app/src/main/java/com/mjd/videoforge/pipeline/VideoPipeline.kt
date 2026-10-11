package com.mjd.videoforge.pipeline

import android.content.Context
import android.util.Log
import com.mjd.videoforge.data.db.SceneDao
import com.mjd.videoforge.data.model.*
import com.mjd.videoforge.providers.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * مسار إنتاج الفيديو الكامل
 *
 * الخطوات:
 * 1. توليد السكربت (هوك + مشاهد + خاتمة)
 * 2. بناء لوحة القصة (Storyboard)
 * 3. توليد الصور لكل مشهد
 * 4. توليد التعليق الصوتي
 * 5. توليد الموسيقى (اختياري)
 * 6. المونتاج التلقائي (Ken Burns + انتقالات + مزامنة)
 * 7. التصدير (MP4 + عنوان + وصف + هاشتاغات)
 */
@Singleton
class VideoPipeline @Inject constructor(
    @ApplicationContext private val context: Context,
    private val providerManager: ProviderManager,
    private val sceneDao: SceneDao,
    private val scriptGenerator: ScriptGenerator,
    private val montageEngine: MontageEngine,
    private val exportManager: ExportManager
) {
    private val TAG = "VideoPipeline"

    // Progress tracking
    private val _progress = MutableStateFlow<PipelineProgress>(PipelineProgress.Idle)
    val progress: StateFlow<PipelineProgress> = _progress.asStateFlow()

    private var _currentProject: Project? = null
    val currentProject: Project? get() = _currentProject

    /**
     * تشغيل مسار الإنتاج الكامل
     */
    suspend fun produce(project: Project): PipelineResult {
        _currentProject = project
        Log.i(TAG, "بدء إنتاج الفيديو: ${project.title}")

        return try {
            // Step 1: Generate Script
            _progress.value = PipelineProgress.Step(
                step = PipelineStep.GENERATING_SCRIPT,
                progress = 0.0f,
                message = "جاري توليد السكربت..."
            )
            val scriptResult = scriptGenerator.generate(project)
            when (scriptResult) {
                is ProviderResult.Success -> {
                    project.copy(
                        script = scriptResult.data.script,
                        generatedTitle = scriptResult.data.title,
                        generatedDescription = scriptResult.data.description,
                        generatedHashtags = scriptResult.data.hashtags,
                        sources = scriptResult.data.sources
                    ).also { _currentProject = it }

                    // Step 2: Build Storyboard
                    _progress.value = PipelineProgress.Step(
                        step = PipelineStep.BUILDING_STORYBOARD,
                        progress = 0.15f,
                        message = "جاري بناء لوحة القصة..."
                    )
                    val scenes = scriptResult.data.scenes.mapIndexed { index, sceneData ->
                        Scene(
                            projectId = project.id,
                            sceneNumber = index + 1,
                            visualDescription = sceneData.visualPrompt,
                            visualDescriptionAr = sceneData.visualDescriptionAr,
                            narrationText = sceneData.narration,
                            narrationTextAr = sceneData.narrationAr,
                            duration = sceneData.duration,
                            cameraMovement = sceneData.cameraMovement,
                            transition = sceneData.transition,
                            order = index
                        )
                    }
                    sceneDao.insertScenes(scenes)

                    // Step 3: Generate Images
                    _progress.value = PipelineProgress.Step(
                        step = PipelineStep.GENERATING_IMAGES,
                        progress = 0.25f,
                        message = "جاري توليد الصور..."
                    )
                    val generatedScenes = generateImages(project, scenes)

                    // Step 4: Generate Audio
                    _progress.value = PipelineProgress.Step(
                        step = PipelineStep.GENERATING_AUDIO,
                        progress = 0.55f,
                        message = "جاري توليد التعليق الصوتي..."
                    )
                    if (project.voiceProviderId.isNotEmpty()) {
                        val audioScenes = generateAudio(project, generatedScenes)

                        // Step 5: Generate Music (optional)
                        var finalScenes = audioScenes
                        if (project.includeMusic) {
                            _progress.value = PipelineProgress.Step(
                                step = PipelineStep.GENERATING_MUSIC,
                                progress = 0.70f,
                                message = "جاري توليد الموسيقى..."
                            )
                            finalScenes = generateMusic(project, audioScenes)
                        }

                        // Step 6: Montage
                        _progress.value = PipelineProgress.Step(
                            step = PipelineStep.MONTAGE,
                            progress = 0.80f,
                            message = "جاري المونتاج..."
                        )
                        val videoPath = montageEngine.assemble(project, finalScenes)

                        // Step 7: Export
                        _progress.value = PipelineProgress.Step(
                            step = PipelineStep.EXPORTING,
                            progress = 0.92f,
                            message = "جاري التصدير..."
                        )
                        val exportedPath = exportManager.export(
                            project = project.copy(
                                outputVideoPath = videoPath,
                                status = ProjectStatus.COMPLETED
                            )
                        )

                        _progress.value = PipelineProgress.Complete(
                            outputPath = exportedPath,
                            title = _currentProject?.generatedTitle ?: project.title,
                            description = _currentProject?.generatedDescription ?: "",
                            hashtags = _currentProject?.generatedHashtags ?: emptyList(),
                            sources = _currentProject?.sources ?: emptyList()
                        )

                        PipelineResult.Success(
                            videoPath = exportedPath,
                            title = _currentProject?.generatedTitle ?: "",
                            description = _currentProject?.generatedDescription ?: "",
                            hashtags = _currentProject?.generatedHashtags ?: emptyList()
                        )
                    } else {
                        // No voice narration - just montage with music/text
                        _progress.value = PipelineProgress.Step(
                            step = PipelineStep.MONTAGE,
                            progress = 0.70f,
                            message = "جاري المونتاج بدون تعليق صوتي..."
                        )
                        val videoPath = montageEngine.assemble(project, generatedScenes)
                        val exportedPath = exportManager.export(
                            project.copy(outputVideoPath = videoPath)
                        )

                        _progress.value = PipelineProgress.Complete(exportedPath, "", "", emptyList(), emptyList())
                        PipelineResult.Success(exportedPath, "", "", emptyList())
                    }
                }
                is ProviderResult.Failure -> {
                    _progress.value = PipelineProgress.Failed(scriptResult.error)
                    PipelineResult.Failed(scriptResult.error)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "فشل الإنتاج: ${e.message}", e)
            _progress.value = PipelineProgress.Failed(e.message ?: "خطأ غير معروف")
            PipelineResult.Failed(e.message ?: "خطأ غير معروف")
        }
    }

    /**
     * توليد صور لكل مشهد مع تبديل تلقائي بين محركات الصور
     */
    private suspend fun generateImages(project: Project, scenes: List<Scene>): List<Scene> {
        val results = mutableListOf<Scene>()
        val styleSuffix = project.imageStyle.promptSuffix

        for ((index, scene) in scenes.withIndex()) {
            val progress = 0.25f + (0.30f * (index.toFloat() / scenes.size))
            _progress.value = PipelineProgress.Step(
                step = PipelineStep.GENERATING_IMAGES,
                progress = progress,
                message = "توليد صورة المشهد ${index + 1} من ${scenes.size}..."
            )

            val prompt = "${scene.visualDescription}, $styleSuffix"

            val result = providerManager.executeWithFallback(TaskType.IMAGE) { provider ->
                provider.generateImage(ImageRequest(
                    prompt = prompt,
                    width = project.aspectRatio.width,
                    height = project.aspectRatio.height
                ))
            }

            when (result) {
                is ProviderResult.Success -> {
                    results.add(scene.copy(
                        generatedImagePath = result.data.imagePath,
                        isGenerated = true,
                        promptUsed = prompt,
                        providerUsed = result.providerId
                    ))
                }
                is ProviderResult.Failure -> {
                    Log.w(TAG, "فشل توليد صورة المشهد ${index + 1}: ${result.error}")
                    results.add(scene) // Keep without image
                }
            }
        }
        return results
    }

    /**
     * توليد تعليق صوتي لكل مشهد
     */
    private suspend fun generateAudio(project: Project, scenes: List<Scene>): List<Scene> {
        val results = mutableListOf<Scene>()

        for ((index, scene) in scenes.withIndex()) {
            if (scene.narrationText.isEmpty()) {
                results.add(scene)
                continue
            }

            val progress = 0.55f + (0.15f * (index.toFloat() / scenes.size))
            _progress.value = PipelineProgress.Step(
                step = PipelineStep.GENERATING_AUDIO,
                progress = progress,
                message = "التعليق الصوتي للمشهد ${index + 1}..."
            )

            val result = providerManager.executeWithFallback(TaskType.AUDIO) { provider ->
                provider.generateAudio(AudioRequest(
                    text = scene.narrationText,
                    voiceId = project.voiceId,
                    speed = project.voiceSpeed,
                    language = project.dialect.code
                ))
            }

            when (result) {
                is ProviderResult.Success -> {
                    results.add(scene.copy(
                        generatedAudioPath = result.data.audioPath,
                        srtPath = result.data.srtPath
                    ))
                }
                is ProviderResult.Failure -> {
                    Log.w(TAG, "فشل التعليق الصوتي للمشهد ${index + 1}: ${result.error}")
                    results.add(scene)
                }
            }
        }
        return results
    }

    /**
     * توليد موسيقى خلفية
     */
    private suspend fun generateMusic(project: Project, scenes: List<Scene>): List<Scene> {
        val totalDuration = scenes.sumOf { it.duration.toInt() }

        val result = providerManager.executeWithFallback(TaskType.MUSIC) { provider ->
            provider.generateMusic(MusicRequest(
                prompt = "خلفية موسيقية ${project.imageStyle.labelAr} لفيديو ${project.title}",
                duration = totalDuration,
                mood = "مysterious"
            ))
        }

        // Music is applied during montage, not per-scene
        return scenes
    }

    /**
     * إعادة توليد مشهد واحد فقط
     */
    suspend fun regenerateScene(project: Project, sceneId: String, regenerateType: RegenerateType): Scene? {
        val scenes = sceneDao.getScenesForProjectOnce(project.id)
        val scene = scenes.find { it.id == sceneId } ?: return null

        return when (regenerateType) {
            RegenerateType.IMAGE -> {
                val result = providerManager.executeWithFallback(TaskType.IMAGE) { provider ->
                    provider.generateImage(ImageRequest(
                        prompt = "${scene.visualDescription}, ${project.imageStyle.promptSuffix}",
                        width = project.aspectRatio.width,
                        height = project.aspectRatio.height
                    ))
                }
                when (result) {
                    is ProviderResult.Success -> scene.copy(generatedImagePath = result.data.imagePath)
                    is ProviderResult.Failure -> null
                }
            }
            RegenerateType.NARRATION -> {
                val result = providerManager.executeWithFallback(TaskType.AUDIO) { provider ->
                    provider.generateAudio(AudioRequest(
                        text = scene.narrationText,
                        voiceId = project.voiceId,
                        speed = project.voiceSpeed
                    ))
                }
                when (result) {
                    is ProviderResult.Success -> scene.copy(generatedAudioPath = result.data.audioPath)
                    is ProviderResult.Failure -> null
                }
            }
            RegenerateType.SCRIPT -> {
                val result = providerManager.executeWithFallback(TaskType.SCRIPT) { provider ->
                    provider.generateText(TextRequest(
                        prompt = "أعد كتابة هذا المشهد بشكل مختلف: ${scene.visualDescriptionAr}",
                        systemPrompt = scriptGenerator.getSystemPrompt(project)
                    ))
                }
                when (result) {
                    is ProviderResult.Success -> scene.copy(narrationText = result.data.content)
                    is ProviderResult.Failure -> null
                }
            }
        }
    }

    fun cancel() {
        _progress.value = PipelineProgress.Idle
    }
}

enum class PipelineStep {
    GENERATING_SCRIPT,
    BUILDING_STORYBOARD,
    GENERATING_IMAGES,
    GENERATING_AUDIO,
    GENERATING_MUSIC,
    MONTAGE,
    EXPORTING
}

sealed class PipelineProgress {
    data object Idle : PipelineProgress()
    data class Step(
        val step: PipelineStep,
        val progress: Float,
        val message: String
    ) : PipelineProgress()
    data class Complete(
        val outputPath: String,
        val title: String,
        val description: String,
        val hashtags: List<String>,
        val sources: List<String>
    ) : PipelineProgress()
    data class Failed(val error: String) : PipelineProgress()
}

sealed class PipelineResult {
    data class Success(
        val videoPath: String,
        val title: String,
        val description: String,
        val hashtags: List<String>
    ) : PipelineResult()
    data class Failed(val error: String) : PipelineResult()
}

enum class RegenerateType { IMAGE, NARRATION, SCRIPT }

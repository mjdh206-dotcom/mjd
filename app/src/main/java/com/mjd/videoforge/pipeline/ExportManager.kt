package com.mjd.videoforge.pipeline

import android.content.Context
import android.util.Log
import com.mjd.videoforge.data.model.Project
import com.mjd.videoforge.providers.ProviderManager
import com.mjd.videoforge.providers.TaskType
import com.mjd.videoforge.providers.TextRequest
import com.mjd.videoforge.providers.ProviderResult
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * مدير التصدير
 *
 * يصدّر الفيديو النهائي مع:
 * - العنوان والوصف والهاشتاغات
 * - الصورة المصغرة (Thumbnail)
 * - ملف SRT للترجمة
 * - مشاركة مباشرة لمنصات التواصل
 */
@Singleton
class ExportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val providerManager: ProviderManager
) {
    private val TAG = "ExportManager"

    data class ExportResult(
        val videoPath: String,
        val thumbnailPath: String?,
        val srtPath: String?,
        val metadataFile: String?
    )

    /**
     * تصدير الفيديو
     */
    suspend fun export(project: Project): String {
        val outputDir = File(context.getExternalFilesDir(null), "exports/${project.id}")
        outputDir.mkdirs()

        val videoFile = project.outputVideoPath?.let { File(it) }
            ?: throw IllegalArgumentException("لا يوجد فيديو للتصدير")

        // Copy to export directory
        val exportedVideo = File(outputDir, "${sanitizeFilename(project.title)}.mp4")
        if (videoFile.exists()) {
            videoFile.copyTo(exportedVideo, overwrite = true)
        }

        // Generate thumbnail if needed
        val thumbnail = generateThumbnail(project, outputDir)

        // Generate SRT if needed
        val srt = generateSrt(project, outputDir)

        // Save metadata
        val metadata = saveMetadata(project, outputDir)

        Log.i(TAG, "تم التصدير: ${exportedVideo.absolutePath}")
        return exportedVideo.absolutePath
    }

    /**
     * توليد صورة مصغرة من أول مشهد
     */
    private fun generateThumbnail(project: Project, outputDir: File): String? {
        // Extract first frame from video or use first scene image
        val thumbnailFile = File(outputDir, "thumbnail.jpg")
        // In real implementation: use MediaMetadataRetriever to extract frame
        return if (thumbnailFile.exists()) thumbnailFile.absolutePath else null
    }

    /**
     * توليد ملف ترجمة SRT
     */
    private fun generateSrt(project: Project, outputDir: File): String? {
        val srtFile = File(outputDir, "subtitles.srt")
        // In real implementation: combine scene narrations with timestamps
        return if (srtFile.exists()) srtFile.absolutePath else null
    }

    /**
     * حفظ البيانات الوصفية
     */
    private fun saveMetadata(project: Project, outputDir: File): String {
        val metadataFile = File(outputDir, "metadata.json")
        val json = """
        {
            "title": "${project.generatedTitle}",
            "description": "${project.generatedDescription}",
            "hashtags": [${project.generatedHashtags.joinToString(",") { "\"$it\"" }}],
            "duration": ${project.duration.seconds},
            "aspectRatio": "${project.aspectRatio.name}",
            "style": "${project.imageStyle.labelAr}",
            "dialect": "${project.dialect.labelAr}",
            "createdAt": "${project.createdAt}",
            "cost": ${project.actualCost}
        }
        """.trimIndent()
        metadataFile.writeText(json)
        return metadataFile.absolutePath
    }

    /**
     * توليد أفكار عناوين بديلة (A/B Testing)
     */
    suspend fun generateTitleAlternatives(project: Project): List<String> {
        val result = providerManager.executeWithFallback(TaskType.TITLE) { provider ->
            provider.generateText(TextRequest(
                prompt = """
أعطني 5 عناوين بديلة جذابة لفيديو قصير عن: ${project.idea}
العنوان الحالي: ${project.generatedTitle}
يجب أن تكون العناوين:
- مشوقة وتثير الفضول
- مناسبة لليوتيوب شورتس وتيك توك
- بال${project.dialect.labelAr}
أعطِ العناوين فقط، كل واحد في سطر.
                """.trimIndent(),
                maxTokens = 500
            ))
        }

        return when (result) {
            is ProviderResult.Success -> {
                result.data.content.lines()
                    .filter { it.isNotBlank() }
                    .map { it.trim().removePrefix("-").trim().removePrefix("•").trim() }
                    .take(5)
            }
            is ProviderResult.Failure -> emptyList()
        }
    }

    private fun sanitizeFilename(name: String): String {
        return name.replace(Regex("[^a-zA-Z0-9\\u0600-\\u06FF\\s]"), "")
            .replace(Regex("\\s+"), "_")
            .take(50)
            .ifEmpty { "video_${System.currentTimeMillis()}" }
    }
}

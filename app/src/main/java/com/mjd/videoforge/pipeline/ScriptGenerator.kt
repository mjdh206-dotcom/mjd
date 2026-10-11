package com.mjd.videoforge.pipeline

import android.util.Log
import com.mjd.videoforge.data.model.*
import com.mjd.videoforge.providers.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * مولّد السكربت الذكي
 *
 * ينتج سكربت متكامل يشمل:
 * - هوك (أول ثانيتين) لجذب الانتباه
 * - تسلسل مشاهد منطقي
 * - جمل ختامية قوية
 * - عنوان ووصف محسّن للسيو
 * - هاشتاغات مناسبة
 */
@Singleton
class ScriptGenerator @Inject constructor(
    private val providerManager: ProviderManager
) {
    private val TAG = "ScriptGenerator"

    data class ScriptOutput(
        val script: String,
        val title: String,
        val description: String,
        val hashtags: List<String>,
        val sources: List<String>,
        val scenes: List<SceneData>
    )

    data class SceneData(
        val visualPrompt: String,
        val visualDescriptionAr: String,
        val narration: String,
        val narrationAr: String,
        val duration: Float,
        val cameraMovement: CameraMovement = CameraMovement.KEN_BURNS,
        val transition: Transition = Transition.DISSOLVE
    )

    /**
     * توليد سكربت كامل من فكرة
     */
    suspend fun generate(project: Project): ProviderResult<ScriptOutput> {
        return providerManager.executeWithFallback(TaskType.SCRIPT) { provider ->
            val systemPrompt = getSystemPrompt(project)
            val userPrompt = buildUserPrompt(project)

            val response = provider.generateText(TextRequest(
                prompt = userPrompt,
                systemPrompt = systemPrompt,
                maxTokens = 4096,
                temperature = 0.8f
            ))

            parseScriptOutput(response.content, project)
        }
    }

    /**
     * بناء الـ System Prompt بناءً على إعدادات المشروع
     */
    fun getSystemPrompt(project: Project): String {
        return """
أنت كاتب سكربتات فيديو قصيرة محترف للوسائل العربية. مهمتك كتابة سكربت جذاب ومشوق.

المواصفات المطلوبة:
- اللهجة: ${project.dialect.labelAr}
- مدة الفيديو: ${project.duration.seconds} ثانية
- أسلوب الصور: ${project.imageStyle.labelAr}
- نسبة العرض: ${project.aspectRatio.labelAr}

قواعد الكتابة:
1. ابدأ بـ "هوك" قوي يجذب الانتباه في أول ثانيتين
2. كل مشهد يجب أن يكون له وصف بصري دقيق (بالإنجليزية) ونص روائي (بالعربية)
3. التسلسل يجب أن يكون منطقياً ومتصاعداً
4. أنهِ بجملة ختامية قوية أو سؤال محفّز
5. أضف عنوان جذاب ووصف محسّن للسيو
6. أضف 10-15 هاشتاغ مناسبة
7. إذا كانت المعلومات تحتاج مصادر، أضفها

أجب دائماً بصيغة JSON بالم siguiente الشكل:
{
  "title": "عنوان جذاب",
  "description": "وصف محسّن للسيو",
  "hashtags": ["هاشتاغ1", "هاشتاغ2", ...],
  "sources": ["مصدر1", ...],
  "scenes": [
    {
      "visualPrompt": "Detailed English visual description for AI image generation",
      "visualDescriptionAr": "وصف المشهد بالعربية",
      "narration": "النص الروائي بالإنجليزية (لترجمته)",
      "narrationAr": "النص الروائي بالعربية باللهجة المطلوبة",
      "duration": 5.0,
      "cameraMovement": "KEN_BURNS",
      "transition": "DISSOLVE"
    }
  ]
}
        """.trimIndent()
    }

    private fun buildUserPrompt(project: Project): String {
        val sceneCount = when (project.duration) {
            VideoDuration.SEC_15 -> 3
            VideoDuration.SEC_25 -> 4
            VideoDuration.SEC_45 -> 6
            VideoDuration.SEC_60 -> 8
        }

        val sceneDuration = project.duration.seconds.toFloat() / sceneCount

        return """
اكتب سكربت فيديو قصير عن الموضوع التالي:
"${project.idea}"

عدد المشاهد: $sceneCount
مدة كل مشهد تقريباً: ${sceneDuration.toInt()} ثانية

المتطلبات:
- ابدأ بهوك قوي جداً (أول مشهد)
- كل مشهد يجب أن يبني على السابق
- انهِ بسؤال أو دعوة للتفاعل
- تأكد من دقة المعلومات
- لا تكرر نفس الفكرة في مشهدين متتاليين
        """.trimIndent()
    }

    /**
     * تحليل الاستجابة وتحويلها لـ ScriptOutput
     */
    private fun parseScriptOutput(response: String, project: Project): ScriptOutput {
        // Try to parse as JSON
        return try {
            val json = com.google.gson.JsonParser.parseString(cleanJsonResponse(response)).asJsonObject

            val title = json.get("title")?.asString ?: project.title
            val description = json.get("description")?.asString ?: ""
            val hashtags = json.getAsJsonArray("hashtags")?.map { it.asString } ?: emptyList()
            val sources = json.getAsJsonArray("sources")?.map { it.asString } ?: emptyList()

            val scenes = json.getAsJsonArray("scenes")?.map { sceneObj ->
                val scene = sceneObj.asJsonObject
                SceneData(
                    visualPrompt = scene.get("visualPrompt")?.asString ?: "",
                    visualDescriptionAr = scene.get("visualDescriptionAr")?.asString ?: "",
                    narration = scene.get("narration")?.asString ?: "",
                    narrationAr = scene.get("narrationAr")?.asString ?: "",
                    duration = scene.get("duration")?.asFloat ?: 5.0f,
                    cameraMovement = try {
                        CameraMovement.valueOf(scene.get("cameraMovement")?.asString ?: "KEN_BURNS")
                    } catch (e: Exception) { CameraMovement.KEN_BURNS },
                    transition = try {
                        Transition.valueOf(scene.get("transition")?.asString ?: "DISSOLVE")
                    } catch (e: Exception) { Transition.DISSOLVE }
                )
            } ?: emptyList()

            ScriptOutput(
                script = response,
                title = title,
                description = description,
                hashtags = hashtags,
                sources = sources,
                scenes = scenes
            )
        } catch (e: Exception) {
            Log.w(TAG, "فشل تحليل JSON - استخدام النص الخام: ${e.message}")
            // Fallback: treat the whole response as a script with auto-generated scenes
            ScriptOutput(
                script = response,
                title = project.idea.take(50),
                description = response.take(200),
                hashtags = listOf("#فيديو", "#محتوى", "#تعلم"),
                sources = emptyList(),
                scenes = listOf(
                    SceneData(
                        visualPrompt = project.idea,
                        visualDescriptionAr = project.idea,
                        narration = response.take(100),
                        narrationAr = response.take(100),
                        duration = project.duration.seconds.toFloat()
                    )
                )
            )
        }
    }

    private fun cleanJsonResponse(response: String): String {
        // Remove markdown code blocks if present
        var clean = response.trim()
        if (clean.startsWith("```json")) {
            clean = clean.removePrefix("```json").trim()
        }
        if (clean.startsWith("```")) {
            clean = clean.removePrefix("```").trim()
        }
        if (clean.endsWith("```")) {
            clean = clean.removeSuffix("```").trim()
        }
        return clean
    }
}

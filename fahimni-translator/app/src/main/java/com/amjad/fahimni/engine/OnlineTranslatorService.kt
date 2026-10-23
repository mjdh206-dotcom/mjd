package com.amjad.fahimni.engine

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.LruCache
import com.amjad.fahimni.data.DialectComparisonItem
import com.amjad.fahimni.data.DialectProfile
import com.amjad.fahimni.data.SlangNote
import com.amjad.fahimni.data.TranslationOutcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.ConnectionPool
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * المحرك السحابي الفائق السرعة عبر الإنترنت:
 * يدمج الذكاء الاصطناعي السحابي للهجات + محرك Google Neural السحابي + المحرك الصرفي العربي
 * لضمان أسرع استجابة صوتية ونصية بدون الحاجة لأي مفتاح خارجي.
 */
class OnlineTranslatorService(private val context: Context) {

    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectionPool(ConnectionPool(6, 3, TimeUnit.MINUTES))
        .connectTimeout(3500, TimeUnit.MILLISECONDS)
        .readTimeout(4000, TimeUnit.MILLISECONDS)
        .writeTimeout(3500, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val cache = LruCache<String, TranslationOutcome>(120)

    fun isOnline(): Boolean {
        return runCatching {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }.getOrDefault(true)
    }

    suspend fun translateSmart(
        rawText: String,
        sourceHint: DialectProfile = DialectProfile.AUTO_DETECT,
        targetDialect: DialectProfile,
        deepCloudAi: Boolean = true,
    ): TranslationOutcome = withContext(Dispatchers.IO) {
        val text = rawText.trim()
        val effectiveTarget = if (targetDialect == DialectProfile.AUTO_DETECT) {
            DialectProfile.AR_SIMPLE
        } else {
            targetDialect
        }

        val cacheKey = "${sourceHint.id}|${effectiveTarget.id}|$deepCloudAi|$text"
        cache.get(cacheKey)?.let { cached ->
            return@withContext cached.copy(latencyMs = 12L)
        }

        val startMs = System.currentTimeMillis()
        val hasArabic = ArabicDialectEngine.containsArabicScript(text)
        val localDetection = ArabicDialectEngine.detectDialect(text)

        val outcome: TranslationOutcome = coroutineScope {
            if (hasArabic && effectiveTarget.isArabicDialect) {
                // ======= مسار 1: لهجة عربية <-> لهجة عربية (أو عربية مبسطة) =======
                val aiDeferred = if (deepCloudAi && isOnline()) {
                    async {
                        withTimeoutOrNull(2900L) {
                            callCloudAiDialect(text, effectiveTarget)
                        }
                    }
                } else null

                val googlePivotDeferred = if (isOnline()) {
                    async {
                        withTimeoutOrNull(1800L) {
                            // تدقيق المعنى الدلالي عبر السحابة لضمان فهم الجمل المركبة
                            val en = callGoogleGtx(localDetection.clearArabicMeaning, "ar", "en")?.first
                            if (!en.isNullOrBlank()) {
                                callGoogleGtx(en, "en", "ar")?.first
                            } else null
                        }
                    }
                } else null

                val aiResult = aiDeferred?.await()
                val pivotArabic = googlePivotDeferred?.await()

                val detected = when {
                    sourceHint != DialectProfile.AUTO_DETECT -> sourceHint
                    localDetection.confidence >= 84 -> localDetection.profile
                    aiResult?.detectedDialect != null && aiResult.detectedDialect != DialectProfile.AUTO_DETECT ->
                        aiResult.detectedDialect
                    else -> localDetection.profile
                }

                val mergedNotes = mergeNotes(localDetection.notes, aiResult?.notes.orEmpty())
                val clearSubtitle = aiResult?.clearArabic?.takeIf { it.isNotBlank() }
                    ?: localDetection.clearArabicMeaning

                val finalTranslated = when {
                    !aiResult?.translated.isNullOrBlank() -> aiResult!!.translated
                    effectiveTarget == DialectProfile.AR_SIMPLE -> localDetection.clearArabicMeaning
                    else -> {
                        val base = localDetection.clearArabicMeaning.ifBlank { pivotArabic ?: text }
                        ArabicDialectEngine.convertToArabicDialect(base, effectiveTarget)
                    }
                }

                TranslationOutcome(
                    originalText = text,
                    translatedText = finalTranslated,
                    clearArabicSubtitle = clearSubtitle,
                    detectedDialect = detected,
                    targetDialect = effectiveTarget,
                    confidence = localDetection.confidence.coerceAtLeast(if (aiResult != null) 92 else 85),
                    slangNotes = mergedNotes,
                    latencyMs = (System.currentTimeMillis() - startMs).coerceAtLeast(35L),
                    engineLabel = if (aiResult != null) "سحابي ذكي فوري" else "محرك اللهجات السريع",
                )
            } else if (hasArabic && !effectiveTarget.isArabicDialect) {
                // ======= مسار 2: أي لهجة عربية -> لغة أجنبية =======
                // نحول اللهجة العامية أولاً لمعناها الدقيق ثم نترجمها سحابياً بأقصى سرعة!
                val semanticArabic = localDetection.clearArabicMeaning.ifBlank { text }

                val gtxDeferred = async {
                    withTimeoutOrNull(2500L) {
                        callGoogleGtx(semanticArabic, "ar", effectiveTarget.googleIso)
                            ?: callMyMemory(semanticArabic, "ar", effectiveTarget.googleIso)
                    }
                }
                val aiDeferred = if (deepCloudAi) {
                    async {
                        withTimeoutOrNull(2500L) {
                            callCloudAiDialect(text, effectiveTarget)
                        }
                    }
                } else null

                val gtx = gtxDeferred.await()
                val ai = aiDeferred?.await()

                val detected = if (sourceHint != DialectProfile.AUTO_DETECT) {
                    sourceHint
                } else {
                    localDetection.profile
                }

                val translated = ai?.translated?.takeIf { it.isNotBlank() }
                    ?: gtx?.first?.takeIf { it.isNotBlank() }
                    ?: semanticArabic

                TranslationOutcome(
                    originalText = text,
                    translatedText = translated,
                    clearArabicSubtitle = semanticArabic,
                    detectedDialect = detected,
                    targetDialect = effectiveTarget,
                    confidence = localDetection.confidence,
                    slangNotes = mergeNotes(localDetection.notes, ai?.notes.orEmpty()),
                    latencyMs = (System.currentTimeMillis() - startMs).coerceAtLeast(40L),
                    engineLabel = "ترجمة سحابية فورية",
                )
            } else {
                // ======= مسار 3: لغة أجنبية (كشف تلقائي) -> لهجتي العربية أو لغة أخرى =======
                val gtxArDeferred = async {
                    withTimeoutOrNull(2600L) {
                        callGoogleGtx(text, "auto", if (effectiveTarget.isArabicDialect) "ar" else effectiveTarget.googleIso)
                            ?: callMyMemory(text, "en", if (effectiveTarget.isArabicDialect) "ar" else effectiveTarget.googleIso)
                    }
                }
                val aiDeferred = if (deepCloudAi) {
                    async {
                        withTimeoutOrNull(2600L) {
                            callCloudAiDialect(text, effectiveTarget)
                        }
                    }
                } else null

                val gtx = gtxArDeferred.await()
                val ai = aiDeferred?.await()

                val detectedForeign = DialectProfile.fromGoogleIso(gtx?.second)
                    ?: ai?.detectedDialect?.takeIf { it != DialectProfile.AUTO_DETECT }
                    ?: if (sourceHint != DialectProfile.AUTO_DETECT) sourceHint else DialectProfile.EN

                val clearArabic = if (effectiveTarget.isArabicDialect) {
                    gtx?.first ?: ai?.clearArabic ?: text
                } else {
                    ai?.clearArabic ?: ""
                }

                val finalTranslated = when {
                    !ai?.translated.isNullOrBlank() -> ai!!.translated
                    effectiveTarget.isArabicDialect && !gtx?.first.isNullOrBlank() ->
                        ArabicDialectEngine.convertToArabicDialect(gtx!!.first, effectiveTarget)
                    !gtx?.first.isNullOrBlank() -> gtx!!.first
                    else -> text
                }

                TranslationOutcome(
                    originalText = text,
                    translatedText = finalTranslated,
                    clearArabicSubtitle = clearArabic,
                    detectedDialect = detectedForeign,
                    targetDialect = effectiveTarget,
                    confidence = 95,
                    slangNotes = ai?.notes.orEmpty(),
                    latencyMs = (System.currentTimeMillis() - startMs).coerceAtLeast(45L),
                    engineLabel = "كشف وترجمة سحابية",
                )
            }
        }

        cache.put(cacheKey, outcome)
        outcome
    }

    suspend fun compareAcrossDialectsOnline(text: String): List<DialectComparisonItem> =
        withContext(Dispatchers.IO) {
            val clean = text.trim()
            if (clean.isBlank()) return@withContext emptyList()
            // إن كان النص بلغة أجنبية نترجمه للعربية أولاً عبر السحابة ثم نولّد كل اللهجات
            val baseArabic = if (!ArabicDialectEngine.containsArabicScript(clean) && isOnline()) {
                callGoogleGtx(clean, "auto", "ar")?.first ?: clean
            } else {
                clean
            }
            ArabicDialectEngine.compareAllDialects(baseArabic)
        }

    private data class CloudAiResponse(
        val detectedDialect: DialectProfile?,
        val translated: String,
        val clearArabic: String,
        val notes: List<SlangNote>,
    )

    private fun callGoogleGtx(text: String, sl: String, tl: String): Pair<String, String?>? {
        return runCatching {
            val encoded = URLEncoder.encode(text, "UTF-8")
            val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=$sl&tl=$tl&dt=t&q=$encoded"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124.0.0.0 Mobile Safari/537.36")
                .get()
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val root = JSONArray(body)
                val sentences = root.optJSONArray(0) ?: return null
                val sb = StringBuilder()
                for (i in 0 until sentences.length()) {
                    val seg = sentences.optJSONArray(i)
                    val piece = seg?.optString(0)
                    if (!piece.isNullOrEmpty() && piece != "null") {
                        sb.append(piece)
                    }
                }
                val detectedIso = root.optString(2, "").takeIf { it.isNotBlank() && it != "null" }
                val result = sb.toString().trim()
                if (result.isBlank()) null else (result to detectedIso)
            }
        }.getOrNull()
    }

    private fun callMyMemory(text: String, sl: String, tl: String): Pair<String, String?>? {
        return runCatching {
            val langPair = "${if (sl == "auto") "ar" else sl}|$tl"
            val encoded = URLEncoder.encode(text, "UTF-8")
            val encodedPair = URLEncoder.encode(langPair, "UTF-8")
            val url = "https://api.mymemory.translated.net/get?q=$encoded&langpair=$encodedPair"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "FahimniAndroid/1.0")
                .get()
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val json = JSONObject(body)
                val translated = json.optJSONObject("responseData")
                    ?.optString("translatedText", "")
                    ?.trim()
                    .orEmpty()
                if (translated.isBlank() || translated.contains("MYMEMORY WARNING", ignoreCase = true)) {
                    null
                } else {
                    translated to sl
                }
            }
        }.getOrNull()
    }

    private fun callCloudAiDialect(text: String, target: DialectProfile): CloudAiResponse? {
        return runCatching {
            val systemPrompt = """
                أنت خبير فوري في ترجمة جميع اللهجات العربية المحكية واللغات العالمية.
                مهمتك:
                1) اكتشف اللهجة أو اللغة الحقيقية للجملة (استخدم أحد الرموز: ar_sy, ar_lb, ar_jo_ps, ar_eg, ar_gulf, ar_iq, ar_ma, ar_dz, ar_tn, ar_ly, ar_ye, ar_sd, ar_simple, en, tr, nl, de, fr, es, it, sv, ru, fa, ur, zh).
                2) حوّل المعنى الحقيقي المقصود (وليس الترجمة الحرفية الجامدة) إلى أسلوب طبيعي جداً في الهدف المطلوب: (${target.aiStylePrompt}).
                3) اكتب أيضاً المعنى بالعربية المبسطة الواضحة في clearArabic.
                4) استخرج حتى 3 كلمات عامية صعبة في الجملة مع معناها المبسط في notes.
                أجب بصيغة JSON فقط بدون أي شرح إضافي:
                {"detected":"ar_ma","translated":"...","clearArabic":"...","notes":[{"w":"...","m":"..."}]}
            """.trimIndent()

            val payload = JSONObject().apply {
                put("model", "openai")
                put("temperature", 0.2)
                put("messages", JSONArray().apply {
                    put(JSONObject().put("role", "system").put("content", systemPrompt))
                    put(JSONObject().put("role", "user").put("content", text))
                })
            }

            val req = Request.Builder()
                .url("https://text.pollinations.ai/openai")
                .header("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val rawBody = resp.body?.string()?.trim() ?: return null
                val contentStr = if (rawBody.startsWith("{") && rawBody.contains("\"choices\"")) {
                    JSONObject(rawBody)
                        .optJSONArray("choices")
                        ?.optJSONObject(0)
                        ?.optJSONObject("message")
                        ?.optString("content", "")
                        .orEmpty()
                } else {
                    rawBody
                }

                val jsonStart = contentStr.indexOf('{')
                val jsonEnd = contentStr.lastIndexOf('}')
                if (jsonStart < 0 || jsonEnd <= jsonStart) return null
                val parsed = JSONObject(contentStr.substring(jsonStart, jsonEnd + 1))

                val translated = parsed.optString("translated", "").trim()
                if (translated.isBlank()) return null

                val detectedCode = parsed.optString("detected", "")
                val clearArabic = parsed.optString("clearArabic", "").trim()
                val notesJson = parsed.optJSONArray("notes")
                val notes = buildList {
                    if (notesJson != null) {
                        for (i in 0 until notesJson.length()) {
                            val item = notesJson.optJSONObject(i) ?: continue
                            val w = item.optString("w", "").trim()
                            val m = item.optString("m", "").trim()
                            if (w.isNotBlank() && m.isNotBlank()) add(SlangNote(w, m))
                        }
                    }
                }

                CloudAiResponse(
                    detectedDialect = DialectProfile.entries.firstOrNull { it.id.equals(detectedCode, ignoreCase = true) },
                    translated = translated,
                    clearArabic = clearArabic,
                    notes = notes,
                )
            }
        }.getOrNull()
    }

    private fun mergeNotes(a: List<SlangNote>, b: List<SlangNote>): List<SlangNote> {
        val map = LinkedHashMap<String, SlangNote>()
        for (item in a + b) {
            val key = ArabicDialectEngine.normalizeForMatch(item.word)
            if (key.isNotBlank() && !map.containsKey(key)) {
                map[key] = item
            }
        }
        return map.values.take(6)
    }
}

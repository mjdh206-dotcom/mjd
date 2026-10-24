package com.amjad.fahimni.engine

import android.util.LruCache
import com.amjad.fahimni.data.LanguageCatalog
import com.amjad.fahimni.data.SpokenLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * محرك الترجمة السحابي الفائق السرعة (~200ms):
 * 1) يكتشف لغة المتحدث تلقائياً ويترجم كلامه للعربي فوراً.
 * 2) يترجم كلامك العربي (بأي لهجة) إلى لغة الشخص الذي أمامك فوراً.
 */
class OnlineTranslatorService {

    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectionPool(ConnectionPool(6, 3, TimeUnit.MINUTES))
        .connectTimeout(3500, TimeUnit.MILLISECONDS)
        .readTimeout(4000, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val toArabicCache = LruCache<String, Pair<SpokenLanguage, String>>(100)
    private val fromArabicCache = LruCache<String, String>(100)

    /**
     * الزر الأول: «اعرف شو لغتو وترجم للعربي»
     */
    suspend fun detectLanguageAndTranslateToArabic(
        rawSpokenText: String,
        acousticLocaleHint: String? = null,
    ): Pair<SpokenLanguage, String> = withContext(Dispatchers.IO) {
        val text = rawSpokenText.trim()
        val cacheKey = "${acousticLocaleHint.orEmpty()}|$text"
        toArabicCache.get(cacheKey)?.let { return@withContext it }

        // إذا كان الكلام أصلاً بحروف عربية (لهجة عربية أخرى)
        if (ArabicDialectEngine.containsArabicScript(text)) {
            val dialect = ArabicDialectEngine.detectArabicDialect(text)
            val clearArabic = ArabicDialectEngine.normalizeToClearArabic(text)
            val pair = dialect to clearArabic
            toArabicCache.put(cacheKey, pair)
            return@withContext pair
        }

        // كشف اللغة الأجنبية تلقائياً وترجمتها للعربية بأقصى سرعة عبر Google Neural
        val gtx = callGoogleGtx(text = text, sl = "auto", tl = "ar")
        if (gtx != null) {
            val (arabicText, detectedIso) = gtx
            val resolvedLang = LanguageCatalog.resolveFromCode(detectedIso)
                ?: LanguageCatalog.resolveFromCode(acousticLocaleHint)
                ?: LanguageCatalog.defaultFallbackTarget
            val pair = resolvedLang to arabicText
            toArabicCache.put(cacheKey, pair)
            return@withContext pair
        }

        // خط احتياطي سريع في حال تعذر الوصول المؤقت
        val fallbackLang = LanguageCatalog.resolveFromCode(acousticLocaleHint)
            ?: LanguageCatalog.defaultFallbackTarget
        val backupArabic = callMyMemory(text, fallbackLang.googleIso, "ar") ?: text
        val pair = fallbackLang to backupArabic
        toArabicCache.put(cacheKey, pair)
        pair
    }

    /**
     * الزر الثاني: «احكي بالعربي (بأي لهجة) ⬅️ يترجملو بناءً على لغتو»
     */
    suspend fun translateMyArabicToPartner(
        mySpokenArabic: String,
        partnerLanguage: SpokenLanguage,
    ): String = withContext(Dispatchers.IO) {
        val cleanInput = mySpokenArabic.trim()
        val clearMeaningArabic = ArabicDialectEngine.normalizeToClearArabic(cleanInput)

        // إذا كان الشخص المقابل عربياً نكتفي بالعربية المبسطة الواضحة للجميع
        if (partnerLanguage.isArabic) {
            return@withContext clearMeaningArabic
        }

        val cacheKey = "${partnerLanguage.googleIso}|$clearMeaningArabic"
        fromArabicCache.get(cacheKey)?.let { return@withContext it }

        val translated = callGoogleGtx(
            text = clearMeaningArabic,
            sl = "ar",
            tl = partnerLanguage.googleIso
        )?.first
            ?: callMyMemory(clearMeaningArabic, "ar", partnerLanguage.googleIso)
            ?: clearMeaningArabic

        fromArabicCache.put(cacheKey, translated)
        translated
    }

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
                val out = sb.toString().trim()
                if (out.isBlank()) null else (out to detectedIso)
            }
        }.getOrNull()
    }

    private fun callMyMemory(text: String, sl: String, tl: String): String? {
        return runCatching {
            val encoded = URLEncoder.encode(text, "UTF-8")
            val pair = URLEncoder.encode("$sl|$tl", "UTF-8")
            val req = Request.Builder()
                .url("https://api.mymemory.translated.net/get?q=$encoded&langpair=$pair")
                .header("User-Agent", "FahimniAndroid/2.0")
                .get()
                .build()

            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val body = resp.body?.string() ?: return null
                val translated = JSONObject(body)
                    .optJSONObject("responseData")
                    ?.optString("translatedText", "")
                    ?.trim()
                    .orEmpty()
                translated.takeIf { it.isNotBlank() && !it.contains("MYMEMORY WARNING", true) }
            }
        }.getOrNull()
    }
}

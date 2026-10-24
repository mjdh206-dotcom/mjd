package com.amjad.fahimni.data

import java.util.Locale

const val DEVELOPER_ATTRIBUTION = "تم تطوير وتنفيذ هذا التطبيق بواسطة.امجد حبيب"

enum class TurnSide {
    OTHER_PERSON, // زر 1: اعرف شو لغتو وترجم للعربي
    ME_ARABIC,    // زر 2: احكي بالعربي ويترجملو بناءً على لغتو
}

data class SpokenLanguage(
    val id: String,
    val nameAr: String,
    val flag: String,
    val googleIso: String,
    val speechLocale: String,
    val ttsLang: String,
    val isArabic: Boolean = false,
)

data class ChatTurn(
    val id: String,
    val side: TurnSide,
    val originalText: String,
    val translatedText: String,
    val partnerLanguage: SpokenLanguage,
    val timestamp: Long,
)

object LanguageCatalog {

    val allLanguages: List<SpokenLanguage> = listOf(
        // الأكثر استخداماً في أوروبا وهولندا والعالم
        SpokenLanguage("nl", "الهولندية", "🇳🇱", "nl", "nl-NL", "nl"),
        SpokenLanguage("en", "الإنجليزية", "🇬🇧", "en", "en-US", "en"),
        SpokenLanguage("de", "الألمانية", "🇩🇪", "de", "de-DE", "de"),
        SpokenLanguage("tr", "التركية", "🇹🇷", "tr", "tr-TR", "tr"),
        SpokenLanguage("fr", "الفرنسية", "🇫🇷", "fr", "fr-FR", "fr"),
        SpokenLanguage("pl", "البولندية", "🇵🇱", "pl", "pl-PL", "pl"),
        SpokenLanguage("es", "الإسبانية", "🇪🇸", "es", "es-ES", "es"),
        SpokenLanguage("ro", "الرومانية", "🇷🇴", "ro", "ro-RO", "ro"),
        SpokenLanguage("bg", "البلغارية", "🇧🇬", "bg", "bg-BG", "bg"),
        SpokenLanguage("uk", "الأوكرانية", "🇺🇦", "uk", "uk-UA", "uk"),
        SpokenLanguage("ru", "الروسية", "🇷🇺", "ru", "ru-RU", "ru"),
        SpokenLanguage("it", "الإيطالية", "🇮🇹", "it", "it-IT", "it"),
        SpokenLanguage("pt", "البرتغالية", "🇵🇹", "pt", "pt-PT", "pt"),
        SpokenLanguage("sv", "السويدية", "🇸🇪", "sv", "sv-SE", "sv"),
        SpokenLanguage("el", "اليونانية", "🇬🇷", "el", "el-GR", "el"),
        SpokenLanguage("da", "الدنماركية", "🇩🇰", "da", "da-DK", "da"),
        SpokenLanguage("no", "النرويجية", "🇳🇴", "no", "nb-NO", "no"),
        SpokenLanguage("fi", "الفنلندية", "🇫🇮", "fi", "fi-FI", "fi"),
        SpokenLanguage("cs", "التشيكية", "🇨🇿", "cs", "cs-CZ", "cs"),
        SpokenLanguage("hu", "المجرية", "🇭🇺", "hu", "hu-HU", "hu"),
        SpokenLanguage("sr", "الصربية", "🇷🇸", "sr", "sr-RS", "sr"),
        SpokenLanguage("hr", "الكرواتية", "🇭🇷", "hr", "hr-HR", "hr"),
        SpokenLanguage("sq", "الألبانية", "🇦🇱", "sq", "sq-AL", "sq"),

        // لغات الشرق والآسيوية والإفريقية
        SpokenLanguage("fa", "الفارسية", "🇮🇷", "fa", "fa-IR", "fa"),
        SpokenLanguage("ku", "الكردية", "☀️", "ku", "ku", "tr"),
        SpokenLanguage("ur", "الأوردو", "🇵🇰", "ur", "ur-PK", "ur"),
        SpokenLanguage("hi", "الهندية", "🇮🇳", "hi", "hi-IN", "hi"),
        SpokenLanguage("bn", "البنغالية", "🇧🇩", "bn", "bn-BD", "bn"),
        SpokenLanguage("id", "الإندونيسية", "🇮🇩", "id", "id-ID", "id"),
        SpokenLanguage("ms", "الماليزية", "🇲🇾", "ms", "ms-MY", "ms"),
        SpokenLanguage("tl", "الفلبينية", "🇵🇭", "tl", "fil-PH", "tl"),
        SpokenLanguage("vi", "الفيتنامية", "🇻🇳", "vi", "vi-VN", "vi"),
        SpokenLanguage("th", "التايلاندية", "🇹🇭", "th", "th-TH", "th"),
        SpokenLanguage("zh", "الصينية", "🇨🇳", "zh-CN", "zh-CN", "zh-CN"),
        SpokenLanguage("ja", "اليابانية", "🇯🇵", "ja", "ja-JP", "ja"),
        SpokenLanguage("ko", "الكورية", "🇰🇷", "ko", "ko-KR", "ko"),
        SpokenLanguage("so", "الصومالية", "🇸🇴", "so", "so-SO", "so"),
        SpokenLanguage("am", "الأمهرية", "🇪🇹", "am", "am-ET", "am"),
        SpokenLanguage("sw", "السواحيلية", "🇰🇪", "sw", "sw-KE", "sw"),

        // اللهجات العربية (في حال كان الشخص المقابل يتحدث لهجة عربية صعبة)
        SpokenLanguage("ar_ma", "العربية (الدارجة المغربية)", "🇲🇦", "ar", "ar-MA", "ar", isArabic = true),
        SpokenLanguage("ar_dz", "العربية (الجزائرية)", "🇩🇿", "ar", "ar-DZ", "ar", isArabic = true),
        SpokenLanguage("ar_tn", "العربية (التونسية)", "🇹🇳", "ar", "ar-TN", "ar", isArabic = true),
        SpokenLanguage("ar_iq", "العربية (العراقية)", "🇮🇶", "ar", "ar-IQ", "ar", isArabic = true),
        SpokenLanguage("ar_eg", "العربية (المصرية)", "🇪🇬", "ar", "ar-EG", "ar", isArabic = true),
        SpokenLanguage("ar_gulf", "العربية (الخليجية)", "🇸🇦", "ar", "ar-SA", "ar", isArabic = true),
        SpokenLanguage("ar_sy", "العربية (الشامية)", "🇸🇾", "ar", "ar-SY", "ar", isArabic = true),
        SpokenLanguage("ar_ye", "العربية (اليمنية)", "🇾🇪", "ar", "ar-YE", "ar", isArabic = true),
        SpokenLanguage("ar_sd", "العربية (السودانية)", "🇸🇩", "ar", "ar-SD", "ar", isArabic = true),
        SpokenLanguage("ar_simple", "العربية المبسطة", "🌍", "ar", "ar-SA", "ar", isArabic = true),
    )

    val defaultFallbackTarget: SpokenLanguage = allLanguages.first { it.id == "en" }

    fun fromId(id: String?): SpokenLanguage? {
        if (id.isNullOrBlank()) return null
        return allLanguages.firstOrNull { it.id.equals(id, ignoreCase = true) }
    }

    /**
     * يطابق أي رمز لغة قادم من حساس الصوت أو من محرك الترجمة السحابي (مثل nl-NL, nl, zh-TW, nb, fil...)
     */
    fun resolveFromCode(code: String?): SpokenLanguage? {
        if (code.isNullOrBlank()) return null
        val clean = code.trim().replace('_', '-')
        val lower = clean.lowercase(Locale.ROOT)

        // مطابقة مباشرة بالمعرف أو speechLocale
        allLanguages.firstOrNull {
            it.id.lowercase(Locale.ROOT) == lower ||
                it.speechLocale.lowercase(Locale.ROOT) == lower ||
                it.googleIso.lowercase(Locale.ROOT) == lower
        }?.let { return it }

        // مطابقة بالبادئة الأساسية (مثلاً nl-BE -> nl، en-GB -> en، zh-TW -> zh)
        val base = lower.substringBefore('-')
        when (base) {
            "nb", "nn" -> return fromId("no")
            "fil" -> return fromId("tl")
            "iw", "he" -> return SpokenLanguage("he", "العبرية", "🌐", "iw", "he-IL", "iw")
        }

        allLanguages.firstOrNull {
            it.googleIso.lowercase(Locale.ROOT).substringBefore('-') == base ||
                it.id.lowercase(Locale.ROOT) == base
        }?.let { return it }

        // إذا كانت لغة نادرة غير مدرجة نولّد اسمها العربي تلقائياً من النظام
        val arabicName = runCatching {
            Locale(base).getDisplayLanguage(Locale("ar")).takeIf { it.isNotBlank() && it != base }
        }.getOrNull() ?: clean

        return SpokenLanguage(
            id = base,
            nameAr = arabicName,
            flag = "🌐",
            googleIso = base,
            speechLocale = clean,
            ttsLang = base,
            isArabic = (base == "ar"),
        )
    }
}

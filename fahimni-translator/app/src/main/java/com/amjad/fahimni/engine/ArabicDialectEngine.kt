package com.amjad.fahimni.engine

import com.amjad.fahimni.data.LanguageCatalog
import com.amjad.fahimni.data.SpokenLanguage

/**
 * محرك فهم جميع اللهجات العربية:
 * يضمن أن أي كلام تقوله بالعربي بأي لهجة (شامي، مصري، عراقي، خليجي، مغربي، أردني، يمني، سوداني...)
 * يُفهم بمعناه الحقيقي الدقيق قبل ترجمته إلى لغة الشخص الذي أمامك.
 */
object ArabicDialectEngine {

    private val tashkeelRegex = Regex("[\\u0617-\\u061A\\u064B-\\u0652\\u0670\\u0640]")

    fun containsArabicScript(text: String): Boolean =
        text.any { it in '\u0600'..'\u06FF' || it in '\u0750'..'\u077F' }

    fun normalizeForMatch(input: String): String {
        return input
            .replace(tashkeelRegex, "")
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ى', 'ي')
            .replace(Regex("[؟?!.,،؛:\"'()\\[\\]]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .lowercase()
    }

    private val dialectCueToId: List<Pair<String, String>> = listOf(
        "دابا" to "ar_ma", "بغيت" to "ar_ma", "بزاف" to "ar_ma", "مزيان" to "ar_ma",
        "واخا" to "ar_ma", "حيت" to "ar_ma", "ديال" to "ar_ma", "علاش" to "ar_ma", "عييت" to "ar_ma",
        "واش راك" to "ar_dz", "راني" to "ar_dz", "درك" to "ar_dz", "بصح" to "ar_dz", "ماكانش" to "ar_dz",
        "برشا" to "ar_tn", "شنوة" to "ar_tn", "باهي" to "ar_tn", "يعيشك" to "ar_tn", "توا" to "ar_tn",
        "شكو ماكو" to "ar_iq", "هسة" to "ar_iq", "ماكو" to "ar_iq", "هواي" to "ar_iq", "كلش" to "ar_iq", "خوية" to "ar_iq",
        "ازيك" to "ar_eg", "عامل ايه" to "ar_eg", "دلوقتي" to "ar_eg", "عايز" to "ar_eg", "عاوز" to "ar_eg", "مفيش" to "ar_eg", "كده" to "ar_eg",
        "طال عمرك" to "ar_gulf", "وش علومك" to "ar_gulf", "ابغى" to "ar_gulf", "الحين" to "ar_gulf", "وايد" to "ar_gulf", "شخبارك" to "ar_gulf",
        "شو" to "ar_sy", "هلا" to "ar_sy", "بدي" to "ar_sy", "بدك" to "ar_sy", "كتير" to "ar_sy", "مشان" to "ar_sy", "دغري" to "ar_sy", "هون" to "ar_sy",
        "اشتي" to "ar_ye", "ذلحين" to "ar_ye", "ما بش" to "ar_ye",
        "يا زول" to "ar_sd", "داير" to "ar_sd", "هسي" to "ar_sd", "سمح" to "ar_sd",
    )

    fun detectArabicDialect(text: String): SpokenLanguage {
        val norm = normalizeForMatch(text)
        val tokens = norm.split(" ").toSet()
        val scores = mutableMapOf<String, Int>()
        for ((cue, id) in dialectCueToId) {
            val c = normalizeForMatch(cue)
            val hit = if (c.contains(" ")) norm.contains(c) else tokens.contains(c)
            if (hit) scores[id] = (scores[id] ?: 0) + 1
        }
        val bestId = scores.maxByOrNull { it.value }?.key ?: "ar_simple"
        return LanguageCatalog.fromId(bestId) ?: LanguageCatalog.allLanguages.last()
    }

    private val phraseToClearRules: List<Pair<Regex, String>> = listOf(
        Regex("شكو\\s+ماكو") to "ما الأخبار",
        Regex("واش\\s+راك") to "كيف حالك",
        Regex("عامل\\s+ايه|عامل\\s+إيه|عاملة\\s+ايه|عاملة\\s+إيه") to "كيف حالك",
        Regex("وش\\s+علومك|شو\\s+الاخبار|شو\\s+الأخبار") to "ما الأخبار",
        Regex("طال\\s+عمرك") to "شكراً لك",
        Regex("ما\\s+قصرت") to "شكراً جزيلاً لك",
        Regex("يعطيك\\s+العافية|الله\\s+يعطيك\\s+العافية|يعطيك\\s+الصحة") to "شكراً جزيلاً لك",
        Regex("تكرم\\s+عينك|على\\s+راسي") to "بكل سرور",
        Regex("بلا\\s+صغرة") to "لو سمحت",
        Regex("يا\\s+زلمة|يا\\s+زول|يا\\s+باشا|يا\\s+خبير|يا\\s+معلم") to "يا صديقي",
        Regex("ما\\s+اشتيش|ما\\s+أشتيش|ما\\s+بدي|مش\\s+عايز|ما\\s+بغيتش|ما\\s+ابي|ما\\s+أبي") to "لا أريد",
        Regex("ما\\s+فيني|مقدرش|ما\\s+بقدر") to "لا أستطيع",
        Regex("ما\\s+بعرف|معرفش|ما\\s+ادري|ما\\s+أدري") to "لا أعرف",
        Regex("ما\\s+عندي") to "ليس لدي",
        Regex("ما\\s+في|ماكو|مفيش|ما\\s+كاينش|ما\\s+فماش|ما\\s+بش|ماكانش") to "لا يوجد",
        Regex("قديش\\s+حقو|قديش\\s+هاد|بشحال\\s+هذا|بكام\\s+ده|بيش\\s+هذا") to "كم سعر هذا",
        Regex("شو\\s+بدك\\s+تساوي|شو\\s+بدك\\s+تعمل") to "ماذا تريد أن تفعل",
        Regex("عم\\s+بحكي|كنهضر|دا\\s+احجي") to "أتكلم الآن",
        Regex("بغيت\\s+نمشي|بدي\\s+روح|بدي\\s+اروح|عايز\\s+اروح|ابي\\s+اروح|أبي\\s+أروح|اريد\\s+اروح") to "أريد أن أذهب",
    )

    private val wordToClearMap: Map<String, String> = mapOf(
        "شو" to "ماذا", "شنو" to "ماذا", "وش" to "ماذا", "ويش" to "ماذا",
        "ايش" to "ماذا", "إيش" to "ماذا", "شنوة" to "ماذا", "ايه" to "ماذا", "إيه" to "ماذا",
        "ليش" to "لماذا", "ليه" to "لماذا", "علاش" to "لماذا", "علاه" to "لماذا", "لمه" to "لماذا",
        "وين" to "أين", "فين" to "أين", "لوين" to "إلى أين", "وينك" to "أين أنت",
        "هون" to "هنا", "هوني" to "هنا", "اهنا" to "هنا",
        "حط" to "ضع", "حطو" to "ضعه", "حطها" to "ضعها", "جيب" to "أحضر", "جيبلي" to "أحضر لي",
        "خذ" to "خذ", "شيل" to "ارفع", "شيلو" to "ارفعه",
        "قدامي" to "أمامي", "قدامك" to "أمامك", "جنبي" to "بجانبي", "حدي" to "بجانبي",
        "امبارح" to "أمس", "مبارح" to "أمس", "البارح" to "أمس", "بكرة" to "غداً", "بكرا" to "غداً", "غدوة" to "غداً",
        "ايمتى" to "متى", "امتى" to "متى", "إمتى" to "متى", "فوقاش" to "متى", "وقتاش" to "متى", "شوكت" to "متى",
        "كيفاش" to "كيف", "ازاي" to "كيف", "إزاي" to "كيف", "شلون" to "كيف",
        "شلونك" to "كيف حالك", "ازيك" to "كيف حالك", "إزيك" to "كيف حالك", "كيفك" to "كيف حالك", "شخبارك" to "كيف حالك",
        "قديش" to "كم", "قداش" to "كم", "بشحال" to "بكم", "شحال" to "كم", "شكد" to "كم", "بكام" to "بكم", "بيش" to "بكم",
        "هلا" to "الآن", "هلأ" to "الآن", "هلق" to "الآن", "هسا" to "الآن", "هسة" to "الآن", "هسه" to "الآن",
        "دلوقتي" to "الآن", "الحين" to "الآن", "ذلحين" to "الآن", "دابا" to "الآن", "درك" to "الآن", "توا" to "الآن", "هسي" to "الآن",
        "كتير" to "كثيراً", "بزاف" to "كثيراً", "برشا" to "كثيراً", "هلبا" to "كثيراً", "هواي" to "كثيراً", "وايد" to "كثيراً", "واجد" to "كثيراً",
        "كلش" to "جداً", "اوي" to "جداً", "أوي" to "جداً", "شديد" to "جداً", "قوي" to "جداً",
        "شوي" to "قليلاً", "شوية" to "قليلاً", "بكير" to "مبكراً", "بدري" to "مبكراً",
        "دغري" to "مباشرةً وبسرعة", "دغيا" to "بسرعة", "فيسع" to "بسرعة", "قوام" to "بسرعة",
        "بدي" to "أريد", "بدك" to "تريد", "بدو" to "يريد", "بدنا" to "نريد",
        "عايز" to "أريد", "عاوز" to "أريد", "بغيت" to "أريد", "بغيتي" to "تريد",
        "ابي" to "أريد", "أبي" to "أريد", "ابغى" to "أريد", "أبغى" to "أريد", "تبغى" to "تريد",
        "اشتي" to "أريد", "أشتي" to "أريد", "داير" to "أريد",
        "فيني" to "أستطيع", "فينا" to "يمكننا",
        "نمشي" to "أذهب", "نشري" to "أشتري", "تساوي" to "تفعل", "عييت" to "تعبت",
        "استنى" to "انتظر", "استناني" to "انتظرني", "بلاتي" to "انتظر لحظة",
        "مشان" to "لكي", "كرمال" to "من أجل", "عشان" to "لأن", "علشان" to "لأن", "حيت" to "لأن", "بصح" to "لكن",
        "وياي" to "معي", "وياك" to "معك",
        "منيح" to "جيد", "منيحة" to "جيدة", "مليح" to "جيد", "مزيان" to "جيد", "باهي" to "جيد", "زين" to "جيد", "خوش" to "ممتاز",
        "واخا" to "حسناً", "صافي" to "حسناً تمام", "ابشر" to "حاضر بكل سرور", "أبشر" to "حاضر بكل سرور", "يسلمو" to "شكراً لك",
        "هاد" to "هذا", "هيدا" to "هذا", "ده" to "هذا", "هيدي" to "هذه", "دي" to "هذه", "هدول" to "هؤلاء", "دول" to "هؤلاء",
        "هيك" to "هكذا", "كده" to "هكذا", "كذي" to "هكذا", "هيج" to "هكذا",
        "غراض" to "أغراض", "حوايج" to "أغراض", "حاجة" to "شيء", "حاجات" to "أشياء", "اشي" to "شيء",
        "مصاري" to "نقود", "فلوس" to "نقود", "عالسوق" to "إلى السوق", "للسوك" to "إلى السوق", "عالبيت" to "إلى البيت",
    )

    fun normalizeToClearArabic(input: String): String {
        if (input.isBlank()) return input
        var text = input.trim()
        for ((regex, replacement) in phraseToClearRules) {
            text = text.replace(regex, replacement)
        }
        val words = text.split(Regex("\\s+"))
        val mapped = words.map { rawWord ->
            val prefixPunct = rawWord.takeWhile { it in "\"'([{" }
            val suffixPunct = rawWord.takeLastWhile { it in "؟?!.,،؛:\"'()]}" }
            val core = rawWord.removePrefix(prefixPunct).removeSuffix(suffixPunct)
            val normCore = core.replace(tashkeelRegex, "")
            val direct = wordToClearMap[normCore] ?: wordToClearMap[normalizeForMatch(normCore)]
            if (direct != null) "$prefixPunct$direct$suffixPunct" else rawWord
        }
        return mapped.joinToString(" ").replace(Regex("\\s+"), " ").trim()
    }
}

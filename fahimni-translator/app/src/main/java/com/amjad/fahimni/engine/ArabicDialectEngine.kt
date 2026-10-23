package com.amjad.fahimni.engine

import com.amjad.fahimni.data.DialectComparisonItem
import com.amjad.fahimni.data.DialectDetectionResult
import com.amjad.fahimni.data.DialectProfile
import com.amjad.fahimni.data.SlangNote

/**
 * المحرك اللغوي المتخصص للهجات العربية (13 لهجة عربية):
 * 1) كشف تلقائي دقيق للهجة المتحدث (مع نسبة الثقة).
 * 2) استخراج الكلمات والمصطلحات الشعبية وتفسيرها بالعربية الواضحة.
 * 3) توحيد أي لهجة عربية إلى العربية المبسطة الواضحة، أو تحويلها إلى أي لهجة عربية أخرى.
 */
object ArabicDialectEngine {

    data class MarkerEntry(
        val dialect: DialectProfile,
        val pattern: String,
        val clearMeaning: String,
        val weight: Double,
        val isPhrase: Boolean = false,
    )

    private val tashkeelRegex = Regex("[\\u0617-\\u061A\\u064B-\\u0652\\u0670\\u0640]")

    fun normalizeForMatch(input: String): String {
        return input
            .replace(tashkeelRegex, "")
            .replace('أ', 'ا')
            .replace('إ', 'ا')
            .replace('آ', 'ا')
            .replace('ٱ', 'ا')
            .replace('ى', 'ي')
            .replace('ؤ', 'و')
            .replace('ئ', 'ي')
            .replace(Regex("[؟?!.,،؛:\"'()\\[\\]]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .lowercase()
    }

    fun containsArabicScript(text: String): Boolean =
        text.any { it in '\u0600'..'\u06FF' || it in '\u0750'..'\u077F' }

    // قاموس غني للمؤشرات اللهجية والمفردات والتعابير عبر اللهجات العربية
    private val markers: List<MarkerEntry> = listOf(
        // ==================== الشامية (السورية) ====================
        MarkerEntry(DialectProfile.AR_SY, "يعطيك العافية", "تحية وتقدير (شكراً لجهدك)", 4.5, true),
        MarkerEntry(DialectProfile.AR_SY, "شو الاخبار", "ما الأخبار", 5.5, true),
        MarkerEntry(DialectProfile.AR_SY, "شو بدك", "ماذا تريد", 6.0, true),
        MarkerEntry(DialectProfile.AR_SY, "شو عم", "ماذا تفعل الآن", 6.0, true),
        MarkerEntry(DialectProfile.AR_SY, "تكرم عينك", "بكل سرور وعلى الرحب والسعة", 6.0, true),
        MarkerEntry(DialectProfile.AR_SY, "على راسي", "بكل احترام وترحيب", 4.0, true),
        MarkerEntry(DialectProfile.AR_SY, "ما عاد", "لم يعد", 4.5, true),
        MarkerEntry(DialectProfile.AR_SY, "ما فيني", "لا أستطيع", 5.5, true),
        MarkerEntry(DialectProfile.AR_SY, "فينا", "يمكننا", 4.0, false),
        MarkerEntry(DialectProfile.AR_SY, "فيني", "أستطيع", 4.5, false),
        MarkerEntry(DialectProfile.AR_SY, "هلا", "الآن", 3.2, false),
        MarkerEntry(DialectProfile.AR_SY, "لهلا", "حتى الآن", 5.0, false),
        MarkerEntry(DialectProfile.AR_SY, "شو", "ماذا", 4.5, false),
        MarkerEntry(DialectProfile.AR_SY, "شلونك", "كيف حالك", 3.5, false),
        MarkerEntry(DialectProfile.AR_SY, "منيح", "جيد / بخير", 4.8, false),
        MarkerEntry(DialectProfile.AR_SY, "منيحة", "جيدة / بخير", 4.8, false),
        MarkerEntry(DialectProfile.AR_SY, "بدي", "أريد", 4.8, false),
        MarkerEntry(DialectProfile.AR_SY, "بدك", "تريد", 4.8, false),
        MarkerEntry(DialectProfile.AR_SY, "بدو", "يريد", 4.8, false),
        MarkerEntry(DialectProfile.AR_SY, "بدنا", "نريد", 4.8, false),
        MarkerEntry(DialectProfile.AR_SY, "كتير", "كثيراً / جداً", 4.2, false),
        MarkerEntry(DialectProfile.AR_SY, "مشان", "من أجل / لكي", 5.5, false),
        MarkerEntry(DialectProfile.AR_SY, "كرمال", "من أجل", 5.2, false),
        MarkerEntry(DialectProfile.AR_SY, "هاد", "هذا", 5.0, false),
        MarkerEntry(DialectProfile.AR_SY, "هدول", "هؤلاء", 5.0, false),
        MarkerEntry(DialectProfile.AR_SY, "هيك", "هكذا", 4.5, false),
        MarkerEntry(DialectProfile.AR_SY, "دغري", "مباشرةً / فوراً", 5.2, false),
        MarkerEntry(DialectProfile.AR_SY, "بكير", "مبكراً", 5.0, false),
        MarkerEntry(DialectProfile.AR_SY, "غراض", "أغراض / حاجيات", 4.2, false),
        MarkerEntry(DialectProfile.AR_SY, "مصاري", "نقود / مال", 4.5, false),
        MarkerEntry(DialectProfile.AR_SY, "تؤبرني", "كلمة محبة شامية (فديتك)", 6.0, false),
        MarkerEntry(DialectProfile.AR_SY, "توبرني", "كلمة محبة شامية (فديتك)", 6.0, false),
        MarkerEntry(DialectProfile.AR_SY, "معلم", "يا أخي / يا أستاذ", 3.5, false),
        MarkerEntry(DialectProfile.AR_SY, "بركي", "ربما / لعل", 5.0, false),
        MarkerEntry(DialectProfile.AR_SY, "بلكي", "ربما / لعل", 4.5, false),
        MarkerEntry(DialectProfile.AR_SY, "طنشتني", "تجاهلتني", 4.5, false),
        MarkerEntry(DialectProfile.AR_SY, "تساوي", "تفعل", 4.2, false),
        MarkerEntry(DialectProfile.AR_SY, "عم بحكي", "أتكلم الآن", 6.0, true),
        MarkerEntry(DialectProfile.AR_SY, "عالسوق", "إلى السوق", 4.5, false),
        MarkerEntry(DialectProfile.AR_SY, "عالبيت", "إلى البيت", 4.5, false),

        // ==================== اللبنانية ====================
        MarkerEntry(DialectProfile.AR_LB, "كيفك", "كيف حالك", 3.8, false),
        MarkerEntry(DialectProfile.AR_LB, "هلق", "الآن", 6.0, false),
        MarkerEntry(DialectProfile.AR_LB, "هيدا", "هذا", 6.5, false),
        MarkerEntry(DialectProfile.AR_LB, "هيدي", "هذه", 6.5, false),
        MarkerEntry(DialectProfile.AR_LB, "هول", "هؤلاء", 5.5, false),
        MarkerEntry(DialectProfile.AR_LB, "عنجد", "حقاً / بجد", 5.5, false),
        MarkerEntry(DialectProfile.AR_LB, "شوي", "قليلاً", 3.0, false),
        MarkerEntry(DialectProfile.AR_LB, "تقبرني", "كلمة مودة (فديتك)", 5.8, false),
        MarkerEntry(DialectProfile.AR_LB, "ميرسي", "شكراً", 5.0, false),
        MarkerEntry(DialectProfile.AR_LB, "مهضوم", "لطيف وخفيف الظل", 5.8, false),

        // ==================== الأردنية والفلسطينية ====================
        MarkerEntry(DialectProfile.AR_JO_PS, "يا زلمة", "يا رجل / يا أخي", 6.5, true),
        MarkerEntry(DialectProfile.AR_JO_PS, "هسا", "الآن", 6.5, false),
        MarkerEntry(DialectProfile.AR_JO_PS, "ايش", "ماذا", 4.5, false),
        MarkerEntry(DialectProfile.AR_JO_PS, "قديش", "كم السعر / كم المقدار", 5.2, false),
        MarkerEntry(DialectProfile.AR_JO_PS, "اشي", "شيء", 5.8, false),
        MarkerEntry(DialectProfile.AR_JO_PS, "فش", "لا يوجد", 5.8, false),
        MarkerEntry(DialectProfile.AR_JO_PS, "مليح", "جيد / بخير", 3.8, false),
        MarkerEntry(DialectProfile.AR_JO_PS, "وينك", "أين أنت", 3.5, false),

        // ==================== المصرية ====================
        MarkerEntry(DialectProfile.AR_EG, "ازيك", "كيف حالك", 6.5, false),
        MarkerEntry(DialectProfile.AR_EG, "عامل ايه", "كيف أحوالك", 6.5, true),
        MarkerEntry(DialectProfile.AR_EG, "عاملة ايه", "كيف أحوالك", 6.5, true),
        MarkerEntry(DialectProfile.AR_EG, "عايز", "أريد", 6.0, false),
        MarkerEntry(DialectProfile.AR_EG, "عاوز", "أريد", 6.0, false),
        MarkerEntry(DialectProfile.AR_EG, "دلوقتي", "الآن", 6.8, false),
        MarkerEntry(DialectProfile.AR_EG, "كده", "هكذا", 6.0, false),
        MarkerEntry(DialectProfile.AR_EG, "كدا", "هكذا", 6.0, false),
        MarkerEntry(DialectProfile.AR_EG, "اوي", "جداً / كثيراً", 5.8, false),
        MarkerEntry(DialectProfile.AR_EG, "خالص", "تماماً / جداً", 4.8, false),
        MarkerEntry(DialectProfile.AR_EG, "عشان", "من أجل / لأن", 4.5, false),
        MarkerEntry(DialectProfile.AR_EG, "علشان", "من أجل / لأن", 5.2, false),
        MarkerEntry(DialectProfile.AR_EG, "ايه", "ماذا", 5.0, false),
        MarkerEntry(DialectProfile.AR_EG, "ليه", "لماذا", 4.8, false),
        MarkerEntry(DialectProfile.AR_EG, "ازاي", "كيف", 6.2, false),
        MarkerEntry(DialectProfile.AR_EG, "بكام", "كم السعر", 6.0, false),
        MarkerEntry(DialectProfile.AR_EG, "مفيش", "لا يوجد", 6.0, false),
        MarkerEntry(DialectProfile.AR_EG, "معرفش", "لا أعرف", 6.0, false),
        MarkerEntry(DialectProfile.AR_EG, "مقدرش", "لا أستطيع", 6.0, false),
        MarkerEntry(DialectProfile.AR_EG, "مش", "ليس / لا", 4.8, false),
        MarkerEntry(DialectProfile.AR_EG, "بتاع", "الخاص بـ", 6.0, false),
        MarkerEntry(DialectProfile.AR_EG, "حاجة", "شيء", 4.8, false),
        MarkerEntry(DialectProfile.AR_EG, "حاجات", "أشياء", 4.8, false),
        MarkerEntry(DialectProfile.AR_EG, "برضو", "أيضاً", 5.5, false),
        MarkerEntry(DialectProfile.AR_EG, "بدري", "مبكراً", 4.0, false),
        MarkerEntry(DialectProfile.AR_EG, "يا باشا", "يا أخي الكريم", 6.0, true),
        MarkerEntry(DialectProfile.AR_EG, "فكك", "دعك من هذا / انسَ الأمر", 6.0, false),
        MarkerEntry(DialectProfile.AR_EG, "ده", "هذا", 5.5, false),
        MarkerEntry(DialectProfile.AR_EG, "دي", "هذه", 4.8, false),
        MarkerEntry(DialectProfile.AR_EG, "دول", "هؤلاء", 5.2, false),
        MarkerEntry(DialectProfile.AR_EG, "بص", "انظر", 5.5, false),
        MarkerEntry(DialectProfile.AR_EG, "معلش", "لا بأس / عذراً", 4.8, false),

        // ==================== الخليجية والسعودية ====================
        MarkerEntry(DialectProfile.AR_GULF, "طال عمرك", "عبارة احترام وتقدير", 6.8, true),
        MarkerEntry(DialectProfile.AR_GULF, "وش علومك", "ما أخبارك وكيف حالك", 6.8, true),
        MarkerEntry(DialectProfile.AR_GULF, "شخبارك", "ما أخبارك", 5.8, false),
        MarkerEntry(DialectProfile.AR_GULF, "شحالك", "كيف حالك", 6.2, false),
        MarkerEntry(DialectProfile.AR_GULF, "اشحالك", "كيف حالك", 6.2, false),
        MarkerEntry(DialectProfile.AR_GULF, "ما قصرت", "شكراً جزيلاً لجهدك", 5.8, true),
        MarkerEntry(DialectProfile.AR_GULF, "ابي", "أريد", 5.8, false),
        MarkerEntry(DialectProfile.AR_GULF, "ابغى", "أريد", 6.5, false),
        MarkerEntry(DialectProfile.AR_GULF, "تبغى", "تريد", 6.5, false),
        MarkerEntry(DialectProfile.AR_GULF, "تبي", "تريد", 5.0, false),
        MarkerEntry(DialectProfile.AR_GULF, "الحين", "الآن", 5.5, false),
        MarkerEntry(DialectProfile.AR_GULF, "وش", "ماذا", 6.0, false),
        MarkerEntry(DialectProfile.AR_GULF, "ويش", "ماذا", 5.5, false),
        MarkerEntry(DialectProfile.AR_GULF, "وايد", "كثيراً جداً", 6.5, false),
        MarkerEntry(DialectProfile.AR_GULF, "واجد", "كثيراً", 5.8, false),
        MarkerEntry(DialectProfile.AR_GULF, "كذي", "هكذا", 6.0, false),
        MarkerEntry(DialectProfile.AR_GULF, "جذي", "هكذا", 6.2, false),
        MarkerEntry(DialectProfile.AR_GULF, "سوالف", "أحاديث / مواضيع", 5.8, false),
        MarkerEntry(DialectProfile.AR_GULF, "دوام", "عمل / وظيفة", 4.5, false),
        MarkerEntry(DialectProfile.AR_GULF, "ابشر", "حاضر بكل سرور", 5.2, false),
        MarkerEntry(DialectProfile.AR_GULF, "يا الغالي", "يا عزيزي", 5.5, true),
        MarkerEntry(DialectProfile.AR_GULF, "يمدي", "يوجد وقت كافٍ / يمكن", 6.0, false),
        MarkerEntry(DialectProfile.AR_GULF, "صج", "حقاً / صدقاً", 5.8, false),

        // ==================== العراقية ====================
        MarkerEntry(DialectProfile.AR_IQ, "شكو ماكو", "ما الأخبار وما الجديد", 7.5, true),
        MarkerEntry(DialectProfile.AR_IQ, "تاج راسي", "عبارة محبة واحترام كبيرة", 6.0, true),
        MarkerEntry(DialectProfile.AR_IQ, "هسة", "الآن", 6.8, false),
        MarkerEntry(DialectProfile.AR_IQ, "هسه", "الآن", 6.8, false),
        MarkerEntry(DialectProfile.AR_IQ, "شنو", "ماذا", 5.2, false),
        MarkerEntry(DialectProfile.AR_IQ, "ماكو", "لا يوجد", 7.0, false),
        MarkerEntry(DialectProfile.AR_IQ, "اكو", "يوجد", 6.5, false),
        MarkerEntry(DialectProfile.AR_IQ, "اريد", "أريد", 3.5, false),
        MarkerEntry(DialectProfile.AR_IQ, "تريد", "تريد", 3.2, false),
        MarkerEntry(DialectProfile.AR_IQ, "هواي", "كثيراً جداً", 7.0, false),
        MarkerEntry(DialectProfile.AR_IQ, "هواية", "كثيراً جداً", 6.8, false),
        MarkerEntry(DialectProfile.AR_IQ, "كلش", "جداً / للغاية", 7.0, false),
        MarkerEntry(DialectProfile.AR_IQ, "خوش", "ممتاز / جيد", 5.8, false),
        MarkerEntry(DialectProfile.AR_IQ, "خوية", "يا أخي", 6.8, false),
        MarkerEntry(DialectProfile.AR_IQ, "عيوني", "يا عزيزي", 4.8, false),
        MarkerEntry(DialectProfile.AR_IQ, "اغاتي", "يا سيدي العزيز", 7.0, false),
        MarkerEntry(DialectProfile.AR_IQ, "فدوة", "فداك / تسلم", 6.2, false),
        MarkerEntry(DialectProfile.AR_IQ, "عوف", "اترك", 6.5, false),
        MarkerEntry(DialectProfile.AR_IQ, "عوفني", "اتركني", 6.8, false),
        MarkerEntry(DialectProfile.AR_IQ, "وياي", "معي", 6.0, false),
        MarkerEntry(DialectProfile.AR_IQ, "وياك", "معك", 6.0, false),
        MarkerEntry(DialectProfile.AR_IQ, "شوكت", "متى", 6.8, false),
        MarkerEntry(DialectProfile.AR_IQ, "شكد", "كم", 6.8, false),
        MarkerEntry(DialectProfile.AR_IQ, "بيش", "بكم السعر", 6.2, false),
        MarkerEntry(DialectProfile.AR_IQ, "هيج", "هكذا", 6.5, false),
        MarkerEntry(DialectProfile.AR_IQ, "خاشوكة", "ملعقة", 7.0, false),
        MarkerEntry(DialectProfile.AR_IQ, "للسوك", "إلى السوق", 6.5, false),
        MarkerEntry(DialectProfile.AR_IQ, "ضوجة", "ملل وضيق", 6.8, false),

        // ==================== المغربية (الدارجة) ====================
        MarkerEntry(DialectProfile.AR_MA, "دابا", "الآن", 7.5, false),
        MarkerEntry(DialectProfile.AR_MA, "بغيت", "أريد", 7.0, false),
        MarkerEntry(DialectProfile.AR_MA, "بغيتي", "تريد", 7.0, false),
        MarkerEntry(DialectProfile.AR_MA, "بزاف", "كثيراً جداً", 5.8, false),
        MarkerEntry(DialectProfile.AR_MA, "مزيان", "جيد / ممتاز", 7.0, false),
        MarkerEntry(DialectProfile.AR_MA, "واخا", "حسناً / موافق", 7.5, false),
        MarkerEntry(DialectProfile.AR_MA, "صافي", "خلاص / تمام", 7.0, false),
        MarkerEntry(DialectProfile.AR_MA, "حيت", "لأن / بسبب", 7.0, false),
        MarkerEntry(DialectProfile.AR_MA, "ديال", "الخاص بـ", 7.0, false),
        MarkerEntry(DialectProfile.AR_MA, "ديالي", "الخاص بي (ملكي)", 7.0, false),
        MarkerEntry(DialectProfile.AR_MA, "ديالك", "الخاص بك", 7.0, false),
        MarkerEntry(DialectProfile.AR_MA, "علاش", "لماذا", 5.5, false),
        MarkerEntry(DialectProfile.AR_MA, "كيفاش", "كيف", 5.5, false),
        MarkerEntry(DialectProfile.AR_MA, "فوقاش", "متى", 7.0, false),
        MarkerEntry(DialectProfile.AR_MA, "بشحال", "كم السعر", 6.8, false),
        MarkerEntry(DialectProfile.AR_MA, "شكون", "مَن", 5.8, false),
        MarkerEntry(DialectProfile.AR_MA, "غادي", "سوف / ذاهب", 6.5, false),
        MarkerEntry(DialectProfile.AR_MA, "نمشي", "أذهب", 5.5, false),
        MarkerEntry(DialectProfile.AR_MA, "نشري", "أشتري", 5.8, false),
        MarkerEntry(DialectProfile.AR_MA, "حوايج", "ملابس / أغراض", 6.5, false),
        MarkerEntry(DialectProfile.AR_MA, "عييت", "تعبت", 6.8, false),
        MarkerEntry(DialectProfile.AR_MA, "دغيا", "بسرعة", 7.0, false),
        MarkerEntry(DialectProfile.AR_MA, "بلاتي", "انتظر لحظة", 7.0, false),
        MarkerEntry(DialectProfile.AR_MA, "زوين", "جميل", 7.0, false),
        MarkerEntry(DialectProfile.AR_MA, "كنهضر", "أتكلم", 7.2, false),
        MarkerEntry(DialectProfile.AR_MA, "ما بقاش", "لم يبقَ / لم يعد", 6.8, true),
        MarkerEntry(DialectProfile.AR_MA, "ما كاينش", "لا يوجد", 7.0, true),
        MarkerEntry(DialectProfile.AR_MA, "لباس عليك", "كيف حالك هل أنت بخير", 7.2, true),

        // ==================== الجزائرية ====================
        MarkerEntry(DialectProfile.AR_DZ, "واش راك", "كيف حالك", 7.5, true),
        MarkerEntry(DialectProfile.AR_DZ, "واش راكي", "كيف حالكِ", 7.5, true),
        MarkerEntry(DialectProfile.AR_DZ, "يعطيك الصحة", "شكراً جزيلاً لك", 6.5, true),
        MarkerEntry(DialectProfile.AR_DZ, "راني", "أنا الآن", 7.0, false),
        MarkerEntry(DialectProfile.AR_DZ, "راك", "أنت الآن", 6.5, false),
        MarkerEntry(DialectProfile.AR_DZ, "راهي", "هي الآن", 6.8, false),
        MarkerEntry(DialectProfile.AR_DZ, "درك", "الآن", 7.2, false),
        MarkerEntry(DialectProfile.AR_DZ, "ضرك", "الآن", 7.2, false),
        MarkerEntry(DialectProfile.AR_DZ, "بصح", "لكن / فعلاً", 7.0, false),
        MarkerEntry(DialectProfile.AR_DZ, "تاع", "الخاص بـ", 6.5, false),
        MarkerEntry(DialectProfile.AR_DZ, "نتاع", "الخاص بـ", 6.8, false),
        MarkerEntry(DialectProfile.AR_DZ, "علاه", "لماذا", 6.8, false),
        MarkerEntry(DialectProfile.AR_DZ, "وقتاش", "متى", 6.0, false),
        MarkerEntry(DialectProfile.AR_DZ, "شحال", "كم", 5.8, false),
        MarkerEntry(DialectProfile.AR_DZ, "ماكانش", "لا يوجد", 7.0, false),
        MarkerEntry(DialectProfile.AR_DZ, "ماكاش", "لا يوجد", 7.0, false),
        MarkerEntry(DialectProfile.AR_DZ, "برك", "فقط", 6.8, false),
        MarkerEntry(DialectProfile.AR_DZ, "صحيت", "شكراً لك", 6.8, false),
        MarkerEntry(DialectProfile.AR_DZ, "معمرة", "مزدحمة / ممتلئة", 5.8, false),
        MarkerEntry(DialectProfile.AR_DZ, "خويا", "يا أخي", 4.5, false),

        // ==================== التونسية ====================
        MarkerEntry(DialectProfile.AR_TN, "عسلامة", "مرحباً وأهلاً", 7.5, false),
        MarkerEntry(DialectProfile.AR_TN, "شنوة احوالك", "كيف حالك", 7.5, true),
        MarkerEntry(DialectProfile.AR_TN, "برشا", "كثيراً جداً", 7.5, false),
        MarkerEntry(DialectProfile.AR_TN, "باهي", "جيد / حسناً", 6.2, false),
        MarkerEntry(DialectProfile.AR_TN, "توا", "الآن", 6.2, false),
        MarkerEntry(DialectProfile.AR_TN, "شنوة", "ماذا", 7.0, false),
        MarkerEntry(DialectProfile.AR_TN, "شنية", "ما هي", 7.0, false),
        MarkerEntry(DialectProfile.AR_TN, "يعيشك", "شكراً / الله يحفظك", 7.2, false),
        MarkerEntry(DialectProfile.AR_TN, "عيشك", "من فضلك / شكراً", 7.0, false),
        MarkerEntry(DialectProfile.AR_TN, "فيسع", "بسرعة", 7.2, false),
        MarkerEntry(DialectProfile.AR_TN, "فما", "يوجد", 6.8, false),
        MarkerEntry(DialectProfile.AR_TN, "ما فماش", "لا يوجد", 7.5, true),
        MarkerEntry(DialectProfile.AR_TN, "متاع", "الخاص بـ", 6.8, false),
        MarkerEntry(DialectProfile.AR_TN, "قداش", "كم", 6.0, false),

        // ==================== الليبية ====================
        MarkerEntry(DialectProfile.AR_LY, "شن الجو", "كيف الحال وما الأخبار", 7.5, true),
        MarkerEntry(DialectProfile.AR_LY, "هلبا", "كثيراً جداً", 7.8, false),
        MarkerEntry(DialectProfile.AR_LY, "نبي", "أريد", 6.5, false),
        MarkerEntry(DialectProfile.AR_LY, "خيرك", "ما بك / ماذا هناك", 6.8, false),
        MarkerEntry(DialectProfile.AR_LY, "شني", "ماذا", 6.8, false),
        MarkerEntry(DialectProfile.AR_LY, "امالا", "إذن / طبعاً", 6.2, false),

        // ==================== اليمنية ====================
        MarkerEntry(DialectProfile.AR_YE, "حيا الله", "أهلاً وسهلاً ومرحباً", 6.0, true),
        MarkerEntry(DialectProfile.AR_YE, "ما اشتيش", "لا أريد", 7.8, true),
        MarkerEntry(DialectProfile.AR_YE, "اشتي", "أريد", 7.8, false),
        MarkerEntry(DialectProfile.AR_YE, "تشتي", "تريد", 7.8, false),
        MarkerEntry(DialectProfile.AR_YE, "ذلحين", "الآن", 7.0, false),
        MarkerEntry(DialectProfile.AR_YE, "ما بش", "لا يوجد", 7.5, true),
        MarkerEntry(DialectProfile.AR_YE, "يا خبير", "يا صديقي / يا أخي", 7.2, true),
        MarkerEntry(DialectProfile.AR_YE, "قوي", "جداً / كثيراً", 5.2, false),
        MarkerEntry(DialectProfile.AR_YE, "لمه", "لماذا", 6.5, false),
        MarkerEntry(DialectProfile.AR_YE, "حالي", "جميل / جيد", 5.0, false),

        // ==================== السودانية ====================
        MarkerEntry(DialectProfile.AR_SD, "يا زول", "يا صديقي / يا رجل", 7.8, true),
        MarkerEntry(DialectProfile.AR_SD, "حبابك", "أهلاً ومرحباً بك", 7.2, false),
        MarkerEntry(DialectProfile.AR_SD, "داير", "أريد / تريد", 7.2, false),
        MarkerEntry(DialectProfile.AR_SD, "دايرة", "تريد", 7.0, false),
        MarkerEntry(DialectProfile.AR_SD, "هسي", "الآن", 7.2, false),
        MarkerEntry(DialectProfile.AR_SD, "حسي", "الآن", 6.8, false),
        MarkerEntry(DialectProfile.AR_SD, "شديد", "جداً / كثيراً", 6.2, false),
        MarkerEntry(DialectProfile.AR_SD, "سمح", "جميل / جيد جداً", 7.0, false),
        MarkerEntry(DialectProfile.AR_SD, "ما في عوجة", "كل شيء بخير ولا مشكلة", 7.8, true),
        MarkerEntry(DialectProfile.AR_SD, "قوام", "بسرعة", 6.5, false),
        MarkerEntry(DialectProfile.AR_SD, "متين", "متى", 6.2, false),
    )

    fun detectDialect(text: String): DialectDetectionResult {
        val norm = normalizeForMatch(text)
        if (norm.isBlank()) {
            return DialectDetectionResult(DialectProfile.AR_SIMPLE, 80, emptyList(), text)
        }
        val tokens = norm.split(" ").filter { it.isNotBlank() }.toSet()
        val scores = mutableMapOf<DialectProfile, Double>()
        val matchedNotes = LinkedHashMap<String, String>()

        for (entry in markers) {
            val normPat = normalizeForMatch(entry.pattern)
            val matched = if (entry.isPhrase || normPat.contains(" ")) {
                norm.contains(normPat)
            } else {
                tokens.contains(normPat)
            }
            if (matched) {
                scores[entry.dialect] = (scores[entry.dialect] ?: 0.0) + entry.weight
                if (matchedNotes.size < 6 && !matchedNotes.containsKey(entry.pattern)) {
                    matchedNotes[entry.pattern] = entry.clearMeaning
                }
            }
        }

        // قواعد صرفية وصوتية إضافية لتعزيز دقة الكشف التلقائي
        if (text.contains('چ') || text.contains('گ')) {
            scores[DialectProfile.AR_IQ] = (scores[DialectProfile.AR_IQ] ?: 0.0) + 6.0
        }
        if (Regex("\\bعم\\s+[بيتنأ]").containsMatchIn(norm)) {
            scores[DialectProfile.AR_SY] = (scores[DialectProfile.AR_SY] ?: 0.0) + 5.0
        }
        if (Regex("\\bكن[مشهدفعكتب]").containsMatchIn(norm) || Regex("\\bكت[مشهدفعكتب]").containsMatchIn(norm)) {
            scores[DialectProfile.AR_MA] = (scores[DialectProfile.AR_MA] ?: 0.0) + 5.5
        }
        if (Regex("\\bبي[عملقولشتغلروححب]").containsMatchIn(norm)) {
            scores[DialectProfile.AR_EG] = (scores[DialectProfile.AR_EG] ?: 0.0) + 4.5
        }
        if (Regex("\\b(دا|جاي|كاعد)\\s+[اينت]").containsMatchIn(norm)) {
            scores[DialectProfile.AR_IQ] = (scores[DialectProfile.AR_IQ] ?: 0.0) + 5.0
        }

        val best = scores.maxByOrNull { it.value }
        val clearMeaning = normalizeToClearArabic(text)
        val notesList = matchedNotes.map { SlangNote(it.key, it.value) }

        return if (best == null || best.value < 2.5) {
            DialectDetectionResult(
                profile = DialectProfile.AR_SIMPLE,
                confidence = 86,
                notes = notesList,
                clearArabicMeaning = clearMeaning,
            )
        } else {
            val totalScore = scores.values.sum().coerceAtLeast(best.value)
            val ratio = (best.value / totalScore).coerceIn(0.45, 1.0)
            val conf = (74 + (ratio * 24) + (best.value.coerceAtMost(15.0) * 0.25))
                .toInt()
                .coerceIn(78, 99)
            DialectDetectionResult(
                profile = best.key,
                confidence = conf,
                notes = notesList,
                clearArabicMeaning = clearMeaning,
            )
        }
    }

    // عبارات مركبة لتوحيد اللهجات إلى العربية المبسطة المفهومة للجميع
    private val phraseToClearRules: List<Pair<Regex, String>> = listOf(
        Regex("شكو\\s+ماكو") to "ما الأخبار",
        Regex("واش\\s+راك") to "كيف حالك",
        Regex("واش\\s+راكي") to "كيف حالكِ",
        Regex("شن\\s+الجو") to "كيف الحال",
        Regex("شنوة\\s+احوالك|شنو\\s+احوالك") to "كيف حالك",
        Regex("لباس\\s+عليك") to "هل أنت بخير",
        Regex("عامل\\s+ايه|عاملة\\s+ايه|عامل\\s+إيه|عاملة\\s+إيه") to "كيف حالك",
        Regex("وش\\s+علومك") to "ما أخبارك",
        Regex("شو\\s+الاخبار|شو\\s+الأخبار") to "ما الأخبار",
        Regex("طال\\s+عمرك") to "حفظك الله",
        Regex("ما\\s+قصرت") to "شكراً جزيلاً لك",
        Regex("يعطيك\\s+العافية|يعطيك\\s+الصحة") to "الله يعطيك العافية وشكراً لك",
        Regex("تكرم\\s+عينك") to "بكل سرور",
        Regex("ما\\s+في\\s+عوجة") to "كل شيء بخير ولا توجد مشكلة",
        Regex("يا\\s+زلمة|يا\\s+زول|يا\\s+باشا|يا\\s+خبير|يا\\s+معلم") to "يا أخي",
        Regex("ما\\s+اشتيش|ما\\s+أشتيش") to "لا أريد",
        Regex("ما\\s+بقاش|ما\\s+بقيتش|ما\\s+عاد") to "لم يعد",
        Regex("ما\\s+كاينش|ما\\s+فماش|ما\\s+بش") to "لا يوجد",
        Regex("ما\\s+فيني") to "لا أستطيع",
        Regex("شو\\s+بدك\\s+تساوي|شو\\s+بدك\\s+تعمل") to "ماذا تريد أن تفعل",
        Regex("عم\\s+بحكي|كنهضر|دا\\s+احجي") to "أتكلم الآن",
        Regex("بغيت\\s+نمشي") to "أريد أن أذهب",
        Regex("بدي\\s+روح|بدي\\s+اروح") to "أريد أن أذهب",
        Regex("عايز\\s+اروح|عايز\\s+أروح|عاوز\\s+اروح") to "أريد أن أذهب",
        Regex("ابي\\s+اروح|أبي\\s+أروح|ابغى\\s+اروح|أبغى\\s+أروح") to "أريد أن أذهب",
        Regex("اريد\\s+اروح|أريد\\s+أروح") to "أريد أن أذهب",
    )

    private val wordToClearMap: Map<String, String> = mapOf(
        // استفهام
        "شو" to "ماذا", "شنو" to "ماذا", "وش" to "ماذا", "ويش" to "ماذا",
        "ايش" to "ماذا", "إيش" to "ماذا", "شنوة" to "ماذا", "شني" to "ماذا",
        "ايه" to "ماذا", "إيه" to "ماذا",
        "ليش" to "لماذا", "ليه" to "لماذا", "علاش" to "لماذا", "علاه" to "لماذا", "لمه" to "لماذا",
        "وين" to "أين", "فين" to "أين", "وينك" to "أين أنت",
        "ايمتى" to "متى", "امتى" to "متى", "إمتى" to "متى", "فوقاش" to "متى",
        "وقتاش" to "متى", "شوكت" to "متى", "متين" to "متى",
        "كيفاش" to "كيف", "ازاي" to "كيف", "إزاي" to "كيف", "شلون" to "كيف",
        "شلونك" to "كيف حالك", "ازيك" to "كيف حالك", "إزيك" to "كيف حالك",
        "شخبارك" to "ما أخبارك", "شحالك" to "كيف حالك", "كيفك" to "كيف حالك",
        "قديش" to "كم", "قداش" to "كم", "بشحال" to "بكم", "شحال" to "كم",
        "شكد" to "كم", "بكام" to "بكم", "بيش" to "بكم", "شكون" to "مَن",

        // زمان ومقدار
        "هلا" to "الآن", "هلأ" to "الآن", "هلق" to "الآن", "هسا" to "الآن",
        "هسة" to "الآن", "هسه" to "الآن", "دلوقتي" to "الآن", "الحين" to "الآن",
        "ذلحين" to "الآن", "دابا" to "الآن", "درك" to "الآن", "ضرك" to "الآن",
        "توا" to "الآن", "هسي" to "الآن", "حسي" to "الآن",
        "كتير" to "كثيراً", "بزاف" to "كثيراً", "برشا" to "كثيراً", "هلبا" to "كثيراً",
        "هواي" to "كثيراً", "هواية" to "كثيراً", "وايد" to "كثيراً", "واجد" to "كثيراً",
        "كلش" to "جداً", "اوي" to "جداً", "أوي" to "جداً", "شديد" to "جداً", "قوي" to "جداً",
        "شوي" to "قليلاً", "شوية" to "قليلاً",
        "بكير" to "مبكراً", "بدري" to "مبكراً",
        "دغري" to "مباشرةً", "دغيا" to "بسرعة", "فيسع" to "بسرعة", "قوام" to "بسرعة",

        // أفعال ورغبات شائعة
        "بدي" to "أريد", "بدك" to "تريد", "بدو" to "يريد", "بدنا" to "نريد",
        "عايز" to "أريد", "عاوز" to "أريد", "بغيت" to "أريد", "بغيتي" to "تريد",
        "ابي" to "أريد", "أبي" to "أريد", "ابغى" to "أريد", "أبغى" to "أريد",
        "تبغى" to "تريد", "نبي" to "أريد", "اشتي" to "أريد", "أشتي" to "أريد",
        "تشتي" to "تريد", "داير" to "أريد",
        "فيني" to "أستطيع", "فينا" to "يمكننا",
        "نمشي" to "أذهب", "نشري" to "أشتري", "تساوي" to "تفعل",
        "عييت" to "تعبت", "عوف" to "اترك", "عوفني" to "اتركني",
        "استناني" to "انتظرني", "بلاتي" to "انتظر لحظة",

        // نفي ووجود وروابط
        "ماكو" to "لا يوجد", "مفيش" to "لا يوجد", "فش" to "لا يوجد",
        "ماكانش" to "لا يوجد", "ماكاش" to "لا يوجد",
        "اكو" to "يوجد", "أكو" to "يوجد", "فما" to "يوجد",
        "معرفش" to "لا أعرف", "مقدرش" to "لا أستطيع", "مش" to "ليس",
        "مشان" to "لكي", "كرمال" to "من أجل", "عشان" to "لأن", "علشان" to "لأن",
        "حيت" to "لأن", "بصح" to "لكن", "برك" to "فقط",
        "ديال" to "الخاص بـ", "ديالي" to "لي", "ديالك" to "لك",
        "بتاع" to "الخاص بـ", "تاع" to "الخاص بـ", "متاع" to "الخاص بـ",
        "وياي" to "معي", "وياك" to "معك",
        "غادي" to "سوف", "راني" to "أنا", "راك" to "أنت", "راهي" to "هي",

        // صفات وأسماء شائعة
        "منيح" to "جيد", "منيحة" to "جيدة", "مليح" to "جيد", "مزيان" to "جيد",
        "باهي" to "جيد", "زين" to "جيد", "خوش" to "ممتاز", "سمح" to "جميل", "زوين" to "جميل",
        "واخا" to "حسناً", "صافي" to "حسناً تمام", "ابشر" to "حاضر بكل سرور", "أبشر" to "حاضر بكل سرور",
        "عسلامة" to "مرحباً", "حبابك" to "أهلاً بك", "صحيت" to "شكراً لك", "يعيشك" to "شكراً لك",
        "خويا" to "يا أخي", "خوية" to "يا أخي",
        "هاد" to "هذا", "هيدا" to "هذا", "ده" to "هذا", "هيدي" to "هذه", "دي" to "هذه",
        "هدول" to "هؤلاء", "دول" to "هؤلاء",
        "هيك" to "هكذا", "كده" to "هكذا", "كدا" to "هكذا", "كذي" to "هكذا", "جذي" to "هكذا", "هيج" to "هكذا",
        "غراض" to "أغراض", "حوايج" to "أغراض", "حاجة" to "شيء", "حاجات" to "أشياء", "اشي" to "شيء",
        "مصاري" to "نقود", "عالسوق" to "إلى السوق", "للسوك" to "إلى السوق", "عالبيت" to "إلى البيت",
        "معمرة" to "مزدحمة", "خاشوكة" to "ملعقة", "بركي" to "ربما", "بلكي" to "ربما",
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
            if (direct != null) {
                "$prefixPunct$direct$suffixPunct"
            } else {
                rawWord
            }
        }
        return mapped.joinToString(" ").replace(Regex("\\s+"), " ").trim()
    }

    /**
     * يحوّل أي جملة عربية إلى اللهجة العربية المطلوبة بأسلوب طبيعي وسلس.
     */
    fun convertToArabicDialect(input: String, target: DialectProfile): String {
        val clear = normalizeToClearArabic(input)
        if (target == DialectProfile.AR_SIMPLE || target == DialectProfile.AUTO_DETECT) {
            return clear
        }

        val rules: List<Pair<Regex, String>> = when (target) {
            DialectProfile.AR_SY -> listOf(
                Regex("كيف حالك") to "شلونك",
                Regex("ما الأخبار|ما أخبارك") to "شو الأخبار",
                Regex("ماذا تريد أن تفعل") to "شو بدك تساوي",
                Regex("أريد أن أذهب") to "بدي روح",
                Regex("إلى السوق") to "عالسوق",
                Regex("إلى البيت") to "عالبيت",
                Regex("لا يوجد") to "ما في",
                Regex("لا أستطيع") to "ما فيني",
                Regex("لا أريد") to "ما بدي",
                Regex("من أجل|لكي") to "مشان",
                Regex("\\bماذا\\b") to "شو",
                Regex("\\bلماذا\\b") to "ليش",
                Regex("\\bأين\\b") to "وين",
                Regex("\\bمتى\\b") to "إيمتى",
                Regex("\\bكم\\b") to "قديش",
                Regex("\\bالآن\\b") to "هلأ",
                Regex("\\bأريد\\b") to "بدي",
                Regex("\\bتريد\\b") to "بدك",
                Regex("\\bنريد\\b") to "بدنا",
                Regex("\\bكثيراً\\b|\\bجداً\\b") to "كتير",
                Regex("\\bقليلاً\\b") to "شوي",
                Regex("\\bمبكراً\\b") to "بكير",
                Regex("\\bمباشرةً\\b|\\bبسرعة\\b") to "دغري",
                Regex("\\bجيد\\b|\\bممتاز\\b") to "منيح",
                Regex("\\bهذا\\b") to "هاد",
                Regex("\\bهكذا\\b") to "هيك",
                Regex("\\bربما\\b") to "بركي",
                Regex("\\bحسناً\\b") to "تمام تكرم عينك",
            )

            DialectProfile.AR_LB -> listOf(
                Regex("كيف حالك") to "كيفك",
                Regex("ما الأخبار|ما أخبارك") to "شو الأخبار",
                Regex("أريد أن أذهب") to "بدي روح",
                Regex("لا يوجد") to "ما في",
                Regex("لا أستطيع") to "ما فيي",
                Regex("\\bماذا\\b") to "شو",
                Regex("\\bلماذا\\b") to "ليش",
                Regex("\\bأين\\b") to "وين",
                Regex("\\bالآن\\b") to "هلق",
                Regex("\\bأريد\\b") to "بدي",
                Regex("\\bتريد\\b") to "بدك",
                Regex("\\bكثيراً\\b|\\bجداً\\b") to "كتير",
                Regex("\\bجيد\\b") to "منيح",
                Regex("\\bهذا\\b") to "هيدا",
                Regex("\\bهذه\\b") to "هيدي",
                Regex("\\bهكذا\\b") to "هيك",
            )

            DialectProfile.AR_JO_PS -> listOf(
                Regex("كيف حالك") to "كيفك يا زلمة",
                Regex("ما الأخبار|ما أخبارك") to "شو الأخبار",
                Regex("أريد أن أذهب") to "بدي أروح",
                Regex("لا يوجد") to "ما في",
                Regex("\\bماذا\\b") to "إيش",
                Regex("\\bلماذا\\b") to "ليش",
                Regex("\\bأين\\b") to "وين",
                Regex("\\bكم\\b") to "قديش",
                Regex("\\bالآن\\b") to "هسا",
                Regex("\\bأريد\\b") to "بدي",
                Regex("\\bتريد\\b") to "بدك",
                Regex("\\bكثيراً\\b|\\bجداً\\b") to "كثير",
                Regex("\\bجيد\\b") to "مليح",
                Regex("\\bشيء\\b") to "إشي",
                Regex("\\bهكذا\\b") to "هيك",
            )

            DialectProfile.AR_EG -> listOf(
                Regex("كيف حالك") to "إزيك عامل إيه",
                Regex("ما الأخبار|ما أخبارك") to "إيه الأخبار",
                Regex("أريد أن أذهب") to "عايز أروح",
                Regex("لا يوجد") to "مفيش",
                Regex("لا أستطيع") to "مقدرش",
                Regex("لا أعرف") to "معرفش",
                Regex("لا أريد") to "مش عايز",
                Regex("من أجل|لكي|لأن") to "عشان",
                Regex("\\bماذا\\b") to "إيه",
                Regex("\\bلماذا\\b") to "ليه",
                Regex("\\bأين\\b") to "فين",
                Regex("\\bكيف\\b") to "إزاي",
                Regex("\\bبكم\\b") to "بكام",
                Regex("\\bالآن\\b") to "دلوقتي",
                Regex("\\bأريد\\b") to "عايز",
                Regex("\\bتريد\\b") to "عايز",
                Regex("\\bكثيراً\\b|\\bجداً\\b") to "أوي",
                Regex("\\bقليلاً\\b") to "شوية",
                Regex("\\bمبكراً\\b") to "بدري",
                Regex("\\bجيد\\b|\\bممتاز\\b") to "تمام أوي",
                Regex("\\bهذا\\b") to "ده",
                Regex("\\bهذه\\b") to "دي",
                Regex("\\bهكذا\\b") to "كده",
                Regex("\\bشيء\\b") to "حاجة",
                Regex("\\bأغراض\\b") to "حاجات",
            )

            DialectProfile.AR_GULF -> listOf(
                Regex("كيف حالك") to "شخبارك عساك بخير",
                Regex("ما الأخبار|ما أخبارك") to "وش علومك",
                Regex("أريد أن أذهب") to "أبي أروح",
                Regex("لا يوجد") to "ما فيه",
                Regex("لا أريد") to "ما أبي",
                Regex("\\bماذا\\b") to "وش",
                Regex("\\bلماذا\\b") to "ليش",
                Regex("\\bأين\\b") to "وين",
                Regex("\\bكيف\\b") to "شلون",
                Regex("\\bالآن\\b") to "الحين",
                Regex("\\bأريد\\b") to "أبغى",
                Regex("\\bتريد\\b") to "تبغى",
                Regex("\\bكثيراً\\b|\\bجداً\\b") to "وايد",
                Regex("\\bقليلاً\\b") to "شوي",
                Regex("\\bجيد\\b|\\bممتاز\\b") to "زين",
                Regex("\\bهكذا\\b") to "كذا",
                Regex("\\bحسناً\\b") to "أبشر طال عمرك",
            )

            DialectProfile.AR_IQ -> listOf(
                Regex("كيف حالك") to "شلونك عيوني",
                Regex("ما الأخبار|ما أخبارك") to "شكو ماكو",
                Regex("أريد أن أذهب") to "أريد أروح",
                Regex("إلى السوق") to "للسوك",
                Regex("لا يوجد") to "ماكو",
                Regex("يوجد") to "أكو",
                Regex("معي") to "وياي",
                Regex("معك") to "وياك",
                Regex("\\bماذا\\b") to "شنو",
                Regex("\\bلماذا\\b") to "ليش",
                Regex("\\bأين\\b") to "وين",
                Regex("\\bمتى\\b") to "شوكت",
                Regex("\\bكم\\b") to "شكد",
                Regex("\\bبكم\\b") to "بيش",
                Regex("\\bالآن\\b") to "هسة",
                Regex("\\bكثيراً\\b") to "هواي",
                Regex("\\bجداً\\b") to "كلش",
                Regex("\\bجيد\\b|\\bممتاز\\b") to "خوش زين",
                Regex("\\bهكذا\\b") to "هيج",
                Regex("\\bيا أخي\\b") to "خوية",
            )

            DialectProfile.AR_MA -> listOf(
                Regex("كيف حالك") to "كيداير لباس عليك",
                Regex("ما الأخبار|ما أخبارك") to "أشنو خبارك",
                Regex("أريد أن أذهب") to "بغيت نمشي",
                Regex("أشتري") to "نشري",
                Regex("لا يوجد") to "ما كاينش",
                Regex("لا أريد") to "ما بغيتش",
                Regex("تعبت") to "عييت",
                Regex("\\bلأن\\b") to "حيت",
                Regex("\\bماذا\\b") to "شنو",
                Regex("\\bلماذا\\b") to "علاش",
                Regex("\\bأين\\b") to "فين",
                Regex("\\bمتى\\b") to "فوقاش",
                Regex("\\bكيف\\b") to "كيفاش",
                Regex("\\bبكم\\b") to "بشحال",
                Regex("\\bالآن\\b") to "دابا",
                Regex("\\bأريد\\b") to "بغيت",
                Regex("\\bتريد\\b") to "بغيتي",
                Regex("\\bكثيراً\\b|\\bجداً\\b") to "بزاف",
                Regex("\\bبسرعة\\b") to "دغيا",
                Regex("\\bجيد\\b|\\bممتاز\\b") to "مزيان",
                Regex("\\bحسناً\\b") to "واخا صافي",
                Regex("\\bأغراض\\b") to "حوايج",
                Regex("\\bيا أخي\\b") to "خويا",
            )

            DialectProfile.AR_DZ -> listOf(
                Regex("كيف حالك") to "واش راك خويا",
                Regex("ما الأخبار|ما أخبارك") to "واش الأخبار",
                Regex("أريد أن أذهب") to "راني حاب نروح",
                Regex("لا يوجد") to "ماكانش",
                Regex("\\bلكن\\b") to "بصح",
                Regex("\\bفقط\\b") to "برك",
                Regex("\\bماذا\\b") to "واش",
                Regex("\\bلماذا\\b") to "علاه",
                Regex("\\bأين\\b") to "وين",
                Regex("\\bمتى\\b") to "وقتاش",
                Regex("\\bكم\\b") to "شحال",
                Regex("\\bالآن\\b") to "درك",
                Regex("\\bأريد\\b") to "حبيت",
                Regex("\\bكثيراً\\b|\\bجداً\\b") to "بزاف",
                Regex("\\bجيد\\b|\\bممتاز\\b") to "مليح",
                Regex("شكراً لك|شكراً جزيلاً لك") to "يعطيك الصحة",
            )

            DialectProfile.AR_TN -> listOf(
                Regex("مرحباً") to "عسلامة",
                Regex("كيف حالك") to "شنوة أحوالك",
                Regex("أريد أن أذهب") to "نحب نمشي",
                Regex("لا يوجد") to "ما فماش",
                Regex("\\bيوجد\\b") to "فما",
                Regex("\\bماذا\\b") to "شنوة",
                Regex("\\bلماذا\\b") to "علاش",
                Regex("\\bأين\\b") to "وين",
                Regex("\\bكم\\b") to "قداش",
                Regex("\\bالآن\\b") to "توا",
                Regex("\\bأريد\\b") to "نحب",
                Regex("\\bكثيراً\\b|\\bجداً\\b") to "برشا",
                Regex("\\bبسرعة\\b") to "فيسع",
                Regex("\\bجيد\\b|\\bحسناً\\b") to "باهي",
                Regex("شكراً لك|من فضلك") to "يعيشك",
            )

            DialectProfile.AR_LY -> listOf(
                Regex("كيف حالك|ما الأخبار") to "شن الجو",
                Regex("أريد أن أذهب") to "نبي نمشي",
                Regex("لا يوجد") to "ما فيش",
                Regex("\\bماذا\\b") to "شني",
                Regex("\\bلماذا\\b") to "علاش",
                Regex("\\bأين\\b") to "وين",
                Regex("\\bالآن\\b") to "توا",
                Regex("\\bأريد\\b") to "نبي",
                Regex("\\bكثيراً\\b|\\bجداً\\b") to "هلبا",
                Regex("\\bجيد\\b|\\bحسناً\\b") to "باهي",
            )

            DialectProfile.AR_YE -> listOf(
                Regex("كيف حالك") to "كيف حالك حيا الله",
                Regex("أريد أن أذهب") to "أشتي أسير",
                Regex("لا أريد") to "ما أشتيش",
                Regex("لا يوجد") to "ما بش",
                Regex("\\bلماذا\\b") to "لمه",
                Regex("\\bالآن\\b") to "ذلحين",
                Regex("\\bأريد\\b") to "أشتي",
                Regex("\\bتريد\\b") to "تشتي",
                Regex("\\bكثيراً\\b|\\bجداً\\b") to "قوي",
                Regex("\\bجيد\\b") to "حالي ومليح",
                Regex("\\bيا أخي\\b") to "يا خبير",
            )

            DialectProfile.AR_SD -> listOf(
                Regex("كيف حالك") to "حبابك كيفك يا زول",
                Regex("أريد أن أذهب") to "داير أمشي",
                Regex("\\bماذا\\b") to "شنو",
                Regex("\\bمتى\\b") to "متين",
                Regex("\\bالآن\\b") to "هسي",
                Regex("\\bأريد\\b") to "داير",
                Regex("\\bتريد\\b") to "داير",
                Regex("\\bكثيراً\\b|\\bجداً\\b") to "شديد",
                Regex("\\bبسرعة\\b") to "قوام",
                Regex("\\bجيد\\b|\\bجميل\\b") to "سمح شديد",
                Regex("\\bيا أخي\\b") to "يا زول",
            )

            else -> emptyList()
        }

        var out = clear
        for ((regex, rep) in rules) {
            out = out.replace(regex, rep)
        }
        return out.replace(Regex("\\s+"), " ").trim()
    }

    /**
     * يعرض نفس الجملة محوّلة إلى 10 لهجات عربية دفعة واحدة للمقارنة السريعة والاستماع.
     */
    fun compareAllDialects(input: String): List<DialectComparisonItem> {
        val showcaseDialects = listOf(
            DialectProfile.AR_SIMPLE,
            DialectProfile.AR_SY,
            DialectProfile.AR_EG,
            DialectProfile.AR_GULF,
            DialectProfile.AR_IQ,
            DialectProfile.AR_MA,
            DialectProfile.AR_DZ,
            DialectProfile.AR_TN,
            DialectProfile.AR_YE,
            DialectProfile.AR_SD,
        )
        return showcaseDialects.map { dialect ->
            DialectComparisonItem(
                dialect = dialect,
                text = convertToArabicDialect(input, dialect),
            )
        }
    }
}

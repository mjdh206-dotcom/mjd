package com.amjad.fahimni.data

const val DEVELOPER_ATTRIBUTION = "تم تطوير وتنفيذ هذا التطبيق بواسطة.امجد حبيب"

enum class SpeakerSide {
    ME,      // أنا أتكلم بلهجتي -> يفهمني الطرف المقابل
    PARTNER  // الطرف المقابل يتكلم -> يكتشف البرنامج لهجته تلقائياً وأفهمه أنا بالصوت
}

enum class DialectProfile(
    val id: String,
    val nameAr: String,
    val shortNameAr: String,
    val regionHint: String,
    val flag: String,
    val isArabicDialect: Boolean,
    val speechLocale: String,
    val googleIso: String,
    val ttsLang: String,
    val aiStylePrompt: String,
) {
    AUTO_DETECT(
        id = "auto",
        nameAr = "كشف تلقائي ذكي للهجة",
        shortNameAr = "تلقائي ذكي",
        regionHint = "يكتشف لهجة ولغة المتحدث تلقائياً",
        flag = "🎯",
        isArabicDialect = true,
        speechLocale = "ar",
        googleIso = "auto",
        ttsLang = "ar",
        aiStylePrompt = "العربية المبسطة الواضحة المفهومة لكل العرب"
    ),
    AR_SIMPLE(
        id = "ar_simple",
        nameAr = "العربية المبسطة (مفهومة للجميع)",
        shortNameAr = "عربي مبسط",
        regionHint = "لهجة بيضاء واضحة وسهلة لكل العرب",
        flag = "🌍",
        isArabicDialect = true,
        speechLocale = "ar-SA",
        googleIso = "ar",
        ttsLang = "ar",
        aiStylePrompt = "عربية بيضاء مبسطة وطبيعية جداً يفهمها أي عربي فوراً بدون تعقيد"
    ),
    AR_SY(
        id = "ar_sy",
        nameAr = "اللهجة الشامية (السورية)",
        shortNameAr = "شامية سورية",
        regionHint = "دمشق · حلب · حمص · الساحل",
        flag = "🇸🇾",
        isArabicDialect = true,
        speechLocale = "ar-SY",
        googleIso = "ar",
        ttsLang = "ar",
        aiStylePrompt = "اللهجة السورية الشامية اليومية الطبيعية (مثل: هلأ، شو، بدي، كتير، منيح، كرمال)"
    ),
    AR_LB(
        id = "ar_lb",
        nameAr = "اللهجة اللبنانية",
        shortNameAr = "لبنانية",
        regionHint = "بيروت · طرابلس · جبل لبنان",
        flag = "🇱🇧",
        isArabicDialect = true,
        speechLocale = "ar-LB",
        googleIso = "ar",
        ttsLang = "ar",
        aiStylePrompt = "اللهجة اللبنانية الطبيعية المحكية (مثل: هلق، شو، بدي، كتير، هيدا، عنجد)"
    ),
    AR_JO_PS(
        id = "ar_jo_ps",
        nameAr = "اللهجة الأردنية / الفلسطينية",
        shortNameAr = "أردنية فلسطينية",
        regionHint = "عمّان · القدس · نابلس · إربد",
        flag = "🇯🇴",
        isArabicDialect = true,
        speechLocale = "ar-JO",
        googleIso = "ar",
        ttsLang = "ar",
        aiStylePrompt = "اللهجة الأردنية والفلسطينية المحكية الطبيعية (مثل: هسا، إيش، بدي، كثير، يا زلمة، قديش)"
    ),
    AR_EG(
        id = "ar_eg",
        nameAr = "اللهجة المصرية",
        shortNameAr = "مصرية",
        regionHint = "القاهرة · الإسكندرية · الصعيد",
        flag = "🇪🇬",
        isArabicDialect = true,
        speechLocale = "ar-EG",
        googleIso = "ar",
        ttsLang = "ar",
        aiStylePrompt = "اللهجة المصرية العامية الطبيعية (مثل: دلوقتي، عايز، إزيك، أوي، كده، عشان)"
    ),
    AR_GULF(
        id = "ar_gulf",
        nameAr = "اللهجة الخليجية / السعودية",
        shortNameAr = "خليجية سعودية",
        regionHint = "السعودية · الإمارات · الكويت · قطر",
        flag = "🇸🇦",
        isArabicDialect = true,
        speechLocale = "ar-SA",
        googleIso = "ar",
        ttsLang = "ar",
        aiStylePrompt = "اللهجة الخليجية والسعودية الطبيعية (مثل: الحين، أبي/أبغى، وش، شلونك، وايد/مرة، زين)"
    ),
    AR_IQ(
        id = "ar_iq",
        nameAr = "اللهجة العراقية",
        shortNameAr = "عراقية",
        regionHint = "بغداد · البصرة · الموصل",
        flag = "🇮🇶",
        isArabicDialect = true,
        speechLocale = "ar-IQ",
        googleIso = "ar",
        ttsLang = "ar",
        aiStylePrompt = "اللهجة العراقية البغدادية المحكية الطبيعية (مثل: هسة، شنو، أريد، هواي، كلش، شلونك، ماكو)"
    ),
    AR_MA(
        id = "ar_ma",
        nameAr = "اللهجة المغربية (الدارجة)",
        shortNameAr = "مغربية دارجة",
        regionHint = "الدار البيضاء · الرباط · مراكش · طنجة",
        flag = "🇲🇦",
        isArabicDialect = true,
        speechLocale = "ar-MA",
        googleIso = "ar",
        ttsLang = "ar",
        aiStylePrompt = "الدارجة المغربية الأصيلة المفهومة للمغاربة (مثل: دابا، بغيت، بزاف، مزيان، علاش، فين، واش)"
    ),
    AR_DZ(
        id = "ar_dz",
        nameAr = "اللهجة الجزائرية",
        shortNameAr = "جزائرية",
        regionHint = "الجزائر · وهران · قسنطينة",
        flag = "🇩🇿",
        isArabicDialect = true,
        speechLocale = "ar-DZ",
        googleIso = "ar",
        ttsLang = "ar",
        aiStylePrompt = "اللهجة الجزائرية المحكية الطبيعية (مثل: درك، واش راك، راني، حبيت، بزاف، مليح، علاه)"
    ),
    AR_TN(
        id = "ar_tn",
        nameAr = "اللهجة التونسية",
        shortNameAr = "تونسية",
        regionHint = "تونس · صفاقس · سوسة",
        flag = "🇹🇳",
        isArabicDialect = true,
        speechLocale = "ar-TN",
        googleIso = "ar",
        ttsLang = "ar",
        aiStylePrompt = "اللهجة التونسية المحكية الطبيعية (مثل: توا، نحب، برشا، باهي، شنوة، علاش، يعيشك)"
    ),
    AR_LY(
        id = "ar_ly",
        nameAr = "اللهجة الليبية",
        shortNameAr = "ليبية",
        regionHint = "طرابلس · بنغازي",
        flag = "🇱🇾",
        isArabicDialect = true,
        speechLocale = "ar-LY",
        googleIso = "ar",
        ttsLang = "ar",
        aiStylePrompt = "اللهجة الليبية المحكية الطبيعية (مثل: توا، نبي، هلبا، باهي، شن الجو، خيرك)"
    ),
    AR_YE(
        id = "ar_ye",
        nameAr = "اللهجة اليمنية",
        shortNameAr = "يمنية",
        regionHint = "صنعاء · عدن · تعز · حضرموت",
        flag = "🇾🇪",
        isArabicDialect = true,
        speechLocale = "ar-YE",
        googleIso = "ar",
        ttsLang = "ar",
        aiStylePrompt = "اللهجة اليمنية المحكية الطبيعية (مثل: ذلحين، أشتي، قوي، مليح، ما بش، حيا الله)"
    ),
    AR_SD(
        id = "ar_sd",
        nameAr = "اللهجة السودانية",
        shortNameAr = "سودانية",
        regionHint = "الخرطوم · أم درمان · بورتسودان",
        flag = "🇸🇩",
        isArabicDialect = true,
        speechLocale = "ar-SD",
        googleIso = "ar",
        ttsLang = "ar",
        aiStylePrompt = "اللهجة السودانية المحكية الطبيعية (مثل: هسي، داير، شديد، سمح، يا زول، شنو)"
    ),

    // اللغات العالمية للتفاهم مع غير العرب أيضاً بأي لهجة عربية تتكلمها
    EN(
        id = "en",
        nameAr = "الإنجليزية (English)",
        shortNameAr = "إنجليزية",
        regionHint = "للتحدث مع الأجانب حول العالم",
        flag = "🇬🇧",
        isArabicDialect = false,
        speechLocale = "en-US",
        googleIso = "en",
        ttsLang = "en",
        aiStylePrompt = "Natural conversational English"
    ),
    TR(
        id = "tr",
        nameAr = "التركية (Türkçe)",
        shortNameAr = "تركية",
        regionHint = "تركيا",
        flag = "🇹🇷",
        isArabicDialect = false,
        speechLocale = "tr-TR",
        googleIso = "tr",
        ttsLang = "tr",
        aiStylePrompt = "Natural conversational Turkish"
    ),
    NL(
        id = "nl",
        nameAr = "الهولندية (Nederlands)",
        shortNameAr = "هولندية",
        regionHint = "هولندا · بلجيكا",
        flag = "🇳🇱",
        isArabicDialect = false,
        speechLocale = "nl-NL",
        googleIso = "nl",
        ttsLang = "nl",
        aiStylePrompt = "Natural conversational Dutch"
    ),
    DE(
        id = "de",
        nameAr = "الألمانية (Deutsch)",
        shortNameAr = "ألمانية",
        regionHint = "ألمانيا · النمسا · سويسرا",
        flag = "🇩🇪",
        isArabicDialect = false,
        speechLocale = "de-DE",
        googleIso = "de",
        ttsLang = "de",
        aiStylePrompt = "Natural conversational German"
    ),
    FR(
        id = "fr",
        nameAr = "الفرنسية (Français)",
        shortNameAr = "فرنسية",
        regionHint = "فرنسا · كندا · بلجيكا",
        flag = "🇫🇷",
        isArabicDialect = false,
        speechLocale = "fr-FR",
        googleIso = "fr",
        ttsLang = "fr",
        aiStylePrompt = "Natural conversational French"
    ),
    ES(
        id = "es",
        nameAr = "الإسبانية (Español)",
        shortNameAr = "إسبانية",
        regionHint = "إسبانيا · أمريكا اللاتينية",
        flag = "🇪🇸",
        isArabicDialect = false,
        speechLocale = "es-ES",
        googleIso = "es",
        ttsLang = "es",
        aiStylePrompt = "Natural conversational Spanish"
    ),
    IT(
        id = "it",
        nameAr = "الإيطالية (Italiano)",
        shortNameAr = "إيطالية",
        regionHint = "إيطاليا",
        flag = "🇮🇹",
        isArabicDialect = false,
        speechLocale = "it-IT",
        googleIso = "it",
        ttsLang = "it",
        aiStylePrompt = "Natural conversational Italian"
    ),
    SV(
        id = "sv",
        nameAr = "السويدية (Svenska)",
        shortNameAr = "سويدية",
        regionHint = "السويد",
        flag = "🇸🇪",
        isArabicDialect = false,
        speechLocale = "sv-SE",
        googleIso = "sv",
        ttsLang = "sv",
        aiStylePrompt = "Natural conversational Swedish"
    ),
    RU(
        id = "ru",
        nameAr = "الروسية (Русский)",
        shortNameAr = "روسية",
        regionHint = "روسيا",
        flag = "🇷🇺",
        isArabicDialect = false,
        speechLocale = "ru-RU",
        googleIso = "ru",
        ttsLang = "ru",
        aiStylePrompt = "Natural conversational Russian"
    ),
    FA(
        id = "fa",
        nameAr = "الفارسية (فارسی)",
        shortNameAr = "فارسية",
        regionHint = "إيران",
        flag = "🇮🇷",
        isArabicDialect = false,
        speechLocale = "fa-IR",
        googleIso = "fa",
        ttsLang = "fa",
        aiStylePrompt = "Natural conversational Persian"
    ),
    UR(
        id = "ur",
        nameAr = "الأوردو (اردو)",
        shortNameAr = "أوردو",
        regionHint = "باكستان · الهند",
        flag = "🇵🇰",
        isArabicDialect = false,
        speechLocale = "ur-PK",
        googleIso = "ur",
        ttsLang = "ur",
        aiStylePrompt = "Natural conversational Urdu"
    ),
    ZH(
        id = "zh",
        nameAr = "الصينية (中文)",
        shortNameAr = "صينية",
        regionHint = "الصين",
        flag = "🇨🇳",
        isArabicDialect = false,
        speechLocale = "zh-CN",
        googleIso = "zh-CN",
        ttsLang = "zh-CN",
        aiStylePrompt = "Natural conversational Mandarin Chinese"
    );

    companion object {
        fun fromId(id: String?): DialectProfile =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: AR_SIMPLE

        fun fromGoogleIso(iso: String?): DialectProfile? {
            if (iso.isNullOrBlank()) return null
            val clean = iso.lowercase().trim()
            if (clean == "ar") return AR_SIMPLE
            if (clean.startsWith("zh")) return ZH
            return entries.firstOrNull { !it.isArabicDialect && it.googleIso.lowercase() == clean }
        }

        val arabicDialects: List<DialectProfile>
            get() = entries.filter { it.isArabicDialect && it != AUTO_DETECT }

        val worldLanguages: List<DialectProfile>
            get() = entries.filter { !it.isArabicDialect }
    }
}

data class SlangNote(
    val word: String,
    val meaning: String,
)

data class DialectDetectionResult(
    val profile: DialectProfile,
    val confidence: Int,
    val notes: List<SlangNote>,
    val clearArabicMeaning: String,
)

data class TranslationOutcome(
    val originalText: String,
    val translatedText: String,
    val clearArabicSubtitle: String,
    val detectedDialect: DialectProfile,
    val targetDialect: DialectProfile,
    val confidence: Int,
    val slangNotes: List<SlangNote>,
    val latencyMs: Long,
    val engineLabel: String,
)

data class ConversationMessage(
    val id: String,
    val side: SpeakerSide,
    val originalText: String,
    val translatedText: String,
    val clearArabicSubtitle: String,
    val detectedDialect: DialectProfile,
    val targetDialect: DialectProfile,
    val confidence: Int,
    val slangNotes: List<SlangNote>,
    val latencyMs: Long,
    val timestamp: Long,
    val isAutoDetected: Boolean,
)

data class DialectComparisonItem(
    val dialect: DialectProfile,
    val text: String,
)

data class SampleTestItem(
    val label: String,
    val side: SpeakerSide,
    val sampleText: String,
)

data class QuickPhraseCategory(
    val id: String,
    val title: String,
    val emoji: String,
    val phrases: List<String>,
)

object BuiltInSamples {
    val quickTests = listOf(
        SampleTestItem(
            label = "🇲🇦 جرب مغربي دارجة (كشف تلقائي)",
            side = SpeakerSide.PARTNER,
            sampleText = "خويا دابا بغيت نمشي للسوق نشري شي حوايج حيت عييت بزاف وما بقاش عندي الوقت"
        ),
        SampleTestItem(
            label = "🇮🇶 جرب عراقي (كشف تلقائي)",
            side = SpeakerSide.PARTNER,
            sampleText = "شكو ماكو خوية هسة أريد أروح للبيت لأن كلش تعبان وماكو أحد وياي"
        ),
        SampleTestItem(
            label = "🇸🇾 جرب أحكي بالشامي",
            side = SpeakerSide.ME,
            sampleText = "يعطيك العافية يا معلم شو بدك تساوي هلأ؟ فينا نلتقي بكير مشان نخلص الشغل دغري"
        ),
        SampleTestItem(
            label = "🇩🇿 جرب جزائري (كشف تلقائي)",
            side = SpeakerSide.PARTNER,
            sampleText = "واش راك خويا راني جاي درك بصح الطريق راهي معمرة بزاف استناني شوية"
        ),
        SampleTestItem(
            label = "🇪🇬 جرب مصري (كشف تلقائي)",
            side = SpeakerSide.PARTNER,
            sampleText = "إزيك يا باشا عامل إيه؟ أنا عايز أعرف المكان ده فين دلوقتي وبكام التذكرة"
        ),
        SampleTestItem(
            label = "🇸🇦 جرب خليجي (كشف تلقائي)",
            side = SpeakerSide.PARTNER,
            sampleText = "شخبارك طال عمرك وش علومك؟ أبي أمر عليك الحين إذا ما عندك دوام"
        ),
        SampleTestItem(
            label = "🇾🇪 جرب يمني (كشف تلقائي)",
            side = SpeakerSide.PARTNER,
            sampleText = "حيا الله يا خبير ما أشتيش أتأخر ذلحين لأن الجو بارد قوي"
        ),
        SampleTestItem(
            label = "🇸🇩 جرب سوداني (كشف تلقائي)",
            side = SpeakerSide.PARTNER,
            sampleText = "حبابك يا زول داير شنو هسي؟ الموضوع سمح شديد وما في عوجة"
        ),
    )

    val phraseCategories = listOf(
        QuickPhraseCategory(
            id = "greet",
            title = "الترحيب والتعارف",
            emoji = "👋",
            phrases = listOf(
                "مرحباً يا أخي، كيف حالك اليوم؟ أتمنى أن تكون بخير",
                "تشرفنا بمعرفتك، أنا سعيد جداً بلقائك",
                "من فضلك تكلم ببطء قليل حتى أفهمك جيداً",
                "شكراً جزيلاً لك، الله يعطيك الصحة والعافية",
                "لا مشكلة أبداً، خذ وقتك براحتك",
            )
        ),
        QuickPhraseCategory(
            id = "market",
            title = "السوق والأسعار",
            emoji = "🛒",
            phrases = listOf(
                "لو سمحت كم سعر هذا؟ هل يوجد خصم جيد؟",
                "أريد شراء هذه الأغراض، هل يمكن الدفع بالبطاقة؟",
                "السعر مرتفع كثيراً، أعطني سعراً مناسباً من فضلك",
                "أين أجد أقرب صراف آلي أو مركز تسوق هنا؟",
                "هل عندك مقاس أكبر أو لون آخر من هذا؟",
            )
        ),
        QuickPhraseCategory(
            id = "transport",
            title = "التكسي والمواصلات",
            emoji = "🚕",
            phrases = listOf(
                "أريد الذهاب إلى هذا العنوان الآن، كم تكلفة المشوار؟",
                "كم دقيقة نحتاج حتى نصل إلى المطار أو المحطة؟",
                "من فضلك توقف هنا على اليمين، شكراً لك",
                "أنا تائه قليلاً، كيف أصل إلى وسط المدينة من هنا؟",
            )
        ),
        QuickPhraseCategory(
            id = "food",
            title = "المطعم والقهوة",
            emoji = "🍽️",
            phrases = listOf(
                "ماذا تنصحني أن أجرب من الأكل الشهي عندكم اليوم؟",
                "أريد قهوة وماء بارد من فضلك بدون سكر",
                "الأكل لذيذ جداً، سلمت يداكم، ممكن الحساب لو سمحت؟",
                "هل هذا الطعام حلال ولا يحتوي على مكونات تسبب الحساسية؟",
            )
        ),
        QuickPhraseCategory(
            id = "emergency",
            title = "الطوارئ والمستشفى",
            emoji = "🏥",
            phrases = listOf(
                "أحتاج مساعدة عاجلة من فضلك، أين أقرب مستشفى أو صيدلية؟",
                "أشعر بألم وأحتاج إلى طبيب ودواء في أسرع وقت",
                "ضاع هاتفي أو محفظتي، هل يمكنك مساعدتي في الاتصال؟",
            )
        ),
        QuickPhraseCategory(
            id = "work",
            title = "العمل والمواعيد",
            emoji = "💼",
            phrases = listOf(
                "متى يمكننا الاجتماع اليوم لننهي العمل معاً؟",
                "سأرسل لك الموقع والتفاصيل على الواتساب بعد قليل",
                "اتفقنا إن شاء الله، نلتقي غداً في الموعد المحدد",
            )
        ),
    )
}

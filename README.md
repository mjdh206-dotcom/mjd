# 🎬 مصنع الفيديو (VideoForge)

> تطبيق أندرويد بواجهة عربية كاملة (RTL) يحول أفكارك إلى فيديوهات جاهزة بالذكاء الاصطناعي

## ✨ المميزات الرئيسية

- **إنتاج فيديو كامل**: من الفكرة إلى ملف MP4 جاهز
- **تبديل ذكي بين المحركات**: ينتقل تلقائياً بين محركات AI عند نفاد الحصة
- **واجهة عربية 100%**: تصميم RTL كامل مع دعم اللهجات
- **محركات متعددة**: Gemini, Claude, GPT, DeepSeek, Mistral, Groq, Grok
- **توليد صور**: Stable Diffusion, DALL-E, Flux
- **تعليق صوتي**: ElevenLabs, Edge TTS (مجاني), OpenAI TTS
- **موسيقى**: Suno AI
- **فيديو**: Runway Gen-3

## 🏗️ البنية التقنية

```
Kotlin + Jetpack Compose + MVVM + Hilt + Room + WorkManager
```

### هيكل المشروع

```
app/src/main/java/com/mjd/videoforge/
├── MainActivity.kt              # النشاط الرئيسي
├── VideoForgeApp.kt             # Application class
├── di/                          # Dependency Injection
│   └── ProvidersModule.kt
├── data/
│   ├── db/                      # Room Database
│   │   ├── Database.kt          # DAOs + Database
│   │   └── Converters.kt
│   ├── model/                   # Data Models
│   │   └── Models.kt
│   ├── repository/              # Repositories
│   │   ├── Repositories.kt
│   │   └── AppSettingsRepository.kt
│   └── security/                # Encryption
│       └── ApiKeyEncryption.kt  # Android Keystore
├── providers/                   # 🔑 قلب التطبيق
│   ├── ProviderInterfaces.kt    # واجهات موحدة
│   ├── ProviderManager.kt       # مدير التبديل الذكي
│   ├── BaseApiProvider.kt       # قاعدة مشتركة
│   ├── gemini/                  # Google Gemini
│   ├── claude/                  # Anthropic Claude
│   ├── gpt/                     # OpenAI GPT + DALL-E + TTS
│   ├── deepseek/                # DeepSeek (مجاني)
│   ├── mistral/                 # Mistral AI
│   ├── groq/                    # Groq (الأسرع)
│   ├── grok/                    # xAI Grok
│   ├── audio/                   # ElevenLabs + Edge TTS
│   ├── image/                   # Stability + Replicate
│   ├── video/                   # Runway
│   └── music/                   # Suno AI
├── pipeline/                    # مسار الإنتاج
│   ├── VideoPipeline.kt         # المسار الكامل
│   ├── ScriptGenerator.kt       # توليد السكربت
│   ├── MontageEngine.kt         # محرك المونتاج
│   └── ExportManager.kt         # التصدير
├── ui/
│   ├── theme/                   # Material 3 Theme
│   ├── navigation/              # Navigation
│   └── screens/                 # الشاشات
│       ├── home/                # الرئيسية + الأسئلة الذكية
│       ├── storyboard/          # لوحة القصة
│       ├── pipeline/            # مسار الإنتاج
│       ├── preview/             # المعاينة والتصدير
│       ├── providers/           # إدارة المحركات
│       ├── settings/            # الإعدادات
│       ├── queue/               # قائمة الانتظار
│       ├── brand/               # هوية القناة
│       └── analytics/           # التحليلات
├── workers/                     # المعالجة الخلفية
│   ├── VideoWorker.kt           # Foreground Service
│   └── Workers.kt               # Queue + Reset + Backup
└── util/
    └── Extensions.kt            # أدوات مساعدة
```

## 🔑 محرك التبديل الذكي

### كيف يعمل؟

```
┌──────────────────────────────────────────────────┐
│                ProviderManager                    │
│                                                    │
│  1. يستقبل طلب (مثال: توليد سكربت)               │
│  2. يختار أفضل محرك حسب:                          │
│     • مجاني أولاً ← أعلى أولوية ← متاح           │
│  3. يحاول التنفيذ:                                │
│     ✓ نجح → تحديث العدّاد + تسجيل التكلفة       │
│     ✗ خطأ 429 → انتقال فوري للمحرك التالي        │
│     ✗ مفتاح خاطئ → تخطي + تحذير                  │
│     ✗ فلتر محتوى → إعادة صياغة أو تخطي           │
│  4. المحرك المنهي → فترة تبريد حتى منتصف الليل   │
│  5. المستخدم لا يشعر بشيء!                       │
└──────────────────────────────────────────────────┘
```

### أولويات المحركات الافتراضية

| المحرك | الأولوية | مجاني؟ | المهمة |
|--------|----------|--------|--------|
| Edge TTS | 0 | ✅ | تعليق صوتي |
| Gemini | 1 | ✅ | نص/سكربت |
| DeepSeek | 1 | ✅ | نص/سكربت |
| Groq | 1 | ✅ | نص/سكربت (الأسرع) |
| Mistral | 2 | ✅ | نص/سكربت |
| Stability | 2 | ❌ | صور |
| Claude | 2 | ❌ | نص/سكربت |
| Replicate | 3 | ❌ | صور (Flux) |
| GPT | 3 | ❌ | نص + صور + صوت |
| Grok | 4 | ❌ | نص/سكربت |
| ElevenLabs | 1 | ❌ | تعليق صوتي |
| Runway | 1 | ❌ | فيديو |
| Suno | 1 | ❌ | موسيقى |

### إضافة محرك جديد

```kotlin
class MyNewProvider(
    apiKeyEncryption: ApiKeyEncryption,
    httpClient: OkHttpClient
) : BaseApiProvider(apiKeyEncryption, httpClient) {
    override val id = "my_provider"
    override val displayNameAr = "محركي الجديد"
    override val supportedTasks = setOf(TaskType.SCRIPT, TaskType.TITLE)
    override val isFree = true
    override val costPerRequest = 0.0
    override var priority = 1

    override suspend fun generateText(request: TextRequest): TextResponse {
        // تنفيذ الطلب...
    }

    override suspend fun healthCheck(): HealthCheckResponse {
        // فحص صحة المفتاح...
    }
}

// تسجيله في DI Module:
providerManager.registerProvider(MyNewProvider(apiKeyEncryption, httpClient))
```

## 📱 الشاشات

| الشاشة | الوصف |
|--------|-------|
| 🏠 الرئيسية | إدخال الفكرة + القوالب + المشاريع |
| ❓ الأسئلة الذكية | المدة، اللهجة، الصوت، الأسلوب |
| 🎬 لوحة القصة | مشاهد مرقمة مع تعديل |
| ⚙️ مسار الإنتاج | تقدم مباشر لكل خطوة |
| 👁️ المعاينة | معاينة + تصدير + مشاركة |
| 🔌 المحركات | حالة + أولويات + فحص صحة |
| ⚙️ إعدادات المحرك | مفاتيح API مشفرة |
| 📋 قائمة الانتظار | معالجة متعددة بالخلفية |
| 🎨 هوية القناة | ألوان + شعار + صوت ثابت |
| 📊 التحليلات | تكلفة + استخدام + نصائح |

## 🔒 الأمان

- كل مفاتيح API مشفرة بـ **Android Keystore** (AES-256-GCM)
- لا يتم إرسال أي مفتاح لأي سيرفر خارجي
- التشفير وفك التشفير يتم محلياً على الجهاز
- دعم تدوير المفاتيح (Key Rotation)

## 🎨 مسار إنتاج الفيديو

```
الفكرة → الأسئلة الذكية → السكربت → لوحة القصة
                                        ↓
          التصدير ← المونتاج ← الصوت ← الصور
             ↓
    MP4 + عنوان + وصف + هاشتاغات + SRT
```

### كل خطوة بالتفصيل

1. **توليد السكربت**: هوك + مشاهد + خاتمة + عنوان + وصف + هاشتاغات
2. **لوحة القصة**: تقسيم لمشاهد مع وصف بصري ونص روائي
3. **توليد الصور**: لكل مشهد مع Ken Burns effect
4. **التعليق الصوتي**: مع ضبط التشكيل العربي
5. **الموسيقى**: خلفية مناسبة (اختياري)
6. **المونتاج**: تجميع + انتقالات + مزامنة + ترجمة
7. **التصدير**: MP4 1080p + metadata

## 📋 المتطلبات

- Android 8.0+ (API 26)
- Kotlin 2.0+
- Jetpack Compose
- Hilt (DI)
- Room (Database)
- WorkManager (Background)
- FFmpeg Kit (Video)
- Media3 Transformer (Video Effects)

## 🚀 التشغيل

```bash
# فتح المشروع في Android Studio
# أو بناء من سطر الأوامر:
./gradlew assembleDebug
```

## 📊 إحصائيات المشروع

- **44 ملف Kotlin**
- **~7,600 سطر كود**
- **59 ملف إجمالي** (Kotlin + XML + Gradle)
- **11 شاشة واجهة**
- **12 موصّل محرك AI**
- **8 جداول قاعدة بيانات**

---

**مصنع الفيديو** - حوّل أفكارك إلى فيديوهات احترافية بالذكاء الاصطناعي 🎬✨

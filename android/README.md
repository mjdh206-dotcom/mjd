# ريموت المكيف برو — AC Remote Pro

**تحميل APK (الأحدث 1.1.1):** [ACRemotePro-1.1.1.apk](https://github.com/mjdh206-dotcom/mjd/raw/arena/18a8441b-mjd/android/ACRemotePro-1.1.1.apk)

**الإصدار السابق:** [ACRemotePro-1.1.0.apk](https://github.com/mjdh206-dotcom/mjd/raw/arena/18a8441b-mjd/android/ACRemotePro-1.1.0.apk)

**الإصدار السابق:** [ACRemotePro-1.0.0.apk](https://github.com/mjdh206-dotcom/mjd/raw/arena/18a8441b-mjd/android/ACRemotePro-1.0.0.apk)

تطبيق أندرويد عربي (RTL) للتحكم في المكيفات عبر باعث الأشعة تحت الحمراء المدمج في الهاتف.

## المميزات
- **بحث تلقائي مستمر**: يرسل الأكواد واحدة بعد الأخرى لوحده (بلا ضغط أزرار)، وبمجرد سماع صوت من
  المكيف تضغط زرًا واحدًا «وجدته» فيتوقف فورًا ويحفظ الكود. إن لم تجد في الجولة الأولى يعيد الكرّة
  بالأكواد البديلة تلقائيًا.
- **تحكم كامل**: تشغيل/إطفاء، تبريد، تدفئة، تجفيف، مروحة، وضع تلقائي، درجة الحرارة، سرعة المروحة،
  تأرجح عمودي/أفقي، تيربو، سكون، إضاءة الشاشة، ووضع اقتصادي.
- **مؤقت إطفاء**: 30 دقيقة / ساعة / ساعتان / 4 ساعات (يعمل عبر AlarmManager حتى لو أُغلق التطبيق).
- **أجهزة متعددة**: احفظ أكثر من مكيف وبدّل بينها.
- **واجهة ليلية عربية** بتصميم دائري متدرّج، بدون إنترنت وبدون إعلانات وبدون صلاحيات موقع.

## البروتوكولات المدعومة
Gree · Midea · Coolix · LG · Samsung · TCL · Haier · Toshiba · Panasonic · Mitsubishi · Daikin ·
Sharp · Hitachi · Fujitsu · Carrier · Electra · Kelvinator  (17 بروتوكولًا + نسخ بديلة)

> تمت برمجة الأكواد اعتمادًا على مواصفات مشروع IRremoteESP8266 مفتوح المصدر.

## ما الجديد في 1.1.1
- تدقيق كامل لبصمات البايتات (mode / temp / fan / power / checksum) في البروتوكولات الـ17
  مقابل مكتبة IRremoteESP8266: صُحّحت Midea وSamsung وTCL وHaier وPanasonic وMitsubishi
  وDaikin وFujitsu وSharp وHitachi وCarrier وElectra وCoolix.
- إعادة توقيت Carrier (8940/4556µs ونبضة 503µs) وإرسال إطار واحد بدل ثلاثة، وترتيب بتات
  Carrier الصحيح (LSB أولًا) ليطابق الإرسال الأصلي.
- Fujitsu: رسالة الإطفاء القصيرة (7 بايت) وإرسال إطارين للتشغيل، وToshiba يرسل إطارين.
- Samsung: إرسال رسالة التشغيل الممتدة دائمًا قبل الحالة العادية (ضروري للخروج من الاستعداد).
- بحث تلقائي مستمر: عند نهاية الجولة يبدأ جولة جديدة بالنسخ البديلة تلقائيًا دون تدخل.

## متطلبات التشغيل
- أندرويد 6.0 (API 23) أو أحدث.
- **هاتف بباعث أشعة تحت الحمراء (IR Blaster)**، مثل معظم هواتف Xiaomi/Huawei/Honor/LG/HTC القديمة.
  إذا لم يكن الهاتف يحتوي على باعث، سيعمل التطبيق لكن الإشارة لن تصل إلى المكيف.

## البناء من المصدر
المشروع يُبنى بدون Gradle باستخدام أدوات Android SDK الرسمية:

```bash
# 1) ترجمة الموارد
aapt2 compile --dir res -o build/compiled/
# 2) الربط وتوليد R.java
aapt2 link -o build/base.apk -I android.jar --manifest AndroidManifest.xml \
  --java build/gen --min-sdk-version 23 --target-sdk-version 34 \
  --version-code 1 --version-name 1.0.0 build/compiled/*.flat
# 3) ترجمة الجافا (Java 8)
javac -source 1.8 -target 1.8 -cp android.jar -d build/classes $(find java build/gen -name '*.java')
# 4) تحويل إلى DEX
d8 --min-api 23 --lib android.jar --release --output build/dex $(find build/classes -name '*.class')
# 5) التغليف والتوقيع
zip -j build/base.apk build/dex/classes.dex
apksigner sign --ks keystore/remotepro.jks --ks-pass pass:RemotePro2026 \
  --key-pass pass:RemotePro2026 --out ACRemotePro-1.0.0.apk build/base.apk
```

مفتاح التوقيع موجود في `keystore/remotepro.jks` (كلمة المرور: `RemotePro2026`) لتمكين تحديثات مستقبلية
بنفس التوقيع. لا تشارك هذا الملف علنًا إذا أردت نشر التطبيق على Google Play.

## بنية الكود
```
java/com/mjd/acremote/
├── MainActivity.java      واجهة الريموت الرئيسية
├── SetupActivity.java     الإعداد + البحث الذكي
├── Receiver.java          مؤقت الإطفاء
├── ac/                    محرّكات البروتوكولات + السجل
├── ir/                    بناء الإشارة وإرسالها عبر ConsumerIrManager
├── data/                  حفظ الملفات الشخصية (SharedPreferences + JSON)
└── ui/                    الواجهة المخصصة (الخلفية، قرص الحرارة، زر الطاقة)
```

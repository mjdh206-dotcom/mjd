# ريموت المكيف برو — AC Remote Pro

تطبيق أندرويد عربي (RTL) للتحكم في المكيفات عبر باعث الأشعة تحت الحمراء المدمج في الهاتف.

## المميزات
- **بحث ذكي**: يرسل أكواد اختبار لـ 11 بروتوكول مكيفات حتى يستجيب جهازك، ثم يحفظ النوع تلقائيًا.
- **تحكم كامل**: تشغيل/إطفاء، تبريد، تدفئة، تجفيف، مروحة، وضع تلقائي، درجة الحرارة، سرعة المروحة،
  تأرجح عمودي/أفقي، تيربو، سكون، إضاءة الشاشة، ووضع اقتصادي.
- **مؤقت إطفاء**: 30 دقيقة / ساعة / ساعتان / 4 ساعات (يعمل عبر AlarmManager حتى لو أُغلق التطبيق).
- **أجهزة متعددة**: احفظ أكثر من مكيف وبدّل بينها.
- **واجهة ليلية عربية** بتصميم دائري متدرّج، بدون إنترنت وبدون إعلانات وبدون صلاحيات موقع.

## البروتوكولات المدعومة
Gree · Midea · Coolix · LG · Samsung · TCL · Haier · Toshiba · Panasonic · Mitsubishi · Daikin

> تمت برمجة الأكواد يدويًا اعتمادًا على مواصفات مشروع IRremoteESP8266 مفتوح المصدر.

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

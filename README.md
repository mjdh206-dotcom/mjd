# محامص الحرمين — تطبيق الزبون وتطبيق المتجر

تطبيقان عربيان مستقلان، متصلان عند إعداد مشروع Firebase المشترك `al-haramain-roasters`:

- **تطبيق الزبون:** تصفح المنتجات والعروض، السلة، إرسال الطلبات ومتابعة حالتها.
- **تطبيق المتجر:** دخول آمن للمدير، مزامنة المنتجات والطلبات، تعديل حالة الطلب ونشر عرض يظهر للزبائن، وتنبيهات FCM صوتية عالية الأولوية.
- الحزم المسجلة: `com.alharamain.customer` و`com.alharamain.store`.
- العملة **الليرة السورية (ل.س)**. الأسعار المبدئية أمثلة وليست أسعار المحل الفعلية؛ حدّثها قبل الاستخدام التجاري.
- الاعتماد «تم تطوير هذا البرنامج بواسطة المبرمج أمجد حبيب» ورقم التواصل `+963988423100` ظاهران في التطبيقين. هذا الرقم ليس رقم استقبال الطلبات.

## التحميل

روابط التنزيل الثابتة:

- [تحميل APK الزبون مباشرة](https://github.com/mjdh206-dotcom/mjd/raw/refs/heads/arena/4cd5a3f7-mjd/dist/al-haramain-customer-1.0.0.apk)
- [تحميل APK المتجر مباشرة](https://github.com/mjdh206-dotcom/mjd/raw/refs/heads/arena/4cd5a3f7-mjd/dist/al-haramain-store-1.0.0.apk)
- [ملصق QR A5 لتنزيل تطبيق الزبون](https://github.com/mjdh206-dotcom/mjd/raw/refs/heads/arena/4cd5a3f7-mjd/dist/al-haramain-customer-qr-a5.pdf)
- [صورة QR للزبون](https://github.com/mjdh206-dotcom/mjd/raw/refs/heads/arena/4cd5a3f7-mjd/dist/al-haramain-customer-qr.png)

## بناء التطبيقات

كل تطبيق مشروع Android مستقل:

```bash
cd al-haramain-customer && ./gradlew assembleDebug
cd ../al-haramain-store && ./gradlew assembleDebug
```

## تفعيل الخدمة قبل الاستخدام الحقيقي

ملفا APK المنشوران سابقاً يسبقان إضافة Firebase. الكود الحالي يضم تكامل Firebase وملف الإعداد الصحيح للمشروع، لكن **مزامنة الطلبات/المنتجات وتنبيه الهاتف لن تعمل فعلياً حتى نشر قواعد Firestore وCloud Function وبناء APKs محدثة**. لم يتم النشر أو اختبار FCM من هذه البيئة. خطوات إعداد Firebase وإنشاء حساب المدير وضبط UID والنشر موجودة في [دليل Firebase العربي](firebase/SETUP-ar.md). يظل QR رابطاً مباشراً ثابتاً؛ سيشير إلى APK المحدّث بعد حفظه في `dist/`.

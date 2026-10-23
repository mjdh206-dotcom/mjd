# خادم واصل — Wasil Backend

خادم Node.js صغير (مكتبة واحدة فقط: Express) يستقبل رسائل SMS الواردة إلى رقمك الأمريكي
من **Twilio** عبر webhook، ويخزّنها في ملف JSON، ويعرضها لتطبيق «واصل» عبر API محمي برمز سري.

> لا قاعدة بيانات، لا اعتماديات معقّدة، ويمكن تشغيله على أي خادم أو منصة سحابية مجانية.

## العقد الذي يفهمه التطبيق

| الطريقة | المسار | الحماية | الوظيفة |
|---|---|---|---|
| GET | `/api/health` | بدون | فحص أن الخادم حي |
| GET | `/api/number` | `Authorization: Bearer API_TOKEN` | رقمك الأمريكي |
| GET | `/api/messages` | `Authorization: Bearer API_TOKEN` | قائمة الرسائل الواردة |
| POST | `/sms/twilio` | توقيع Twilio اختياري | webhook الرسائل من Twilio |
| POST | `/sms/generic` | ترويسة `X-Webhook-Secret` | webhook عام لمزوّدين آخرين |

شكل الرسالة المُعادة:

```json
{
  "messages": [
    { "id": "SM…", "from": "WhatsApp", "to": "+1…", "body": "رمز التفعيل: 348-291",
      "ts": 1729700000000, "receivedAt": "2026-10-23T10:00:00.000Z" }
  ]
}
```

## التشغيل محلياً

```bash
cd backend
cp .env.example .env        # عدّل القيم: API_TOKEN و US_NUMBER
npm install
npm start
```

اختبره بسرعة:

```bash
curl http://localhost:3000/api/health

curl -X POST http://localhost:3000/sms/twilio \
  -H "Content-Type: application/x-www-form-urlencoded" \
  --data "From=WhatsApp&To=%2B14155550134&Body=رمز التفعيل: 348-291&MessageSid=SM123"

curl http://localhost:3000/api/messages -H "Authorization: Bearer الرمز_الذي_في_.env"
```

للاخدام من الهاتف على خادم محلي، اكشف منفذك عبر نفق HTTPS مثل
`cloudflared tunnel --url http://localhost:3000` وضع الرابط في التطبيق.

## الربط مع Twilio (الخطوات الحقيقية)

1. أنشئ حساباً على [twilio.com](https://www.twilio.com) باسمك الحقيقي (الحساب التجريبي يمنح رصيداً مجانياً للاختبار).
2. من **Phone Numbers → Buy a Number** اشترِ رقماً أمريكياً قادراً على استقبال SMS
   (التكلفة ≈ 1.15 دولار شهرياً + سنتات لكل رسالة واردة).
3. افتح صفحة الرقم، وفي قسم **Messaging → A Message Comes In** اختر:
   - النوع: **Webhook**
   - الرابط: `https://رابط-خادمك/sms/twilio`
   - الطريقة: **HTTP POST**
4. (مستحسن) انسخ **Auth Token** من الصفحة الرئيسية لحسابك إلى `TWILIO_AUTH_TOKEN`
   في `.env` — سيرفض الخادم حينها أي طلب webhook غير موقع من Twilio فعلاً.
5. أرسل أي رسالة إلى رقمك (أو اطلب رمز تفعيل) وستظهر فوراً في التطبيق.

## النشر على منصة سحابية

أي منصة تدعم Node.js تكفي. الأبسط للمبتدئين:

- **Render** أو **Railway**: أنشئ خدمة Web Service من مجلد `backend/`،
  وأضف متغيرات البيئة نفسها الموجودة في `.env.example`، وستأخذ المنصة `PORT` تلقائياً.
- **VPS صغير**: `npm install && npm start` خلف Nginx مع شهادة HTTPS (Let’s Encrypt).

احرص على أن يكون الرابط العام **HTTPS** (إلزامي لـ webhooks Twilio ومعظم المنصات).

## الأمان

- `API_TOKEN` يفصل هاتفك عن بقية العالم: بدونه تُرفض كل طلبات `/api/*` (401).
- فعّل `TWILIO_AUTH_TOKEN` للتأكد أن رسائل الـ webhook قادمة من Twilio حصراً.
- الملف `.env` ومجلد `data/` مستثنيان من Git في `.gitignore` — لا ترفعهما أبداً.

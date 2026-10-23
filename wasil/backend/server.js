/**
 * خادم واصل — Wasil backend
 * --------------------------
 * يستقبل رسائل SMS الواردة إلى رقمك الأمريكي من Twilio عبر webhook،
 * يخزّنها في ملف JSON محلي، ويعرضها لتطبيق أندرويد عبر API محمي برمز Bearer.
 *
 * التشغيل:
 *   1) cp .env.example .env  ثم عدّل القيم
 *   2) npm install
 *   3) npm start
 *
 * المتطلبات: Node.js 18 فأعلى (لا حاجة لقاعدة بيانات).
 */

const express = require('express');
const crypto = require('crypto');
const fs = require('fs');
const path = require('path');

// تحميل ملف .env يدوياً بدون أي مكتبة إضافية
try {
    const envPath = path.join(__dirname, '.env');
    for (const line of fs.readFileSync(envPath, 'utf8').split('\n')) {
        const match = line.match(/^\s*([A-Z0-9_]+)\s*=\s*(.*)\s*$/);
        if (match && process.env[match[1]] === undefined) {
            process.env[match[1]] = match[2].replace(/^["']|["']$/g, '');
        }
    }
} catch {
    /* ملف .env غير موجود: نعتمد على متغيرات البيئة (حال منصات الاستضافة) */
}

const app = express();
app.set('trust proxy', 1); // خلف منصات مثل Render/Railway
app.use(express.urlencoded({ extended: false })); // نموذج Twilio
app.use(express.json()); // webhook العام JSON

const PORT = process.env.PORT || 3000;
const API_TOKEN = process.env.API_TOKEN || '';
const US_NUMBER = process.env.US_NUMBER || '';
const TWILIO_AUTH_TOKEN = process.env.TWILIO_AUTH_TOKEN || '';
const WEBHOOK_SECRET = process.env.WEBHOOK_SECRET || '';
const MAX_MESSAGES = 200;

const DATA_DIR = path.join(__dirname, 'data');
const DATA_FILE = path.join(DATA_DIR, 'messages.json');

/* ---------------- تخزين بسيط في ملف JSON ---------------- */

function loadMessages() {
    try {
        return JSON.parse(fs.readFileSync(DATA_FILE, 'utf8'));
    } catch {
        return [];
    }
}

function saveMessages(list) {
    fs.mkdirSync(DATA_DIR, { recursive: true });
    fs.writeFileSync(DATA_FILE, JSON.stringify(list, null, 2));
}

function addMessage({ id, from, to, body }) {
    const list = loadMessages();
    const message = {
        id: id || crypto.randomUUID(),
        from: from || 'غير معروف',
        to: to || US_NUMBER || '',
        body: body || '',
        ts: Date.now(),
        receivedAt: new Date().toISOString(),
    };
    list.unshift(message);
    saveMessages(list.slice(0, MAX_MESSAGES));
    console.log(`[واصل] رسالة جديدة من ${message.from}: ${message.body.slice(0, 60)}`);
    return message;
}

/* ---------------- حماية API التطبيق ---------------- */

function safeEqual(a, b) {
    const bufA = Buffer.from(String(a));
    const bufB = Buffer.from(String(b));
    return bufA.length === bufB.length && crypto.timingSafeEqual(bufA, bufB);
}

function requireApiToken(req, res, next) {
    if (!API_TOKEN) {
        return res.status(500).json({ error: 'API_TOKEN غير مضبوط على الخادم' });
    }
    const header = req.headers.authorization || '';
    const token = header.startsWith('Bearer ') ? header.slice(7) : '';
    if (!safeEqual(token, API_TOKEN)) {
        return res.status(401).json({ error: 'الرمز السري غير صحيح' });
    }
    next();
}

/* ---------------- نقاط API لتطبيق أندرويد ---------------- */

app.get('/', (req, res) => {
    res.json({ service: 'wasil-backend', status: 'running' });
});

app.get('/api/health', (req, res) => {
    res.json({ ok: true, service: 'wasil', ts: Date.now() });
});

app.get('/api/number', requireApiToken, (req, res) => {
    res.json({ number: US_NUMBER, country: 'US' });
});

app.get('/api/messages', requireApiToken, (req, res) => {
    res.json({ messages: loadMessages() });
});

/* ---------------- webhook الوارد من Twilio ---------------- */

function verifyTwilioSignature(req) {
    if (!TWILIO_AUTH_TOKEN) return true; // التحقق اختياري
    const signature = req.headers['x-twilio-signature'] || '';
    const fullUrl = `${req.protocol}://${req.get('host')}${req.path}`;
    const params = req.body || {};
    const sorted = Object.keys(params).sort().map((k) => `${k}${params[k]}`).join('');
    const expected = crypto
        .createHmac('sha1', TWILIO_AUTH_TOKEN)
        .update(fullUrl + sorted)
        .digest('base64');
    return safeEqual(signature, expected);
}

app.post('/sms/twilio', (req, res) => {
    if (!verifyTwilioSignature(req)) {
        console.warn('[واصل] محاولة webhook بتوقيع غير صالح');
        return res.status(403).send('Invalid signature');
    }
    const { From, To, Body, MessageSid } = req.body;
    if (!From || Body === undefined) {
        return res.status(400).send('Missing From/Body');
    }
    addMessage({ id: MessageSid, from: From, to: To, body: Body });
    res.type('text/xml').send(
        '<?xml version="1.0" encoding="UTF-8"?><Response></Response>'
    );
});

/* ---------------- webhook عام لمزوّدين آخرين (Telnyx/Plivo/مخصص) ---------------- */

app.post('/sms/generic', (req, res) => {
    if (!WEBHOOK_SECRET) {
        return res.status(403).json({ error: 'WEBHOOK_SECRET غير مضبوط على الخادم' });
    }
    const secret = req.headers['x-webhook-secret'] || '';
    if (!safeEqual(secret, WEBHOOK_SECRET)) {
        return res.status(403).json({ error: 'سر الـ webhook غير صحيح' });
    }
    const { from, body, to, id } = req.body || {};
    if (!from || body === undefined) {
        return res.status(400).json({ error: 'الحقلان from و body مطلوبان' });
    }
    const message = addMessage({ id, from, to, body });
    res.json({ ok: true, id: message.id });
});

app.listen(PORT, () => {
    console.log(`خادم واصل يعمل على المنفذ ${PORT}`);
    if (!API_TOKEN) console.warn('تحذير: API_TOKEN غير مضبوط — لن يعمل التطبيق مع هذا الخادم.');
});

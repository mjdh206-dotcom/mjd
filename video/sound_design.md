# 🔊 Sound Design — "هل تعلم عن المغرب؟" (18s)
## القاعدة غير القابلة للتفاوض: **لا موسيقى إطلاقاً.**
الصمت الدرامي + المؤثرات السينمائية وحدها = إحساس "استديو هوليوود". أي موسيقى خلفية تكسر هذا الإحساس وتُخفض تفرّد الفيديو لدى الخوارزمية.

## Cue Sheet (مضبوط على الماستر النهائي)
| التوقيت | المؤثر | الوظيفة الدرامية |
|---------|--------|------------------|
| 00:00.0 | Cinematic Whoosh (خروج من السواد) | فتح الستارة |
| 00:00.1 | Low Bass Impact (56→26Hz) | صدمة افتتاحية تثبّت المشاهد |
| 00:00–18 | Air Bed خافت (~-30dB, 40–320Hz) | "غرفة" سينمائية تمنع الصمت الميت |
| 03.2 | Subtle Rising Tension (riser ناعم) | شحن الفضول نحو المعلومة الثانية |
| 03.3 | Soft Texture Whoosh (Pan يسار) | حركة كاميرا ضمنية |
| 06.9 | Atmospheric Sweep L→R | عبور الكاميرا إلى الكرة الأرضية |
| 07.1 | Deep Air Whoosh + Mini Impact | تثبيت نقطة الالتقاء |
| 10.9 | Low Cinematic Hit + Dark Reverb Tail | هيبة العمارة |
| 14.1 | Strong Cinematic Impact | ذروة الفيديو |
| 14.1 | Deep Bass Drop (42→24Hz, 2.8s) | "قرار" نهائي بدون موسيقى |
| 14.2 | Air Swell متلاشٍ | تنفيس حتى النهاية |
| 17.0–18.0 | Fade-out كامل | إغلاق سينمائي |

## الملفات
- `../audio/voiceover_18s_with_sfx.mp3` — **الماستر النهائي** (VO + SFX، بدون موسيقى) ← ضعه تحت الصورة مباشرة.
- `../audio/voiceover_18s_clean.mp3` — VO نظيف للمونتير (تحكم كامل).
- `../audio/sfx_bed_18s.wav` — سرير المؤثرات وحده (استبداله أو تعزيزه).
- `../audio/vo_01..05_*.mp3` — الأسطر الخام قبل المعالجة.

## كيف صُنع السرير؟
`../build/audio_assemble.py` يولّده تركيبياً (numpy): Sub-sine sweeps أسية للانقضاضات، ضجيج مُمرَّر نطاقياً (FFT bandpass) للـ whooshes وrisers، وغلاف جيبي للتضخيم والتلاشي — ثم مزج تحت VO بنسبة ~-12dB peak وتطبيع نهائي -1dBFS.
إعادة البناء: `python3 video/build/audio_assemble.py`

## إن أردت ترقية المؤثرات بمكتبات جاهزة (اختياري)
استبدل أي cue من السرير بعينه من: **Boom Library — Cinematic Trailers / Designed Impacts**، أو **Articulated Sounds — Cinematic Whooshes**، أو **SoundBits — Cinematic Sound Effects Vault**. ضع البديل على نفس التوقيت في الـ Cue Sheet وحافظ على مستوياته تحت الـ VO بـ 8–12dB.

## Loudness للنشر
الماستر مضبوط peak ≈ -1 dBFS. منصات Shorts/Reels/TikTok تعيد التطبيع تلقائياً؛ لا تضف limiter إضافياً ولا ترفع الـ bass فوق ذلك حتى لا يحدث pumping على سماعات الهاتف.

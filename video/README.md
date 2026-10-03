# 🎬 حزمة إنتاج — "هل تعلم عن المغرب؟" (18s · 9:16 · بدون موسيقى)

فيديو عمودي سينمائي بأسلوب تريلر وثائقي هوليوودي، مصمم لأعلى نسبة إكمال مشاهدة على
YouTube Shorts / TikTok / Instagram Reels / Snapchat.

## محتويات الحزمة
| المسار | المحتوى |
|--------|---------|
| `output/hal_ta3lam_an_almaghrib_18s_vertical.mp4` | **الفيديو النهائي**: 18.00s · 1080×1920 · 30fps — مونتاج اللوحات بحركة كاميرا سينمائية، كابشنز عربية محروقة، عنوان ختامي، والماستر الصوتي بدون موسيقى |
| `storyboard/index.html` | **كتاب الإنتاج التفاعلي**: اللقطات الخمس، التوقيتات، طقم النشر، قواعد الاحتفاظ |
| `storyboard/01..05_*.png` | لوحات الستوري بورد العمودية (تُستخدم كـ First Frame في أدوات الفيديو) |
| `audio/voiceover_18s_with_sfx.mp3` | **الماستر النهائي** 18.00s: راوٍ عربي عميق + سرير مؤثرات سينمائي، بدون موسيقى |
| `audio/voiceover_18s_clean.mp3` | التعليق الصوتي نظيفاً للمونتاج |
| `audio/sfx_bed_18s.wav` | سرير المؤثرات وحده (Whooshes / Impacts / Air Swells / Bass Drops) |
| `audio/vo_01..05_*.mp3` | أسطر الراوي الخام |
| `subtitles/captions_ar.srt` | كابشنز عربية مضبوطة على الماستر |
| `prompts/shots.md` | برومبتات لقطة‑بلقطة لـ Hailuo / Wan / Kling + Negative + Style Block |
| `sound_design.md` | Cue Sheet كامل للمؤثرات + قاعدة "لا موسيقى" + خيارات الترقية |
| `build/audio_assemble.py` | سكربت إعادة بناء الصوت كاملاً (trim + tempo موحّد + توليد SFX + مزج + SRT) |

## إعادة بناء الصوت والفيديو من الصفر
```bash
pip install --break-system-packages numpy imageio-ffmpeg arabic-reshaper python-bidi pillow fonttools
python3 video/build/audio_assemble.py   # الماستر الصوتي 18s + SRT
python3 video/build/render_video.py     # الفيديو النهائي 9:16 (خط Amiri مضمّن في build/assets)
```

## شبكة الزمن (القطوع البصرية ثابتة، والصوت يعبرها كجسر)
`0–3 | 3–7 | 7–11 | 11–14 | 14–18` — التفاصيل في `storyboard/index.html`.

## قاعدة النشر الذهبية
لا موسيقى خلفية أبداً. الصمت الدرامي + المؤثرات = بصمة استديو، لا بصمة تطبيق مونتاج.

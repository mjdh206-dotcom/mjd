# 🎬 "هل تعلم عن المغرب؟" — برومبتات اللقطات (Shot-by-Shot Prompts)
**الأدوات المستهدفة:** Hailuo AI (الأفضل لهذا الطابع) ← Wan AI ← Kling AI ← Pixverse / Vidu / Krea
**قاعدة ذهبية:** ارفع لوحة الستوري بورد المطابقة لكل لقطة (من `../storyboard/`) كـ **First Frame / Image-to-Video** حتى تتطابق الحركة مع الهوية البصرية بالضبط، ثم الصق برومبت الحركة أدناه.
**مدة كل لقطة:** أنتجها 4–5 ثوانٍ ثم قصّها على شبكة المونتاج أدناه (كل الأدوات تولّد 4–10 ثوانٍ).
**مهم:** اكتم الصوت الذي تولّده أداة الفيديو تماماً — الصوت النهائي هو `../audio/voiceover_18s_with_sfx.mp3` فقط (لا موسيقى).

---

## GLOBAL STYLE BLOCK (أضفه لأول برومبت أو كـ Style Reference)
```text
CINEMATIC HOLLYWOOD DOCUMENTARY TRAILER look. Vertical 9:16. Photorealistic,
hyper-detailed architectural realism. 35mm film look, anamorphic lens, subtle
cinematic lens haze. Cinematic color grading: teal shadows, warm gold highlights.
Cinematic golden rim light, soft volumetric god rays, moody chiaroscuro backlighting.
Slow, heavy, dramatic camera moves — never fast, never handheld-shaky.
NO music. NO text overlays. NO human faces, NO people, NO flags, NO cars.
NO flash / glitch / swipe transitions — only soft light-and-fog dissolves.
```

## GLOBAL NEGATIVE PROMPT
```text
music, soundtrack, song, text, subtitles, watermark, logo, flags, people, human faces,
crowd, cars, vehicles, airplanes, cartoon, anime, illustration, low quality, blurry,
oversaturated, glitch, flash transitions, swipe, jump cuts, fisheye, modern buildings
```

---

## SHOT 1 — 00:00 → 00:03 | "الخريطة تخرج من الظلام"
**First frame:** `../storyboard/01_map_from_darkness.png`
**كاميرا:** Slow Forward Dolly + Smooth Cinematic Push In (بطيء ثقيل).
**انتقال خروج:** ذوبان عبر الضوء والضباب إلى لقطة 2.
```text
Pure black screen for one beat, then a colossal 3D relief map of Morocco sculpted in
dark marble inlaid with polished gold emerges from darkness through thin cinematic
fog. Cold golden rim light ignites its borders. Very slow heavy dolly push-in,
volumetric god rays, teal shadows, 35mm anamorphic, documentary trailer gravity.
Vertical 9:16. No music, no text, no people.
```
**SFX المقابل:** Deep Cinematic Whoosh + Low Bass Impact @0.1s

## SHOT 2 — 00:03 → 00:07 | "ضوء ذهبي على المدن العتيقة"
**First frame:** `../storyboard/02_aerial_old_cities.png`
**كاميرا:** Cinematic Drone Camera، Slow Forward Dolly فوق الأسوار.
**انتقال خروج:** الضوء الذهبي يملأ الكادر → ذوبان إلى لقطة 3.
```text
Cinematic aerial drone glide at dawn over an ancient Moroccan medina: ramparts,
carved doors, Andalusian arches, green tiled roofs, empty winding alleys. Golden
light slowly creeps between the historic buildings and ignites the ground step by
step. Slow forward dolly, dust haze, volumetric god rays, teal-and-gold grading,
35mm anamorphic film look. Vertical 9:16. No people, no cars, no music, no text.
```
**SFX المقابل:** Subtle Rising Tension + Soft Cinematic Texture @3.3s

## SHOT 3 — 00:07 → 00:11 | "نقطة التقاء القارات الثلاث"
**First frame:** `../storyboard/03_earth_crossroads.png`
**كاميرا:** Dramatic Orbit حول الكرة ثم Smooth Cinematic Push In سريع-ثقيل نحو شمال أفريقيا.
**انتقال خروج:** اقتراب شديد حتى يملأ التوهج الذهبي الكادر → ذوبان إلى لقطة 4.
```text
Dark moody 3D Earth seen from space. Camera orbits dramatically then dives in a
heavy cinematic push toward North-West Africa, where Morocco glows as one huge
golden point of light at the meeting point of the Arab world, Africa and Europe,
thin luminous arcs faintly connecting the three continents across the Mediterranean.
Deep black atmosphere, golden rim light, volumetric haze, anamorphic 35mm look.
Vertical 9:16. No labels, no text, no satellites, no music.
```
**SFX المقابل:** Atmospheric Sweep + Deep Air Whoosh @6.9s

## SHOT 4 — 00:11 → 00:14 | "بوابة بين الحضارات"
**First frame:** `../storyboard/04_architecture_details.png`
**كاميرا:** Smooth Cinematic Push In بزاوية منخفضة نحو القوس/الباب.
**انتقال خروج:** شعاع الضوء يتسع → قطع ناعم علىImpact إلى لقطة 5.
```text
Monumental Moroccan architecture: giant Moorish-Andalusian horseshoe arch, intricate
geometric zellige tilework, carved plaster muqarnas, ornate cedar door with ironwork.
Low-angle slow push-in, dark chiaroscuro, strong golden rim light grazing the
carvings, dust particles in volumetric god rays, hyper-detailed photorealistic
materials, 35mm anamorphic, majestic and solemn. Vertical 9:16. No people, no music.
```
**SFX المقابل:** Low Cinematic Hit + Dark Reverb Impact @10.9s

## SHOT 5 — 00:14 → 00:18 | "الخريطة المضيئة + العنوان"
**First frame:** `../storyboard/05_final_golden_map.png`
**كاميرا:** Cinematic Zoom Out بطيء ثم ثبات مهيب.
**نهاية:** إضافة نص العنوان في المونتاج (الثلث العلوي الفارغ): **«هل تعلم عن المغرب ؟»** بخط عربي سينمائي ذهبي، ثم Fade to Black @17.2–18.0.
```text
Dramatic slow zoom-out: the 3D marble-and-gold relief map of Morocco now fully
illuminated, glowing warm gold, floating in deep black space with thin fog and a
soft golden aura. Camera settles into a reverent hold. Upper third stays clean dark
space for a title. Cinematic teal shadows, god rays, 35mm anamorphic haze.
Vertical 9:16. No text generated in-frame, no music, fade to black at the end.
```
**SFX المقابل:** Strong Cinematic Impact + Deep Bass Drop (بدون موسيقى) @14.1s + تتلاشى الأجواء 17→18

---

## شبكة المونتاج النهائية (Edit Timeline)
| # | القص | بداية VO الفعلية | نهاية VO الفعلية |
|---|------|------------------|------------------|
| 1 | 0.00–3.00 | 0.10 | 3.56 |
| 2 | 3.00–7.00 | 3.68 | 7.22 |
| 3 | 7.00–11.00 | 7.34 | 10.97 |
| 4 | 11.00–14.00 | 11.09 | 14.02 |
| 5 | 14.00–18.00 | 14.14 | 17.89 |

> الصوت يعبر القصود بثوانٍ معدودة (Audio Bridge) — هذه تقنية مقصودة تربط المشاهد ببعضها وتجعل القطع يشعر بأنه "نَفَس واحد".
> الكابشنز المحروقة: استخدم `../subtitles/captions_ar.srt` كما هي (توقيتاتها مضبوطة على الماستر النهائي).

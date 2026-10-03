#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Renders the final 18.000s 9:16 vertical video for "هل تعلم عن المغرب؟"
- cinematic Ken-Burns camera moves on the 5 storyboard plates (zoompan)
- soft light dissolves between shots (xfade), fade-to-black ending
- film grain + gentle vignette (no music ever — audio = VO+SFX master)
- burned-in Arabic captions + final title (PIL shaping + bidi -> PNG overlays)
Output: video/output/hal_ta3lam_an_almaghrib_18s_vertical.mp4
"""
import os, subprocess, glob
import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter
import arabic_reshaper
from bidi.algorithm import get_display
import imageio_ffmpeg

FF = imageio_ffmpeg.get_ffmpeg_exe()
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
ASSETS = os.path.join(ROOT, "build", "assets")
OUT = os.path.join(ROOT, "output")
os.makedirs(OUT, exist_ok=True)
W, H, FPS, D = 1080, 1920, 30, 125          # per-shot frames (4.1667s)
GOLD = (232, 201, 135, 255)

def shaped(text):
    return "\n".join(get_display(arabic_reshaper.reshape(ln)) for ln in text.split("\n"))

def make_overlay(path, text, size, y, weight="Amiri-Bold.ttf"):
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    font = ImageFont.truetype(os.path.join(ASSETS, weight), size)
    probe = ImageDraw.Draw(img)
    max_w = W - 140
    while size > 40:
        bb = probe.multiline_textbbox((0, 0), shaped(text), font=font, spacing=22, align="center")
        if bb[2] - bb[0] <= max_w:
            break
        size -= 6
        font = ImageFont.truetype(os.path.join(ASSETS, weight), size)
    # soft black glow behind
    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.multiline_text((W/2, y), shaped(text), font=font, fill=(0, 0, 0, 200),
                      anchor="mm", spacing=22, align="center")
    glow = glow.filter(ImageFilter.GaussianBlur(9))
    img = Image.alpha_composite(img, glow)
    d = ImageDraw.Draw(img)
    d.multiline_text((W/2, y), shaped(text), font=font, fill=GOLD,
                     anchor="mm", spacing=22, align="center",
                     stroke_width=3, stroke_fill=(0, 0, 0, 230))
    img.save(path)

CAPS = [  # (text, size, y, in, out, weight)
    ("هوية لم تنقطع منذ اثني عشر قرناً.", 78, 1470, 3.70, 6.90, "Amiri-Bold.ttf"),
    ("عند نقطة التقاء العالم العربي،\nوأفريقيا، وأوروبا.", 78, 1470, 7.30, 10.90, "Amiri-Bold.ttf"),
    ("لهذا كان بوابة الحضارات عبر التاريخ.", 78, 1470, 11.10, 13.95, "Amiri-Bold.ttf"),
    ("هل تعلم عن المغرب ؟", 150, 420, 14.30, 17.30, "Amiri-Bold.ttf"),
]
cap_paths = []
for i, (txt, sz, y, t0, t1, wt) in enumerate(CAPS):
    p = os.path.join(ASSETS, f"cap{i}.png")
    make_overlay(p, txt, sz, y, wt)
    cap_paths.append(p)

SHOTS = [  # (image, zoom expr, y expr)
    ("01_map_from_darkness.png",  "1.00+0.14*on/125", "ih/2-(ih/zoom/2)"),
    ("02_aerial_old_cities.png",  "1.05+0.12*on/125", "(ih/2-(ih/zoom/2))+(on/125)*ih*0.02"),
    ("03_earth_crossroads.png",   "1.00+0.30*pow(on/125,1.4)", "ih/2-(ih/zoom/2)"),
    ("04_architecture_details.png","1.08+0.10*on/125", "ih/2-(ih/zoom/2)"),
    ("05_final_golden_map.png",   "1.20-0.18*on/125", "ih/2-(ih/zoom/2)"),
]
fc = []
for i, (img, z, y) in enumerate(SHOTS):
    fc.append(
        f"[{i}:v]scale=1620:2880:force_original_aspect_ratio=increase,crop=1620:2880,"
        f"zoompan=z='{z}':x='iw/2-(iw/zoom/2)':y='{y}':d={D}:s={W}x{H}:fps={FPS},"
        f"setsar=1[v{i}]")
OFF = [3.4667, 6.9333, 10.4, 13.8667]
fc.append(f"[v0][v1]xfade=transition=fade:duration=0.7:offset={OFF[0]}[x1]")
fc.append(f"[x1][v2]xfade=transition=fade:duration=0.7:offset={OFF[1]}[x2]")
fc.append(f"[x2][v3]xfade=transition=fade:duration=0.7:offset={OFF[2]}[x3]")
fc.append(f"[x3][v4]xfade=transition=fade:duration=0.7:offset={OFF[3]}[x4]")
fc.append(f"[x4]fade=t=out:st=17.2:d=0.8,noise=alls=4:allf=t,vignette=a=PI/6,format=yuv420p[base]")
for i, (txt, sz, y, t0, t1, wt) in enumerate(CAPS):
    fc.append(f"[{6+i}:v]format=rgba,fade=t=in:st={t0}:d=0.35:alpha=1,"
              f"fade=t=out:st={t1-0.35}:d=0.35:alpha=1[c{i}]")
prev = "base"
for i in range(len(CAPS)):
    nxt = f"b{i}"
    fc.append(f"[{prev}][c{i}]overlay=0:0:eof_action=pass[{nxt}]")
    prev = nxt
filter_complex = ";".join(fc)

cmd = [FF, "-hide_banner", "-loglevel", "error", "-y"]
for img, _, _ in SHOTS:
    cmd += ["-i", os.path.join(ROOT, "storyboard", img)]
cmd += ["-i", os.path.join(ROOT, "audio", "voiceover_18s_with_sfx.wav")]
for p in cap_paths:
    cmd += ["-loop", "1", "-t", "18", "-framerate", str(FPS), "-i", p]
cmd += ["-filter_complex", filter_complex,
        "-map", f"[{prev}]", "-map", "5:a",
        "-t", "18", "-r", str(FPS),
        "-c:v", "libx264", "-crf", "18", "-preset", "medium", "-pix_fmt", "yuv420p",
        "-c:a", "aac", "-b:a", "192k", "-movflags", "+faststart",
        os.path.join(OUT, "hal_ta3lam_an_almaghrib_18s_vertical.mp4")]
r = subprocess.run(cmd, capture_output=True, text=True)
if r.returncode != 0:
    print(r.stderr[-3000:]); raise SystemExit(1)
pr = subprocess.run([FF, "-i", os.path.join(OUT, "hal_ta3lam_an_almaghrib_18s_vertical.mp4")],
                    capture_output=True, text=True)
print([l.strip() for l in pr.stderr.splitlines() if "Duration" in l or "Stream" in l])
print("RENDER OK")

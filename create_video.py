#!/usr/bin/env python3
"""Render a seamless, Arabic, vertical deep-sea short from the local source assets."""
from __future__ import annotations

import math
import os
import subprocess
import sys
import wave
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageEnhance, ImageFont, ImageOps
import arabic_reshaper
from bidi.algorithm import get_display
import imageio_ffmpeg

ROOT = Path(__file__).resolve().parent
ASSET_DIR = ROOT / "video_assets"
OUT = ROOT / "black-seadevil-loop.mp4"
W, H = 1080, 1920
FPS = 24
DURATION = 38.0
SAMPLES_PER_FRAME = 1.0 / FPS
AUDIO_SR = 48000
FONT_PATH = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
FONT_BOLD_PATH = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"

# Timed shots: image, push-in, and focal drift. Each new shot cross-dissolves in.
SCENES = [
    {"start": 0.0,  "image": "hero",     "zoom": (1.015, 1.095), "focus": ((0.50, 0.53), (0.48, 0.51))},
    {"start": 4.0,  "image": "abyss",    "zoom": (1.02, 1.16),  "focus": ((0.53, 0.53), (0.51, 0.52))},
    {"start": 10.9, "image": "closeup", "zoom": (1.00, 1.095), "focus": ((0.49, 0.56), (0.50, 0.54))},
    {"start": 17.7, "image": "closeup", "zoom": (1.10, 1.20),  "focus": ((0.50, 0.57), (0.52, 0.55))},
    {"start": 23.8, "image": "pair",     "zoom": (1.015, 1.095), "focus": ((0.50, 0.56), (0.48, 0.54))},
    {"start": 30.2, "image": "hero",     "zoom": (1.07, 1.13),  "focus": ((0.49, 0.54), (0.50, 0.53))},
    {"start": 34.0, "image": "closeup", "zoom": (1.04, 1.10),  "focus": ((0.50, 0.54), (0.50, 0.54))},
    # The last shot deliberately returns to the opening hero composition.
    {"start": 36.0, "image": "hero",     "zoom": (1.10, 1.015), "focus": ((0.50, 0.54), (0.50, 0.53))},
]
TRANSITION = 0.82

CAPTIONS = [
    {"start": 3.78, "end": 11.95, "tag": "في ظلام المحيط السحيق", "lines": ["سمكة الشيطان الأسود"]},
    {"start": 11.82, "end": 18.10, "tag": "النتوء المضيء فوق رأسها", "lines": ["ليس زينةً... بل طُعمٌ."]},
    {"start": 18.03, "end": 24.30, "tag": "فمٌ واسع وأسنانٌ منحنية", "lines": ["تُمسك بالفريسة."]},
    {"start": 24.20, "end": 31.00, "tag": "والأغرب؟", "lines": ["في بعض الأنواع، الذكر أصغر بكثير،", "ويلتصق بالأنثى طوال حياته."]},
    {"start": 30.92, "end": 34.42, "tag": "استراتيجية الصيد", "lines": ["لا تطارد الضوء...", "بل تنتظر فريستها."]},
    {"start": 34.25, "end": 36.95, "tag": "لو ظهر لك هذا الضوء...", "lines": ["هل كنت ستقترب؟"]},
]


def shaped(text: str) -> str:
    """Return Arabic text in the visual order expected by Pillow without RAQM."""
    return get_display(arabic_reshaper.reshape(text))


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(FONT_BOLD_PATH if bold else FONT_PATH, size=size)


def ease(v: float) -> float:
    v = max(0.0, min(1.0, v))
    return v * v * (3.0 - 2.0 * v)


def fade_alpha(t: float, start: float, end: float, fi: float = 0.30, fo: float = 0.30,
               full_at_start: bool = False) -> float:
    if t < start or t > end:
        return 0.0
    a = 1.0 if full_at_start else ease((t - start) / max(fi, 0.001))
    b = ease((end - t) / max(fo, 0.001))
    return min(a, b)


def fade_layer(layer: Image.Image, amount: float) -> Image.Image:
    """Apply opacity to a small RGBA overlay while keeping its RGB intact."""
    if amount >= 0.999:
        return layer
    result = layer.copy()
    alpha = result.getchannel("A").point(lambda x: int(x * max(0.0, min(1.0, amount))))
    result.putalpha(alpha)
    return result


def fit_scene(path: Path) -> Image.Image:
    im = Image.open(path).convert("RGB")
    im = ImageOps.fit(im, (1200, 2134), method=Image.Resampling.LANCZOS, centering=(0.5, 0.5))
    # Slightly restrained saturation and lifted shadows keep the blue/amber look consistent.
    im = ImageEnhance.Color(im).enhance(0.88)
    im = ImageEnhance.Contrast(im).enhance(1.045)
    im = ImageEnhance.Brightness(im).enhance(0.97)
    return im


def build_backgrounds() -> dict[str, Image.Image]:
    files = {
        "hero": ASSET_DIR / "anglerfish_hero.png",
        "abyss": ASSET_DIR / "abyss_lure.png",
        "closeup": ASSET_DIR / "anglerfish_closeup.png",
        "pair": ASSET_DIR / "anglerfish_pair.png",
    }
    return {key: fit_scene(path) for key, path in files.items()}


def scene_frame(image: Image.Image, scene: dict, progress: float) -> Image.Image:
    z0, z1 = scene["zoom"]
    z = z0 + (z1 - z0) * ease(progress)
    (fx0, fy0), (fx1, fy1) = scene["focus"]
    fx = fx0 + (fx1 - fx0) * ease(progress)
    fy = fy0 + (fy1 - fy0) * ease(progress)
    crop_w = min(1200, W / z)
    crop_h = min(2134, H / z)
    x0 = max(0.0, min(1200 - crop_w, fx * 1200 - crop_w / 2))
    y0 = max(0.0, min(2134 - crop_h, fy * 2134 - crop_h / 2))
    crop = image.crop((int(x0), int(y0), int(x0 + crop_w), int(y0 + crop_h)))
    return crop.resize((W, H), Image.Resampling.LANCZOS)


def render_shot(t: float, backgrounds: dict[str, Image.Image]) -> Image.Image:
    idx = 0
    for i, scene in enumerate(SCENES):
        if scene["start"] <= t:
            idx = i
        else:
            break
    current = SCENES[idx]
    next_start = SCENES[idx + 1]["start"] if idx + 1 < len(SCENES) else DURATION
    progress = (t - current["start"]) / max(next_start - current["start"], 0.001)
    current_img = scene_frame(backgrounds[current["image"]], current, progress)

    # A short, smooth cross-dissolve on each cut; no hard flashes between generated stills.
    if idx > 0 and t < current["start"] + TRANSITION:
        prev = SCENES[idx - 1]
        prev_next = current["start"]
        prev_progress = (t - prev["start"]) / max(prev_next - prev["start"], 0.001)
        prev_img = scene_frame(backgrounds[prev["image"]], prev, prev_progress)
        a = ease((t - current["start"]) / TRANSITION)
        current_img = Image.blend(prev_img, current_img, a)

    # Restrained top/bottom scrim for readable typography, while retaining the dark scene.
    rgba = current_img.convert("RGBA")
    scrim = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    sd = ImageDraw.Draw(scrim)
    # Top gradient: strongest behind the hook and fading before the lure.
    for y in range(0, 760, 4):
        alpha = int(48 * max(0.0, 1.0 - y / 780.0) ** 1.35)
        if alpha:
            sd.rectangle((0, y, W, min(H, y + 4)), fill=(1, 7, 15, alpha))
    # Gentle lower fade behind the information card.
    for y in range(1260, H, 4):
        alpha = int(30 * max(0.0, (y - 1260) / (H - 1260)) ** 1.3)
        if alpha:
            sd.rectangle((0, y, W, min(H, y + 4)), fill=(1, 7, 15, alpha))
    rgba = Image.alpha_composite(rgba, scrim)
    return rgba.convert("RGB")


def draw_rtl(draw: ImageDraw.ImageDraw, text: str, xy: tuple[int, int], fnt: ImageFont.FreeTypeFont,
             fill: tuple[int, int, int, int], anchor: str = "mm", stroke_width: int = 0,
             stroke_fill: tuple[int, int, int, int] = (0, 0, 0, 0)) -> None:
    draw.text(xy, shaped(text), font=fnt, fill=fill, anchor=anchor,
              stroke_width=stroke_width, stroke_fill=stroke_fill)


def make_header() -> Image.Image:
    layer = Image.new("RGBA", (W, 190), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    # Fine cyan rule and amber locator dot.
    d.rounded_rectangle((68, 126, 210, 130), radius=2, fill=(88, 205, 218, 175))
    d.ellipse((52, 119, 64, 131), fill=(255, 190, 106, 245))
    draw_rtl(d, "حكايات الأعماق", (1010, 129), font(28, True), (214, 238, 240, 235), anchor="rm")
    draw_rtl(d, "مشاهد تخيّلية", (1010, 166), font(18, False), (137, 184, 193, 218), anchor="rm")
    return layer


def make_hook() -> Image.Image:
    layer = Image.new("RGBA", (W, 560), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    draw_rtl(d, "انظر إلى الضوء...", (W // 2, 202), font(28, True), (124, 220, 231, 245))
    draw_rtl(d, "هذا الضوء الصغير؟", (W // 2, 302), font(73, True), (249, 251, 251, 255), stroke_width=2,
             stroke_fill=(0, 7, 15, 190))
    draw_rtl(d, "قد يكون آخر شيء تراه.", (W // 2, 404), font(66, True), (255, 192, 112, 255), stroke_width=2,
             stroke_fill=(0, 7, 15, 210))
    # Short editorial accent, centered under the hook.
    d.rounded_rectangle((W // 2 - 44, 465, W // 2 + 44, 470), radius=2, fill=(255, 184, 93, 225))
    return layer


def make_card(tag: str, lines: list[str]) -> Image.Image:
    cw, ch = 950, 258
    layer = Image.new("RGBA", (cw, ch), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    d.rounded_rectangle((2, 2, cw - 3, ch - 3), radius=27,
                        fill=(3, 13, 24, 202), outline=(96, 190, 205, 116), width=2)
    d.rounded_rectangle((cw - 22, 50, cw - 16, ch - 50), radius=3, fill=(255, 185, 99, 225))
    draw_rtl(d, tag, (cw - 42, 60), font(27, True), (123, 222, 232, 248), anchor="rm")
    if len(lines) == 1:
        size = 53
        # Reduce only long single-line captions to stay inside the safe width.
        measure = font(size, True).getlength(shaped(lines[0]))
        if measure > cw - 96:
            size = max(43, int(size * (cw - 96) / measure))
        draw_rtl(d, lines[0], (cw - 42, 155), font(size, True), (249, 250, 249, 255), anchor="rm",
                 stroke_width=1, stroke_fill=(0, 8, 15, 175))
    else:
        size = 43 if max(font(46, True).getlength(shaped(s)) for s in lines) > cw - 100 else 46
        draw_rtl(d, lines[0], (cw - 42, 132), font(size, True), (249, 250, 249, 255), anchor="rm",
                 stroke_width=1, stroke_fill=(0, 8, 15, 175))
        draw_rtl(d, lines[1], (cw - 42, 190), font(size, True), (255, 196, 119, 255), anchor="rm",
                 stroke_width=1, stroke_fill=(0, 8, 15, 175))
    return layer


def apply_layer(frame: Image.Image, layer: Image.Image, xy: tuple[int, int], opacity: float = 1.0) -> None:
    if opacity <= 0.004:
        return
    use = fade_layer(layer, opacity)
    frame.paste(use, xy, use.getchannel("A"))


def draw_particles(frame: Image.Image, frame_index: int, total_frames: int) -> None:
    d = ImageDraw.Draw(frame, "RGBA")
    phase = 2 * math.pi * frame_index / max(total_frames - 1, 1)
    # Gentle, periodic marine snow. The final frame is copied from the opening frame below.
    for j in range(34):
        base_x = (j * 317 + 83) % W
        base_y = (j * 547 + 191) % H
        theta = j * 2.399963229728653
        x = int((base_x + 18 * math.sin(phase + theta)) % W)
        y = int((base_y + 27 * math.cos(phase + theta * 0.71)) % H)
        twinkle = 0.55 + 0.45 * math.sin(phase + theta * 1.31) ** 2
        alpha = int((24 + (j % 5) * 9) * twinkle)
        r = 1 if j % 4 else 2
        color = (115, 207, 222, alpha) if j % 3 else (255, 194, 113, alpha)
        d.ellipse((x - r, y - r, x + r, y + r), fill=color)


def prepare_overlays() -> tuple[Image.Image, Image.Image, list[Image.Image]]:
    header = make_header()
    hook = make_hook()
    cards = [make_card(c["tag"], c["lines"]) for c in CAPTIONS]
    return header, hook, cards


def create_ambient(path: Path) -> None:
    """Create an original, quiet, periodic deep-ocean drone (no third-party music)."""
    n = int(DURATION * AUDIO_SR)
    t = np.arange(n, dtype=np.float64) / AUDIO_SR
    # Integer-cycle oscillators make the bed wrap cleanly at the end of the video.
    def osc(target_hz: float, phase: float = 0.0) -> np.ndarray:
        cycles = max(1, round(target_hz * DURATION))
        return np.sin((2.0 * np.pi * cycles / DURATION) * t + phase)

    lfo = 0.5 + 0.5 * osc(0.08, 0.4)
    low = 0.038 * osc(43.0, 0.15) + 0.022 * osc(54.0, 1.8) + 0.014 * osc(65.0, 0.8)
    mid = 0.008 * osc(108.0, 0.6) + 0.006 * osc(162.0, 2.1)
    glass = 0.0032 * lfo * osc(286.0, 0.4) + 0.0021 * (1.0 - lfo) * osc(382.0, 1.1)
    # Low-level interpolated noise gives the pad a soft underwater texture.
    rng = np.random.default_rng(846)
    coarse_count = int(DURATION * 120)
    coarse = rng.normal(0.0, 1.0, coarse_count)
    phase = np.arange(n, dtype=np.float64) * coarse_count / n
    noise = np.interp(phase, np.arange(coarse_count + 1), np.r_[coarse, coarse[0]])
    wash = 0.0024 * noise * (0.65 + 0.35 * lfo)
    mono = low + mid + glass + wash
    left = mono
    right = 0.91 * low + 0.98 * mid + 1.12 * glass + 0.0022 * np.roll(noise, 23) * (0.65 + 0.35 * lfo)
    stereo = np.stack((left, right), axis=1)
    peak = float(np.max(np.abs(stereo)))
    stereo = np.clip(stereo / max(1.0, peak / 0.12), -0.98, 0.98)
    pcm = (stereo * 32767.0).astype("<i2")
    with wave.open(str(path), "wb") as wf:
        wf.setnchannels(2)
        wf.setsampwidth(2)
        wf.setframerate(AUDIO_SR)
        wf.writeframes(pcm.tobytes())


def render() -> None:
    backgrounds = build_backgrounds()
    header, hook, cards = prepare_overlays()
    total_frames = round(DURATION * FPS)
    ambient_path = ASSET_DIR / "ambient_loop.wav"
    create_ambient(ambient_path)
    ffmpeg = imageio_ffmpeg.get_ffmpeg_exe()
    filter_complex = (
        "[1:a]atempo=1.08,aresample=48000,highpass=f=75,volume=1.10[voice];"
        "[2:a]volume=0.74[bed];"
        "[voice][bed]amix=inputs=2:duration=longest:dropout_transition=0,"
        "alimiter=limit=0.95,loudnorm=I=-16:TP=-1.5:LRA=10[a]"
    )
    cmd = [
        ffmpeg, "-hide_banner", "-loglevel", "error", "-y",
        "-f", "rawvideo", "-pix_fmt", "rgb24", "-s", f"{W}x{H}", "-r", str(FPS), "-i", "pipe:0",
        "-i", str(ASSET_DIR / "arabic_narration.mp3"), "-i", str(ambient_path),
        "-filter_complex", filter_complex,
        "-map", "0:v:0", "-map", "[a]", "-t", f"{DURATION:.3f}",
        "-c:v", "libx264", "-preset", "medium", "-crf", "19", "-profile:v", "high",
        "-pix_fmt", "yuv420p", "-r", str(FPS), "-g", str(FPS * 2),
        "-c:a", "aac", "-b:a", "192k", "-ar", "48000", "-ac", "2",
        "-metadata", "title=سمكة الشيطان الأسود | حكايات الأعماق",
        "-metadata", "comment=Vertical Arabic short with a seamless visual loop",
        "-movflags", "+faststart", str(OUT),
    ]
    print(f"Rendering {total_frames} frames at {W}x{H}, {FPS} fps -> {OUT.name}", flush=True)
    proc = subprocess.Popen(cmd, stdin=subprocess.PIPE, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)
    first_frame: Image.Image | None = None
    try:
        assert proc.stdin is not None
        for idx in range(total_frames):
            if idx == total_frames - 1 and first_frame is not None:
                frame = first_frame.copy()
            else:
                t = idx / FPS
                frame = render_shot(t, backgrounds)
                draw_particles(frame, idx, total_frames)
                # Fixed editorial slug.
                apply_layer(frame, header, (0, 0), 1.0)
                # Opening hook: immediate at frame zero, then disappears; it returns at the end.
                if t <= 3.82:
                    a = fade_alpha(t, 0.0, 3.82, fi=0.001, fo=0.42, full_at_start=True)
                    apply_layer(frame, hook, (0, 0), a)
                elif t >= 36.18:
                    a = fade_alpha(t, 36.18, DURATION, fi=0.78, fo=0.001)
                    apply_layer(frame, hook, (0, 0), a)
                # One compact lower-third at a time; the final question flows back to the hook.
                for card_idx, card_spec in enumerate(CAPTIONS):
                    a = fade_alpha(t, card_spec["start"], card_spec["end"], fi=0.28, fo=0.32)
                    if a > 0.004:
                        apply_layer(frame, cards[card_idx], (65, 1408), a)
                if idx == 0:
                    first_frame = frame.copy()
            proc.stdin.write(frame.tobytes())
            if idx % 120 == 0:
                print(f"  {idx:4d}/{total_frames} frames", flush=True)
        proc.stdin.close()
    except BrokenPipeError:
        if proc.stdin:
            proc.stdin.close()
        error = proc.stderr.read().decode("utf-8", "replace") if proc.stderr else ""
        raise RuntimeError("ffmpeg stopped while encoding:\n" + error)
    stderr = proc.stderr.read().decode("utf-8", "replace") if proc.stderr else ""
    code = proc.wait()
    if code:
        raise RuntimeError(f"ffmpeg exited with {code}:\n{stderr}")
    print(f"Done: {OUT} ({OUT.stat().st_size / 1_000_000:.1f} MB)", flush=True)


if __name__ == "__main__":
    render()

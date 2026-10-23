#!/usr/bin/env python3
"""
Render a broadcast-grade vertical short (<30s) in clear Syrian Damascene Arabic
celebrating the Syrian Arab Republic (الجمهورية العربية السورية), complete with
synchronized multi-layer cinematic sound effects (SFX), Levantine oud/qanun bed,
reactive audio visualizer, HarfBuzz-shaped Tajawal typography, and official symbols.
"""
from __future__ import annotations

import math
import re
import subprocess
import sys
import wave
from pathlib import Path

import freetype
import imageio_ffmpeg
import numpy as np
import uharfbuzz as hb
from PIL import Image, ImageDraw, ImageEnhance, ImageFilter, ImageOps
from scipy import signal

ROOT = Path(__file__).resolve().parent
ASSET_DIR = ROOT / "syria_video_assets"
OUT_VIDEO = ROOT / "syrian-arab-republic.mp4"

FONT_DIR = ROOT / "shuaa-remote" / "app" / "src" / "main" / "res" / "font"
FONT_EXTRABOLD = str(FONT_DIR / "tajawal_extrabold.ttf")
FONT_BOLD = str(FONT_DIR / "tajawal_bold.ttf")
FONT_MEDIUM = str(FONT_DIR / "tajawal_medium.ttf")

W, H = 1080, 1920
FPS = 30
AUDIO_SR = 48000

# Official Syrian Flag & Visual Identity Palette
COL_FLAG_GREEN = (0, 122, 61)
COL_FLAG_WHITE = (248, 250, 249)
COL_FLAG_BLACK = (18, 20, 20)
COL_FLAG_RED = (206, 17, 38)

COL_EMERALD_DEEP = (6, 30, 23)
COL_GOLD_BRIGHT = (250, 220, 138)
COL_GOLD_RICH = (224, 178, 76)
COL_GOLD_DARK = (166, 118, 30)
COL_TEXT_WHITE = (250, 252, 250)


# ---------------------------------------------------------------------------
# 1. HarfBuzz + FreeType Native OpenType Arabic Text Renderer
# ---------------------------------------------------------------------------
_FONT_BYTES_CACHE: dict[str, bytes] = {}


def render_arabic_text(
    text: str,
    font_path: str,
    size_px: int,
    color: tuple[int, int, int, int] = (255, 255, 255, 255),
    stroke_px: int = 0,
    stroke_color: tuple[int, int, int, int] = (4, 16, 12, 220),
    shadow: bool = True,
) -> Image.Image:
    if font_path not in _FONT_BYTES_CACHE:
        _FONT_BYTES_CACHE[font_path] = Path(font_path).read_bytes()
    fontdata = _FONT_BYTES_CACHE[font_path]

    hb_face = hb.Face(fontdata)
    hb_font = hb.Font(hb_face)
    hb_font.scale = (size_px * 64, size_px * 64)

    # Split into RTL text runs vs LTR numeric runs so digits like ١٤ / 14 never invert
    tokens = [tok for tok in re.split(r"([0-9\u0660-\u0669]+)", text) if tok]
    # Visual order for an overall RTL line is reversed token order; each numeric token shapes LTR
    visual_runs = list(reversed(tokens))

    ft_face = freetype.Face(font_path)
    ft_face.set_pixel_sizes(0, size_px)

    pen_x, pen_y = 0.0, 0.0
    glyph_draws: list[tuple[int, int, int, int, np.ndarray]] = []
    min_x, min_y, max_x, max_y = 10**9, 10**9, -(10**9), -(10**9)

    for run_str in visual_runs:
        is_num = bool(re.fullmatch(r"[0-9\u0660-\u0669]+", run_str))
        buf = hb.Buffer()
        buf.add_str(run_str)
        buf.direction = "ltr" if is_num else "rtl"
        buf.script = "Latn" if is_num else "Arab"
        buf.language = "ar"
        hb.shape(hb_font, buf, {"kern": True, "liga": True, "calt": True, "mark": True, "mkmk": True})

        for info, pos in zip(buf.glyph_infos, buf.glyph_positions):
            gid = info.codepoint
            ft_face.load_glyph(gid, freetype.FT_LOAD_RENDER | freetype.FT_LOAD_TARGET_NORMAL)
            bmp = ft_face.glyph.bitmap
            left = ft_face.glyph.bitmap_left
            top = ft_face.glyph.bitmap_top
            gx = int(round(pen_x + pos.x_offset / 64.0)) + left
            gy = int(round(pen_y - pos.y_offset / 64.0)) - top
            bw, bh = bmp.width, bmp.rows
            if bw > 0 and bh > 0:
                arr = np.array(bmp.buffer, dtype=np.uint8).reshape(bh, bw)
                glyph_draws.append((gx, gy, bw, bh, arr))
                min_x = min(min_x, gx)
                min_y = min(min_y, gy)
                max_x = max(max_x, gx + bw)
                max_y = max(max_y, gy + bh)
            pen_x += pos.x_advance / 64.0
            pen_y += pos.y_advance / 64.0

    if not glyph_draws:
        return Image.new("RGBA", (4, 4), (0, 0, 0, 0))

    pad = stroke_px + 14
    tw = (max_x - min_x) + 2 * pad
    th = (max_y - min_y) + 2 * pad
    alpha = np.zeros((th, tw), dtype=np.uint8)
    for gx, gy, bw, bh, arr in glyph_draws:
        x0 = gx - min_x + pad
        y0 = gy - min_y + pad
        alpha[y0 : y0 + bh, x0 : x0 + bw] = np.maximum(alpha[y0 : y0 + bh, x0 : x0 + bw], arr)

    mask_im = Image.fromarray(alpha, mode="L")
    out = Image.new("RGBA", (tw, th), (0, 0, 0, 0))

    if shadow:
        sh_mask = mask_im.filter(ImageFilter.GaussianBlur(5.0))
        sh_layer = Image.new("RGBA", (tw, th), (2, 10, 8, 0))
        sh_layer.putalpha(sh_mask.point(lambda a: int(a * 0.62)))
        shifted = Image.new("RGBA", (tw, th), (0, 0, 0, 0))
        shifted.paste(sh_layer, (0, 4))
        out = Image.alpha_composite(out, shifted)

    if stroke_px > 0:
        dilated = mask_im.filter(ImageFilter.MaxFilter(stroke_px * 2 + 1)).filter(ImageFilter.GaussianBlur(0.9))
        s_layer = Image.new("RGBA", (tw, th), stroke_color[:3] + (0,))
        s_layer.putalpha(dilated.point(lambda a: int(a * (stroke_color[3] / 255.0))))
        out = Image.alpha_composite(out, s_layer)

    f_layer = Image.new("RGBA", (tw, th), color[:3] + (0,))
    f_layer.putalpha(mask_im.point(lambda a: int(a * (color[3] / 255.0))))
    out = Image.alpha_composite(out, f_layer)
    return out


# ---------------------------------------------------------------------------
# 2. Vector Symbols: Official Flag & Golden Eagle + 3 Stars Emblem
# ---------------------------------------------------------------------------
def star_points(cx: float, cy: float, r_outer: float, r_inner: float | None = None, rot_deg: float = -90.0) -> list[tuple[float, float]]:
    if r_inner is None:
        r_inner = r_outer * 0.382
    pts = []
    for k in range(10):
        ang = math.radians(rot_deg + k * 36.0)
        r = r_outer if k % 2 == 0 else r_inner
        pts.append((cx + r * math.cos(ang), cy + r * math.sin(ang)))
    return pts


def make_syrian_flag_icon(width: int, height: int, radius: int = 8, border: bool = True) -> Image.Image:
    ss = 3
    w, h = width * ss, height * ss
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    flag = Image.new("RGBA", (w, h), (0, 0, 0, 255))
    d = ImageDraw.Draw(flag)
    b1 = h // 3
    b2 = (2 * h) // 3
    d.rectangle((0, 0, w, b1), fill=COL_FLAG_GREEN + (255,))
    d.rectangle((0, b1, w, b2), fill=COL_FLAG_WHITE + (255,))
    d.rectangle((0, b2, w, h), fill=COL_FLAG_BLACK + (255,))

    cy = (b1 + b2) / 2.0
    sr = (b2 - b1) * 0.34
    for sx in (w * 0.27, w * 0.50, w * 0.73):
        d.polygon(star_points(sx, cy, sr), fill=COL_FLAG_RED + (255,))

    mask = Image.new("L", (w, h), 0)
    md = ImageDraw.Draw(mask)
    md.rounded_rectangle((0, 0, w - 1, h - 1), radius=radius * ss, fill=255)
    im.paste(flag, (0, 0), mask)

    if border:
        bd = ImageDraw.Draw(im)
        bd.rounded_rectangle((1, 1, w - 2, h - 2), radius=radius * ss, outline=COL_GOLD_RICH + (220,), width=max(2, ss))
    return im.resize((width, height), Image.Resampling.LANCZOS)


def make_golden_eagle_emblem(size: int = 300) -> Image.Image:
    """
    Render a stylized geometric Golden Eagle emblem inspired by the Syrian Arab Republic's
    visual identity: 14 wing feathers (7 per side), 5 tail feathers, and 3 stars arched above.
    """
    ss = 3
    S = size * ss
    cx, cy = S * 0.50, S * 0.54
    mask = Image.new("L", (S, S), 0)
    md = ImageDraw.Draw(mask)

    # 7 sculpted wing feathers on each side = 14 Syrian governorates
    for side in (-1, 1):
        for i in range(7):
            # i=0 is top outer primary feather, i=6 is inner secondary feather
            frac = i / 6.0
            ang_deg = -18.0 + frac * 54.0  # sweeps from slight upward spread down inward
            ang = math.radians(ang_deg)
            length = S * (0.43 - 0.14 * frac)
            root_x = cx + side * S * (0.055 + 0.012 * frac)
            root_y = cy - S * 0.03 + frac * S * 0.095
            tip_x = root_x + side * length * math.cos(ang)
            tip_y = root_y + length * math.sin(ang) - S * 0.08 * (1.0 - frac)

            perp_x = -math.sin(ang) * side
            perp_y = math.cos(ang)
            w_half = S * (0.020 - 0.003 * frac)

            mid_x = (root_x + tip_x) * 0.5 + perp_x * S * 0.012
            mid_y = (root_y + tip_y) * 0.5 - S * 0.012
            poly = [
                (root_x, root_y - w_half * 0.7),
                (mid_x + perp_x * w_half, mid_y - w_half * 0.8),
                (tip_x, tip_y),
                (mid_x - perp_x * w_half * 0.8, mid_y + w_half * 0.9),
                (root_x, root_y + w_half * 0.8),
            ]
            md.polygon(poly, fill=255)

    # 5 tail feathers = 5 geographic regions of Syria
    for j in range(-2, 3):
        tang = math.radians(90.0 + j * 13.5)
        tlen = S * (0.24 - 0.016 * abs(j))
        rx = cx + j * S * 0.016
        ry = cy + S * 0.10
        tx = rx + tlen * math.cos(tang)
        ty = ry + tlen * math.sin(tang)
        tw_half = S * 0.019
        poly = [
            (rx - tw_half * 0.7, ry),
            (tx - tw_half, ty - S * 0.025),
            (tx, ty + S * 0.015),
            (tx + tw_half, ty - S * 0.025),
            (rx + tw_half * 0.7, ry),
        ]
        md.polygon(poly, fill=255)

    # Central torso shield-like breast & broad arched eagle shoulders
    torso = [
        (cx - S * 0.135, cy - S * 0.105),
        (cx - S * 0.055, cy - S * 0.145),
        (cx + S * 0.055, cy - S * 0.145),
        (cx + S * 0.135, cy - S * 0.105),
        (cx + S * 0.068, cy + S * 0.105),
        (cx, cy + S * 0.165),
        (cx - S * 0.068, cy + S * 0.105),
    ]
    md.polygon(torso, fill=255)

    # Noble sculpted Imperial Eagle neck, crown & curved aquiline beak facing left
    head_poly = [
        (cx - S * 0.048, cy - S * 0.132),
        (cx - S * 0.046, cy - S * 0.195),
        (cx - S * 0.072, cy - S * 0.212),
        (cx - S * 0.102, cy - S * 0.205),  # hooked downward tip of eagle beak
        (cx - S * 0.095, cy - S * 0.228),  # curved upper ridge of beak
        (cx - S * 0.062, cy - S * 0.244),  # brow ridge
        (cx - S * 0.025, cy - S * 0.262),  # regal crown peak
        (cx + S * 0.022, cy - S * 0.254),  # back of crown
        (cx + S * 0.048, cy - S * 0.228),  # nape feather tuft
        (cx + S * 0.036, cy - S * 0.205),
        (cx + S * 0.050, cy - S * 0.132),
    ]
    md.polygon(head_poly, fill=255)

    # Sharp heraldic eye slit & collar V-cutout for unmistakable eagle silhouette
    md.polygon(
        [
            (cx - S * 0.050, cy - S * 0.232),
            (cx - S * 0.030, cy - S * 0.236),
            (cx - S * 0.036, cy - S * 0.224),
        ],
        fill=0,
    )
    md.polygon(
        [
            (cx - S * 0.044, cy - S * 0.122),
            (cx, cy - S * 0.092),
            (cx + S * 0.044, cy - S * 0.122),
            (cx, cy - S * 0.108),
        ],
        fill=0,
    )

    # Three five-pointed stars in a proud upward arc above the eagle's head
    star_specs = [
        (cx - S * 0.145, cy - S * 0.335, S * 0.040),
        (cx, cy - S * 0.378, S * 0.046),
        (cx + S * 0.145, cy - S * 0.335, S * 0.040),
    ]
    for sx, sy, sr in star_specs:
        md.polygon(star_points(sx, sy, sr), fill=255)

    # Build rich metallic gold gradient texture
    yy = np.linspace(0.0, 1.0, S, dtype=np.float32)[:, None]
    xx = np.linspace(-1.0, 1.0, S, dtype=np.float32)[None, :]
    sheen = 0.5 + 0.5 * np.cos(xx * 2.6 - yy * 1.4)
    r = np.clip(252 - 58 * yy + 22 * sheen, 0, 255).astype(np.uint8)
    g = np.clip(224 - 78 * yy + 24 * sheen, 0, 255).astype(np.uint8)
    b = np.clip(142 - 92 * yy + 28 * sheen, 0, 255).astype(np.uint8)
    gold_rgb = np.dstack([r, g, b, np.full((S, S), 255, dtype=np.uint8)])
    gold_im = Image.fromarray(gold_rgb, mode="RGBA")

    out = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    # Soft outer golden glow + dark drop shadow
    glow_mask = mask.filter(ImageFilter.GaussianBlur(S * 0.032))
    glow_im = Image.new("RGBA", (S, S), (255, 206, 98, 0))
    glow_im.putalpha(glow_mask.point(lambda a: int(a * 0.45)))
    out = Image.alpha_composite(out, glow_im)

    shadow_mask = mask.filter(ImageFilter.GaussianBlur(S * 0.014))
    sh_im = Image.new("RGBA", (S, S), (2, 12, 9, 0))
    sh_im.putalpha(shadow_mask.point(lambda a: int(a * 0.72)))
    sh_shift = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    sh_shift.paste(sh_im, (0, int(S * 0.012)))
    out = Image.alpha_composite(out, sh_shift)

    eagle_layer = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    eagle_layer.paste(gold_im, (0, 0), mask)
    out = Image.alpha_composite(out, eagle_layer)

    return out.resize((size, size), Image.Resampling.LANCZOS)


# ---------------------------------------------------------------------------
# 3. Voiceover Tightening & Multi-Layer Procedural SFX + Levantine Score
# ---------------------------------------------------------------------------
def load_and_tighten_vo(path: Path, tempo: float = 1.15) -> np.ndarray:
    ffmpeg = imageio_ffmpeg.get_ffmpeg_exe()
    p = subprocess.run(
        [
            ffmpeg,
            "-i",
            str(path),
            "-af",
            f"atempo={tempo},highpass=f=80,acompressor=threshold=-18dB:ratio=2.6:attack=6:release=75",
            "-f",
            "f32le",
            "-ac",
            "1",
            "-ar",
            str(AUDIO_SR),
            "-",
        ],
        capture_output=True,
        check=True,
    )
    x = np.frombuffer(p.stdout, dtype=np.float32).copy()
    hop = AUDIO_SR // 100  # 10ms
    n = len(x) // hop
    rms = np.sqrt(np.mean(x[: n * hop].reshape(n, hop) ** 2, axis=1))
    active = np.where(rms > 0.007)[0]
    s0 = max(0, int(active[0]) - 3) * hop
    s1 = min(len(x), (int(active[-1]) + 5) * hop)
    x = x[s0:s1]

    n = len(x) // hop
    rms = np.sqrt(np.mean(x[: n * hop].reshape(n, hop) ** 2, axis=1))
    silent = rms < 0.005
    chunks = []
    i = 0
    while i < n:
        if not silent[i]:
            j = i
            while j < n and not silent[j]:
                j += 1
            chunks.append(x[i * hop : j * hop])
            i = j
        else:
            j = i
            while j < n and silent[j]:
                j += 1
            keep_frames = min(j - i, 16)
            chunks.append(x[i * hop : (i + keep_frames) * hop])
            i = j
    out = np.concatenate(chunks)
    fade = int(0.018 * AUDIO_SR)
    out[:fade] *= np.linspace(0.0, 1.0, fade, dtype=np.float32)
    out[-fade:] *= np.linspace(1.0, 0.0, fade, dtype=np.float32)
    peak = float(np.max(np.abs(out)))
    if peak > 1e-5:
        out = out * (0.86 / peak)
    return out


def add_stereo(mix: np.ndarray, start_s: float, sig_l: np.ndarray, sig_r: np.ndarray | None = None, gain: float = 1.0) -> None:
    if sig_r is None:
        sig_r = sig_l
    i0 = max(0, int(round(start_s * AUDIO_SR)))
    if i0 >= len(mix):
        return
    n = min(len(sig_l), len(mix) - i0)
    if n <= 0:
        return
    mix[i0 : i0 + n, 0] += sig_l[:n] * gain
    mix[i0 : i0 + n, 1] += sig_r[:n] * gain


def synth_oud_pluck(freq: float, dur_s: float, sr: int = AUDIO_SR, brightness: float = 0.58) -> np.ndarray:
    """Karplus-Strong plucked acoustic string with warm woody Oud body resonance."""
    n = int(dur_s * sr)
    delay = max(4, int(round(sr / freq)))
    rng = np.random.default_rng(int(freq * 100) % 99991)
    burst = rng.uniform(-1.0, 1.0, delay).astype(np.float64)
    # Soft pick lowpass on excitation
    burst = np.convolve(burst, np.ones(5) / 5.0, mode="same")
    y = np.zeros(n, dtype=np.float64)
    buf = burst.copy()
    decay = 0.9945
    idx = 0
    for i in range(n):
        y[i] = buf[idx]
        nxt = (idx + 1) % delay
        buf[idx] = decay * ((1.0 - brightness) * buf[idx] + brightness * buf[nxt])
        idx = nxt
    # Warm Oud body resonances around 195 Hz and 340 Hz
    sos_body = signal.butter(2, [130, 3200], btype="bandpass", fs=sr, output="sos")
    y = signal.sosfilt(sos_body, y)
    env = np.exp(-np.linspace(0, 4.2, n))
    y *= env
    peak = np.max(np.abs(y))
    return (y / max(peak, 1e-6)).astype(np.float32)


def synth_cinematic_boom(dur_s: float = 1.65, f_start: float = 96.0, f_end: float = 34.0) -> tuple[np.ndarray, np.ndarray]:
    n = int(dur_s * AUDIO_SR)
    t = np.arange(n, dtype=np.float64) / AUDIO_SR
    inst_f = f_end + (f_start - f_end) * np.exp(-t * 6.5)
    phase = 2.0 * np.pi * np.cumsum(inst_f) / AUDIO_SR
    sub = np.sin(phase) * np.exp(-t * 2.6)
    harm = 0.38 * np.sin(2.0 * phase) * np.exp(-t * 4.5)

    # Frame drum (Daf / Mazhar) transient punch + airy stereo reverb tail
    rng = np.random.default_rng(2026)
    noise_l = rng.normal(0, 1, n)
    noise_r = rng.normal(0, 1, n)
    sos_punch = signal.butter(2, [85, 650], btype="bandpass", fs=AUDIO_SR, output="sos")
    punch_l = signal.sosfilt(sos_punch, noise_l) * np.exp(-t * 16.0) * 0.55
    punch_r = signal.sosfilt(sos_punch, noise_r) * np.exp(-t * 15.5) * 0.55

    sos_tail = signal.butter(2, [180, 1800], btype="bandpass", fs=AUDIO_SR, output="sos")
    tail_l = signal.sosfilt(sos_tail, noise_l) * np.exp(-t * 3.1) * 0.16
    tail_r = signal.sosfilt(sos_tail, noise_r) * np.exp(-t * 3.0) * 0.16

    attack = np.minimum(1.0, t / 0.006)
    l = np.tanh((sub + harm + punch_l + tail_l) * attack * 1.4).astype(np.float32)
    r = np.tanh((sub + harm + punch_r + tail_r) * attack * 1.4).astype(np.float32)
    return l, r


def synth_whoosh(dur_s: float = 0.58, direction: int = 1, seed: int = 10) -> tuple[np.ndarray, np.ndarray]:
    """Stereo-swept aerodynamic transition whoosh."""
    n = int(dur_s * AUDIO_SR)
    t = np.linspace(0.0, 1.0, n, dtype=np.float64)
    rng = np.random.default_rng(seed)
    raw = rng.normal(0, 1, n)
    # Time-chunked bandpass sweep from 240Hz -> 2400Hz -> 360Hz
    chunks = 24
    step = n // chunks
    out = np.zeros(n, dtype=np.float64)
    for k in range(chunks):
        c0 = k * step
        c1 = n if k == chunks - 1 else (k + 1) * step
        u = (k + 0.5) / chunks
        fc = 260.0 + 2100.0 * math.sin(math.pi * u) ** 1.4
        lo = max(90.0, fc * 0.55)
        hi = min(8000.0, fc * 1.75)
        sos = signal.butter(2, [lo, hi], btype="bandpass", fs=AUDIO_SR, output="sos")
        out[c0:c1] = signal.sosfilt(sos, raw[c0:c1])
    env = np.sin(np.pi * t) ** 1.8
    # Add subtle tonal air whistle
    whistle_f = 180.0 + 420.0 * np.sin(np.pi * t)
    tonal = 0.18 * np.sin(2.0 * np.pi * np.cumsum(whistle_f) / AUDIO_SR) * env
    sig = (out * env * 0.85 + tonal)
    peak = np.max(np.abs(sig))
    sig = sig / max(peak, 1e-6)
    pan = t if direction > 0 else (1.0 - t)
    l = (sig * np.cos(pan * np.pi * 0.5)).astype(np.float32)
    r = (sig * np.sin(pan * np.pi * 0.5)).astype(np.float32)
    return l, r


def synth_riser(dur_s: float = 1.15) -> tuple[np.ndarray, np.ndarray]:
    """Dramatic cinematic riser building tension right before the Republic reveal."""
    n = int(dur_s * AUDIO_SR)
    t = np.linspace(0.0, 1.0, n, dtype=np.float64)
    env = (t ** 2.3) * np.minimum(1.0, (1.0 - t) / 0.03)
    f1 = 146.83 * (2.0 ** (t * 1.5))  # D3 sweeping upward
    f2 = 220.00 * (2.0 ** (t * 1.5))  # A3 fifth sweeping upward
    p1 = 2.0 * np.pi * np.cumsum(f1) / AUDIO_SR
    p2 = 2.0 * np.pi * np.cumsum(f2) / AUDIO_SR
    rng = np.random.default_rng(77)
    shimmer = signal.sosfilt(
        signal.butter(2, [900, 5200], btype="bandpass", fs=AUDIO_SR, output="sos"),
        rng.normal(0, 1, n),
    )
    l = ((0.45 * np.sin(p1) + 0.30 * np.sin(p2) + 0.35 * shimmer) * env).astype(np.float32)
    r = ((0.38 * np.sin(p1 + 0.4) + 0.38 * np.sin(p2 + 0.7) + 0.35 * np.roll(shimmer, 37)) * env).astype(np.float32)
    return l, r


def synth_chime(freqs: list[float], dur_s: float = 1.1) -> tuple[np.ndarray, np.ndarray]:
    """Delicate golden qanun/bell shimmer for text & emblem reveals."""
    n = int(dur_s * AUDIO_SR)
    t = np.arange(n, dtype=np.float64) / AUDIO_SR
    l = np.zeros(n, dtype=np.float64)
    r = np.zeros(n, dtype=np.float64)
    for idx, f in enumerate(freqs):
        d = int(idx * 0.045 * AUDIO_SR)
        m = n - d
        if m <= 0:
            continue
        tt = t[:m]
        tone = (np.sin(2 * np.pi * f * tt) + 0.32 * np.sin(2 * np.pi * f * 2.0 * tt)) * np.exp(-tt * 5.2)
        tone *= np.minimum(1.0, tt / 0.004)
        pan = 0.3 + 0.4 * (idx / max(1, len(freqs) - 1))
        l[d:] += tone * (1.0 - pan * 0.5)
        r[d:] += tone * (0.5 + pan * 0.5)
    peak = max(np.max(np.abs(l)), np.max(np.abs(r)), 1e-6)
    return (l / peak).astype(np.float32), (r / peak).astype(np.float32)


def synth_damascus_morning(dur_s: float = 5.4) -> tuple[np.ndarray, np.ndarray]:
    """Soft Damascus courtyard fountain water trickle + gentle morning birdsong."""
    n = int(dur_s * AUDIO_SR)
    t = np.arange(n, dtype=np.float64) / AUDIO_SR
    rng = np.random.default_rng(301)
    # Soft courtyard fountain trickle
    water = signal.sosfilt(
        signal.butter(2, [900, 4200], btype="bandpass", fs=AUDIO_SR, output="sos"),
        rng.normal(0, 1, n),
    )
    am = 0.55 + 0.45 * np.sin(2 * np.pi * 6.5 * t) * np.sin(2 * np.pi * 2.3 * t)
    l = water * am * 0.16
    r = np.roll(water, 65) * am * 0.16

    # Three delicate FM morning bird chirps
    for bt, bf, pan in [(0.32, 2650.0, 0.25), (0.52, 3100.0, 0.30), (2.85, 2850.0, 0.75)]:
        b0 = int(bt * AUDIO_SR)
        bn = int(0.16 * AUDIO_SR)
        if b0 + bn < n:
            tt = np.linspace(0, 1, bn)
            f_mod = bf * (1.0 + 0.18 * np.sin(2 * np.pi * 16.0 * tt) - 0.12 * tt)
            chirp = np.sin(2 * np.pi * np.cumsum(f_mod) / AUDIO_SR) * (np.sin(np.pi * tt) ** 2) * 0.22
            l[b0 : b0 + bn] += chirp * (1.0 - pan * 0.6)
            r[b0 : b0 + bn] += chirp * (0.4 + pan * 0.6)
    env = np.minimum(1.0, t / 0.25) * np.minimum(1.0, (dur_s - t) / 0.5)
    return (l * env).astype(np.float32), (r * env).astype(np.float32)


def synth_waterwheel_and_sea(dur_s: float = 4.2) -> tuple[np.ndarray, np.ndarray]:
    """Wooden Hama Noria water splash transitioning into gentle Mediterranean surf."""
    n = int(dur_s * AUDIO_SR)
    t = np.arange(n, dtype=np.float64) / AUDIO_SR
    rng = np.random.default_rng(404)
    splash = signal.sosfilt(
        signal.butter(2, [350, 3400], btype="bandpass", fs=AUDIO_SR, output="sos"),
        rng.normal(0, 1, n),
    )
    surf = signal.sosfilt(
        signal.butter(2, [140, 1400], btype="bandpass", fs=AUDIO_SR, output="sos"),
        rng.normal(0, 1, n),
    )
    # First half: rhythmic waterwheel paddle splashes; second half: smooth sea wash
    paddle_env = (0.45 + 0.55 * np.abs(np.sin(2 * np.pi * 1.6 * t))) * np.clip(1.0 - t / 2.4, 0.0, 1.0)
    sea_env = (0.5 + 0.5 * np.sin(2 * np.pi * 0.35 * t)) * np.clip((t - 1.5) / 1.2, 0.0, 1.0)
    sig_l = splash * paddle_env * 0.30 + surf * sea_env * 0.28
    sig_r = np.roll(splash, 48) * paddle_env * 0.30 + np.roll(surf, 90) * sea_env * 0.28
    fade = np.minimum(1.0, t / 0.2) * np.minimum(1.0, (dur_s - t) / 0.4)
    return (sig_l * fade).astype(np.float32), (sig_r * fade).astype(np.float32)


def synth_flag_flutter(dur_s: float = 5.0) -> tuple[np.ndarray, np.ndarray]:
    """Proud banner fabric snapping and fluttering in mountain wind."""
    n = int(dur_s * AUDIO_SR)
    t = np.arange(n, dtype=np.float64) / AUDIO_SR
    rng = np.random.default_rng(505)
    wind = signal.sosfilt(
        signal.butter(2, [110, 780], btype="bandpass", fs=AUDIO_SR, output="sos"),
        rng.normal(0, 1, n),
    )
    cloth = signal.sosfilt(
        signal.butter(2, [220, 1850], btype="bandpass", fs=AUDIO_SR, output="sos"),
        rng.normal(0, 1, n),
    )
    snap_mod = (0.35 + 0.65 * (np.sin(2 * np.pi * 3.8 * t + 0.6 * np.sin(2 * np.pi * 0.9 * t)) ** 4))
    sig_l = wind * 0.20 + cloth * snap_mod * 0.32
    sig_r = np.roll(wind, 80) * 0.20 + np.roll(cloth, 35) * snap_mod * 0.32
    fade = np.minimum(1.0, t / 0.3) * np.minimum(1.0, (dur_s - t) / 0.5)
    return (sig_l * fade).astype(np.float32), (sig_r * fade).astype(np.float32)


def synth_daf_pulse(accent: bool = False) -> np.ndarray:
    """Warm oriental Daf 'Dum' or 'Tak' percussion hit."""
    dur = 0.28 if accent else 0.16
    n = int(dur * AUDIO_SR)
    t = np.arange(n, dtype=np.float64) / AUDIO_SR
    if accent:
        f = 78.0 + 48.0 * np.exp(-t * 28.0)
        body = np.sin(2 * np.pi * np.cumsum(f) / AUDIO_SR) * np.exp(-t * 11.0)
        skin = np.random.default_rng(12).normal(0, 1, n) * np.exp(-t * 45.0) * 0.22
        return ((body + skin) * 0.85).astype(np.float32)
    else:
        rng = np.random.default_rng(19)
        tak = signal.sosfilt(
            signal.butter(2, [950, 4800], btype="bandpass", fs=AUDIO_SR, output="sos"),
            rng.normal(0, 1, n),
        )
        return (tak * np.exp(-t * 36.0) * 0.45).astype(np.float32)


def build_master_soundtrack(vo_clips: list[np.ndarray]) -> tuple[Path, float, list[tuple[float, float]], np.ndarray]:
    """
    Assemble voiceover, synchronized multi-layer SFX, environmental foley,
    and sidechain-ducked Levantine Oud/Qanun + Daf score into a mastered 48kHz WAV.
    Returns (wav_path, total_duration, clip_windows, per_frame_audio_energy).
    """
    t_cursor = 0.52
    gaps = [0.30, 0.34, 0.30, 0.32, 1.38]
    clip_windows: list[tuple[float, float]] = []
    for idx, clip in enumerate(vo_clips):
        dur = len(clip) / AUDIO_SR
        clip_windows.append((t_cursor, t_cursor + dur))
        t_cursor += dur + gaps[idx]

    total_dur = round(t_cursor, 2)  # ~28.5s (strictly < 30s)
    total_samples = int(round(total_dur * AUDIO_SR))

    vo_track = np.zeros((total_samples, 2), dtype=np.float32)
    sfx_track = np.zeros((total_samples, 2), dtype=np.float32)
    music_track = np.zeros((total_samples, 2), dtype=np.float32)

    for clip, (s0, _s1) in zip(vo_clips, clip_windows):
        add_stereo(vo_track, s0, clip, clip, gain=1.0)

    # Voice envelope for smooth sidechain ducking of music & ambience
    win = int(0.06 * AUDIO_SR)
    vo_abs = np.abs(vo_track[:, 0])
    vo_env = np.convolve(vo_abs, np.ones(win) / win, mode="same")
    duck = 1.0 - 0.56 * np.clip(vo_env / 0.11, 0.0, 1.0)

    # --- Key Timeline Landmarks ---
    c1_s, c1_e = clip_windows[0]
    c2_s, c2_e = clip_windows[1]
    c3_s, c3_e = clip_windows[2]
    c4_s, c4_e = clip_windows[3]
    c5_s, c5_e = clip_windows[4]

    cut_aleppo = c2_s - 0.14
    cut_hama = c2_s + (c2_e - c2_s) * 0.34
    cut_coast = c2_s + (c2_e - c2_s) * 0.66
    cut_republic = c3_s - 0.14
    cut_flag = c4_s - 0.14
    cut_sunrise = c5_s - 0.14
    cut_finale_card = c5_e - 1.55

    # 1) Cinematic Booms & Impacts
    boom_open_l, boom_open_r = synth_cinematic_boom(1.6, 92.0, 35.0)
    add_stereo(sfx_track, 0.03, boom_open_l, boom_open_r, gain=0.46)

    riser_l, riser_r = synth_riser(1.12)
    add_stereo(sfx_track, cut_republic - 1.08, riser_l, riser_r, gain=0.34)

    boom_hero_l, boom_hero_r = synth_cinematic_boom(1.85, 108.0, 32.0)
    add_stereo(sfx_track, cut_republic, boom_hero_l, boom_hero_r, gain=0.52)

    boom_end_l, boom_end_r = synth_cinematic_boom(2.0, 98.0, 33.0)
    add_stereo(sfx_track, c5_e - 0.08, boom_end_l, boom_end_r, gain=0.50)

    # 2) Stereo Aerodynamic Whooshes on Every Cut
    cuts = [cut_aleppo, cut_hama, cut_coast, cut_republic, cut_flag, cut_sunrise]
    for idx, ct in enumerate(cuts):
        wl, wr = synth_whoosh(0.54, direction=1 if idx % 2 == 0 else -1, seed=20 + idx)
        add_stereo(sfx_track, ct - 0.22, wl, wr, gain=0.28)

    # 3) Golden Qanun / Chime Accents on Key Title & Emblem Reveals
    # D-minor / Nahawand sparkling intervals: D5, F5, A5, D6
    ch1_l, ch1_r = synth_chime([587.33, 698.46, 880.0], 0.95)
    add_stereo(sfx_track, 0.12, ch1_l, ch1_r, gain=0.20)

    # Majestic Golden Eagle reveal arpeggio
    ch2_l, ch2_r = synth_chime([587.33, 880.0, 1174.66, 1396.91], 1.25)
    add_stereo(sfx_track, cut_republic + 0.04, ch2_l, ch2_r, gain=0.24)

    # Mid-Clip 3 accent right on "اسمها الجمهورية العربية السورية!"
    ch3_l, ch3_r = synth_chime([698.46, 880.0, 1174.66], 1.0)
    add_stereo(sfx_track, c3_s + (c3_e - c3_s) * 0.56, ch3_l, ch3_r, gain=0.19)

    # Finale Picardy D-major uplifting resolve arpeggio (D5, F#5, A5, D6, F#6)
    ch_fin_l, ch_fin_r = synth_chime([587.33, 739.99, 880.0, 1174.66, 1479.98], 1.55)
    add_stereo(sfx_track, c5_e - 0.05, ch_fin_l, ch_fin_r, gain=0.30)

    # 4) Environmental Foley Textures (Old Damascus courtyard -> Hama/Sea -> Waving Flag)
    dam_l, dam_r = synth_damascus_morning(min(5.2, cut_aleppo))
    add_stereo(sfx_track, 0.05, dam_l, dam_r, gain=0.38)

    wat_l, wat_r = synth_waterwheel_and_sea(max(2.0, cut_republic - cut_hama))
    add_stereo(sfx_track, cut_hama, wat_l, wat_r, gain=0.28)

    flg_l, flg_r = synth_flag_flutter(max(2.0, cut_sunrise - cut_flag))
    add_stereo(sfx_track, cut_flag, flg_l, flg_r, gain=0.34)

    # 5) Warm Levantine Oud + Pad + Gentle Daf Heartbeat Bed
    t_all = np.arange(total_samples, dtype=np.float64) / AUDIO_SR
    # Warm stereo string pad progressing Dm -> Bb -> Gm -> A -> D
    pad_l = (
        0.18 * np.sin(2 * np.pi * 146.83 * t_all)
        + 0.11 * np.sin(2 * np.pi * 220.00 * t_all + 0.4)
        + 0.06 * np.sin(2 * np.pi * 293.66 * t_all + 1.1)
    )
    pad_r = (
        0.18 * np.sin(2 * np.pi * 146.83 * t_all + 0.2)
        + 0.11 * np.sin(2 * np.pi * 220.00 * t_all + 0.9)
        + 0.06 * np.sin(2 * np.pi * 349.23 * t_all + 1.7)
    )
    pad_env = np.minimum(1.0, t_all / 0.8) * np.minimum(1.0, (total_dur - t_all) / 0.85)
    music_track[:, 0] += (pad_l * pad_env * 0.22).astype(np.float32)
    music_track[:, 1] += (pad_r * pad_env * 0.22).astype(np.float32)

    # Melodic Levantine Oud arpeggio phrases (Maqam Nahawand on D: D3, F3, G3, A3, Bb3, C#4, D4, F4)
    oud_notes = [
        (0.08, 146.83, 1.4), (0.42, 220.00, 1.2), (0.78, 293.66, 1.5),
        (2.60, 349.23, 1.1), (3.10, 329.63, 1.1), (3.65, 293.66, 1.4),
        (5.50, 233.08, 1.4), (6.05, 293.66, 1.2), (6.60, 349.23, 1.4),
        (8.40, 196.00, 1.4), (9.00, 293.66, 1.2), (9.65, 392.00, 1.4),
        (11.88, 146.83, 1.8), (12.25, 220.00, 1.5), (12.62, 293.66, 1.6), (13.10, 440.00, 1.6),
        (15.20, 277.18, 1.2), (15.70, 293.66, 1.6),
        (17.38, 233.08, 1.5), (17.90, 349.23, 1.3), (18.45, 440.00, 1.5),
        (20.30, 220.00, 1.3), (20.85, 277.18, 1.3), (21.40, 293.66, 1.5),
        (22.60, 146.83, 1.6), (23.15, 220.00, 1.4), (23.70, 349.23, 1.4),
        # Finale warm major resolve chord
        (c5_e - 0.04, 146.83, 1.45), (c5_e + 0.04, 220.00, 1.40),
        (c5_e + 0.12, 293.66, 1.35), (c5_e + 0.20, 369.99, 1.30),
    ]
    for idx, (nt, nf, nd) in enumerate(oud_notes):
        if nt < total_dur - 0.2:
            plk = synth_oud_pluck(nf, min(nd, total_dur - nt))
            pan = 0.35 if idx % 2 == 0 else 0.65
            add_stereo(music_track, nt, plk * (1.0 - pan * 0.4), plk * (0.6 + pan * 0.4), gain=0.24)

    # Gentle patriotic Daf rhythm starting at Scene 3 ("الجمهورية العربية السورية")
    dum = synth_daf_pulse(accent=True)
    tak = synth_daf_pulse(accent=False)
    beat_t = cut_republic
    beat_idx = 0
    while beat_t < c5_e - 0.15:
        if beat_idx % 4 in (0, 2):
            add_stereo(music_track, beat_t, dum, dum, gain=0.20 if beat_idx % 4 == 0 else 0.13)
        else:
            add_stereo(music_track, beat_t, tak * 0.85, tak * 1.0, gain=0.14)
        beat_t += 0.46
        beat_idx += 1

    # Apply smooth sidechain ducking to music and subtle ducking to SFX under speech
    music_track *= duck[:, None]
    sfx_duck = 1.0 - 0.22 * np.clip(vo_env / 0.12, 0.0, 1.0)
    sfx_track *= sfx_duck[:, None]

    master = vo_track * 1.12 + sfx_track * 0.92 + music_track * 0.88
    # Clean master fade-out over the last 0.38s
    fade_out_n = int(0.38 * AUDIO_SR)
    master[-fade_out_n:] *= np.linspace(1.0, 0.0, fade_out_n, dtype=np.float32)[:, None]

    # Soft transparent master limiter
    peak = float(np.max(np.abs(master)))
    if peak > 0.96:
        master = np.tanh(master * (1.05 / peak)) * 0.94

    # Compute per-frame energy at FPS for the live reactive audio visualizer in the header
    total_frames = int(round(total_dur * FPS))
    frame_energy = np.zeros(total_frames, dtype=np.float32)
    spf = AUDIO_SR / FPS
    mono_abs = np.mean(np.abs(master), axis=1)
    for f_idx in range(total_frames):
        a0 = int(f_idx * spf)
        a1 = min(total_samples, int((f_idx + 1) * spf))
        if a1 > a0:
            frame_energy[f_idx] = float(np.sqrt(np.mean(mono_abs[a0:a1] ** 2)))
    if np.max(frame_energy) > 1e-6:
        frame_energy = np.clip(frame_energy / (np.percentile(frame_energy, 95) + 1e-6), 0.08, 1.0)

    wav_path = ASSET_DIR / "master_mix.wav"
    pcm = np.clip(master * 32767.0, -32767, 32767).astype("<i2")
    with wave.open(str(wav_path), "wb") as wf:
        wf.setnchannels(2)
        wf.setsampwidth(2)
        wf.setframerate(AUDIO_SR)
        wf.writeframes(pcm.tobytes())

    return wav_path, total_dur, clip_windows, frame_energy


# ---------------------------------------------------------------------------
# 4. Visual Compositing, Motion Graphics & Overlays
# ---------------------------------------------------------------------------
def ease_out_cubic(x: float) -> float:
    x = max(0.0, min(1.0, x))
    return 1.0 - (1.0 - x) ** 3


def smoothstep(x: float) -> float:
    x = max(0.0, min(1.0, x))
    return x * x * (3.0 - 2.0 * x)


def window_alpha(t: float, start: float, end: float, fi: float = 0.28, fo: float = 0.28) -> float:
    if t < start or t > end:
        return 0.0
    a = smoothstep((t - start) / max(fi, 0.001))
    b = smoothstep((end - t) / max(fo, 0.001))
    return min(a, b)


def load_scene_images() -> dict[str, Image.Image]:
    specs = {
        "damascus": ASSET_DIR / "scene1_damascus.jpg",
        "aleppo": ASSET_DIR / "scene2_aleppo.jpg",
        "hama": ASSET_DIR / "scene3_hama.jpg",
        "coast": ASSET_DIR / "scene3b_coast.jpg",
        "flag": ASSET_DIR / "scene4_flag.jpg",
        "sunrise": ASSET_DIR / "scene5_sunrise.jpg",
    }
    loaded: dict[str, Image.Image] = {}
    for k, p in specs.items():
        im = Image.open(p).convert("RGB")
        im = ImageOps.fit(im, (1240, 2204), method=Image.Resampling.LANCZOS, centering=(0.5, 0.5))
        im = ImageEnhance.Contrast(im).enhance(1.05)
        im = ImageEnhance.Color(im).enhance(1.04)
        loaded[k] = im
    return loaded


def sample_camera(im: Image.Image, zoom: tuple[float, float], focus: tuple[tuple[float, float], tuple[float, float]], u: float) -> Image.Image:
    iw, ih = im.size
    e = smoothstep(u)
    z = zoom[0] + (zoom[1] - zoom[0]) * e
    fx = focus[0][0] + (focus[1][0] - focus[0][0]) * e
    fy = focus[0][1] + (focus[1][1] - focus[0][1]) * e
    cw = min(iw, W / z)
    ch = min(ih, H / z)
    x0 = max(0.0, min(iw - cw, fx * iw - cw * 0.5))
    y0 = max(0.0, min(ih - ch, fy * ih - ch * 0.5))
    return im.crop((int(x0), int(y0), int(x0 + cw), int(y0 + ch))).resize((W, H), Image.Resampling.BILINEAR)


def build_scrim() -> Image.Image:
    """Precomputed top & bottom cinematic gradient scrim for crystal-clear text legibility."""
    arr = np.zeros((H, W, 4), dtype=np.uint8)
    y = np.arange(H, dtype=np.float32)
    top_a = np.clip(((540.0 - y) / 540.0), 0.0, 1.0) ** 1.45 * 145.0
    bot_a = np.clip(((y - 1180.0) / (H - 1180.0)), 0.0, 1.0) ** 1.30 * 195.0
    alpha = np.clip(np.maximum(top_a, bot_a), 0, 255).astype(np.uint8)
    arr[:, :, 0] = 4
    arr[:, :, 1] = 19
    arr[:, :, 2] = 15
    arr[:, :, 3] = alpha[:, None]
    return Image.fromarray(arr, mode="RGBA")


def make_header_base(flag_icon: Image.Image, eagle_small: Image.Image) -> Image.Image:
    layer = Image.new("RGBA", (W, 175), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    # Glassmorphic pill container
    d.rounded_rectangle((46, 52, W - 46, 148), radius=48, fill=(5, 26, 20, 206), outline=COL_GOLD_RICH + (165,), width=2)
    # Right side (RTL): Flag + Title
    layer.alpha_composite(flag_icon, (W - 134, 79))
    layer.alpha_composite(eagle_small, (W - 196, 71))

    t_main = render_arabic_text("الجمهورية العربية السورية", FONT_EXTRABOLD, 30, COL_GOLD_BRIGHT + (255,), shadow=False)
    t_sub = render_arabic_text("هوية وطن • عربي شامي أصيل", FONT_MEDIUM, 20, (205, 228, 220, 235), shadow=False)
    layer.alpha_composite(t_main, (W - 204 - t_main.width, 60))
    layer.alpha_composite(t_sub, (W - 204 - t_sub.width, 98))
    return layer


def draw_audio_eq(frame: Image.Image, energy: float, t: float) -> None:
    """Draw live 9-bar golden audio equalizer inside the left side of the top header pill."""
    d = ImageDraw.Draw(frame, "RGBA")
    bx0 = 84
    cy = 100
    for b in range(9):
        wave_mod = 0.45 + 0.55 * abs(math.sin(t * (7.5 + b * 1.1) + b * 0.85))
        h_bar = int(6 + 26 * energy * wave_mod)
        x = bx0 + b * 11
        col = COL_GOLD_BRIGHT + (235,) if b % 2 == 0 else (72, 208, 138, 225)
        d.rounded_rectangle((x, cy - h_bar // 2, x + 6, cy + h_bar // 2), radius=3, fill=col)


def make_opening_hook() -> Image.Image:
    layer = Image.new("RGBA", (W, 420), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    tag = render_arabic_text("من ياسمين الشام لكل شبر بالوطن", FONT_BOLD, 30, COL_GOLD_BRIGHT + (255,), stroke_px=2)
    h1 = render_arabic_text("حكاية وطن", FONT_EXTRABOLD, 76, COL_TEXT_WHITE + (255,), stroke_px=3)
    h2 = render_arabic_text("ما بيشبه غير حالو", FONT_EXTRABOLD, 68, COL_GOLD_BRIGHT + (255,), stroke_px=3)

    d.rounded_rectangle(((W - (tag.width + 44)) // 2, 18, (W + (tag.width + 44)) // 2, 74), radius=28,
                        fill=(6, 32, 24, 195), outline=COL_GOLD_RICH + (170,), width=2)
    layer.alpha_composite(tag, ((W - tag.width) // 2, 18))
    layer.alpha_composite(h1, ((W - h1.width) // 2, 92))
    layer.alpha_composite(h2, ((W - h2.width) // 2, 188))
    d.rounded_rectangle((W // 2 - 70, 294, W // 2 + 70, 300), radius=3, fill=COL_GOLD_RICH + (235,))
    return layer


def make_republic_centerpiece(eagle_lg: Image.Image) -> Image.Image:
    """Heroic centerpiece medallion revealed when proclaiming 'الجمهورية العربية السورية'."""
    layer = Image.new("RGBA", (W, 470), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    cx, cy = W // 2, 185
    d.ellipse((cx - 148, cy - 148, cx + 148, cy + 148), fill=(5, 28, 21, 188), outline=COL_GOLD_RICH + (210,), width=3)
    d.ellipse((cx - 134, cy - 134, cx + 134, cy + 134), outline=COL_GOLD_BRIGHT + (95,), width=1)
    layer.alpha_composite(eagle_lg, (cx - eagle_lg.width // 2, cy - eagle_lg.height // 2))

    banner = render_arabic_text("الجمهورية العربية السورية", FONT_EXTRABOLD, 56, COL_GOLD_BRIGHT + (255,), stroke_px=3)
    bw = banner.width + 58
    d.rounded_rectangle(((W - bw) // 2, 344, (W + bw) // 2, 430), radius=24,
                        fill=(5, 28, 21, 224), outline=COL_GOLD_RICH + (215,), width=2)
    layer.alpha_composite(banner, ((W - banner.width) // 2, 348))
    return layer


def make_caption_card(tag_text: str, line1: str, line2: str, highlight_gold_line2: bool = True) -> Image.Image:
    cw, ch = 972, 272
    layer = Image.new("RGBA", (cw, ch), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    d.rounded_rectangle((2, 2, cw - 3, ch - 3), radius=30, fill=(5, 24, 19, 218), outline=COL_GOLD_RICH + (165,), width=2)

    # Right-side vertical official tricolor + gold accent bar
    d.rounded_rectangle((cw - 24, 34, cw - 16, 98), radius=3, fill=COL_FLAG_GREEN + (245,))
    d.rounded_rectangle((cw - 24, 102, cw - 16, 166), radius=3, fill=COL_FLAG_WHITE + (245,))
    d.rounded_rectangle((cw - 24, 170, cw - 16, 234), radius=3, fill=COL_GOLD_RICH + (245,))

    # Tag pill at top-right
    t_tag = render_arabic_text(tag_text, FONT_BOLD, 26, COL_GOLD_BRIGHT + (255,), shadow=False)
    pw = t_tag.width + 34
    d.rounded_rectangle((cw - 44 - pw, 22, cw - 44, 68), radius=22, fill=(14, 58, 43, 225), outline=COL_GOLD_RICH + (120,), width=1)
    layer.alpha_composite(t_tag, (cw - 44 - pw + 17, 20))

    # Auto-fit lines cleanly inside safe card width
    max_w = cw - 92
    sz1 = 46
    im1 = render_arabic_text(line1, FONT_EXTRABOLD, sz1, COL_TEXT_WHITE + (255,), stroke_px=2)
    if im1.width > max_w:
        sz1 = max(34, int(sz1 * max_w / im1.width))
        im1 = render_arabic_text(line1, FONT_EXTRABOLD, sz1, COL_TEXT_WHITE + (255,), stroke_px=2)

    col2 = (COL_GOLD_BRIGHT if highlight_gold_line2 else COL_TEXT_WHITE) + (255,)
    sz2 = 48
    im2 = render_arabic_text(line2, FONT_EXTRABOLD, sz2, col2, stroke_px=2)
    if im2.width > max_w:
        sz2 = max(35, int(sz2 * max_w / im2.width))
        im2 = render_arabic_text(line2, FONT_EXTRABOLD, sz2, col2, stroke_px=2)

    layer.alpha_composite(im1, (cw - 46 - im1.width, 82))
    layer.alpha_composite(im2, (cw - 46 - im2.width, 158))
    return layer


def make_governorates_strip() -> Image.Image:
    text = (
        "دمشق  •  ريف دمشق  •  حلب  •  حمص  •  حماة  •  اللاذقية  •  طرطوس  •  "
        "إدلب  •  درعا  •  السويداء  •  القنيطرة  •  دير الزور  •  الرقة  •  الحسكة  •  "
    )
    return render_arabic_text(text, FONT_BOLD, 27, (236, 244, 240, 238), shadow=False)


def make_finale_poster(eagle_xl: Image.Image, flag_lg: Image.Image) -> Image.Image:
    """Grand closing lockup celebrating the united Syrian Arab Republic."""
    layer = Image.new("RGBA", (W, H), (4, 22, 17, 178))
    d = ImageDraw.Draw(layer)

    # Central majestic card
    cx = W // 2
    d.rounded_rectangle((68, 280, W - 68, 1540), radius=44, fill=(5, 26, 20, 222), outline=COL_GOLD_RICH + (210,), width=3)
    d.rounded_rectangle((86, 298, W - 86, 1522), radius=36, outline=COL_GOLD_BRIGHT + (75,), width=1)

    layer.alpha_composite(eagle_xl, (cx - eagle_xl.width // 2, 330))

    t_syria = render_arabic_text("سوريا", FONT_EXTRABOLD, 112, COL_TEXT_WHITE + (255,), stroke_px=3)
    t_rep = render_arabic_text("الجمهورية العربية السورية", FONT_EXTRABOLD, 66, COL_GOLD_BRIGHT + (255,), stroke_px=3)
    t_motto = render_arabic_text("أرض وحدة  •  شعب واحد  •  ١٤ محافظة بقلب واحد", FONT_BOLD, 34, (220, 240, 232, 255), shadow=False)
    t_closing = render_arabic_text("بتضلّ الشام عامرة بأهلها وياسمينها", FONT_BOLD, 38, COL_GOLD_BRIGHT + (245,), shadow=False)

    layer.alpha_composite(t_syria, (cx - t_syria.width // 2, 700))
    layer.alpha_composite(t_rep, (cx - t_rep.width // 2, 855))

    # Official Flag centerpiece bar
    layer.alpha_composite(flag_lg, (cx - flag_lg.width // 2, 995))

    d.rounded_rectangle((170, 1145, W - 170, 1149), radius=2, fill=COL_GOLD_RICH + (165,))
    layer.alpha_composite(t_motto, (cx - t_motto.width // 2, 1185))
    layer.alpha_composite(t_closing, (cx - t_closing.width // 2, 1275))

    # 3 stars ornament at bottom of card
    for sx, sr in [(cx - 56, 14), (cx, 18), (cx + 56, 14)]:
        d.polygon(star_points(sx, 1425, sr), fill=COL_GOLD_BRIGHT + (240,))

    return layer


def paste_rgba(dst_rgb: Image.Image, overlay: Image.Image, xy: tuple[int, int], opacity: float = 1.0) -> None:
    if opacity <= 0.005:
        return
    if opacity >= 0.995:
        use = overlay
    else:
        use = overlay.copy()
        a = use.getchannel("A").point(lambda v: int(v * opacity))
        use.putalpha(a)
    dst_rgb.paste(use, xy, use.getchannel("A"))


def draw_golden_motes(frame: Image.Image, t: float) -> None:
    d = ImageDraw.Draw(frame, "RGBA")
    for j in range(28):
        bx = (j * 353 + 97) % W
        by = (j * 571 + 140) % H
        x = int((bx + 26.0 * math.sin(t * 0.65 + j * 1.7)) % W)
        y = int((by - t * (18.0 + (j % 4) * 5.0)) % H)
        twinkle = 0.4 + 0.6 * (math.sin(t * 2.2 + j * 1.3) ** 2)
        alpha = int((26 + (j % 4) * 12) * twinkle)
        r = 2 if j % 3 == 0 else 3
        col = (252, 222, 142, alpha) if j % 3 else (255, 255, 255, alpha)
        d.ellipse((x - r, y - r, x + r, y + r), fill=col)


# ---------------------------------------------------------------------------
# 5. Full Pipeline Renderer & Preview Frame Exporter
# ---------------------------------------------------------------------------
def build_pipeline():
    vo_clips = [load_and_tighten_vo(ASSET_DIR / f"vo_0{i}.mp3", tempo=1.15) for i in range(1, 6)]
    wav_path, total_dur, clip_windows, frame_energy = build_master_soundtrack(vo_clips)

    c1_s, c1_e = clip_windows[0]
    c2_s, c2_e = clip_windows[1]
    c3_s, c3_e = clip_windows[2]
    c4_s, c4_e = clip_windows[3]
    c5_s, c5_e = clip_windows[4]

    cut_aleppo = c2_s - 0.14
    cut_hama = c2_s + (c2_e - c2_s) * 0.34
    cut_coast = c2_s + (c2_e - c2_s) * 0.66
    cut_republic = c3_s - 0.14
    cut_flag = c4_s - 0.14
    cut_sunrise = c5_s - 0.14

    shots = [
        {"start": 0.0,          "img": "damascus", "zoom": (1.02, 1.11), "focus": ((0.50, 0.50), (0.49, 0.47))},
        {"start": cut_aleppo,   "img": "aleppo",   "zoom": (1.03, 1.12), "focus": ((0.52, 0.50), (0.50, 0.48))},
        {"start": cut_hama,     "img": "hama",     "zoom": (1.02, 1.11), "focus": ((0.50, 0.54), (0.48, 0.51))},
        {"start": cut_coast,    "img": "coast",    "zoom": (1.03, 1.12), "focus": ((0.50, 0.52), (0.52, 0.49))},
        {"start": cut_republic, "img": "damascus", "zoom": (1.10, 1.19), "focus": ((0.50, 0.46), (0.50, 0.43))},
        {"start": cut_flag,     "img": "flag",     "zoom": (1.01, 1.10), "focus": ((0.50, 0.45), (0.49, 0.43))},
        {"start": cut_sunrise,  "img": "sunrise",  "zoom": (1.02, 1.12), "focus": ((0.50, 0.48), (0.50, 0.45))},
    ]

    captions_spec = [
        {
            "start": c1_s - 0.10,
            "end": c1_e + 0.12,
            "card": make_caption_card("دمشق العتيقة • قلب الشام", "من قلب الشام العتيقة وياسمينها", "بتبلّش حكاية وطن ما بيشبه غير حالو"),
        },
        {
            "start": c2_s - 0.08,
            "end": c2_e + 0.12,
            "card": make_caption_card("حلب • حماة • الساحل • حوران • الجزيرة", "من قلعة حلب ونواعير حماة لبحر الساحل", "وسهول حوران والجزيرة... روح وحدة"),
        },
        {
            "start": c3_s - 0.08,
            "end": c3_e + 0.14,
            "card": make_caption_card("الهوية الوطنية الجامعة", "سوريا مو بس اسم عالخريطة... دولة وعزّ وهوية", "اسمها: الجمهورية العربية السورية!"),
        },
        {
            "start": c4_s - 0.08,
            "end": c4_e + 0.14,
            "card": make_caption_card("راية الحرية والوحدة", "رايتها عالية بنجماتها الثلاث", "وشعبها إيد وحدة وقلب واحد ما بتقبل القسمة"),
        },
        {
            "start": c5_s - 0.08,
            "end": c5_e - 1.25,
            "card": make_caption_card("شام الأمل والمستقبل", "رح تضلّ الشام عامرة بأهلها...", "وتضلّ سوريا الجمهورية العربية السورية!"),
        },
    ]

    imgs = load_scene_images()
    scrim = build_scrim()
    flag_sm = make_syrian_flag_icon(62, 42, radius=7, border=True)
    flag_lg = make_syrian_flag_icon(186, 118, radius=14, border=True)
    eagle_sm = make_golden_eagle_emblem(58)
    eagle_lg = make_golden_eagle_emblem(246)
    eagle_xl = make_golden_eagle_emblem(330)

    header = make_header_base(flag_sm, eagle_sm)
    hook = make_opening_hook()
    rep_center = make_republic_centerpiece(eagle_lg)
    gov_strip = make_governorates_strip()
    finale_poster = make_finale_poster(eagle_xl, flag_lg)

    transition_dur = 0.42

    def render_frame_at(f_idx: int) -> Image.Image:
        t = f_idx / FPS
        # Determine active shot
        s_idx = 0
        for i, sh in enumerate(shots):
            if sh["start"] <= t:
                s_idx = i
            else:
                break
        cur = shots[s_idx]
        nxt_t = shots[s_idx + 1]["start"] if s_idx + 1 < len(shots) else total_dur
        u = (t - cur["start"]) / max(nxt_t - cur["start"], 0.001)
        frame = sample_camera(imgs[cur["img"]], cur["zoom"], cur["focus"], u)

        # Cross-dissolve + warm golden flash on cut
        if s_idx > 0 and t < cur["start"] + transition_dur:
            prev = shots[s_idx - 1]
            prev_u = (t - prev["start"]) / max(cur["start"] - prev["start"], 0.001)
            prev_frame = sample_camera(imgs[prev["img"]], prev["zoom"], prev["focus"], min(1.0, prev_u))
            alpha_cut = smoothstep((t - cur["start"]) / transition_dur)
            frame = Image.blend(prev_frame, frame, alpha_cut)
            # Subtle warm light leak flash peaking at midpoint of transition
            flash = math.sin(math.pi * ((t - cur["start"]) / transition_dur)) * 0.14
            if flash > 0.01:
                gold_veil = Image.new("RGB", (W, H), (255, 224, 152))
                frame = Image.blend(frame, gold_veil, flash)

        paste_rgba(frame, scrim, (0, 0), 1.0)
        draw_golden_motes(frame, t)

        # Opening hook title (Scene 1)
        if t < c1_e + 0.15:
            ha = window_alpha(t, 0.08, c1_e + 0.05, fi=0.35, fo=0.35)
            slide = int((1.0 - ease_out_cubic(min(1.0, max(0.0, (t - 0.08) / 0.45)))) * 26)
            paste_rgba(frame, hook, (0, 210 + slide), ha)

        # Centerpiece Golden Eagle & Republic Medallion (Scene 3)
        if cut_republic - 0.05 <= t <= cut_flag + 0.10:
            ra = window_alpha(t, cut_republic, cut_flag - 0.05, fi=0.38, fo=0.35)
            slide = int((1.0 - ease_out_cubic(min(1.0, max(0.0, (t - cut_republic) / 0.48)))) * 30)
            paste_rgba(frame, rep_center, (0, 215 + slide), ra)

        # Lower-third caption cards
        for cap in captions_spec:
            ca = window_alpha(t, cap["start"], cap["end"], fi=0.24, fo=0.24)
            if ca > 0.005:
                slide = int((1.0 - ease_out_cubic(min(1.0, max(0.0, (t - cap["start"]) / 0.32)))) * 28)
                paste_rgba(frame, cap["card"], ((W - cap["card"].width) // 2, 1348 + slide), ca)

        # 14 Governorates scrolling ribbon
        finale_start = c5_e - 1.30
        if t < finale_start + 0.35:
            ga = window_alpha(t, 0.25, finale_start + 0.25, fi=0.35, fo=0.30)
            if ga > 0.01:
                ribbon = Image.new("RGBA", (W - 108, 62), (0, 0, 0, 0))
                rd = ImageDraw.Draw(ribbon)
                rd.rounded_rectangle((0, 0, ribbon.width - 1, 61), radius=30,
                                     fill=(5, 26, 20, 196), outline=COL_GOLD_RICH + (120,), width=1)
                sw = gov_strip.width
                offset = int((t * 115.0) % sw)
                ribbon.alpha_composite(gov_strip, (-offset, 11))
                ribbon.alpha_composite(gov_strip, (sw - offset, 11))
                # Clip to rounded pill
                rmask = Image.new("L", ribbon.size, 0)
                ImageDraw.Draw(rmask).rounded_rectangle((0, 0, ribbon.width - 1, 61), radius=30, fill=255)
                clipped = Image.new("RGBA", ribbon.size, (0, 0, 0, 0))
                clipped.paste(ribbon, (0, 0), rmask)
                paste_rgba(frame, clipped, (54, 1662), ga)

        # Finale Grand Poster Lockup
        if t >= finale_start:
            fa = smoothstep((t - finale_start) / 0.55)
            paste_rgba(frame, finale_poster, (0, 0), fa)

        # Top header + reactive live audio visualizer stays cleanly anchored at top
        paste_rgba(frame, header, (0, 0), 1.0)
        e_val = float(frame_energy[min(f_idx, len(frame_energy) - 1)])
        draw_audio_eq(frame, e_val, t)

        # Bottom golden + tricolor timeline progress bar
        d_prog = ImageDraw.Draw(frame, "RGBA")
        d_prog.rectangle((54, 1872, W - 54, 1882), fill=(255, 255, 255, 48))
        pw = int((W - 108) * min(1.0, t / max(total_dur - 1.0 / FPS, 0.01)))
        if pw > 4:
            d_prog.rounded_rectangle((W - 54 - pw, 1872, W - 54, 1882), radius=5, fill=COL_GOLD_BRIGHT + (240,))

        return frame

    return render_frame_at, wav_path, total_dur, clip_windows


def render_preview_frames() -> None:
    render_frame_at, _wav, total_dur, cw = build_pipeline()
    print(f"Total video duration: {total_dur:.2f}s (must be < 30.0s)")
    sample_times = [1.5, cw[1][0] + 0.8, cw[2][0] + 1.2, cw[3][0] + 1.2, total_dur - 0.6]
    for idx, st in enumerate(sample_times, 1):
        f_idx = min(int(total_dur * FPS) - 1, int(round(st * FPS)))
        im = render_frame_at(f_idx)
        out_p = Path(f"/tmp/syria_preview_{idx}.jpg")
        im.save(out_p, quality=92)
        print(f"Saved preview {out_p} at t={st:.2f}s")


def render_full_video() -> None:
    render_frame_at, wav_path, total_dur, _cw = build_pipeline()
    if total_dur >= 30.0:
        raise RuntimeError(f"Video duration {total_dur:.2f}s exceeds 30s limit!")
    total_frames = int(round(total_dur * FPS))
    ffmpeg = imageio_ffmpeg.get_ffmpeg_exe()

    cmd = [
        ffmpeg,
        "-hide_banner",
        "-loglevel",
        "error",
        "-y",
        "-f",
        "rawvideo",
        "-pix_fmt",
        "rgb24",
        "-s",
        f"{W}x{H}",
        "-r",
        str(FPS),
        "-i",
        "pipe:0",
        "-i",
        str(wav_path),
        "-filter_complex",
        "[1:a]loudnorm=I=-14:TP=-1.2:LRA=9[a]",
        "-map",
        "0:v:0",
        "-map",
        "[a]",
        "-t",
        f"{total_dur:.3f}",
        "-c:v",
        "libx264",
        "-preset",
        "medium",
        "-crf",
        "18",
        "-profile:v",
        "high",
        "-pix_fmt",
        "yuv420p",
        "-r",
        str(FPS),
        "-g",
        str(FPS * 2),
        "-c:a",
        "aac",
        "-b:a",
        "192k",
        "-ar",
        str(AUDIO_SR),
        "-ac",
        "2",
        "-metadata",
        "title=سوريا — الجمهورية العربية السورية",
        "-metadata",
        "comment=فيديو قصير باللهجة السورية الشامية مع مؤثرات صوتية احترافية",
        "-movflags",
        "+faststart",
        str(OUT_VIDEO),
    ]

    print(f"Rendering {total_frames} frames ({total_dur:.2f}s @ {FPS}fps) -> {OUT_VIDEO.name}", flush=True)
    proc = subprocess.Popen(cmd, stdin=subprocess.PIPE, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)
    assert proc.stdin is not None
    try:
        for idx in range(total_frames):
            frame = render_frame_at(idx)
            proc.stdin.write(frame.tobytes())
            if idx % 150 == 0 or idx == total_frames - 1:
                print(f"  {idx + 1:4d}/{total_frames} frames ({(idx + 1) * 100 // total_frames}%)", flush=True)
        proc.stdin.close()
    except BrokenPipeError:
        proc.stdin.close()
        err = proc.stderr.read().decode("utf-8", "replace") if proc.stderr else ""
        raise RuntimeError(f"FFmpeg broken pipe:\n{err}")

    stderr = proc.stderr.read().decode("utf-8", "replace") if proc.stderr else ""
    rc = proc.wait()
    if rc != 0:
        raise RuntimeError(f"FFmpeg exited {rc}:\n{stderr}")
    size_mb = OUT_VIDEO.stat().st_size / (1024 * 1024)
    print(f"Done! {OUT_VIDEO} ({size_mb:.2f} MB, {total_dur:.2f}s)", flush=True)


if __name__ == "__main__":
    if "--preview" in sys.argv:
        render_preview_frames()
    else:
        render_full_video()

#!/usr/bin/env python3
"""Render 'أول اكتشاف أثري في مصر' — 30.00s 1080x1920 @ 30fps Vertical Cinema Short.

Features:
- Real-time dense 3D depth-map parallax camera rig (cv2.remap + cv2.warpPerspective)
- 10 fast-paced documentary cuts (~2.5-3.2s avg) synced to studio Arabic narration
- Live procedural VFX inside scenes:
  * 3D perspective flyover of Egypt obsidian-gold terrain + animated Nile surge & radar rings
  * High-speed 3,000-year chrono-rewind counter (1799 م -> 1250 ق.م) + 3D flying rings
  * Multi-layer Sahara sandstorm & rolling storm clouds
  * Live turbulent torch flames + dynamic flickering firelight illumination
  * Physical desert sand-erosion wipe uncovering carved limestone cartouches + chisel sparks
  * Celestial time-lapse sweeping sunbeams & wheeling starfield across Colossi
  * Rosetta Stone molten-gold laser decipherment scanner + 3D floating golden hieroglyphs
  * Soaring animated falcons & volumetric god-rays over Giza Pyramids at sunrise
- Whip-pan directional motion-blur & golden anamorphic flare transitions
- Word-by-word spring-animated Arabic kinetic captions (HarfBuzz + Cairo Black 900)
"""
from __future__ import annotations

import io
import json
import math
import re
import subprocess
import sys
from pathlib import Path

import cv2
import freetype
import imageio_ffmpeg
import numpy as np
import uharfbuzz as hb
from PIL import Image

ROOT = Path(__file__).resolve().parent
ASSETS = ROOT / "egypt_assets"
FONTS = ROOT / "fonts"
OUT_MP4 = ROOT / "egypt-first-discovery.mp4"

W, H = 1080, 1920
OV_W, OV_H = 1296, 2304  # 20% overscan canvas for 3D parallax & camera shake
FPS = 30
DURATION = 30.0
TOTAL_FRAMES = int(round(DURATION * FPS))  # 900 frames


# ==============================================================================
# 1. HARFBUZZ + FREETYPE NATIVE OPENTYPE ARABIC RENDERER (ZERO TOFU, RTL PERFECT)
# ==============================================================================
class HarfBuzzArabic:
    def __init__(self, font_path: Path):
        self.font_bytes = font_path.read_bytes()
        self.hb_blob = hb.Blob(self.font_bytes)
        self.hb_face = hb.Face(self.hb_blob)
        self.upem = self.hb_face.upem
        self._cache: dict[tuple, np.ndarray] = {}

    def render_word(
        self,
        word: str,
        font_size: int,
        color_rgb: tuple[int, int, int],
        stroke_px: int = 5,
        glow: bool = False,
    ) -> tuple[np.ndarray, int, int]:
        """Return (rgba_uint8, advance_width, baseline_offset_from_top) for one word."""
        key = (word, font_size, color_rgb, stroke_px, glow)
        if key in self._cache:
            return self._cache[key]

        ft_face = freetype.Face(io.BytesIO(self.font_bytes))
        ft_face.set_pixel_sizes(0, font_size)
        scale = font_size / self.upem

        is_ar = bool(re.search(r"[\u0600-\u06FF]", word))
        hb_font = hb.Font(self.hb_face)
        buf = hb.Buffer()
        buf.add_str(word)
        buf.direction = "rtl" if is_ar else "ltr"
        buf.script = "Arab" if is_ar else "Latn"
        buf.language = "ar" if is_ar else "en"
        hb.shape(hb_font, buf)

        glyphs = []
        pen_x = 0.0
        for info, pos in zip(buf.glyph_infos, buf.glyph_positions):
            gid = info.codepoint
            x_off = pos.x_offset * scale
            y_off = pos.y_offset * scale
            x_adv = pos.x_advance * scale
            ft_face.load_glyph(gid, freetype.FT_LOAD_RENDER | freetype.FT_LOAD_TARGET_NORMAL)
            bmp = ft_face.glyph.bitmap
            arr = (
                np.array(bmp.buffer, dtype=np.uint8).reshape(bmp.rows, bmp.width)
                if bmp.rows > 0 and bmp.width > 0
                else np.zeros((0, 0), dtype=np.uint8)
            )
            glyphs.append((pen_x + x_off + ft_face.glyph.bitmap_left, -y_off - ft_face.glyph.bitmap_top, arr))
            pen_x += x_adv

        adv_w = max(4, int(round(pen_x)))
        pad = stroke_px * 3 + (22 if glow else 14)
        box_w = adv_w + pad * 2
        box_h = int(font_size * 1.95) + pad * 2
        base_y = int(box_h * 0.62)

        alpha = np.zeros((box_h, box_w), dtype=np.float32)
        for gx, gy, arr in glyphs:
            if arr.size == 0:
                continue
            x0 = int(round(pad + gx))
            y0 = int(round(base_y + gy))
            gh, gw = arr.shape
            if x0 < 0 or y0 < 0 or x0 + gw > box_w or y0 + gh > box_h:
                continue
            a = arr.astype(np.float32) / 255.0
            alpha[y0 : y0 + gh, x0 : x0 + gw] = np.maximum(alpha[y0 : y0 + gh, x0 : x0 + gw], a)

        if stroke_px > 0:
            k = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (stroke_px * 2 + 1, stroke_px * 2 + 1))
            stroke_a = cv2.dilate(alpha, k)
            stroke_a = cv2.GaussianBlur(stroke_a, (3, 3), 0.75) * 0.94
        else:
            stroke_a = np.zeros_like(alpha)

        shadow_a = cv2.GaussianBlur(stroke_a if stroke_px > 0 else alpha, (19, 19), 5.5) * 0.62
        shadow_a = np.roll(shadow_a, 5, axis=0)

        out = np.zeros((box_h, box_w, 4), dtype=np.float32)

        def comp_solid(rgb: tuple[int, int, int], src_a: np.ndarray) -> None:
            sa = np.clip(src_a, 0.0, 1.0)
            da = out[..., 3]
            ca = sa + da * (1.0 - sa)
            m = ca > 1e-5
            for c in range(3):
                out[..., c] = np.where(m, (rgb[c] * sa + out[..., c] * da * (1.0 - sa)) / np.maximum(ca, 1e-5), 0.0)
            out[..., 3] = ca

        comp_solid((0, 0, 0), shadow_a)
        if glow:
            glow_a = cv2.GaussianBlur(alpha, (29, 29), 9.0) * 0.58
            comp_solid(color_rgb, glow_a)
        if stroke_px > 0:
            comp_solid((8, 6, 12), stroke_a)
        comp_solid(color_rgb, alpha)

        out[..., 3] *= 255.0
        res = (np.clip(out, 0, 255).astype(np.uint8), adv_w, pad)
        self._cache[key] = res
        return res

    def render_static_line(
        self,
        words_colors: list[tuple[str, tuple[int, int, int]]],
        font_size: int,
        stroke_px: int = 5,
        glow: bool = False,
    ) -> np.ndarray:
        """Render an entire RTL line into a tight RGBA numpy array."""
        rendered = [self.render_word(w, font_size, col, stroke_px, glow) for w, col in words_colors]
        space = int(font_size * 0.27)
        total_adv = sum(r[1] for r in rendered) + space * max(0, len(rendered) - 1)
        pad = rendered[0][2] if rendered else 20
        box_h = rendered[0][0].shape[0] if rendered else font_size * 2
        box_w = total_adv + pad * 2
        canvas = np.zeros((box_h, box_w, 4), dtype=np.uint8)
        cur_right = box_w - pad
        for rgba, adv_w, w_pad in rendered:
            x_word = cur_right - adv_w - w_pad
            paste_rgba(canvas, rgba, x_word, 0, 1.0)
            cur_right -= adv_w + space
        return canvas


def paste_rgba(dst_rgb_or_rgba: np.ndarray, src_rgba: np.ndarray, x: int, y: int, opacity: float = 1.0) -> None:
    """Fast vectorized alpha composite onto RGB (H,W,3) or RGBA (H,W,4) canvas."""
    if opacity <= 0.003:
        return
    dh, dw = dst_rgb_or_rgba.shape[:2]
    sh, sw = src_rgba.shape[:2]
    x0, y0 = max(0, x), max(0, y)
    x1, y1 = min(dw, x + sw), min(dh, y + sh)
    if x1 <= x0 or y1 <= y0:
        return
    sx0, sy0 = x0 - x, y0 - y
    sub_src = src_rgba[sy0 : sy0 + (y1 - y0), sx0 : sx0 + (x1 - x0)]
    sa = (sub_src[..., 3:4].astype(np.float32) / 255.0) * min(1.0, max(0.0, opacity))
    if dst_rgb_or_rgba.shape[2] == 3:
        dst_slice = dst_rgb_or_rgba[y0:y1, x0:x1].astype(np.float32)
        blended = sub_src[..., :3].astype(np.float32) * sa + dst_slice * (1.0 - sa)
        dst_rgb_or_rgba[y0:y1, x0:x1] = np.clip(blended, 0, 255).astype(np.uint8)
    else:
        dst_slice = dst_rgb_or_rgba[y0:y1, x0:x1].astype(np.float32)
        da = dst_slice[..., 3:4] / 255.0
        ca = sa + da * (1.0 - sa)
        rgb = (sub_src[..., :3].astype(np.float32) * sa + dst_slice[..., :3] * da * (1.0 - sa)) / np.maximum(ca, 1e-5)
        dst_rgb_or_rgba[y0:y1, x0:x1, :3] = np.clip(rgb, 0, 255).astype(np.uint8)
        dst_rgb_or_rgba[y0:y1, x0:x1, 3:4] = np.clip(ca * 255.0, 0, 255).astype(np.uint8)


# ==============================================================================
# 2. MATH, EASING & PROCEDURAL NOISE HELPERS
# ==============================================================================
def clamp(v: float, lo: float = 0.0, hi: float = 1.0) -> float:
    return max(lo, min(hi, v))


def smoothstep(v: float) -> float:
    v = clamp(v)
    return v * v * (3.0 - 2.0 * v)


def ease_out_back(v: float) -> float:
    """Spring pop overshoot for viral Reels typography."""
    v = clamp(v)
    c1 = 1.95
    c3 = c1 + 1.0
    return 1.0 + c3 * ((v - 1.0) ** 3) + c1 * ((v - 1.0) ** 2)


def make_tileable_turbulence(h: int = 512, w: int = 512, seed: int = 101) -> np.ndarray:
    """Multi-octave smooth turbulence field in [0, 1] for live fire, sand & clouds."""
    rng = np.random.default_rng(seed)
    acc = np.zeros((h, w), dtype=np.float32)
    weight = 0.0
    for octave, (gh, gw, amp) in enumerate([(8, 8, 0.50), (16, 16, 0.28), (32, 32, 0.14), (64, 64, 0.08)]):
        g = rng.uniform(0.0, 1.0, (gh, gw)).astype(np.float32)
        up = cv2.resize(g, (w, h), interpolation=cv2.INTER_CUBIC)
        acc += up * amp
        weight += amp
    acc /= weight
    return np.clip((acc - acc.min()) / max(1e-5, acc.max() - acc.min()), 0.0, 1.0)


# ==============================================================================
# 3. BESPOKE 3D DEPTH MAPS & SCENE PLATE LOADER
# ==============================================================================
def load_plate(name: str) -> np.ndarray:
    im = Image.open(ASSETS / name).convert("RGB")
    im = im.resize((OV_W, OV_H), Image.Resampling.LANCZOS)
    arr = np.array(im, dtype=np.uint8)
    # Subtle cinema micro-contrast sharpen
    blur = cv2.GaussianBlur(arr, (0, 0), 1.4)
    arr = cv2.addWeighted(arr, 1.18, blur, -0.18, 0)
    return arr


def build_depth_map(plate_rgb: np.ndarray, mode: str) -> np.ndarray:
    """Construct edge-guided dense 3D depth map D(y,x) in [0, 1] (1=near camera, 0=horizon)."""
    yy = np.linspace(0.0, 1.0, OV_H, dtype=np.float32)[:, None]
    xx = np.linspace(0.0, 1.0, OV_W, dtype=np.float32)[None, :]
    d = np.zeros((OV_H, OV_W), dtype=np.float32)

    if mode == "rosetta_hook":
        # Bottom stone rubble near (1.0), left wall (0.72), Rosetta center (0.48), window/right cavern (0.12)
        d = 0.14 + 0.86 * np.clip((yy - 0.62) / 0.38, 0, 1) ** 1.3
        left_wall = np.clip((0.34 - xx) / 0.20, 0, 1) * (1.0 - 0.3 * yy)
        stone = np.exp(-(np.abs((xx - 0.49) / 0.20) ** 4 + np.abs((yy - 0.50) / 0.20) ** 4))
        d = np.maximum(d, 0.68 * left_wall)
        d = np.maximum(d, 0.48 * stone)
    elif mode == "saqqara_buried":
        # Sky/clouds (0.05), Step Pyramid (0.30), temple ruins (0.55), foreground sand dune crest (1.0)
        d = np.where(yy < 0.39, 0.05, 0.18 + 0.82 * np.clip((yy - 0.39) / 0.61, 0, 1) ** 1.45)
        pyr = np.exp(-(np.abs((xx - 0.48) / 0.22) ** 2 + np.abs((yy - 0.47) / 0.09) ** 2))
        d = np.where((yy > 0.36) & (yy < 0.56), np.maximum(d * 0.7, 0.32 * pyr + 0.22), d)
    elif mode == "prince":
        # Distant pyramid/sky (0.10), Prince Khaemwaset (0.56), foreground left pillar & bottom rocks (1.0)
        d = 0.10 + 0.35 * np.clip((yy - 0.50) / 0.50, 0, 1)
        prince = np.exp(-(np.abs((xx - 0.52) / 0.21) ** 2.6 + np.abs((yy - 0.58) / 0.28) ** 2.6))
        pillar = np.clip((0.20 - xx) / 0.06, 0, 1)
        ledge = np.clip((yy - 0.82) / 0.14, 0, 1)
        d = np.maximum(d, 0.56 * prince)
        d = np.maximum(d, 0.96 * pillar)
        d = np.maximum(d, 0.90 * ledge)
    elif mode == "excavation":
        # Step pyramid sky (0.08), trench & portal (0.48), foreground coiled ropes & beams (1.0)
        d = np.where(yy < 0.26, 0.08, 0.25 + 0.75 * np.clip((yy - 0.26) / 0.74, 0, 1) ** 1.55)
        left_rig = np.clip((0.25 - xx) / 0.12, 0, 1) * np.clip((yy - 0.34) / 0.3, 0, 1)
        d = np.maximum(d, 0.85 * left_rig)
    elif mode == "stele_carving":
        # Dark tomb wall (0.10), limestone stele (0.52), sculptor hands & mallet (0.92)
        stele = np.clip((xx - 0.26) / 0.12, 0, 1) * np.clip((yy - 0.26) / 0.10, 0, 1)
        hands = np.exp(-(np.abs((xx - 0.32) / 0.26) ** 2.2 + np.abs((yy - 0.58) / 0.28) ** 2.2))
        d = 0.10 + 0.45 * stele
        d = np.maximum(d, 0.92 * hands)
    elif mode == "colossi":
        # Night sky (0.04), right column colonnade (0.42..0.75), colossal statue & pedestal (0.68..0.95)
        gray = cv2.cvtColor(plate_rgb, cv2.COLOR_RGB2GRAY).astype(np.float32) / 255.0
        sky_mask = (yy < 0.42) & (plate_rgb[..., 2] > plate_rgb[..., 0] * 0.95) & (gray < 0.35)
        statue = np.exp(-(np.abs((xx - 0.27) / 0.23) ** 2.4 + np.abs((yy - 0.55) / 0.38) ** 2.4))
        right_col = np.clip((xx - 0.68) / 0.22, 0, 1) * (0.45 + 0.45 * yy)
        pedestal = np.clip((0.55 - xx) / 0.15, 0, 1) * np.clip((yy - 0.68) / 0.15, 0, 1)
        d = 0.28 + 0.35 * yy
        d = np.maximum(d, 0.66 * statue)
        d = np.maximum(d, right_col)
        d = np.maximum(d, 0.92 * pedestal)
        d = np.where(sky_mask, 0.04, d)
    elif mode == "rosetta_glow":
        # Chamber walls (0.14), Rosetta slab (0.62), foreground stone plinth & floor (0.85..1.0)
        slab = np.exp(-(np.abs((xx - 0.50) / 0.24) ** 3.2 + np.abs((yy - 0.54) / 0.24) ** 3.2))
        floor = np.clip((yy - 0.74) / 0.26, 0, 1)
        d = np.where(yy > 0.74, 0.62 + 0.38 * floor, 0.14 + 0.52 * slab)
    elif mode == "giza_finale":
        # Clouds/sun (0.04), Nile (0.16), Pyramids (0.38), Sphinx (0.58), foreground rocky ridge (1.0)
        d = np.where(yy < 0.38, 0.04, 0.14 + 0.86 * np.clip((yy - 0.38) / 0.62, 0, 1) ** 1.65)
        ridge = np.clip((yy - 0.68) / 0.14, 0, 1)
        d = np.maximum(d, 0.95 * ridge)

    d = np.nan_to_num(d, nan=0.2).astype(np.float32)
    # Edge-preserving bilateral refinement at 1/4 res so depth hugs real silhouettes!
    small_d = cv2.resize(d, (OV_W // 4, OV_H // 4), interpolation=cv2.INTER_AREA).astype(np.float32)
    small_d = cv2.bilateralFilter(small_d, d=9, sigmaColor=0.22, sigmaSpace=11.0)
    return cv2.resize(small_d, (OV_W, OV_H), interpolation=cv2.INTER_CUBIC).astype(np.float32)


class ParallaxRig:
    def __init__(self):
        # Precompute normalized output grid for W x H
        gx = np.linspace(0.0, 1.0, W, dtype=np.float32)
        gy = np.linspace(0.0, 1.0, H, dtype=np.float32)
        self.grid_x, self.grid_y = np.meshgrid(gx, gy)

    def render(
        self,
        plate: np.ndarray,
        depth: np.ndarray,
        zoom: float,
        cam_x: float,
        cam_y: float,
        parallax_x: float,
        parallax_y: float,
        roll_deg: float = 0.0,
    ) -> np.ndarray:
        """Render W x H view from OV_W x OV_H plate with 3D depth parallax & camera roll."""
        # Sample depth at current approximate camera center
        inv_z = 1.0 / max(1.0, zoom)
        u = 0.5 + (self.grid_x - 0.5) * inv_z + cam_x
        v = 0.5 + (self.grid_y - 0.5) * inv_z + cam_y

        if abs(roll_deg) > 0.01:
            ang = math.radians(roll_deg)
            ca, sa = math.cos(ang), math.sin(ang)
            du = u - 0.5
            dv = (v - 0.5) * (H / W)
            u = 0.5 + du * ca - dv * sa
            v = 0.5 + (du * sa + dv * ca) * (W / H)

        mx0 = np.clip(u * (OV_W - 1), 0, OV_W - 1)
        my0 = np.clip(v * (OV_H - 1), 0, OV_H - 1)
        d_view = cv2.remap(depth, mx0, my0, cv2.INTER_LINEAR, borderMode=cv2.BORDER_REFLECT_101)

        # Near objects shift opposite to camera translation -> true 3D motion parallax!
        disp_x = (d_view - 0.35) * parallax_x * OV_W
        disp_y = (d_view - 0.35) * parallax_y * OV_H
        # Depth-dependent perspective expansion during dolly-in
        dolly_scale = 1.0 + (zoom - 1.0) * 0.28 * (d_view - 0.35)
        mx = 0.5 * OV_W + (mx0 - 0.5 * OV_W) / np.maximum(0.75, dolly_scale) + disp_x
        my = 0.5 * OV_H + (my0 - 0.5 * OV_H) / np.maximum(0.75, dolly_scale) + disp_y

        return cv2.remap(plate, mx.astype(np.float32), my.astype(np.float32), cv2.INTER_LINEAR, borderMode=cv2.BORDER_REFLECT_101)


# ==============================================================================
# 4. LIVE INTERNAL SCENE SHADERS (FIRE, SANDSTORM, GOD-RAYS, 3D MAP, SCANNER)
# ==============================================================================
def add_god_rays(frame: np.ndarray, light_xy: tuple[float, float], intensity: float, color=(255, 206, 118)) -> np.ndarray:
    """Fast radial volumetric light-shaft crepuscular rays."""
    if intensity <= 0.01:
        return frame
    sh, sw = H // 4, W // 4
    small = cv2.resize(frame, (sw, sh), interpolation=cv2.INTER_AREA).astype(np.float32)
    lum = (0.299 * small[..., 0] + 0.587 * small[..., 1] + 0.114 * small[..., 2]) / 255.0
    bright = np.clip((lum - 0.48) * 2.2, 0.0, 1.0)

    lx, ly = light_xy[0] * sw, light_xy[1] * sh
    yy, xx = np.mgrid[0:sh, 0:sw].astype(np.float32)
    acc = np.zeros_like(bright)
    # 10-step radial march toward light source
    for step in range(1, 11):
        frac = 1.0 - step * 0.065
        sx = lx + (xx - lx) * frac
        sy = ly + (yy - ly) * frac
        acc += cv2.remap(bright, sx, sy, cv2.INTER_LINEAR, borderMode=cv2.BORDER_CONSTANT) * (0.88 ** step)
    acc = cv2.GaussianBlur(acc / 5.5, (15, 15), 4.0)
    rays = cv2.resize(acc, (W, H), interpolation=cv2.INTER_LINEAR)[..., None] * intensity
    out = frame.astype(np.float32) + rays * np.array(color, dtype=np.float32)[None, None, :]
    return np.clip(out, 0, 255).astype(np.uint8)


def add_live_torch_fire(
    frame: np.ndarray,
    turb: np.ndarray,
    center_xy: tuple[float, float],
    radius_px: int,
    t: float,
    illuminate_strength: float = 0.28,
) -> np.ndarray:
    """Procedural multi-octave rising fire tongues + flickering warm light on scene."""
    out = frame.astype(np.float32)
    cx, cy = int(center_xy[0] * W), int(center_xy[1] * H)

    # 1. Dynamic warm orange-gold point-light flicker across nearby stone & character
    flicker = 0.78 + 0.22 * math.sin(t * 19.3) * math.cos(t * 11.7) + 0.10 * math.sin(t * 37.0)
    yy, xx = np.ogrid[0:H:4, 0:W:4]
    dist2 = ((xx - cx) ** 2 + (yy - cy) ** 2) / float((W * 0.58) ** 2)
    falloff = np.exp(-dist2 * 1.45).astype(np.float32) * illuminate_strength * flicker
    falloff_full = cv2.resize(falloff, (W, H), interpolation=cv2.INTER_LINEAR)[..., None]
    warm = np.array([255.0, 152.0, 54.0], dtype=np.float32)[None, None, :]
    out = out * (1.0 + falloff_full * 0.35) + warm * falloff_full * 0.55

    # 2. Procedural turbulent flame tongue sprite around torch tip
    fw, fh = radius_px * 2, int(radius_px * 3.1)
    x0, y0 = cx - radius_px, cy - int(fh * 0.72)
    x1, y1 = x0 + fw, y0 + fh
    if x1 > 0 and y1 > 0 and x0 < W and y0 < H:
        u = np.linspace(-1.0, 1.0, fw, dtype=np.float32)[None, :]
        v = np.linspace(0.0, 1.0, fh, dtype=np.float32)[:, None]  # 0=tip, 1=base
        # Sample scrolling turbulence upward!
        ty = ((v * 180.0 + t * 290.0) % 512).astype(np.float32)
        tx = (((u * 0.5 + 0.5) * 140.0 + 25.0 * np.sin(v * 6.0 - t * 14.0)) % 512).astype(np.float32)
        tx_g, ty_g = np.broadcast_arrays(tx, ty)
        n_val = cv2.remap(turb, tx_g, ty_g, cv2.INTER_LINEAR, borderMode=cv2.BORDER_WRAP)

        # Teardrop flame shape narrowed at top, swayed by wind & turbulence
        sway = (1.0 - v) * (0.28 * math.sin(t * 13.0) + 0.35 * (n_val - 0.5))
        width_profile = 0.12 + 0.85 * (v ** 0.65) * np.sin(np.clip(v * math.pi * 0.92, 0, math.pi))
        core = np.clip(1.0 - np.abs(u - sway) / np.maximum(0.08, width_profile), 0.0, 1.0)
        heat = np.clip(core * (0.62 + 0.75 * n_val) * (0.35 + 0.75 * v), 0.0, 1.25)

        # Blackbody fire color ramp: outer deep orange -> gold -> white-hot core
        fr = np.clip(heat * 2.4, 0, 1) * 255.0
        fg = np.clip((heat - 0.22) * 1.85, 0, 1) * 235.0
        fb = np.clip((heat - 0.62) * 2.2, 0, 1) * 210.0
        flame_rgb = np.stack([fr, fg, fb], axis=-1)
        flame_rgb = cv2.GaussianBlur(flame_rgb, (9, 9), 2.2)

        cx0, cy0 = max(0, x0), max(0, y0)
        cx1, cy1 = min(W, x1), min(H, y1)
        sub_f = flame_rgb[cy0 - y0 : cy1 - y0, cx0 - x0 : cx1 - x0]
        # Screen/Additive blend for realistic self-luminous fire!
        out[cy0:cy1, cx0:cx1] = 255.0 - (255.0 - out[cy0:cy1, cx0:cx1]) * (255.0 - sub_f * 0.88) / 255.0

    # 3. Rising glowing fire embers (additive bloom layer — crisp golden-white sparks!)
    ember_layer = np.zeros((H, W, 3), dtype=np.uint8)
    for i in range(22):
        life = (t * (0.65 + (i % 5) * 0.14) + i * 0.173) % 1.0
        ex = int(cx + math.sin(life * 8.0 + i * 2.1) * (28 + life * 95) + life * 45)
        ey = int(cy - life * 420)
        if 4 <= ex < W - 4 and 4 <= ey < H - 4:
            a_emb = clamp((1.0 - life) * (0.65 + 0.35 * math.sin(t * 28 + i)))
            r_px = 2 if i % 3 else 3
            col_core = (int(255 * a_emb), int(215 * a_emb), int(115 * a_emb))
            cv2.circle(ember_layer, (ex, ey), r_px, col_core, -1, cv2.LINE_AA)
    ember_bloom = cv2.GaussianBlur(ember_layer, (11, 11), 3.0).astype(np.float32) * 1.4
    d_canvas = out + ember_layer.astype(np.float32) + ember_bloom

    return np.clip(d_canvas, 0, 255).astype(np.uint8)


def add_sandstorm_layers(frame: np.ndarray, turb: np.ndarray, t: float, strength: float = 1.0) -> np.ndarray:
    """3-plane turbulent Sahara blowing sand sheets + high-velocity foreground wind streaks."""
    out = frame.astype(np.float32)
    yy = np.linspace(0.0, 1.0, H // 2, dtype=np.float32)[:, None]
    xx = np.linspace(0.0, 1.0, W // 2, dtype=np.float32)[None, :]

    # Midground swirling amber sand haze (stronger near dunes y > 0.35)
    tx1 = ((xx * 320.0 + t * 260.0) % 512).astype(np.float32)
    ty1 = ((yy * 260.0 + 18.0 * np.sin(xx * 5.0 + t * 3.5)) % 512).astype(np.float32)
    tx1_g, ty1_g = np.broadcast_arrays(tx1, ty1)
    haze1 = cv2.remap(turb, tx1_g, ty1_g, cv2.INTER_LINEAR, borderMode=cv2.BORDER_WRAP)

    # Fast foreground wind-sheared sand sheet
    tx2 = ((xx * 190.0 + t * 620.0) % 512).astype(np.float32)
    ty2 = ((yy * 420.0 - t * 45.0) % 512).astype(np.float32)
    tx2_g, ty2_g = np.broadcast_arrays(tx2, ty2)
    haze2 = cv2.remap(turb, tx2_g, ty2_g, cv2.INTER_LINEAR, borderMode=cv2.BORDER_WRAP)
    # Directional motion blur along wind vector
    k_wind = np.ones((1, 23), dtype=np.float32) / 23.0
    haze2 = cv2.filter2D(haze2, -1, k_wind)

    height_mask = np.clip((yy - 0.18) / 0.72, 0.08, 1.0) ** 1.15
    sand_a = np.clip((0.45 * (haze1 ** 1.6) + 0.65 * np.clip(haze2 - 0.38, 0, 1) * 1.8) * height_mask * strength * 0.42, 0.0, 0.58)
    sand_full = cv2.resize(sand_a, (W, H), interpolation=cv2.INTER_LINEAR)[..., None]
    sand_col = np.array([232.0, 184.0, 114.0], dtype=np.float32)[None, None, :]
    out = out * (1.0 - sand_full) + sand_col * sand_full

    # Individual fast motion-blurred foreground sand streaks
    overlay = np.zeros_like(frame)
    for i in range(38):
        u = ((t * (1.35 + (i % 6) * 0.22) + i * 0.137) % 1.0)
        sx = int(W * (1.15 - 1.35 * u))
        sy = int(H * (0.30 + 0.68 * ((i * 73) % 100) / 100.0) + 28 * math.sin(u * 5.0 + i))
        length = 35 + (i % 4) * 22
        cv2.line(overlay, (sx, sy), (sx + length, sy - 4), (245, 205, 145), 2, cv2.LINE_AA)
    out += overlay.astype(np.float32) * (0.28 * strength)
    return np.clip(out, 0, 255).astype(np.uint8)


def render_shot2_3d_map(
    map_plate: np.ndarray,
    hb_black: HarfBuzzArabic,
    hb_bold: HarfBuzzArabic,
    t: float,
    t0: float,
    t1: float,
) -> np.ndarray:
    """Pure 3D perspective camera flyover of Egypt obsidian-gold terrain +
    live Nile pulse + radar lock-on + spinning 1799 -> 1250 BC counter + 3D rings!"""
    prog = clamp((t - t0) / max(0.01, t1 - t0))
    ep = smoothstep(prog)

    # 1. True 3D perspective camera dive & tilt via homography (cv2.warpPerspective)
    # Tilt top edge inward (3D pitch ~32 deg) and pan from Rosetta Delta down toward Saqqara!
    tilt = 0.14 + 0.10 * ep
    roll = -0.03 + 0.06 * ep
    zoom_m = 1.16 + 0.34 * ep
    pan_y = -0.10 + 0.08 * ep  # Focus upper-middle Egypt (Delta -> Giza/Saqqara)

    src_pts = np.float32([[0, 0], [OV_W, 0], [OV_W, OV_H], [0, OV_H]])
    cx, cy = W * 0.5, H * (0.52 - pan_y)
    hw, hh = (W * 0.5) * zoom_m, (H * 0.5) * zoom_m
    dst_pts = np.float32([
        [cx - hw * (1.0 - tilt), cy - hh * (0.86) + hw * roll],
        [cx + hw * (1.0 - tilt), cy - hh * (0.86) - hw * roll],
        [cx + hw * (1.0 + tilt * 0.85), cy + hh * 1.05 - hw * roll],
        [cx - hw * (1.0 + tilt * 0.85), cy + hh * 1.05 + hw * roll],
    ])
    H_mat = cv2.getPerspectiveTransform(src_pts, dst_pts)
    frame = cv2.warpPerspective(map_plate, H_mat, (W, H), flags=cv2.INTER_LINEAR, borderMode=cv2.BORDER_CONSTANT, borderValue=(10, 9, 12))

    # Project 2 key geographic coordinates through H_mat!
    # In map_plate (OV_W x OV_H):
    # Rashid (Rosetta, Western Nile Delta mouth): ~(0.33, 0.085)
    # Saqqara / Memphis (just below Delta apex): ~(0.445, 0.225)
    pts_map = np.float32([[[0.33 * OV_W, 0.085 * OV_H], [0.415 * OV_W, 0.165 * OV_H], [0.445 * OV_W, 0.225 * OV_H]]])
    pts_scr = cv2.perspectiveTransform(pts_map, H_mat)[0]
    p_rashid = tuple(pts_scr[0].astype(int))
    p_apex = tuple(pts_scr[1].astype(int))
    p_saqqara = tuple(pts_scr[2].astype(int))

    # 2. Live Animated Golden Energy Pulse + Radar Rings on Rashid -> Saqqara!
    overlay = np.zeros_like(frame, dtype=np.float32)
    for pulse_i in range(3):
        r_rad = int(((t * 1.6 + pulse_i * 0.33) % 1.0) * 115)
        a_r = 1.0 - r_rad / 115.0
        target_pt = p_rashid if prog < 0.32 else p_saqqara
        col_r = (255 * a_r, 195 * a_r, 70 * a_r)
        cv2.ellipse(overlay, target_pt, (r_rad, int(r_rad * 0.68)), 0, 0, 360, col_r, 3, cv2.LINE_AA)

    # Animated glowing trajectory arc from Rashid (1799) -> Saqqara (1250 BC)
    trace_p = clamp((prog - 0.12) / 0.55)
    if trace_p > 0.01:
        n_seg = 40
        curve_pts = []
        for k in range(int(n_seg * trace_p) + 1):
            u = k / n_seg
            bx = (1 - u) ** 2 * p_rashid[0] + 2 * (1 - u) * u * p_apex[0] + u ** 2 * p_saqqara[0]
            by = (1 - u) ** 2 * p_rashid[1] + 2 * (1 - u) * u * p_apex[1] + u ** 2 * p_saqqara[1]
            curve_pts.append([int(bx), int(by)])
        if len(curve_pts) >= 2:
            arr_pts = np.array(curve_pts, dtype=np.int32)
            cv2.polylines(overlay, [arr_pts], False, (255, 220, 110), 7, cv2.LINE_AA)
            cv2.circle(overlay, tuple(arr_pts[-1]), 14, (255, 245, 190), -1, cv2.LINE_AA)

    # 3. Flying 3D Perspective Hieroglyphic Chrono-Rings rushing past camera!
    for ring_i in range(5):
        z_phase = ((prog * 2.4 + ring_i * 0.20) % 1.0)
        rad = int(90 + (z_phase ** 2.1) * 820)
        a_ring = math.sin(z_phase * math.pi) * 0.38
        cv2.circle(overlay, (W // 2, int(H * 0.44)), rad, (255 * a_ring, 185 * a_ring, 65 * a_ring), 2, cv2.LINE_AA)

    glow_ov = cv2.GaussianBlur(overlay, (17, 17), 5.0)
    frame = np.clip(frame.astype(np.float32) + overlay + glow_ov * 0.75, 0, 255).astype(np.uint8)

    # Map pin callouts (keep digits & Arabic era as separate tokens so digits stay LTR!)
    pin_rashid = hb_bold.render_static_line([("رشيد", (255, 255, 255)), ("•", (255, 195, 110)), ("1799", (255, 165, 125)), ("م", (255, 165, 125))], 34, stroke_px=4)
    paste_rgba(frame, pin_rashid, clamp_int(p_rashid[0] - pin_rashid.shape[1] // 2, 20, W - 360), clamp_int(p_rashid[1] - 78, 120, H - 400), 0.94)
    if prog > 0.35:
        a_sq = clamp((prog - 0.35) / 0.18)
        pin_sq = hb_bold.render_static_line([("سقارة", (255, 220, 95)), ("•", (130, 235, 245)), ("1250", (130, 235, 245)), ("ق.م", (130, 235, 245))], 38, stroke_px=4, glow=True)
        paste_rgba(frame, pin_sq, clamp_int(p_saqqara[0] - pin_sq.shape[1] // 2, 20, W - 390), clamp_int(p_saqqara[1] + 18, 180, H - 500), a_sq)

    # 4. Giant Spinning Chrono-Rewind Year Counter (1799 م -> 1250 ق.م)
    # Rewinds rapidly across 3,049 years!
    yr_prog = smoothstep(clamp((prog - 0.06) / 0.76))
    cur_year = int(round(1799 - (1799 + 1250) * yr_prog))
    if cur_year >= 0:
        yr_str = f"{cur_year}"
        era_str = "م"
        yr_col = (255, 195, 110)
    else:
        yr_str = f"{abs(cur_year)}"
        era_str = "ق.م"
        yr_col = (255, 222, 88)

    # Translucent dark glass HUD plate behind counter
    hud_w, hud_h = 660, 210
    hx, hy = (W - hud_w) // 2, int(H * 0.43)
    sub = frame[hy : hy + hud_h, hx : hx + hud_w].astype(np.float32)
    frame[hy : hy + hud_h, hx : hx + hud_w] = np.clip(sub * 0.32 + np.array([14, 11, 20], dtype=np.float32) * 0.68, 0, 255).astype(np.uint8)
    cv2.rectangle(frame, (hx, hy), (hx + hud_w, hy + hud_h), (245, 192, 78), 2, cv2.LINE_AA)

    lbl = hb_bold.render_static_line([("عودة", (200, 225, 235)), ("بالزمن", (200, 225, 235)), ("3,000", (255, 212, 90)), ("عام", (200, 225, 235))], 32, stroke_px=3)
    paste_rgba(frame, lbl, (W - lbl.shape[1]) // 2, hy + 10, 0.95)

    yr_img = hb_black.render_static_line([(yr_str, yr_col), (era_str, (255, 255, 255))], 86, stroke_px=6, glow=True)
    paste_rgba(frame, yr_img, (W - yr_img.shape[1]) // 2, hy + 52, 1.0)
    return frame


def clamp_int(v: int, lo: int, hi: int) -> int:
    return max(lo, min(hi, v))


def apply_sand_wipe_and_chisel(
    frame: np.ndarray,
    turb: np.ndarray,
    t: float,
    t0: float,
    chisel_strikes: list[float],
) -> np.ndarray:
    """Shot 6: Physical desert sand blowing off the limestone stele + bronze chisel sparks!"""
    out = frame.astype(np.float32)
    dt = t - t0

    # 1. Sand-erosion wipe across the right limestone tablet surface (first 1.45s of shot)
    wipe_p = clamp(dt / 1.45)
    if wipe_p < 0.99:
        yy = np.linspace(0.0, 1.0, H // 2, dtype=np.float32)[:, None]
        xx = np.linspace(0.0, 1.0, W // 2, dtype=np.float32)[None, :]
        # Tablet region on screen: x > 0.38, y in 0.26..0.92
        tablet_mask = np.clip((xx - 0.36) / 0.10, 0, 1) * np.clip((yy - 0.26) / 0.08, 0, 1)
        tx = ((xx * 240.0 + dt * 180.0) % 512).astype(np.float32)
        ty = ((yy * 240.0) % 512).astype(np.float32)
        tx_g, ty_g = np.broadcast_arrays(tx, ty)
        n_tex = cv2.remap(turb, tx_g, ty_g, cv2.INTER_LINEAR, borderMode=cv2.BORDER_WRAP)
        # Erosion front sweeps left-to-right across the carved cartouche!
        front = 0.34 + wipe_p * 0.82
        sand_cov = np.clip((xx + (n_tex - 0.5) * 0.22 - front) / 0.11, 0.0, 1.0) * tablet_mask
        # Blowing edge spray right at the erosion boundary
        edge_spray = np.exp(-(((xx + (n_tex - 0.5) * 0.16 - front) / 0.055) ** 2)) * tablet_mask * (1.0 - wipe_p * 0.4)

        cov_full = cv2.resize(sand_cov, (W, H), interpolation=cv2.INTER_LINEAR)[..., None]
        spray_full = cv2.resize(edge_spray, (W, H), interpolation=cv2.INTER_LINEAR)[..., None]
        fine_grain = (turb[: (H // 4), : (W // 4)] - 0.5) * 32.0
        fine_grain = cv2.resize(fine_grain, (W, H), interpolation=cv2.INTER_LINEAR)[..., None]
        sand_rgb = np.array([218.0, 172.0, 102.0], dtype=np.float32)[None, None, :] + fine_grain

        out = out * (1.0 - cov_full * 0.88) + sand_rgb * (cov_full * 0.88)
        out = np.clip(out + spray_full * np.array([250.0, 210.0, 140.0])[None, None, :] * 0.45, 0, 255)

    # 2. Chisel strike sparks & golden royal cartouche glow at (0.65, 0.49)
    cx, cy = int(0.655 * W), int(0.495 * H)
    # Subtle golden glow building inside the royal cartouche as it's carved!
    cartouche_glow = clamp((dt - 0.35) / 1.6)
    if cartouche_glow > 0.01:
        yy_f, xx_f = np.ogrid[0:H:4, 0:W:4]
        cg = np.exp(-(((xx_f - cx) / 95.0) ** 2 + ((yy_f - (cy + 55)) / 185.0) ** 2)).astype(np.float32)
        cg_full = cv2.resize(cg, (W, H), interpolation=cv2.INTER_LINEAR)[..., None]
        lum = (out[..., 0:1] * 0.3 + out[..., 1:2] * 0.59 + out[..., 2:3] * 0.11) / 255.0
        carved_grooves = np.clip((0.52 - lum) * 3.0, 0.0, 1.0)
        out += cg_full * carved_grooves * cartouche_glow * np.array([255.0, 200.0, 75.0])[None, None, :] * 0.85

    spark_layer = np.zeros((H, W, 3), dtype=np.uint8)
    for st_idx, st in enumerate(chisel_strikes):
        age = t - st
        if 0.0 <= age <= 0.42:
            p_s = age / 0.42
            flash = (1.0 - p_s) ** 2.2
            cv2.circle(
                spark_layer,
                (cx, cy),
                max(2, int(16 * (1.0 - p_s * 0.55))),
                (int(255 * flash), int(235 * flash), int(165 * flash)),
                -1,
                cv2.LINE_AA,
            )
            for sp_i in range(20):
                ang = sp_i * 0.314 + st_idx * 1.1
                spd = 155.0 + (sp_i % 5) * 72.0
                sx = int(cx + math.cos(ang) * spd * age)
                sy = int(cy + math.sin(ang) * spd * age + 290.0 * (age ** 2))
                sx_prev = int(cx + math.cos(ang) * spd * max(0.0, age - 0.032))
                sy_prev = int(cy + math.sin(ang) * spd * max(0.0, age - 0.032) + 290.0 * (max(0.0, age - 0.032) ** 2))
                if 0 <= sx < W and 0 <= sy < H:
                    br = clamp(1.0 - p_s * 0.85)
                    col_sp = (int(255 * br), int(218 * br), int(115 * br))
                    cv2.line(spark_layer, (sx_prev, sy_prev), (sx, sy), col_sp, 3, cv2.LINE_AA)
    if np.any(spark_layer):
        spark_glow = cv2.GaussianBlur(spark_layer, (15, 15), 4.0).astype(np.float32) * 1.35
        out = out + spark_layer.astype(np.float32) + spark_glow

    return np.clip(out, 0, 255).astype(np.uint8)


def apply_colossi_timelapse(frame: np.ndarray, t: float, t0: float, t1: float) -> np.ndarray:
    """Shot 7: Celestial starfield rotation + sweeping golden sunbeams across Colossi."""
    prog = clamp((t - t0) / max(0.01, t1 - t0))
    out = frame.astype(np.float32)

    # Detect dark twilight sky pixels at top of frame
    lum = (0.299 * out[..., 0] + 0.587 * out[..., 1] + 0.114 * out[..., 2]) / 255.0
    yy = np.linspace(0.0, 1.0, H, dtype=np.float32)[:, None]
    sky_mask = np.clip((0.42 - yy) / 0.12, 0, 1) * np.clip((0.36 - lum) * 4.5, 0, 1)

    # Wheeling star trails arcing across the ancient Egyptian night sky!
    stars = np.zeros_like(out)
    rot_ang = prog * 0.42
    piv_x, piv_y = int(W * 0.5), int(H * 0.55)
    for i in range(65):
        r_orb = 180 + (i * 47) % 680
        base_a = -math.pi * 0.85 + (i * 0.6180339) % (math.pi * 0.72)
        a1 = base_a + rot_ang
        a0 = a1 - 0.045
        p0 = (int(piv_x + math.cos(a0) * r_orb), int(piv_y + math.sin(a0) * r_orb))
        p1 = (int(piv_x + math.cos(a1) * r_orb), int(piv_y + math.sin(a1) * r_orb))
        if 0 <= p1[0] < W and 0 <= p1[1] < int(H * 0.45):
            br = 150 + (i % 4) * 35
            cv2.line(stars, p0, p1, (br, br + 10, 255), 2, cv2.LINE_AA)
    out += stars * sky_mask[..., None] * 0.85

    # Sweeping golden sunbeam angle across the colossal statue
    sx_light = 0.38 + 0.24 * prog
    sy_light = 0.66 - 0.26 * prog
    return add_god_rays(np.clip(out, 0, 255).astype(np.uint8), (sx_light, sy_light), 0.34, (255, 195, 95))


def apply_rosetta_scanner(
    frame: np.ndarray,
    hb_bold: HarfBuzzArabic,
    t: float,
    t0: float,
    t1: float,
) -> np.ndarray:
    """Shot 8: Molten-gold laser decipherment scanner + 3 script HUD badges + 3D floating glyphs!"""
    prog = clamp((t - t0) / max(0.01, t1 - t0))
    out = frame.astype(np.float32)

    # Laser beam sweeps vertically down the Rosetta Stone (y = 0.31 -> 0.74)
    scan_y_norm = 0.30 + 0.46 * smoothstep(clamp(prog / 0.85))
    scan_y = int(scan_y_norm * H)
    stone_x0, stone_x1 = int(0.24 * W), int(0.76 * W)

    # Illuminated decoded hieroglyphic gold highlight above scan_y!
    yy = np.arange(H, dtype=np.float32)[:, None]
    xx = np.arange(W, dtype=np.float32)[None, :]
    in_stone = np.exp(-(((xx - W * 0.50) / (W * 0.25)) ** 4)) * ((yy > H * 0.31) & (yy < scan_y)).astype(np.float32)
    lum = (out[..., 0] * 0.3 + out[..., 1] * 0.59 + out[..., 2] * 0.11) / 255.0
    engraved = np.clip((lum - 0.22) * (0.72 - lum) * 7.5, 0.0, 1.0)
    gold_decode = in_stone * engraved
    out += gold_decode[..., None] * np.array([255.0, 205.0, 75.0])[None, None, :] * 0.65

    # Glowing molten-gold horizontal laser line
    beam_profile = np.exp(-(((yy - scan_y) / 14.0) ** 2)) * np.exp(-(((xx - W * 0.50) / (W * 0.29)) ** 4))
    core_profile = np.exp(-(((yy - scan_y) / 3.0) ** 2)) * np.exp(-(((xx - W * 0.50) / (W * 0.27)) ** 4))
    out += beam_profile[..., None] * np.array([255.0, 185.0, 55.0])[None, None, :] * 0.85
    out += core_profile[..., None] * np.array([255.0, 250.0, 220.0])[None, None, :] * 0.90

    # Floating 3D golden geometric cartouche/ankh symbols lifting toward the viewer!
    glyph_ov = np.zeros_like(out)
    for gi in range(9):
        g_prog = ((prog * 1.45 + gi * 0.11) % 1.0)
        gx = int(W * (0.30 + 0.40 * ((gi * 37) % 100) / 100.0) + (gi % 2 * 2 - 1) * g_prog * 140)
        gy = int(H * (0.62 - g_prog * 0.36))
        sz = int(14 + (g_prog ** 1.7) * 34)
        ga = math.sin(g_prog * math.pi) * 0.78
        col_g = (255 * ga, 208 * ga, 82 * ga)
        if gi % 3 == 0:  # Ankh symbol
            cv2.ellipse(glyph_ov, (gx, gy - sz // 2), (sz // 3, sz // 2), 0, 0, 360, col_g, 3, cv2.LINE_AA)
            cv2.line(glyph_ov, (gx, gy), (gx, gy + sz), col_g, 3, cv2.LINE_AA)
            cv2.line(glyph_ov, (gx - sz // 2, gy + sz // 4), (gx + sz // 2, gy + sz // 4), col_g, 3, cv2.LINE_AA)
        elif gi % 3 == 1:  # Royal Cartouche oval
            cv2.ellipse(glyph_ov, (gx, gy), (sz // 2, sz), 0, 0, 360, col_g, 3, cv2.LINE_AA)
            cv2.line(glyph_ov, (gx - sz // 2, gy + sz), (gx + sz // 2, gy + sz), col_g, 4, cv2.LINE_AA)
        else:  # Eye / Sun-disc of Ra
            cv2.circle(glyph_ov, (gx, gy), sz // 2, col_g, 3, cv2.LINE_AA)
            cv2.circle(glyph_ov, (gx, gy), max(2, sz // 6), col_g, -1, cv2.LINE_AA)

    out += glyph_ov + cv2.GaussianBlur(glyph_ov, (15, 15), 4.5) * 0.75
    frame_u8 = np.clip(out, 0, 255).astype(np.uint8)

    # 3 Script Band Callout Pills lighting up sequentially as laser passes!
    bands = [
        (0.08, int(H * 0.36), "الهيروغليفية", (255, 215, 85)),
        (0.34, int(H * 0.50), "الديموطيقية", (140, 232, 245)),
        (0.58, int(H * 0.64), "اليونانية", (180, 245, 175)),
    ]
    for trig, by, label_txt, b_col in bands:
        if prog >= trig:
            ba = clamp((prog - trig) / 0.14)
            badge = hb_bold.render_static_line([("•", b_col), (label_txt, b_col)], 34, stroke_px=4, glow=True)
            paste_rgba(frame_u8, badge, W - badge.shape[1] - 34, by - badge.shape[0] // 2, ba)

    return frame_u8


def add_soaring_falcons(frame: np.ndarray, t: float, t0: float) -> np.ndarray:
    """Shot 9 Finale: Graceful animated wing-flapping falcons soaring across Giza golden sky!"""
    dt = t - t0
    out = frame.copy()
    birds = [
        (0.18, 0.24, 1.0, 0.0),
        (0.12, 0.27, 0.78, 1.4),
        (0.24, 0.22, 0.68, 2.7),
        (0.08, 0.29, 0.60, 4.1),
        (0.29, 0.26, 0.52, 5.2),
    ]
    for x_start, y_start, sc, ph in birds:
        bx = int((x_start + dt * 0.085 * sc) * W)
        by = int((y_start - dt * 0.014 * sc + 0.006 * math.sin(dt * 2.5 + ph)) * H)
        flap = math.sin(dt * 8.2 + ph)
        span = int(22 * sc)
        tip_y = int(by - flap * 11 * sc)
        mid_y = int(by - abs(flap) * 4 * sc)
        pts_l = np.array([[bx, by], [bx - span // 2, mid_y], [bx - span, tip_y]], dtype=np.int32)
        pts_r = np.array([[bx, by], [bx + span // 2, mid_y], [bx + span, tip_y]], dtype=np.int32)
        cv2.polylines(out, [pts_l, pts_r], False, (42, 28, 14), max(2, int(2.6 * sc)), cv2.LINE_AA)
    return out


# ==============================================================================
# 5. CINEMA TRANSITIONS (WHIP-PAN MOTION BLUR & ANAMORPHIC GOLDEN FLARES)
# ==============================================================================
def whip_transition(frame_a: np.ndarray, frame_b: np.ndarray, p: float, mode: str = "whip_left") -> np.ndarray:
    """Dynamic 0.22s cinema cut with directional motion blur + golden light flash."""
    ep = smoothstep(p)
    strength = math.sin(clamp(p) * math.pi)  # Peaks at 0.5 right on the cut!

    if mode in ("whip_left", "whip_right"):
        sign = -1 if mode == "whip_left" else 1
        shift_a = int(sign * ep * W * 0.85)
        shift_b = int(sign * (ep - 1.0) * W * 0.85)
        comp = np.zeros_like(frame_a)
        for src, sh in [(frame_a, shift_a), (frame_b, shift_b)]:
            M = np.float32([[1, 0, sh], [0, 1, 0]])
            comp = np.maximum(comp, cv2.warpAffine(src, M, (W, H), borderMode=cv2.BORDER_REFLECT_101))
        # Real horizontal motion blur proportional to whip velocity!
        k_sz = max(3, int(strength * 45) | 1)
        kernel = np.zeros((1, k_sz), dtype=np.float32)
        kernel[0, :] = 1.0 / k_sz
        comp = cv2.filter2D(comp, -1, kernel)
    elif mode == "whip_down":
        shift_a = int(ep * H * 0.65)
        shift_b = int((ep - 1.0) * H * 0.65)
        comp = np.zeros_like(frame_a)
        for src, sh in [(frame_a, shift_a), (frame_b, shift_b)]:
            M = np.float32([[1, 0, 0], [0, 1, sh]])
            comp = np.maximum(comp, cv2.warpAffine(src, M, (W, H), borderMode=cv2.BORDER_REFLECT_101))
        k_sz = max(3, int(strength * 41) | 1)
        kernel = np.zeros((k_sz, 1), dtype=np.float32)
        kernel[:, 0] = 1.0 / k_sz
        comp = cv2.filter2D(comp, -1, kernel)
    else:  # "zoom_flash" / sunburst punch
        z_a = 1.0 + 0.35 * ep
        z_b = 1.28 - 0.28 * ep
        ca_img = cv2.resize(frame_a, (int(W * z_a), int(H * z_a)), interpolation=cv2.INTER_LINEAR)
        cb_img = cv2.resize(frame_b, (int(W * z_b), int(H * z_b)), interpolation=cv2.INTER_LINEAR)
        xa, ya = (ca_img.shape[1] - W) // 2, (ca_img.shape[0] - H) // 2
        xb, yb = (cb_img.shape[1] - W) // 2, (cb_img.shape[0] - H) // 2
        comp = cv2.addWeighted(ca_img[ya : ya + H, xa : xa + W], 1.0 - ep, cb_img[yb : yb + H, xb : xb + W], ep, 0)

    # Anamorphic horizontal golden flare streak at transition peak
    flare_a = (strength ** 1.8) * 0.58
    yy = np.linspace(0.0, 1.0, H, dtype=np.float32)[:, None]
    streak = np.exp(-(((yy - 0.50) / 0.14) ** 2)) * flare_a
    out = comp.astype(np.float32) + streak[..., None] * np.array([255.0, 205.0, 98.0])[None, None, :]
    return np.clip(out, 0, 255).astype(np.uint8)


# ==============================================================================
# 6. WORD-BY-WORD KINETIC ARABIC SUBTITLES & CINEMATIC UI OVERLAYS
# ==============================================================================
def draw_kinetic_caption_group(
    frame: np.ndarray,
    hb_black: HarfBuzzArabic,
    t: float,
    start_t: float,
    end_t: float,
    lines_words: list[list[tuple[str, bool]]],
    center_y: int = 1495,
    font_size: int = 58,
) -> None:
    """Render word-by-word animated RTL Arabic captions with active word gold pop!
    lines_words: list of lines, each line is [(word_str, is_accent_keyword), ...] in reading order.
    """
    if t < start_t - 0.05 or t > end_t + 0.18:
        return
    group_fade = clamp((end_t + 0.18 - t) / 0.22) if t > end_t - 0.04 else 1.0

    # Flatten words to assign sequential activation timestamps across [start_t, end_t - 0.28]
    total_words = sum(len(ln) for ln in lines_words)
    span = max(0.35, (end_t - start_t) * 0.84)
    word_step = span / max(1, total_words)

    # Soft dark gradient pill behind lower caption area for 100% legibility on bright desert sand
    pill_h = len(lines_words) * int(font_size * 1.48) + 64
    pill_w = 980
    px, py = (W - pill_w) // 2, center_y - pill_h // 2
    py0, py1 = max(0, py), min(H, py + pill_h)
    sub = frame[py0:py1, px : px + pill_w].astype(np.float32)
    scrim_a = 0.54 * group_fade * clamp((t - start_t + 0.05) / 0.15)
    frame[py0:py1, px : px + pill_w] = np.clip(sub * (1.0 - scrim_a) + np.array([8.0, 7.0, 13.0]) * scrim_a, 0, 255).astype(np.uint8)
    # Subtle golden right-edge accent bar (RTL reading anchor!)
    bar_h = int((py1 - py0 - 28) * clamp((t - start_t) / 0.25))
    if bar_h > 4:
        cv2.rectangle(frame, (px + pill_w - 10, py0 + 14), (px + pill_w - 4, py0 + 14 + bar_h), (255, 204, 82), -1)

    global_w_idx = 0
    space_px = int(font_size * 0.28)
    line_step_y = int(font_size * 1.44)
    start_line_y = center_y - int((len(lines_words) - 1) * line_step_y * 0.5)

    for ln_i, ln in enumerate(lines_words):
        # Measure line width first so words center cleanly right-to-left
        word_specs = []
        for w_str, is_kw in ln:
            w_trig = start_t + global_w_idx * word_step
            is_active = (w_trig <= t < w_trig + word_step * 1.35)
            spoken = t >= w_trig
            if is_active:
                col = (255, 218, 75)
                glow = True
            elif is_kw and spoken:
                col = (255, 206, 92)
                glow = False
            elif spoken:
                col = (252, 252, 250)
                glow = False
            else:
                col = (180, 184, 194)
                glow = False
            rgba, adv_w, pad = hb_black.render_word(w_str, font_size, col, stroke_px=5, glow=glow)
            word_specs.append((rgba, adv_w, pad, w_trig, is_active, spoken))
            global_w_idx += 1

        total_line_w = sum(s[1] for s in word_specs) + space_px * max(0, len(word_specs) - 1)
        cur_right = (W + total_line_w) // 2
        ly_center = start_line_y + ln_i * line_step_y

        for rgba, adv_w, pad, w_trig, is_active, spoken in word_specs:
            age = (t - w_trig) / 0.19
            if age < -0.35:
                w_op = 0.42 * group_fade
                sc = 0.95
                y_pop = 6
            elif age < 1.0:
                p_pop = clamp((age + 0.35) / 1.35)
                w_op = (0.25 + 0.75 * p_pop) * group_fade
                sc = 0.86 + 0.14 * ease_out_back(p_pop) + (0.07 if is_active else 0.0)
                y_pop = int((1.0 - p_pop) * 14)
            else:
                w_op = 1.0 * group_fade
                sc = 1.06 if is_active else 1.0
                y_pop = -3 if is_active else 0

            use_rgba = rgba
            if abs(sc - 1.0) > 0.015:
                nw = max(4, int(rgba.shape[1] * sc))
                nh = max(4, int(rgba.shape[0] * sc))
                use_rgba = cv2.resize(rgba, (nw, nh), interpolation=cv2.INTER_LINEAR)

            wx = cur_right - adv_w - pad - (use_rgba.shape[1] - rgba.shape[1]) // 2
            wy = ly_center - use_rgba.shape[0] // 2 + y_pop
            paste_rgba(frame, use_rgba, wx, wy, w_op)
            cur_right -= adv_w + space_px


def draw_top_chrome(frame: np.ndarray, hb_bold: HarfBuzzArabic, badge_cache: np.ndarray, t: float) -> None:
    """Sleek top documentary header + animated 30s story progress bar."""
    # Subtle top vignette for header clarity
    top_h = 155
    yy = np.linspace(1.0, 0.0, top_h, dtype=np.float32)[:, None, None] ** 1.5
    frame[:top_h] = np.clip(frame[:top_h].astype(np.float32) * (1.0 - 0.48 * yy), 0, 255).astype(np.uint8)

    # Progress bar at y=44
    bx0, bx1, by = 54, W - 54, 44
    cv2.rectangle(frame, (bx0, by), (bx1, by + 5), (90, 85, 80), -1)
    prog_x = bx0 + int((bx1 - bx0) * clamp(t / DURATION))
    cv2.rectangle(frame, (bx0, by), (prog_x, by + 5), (255, 208, 84), -1)

    paste_rgba(frame, badge_cache, W - badge_cache.shape[1] - 46, 62, 0.92)


# ==============================================================================
# 7. MASTER COMPOSITOR & MP4 ENCODER
# ==============================================================================
class EgyptCinemaEngine:
    def __init__(self):
        self.meta = json.loads((ASSETS / "timeline_meta.json").read_text())
        self.hb_black = HarfBuzzArabic(FONTS / "Cairo-Full-900.ttf")
        self.hb_bold = HarfBuzzArabic(FONTS / "Cairo-Full-700.ttf")
        self.rig = ParallaxRig()
        self.turb = make_tileable_turbulence(512, 512, seed=2026)

        print("Loading & building 3D depth maps for 9 IMAX vertical plates...", flush=True)
        self.plates = {
            "s1": load_plate("scene1_rosetta_hook.png"),
            "s2": load_plate("scene2b_nile_ancient_map.png"),
            "s3": load_plate("scene3_saqqara_buried.png"),
            "s4": load_plate("scene4_khaemwaset.png"),
            "s5": load_plate("scene5_excavation.png"),
            "s6": load_plate("scene6_stele_carving.png"),
            "s7": load_plate("scene7_colossi_timelapse.png"),
            "s8": load_plate("scene8_rosetta_glow.png"),
            "s9": load_plate("scene9_giza_finale.png"),
        }
        self.depths = {
            "s1": build_depth_map(self.plates["s1"], "rosetta_hook"),
            "s3": build_depth_map(self.plates["s3"], "saqqara_buried"),
            "s4": build_depth_map(self.plates["s4"], "prince"),
            "s5": build_depth_map(self.plates["s5"], "excavation"),
            "s6": build_depth_map(self.plates["s6"], "stele_carving"),
            "s7": build_depth_map(self.plates["s7"], "colossi"),
            "s8": build_depth_map(self.plates["s8"], "rosetta_glow"),
            "s9": build_depth_map(self.plates["s9"], "giza_finale"),
        }

        self.badge_top = self.hb_bold.render_static_line(
            [("أسرار", (255, 212, 88)), ("مصر", (255, 212, 88)), ("•", (160, 195, 210)), ("وثائقي", (235, 240, 245))],
            28,
            stroke_px=3,
        )
        # Pre-render Hero ID card & Finale titles
        self.prince_tag1 = self.hb_black.render_static_line(
            [("الأمير", (255, 255, 255)), ("خعمواست", (255, 215, 82))], 42, stroke_px=5, glow=True
        )
        self.prince_tag2 = self.hb_bold.render_static_line(
            [("ابن", (215, 232, 242)), ("رمسيس", (255, 210, 95)), ("الثاني", (255, 210, 95)), ("•", (140, 220, 235)), ("1250", (140, 220, 235)), ("ق.م", (140, 220, 235))],
            30,
            stroke_px=4,
        )
        self.finale_l1 = self.hb_bold.render_static_line(
            [("مصر", (255, 220, 95)), ("لم", (255, 255, 255)), ("تصنع", (255, 255, 255)), ("التاريخ", (255, 255, 255)), ("فقط...", (255, 220, 95))],
            50,
            stroke_px=5,
        )
        self.finale_l2 = self.hb_black.render_static_line(
            [("مصر", (255, 218, 75)), ("أول", (255, 255, 255)), ("من", (255, 255, 255)), ("اكتشفه!", (255, 218, 75))],
            72,
            stroke_px=6,
            glow=True,
        )

        # Build exact shot table from timeline_meta.json
        m = self.meta
        self.shots = [
            ("shot1", m["shot1_start"], m["shot2_twist"], "zoom_flash"),
            ("shot2", m["shot2_twist"], m["shot3_saqqara"], "whip_left"),
            ("shot3", m["shot3_saqqara"], m["shot4a_prince"], "zoom_flash"),
            ("shot4a", m["shot4a_prince"], m["shot4b_torch"], "zoom_flash"),
            ("shot4b", m["shot4b_torch"], m["shot5_excav"], "whip_down"),
            ("shot5", m["shot5_excav"], m["shot6_stele"], "zoom_flash"),
            ("shot6", m["shot6_stele"], m["shot7_colossi"], "whip_right"),
            ("shot7", m["shot7_colossi"], m["shot8_rosetta"], "zoom_flash"),
            ("shot8", m["shot8_rosetta"], m["shot9_finale"], "zoom_flash"),
            ("shot9", m["shot9_finale"], DURATION + 0.1, "zoom_flash"),
        ]

        # Word-by-word synced Arabic caption groups
        vo = m["vo_segments"]
        self.captions = [
            # Hook part 1 (rendered large at top/upper-center in Shot 1, plus lower-third for shots 2..8)
            {
                "start": vo[0]["split"],
                "end": vo[0]["end"] + 0.10,
                "y": 1525,
                "size": 62,
                "lines": [[("الحقيقة", False), ("أقدم", True), ("بـ", True), ("3,000", True), ("عام!", True)]],
            },
            {
                "start": vo[1]["start"],
                "end": vo[1]["split"] - 0.04,
                "y": 1520,
                "size": 56,
                "lines": [[("قبل", False), ("نابليون", True), ("بقرون", False), ("طويلة", True)]],
            },
            {
                "start": vo[1]["split"],
                "end": vo[1]["end"] + 0.08,
                "y": 1500,
                "size": 52,
                "lines": [
                    [("قاد", False), ("الأمير", False), ("المصري", True), ("خعمواست", True)],
                    [("أول", True), ("تنقيب", True), ("أثري", True), ("في", False), ("التاريخ!", False)],
                ],
            },
            {
                "start": vo[2]["start"],
                "end": vo[2]["split"] - 0.04,
                "y": 1505,
                "size": 54,
                "lines": [
                    [("أزال", False), ("رمال", True), ("سقارة", True)],
                    [("عن", False), ("الأهرامات", True), ("المنسية", False)],
                ],
            },
            {
                "start": vo[2]["split"],
                "end": vo[2]["end"] + 0.08,
                "y": 1505,
                "size": 52,
                "lines": [
                    [("ونقش", True), ("عليها", False), ("أسماء", False)],
                    [("ملوكها", True), ("الأصليين!", True)],
                ],
            },
            {
                "start": vo[3]["start"],
                "end": vo[3]["split"] - 0.04,
                "y": 1505,
                "size": 54,
                "lines": [
                    [("وحين", False), ("فكّ", False), ("حجر", True), ("رشيد", True)],
                    [("شفرة", False), ("الهيروغليفية...", True)],
                ],
            },
            {
                "start": vo[3]["split"],
                "end": vo[3]["end"] + 0.08,
                "y": 1505,
                "size": 52,
                "lines": [
                    [("نطقت", True), ("الحجارة", True), ("لتشهد", False)],
                    [("على", False), ("إنجازه", True), ("العظيم!", True)],
                ],
            },
        ]

    def camera_shake(self, t: float) -> tuple[float, float, float]:
        """Organic 35mm cinema handheld drift + impact shockwave shake on bass hits."""
        hx = 0.0045 * math.sin(t * 2.1) + 0.0022 * math.cos(t * 4.7)
        hy = 0.0040 * math.cos(t * 1.8) + 0.0020 * math.sin(t * 5.3)
        hroll = 0.38 * math.sin(t * 1.45)

        m = self.meta
        impacts = [0.02, m["shot2_twist"], m["shot4a_prince"], m["shot8_rosetta"], m["shot9_finale"], m["shot9_punch"]] + m.get("chisel_strikes", [])
        for imp in impacts:
            dt = t - imp
            if 0.0 <= dt <= 0.36:
                amp = math.exp(-dt * 14.0) * (0.016 if imp < 1.0 or imp > 25.0 else 0.011)
                hx += amp * math.sin(dt * 92.0)
                hy += amp * math.cos(dt * 118.0)
                hroll += amp * 75.0 * math.sin(dt * 75.0)
        return hx, hy, hroll

    def render_single_shot(self, shot_name: str, t: float, t0: float, t1: float) -> np.ndarray:
        u = clamp((t - t0) / max(0.01, t1 - t0))
        eu = smoothstep(u)
        hx, hy, hroll = self.camera_shake(t)

        if shot_name == "shot1":
            # Explosive frame-0 impact zoom-out then intense 3D parallax push onto Rosetta Stone
            punch = math.exp(-t * 9.0) * 0.16
            z = 1.06 + 0.24 * eu + punch
            px = -0.055 + 0.110 * eu
            py = 0.030 - 0.060 * eu
            frame = self.rig.render(self.plates["s1"], self.depths["s1"], z, hx - 0.015 * eu, hy - 0.02 * eu, px, py, hroll - 1.2 + 2.4 * eu)
            frame = add_god_rays(frame, (0.44, 0.18), 0.32 + 0.18 * math.sin(t * 4.0), (255, 205, 110))
            # Mysterious pulsing golden energy in carved hieroglyphs + rising embers
            frame = add_live_torch_fire(frame, self.turb, (0.48, 0.72), 18, t, illuminate_strength=0.14)
            return frame

        if shot_name == "shot2":
            return render_shot2_3d_map(self.plates["s2"], self.hb_black, self.hb_bold, t, t0, t1)

        if shot_name == "shot3":
            # Independent rolling storm clouds + fast lateral 3D drone sweep over buried Saqqara!
            plate = self.plates["s3"].copy()
            cloud_shift = int((u * 85))
            sky_h = int(OV_H * 0.37)
            plate[:sky_h] = np.roll(plate[:sky_h], cloud_shift, axis=1)
            z = 1.08 + 0.18 * eu
            px = 0.075 - 0.150 * eu
            py = 0.020 - 0.045 * eu
            frame = self.rig.render(plate, self.depths["s3"], z, hx + 0.03 - 0.06 * eu, hy, px, py, hroll + 1.0 * (1 - 2 * eu))
            frame = add_sandstorm_layers(frame, self.turb, t, strength=1.08)
            frame = add_god_rays(frame, (0.70, 0.20), 0.26, (255, 210, 125))
            return frame

        if shot_name == "shot4a":
            # Heroic wide 3D parallax reveal of Prince Khaemwaset + live torch flame!
            z = 1.04 + 0.15 * eu
            px = -0.065 + 0.130 * eu  # Foreground left column sweeps dramatically!
            py = 0.018 - 0.036 * eu
            frame = self.rig.render(self.plates["s4"], self.depths["s4"], z, hx + 0.02 * eu, hy - 0.015 * eu, px, py, hroll)
            # Torch position tracks smoothly with zoom/pan around (0.28, 0.33)
            tx_pos = 0.285 - 0.022 * eu
            ty_pos = 0.330 - 0.015 * eu
            frame = add_live_torch_fire(frame, self.turb, (tx_pos, ty_pos), 46, t, illuminate_strength=0.34)
            # Slide-in documentary ID card at upper-right
            card_a = clamp((t - t0 - 0.18) / 0.25)
            paste_rgba(frame, self.prince_tag1, W - self.prince_tag1.shape[1] - 48, 150, card_a)
            paste_rgba(frame, self.prince_tag2, W - self.prince_tag2.shape[1] - 48, 222, card_a)
            return frame

        if shot_name == "shot4b":
            # Punch-in medium close-up on Prince Khaemwaset, papyrus scroll & blazing torch!
            z = 1.34 + 0.16 * eu
            px = 0.040 - 0.085 * eu
            py = -0.015 + 0.030 * eu
            frame = self.rig.render(self.plates["s4"], self.depths["s4"], z, hx + 0.035, hy - 0.055, px, py, hroll - 0.9)
            frame = add_live_torch_fire(frame, self.turb, (0.205, 0.305), 62, t, illuminate_strength=0.40)
            paste_rgba(frame, self.prince_tag1, W - self.prince_tag1.shape[1] - 48, 150, 0.95)
            paste_rgba(frame, self.prince_tag2, W - self.prince_tag2.shape[1] - 48, 222, 0.95)
            return frame

        if shot_name == "shot5":
            # Crane-down + forward push into Saqqara excavation trench, foreground ropes parallax!
            z = 1.07 + 0.20 * eu
            px = -0.050 + 0.105 * eu
            py = -0.055 + 0.110 * eu
            frame = self.rig.render(self.plates["s5"], self.depths["s5"], z, hx, hy - 0.03 + 0.05 * eu, px, py, hroll)
            frame = add_sandstorm_layers(frame, self.turb, t * 0.85, strength=0.68)
            frame = add_god_rays(frame, (0.18, 0.12), 0.30, (255, 215, 130))
            return frame

        if shot_name == "shot6":
            # Macro bronze chisel & limestone royal cartouche + live sand-wipe & strike sparks!
            z = 1.06 + 0.16 * eu
            px = 0.045 - 0.090 * eu
            py = 0.025 - 0.050 * eu
            frame = self.rig.render(self.plates["s6"], self.depths["s6"], z, hx + 0.02 * eu, hy, px, py, hroll)
            frame = add_live_torch_fire(frame, self.turb, (0.03, 0.27), 44, t, illuminate_strength=0.28)
            frame = apply_sand_wipe_and_chisel(frame, self.turb, t, t0, self.meta.get("chisel_strikes", []))
            return frame

        if shot_name == "shot7":
            # Low-angle push & crane-up past Colossi + wheeling stars & sweeping sunbeams!
            z = 1.06 + 0.18 * eu
            px = -0.055 + 0.115 * eu
            py = 0.050 - 0.100 * eu
            frame = self.rig.render(self.plates["s7"], self.depths["s7"], z, hx, hy + 0.03 - 0.06 * eu, px, py, hroll)
            frame = apply_colossi_timelapse(frame, t, t0, t1)
            return frame

        if shot_name == "shot8":
            # 3D orbital push onto Rosetta Stone + golden laser scanner & 3D floating glyphs!
            z = 1.05 + 0.20 * eu
            px = 0.050 - 0.100 * eu
            py = -0.025 + 0.050 * eu
            frame = self.rig.render(self.plates["s8"], self.depths["s8"], z, hx, hy, px, py, hroll + 1.2 * (0.5 - eu))
            frame = apply_rosetta_scanner(frame, self.hb_bold, t, t0, t1)
            return frame

        # shot9: Glorious Giza Pyramids aerial pull-back & crane rise + soaring falcons!
        plate = self.plates["s9"].copy()
        sky_h = int(OV_H * 0.36)
        plate[:sky_h] = np.roll(plate[:sky_h], int(u * 55), axis=1)
        z = 1.26 - 0.20 * eu  # Epic pull-back revealing full majesty of Giza & foreground ridge!
        px = -0.045 + 0.090 * eu
        py = 0.055 - 0.105 * eu
        frame = self.rig.render(plate, self.depths["s9"], z, hx, hy - 0.02 + 0.03 * eu, px, py, hroll * 0.6)
        frame = add_soaring_falcons(frame, t, t0)
        frame = add_god_rays(frame, (0.61, 0.25), 0.32 + 0.22 * eu, (255, 212, 115))
        return frame

    def draw_hook_typography(self, frame: np.ndarray, t: float) -> None:
        """Full-impact viral Hook title in Shot 1 (0.00s .. shot2_twist)."""
        end_h = self.meta["shot2_twist"]
        if t >= end_h:
            return
        fade = clamp((end_h - t) / 0.22)

        # Dark upper-middle cinematic scrim behind hook text
        yy = np.linspace(0.0, 1.0, 560, dtype=np.float32)[:, None, None]
        scrim = (np.clip(np.sin(yy * math.pi), 0.0, 1.0) ** 0.85) * 0.56 * fade
        frame[150:710] = np.clip(frame[150:710].astype(np.float32) * (1.0 - scrim), 0, 255).astype(np.uint8)

        draw_kinetic_caption_group(
            frame,
            self.hb_black,
            t,
            start_t=0.10,
            end_t=end_h - 0.05,
            lines_words=[
                [("تظن", False), ("أن", False), ("حجر", True), ("رشيد", True)],
                [("أول", True), ("اكتشاف", False), ("أثري؟", True)],
            ],
            center_y=415,
            font_size=68,
        )

    def draw_finale_lockup(self, frame: np.ndarray, t: float) -> None:
        """Regal finale typography synced to vo_06_finale ('مصر لم تصنع التاريخ فقط... مصر أول من اكتشفه!')."""
        vo5 = self.meta["vo_segments"][4]
        if t < vo5["start"] - 0.08:
            return

        # Translucent royal obsidian-gold lower centerpiece card
        a1 = clamp((t - vo5["start"] + 0.05) / 0.28)
        cw, ch = 980, 310
        cx, cy = (W - cw) // 2, 1365
        sub = frame[cy : cy + ch, cx : cx + cw].astype(np.float32)
        bg_a = 0.68 * a1
        frame[cy : cy + ch, cx : cx + cw] = np.clip(sub * (1.0 - bg_a) + np.array([10.0, 8.0, 14.0]) * bg_a, 0, 255).astype(np.uint8)
        cv2.rectangle(frame, (cx, cy), (cx + cw, cy + ch), (245, 196, 78), 3, cv2.LINE_AA)
        cv2.line(frame, (cx + 260, cy + 148), (cx + cw - 260, cy + 148), (245, 196, 78), 2, cv2.LINE_AA)

        sc1 = 0.88 + 0.12 * ease_out_back(a1)
        l1 = self.finale_l1 if abs(sc1 - 1.0) < 0.015 else cv2.resize(self.finale_l1, (int(self.finale_l1.shape[1] * sc1), int(self.finale_l1.shape[0] * sc1)))
        paste_rgba(frame, l1, (W - l1.shape[1]) // 2, cy + 18, a1)

        if t >= vo5["split"] - 0.05:
            a2 = clamp((t - vo5["split"] + 0.05) / 0.24)
            sc2 = 0.82 + 0.18 * ease_out_back(a2)
            l2 = self.finale_l2.copy()
            # Golden specular light-sweep gliding across the final line!
            sweep_x = int(((t - vo5["split"]) / 1.45) * (l2.shape[1] + 240)) - 120
            if 0 <= sweep_x < l2.shape[1] + 120:
                xx = np.arange(l2.shape[1], dtype=np.float32)[None, :]
                sh = np.exp(-(((xx - sweep_x) / 55.0) ** 2))[..., None]
                l2[..., :3] = np.clip(l2[..., :3].astype(np.float32) + sh * 115.0, 0, 255).astype(np.uint8)
            if abs(sc2 - 1.0) >= 0.015:
                l2 = cv2.resize(l2, (int(l2.shape[1] * sc2), int(l2.shape[0] * sc2)))
            paste_rgba(frame, l2, (W - l2.shape[1]) // 2, cy + 142, a2)

    def render_frame(self, idx: int) -> np.ndarray:
        t = idx / FPS

        # Determine active shot & check if inside a 0.22s whip/flash transition window
        trans_half = 0.11
        cur_idx = 0
        for i, (_, s0, s1, _) in enumerate(self.shots):
            if t >= s0:
                cur_idx = i

        name_c, s0_c, s1_c, tr_mode = self.shots[cur_idx]
        if cur_idx > 0 and (t - s0_c) < trans_half:
            # Second half of transition from previous shot -> current shot
            name_p, s0_p, s1_p, tr_prev = self.shots[cur_idx - 1]
            p_tr = 0.5 + 0.5 * ((t - s0_c) / trans_half)
            fa = self.render_single_shot(name_p, t, s0_p, s1_p)
            fb = self.render_single_shot(name_c, t, s0_c, s1_c)
            frame = whip_transition(fa, fb, p_tr, tr_prev)
        elif cur_idx + 1 < len(self.shots) and (s1_c - t) < trans_half:
            # First half of transition from current shot -> next shot
            name_n, s0_n, s1_n, _ = self.shots[cur_idx + 1]
            p_tr = 0.5 * (1.0 - (s1_c - t) / trans_half)
            fa = self.render_single_shot(name_c, t, s0_c, s1_c)
            fb = self.render_single_shot(name_n, t, s0_n, s1_n)
            frame = whip_transition(fa, fb, p_tr, tr_mode)
        else:
            frame = self.render_single_shot(name_c, t, s0_c, s1_c)

        # Subtle dynamic 35mm cinema film grain (prevents flat digital stillness!)
        gx = (idx * 37) % 180
        gy = (idx * 53) % 180
        grain = (self.turb[gy : gy + 240, gx : gx + 135] - 0.5) * 14.0
        grain_full = cv2.resize(grain, (W, H), interpolation=cv2.INTER_NEAREST)[..., None]
        frame = np.clip(frame.astype(np.float32) + grain_full, 0, 255).astype(np.uint8)

        # Overlays & Kinetic Arabic Captions
        draw_top_chrome(frame, self.hb_bold, self.badge_top, t)
        self.draw_hook_typography(frame, t)
        for cap in self.captions:
            draw_kinetic_caption_group(
                frame, self.hb_black, t, cap["start"], cap["end"], cap["lines"], cap["y"], cap["size"]
            )
        self.draw_finale_lockup(frame, t)

        # Clean 0.30s fade-in from gold flash at frame 0 & subtle 0.25s end polish
        if t < 0.16:
            fl = (1.0 - t / 0.16) ** 2 * 0.45
            frame = np.clip(frame.astype(np.float32) + fl * np.array([255.0, 210.0, 110.0]), 0, 255).astype(np.uint8)
        return frame


def encode_full_video() -> None:
    engine = EgyptCinemaEngine()
    ffmpeg = imageio_ffmpeg.get_ffmpeg_exe()
    audio_wav = ASSETS / "master_audio.wav"

    cmd = [
        ffmpeg, "-hide_banner", "-loglevel", "error", "-y",
        "-f", "rawvideo", "-pix_fmt", "rgb24", "-s", f"{W}x{H}", "-r", str(FPS), "-i", "pipe:0",
        "-i", str(audio_wav),
        "-map", "0:v:0", "-map", "1:a:0",
        "-t", f"{DURATION:.3f}",
        "-c:v", "libx264", "-preset", "medium", "-crf", "18", "-profile:v", "high",
        "-pix_fmt", "yuv420p", "-r", str(FPS), "-g", str(FPS * 2),
        "-c:a", "aac", "-b:a", "192k", "-ar", "48000", "-ac", "2",
        "-metadata", "title=أول اكتشاف أثري في مصر — سرّ الأمير خعمواست وحجر رشيد",
        "-metadata", "comment=1080x1920 30fps Vertical Arabic Documentary Short",
        "-movflags", "+faststart",
        str(OUT_MP4),
    ]
    print(f"Encoding {TOTAL_FRAMES} frames ({DURATION:.2f}s @ {FPS}fps) -> {OUT_MP4}", flush=True)
    proc = subprocess.Popen(cmd, stdin=subprocess.PIPE, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)
    try:
        assert proc.stdin is not None
        for idx in range(TOTAL_FRAMES):
            frame = engine.render_frame(idx)
            proc.stdin.write(frame.tobytes())
            if idx % 90 == 0 or idx == TOTAL_FRAMES - 1:
                print(f"  Rendered {idx + 1:3d}/{TOTAL_FRAMES} frames ({(idx + 1)/TOTAL_FRAMES*100:5.1f}%)", flush=True)
        proc.stdin.close()
    except BrokenPipeError:
        if proc.stdin:
            proc.stdin.close()
        err = proc.stderr.read().decode("utf-8", "replace") if proc.stderr else ""
        raise RuntimeError(f"FFmpeg broken pipe:\n{err}")

    stderr = proc.stderr.read().decode("utf-8", "replace") if proc.stderr else ""
    rc = proc.wait()
    if rc != 0:
        raise RuntimeError(f"FFmpeg failed ({rc}):\n{stderr}")
    print(f"SUCCESS: {OUT_MP4} ({OUT_MP4.stat().st_size / 1_000_000:.2f} MB)", flush=True)


def save_preview_frames() -> None:
    engine = EgyptCinemaEngine()
    sample_times = [0.8, 4.9, 9.8, 16.9, 23.6, 28.4]
    for st in sample_times:
        f = engine.render_frame(int(st * FPS))
        Image.fromarray(f).save(f"/tmp/preview_{st:.1f}s.jpg", quality=92)
        print(f"Saved /tmp/preview_{st:.1f}s.jpg")


if __name__ == "__main__":
    if "--preview" in sys.argv:
        save_preview_frames()
    else:
        encode_full_video()

# -*- coding: utf-8 -*-
"""Generate text overlays (Arabic), marine-snow particle layer and SFX for the reel."""
import os, math, glob
import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter
import arabic_reshaper
from bidi.algorithm import get_display

W, H = 1080, 1920
ASSETS = "/home/user/mjd/assets"
BUILD = "/home/user/mjd/build"
FONTS = "/home/user/.local/fonts"
os.makedirs(BUILD, exist_ok=True)

# Fonts with full Arabic Presentation-Forms coverage (PIL here has no raqm/HarfBuzz)
F_TITLE = os.path.join(FONTS, "NotoKufiArabic.ttf")     # wght axis
F_BOLD = os.path.join(FONTS, "IBMPlexSansArabic-Bold.ttf")
F_SEMI = os.path.join(FONTS, "IBMPlexSansArabic-SemiBold.ttf")
F_SERIF = os.path.join(FONTS, "Amiri-Bold.ttf")

CYAN = (120, 226, 255)
WHITE = (255, 255, 255)


def ar(txt):
    return get_display(arabic_reshaper.reshape(txt))


def load(path, size, axes=None):
    f = ImageFont.truetype(path, size)
    if axes:
        try:
            f.set_variation_by_axes(axes)
        except Exception:
            pass
    return f


def text_size(font, s):
    bb = font.getbbox(s)
    return bb[2] - bb[0], bb[3] - bb[1], bb[0], bb[1]


def draw_glow_text(base, xy, s, font, fill, glow=18, glow_alpha=110, anchor="mm", shadow=True):
    """Draw text with an outer glow + drop shadow for legibility on any background."""
    layer = Image.new("RGBA", base.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    d.text(xy, s, font=font, fill=fill, anchor=anchor)
    if shadow:
        sh = Image.new("RGBA", base.size, (0, 0, 0, 0))
        ImageDraw.Draw(sh).text((xy[0] + 4, xy[1] + 6), s, font=font,
                                fill=(0, 0, 0, 200), anchor=anchor)
        sh = sh.filter(ImageFilter.GaussianBlur(8))
        base.alpha_composite(sh)
    if glow > 0:
        g = layer.filter(ImageFilter.GaussianBlur(glow))
        a = g.split()[3].point(lambda v: min(255, int(v * glow_alpha / 255.0)))
        g.putalpha(a)
        base.alpha_composite(g)
    base.alpha_composite(layer)


# ---------------------------------------------------------------- title card
def make_title_card():
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    f1 = load(F_TITLE, 128, axes=[860])
    f2 = load(F_SEMI, 50)
    cy = int(H * 0.42)

    # soft dark scrim so the title reads on any frame
    scrim = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    sd = ImageDraw.Draw(scrim)
    sd.rounded_rectangle([40, cy - 230, W - 40, cy + 165], radius=60, fill=(2, 9, 17, 150))
    img.alpha_composite(scrim.filter(ImageFilter.GaussianBlur(34)))

    t1 = ar("أسرار المحيطات")
    draw_glow_text(img, (W // 2, cy - 96), t1, f1, WHITE, glow=30, glow_alpha=150)
    d = ImageDraw.Draw(img)
    y = cy + 26
    d.rounded_rectangle([W // 2 - 118, y, W // 2 + 118, y + 6], radius=3, fill=CYAN + (240,))
    d.ellipse([W // 2 - 152, y - 4, W // 2 - 140, y + 8], fill=CYAN + (200,))
    d.ellipse([W // 2 + 140, y - 4, W // 2 + 152, y + 8], fill=CYAN + (200,))
    t2 = ar("ما يخفيه العالم الأزرق")
    draw_glow_text(img, (W // 2, cy + 100), t2, f2, (198, 232, 250, 255), glow=14, glow_alpha=95)
    img.save(f"{BUILD}/title_card.png")


# ------------------------------------------------------------ brand watermark
def make_watermark():
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    f = load(F_SEMI, 40)
    t = ar("أسرار المحيطات")
    bb = f.getbbox(t)
    tw = bb[2] - bb[0]
    cx = W // 2
    cy = 176
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(layer).text((cx, cy), t, font=f, fill=(226, 246, 255, 168), anchor="mm")
    g = layer.filter(ImageFilter.GaussianBlur(10))
    g.putalpha(g.split()[3].point(lambda v: int(v * 0.55)))
    img.alpha_composite(g)
    img.alpha_composite(layer)
    d = ImageDraw.Draw(img)
    d.ellipse([cx - tw // 2 - 44, cy - 5, cx - tw // 2 - 34, cy + 5], fill=CYAN + (190,))
    d.ellipse([cx + tw // 2 + 34, cy - 5, cx + tw // 2 + 44, cy + 5], fill=CYAN + (190,))
    img.save(f"{BUILD}/watermark.png")


# ---------------------------------------------------------------- captions
def make_caption(lines, out, size=62, cy=1490, accent=True):
    f = load(F_BOLD, size)
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))

    # bottom scrim: smooth vertical gradient (keeps text legible, stays cinematic)
    grad = np.zeros((H, 1, 4), dtype=np.float32)
    y = np.arange(H, dtype=np.float32)
    t = np.clip((y - (cy - 470)) / 620.0, 0, 1) ** 1.35
    grad[:, 0, 3] = t * 215.0
    grad[:, 0, 0] = 2.0
    grad[:, 0, 1] = 10.0
    grad[:, 0, 2] = 20.0
    scrim = Image.fromarray(np.repeat(grad, W, axis=1).astype(np.uint8), "RGBA")
    img.alpha_composite(scrim)

    shaped = [ar(l) for l in lines]
    line_h = int(size * 1.46)
    total = line_h * len(shaped)
    y0 = int(cy - total // 2)
    if accent:
        d = ImageDraw.Draw(img)
        by = y0 - 40
        d.rounded_rectangle([W // 2 - 66, by, W // 2 + 66, by + 5], radius=3,
                            fill=CYAN + (225,))
    for i, sx in enumerate(shaped):
        draw_glow_text(img, (W // 2, y0 + line_h * i + line_h // 2), sx, f,
                       WHITE, glow=12, glow_alpha=90, shadow=True)
    img.save(out)


# ------------------------------------------------------- marine snow overlay
def make_particles(seed=7, n=420, path=f"{BUILD}/particles.png"):
    rng = np.random.default_rng(seed)
    pw, ph = 1080, 2160          # taller than frame so vertical drift stays covered
    img = Image.new("RGBA", (pw, ph), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for _ in range(n):
        x = rng.integers(0, pw)
        y = rng.integers(0, ph)
        r = float(rng.choice([0.8, 1.1, 1.4, 1.8, 2.4], p=[.28, .26, .2, .16, .10]))
        a = int(rng.integers(40, 190))
        d.ellipse([x - r, y - r, x + r, y + r], fill=(215, 242, 255, a))
    img = img.filter(ImageFilter.GaussianBlur(1.1))
    big = Image.new("RGBA", (pw, ph), (0, 0, 0, 0))
    d = ImageDraw.Draw(big)
    for _ in range(26):           # a few closer, softer out-of-focus motes
        x = rng.integers(0, pw); y = rng.integers(0, ph)
        r = float(rng.choice([5, 8, 12]))
        d.ellipse([x - r, y - r, x + r, y + r], fill=(200, 235, 255, int(rng.integers(28, 62))))
    big = big.filter(ImageFilter.GaussianBlur(6))
    img.alpha_composite(big)
    img.save(path)


# ================================================================== SFX
SR = 48000


def np_save(x, path):
    x = np.clip(x, -1.0, 1.0)
    pcm = (x * 32767.0).astype("<i2")
    import wave
    with wave.open(path, "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes(pcm.tobytes())


def onepole_lp(x, cutoff):
    a = math.exp(-2 * math.pi * cutoff / SR)
    y = np.empty_like(x)
    acc = 0.0
    for i in range(len(x)):
        acc = (1 - a) * x[i] + a * acc
        y[i] = acc
    return y


def fft_band(x, lo, hi, slope=1.0):
    n = len(x)
    X = np.fft.rfft(x)
    f = np.fft.rfftfreq(n, 1.0 / SR)
    g = np.ones_like(f)
    g[f < lo] = (np.maximum(f[f < lo], 1e-6) / lo) ** slope
    g[f > hi] = (hi / np.maximum(f[f > hi], 1e-6)) ** slope
    return np.fft.irfft(X * g, n)


def lfo_amp(n, rate, depth, phase=0.0):
    t = np.arange(n) / SR
    return 1.0 - depth + depth * (0.5 + 0.5 * np.sin(2 * np.pi * rate * t + phase))


# 1) continuous underwater ambience -----------------------------------------
def make_ambience(dur=12.4):
    n = int(dur * SR)
    rng = np.random.default_rng(11)
    w = rng.standard_normal(n)
    brown = np.cumsum(w)
    brown = brown / (np.max(np.abs(brown)) + 1e-9)
    deep = fft_band(brown, 18, 420, slope=1.15)
    deep = deep / (np.max(np.abs(deep)) + 1e-9)
    deep *= lfo_amp(n, 0.075, 0.42) * lfo_amp(n, 0.19, 0.18)

    # faint current hiss (very low level, keeps it "watery" without being music)
    hiss = fft_band(rng.standard_normal(n), 900, 5200, slope=1.0)
    hiss = hiss / (np.max(np.abs(hiss)) + 1e-9)
    hiss *= lfo_amp(n, 0.13, 0.55, phase=1.1) * 0.11

    # slow pressure drone: two detuned sub sines, no musical interval feel
    t = np.arange(n) / SR
    drone = (0.5 * np.sin(2 * np.pi * 41.0 * t) + 0.34 * np.sin(2 * np.pi * 41.6 * t))
    drone *= lfo_amp(n, 0.05, 0.5, phase=2.0) * 0.30

    mix = 0.72 * deep + hiss + drone
    mix = np.tanh(mix * 1.25) * 0.5
    # fade in/out
    fi, fo = int(0.9 * SR), int(1.3 * SR)
    mix[:fi] *= np.linspace(0, 1, fi) ** 1.5
    mix[-fo:] *= np.linspace(1, 0, fo) ** 1.4
    np_save(mix, f"{BUILD}/sfx_ambience.wav")


# 2) deep impact booms ------------------------------------------------------
def make_boom(path, dur=1.6, f0=44.0, sub=True):
    n = int(dur * SR)
    t = np.arange(n) / SR
    env = np.exp(-t * 3.1)
    tone = np.sin(2 * np.pi * (f0 * np.exp(-t * 0.55) + 21.0) * t)
    click = fft_band(np.random.default_rng(3).standard_normal(n), 60, 1400, 1.0)
    click = click / (np.max(np.abs(click)) + 1e-9) * np.exp(-t * 26) * 0.5
    x = tone * env + click
    if sub:
        x += np.sin(2 * np.pi * 29.0 * t) * np.exp(-t * 2.2) * 0.5
    x = np.tanh(x * 1.1)
    fo = int(0.5 * SR)
    x[-fo:] *= np.linspace(1, 0, fo)
    np_save(x * 0.85, path)


# 3) water whooshes (transition swells, no tonal/musical content) ------------
def make_whoosh(path, dur=1.15, lo=140, hi=3400, seed=5, rev=False):
    n = int(dur * SR)
    rng = np.random.default_rng(seed)
    src = rng.standard_normal(n)
    x = fft_band(src, lo, hi, slope=1.0)
    x = x / (np.max(np.abs(x)) + 1e-9)
    t = np.arange(n) / SR
    p = t / dur
    env = np.sin(np.pi * p) ** 1.7                      # swell
    env *= 0.65 + 0.35 * np.sin(2 * np.pi * (2.2 if not rev else 1.6) * p)
    # spectral movement: darker at the edges, brighter in the middle
    bright = fft_band(src, 2200, 7200, 1.0)
    bright = bright / (np.max(np.abs(bright)) + 1e-9)
    x = x * env + bright * (np.sin(np.pi * p) ** 4) * 0.45
    x = np.tanh(x * 1.3) * 0.8
    fi = int(0.02 * SR)
    x[:fi] *= np.linspace(0, 1, fi)
    np_save(x, path)


# 4) bubble clusters --------------------------------------------------------
def make_bubbles(path, dur=6.0, rate=1.9, seed=9):
    n = int(dur * SR)
    out = np.zeros(n)
    rng = np.random.default_rng(seed)
    t = 0.15
    while t < dur - 0.4:
        for _ in range(int(rng.integers(2, 6))):
            f0 = float(rng.uniform(330, 900))
            f1 = f0 * float(rng.uniform(1.7, 2.9))
            d = float(rng.uniform(0.045, 0.085))
            m = int(d * SR)
            tt = np.arange(m) / SR
            k = (f1 - f0) / d
            ph = 2 * np.pi * (f0 * tt + 0.5 * k * tt ** 2)
            b = np.sin(ph) * np.exp(-tt / (d * 0.30))
            b *= np.linspace(0.4, 1.0, m)
            i = int((t + rng.uniform(0, 0.12)) * SR)
            if i + m < n:
                out[i:i + m] += b * float(rng.uniform(0.25, 0.7))
            t += float(rng.uniform(0.02, 0.11))
        t += float(rng.exponential(1.0 / rate))
    out = fft_band(out, 180, 8000, 1.0)
    out = np.tanh(out * 1.4) * 0.55
    np_save(out, path)


# 5) sonar ping with echo ---------------------------------------------------
def make_sonar(path, dur=2.6, freq=880.0):
    n = int(dur * SR)
    x = np.zeros(n)
    t0 = np.arange(int(1.6 * SR)) / SR
    base = (np.sin(2 * np.pi * freq * t0) * 0.72 +
            np.sin(2 * np.pi * freq * 2.01 * t0) * 0.20)
    base *= np.exp(-t0 * 3.0)
    base[: int(0.004 * SR)] *= np.linspace(0, 1, int(0.004 * SR))
    x[: len(base)] += base
    for delay, gain in ((0.34, 0.30), (0.71, 0.14), (1.12, 0.06)):
        i = int(delay * SR)
        seg = base[: n - i]
        x[i:i + len(seg)] += seg * gain
    x = fft_band(x, 200, 9000, 1.0)
    fo = int(0.5 * SR)
    x[-fo:] *= np.linspace(1, 0, fo)
    np_save(x * 0.8, path)


# 6) riser (tension build) --------------------------------------------------
def make_riser(path, dur=1.5, seed=13):
    n = int(dur * SR)
    rng = np.random.default_rng(seed)
    src = rng.standard_normal(n)
    out = np.zeros(n)
    steps = 26
    for i in range(steps):
        a, b = int(i * n / steps), int((i + 1) * n / steps)
        seg = src[a:b]
        X = np.fft.rfft(seg)
        f = np.fft.rfftfreq(len(seg), 1.0 / SR)
        center = 220 * (1.0 + 7.5 * (i / steps) ** 1.25)
        g = np.exp(-0.5 * ((f - center) / (center * 0.85)) ** 2)
        out[a:b] = np.fft.irfft(X * g, len(seg))
    out = out / (np.max(np.abs(out)) + 1e-9)
    t = np.arange(n) / SR
    out *= (t / dur) ** 1.6
    np_save(out * 0.75, path)


if __name__ == "__main__":
    make_title_card()
    make_watermark()
    make_caption(["في أعماق المحيط", "يختبئ عالم لم يره أحد"], f"{BUILD}/cap1.png", size=60)
    make_caption(["أسرار دُفنت", "منذ ملايين السنين"], f"{BUILD}/cap2.png", size=62)
    make_caption(["وثمانون بالمئة منه", "لم تُكتشف بعد"], f"{BUILD}/cap3.png", size=58)
    make_particles()
    make_ambience()
    make_boom(f"{BUILD}/sfx_boom.wav", f0=46)
    make_boom(f"{BUILD}/sfx_boom2.wav", dur=2.2, f0=36)
    make_whoosh(f"{BUILD}/sfx_whoosh1.wav", seed=21)
    make_whoosh(f"{BUILD}/sfx_whoosh2.wav", seed=22)
    make_whoosh(f"{BUILD}/sfx_whoosh3.wav", seed=23, lo=110, hi=2800, dur=1.3)
    make_bubbles(f"{BUILD}/sfx_bubbles.wav", dur=12.0)
    make_sonar(f"{BUILD}/sfx_sonar.wav")
    make_riser(f"{BUILD}/sfx_riser.wav")
    print("assets done")

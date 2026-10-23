#!/usr/bin/env python3
"""Build the 30.000s stereo 48kHz mastered audio track:
- Studio Arabic narration (5 natural segments synced to visual cuts)
- Original Hijaz-scale Plucked Oud + Cinematic Brass/Sub Drone + Darbuka/Taiko Score
- Precision-synced Foley & VFX Sound Design (Hook Boom, Time-Rewind Ticks, Sahara Wind,
  Torch Fire Crackle, Bronze Chisel Strikes, Rosetta Laser Shimmer, Whip-Pan Whooshes)
"""
from __future__ import annotations

import json
import math
import subprocess
import wave
from pathlib import Path

import imageio_ffmpeg
import numpy as np
from scipy import signal

ROOT = Path(__file__).resolve().parent
ASSETS = ROOT / "egypt_assets"
SR = 48000
DURATION = 30.0
N_SAMPLES = int(DURATION * SR)


def load_vo_clip(path: Path, tempo: float = 1.15) -> np.ndarray:
    ffmpeg = imageio_ffmpeg.get_ffmpeg_exe()
    af = (
        f"silenceremove=start_periods=1:start_threshold=-42dB:"
        f"stop_periods=-1:stop_duration=0.20:stop_threshold=-42dB,"
        f"atempo={tempo:.3f}"
    )
    res = subprocess.run(
        [ffmpeg, "-i", str(path), "-af", af, "-f", "s16le", "-ac", "1", "-ar", str(SR), "-"],
        capture_output=True,
        check=True,
    )
    pcm = np.frombuffer(res.stdout, dtype=np.int16).astype(np.float64) / 32768.0
    win = int(0.015 * SR)
    rms = np.sqrt(np.convolve(pcm * pcm, np.ones(win) / win, mode="same"))
    nz = np.where(rms > 0.007)[0]
    if len(nz) > 0:
        s0 = max(0, nz[0] - int(0.02 * SR))
        s1 = min(len(pcm), nz[-1] + int(0.06 * SR))
        pcm = pcm[s0:s1]
    # Short 15ms cosine fade-in / 35ms fade-out for zero clicks
    fi = min(int(0.015 * SR), len(pcm) // 4)
    fo = min(int(0.035 * SR), len(pcm) // 4)
    if fi > 0:
        pcm[:fi] *= 0.5 * (1.0 - np.cos(np.linspace(0, math.pi, fi)))
    if fo > 0:
        pcm[-fo:] *= 0.5 * (1.0 + np.cos(np.linspace(0, math.pi, fo)))
    return pcm


def find_clause_split(pcm: np.ndarray, approx_frac: float = 0.5, window_sec: float = 1.4) -> float:
    """Find the cleanest natural speech dip near approx_frac of the clip."""
    win = int(0.025 * SR)
    rms = np.sqrt(np.convolve(pcm * pcm, np.ones(win) / win, mode="same"))
    center = int(len(pcm) * approx_frac)
    half = int(window_sec * SR * 0.5)
    lo = max(int(0.15 * len(pcm)), center - half)
    hi = min(int(0.85 * len(pcm)), center + half)
    idx = lo + int(np.argmin(rms[lo:hi]))
    return idx / SR


def karplus_strong(freq: float, dur: float, brightness: float = 0.992, rng: np.random.Generator | None = None) -> np.ndarray:
    """Physical-modeling plucked Oud/Qanun string synthesis."""
    if rng is None:
        rng = np.random.default_rng(42)
    n = int(dur * SR)
    delay = max(2, int(round(SR / freq)))
    # Warm plucked excitation burst (filtered noise + body resonance)
    burst = rng.uniform(-1.0, 1.0, delay)
    b, a = signal.butter(2, min(0.45, (freq * 6.5) / (SR * 0.5)), btype="low")
    burst = signal.lfilter(b, a, burst)
    out = np.zeros(n, dtype=np.float64)
    out[: min(delay, n)] = burst[: min(delay, n)]
    for i in range(delay, n):
        out[i] = brightness * (0.54 * out[i - delay] + 0.46 * out[i - delay + 1])
    # Plucked attack envelope
    env = np.exp(-np.linspace(0, 3.8, n))
    out *= env
    # Subtle oud wooden body resonance around 210 Hz
    b_res, a_res = signal.iirpeak(215.0 / (SR * 0.5), Q=3.5)
    out = 0.72 * out + 0.28 * signal.lfilter(b_res, a_res, out)
    peak = np.max(np.abs(out))
    return out / max(peak, 1e-6)


def add_stereo(buf: np.ndarray, start_sec: float, mono: np.ndarray, gain_l: float, gain_r: float) -> None:
    i0 = int(round(start_sec * SR))
    if i0 >= N_SAMPLES or i0 + len(mono) <= 0:
        return
    src0 = max(0, -i0)
    dst0 = max(0, i0)
    length = min(len(mono) - src0, N_SAMPLES - dst0)
    if length <= 0:
        return
    seg = mono[src0 : src0 + length]
    buf[dst0 : dst0 + length, 0] += seg * gain_l
    buf[dst0 : dst0 + length, 1] += seg * gain_r


def make_impact_boom(dur: float, f_start: float = 95.0, f_end: float = 31.0, metallic: bool = True, rng=None) -> np.ndarray:
    if rng is None:
        rng = np.random.default_rng(7)
    n = int(dur * SR)
    t = np.arange(n, dtype=np.float64) / SR
    # Exponential frequency sweep for sub-bass impact
    phase = 2.0 * math.pi * (f_end * t + (f_start - f_end) * (1.0 - np.exp(-t * 9.0)) / 9.0)
    sub = np.sin(phase) * np.exp(-t * 2.4)
    punch = np.sin(2.0 * phase) * np.exp(-t * 9.0) * 0.45
    noise = rng.normal(0, 1, n)
    b, a = signal.butter(2, 260.0 / (SR * 0.5), btype="low")
    thud = signal.lfilter(b, a, noise) * np.exp(-t * 14.0) * 0.8
    sig = sub + punch + thud
    if metallic:
        # Ancient bronze gong shimmer partials (inharmonic ratios)
        for f_part, amp, dec in [(220.0, 0.14, 1.8), (337.0, 0.10, 2.2), (512.0, 0.07, 2.9), (784.0, 0.05, 3.6)]:
            sig += amp * np.sin(2.0 * math.pi * f_part * t) * np.exp(-t * dec)
    return sig / max(np.max(np.abs(sig)), 1e-6)


def make_whoosh(dur: float = 0.42, direction: int = 1, rng=None) -> np.ndarray:
    """Directional stereo-panned cinematic whip-pan whoosh."""
    if rng is None:
        rng = np.random.default_rng(19)
    n = int(dur * SR)
    t = np.linspace(0.0, 1.0, n)
    noise = rng.normal(0.0, 1.0, n)
    # Time-varying bandpass sweep via chunked filtering
    out = np.zeros(n, dtype=np.float64)
    chunks = 16
    step = n // chunks
    for c in range(chunks):
        s, e = c * step, (c + 1) * step if c < chunks - 1 else n
        u = (c + 0.5) / chunks
        fc = 220.0 + 1900.0 * math.sin(math.pi * u) ** 1.4
        b, a = signal.butter(2, [max(80.0, fc * 0.55) / (SR * 0.5), min(6000.0, fc * 1.8) / (SR * 0.5)], btype="band")
        out[s:e] = signal.lfilter(b, a, noise[s:e])
    env = np.sin(math.pi * t) ** 1.8
    out *= env
    out /= max(np.max(np.abs(out)), 1e-6)
    pan = np.linspace(0.15, 0.85, n) if direction > 0 else np.linspace(0.85, 0.15, n)
    return np.stack([out * np.cos(pan * math.pi * 0.5), out * np.sin(pan * math.pi * 0.5)], axis=1)


def make_chisel_strike(rng=None) -> np.ndarray:
    """Crisp metallic bronze chisel striking limestone + stone grit burst."""
    if rng is None:
        rng = np.random.default_rng(91)
    n = int(0.32 * SR)
    t = np.arange(n, dtype=np.float64) / SR
    # High metallic bronze partials
    metal = (
        0.45 * np.sin(2 * math.pi * 2480 * t) * np.exp(-t * 28.0)
        + 0.32 * np.sin(2 * math.pi * 3910 * t) * np.exp(-t * 36.0)
        + 0.22 * np.sin(2 * math.pi * 5860 * t) * np.exp(-t * 48.0)
        + 0.30 * np.sin(2 * math.pi * 1140 * t) * np.exp(-t * 22.0)
    )
    # Stone mallet knock + grit
    grit = rng.normal(0, 1, n)
    b, a = signal.butter(2, [900 / (SR * 0.5), 7500 / (SR * 0.5)], btype="band")
    stone = signal.lfilter(b, a, grit) * np.exp(-t * 32.0) * 0.75
    sig = metal + stone
    return sig / max(np.max(np.abs(sig)), 1e-6)


def make_darbuka_hit(kind: str = "doum", rng=None) -> np.ndarray:
    if rng is None:
        rng = np.random.default_rng(55)
    if kind == "doum":
        n = int(0.26 * SR)
        t = np.arange(n) / SR
        phase = 2 * math.pi * (82.0 * t + 75.0 * (1 - np.exp(-t * 26.0)) / 26.0)
        body = np.sin(phase) * np.exp(-t * 11.5)
        skin = rng.normal(0, 1, n)
        b, a = signal.butter(2, 420 / (SR * 0.5), btype="low")
        hit = body + 0.25 * signal.lfilter(b, a, skin) * np.exp(-t * 35.0)
    else:  # "tak" / "ka"
        n = int(0.11 * SR)
        t = np.arange(n) / SR
        ring = 0.55 * np.sin(2 * math.pi * 430 * t) * np.exp(-t * 38.0) + 0.35 * np.sin(2 * math.pi * 1280 * t) * np.exp(-t * 55.0)
        snap = rng.normal(0, 1, n)
        b, a = signal.butter(2, [1400 / (SR * 0.5), 6500 / (SR * 0.5)], btype="band")
        hit = ring + 0.65 * signal.lfilter(b, a, snap) * np.exp(-t * 60.0)
    return hit / max(np.max(np.abs(hit)), 1e-6)


def build() -> None:
    rng = np.random.default_rng(2026)

    # 1. Load & prepare the 5 studio Arabic voiceover clips
    c1 = load_vo_clip(ASSETS / "vo_p1.mp3", tempo=1.16)
    c2 = load_vo_clip(ASSETS / "vo_p2.mp3", tempo=1.16)
    c3 = load_vo_clip(ASSETS / "vo_p3.mp3", tempo=1.16)
    c4 = load_vo_clip(ASSETS / "vo_p4.mp3", tempo=1.16)
    c5 = load_vo_clip(ASSETS / "vo_06_finale.mp3", tempo=1.13)

    d1, d2, d3, d4, d5 = [len(c) / SR for c in (c1, c2, c3, c4, c5)]
    total_speech = d1 + d2 + d3 + d4 + d5
    # Distribute gaps cleanly across 0.14s .. 29.45s
    avail_gaps = (29.42 - 0.14) - total_speech
    gap = max(0.18, avail_gaps / 4.0)

    s1 = 0.14
    s2 = s1 + d1 + gap
    s3 = s2 + d2 + gap
    s4 = s3 + d3 + gap
    s5 = s4 + d4 + gap

    # Exact internal clause splits inside each clip
    sp1 = s1 + find_clause_split(c1, 0.51, 1.0)   # "الحقيقة أقدم بثلاثة آلاف عام!"
    sp2 = s2 + find_clause_split(c2, 0.33, 1.0)   # "قاد الأمير المصري خعمواست..."
    sp3 = s3 + find_clause_split(c3, 0.54, 1.0)   # "ونقش عليها أسماء ملوكها الأصليين"
    sp4 = s4 + find_clause_split(c4, 0.50, 1.0)   # "نطقت الحجارة لتشهد..."
    sp5 = s5 + find_clause_split(c5, 0.48, 0.9)   # "مصر أول من اكتشفه!"

    cuts = {
        "shot1_start": 0.0,
        "shot2_twist": round(sp1, 3),          # ~3.15s: 3D Map & 3000-yr rewind
        "shot3_saqqara": round(s2 - 0.08, 3),  # ~6.30s: Buried Saqqara sandstorm
        "shot4a_prince": round(sp2, 3),        # ~8.65s: Prince Khaemwaset hero wide
        "shot4b_torch": round((sp2 + s3) * 0.5 + 0.15, 3),  # ~11.1s: Close-up Prince & torch
        "shot5_excav": round(s3 - 0.08, 3),    # ~13.3s: Saqqara excavation trench
        "shot6_stele": round(sp3, 3),          # ~16.4s: Sand-wipe & chisel sparks
        "shot7_colossi": round(s4 - 0.08, 3),  # ~19.1s: Colossi timelapse sweep
        "shot8_rosetta": round(sp4, 3),        # ~22.3s: 3D Rosetta laser scanner
        "shot9_finale": round(s5 - 0.10, 3),   # ~25.6s: Giza Pyramids aerial finale
        "shot9_punch": round(sp5, 3),          # ~27.6s: "مصر أول من اكتشفه!"
        "vo_segments": [
            {"id": 1, "start": round(s1, 3), "split": round(sp1, 3), "end": round(s1 + d1, 3)},
            {"id": 2, "start": round(s2, 3), "split": round(sp2, 3), "end": round(s2 + d2, 3)},
            {"id": 3, "start": round(s3, 3), "split": round(sp3, 3), "end": round(s3 + d3, 3)},
            {"id": 4, "start": round(s4, 3), "split": round(sp4, 3), "end": round(s4 + d4, 3)},
            {"id": 5, "start": round(s5, 3), "split": round(sp5, 3), "end": round(s5 + d5, 3)},
        ],
    }
    (ASSETS / "timeline_meta.json").write_text(json.dumps(cuts, indent=2, ensure_ascii=False))
    print("Timeline meta:", json.dumps(cuts, indent=2, ensure_ascii=False))

    # Place voiceover into mono buffer + crisp broadcast EQ
    vo_mono = np.zeros(N_SAMPLES, dtype=np.float64)
    for start_t, clip in [(s1, c1), (s2, c2), (s3, c3), (s4, c4), (s5, c5)]:
        i0 = int(round(start_t * SR))
        length = min(len(clip), N_SAMPLES - i0)
        vo_mono[i0 : i0 + length] += clip[:length]

    # Highpass 80Hz + subtle 3.4kHz presence boost + gentle soft-knee compression
    b_hp, a_hp = signal.butter(2, 80.0 / (SR * 0.5), btype="high")
    vo_mono = signal.lfilter(b_hp, a_hp, vo_mono)
    b_pr, a_pr = signal.iirpeak(3400.0 / (SR * 0.5), Q=1.4)
    vo_mono = vo_mono + 0.22 * signal.lfilter(b_pr, a_pr, vo_mono)
    vo_mono = np.tanh(vo_mono * 1.45) / np.tanh(1.45)
    vo_mono /= max(np.max(np.abs(vo_mono)), 1e-6)

    # Compute smooth voice envelope for sidechain ducking of music/SFX
    env_win = int(0.08 * SR)
    vo_env = np.sqrt(np.convolve(vo_mono * vo_mono, np.ones(env_win) / env_win, mode="same"))
    vo_env = np.clip(vo_env / max(np.percentile(vo_env, 95), 1e-4), 0.0, 1.0)
    duck = 1.0 - 0.52 * vo_env  # Ducks background bed ~6dB when voice is speaking

    # 2. Synthesize Original Pharaonic Hijaz Score (D Hijaz: D, Eb, F#, G, A, Bb, C, D)
    music = np.zeros((N_SAMPLES, 2), dtype=np.float64)
    t_full = np.arange(N_SAMPLES, dtype=np.float64) / SR

    # 2a. Deep Orchestral Sub-Drone & Warm Brass Pad (D2=73.42Hz, A2=110Hz, D3=146.83Hz)
    drone_l = (
        0.38 * np.sin(2 * math.pi * 73.42 * t_full)
        + 0.20 * np.sin(2 * math.pi * 110.00 * t_full + 0.4)
        + 0.14 * signal.sawtooth(2 * math.pi * 73.42 * t_full)
        + 0.10 * signal.sawtooth(2 * math.pi * 146.83 * t_full + 1.1)
    )
    drone_r = (
        0.38 * np.sin(2 * math.pi * 73.42 * t_full)
        + 0.20 * np.sin(2 * math.pi * 110.00 * t_full - 0.4)
        + 0.14 * signal.sawtooth(2 * math.pi * 73.60 * t_full + 0.7)
        + 0.10 * signal.sawtooth(2 * math.pi * 147.10 * t_full + 0.2)
    )
    # Sweeping analog lowpass filter that opens up in the finale!
    cutoff_curve = np.interp(
        t_full,
        [0.0, sp1, s3, sp4, s5, 29.0, 30.0],
        [260, 420, 380, 520, 950, 1100, 320],
    )
    # Apply warm LP filter in blocks
    blk = 2048
    for i in range(0, N_SAMPLES, blk):
        e = min(N_SAMPLES, i + blk)
        fc = float(np.mean(cutoff_curve[i:e]))
        b_lp, a_lp = signal.butter(2, fc / (SR * 0.5), btype="low")
        music[i:e, 0] += signal.lfilter(b_lp, a_lp, drone_l[i:e]) * 0.24
        music[i:e, 1] += signal.lfilter(b_lp, a_lp, drone_r[i:e]) * 0.24

    # 2b. Plucked Oud / Qanun Hijaz Ostinato (120 BPM = 0.50s per beat, 0.25s 8th notes)
    hijaz_riff = [
        146.83, 155.56, 185.00, 196.00, 220.00, 185.00, 196.00, 155.56,
        146.83, 155.56, 185.00, 220.00, 233.08, 220.00, 185.00, 146.83,
    ]
    note_cache: dict[float, np.ndarray] = {}
    for f_hz in set(hijaz_riff + [293.66, 370.00, 440.00]):
        note_cache[f_hz] = karplus_strong(f_hz, 0.65, brightness=0.993, rng=rng)

    beat_step = 0.25  # 16th/8th pulse
    step_idx = 0
    t_cur = sp1  # Oud enters dramatically right on the 3,000-year Twist!
    while t_cur < 29.2:
        f_hz = hijaz_riff[step_idx % len(hijaz_riff)]
        accent = 1.0 if (step_idx % 4 == 0) else 0.68
        # Finale octave doubling for epic emotional climax!
        if t_cur >= cuts["shot9_finale"]:
            accent *= 1.35
            hi_f = 293.66 if f_hz < 180 else (370.00 if f_hz < 200 else 440.00)
            add_stereo(music, t_cur, note_cache[hi_f], 0.11 * accent, 0.14 * accent)
        pan_l = 0.16 * accent if (step_idx % 2 == 0) else 0.11 * accent
        pan_r = 0.11 * accent if (step_idx % 2 == 0) else 0.16 * accent
        add_stereo(music, t_cur, note_cache[f_hz], pan_l, pan_r)
        t_cur += beat_step
        step_idx += 1

    # 2c. Darbuka & Taiko Driving Percussion (Maqsum / Baladi cinematic groove: D - T - - T D - T -)
    doum = make_darbuka_hit("doum", rng)
    tak = make_darbuka_hit("tak", rng)
    pattern = [("doum", 1.0), (None, 0), ("tak", 0.65), ("tak", 0.50), ("doum", 0.85), (None, 0), ("tak", 0.75), (None, 0)]
    p_idx = 0
    t_cur = sp1
    while t_cur < 29.0:
        # Brief dramatic drum drop during the mysterious Colossi transition (shot7)
        if not (cuts["shot7_colossi"] + 0.3 <= t_cur <= cuts["shot8_rosetta"] - 0.1):
            kind, vel = pattern[p_idx % len(pattern)]
            if t_cur >= cuts["shot9_finale"]:
                vel *= 1.30
            if kind == "doum":
                add_stereo(music, t_cur, doum, 0.26 * vel, 0.26 * vel)
            elif kind == "tak":
                add_stereo(music, t_cur, tak, 0.15 * vel, 0.19 * vel)
        t_cur += 0.25
        p_idx += 1

    # 3. Synthesize Precision Foley & Cinematic Sound Effects
    sfx = np.zeros((N_SAMPLES, 2), dtype=np.float64)

    # Big cinematic sub-booms on major beats: Hook (0.0s), Twist (sp1), Prince (sp2), Rosetta (sp4), Finale (s5 & sp5)
    boom_heavy = make_impact_boom(1.8, 105.0, 30.0, metallic=True, rng=rng)
    boom_tight = make_impact_boom(1.1, 90.0, 34.0, metallic=False, rng=rng)
    for bt, g in [
        (0.01, 0.58),
        (cuts["shot2_twist"], 0.55),
        (cuts["shot4a_prince"], 0.44),
        (cuts["shot8_rosetta"], 0.44),
        (cuts["shot9_finale"], 0.56),
        (cuts["shot9_punch"], 0.62),
    ]:
        add_stereo(sfx, bt, boom_heavy if g > 0.5 else boom_tight, g, g)

    # Whip-pan directional whooshes on every visual transition
    cut_times = [
        cuts["shot2_twist"], cuts["shot3_saqqara"], cuts["shot4a_prince"],
        cuts["shot4b_torch"], cuts["shot5_excav"], cuts["shot6_stele"],
        cuts["shot7_colossi"], cuts["shot8_rosetta"], cuts["shot9_finale"],
    ]
    for idx_c, ct in enumerate(cut_times):
        wh = make_whoosh(0.38, direction=1 if idx_c % 2 == 0 else -1, rng=rng)
        i0 = max(0, int((ct - 0.18) * SR))
        ln = min(len(wh), N_SAMPLES - i0)
        sfx[i0 : i0 + ln] += wh[:ln] * 0.30

    # Time-rewind rapid mechanical ticks (Shot 2: 1799 -> 1250 BC)
    t_tick = cuts["shot2_twist"] + 0.18
    tick_end = cuts["shot3_saqqara"] - 0.20
    tick_i = 0
    while t_tick < tick_end:
        prog = (t_tick - cuts["shot2_twist"]) / max(0.2, tick_end - cuts["shot2_twist"])
        n_tk = int(0.035 * SR)
        tt = np.arange(n_tk) / SR
        f_tk = 1650.0 - 520.0 * prog
        tk_wave = np.sin(2 * math.pi * f_tk * tt) * np.exp(-tt * 95.0)
        pan = 0.3 + 0.4 * ((tick_i % 2) * 2 - 1) * 0.5
        add_stereo(sfx, t_tick, tk_wave, 0.16 * (1 - pan * 0.3), 0.16 * (0.7 + pan * 0.3))
        t_tick += 0.065 + 0.055 * (prog ** 2)
        tick_i += 1

    # Sahara sandstorm howling wind (Shot 3 & Shot 5)
    for w_s, w_e, w_gain in [
        (cuts["shot3_saqqara"], cuts["shot4a_prince"] + 0.3, 0.20),
        (cuts["shot5_excav"], cuts["shot6_stele"] + 0.8, 0.16),
    ]:
        nw = int((w_e - w_s) * SR)
        if nw > 0:
            tw = np.linspace(0, 1, nw)
            wnoise = rng.normal(0, 1, nw)
            b_w, a_w = signal.butter(2, [240 / (SR * 0.5), 1150 / (SR * 0.5)], btype="band")
            gust = signal.lfilter(b_w, a_w, wnoise) * (0.55 + 0.45 * np.sin(2 * math.pi * 1.7 * tw))
            gust *= np.sin(math.pi * tw) ** 0.7
            gust /= max(np.max(np.abs(gust)), 1e-6)
            add_stereo(sfx, w_s, gust, w_gain, w_gain * 0.88)

    # Torch crackling fire foley (Shot 4A & 4B)
    fire_s, fire_e = cuts["shot4a_prince"], cuts["shot5_excav"]
    n_f = int((fire_e - fire_s) * SR)
    if n_f > 0:
        rumble = signal.lfilter(*signal.butter(2, 190 / (SR * 0.5), btype="low"), rng.normal(0, 1, n_f))
        pops = (rng.uniform(0, 1, n_f) > 0.9988).astype(np.float64) * rng.uniform(0.3, 1.0, n_f)
        pops = signal.lfilter(*signal.butter(2, [1200 / (SR * 0.5), 6000 / (SR * 0.5)], btype="band"), pops)
        fire = 0.35 * rumble / max(np.max(np.abs(rumble)), 1e-6) + 0.65 * pops / max(np.max(np.abs(pops)), 1e-6)
        add_stereo(sfx, fire_s, fire, 0.14, 0.10)

    # 3 Crisp Bronze Chisel Strikes on Limestone (Shot 6)
    chisel = make_chisel_strike(rng)
    chisel_times = [
        round(cuts["shot6_stele"] + 0.45, 3),
        round(cuts["shot6_stele"] + 1.18, 3),
        round(cuts["shot6_stele"] + 1.90, 3),
    ]
    cuts["chisel_strikes"] = chisel_times
    (ASSETS / "timeline_meta.json").write_text(json.dumps(cuts, indent=2, ensure_ascii=False))
    for ct in chisel_times:
        if ct < cuts["shot7_colossi"] - 0.15:
            add_stereo(sfx, ct, chisel, 0.30, 0.34)

    # Mystical Crystalline Shimmer during Rosetta Laser Scan (Shot 8)
    r_s, r_e = cuts["shot8_rosetta"], cuts["shot9_finale"]
    nr = int((r_e - r_s) * SR)
    if nr > 0:
        tr = np.arange(nr) / SR
        env_r = np.sin(math.pi * np.linspace(0, 1, nr)) ** 1.2
        shimmer = (
            0.35 * np.sin(2 * math.pi * 587.33 * tr)
            + 0.28 * np.sin(2 * math.pi * 880.00 * tr + np.sin(tr * 6.0))
            + 0.22 * np.sin(2 * math.pi * 1174.66 * tr)
            + 0.15 * np.sin(2 * math.pi * 1480.00 * tr)
        ) * env_r
        shimmer /= max(np.max(np.abs(shimmer)), 1e-6)
        add_stereo(sfx, r_s, shimmer, 0.11, 0.13)

    # 4. Final Mix + Sidechain Ducking + Broadcast Limiter
    bed = (music + sfx) * duck[:, None]
    stereo = bed.copy()
    stereo[:, 0] += vo_mono * 0.92
    stereo[:, 1] += vo_mono * 0.92

    # Clean 0.45s tail fade-out right before 30.000s
    fade_out_n = int(0.42 * SR)
    fade_curve = 0.5 * (1.0 + np.cos(np.linspace(0, math.pi, fade_out_n)))
    stereo[-fade_out_n:] *= fade_curve[:, None]

    # Soft-clip mastering limiter to -1.0 dBTP
    peak = float(np.max(np.abs(stereo)))
    stereo = stereo / max(peak, 1e-6) * 1.15
    stereo = np.tanh(stereo) * 0.91

    out_wav = ASSETS / "master_audio.wav"
    pcm16 = np.clip(stereo * 32767.0, -32768, 32767).astype("<i2")
    with wave.open(str(out_wav), "wb") as wf:
        wf.setnchannels(2)
        wf.setsampwidth(2)
        wf.setframerate(SR)
        wf.writeframes(pcm16.tobytes())
    print(f"Saved mastered audio: {out_wav} ({len(pcm16)/SR:.3f}s)")


if __name__ == "__main__":
    build()

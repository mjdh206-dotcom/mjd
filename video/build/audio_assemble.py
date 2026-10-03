#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
"هل تعلم عن المغرب؟" — 18.000s vertical short | audio assembly
VO placement + silence trim + gentle tempo fit + synthesized CINEMATIC SFX bed (NO MUSIC).
Outputs:
  video/audio/voiceover_18s_clean.wav/.mp3     (VO only, exact timeline)
  video/audio/sfx_bed_18s.wav                  (SFX only, no music, no VO)
  video/audio/voiceover_18s_with_sfx.wav/.mp3  (final mix to lay under picture)
  video/subtitles/captions_ar.srt              (cues = measured VO in/out)
"""
import json, os, subprocess, sys, wave
import numpy as np

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
import imageio_ffmpeg
FF = imageio_ffmpeg.get_ffmpeg_exe()
SR = 48000
TOTAL = 18.000

SEGMENTS = [  # (file, start, end_of_window)
    ("vo_01_hook.mp3",       0.10,  3.30),
    ("vo_02_identity.mp3",   3.35,  6.95),
    ("vo_03_crossroads.mp3", 7.00, 10.85),
    ("vo_04_gateway.mp3",   10.90, 14.05),
    ("vo_05_closing.mp3",   14.10, 17.90),
]
MAX_TEMPO = 1.25
TRIM_THRESH = 0.010          # ~ -40 dB
PAD = 0.04

def ff(args, inp=None):
    p = subprocess.run([FF, "-hide_banner", "-loglevel", "error"] + args,
                       input=inp, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if p.returncode != 0:
        raise RuntimeError(p.stderr.decode()[-800:])
    return p.stdout

def decode_mono(path):
    raw = ff(["-i", path, "-f", "f32le", "-ac", "1", "-ar", str(SR), "-"])
    return np.frombuffer(raw, dtype=np.float32).copy()

def encode_wav(path, data_stereo):
    d = np.clip(data_stereo, -1, 1)
    pcm = (d * 32767).astype("<i2")
    with wave.open(path, "wb") as w:
        w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR)
        w.writeframes(pcm.tobytes())

def encode_mp3(wav_path, mp3_path):
    try:
        ff(["-y", "-i", wav_path, "-codec:a", "libmp3lame", "-q:a", "2", mp3_path])
        return True
    except RuntimeError as e:
        print("mp3 encode skipped:", e); return False

def trim_silence(x):
    idx = np.where(np.abs(x) > TRIM_THRESH)[0]
    if len(idx) == 0: return x
    a, b = max(0, idx[0] - int(PAD*SR)), min(len(x), idx[-1] + int(PAD*SR))
    return x[a:b]

def atempo(x, factor):
    wp = "/tmp/_tmpo.wav"; op = "/tmp/_tmpo_out.wav"
    encode_wav(wp, np.stack([x, x], 1))
    ff(["-y", "-i", wp, "-filter:a", f"atempo={factor:.4f}", op])
    with wave.open(op) as w:
        n = w.getnframes(); raw = w.readframes(n)
    y = np.frombuffer(raw, dtype="<i2").astype(np.float32) / 32767.0
    return y[::2] if w.getnchannels() == 2 else y

# ---------------------------------------------------------------- SFX synthesis
def env_adsr(n, a, d, sustain, r):
    e = np.ones(n) * sustain
    ai, di, ri = int(a*SR), int(d*SR), int(r*SR)
    if ai: e[:ai] = np.linspace(0, sustain, ai)
    if di: e[ai:ai+di] = np.linspace(sustain, sustain*0.6, di)
    if ri: e[-ri:] *= np.linspace(1, 0, ri)
    return e

def bandpass(x, lo, hi):
    X = np.fft.rfft(x)
    f = np.fft.rfftfreq(len(x), 1/SR)
    m = np.exp(-0.5*((f-(lo+hi)/2)/((hi-lo)/2 or 1))**2)
    return np.fft.irfft(X*m, len(x)).astype(np.float32)

def into(buf, sig, t0, gain=1.0, pan=0.0):
    i0 = int(t0*SR); n = len(sig)
    if i0 >= len(buf): return
    n = min(n, len(buf)-i0)
    gl, gr = gain*np.cos((pan+1)*np.pi/4), gain*np.sin((pan+1)*np.pi/4)
    buf[i0:i0+n, 0] += sig[:n]*gl
    buf[i0:i0+n, 1] += sig[:n]*gr

def whoosh(dur, lo, hi, swell=True):
    n = int(dur*SR)
    x = np.random.default_rng(7).standard_normal(n).astype(np.float32)
    x = bandpass(x, lo, hi)
    e = np.sin(np.linspace(0, np.pi, n))**1.5 if swell else np.linspace(1, 0, n)**2
    return x*e

def impact(dur=1.5, f0=54, f1=26, gain=1.0):
    n = int(dur*SR); t = np.arange(n)/SR
    f = f0*(f1/f0)**(t/dur)
    ph = 2*np.pi*np.cumsum(f)/SR
    body = np.sin(ph)*np.exp(-t*3.2)
    rng = np.random.default_rng(3)
    tick = bandpass(rng.standard_normal(n).astype(np.float32), 60, 240)*np.exp(-t*22)*0.7
    return ((body+tick)*gain).astype(np.float32)

def subdrop(dur=2.6, f0=42, f1=24):
    n = int(dur*SR); t = np.arange(n)/SR
    f = f0*(f1/f0)**(t/dur)
    return (np.sin(2*np.pi*np.cumsum(f)/SR)*np.exp(-t*1.6)).astype(np.float32)

def riser(dur=3.6):
    n = int(dur*SR)
    x = np.random.default_rng(11).standard_normal(n).astype(np.float32)
    x = bandpass(x, 200, 2400)
    return (x*np.linspace(0.05, 1.0, n)**2 * np.sin(np.linspace(0, np.pi/2, n))).astype(np.float32)

def build_bed():
    N = int(TOTAL*SR)
    bed = np.zeros((N, 2), np.float32)
    rng = np.random.default_rng(21)
    air = bandpass(rng.standard_normal(N).astype(np.float32), 40, 320)*0.045
    air[:int(1.0*SR)] *= np.linspace(0, 1, int(1.0*SR))
    air[-int(1.2*SR):] *= np.linspace(1, 0, int(1.2*SR))
    bed[:, 0] += air; bed[:, 1] += air*0.95
    # 00:00 deep bass impact + cinematic whoosh out of darkness
    into(bed, impact(1.6, 56, 26, 0.95), 0.08)
    into(bed, whoosh(1.4, 120, 900), 0.00, 0.50)
    # 00:03 subtle rising tension + soft texture
    into(bed, riser(3.7), 3.20, 0.22)
    into(bed, whoosh(2.2, 80, 500, swell=False), 3.30, 0.18, pan=-0.4)
    # 00:07 atmospheric sweep + deep air whoosh (panned L->R)
    into(bed, whoosh(1.0, 150, 1400), 6.88, 0.50, pan=-0.7)
    into(bed, whoosh(1.2, 60, 400), 7.05, 0.42, pan=0.7)
    into(bed, impact(1.1, 48, 30, 0.35), 7.10)
    # 00:11 low cinematic hit + dark reverb impact
    into(bed, impact(1.5, 50, 28, 0.75), 10.90)
    into(bed, whoosh(1.6, 70, 350, swell=False), 10.95, 0.30)
    # 00:14 strong cinematic impact + deep bass drop (no music) + air swell out
    into(bed, impact(2.0, 58, 24, 1.00), 14.05)
    into(bed, subdrop(2.8), 14.12, 0.80)
    sw = whoosh(3.2, 90, 600); into(bed, sw, 14.20, 0.20)
    bed[-int(1.0*SR):] *= np.linspace(1, 0, int(1.0*SR))[:, None]
    return bed

# ---------------------------------------------------------------- assemble
def main():
    N = int(TOTAL*SR)
    vo_buf = np.zeros((N, 2), np.float32)
    report = []
    clips = []
    for fname, t0, t1 in SEGMENTS:
        x = decode_mono(os.path.join(ROOT, "audio", fname))
        clips.append((fname, trim_silence(x)))
    # continuous flow: ONE uniform gentle tempo for all lines (no per-line jumps)
    GAP = 0.12
    t_first, t_last = 0.10, 17.85
    total = sum(len(x) for _, x in clips) / SR
    avail = (t_last - t_first) - GAP * (len(clips) - 1)
    tempo = min(MAX_TEMPO, max(1.0, total / avail))
    if tempo > 1.0005:
        clips = [(f, atempo(x, tempo)) for f, x in clips]
    t = t_first
    for fname, x in clips:
        n = min(len(x), N - int(t*SR))
        i0 = int(t*SR)
        vo_buf[i0:i0+n, 0] += x[:n]; vo_buf[i0:i0+n, 1] += x[:n]
        report.append({"file": fname, "start": round(t, 3),
                       "end": round(t + n/SR, 3), "tempo": round(tempo, 3)})
        print(f"{fname}: placed {t:.2f}->{t+n/SR:.2f}s uniform tempo={tempo:.3f}")
        t += n/SR + GAP
    print(f"VO stream ends at {t-GAP:.2f}s of {TOTAL}s")

    def peak_norm(b, db):
        p = np.abs(b).max() or 1.0
        return b * (10**(db/20)/p)

    vo_only = peak_norm(vo_buf, -1.5)
    bed = build_bed()
    bed = peak_norm(bed, -12.0)          # bed sits ~10 dB under VO
    mix = peak_norm(vo_buf*0.92 + bed, -1.0)

    os.makedirs(os.path.join(ROOT, "audio"), exist_ok=True)
    os.makedirs(os.path.join(ROOT, "subtitles"), exist_ok=True)
    encode_wav(os.path.join(ROOT, "audio", "voiceover_18s_clean.wav"), vo_only)
    encode_wav(os.path.join(ROOT, "audio", "sfx_bed_18s.wav"), bed)
    encode_wav(os.path.join(ROOT, "audio", "voiceover_18s_with_sfx.wav"), mix)
    for stem in ("voiceover_18s_clean", "voiceover_18s_with_sfx"):
        encode_mp3(os.path.join(ROOT, "audio", stem + ".wav"),
                   os.path.join(ROOT, "audio", stem + ".mp3"))

    # SRT from measured in/out
    texts = ["هل تعلم، أن المغرب من أقدم دول العالم العربي؟",
             "دولة بهوية لم تنقطع منذ اثني عشر قرناً.",
             "عند نقطة التقاء العالم العربي، وأفريقيا، وأوروبا.",
             "لهذا كان بوابة الحضارات عبر التاريخ.",
             "بل وأكثر... للمغرب مكانة خاصة جداً في التاريخ."]
    def ts(s):
        ms = int(round(s*1000)); h, ms = divmod(ms, 3600000); m, ms = divmod(ms, 60000)
        s, ms = divmod(ms, 1000); return f"{h:02d}:{m:02d}:{s:02d},{ms:03d}"
    lines = []
    for i, (r, t) in enumerate(zip(report, texts), 1):
        lines += [str(i), f"{ts(r['start'])} --> {ts(min(r['end'], TOTAL))}", t, ""]
    with open(os.path.join(ROOT, "subtitles", "captions_ar.srt"), "w", encoding="utf-8") as f:
        f.write("\n".join(lines))
    with open(os.path.join(ROOT, "build", "vo_report.json"), "w") as f:
        json.dump(report, f, ensure_ascii=False, indent=2)
    print("OK: audio + srt written")

if __name__ == "__main__":
    main()

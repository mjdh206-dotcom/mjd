# -*- coding: utf-8 -*-
"""Build the vertical 12s ocean-secrets reel: Ken-Burns shots, cross dissolves,
marine-snow overlay, Arabic title/captions, full SFX mix (no music)."""
import os, re, subprocess, json, math, wave, sys

FF = open("/home/user/.local/ffmpeg_path.txt").read().strip()
REPO = "/home/user/mjd"
ASSETS = f"{REPO}/assets"
BUILD = f"{REPO}/build"
OUT = f"{REPO}/ocean_secrets_reel.mp4"

W, H, FPS = 1080, 1920, 30
TOTAL = 12.0
XF = 0.7                                     # cross-dissolve length
SR = 48000

def run(args, label=""):
    print(f"  -> {label} " + " ".join(args)[:160] + " ...")
    p = subprocess.run([FF, "-y", "-hide_banner", "-loglevel", "error"] + args,
                       capture_output=True, text=True)
    if p.returncode != 0:
        print(p.stderr[-4000:])
        raise SystemExit(f"ffmpeg failed: {label}")
    return p


# ============================================================== 1. video shots
SHOTS = [
    dict(img="shot1_surface.jpg",   dur=3.55, z0=1.00, z1=1.11, pan="up"),
    dict(img="shot2_jellyfish.jpg", dur=3.35, z0=1.15, z1=1.00, pan="centre"),
    dict(img="shot3_squid.jpg",     dur=3.45, z0=1.02, z1=1.13, pan="left"),
    dict(img="shot4_vents.jpg",     dur=4.10, z0=1.03, z1=1.14, pan="up"),
]
XFADES = [2.85, 5.50, 8.25]                  # transition offsets


def shot_args(i, s):
    n = max(1, round(s["dur"] * FPS) - 1)
    z0, z1 = s["z0"], s["z1"]
    z = f"{z0}+({z1 - z0})*on/{n}"
    x = f"iw/2-(iw/zoom)/2"
    y = f"ih/2-(ih/zoom)/2"
    if s["pan"] == "left":
        x = f"iw/2-(iw/zoom)/2-(iw*0.035)*on/{n}"
    elif s["pan"] == "up":
        y = f"ih/2-(ih/zoom)/2+(ih*0.030)*on/{n}"
    vf = (
        "scale=2160:3840:force_original_aspect_ratio=increase,crop=2160:3840,"
        f"zoompan=z='{z}':x='{x}':y='{y}':d=1:s={W}x{H}:fps={FPS},"
        "eq=contrast=1.07:saturation=1.12:brightness=-0.016:gamma=0.99,"
        "vignette=angle=PI/4.8,format=yuv420p,setsar=1"
    )
    return ["-loop", "1", "-framerate", str(FPS), "-t", f"{s['dur']:.3f}",
            "-i", f"{ASSETS}/{s['img']}"], vf


def build_shots():
    args, filters, labels = [], [], []
    for i, s in enumerate(SHOTS):
        a, vf = shot_args(i, s)
        args += a
        filters.append(f"[{i}:v]{vf}[s{i}]")
        labels.append(f"s{i}")
    # cross dissolves
    cur = labels[0]
    for k, off in enumerate(XFADES):
        nxt = labels[k + 1]
        out = f"x{k}"
        filters.append(f"[{cur}][{nxt}]xfade=transition=fade:duration={XF}:"
                       f"offset={off}[{out}]")
        cur = out
    filters.append(f"[{cur}]format=yuv420p[vout]")
    args += ["-filter_complex", ";".join(filters), "-map", "[vout]",
             "-c:v", "libx264", "-preset", "slow", "-crf", "14",
             "-pix_fmt", "yuv420p", "-r", str(FPS), f"{BUILD}/base.mp4"]
    run(args, "base video (4 shots + dissolves)")


# ============================================================ 2. overlays pass
def fade_window(name, start, end, d_in=0.45, d_out=0.5):
    return (f"[{name}]format=rgba,"
            f"fade=t=in:st={start}:d={d_in}:alpha=1,"
            f"fade=t=out:st={end - d_out:.2f}:d={d_out}:alpha=1")


def build_overlay():
    args = ["-i", f"{BUILD}/base.mp4",
            "-loop", "1", "-t", "13", "-i", f"{BUILD}/particles.png"]
    ovl = [("title_card.png", 0.10, 2.15),
           ("watermark.png",  2.30, 12.05),
           ("cap1.png",       2.30, 5.15),
           ("cap2.png",       5.15, 8.70),
           ("cap3.png",       8.70, 12.05)]
    for n, _, _ in ovl:
        args += ["-loop", "1", "-t", str(TOTAL), "-i", f"{BUILD}/{n}"]

    f = ["[0:v]format=rgba[base]",
         "[1:v]format=rgba,colorchannelmixer=aa=0.60,scale=1080:2160[pt]",
         "[base][pt]overlay=x='5*sin(t*0.42)':y='-215+190*(t/12)':"
         "shortest=1[bg]"]
    cur = "bg"
    for idx, (name, s, e) in enumerate(ovl):
        filt = fade_window(f"{idx + 2}:v", s, e)
        out = f"o{idx}"
        f.append(f"{filt}[t{idx}]")
        f.append(f"[{cur}][t{idx}]overlay=0:0:eof_action=pass[{out}]")
        cur = out
    f.append(f"[{cur}]format=yuv420p,fade=t=out:st=11.52:d=0.48[vout]")
    args += ["-filter_complex", ";".join(f),
             "-map", "[vout]", "-c:v", "libx264", "-preset", "slow",
             "-crf", "16", "-pix_fmt", "yuv420p", "-r", str(FPS),
             "-frames:v", str(int(TOTAL * FPS) + 1), f"{BUILD}/video_only.mp4"]
    run(args, "particles + Arabic text overlays")


# ================================================================= 3. audio
def probe_silence(path):
    p = subprocess.run([FF, "-hide_banner", "-i", path, "-af",
                        "silencedetect=noise=-40dB:d=0.12", "-f", "null", "-"],
                       capture_output=True, text=True)
    txt = p.stderr
    dur = float(re.search(r"Duration: (\d+):(\d+):([\d.]+)", txt).group(1)) * 3600 \
        + float(re.search(r"Duration: (\d+):(\d+):([\d.]+)", txt).group(2)) * 60 \
        + float(re.search(r"Duration: (\d+):(\d+):([\d.]+)", txt).group(3))
    starts = [float(m) for m in re.findall(r"silence_start: ([\d.]+)", txt)]
    ends = [float(m) for m in re.findall(r"silence_end: ([\d.]+)", txt)]
    s0 = ends[0] if starts and starts[0] == 0 and ends else 0.0
    s1 = starts[-1] if starts and starts[-1] > s0 else dur
    return dur, s0, s1


def prep_vo(atempo):
    """trim silence, normalise, speed up. returns [(path, speech_start, dur, speech_end)]"""
    out = []
    for i in (1, 2, 3):
        src = f"{ASSETS}/vo_{i}.mp3"
        dur, s0, s1 = probe_silence(src)
        a, b = max(0.0, s0 - 0.06), min(dur, s1 + 0.10)
        dst = f"{BUILD}/vo{i}_p.wav"
        af = (f"asetpts=PTS-STARTPTS,atrim=start={a:.3f}:end={b:.3f},"
              f"asetpts=PTS-STARTPTS,atempo={atempo},"
              "highpass=f=80,lowpass=f=11500,"
              "acompressor=threshold=0.13:ratio=2.6:attack=6:release=160:makeup=3,"
              "loudnorm=I=-17:TP=-2.0:LRA=9")
        run(["-i", src, "-af", af, "-ar", str(SR), "-ac", "1",
             "-c:a", "pcm_s16le", dst], f"prep VO{i}")
        nd = (b - a) / atempo
        sp = min(0.06 / atempo, nd)
        out.append((dst, sp, nd, nd))
    return out


def build_audio():
    atempo = 1.06
    while True:
        vos = prep_vo(atempo)
        LEAD, GAP = 0.35, 0.30
        t = LEAD
        plan = []
        for (p_, sp, nd, _) in vos:
            plan.append((p_, t, nd, t + sp))
            t += nd + GAP
        end = t - GAP
        if end <= TOTAL - 0.35 or atempo >= 1.14:
            break
        atempo = round(atempo + 0.02, 2)
    print(f"  voice-over speed x{atempo}, ends at {end:.2f}s "
          f"(speech {plan[0][3]:.2f} / {plan[1][3]:.2f} / {plan[2][3]:.2f})")

    # ---- audio graph ------------------------------------------------------
    ins = [f"{BUILD}/sfx_ambience.wav"]            # 0
    for (p_, _, _, _) in plan:
        ins.append(p_)                             # 1..3
    sfx_names = ["sfx_boom.wav", "sfx_boom2.wav", "sfx_whoosh1.wav",
                 "sfx_whoosh2.wav", "sfx_whoosh3.wav", "sfx_bubbles.wav",
                 "sfx_sonar.wav", "sfx_riser.wav"]
    for n in sfx_names:
        ins.append(f"{BUILD}/{n}")
    # index map
    I = {n: 4 + i for i, n in enumerate(sfx_names)}
    I["amb"] = 0
    I["vo1"], I["vo2"], I["vo3"] = 1, 2, 3

    # placement (seconds)
    T = XFADES
    at = {
        "sfx_boom.wav":    0.12,
        "sfx_whoosh1.wav": T[0] - 0.55,
        "sfx_boom2.wav":   T[2] + 0.05,
        "sfx_whoosh2.wav": T[1] - 0.55,
        "sfx_whoosh3.wav": T[2] - 0.50,
        "sfx_sonar.wav":   4.35,
        "sfx_riser.wav":   T[2] - 1.30,
        "sfx_bubbles.wav": 0.00,
    }
    gain = {
        "sfx_boom.wav": -12.0, "sfx_boom2.wav": -7.5,
        "sfx_whoosh1.wav": -15.0, "sfx_whoosh2.wav": -15.0,
        "sfx_whoosh3.wav": -13.0, "sfx_bubbles.wav": -27.0,
        "sfx_sonar.wav": -25.0, "sfx_riser.wav": -19.0,
    }
    # extras: boom on each of the first two cuts
    extra = [("sfx_boom.wav", T[0] - 0.05), ("sfx_boom.wav", T[1] - 0.02),
             ("sfx_boom.wav", 11.40)]

    f = []
    # ambience (looped/trimmed to the reel length)
    f.append(f"[{I['amb']}:a]atrim=0:{TOTAL},asetpts=PTS-STARTPTS,"
             f"volume=-21dB,aformat=sample_rates={SR}:channel_layouts=mono[amb]")
    # voice-over
    for k, (p_, st, nd, _) in enumerate(plan, start=1):
        f.append(f"[{I[f'vo{k}']}:a]adelay={int(st * 1000)}:all=1,"
                 f"volume=1.0,aformat=sample_rates={SR}:channel_layouts=mono"
                 f"[v{k}]")
    f.append("[v1][v2][v3]amix=inputs=3:normalize=0:duration=longest[vo]")
    # duck the ambience under the voice (gentle side-chain)
    f.append("[vo]asplit=2[vomix][vosc]")
    f.append("[amb][vosc]sidechaincompress=threshold=0.045:ratio=4:attack=25:"
             "release=520:makeup=1:level_sc=1.4[ambd]")
    # sfx
    parts = ["[ambd]", "[vomix]"]
    for name, st_i in list(at.items()):
        idx = I[name]
        g = gain[name]
        if name == "sfx_bubbles.wav":
            f.append(f"[{idx}:a]atrim=0:{TOTAL},asetpts=PTS-STARTPTS,"
                     f"volume={g}dB,aformat=sample_rates={SR}:"
                     f"channel_layouts=mono[b_{idx}]")
        else:
            f.append(f"[{idx}:a]adelay={int(st_i * 1000)}:all=1,volume={g}dB,"
                     f"aformat=sample_rates={SR}:channel_layouts=mono[b_{idx}]")
        parts.append(f"[b_{idx}]")
    for j, (name, st_i) in enumerate(extra):
        idx = I[name]
        f.append(f"[{idx}:a]adelay={int(st_i * 1000)}:all=1,"
                 f"volume={gain[name] + (1.5 if j < 2 else -2.0)}dB,"
                 f"aformat=sample_rates={SR}:"
                 f"channel_layouts=mono[e{j}]")
        parts.append(f"[e{j}]")
    n = len(parts)
    f.append(f"{''.join(parts)}amix=inputs={n}:normalize=0:"
             f"duration=longest:dropout_transition=0[mix]")
    f.append("[mix]alimiter=limit=0.95:attack=5:release=60,"
             "loudnorm=I=-14:TP=-1.5:LRA=11:linear=true,"
             "alimiter=limit=0.97,pan=stereo|c0=c0|c1=c0,"
             f"afade=t=in:st=0:d=0.25,afade=t=out:st={TOTAL - 0.45}:d=0.45,"
             f"atrim=0:{TOTAL},asetpts=PTS-STARTPTS[aout]")

    args = []
    for p_ in ins:
        args += ["-i", p_]
    args += ["-filter_complex", ";".join(f), "-map", "[aout]",
             "-ar", str(SR), "-ac", "2", "-c:a", "pcm_s16le",
             f"{BUILD}/mix.wav"]
    run(args, "audio mix (VO + SFX + ambience)")


# =================================================================== 4. mux
def mux():
    run(["-i", f"{BUILD}/video_only.mp4", "-i", f"{BUILD}/mix.wav",
         "-map", "0:v:0", "-map", "1:a:0", "-c:v", "copy",
         "-c:a", "aac", "-b:a", "192k", "-ar", "48000", "-ac", "2",
         "-movflags", "+faststart", "-shortest", OUT], "final mux")


if __name__ == "__main__":
    only = sys.argv[1] if len(sys.argv) > 1 else "all"
    if only in ("all", "video"):
        build_shots()
        build_overlay()
    if only in ("all", "audio"):
        build_audio()
    if only in ("all", "mux"):
        mux()
    print("DONE ->", OUT)

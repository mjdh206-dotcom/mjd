#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""يبني فيديو عمودي 9:16 عن أغرب أسماك أعماق المحيطات."""
import json, os, re, subprocess, sys

BASE = os.path.dirname(os.path.abspath(__file__))
FF = subprocess.run(
    ["python3", "-c", "import imageio_ffmpeg; print(imageio_ffmpeg.get_ffmpeg_exe())"],
    capture_output=True, text=True).stdout.strip()
FPS = 30
W, H = 1080, 1920

# (صورة, عنوان رئيسي, سطر معلومات, مسار الصوت)
SCENES = [
    ("img0_title.png",   "أغرب أسماك أعماق المحيطات", "رحلةٌ إلى العالم المظلم حيث يبدأ المجهول", "audio/sc0.mp3"),
    ("img1_anglerfish.png", "سمكة الصياد", "مصباحٌ حيوي على رأسها يجتذب الفرائس إلى أنيابها", "audio/sc1.mp3"),
    ("img2_blobfish.png",   "سمكة البليب فيش", "جسدٌ هلامي بلا عظام تحت ضغط الأعماق الهائل", "audio/sc2.mp3"),
    ("img3_gulper.png",     "أنقليس الجالبر", "فمٌ ضخم يبتلع فرائسًا أكبر من جسده", "audio/sc3.mp3"),
    ("img4_barreleye.png",  "سمكة الرأس الشفاف", "رأسٌ زجاجي تكشفه عينان خضراوان تتطلعان للأعلى", "audio/sc4.mp3"),
    ("img5_vampire.png",    "حبارة فامبير", "سحابةُ مخاطٍ متوهج لترويض أعدائها في العتمة", "audio/sc5.mp3"),
    ("img6_oarfish.png",    "سمكة المجذاف", "أطول سمكةٍ على الأرض.. أحدَ عشرَ مترًا من الغموض", "audio/sc6.mp3"),
    ("img0_title.png",      "عالمٌ لا يزال غامضًا", "أعجبك الفيديو؟ تابعنا لمزيد من غرائب البحار", "audio/sc7.mp3"),
]


def audio_duration(path):
    out = subprocess.run([FF, "-hide_banner", "-i", path],
                         capture_output=True, text=True).stderr
    m = re.search(r"Duration: (\d+):(\d+):(\d+\.\d+)", out)
    h, mm, s = m.groups()
    return int(h) * 3600 + int(mm) * 60 + float(s)


def ts(sec):
    sec = max(0.0, sec)
    h = int(sec // 3600); m = int((sec % 3600) // 60); s = sec % 60
    return f"{h}:{m:02d}:{s:05.2f}"


def main():
    os.chdir(BASE)
    durs, t = [], 0.0
    for _, _, _, ap in SCENES:
        d = audio_duration(ap) / 1.18          # تسريع طفيف لوتيرة وثائقية
        durs.append(max(5.2, round(d + 1.15, 2)))
        t += durs[-1]
    starts = [sum(durs[:i]) for i in range(len(durs))]
    total = round(sum(durs), 2)
    print(f"scenes={len(SCENES)} total={total}s durs={durs}")

    # ---------- ملف الترجمة ASS ----------
    C_TITLE = "&H00F5E97F"   # فيروزي فاتح
    ass = """[Script Info]
PlayResX: 1080
PlayResY: 1920
ScriptType: v4.00+
WrapStyle: 0
ScaledBorderAndShadow: yes
YCbCr Matrix: TV.709

[V4+ Styles]
Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
Style: Tag,Noto Kufi Arabic,42,&H00BFE8F5,&H000019FF,&H001E1E1E,&H96000000,-1,0,0,0,100,100,0,0,1,3,2,8,60,60,150,1
Style: Title,Noto Kufi Arabic,84,&H00FFFFFF,&H000019FF,&H00141414,&HA0000000,-1,0,0,0,100,100,0,0,1,5,3,2,60,60,640,1
Style: Sub,Noto Naskh Arabic,50,&H00E6F4F7,&H000019FF,&H00141414,&H96000000,0,0,0,0,100,100,0,0,1,3,2,2,60,60,540,1
Style: Name,Noto Kufi Arabic,76,&H00F5E97F,&H000019FF,&H00141414,&HA0000000,-1,0,0,0,100,100,0,0,1,4,3,2,60,60,470,1
Style: Fact,Noto Naskh Arabic,50,&H00F0F0F0,&H000019FF,&H00141414,&H96000000,0,0,0,0,100,100,0,0,1,3,2,2,60,60,385,1

[Events]
Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
"""
    for i, (_, title, fact, _) in enumerate(SCENES):
        st, d = starts[i], durs[i]
        if i == 0:
            ass += f"Dialogue: 0,{ts(st+0.2)},{ts(st+d-0.1)},Tag,,0,0,0,,{{\\fad(400,300)}}من أعماق المحيط\n"
            ass += f"Dialogue: 0,{ts(st+0.5)},{ts(st+d-0.1)},Title,,0,0,0,,{{\\fad(500,400)}}{title}\n"
            ass += f"Dialogue: 0,{ts(st+1.0)},{ts(st+d-0.1)},Sub,,0,0,0,,{{\\fad(500,400)}}{fact}\n"
        elif i == len(SCENES) - 1:
            ass += f"Dialogue: 0,{ts(st+0.4)},{ts(st+d-0.1)},Name,,0,0,0,,{{\\fad(400,300)}}{title}\n"
            ass += f"Dialogue: 0,{ts(st+0.9)},{ts(st+d-0.1)},Fact,,0,0,0,,{{\\fad(400,300)}}{fact}\n"
        else:
            ass += f"Dialogue: 0,{ts(st+0.35)},{ts(st+d-0.1)},Name,,0,0,0,,{{\\fad(350,250)}}{title}\n"
            ass += f"Dialogue: 0,{ts(st+0.85)},{ts(st+d-0.1)},Fact,,0,0,0,,{{\\fad(350,250)}}{fact}\n"
    with open("subs.ass", "w", encoding="utf-8") as f:
        f.write(ass)

    # ---------- رسم الفيديو ----------
    parts, labels = [], []
    for i, (img, _, _, _) in enumerate(SCENES):
        d = durs[i]
        n = int(round(d * FPS))
        zdir = 1 if i % 2 == 0 else -1
        if zdir == 1:   # تقريب
            z = f"1+0.13*on/{n}"
        else:           # إبعاد
            z = f"1.13-0.13*on/{n}"
        parts.append(
            f"[{i}:v]scale=1620:2880:force_original_aspect_ratio=increase:flags=lanczos,"
            f"crop=1620:2880,fps={FPS},"
            f"zoompan=z='{z}':x='iw/2-(iw/zoom/2)':y='ih/2-(ih/zoom/2)':d=1:s={W}x{H}:fps={FPS},"
            f"eq=contrast=1.06:saturation=1.13:brightness=0.010,"
            f"vignette=PI/4.8,trim=duration={d},setpts=PTS-STARTPTS[v{i}]"
        )
        labels.append(f"[v{i}]")

    # ---------- الصوت ----------
    ping_freqs = [620, 700, 660, 745, 680, 720, 640, 765]
    ping_terms = "+".join(
        f"0.5*gt(t\\,{starts[i]:.2f})*exp(-2.6*(t-{starts[i]:.2f}))*sin(2*PI*{ping_freqs[i]}*t)"
        for i in range(len(SCENES)))
    audio = []
    for i, (_, _, _, ap) in enumerate(SCENES):
        audio.append(f"[{len(SCENES)+i}:a]aresample=48000,adelay={int(starts[i]*1000)}:all=1[n{i}]")
    n_mix = "".join(f"[n{i}]" for i in range(len(SCENES)))
    audio.append(f"{n_mix}amix=inputs={len(SCENES)}:normalize=0,volume=2.4[narr]")
    audio.append(f"aevalsrc='0.55*sin(2*PI*47*t)+0.35*sin(2*PI*94.7*t)+0.22*sin(2*PI*141.3*t)*(0.45+0.55*sin(2*PI*0.08*t))':s=48000:d={total},lowpass=f=250,volume=0.55[drone]")
    audio.append(f"anoisesrc=color=brown:s=48000:r=48000:d={total},lowpass=f=430,volume=0.11[noise]")
    audio.append(f"aevalsrc='{ping_terms}':s=48000:d={total},lowpass=f=1800,volume=0.5[pings]")
    audio.append("[drone][noise][pings]amix=inputs=3:normalize=0,volume=0.42,"
                 f"afade=t=in:st=0:d=2.5,afade=t=out:st={total-2.5:.2f}:d=2.5[music]")
    audio.append("[narr][music]amix=inputs=2:normalize=0,alimiter=limit=0.92:level=false,"
                 f"atrim=0:{total},asetpts=PTS-STARTPTS[aout]")

    graph = ";".join(parts) + ";" + "".join(labels) + f"concat=n={len(SCENES)}:v=1:a=0[vc];" \
            "[vc]ass=subs.ass:fontsdir=fonts,fade=t=in:st=0:d=0.9," \
            f"fade=t=out:st={total-0.9:.2f}:d=0.9," \
            f"drawbox=x=0:y=1898:w='iw*t/{total}':h=7:color=0xF5E97F@0.85:t=fill," \
            "setsar=1,format=yuv420p[vout];" + ";".join(audio)

    with open("graph.txt", "w", encoding="utf-8") as f:
        f.write(graph)

    cmd = [FF, "-y", "-hide_banner", "-loglevel", "warning", "-stats"]
    for img, _, _, _ in SCENES:
        cmd += ["-loop", "1", "-framerate", str(FPS), "-i", img]
    for _, _, _, ap in SCENES:
        cmd += ["-i", ap]
    cmd += ["-filter_complex_script", "graph.txt", "-map", "[vout]", "-map", "[aout]",
            "-r", str(FPS), "-c:v", "libx264", "-preset", "medium", "-crf", "20",
            "-profile:v", "high", "-level", "4.0", "-pix_fmt", "yuv420p",
            "-c:a", "aac", "-b:a", "192k", "-ar", "48000", "-ac", "2",
            "-movflags", "+faststart", "-shortest", "out.mp4"]
    print(" ".join(cmd[:12]), "...")
    r = subprocess.run(cmd, cwd=BASE)
    print("ffmpeg exit:", r.returncode)
    sys.exit(r.returncode)


if __name__ == "__main__":
    main()

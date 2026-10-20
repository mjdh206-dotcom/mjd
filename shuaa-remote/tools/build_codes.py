#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
يبني مكتبة أكواد الريموت (assets/codes.json) من قاعدة Flipper-IRDB.
كل إشارة تُختصر إلى: البروتوكول:العنوان:الأمر  hex
"""
import os, re, json, sys, hashlib, unicodedata

SRC = sys.argv[1] if len(sys.argv) > 1 else "/tmp/irdb_full/Flipper-IRDB-main/TVs"
OUT = sys.argv[2] if len(sys.argv) > 2 else "app/src/main/assets/codes.json"

SUPPORTED = {"NEC", "NECext", "NEC42", "NEC42ext", "Samsung32", "SIRC", "SIRC15", "SIRC20",
             "RC5", "RC5X", "RC6", "RCA", "Kaseikyo"}

# مرادفات الأزرار -> المفتاح القياسي
ALIASES = {
    "power": ["power", "on_off", "onoff", "pwr", "power_toggle", "powertoggle", "tv_power", "standby", "on", "power_on_off"],
    "source": ["source", "input", "av", "tv_av", "input_source", "source_btn", "inputselect", "input_select", "hdmi"],
    "mute": ["mute", "muting", "sound_mute"],
    "vol_up": ["vol_up", "volup", "volume_up", "vol_plus", "vup", "vol+", "volumeup"],
    "vol_dn": ["vol_dn", "vol_dn", "voldown", "volume_down", "vol_down", "vol_minus", "vdown", "vol-", "volumedown"],
    "ch_up": ["ch_next", "ch_up", "channel_up", "chup", "ch+", "program_up", "pr_up", "prog_up", "channelup", "p_up"],
    "ch_dn": ["ch_prev", "ch_dn", "channel_down", "chdown", "ch-", "program_down", "pr_down", "prog_down", "channeldown", "p_down"],
    "up": ["up", "dpad_up", "arrow_up", "cursor_up", "ch_up_arrow"],
    "down": ["down", "dpad_down", "arrow_down", "cursor_down"],
    "left": ["left", "dpad_left", "arrow_left", "cursor_left"],
    "right": ["right", "dpad_right", "arrow_right", "cursor_right"],
    "ok": ["ok", "select", "enter", "dpad_center", "dpad_ok", "confirm"],
    "back": ["back", "return", "previous", "prev_menu"],
    "exit": ["exit", "cancel"],
    "menu": ["menu", "settings", "main_menu"],
    "home": ["home", "smarthub", "smart_hub", "smart", "apps", "smart_tv", "netflix_home"],
    "info": ["info", "display", "i", "status"],
    "guide": ["guide", "epg", "tv_guide"],
    "list": ["list", "ch_list", "channel_list"],
    "play": ["play", "play_pause", "playpause"],
    "pause": ["pause"],
    "stop": ["stop"],
    "rew": ["rew", "rewind", "fast_ba", "fast_backward", "fast_back", "<<"],
    "ffw": ["ffw", "fast_forward", "fast_fo", "fast_for", ">>"],
    "rec": ["rec", "record"],
    "next": ["next", "skip_next", "track_next", "ch_next_media"],
    "prev": ["prev", "skip_prev", "track_prev"],
    "red": ["red", "red_button", "f1"],
    "green": ["green", "green_button", "f2"],
    "yellow": ["yellow", "yellow_button", "f3"],
    "blue": ["blue", "blue_button", "f4"],
    "subtitle": ["subtitle", "subtitles", "cc", "closed_caption", "text", "teletext"],
    "aspect": ["aspect", "aspect_ratio", "format", "zoom", "screen_size", "p_size"],
    "audio": ["audio", "sound", "audio_mode", "sound_mode", "lang", "language"],
    "sleep": ["sleep", "sleep_timer", "timer"],
    "fav": ["fav", "favorite", "favourites", "favorites"],
    "pic": ["pic", "picture", "picture_mode", "image_mode"],
    "dash": ["dash", "-", "--", "dash_"],
    "key0": ["0", "num_0", "digit0", "key0", "button_0"],
    "key1": ["1", "num_1", "digit1", "key1", "button_1"],
    "key2": ["2", "num_2", "digit2", "key2", "button_2"],
    "key3": ["3", "num_3", "digit3", "key3", "button_3"],
    "key4": ["4", "num_4", "digit4", "key4", "button_4"],
    "key5": ["5", "num_5", "digit5", "key5", "button_5"],
    "key6": ["6", "num_6", "digit6", "key6", "button_6"],
    "key7": ["7", "num_7", "digit7", "key7", "button_7"],
    "key8": ["8", "num_8", "digit8", "key8", "button_8"],
    "key9": ["9", "num_9", "digit9", "key9", "button_9"],
}
LOOKUP = {}
for k, v in ALIASES.items():
    for a in v:
        LOOKUP[a] = k

BRAND_AR = {
 "AWA":"أوا","Akai":"أكاي","Amazon":"أمازون","AmazonBasics":"أمازون بيسيكس","Android":"أندرويد",
 "Apex":"أبيكس","BBK":"BBK","BGH":"BGH","Baird":"بيرد","Bauhn":"باوهن","Blaupunkt":"بلاوبونكت",
 "Blitzwolf":"بليتز وولف","Bolva":"بولفا","Bose":"بوز","Boulanger":"بولانجيه","Brandt":"براندت",
 "Bush":"بوش","CCE":"CCE","Cadillac":"كاديلاك","ContinentalEdison":"كونتيننتال إديسون",
 "Cranker":"كرانكر","Crown":"كراون","DYON":"دايون","Daewoo":"دايوو","Denver":"دنفر",
 "Devant":"ديفانت","Digihome":"ديجي هوم","Dual":"ديوال","DuraBrand":"ديورا براند",
 "Dynex":"داينكس","EdenWood":"إيدن وود","Element":"إلمنت","Elements":"إلمنتس","Elitelux":"إيليت لوكس",
 "Emerson":"إيمرسون","Enseo":"إنسيو","Erisson":"إيريسون","EssentielB":"إيسنسيل بي","FINLUX":"فينلوكس",
 "Fetch":"فيتش","Ffalcon":"فالكون","Funai":"فوناي","Furrion":"فوريون","GPX":"GPX","GVA":"GVA",
 "Gigabyte":"جيجابايت","Gogen":"جوجن","Grandin":"جراندين","Grundig":"جرونديج","GuestTek":"جست تك",
 "Haier":"هاير","Hisense":"هايسنس","Hitachi":"هيتاشي","Hotels":"فنادق","Insignia":"إنسيجنيا",
 "JVC":"JVC","Kendo":"كيندو","Kogan":"كوجان","Kraft":"كرافت","LG":"إل جي","MStar":"إم ستار",
 "Magnavox":"ماجنافوكس","Majestic":"ماجستيك","Medion":"ميديون","Metz":"ميتز","Midea":"ميديا",
 "Mitsubishi":"ميتسوبيشي","Muse":"ميوز","NEC":"إن إي سي","Nokia":"نوكيا","Norcent":"نورسنت",
 "Olevia":"أوليفيا","Onida":"أونيدا","Oppo":"أوبو","Orion":"أوريون","Panasonic":"باناسونيك",
 "Philco":"فيلكو","Philips":"فيليبس","Pioneer":"باينير","Polaroid":"بولارويد","Prima":"بريما",
 "Proscan":"بروسكان","RCA":"آر سي إيه","Realistic":"ريالستك","Roku":"روكو","SCEPTRE":"سيبتر",
 "Samsung":"سامسونج","Sansui":"سانسوي","Sanyo":"سانيو","Schneider":"شنايدر","Seiki":"سيكي",
 "Sharp":"شارب","Sherwood":"شيروود","Skyworth":"سكاي وورث","Sony":"سوني","Soundstream":"ساوند ستريم",
 "Sylvania":"سيلفانيا","Symphonic":"سيمفونيك","TCL":"تي سي إل","TEAC":"تيك","TVS":"TVS",
 "Tatung":"تاتونج","Technika":"تكنيكا","Technomate":"تكنومات","Teco":"تيكو","Telefunken":"تيليفونكن",
 "Thomson":"تومسون","Toshiba":"توشيبا","Vestel":"فيستل","Viore":"فيوري","Vizio":"فيزيو",
 "Walker":"ووكر","Westinghouse":"ويستنجهاوس","Xiaomi":"شاومي","iFFALCON":"أيفالكون",
 "Changhong":"تشانغهونغ","Konka":"كونكا","SkyWorth":"سكاي وورث","AOC":"إيه أو سي","Anker":"أنكر",
 "Aristona":"أريستونا","Asus":"أسوس","BPL":"BPL","BenQ":"بنكيو","Cat":"كات","ChanghongRubin":"تشانغهونغ",
 "Chimei":"شيمي","Clarion":"كلاريون","Compaq":"كومباك","Curtis":"كيرتس","Dell":"ديل","Denon":"دينون",
 "DirecTV":"ديركت تي في","Dish":"ديش","Dumont":"دومونت","Epson":"إبسون","Ferguson":"فيرجسون",
 "Fisher":"فيشر","Fujitsu":"فوجيتسو","Gateway":"جيتواي","General":"جنرال","Goodmans":"جودمانز",
 "Grundig_":"جرونديج","Harman":"هارمان","Hyundai":"هيونداي","ITT":"ITT","Inca":"إنكا","Kioto":"كيوتو",
 "Kolin":"كولين","Lenovo":"لينوفو","Luxor":"لوكسور","Manta":"مانتا","Marantz":"مارانتز","Metz_":"ميتز",
 "Minerva":"مينيرفا","NAD":"NAD","NEC_":"إن إي سي","Nova":"نوفا","Odys":"أوديس","Olive":"أوليف",
 "Orion_":"أوريون","Palsonic":"بالسونيك","Pathe":"باتيه","Pilot":"بايلوت","Princeton":"برينستون",
 "Relisys":"ريليسيس","Rowa":"روا","Saba":"سابا","Salora":"سالورا","Siemens":"سيمنس","Sinudyne":"سينوداين",
 "Skantic":"سكانتيك","Sogo":"سوجو","Tandberg":"تانبرج","Tatung_":"تاتونج","Teac":"تيك","Thorn":"ثورن",
 "Toshiba_":"توشيبا","Uniden":"يونيدن","Universum":"يونيفرسوم","Videocon":"فيديوكون","Watson":"واتسون",
 "Yamaha":"ياماها","Zenith":"زينيث","Andersson":"أندرسون","AudioSonic":"أوديو سونيك","Elbe":"إلبي",
 "F&U":"إف آند يو","Hanseatic":"هانزياتك","Hoher":"هوهر","Hyundai_":"هيونداي","Karcher":"كارشر",
 "Medion_":"ميديون","OK":"أو كي","Okano":"أوكانو","Peaq":"بيك","Reflexion":"ريفلكسيون",
 "Sunstech":"سنستك","Telefunken_":"تيليفونكن","Telestar":"تيليستار","Tevion":"تيفيون","Xoro":"زورو",
 "Yeti":"يتي","Zanussi":"زانوسي","Beovision":"بيو فيجن","BangOlufsen":"بانج أند أولفسن",
}

def clean(name):
    n = unicodedata.normalize("NFKD", name).lower()
    n = re.sub(r"[\s\-\.\(\)]+", "_", n.strip())
    return n

def canon_key(name):
    n = clean(name)
    if n in LOOKUP:
        return LOOKUP[n]
    # أرقام عربية/مفاتيح مثل "Key 1"
    m = re.fullmatch(r"(?:key|num|digit|button)_?([0-9])", n)
    if m:
        return "key" + m.group(1)
    n2 = n.replace("_button", "").replace("_key", "")
    if n2 in LOOKUP:
        return LOOKUP[n2]
    return None

BTN_ORDER = ["power","source","mute","vol_up","vol_dn","ch_up","ch_dn","up","down","left","right","ok",
             "back","exit","menu","home","info","guide","list","fav","play","pause","stop","rew","ffw",
             "rec","prev","next","red","green","yellow","blue","subtitle","aspect","audio","sleep","pic","dash",
             "key1","key2","key3","key4","key5","key6","key7","key8","key9","key0"]

REQUIRED = {"power"}
CORE = ["power","vol_up","vol_dn","ch_up","ch_dn","up","down","left","right","ok"]

def parse_file(path):
    """يرجع dict: key -> 'proto:addr:cmd'"""
    raw = open(path, "r", encoding="utf-8", errors="ignore").read()
    if "type: parsed" not in raw:
        return {}
    entries = []
    cur = {}
    for line in raw.splitlines():
        line = line.strip()
        if line.startswith("name:"):
            if cur:
                entries.append(cur)
            cur = {"name": line[5:].strip()}
        elif ":" in line:
            k, v = line.split(":", 1)
            k = k.strip(); v = v.strip()
            if k in ("protocol", "address", "command", "type"):
                cur[k] = v
    if cur:
        entries.append(cur)

    out = {}
    for e in entries:
        proto = e.get("protocol", "")
        if proto not in SUPPORTED:
            continue
        key = canon_key(e.get("name", ""))
        if not key or key in out:
            continue
        try:
            addr = "".join(e.get("address", "00 00 00 00").split())
            cmd = "".join(e.get("command", "00 00 00 00").split())
        except Exception:
            continue
        out[key] = f"{proto}:{addr}:{cmd}"
    return out

def score(codes):
    s = 0
    if "power" in codes: s += 100
    for k in CORE:
        if k in codes: s += 10
    s += len(codes)
    return s

def main():
    brands = {}
    total_files = 0
    for brand in sorted(os.listdir(SRC)):
        bpath = os.path.join(SRC, brand)
        if not os.path.isdir(bpath):
            continue
        files = []
        for f in os.listdir(bpath):
            if not f.lower().endswith(".ir"):
                continue
            if "raw" in f.lower():   # ملفات raw لا تدعمها المكتبة
                pass
            files.append(os.path.join(bpath, f))
        sets = []
        seen = set()
        for f in sorted(files):
            codes = parse_file(f)
            if not codes or "power" not in codes:
                continue
            total_files += 1
            sig = hashlib.md5(json.dumps(codes, sort_keys=True).encode()).hexdigest()
            if sig in seen:
                continue
            seen.add(sig)
            title = os.path.basename(f)[:-3].replace("_", " ").strip()
            sets.append((score(codes), title, codes))
        if not sets:
            continue
        sets.sort(key=lambda x: -x[0])
        # نُبقي أفضل ٦ مجموعات، مع تنويع البروتوكول
        keep = []
        proto_count = {}
        for sc, title, codes in sets:
            p = codes["power"].split(":")[0]
            if proto_count.get(p, 0) >= 3:
                continue
            proto_count[p] = proto_count.get(p, 0) + 1
            keep.append((title, codes))
            if len(keep) >= 6:
                break
        brands[brand] = keep

    data = {"version": 1, "source": "Flipper-IRDB (CC BY-SA 4.0)", "brands": []}
    for brand in sorted(brands):
        sets = brands[brand]
        data["brands"].append({
            "id": re.sub(r"[^a-z0-9]+", "-", brand.lower()).strip("-"),
            "en": brand,
            "ar": BRAND_AR.get(brand, brand),
            "sets": [{"name": t, "codes": dict(sorted(c.items(), key=lambda kv: BTN_ORDER.index(kv[0]) if kv[0] in BTN_ORDER else 99))}
                     for t, c in sets],
        })

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8") as fh:
        json.dump(data, fh, ensure_ascii=False, separators=(",", ":"))
    nb = len(data["brands"]); ns = sum(len(b["sets"]) for b in data["brands"])
    print(f"ملفات مقروءة: {total_files} | ماركات: {nb} | مجموعات: {ns} | حجم: {os.path.getsize(OUT)/1024:.0f} KB")

if __name__ == "__main__":
    main()

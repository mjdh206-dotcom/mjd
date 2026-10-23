#!/usr/bin/env python3
"""Live preview & direct-download server for syrian-arab-republic.mp4 (binds 0.0.0.0:8080)."""
from __future__ import annotations

import os
import re
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

ROOT = Path(__file__).resolve().parent
VIDEO_FILE = ROOT / "syrian-arab-republic.mp4"
GITHUB_RAW_URL = "https://github.com/mjdh206-dotcom/mjd/raw/arena/8b1e6140-mjd/syrian-arab-republic.mp4?download=1"

HTML_PAGE = f"""<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>سوريا — الجمهورية العربية السورية | معاينة وتحميل الفيديو</title>
  <style>
    :root {{
      --bg: #041913;
      --card: #082920;
      --gold: #f4cf76;
      --gold-dark: #c99635;
      --green: #007a3d;
      --text: #f6faf8;
      --muted: #b8d0c7;
    }}
    * {{ box-sizing: border-box; margin: 0; padding: 0; }}
    body {{
      font-family: system-ui, -apple-system, "Segoe UI", Tahoma, Arial, sans-serif;
      background: radial-gradient(circle at top, #0c3d2e 0%, var(--bg) 70%);
      color: var(--text);
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 24px 16px;
    }}
    .wrap {{
      width: 100%;
      max-width: 520px;
      background: rgba(8, 41, 32, 0.92);
      border: 1.5px solid rgba(244, 207, 118, 0.42);
      border-radius: 26px;
      padding: 22px;
      box-shadow: 0 24px 60px rgba(0, 0, 0, 0.55);
      text-align: center;
    }}
    .badge {{
      display: inline-block;
      background: rgba(0, 122, 61, 0.28);
      border: 1px solid rgba(244, 207, 118, 0.45);
      color: var(--gold);
      font-size: 0.85rem;
      font-weight: 700;
      padding: 6px 16px;
      border-radius: 999px;
      margin-bottom: 10px;
    }}
    h1 {{
      font-size: 1.55rem;
      color: var(--gold);
      margin-bottom: 6px;
    }}
    p.sub {{
      font-size: 0.95rem;
      color: var(--muted);
      margin-bottom: 16px;
      line-height: 1.5;
    }}
    .player-box {{
      width: 100%;
      max-width: 340px;
      margin: 0 auto 18px;
      border-radius: 18px;
      overflow: hidden;
      border: 2px solid rgba(244, 207, 118, 0.45);
      background: #000;
      aspect-ratio: 9 / 16;
    }}
    video {{
      width: 100%;
      height: 100%;
      display: block;
      object-fit: cover;
    }}
    .actions {{
      display: flex;
      flex-direction: column;
      gap: 10px;
    }}
    a.btn-primary {{
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      text-decoration: none;
      background: linear-gradient(135deg, var(--gold) 0%, var(--gold-dark) 100%);
      color: #062018;
      font-weight: 800;
      font-size: 1.05rem;
      padding: 14px 20px;
      border-radius: 14px;
      box-shadow: 0 8px 22px rgba(244, 207, 118, 0.25);
    }}
    a.btn-secondary {{
      display: inline-flex;
      align-items: center;
      justify-content: center;
      text-decoration: none;
      background: rgba(255, 255, 255, 0.07);
      border: 1px solid rgba(244, 207, 118, 0.35);
      color: var(--text);
      font-weight: 600;
      font-size: 0.92rem;
      padding: 11px 16px;
      border-radius: 12px;
    }}
    .specs {{
      margin-top: 14px;
      font-size: 0.82rem;
      color: var(--muted);
    }}
  </style>
</head>
<body>
  <div class="wrap">
    <span class="badge">الجمهورية العربية السورية • هوية وطن</span>
    <h1>سوريا — الجمهورية العربية السورية</h1>
    <p class="sub">فيديو قصير باللهجة السورية الشامية المفهومة مع مؤثرات صوتية سينمائية وعزف عود وقانون شامي (28.5 ثانية • 1080p HD)</p>
    <div class="player-box">
      <video controls playsinline preload="metadata" src="/video/syrian-arab-republic.mp4"></video>
    </div>
    <div class="actions">
      <a class="btn-primary" href="/download/syrian-arab-republic.mp4" download="syrian-arab-republic.mp4">
        ⬇ تحميل الفيديو مباشرة (MP4 — 23.4 MB)
      </a>
      <a class="btn-secondary" href="{GITHUB_RAW_URL}" target="_blank" rel="noopener">
        🔗 رابط التحميل المباشر الدائم عبر GitHub
      </a>
    </div>
    <div class="specs">الدقة: 1080×1920 (9:16) • 30 إطار/ثانية • المدة: 28.58 ثانية • صوت ستيريو 48kHz</div>
  </div>
</body>
</html>
"""


class VideoHandler(BaseHTTPRequestHandler):
    def log_message(self, fmt: str, *args) -> None:
        pass

    def _send_common_headers(self) -> None:
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Cache-Control", "no-cache")

    def do_HEAD(self) -> None:
        self._handle_request(send_body=False)

    def do_GET(self) -> None:
        self._handle_request(send_body=True)

    def _handle_request(self, send_body: bool) -> None:
        path = self.path.split("?", 1)[0]
        if path in ("/", "/index.html"):
            body = HTML_PAGE.encode("utf-8")
            self.send_response(HTTPStatus.OK)
            self._send_common_headers()
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            if send_body:
                self.wfile.write(body)
            return

        if path in ("/video/syrian-arab-republic.mp4", "/download/syrian-arab-republic.mp4", "/syrian-arab-republic.mp4"):
            if not VIDEO_FILE.exists():
                self.send_error(HTTPStatus.NOT_FOUND, "Video not found")
                return
            is_download = path.startswith("/download/")
            size = VIDEO_FILE.stat().st_size
            range_hdr = self.headers.get("Range")
            if range_hdr and not is_download:
                m = re.match(r"bytes=(\d+)-(\d*)", range_hdr)
                if m:
                    start = int(m.group(1))
                    end = int(m.group(2)) if m.group(2) else size - 1
                    end = min(end, size - 1)
                    if start < size and start <= end:
                        length = end - start + 1
                        self.send_response(HTTPStatus.PARTIAL_CONTENT)
                        self._send_common_headers()
                        self.send_header("Content-Type", "video/mp4")
                        self.send_header("Accept-Ranges", "bytes")
                        self.send_header("Content-Range", f"bytes {start}-{end}/{size}")
                        self.send_header("Content-Length", str(length))
                        self.end_headers()
                        if send_body:
                            with open(VIDEO_FILE, "rb") as f:
                                f.seek(start)
                                self.wfile.write(f.read(length))
                        return

            self.send_response(HTTPStatus.OK)
            self._send_common_headers()
            self.send_header("Content-Type", "video/mp4")
            self.send_header("Accept-Ranges", "bytes")
            self.send_header("Content-Length", str(size))
            if is_download:
                self.send_header("Content-Disposition", 'attachment; filename="syrian-arab-republic.mp4"')
            self.end_headers()
            if send_body:
                with open(VIDEO_FILE, "rb") as f:
                    while chunk := f.read(256 * 1024):
                        self.wfile.write(chunk)
            return

        self.send_error(HTTPStatus.NOT_FOUND, "Not Found")


if __name__ == "__main__":
    port = int(os.environ.get("PORT", "8080"))
    srv = ThreadingHTTPServer(("0.0.0.0", port), VideoHandler)
    print(f"Serving Syrian Arab Republic video preview & download on http://0.0.0.0:{port}", flush=True)
    srv.serve_forever()

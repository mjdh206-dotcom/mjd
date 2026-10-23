#!/usr/bin/env python3
"""Lightweight streaming + direct-download HTTP server for egypt-first-discovery.mp4.
Supports HTTP Range requests (for instant mobile/desktop playback) and forced
Content-Disposition attachment download on /download/egypt-first-discovery.mp4.
"""
from __future__ import annotations

import os
import re
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

ROOT = Path(__file__).resolve().parent
VIDEO_PATH = ROOT / "egypt-first-discovery.mp4"
PORT = int(os.environ.get("PORT", "8080"))

HTML_PAGE = """<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>أول اكتشاف أثري في مصر — تحميل الفيديو (1080p)</title>
  <style>
    :root {
      --bg: #09080d;
      --card: #13111b;
      --gold: #f5c451;
      --gold-bright: #ffe082;
      --text: #f7f6f2;
      --muted: #a9a5b8;
    }
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      min-height: 100vh;
      background: radial-gradient(circle at 50% 0%, #241b10 0%, var(--bg) 62%);
      color: var(--text);
      font-family: system-ui, -apple-system, "Segoe UI", Tahoma, Arial, sans-serif;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 24px 16px;
    }
    .wrap {
      max-width: 920px;
      width: 100%;
      display: grid;
      grid-template-columns: 340px 1fr;
      gap: 28px;
      background: rgba(19, 17, 27, 0.88);
      border: 1px solid rgba(245, 196, 81, 0.28);
      border-radius: 24px;
      padding: 24px;
      box-shadow: 0 24px 70px rgba(0, 0, 0, 0.65);
    }
    @media (max-width: 760px) {
      .wrap { grid-template-columns: 1fr; max-width: 430px; }
    }
    .phone {
      border-radius: 18px;
      overflow: hidden;
      border: 2px solid rgba(245, 196, 81, 0.45);
      background: #000;
      aspect-ratio: 9 / 16;
      box-shadow: 0 14px 40px rgba(0, 0, 0, 0.6);
    }
    video { width: 100%; height: 100%; object-fit: cover; display: block; }
    .info { display: flex; flex-direction: column; justify-content: center; gap: 16px; }
    .badge {
      display: inline-flex;
      align-self: flex-start;
      background: rgba(245, 196, 81, 0.14);
      color: var(--gold);
      border: 1px solid rgba(245, 196, 81, 0.4);
      border-radius: 999px;
      padding: 6px 14px;
      font-size: 0.86rem;
      font-weight: 700;
    }
    h1 { font-size: 1.75rem; line-height: 1.35; color: #fff; }
    h1 span { color: var(--gold); }
    p { color: var(--muted); line-height: 1.7; font-size: 0.98rem; }
    .specs {
      display: flex;
      flex-wrap: wrap;
      gap: 10px;
      margin: 4px 0;
    }
    .spec {
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid rgba(255, 255, 255, 0.09);
      padding: 7px 12px;
      border-radius: 10px;
      font-size: 0.85rem;
      color: #e4e1ee;
    }
    .btn-dl {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
      background: linear-gradient(135deg, var(--gold-bright), #d89b28);
      color: #120e05;
      font-weight: 800;
      font-size: 1.08rem;
      text-decoration: none;
      padding: 15px 24px;
      border-radius: 14px;
      margin-top: 6px;
      box-shadow: 0 10px 28px rgba(245, 196, 81, 0.28);
      transition: transform 0.15s ease, box-shadow 0.15s ease;
    }
    .btn-dl:hover {
      transform: translateY(-2px);
      box-shadow: 0 14px 34px rgba(245, 196, 81, 0.42);
    }
  </style>
</head>
<body>
  <div class="wrap">
    <div class="phone">
      <video src="/video/egypt-first-discovery.mp4" controls playsinline preload="metadata"></video>
    </div>
    <div class="info">
      <span class="badge">وثائقي عمودي احترافي • Reels / Shorts / TikTok</span>
      <h1>سرّ أول اكتشاف أثري في <span>مصر والتاريخ</span></h1>
      <p>
        قبل نابليون وحجر رشيد بثلاثة آلاف عام، قاد الأمير المصري <strong>خعمواست</strong> ابن رمسيس الثاني
        أول بعثة تنقيب وترميم أثري موثّقة في تاريخ البشرية في سقارة (1250 ق.م).
      </p>
      <div class="specs">
        <div class="spec">📐 1080×1920 (9:16 عمودي)</div>
        <div class="spec">🎞️ 30 FPS حركة ثلاثية الأبعاد</div>
        <div class="spec">⏱️ المدة: 30.00 ثانية</div>
        <div class="spec">🔊 تعليق صوتي + موسيقى ومؤثرات</div>
      </div>
      <a class="btn-dl" href="/download/egypt-first-discovery.mp4" download="egypt-first-discovery.mp4">
        ⬇ تحميل الفيديو مباشرة (MP4 • 1080p)
      </a>
    </div>
  </div>
</body>
</html>
"""


class VideoHandler(BaseHTTPRequestHandler):
    def log_message(self, fmt: str, *args) -> None:
        return

    def end_headers(self) -> None:
        self.send_header("Access-Control-Allow-Origin", "*")
        super().end_headers()

    def do_HEAD(self) -> None:
        self._serve(head_only=True)

    def do_GET(self) -> None:
        self._serve(head_only=False)

    def _serve(self, head_only: bool) -> None:
        path = self.path.split("?", 1)[0]
        if path in ("/", "/index.html"):
            body = HTML_PAGE.encode("utf-8")
            self.send_response(HTTPStatus.OK)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            if not head_only:
                self.wfile.write(body)
            return

        if path.startswith("/download/") or path.startswith("/video/") or path.endswith(".mp4"):
            if not VIDEO_PATH.exists():
                self.send_error(HTTPStatus.NOT_FOUND, "Video not found")
                return
            is_download = path.startswith("/download/")
            size = VIDEO_PATH.stat().st_size
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
                        self.send_header("Content-Type", "video/mp4")
                        self.send_header("Accept-Ranges", "bytes")
                        self.send_header("Content-Range", f"bytes {start}-{end}/{size}")
                        self.send_header("Content-Length", str(length))
                        self.end_headers()
                        if not head_only:
                            with VIDEO_PATH.open("rb") as f:
                                f.seek(start)
                                self.wfile.write(f.read(length))
                        return

            self.send_response(HTTPStatus.OK)
            self.send_header("Content-Type", "application/octet-stream" if is_download else "video/mp4")
            self.send_header("Accept-Ranges", "bytes")
            self.send_header("Content-Length", str(size))
            if is_download:
                self.send_header("Content-Disposition", 'attachment; filename="egypt-first-discovery.mp4"')
            self.end_headers()
            if not head_only:
                with VIDEO_PATH.open("rb") as f:
                    while chunk := f.read(1024 * 256):
                        self.wfile.write(chunk)
            return

        self.send_error(HTTPStatus.NOT_FOUND, "Not Found")


if __name__ == "__main__":
    server = ThreadingHTTPServer(("0.0.0.0", PORT), VideoHandler)
    print(f"Serving Egypt video portal on http://0.0.0.0:{PORT}", flush=True)
    server.serve_forever()

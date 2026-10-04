#!/usr/bin/env python3
"""خادم ملفات ثابت مع دعم طلبات Range لبثّ الفيديو وتحميله من أي متصفح.

التشغيل:  python3 serve.py [المنفذ]   (الافتراضي 8000)
"""
import os
import re
import sys
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer


class _RangeReader:
    """يقرأ عدداً محدوداً من البايتات فقط (لبثّ جزء من الملف)."""

    def __init__(self, fh, length):
        self.fh = fh
        self.left = length

    def read(self, n=-1):
        if self.left <= 0:
            return b""
        if n is None or n < 0:
            n = self.left
        data = self.fh.read(min(n, self.left))
        self.left -= len(data)
        return data

    def close(self):
        self.fh.close()


class RangeHandler(SimpleHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def send_head(self):
        path = self.translate_path(self.path)
        if os.path.isdir(path):
            return super().send_head()
        if not os.path.exists(path):
            self.send_error(404, "File not found")
            return None

        try:
            fh = open(path, "rb")
        except OSError:
            self.send_error(404, "File not found")
            return None

        size = os.fstat(fh.fileno()).st_size
        ctype = self.guess_type(path)
        rng = self.headers.get("Range")

        if rng:
            m = re.match(r"bytes=(\d*)-(\d*)\s*$", rng.strip())
            if m and (m.group(1) or m.group(2)):
                start = int(m.group(1)) if m.group(1) else max(
                    0, size - int(m.group(2)))
                end = int(m.group(2)) if m.group(1) else size - 1
                start = max(0, start)
                end = min(end, size - 1)
                if start > end:
                    fh.close()
                    self.send_response(416)
                    self.send_header("Content-Range", f"bytes */{size}")
                    self.send_header("Content-Length", "0")
                    self.end_headers()
                    return None
                self.send_response(206)
                self.send_header("Content-Type", ctype)
                self.send_header("Accept-Ranges", "bytes")
                self.send_header("Content-Range",
                                 f"bytes {start}-{end}/{size}")
                self.send_header("Content-Length", str(end - start + 1))
                self.end_headers()
                fh.seek(start)
                return _RangeReader(fh, end - start + 1)

        self.send_response(200)
        self.send_header("Content-Type", ctype)
        self.send_header("Accept-Ranges", "bytes")
        self.send_header("Content-Length", str(size))
        self.end_headers()
        return fh

    def log_message(self, fmt, *args):
        sys.stderr.write("%s - %s\n" % (self.address_string(), fmt % args))


if __name__ == "__main__":
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8000
    os.chdir(os.path.dirname(os.path.abspath(__file__)))
    srv = ThreadingHTTPServer(("0.0.0.0", port), RangeHandler)
    print(f"serving {os.getcwd()} on http://0.0.0.0:{port}", flush=True)
    srv.serve_forever()

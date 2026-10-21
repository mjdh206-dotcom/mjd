#!/usr/bin/env python3
import http.server
import os
import socketserver

PORT = 8080
REPO_ROOT = "/home/user/mjd"
PREVIEW_DIR = os.path.join(REPO_ROOT, "haris-wifi", "preview")


class HarisPreviewHandler(http.server.SimpleHTTPRequestHandler):
    def do_GET(self):
        if self.path in ("/", "/index.html"):
            self.send_file(os.path.join(PREVIEW_DIR, "index.html"), "text/html; charset=utf-8")
        elif self.path.startswith("/dist/haris-wifi-1.0.0-debug.apk"):
            apk_path = os.path.join(REPO_ROOT, "dist", "haris-wifi-1.0.0-debug.apk")
            self.send_file(
                apk_path,
                "application/vnd.android.package-archive",
                attachment_name="haris-wifi-1.0.0.apk",
            )
        else:
            self.send_file(os.path.join(PREVIEW_DIR, "index.html"), "text/html; charset=utf-8")

    def send_file(self, filepath, content_type, attachment_name=None):
        if not os.path.isfile(filepath):
            self.send_error(404, "File not found")
            return
        size = os.path.getsize(filepath)
        self.send_response(200)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(size))
        self.send_header("Access-Control-Allow-Origin", "*")
        if attachment_name:
            self.send_header("Content-Disposition", f'attachment; filename="{attachment_name}"')
        self.end_headers()
        with open(filepath, "rb") as f:
            while chunk := f.read(65536):
                self.wfile.write(chunk)


if __name__ == "__main__":
    socketserver.TCPServer.allow_reuse_address = True
    with socketserver.TCPServer(("0.0.0.0", PORT), HarisPreviewHandler) as httpd:
        print(f"Haris Wi-Fi Preview & APK server listening on 0.0.0.0:{PORT}", flush=True)
        httpd.serve_forever()

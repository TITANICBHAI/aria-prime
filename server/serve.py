"""
Aria-Prime documentation browser.

A small read-only web app that lets you explore the markdown documentation
and source tree of the aria-prime Android project from the Replit preview.

This project is an Android (Kotlin + Jetpack Compose + JNI llama.cpp) app,
so there is no production web frontend or backend. This server exists
solely to give the Replit workspace a visible preview that surfaces the
project's documentation and structure.
"""

from __future__ import annotations

import html
import json
import mimetypes
import os
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import unquote, urlparse

ROOT = Path(__file__).resolve().parent.parent
HOST = "0.0.0.0"
PORT = 5000

ALLOWED_EXTS = {
    ".md", ".txt", ".kt", ".kts", ".java", ".gradle", ".xml",
    ".cpp", ".h", ".hpp", ".c", ".cmake", ".pro", ".json",
    ".yml", ".yaml", ".toml", ".properties", ".sh", ".ts",
    ".tsx", ".js", ".cfg", ".ini", ".gitignore",
}

IGNORE_DIRS = {
    ".git", ".cache", ".local", ".agents", "node_modules",
    "build", ".gradle", "dist", "__pycache__",
}

PRIMARY_DOCS = [
    "README.md",
    "AGENT_INSTRUCTIONS.md",
    "ARCHITECTURE.md",
    "GAP_AUDIT.md",
    "DONOR_INVENTORY.md",
]


def safe_path(rel: str) -> Path | None:
    """Resolve `rel` against ROOT and refuse anything that escapes ROOT."""
    rel = unquote(rel).lstrip("/")
    candidate = (ROOT / rel).resolve()
    try:
        candidate.relative_to(ROOT)
    except ValueError:
        return None
    return candidate


def list_tree() -> list[dict]:
    """Return a sorted list of viewable files relative to ROOT."""
    entries: list[dict] = []
    for dirpath, dirnames, filenames in os.walk(ROOT):
        dirnames[:] = sorted(d for d in dirnames if d not in IGNORE_DIRS)
        for name in sorted(filenames):
            full = Path(dirpath) / name
            ext = full.suffix.lower()
            if ext not in ALLOWED_EXTS and name not in {"Dockerfile", "Makefile"}:
                continue
            try:
                size = full.stat().st_size
            except OSError:
                continue
            if size > 1_500_000:  # skip absurdly large files
                continue
            rel = str(full.relative_to(ROOT))
            entries.append({"path": rel, "size": size})
    return entries


INDEX_HTML = """<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>aria-prime — project browser</title>
  <link rel="preconnect" href="https://cdn.jsdelivr.net" />
  <script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>
  <script src="https://cdn.jsdelivr.net/npm/dompurify@3/dist/purify.min.js"></script>
  <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/highlight.js@11.9.0/styles/github-dark.min.css" />
  <script src="https://cdn.jsdelivr.net/npm/highlight.js@11.9.0/lib/common.min.js"></script>
  <style>
    :root {
      color-scheme: dark;
      --bg: #0d1117;
      --panel: #161b22;
      --border: #30363d;
      --text: #c9d1d9;
      --muted: #8b949e;
      --accent: #58a6ff;
      --accent-soft: #1f6feb33;
    }
    * { box-sizing: border-box; }
    html, body { margin: 0; padding: 0; height: 100%; background: var(--bg); color: var(--text); font: 14px/1.5 -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; }
    a { color: var(--accent); text-decoration: none; }
    a:hover { text-decoration: underline; }
    .layout { display: grid; grid-template-columns: 320px 1fr; height: 100vh; }
    aside { background: var(--panel); border-right: 1px solid var(--border); overflow-y: auto; }
    main { overflow-y: auto; }
    header { padding: 16px 20px; border-bottom: 1px solid var(--border); position: sticky; top: 0; background: var(--panel); z-index: 1; }
    header h1 { margin: 0 0 4px; font-size: 18px; }
    header p { margin: 0; color: var(--muted); font-size: 12px; }
    .filter { padding: 12px 16px; }
    .filter input { width: 100%; background: #0d1117; color: var(--text); border: 1px solid var(--border); border-radius: 6px; padding: 8px 10px; font-size: 13px; }
    .section-title { padding: 12px 16px 4px; font-size: 11px; text-transform: uppercase; letter-spacing: 0.08em; color: var(--muted); }
    nav ul { list-style: none; margin: 0; padding: 0 8px 24px; }
    nav li > a { display: block; padding: 6px 10px; border-radius: 6px; color: var(--text); font-size: 13px; word-break: break-all; }
    nav li > a:hover { background: #21262d; text-decoration: none; }
    nav li > a.active { background: var(--accent-soft); color: var(--accent); }
    .badge { float: right; font-size: 10px; color: var(--muted); margin-left: 6px; }
    .content { padding: 28px 36px 80px; max-width: 980px; margin: 0 auto; }
    .crumbs { color: var(--muted); font-size: 12px; margin-bottom: 12px; }
    .crumbs code { background: var(--panel); padding: 2px 6px; border-radius: 4px; }
    article.markdown h1, article.markdown h2, article.markdown h3 { border-bottom: 1px solid var(--border); padding-bottom: 6px; }
    article.markdown table { border-collapse: collapse; width: 100%; margin: 12px 0; }
    article.markdown th, article.markdown td { border: 1px solid var(--border); padding: 6px 10px; text-align: left; }
    article.markdown code { background: #161b22; padding: 1px 6px; border-radius: 4px; font-size: 12.5px; }
    article.markdown pre { background: #161b22; padding: 14px 16px; border-radius: 8px; overflow-x: auto; border: 1px solid var(--border); }
    article.markdown pre code { background: transparent; padding: 0; }
    pre.source { background: #161b22; border: 1px solid var(--border); border-radius: 8px; padding: 14px 16px; overflow-x: auto; font-size: 12.5px; }
    .empty { color: var(--muted); padding: 40px 0; text-align: center; }
    .pill { display: inline-block; padding: 2px 8px; background: var(--accent-soft); color: var(--accent); border-radius: 999px; font-size: 11px; margin-left: 8px; vertical-align: middle; }
  </style>
</head>
<body>
  <div class="layout">
    <aside>
      <header>
        <h1>aria-prime <span class="pill">Android</span></h1>
        <p>Documentation &amp; source browser</p>
      </header>
      <div class="filter"><input id="filter" placeholder="Filter files…" autocomplete="off" /></div>
      <div class="section-title">Primary docs</div>
      <nav><ul id="primary"></ul></nav>
      <div class="section-title">All files</div>
      <nav><ul id="files"></ul></nav>
    </aside>
    <main>
      <div class="content">
        <div class="crumbs" id="crumbs">Loading…</div>
        <div id="view"></div>
      </div>
    </main>
  </div>
  <script>
    const PRIMARY = __PRIMARY__;
    const view = document.getElementById('view');
    const crumbs = document.getElementById('crumbs');
    const filesEl = document.getElementById('files');
    const primaryEl = document.getElementById('primary');
    const filterEl = document.getElementById('filter');
    let allFiles = [];

    function escapeHtml(s) {
      return s.replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
    }

    function renderList(target, items, current) {
      target.innerHTML = '';
      for (const f of items) {
        const li = document.createElement('li');
        const a = document.createElement('a');
        a.href = '#' + encodeURIComponent(f.path);
        a.textContent = f.path;
        if (current === f.path) a.classList.add('active');
        const badge = document.createElement('span');
        badge.className = 'badge';
        badge.textContent = (f.size / 1024).toFixed(1) + ' KB';
        a.appendChild(badge);
        li.appendChild(a);
        target.appendChild(li);
      }
    }

    function applyFilter() {
      const q = filterEl.value.trim().toLowerCase();
      const filtered = q
        ? allFiles.filter(f => f.path.toLowerCase().includes(q))
        : allFiles;
      const current = decodeURIComponent(location.hash.slice(1));
      renderList(filesEl, filtered, current);
    }

    async function loadFiles() {
      const res = await fetch('/api/files');
      const data = await res.json();
      allFiles = data.files;
      const primaryItems = PRIMARY
        .map(p => allFiles.find(f => f.path === p))
        .filter(Boolean);
      renderList(primaryEl, primaryItems, decodeURIComponent(location.hash.slice(1)));
      applyFilter();
    }

    async function loadFile(path) {
      crumbs.innerHTML = 'Loading <code>' + escapeHtml(path) + '</code>…';
      view.innerHTML = '';
      try {
        const res = await fetch('/api/file?path=' + encodeURIComponent(path));
        if (!res.ok) throw new Error('HTTP ' + res.status);
        const text = await res.text();
        crumbs.innerHTML = '<code>' + escapeHtml(path) + '</code>';
        if (path.toLowerCase().endsWith('.md')) {
          const html = DOMPurify.sanitize(marked.parse(text));
          const article = document.createElement('article');
          article.className = 'markdown';
          article.innerHTML = html;
          view.appendChild(article);
        } else {
          const pre = document.createElement('pre');
          pre.className = 'source';
          const code = document.createElement('code');
          code.textContent = text;
          pre.appendChild(code);
          view.appendChild(pre);
          if (window.hljs) hljs.highlightElement(code);
        }
      } catch (e) {
        crumbs.textContent = 'Error';
        view.innerHTML = '<div class="empty">Could not load <code>' + escapeHtml(path) + '</code>: ' + escapeHtml(String(e)) + '</div>';
      }
    }

    function route() {
      const path = decodeURIComponent(location.hash.slice(1));
      if (path) {
        loadFile(path);
      } else {
        loadFile('README.md');
      }
      applyFilter();
      // refresh primary highlight
      const current = path || 'README.md';
      [...primaryEl.querySelectorAll('a')].forEach(a => {
        a.classList.toggle('active', decodeURIComponent(a.getAttribute('href').slice(1)) === current);
      });
    }

    filterEl.addEventListener('input', applyFilter);
    window.addEventListener('hashchange', route);
    loadFiles().then(route);
  </script>
</body>
</html>
"""


class Handler(BaseHTTPRequestHandler):
    server_version = "aria-prime-docs/1.0"

    def log_message(self, format: str, *args) -> None:
        # Concise single-line access log
        print(f"{self.address_string()} - {format % args}")

    def _send(self, status: int, body: bytes, content_type: str) -> None:
        self.send_response(status)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(body)

    def do_GET(self) -> None:  # noqa: N802
        parsed = urlparse(self.path)
        route = parsed.path

        if route == "/" or route == "/index.html":
            page = INDEX_HTML.replace("__PRIMARY__", json.dumps(PRIMARY_DOCS))
            self._send(200, page.encode("utf-8"), "text/html; charset=utf-8")
            return

        if route == "/api/files":
            payload = json.dumps({"files": list_tree()}).encode("utf-8")
            self._send(200, payload, "application/json")
            return

        if route == "/api/file":
            params = dict(p.split("=", 1) for p in parsed.query.split("&") if "=" in p)
            rel = params.get("path", "")
            target = safe_path(rel)
            if not target or not target.is_file():
                self._send(404, b"not found", "text/plain")
                return
            try:
                data = target.read_bytes()
            except OSError as e:
                self._send(500, str(e).encode("utf-8"), "text/plain")
                return
            self._send(200, data, "text/plain; charset=utf-8")
            return

        if route == "/healthz":
            self._send(200, b"ok", "text/plain")
            return

        self._send(404, b"not found", "text/plain")


def main() -> None:
    server = ThreadingHTTPServer((HOST, PORT), Handler)
    print(f"aria-prime docs server listening on http://{HOST}:{PORT}")
    print(f"serving from: {ROOT}")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("shutting down")
        server.server_close()


if __name__ == "__main__":
    mimetypes.add_type("text/markdown", ".md")
    main()

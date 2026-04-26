# aria-prime — Replit setup notes

## What this project is

`aria-prime` is an **Android** application (Kotlin + Jetpack Compose + JNI llama.cpp). The actual runtime target is a phone, not a web server. The `android/` tree is the live Kotlin spine, and `donors/` is read-only Java reference code from sibling repos waiting to be ported.

For the full project background read `README.md`, then `AGENT_INSTRUCTIONS.md`, `GAP_AUDIT.md`, `DONOR_INVENTORY.md`, and `ARCHITECTURE.md`.

## Why there is a Python server

The Replit workspace expects a foreground process bound to port 5000 so the preview pane has something to show. Since this project has no production web frontend or backend, `server/serve.py` runs a tiny read-only documentation/source browser:

- `GET /` — single-page UI that renders Markdown client-side and shows a sortable file tree.
- `GET /api/files` — JSON list of viewable files in the repo.
- `GET /api/file?path=…` — raw text of a single file (path is sandboxed to the repo root).
- `GET /healthz` — liveness probe used by the deployment.

Only documentation and source files are exposed; build outputs, caches, and `.local/` are filtered out. Files larger than ~1.5 MB are skipped.

## How to run it

The `Start application` workflow runs `python3 server/serve.py` and listens on `0.0.0.0:5000`. Restart it after changes to `server/serve.py`.

## Deployment

Configured as an `autoscale` deployment running `python3 server/serve.py`. The server is stateless and safe to autoscale.

## Building the actual Android app

The Android build is **not** done in Replit — it requires the Android SDK + NDK r26+ which are outside this environment. Follow the steps in `AGENT_INSTRUCTIONS.md` §1 on a workstation that has them installed.

## Project layout (top level)

```
android/             live Kotlin Android app
donors/              read-only Java reference code from sibling repos
docs/                user-facing docs (operating, training, dashboard guides)
scripts/             tiny TypeScript utilities (uses pnpm catalog refs)
server/              Python preview server for the Replit workspace
*.md                 README, AGENT_INSTRUCTIONS, GAP_AUDIT, etc.
```

## Recent changes

- 2026-04-26 — Initial Replit import. Installed Python 3.11, added `server/serve.py` documentation browser, configured the `Start application` workflow on port 5000, configured autoscale deployment.

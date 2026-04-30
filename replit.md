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
- 2026-04-26 — Added `scripts/push.sh` + `Push to GitHub` workflow that uses the `GITHUB_TOKEN` secret via `http.extraheader` (token is never written to disk).
- 2026-04-26 — **Ported `donors/orchestration-java/` (14 Java files, ~1,965 LOC) to idiomatic Kotlin** under `android/app/src/main/kotlin/com/ariaagent/mobile/core/orchestration/`. Closes the orchestration gap from `AGENT_INSTRUCTIONS.md` table item #3. Key changes during the port: `CentralOrchestrator` is now a plain coroutine class (no longer extends `Service`); `ProblemSolvingBroker` no longer depends on cloud Groq (it takes a `ProblemSolver` interface the spine plugs `LlamaEngine` into); `OrchestrationScheduler` no longer references `FeedbackSystem` / `ErrorResolutionWorkflow` (stage execution is now pluggable via `StageExecutor`); the donor `CircuitBreaker.java` package typo is fixed. See `core/orchestration/README.md` for the follow-up wiring needed.
- 2026-04-26 — Added `core/ai/LlamaProblemSolver.kt` — adapter that lets `ProblemSolvingBroker` use the on-device `LlamaEngine` for component-failure diagnostics. Defaults: `maxTokens=256`, `temperature=0.3`. Throws when the model is not loaded so the broker escalates the ticket. First of the four orchestration follow-ups (the LLM adapter) is now done.
- 2026-04-30 — **Added end-to-end logging stack.** New `core/logging/` package: `AriaLog` (mirrored `android.util.Log` + on-device file), `FileLogWriter` (rolling 2 MB × 5 files), `CrashHandler` (JVM uncaught), `AnrWatchdog` (main-thread freeze ≥ 5 s), `LogcatCollector` (`logcat -d` snapshot), `NativeCrashHandler` (Kotlin facade), `StrictModeInstaller` (debug invariants), `LogManager` (lifecycle + share zip). New `cpp/aria_crash_handler.cpp` installs SIGSEGV/SIGBUS/SIGFPE/SIGILL/SIGABRT/SIGPIPE handlers with backtrace + register dump. Wired in `MainApplication.onCreate`. Added host-side scripts in `scripts/logging/` (`build-with-logs.sh`, `parse-build-errors.sh`, `pull-logcat.sh`, `pull-app-logs.sh`, `symbolicate-native.sh`) and made the GitHub Actions APK build upload CMake/NDK logs on every run, not just on failure. Full reference in `docs/08_LOGGING.md`.

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
- 2026-05-02 — **Round 8 — closed/advanced 8 GAP_AUDIT items:**
  1. **`LlamaEngine` stub-mode surface (§9)** — `isStubMode: Boolean` computed property (`!jniAvailable`). `ModuleUiState.isStubMode` wired from it in `refreshModuleState`. `ModulesScreen` shows an amber "STUB MODE — NDK NOT COMPILED" banner when true. Stub `runInference()` now returns keyword-aware JSON (Type/Swipe/Back/Wait/LongPress/Click) via `buildStubResponse()` instead of a hardcoded Click.
  2. **Session stats on Dashboard (new §21)** — `SessionStatsUiState` (tasksCompleted, tasksErrored, totalSteps, successRate, avgStepsPerTask, sessionDurationMinutes) + `_sessionStats: StateFlow`. `handleStatusChanged` increments counters on running→idle/done/error. `DashboardScreen` shows `SessionStatsCard` after first task finishes.
  3. **`SafetyConfig` custom sensitive packages (new §22)** — `customSensitivePackages: Set<String>` field + `addCustomSensitivePackage`/`removeCustomSensitivePackage` VM methods. `SafetyScreen` §6 "User-Added Sensitive Apps" card with add/remove/block-toggle per package.
  4. **`LoraTrainer.TrainingResult.isStubMode` (new §23)** — Field added; return site sets `isStubMode = !jniSucceeded`.
  5. **`IrlModule.inferActionHeuristic` improved (new §24)** — Candidate-word extraction, TYPE detection, asymmetric Back threshold. No longer always emits "Click #1".
  6. **Lint CI workflow (§12 / §20)** — `.github/workflows/lint.yml` runs `./gradlew lint` on push/PR (Ubuntu, JDK 17, no NDK; stubs llama.cpp submodule so CMake doesn't abort). Uploads lint HTML/XML as artifact.
  7. **Duplicate KDoc comment** — Stale Round-7 KDoc above `SessionStatsUiState` removed.
  8. **`RecentGoalItem` KDoc preserved** — Kept above the correct class.
  **Files changed:** `LlamaEngine.kt`, `IrlModule.kt`, `LoraTrainer.kt`, `AgentViewModel.kt`, `DashboardScreen.kt`, `ModulesScreen.kt`, `SafetyScreen.kt`, `GAP_AUDIT.md`, `replit.md`. **New:** `.github/workflows/lint.yml`.
- 2026-05-02 — **Round 7 — closed 7 open GAP_AUDIT items:**
  1. **`PolicyNetwork` cold-start bias (§2)** — Added `outputBias: FloatArray` (size 7) to `PolicyNetwork.kt`. Fresh installs set tap=+0.3f (all others=0f), raising tap probability from 14.3%→~21% at softmax. Full Adam optimizer state (`mBias`, `vBias`) wired into `reinforce()`. Save/load with backward-compatible `EOFException` fallback for pre-Round-7 weight files.
  2. **`ChatScreen` preset chips hardcoded (§5)** — `PRESET_PROMPTS` static list replaced with `presetPromptsFor(status: String)`. Three chip sets: running/acting/thinking (task-aware), error/failed (failure-analysis), idle/default (capability discovery). `remember(status)` for efficient recomposition.
  3. **`GoalsScreen GOAL_TEMPLATES` static (§5)** — Added `RecentGoalItem` data class + `_recentGoals: StateFlow<List<RecentGoalItem>>` to `AgentViewModel`. `handleStatusChanged` pushes completed goals on running→idle/done transitions (max 6, deduped by goal text). `TemplatesTab` gains a "RECENTLY COMPLETED" horizontal scroll row with `RecentGoalChip` (Replay icon, one-tap re-enqueue) above the static template grid.
  4. **`ActivityScreen.kt` stale legacy comment (§10)** — Removed "DO NOT DELETE logs.tsx until this screen is verified on emulator" gate. KDoc updated to declare this screen the canonical implementation superseding the React Native `logs.tsx`.
  5. **`ControlScreen.kt` unverified (§5)** — Verified complete. All 7 Phase 4 checklist items in the file header are implemented and wired. Closed in GAP_AUDIT.
  6. **`ModulesScreen.kt` "placeholder" claim (§5)** — Stale GAP_AUDIT claim debunked: App Skills, Vision, and SAM2 sections all read real `ModuleUiState` fields and have working download/load/unload controls. Closed as already done.
  7. **`InferenceEngineImpl.kt` ghost entry (§2)** — File does not exist. Closed as N/A.
  **Files changed:** `PolicyNetwork.kt`, `AgentViewModel.kt`, `GoalsScreen.kt`, `ChatScreen.kt`, `ActivityScreen.kt`, `GAP_AUDIT.md`.
- 2026-05-02 — **Round 6 — closed four open GAP_AUDIT items:**
  1. **Screen hash jitter (§4)** — `ScreenSnapshot.screenHashFuzzy()` added to `ScreenObserver.kt`. Strips `\b\d+(?:[,.]\d+)*\b` digit sequences before SHA-256. `AgentLoop` now tracks `lastScreenHashFuzzy` and uses it for stuck-detection comparisons at every step; the exact `screenHash()` is kept for all DB keying (ObjectLabelStore, VisionEmbeddingStore, SessionReplayStore). Counter increments, badge counts, "X min ago" labels no longer reset `stuckCount`.
  2. **Task queue ceiling (§4)** — `TaskQueueManager.MAX_QUEUE_SIZE = 20`; `enqueue()` returns `QueuedTask?` (null when full). `AgentViewModel.enqueueTask()` logs the rejection. `ControlScreen` disables the Add-to-Queue button and renders "(N/20 — FULL)" in red when at capacity.
  3. **`_triggers` StateFlow empty on startup** — `loadTriggers()` was only called from `GoalsScreen` via `LaunchedEffect`. Added to `AgentViewModel.init {}` so the Triggers tab is correctly pre-populated even before the user navigates there.
  4. **`AgentEventBus` event catalogue** — Added 7 previously undocumented events: `skill_updated`, `task_chain_advanced`, `scheduler_training_started/stopped`, `trigger_fired`, `app_focus_changed`, `orchestration.*` wildcard.
  **Files changed:** `ScreenObserver.kt`, `AgentLoop.kt` (6 sites), `AgentViewModel.kt`, `TaskQueueManager.kt`, `ControlScreen.kt`, `AgentEventBus.kt`.
- 2026-05-02 — **Round 5 — closed four open GAP_AUDIT items:**
  1. **`learning_cycle_complete` never emitted (§3)** — `LearningScheduler.runTrainingCycle()` now emits `learning_cycle_complete` to `AgentEventBus` after each training cycle. `AgentViewModel.handleLearningComplete()` already read `PolicyNetwork.adamStepCount`/`lastPolicyLoss` directly, so Dashboard/TrainScreen `adamStep` and `lastPolicyLoss` now update after charging-window training runs.
  2. **Triggers feature had no backend (§5 / priority #18)** — new `core/triggers/TriggerEvaluator.kt` (200 LOC) implements the full runtime evaluation engine: TIME_DAILY/WEEKLY/ONCE via a 60 s polling coroutine with per-trigger epoch-day deduplication; APP_LAUNCH via `AgentEventBus` `app_focus_changed` subscription; CHARGING via `BroadcastReceiver`. Fires by emitting `trigger_fired`; `AgentForegroundService` handles it and starts `AgentLoop` when idle.
  3. **`app_focus_changed` event never emitted** — `AgentAccessibilityService.onAccessibilityEvent()` `TYPE_WINDOW_STATE_CHANGED` handler now emits `app_focus_changed` whenever the foreground non-IME package changes. Required by `TriggerEvaluator` for APP_LAUNCH triggers.
  4. **Dead A11y node infinite loop (§4)** — `AgentLoop` now maintains a per-run `nodeFailureMap: MutableMap<String, Int>`. After `DEAD_NODE_THRESHOLD` (3) consecutive `GestureEngine` failures on the same nodeId, a `stuckHint` is injected into the LLM prompt telling it to stop targeting that node. Counter resets on success. Closes the loop-until-MAX_STEPS failure mode.
  **Files changed:** `LearningScheduler.kt`, `AgentAccessibilityService.kt`, `AgentForegroundService.kt`, `AgentLoop.kt`. **New file:** `core/triggers/TriggerEvaluator.kt`.
- 2026-04-30 — **Added end-to-end logging stack.** New `core/logging/` package: `AriaLog` (mirrored `android.util.Log` + on-device file), `FileLogWriter` (rolling 2 MB × 5 files), `CrashHandler` (JVM uncaught), `AnrWatchdog` (main-thread freeze ≥ 5 s), `LogcatCollector` (`logcat -d` snapshot), `NativeCrashHandler` (Kotlin facade), `StrictModeInstaller` (debug invariants), `LogManager` (lifecycle + share zip). New `cpp/aria_crash_handler.cpp` installs SIGSEGV/SIGBUS/SIGFPE/SIGILL/SIGABRT/SIGPIPE handlers with backtrace + register dump. Wired in `MainApplication.onCreate`. Added host-side scripts in `scripts/logging/` (`build-with-logs.sh`, `parse-build-errors.sh`, `pull-logcat.sh`, `pull-app-logs.sh`, `symbolicate-native.sh`) and made the GitHub Actions APK build upload CMake/NDK logs on every run, not just on failure. Full reference in `docs/08_LOGGING.md`.

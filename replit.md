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
- 2026-05-02 — **Round 10 — closed/advanced 8 GAP_AUDIT items (new §33–40):**
  1. **`HardwareStats.batteryPercent`** — Added to `HardwareStats` data class; `statsFlow()` reads it via `ACTION_BATTERY_CHANGED` sticky broadcast (BatteryManager; -1 when unavailable).
  2. **Battery meter bar** — New `BatteryMeterBar` composable with inverted color scale (green ≥50 → amber → red <20) appended as 4th bar in `HardwareMeterRow`. `HardwareMiniStrip` gains BAT chip. Private `batteryColor()` helper added. All sites (Dashboard, Control, Chat overlay) now show live battery.
  3. **`SafetyScreen` SENSITIVE_APP_PRESETS** — Expanded from 4 to 7 categories. New: Crypto & Investing (8 packages), Email & Cloud Storage (8 packages), Government & ID (6 packages). Existing 4 categories each ~doubled. Total ~50 sensitive packages.
  4. **`CrashHandler` crash notification** — On uncaught exception: `ensureNotificationChannel("aria_crash")`; `postCrashNotification()` posts `PRIORITY_HIGH` `NotificationCompat` with BigText (class + message + filename). Tap re-opens app. `writeFile()` now returns `File`. All wrapped in try/catch.
  5. **`AgentForegroundService` exponential backoff** — Fixed 5s retry delay replaced with `2s × 2^(attempt-1)` capped at 30s. Notification shows countdown. Log records delay.
  6. **`PolicyNetwork` corruption guard** — `loadFromBinary()` validates each size field against expected dimension before allocating. Bad size → `initRandom()` + `Log.w` instead of OOM/crash.
  7. **`LocalSnapshotStore` + `MonitoringPusher` battery/uptime** — `defaultStatus()` gains `batteryPercent: -1` and `uptimeSec: 0`. `buildStatus()` populates live battery + uptime. `/aria/status` endpoint now surfaces both.
  8. **Round 10 GAP_AUDIT update** — §33–40 added; header updated to Round 10.
  **Files changed:** `HardwareMonitor.kt`, `HardwareMeterBar.kt`, `SafetyScreen.kt`, `CrashHandler.kt`, `AgentForegroundService.kt`, `PolicyNetwork.kt`, `LocalSnapshotStore.kt`, `MonitoringPusher.kt`, `GAP_AUDIT.md`, `replit.md`.
- 2026-05-02 — **Round 11 — closed 8 GAP_AUDIT items (§41–48):**
  1. **`AgentEventBus` ring buffer** — `_history: ArrayDeque<Triple<…>>` (150 entries), thread-safe via `synchronized(historyLock)`. `recentEvents` returns immutable snapshot. `clearHistory()` for tests. `emit()` appends before broadcasting.
  2. **`AgentLoop` LLM inference timeout** — `LLM_INFERENCE_TIMEOUT_MS = 90_000L`. `withTimeoutOrNull()` wraps `LlamaEngine.infer()` / `inferWithVision()` inside existing `try-finally` (bitmap still recycled). Null → log, emit `inference_timeout` event, `continue`. Prevents JNI hang blocking the loop forever.
  3. **`AgentLoop` step duration telemetry** — `stepStartMs` captured before Observe phase. `"stepDurationMs"` added to `action_performed` payload (full observe→reason→act wall-clock latency).
  4. **`AgentViewModel` periodic battery polling** — 60 s `while(true)` loop in `init` on IO dispatcher. Keeps battery chip live during idle/paused states.
  5. **`AgentViewModel` `avgStepDurationMs` EMA** — `avgStepDurationMs: Long = 0L` in `SessionStatsUiState`. `handleActionPerformed` updates via α≈0.2 EMA from `stepDurationMs` event field.
  6. **`ProgressPersistence.pruneOldLogs()`** — Trims `aria_progress.txt` lines with timestamps older than `daysToKeep=7`. Blank/separator lines (no timestamp) always kept. Called on cold-start from ViewModel init.
  7. **`DiagnosticsScreen` crash file list** — New "CRASH REPORTS" section: `CrashHandler.listCrashes()` on IO dispatcher. One expandable card per file: name, size KB, relative time. "View/Collapse" toggle. Header turns red when any files exist.
  8. **`DashboardScreen` avg step duration chip** — `SessionStatsCard` footer appends `• Xms/step` when `avgStepDurationMs > 0`.
  **Files changed:** `AgentEventBus.kt`, `AgentLoop.kt`, `AgentViewModel.kt`, `SessionStatsUiState` (in AgentViewModel), `ProgressPersistence.kt`, `DiagnosticsScreen.kt`, `DashboardScreen.kt`, `GAP_AUDIT.md`, `replit.md`.
- 2026-05-02 — **Round 9 — closed/advanced 9 GAP_AUDIT items (new §25–32):**
  1. **Battery-aware LearningScheduler** — `maybeStartTraining()` reads `BatteryManager.BATTERY_PROPERTY_CAPACITY` and skips below 20%. Also skips when free RAM < 900 MB via `ActivityManager.MemoryInfo`. Both skips emit bus events. `getBatteryLevel()` + `getAvailableRamMb()` helpers added.
  2. **NSD/mDNS device discovery** — `LocalDeviceServer.startNsd(context)` registers `ARIA-Device._aria._tcp` via `NsdManager`. Called from `AgentViewModel.toggleLocalServer()`. Dashboards on the same LAN can auto-discover the device without manual IP entry.
  3. **ControlScreen permission banner** — Amber `PERMISSIONS NEEDED` card below hardware meters when a11y or screen-capture is missing. Accessibility row deep-links to Android Accessibility Settings. New `PermissionRow` private composable.
  4. **DiagnosticsScreen crash log viewer** — "ON-DEVICE LOG" section reads last 60 lines of `{filesDir}/logs/app.log` with Expand/Collapse toggle and monospace rendering.
  5. **AgentViewModel battery + permissions StateFlows** — `_batteryLevel: StateFlow<Int>` (init-polled via `refreshBatteryLevel()`). `permissionsAllGood: StateFlow<Boolean>` derived with `SharingStarted.Eagerly`. Added `kotlinx.coroutines.flow.map` import.
  6. **SettingsScreen Build Info card** — Device model, API level, ABI, Inference mode (Stub/Native JNI), stub-mode flag highlighted amber when active.
  7. **First JVM unit tests** — `SessionStatsUiStateTest.kt` (8 tests) + `SafetyConfigTest.kt` (6 tests) in `android/app/src/test/java/com/ariaagent/mobile/`. Pure JVM, no Android SDK dependency.
  8. **GAP_AUDIT §11 consistency fix** — `WAKE_LOCK` + `ONNX reflection` rows corrected from `[ ]` → `[x]`, now consistent with Priority table items 12–13.
  9. **LocalDeviceServer NSD** — `stopNsd()` unregisters cleanly on `stop()`. `nsdRegistered: Boolean` volatile flag exposed for module state inspection.
  **Files changed:** `LearningScheduler.kt`, `LocalDeviceServer.kt`, `ControlScreen.kt`, `DiagnosticsScreen.kt`, `SettingsScreen.kt`, `AgentViewModel.kt`, `GAP_AUDIT.md`, `replit.md`. **New:** `SessionStatsUiStateTest.kt`, `SafetyConfigTest.kt`.
- 2026-05-02 — **Round 13 — closed 11 GAP_AUDIT items (§59–69):**
  1. **`NetworkMonitor` (§59)** — New `core/system/NetworkMonitor.kt` with `connectionType(context): ConnectionType` (WIFI/MOBILE/NONE) using `ConnectivityManager.getNetworkCapabilities()`.
  2. **`AgentViewModel.networkType` StateFlow (§60)** — `_networkType` polled every 30s from `NetworkMonitor`. Imports added: `NotificationManager`, `PendingIntent`, `NotificationCompat`, `NetworkMonitor`, `ComposeMainActivity`, `withContext`, `File`.
  3. **`DashboardScreen` network chip (§61)** — Color-coded `NET WiFi` / `NET Mobile` / `OFFLINE` chip at the bottom of the Hardware card.
  4. **`ThermalGuard.pauseDurationMs()` (§62)** — Scaled thermal pause: MODERATE=5s, SEVERE=15s, CRITICAL=30s. `AgentLoop` uses `pauseDurationMs().coerceAtLeast(10_000L)`.
  5. **Vision description cache (§63)** — `lastVisionDescription` var; skips `VisionEngine.describe()` when `screenHash == lastScreenHash`, saving ~400ms/step.
  6. **JSON parse-failure retry (§64)** — `MAX_PARSE_RETRIES=2`; repair prompt with `maxTokens=80`, `temperature=0.05` on `no action parsed`/`malformed json` fallback.
  7. **`stuck_abort` `DONE`→`ERROR` fix (§65)** — Critical bug fix: stuck abort now correctly sets `Status.ERROR` instead of `Status.DONE`.
  8. **stuckCount ≥ 6 Home press (§66)** — New recovery tier: Home press + hint before the ≥8 abort. `AgentAccessibilityService.performHome()` added.
  9. **Task completion notifications (§67)** — `postTaskNotification()` posts OS notification on task done/error via `aria_agent_reasoning` channel.
  10. **ActivityScreen share button (§68)** — Share icon button in ACTIVITY header exports last 100 actions as plain text via system share sheet.
  11. **Chat history persistence (§69)** — `saveChatHistory()` + `loadChatHistory()` to/from `{filesDir}/aria_chat.json` (max 100 messages); called on send, clear, and cold start.
  **Files changed:** `NetworkMonitor.kt` (new), `ThermalGuard.kt`, `AgentLoop.kt`, `AgentAccessibilityService.kt`, `AgentViewModel.kt`, `DashboardScreen.kt`, `ActivityScreen.kt`, `GAP_AUDIT.md`, `replit.md`.
- 2026-05-02 — **Round 12 — closed 10 GAP_AUDIT items (§49–58):**
  1. **`AgentLoop` consecutive-timeout abort (§49)** — `MAX_CONSECUTIVE_TIMEOUTS=3` constant. `consecutiveTimeouts` counter: reset to 0 on good inference, incremented on timeout. After 3 back-to-back timeouts: `status=ERROR`, `logTaskEnd`, `SustainedPerformanceManager.disable()`, `emitStatus()`, `break`. Prevents infinite spin on wedged JNI model.
  2. **`step_started` event enriched (§50)** — `"appPackage"` and `"goalText"` fields added to the `step_started` event payload so downstream analytics can correlate steps without additional lookups.
  3. **`SessionStatsUiState.inferenceTimeoutCount` (§51)** — New `Int` field; incremented by `handleInferenceTimeout()`; reset by `resetSession()`.
  4. **`AgentViewModel.handleInferenceTimeout()` + dispatch (§52)** — New private handler wired to `"inference_timeout"` in the AgentEventBus `when` table.
  5. **`AgentViewModel.resetSession()` (§53)** — Clears action logs, session stats (new `sessionStartMs`), token stream, step state. No agent stop.
  6. **`ControlScreen` "RESET SESSION" button (§54)** — Right-aligned `TextButton` below Start/Stop/Pause row; confirmation `AlertDialog` before clearing.
  7. **`ActivityScreen` date-filter chips (§55)** — `ActionDateFilter` enum (`ALL`/`TODAY`/`WEEK`). `LazyRow` of `FilterChip`s above the action `LazyColumn`. Count badge shown when filtered.
  8. **`DiagnosticsScreen` "Clear All" crash button (§56)** — Red `TextButton` in CRASH REPORTS header (visible only when files exist). `AlertDialog` confirmation deletes all files and clears state.
  9. **`DiagnosticsScreen` progress.txt size chip (§57)** — `ProgressPersistence.logFileSizeBytes(context)` shown as monospace `"X.X KB"` in the ON-DEVICE LOG header row.
  10. **`DashboardScreen` timeout count in stats footer (§58)** — Footer appends `• N timeout(s)` in amber whenever `inferenceTimeoutCount > 0`; footer visible even before first task completes if timeouts have occurred.
  **Files changed:** `AgentLoop.kt`, `AgentViewModel.kt`, `SessionStatsUiState` (in ViewModel), `ProgressPersistence.kt`, `ControlScreen.kt`, `ActivityScreen.kt`, `DiagnosticsScreen.kt`, `DashboardScreen.kt`, `GAP_AUDIT.md`, `replit.md`.
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
- 2026-05-02 — **Round 14 — closed GAP_AUDIT §70–§80 (11 items):**
  1. **`AgentLoop` adaptive step delay (§70)** — `STEP_DELAY_FAST_MS = 400L`; `consecutiveSuccesses` counter; end-of-loop delay halved to 400 ms after 3+ consecutive successes, resets to 800 ms on any failure.
  2. **`AgentLoop` A11y abort DONE→ERROR (§71)** — `accessibility_service_dead` abort path correctly sets `Status.ERROR` (was `Status.DONE`).
  3. **`AgentLoop` exception class name in error (§72)** — `"${e.javaClass.simpleName}: ${e.message}"` stored in `lastError`; same passed to `ProgressPersistence.logNote`.
  4. **`ControlScreen` recent-goals chips (§73)** — Horizontal chip row using `vm.recentGoals` StateFlow; one-tap populates goal + package fields.
  5. **`ControlScreen` package auto-suggest chips (§74)** — 12-entry `PACKAGE_SUGGESTIONS` map; `remember(goalText)` derivation; `SuggestionChip` row auto-fills target-app field.
  6. **`AgentViewModel` uptime timer (§75)** — `_uptimeSeconds` StateFlow + `uptimeJob`; starts/resets on running, cancels on any other status.
  7. **`DashboardScreen` uptime chip (§76)** — `MetricChip(Timer, "M:SS")` shown while running.
  8. **`DiagnosticsScreen` device info card (§77)** — `ActivityManager.MemoryInfo` + `Build` fields; 6-row card: Model, Android, ABI, Total/Avail RAM, Low RAM flag; `DiagInfoRow` composable.
  9. **`SettingsScreen` export config button (§78)** — `OutlinedButton` shares 13-field JSON via `Intent.ACTION_SEND`.
  10. **`ActivityScreen` avg-confidence chip (§79)** — `remember(entries)` avg-reward derivation; `MemStatChip("Avg Conf", "$avgConf%")` added as 6th chip.
  11. **`AgentLoop` CancellationException rethrow (§80)** — `catch (e: Exception)` rethrows `CancellationException` to preserve structured-concurrency cancellation.
  **Files changed:** `AgentLoop.kt`, `AgentViewModel.kt`, `DashboardScreen.kt`, `DiagnosticsScreen.kt`, `ActivityScreen.kt`, `ControlScreen.kt`, `SettingsScreen.kt`, `GAP_AUDIT.md`, `replit.md`.
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
- 2026-05-02 — **Round 15 — closed GAP_AUDIT §81–§92 (12 items):**
  1. **`AgentLoop` MAX_STEPS approach hint (§81)** — When ≥80% of the step budget is consumed, injects a "⚠ Budget warning: only N steps remain" string into `stuckHint` so the LLM wraps up instead of exploring indefinitely.
  2. **`ChatScreen` copy on long-press (§82)** — `pointerInput` + `detectTapGestures(onLongPress)` added to user and assistant bubble `Box`es; copies message text to `ClipboardManager` + shows `Toast("Copied")`.
  3. **`GoalsScreen` template search bar (§83)** — `searchQuery` var + `filteredTemplates = remember(searchQuery)` derivation; `OutlinedTextField` with leading Search and trailing Clear icon above the grid; `items(GOAL_TEMPLATES)` → `items(filteredTemplates)`.
  4. **`ControlScreen` task priority selector (§84)** — `queuePriority` var (-1/0/1); Low/Normal/High `FilterChip` row above Add-to-Queue button; priority passed to `vm.enqueueTask(goal, app, priority)` and reset to Normal after enqueue.
  5. **`DashboardScreen` quick-action strip (§85)** — When status is `"error"`: shows `lastError` in 9 sp monospace red, a "Retry Task" button (`vm.startAgent(currentTask, currentApp)`), and a "Dismiss" button (`vm.clearError()`).
  6. **`DiagnosticsScreen` clear progress.txt button (§86)** — `progress.txt: X KB` label now in `Row` with a red "CLEAR" `TextButton`; confirmation `AlertDialog`; calls `ProgressPersistence.clear(context)` on IO dispatcher, resets `progressLogBytes = 0L`.
  7. **`ActivityScreen` export memory JSON button (§87)** — `Upload` `IconButton` added to header row when Memory tab is active; calls `vm.exportMemory()`.
  8. **`SettingsScreen` reset config to defaults button (§88)** — New `SettingsCard` in Danger Zone with "Reset Config to Defaults" button; `AlertDialog` guard; calls `vm.saveConfig(AriaConfig())` on confirm.
  9. **`AgentLoop` emit `action_failed` event (§89)** — After every failed action, `AgentEventBus.emit("action_failed", {step, appPackage, actionJson})` for external observers.
  10. **`AgentViewModel.clearError()` (§90)** — Sets status → `"idle"` + clears `lastError` without touching session data; used by Dashboard Dismiss button.
  11. **`ControlScreen` metered network warning (§91)** — `networkType` StateFlow collected; amber banner with `SignalCellularAlt` shown when on `"mobile"` data and agent is idle.
  12. **`AgentViewModel.exportMemory()` (§92)** — Serialises `_memoryEntries` to JSON (app/summary/result/reward/taskType/isEdgeCase); shares via `Intent.ACTION_SEND` with `FLAG_ACTIVITY_NEW_TASK`.
  **Files changed:** `AgentLoop.kt`, `AgentViewModel.kt`, `ChatScreen.kt`, `GoalsScreen.kt`, `DashboardScreen.kt`, `DiagnosticsScreen.kt`, `ActivityScreen.kt`, `SettingsScreen.kt`, `ControlScreen.kt`, `GAP_AUDIT.md`, `replit.md`.
- 2026-05-04 — **Round 24 — closed GAP_AUDIT §189–§200 (12 items):**
  1. **`ChatScreen` "Export chat" share button (§189)** — TextButton + Share icon below ContextTagBar when `messages.size > 1`; shares all messages as "You: / ARIA:" plain text.
  2. **`ActivityScreen` MemoryList sort-by-reward FilterChip (§190)** — `var sortByReward` state + `FilterChip("↑ Reward"/"Recent")` in the chips Row; Success color when selected.
  3. **`AgentViewModel` `totalSessionTokens` in `SessionStatsUiState` + increment (§191)** — `val totalSessionTokens: Long = 0L` added; incremented by word-count × 1.3 estimate in `sendChatMessage`.
  4. **`DashboardScreen` session tokens chip in SessionStatsCard (§192)** — `SessionStat("Tokens", "NK tok")` chip when `totalSessionTokens > 0`.
  5. **`ControlScreen` "Paste from clipboard" quick-fill button (§193)** — TextButton(ContentPaste + "Paste: prefix…") after goalText field when idle and clipboard has non-empty different text.
  6. **`ActivityScreen` ReplayList sessions + steps header chip (§195)** — `val totalReplaySteps = sessions.sumOf { it.stepCount }`; Primary-tinted "N session(s) · M total step(s)" Row shown as first LazyColumn item.
  7. **`AgentViewModel.clearRecentGoals()` (§196)** — `fun clearRecentGoals() { _recentGoals.value = emptyList() }` added next to `recentGoals` StateFlow.
  8. **`ControlScreen` "Clear history" button in RECENT GOALS row (§197)** — RECENT GOALS label wrapped in `Row(SpaceBetween)` with trailing `TextButton("Clear")` calling `vm.clearRecentGoals()`.
  9. **`SafetyScreen` "Copy blocklist" clipboard button (§198)** — TextButton with ContentCopy/CheckCircle icon copies `blockedPackages.joinToString("\n")` to clipboard; shows "Copied!" on success.
  10. **`GoalsScreen` QueueTab drag-to-reorder hint text (§199)** — Centered `Row(DragHandle icon + "Tap goals above to edit priority order")` hint when `taskQueue.size > 1`.
  11. **`DiagnosticsScreen` "Copy log" icon button (§200)** — `IconButton(ContentCopy/CheckCircle)` in the log header Row copies full log to clipboard when `progressLog.isNotBlank()`.
  **Files changed:** `ChatScreen.kt`, `ActivityScreen.kt` (×2), `AgentViewModel.kt` (×3), `DashboardScreen.kt`, `ControlScreen.kt` (×2), `SafetyScreen.kt`, `GoalsScreen.kt`, `DiagnosticsScreen.kt`, `GAP_AUDIT.md`, `replit.md`.
- 2026-05-04 — **Round 23 — closed GAP_AUDIT §177–§188 (12 items):**
  1. **`DiagnosticsScreen` device uptime in device info card (§177)** — `SystemClock.elapsedRealtime()` → `DiagInfoRow("Device uptime", "Xh Ym")`.
  2. **`ActivityScreen` LabelsList no-results empty state (§179)** — `SearchOff` icon + `"No labels match X"` box when `displayLabels.isEmpty() && labelSearch.isNotBlank()`.
  3. **`TrainScreen` RL training progress bar (§180)** — `LinearProgressIndicator((adamStep % 1000) / 1000f)` after estimated-samples line; color Success < 0.05 loss else Primary; `"Nk+M / 1K block"` label.
  4. **`GoalsScreen` QueueTab "Share queue" chip (§181)** — `TextButton(Share + "Share queue")` when `taskQueue.isNotEmpty()`; shares numbered task list as plain text.
  5. **`SettingsScreen` "Export configuration" OutlinedButton (§182)** — After Save button; shares key config fields (model, quant, context, temp, GPU, flash attn, RL, LoRA) as plain text via `ACTION_SEND`.
  6. **`DashboardScreen` pending tasks chip + `taskQueue` collection (§183)** — `vm.taskQueue.collectAsStateWithLifecycle()`; Accent `"N task(s) pending in queue"` chip above SYSTEM card when queue non-empty.
  7. **`ModulesScreen` total loaded model RAM chip in header (§184)** — `remember(loadedLlms) { sumOf { displaySizeMb } }`; Warning `"~N MB"` chip in header Row when > 0.
  8. **`GoalsScreen` TriggersTab search empty state (§185)** — `SearchOff` icon + `"No triggers match X"` when `filteredTriggers.isEmpty() && triggerSearch.isNotBlank()`.
  9. **`DiagnosticsScreen` session age column in heap stats card (§186)** — Fourth stat column `"Nm / Session age"` using `(currentTimeMs - sessionStartMs) / 60_000`.
  **Files changed:** `DiagnosticsScreen.kt` (×2), `ActivityScreen.kt`, `TrainScreen.kt`, `GoalsScreen.kt` (×2), `SettingsScreen.kt`, `DashboardScreen.kt` (×2), `ModulesScreen.kt`, `GAP_AUDIT.md`, `replit.md`.
- 2026-05-04 — **Round 22 — closed GAP_AUDIT §165–§176 (12 items):**
  1. **`DashboardScreen` SessionStatsCard `uptimeSeconds` + Uptime chip (§165)** — `uptimeSeconds: Long = 0L` param; `SessionStat("Uptime", "Xm Ys")` chip when running.
  2. **`DashboardScreen` SessionStatsCard "Reset" TextButton (§166)** — `onResetSession: (() -> Unit)?` param; shown when `tasksCompleted + tasksErrored > 0`; calls `vm.resetSession()`.
  3. **`DashboardScreen` "Loop Err" chip in SessionStatsCard (§167)** — `SessionStat("Loop Err", "N", Error)` when `agentLoopErrors > 0`.
  4. **`ActivityScreen` MemoryList `totalReward` MemStatChip (§168)** — `MemStatChip("Reward", "%.1f", Success/Destructive)` appended when `totalReward != 0.0`.
  5. **`ActivityScreen` LabelsList search filter (§169)** — `var labelSearch` + `displayLabels`; `OutlinedTextField` first in LazyColumn; `items()` updated; header count shows `"N / M"` when active.
  6. **`ChatScreen` clear-chat dialog shows exact message count (§170)** — `"All N message(s) will be removed."` with singular/plural.
  7. **`GoalsScreen` QueueTab estimated completion time (§171)** — `avgStepDurationMs: Long = 0L` param; `"~est. Xm Ys remaining"` 9 sp monospace shown before queue items list.
  8. **`ControlScreen` last-error excerpt chip in status row (§172/§175)** — when `status == "error" && lastError.isNotBlank()`, red 10% alpha chip shows first 28 chars of `lastError`.
  9. **`TrainScreen` "Share adapter" icon button (§173)** — `IconButton(Share)` in adapter filename `Row` when `sizeKb > 0`; shares via `FileProvider` + `Intent.ACTION_SEND`.
  10. **`ModulesScreen` LlmSlotCard model filename hint (§174)** — `slotEntry?.modelId?.substringAfterLast('/')` shown as 9 sp 70% alpha Muted text before the `~N MB` chip.
  11. **`DashboardScreen` `onResetSession` wired at call site (§176)** — `SessionStatsCard(..., uptimeSeconds, onResetSession = { vm.resetSession() })`.
  **Files changed:** `DashboardScreen.kt` (×4), `ActivityScreen.kt` (×4), `ChatScreen.kt`, `GoalsScreen.kt` (×3), `ControlScreen.kt`, `TrainScreen.kt`, `ModulesScreen.kt`, `GAP_AUDIT.md`, `replit.md`.
- 2026-05-04 — **Round 21 — closed GAP_AUDIT §153–§164 (12 items):**
  1. **`AgentViewModel` `agentLoopErrors: Int = 0` in `SessionStatsUiState` (§153)** — cumulative error counter persists across the session.
  2. **`AgentViewModel` increment `agentLoopErrors` on "error" status (§154)** — `_sessionStats.update { copy(agentLoopErrors + 1) }` inside `handleStatusChanged`.
  3. **`DashboardScreen` `LearningStat` `valueColor` param + colored Loss chip (§155)** — `valueColor: Color = ARIAColors.Accent`; Loss uses green/amber/red severity palette.
  4. **`AgentViewModel` `fun clearActionLog()` (§156)** — public method sets `_actionLogs.value = emptyList()`.
  5. **`ActivityScreen` "Clear log" chip with confirmation dialog in ActionsList (§157)** — `InputChip(DeleteSweep, "Clear log")` in filter row; confirmation `AlertDialog` before clearing; `onClearLog` param threaded from call site.
  6. **`GoalsScreen` TemplatesTab empty-state block (§158)** — centered `SearchOff` icon + "No results for X" when both `filteredTemplates` and `filteredRecentGoals` are empty while searchQuery active.
  7. **`DiagnosticsScreen` JVM heap usage + loop errors ARIACard (§159)** — first card in scroll: "Heap used / Heap max / Loop errors" three-column stats row; collects `vm.sessionStats`.
  8. **`ControlScreen` token-rate `%.1f t/s` chip in header (§160)** — Success-tinted chip appended after status dot when `status == "running" && tokenRate > 0`.
  9. **`ModulesScreen` "Refresh all" chip (§161)** — single combined chip replacing two icon buttons; calls both `vm.refreshAppSkills()` + `vm.refreshModuleState()`.
  10. **`ChatScreen` loaded model name in contextLine (§162)** — collects `vm.loadedLlms`; first loaded model's `modelId` filename prepended to context sub-label.
  11. **`TrainScreen` adapter file size in KB (§163)** — adapter filename wrapped in `Row` with `"${sizeKb} KB"` Success text when file exists.
  12. **`DiagnosticsScreen` `agentLoopErrors` loop-errors stat column (§164)** — third column in §159 card; color Success when 0, Error when > 0.
  **Files changed:** `AgentViewModel.kt` (×4), `DashboardScreen.kt` (×2), `ActivityScreen.kt` (×3), `GoalsScreen.kt`, `DiagnosticsScreen.kt` (×2), `ControlScreen.kt`, `ModulesScreen.kt`, `ChatScreen.kt`, `TrainScreen.kt`, `GAP_AUDIT.md`, `replit.md`.
- 2026-05-04 — **Round 20 — closed GAP_AUDIT §141–§152 (12 items):**
  1. **`DashboardScreen` "Last task" preview when idle (§141)** — 9 sp monospace `"Last: {goal.take(55)}"` shown below status card when idle + lastCompletedGoal set.
  2. **`GoalsScreen` TemplatesTab filter recentGoals by searchQuery (§142)** — `filteredRecentGoals` via `remember(searchQuery, recentGoals)`; both `if()` guard and `items(...)` use filtered list.
  3. **`ActivityScreen` action-search match count label (§143)** — `"N match(es) for 'X'"` Muted monospace 9 sp below search field when results non-empty.
  4. **`AgentViewModel` `chatMessagesCount` in `SessionStatsUiState` (§144)** — `val chatMessagesCount: Int = 0` added to `SessionStatsUiState`.
  5. **`DashboardScreen` SessionStatsCard "Chat" chip (§145)** — `SessionStat("Chat", "N")` chip when `chatMessagesCount > 0`.
  6. **`DiagnosticsScreen` "Share app.log" icon button (§146)** — `IconButton(Share)` added alongside Expand/Collapse button in log card header; shares `crashLines.joinToString("\n")` via `ACTION_SEND`.
  7. **`GoalsScreen` TriggersTab search filter (§147)** — `var triggerSearch` + `filteredTriggers` (by `goal`/`watchPackage`/`goalAppPackage`); search `OutlinedTextField` as first LazyColumn item when `triggers.size > 1`; `items()` updated.
  8. **`ModulesScreen` skills count chip in header (§148)** — `"${appSkills.size} skills"` Accent chip added left of the loaded-model chip.
  9. **`SafetyScreen` blocked-package count in header subtitle (§149)** — `"N app(s) blocked"` replaces default subtitle when `safetyConfig.blockedPackages.size > 0`.
  10. **`AgentViewModel` `sendChatMessage` increments `chatMessagesCount` (§150)** — `_sessionStats.update { it.copy(chatMessagesCount + 1) }` after `_chatMessages.update`.
  11. **`TrainScreen` RlStatusCard "N training steps" summary (§151)** — `"Nk training steps  •  policy ready/pending"` in 9 sp monospace below StatBadge row; color Success/Accent based on policyReady.
  12. **`ChatScreen` input character count label (§152)** — `ChatInputBar` wraps `OutlinedTextField` in `Column(weight(1f))`; right-aligned 9 sp `"N chars"` below field when `> 100`; Warning tint when `> 400`.
  **Files changed:** `DashboardScreen.kt` (×2), `GoalsScreen.kt` (×5), `ActivityScreen.kt`, `AgentViewModel.kt` (×3), `DiagnosticsScreen.kt`, `ModulesScreen.kt`, `SafetyScreen.kt`, `TrainScreen.kt`, `ChatScreen.kt`, `GAP_AUDIT.md`, `replit.md`.
- 2026-05-04 — **Round 19 — closed GAP_AUDIT §129–§140 (12 items):**
  1. **`ChatHeader` message count badge (§129)** — `messageCount: Int = 0` param; primary-tinted badge next to title when `messageCount > 1`.
  2. **`TrainScreen` RlStatusCard loss color (§130)** — Policy-loss text: green < 0.05, amber < 0.30, red ≥ 0.30; `SemiBold`.
  3. **`GoalsScreen` QueueTab "Clear All" button (§131)** — `onClearAll` param added; destructive `TextButton` in NEXT UP row when queue non-empty.
  4. **`ActivityScreen` LabelsList count + enriched header (§132)** — First `item` shows `"LABELS"` + `"N total • M enriched"` in a `SpaceBetween` Row.
  5. **`ModulesScreen` loaded model chip in header (§133)** — `"N / M loaded"` primary chip in header row when `loadedLlms.isNotEmpty()`.
  6. **`AgentViewModel` `lastCompletedGoal` field (§134)** — Added to `AgentUiState`; set in `handleStatusChanged` when task transitions running→done/idle.
  7. **`ControlScreen` "Run again" quick chip (§135)** — Full-width `TextButton` with `Replay` icon pre-fills `goalText` when idle + goal blank + lastCompletedGoal set.
  8. **`DiagnosticsScreen` ERROR/WARN count above filter bar (§136)** — `"N ERR"` (red) / `"M WARN"` (amber) labels above the log filter field when log is expanded.
  9. **`GoalsScreen` QueueTab `"NEXT UP (N)"` count header (§137)** — `"NEXT UP"` becomes `"NEXT UP  (N)"` in a `SpaceBetween` Row with the Clear All button.
  10. **`TrainScreen` IRL LLM-assist rate % (§138)** — `"N% LLM-labelled"` in primary 80% alpha monospace when `tuplesExtracted > 0 && llmAssistedCount > 0`.
  11. **`ControlScreen` running-task banner shows target app (§139)** — `currentApp.substringAfterLast('.')` rendered in banner-tint 9 sp monospace below task text.
  12. **`TrainScreen` LoRA version "v" prefix (§140)** — `StatBadge` shows `"v{N}"` when adapter exists, plain number otherwise.
  **Files changed:** `ChatScreen.kt` (×3), `TrainScreen.kt` (×3), `GoalsScreen.kt` (×3), `ActivityScreen.kt`, `ModulesScreen.kt`, `AgentViewModel.kt` (×2), `ControlScreen.kt` (×2), `DiagnosticsScreen.kt`, `GAP_AUDIT.md`, `replit.md`.
- 2026-05-04 — **Round 18 — closed GAP_AUDIT §117–§128 (12 items):**
  1. **`ControlScreen` running-task banner (§117)** — When `!isIdle && currentTask.isNotBlank()`, a tinted pill-row (green/red/amber dot + truncated task text) renders below hardware meters card.
  2. **`ActivityScreen` action-log search state (§118)** — `var actionSearch` added to `ActionsList`; `filtered` now trims by `tool` or `nodeId` containment when non-blank.
  3. **`DashboardScreen` avg step-duration chip (§119)** — `SessionStat("ms/Step", "…")` chip appended to stats row when `avgStepDurationMs > 0`.
  4. **`TrainScreen` IRL extraction yield rate (§120)** — `"N% extraction yield"` line in accent monospace 10 sp below tuples row when `framesProcessed > 0`.
  5. **`DiagnosticsScreen` matching line count in header (§121)** — Header text shows `"N / M matching"` instead of total when `logFilter.isNotBlank()`.
  6. **`SafetyScreen` blocked-app count in section label (§122)** — `SafetySectionLabel` title becomes `"Blocked Apps (N)"` when blocklist is non-empty.
  7. **`AgentViewModel` `clearError()` resets `lastErrorAt` (§123)** — `clearError()` now sets `lastErrorAt = 0L` in addition to clearing status/lastError.
  8. **`GoalsScreen` active triggers count badge (§124)** — `"ACTIVE TRIGGERS"` header wrapped in `Row` with an Accent-tinted `"N"` badge.
  9. **`ControlScreen` queue fill progress bar (§125)** — 3 dp `LinearProgressIndicator` below estimated wait label; `progress = taskQueue.size / 20f`.
  10. **`ChatScreen` model-not-loaded warning banner (§126)** — Warning-tinted row with `Warning` icon shown between `ContextTagBar` and `HardwareMeterRow` when `!llmLoaded`.
  11. **`ActivityScreen` action-log search field UI (§127)** — Full-width `OutlinedTextField` (Search+Clear icons) between date-chip row and action list; empty state message adapts to search query.
  12. **`DashboardScreen` `SessionStat` color param + timeout chip (§128)** — `SessionStat` gains `valueColor` param; `"Timeouts"` chip shown in `ARIAColors.Warning` when `inferenceTimeoutCount > 0`.
  **Files changed:** `ControlScreen.kt` (×2), `ActivityScreen.kt` (×2), `DashboardScreen.kt` (×3), `TrainScreen.kt`, `DiagnosticsScreen.kt`, `SafetyScreen.kt`, `AgentViewModel.kt`, `GoalsScreen.kt`, `ChatScreen.kt`, `GAP_AUDIT.md`, `replit.md`.
- 2026-05-04 — **Round 17 — closed GAP_AUDIT §105–§116 (12 items):**
  1. **`OnboardingScreen` overall progress bar (§105)** — 3 dp `LinearProgressIndicator` at top of onboarding `Column`; advances from 0 → 100% across all 6 steps.
  2. **`TrainScreen` total adapter disk usage (§106)** — `LoraHistoryCard` header shows `totalKb = checkpoints.sumOf { it.sizeKb }` as `"X.X MB total"` in accent monospace below the adapter count.
  3. **`DashboardScreen` last-action preview (§107)** — `"↳ {lastAction.take(60)}"` in 9 sp monospace muted font shown inside the running status block before `StepActivityBar`.
  4. **`ControlScreen` queue estimated wait time (§108)** — `"Est. ~Nm wait (N tasks × ~2m avg)"` monospace label below queue-count chip when queue is non-empty.
  5. **`ActivityScreen` memory search bar (§109)** — `var memSearch` + `displayEntries` filtered by app/summary; full-width `OutlinedTextField` with Search+Clear icons as second `LazyColumn item` in `MemoryList`.
  6. **`AgentLoop` action-diversity stuck hint (§110)** — After `actionToolHistory.add(t)`, if last 3 tools are identical and `stuckHint` is blank, injects `"You called 't' three consecutive times… try a different tool"`.
  7. **`DiagnosticsScreen` log filter bar (§111)** — `var logFilter` added; when log is expanded, `OutlinedTextField` filter bar shown first, then `visibleLines` filtered by `logFilter` replaces raw `crashLines.takeLast(40)`.
  8. **`GoalsScreen` recently-completed count badge (§112)** — "RECENTLY COMPLETED" label wrapped in `Row` with a primary-tinted badge showing `"${recentGoals.size}"`.
  9. **`ControlScreen` queue-goal character count (§113)** — `"N / 200"` right-aligned monospace counter below queue-goal field; Muted → Warning (≥160) → Destructive (≥200).
  10. **`ChatScreen` scroll-to-bottom FAB (§114)** — `derivedStateOf { listState.canScrollForward }` drives a `SmallFloatingActionButton` overlaid at `BottomEnd` of the message `Box`; tap animates to latest message.
  11. **`AgentViewModel` `lastErrorAt` + Dashboard timestamp (§115)** — `lastErrorAt: Long = 0L` added to `AgentUiState`; set to `currentTimeMillis()` on error; DashboardScreen error panel shows `"at HH:mm:ss"`.
  12. **`GoalsScreen` Queue tab live count (§116)** — Tab label shows `"Queue (N)"` when `taskQueue.isNotEmpty()`, giving real-time task-count badge on the tab itself.
  **Files changed:** `OnboardingScreen.kt`, `TrainScreen.kt`, `DashboardScreen.kt`, `ControlScreen.kt` (×2), `ActivityScreen.kt`, `AgentLoop.kt`, `DiagnosticsScreen.kt`, `GoalsScreen.kt` (×2), `ChatScreen.kt`, `AgentViewModel.kt`, `GAP_AUDIT.md`, `replit.md`.
- 2026-05-02 — **Round 16 — closed GAP_AUDIT §93–§104 (12 items):**
  1. **`OnboardingScreen` step N-of-M counter (§93)** — `"Step N of M"` text added below dot indicators so users know their numeric position.
  2. **`TrainScreen` estimated samples processed (§94)** — `adamStep × 32` displayed as `"Est. samples processed: NK"` accent-coloured monospace line in `RlStatusCard`.
  3. **`SafetyScreen` preset N/M blocked badge (§95)** — `SensitiveCategoryRow` shows `"N/M"` count badge (warning=partial, red=all) replacing the text-only "PARTIAL" label.
  4. **`LabelerScreen` enrichment progress bar (§96)** — 3 dp `LinearProgressIndicator` shown below toolbar when `enriching == true`.
  5. **`AgentLoop` screen-power guard (§97)** — `PowerManager.isInteractive` checked after `step_started`; loop waits 3 s and skips inference when screen is off.
  6. **`DashboardScreen` last-task duration chip (§98)** — `lastTaskDurationMs` StateFlow collected and displayed as `SessionStat("Last Task", "Xm Ys")` chip in `SessionStatsCard`.
  7. **`ActivityScreen` edge-cases filter chip (§99)** — `FilterChip("Edge Cases (N)")` with amber selection added as first LazyColumn item; filters `displayEntries` to `isEdgeCase == true`.
  8. **`ControlScreen` goal character count (§100)** — Right-aligned `"N / 200"` monospace counter below goal field; amber at 160, red at 200.
  9. **`ChatScreen` message count in context line (§101)** — `userMsgCount` appended to `contextLine` parts (e.g. "3 msgs") when > 0.
  10. **`AgentViewModel` `lastTaskDurationMs` StateFlow (§102)** — Records wall-clock start on `running` entry and computes duration on `idle/done/error` exit; exposed as `StateFlow<Long>`.
  11. **`DiagnosticsScreen` crash file share button (§103)** — 28 dp share `IconButton` added per crash file row; launches `Intent.ACTION_SEND` with file content + filename as subject.
  12. **`GoalsScreen` search results count badge (§104)** — `"N of M templates"` label above grid when search is active; red when zero results.
  **Files changed:** `OnboardingScreen.kt`, `TrainScreen.kt`, `SafetyScreen.kt`, `LabelerScreen.kt`, `AgentLoop.kt`, `AgentViewModel.kt`, `DashboardScreen.kt`, `ActivityScreen.kt`, `ControlScreen.kt`, `ChatScreen.kt`, `DiagnosticsScreen.kt`, `GoalsScreen.kt`, `GAP_AUDIT.md`, `replit.md`.

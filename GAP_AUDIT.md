# ARIA Project — Comprehensive Gap Audit
> Last updated: May 02, 2026 (Round 11)  
> Status key: `[ ]` Open · `[~]` In Progress · `[x]` Done  
> Sections: [1 Stub Files](#1-stub-files) · [2 Fake Core Features](#2-fake-core-features-stubs-masquerading-as-real-logic) · [3 ViewModel Data Disconnects](#3-viewmodel--data-layer-disconnects) · [4 Agent Loop Failure Modes](#4-agent-loop-failure-modes) · [5 UI Screen Holes](#5-ui-screens-with-known-holes) · [6 System-Level Gaps](#6-system-level-gaps) · [7 Connection Gaps](#7-connection-gaps-things-that-should-talk-but-dont) · [8 Dashboard Not Wired](#8-web-dashboard-not-connected-to-its-own-backend) · [9 Reality Check Admissions](#9-aria_reality_checkmd--confirmed-but-untracked-gaps) · [10 Migration Debt](#10-legacy--migration-debt) · [11 Manifest & Build](#11-manifest--build-gaps) · [12 Missing Infrastructure](#12-missing-infrastructure) · [Priority](#priority-order-recommended)

---

## 1. Stub Files

Files that exist only as placeholders and actively mislead anyone reading the codebase.

| | File | Problem | Phase Target |
|--|------|---------|-------------|
| `[ ]` | `MainActivity.kt` | No-op migration stub. Still declared in `AndroidManifest.xml` as an activity. Anyone reading the manifest will think it does something. | Phase 8 |
| `[ ]` | `expo/modules/ExpoModulesPackageList.kt` | Kept only to satisfy old React Native package declarations. Dead weight with no purpose in a native build. | Phase 8 |

**Action:** Delete both only after the Phase 8 gate-check confirms all Compose screens are verified and nothing still imports them.

---

## 2. Fake Core Features (Stubs Masquerading as Real Logic)

The most critical gaps. The engine looks wired up but fires blanks.

| | File | What It Claims | What It Actually Does |
|--|------|---------------|-----------------------|
| `[ ]` | `core/ai/LlamaEngine.kt` | On-device LLM inference | Returns a hardcoded JSON string: `{"tool":"Click","node_id":"#1","reason":"stub inference — llama.cpp not compiled"}` |
| `[ ]` | `core/ai/LlamaEngine.kt` → `loadVision()` | Load SmolVLM vision model | Returns a sentinel handle and pre-canned text descriptions. No model is loaded. |
| `[ ]` | `core/rl/LoraTrainer.kt` | Fine-tune model on-device via LoRA | Calls `stubTrainLora()` — writes a metadata-only `.bin` file. No weights are ever updated. |
| `[x]` | `core/ai/InferenceEngineImpl.kt` | N/A — file does not exist in the codebase. The GAP_AUDIT entry was a ghost, likely referring to error-handling gaps inside `LlamaEngine.kt` directly. `LlamaEngine.kt` surfaces inference errors via return values and `Log.e`. No separate `InferenceEngineImpl.kt` was ever created. **Closed as N/A.** |
| `[x]` | `core/rl/PolicyNetwork.kt` | Intelligent action selection | Added explicit `outputBias` vector (size 7). On fresh install: tap=+0.3f, all others=0f — raises tap probability from 14.3% to ~21% at softmax. Adam optimizer trained on the bias alongside all other weights; old files load with zero-bias fallback. ~50 training episodes fully erases the prior if it is wrong. **Fixed.** |
| `[ ]` | `core/perception/ObjectDetectorEngine.kt` | Detect Android UI elements on screen | Uses **EfficientDet-Lite0**, which is trained on COCO categories (people, cars, dogs). It **cannot detect buttons, text fields, or any Android UI element**. Relies entirely on the Accessibility Tree for UI navigation. |
| `[ ]` | `core/rl/IrlModule.kt` → video path | Learn from video using LLM inference | Falls back to a **word-Jaccard heuristic** when LLM is not loaded — loses all coordinate data and only guesses action type from text differences. |

**Why this matters:** The agent loop runs, the UI shows activity, but no real intelligence is happening. A user or developer would not know this without reading deeply.

---

## 3. ViewModel & Data Layer Disconnects

The UI is built and shows these fields. The ViewModel has the state for them. But the underlying engines never actually send the data. Result: the UI always shows zeroes or stale defaults.

| | ViewModel Field | UI That Shows It | Who Should Update It | What Actually Updates It |
|--|----------------|-----------------|---------------------|--------------------------|
| `[x]` | `_loraTrainingProgress` | `TrainScreen.kt` (training progress bar) | `LoraTrainer.kt` via `reportLoraTrainingProgress()` | `runRlCycle()` in AgentViewModel now emits progress at 10% (loading_data), 25% (training), and 100%/0% (complete/failed). **Fixed.** |
| `[x]` | `reportLoraTrainingProgress()` | — | Should be called by `LoraTrainer` | Now called by `runRlCycle()` in AgentViewModel at each training phase transition. **Fixed.** |
| `[x]` | `clearLoraTrainingProgress()` | — | Should be called post-training | Called at 0% emission on completion/failure in `runRlCycle()`. **Fixed.** |
| `[x]` | `learningState.adamStep` | `DashboardScreen`, `ModulesScreen` | `LearningScheduler` via `AgentEventBus` `learning_cycle_complete` | `LearningScheduler.runTrainingCycle()` now emits `learning_cycle_complete` with `loraVersion`+`policyVersion`; `handleLearningComplete()` reads `PolicyNetwork.adamStepCount` directly. **Fixed.** |
| `[x]` | `learningState.lastPolicyLoss` | `DashboardScreen`, `ModulesScreen` | `LearningScheduler` via `AgentEventBus` `learning_cycle_complete` | Same fix — `handleLearningComplete()` reads `PolicyNetwork.lastPolicyLoss` directly when the event fires. **Fixed.** |
| `[x]` | `_thermalState` | `DashboardScreen` (warning banners) | `ThermalGuard.kt` via `AgentEventBus` event `"thermal_status_changed"` | `ThermalGuard.updateLevel()` now emits `"thermal_status_changed"` with `level`, `inferenceSafe`, `trainingSafe`, `emergency`. **Fixed.** |
| `[x]` | `ModuleUiState.tokensPerSecond` | `ModulesScreen.kt` | `refreshModuleState()` | Set from `LlamaEngine.lastToksPerSec` in `refreshModuleState()` at AgentViewModel line 660. **Fixed.** |

---

## 4. Agent Loop Failure Modes

Specific ways the Observe → Think → Act cycle breaks, loops forever, or fails silently with no user feedback.

### Silent Failures

| | Location | What Happens | Why It's a Problem |
|--|----------|-------------|-------------------|
| `[x]` | `GestureEngine.kt` `onCancelled` callback | All 5 callbacks (tap, swipe, longPress, tapXY, swipeXY) now log a `Log.w` with the cancelled node/coordinates so failures are visible in logcat. **Fixed.** | Agent can't distinguish "gesture worked but missed the target" from "OS blocked the gesture entirely." Action history is misleading. |
| `[x]` | `GestureEngine.kt` `executeFromJson` | Both malformed JSON (catch block) and unknown tool names (else branch) now emit `Log.w` with the full JSON so failures are visible in logcat. **Fixed.** | If the LLM consistently produces bad JSON, the agent silently fails every single step until it hits the 50-step limit, with no UI explanation of why. |
| `[x]` | `AgentAccessibilityService.kt` → `instance` is `null` | AgentLoop now hard-aborts at the top of every step when `AgentAccessibilityService.isActive` is false. Logs the abort reason, ends the task, and calls `recordAndChain`. **Fixed.** | Agent runs until `MAX_STEPS` burning battery and producing garbage. No hard-stop or user alert when the service dies mid-task. |

### Infinite / Near-Infinite Loop Risks

| | Location | What Happens | Why It's a Problem |
|--|----------|-------------|-------------------|
| `[x]` | `AgentLoop.kt` — `Wait` tool handling | Stuck detection (`stuckCount++`) now gates on `!isDoneNow && !isWaiting && !isBacking && currentHash == lastScreenHash`. Wait does not increment stuckCount and does not reset it — it is simply skipped. **Fixed.** | An LLM that hallucinates "Wait" repeatedly will never trigger the stuck-detection abort and will never hit `MAX_STEPS` quickly. The agent loops indefinitely. |
| `[x]` | `AgentLoop.kt` — screen hash comparison | `ScreenSnapshot.screenHashFuzzy()` strips digit sequences (`\b\d+(?:[,.]\d+)*\b`) before hashing. `AgentLoop` now tracks `lastScreenHashFuzzy` and uses it for stuck-detection comparisons; exact `screenHash()` is preserved for all DB keying. Counter increments, badge counts, and "X min ago" labels no longer reset `stuckCount`. **Fixed.** | Agent can waste all 50 `MAX_STEPS` on effectively the same screen without ever being flagged as stuck. |
| `[x]` | `AgentLoop.kt` — task chaining (`recordAndChain`) | `TaskQueueManager.MAX_QUEUE_SIZE = 20` enforced in `enqueue()` — returns `null` when at capacity. `AgentViewModel.enqueueTask()` logs the rejection. `ControlScreen` disables the Add-to-Queue button and shows `(N/20 — FULL)` in the queue header. **Fixed.** | If tasks keep being added to the queue (by user or by a buggy script), the agent runs indefinitely with no ceiling. |
| `[x]` | `AgentLoop.kt` — dead A11y node loop | Per-nodeId `nodeFailureMap` tracks consecutive `GestureEngine` failures. After `DEAD_NODE_THRESHOLD` (3) failures on the same nodeId, a `stuckHint` is injected into the LLM prompt instructing it to target a different element. Counter resets on success. **Fixed.** | If the LLM keeps targeting a node that no longer exists but is still in its context window, it loops until `MAX_STEPS` with every step silently failing. |

---

## 5. UI Screens With Known Holes

All 11 navigation routes exist. No missing screen files. But several screens have confirmed incomplete sections.

| | Screen | Gap | Severity |
|--|--------|-----|---------|
| `[x]` | `ControlScreen.kt` | Phase 4 gap-fill verified complete. All 7 checklist items in the file header (chained task banner, learn-only toggle, LLM Load Gate card, active task display, split queue fields, "Teach the Agent" entry point, status dot) are implemented and wired. **Fixed.** | Medium |
| `[x]` | `SettingsScreen.kt` line 66 | ~~`TODO (Phase 10)`~~ **Done.** "Web Dashboard" card (lines 436–503) added: shows server state, live URL, start/stop toggle, and clipboard copy. | Medium |
| `[x]` | `ModulesScreen.kt` | All three previously-claimed "placeholder" sections are actually wired: **App Skills** reads `vm.appSkills` StateFlow + per-app success rate / learned elements rows; **Vision Model** reads `modules.visionReady/visionLoaded/visionDownloadPercent` with download and load/unload buttons; **SAM2/MobileSAM** reads `modules.sam2Ready/sam2Loaded/sam2DownloadedMb` with the same controls. GAP_AUDIT entry was stale. **Closed as already done.** | Low–Medium |
| `[x]` | `GoalsScreen.kt` — Triggers tab | New `core/triggers/TriggerEvaluator.kt` provides the full runtime backend: TIME_DAILY/WEEKLY/ONCE via 60 s polling loop, APP_LAUNCH via `app_focus_changed` AgentEventBus events (emitted by `AgentAccessibilityService`), CHARGING via BroadcastReceiver. Fires by emitting `trigger_fired`; `AgentForegroundService` handles it and starts the agent loop. **Fixed.** | Low |
| `[x]` | `ChatScreen.kt` — Preset Prompt Chips | Static list replaced with `presetPromptsFor(status)` — chips now adapt to agent state: running → task-aware questions; error → failure-analysis prompts; idle → capability discovery. `remember(status)` ensures efficient recomposition. **Fixed.** | Low |
| `[ ]` | `SafetyScreen.kt` — `SENSITIVE_APP_PRESETS` | Hardcoded package names (e.g., `com.chase.sig.android`) for convenience blocklists. Not maintained or synced with any real source. | Low |
| `[x]` | `GoalsScreen.kt` — `GOAL_TEMPLATES` | Static grid is now supplemented by a live "RECENTLY COMPLETED" horizontal scroll row at the top of the Templates tab. Populated from `AgentViewModel.recentGoals` StateFlow (max 6 entries, deduped by goal text, fed by `handleStatusChanged` on running→idle/done transitions). One-tap to re-enqueue. **Fixed.** | Low |

---

## 6. System-Level Gaps

| | File | Gap | Risk |
|--|------|-----|------|
| `[x]` | `AgentAccessibilityService.kt` | `onInterrupt()` now sets `isActive = false` and clears `instance`. **Fixed.** | Medium — may leave the agent in a bad state after interruption |
| `[x]` | `core/ocr/ObjectLabelStore.kt` | `onUpgrade()` now drops and recreates the table. **Fixed.** | High — data corruption on update |
| `[x]` | `core/memory/ExperienceStore.kt` | `onUpgrade()` now drops both tables and recreates. **Fixed.** | High — data corruption on update |
| `[x]` | `AgentLoop.kt` Phase 14–19 markers | All integrated: Phase 14.1 PixelVerifier, 14.3 SustainedPerformanceManager, 14.4 ProgressPersistence, Phase 18 stuck detection (3/5/8-step thresholds with hint/Back/abort), Phase 19 TaskDecomposer + sub-task advancement. **Fixed.** | High — agent is incomplete by design |
| `[x]` | `ThermalGuard.kt` | `AgentLoop` checks `ThermalGuard.isEmergency()` every step: pauses 10 s then breaks if still hot. ThermalGuard also emits events to AgentEventBus for UI. **Fixed.** | High — device safety |

---

## 7. Connection Gaps (Things That Should Talk but Don't)

| | Gap | Impact |
|--|-----|--------|
| `[ ]` | `LlamaEngine` ↔ `llama.cpp` JNI | JNI functions are declared and C++ implementations exist and match. But the native library (`libllama-jni.so`) is **not compiled** without an explicit NDK build step. Without the `.so`, all inference stubs out. | Entire AI pipeline non-functional |
| `[x]` | Vision C++ code ↔ Kotlin side | `LlamaEngine.kt` now exposes `loadVision()`, `isVisionLoaded()`, `inferWithVision()`, `unloadVision()` with matching JNI declarations (`nativeInitVision`, `nativeRunVisionInference`, `nativeFreeVision`). `VisionEngine.kt` wraps these and is called in AgentLoop. **Fixed** (active once NDK `.so` compiles). | Vision feature entirely inaccessible |
| `[x]` | `LoraTrainer` ↔ `ExperienceStore` | `runRlCycle()` and `LearningScheduler` now pass `ObjectLabelStore.getInstance(context)` — human-annotated 3× weighted labels are included in training batches. **Fixed.** | Learning loop is an illusion |
| `[x]` | `LoraTrainer` ↔ `AgentViewModel` | Training progress events emitted from `runRlCycle()` in AgentViewModel at 10%/25%/100%/error transition points. **Fixed.** | Training progress bar always blank |
| `[x]` | `ThermalGuard` ↔ `AgentEventBus` | `ThermalGuard.updateLevel()` now emits `"thermal_status_changed"` with `level`, `inferenceSafe`, `trainingSafe`, `emergency`. **Fixed.** | Thermal warnings never appear in UI |
| `[x]` | `AgentLoop` ↔ `TaskDecomposer` | `AgentLoop` calls `TaskDecomposer.decompose(goal)` before the main loop begins, persists the plan via `ProgressPersistence.initGoals`, and advances sub-tasks when LLM returns `Done`. **Fixed.** | Agent cannot break complex goals into steps |
| `[x]` | `LocalDeviceServer` ↔ `aria-dashboard` frontend | Dashboard now has a **Live tab** that polls all 8 endpoints every 2 seconds. Device IP/port are configurable and persisted to localStorage. **Fixed.** | Dashboard shows fake data |

---

## 8. Web Dashboard Not Connected to Its Own Backend

This is a self-contained gap worth calling out specifically.

**What exists:**
- `LocalDeviceServer.kt` — a fully implemented lightweight HTTP server running on the Android device at port `8765`, with these live endpoints:
  - `GET /aria/status` — agent operational state
  - `GET /aria/thermal` — device thermal levels
  - `GET /aria/rl` — RL metrics (Adam steps, policy loss)
  - `GET /aria/lora` — LoRA adapter history
  - `GET /aria/memory` — embedding store stats
  - `GET /aria/activity` — recent action logs
  - `GET /aria/modules` — per-module readiness
  - `GET /health` — health check
- `LocalSnapshotStore.kt` — feeds live data to the server from `AgentViewModel`
- CORS headers set to `*` so a browser can call it

**What's missing:**
| | Item | Status |
|--|------|--------|
| `[x]` | `artifacts/aria-dashboard/src/pages/Dashboard.tsx` | New **Live tab** added. Polls all 8 endpoints every 2 seconds. Shows status, thermal, RL metrics, modules, and activity log. **Fixed.** |
| `[x]` | No environment config for device IP/port | Live tab has an IP/port input field that persists to `localStorage`. **Fixed.** |
| `[x]` | No entry point in `SettingsScreen` to start/view the server | `SettingsScreen.kt` lines 436–503: "Web Dashboard" section with start/stop toggle, live URL display, and one-tap clipboard copy. **Fixed.** |

---

## 9. `ARIA_REALITY_CHECK.md` — Confirmed but Untracked Gaps

The project's own honesty doc admits these. They are tracked here so they don't get lost.

| | Gap | Where Admitted |
|--|-----|---------------|
| `[~]` | LLM inference only works if NDK build + 870 MB model download are both complete. Default behavior is stub mode. **Round 8:** `LlamaEngine.isStubMode` property added; `ModuleUiState.isStubMode` wired from it; `ModulesScreen` shows a persistent amber warning banner whenever `isStubMode=true`; stub `runInference()` now returns keyword-aware JSON (Type/Swipe/Back/Wait/LongPress/Click) instead of a hardcoded Click. | `ARIA_REALITY_CHECK.md` |
| `[ ]` | Accessibility Service + MediaProjection require **manual user permission steps** before anything works. High friction, no in-app guidance. | `ARIA_REALITY_CHECK.md` |
| `[ ]` | "8–15 tok/s" and "1700 MB RSS" are **hardcoded estimates** in the C++ code, not live measurements from this build. | `ARIA_REALITY_CHECK.md` |
| `[ ]` | Policy Network starts with **random weights**. First N agent episodes are essentially random. | `ARIA_REALITY_CHECK.md` |
| `[ ]` | `EfficientDet-Lite0` cannot detect Android UI elements. The object detector is practically decorative for the agent's core use case. | `ARIA_REALITY_CHECK.md` |
| `[ ]` | IRL video training falls back to word-Jaccard heuristic without LLM — loses all coordinates, only guesses action type. | `ARIA_REALITY_CHECK.md` |
| `[ ]` | Phases 5 (`ChatScreen`), 6 (`TrainScreen`), 7 (`LabelerScreen`) are marked `[~] WRITTEN` but **not verified** on emulator or device. | `migration.md` |
| `[ ]` | `migration.md` Phase headers mark some items `✅ DONE` while the Reality Check table for the same items says `[~] written — needs emulator verify`. The two documents contradict each other. | `migration.md` vs `ARIA_REALITY_CHECK.md` |

---

## 10. Legacy / Migration Debt

| | Item | Notes |
|--|------|-------|
| `[ ]` | `artifacts/mobile/` (React Native screens) | Still present as "specs." Not running, but adds noise and can confuse contributors. Remove once all Kotlin counterparts are verified. | 
| `[ ]` | Phases 8 and 9 | **Not started.** Phase 8 = delete all `.tsx` files. Phase 9 = strip Expo/RN from the build system entirely. Gate-check prerequisites not yet met. |
| `[ ]` | `migration.md` phase tracking | Phases 1–19 tracked in a flat doc with no automated gate — easy to mark complete prematurely (and it has already happened). |
| `[x]` | `ActivityScreen.kt` line 42 comment | Stale "DO NOT DELETE logs.tsx" gate removed. KDoc updated to declare this screen the canonical implementation superseding the legacy RN `logs.tsx`. **Fixed.** |
| `[ ]` | `ModulesScreen.kt` in migration doc | Listed as `[~] written` in the Reality Check table but has no dedicated Phase entry for "filling its gaps," unlike every other screen. Falls through the cracks. |

---

## 11. Manifest & Build Gaps

| | Item | Gap |
|--|------|-----|
| `[x]` | `WAKE_LOCK` permission | **Fixed** — `LearningScheduler.runTrainingCycle()` acquires `PARTIAL_WAKE_LOCK` (2 h safety cap) before the training coroutine and releases it in `finally` + `cancelTraining()`. Permission is no longer orphaned. |
| `[x]` | ONNX Runtime via reflection | **Fixed** — `EmbeddingEngine` and `Sam2Engine` now use direct `ai.onnxruntime.*` imports. ProGuard keep rule covers `ai.onnxruntime.**`. No more silent `ClassNotFoundException` risk. |
| `[ ]` | No NDK build step in CI | The CMakeLists.txt and JNI bridge are correct, but there is no script or CI job that actually triggers `./gradlew assembleDebug` with NDK. The `.so` is never built automatically. |

---

## 12. Missing Infrastructure

| | Item | Why It Matters |
|--|------|---------------|
| `[~]` | No CI / automated build pipeline | **Round 8:** `.github/workflows/lint.yml` added — runs `./gradlew lint` on every push/PR (Ubuntu, JDK 17, no NDK required; stubs the `llama.cpp` submodule so CMake doesn't abort). NDK compile step still missing. |
| `[~]` | No unit or integration tests | **Round 9:** `android/app/src/test/java/com/ariaagent/mobile/SessionStatsUiStateTest.kt` (8 JUnit4 tests covering successRate, avgStepsPerTask, sessionDuration, copy) and `SafetyConfigTest.kt` (6 JUnit4 tests for customSensitivePackages, blockedPackages, flag toggling) added. No Android dependencies needed for either. NDK integration tests still open. |
| `[ ]` | No crash / error reporting | Agent runs as a foreground service. Silent failures (stub returns, empty `onUpgrade`, swallowed error codes, empty `onCancelled`) have zero observability in production. **Round 9 partial:** DiagnosticsScreen now reads and displays the last 60 lines of `app.log` (the FileLogWriter rolling file) with an Expand/Collapse toggle. |
| `[x]` | No device IP discovery for dashboard | **Round 9:** `LocalDeviceServer.startNsd(context)` registers the server as `ARIA-Device._aria._tcp` via `NsdManager` each time the server starts. `AgentViewModel.toggleLocalServer()` calls `startNsd` automatically. mDNS-capable dashboards/browsers on the same LAN can resolve `aria-device.local:8765` without manual IP entry. |
| `[x]` | No in-app guide for required permissions | **Round 9:** `ControlScreen` now shows an amber "PERMISSIONS NEEDED" banner below the hardware meters whenever accessibility service or screen-capture permission is missing. Each missing permission has a label + detail row; the accessibility row has a live "Fix →" deep-link that opens Android Accessibility Settings directly. |

---

## Priority Order (Recommended)

Items are ranked by impact and safety risk.

| # | Item | Status | Why First |
|---|------|--------|-----------|
| 1 | **Build the NDK / JNI library** | `[ ]` | Nothing real works until `libllama-jni.so` compiles. All AI gaps depend on this. |
| 2 | **Fix both `onUpgrade()` stubs** (`ObjectLabelStore`, `ExperienceStore`) | `[x]` **Done** | Silent data corruption on every app update. |
| 3 | **Hook `ThermalGuard` into `AgentLoop`** as hard abort | `[x]` **Done** | AgentLoop already had the 10s pause + break check; EventBus emission from ThermalGuard was the missing piece (fixed in session 2). |
| 4 | **Add hard-abort when Accessibility Service is null** | `[x]` **Done** | Guard added at top of every step iteration in AgentLoop: checks `AgentAccessibilityService.isActive`, calls `recordAndChain` and breaks immediately if dead. |
| 5 | **Fix empty `onCancelled` in GestureEngine** | `[x]` **Done** | All 5 gesture callbacks now log OS cancellations. |
| 6 | **Wire `ThermalGuard` → `AgentEventBus`** | `[x]` **Done** | Thermal warnings in the UI now fire from real data. |
| 7 | **Wire `LoraTrainer` → `reportLoraTrainingProgress()`** | `[x]` **Done** | `runRlCycle()` in AgentViewModel now emits progress at 10% (loading_data), 25% (training), and 100%/0% (complete/failed). Error path also emits "failed". |
| 8 | **Wire `Dashboard.tsx` to `LocalDeviceServer`** | `[x]` **Done** | Live tab added with real polling, IP config, and all 8 endpoints wired. |
| 9 | **Wire `LoraTrainer` → `ExperienceStore` + `ObjectLabelStore`** | `[x]` **Done** | Both callers (`runRlCycle` in ViewModel, `LearningScheduler`) now pass `ObjectLabelStore.getInstance(context)` — human-annotated 3× weighted labels are no longer silently dropped from training. |
| 10 | **Expose vision C++ code to Kotlin** | `[x]` **Done** | `LlamaEngine.kt` exposes `loadVision()`, `inferWithVision()`, `unloadVision()` with matching JNI declarations. `VisionEngine.kt` wraps them; `AgentLoop` calls `VisionEngine.describe()` every step. Active once `libllama-jni.so` compiles. |
| 11 | **Fix `onInterrupt()` in `AgentAccessibilityService`** | `[x]` **Done** | Service now clears state properly when interrupted. |
| 12 | **Fix `WAKE_LOCK` orphan** | `[x]` **Done** | `LearningScheduler` now acquires `PARTIAL_WAKE_LOCK` (2 h timeout) before launching the training coroutine and releases it in `finally` and `cancelTraining()`. Permission in manifest is now backed by real code. |
| 13 | **Replace ONNX reflection calls with direct imports** | `[x]` **Done** | `EmbeddingEngine` and `Sam2Engine` now use `ai.onnxruntime.OrtEnvironment`, `OrtSession`, `OnnxTensor` directly. ProGuard keep rule updated to cover `ai.onnxruntime.**`. No more `ClassNotFoundException` risk. |
| 14 | **Verify Phases 5, 6, 7 on emulator** | `[ ]` | Three screens are "written but not verified." Gate Phase 8 on this. |
| 15 | **Reconcile `migration.md` vs `ARIA_REALITY_CHECK.md`** | `[x]` | `ui-backend-audit.md` Part B rewritten — all 7 "Gap:" entries updated to "✅ Fixed". `ARIA_REALITY_CHECK.md` Q9 corrected (vision API exists). Remaining `[~]` items in migration.md are accurate (emulator verify still needed). |
| 16 | **Complete `AgentLoop` Phase 14 integration** | `[x]` **Done** | All integrated: PixelVerifier (14.1), SustainedPerformanceManager (14.3), ProgressPersistence (14.4), stuck detection (Phase 18), TaskDecomposer + sub-task loop (Phase 19). |
| 17 | **Add Web Dashboard entry point in SettingsScreen** | `[x]` **Done** | `SettingsScreen.kt` "Web Dashboard" section (lines 436–503) shows a start/stop toggle and copies the server URL to clipboard when running. |
| 18 | **Build Triggers feature in GoalsScreen** | `[x]` **Done** | `TriggerEvaluator` backend added in `core/triggers/`; `app_focus_changed` wired in `AgentAccessibilityService`; `trigger_fired` handler wired in `AgentForegroundService`. All five trigger types (TIME_ONCE/DAILY/WEEKLY, APP_LAUNCH, CHARGING) now fire end-to-end. |
| 19 | **Phase 8 cleanup** — delete `MainActivity`, `ExpoModulesPackageList`, `artifacts/mobile/` | `[ ]` | Remove dead weight once verification gates pass. |
| 20 | **Add CI, NDK build step, and test gates** | `[~]` | **Round 8:** Kotlin lint CI added (`.github/workflows/lint.yml`). NDK build step and test gates still open. |
| 21 | **Session stats surface on Dashboard** | `[x]` **Done** | `SessionStatsUiState` data class + `_sessionStats` StateFlow added to AgentViewModel. `handleStatusChanged` increments `tasksCompleted`/`tasksErrored` and accumulates `totalSteps` on running→idle/done/error transitions. `DashboardScreen` shows a `SessionStatsCard` (Done / Errors / Steps / Success% + avg steps/task + session duration) visible after the first task completes. |
| 22 | **`SafetyConfig` user-defined sensitive apps** | `[x]` **Done** | `SafetyConfig.customSensitivePackages: Set<String>` added. `addCustomSensitivePackage`/`removeCustomSensitivePackage` VM functions added (mirror blocklist pattern). `SafetyScreen` gains a §6 "User-Added Sensitive Apps" card with add/remove/block/unblock per-package controls. |
| 23 | **`LoraTrainer.TrainingResult.isStubMode`** | `[x]` **Done** | Field added; return site sets `isStubMode = !jniSucceeded` so callers know whether real weights were updated. |
| 24 | **`IrlModule.inferActionHeuristic` improved** | `[x]` **Done** | Candidate-word extraction (longest meaningful changed word used as `node_id` hint); TYPE detection (single short alphanumeric new token + no lost words → Type action); asymmetric Back threshold loosened (`lostWords > newWords + 2`). Eliminates "Click #1" as the answer for every scenario. |
| 25 | **Battery-aware `LearningScheduler`** | `[x]` **Done** (Round 9) | `maybeStartTraining()` now reads `BatteryManager.BATTERY_PROPERTY_CAPACITY` directly and skips training when battery < 20%. Also skips when free RAM < 900 MB via `ActivityManager.MemoryInfo`. Both skips emit `training_skipped_low_battery` / `training_skipped_low_ram` bus events for dashboard observability. |
| 26 | **NSD/mDNS device discovery** | `[x]` **Done** (Round 9) | `LocalDeviceServer.startNsd(context)` registers `ARIA-Device._aria._tcp` via Android `NsdManager`. Called automatically from `AgentViewModel.toggleLocalServer()`. `stopNsd()` unregisters on server stop. Browsers on the same LAN can auto-discover the monitoring server. |
| 27 | **ControlScreen permission banner** | `[x]` **Done** (Round 9) | Amber `PERMISSIONS NEEDED` card shown below hardware meters when a11y or screen-capture is missing. Accessibility row is tappable (deep-links to Accessibility Settings via `ACTION_ACCESSIBILITY_SETTINGS`). `PermissionRow` composable added to private helpers. |
| 28 | **DiagnosticsScreen crash log viewer** | `[x]` **Done** (Round 9) | New "ON-DEVICE LOG" section reads the last 60 lines of `{filesDir}/logs/app.log` on a background dispatcher. Expand/Collapse toggle. Monospace rendering with line height optimised for log content. |
| 29 | **AgentViewModel `batteryLevel` StateFlow** | `[x]` **Done** (Round 9) | `_batteryLevel: MutableStateFlow<Int>` populated by `refreshBatteryLevel()` (called in `init` and available for UI call sites). `permissionsAllGood: StateFlow<Boolean>` derived from `_moduleState.map` with `SharingStarted.Eagerly`. `kotlinx.coroutines.flow.map` import added. |
| 30 | **SettingsScreen Build Info card** | `[x]` **Done** (Round 9) | New "Build Info" `SettingsCard` shows Device model, Android API level, ABI, Inference mode (Stub vs Native JNI), and stub-mode flag. Stub mode cell highlighted amber when active. |
| 31 | **First unit tests** | `[x]` **Done** (Round 9) | `android/app/src/test/java/com/ariaagent/mobile/SessionStatsUiStateTest.kt` (8 tests) + `SafetyConfigTest.kt` (6 tests) — pure JVM, no Android dependency, cover successRate, avgStepsPerTask, sessionDuration, copy semantics, and all SafetyConfig field combinations. |
| 32 | **GAP_AUDIT §11 consistency fix** | `[x]` **Done** (Round 9) | `WAKE_LOCK` and `ONNX reflection` rows in §11 updated from `[ ]` to `[x]` — now consistent with the Priority table entries #12 and #13. |
| 33 | **`HardwareStats.batteryPercent` field** | `[x]` **Done** (Round 10) | `HardwareStats` data class gains `batteryPercent: Int = -1`. `HardwareMonitor.statsFlow()` reads battery via `ACTION_BATTERY_CHANGED` sticky broadcast (handles -1 / emulator gracefully). `BatteryManager` + `IntentFilter` imports added. |
| 34 | **Battery meter in `HardwareMeterBar`** | `[x]` **Done** (Round 10) | New `BatteryMeterBar` composable (inverted color: green ≥50%, amber 20–49%, red <20%). Added as 4th bar in `HardwareMeterRow` (only shown when `batteryPercent ≥ 0`). `HardwareMiniStrip` gains a `BAT` chip with `invertScale=true`. Private `batteryColor()` helper added. |
| 35 | **`SafetyScreen` SENSITIVE_APP_PRESETS expansion** | `[x]` **Done** (Round 10) | Presets grown from 4 to 7 categories. New categories: **Crypto & Investing** (Coinbase, Kraken, Binance, Robinhood, E*Trade, Webull, SoFi, Blockchain.com), **Email & Cloud Storage** (Gmail, Outlook, ProtonMail, Fastmail, Google Drive, OneDrive, Box), **Government & ID** (SSA, IRS2Go, CBP ONE, TSA PreCheck, state ID apps). Existing 4 categories each doubled with popular alternatives. |
| 36 | **`CrashHandler` crash notification** | `[x]` **Done** (Round 10) | `install()` now calls `ensureNotificationChannel()` upfront. On each uncaught exception, `postCrashNotification()` posts a `NotificationCompat.PRIORITY_HIGH` notification (channel `aria_crash`) with exception class + message in BigText style. Tap re-opens ARIA. `writeFile()` now returns `File` so notification can show the filename. All notification calls are best-effort (wrapped in try/catch) — crash handler never double-faults. |
| 37 | **`AgentForegroundService` exponential backoff** | `[x]` **Done** (Round 10) | Fixed `RETRY_DELAY_MS=5s` replaced with exponential: `RETRY_BASE_DELAY_MS=2s`, `RETRY_MAX_DELAY_MS=30s`. Retry #1=2s, #2=4s, #3=8s. Notification shows countdown ("retrying in Xs"). `Log.w` records attempt number + delay. |
| 38 | **`PolicyNetwork` corrupted-file bounds check** | `[x]` **Done** (Round 10) | `loadFromBinary()` now validates each size field against the expected dimension (`HIDDEN1*INPUT_DIM`, `HIDDEN2*HIDDEN1`, `OUTPUT_DIM*HIDDEN2`) before allocating the array. Negative, zero, or mismatched sizes cause an immediate `initRandom()` fallback with a `Log.w`. Prevents OOM from maliciously or accidentally corrupted weight files. |
| 39 | **`LocalSnapshotStore` battery + uptime fields** | `[x]` **Done** (Round 10) | `defaultStatus()` gains `"batteryPercent": -1` and `"uptimeSec": 0L`. `MonitoringPusher.buildStatus()` populates both live values: battery via `ACTION_BATTERY_CHANGED` sticky broadcast; uptime via `SystemClock.elapsedRealtime()`. Web dashboard `/aria/status` now surfaces these fields. |
| 40 | **`MonitoringPusher` BatteryManager imports** | `[x]` **Done** (Round 10) | `android.content.Intent`, `IntentFilter`, `android.os.BatteryManager` added to `MonitoringPusher.kt` to support the battery read in `buildStatus()`. |
| 41 | **`AgentEventBus` in-memory ring buffer** | `[x]` **Done** (Round 11) | `_history: ArrayDeque<Triple<String, Map, Long>>` (150 entries) with thread-safe `synchronized(historyLock)`. `recentEvents` property returns an immutable snapshot ordered oldest→newest. `clearHistory()` for tests. `emit()` appends to ring before broadcasting on SharedFlow. Late-arriving UI screens can now display past events without needing a replay SharedFlow. |
| 42 | **`AgentLoop` LLM inference hard timeout** | `[x]` **Done** (Round 11) | `LLM_INFERENCE_TIMEOUT_MS = 90_000L` constant added. `withTimeoutOrNull()` wraps the entire `LlamaEngine.infer()` / `inferWithVision()` call inside the existing `try-finally` (bitmap still recycled on timeout). Null return skips the step: logs to `ProgressPersistence`, emits `inference_timeout` event, sleeps `STEP_DELAY_MS`, then `continue`s. Prevents a JNI hang from blocking the loop permanently. |
| 43 | **`AgentLoop` step duration telemetry** | `[x]` **Done** (Round 11) | `stepStartMs = System.currentTimeMillis()` captured before the Observe phase. `"stepDurationMs" to (now - stepStartMs)` added to the `action_performed` event payload — reflects full observe→reason→act wall-clock latency including LLM inference time. |
| 44 | **`AgentViewModel` periodic battery polling** | `[x]` **Done** (Round 11) | `viewModelScope.launch(Dispatchers.IO) { while(true) { delay(60_000); refreshBatteryLevel() } }` added to `init`. Keeps dashboard battery chip live during idle/paused states when no agent events arrive. |
| 45 | **`AgentViewModel` `avgStepDurationMs` EMA** | `[x]` **Done** (Round 11) | `avgStepDurationMs: Long = 0L` added to `SessionStatsUiState`. `handleActionPerformed` reads `stepDurationMs` from event payload and updates via α≈0.2 exponential moving average (`(prev*4 + new) / 5`). Smooths noisy individual readings while converging quickly after model loads. |
| 46 | **`ProgressPersistence.pruneOldLogs()`** | `[x]` **Done** (Round 11) | New `pruneOldLogs(context, daysToKeep=7)` trims `aria_progress.txt` lines older than the retention window. Timestamp regex `\[YYYY-MM-DD HH:mm:ss\]` used for line dating; blank/separator lines (no timestamp) always kept. Thread-safe via `synchronized(PROGRESS_FILE)`. Called once on cold start from `AgentViewModel.init`. |
| 47 | **`DiagnosticsScreen` crash file list** | `[x]` **Done** (Round 11) | New "CRASH REPORTS" section below the log viewer. Calls `CrashHandler.listCrashes()` on IO dispatcher. One expandable `ARIACard` per file: filename (monospace), file size in KB, relative timestamp. "View/Collapse" toggle reads file text on first expand. Header turns red when any crash files exist. |
| 48 | **`DashboardScreen` avg step duration chip** | `[x]` **Done** (Round 11) | `SessionStatsCard` footer line now appends `• Xms/step` when `avgStepDurationMs > 0`. Shows only after at least one completed task so the initial 0 value is never displayed. |
| 49 | **`AgentLoop` consecutive-timeout abort** | `[x]` **Done** (Round 12) | `MAX_CONSECUTIVE_TIMEOUTS = 3` constant added. `consecutiveTimeouts` counter incremented on each timeout; reset to 0 on successful inference. After 3 back-to-back timeouts the loop sets `status = ERROR`, logs task end, calls `SustainedPerformanceManager.disable()`, emits `agent_status_changed`, and breaks — prevents the loop from spinning indefinitely on a wedged or OOM-killed model. |
| 50 | **`step_started` event enriched** | `[x]` **Done** (Round 12) | `AgentLoop` now adds `"appPackage"` and `"goalText"` fields to the `step_started` event payload (alongside the existing `stepNumber` + `activity`). Downstream subscribers and analytics tools can correlate steps to the goal + target app without additional lookups. |
| 51 | **`SessionStatsUiState.inferenceTimeoutCount`** | `[x]` **Done** (Round 12) | New `inferenceTimeoutCount: Int = 0` field added to `SessionStatsUiState`. `handleInferenceTimeout()` increments it on every `inference_timeout` event. `resetSession()` resets it to 0. |
| 52 | **`AgentViewModel.handleInferenceTimeout()`** | `[x]` **Done** (Round 12) | New private handler added; wired to `"inference_timeout"` in the AgentEventBus `when` dispatch table. Increments `SessionStatsUiState.inferenceTimeoutCount` via `_sessionStats.update`. |
| 53 | **`AgentViewModel.resetSession()`** | `[x]` **Done** (Round 12) | Clears `_actionLogs`, `_sessionStats` (fresh `SessionStatsUiState` with new `sessionStartMs`), `_streamBuffer`, and `_stepState` — reclaims Compose list memory without stopping the running agent. |
| 54 | **`ControlScreen` "RESET SESSION" button** | `[x]` **Done** (Round 12) | Small `TextButton` (Refresh icon + "RESET SESSION" label, right-aligned) added below the Start/Stop/Pause row. Guarded by an `AlertDialog` confirmation. Calls `vm.resetSession()` on confirm. |
| 55 | **`ActivityScreen` date-filter chips** | `[x]` **Done** (Round 12) | `ActionDateFilter` enum (`ALL` / `TODAY` / `WEEK`) added. `ActionsList` now renders a `LazyRow` of `FilterChip`s above the action list. Filtered count badge ("N / Total") shown when a filter is active. `remember(logs, dateFilter)` derivation is O(n) but scoped to the chip row — main list recomposition unchanged. |
| 56 | **`DiagnosticsScreen` "Clear All" crash button** | `[x]` **Done** (Round 12) | "CRASH REPORTS" header row gains a `TextButton("Clear All")` (red label, only visible when files exist). Tap opens `AlertDialog` confirmation; on confirm deletes all `File` objects returned by `CrashHandler.listCrashes()` and clears the local state list. |
| 57 | **`DiagnosticsScreen` progress.txt size chip** | `[x]` **Done** (Round 12) | `ProgressPersistence.logFileSizeBytes(context)` called on IO dispatcher via `LaunchedEffect`. Result shown as a monospace `"progress.txt: X.X KB"` label in the ON-DEVICE LOG header row. Helps users spot a runaway log before it fills device storage. |
| 58 | **`DashboardScreen` timeout count in stats footer** | `[x]` **Done** (Round 12) | `SessionStatsCard` footer text appends `• N timeout(s)` in amber (`ARIAColors.Warning`) when `inferenceTimeoutCount > 0`. Footer is now shown even if `tasksCompleted == 0` as long as any timeouts have occurred — makes model instability visible immediately. |
| 59 | **`NetworkMonitor` connectivity helper** | `[x]` **Done** (Round 13) | New `core/system/NetworkMonitor.kt` singleton. `connectionType(context): ConnectionType` returns `WIFI`, `MOBILE`, or `NONE` via `ConnectivityManager.getNetworkCapabilities()` (API 21+). Handles VPN transport, ETHERNET, and catch-all `NONE` gracefully. `isOnline(context): Boolean` convenience wrapper. |
| 60 | **`AgentViewModel.networkType` StateFlow + poll** | `[x]` **Done** (Round 13) | `_networkType: MutableStateFlow<String>` (`"wifi"` / `"mobile"` / `"none"`) added. `init` launches a background IO coroutine that polls `NetworkMonitor.connectionType()` every 30 s so the chip stays live without a `NetworkCallback` lifecycle concern. |
| 61 | **`DashboardScreen` network status chip** | `[x]` **Done** (Round 13) | Bottom of the Hardware card shows a colour-coded monospace chip: `NET  WiFi` (green) / `NET  Mobile` (amber) / `OFFLINE` (red). Driven by `vm.networkType` StateFlow. Alerts the user immediately if the device goes offline before starting a web-dependent task. |
| 62 | **`ThermalGuard.pauseDurationMs()` scaled pause** | `[x]` **Done** (Round 13) | New `pauseDurationMs(): Long` public method: SAFE/LIGHT → 0 ms, MODERATE → 5 000 ms, SEVERE → 15 000 ms, CRITICAL → 30 000 ms. `AgentLoop` thermal pause now uses `ThermalGuard.pauseDurationMs().coerceAtLeast(10_000L)` instead of a hardcoded 10 s flat delay — CRITICAL devices cool down properly before inference restarts. |
| 63 | **`AgentLoop` vision description cache** | `[x]` **Done** (Round 13) | `var lastVisionDescription = ""` added to run-local vars. Before calling `VisionEngine.describe()`, the loop checks `snapHash == lastScreenHash && lastVisionDescription.isNotBlank()`. On a cache hit it reuses the cached description, saving ~400 ms of SmolVLM inference per unchanged step. Cache is updated whenever vision runs successfully on a new hash. |
| 64 | **`AgentLoop` JSON parse-failure retry** | `[x]` **Done** (Round 13) | `MAX_PARSE_RETRIES = 2` constant added. After `PromptBuilder.parseAction()` returns a fallback Wait (`"reason":"no action parsed"` / `"malformed json"`), the loop re-calls `LlamaEngine.infer()` with a terse repair prompt (`maxTokens=80`, `temperature=0.05`) up to 2 times. Prevents a prompt-formatting glitch from wasting an entire step as a no-op. |
| 65 | **`AgentLoop` `stuck_abort` `DONE`→`ERROR` fix** | `[x]` **Done** (Round 13) | Bug: `stuck_abort` was setting `status = Status.DONE` (falsely reporting success). Fixed to `status = Status.ERROR`. This correctly increments `tasksErrored` in session stats and triggers the task-failure notification. |
| 66 | **`AgentLoop` stuckCount ≥ 6 Home press recovery** | `[x]` **Done** (Round 13) | New recovery tier between the existing Back-press (≥5) and abort (≥8). At stuckCount ≥ 6 the loop calls `AgentAccessibilityService.performHome()` and injects a prompt hint directing the model to reopen the target app and try a different strategy. `performHome()` added to `AgentAccessibilityService` companion object. |
| 67 | **`AgentViewModel` task completion notifications** | `[x]` **Done** (Round 13) | `postTaskNotification(title, body)` helper added — posts a `NotificationCompat.PRIORITY_DEFAULT` notification on `"aria_agent_reasoning"` channel (reuses existing channel, no new permission needed). Tapped notification re-opens ARIA. Called from `handleStatusChanged` on `done` and `error` transitions, giving the user an OS-level alert even when the app is backgrounded. |
| 68 | **`ActivityScreen` share action log button** | `[x]` **Done** (Round 13) | `IconButton` with `Icons.Default.Share` (accent colour) appears in the ACTIVITY header when the Actions tab is active and the log is non-empty. Tap formats the last 100 log entries as `[HH:mm:ss] TOOL: nodeId (ok/fail)` and launches `Intent.ACTION_SEND` via the system share sheet. |
| 69 | **Chat history persistence** | `[x]` **Done** (Round 13) | `saveChatHistory()` serialises the non-system chat messages (max 100) to `{filesDir}/aria_chat.json` after each successful LLM response and on `clearChat()`. `loadChatHistory()` restores them on ViewModel cold start using a regex-based parser that handles JSON escaping correctly. Conversation now survives app restarts. |
| 70 | **`AgentLoop` adaptive step delay** | `[x]` **Done** (Round 14) | `STEP_DELAY_FAST_MS = 400L` constant added. `consecutiveSuccesses` counter incremented after each successful action and reset to 0 on failure. End-of-loop `delay()` uses `STEP_DELAY_FAST_MS` when `consecutiveSuccesses ≥ 3`, halving latency during momentum runs and falling back to `STEP_DELAY_MS (800 ms)` on any hiccup. |
| 71 | **`AgentLoop` A11y abort `DONE`→`ERROR` fix** | `[x]` **Done** (Round 14) | `accessibility_service_dead` abort path was incorrectly setting `status = Status.DONE` (falsely implying success). Fixed to `Status.ERROR` so session stats correctly increment `tasksErrored` and the failure notification fires. |
| 72 | **`AgentLoop` exception class name in error field** | `[x]` **Done** (Round 14) | `catch (e: Exception)` handler now stores `"${e.javaClass.simpleName}: ${e.message ?: "unknown"}"` in `lastError` instead of just `e.message`. `ProgressPersistence.logNote` receives the same enriched string. Distinguishes `NullPointerException` from `IOException` in crash reports without needing a logcat. |
| 73 | **`ControlScreen` recent-goals chips** | `[x]` **Done** (Round 14) | `RecentGoalItem` import added. `val recentGoals by vm.recentGoals.collectAsStateWithLifecycle()` collected in composable. When `recentGoals.isNotEmpty() && isIdle`, a horizontal `LazyRow` of `AssistChip`s (up to 5) is rendered below the goal `OutlinedTextField`. Tapping a chip populates both `goalText` and `targetApp` in one tap. |
| 74 | **`ControlScreen` package auto-suggest chips** | `[x]` **Done** (Round 14) | `PACKAGE_SUGGESTIONS` private val maps 12 English keywords (youtube, chrome, settings, maps, gmail, whatsapp, instagram, calculator, camera, gallery, spotify, twitter) to canonical package names. `packageSuggests = remember(goalText)` filters to matching entries when goal text ≥ 3 chars. `SuggestionChip` row appears below the target-app field when matches exist and the field is blank; tapping auto-fills the package. |
| 75 | **`AgentViewModel` uptime timer `StateFlow`** | `[x]` **Done** (Round 14) | `_uptimeSeconds: MutableStateFlow<Long>` + `uptimeJob: Job?` added. `handleStatusChanged` starts a `viewModelScope` coroutine incrementing `_uptimeSeconds` every second when status transitions to `"running"` (resetting to 0) and cancels it on any non-running transition. Exposes `uptimeSeconds: StateFlow<Long>`. |
| 76 | **`DashboardScreen` uptime chip** | `[x]` **Done** (Round 14) | `val uptimeSeconds by vm.uptimeSeconds.collectAsStateWithLifecycle()` collected. While `agentState.status == "running"` a third `MetricChip(Icons.Default.Timer, "M:SS")` is shown alongside the token-rate and step-count chips, giving a live wall-clock view of how long the current task has been running. |
| 77 | **`DiagnosticsScreen` device / system info card** | `[x]` **Done** (Round 14) | `ActivityManager` + `Build` imports added. New `ARIACard` inserted before the Orchestrator header. Reads `ActivityManager.MemoryInfo` once via `remember` and shows 6 rows: Model (`MANUFACTURER MODEL`), Android (`X.Y (API NN)`), ABI, Total RAM (MB), Available RAM (MB + % free), Low RAM flag. `DiagInfoRow` private composable added for consistent key/value layout. |
| 78 | **`SettingsScreen` export configuration button** | `[x]` **Done** (Round 14) | `OutlinedButton` inserted below the Save Configuration button. On tap, serialises `AriaConfig` to a 13-field JSON string and fires `Intent.ACTION_SEND` (MIME `text/plain`) through the system share sheet — no `FileProvider` complexity needed. Subject line set to `"ARIA Configuration Export"`. |
| 79 | **`ActivityScreen` memory avg-confidence chip** | `[x]` **Done** (Round 14) | `avgConf = remember(entries)` derivation added to `MemoryList` — computes `(Σ reward.coerceIn(0,1) / n) * 100` as `Int`. New `MemStatChip("Avg Conf", "$avgConf%", ARIAColors.Primary)` appended as the 6th chip in the stats bar (alongside Total / Success / Fail / Edge / Untrained). |
| 80 | **`AgentLoop` `CancellationException` rethrow** | `[x]` **Done** (Round 14) | Top-level `catch (e: Exception)` block now checks `if (e is CancellationException) throw e` before any error handling. Previously a structured-concurrency cancellation would be swallowed, leaving the agent coroutine in a zombie state. Fix ensures `AgentForegroundService.stopSelf()` + `Job.cancel()` cleanly terminate the loop. |

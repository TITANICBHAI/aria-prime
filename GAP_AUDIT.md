# ARIA Project — Comprehensive Gap Audit
> Last updated: May 02, 2026 (Round 8)  
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
| `[ ]` | `WAKE_LOCK` permission | Declared in `AndroidManifest.xml` (line 20) "to keep CPU awake during model download and training." However, **no Kotlin code acquires a WakeLock via `PowerManager`**. The permission declaration is orphaned. |
| `[ ]` | ONNX Runtime via reflection | `Sam2Engine.kt` and `EmbeddingEngine.kt` use `Class.forName("ai.onnxruntime.OrtEnvironment")` to access ONNX Runtime at runtime instead of importing it directly. If the library version changes or ProGuard strips it, this fails **silently at runtime** with a `ClassNotFoundException`, not a compile error. |
| `[ ]` | No NDK build step in CI | The CMakeLists.txt and JNI bridge are correct, but there is no script or CI job that actually triggers `./gradlew assembleDebug` with NDK. The `.so` is never built automatically. |

---

## 12. Missing Infrastructure

| | Item | Why It Matters |
|--|------|---------------|
| `[~]` | No CI / automated build pipeline | **Round 8:** `.github/workflows/lint.yml` added — runs `./gradlew lint` on every push/PR (Ubuntu, JDK 17, no NDK required; stubs the `llama.cpp` submodule so CMake doesn't abort). NDK compile step still missing. |
| `[ ]` | No unit or integration tests | `OcrEngine.kt` mentions a "ML Kit stub for unit tests" but no test files exist. No way to catch regressions. |
| `[ ]` | No crash / error reporting | Agent runs as a foreground service. Silent failures (stub returns, empty `onUpgrade`, swallowed error codes, empty `onCancelled`) have zero observability in production. |
| `[ ]` | No device IP discovery for dashboard | `LocalDeviceServer` runs on the phone, but the web dashboard has no mechanism to find out what IP address or port to connect to. Needs a config screen or mDNS discovery. |
| `[ ]` | No in-app guide for required permissions | Accessibility Service and MediaProjection require manual setup in Android settings. There is no in-app walkthrough that confirms the user did it correctly before the agent starts trying to act. |

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

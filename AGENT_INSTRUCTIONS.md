# Agent Instructions — read this first

You are an AI (or human) picking up `aria-prime` to continue the work. This document is your operating manual. Follow it in order.

## 0. Mental model

This repo has **two layers**:

```
android/              ← live Kotlin spine you build, run, and ship
   └── app/src/main/kotlin/com/ariaagent/...   (~71 .kt files, ~26.5K LOC)

donors/               ← read-only reference code that came from sibling repos
   ├── orchestration-java/              (~14 files, ~2,000 LOC)  ← from AII
   ├── orchestration-components-java/   (~5 files, ~800 LOC)     ← from AII
   ├── rl-algorithms-java/              (~12 files, ~3,500 LOC)  ← from AII
   ├── scheduler-java/                  (~46 files, ~7,000 LOC)  ← from SmartAssistant
   ├── perception-java/                 (~25 files, ~4,000 LOC)  ← from SmartAssistant
   ├── learning-video-java/             (~3 files, ~400 LOC)     ← from SmartAssistant
   ├── labels/                          (text label dictionaries) ← from AII
   └── docs/                            (md reports)              ← from AII
```

You **never run** the donor code directly. You read it, decide what's worth porting, port the parts that fill a gap in the spine, and leave a paper trail (commit message + comment) pointing back to the donor file.

The spine is **Kotlin**. The donors are **Java**. Kotlin/Java interop is bidirectional and trivial in Android — you can call Java from Kotlin with no glue. Translation to idiomatic Kotlin is preferred but not required for everything; some donor classes (especially the orchestration ones) are clean enough to drop in as Java and call from the spine.

## 1. Day-1 environment setup

```
# 1. Re-add the llama.cpp submodule (deliberately not checked in):
cd android/app/src/main/cpp
git submodule add https://github.com/ggerganov/llama.cpp llama.cpp
git submodule update --init --recursive

# 2. Build:
cd ../../../..   # back to android/
./gradlew assembleDebug

# 3. If you get NDK errors, install NDK r26+ and Android API 34 SDK first.
```

That should produce `android/app/build/outputs/apk/debug/app-debug.apk`. If `LlamaEngine.kt` runs in stub mode, the build still succeeds — see Section 5.

## 2. The spine in one paragraph

The Android app boots into Compose (`MainActivity` → `AriaApp`). The user's voice/text command goes into the agent loop (`core/agent/AgentLoop.kt`, ~1,063 LOC), which iterates: *read screen → call LLM → produce a tool call → execute the tool → observe → repeat*. The LLM is `core/ai/LlamaEngine.kt` (483 LOC, JNI bridge to llama.cpp). Screen reading uses Android's accessibility tree plus `core/perception/ObjectDetectorEngine.kt` (MediaPipe EfficientDet-Lite0). Tool execution uses `system/actions/GestureEngine.kt`. RL fast-path is `core/rl/PolicyNetwork.kt` (REINFORCE) trained by `core/rl/LoraTrainer.kt` (LoRA via JNI). Events flow on `core/events/AgentEventBus.kt`. UI is 13 Compose screens under `ui/`.

## 3. Read these spine files in order

When you first arrive, in this exact order:

1. `GAP_AUDIT.md` (root) — every known stub in priority order. **This is your backlog.**
2. `android/app/src/main/kotlin/com/ariaagent/mobile/core/agent/AgentLoop.kt` — the central loop.
3. `android/app/src/main/kotlin/com/ariaagent/mobile/core/ai/LlamaEngine.kt` — JNI surface to llama.cpp.
4. `android/app/src/main/kotlin/com/ariaagent/mobile/core/rl/PolicyNetwork.kt` — REINFORCE policy.
5. `android/app/src/main/kotlin/com/ariaagent/mobile/core/rl/LoraTrainer.kt` — LoRA fine-tune via JNI.
6. `android/app/src/main/kotlin/com/ariaagent/mobile/core/perception/ObjectDetectorEngine.kt` — MediaPipe object detection.
7. `android/app/src/main/cpp/llama_jni.cpp` and `aria_math.cpp` — the native side.

That gives you the whole vertical slice in under 3,500 LOC.

## 4. Read these donor files next

When you have the spine model in your head, then read:

1. `donors/orchestration-java/CentralAIOrchestrator.java` — the orchestrator pattern AII built (Service, registry, scheduler, health, diff). The aria-ai spine has nothing equivalent yet; the `AgentEventBus` is just a pub/sub.
2. `donors/orchestration-java/EventRouter.java` + `CircuitBreaker.java` + `HealthMonitor.java` + `DiffEngine.java` — the supporting cast.
3. `donors/rl-algorithms-java/PPOAlgorithm.java` (562 LOC) and `DQN.java` (356 LOC) — production-grade RL implementations the spine's REINFORCE could be promoted to.
4. `donors/scheduler-java/TaskSchedulerManager.java` and `executor/ActionExecutor.java` — the task scheduler the spine doesn't have.
5. `donors/perception-java/ml/TensorflowLiteObjectDetector.java` — alternative object detection path (not necessarily better than MediaPipe; useful as reference for custom-model loading).

## 5. Top priority gaps and the donor that fills each

The following table is the **canonical mapping** between aria-ai's `GAP_AUDIT.md` and the donor code. Tackle in this order:

| # | Spine gap | Symptom | Donor that addresses it | Notes |
|---|-----------|---------|-------------------------|-------|
| 1 | `LlamaEngine.kt` runs in stub mode | Agent always returns `{"tool":"Click",...,"reason":"stub inference"}` | None — fix is **`git submodule add llama.cpp`** + build | This is the single most important fix. Nothing else matters until the LLM runs. |
| 2 | `LoraTrainer.kt` writes metadata-only `.bin` | LoRA fine-tune does not actually update weights | None — same as above. The JNI is wired; it just needs `liblora_train.so` compiled. | See `core/rl/LoraTrainer.kt` lines ~290 (the stub fallback). |
| 3 | ~~No central orchestrator — `AgentEventBus` is just a topic bus~~ **Port complete.** | ~~No component lifecycle, no health checks, no circuit breaking~~ | **`donors/orchestration-java/*` → `core/orchestration/`** (Kotlin, 14 files) | Donor port done — see `core/orchestration/README.md`. Follow-ups: ~~(a) implement `LlamaProblemSolver` adapter for `ProblemSolvingBroker`~~ **done** (`core/ai/LlamaProblemSolver.kt`); (b) supply real `OrchestrationScheduler.StageExecutor`, (c) bridge `EventRouter` ↔ `AgentEventBus`, (d) make `AgentLoop`, `LlamaEngine`, `ObjectDetectorEngine`, `PolicyNetwork`, `LoraTrainer` implement `ComponentInterface` and register on the orchestrator. |
| 4 | `PolicyNetwork.kt` is REINFORCE only — high variance, slow convergence | Agent learns slowly from feedback | **`donors/rl-algorithms-java/PPOAlgorithm.java` + `DQN.java`** | Add a strategy interface so the policy can switch algorithm. Keep REINFORCE for warm-up, switch to PPO once buffer is large. |
| 5 | No task scheduler — agent only acts on immediate user prompt | "Remind me at 5pm" doesn't work | **`donors/scheduler-java/`** entire tree | This is the single largest donor (54 files). Port `TaskSchedulerManager` + `ScheduledTask` + 8 `ActionHandler`s. Most likely path: leave it as Java in `app/src/main/java/...` and call from Kotlin. |
| 6 | `ObjectDetectorEngine` only knows COCO classes | Cannot recognize game UI elements (HUD, buttons, characters) | **`donors/labels/labels/*`** dictionaries + a real custom-trained model (NOT the AII tflites — those are fakes) | Use the label dictionaries to specify the classes; train (or source) a real EfficientDet-Lite0 INT8 model on those classes. Document the model URL in `ObjectDetectorEngine.kt` once available. |
| 7 | `IrlModule.kt` falls back to Jaccard similarity | Imitation learning quality is poor | Spine fix — replace Jaccard with the policy network's embedding cosine | No donor code helps here directly; the orchestration components have related logic in `donors/orchestration-components-java/BehaviorDetector.java` worth reading. |
| 8 | No video processing | Cannot learn from screen recordings | **`donors/learning-video-java/VideoProcessor.java`** | Port to Kotlin under `core/learning/video/`. |

Anything not in this table is lower priority than #1 and #2.

## 6. Known donor pitfalls

- **Typo in `donors/orchestration-java/CircuitBreaker.java`**: line 1 reads `package com.aiassistant.core/orchestration;` (slash instead of dot). Fix on port.
- **AII RL algorithms** sometimes import classes that don't exist in this curated subset (e.g. `KnowledgeEntry`). Either pull the missing dependency from the AII source on GitHub (`TITANICBHAI/AI-ASSISTANT-INCOMPLETE`, full clone) or stub the dependency during the port.
- **AII orchestration components** (`donors/orchestration-components-java/`) reference classes from `com.aiassistant.core.ai.*` that we did **not** port. Treat these as design references, not drop-ins — the patterns are gold but you may need to write thin replacement versions.
- **SmartAssistant detection classes** (`donors/perception-java/detection/`) are largely TFLite-bound. Since AII's tflites are fake, these will need either a real custom model or a redesign that uses MediaPipe (which the spine already does).
- **`donors/perception-java/ml/TensorflowLiteObjectDetector.java`** is a generic TFLite loader — useful as a reference, but the spine already standardized on MediaPipe Tasks API in `ObjectDetectorEngine.kt`. Don't re-introduce raw TFLite without a strong reason.

## 7. How to port a donor file

Repeatable recipe:

1. Pick a target Kotlin location, e.g. `android/app/src/main/kotlin/com/ariaagent/mobile/core/orchestration/CircuitBreaker.kt`.
2. Convert Java → Kotlin (Android Studio: `Code → Convert Java File to Kotlin`, then hand-clean).
3. Top of the new file, add a 3-line comment:
   ```kotlin
   // Ported from donors/orchestration-java/CircuitBreaker.java
   // Original repo: TITANICBHAI/AI-ASSISTANT-INCOMPLETE
   // Changes: package fix, idiomatic Kotlin, coroutine-based cooldown
   ```
4. Wire it into the spine (registry, DI, whatever).
5. **Do not delete** the donor file. It stays as reference.
6. Commit with message: `port: CircuitBreaker from AII donor — closes GAP_AUDIT #X`.

## 8. Definition of done for this project

The user's north-star: a working on-device AI assistant on Samsung Galaxy M31 (Exynos 9611, 6 GB RAM) that can:

- Run a 1B-parameter LLM (Llama-3.2-1B-Instruct-Q4_K_M.gguf) at ≥ 2 tok/s.
- See the screen via accessibility tree + EfficientDet-Lite0 + OCR.
- Execute tool calls (tap, swipe, type, back) via the gesture engine.
- Learn from feedback via PPO/DQN policy network + LoRA fine-tune.
- Remember context across sessions.
- Schedule and trigger tasks.

You are done when GAP_AUDIT items #1-#8 above are closed and a clean build of the M31-target APK launches, accepts a voice command, and completes a multi-step task end-to-end on a real device.

## 9. Things you should NOT do

- Do not regress aria-ai's spine to Java. The Kotlin spine is the architectural decision; donors are reference, not boss.
- Do not introduce frameworks not already in `android/build.gradle.kts` without writing why in `ARCHITECTURE.md`.
- Do not ship `.tflite` files unless you trained or downloaded a verifiable real one. The user has been burned by stub tflites before.
- Do not check the `llama.cpp` submodule's full source into this repo. Submodule reference only.
- Do not remove `donors/` once you've ported a file. Keep the trail.
- Do not break `GAP_AUDIT.md` formatting — it's the user's living roadmap.

## 10. When you finish a session

Before handing off, update three files:

- `GAP_AUDIT.md` — strike through items you closed; add new ones you discovered.
- `ARCHITECTURE.md` — note any new directories, components, or patterns you introduced.
- `DONOR_INVENTORY.md` — mark donor files you ported (don't delete the row).

Then commit and push. The next agent will start at Section 0 of this file.

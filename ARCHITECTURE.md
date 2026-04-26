# aria-prime Architecture

## High-level

```
                         ┌───────────────────────────────────────┐
                         │           Compose UI (13 screens)      │
                         │  ChatScreen / AgentControl / Settings │
                         └───────────────┬───────────────────────┘
                                         │ ViewModels
                                         ▼
                         ┌───────────────────────────────────────┐
                         │          AgentLoop (1063 LOC)          │  ← core/agent/
                         │    read → decide → act → observe       │
                         └───┬─────────┬──────────┬──────────┬────┘
              decide         │  read   │   act    │ observe  │
                             ▼         ▼          ▼          ▼
                         ┌───────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐
                         │ LLM   │ │  See    │ │ Gesture │ │  RL     │
                         │ Llama │ │ A11y +  │ │ Engine  │ │ Policy  │
                         │ Engine│ │ Object  │ │         │ │ Network │
                         │ (JNI) │ │ Detector│ │         │ │ + LoRA  │
                         └───┬───┘ └─────────┘ └─────────┘ └─────────┘
                             │ JNI
                             ▼
                         ┌───────────────┐
                         │  llama.cpp    │  ← submodule (not in repo)
                         │  +  GGUF      │
                         │  + aria_math  │
                         └───────────────┘
```

After porting `donors/orchestration-java/`, every box above becomes a registered **Component** in the orchestration layer:

```
   ┌──────────────────── CentralOrchestrator (Service) ─────────────────────┐
   │                                                                         │
   │   ComponentRegistry  ──  EventRouter  ──  HealthMonitor  ──  CircuitBreaker
   │           │                   ▲                  │                       │
   │           │                   │                  ▼                       │
   │   register/start            publish/         heartbeat                  │
   │   /stop/snapshot           subscribe         + diff                    │
   │                                                                         │
   └─────────────────────────────────────────────────────────────────────────┘
                  ▲                ▲                ▲              ▲
                  │                │                │              │
              LlamaEngine    AgentLoop       ObjectDetector   PolicyNetwork
               (Component)   (Component)       (Component)     (Component)
```

## Layers

| Layer | Path | Language | Purpose |
|-------|------|----------|---------|
| UI | `android/app/src/main/kotlin/com/ariaagent/mobile/ui/` | Kotlin / Compose | 13 screens, MVVM-style |
| Agent loop | `core/agent/` | Kotlin | central deliberation loop, prompt building, task queue |
| Inference | `core/ai/` | Kotlin → JNI | LLM and (later) vision LLM via llama.cpp |
| Perception | `core/perception/` | Kotlin | A11y + MediaPipe object detection + (TBD) OCR |
| RL | `core/rl/` | Kotlin → JNI | REINFORCE policy network, LoRA fine-tune, IRL fallback |
| Actions | `system/actions/` | Kotlin | accessibility-driven gesture executor |
| Events | `core/events/` | Kotlin | pub/sub event bus (to be replaced by EventRouter from donors) |
| Native | `android/app/src/main/cpp/` | C++ | aria_math, llama_jni, lora_train (compiled separately) |
| **Orchestration** | `core/orchestration/` | **TBD — to be ported from donors** | lifecycle, health, circuit breaking, diffing, scheduling |
| **Scheduler** | `app/src/main/java/com/aiassistant/scheduler/` | **TBD — Java drop-in from donors** | named tasks, triggers, action handlers |
| **Algorithms** | `core/rl/algorithms/` | **TBD — port from donors** | DQN, PPO, SARSA, MetaLearning |

## Process model

Single Android process. The agent itself runs inside `CentralAIOrchestrator`, an Android `Service` (foreground when active, bound when interacting with the UI). Screen access requires the `AccessibilityService` to be enabled by the user. The gesture executor uses `AccessibilityService.dispatchGesture()`.

## Memory budget (Samsung Galaxy M31, 6 GB RAM, target device)

| Consumer | RAM | Notes |
|----------|----:|-------|
| LLM (Llama-3.2-1B-Q4_K_M, heap, no mmap) | 1,400 MB | KV-cache adds 2048-ctx overhead |
| OCR | 100 MB | when active |
| Object detector (EfficientDet-Lite0 INT8) | 30 MB | only when invoked |
| RL policy network | 5 MB | always resident |
| Misc + Compose | 200 MB | |
| **Total app** | **~1,735 MB** | within 2.5–3.5 GB safe budget |

## Threading

| Concern | Thread / Dispatcher |
|---------|---------------------|
| LLM inference | `Dispatchers.Default`, mutex-guarded (one active call) |
| Object detection | `Dispatchers.Default` |
| OCR | `Dispatchers.IO` |
| RL update | `Dispatchers.Default` |
| Gesture dispatch | Main / Accessibility callback |
| Event bus | dedicated cached thread pool inside `EventRouter` (after port) |
| File I/O (model load, LoRA save) | `Dispatchers.IO` |

## Data on disk

```
/data/data/com.ariaagent.mobile/
   ├── files/models/
   │     ├── Llama-3.2-1B-Instruct-Q4_K_M.gguf      (~870 MB, downloaded on first run)
   │     └── efficientdet_lite0_int8.tflite          (~4.4 MB)
   ├── files/rl/
   │     ├── policy_latest.bin                       (REINFORCE weights)
   │     ├── policy_adam.bin                         (optimizer state)
   │     └── lora/adapter_v{N}.bin                   (LoRA adapters, versioned)
   ├── files/sessions/                                (chat history, JSON)
   └── databases/                                     (Room DB for tasks, labels, feedback)
```

## Build

- Gradle 8+, AGP 8+, Kotlin 1.9+
- NDK r26+ for the C++ side
- Min SDK 26, target SDK 34
- ABIs: `arm64-v8a` only (target device)

## Testing strategy

The spine has limited automated tests. The user's preferred validation is **install on the M31 and try a real task**. CI is GitHub Actions in `.github/workflows/` (preserved from aria-ai). Add unit tests for orchestration layer when porting.

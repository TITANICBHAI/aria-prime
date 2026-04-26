# aria-prime

A curated mixed-AI codebase that takes the **best parts of three sibling repos** and fuses them into one coherent Android assistant project.

## What this is

`aria-prime` is **`aria-ai` as the spine** (Kotlin + Jetpack Compose + JNI llama.cpp) plus **surgically chosen donor pieces** from `SmartAssistant` and `AI-ASSISTANT-INCOMPLETE` (henceforth "AII") sitting in a separate `donors/` tree, ready to be promoted into the spine when the time is right.

Nothing has been merged into the Kotlin spine yet — the donor code is preserved exactly as it was in its source repos so the next agent (human or AI) can:

1. Read each donor file in its original form.
2. Decide where (if anywhere) it should live in the Kotlin spine.
3. Port it (Java → Kotlin) or call it via interop, with the original right next to the new code for reference.

## Why three repos became one

The three source repos were three *attempts at the same product*. They overlap heavily but each one made a different bet that paid off in a different area:

| Repo | What it nailed |
|---|---|
| `aria-ai` | Modern Kotlin spine, Compose UI, real JNI llama.cpp wrapper, REINFORCE policy network, full agent loop, brutally honest `GAP_AUDIT.md` |
| `AI-ASSISTANT-INCOMPLETE` (AII) | Production-grade orchestration layer (Service, EventRouter, CircuitBreaker, HealthMonitor, DiffEngine, ProblemSolvingBroker), full RL algorithm library (DQN, PPO, SARSA, Q-learning, MetaLearning), game-specific label dictionaries |
| `SmartAssistant` | Comprehensive task scheduler with 8 action handler types, video processing pipeline, OCR/object/UI detection scaffolding |

The verdict: **`aria-ai` wins as the foundation** because it is the only one of the three written in modern, idiomatic Kotlin with a real (compileable) native bridge and a maintained gap-analysis document. The other two are donors of *components*, not architectures.

## What was deliberately left out

- **All `.tflite` files** — the user confirmed AII's TFLite models are fakes / stubs (file size correct, weights random or absent). The label files and metadata JSONs are kept; the binary models are not.
- **`llama.cpp` vendor source** — too large for source control; restored via `git submodule add https://github.com/ggerganov/llama.cpp android/app/src/main/cpp/llama.cpp` (instructions in `android/app/src/main/cpp/README.md` and inside `LlamaEngine.kt`).
- **All `.bak` files** from `SmartAssistant` — every donor file had a `.bak` companion from a half-finished migration; the `.java` was kept, the `.bak` was dropped.
- **`SmartAssistant`'s root shadow folders** (`android/`, `com/`, `java/`, `org/`, `models/`, `utils/` at the repo root) — duplicated cruft with stale `.class` and `.bak` files that mirror `app/src/main/...`.
- **AII OpenCV-android-sdk vendor** — restore via SDK download if needed.

## Where to start reading

1. **`AGENT_INSTRUCTIONS.md`** ← read this first if you are an AI agent picking up this project.
2. **`GAP_AUDIT.md`** — aria-ai's brutally honest list of every stub in the spine, in priority order.
3. **`DONOR_INVENTORY.md`** — every donor file, its source repo, line count, and the gap it can fill.
4. **`ARCHITECTURE.md`** — the merged architecture once donor code is integrated.
5. **`android/`** — the live Kotlin spine.
6. **`donors/`** — donor source, sorted by capability area. Read-only reference until you choose to port.

## Repo size

~3 MB on disk (Kotlin + Java sources + label files + docs). No vendor binaries, no submodules checked in. A clean clone is fast.

## Provenance

All three source repos are owned by the same GitHub account (`TITANICBHAI`). No third-party code is included beyond what those repos already contained. Donor files keep their original package declarations so you can trace any line back to its origin.

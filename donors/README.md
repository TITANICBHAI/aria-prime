# donors/ — read-only reference code

This directory holds source code from sibling repos that the aria-ai spine doesn't yet have but should eventually absorb. Files here are **not compiled** by the Android build — they are reference material for the next port.

| Subdirectory | Origin repo | What it provides |
|---|---|---|
| `orchestration-java/` | `TITANICBHAI/AI-ASSISTANT-INCOMPLETE` | Central orchestrator + EventRouter + CircuitBreaker + HealthMonitor + DiffEngine + ComponentRegistry + ProblemSolvingBroker + scheduler — the spine layer aria-ai never wrote |
| `orchestration-components-java/` | `TITANICBHAI/AI-ASSISTANT-INCOMPLETE` | Concrete Components (ActionRecommender, BatteryMonitor, BehaviorDetector, CommandProcessor, ContextAnalyzer) — design references |
| `rl-algorithms-java/` | `TITANICBHAI/AI-ASSISTANT-INCOMPLETE` | RL algorithm library: DQN, PPO, SARSA, Q-learning, MetaLearning, KMeans clustering |
| `scheduler-java/` | `TITANICBHAI/SmartAssistant` | Full task scheduler with 8 action handler types (notification, email, system action, app launch, app control, API call, wait, custom) |
| `perception-java/` | `TITANICBHAI/SmartAssistant` | Game-aware detection + ml utilities (TFLite-bound; reference only since AII tflites were fakes) |
| `learning-video-java/` | `TITANICBHAI/SmartAssistant` | Video processing pipeline |
| `labels/` | `TITANICBHAI/AI-ASSISTANT-INCOMPLETE` | Plain-text label dictionaries (UI elements, environments, items, combat effects, game-specific lists) |
| `docs/` | `TITANICBHAI/AI-ASSISTANT-INCOMPLETE` | Markdown reports describing AII's coordinated AI system vision |

See `../DONOR_INVENTORY.md` for the full file-by-file table with line counts and porting status.

See `../AGENT_INSTRUCTIONS.md` for the recipe to port a donor file into the spine.

## Why none of this is compiled

The `donors/` tree is excluded from the Gradle source set. Three reasons:

1. **Package collisions** — donor files keep their original `com.aiassistant.*` package, while the spine uses `com.ariaagent.*`. Compiling both at once would duplicate class names.
2. **Missing dependencies** — donor files reference classes from their original repos that we did not port. They wouldn't compile standalone.
3. **Single-source-of-truth discipline** — once a donor file is ported into the spine, the spine version is authoritative. The donor stays as historical reference, not as a parallel runtime.

# `core/orchestration/` — ported from AII donors

This package closes **GAP_AUDIT.md item #3** ("No central orchestrator —
`AgentEventBus` is just a topic bus").

## What's in here

| File | Ported from | Role |
|---|---|---|
| `ComponentInterface.kt` | `donors/orchestration-java/ComponentInterface.java` | Contract every plug-in component must satisfy. |
| `ComponentStateSnapshot.kt` | `…/ComponentStateSnapshot.java` | Immutable point-in-time component state + content hash. |
| `StateDiff.kt` | `…/StateDiff.java` | Per-field deltas + severity tier. |
| `ValidationContract.kt` | `…/ValidationContract.java` | Optional output validator. |
| `OrchestrationEvent.kt` | `…/OrchestrationEvent.java` | Event payload + canonical event-type constants. |
| `ProblemTicket.kt` | `…/ProblemTicket.java` | Mutable problem report submitted to the broker. |
| `ComponentRegistry.kt` | `…/ComponentRegistry.java` | Thread-safe registry, capability lookup, status tracking. |
| `EventRouter.kt` | `…/EventRouter.java` | Topic + wildcard subscriptions, async dispatch on `Dispatchers.Default`. |
| `CircuitBreaker.kt` | `…/CircuitBreaker.java` | Three-state breaker (CLOSED → OPEN → HALF_OPEN). Donor package typo fixed. |
| `DiffEngine.kt` | `…/DiffEngine.java` | Snapshot comparison, throttled per-component, severity heuristic. |
| `HealthMonitor.kt` | `…/HealthMonitor.java` | Heartbeats, error/success counters, owns one circuit breaker per component. |
| `ProblemSolvingBroker.kt` | `…/ProblemSolvingBroker.java` | LLM-backed diagnosis. Cloud Groq replaced with `ProblemSolver` interface. |
| `OrchestrationScheduler.kt` | `…/OrchestrationScheduler.java` | Pipelines + trigger rules. Stage execution delegated to `StageExecutor`. |
| `CentralOrchestrator.kt` | `…/CentralAIOrchestrator.java` | The coordinator. **Not** an Android `Service` — plain coroutine class. |

## What changed during the port

1. **No `android.app.Service` base class.** The donor's
   `CentralAIOrchestrator` *was* the Android service. The spine already has
   `AgentForegroundService`, so `CentralOrchestrator` here is a regular
   long-lived object the foreground service can hold.
2. **`ExecutorService` / `Handler` → `CoroutineScope` + `Dispatchers`.** All
   periodic work, fan-out, and async dispatch is now coroutine-based.
3. **Donor `Groq`/`FeedbackSystem`/`ErrorResolutionWorkflow` deps dropped.**
   Those classes don't exist in the curated donor subset and the spine is
   on-device-only. `ProblemSolvingBroker` now takes a
   `ProblemSolver` interface — the spine plugs `LlamaEngine` in via an
   adapter (TODO: `LlamaProblemSolver` next to `LlamaEngine.kt`).
4. **`CircuitBreaker.java`'s package typo fixed.** Donor file declares
   `package com.aiassistant.core/orchestration;` (slash instead of dot).
   The Kotlin port uses the correct dotted path.
5. **`OrchestrationScheduler` stage execution** is now pluggable via a
   `StageExecutor` interface. The donor stubbed every stage with a fake
   "success" result; the spine will install a real executor that calls
   registered components by id.
6. **Idiomatic Kotlin throughout:** `data class` for immutable carriers
   (`ComponentStateSnapshot`, `StateDiff`, `OrchestrationEvent`), `enum class`
   for state machines, `@Volatile` + atomics for cheap mutable shared state,
   `Mutex` for state-machine lifecycle transitions.

## Wiring it into the spine

Pseudo-code, to be expanded in a follow-up port:

```kotlin
val orchestrator = CentralOrchestrator(
    context = appContext,
    problemSolver = LlamaProblemSolver(),  // wraps LlamaEngine.infer()
)

// Bridge orchestration events onto the spine's UI-facing AgentEventBus
orchestrator.eventRouter.subscribe(EventRouter.WILDCARD) { evt ->
    AgentEventBus.emit(
        name = "orchestration.${evt.eventType}",
        data = mapOf("source" to evt.source) + evt.data,
    )
}

// Register the LLM, perception, RL, and gesture engines as components
orchestrator.registry.registerComponent(
    componentId = "llama_engine",
    componentName = "Llama Engine",
    capabilities = listOf("inference", "embedding"),
)
// …same for ObjectDetectorEngine, PolicyNetwork, GestureEngine, AgentLoop

scope.launch { orchestrator.start() }
```

## Open follow-ups

- ~~Implement `LlamaProblemSolver` adapter that calls `LlamaEngine.infer()`.~~
  **Done** — `core/ai/LlamaProblemSolver.kt`. Defaults to `maxTokens=256`,
  `temperature=0.3` (conservative diagnostics). Throws when the model is not
  loaded so the broker escalates the ticket honestly.
- ~~Bridge `EventRouter` ↔ `AgentEventBus` so orchestration health events show
  up in the Compose UI's Activity / Modules screens.~~ **Done (2026-04-27)** —
  `AgentForegroundService.onCreate()` subscribes a wildcard listener on
  `EventRouter` and re-emits each event onto `AgentEventBus` under the
  `"orchestration.<eventType>"` namespace, with the originating component id
  copied into the payload as `"source"`. The Compose UI subscribes to that
  same bus, so any orchestration event is one `flow.collect { … }` away.
- ~~Wire `CentralOrchestrator` into the Android lifecycle.~~ **Done
  (2026-04-27)** — constructed with `LlamaProblemSolver()` in
  `AgentForegroundService.onCreate()`, started after registering the four
  current engines (`llama_engine`, `agent_loop`, `vision_engine`,
  `policy_network`), and torn down in `onDestroy()`. The instance is exposed
  to other modules as `AgentForegroundService.sharedOrchestrator`.
- Implement a real `OrchestrationScheduler.StageExecutor` that resolves
  `componentId` → `ComponentInterface` and invokes `execute()`. Currently the
  scheduler still uses `NoopStageExecutor`, so registered pipelines log
  `{"status":"noop", …}` instead of running real work — fine while the engines
  haven't yet been migrated to `ComponentInterface`.
- Migrate `AgentLoop`, `LlamaEngine`, `ObjectDetectorEngine`, `PolicyNetwork`,
  `LoraTrainer`, `GestureEngine` to implement `ComponentInterface`. Once any
  engine implements the interface, the spine can plug a real `StageExecutor`
  that fans out into `ComponentInterface.execute(input)` via the registry.

# Donor Inventory

Every file under `donors/` listed with its line count, source repo, and the spine gap it can address.
**Status legend:** `pending` = not yet ported · `ported` = lives in the spine now · `skip` = read-only reference, not for porting.

When you port a file, change its status to `ported` and add a line under "Ported to:" pointing at the destination Kotlin file. **Do not delete rows.**

---

## donors/orchestration-java/ — from `TITANICBHAI/AI-ASSISTANT-INCOMPLETE`

The orchestration spine that aria-ai is missing. Port the whole package as a unit — the classes reference each other.

| File | Lines | Status | Spine target |
|------|------:|--------|--------------|
| CentralAIOrchestrator.java | 307 | pending | `core/orchestration/CentralOrchestrator.kt` |
| EventRouter.java | 91 | pending | `core/orchestration/EventRouter.kt` |
| CircuitBreaker.java | 95 | pending | `core/orchestration/CircuitBreaker.kt` (fix package typo on line 1) |
| HealthMonitor.java | 209 | pending | `core/orchestration/HealthMonitor.kt` |
| DiffEngine.java | 179 | pending | `core/orchestration/DiffEngine.kt` |
| ComponentRegistry.java | 188 | pending | `core/orchestration/ComponentRegistry.kt` |
| ComponentInterface.java | 31 | pending | `core/orchestration/Component.kt` (rename to interface) |
| ComponentStateSnapshot.java | 65 | pending | `core/orchestration/ComponentStateSnapshot.kt` |
| OrchestrationEvent.java | 48 | pending | `core/orchestration/OrchestrationEvent.kt` |
| OrchestrationScheduler.java | 339 | pending | `core/orchestration/OrchestrationScheduler.kt` |
| ProblemSolvingBroker.java | 174 | pending | `core/orchestration/ProblemSolvingBroker.kt` |
| ProblemTicket.java | 113 | pending | `core/orchestration/ProblemTicket.kt` |
| StateDiff.java | 94 | pending | `core/orchestration/StateDiff.kt` |
| ValidationContract.java | 32 | pending | `core/orchestration/ValidationContract.kt` |

**Total:** 14 files, ~1,965 LOC. Highest priority donor block.

## donors/orchestration-components-java/ — from AII

Concrete components that plug into the orchestrator. Read these for design patterns; reimplement against the spine's actual classes (these reference AII-specific types not ported here).

| File | Lines | Status | Spine target |
|------|------:|--------|--------------|
| ActionRecommender.java | 151 | pending | `core/orchestration/components/ActionRecommender.kt` |
| BatteryMonitor.java | 163 | pending | `core/orchestration/components/BatteryMonitor.kt` |
| BehaviorDetector.java | 147 | pending | `core/orchestration/components/BehaviorDetector.kt` (also informs IrlModule rewrite — GAP_AUDIT #7) |
| CommandProcessor.java | 164 | pending | `core/orchestration/components/CommandProcessor.kt` |
| ContextAnalyzer.java | 166 | pending | `core/orchestration/components/ContextAnalyzer.kt` |

**Total:** 5 files, ~791 LOC.

## donors/rl-algorithms-java/ — from AII

A library of reinforcement learning algorithms. The spine currently has only REINFORCE in `PolicyNetwork.kt`. These are the upgrade path.

| File | Lines | Status | Spine target |
|------|------:|--------|--------------|
| Algorithm.java | 24 | pending | `core/rl/algorithms/Algorithm.kt` (interface) |
| RLAlgorithm.java | 119 | pending | `core/rl/algorithms/RLAlgorithm.kt` (base class) |
| ReinforcementLearningAlgorithm.java | 308 | pending | `core/rl/algorithms/ReinforcementLearningAlgorithm.kt` |
| DeepRLAlgorithm.java | 359 | pending | `core/rl/algorithms/DeepRLAlgorithm.kt` |
| DQN.java | 356 | pending | `core/rl/algorithms/DQN.kt` |
| DQNAlgorithm.java | 139 | pending | `core/rl/algorithms/DQNAlgorithm.kt` |
| PPOAlgorithm.java | 562 | **highest priority** | `core/rl/algorithms/PPO.kt` — closes GAP_AUDIT #4 |
| QLearning.java | 176 | pending | `core/rl/algorithms/QLearning.kt` |
| QLearningAlgorithm.java | 367 | pending | `core/rl/algorithms/QLearningAlgorithm.kt` |
| SARSAAlgorithm.java | 495 | pending | `core/rl/algorithms/SARSA.kt` |
| MetaLearningAlgorithm.java | 350 | pending | `core/rl/algorithms/MetaLearning.kt` |
| KMeansClusteringAlgorithm.java | 254 | pending | `core/rl/algorithms/KMeansClustering.kt` (used for state space discretization) |

**Total:** 12 files, ~3,509 LOC.

## donors/scheduler-java/ — from `TITANICBHAI/SmartAssistant`

The full task scheduler. Recommended path: drop in as Java under `app/src/main/java/com/aiassistant/scheduler/`, call from Kotlin. Translation is optional and time-consuming; Java/Kotlin interop on Android is free.

### Top level
| File | Lines | Status |
|------|------:|--------|
| TaskSchedulerManager.java | 791 | pending — closes GAP_AUDIT #5 |
| ScheduledTask.java | 269 | pending |
| Action.java | 226 | pending |
| ActionResult.java | 140 | pending |
| ActionSequence.java | 249 | pending |
| ActionType.java | 35 | pending |
| Condition.java | 234 | pending |
| ConditionOperator.java | 38 | pending |
| ScheduleType.java | 33 | pending |
| TaskPriority.java | 24 | pending |
| TaskStatus.java | 32 | pending |
| TaskTriggerType.java | 239 | pending |
| Trigger.java | 153 | pending |
| TriggerType.java | 35 | pending |

### executor/
| File | Lines | Status |
|------|------:|--------|
| ActionExecutor.java | 165 | pending |
| ActionHandler.java | 136 | pending |
| ActionResult.java | 209 | pending |
| ActionStatus.java | 141 | pending |
| ActionCallback.java | 22 | pending |
| StandardizedActionHandler.java | 76 | pending |
| StandardizedActionHandlerAdapter.java | 78 | pending |
| ApiCallActionHandler.java | 152 | pending |
| ApiCallHandler.java | 284 | pending |
| AppControlActionHandler.java | 184 | pending |
| AppControlHandler.java | 513 | pending |
| AppLaunchActionHandler.java | 163 | pending |
| EmailActionHandler.java | 537 | pending |
| NotificationActionHandler.java | 247 | pending |
| SystemActionHandler.java | 540 | pending |
| WaitActionHandler.java | 195 | pending |
| CustomActionHandler.java | 161 | pending |

### executor/handlers/ (newer parallel handler hierarchy)
| File | Lines | Status |
|------|------:|--------|
| ActionHandler.java | 14 | pending — interface |
| ApiCallHandler.java | 264 | pending |
| AppControlHandler.java | 304 | pending |
| EmailHandler.java | 289 | pending |
| NotificationHandler.java | 206 | pending |
| SystemActionHandler.java | 403 | pending |
| WaitHandler.java | 146 | pending |
| CustomActionHandler.java | 140 | pending |

> ⚠️ Note: `executor/` and `executor/handlers/` are two parallel handler hierarchies in the source repo (mid-migration). Pick one (the newer `handlers/` is cleaner) and discard the other. Do not port both.

**Total:** 39 files, ~7,266 LOC.

## donors/perception-java/ — from SmartAssistant

Game-aware perception. Most files lean on TFLite — since AII's tflites were fakes, treat these as **design references** unless you have a real model. The spine already uses MediaPipe.

### detection/
| File | Lines | Status | Notes |
|------|------:|--------|-------|
| TextRecognizer.java | 332 | pending | OCR scaffolding — useful pattern, could replace ML Kit fallback |
| ContentRecognizer.java | 339 | pending | |
| ObjectDetector.java | 309 | skip | TFLite-bound, replaced by spine MediaPipe path |
| ObjectDetectorOptions.java | 148 | skip | |
| EnemyDetector.java | 923 | skip | game-specific, needs real model |
| GameAppElementDetector.java | 547 | pending | useful for game UI parsing |
| ElementUtils.java | 277 | pending | utility methods worth lifting |
| UIElement.java | 152 | pending | data class — port directly |

### ml/
| File | Lines | Status | Notes |
|------|------:|--------|-------|
| TensorflowLiteObjectDetector.java | 350 | skip | reference for custom-model loading; spine uses MediaPipe |
| OptimizedImageProcessor.java | 628 | pending | bitmap pipeline — useful for screenshot preprocessing |
| GamePatternRecognizer.java | 1240 | pending | the giant one; likely worth extracting useful sub-utilities only |
| GameRuleUnderstanding.java | 891 | pending | |
| RuleExtractionSystem.java | 735 | pending | |
| GameTrainer.java | 716 | pending | |
| PredictiveActionSystem.java | 992 | pending | |
| ActionPrioritizer.java | 581 | pending | |
| ActionPrioritization.java | 762 | pending | |
| ActionSuggestion.java | 362 | pending | |
| AdvancedActionSequencer.java | 933 | pending | |
| UserFeedbackSystem.java | 1141 | pending | |
| GameAction.java | 374 | pending | |
| GameType.java | 237 | pending | |
| Context.java | 290 | pending | |
| DeepRLModel.java | 448 | pending | |
| ObjectDetectorOptions.java | 256 | skip | duplicate |

**Total:** 25 files, ~12,766 LOC. **Take what's useful; do not port wholesale.**

## donors/learning-video-java/ — from SmartAssistant

| File | Lines | Status |
|------|------:|--------|
| VideoProcessor.java | 530 | pending — closes GAP_AUDIT #8 |
| VideoProcessingResult.java | 200 | pending |
| VideoProcessingStatus.java | 28 | pending |

**Total:** 3 files, ~758 LOC.

## donors/labels/ — from AII

Plain-text label dictionaries for the visual perception layer. These map class indices to human-readable names. Not source code — copy into `android/app/src/main/assets/labels/` when wiring the perception layer.

- `labels/labels/` — extended class lists (UI elements, environments, items, combat effects, game-specific for COD/PUBG/Free Fire)
- `labels/models/` — original COCO labels + per-game label maps + a `labelmap.txt` index
- `labels/ml_models_metadata/` — JSON metadata describing what each AII tflite *was supposed to be* (the tflites themselves are not included because they were fake)

## donors/docs/ — from AII

Markdown reports describing what AII was trying to build. Read for context, especially `AII_COORDINATED_AI_SYSTEM_COMPLETE.md` and `AII_COORDINATED_LOOP_SYSTEM.md` — those describe the orchestration vision behind `donors/orchestration-java/`.

---

## Grand totals

- **Donor Java files:** 113
- **Donor LOC:** ~26,300
- **Donor label files:** 28 text + 5 JSON
- **Donor docs:** 6 markdown

The spine itself is ~26,500 LOC of Kotlin + ~2,000 LOC of C++ (excluding the llama.cpp submodule). After full integration, expect a project of ~50K LOC across both languages.

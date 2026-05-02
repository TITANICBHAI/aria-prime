package com.ariaagent.mobile.core.events

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.Collections

/**
 * AgentEventBus — the internal event backbone for ARIA's Kotlin brain.
 *
 * AgentViewModel (via StateFlow → Compose UI) subscribes to this bus.
 * The React Native bridge has been removed — this is the sole subscriber path.
 *
 * Pattern: AgentLoop/GameLoop/LearningScheduler/ThermalGuard emit here.
 *          Subscribers react independently — no coupling between them.
 *
 * SharedFlow config:
 *   - replay = 0:       No stale events replayed to late subscribers
 *   - extraBuffer = 128: Room for 128 events before any subscriber falls behind
 *   - DROP_OLDEST:      Under backpressure, newest events win (UI freshness matters)
 *
 * Thread safety: SharedFlow is coroutine-safe. tryEmit() is non-suspending and
 *   can be called from any thread (Dispatchers.Default, Main, or IO).
 *
 * Event catalogue (name → payload keys):
 *   agent_status_changed         status, currentTask, currentApp, stepCount, lastAction, lastError, gameMode
 *   token_generated              token, tokensPerSecond
 *   action_performed             tool, nodeId, success, reward, stepCount, appPackage, timestamp
 *   step_started                 stepNumber, activity ("observe"|"reason"|"act"|"store")
 *   learning_cycle_complete      loraVersion, policyVersion
 *   thermal_status_changed       level, inferenceSafe, trainingSafe, emergency
 *   game_loop_status             isActive, gameType, episodeCount, stepCount, currentScore, highScore, totalReward, lastAction, isGameOver
 *   model_download_progress      percent, downloadedMb, totalMb, speedMbps
 *   model_download_complete      path
 *   model_download_error         error
 *   config_updated               (same keys as getConfig return)
 *   skill_updated                package, successRate, taskCount, elementCount
 *   task_chain_advanced          goal, appPackage, queueSize
 *   scheduler_training_started   (no payload)
 *   scheduler_training_stopped   (no payload)
 *   trigger_fired                triggerId, triggerType, goal, appPackage
 *   app_focus_changed            package, previousPackage
 *   orchestration.*              (wildcard) componentId, event, data — routed to DiagnosticsScreen ring buffer
 *
 * Phase: 10+ (Migration complete) — sole event channel for Compose ViewModel.
 */
object AgentEventBus {

    private val _flow = MutableSharedFlow<Pair<String, Map<String, Any>>>(
        replay = 0,
        extraBufferCapacity = 128,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    /** Subscribe to all agent events as a SharedFlow. */
    val flow: SharedFlow<Pair<String, Map<String, Any>>> = _flow.asSharedFlow()

    // ── In-memory ring buffer ──────────────────────────────────────────────────
    // Keeps the last HISTORY_SIZE events so late subscribers (UI screens that
    // navigate in after events were emitted) can display recent activity without
    // needing a replay-capable SharedFlow (which would deliver stale data to all
    // subscribers). Thread-safe via a synchronised wrapper.
    private const val HISTORY_SIZE = 150
    private val _history: ArrayDeque<Triple<String, Map<String, Any>, Long>> =
        ArrayDeque(HISTORY_SIZE + 1)
    private val historyLock = Any()

    /**
     * Snapshot of the last ≤[HISTORY_SIZE] emitted events as an immutable list,
     * ordered oldest → newest. Each entry is (name, data, timestampMs).
     * Returns a copy — safe to iterate from any thread.
     */
    val recentEvents: List<Triple<String, Map<String, Any>, Long>>
        get() = synchronized(historyLock) { _history.toList() }

    /**
     * Emit an event. Non-suspending, safe to call from any thread.
     * Also appends to the in-memory ring buffer for late subscribers.
     * Returns false only if the buffer is full and DROP_OLDEST didn't make space
     * (practically never happens with a 128-element buffer).
     */
    fun emit(name: String, data: Map<String, Any> = emptyMap()): Boolean {
        synchronized(historyLock) {
            _history.addLast(Triple(name, data, System.currentTimeMillis()))
            while (_history.size > HISTORY_SIZE) _history.removeFirst()
        }
        return _flow.tryEmit(name to data)
    }

    /** Clear the ring buffer (useful for tests). */
    fun clearHistory() = synchronized(historyLock) { _history.clear() }
}

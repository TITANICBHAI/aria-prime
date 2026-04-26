// Ported from donors/orchestration-java/CentralAIOrchestrator.java
// Original repo: TITANICBHAI/AI-ASSISTANT-INCOMPLETE
// Changes: NOT an Android Service (the spine has AgentForegroundService for
// that). Plain Kotlin coordinator class. FeedbackSystem and
// ErrorResolutionWorkflow dependencies dropped; those slots are exposed as
// optional listeners on the orchestrator. Coroutine-based periodic audit.

package com.ariaagent.mobile.core.orchestration

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The brain of the orchestration layer.
 *
 * Wires up [ComponentRegistry], [EventRouter], [DiffEngine], [HealthMonitor],
 * [OrchestrationScheduler], and [ProblemSolvingBroker], and runs a periodic
 * audit (health check + diff sweep) on its own coroutine.
 *
 * Unlike the donor (which extended `android.app.Service`), this is a plain
 * coroutine-friendly class. The Android `Service` lifecycle in the spine is
 * owned by `AgentForegroundService`; this orchestrator is a long-lived
 * *object* that the foreground service holds on to.
 *
 * Wire-up:
 * ```kotlin
 * val solver = LlamaProblemSolver(llamaEngine)
 * val orch = CentralOrchestrator(context, solver)
 * orch.initialize()
 * orch.start()
 * ```
 *
 * Dependents register themselves with `orch.registry.registerComponent(...)`
 * and emit heartbeats to `orch.healthMonitor.recordHeartbeat(componentId)`.
 */
class CentralOrchestrator(
    context: Context,
    problemSolver: ProblemSolvingBroker.ProblemSolver,
    private val periodicAuditIntervalMs: Long = 60_000L,
    private val initialAuditDelayMs: Long = 30_000L,
) {
    private val appContext: Context = context.applicationContext

    val registry: ComponentRegistry = ComponentRegistry()
    val eventRouter: EventRouter = EventRouter()
    val diffEngine: DiffEngine = DiffEngine()
    val healthMonitor: HealthMonitor = HealthMonitor(registry)
    val problemSolvingBroker: ProblemSolvingBroker = ProblemSolvingBroker(problemSolver)
    val scheduler: OrchestrationScheduler =
        OrchestrationScheduler(registry, eventRouter, healthMonitor)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val stateMutex = Mutex()
    private var auditJob: Job? = null

    @Volatile var isInitialized: Boolean = false; private set
    @Volatile var isRunning: Boolean = false; private set

    /** Wire dependencies and subscribe to lifecycle events. Idempotent. */
    suspend fun initialize(): Unit = stateMutex.withLock {
        if (isInitialized) {
            Log.w(TAG, "Already initialized")
            return
        }
        Log.i(TAG, "Initializing CentralOrchestrator…")

        registry.setEventRouter(eventRouter)
        diffEngine.setEventRouter(eventRouter)
        healthMonitor.setEventRouter(eventRouter)

        subscribeToEvents()

        isInitialized = true
        Log.i(TAG, "CentralOrchestrator initialized successfully")
    }

    /** Begin health monitoring, scheduler, and the periodic audit loop. */
    suspend fun start() {
        stateMutex.withLock {
            if (isRunning) {
                Log.w(TAG, "Already running")
                return
            }
            if (!isInitialized) {
                Log.w(TAG, "start() called before initialize() — initializing first")
            }
        }
        if (!isInitialized) initialize()

        stateMutex.withLock {
            healthMonitor.start()
            scheduler.start()

            auditJob = scope.launch {
                delay(initialAuditDelayMs)
                while (isActive) {
                    try {
                        performPeriodicAudit()
                    } catch (t: Throwable) {
                        Log.e(TAG, "Error in periodic audit", t)
                    }
                    delay(periodicAuditIntervalMs)
                }
            }
            isRunning = true
            Log.i(TAG, "CentralOrchestrator started successfully")
        }
    }

    suspend fun stop(): Unit = stateMutex.withLock {
        if (!isRunning) {
            Log.w(TAG, "Not running")
            return
        }
        Log.i(TAG, "Stopping CentralOrchestrator…")
        scheduler.stop()
        healthMonitor.stop()
        auditJob?.cancel()
        auditJob = null
        isRunning = false
        Log.i(TAG, "CentralOrchestrator stopped")
    }

    /** Tear down everything — call from app/service onDestroy. */
    fun shutdown() {
        scheduler.shutdown()
        problemSolvingBroker.shutdown()
        eventRouter.shutdown()
        scope.cancel()
        isRunning = false
        isInitialized = false
        Log.i(TAG, "CentralOrchestrator shut down")
    }

    private fun subscribeToEvents() {
        eventRouter.subscribe(OrchestrationEvent.Type.COMPONENT_ERROR) { event ->
            Log.w(TAG, "Component error detected: ${event.source}")
            handleComponentError(event)
        }
        eventRouter.subscribe(OrchestrationEvent.Type.COMPONENT_DEGRADED) { event ->
            Log.w(TAG, "Component degraded: ${event.source}")
            handleComponentDegradation(event)
        }
        eventRouter.subscribe(OrchestrationEvent.Type.STATE_DIFF_DETECTED) { event ->
            Log.i(TAG, "State diff detected on ${event.source}")
            handleStateDiff(event)
        }
        eventRouter.subscribe(OrchestrationEvent.Type.HEALTH_CHECK_FAILED) { event ->
            Log.e(TAG, "Health check failed: ${event.source}")
            healthMonitor.attemptWarmRestart(event.source)
        }
    }

    private fun handleComponentError(event: OrchestrationEvent) {
        val componentId = event.source
        val errorType = event.get("error_type") as? String ?: "COMPONENT_ERROR"
        val errorMessage = event.get("error_message") as? String ?: "Unknown error"

        // Donor used FeedbackSystem + ErrorResolutionWorkflow first; both are
        // unported. Skip directly to broker submission so the LLM tries to
        // diagnose.
        val ticket = ProblemTicket(
            componentId = componentId,
            problemType = errorType,
            description = errorMessage,
            context = event.data,
        )
        problemSolvingBroker.submitProblem(ticket)
    }

    private fun handleComponentDegradation(event: OrchestrationEvent) {
        val componentId = event.source
        healthMonitor.isolateComponent(componentId)
        scheduler.adjustScheduling(componentId, enable = false)
    }

    private fun handleStateDiff(event: OrchestrationEvent) {
        val diff = event.get("diff") as? StateDiff ?: return
        if (diff.severity == StateDiff.Severity.CRITICAL) {
            Log.e(TAG, "Critical state diff on ${diff.componentId}: ${diff.description}")
            // Future: forward to ErrorResolutionWorkflow once it lives in the spine.
        }
    }

    private fun performPeriodicAudit() {
        Log.d(TAG, "Performing periodic audit")
        healthMonitor.performHealthCheck()
        diffEngine.performPeriodicDiffCheck()
    }

    /** Convenience accessor for the application context held by orchestration. */
    fun applicationContext(): Context = appContext

    companion object {
        private const val TAG = "CentralOrchestrator"

        /** Stub solver that returns a polite no-op response — used until the
         *  spine wires the real LlamaEngine adapter. */
        val NoopProblemSolver = ProblemSolvingBroker.ProblemSolver { _ ->
            "ProblemSolver not configured. Component error noted but no LLM-based diagnosis available."
        }
    }
}

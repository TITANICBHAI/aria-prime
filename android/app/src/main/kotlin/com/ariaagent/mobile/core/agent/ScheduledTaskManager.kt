package com.ariaagent.mobile.core.agent

import android.content.Context
import android.util.Log
import com.ariaagent.mobile.core.events.AgentEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * ScheduledTaskManager — priority-sorted task scheduler for the agent.
 *
 * Ported from donors/scheduler-java/TaskSchedulerManager.java with:
 *   - java.util.concurrent.ScheduledExecutorService → Kotlin coroutines (1 s ticker)
 *   - Java object serialization  → org.json (JSON file, no Serializable classes)
 *   - ActionExecutor callback    → suspend `TaskExecutor` functional interface
 *   - Main-thread handler        → structured coroutine launch on Main dispatcher
 *
 * Key features (all preserved from donor):
 *   ✓ Priority ordering: HIGH > NORMAL > LOW (secondary: earliest scheduled time)
 *   ✓ Max 5 concurrent tasks (adjustable via MAX_CONCURRENT)
 *   ✓ Max 100 total tasks; oldest idle task evicted when full
 *   ✓ Recurrence: ONCE / INTERVAL_MINS / DAILY / WEEKLY / MONTHLY
 *   ✓ Per-task enabled flag + execution/success/failure counters
 *   ✓ Persistence: {filesDir}/tasks/aria_tasks.json
 *   ✓ Task lifecycle events via AgentEventBus
 *
 * Integration with AgentForegroundService:
 *   val mgr = ScheduledTaskManager(context, executor = { task ->
 *       agentLoop.runOnce(task.goal, task.appPackage)
 *   })
 *   mgr.start()
 *
 * Phase: 3 (Task Queue / Orchestration)
 */
class ScheduledTaskManager(
    private val context: Context,
    private val executor: TaskExecutor? = null
) {

    // ── Data types ─────────────────────────────────────────────────────────────

    enum class Priority(val value: Int) { LOW(0), NORMAL(1), HIGH(2) }

    enum class RecurrenceType { ONCE, INTERVAL_MINS, DAILY, WEEKLY, MONTHLY }

    enum class Status { PENDING, SCHEDULED, RUNNING, COMPLETED, FAILED, CANCELLED }

    data class ScheduledTask(
        val id:             String            = "task_${UUID.randomUUID().toString().take(12)}",
        val name:           String            = "",
        val goal:           String            = "",
        val appPackage:     String            = "",
        val priority:       Priority          = Priority.NORMAL,
        val recurrence:     RecurrenceType    = RecurrenceType.ONCE,
        val intervalMins:   Long              = 0L,   // for INTERVAL_MINS
        val scheduledAtMs:  Long              = System.currentTimeMillis(),
        val enabled:        Boolean           = true,
        var status:         Status            = Status.SCHEDULED,
        var executionCount: Int               = 0,
        var successCount:   Int               = 0,
        var failureCount:   Int               = 0,
        var lastRunMs:      Long              = 0L,
        var nextRunMs:      Long              = System.currentTimeMillis(),
        var lastError:      String            = ""
    )

    fun interface TaskExecutor {
        suspend fun execute(task: ScheduledTask): Boolean
    }

    // ── State ──────────────────────────────────────────────────────────────────

    private val tasks        = ConcurrentHashMap<String, ScheduledTask>()
    private val runningIds   = ConcurrentHashMap.newKeySet<String>()
    private val scope        = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var tickerJob:   Job? = null

    private val _allTasks    = MutableStateFlow<List<ScheduledTask>>(emptyList())
    val allTasksFlow: StateFlow<List<ScheduledTask>> = _allTasks

    private val _isRunning   = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning

    companion object {
        private const val TAG              = "ScheduledTaskManager"
        private const val TICK_MS          = 1_000L
        private const val MAX_CONCURRENT   = 5
        private const val MAX_TASKS        = 100
        private const val TASKS_FILENAME   = "aria_tasks.json"
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    init { loadTasks() }

    /** Start the 1-second ticker. Idempotent. */
    fun start() {
        if (_isRunning.value) return
        _isRunning.value = true
        tickerJob = scope.launch {
            Log.i(TAG, "Scheduler started — ${tasks.size} tasks loaded")
            while (isActive) {
                processTasks()
                delay(TICK_MS)
            }
        }
    }

    /** Stop the ticker and cancel all running tasks. */
    suspend fun stop() {
        _isRunning.value = false
        tickerJob?.cancelAndJoin()
        tickerJob = null
        runningIds.forEach { cancelTask(it) }
        Log.i(TAG, "Scheduler stopped")
    }

    // ── Public task management ─────────────────────────────────────────────────

    /**
     * Schedule a new task. Returns the task ID, or null if the queue is full.
     */
    fun schedule(task: ScheduledTask): String? {
        if (tasks.size >= MAX_TASKS) evictOldest() ?: return null
        tasks[task.id] = task.copy(status = Status.SCHEDULED, nextRunMs = task.scheduledAtMs)
        saveTasks()
        publishState()
        AgentEventBus.emit("task_scheduled", mapOf("id" to task.id, "name" to task.name))
        Log.d(TAG, "Scheduled: ${task.id} — '${task.name}' @ ${task.priority}")
        return task.id
    }

    /** Cancel a task (running or scheduled). */
    fun cancelTask(id: String): Boolean {
        val task = tasks[id] ?: return false
        val old = task.status
        task.status = Status.CANCELLED
        runningIds.remove(id)
        saveTasks(); publishState()
        AgentEventBus.emit("task_cancelled", mapOf("id" to id, "prevStatus" to old.name))
        return true
    }

    /** Delete a task permanently. */
    fun deleteTask(id: String): Boolean {
        cancelTask(id)
        val removed = tasks.remove(id) != null
        if (removed) { saveTasks(); publishState() }
        return removed
    }

    /** Enable or disable a task without deleting it. */
    fun setEnabled(id: String, enabled: Boolean) {
        tasks[id]?.let { it.status = if (enabled) Status.SCHEDULED else Status.CANCELLED }
        saveTasks(); publishState()
    }

    fun getTask(id: String): ScheduledTask? = tasks[id]

    fun getByStatus(status: Status): List<ScheduledTask> =
        tasks.values.filter { it.status == status }

    fun getByPriority(priority: Priority): List<ScheduledTask> =
        tasks.values.filter { it.priority == priority }

    /** Number of tasks currently executing. */
    fun runningCount(): Int = runningIds.size

    // ── Ticker ─────────────────────────────────────────────────────────────────

    private fun processTasks() {
        if (runningIds.size >= MAX_CONCURRENT) return
        val now = System.currentTimeMillis()

        val candidates = tasks.values
            .filter { it.enabled && (it.status == Status.SCHEDULED || it.status == Status.PENDING) && it.nextRunMs <= now }
            .sortedWith(compareByDescending<ScheduledTask> { it.priority.value }.thenBy { it.nextRunMs })

        for (task in candidates) {
            if (runningIds.size >= MAX_CONCURRENT) break
            executeTask(task)
        }
    }

    private fun executeTask(task: ScheduledTask) {
        if (runningIds.contains(task.id)) return
        runningIds += task.id
        val old = task.status
        task.status        = Status.RUNNING
        task.executionCount++
        task.lastRunMs     = System.currentTimeMillis()
        publishState()
        AgentEventBus.emit("task_started", mapOf("id" to task.id, "name" to task.name))

        scope.launch {
            var success = false
            try {
                success = executor?.execute(task) ?: true
            } catch (e: Exception) {
                Log.e(TAG, "Task ${task.id} threw: ${e.message}")
                task.lastError = "${e.javaClass.simpleName}: ${e.message}"
            } finally {
                completeTask(task, success)
            }
        }
    }

    private fun completeTask(task: ScheduledTask, success: Boolean) {
        runningIds -= task.id
        if (success) { task.status = Status.COMPLETED; task.successCount++ }
        else         { task.status = Status.FAILED;    task.failureCount++ }

        if (task.recurrence != RecurrenceType.ONCE) reschedule(task)

        saveTasks(); publishState()
        AgentEventBus.emit(
            "task_completed",
            mapOf("id" to task.id, "success" to success, "executions" to task.executionCount)
        )
        Log.d(TAG, "Task ${task.id} ${if (success) "completed" else "failed"} " +
                   "(tries=${task.executionCount}, ok=${task.successCount})")
    }

    // ── Recurrence ─────────────────────────────────────────────────────────────

    private fun reschedule(task: ScheduledTask) {
        val now = System.currentTimeMillis()
        val nextMs = when (task.recurrence) {
            RecurrenceType.ONCE          -> return
            RecurrenceType.INTERVAL_MINS -> now + task.intervalMins * 60_000L
            RecurrenceType.DAILY         -> now + 24 * 60 * 60_000L
            RecurrenceType.WEEKLY        -> now + 7 * 24 * 60 * 60_000L
            RecurrenceType.MONTHLY       -> now + 30L * 24 * 60 * 60_000L
        }
        task.nextRunMs = nextMs
        task.status    = Status.SCHEDULED
        Log.d(TAG, "Rescheduled ${task.id} → ${java.util.Date(nextMs)}")
    }

    // ── Eviction ───────────────────────────────────────────────────────────────

    private fun evictOldest(): ScheduledTask? {
        val oldest = tasks.values
            .filter { !runningIds.contains(it.id) }
            .minByOrNull { it.scheduledAtMs }
        oldest?.let { tasks.remove(it.id) }
        return oldest
    }

    // ── Persistence (JSON) ─────────────────────────────────────────────────────

    private fun tasksFile(): File {
        val dir = File(context.filesDir, "tasks").also { it.mkdirs() }
        return File(dir, TASKS_FILENAME)
    }

    private fun saveTasks() {
        try {
            val arr = JSONArray()
            tasks.values.forEach { t ->
                arr.put(JSONObject().apply {
                    put("id",             t.id)
                    put("name",           t.name)
                    put("goal",           t.goal)
                    put("appPackage",     t.appPackage)
                    put("priority",       t.priority.name)
                    put("recurrence",     t.recurrence.name)
                    put("intervalMins",   t.intervalMins)
                    put("scheduledAtMs",  t.scheduledAtMs)
                    put("enabled",        t.enabled)
                    put("status",         t.status.name)
                    put("executionCount", t.executionCount)
                    put("successCount",   t.successCount)
                    put("failureCount",   t.failureCount)
                    put("lastRunMs",      t.lastRunMs)
                    put("nextRunMs",      t.nextRunMs)
                    put("lastError",      t.lastError)
                })
            }
            tasksFile().writeText(arr.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Save failed: ${e.message}")
        }
    }

    private fun loadTasks() {
        try {
            val file = tasksFile()
            if (!file.exists() || file.length() == 0L) return
            val arr = JSONArray(file.readText())
            repeat(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                val task = ScheduledTask(
                    id             = o.getString("id"),
                    name           = o.optString("name"),
                    goal           = o.optString("goal"),
                    appPackage     = o.optString("appPackage"),
                    priority       = runCatching { Priority.valueOf(o.getString("priority")) }.getOrDefault(Priority.NORMAL),
                    recurrence     = runCatching { RecurrenceType.valueOf(o.getString("recurrence")) }.getOrDefault(RecurrenceType.ONCE),
                    intervalMins   = o.optLong("intervalMins", 0L),
                    scheduledAtMs  = o.optLong("scheduledAtMs", System.currentTimeMillis()),
                    enabled        = o.optBoolean("enabled", true),
                    status         = runCatching { Status.valueOf(o.getString("status")) }.getOrDefault(Status.SCHEDULED),
                    executionCount = o.optInt("executionCount", 0),
                    successCount   = o.optInt("successCount", 0),
                    failureCount   = o.optInt("failureCount", 0),
                    lastRunMs      = o.optLong("lastRunMs", 0L),
                    nextRunMs      = o.optLong("nextRunMs", System.currentTimeMillis()),
                    lastError      = o.optString("lastError", "")
                )
                // Never restore RUNNING status across restarts — it's orphaned
                if (task.status != Status.RUNNING) tasks[task.id] = task
                else tasks[task.id] = task.copy(status = Status.FAILED, lastError = "Interrupted by restart")
            }
            publishState()
            Log.i(TAG, "Loaded ${tasks.size} tasks from disk")
        } catch (e: Exception) {
            Log.w(TAG, "Load failed: ${e.message}")
        }
    }

    private fun publishState() {
        _allTasks.value = tasks.values
            .sortedWith(compareByDescending<ScheduledTask> { it.priority.value }.thenBy { it.nextRunMs })
            .toList()
    }
}

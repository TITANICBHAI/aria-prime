package com.ariaagent.mobile.core.triggers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import com.ariaagent.mobile.core.events.AgentEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * TriggerEvaluator — runtime backend for the Triggers feature.
 *
 * Evaluates all stored [TriggerItem]s and fires them at the right moment.
 * Wired into [com.ariaagent.mobile.system.AgentForegroundService] which
 * starts and stops it as part of the service lifecycle.
 *
 * Evaluation strategies by [TriggerType]:
 *   TIME_ONCE    — fires once at a specific HH:MM; marked done via SharedPrefs so
 *                  it will never fire again even across restarts.
 *   TIME_DAILY   — fires once per calendar day at HH:MM; deduped by epoch-day number.
 *   TIME_WEEKLY  — fires once per calendar week on the configured day-of-week + time;
 *                  deduped by epoch-day number.
 *   APP_LAUNCH   — fires on every "app_focus_changed" AgentEventBus event that matches
 *                  [TriggerItem.watchPackage]. Emitted by AgentAccessibilityService
 *                  when the foreground app switches to a new package.
 *   CHARGING     — fires once per calendar day when the device starts charging; driven
 *                  by a BroadcastReceiver for ACTION_BATTERY_CHANGED.
 *
 * Firing is done by emitting a "trigger_fired" event on [AgentEventBus].
 * AgentForegroundService subscribes to that event and starts the agent loop.
 * This keeps the trigger layer independent of the service layer — no circular
 * imports between core/ and system/.
 *
 * Thread safety:
 *   [start] and [stop] must be called from the main thread (service lifecycle).
 *   The internal [scope] and broadcast receiver manage all cross-thread work safely.
 *
 * Deduplication:
 *   Per-trigger "last fired epoch day" is stored in SharedPreferences under the key
 *   "fired_day_<triggerId>". TIME_ONCE uses -1 as sentinel (never fired).
 */
class TriggerEvaluator(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // ── Deduplication helpers ─────────────────────────────────────────────────

    /**
     * Returns the current calendar day as an integer (days since Unix epoch).
     * Used for once-per-day deduplication without timezone-sensitive date strings.
     */
    private fun epochDay(): Int = (System.currentTimeMillis() / 86_400_000L).toInt()

    /**
     * Returns the epoch-day on which [triggerId] was last fired, or -1 if never.
     */
    private fun lastFiredDay(triggerId: String): Int =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt("$KEY_FIRED_PREFIX$triggerId", -1)

    /**
     * Persists today's epoch-day as the last-fired marker for [triggerId].
     * For TIME_ONCE triggers this permanently prevents re-firing.
     */
    private fun markFiredToday(triggerId: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt("$KEY_FIRED_PREFIX$triggerId", epochDay())
            .apply()
    }

    // ── Charging broadcast ────────────────────────────────────────────────────

    private val chargingReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
            if (isCharging) {
                scope.launch { evaluateChargingTriggers() }
            }
        }
    }

    // ── Public lifecycle ───────────────────────────────────────────────────────

    /**
     * Start the evaluator. Registers the charging receiver and launches coroutines
     * for the time-based polling loop and the app-launch event subscriber.
     * Safe to call multiple times — subsequent calls are no-ops for already-running jobs.
     */
    fun start() {
        // ── Time-based polling: check every 60 s to catch any HH:MM window ──
        scope.launch {
            while (isActive) {
                try {
                    evaluateTimeTriggers()
                } catch (e: Exception) {
                    Log.w(TAG, "Time trigger evaluation failed: ${e.message}")
                }
                delay(POLL_INTERVAL_MS)
            }
        }

        // ── App-launch triggers: subscribe to AgentEventBus ───────────────────
        // AgentAccessibilityService emits "app_focus_changed" on every foreground
        // app switch (TYPE_WINDOW_STATE_CHANGED for non-IME packages).
        scope.launch {
            AgentEventBus.flow.collect { (name, data) ->
                if (name == "app_focus_changed") {
                    val pkg = data["package"] as? String ?: return@collect
                    try {
                        evaluateAppLaunchTriggers(pkg)
                    } catch (e: Exception) {
                        Log.w(TAG, "App-launch trigger evaluation failed: ${e.message}")
                    }
                }
            }
        }

        // ── Charging triggers: battery broadcast ───────────────────────────────
        context.registerReceiver(
            chargingReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        Log.i(TAG, "TriggerEvaluator started")
    }

    /**
     * Stop the evaluator. Unregisters the charging receiver and cancels all coroutines.
     * Always safe to call even if [start] was never called.
     */
    fun stop() {
        try { context.unregisterReceiver(chargingReceiver) } catch (_: Exception) {}
        scope.cancel()
        Log.i(TAG, "TriggerEvaluator stopped")
    }

    // ── Evaluation methods ─────────────────────────────────────────────────────

    /**
     * Evaluate all time-based triggers (DAILY, WEEKLY, ONCE) against the current
     * clock reading. Called every [POLL_INTERVAL_MS] by the polling coroutine.
     */
    private suspend fun evaluateTimeTriggers() {
        val cal       = Calendar.getInstance()
        val hour      = cal.get(Calendar.HOUR_OF_DAY)
        val minute    = cal.get(Calendar.MINUTE)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)   // Calendar.SUNDAY=1 … SATURDAY=7
        val today     = epochDay()

        val triggers = withContext(Dispatchers.IO) { TriggerStore.load(context) }
        triggers.filter { it.enabled }.forEach { trigger ->
            val shouldFire = when (trigger.type) {
                TriggerType.TIME_DAILY ->
                    trigger.hourOfDay == hour &&
                    trigger.minuteOfHour == minute &&
                    lastFiredDay(trigger.id) != today

                TriggerType.TIME_WEEKLY ->
                    trigger.dayOfWeek == dayOfWeek &&
                    trigger.hourOfDay == hour &&
                    trigger.minuteOfHour == minute &&
                    lastFiredDay(trigger.id) != today

                TriggerType.TIME_ONCE ->
                    trigger.hourOfDay == hour &&
                    trigger.minuteOfHour == minute &&
                    lastFiredDay(trigger.id) < 0   // never fired sentinel

                else -> false
            }
            if (shouldFire) {
                markFiredToday(trigger.id)
                fireTrigger(trigger)
            }
        }
    }

    /**
     * Evaluate all APP_LAUNCH triggers whenever the foreground app changes to
     * [launchedPackage]. Does NOT deduplicate — every foreground switch fires.
     */
    private suspend fun evaluateAppLaunchTriggers(launchedPackage: String) {
        val triggers = withContext(Dispatchers.IO) { TriggerStore.load(context) }
        triggers
            .filter { it.enabled &&
                it.type == TriggerType.APP_LAUNCH &&
                it.watchPackage == launchedPackage }
            .forEach { fireTrigger(it) }
    }

    /**
     * Evaluate all CHARGING triggers. Fires at most once per calendar day per trigger.
     */
    private suspend fun evaluateChargingTriggers() {
        val today    = epochDay()
        val triggers = withContext(Dispatchers.IO) { TriggerStore.load(context) }
        triggers
            .filter { it.enabled &&
                it.type == TriggerType.CHARGING &&
                lastFiredDay(it.id) != today }
            .forEach { trigger ->
                markFiredToday(trigger.id)
                fireTrigger(trigger)
            }
    }

    // ── Firing ─────────────────────────────────────────────────────────────────

    /**
     * Emit a "trigger_fired" event to [AgentEventBus].
     *
     * [AgentForegroundService] listens for this event and starts the agent loop
     * for the trigger's goal. This indirection keeps TriggerEvaluator free of
     * any dependency on the system/ layer.
     */
    private fun fireTrigger(trigger: TriggerItem) {
        if (trigger.goal.isBlank()) {
            Log.w(TAG, "Trigger ${trigger.id} (${trigger.type}) has a blank goal — skipping")
            return
        }
        Log.i(TAG, "Firing trigger ${trigger.type} id=${trigger.id} goal='${trigger.goal.take(60)}'")
        AgentEventBus.emit(
            "trigger_fired",
            mapOf(
                "triggerId"   to trigger.id,
                "triggerType" to trigger.type.name,
                "goal"        to trigger.goal,
                "appPackage"  to trigger.goalAppPackage,
            )
        )
    }

    companion object {
        private const val TAG              = "TriggerEvaluator"
        private const val PREFS_NAME       = "aria_trigger_state"
        private const val KEY_FIRED_PREFIX = "fired_day_"
        private const val POLL_INTERVAL_MS = 60_000L
    }
}

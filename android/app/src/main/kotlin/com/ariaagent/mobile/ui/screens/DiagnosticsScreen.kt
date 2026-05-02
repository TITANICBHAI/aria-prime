@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.ariaagent.mobile.ui.screens

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ariaagent.mobile.core.logging.CrashHandler
import com.ariaagent.mobile.ui.theme.ARIAColors
import com.ariaagent.mobile.ui.viewmodel.AgentViewModel
import com.ariaagent.mobile.ui.viewmodel.OrchestrationComponentUi
import com.ariaagent.mobile.ui.viewmodel.OrchestrationEventUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * DiagnosticsScreen — surfaces the CentralOrchestrator subsystem (rounds 1–3)
 * that previously had no UI. Shows:
 *
 *   • Floating-chat overlay toggle (also requests SYSTEM_ALERT_WINDOW if missing)
 *   • Orchestrator availability + last refresh timestamp
 *   • Live component registry + per-component health (status, errors, restarts,
 *     circuit-breaker state, last heartbeat)
 *   • Recent orchestration bus events (last 50, color-coded by severity)
 *   • Last LLM problem-solver resolution
 *
 * Polls vm.refreshOrchestrationStatus() every 5 seconds while visible.
 */
@Composable
fun DiagnosticsScreen(
    vm: AgentViewModel = viewModel(),
    onBack: () -> Unit,
) {
    val status        by vm.orchestrationStatus.collectAsStateWithLifecycle()
    val events        by vm.orchestrationEvents.collectAsStateWithLifecycle()
    val overlayActive by vm.floatingChatActive.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        vm.refreshOrchestrationStatus()
        vm.refreshFloatingChatActive()
        while (true) {
            delay(5_000)
            vm.refreshOrchestrationStatus()
            vm.refreshFloatingChatActive()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ARIAColors.Background)
    ) {
        // ── Top bar ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ARIAColors.Surface)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ARIAColors.OnSurface)
            }
            Text(
                "Diagnostics",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = ARIAColors.OnSurface, fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { vm.refreshOrchestrationStatus() }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = ARIAColors.Muted)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {

            // ── Floating-chat overlay toggle ────────────────────────────────
            ARIACard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        Icons.Default.PictureInPicture,
                        contentDescription = null,
                        tint = if (overlayActive) ARIAColors.Success else ARIAColors.Muted,
                        modifier = Modifier.size(22.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Floating Chat Overlay",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = ARIAColors.OnSurface, fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            if (overlayActive) "Visible — tap to dismiss"
                            else "Hidden — tap to summon (requires display-over-other-apps)",
                            style = MaterialTheme.typography.bodySmall.copy(color = ARIAColors.Muted)
                        )
                    }
                    Switch(
                        checked = overlayActive,
                        onCheckedChange = { vm.toggleFloatingChat() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor   = ARIAColors.Primary,
                            checkedTrackColor   = ARIAColors.Primary.copy(alpha = 0.4f),
                            uncheckedThumbColor = ARIAColors.Muted,
                            uncheckedTrackColor = ARIAColors.Surface,
                        )
                    )
                }
            }

            // ── Round 14 §77: Device / system info card ────────────────────
            ARIACard {
                val sysCtx = LocalContext.current
                val memInfo = remember {
                    ActivityManager.MemoryInfo().also { mi ->
                        (sysCtx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
                            .getMemoryInfo(mi)
                    }
                }
                Text(
                    "DEVICE INFO",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = ARIAColors.Muted, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp
                    )
                )
                Spacer(Modifier.height(8.dp))
                DiagInfoRow("Model",     "${Build.MANUFACTURER} ${Build.MODEL}")
                DiagInfoRow("Android",   "${Build.VERSION.RELEASE}  (API ${Build.VERSION.SDK_INT})")
                DiagInfoRow("ABI",       Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown")
                DiagInfoRow("Total RAM", "${memInfo.totalMem / (1024 * 1024)} MB")
                DiagInfoRow("Avail RAM", "${memInfo.availMem  / (1024 * 1024)} MB  " +
                    "(${(memInfo.availMem * 100 / memInfo.totalMem.coerceAtLeast(1)).toInt()}% free)")
                DiagInfoRow("Low RAM",   if (memInfo.lowMemory) "YES ⚠" else "No")
            }

            // ── Orchestrator header ─────────────────────────────────────────
            ARIACard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (status.available) ARIAColors.Success else ARIAColors.Muted)
                    )
                    Text(
                        if (status.available) "Orchestrator: ONLINE" else "Orchestrator: not started",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (status.available) ARIAColors.Success else ARIAColors.Muted,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    if (status.lastRefreshAt > 0L) {
                        Text(
                            "refreshed " + relativeTime(status.lastRefreshAt),
                            style = MaterialTheme.typography.labelSmall.copy(color = ARIAColors.Muted)
                        )
                    }
                }
                if (!status.available) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "The CentralOrchestrator starts when the agent foreground service runs. " +
                        "Use Control → Start to bring it up; this screen will populate.",
                        style = MaterialTheme.typography.bodySmall.copy(color = ARIAColors.Muted)
                    )
                }
            }

            // ── Components ──────────────────────────────────────────────────
            if (status.components.isNotEmpty()) {
                Text(
                    "REGISTERED COMPONENTS (${status.components.size})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = ARIAColors.Primary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                    )
                )
                status.components.forEach { c ->
                    ComponentRow(c)
                }
            }

            // ── Last LLM resolution ─────────────────────────────────────────
            if (status.lastResolution.isNotBlank()) {
                Text(
                    "LAST LLM RESOLUTION",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = ARIAColors.Accent, fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                    )
                )
                ARIACard {
                    Column {
                        Text(
                            relativeTime(status.lastResolutionAt),
                            style = MaterialTheme.typography.labelSmall.copy(color = ARIAColors.Muted)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            status.lastResolution,
                            style = MaterialTheme.typography.bodySmall.copy(color = ARIAColors.OnSurface)
                        )
                    }
                }
            }

            // ── Recent events ───────────────────────────────────────────────
            Text(
                "RECENT ORCHESTRATION EVENTS (${events.size})",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = ARIAColors.Primary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                )
            )
            if (events.isEmpty()) {
                ARIACard {
                    Text(
                        "No events yet. Component lifecycle, health-degrade, error and " +
                        "state-diff signals will appear here as they happen.",
                        style = MaterialTheme.typography.bodySmall.copy(color = ARIAColors.Muted)
                    )
                }
            } else {
                events.forEach { evt -> EventRow(evt) }
            }

            // ── Crash / error log viewer ─────────────────────────────────────
            val context = LocalContext.current
            var crashLines by remember { mutableStateOf<List<String>>(emptyList()) }
            var logExpanded by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                crashLines = withContext(Dispatchers.IO) { readLastLogLines(context, 60) }
            }

            // ── Progress log size chip ───────────────────────────────────────
            // Round 12: shows the current size of aria_progress.txt on disk so
            // users can see if it's growing unexpectedly.
            var progressLogBytes by remember { mutableLongStateOf(0L) }
            LaunchedEffect(Unit) {
                progressLogBytes = withContext(Dispatchers.IO) {
                    com.ariaagent.mobile.core.persistence.ProgressPersistence.logFileSizeBytes(context)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "ON-DEVICE LOG (last lines)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = ARIAColors.Accent, fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                    )
                )
                if (progressLogBytes > 0L) {
                    // Round 15 §86: clear progress.txt inline button with confirmation.
                    var showClearProgressDialog by remember { mutableStateOf(false) }
                    if (showClearProgressDialog) {
                        AlertDialog(
                            onDismissRequest = { showClearProgressDialog = false },
                            title = { Text("Clear Progress Log?") },
                            text  = { Text("This will delete all task history in progress.txt. This cannot be undone.") },
                            confirmButton = {
                                TextButton(onClick = {
                                    showClearProgressDialog = false
                                    kotlinx.coroutines.MainScope().launch {
                                        withContext(Dispatchers.IO) {
                                            com.ariaagent.mobile.core.persistence.ProgressPersistence.clear(context)
                                        }
                                        progressLogBytes = 0L
                                    }
                                }) { Text("Clear", color = ARIAColors.Destructive) }
                            },
                            dismissButton = {
                                TextButton(onClick = { showClearProgressDialog = false }) {
                                    Text("Cancel", color = ARIAColors.Muted)
                                }
                            }
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "progress.txt: ${String.format("%.1f", progressLogBytes / 1024.0)} KB",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ARIAColors.Muted, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        )
                        TextButton(
                            onClick = { showClearProgressDialog = true },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                        ) {
                            Text("CLEAR", style = MaterialTheme.typography.labelSmall.copy(
                                color = ARIAColors.Destructive, fontSize = 9.sp, fontWeight = FontWeight.Bold
                            ))
                        }
                    }
                }
            }
            ARIACard {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            if (crashLines.isEmpty()) "No log file found"
                            else "${crashLines.size} lines from app.log",
                            style = MaterialTheme.typography.bodySmall.copy(color = ARIAColors.Muted)
                        )
                        if (crashLines.isNotEmpty()) {
                            TextButton(
                                onClick = { logExpanded = !logExpanded },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    if (logExpanded) "Collapse" else "Expand",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = ARIAColors.Primary
                                    )
                                )
                            }
                        }
                    }
                    if (logExpanded && crashLines.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ARIAColors.Background, RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                crashLines.takeLast(40).joinToString("\n"),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color      = ARIAColors.OnSurface,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize   = 10.sp,
                                    lineHeight = 14.sp
                                )
                            )
                        }
                    }
                }
            }

            // ── Crash file list ──────────────────────────────────────────────
            // Round 11: lists every .txt crash report written by CrashHandler.
            // Round 12: adds "Clear All" button to delete all crash files at once.
            var crashFiles by remember { mutableStateOf<List<File>>(emptyList()) }
            var expandedCrashFile by remember { mutableStateOf<String?>(null) }
            var showClearCrashConfirm by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                crashFiles = withContext(Dispatchers.IO) { CrashHandler.listCrashes() }
            }

            if (showClearCrashConfirm) {
                AlertDialog(
                    onDismissRequest = { showClearCrashConfirm = false },
                    title   = { Text("Clear all crash reports?", fontWeight = FontWeight.SemiBold) },
                    text    = { Text("Deletes all ${crashFiles.size} crash file(s) from device storage. This cannot be undone.") },
                    confirmButton = {
                        TextButton(onClick = {
                            crashFiles.forEach { it.delete() }
                            crashFiles = emptyList()
                            expandedCrashFile = null
                            showClearCrashConfirm = false
                        }) { Text("Delete All", color = ARIAColors.Error, fontWeight = FontWeight.Bold) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearCrashConfirm = false }) { Text("Cancel") }
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "CRASH REPORTS (${crashFiles.size})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (crashFiles.isEmpty()) ARIAColors.Muted else ARIAColors.Error,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
                if (crashFiles.isNotEmpty()) {
                    TextButton(
                        onClick = { showClearCrashConfirm = true },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "Clear All",
                            style = MaterialTheme.typography.labelSmall.copy(color = ARIAColors.Error)
                        )
                    }
                }
            }
            if (crashFiles.isEmpty()) {
                ARIACard {
                    Text(
                        "No crash reports on disk.",
                        style = MaterialTheme.typography.bodySmall.copy(color = ARIAColors.Muted)
                    )
                }
            } else {
                crashFiles.sortedByDescending { it.lastModified() }.forEach { file ->
                    val isExpanded = expandedCrashFile == file.name
                    ARIACard(containerColor = if (isExpanded) ARIAColors.Surface else ARIAColors.Background) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    file.name,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = ARIAColors.Error, fontWeight = FontWeight.SemiBold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                )
                                Text(
                                    "${String.format("%.1f", file.length() / 1024.0)} KB  •  " +
                                    relativeTime(file.lastModified()),
                                    style = MaterialTheme.typography.labelSmall.copy(color = ARIAColors.Muted)
                                )
                            }
                            // Round 16 §103: share individual crash report via system share sheet.
                            val diagCtx = LocalContext.current
                            IconButton(
                                onClick  = {
                                    val text = runCatching { file.readText() }.getOrDefault("(unreadable)")
                                    diagCtx.startActivity(
                                        Intent.createChooser(
                                            Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, text)
                                                putExtra(Intent.EXTRA_SUBJECT, "ARIA Crash: ${file.name}")
                                            },
                                            "Share Crash Report"
                                        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                    )
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Share, "Share crash", tint = ARIAColors.Muted, modifier = Modifier.size(14.dp))
                            }
                            TextButton(
                                onClick = { expandedCrashFile = if (isExpanded) null else file.name },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    if (isExpanded) "Collapse" else "View",
                                    style = MaterialTheme.typography.labelSmall.copy(color = ARIAColors.Primary)
                                )
                            }
                        }
                        if (isExpanded) {
                            Spacer(Modifier.height(6.dp))
                            val content = remember(file.name) {
                                runCatching { file.readText() }.getOrDefault("(unreadable)")
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(ARIAColors.Background, RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    content,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color      = ARIAColors.OnSurface,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize   = 9.sp,
                                        lineHeight = 13.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

// ─── Log reader helper ────────────────────────────────────────────────────────

/**
 * Reads the last [maxLines] from the rolling `app.log` file written by
 * [com.ariaagent.mobile.core.logging.FileLogWriter].
 * Returns an empty list if the file does not exist yet.
 */
private fun readLastLogLines(context: Context, maxLines: Int): List<String> {
    return try {
        val logDir  = File(context.filesDir, "logs")
        val logFile = File(logDir, "app.log")
        if (!logFile.exists()) return emptyList()
        val allLines = logFile.readLines(Charsets.UTF_8)
        allLines.takeLast(maxLines)
    } catch (_: Exception) { emptyList() }
}

// ─── Sub-composables ──────────────────────────────────────────────────────────

@Composable
private fun ComponentRow(c: OrchestrationComponentUi) {
    val statusColor = when (c.status) {
        "ACTIVE"                 -> ARIAColors.Success
        "DEGRADED"               -> ARIAColors.Warning
        "ERROR", "ISOLATED"      -> ARIAColors.Error
        "INITIALIZING"           -> ARIAColors.Accent
        else                     -> ARIAColors.Muted
    }
    ARIACard(
        modifier = Modifier.border(1.dp, statusColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
                Text(
                    c.componentName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = ARIAColors.OnSurface, fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    c.status,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = statusColor, fontWeight = FontWeight.Bold
                    )
                )
            }
            Text(
                "id=${c.componentId}",
                style = MaterialTheme.typography.labelSmall.copy(color = ARIAColors.Muted)
            )
            if (c.capabilities.isNotEmpty()) {
                Text(
                    "caps: " + c.capabilities.joinToString(", "),
                    style = MaterialTheme.typography.labelSmall.copy(color = ARIAColors.Muted)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricChip("ok",   c.successCount.toString(), ARIAColors.Success)
                MetricChip("err",  c.errorCount.toString(),
                    if (c.errorCount > 0) ARIAColors.Error else ARIAColors.Muted)
                MetricChip("seq-fail", c.consecutiveFailures.toString(),
                    if (c.consecutiveFailures > 0) ARIAColors.Warning else ARIAColors.Muted)
                MetricChip("restart", c.restartCount.toString(),
                    if (c.restartCount > 0) ARIAColors.Warning else ARIAColors.Muted)
                if (c.circuitBreakerOpen) {
                    MetricChip("BREAKER", "OPEN", ARIAColors.Error)
                }
            }
            Text(
                "heartbeat: " + relativeTime(c.lastHeartbeat),
                style = MaterialTheme.typography.labelSmall.copy(color = ARIAColors.Muted)
            )
        }
    }
}

@Composable
private fun MetricChip(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(color = ARIAColors.Muted)
        )
        Text(
            value,
            style = MaterialTheme.typography.labelSmall.copy(
                color = color, fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
private fun EventRow(evt: OrchestrationEventUi) {
    val color = when (evt.severity) {
        "error" -> ARIAColors.Error
        "warn"  -> ARIAColors.Warning
        else    -> ARIAColors.Muted
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ARIAColors.Surface, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                evt.summary,
                style = MaterialTheme.typography.bodySmall.copy(color = ARIAColors.OnSurface)
            )
            Text(
                "${evt.eventType}  ·  ${formatClock(evt.timestamp)}",
                style = MaterialTheme.typography.labelSmall.copy(color = ARIAColors.Muted)
            )
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

@Composable
private fun DiagInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall.copy(color = ARIAColors.Muted)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall.copy(
                color = ARIAColors.OnSurface,
                fontFamily = FontFamily.Monospace,
            )
        )
    }
}

private val clockFmt = SimpleDateFormat("HH:mm:ss", Locale.US)
private fun formatClock(ts: Long): String = clockFmt.format(Date(ts))

private fun relativeTime(ts: Long): String {
    if (ts <= 0L) return "—"
    val delta = (System.currentTimeMillis() - ts).coerceAtLeast(0L)
    return when {
        delta < 1_000L         -> "just now"
        delta < 60_000L        -> "${delta / 1_000}s ago"
        delta < 3_600_000L     -> "${delta / 60_000}m ago"
        delta < 86_400_000L    -> "${delta / 3_600_000}h ago"
        else                   -> "${delta / 86_400_000}d ago"
    }
}

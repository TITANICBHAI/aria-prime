@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.ariaagent.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ariaagent.mobile.core.ai.ModelCatalog
import com.ariaagent.mobile.core.ai.ModelManager
import com.ariaagent.mobile.ui.viewmodel.AgentViewModel
import com.ariaagent.mobile.ui.viewmodel.ChainedTaskItem
import com.ariaagent.mobile.ui.viewmodel.QueuedTaskItem
import com.ariaagent.mobile.ui.viewmodel.RecentGoalItem
import com.ariaagent.mobile.ui.theme.ARIAColors
import java.text.SimpleDateFormat
import java.util.*

/**
 * ControlScreen — start/pause/stop the agent and manage the task queue.
 *
 * Phase 4 gap-fill over the existing stub:
 *   ✓ Chained task notification banner  (chainedTask + dismissChainNotification)
 *   ✓ Learn-only mode toggle            (vm.startLearnOnly vs vm.startAgent)
 *   ✓ LLM Load Gate card               (when !moduleState.modelLoaded)
 *   ✓ Active task display              (agentState.currentTask green box)
 *   ✓ Separate queue-goal + queue-app fields (split from main goal field)
 *   ✓ "Teach the Agent" → LabelerScreen entry point
 *   ✓ Status dot in header
 *
 * Pure Compose — calls AgentViewModel which calls AgentLoop / AgentForegroundService directly.
 * No bridge, no React Native, no JS.
 */

/** Round 14 §74: keyword → package suggestions surfaced below the target-app field. */
private val PACKAGE_SUGGESTIONS = listOf(
    "youtube"     to "com.google.android.youtube",
    "chrome"      to "com.android.chrome",
    "settings"    to "com.android.settings",
    "maps"        to "com.google.android.apps.maps",
    "gmail"       to "com.google.android.gm",
    "whatsapp"    to "com.whatsapp",
    "instagram"   to "com.instagram.android",
    "calculator"  to "com.android.calculator2",
    "camera"      to "com.android.camera2",
    "gallery"     to "com.google.android.apps.photos",
    "spotify"     to "com.spotify.music",
    "twitter"     to "com.twitter.android",
)

private val PRESET_TASKS = listOf(
    "Open YouTube and play the trending video",
    "Open Settings and check available storage",
    "Open Gallery and find the latest photo",
    "Open Chrome and go to google.com",
    "Open WhatsApp and read the latest message",
    "Open Maps and search for nearby restaurants",
    "Open Calculator and compute 15% tip on 85",
    "Check battery level in Settings",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ControlScreen(
    vm: AgentViewModel = viewModel(),
    onNavigateToLabeler: (() -> Unit)? = null,
    onNavigateToGoals: (() -> Unit)? = null,
) {
    val context      = LocalContext.current
    val agentState   by vm.agentState.collectAsStateWithLifecycle()
    val moduleState  by vm.moduleState.collectAsStateWithLifecycle()
    val taskQueue    by vm.taskQueue.collectAsStateWithLifecycle()
    val chainedTask  by vm.chainedTask.collectAsStateWithLifecycle()
    val loadedLlms   by vm.loadedLlms.collectAsStateWithLifecycle()
    val recentGoals  by vm.recentGoals.collectAsStateWithLifecycle()
    val networkType  by vm.networkType.collectAsStateWithLifecycle()
    val hwStats      by vm.hardwareStats.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val activeModel  = remember { ModelManager.activeEntry(context) }
    val loadedCount  = loadedLlms.values.count { it.isLoaded }

    var goalText    by remember { mutableStateOf("") }
    var targetApp   by remember { mutableStateOf("") }
    var queueGoal   by remember { mutableStateOf("") }
    var queueApp    by remember { mutableStateOf("") }
    var learnOnly   by remember { mutableStateOf(false) }

    val isRunning  = agentState.status == "running"
    val isPaused   = agentState.status == "paused"
    val isIdle     = agentState.status == "idle"
            || agentState.status == "done"
            || agentState.status == "error"
    val canStart   = isIdle && goalText.isNotBlank()
            && moduleState.modelLoaded
            && moduleState.accessibilityGranted
    val queueAtCapacity = taskQueue.size >= 20  // TaskQueueManager.MAX_QUEUE_SIZE
    // Round 15 §84: queue priority for the Add-to-Queue flow (-1=Low, 0=Normal, 1=High).
    var queuePriority by remember { mutableIntStateOf(0) }

    // Round 14 §74: packages that match keywords found in the typed goal text.
    val packageSuggests = remember(goalText) {
        if (goalText.length < 3) emptyList()
        else PACKAGE_SUGGESTIONS.filter { (keyword, _) ->
            goalText.contains(keyword, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ARIAColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        // ── Header with status dot ──────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "CONTROL",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = ARIAColors.Primary,
                    fontWeight = FontWeight.Bold
                )
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Goals shortcut button
                if (onNavigateToGoals != null) {
                    IconButton(
                        onClick  = onNavigateToGoals,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ARIAColors.Primary.copy(alpha = 0.10f))
                    ) {
                        Icon(
                            Icons.Default.Queue,
                            contentDescription = "Manage Goals",
                            tint     = ARIAColors.Primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                val dotColor = when (agentState.status) {
                    "running" -> ARIAColors.Success
                    "paused"  -> ARIAColors.Warning
                    "error"   -> ARIAColors.Error
                    else      -> ARIAColors.Muted
                }
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
                Text(
                    agentState.status.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall.copy(color = ARIAColors.Muted)
                )
                // Round 21 §160: show token rate chip next to status when running.
                if (agentState.status == "running" && agentState.tokenRate > 0.0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(ARIAColors.Success.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "%.1f t/s".format(agentState.tokenRate),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ARIAColors.Success, fontWeight = FontWeight.SemiBold, fontSize = 9.sp
                            )
                        )
                    }
                }
            }
        }

        // ── Hardware meters ───────────────────────────────────────────────────
        ARIACard {
            HardwareMeterRow(stats = hwStats)
        }

        // Round 18 §117: compact running-task banner — shows the active task at a glance.
        if (!isIdle && agentState.currentTask.isNotBlank()) {
            val bannerTint = when (agentState.status) {
                "running" -> ARIAColors.Success
                "error"   -> ARIAColors.Error
                else      -> ARIAColors.Warning
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(bannerTint.copy(alpha = 0.11f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(bannerTint))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        agentState.currentTask.take(58),
                        style    = MaterialTheme.typography.labelSmall.copy(
                            color    = ARIAColors.OnSurface,
                            fontSize = 11.sp,
                        ),
                        maxLines = 1,
                    )
                    // Round 19 §139: show target-app package as a secondary line in the banner.
                    if (agentState.currentApp.isNotBlank()) {
                        Text(
                            agentState.currentApp.substringAfterLast('.'),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color      = bannerTint,
                                fontSize   = 9.sp,
                                fontFamily = FontFamily.Monospace,
                            ),
                        )
                    }
                }
            }
        }

        // Round 19 §135: "Run again" quick chip — pre-fills the goal field with the last completed task.
        if (isIdle && agentState.lastCompletedGoal.isNotBlank() && goalText.isBlank()) {
            TextButton(
                onClick  = { goalText = agentState.lastCompletedGoal },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Replay, null, tint = ARIAColors.Primary, modifier = Modifier.size(12.dp))
                    Text(
                        "↩ Run again: ${agentState.lastCompletedGoal.take(45)}",
                        style    = MaterialTheme.typography.labelSmall.copy(
                            color    = ARIAColors.Primary,
                            fontSize = 11.sp,
                        ),
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // ── Permissions banner ────────────────────────────────────────────────
        // Shown whenever a required permission is missing so the user knows
        // exactly what to fix before starting the agent.
        val missingA11y   = !moduleState.accessibilityGranted
        val missingCapture = !moduleState.screenCaptureGranted
        if (missingA11y || missingCapture) {
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(10.dp),
                colors    = CardDefaults.cardColors(
                    containerColor = ARIAColors.Warning.copy(alpha = 0.10f)
                ),
                elevation = CardDefaults.cardElevation(0.dp),
                border    = androidx.compose.foundation.BorderStroke(
                    1.dp, ARIAColors.Warning.copy(alpha = 0.45f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.WarningAmber, contentDescription = null,
                            tint = ARIAColors.Warning, modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "PERMISSIONS NEEDED",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ARIAColors.Warning,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                    if (missingA11y) {
                        PermissionRow(
                            label  = "Accessibility Service",
                            detail = "Allows ARIA to see and interact with other apps",
                            onClick = {
                                context.startActivity(
                                    android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            }
                        )
                    }
                    if (missingCapture) {
                        PermissionRow(
                            label  = "Screen Capture",
                            detail = "Lets ARIA observe the screen — needed for vision",
                            onClick = null
                        )
                    }
                }
            }
        }

        // ── Chained task notification banner ───────────────────────────────────
        chainedTask?.let { chain ->
            ARIACard(
                modifier = Modifier.border(
                    1.dp,
                    ARIAColors.Primary.copy(alpha = 0.35f),
                    RoundedCornerShape(12.dp)
                ),
                containerColor = ARIAColors.Primary.copy(alpha = 0.08f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.Link,
                        contentDescription = null,
                        tint = ARIAColors.Primary,
                        modifier = Modifier
                            .size(15.dp)
                            .padding(top = 2.dp)
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "AUTO-CHAINED TO NEXT TASK",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = ARIAColors.Primary, fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            chain.goal,
                            style = MaterialTheme.typography.bodySmall.copy(color = ARIAColors.OnSurface),
                            maxLines = 2
                        )
                        val sub = buildString {
                            if (chain.appPackage.isNotBlank()) append(chain.appPackage)
                            if (chain.queueSize > 0) {
                                if (isNotEmpty()) append(" · ")
                                append("${chain.queueSize} more in queue")
                            }
                        }
                        if (sub.isNotBlank()) {
                            Text(
                                sub,
                                style = MaterialTheme.typography.labelSmall.copy(color = ARIAColors.Muted)
                            )
                        }
                    }
                    IconButton(
                        onClick = { vm.dismissChainNotification() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = ARIAColors.Muted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // ── LLM Load Gate ──────────────────────────────────────────────────────
        if (loadedCount > 0) {
            // ── Loaded models summary ─────────────────────────────────────────
            ARIACard(
                modifier = Modifier.border(
                    1.dp,
                    ARIAColors.Primary.copy(alpha = 0.40f),
                    RoundedCornerShape(12.dp)
                ),
                containerColor = ARIAColors.Primary.copy(alpha = 0.06f)
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Memory, null,
                        tint = ARIAColors.Primary, modifier = Modifier.size(20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "$loadedCount model${if (loadedCount == 1) "" else "s"} in RAM",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = ARIAColors.Primary, fontWeight = FontWeight.SemiBold
                            )
                        )
                        loadedLlms.values.filter { it.isLoaded }.forEach { entry ->
                            val cat = com.ariaagent.mobile.core.ai.ModelCatalog.findById(entry.modelId)
                            Text(
                                "• ${cat?.displayName ?: entry.modelId}  ·  ${entry.role.label}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = ARIAColors.Muted, fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }
        } else if (!moduleState.modelLoaded) {
            // ── No model loaded gate ──────────────────────────────────────────
            ARIACard(
                modifier = Modifier.border(
                    1.dp,
                    ARIAColors.Accent.copy(alpha = 0.40f),
                    RoundedCornerShape(12.dp)
                ),
                containerColor = ARIAColors.Accent.copy(alpha = 0.08f)
            ) {
                // Quick-load current active model
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick    = { vm.loadModel() }
                        ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Memory, null,
                        tint = ARIAColors.Accent, modifier = Modifier.size(22.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Load ${activeModel.displayName}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = ARIAColors.Accent, fontWeight = FontWeight.SemiBold
                            )
                        )
                        Text(
                            when {
                                !moduleState.modelReady ->
                                    "Not downloaded — go to Modules to pick & download a model"
                                else ->
                                    "Tap to load into RAM  ·  or go to Modules to load multiple"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(color = ARIAColors.Muted)
                        )
                    }
                    Icon(Icons.Default.ChevronRight, null,
                        tint = ARIAColors.Accent, modifier = Modifier.size(18.dp))
                }
            }
        }

        // ── Readiness checks ──────────────────────────────────────────────────
        ARIACard {
            Text("READINESS", style = MaterialTheme.typography.labelSmall.copy(color = ARIAColors.Muted))
            Spacer(Modifier.height(8.dp))
            ReadinessRow("Model downloaded", ok = moduleState.modelReady)
            ReadinessRow("Model loaded",     ok = moduleState.modelLoaded)
            ReadinessRow("Accessibility",    ok = moduleState.accessibilityGranted)
            ReadinessRow("Screen capture",   ok = moduleState.screenCaptureGranted)
        }

        // ── Goal input ────────────────────────────────────────────────────────
        ARIACard {
            Text("TASK GOAL", style = MaterialTheme.typography.labelSmall.copy(color = ARIAColors.Muted))
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = goalText,
                onValueChange = { goalText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "e.g. Open YouTube and search for cooking tutorials",
                        style = MaterialTheme.typography.bodySmall.copy(color = ARIAColors.Muted)
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = ARIAColors.Primary,
                    unfocusedBorderColor = ARIAColors.Divider,
                    focusedTextColor     = ARIAColors.OnSurface,
                    unfocusedTextColor   = ARIAColors.OnSurface,
                    cursorColor          = ARIAColors.Primary,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.clearFocus() }),
                maxLines = 3,
                enabled = isIdle
            )
            // Round 16 §100: character count indicator — turns amber at 160, red at 200.
            if (goalText.isNotBlank()) {
                Text(
                    "${goalText.length} / 200",
                    style    = MaterialTheme.typography.labelSmall.copy(
                        color      = when {
                            goalText.length >= 200 -> ARIAColors.Destructive
                            goalText.length >= 160 -> ARIAColors.Warning
                            else                   -> ARIAColors.Muted
                        },
                        fontFamily = FontFamily.Monospace,
                        fontSize   = 9.sp,
                    ),
                    modifier  = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                )
            }
            // Round 14 §73: recently-completed goals as one-tap chips.
            if (recentGoals.isNotEmpty() && isIdle) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "RECENT GOALS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = ARIAColors.Muted, fontSize = 9.sp
                    )
                )
                Spacer(Modifier.height(4.dp))
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(end = 8.dp)
                ) {
                    androidx.compose.foundation.lazy.items(recentGoals.take(5)) { item ->
                        androidx.compose.material3.AssistChip(
                            onClick = {
                                goalText  = item.goal
                                if (item.appPackage.isNotBlank()) targetApp = item.appPackage
                            },
                            label = {
                                Text(
                                    item.goal.take(40),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = ARIAColors.OnSurface, fontSize = 10.sp
                                    ),
                                    maxLines = 1,
                                )
                            },
                            colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
                                containerColor = ARIAColors.Surface,
                                labelColor     = ARIAColors.OnSurface,
                            ),
                            border = androidx.compose.material3.AssistChipDefaults.assistChipBorder(
                                enabled = true,
                                borderColor = ARIAColors.Divider
                            )
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = targetApp,
                onValueChange = { targetApp = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Target app package (optional)",
                        style = MaterialTheme.typography.bodySmall.copy(color = ARIAColors.Muted)
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = ARIAColors.Primary,
                    unfocusedBorderColor = ARIAColors.Divider,
                    focusedTextColor     = ARIAColors.OnSurface,
                    unfocusedTextColor   = ARIAColors.OnSurface,
                    cursorColor          = ARIAColors.Primary,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                maxLines = 1,
                enabled = isIdle
            )
            // Round 14 §74: package auto-suggest chips driven by goal text keywords.
            if (packageSuggests.isNotEmpty() && targetApp.isBlank() && isIdle) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "SUGGESTED PACKAGE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = ARIAColors.Muted, fontSize = 9.sp
                    )
                )
                Spacer(Modifier.height(4.dp))
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    androidx.compose.foundation.lazy.items(packageSuggests) { (_, pkg) ->
                        androidx.compose.material3.SuggestionChip(
                            onClick = { targetApp = pkg },
                            label = {
                                Text(
                                    pkg,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = ARIAColors.Primary,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontSize   = 10.sp,
                                    ),
                                    maxLines = 1
                                )
                            }
                        )
                    }
                }
            }
        }

        // ── Preset task chips ─────────────────────────────────────────────────
        ARIACard {
            Text(
                "QUICK GOALS",
                style = MaterialTheme.typography.labelSmall.copy(color = ARIAColors.Muted)
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PRESET_TASKS.forEach { preset ->
                    SuggestionChip(
                        onClick = {
                            goalText = preset
                            focusManager.clearFocus()
                        },
                        label = {
                            Text(
                                preset.take(32) + if (preset.length > 32) "…" else "",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = ARIAColors.Primary.copy(alpha = 0.08f),
                            labelColor = ARIAColors.Primary
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = ARIAColors.Primary.copy(alpha = 0.25f)
                        )
                    )
                }
            }
        }

        // Round 15 §91: metered network warning — visible when on mobile data and agent is idle.
        if (networkType == "mobile" && isIdle) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(ARIAColors.Warning.copy(alpha = 0.10f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.SignalCellularAlt, null, tint = ARIAColors.Warning, modifier = Modifier.size(16.dp))
                Text(
                    "Mobile data detected — large inference tasks may use significant data.",
                    style = MaterialTheme.typography.bodySmall.copy(color = ARIAColors.Warning)
                )
            }
        }

        // ── Learn-only mode toggle ────────────────────────────────────────────
        ARIACard(
            containerColor = if (learnOnly)
                ARIAColors.Accent.copy(alpha = 0.08f)
            else
                ARIAColors.Surface,
            modifier = if (learnOnly) Modifier.border(
                1.dp,
                ARIAColors.Accent.copy(alpha = 0.35f),
                RoundedCornerShape(12.dp)
            ) else Modifier
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = if (learnOnly) ARIAColors.Accent else ARIAColors.Muted,
                    modifier = Modifier.size(20.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Learn-Only Mode",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (learnOnly) ARIAColors.Accent else ARIAColors.OnSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        if (learnOnly)
                            "Observing & reasoning — no gestures dispatched"
                        else
                            "Observe, reason, and act on the device",
                        style = MaterialTheme.typography.bodySmall.copy(color = ARIAColors.Muted)
                    )
                }
                Switch(
                    checked = learnOnly,
                    onCheckedChange = { learnOnly = it },
                    enabled = isIdle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ARIAColors.Accent,
                        checkedTrackColor = ARIAColors.Accent.copy(alpha = 0.45f),
                    )
                )
            }
        }

        // ── Control buttons ───────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    focusManager.clearFocus()
                    if (learnOnly) vm.startLearnOnly(goalText.trim(), targetApp.trim())
                    else           vm.startAgent(goalText.trim(), targetApp.trim())
                },
                modifier = Modifier.weight(1f),
                enabled = canStart,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ARIAColors.Primary,
                    disabledContainerColor = ARIAColors.Divider
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    if (learnOnly) "START LEARNING" else "START AGENT",
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = {
                    if (isPaused) vm.startAgent(agentState.currentTask, agentState.currentApp)
                    else          vm.pauseAgent()
                },
                modifier = Modifier.weight(1f),
                enabled = isRunning || isPaused,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ARIAColors.Warning),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(if (isPaused) "RESUME" else "PAUSE", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { vm.stopAgent() },
                modifier = Modifier.weight(1f),
                enabled = isRunning || isPaused,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ARIAColors.Error),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("STOP", fontWeight = FontWeight.Bold)
            }
        }

        // ── Round 12: Session reset ───────────────────────────────────────────
        // Clears the action log, session stats, token stream, and step indicator
        // without stopping the running agent. Guarded by a confirmation dialog.
        var showResetConfirm by remember { mutableStateOf(false) }
        if (showResetConfirm) {
            AlertDialog(
                onDismissRequest = { showResetConfirm = false },
                title   = { Text("Reset session data?", fontWeight = FontWeight.SemiBold) },
                text    = { Text(
                    "Clears the action log, session stats, and token stream. " +
                    "The agent is not stopped — it continues running.",
                    style = MaterialTheme.typography.bodySmall
                )},
                confirmButton = {
                    TextButton(onClick = { vm.resetSession(); showResetConfirm = false }) {
                        Text("Reset", color = ARIAColors.Error, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
                }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = { showResetConfirm = true },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Reset session",
                    modifier = Modifier.size(13.dp),
                    tint = ARIAColors.Muted
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "RESET SESSION",
                    style = MaterialTheme.typography.labelSmall.copy(color = ARIAColors.Muted)
                )
            }
        }

        // ── Active task display ───────────────────────────────────────────────
        if (agentState.currentTask.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ARIAColors.Success.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
                    .border(1.dp, ARIAColors.Success.copy(alpha = 0.28f), RoundedCornerShape(10.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Default.RadioButtonChecked,
                    contentDescription = null,
                    tint = ARIAColors.Success,
                    modifier = Modifier
                        .size(14.dp)
                        .padding(top = 2.dp)
                )
                Text(
                    agentState.currentTask,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = ARIAColors.OnSurface,
                        lineHeight = 18.sp
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ── Task Queue ────────────────────────────────────────────────────────
        ARIACard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.PlaylistPlay,
                        contentDescription = null,
                        tint = ARIAColors.Accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "TASK QUEUE",
                        style = MaterialTheme.typography.labelSmall.copy(color = ARIAColors.Muted)
                    )
                    if (taskQueue.isNotEmpty()) {
                        Text(
                            if (queueAtCapacity) "(${taskQueue.size}/20 — FULL)" else "(${taskQueue.size}/20)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (queueAtCapacity) ARIAColors.Error else ARIAColors.Accent,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (taskQueue.isNotEmpty()) {
                        TextButton(
                            onClick = { vm.clearTaskQueue() },
                            colors = ButtonDefaults.textButtonColors(contentColor = ARIAColors.Error)
                        ) {
                            Text("Clear all", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    IconButton(
                        onClick = { vm.refreshTaskQueue() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = ARIAColors.Muted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Round 17 §108: estimated queue wait time (rough: tasks × 2 min avg).
            if (taskQueue.isNotEmpty()) {
                val estMins = taskQueue.size * 2
                Text(
                    "Est. ~${estMins}m wait  (${taskQueue.size} task${if (taskQueue.size == 1) "" else "s"} × ~2m avg)",
                    style    = MaterialTheme.typography.labelSmall.copy(
                        color      = ARIAColors.Muted,
                        fontFamily = FontFamily.Monospace,
                        fontSize   = 9.sp,
                    ),
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )
                // Round 18 §125: queue fill progress bar (max 20 tasks).
                val queueFill = (taskQueue.size / 20f).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress          = { queueFill },
                    modifier          = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                    color             = ARIAColors.Primary,
                    trackColor        = ARIAColors.Surface3,
                )
            }

            Spacer(Modifier.height(10.dp))

            // ── Separate queue-goal + queue-app fields ──────────────────────
            OutlinedTextField(
                value = queueGoal,
                onValueChange = { queueGoal = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Goal to queue after current task…",
                        style = MaterialTheme.typography.bodySmall.copy(color = ARIAColors.Muted)
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = ARIAColors.Accent,
                    unfocusedBorderColor = ARIAColors.Divider,
                    focusedTextColor     = ARIAColors.OnSurface,
                    unfocusedTextColor   = ARIAColors.OnSurface,
                    cursorColor          = ARIAColors.Accent,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.clearFocus() }),
                maxLines = 2,
            )
            // Round 17 §113: character count under the queue-goal field — matches §100 UX for direct goal.
            if (queueGoal.isNotBlank()) {
                Text(
                    "${queueGoal.length} / 200",
                    style    = MaterialTheme.typography.labelSmall.copy(
                        color      = when {
                            queueGoal.length >= 200 -> ARIAColors.Destructive
                            queueGoal.length >= 160 -> ARIAColors.Warning
                            else                    -> ARIAColors.Muted
                        },
                        fontFamily = FontFamily.Monospace,
                        fontSize   = 9.sp,
                    ),
                    modifier  = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                )
            }
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = queueApp,
                onValueChange = { queueApp = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "App package (optional, e.g. com.google.android.youtube)",
                        style = MaterialTheme.typography.bodySmall.copy(color = ARIAColors.Muted)
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = ARIAColors.Accent,
                    unfocusedBorderColor = ARIAColors.Divider,
                    focusedTextColor     = ARIAColors.OnSurface,
                    unfocusedTextColor   = ARIAColors.OnSurface,
                    cursorColor          = ARIAColors.Accent,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                maxLines = 1,
            )
            // Round 15 §84: priority selector chips for the queue.
            Spacer(Modifier.height(8.dp))
            Text(
                "PRIORITY",
                style = MaterialTheme.typography.labelSmall.copy(color = ARIAColors.Muted, fontSize = 9.sp)
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(-1 to "Low", 0 to "Normal", 1 to "High").forEach { (p, label) ->
                    val selected = queuePriority == p
                    val chipColor = when (p) {
                        1    -> ARIAColors.Destructive
                        -1   -> ARIAColors.Muted
                        else -> ARIAColors.Accent
                    }
                    FilterChip(
                        selected = selected,
                        onClick  = { queuePriority = p },
                        label    = {
                            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
                        },
                        colors   = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = chipColor.copy(alpha = 0.18f),
                            selectedLabelColor     = chipColor,
                        ),
                        border   = FilterChipDefaults.filterChipBorder(
                            enabled          = true,
                            selected         = selected,
                            selectedBorderColor = chipColor.copy(alpha = 0.55f),
                            borderColor         = ARIAColors.Divider,
                        )
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    if (queueGoal.isNotBlank()) {
                        focusManager.clearFocus()
                        vm.enqueueTask(queueGoal.trim(), queueApp.trim(), queuePriority)
                        queueGoal     = ""
                        queueApp      = ""
                        queuePriority = 0
                    }
                },
                enabled = queueGoal.isNotBlank() && !queueAtCapacity,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (queueAtCapacity) ARIAColors.Muted else ARIAColors.Accent
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    if (queueAtCapacity) "Queue Full (max 20)" else "Add to Queue",
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (taskQueue.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = ARIAColors.Divider)
                Spacer(Modifier.height(8.dp))

                taskQueue.take(10).forEachIndexed { index, task ->
                    QueuedTaskRow(
                        index  = index + 1,
                        task   = task,
                        onRemove = { vm.removeQueuedTask(task.id) }
                    )
                    if (index < taskQueue.size - 1 && index < 9) {
                        HorizontalDivider(
                            color = ARIAColors.Divider.copy(alpha = 0.4f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
                if (taskQueue.size > 10) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "… and ${taskQueue.size - 10} more tasks",
                        style = MaterialTheme.typography.labelSmall.copy(color = ARIAColors.Muted)
                    )
                }
            } else {
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.List,
                        contentDescription = null,
                        tint = ARIAColors.Muted,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "No tasks queued · Add a goal above to chain automatically after the current task finishes",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = ARIAColors.Muted,
                            lineHeight = 18.sp
                        )
                    )
                }
            }
        }

        // ── Teach the Agent entry point ───────────────────────────────────────
        ARIACard {
            Text(
                "TEACH THE AGENT",
                style = MaterialTheme.typography.labelSmall.copy(color = ARIAColors.Muted)
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onNavigateToLabeler?.invoke() }
                    ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ARIAColors.Accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Label,
                        contentDescription = null,
                        tint = ARIAColors.Accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Object Labeler",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = ARIAColors.OnSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        "Annotate UI elements · LLM enriches context · Agent learns",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = ARIAColors.Muted,
                            lineHeight = 16.sp
                        )
                    )
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = ARIAColors.Muted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // ── Status banners ────────────────────────────────────────────────────
        if (!moduleState.modelReady) {
            InfoBanner("Model not downloaded. Go to Modules to download it.", ARIAColors.Warning)
        }
        if (!moduleState.accessibilityGranted) {
            InfoBanner("Accessibility permission not granted. Enable ARIA in Accessibility Settings.", ARIAColors.Error)
        }
        if (agentState.lastError.isNotBlank()) {
            InfoBanner("Last error: ${agentState.lastError}", ARIAColors.Error)
        }
    }
}

// ─── Private composables ──────────────────────────────────────────────────────

/**
 * Single-row item inside the permission banner.
 * If [onClick] is non-null the row is tappable and navigates to the relevant setting.
 */
@Composable
private fun PermissionRow(
    label:   String,
    detail:  String,
    onClick: (() -> Unit)?,
) {
    val rowMod = if (onClick != null)
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    else
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)

    Row(
        modifier              = rowMod,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Icon(
            if (onClick != null) Icons.Default.OpenInNew else Icons.Default.Lock,
            contentDescription = null,
            tint     = if (onClick != null) ARIAColors.Warning else ARIAColors.Muted,
            modifier = Modifier.size(14.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall.copy(
                    color      = ARIAColors.OnSurface,
                    fontWeight = FontWeight.SemiBold
                )
            )
            Text(
                detail,
                style = MaterialTheme.typography.labelSmall.copy(color = ARIAColors.Muted)
            )
        }
        if (onClick != null) {
            Text(
                "Fix →",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = ARIAColors.Warning, fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

@Composable
private fun QueuedTaskRow(
    index: Int,
    task: QueuedTaskItem,
    onRemove: () -> Unit
) {
    val fmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(ARIAColors.Primary.copy(alpha = 0.14f))
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Text(
                "#$index",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = ARIAColors.Primary, fontWeight = FontWeight.Bold, fontSize = 10.sp
                )
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                task.goal,
                style = MaterialTheme.typography.bodySmall.copy(color = ARIAColors.OnSurface),
                maxLines = 2
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (task.appPackage.isNotBlank()) {
                    Text(
                        task.appPackage.substringAfterLast('.'),
                        style = MaterialTheme.typography.labelSmall.copy(color = ARIAColors.Muted, fontSize = 10.sp)
                    )
                }
                Text(
                    fmt.format(Date(task.enqueuedAt)),
                    style = MaterialTheme.typography.labelSmall.copy(color = ARIAColors.Muted, fontSize = 10.sp)
                )
                if (task.priority != 0) {
                    Text(
                        "p${task.priority}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = ARIAColors.Warning, fontSize = 10.sp
                        )
                    )
                }
            }
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
            Icon(
                Icons.Default.RemoveCircleOutline,
                contentDescription = "Remove",
                tint = ARIAColors.Error,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
private fun ReadinessRow(label: String, ok: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall.copy(color = ARIAColors.OnSurface))
        Icon(
            if (ok) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = null,
            tint = if (ok) ARIAColors.Success else ARIAColors.Error,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun InfoBanner(message: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Info, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(message, style = MaterialTheme.typography.bodySmall.copy(color = color))
    }
}

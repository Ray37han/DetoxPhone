package dev.minimalist.launcher.detox.ui.dashboard

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dev.minimalist.launcher.detox.domain.model.MonitoredApp
import dev.minimalist.launcher.detox.domain.model.ProtectionLevel
import dev.minimalist.launcher.detox.ui.digest.DigestDrawerSheet
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetoxDashboardScreen(
    onNavigateBack: () -> Unit,
    onAddApp: () -> Unit,
    viewModel: DetoxDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var showDigest by remember { mutableStateOf(false) }
    var showA11yDisclosure by remember { mutableStateOf(false) }
    var expandedAppPackage by remember { mutableStateOf<String?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions()
                viewModel.syncUsageMetrics()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showDigest) {
        DigestDrawerSheet(onDismiss = { showDigest = false })
    }

    // Prominent Disclosure Modal per Section 8 of plan
    if (showA11yDisclosure) {
        AlertDialog(
            onDismissRequest = { showA11yDisclosure = false },
            title = {
                Text(
                    text = "Why Detox Phone Uses the Accessibility API",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Detox Phone uses the Android AccessibilityService API solely on your device to:\n\n" +
                            "• (1) Detect when you open self-selected distracting apps to display your mindful breathing pause.\n" +
                            "• (2) Detect short-video feed containers (such as Reels and Shorts) to automatically exit them when Short-Video Blocking is enabled.\n" +
                            "• (3) Count vertical scroll gestures to display scroll-distance reminders and gentle decay.\n\n" +
                            "Detox Phone does NOT read personal messages, passwords, or keystrokes, and NEVER transmits any data off your device.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showA11yDisclosure = false
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                ) {
                    Text("I Understand & Enable")
                }
            },
            dismissButton = {
                TextButton(onClick = { showA11yDisclosure = false }) {
                    Text("Decline")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detox Dashboard", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onAddApp) {
                        Icon(Icons.Default.Add, contentDescription = "Add app to monitor")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Permission Banners ──────────────────────────────────────────
            if (!uiState.isAccessibilityEnabled) {
                item {
                    PermissionAlertCard(
                        title = "Detox Engine Disabled",
                        description = "Accessibility Service is required to intercept apps, block Reels, and track scrolls.",
                        buttonLabel = "Enable Detox Engine",
                        onClick = { showA11yDisclosure = true }
                    )
                }
            }

            if (!uiState.hasUsageAccess) {
                item {
                    PermissionAlertCard(
                        title = "Usage Access Required",
                        description = "Required to calculate real-time screen time and enforce daily app budgets.",
                        buttonLabel = "Grant Usage Access",
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                    )
                }
            }

            if (!uiState.isNotificationListenerEnabled) {
                item {
                    PermissionAlertCard(
                        title = "Notification Vault Disabled",
                        description = "Required to intercept non-urgent alerts into the Quiet Notification Digest.",
                        buttonLabel = "Enable Notification Access",
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                    )
                }
            }

            // ── Today's Stats ────────────────────────────────────────
            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Today",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "Impulses Deflected",
                        value = "${uiState.totalImpulsesDeflected}",
                        icon = Icons.Default.Shield
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "Screen Time",
                        value = formatMs(uiState.totalScreenTimeMs),
                        icon = Icons.Default.Schedule
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "Scroll Distance",
                        value = "${uiState.estimatedScrollMeters.roundToInt()}m",
                        icon = Icons.Default.SwapVert
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "Notifications",
                        value = "${uiState.unreadNotificationCount} unread",
                        icon = Icons.Default.Notifications,
                        onClick = { showDigest = true }
                    )
                }
            }

            // ── Strict Mode ───────────────────────────────────────────
            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Commitment Lock",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                StrictModeCard(
                    isActive = uiState.strictMode?.isActive == true,
                    onActivate = { viewModel.activateStrictMode(hours = 2) },
                    onDeactivate = { viewModel.deactivateStrictMode() }
                )
            }

            // ── Monitored Apps ─────────────────────────────────────────
            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Protected Apps (${uiState.monitoredApps.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (uiState.monitoredApps.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No apps protected yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                "Tap + to add apps to monitor",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            } else {
                items(uiState.monitoredApps, key = { it.packageName }) { app ->
                    ConfigurableMonitoredAppRow(
                        app = app,
                        isExpanded = expandedAppPackage == app.packageName,
                        onToggleExpand = {
                            expandedAppPackage = if (expandedAppPackage == app.packageName) null else app.packageName
                        },
                        onUpdate = { updatedApp -> viewModel.updateMonitoredApp(updatedApp) },
                        onRemove = { viewModel.removeMonitoredApp(app.packageName) }
                    )
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun PermissionAlertCard(
    title: String,
    description: String,
    buttonLabel: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E0A0A)
        ),
        border = BorderStroke(1.dp, Color(0xFFFF3B30)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFFF3B30),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFFFF3B30)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF3B30),
                    contentColor = Color.White
                ),
                contentPadding = ButtonDefaults.TextButtonContentPadding,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(buttonLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigurableMonitoredAppRow(
    app: MonitoredApp,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onUpdate: (MonitoredApp) -> Unit,
    onRemove: () -> Unit
) {
    val levelLabel = when (app.protectionLevel) {
        ProtectionLevel.NONE -> "None"
        ProtectionLevel.FRICTION_ONLY -> "Friction"
        ProtectionLevel.FRICTION_TIMEBOX -> "Timebox"
        ProtectionLevel.FULL_BLOCK -> "Full Block"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.customAlias ?: app.packageName.substringAfterLast("."),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = app.packageName,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                when (app.protectionLevel) {
                                    ProtectionLevel.FULL_BLOCK -> Color(0xFF2A0A0A)
                                    ProtectionLevel.FRICTION_TIMEBOX -> Color(0xFF222222)
                                    ProtectionLevel.FRICTION_ONLY -> Color(0xFF222222)
                                    ProtectionLevel.NONE -> Color.Transparent
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = levelLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = when (app.protectionLevel) {
                                ProtectionLevel.FULL_BLOCK -> Color(0xFFFF3B30)
                                ProtectionLevel.FRICTION_TIMEBOX -> Color.White
                                ProtectionLevel.FRICTION_ONLY -> Color.White
                                ProtectionLevel.NONE -> Color.White.copy(alpha = 0.5f)
                            }
                        )
                    }

                    Spacer(Modifier.width(8.dp))
                    Icon(
                        if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand settings",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // ── Expanded Engine Configuration Controls ──────────────────────
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    // 1. Protection Level
                    Text("Protection Level", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(6.dp))
                    val levels = listOf(
                        ProtectionLevel.FRICTION_ONLY to "Friction",
                        ProtectionLevel.FRICTION_TIMEBOX to "Timebox",
                        ProtectionLevel.FULL_BLOCK to "Block"
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        levels.forEachIndexed { index, (lvl, label) ->
                            SegmentedButton(
                                selected = app.protectionLevel == lvl,
                                onClick = { onUpdate(app.copy(protectionLevel = lvl)) },
                                shape = SegmentedButtonDefaults.itemShape(index, levels.size)
                            ) {
                                Text(label, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // 2. Engine Toggles
                    EngineSwitchRow(
                        title = "Block Reels / Shorts",
                        subtitle = "Auto-eject short-form video containers",
                        checked = app.blockShortVideos,
                        onCheckedChange = { onUpdate(app.copy(blockShortVideos = it)) }
                    )

                    EngineSwitchRow(
                        title = "Kinetic Scroll Decay",
                        subtitle = "Dim and blur feed after 60 scrolls",
                        checked = app.enableScrollDecay,
                        onCheckedChange = { onUpdate(app.copy(enableScrollDecay = it)) }
                    )

                    EngineSwitchRow(
                        title = "Dynamic Grayscale",
                        subtitle = "Drain color while app is in foreground",
                        checked = app.forceGrayscale,
                        onCheckedChange = { onUpdate(app.copy(forceGrayscale = it)) }
                    )

                    EngineSwitchRow(
                        title = "Batch Notifications",
                        subtitle = "Silence alerts into Quiet Digest",
                        checked = app.batchNotifications,
                        onCheckedChange = { onUpdate(app.copy(batchNotifications = it)) }
                    )

                    Spacer(Modifier.height(10.dp))

                    // 3. Daily Budget & Cooldown Pickers
                    Text("Daily Budget", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(15, 30, 45, 60).forEach { mins ->
                            FilterChip(
                                selected = app.dailyBudgetMinutes == mins,
                                onClick = { onUpdate(app.copy(dailyBudgetMinutes = mins)) },
                                label = { Text("${mins}m", fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text("Session Cooldown", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(5, 10, 15, 30).forEach { mins ->
                            FilterChip(
                                selected = app.cooldownMinutes == mins,
                                onClick = { onUpdate(app.copy(cooldownMinutes = mins)) },
                                label = { Text("${mins}m", fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // 4. Remove button
                    Button(
                        onClick = onRemove,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E0A0A),
                            contentColor = Color(0xFFFF3B30)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFFF3B30).copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Remove Protection", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun EngineSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(
                subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF121212)
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick ?: {}
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color.White
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun StrictModeCard(
    isActive: Boolean,
    onActivate: () -> Unit,
    onDeactivate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Color(0xFF1E0A0A) else Color(0xFF121212)
        ),
        border = BorderStroke(1.dp, if (isActive) Color(0xFFFF3B30) else Color.White.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Strict Mode",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = if (isActive) Color(0xFFFF3B30) else Color.White
                )
                Text(
                    if (isActive) "All protected apps are hard-locked" else "Tap to enable a 2h commitment lock",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            Switch(
                checked = isActive,
                onCheckedChange = { if (it) onActivate() else onDeactivate() }
            )
        }
    }
}

private fun formatMs(ms: Long): String {
    val hours = ms / 3600_000
    val mins = (ms % 3600_000) / 60_000
    return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
}
